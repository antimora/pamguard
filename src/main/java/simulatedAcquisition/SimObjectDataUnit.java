package simulatedAcquisition;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

import propagation.PropagationModel;

import Acquisition.AcquisitionProcess;
import Array.ArrayManager;
import Array.HydrophoneLocator;
import Array.PamArray;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;

public class SimObjectDataUnit extends PamDataUnit {

	private SimObject simObject;

	protected LatLong currentPosition = null;
	
	protected long lastUpdateTime;
	
	private SimProcess simProcess;
	
	private SimSignal simSignal;
	
	private Random random = new Random();
	
	private LinkedList<SimSound> simSounds = new LinkedList<SimSound>();
	
	private long lastGenSample = 0;
	
	public SimObjectDataUnit(SimProcess simProcess, SimObject simObject, long firstSigTime) {
		super(0);
		this.simProcess = simProcess;
		this.simObject = simObject;
		simObject.simObjectDataUnit = this;
		
	}
	
	synchronized protected void prepareSimulation() {
		lastUpdateTime = 0;
		lastGenSample = 0;
		clearSignals();
		System.out.println("Prepare simulation object");
	}
	synchronized protected void clearSignals() {
		simSounds.clear();
	}
	
	/**
	 * Generate the next sound - this is called whenever there is no sound 
	 * in the current list or when the last sound has been started. 
	 * @param snipStartSample start sample of the snip from which this sound was generated
	 */
	protected void genSignal(long snipStartSample) {
//		System.out.println("Generate sound " + lastGenSample);
		simSignal = getSimSignal();
		if (simSignal == null) {
			System.out.println("Can't generate");
			return;
		}
		double sampleRate = simProcess.getSampleRate();
		if (simObject.randomIntervals == false && lastGenSample > 0) {
			lastGenSample += (long) (sampleRate * simObject.meanInterval);
		}
		else {
			lastGenSample += (long) (random.nextDouble() * 2 * sampleRate * simObject.meanInterval);
		}
		SimSound simSound = new SimSound(lastGenSample, PamCalendar.getTimeInMillis(), 
				currentPosition, simObject.getHeight(), simSignal.getSignal());
		/*
		 * At this point we should work out the time delay to each hydrophone
		 * and store it in the sound
		 */
		AcquisitionProcess daqProcess = simProcess.getDaqControl().getAcquisitionProcess();
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		HydrophoneLocator hLocator = array.getHydrophoneLocator();
		long timeMillis = PamCalendar.getTimeInMillis();
//		AcquisitionParameters daqParam = simProcess.g
		int n = array.getHydrophoneCount();
		double[][] delays = new double[n][]; // transmission delays
		double[][] gains = new double[n][]; // transmission gains
		LatLong phoneLatLong;
		double dist;
		PropagationModel propagationModel = simProcess.getPropagationModel();
		double[] sigAmplitude = new double[n]; // signal amplitude if it were at 1 m.
		for (int i = 0; i < n; i++) {
			propagationModel.setLocations(hLocator.getPhoneLatLong(timeMillis, i), 
					hLocator.getPhoneHeight(timeMillis, i), currentPosition, simObject.getHeight());
			delays[i] = propagationModel.getDelays();
			gains[i] = propagationModel.getGains();
			sigAmplitude[i] = daqProcess.dbMicropascalToSignal(i, simObject.amplitude);
		}
		simSound.setHydrophoneDelays(delays);
		simSound.setTransmissionGains(gains);
		simSound.setSoundAmplitude(sigAmplitude);
		// work out the amplitude - from dB !
		double ampDB = simObject.amplitude;
		
		/*
		 * At the start before the hydrophone locator is working correctly
		 * it can get a very bad position. Check the time delay is less 
		 * than a few seconds. 
		 */
		if (delays[0][0] < 10) {
			simSounds.add(simSound);
//			System.out.println("Sound generated (s)" + delays[0][0]);
		}
		else {
//			System.out.println("Bad simulation - sound too far off (s) " + delays[0][0]);
			lastGenSample = PamCalendar.getTimeInMillis() + 1000;
		}
	}
	
