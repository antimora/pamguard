package Acquisition;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Timer;

import Array.ArrayManager;
import Array.Hydrophone;
import Array.PamArray;
import Array.Preamplifier;
import PamController.PamController;
import PamDetection.RawDataUnit;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RequestCancellationObject;

/**
 * Data acquisition process for all types of input device. 
 * This arranges the output data block and starts and stops the 
 * device in the detected DaqSystem. Each DaqSystem should 
 * operate a different thread to read the device / file and add its
 * data to the volatile Vector newDataUnits. AcquisitonProcess will 
 * poll newDataUnits on a timer and when new data is found, put that 
 * data into PamDataUnits and PamRawDataBlocks to be sent out for 
 * processing.
 * <p>
 * @author Doug Gillespie
 * @see Acquisition.DaqSystem
 * @see PamguardMVC.PamRawDataBlock
 * @see PamguardMVC.PamDataUnit
 *
 */
public class AcquisitionProcess extends PamProcess {

	AcquisitionControl acquisitionControl;

	private volatile DaqSystem runningSystem = null;

	private PamRawDataBlock rawDataBlock;

	private PamDataBlock<DaqStatusDataUnit> daqStatusDataBlock;

	private volatile List<RawDataUnit> newDataUnits;

	AcquisitionProcess acquisitionProcess;

	private int dataBlockLength = 0;

	long[] totalSamples;

	private volatile boolean keepRunning = false;

	private volatile boolean bufferOverflow = false;

	private Timer restartTimer;

	private final double sqrt2 = Math.sqrt(2.0);

	private Object runingSynchObject = new Object();

	protected AcquisitionProcess(AcquisitionControl acquisitionControl) {

		super(acquisitionControl, null);

		acquisitionProcess = this;

		this.acquisitionControl = acquisitionControl;

		String name = String.format("Raw input data from %s", acquisitionControl.getUnitName());

		//addOutputDataBlock(rawDataBlock = new PamRawDataBlock(name, this,
		//		PamUtils.makeChannelMap(acquisitionControl.acquisitionParameters.nChannels), 
		//		acquisitionControl.acquisitionParameters.sampleRate));

		addOutputDataBlock(rawDataBlock = new PamRawDataBlock(name, this, //Xiao Yan Deng
				PamUtils.makeChannelMap(acquisitionControl.acquisitionParameters.nChannels,acquisitionControl.acquisitionParameters.getHardwareChannelList()),
				acquisitionControl.acquisitionParameters.sampleRate));


		daqStatusDataBlock = new PamDataBlock<DaqStatusDataUnit>(DaqStatusDataUnit.class, acquisitionControl.getUnitName(),
				this, 0);
		addOutputDataBlock(daqStatusDataBlock);
		daqStatusDataBlock.SetLogging(new AcquisitionLogging(daqStatusDataBlock, acquisitionControl));
		daqStatusDataBlock.setMixedDirection(PamDataBlock.MIX_INTODATABASE);

		setupDataBlock();

		restartTimer = new Timer(200, new RestartTimerFunction());
		restartTimer.setRepeats(false);

		bufferTimer = new Timer(1000, new BufferTimerTest());

	}

	protected AcquisitionProcess(AcquisitionControl acquisitionControl, boolean isSimulator) {

		super(acquisitionControl, null);

		acquisitionProcess = this;

		this.acquisitionControl = acquisitionControl;

	}

	/**
	 * called when acquisition parameters change.
	 *
	 */
	public void setupDataBlock() {
		//rawDataBlock.setChannelMap(PamUtils.makeChannelMap(acquisitionControl.acquisitionParameters.nChannels));
		//		rawDataBlock.setChannelMap(PamUtils.makeChannelMap(acquisitionControl.acquisitionParameters.nChannels,
		//				                                            acquisitionControl.acquisitionParameters.getHardwareChannelList()));//Xiao Yan Deng
		rawDataBlock.setChannelMap(PamUtils.makeChannelMap(acquisitionControl.acquisitionParameters.nChannels));
		setSampleRate(acquisitionControl.acquisitionParameters.sampleRate, true);
	}