	/**
	 * Iterate through the current list of sounds and add their signals
	 * at the appropriate time within this data unit. 
	 * @param snip waveform data for a channel of this data unit
	 * @param phone hydrophone number
	 * @param snipStartSample start sample of the current data unit.
	 */
	synchronized protected void takeSignals(double[] snip, int phone, long snipStartSample) {
		/*
		 *  iterate through list of sounds. 
		 *  if there are none, or we've started taking from the last one
		 *  then generate a new one
		 *  If a sound is complete, delete it.  
		 */
		double startSample;
		double endSample;
		ListIterator<SimSound> li = simSounds.listIterator();
		SimSound simSound = null;
		double[] w;
		double simPos;
		double simOffset;
		double sampleRate = simProcess.getSampleRate();
		int nDelays = simProcess.getPropagationModel().getNumPaths();
		double tranmissionGain, amplitude;
		
		// iterate over list of simulated sounds. 
		while (li.hasNext()) {
			simSound = li.next();
			for (int iD = 0; iD < nDelays; iD++) {
				// absolute start sample of the simulated sound after being delayed. 
				w = simSound.waveform;
				startSample = simSound.startSample + (simSound.getHydrophoneDelay(phone, iD) * sampleRate);
//				startSample = simSound.startSample - (int) (simSound.getHydrophoneDelay(phone, iD) * sampleRate);
				endSample = startSample + w.length;
//				System.out.println(String.format("Sim sound start %3.3f / %3.3f, Snip start %3.3f",
//						startSample / sampleRate, simSound.startSample / sampleRate, snipStartSample / sampleRate));
				if (endSample < snipStartSample) {
					simSound.setCompleteChannel(phone);
					continue; // This sound has not reached us yet. 
				}
				if (startSample >= snipStartSample + snip.length) {
					continue; // we've passed the end of this sound.
				}
				tranmissionGain = simSound.getTranmissionGain(phone, iD);
				amplitude = simSound.getSoundAmplitude(phone);
				// now find the overlapping region and copy it into the snip.
				// the generated sound is probably shorter than the snip
				// so loop through the gen sound
				w = simSound.waveform;
				simSound.started = true;
				simOffset = startSample - snipStartSample;
//				System.out.println(String.format("Sim sound Chan %d start %3.3f, Snip start %3.3f, offset %3.2f samples",
//						phone, startSample / sampleRate, snipStartSample / sampleRate, simOffset));
						
				// i is the sample number within the data unit we're generating (the snip)
				int i = (int) simOffset;
				i = Math.max(i, 0);
				while (i < snip.length) {
//				for (i; i < w.length; i++) {
					// calculate the exact position within the simulated sound. 
					simPos = i - simOffset;
					if (simPos > w.length) {
						// only breaks after a complete extra sample since we're interpolating !
						break;
					}
					snip[i] += getInterpolatedAmplitude(w, simPos) *
					amplitude * tranmissionGain / 2; 
					i++;
				}
			}
		}
		// here, simSound will either be null or the last sound
		// if null or last sound has started, then generate new
		if (simSound == null || simSound.started) {
			genSignal(snipStartSample);
		}
	}
	
	/**
	 * Get the amplitude of the simulated signal using polynomial interpolation
	 * to estimate the amplitude at the (non-integer) sample
	 * @param signal simulated signal
	 * @param intSample non-integer sample number
	 * @param offset offset from integer value. 
	 * @return estimated amplitude. 
	 */
	double getInterpolatedAmplitude(double[] signal, double simSample) {
		double v1, v2, v3;
		int midSample = (int) (simSample + 0.5);
		double offSet = simSample-midSample;
		v1 = pickSample(signal, midSample-1);
		v2 = pickSample(signal, midSample);
		v3 = pickSample(signal, midSample+1);
		double a = (v3+v1-2*v2)/2.;
		double b = (v3-v1)/2;
//		return v2;
		return a*offSet*offSet + b*offSet + v2;
	}
	
	/**
	 * Pick a sample out of a signal, returning 0 if the sample 
	 * number is outwith the bounds of the signal array. 
	 * @param signal signal
	 * @param sample sample number
	 * @return sample value or 0
	 */
	private double pickSample(double[] signal, int sample) {
		if (sample >= 0 && sample < signal.length) {
			return signal[sample];
		}
		else {
			return 0;
		}
	}
	
	synchronized protected void clearOldSounds(int channelMap) {
		ListIterator<SimSound> li = simSounds.listIterator();
		SimSound simSound = null;
		while (li.hasNext()) {
			simSound = li.next();
			if (simSound.isComplete(channelMap)) {
				li.remove();
//				System.out.println("Remove generated sound");
			}
		}
	}

	public SimObject getSimObject() {
		return simObject;
	}

	public void setSimObject(SimObject simObject) {
		this.simObject = simObject;
	}
	
	public SimSignal getSimSignal() {
		if (simSignal == null) {
			if (simObject.signalName == null) {
				simObject.signalName = "Impulse";
			}
			setSimSignal();
		}
		return simSignal;
	}

	public void setSimSignal(SimSignal simSignal) {
		this.simSignal = simSignal;
	}
	
	public boolean setSimSignal() {
		simSignal = simProcess.simSignals.findSignal(simObject.signalName);
		return (simSignal != null);
	}
	

}