	private boolean systemPrepared;
	/**
	 * Time of last DAQ check in milliseconds
	 */
	//	private long daqCheckTime;
	/**
	 * Interval between daq checks in milliseconds
	 */
	//	private long daqCheckInterval = 60 * 1000;
	@Override
	public void pamStart() {
		// called by PamController. Don't actually start if 
		// we're in network receive mode. 

		if (systemPrepared == false) return;

		//		before starting, clear all old data
		rawDataBlock.clearAll();

		newDataUnits.clear();
		
		boolean netRX = PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER;
		if (!netRX) {
			if (runningSystem.startSystem(acquisitionControl) == false) return;
		}
		//		daqTimer.start();
		//		if (runningSystem.isRealTime()) {
		//			daqTimer.setDelay(10);
		//		}
		//		else {
		//			daqTimer.setDelay(5);
		//		}
		collectDataThread = new Thread(new WaitForData());
		collectDataThread.setPriority(Thread.MAX_PRIORITY);

		acquisitionControl.daqMenuEnabler.enableItems(false);
		acquisitionControl.fillStatusBarText();

		bufferTimer.start();

		/*
		 * All systems work in much the same way - set up a timer to look for new data which is
		 * put there by a separate thread that gets the data from it's source.
		 * 
		 */

		DaqStatusDataUnit ds = new DaqStatusDataUnit(PamCalendar.getTimeInMillis(), "Start", "", 
				runningSystem.getSystemName(), getSampleRate(), acquisitionControl.acquisitionParameters.nChannels,
				acquisitionControl.acquisitionParameters.voltsPeak2Peak, 0, 0);
		daqStatusDataBlock.addPamData(ds);
		bufferOverflow = false;
		//		daqCheckTime = PamCalendar.getTimeInMillis();
		Timer t = new Timer(1, new ReallyStart());
		t.setRepeats(false);
		t.start();
	}

	Thread collectDataThread;
	class ReallyStart implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			collectDataThread.start();
		}

	}

	@Override
	public void pamStop() {
		// called by PamController.
		// stop the running system - not the selected system since
		// this may have changed
		restartTimer.stop();
		pamStop("");


	}

	public void pamStop(String reason) {
		if (runningSystem == null) return;
		if (reason == null) reason = new String("");
		if (totalSamples == null) {
			return;
		}
		double duration = (double) totalSamples[0] / getSampleRate();
		double clockError = checkClockSpeed(totalSamples[0], 1);
		DaqStatusDataUnit ds = new DaqStatusDataUnit(PamCalendar.getTimeInMillis(), "Stop", reason, 
				runningSystem.getSystemName(), getSampleRate(), acquisitionControl.acquisitionParameters.nChannels,
				acquisitionControl.acquisitionParameters.voltsPeak2Peak, duration, clockError);
		daqStatusDataBlock.addPamData(ds);

		runningSystem.stopSystem(acquisitionControl);

		keepRunning = false;

		bufferTimer.stop();

		acquisitionStopped();

	}

	public void acquisitionStopped() {
		/* 
		 * can get called by a DaqSystem thread just as it exits to 
		 * say that DAQ has stopped. Only needs to be implemented for things
		 * like files which will stop themselves. Can also be implemented for 
		 * other devices which might stop accidentally (e.g. UDP sources)
		 */

		DaqSystem selectedSystem = runningSystem;

		if (collectDataThread == null) {
			return;
		}
		//		daqTimer.stop();
		while (collectDataThread.isAlive()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// call acquireData one last time to make sure that 
		// all data have been flushed from the buffer. 
		if (bufferOverflow = false) {
			acquireData();
		}
		else {
			clearData();
		}

		acquisitionControl.daqMenuEnabler.enableItems(true);

		/*
		 * runningSystem needs to be set null here since the call to PamController.PamStop()
		 * will call back to pamStop and we'll get an infinite loop !
		 * Synch on runingSynchObject to stop crash during shut down of system. 
		 */
		synchronized(runingSynchObject) {
			runningSystem = null;
		}
		//		System.out.println("Set running system null");

		acquisitionControl.fillStatusBarText();

		acquisitionControl.pamController.pamStop();

		if (selectedSystem == null){
			selectedSystem = acquisitionControl.findDaqSystem(null);
		}
		if (selectedSystem != null) {
			selectedSystem.daqHasEnded();
		}


	}

	@Override
	// prepares data acquisition
	public void prepareProcess() {

		systemPrepared = false;

		setSampleRate(acquisitionControl.acquisitionParameters.sampleRate, true);

		super.prepareProcess();

		if (runningSystem != null) {
			pamStop();
		}
		dataBlockLength = -1;
		totalSamples = new long[PamConstants.MAX_CHANNELS];
		runningSystem = acquisitionControl.findDaqSystem(null);
		if (runningSystem == null) return;
		newDataUnits = Collections.synchronizedList(new LinkedList<RawDataUnit>());


		systemPrepared = runningSystem.prepareSystem(acquisitionControl);

	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		acquisitionControl.acquisitionParameters.sampleRate = sampleRate;
		//		System.out.println("Acquisition set sample rate to " + sampleRate);
		super.setSampleRate(sampleRate, notify);

	}

	public void setNumChannels(int numChannels) {  //Xiao Yan Deng commented
		acquisitionControl.acquisitionParameters.nChannels = numChannels;
		rawDataBlock.setChannelMap(PamUtils.makeChannelMap(numChannels));
	}

	/**
	 * Set up channels when using a channel list - note
	 * that hardware channels are no longer passed through the system
	 * so software channels are now used throughout. 
	 * @param numChannels
	 * @param channelList
	 */
	public void setNumChannels(int numChannels, int[] channelList) { //Xiao Yan Deng
		acquisitionControl.acquisitionParameters.nChannels = numChannels;
		acquisitionControl.acquisitionParameters.setChannelList(channelList);
		rawDataBlock.setChannelMap(PamUtils.makeChannelMap(numChannels));
		//		rawDataBlock.setChannelMap(PamUtils.makeChannelMap(numChannels, channelList)); 
	}

	/*
	 * The timer looks for new data in the vector newDataUnits.
	 * Whenever new data is there it is taken out and passed on
	 * to the rest of Pamguard by adding it to the datablock.
	 */
	//	Timer daqTimer = new Timer(5, new ActionListener() {
	//		public void actionPerformed(ActionEvent evt) {
	////			runTimerActions();
	//		}
	//	});

	Timer bufferTimer;

	class BufferTimerTest implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			acquisitionControl.fillStatusBarText();

			if (needRestart()) {
				bufferOverflow = true;
				restartTimer.start();
			}
		}

	}

	class WaitForData implements Runnable {

		@Override
		public void run() {
			keepRunning = true;
			while (keepRunning) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (acquireData() == false) {
					break;
				}
			}
		}

	}


	int trials = 0;
	int counts = 0;
	double maxLevel;
	long levelSamples;
	double[] rawData;

	public void streamClosed() {
		//		acquisitionStopped();
	}
	public void streamEnded() {
		//		acquisitionStopped();
	}
	public void streamOpen() {

	}
	public void streamPaused() {

	}


	/**
	 * Used to restart after a buffer overflow. 
	 * @author Doug Gillespie
	 *
	 */
	class RestartTimerFunction implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {

			System.out.println("PAMGUARD cannot process data at the required rate and is restarting");

			restartTimer.stop();

			PamController.getInstance().pamStop();

			PamController.getInstance().pamStart(false);

		}

	}

	private boolean acquireData() {
		if (runningSystem == null) {
			return false;
		}
		RawDataUnit newDataUnit, threadDataUnit;

		int readCount = 0;
		while (!newDataUnits.isEmpty()) {
//			System.out.println("size:"+newDataUnits.size());
			threadDataUnit = newDataUnits.remove(0);

			int channel = PamUtils.getSingleChannel(threadDataUnit.getChannelBitmap());
			newDataUnit = new RawDataUnit(absSamplesToMilliseconds(threadDataUnit.getStartSample()), 
					threadDataUnit.getChannelBitmap(), threadDataUnit.getStartSample(),
					threadDataUnit.getDuration());
			newDataUnit.setRawData(threadDataUnit.getRawData(), true);
			acquisitionControl.setStatusBarLevel(newDataUnit.getMeasuredAmplitude());
			// can also here convert the measured amplitude (which was
			// calculated in the call to setRawData, to dB.
			newDataUnit.setCalculatedAmlitudeDB(rawAmplitude2dB(newDataUnit.getMeasuredAmplitude(),
					PamUtils.getSingleChannel(threadDataUnit.getChannelBitmap()), false));
			update(null, newDataUnit);
			rawData = newDataUnit.getRawData();
			dataBlockLength = rawData.length;
			totalSamples[channel] += dataBlockLength;
			for (int i = 0; i < rawData.length; i++) {
				maxLevel = Math.max(maxLevel, Math.abs(rawData[i]));
			}
			levelSamples += rawData.length;
			if (bufferOverflow) {
				break;
			}
		}
		return true;
	}

	private void clearData() {
		newDataUnits.clear();
	}

	//	private void streamRunning(boolean finalFlush) {
	//		if (runningSystem == null) return;
	//		if (runningSystem.isRealTime()) {
	//			/*
	//			 * Do a test to see if we're getting too far behind. For now hard wire a
	//			 * buffer with a 10s maximum
	//			 */
	//			if (needRestart() && finalFlush == false) {
	//				
	//				System.out.println(PamCalendar.formatDateTime(System.currentTimeMillis()) + 
	//						" : Emergency sound system restart due to buffer overflow");
	//				pamStop("Buffer overflow in sound system");
	//				
	//				newDataUnits.clear();
	//				
	//				acquisitionStopped();
	//				
	//				restartTimer.start();
	//				
	//				return;
	//			}
	//			
	//			long now = PamCalendar.getTimeInMillis();
	//			if (now - daqCheckTime >= daqCheckInterval) {
	//				double duration = (double) totalSamples[0] / getSampleRate();
	//				double clockError = checkClockSpeed(totalSamples[0], 1);
	//				DaqStatusDataUnit ds = new DaqStatusDataUnit(PamCalendar.getTimeInMillis(), "Continue", "Check", 
	//						runningSystem.getSystemName(), getSampleRate(), acquisitionControl.acquisitionParameters.nChannels,
	//						acquisitionControl.acquisitionParameters.voltsPeak2Peak, duration, clockError);
	//				daqStatusDataBlock.addPamData(ds);
	//				daqCheckTime = now;
	//			}
	//			
	//			/*
	//			 * The blocks should be in pairs, so there should generally 
	//			 * be two blocks there every time this gets called. Adjust timing
	//			 * automatically to deal with just about any data rate. Start at a low
	//			 * value though since file reading only adds blocks if there are none
	//			 * there - so would never reduce the delay ! 
	//			 * 
	//			 * Don't do this if it isn't a real time process since we want to 
	//			 * keep going as fast as possible
	//			 */
	////			trials++;
	////			counts += newDataUnits.size();
	////			if (trials == 15 || counts >= 40) {
	////				if (trials > counts * 3) {
	////					daqTimer.setDelay(Math.max(10,daqTimer.getDelay() * 5 / 4));
	////					System.out.println("Increasing timer delay to " + daqTimer.getDelay() + " ms");					
	////				}
	////				else if (counts > trials * 2) {
	////					daqTimer.setDelay(Math.max(1,daqTimer.getDelay() * 2 / 3));
	////					System.out.println("Reducing timer delay to " + daqTimer.getDelay() + " ms");		
	////				}
	////				trials = counts = 0;
	////			}
	//		}
	//		
	//		RawDataUnit newDataUnit, threadDataUnit;
	//		
	//		int readCount = 0;
	//		while (!newDataUnits.isEmpty()) {
	//
	//			threadDataUnit = newDataUnits.remove(0);
	//			
	//			int channel = PamUtils.getSingleChannel(threadDataUnit.getChannelBitmap());
	//			newDataUnit = new RawDataUnit(absSamplesToMilliseconds(threadDataUnit.getStartSample()), 
	//					threadDataUnit.getChannelBitmap(), threadDataUnit.getStartSample(),
	//					threadDataUnit.getDuration());
	//			newDataUnit.setRawData(threadDataUnit.getRawData(), true);
	//			// can also here convert the measured amplitude (which was
	//			// calculated in the call to setRawData, to dB.
	//			newDataUnit.setCalculatedAmlitudeDB(rawAmplitude2dB(newDataUnit.getMeasuredAmplitude(),
	//					PamUtils.getSingleChannel(threadDataUnit.getChannelBitmap()), false));
	//			update(null, newDataUnit);
	//			rawData = newDataUnit.getRawData();
	//			dataBlockLength = rawData.length;
	//			totalSamples[channel] += dataBlockLength;
	//			for (int i = 0; i < rawData.length; i++) {
	//				maxLevel = Math.max(maxLevel, Math.abs(rawData[i]));
	//			}
	//			levelSamples += rawData.length;
	//			
	//
	//			// about every 5 seconds, check the buffer isn't filling
	//			if (newDataUnit.getAbsBlockIndex() % (50 * acquisitionControl.acquisitionParameters.nChannels) == 0) {
	//				double buffer = getBufferEstimate(newDataUnit.getStartSample() + newDataUnit.getDuration());
	//				if (buffer > 3) {
	//					System.out.println(PamCalendar.formatDateTime(System.currentTimeMillis()) + 
	//					" : Emergency sound system restart due to Buffer overflow type 2");
	//					pamStop("Type 2 Buffer overflow in sound system");
	//
	//					newDataUnits.clear();
	//
	//					acquisitionStopped();
	//
	//					restartTimer.start();
	//
	//					return;
	//				}
	//			}
	//				
	//			// about every minute, or every 1200 blocks, check the timing
	//			if ((newDataUnit.getAbsBlockIndex()+1) % (600 * acquisitionControl.acquisitionParameters.nChannels) == 0) {
	//				checkClockSpeed(newDataUnit.getStartSample() + newDataUnit.getDuration(), 1);
	//			}
	//			// only ever stay in here for 10 reads - if it's getting behind, tough !
	//			// need to keep the GUI going whatever.
	//			if (++readCount >= acquisitionControl.acquisitionParameters.nChannels * 4 && finalFlush == false) {
	//				break;
	//			}
	//			
	//		}
	//		if (levelSamples >= sampleRate * acquisitionControl.acquisitionParameters.nChannels * 2) {
	//			acquisitionControl.setStatusBarLevel(maxLevel);
	//			acquisitionControl.fillStatusBarText();
	//			levelSamples = 0;
	//			maxLevel = 0;
	//		}
	//	}

	/**
	 * Check and optionally display clock speed error
	 * @param totalSamples total samples acquired
	 * @param print 0 = never, 1 = if the error is > .2%, 2 always
	 * @return % clock speed error
	 */
	private double checkClockSpeed(long totalSamples, int print) {
		double clockSeconds = (PamCalendar.getTimeInMillis() - PamCalendar.getSessionStartTime()) / 1000.;
		double sampleSeconds = (double) totalSamples / getSampleRate();
		if (clockSeconds == 0) {
			return 0;
		}
		double soundCardError = (sampleSeconds - clockSeconds) / clockSeconds * 100;
		int missingSamples = (int) ((sampleSeconds - clockSeconds) * getSampleRate());
		//if (print >= 2 || (print > 1 && shouldPrintSoundCardError(soundCardError))) {
		System.out.println(String.format("%s at %3.2f%% PC clock speed after %d seconds (about %3.1f seconds or %d samples)", 
				runningSystem.getSystemName(), soundCardError, (int)clockSeconds, (sampleSeconds - clockSeconds), missingSamples));
		//}
		return soundCardError;
	}

	private double lastError = 0;
	private boolean shouldPrintSoundCardError(double error) {
		if (lastError == error) {
			return false;
		}
		double change = Math.abs(error / (error - lastError));

		lastError = error;
		return (Math.abs(error) > 0.05 && change > 0.1);
	}

	//	private double getBufferEstimate(long totalSamples) {
	//		double clockSeconds = (PamCalendar.getTimeInMillis() - PamCalendar.getSessionStartTime()) / 1000.;
	//		double sampleSeconds = (double) totalSamples / sampleRate;
	//		return clockSeconds - sampleSeconds;
	//	}

	public double getBufferSeconds()
	{
		if (newDataUnits == null) return 0;
		if (dataBlockLength <= 0) return 0;
		double blocksPerSecond = getSampleRate() / dataBlockLength * 
		acquisitionControl.acquisitionParameters.nChannels;
		return newDataUnits.size() / blocksPerSecond;
	}

	/**
	 * 
	 * @return the maximum number of seconds of data which can be buffered. 
	 * This used to be fixed at 3, but now that individual raw data blocks contain >> 1s
	 * of data for low frequency DAQ, this can be exceeded in a single
	 * data unit, which causes continual resets. 
	 */
	public double getMaxBufferSeconds() {
		/**
		 * return a default of 3s for standard 1/10s data units.
		 */
		return runningSystem.getDataUnitSamples() / getSampleRate() * 30;
	}

	public boolean needRestart() {
		if (getBufferSeconds() > getMaxBufferSeconds()) {
			return true;
		}
		return false; 
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		rawDataBlock.addPamData((RawDataUnit)arg);
		rawDataBlock.masterClockUpdate(PamCalendar.getTimeInMillis(), ((RawDataUnit)arg).getStartSample());
	}

	public List<RawDataUnit> getNewDataUnits() {
		return newDataUnits;
	}

	public DaqSystem getRunningSystem() {
		return runningSystem;
	}

	/**
	 * Convert a raw amplitude to dB re 1 micropascal based on
	 * calibration information held in the AcquisitionController
	 * 
	 * @param rawAmplitude raw amplitude (should be -1 < rawAmplitude < 1)
	 * @return amplitude in dB re 1 uPa.
	 */
	public double rawAmplitude2dB(double rawAmplitude, int channel, boolean fast){

		double constantTerm;
		if (fast) constantTerm = fixedAmplitudeConstantTerm;
		else constantTerm = getAmplitudeConstantTerm(channel); 

		double vp2p = getPeak2PeakVoltage(channel);

		/*
		 * Need an extra divide by 2 in here since the standard scaling of PAMGUARD
		 * data is -1 to +1, so data really needed to be scaled against half
		 * the peak to peak voltage. 
		 */
		double dB = 20 * Math.log10(rawAmplitude * vp2p / 2) - constantTerm;

		return dB;
	}


	/**
	 * Some devices may be setting this per channel.
	 * @param swChannel software channel number
	 * @return peak to peak voltage range. 
	 */
	public double getPeak2PeakVoltage(int swChannel) {

		synchronized(runingSynchObject) {
			if (runningSystem == null) {
				return acquisitionControl.acquisitionParameters.voltsPeak2Peak;
			}
			double vpv = runningSystem.getPeak2PeakVoltage(swChannel);
			if (vpv < 0) {
				return acquisitionControl.acquisitionParameters.voltsPeak2Peak;
			}
			return vpv;
		}
	}

	/**
	 * A Constant used for fast amplitude calculations when things
	 * like preamp gain will remain constant. Contains a constant
	 * term in the SPL calculations bases on preamp gains and 
	 * hdrophone sensitivities. 
	 */
	double fixedAmplitudeConstantTerm;
	
	DaqSystem ampSystem;
	/**
	 * Gets the fixedAmplitudeConstantTerm based on channel and hydrophone
	 * numbers. 
	 * @param channel = single software channel
	 * @return constant term for amplitude calculations
	 */
	private double getAmplitudeConstantTerm(int channel) {
		// need to fish around a bit working out which hydrophone it is, etc.
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		int hydrophoneChannel = acquisitionControl.getChannelHydrophone(channel);
		if (hydrophoneChannel < 0) hydrophoneChannel = 0;
		Hydrophone hydrophone = array.getHydrophone(hydrophoneChannel);
		if (hydrophone == null) return 0;
		Preamplifier preamp = acquisitionControl.acquisitionParameters.preamplifier;
		if (getRunningSystem() != null) {
			ampSystem = getRunningSystem();
		}
		if (ampSystem == null) {
			ampSystem = acquisitionControl.findDaqSystem(null);
		}
		double xtra = 0;
		if (ampSystem != null) {
			xtra = ampSystem.getChannelGain(channel); 
		}
		return (hydrophone.getSensitivity() + hydrophone.getPreampGain() + preamp.getGain() + xtra);
	}
	/**
	 * Prepares for fast amplitude calculations
	 * @param channel
	 */
	public void prepareFastAmplitudeCalculation(int channel) {
		fixedAmplitudeConstantTerm = getAmplitudeConstantTerm(channel);
	}

	/**
	 * Convert a raw amplitude to dB re 1 micropascal based on
	 * calibration information held in the AcquisitionController
	 * for an array of double data
	 * 
	 * @param rawAmplitude raw amplitude (should be -1 < rawAmplitude < 1)
	 * @return amplitude in dB re 1 uPa.
	 */
	public double[] rawAmplitude2dB(double[] rawAmplitude, int channel){
		double[] ans = new double[rawAmplitude.length];
		prepareFastAmplitudeCalculation(channel);
		for (int i = 0; i < rawAmplitude.length; i++) {
			ans[i] = rawAmplitude2dB(rawAmplitude[i], channel, true);
		}
		return ans;
	}
	/**
	 * Convert the amplitude of fft data into a spectrum level measurement in
	 * dB re 1 micropacal / sqrt(Hz).
	 * @param fftAmplitude magnitude of the fft data (not the magnitude squared !)
	 * @param fftLength lengthof the fft (needed for Parsevals correction) 
	 * @return spectrum level amplitude.
	 */
	public double fftAmplitude2dB(double fftAmplitude, int channel, int fftLength, boolean isSquared, boolean fast){
		if (isSquared) {
			fftAmplitude = Math.sqrt(fftAmplitude);
		}
		// correct for PArsevel (1/sqrt(fftLength and for the fact that the data were summed
		// over a fft length which requires an extra 1/sqrt(fftLength) correction.
		fftAmplitude /= fftLength;
		// allow for negative frequencies
		fftAmplitude *= sqrt2;
		// thats the energy in an nHz bandwidth. also need bandwidth correction to get
		// to spectrum level data
		double binWidth = getSampleRate() / fftLength;
		fftAmplitude /= Math.sqrt(binWidth);
		double dB = rawAmplitude2dB(fftAmplitude, channel, fast);
		return dB;
	}/**
	 * Convert the amplitude of fft data into a  level measurement in
	 * dB re 1 micropacal / sqrt(Hz).
	 * <p>
	 * Note that this function differs from fftAmplitude2dB in that this one used the 
	 * FFT length to correct for Parsevals theorum and integratin over the length of the 
	 * FFT, but it does NOT convert the result to a spectrum level measurement.  
	 * @param fftAmplitude magnitude of the fft data (not the magnitude squared !)
	 * @param fftLength lengthof the fft (needed for Parsevals correction) 
	 * @return  level amplitude in dB
	 */
	public double fftBandAmplitude2dB(double fftAmplitude, int channel, int fftLength, boolean isSquared, boolean fast){
		if (isSquared) {
			fftAmplitude = Math.sqrt(fftAmplitude);
		}
		// correct for PArsevel (1/sqrt(fftLength and for the fact that the data were summed
		// over a fft length which requires an extra 1/sqrt(fftLength) correction.
		fftAmplitude /= fftLength;
		// allow for negative frequencies
		fftAmplitude *= sqrt2;
		// thats the energy in an nHz bandwidth. also need bandwidth correction to get
		// to spectrum level data
		//		double binWidth = sampleRate / fftLength;
		//		fftAmplitude /= Math.sqrt(binWidth);
		double dB = rawAmplitude2dB(fftAmplitude, channel, fast);
		return dB;
	}

	/**
	 * Convert the amplitude of fft data into a spectrum level measurement in
	 * dB re 1 micropacal / sqrt(Hz) for an array of double values.
	 * @param fftAmplitude magnitude of the fft data (not the magnitude squared !)
	 * @param fftLength lengthof the fft (needed for Parsevals correction) 
	 * @return spectrum level amplitude.
	 */
	public double[] fftAmplitude2dB(double[] fftAmplitude, int channel, int fftLength, boolean isSquared){
		double[] ans = new double[fftAmplitude.length];
		prepareFastAmplitudeCalculation(channel);
		for (int i = 0; i < fftAmplitude.length; i++) {
			ans[i] = fftAmplitude2dB(fftAmplitude[i], channel, fftLength, isSquared, true);
		}
		return ans;
	}

	public double dbMicropascalToSignal(int channel, double dBMuPascal) {
		double db = dBMuPascal + getAmplitudeConstantTerm(channel);
		return Math.pow(10, db/20);
	}

	/**
	 * @return Returns the acquisitionControl.
	 */
	public AcquisitionControl getAcquisitionControl() {
		return acquisitionControl;
	}

	/**
	 * @return the rawDataBlock
	 */
	public PamRawDataBlock getRawDataBlock() {
		return rawDataBlock;
	}

	@Override
	public int getOfflineData(PamDataBlock dataBlock, PamObserver endUser, long startMillis,
			long endMillis, RequestCancellationObject cancellationObject) {
		if (acquisitionControl.getOfflineFileServer() == null) {
			return PamDataBlock.REQUEST_NO_DATA;
		}
		if (acquisitionControl.getOfflineFileServer().loadData(getRawDataBlock(), startMillis, 
				endMillis, cancellationObject, null)) {
			return PamDataBlock.REQUEST_DATA_LOADED;
		}
		else {
			return PamDataBlock.REQUEST_NO_DATA;
		}
	}

}
