package echoDetector;

import java.util.ArrayList;

import clickDetector.ClickDetection;
import fftManager.Complex;
import fftManager.FFT;
import fftManager.FastFFT;
import Acquisition.AcquisitionProcess;
import PamController.PamController;
import PamUtils.PamUtils;
import PamView.PamDetectionOverlayGraphics;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

/**
 * This class subscribes to ClickDetection data blocks and processes the 
 * raw data in those blocks. EchoProcess creates EchoDataUnits, which contain
 * the computed cepstrum as well as the estimated delay. 
 * @author Brian Miller
 *
 */
public class EchoProcess extends PamProcess {
	/**
	 * Refefence to PamContolledUnit for this module
	 */
	EchoController echoController;
	
	/**
	 * Reference to the click data unit
	 */
	//ClickDataBlock clickDataBlock;
	PamDataBlock<ClickDetection> clickDataBlock;
	/**
	 * Datablock for output data.
	 */
	PamDataBlock<EchoDataUnit> outputDataBlock;
	
	/**
	 * bitmap of channels in use. 
	 */
	int usedChannels; // bitmap of channels being analysed. 
	
	/**
	 * reference to a list of detectors handling data from a single channel each. 
	 */
	ChannelDetector[] channelDetectors;
	
	/**
	 * Time limits for echo detection. (In echoProcessParameters)
	 * These are in samples, not seconds or milliseconds 
	 */
	int minEchoSample, maxEchoSample, echoDuration;
	
	/**
	 * Echo detection threshold as a simple ratio (not dB !)
	 * If the echo detection function peak does not exceed this
	 * ratio, then do not consider this as a valid echo detection
	 */
	double threshold;
	
	/**
	 * At some point we'll need to get back to the original ADC data
	 * and hydrophone information in order compute the maxEchoSample
	 *  - so we'll need daqProcess. 
	 */
	AcquisitionProcess daqProcess;

	private int dataSampleRate;
	
	public EchoProcess(EchoController echoController) {
		// you must call the constructor from the super class. 
		super(echoController, null);
		
		// keep a reference to the PamControlledUnit controlling this process. 
		this.echoController = echoController;
		
		/* create an output data block and add to the output block list so that other detectors
		 * can see it. Also set up an overlay graphics class for use with the data block. 
		 */ 
		/*
		 * My preferred method now days s subclass off PamDataBlock to some other class
		 * I may even make PamDataBlock abstract one day, to force people to do this. 
		 */
		outputDataBlock = new PamDataBlock<EchoDataUnit>(EchoDataUnit.class, 
				echoController.getUnitName(), this, usedChannels);

		PamDetectionOverlayGraphics detectionOverlayGraphics = new PamDetectionOverlayGraphics(outputDataBlock);
		outputDataBlock.setOverlayDraw(detectionOverlayGraphics);
		//outputDataBlock.SetLogging(new WorkshopSQLLogging(workshopController, outputDataBlock));
		addOutputDataBlock(outputDataBlock);
		
	}
	
	
	@Override
	public void pamStart() {
		
		for (int i = 0; i <= PamUtils.getHighestChannel(usedChannels); i++) {
			if (((1<<i) & usedChannels) > 0) {
				channelDetectors[i].pamStart();
			}
		}
		
	}

	@Override
	public void pamStop() {
		// this method is abstract in the superclass and must therefore be overridden
		// even though it doesn't do anything. 
		
	}

	@Override
	/**
	 * PamProcess already implements PamObserver, enabling it to subscribe
	 * to PamDataBlocks and get notifications of new data. So that the 
	 * Pamguard Profiler can monitor CPU usage in each module, the function
	 * update(PamObservable o, PamDataUnit arg) from the PAmObserver interface
	 * is implemented in PamProcess. PamProcess.update handles profiling
	 * tasks and calls newData where the developer can process newly 
	 * arrived data. 
	 */
	public void newData(PamObservable o, PamDataUnit arg) {
		// see which channel it's from
		ClickDetection clickDetection = (ClickDetection) arg;
		int chan = PamUtils.getSingleChannel(clickDetection.getChannelBitmap());
		// check that a detector has been instantiated for that detector
		if (channelDetectors == null || 
				channelDetectors.length <= chan || 
				channelDetectors[chan] == null) {
			return;
		}
		
		channelDetectors[chan].newData(o, clickDetection);
	}

	@Override
	/**
	 * One of the 'annoyances' of the very flexible Pamguard model is that 
	 * it's impossible to say at development time exactly what order 
	 * pamguard models will be generated in. This example is dependent on 
	 * FFT data but it's possible (although unlikely) that the FFT producing 
	 * module will be created after this module. Therefore there isn't much point in 
	 * looking for the FFT data in this modules constructor. When new modules 
	 * are added, a notification is sent to all PamControlledUnits. I've arranged 
	 * for WorkshopController to call prepareProcess() when this happens so 
	 * that we can look around and try to find an FFT data block. 
	 */
	public void prepareProcess() {
		// TODO Auto-generated method stub
		super.prepareProcess();
		/*
		 * Need to hunt aroudn now in the Pamguard model and try to find the FFT
		 * data block we want and then subscrine to it. 
		 * First get a list of all PamDataBlocks holding FFT data from the PamController
		 */
		ArrayList<PamDataBlock> clickDetectors = PamController.getInstance().getDataBlocks(ClickDetection.class, false);
		if (clickDetectors == null || 
				clickDetectors.size() <= echoController.echoProcessParameters.clickDetector) {
			setParentDataBlock(null);
			return;
		}
		/*
		 * If the detection data blocks exist, subscribe to the one in the parameters. 
		 */
		//clickDataBlock = (ClickDataBlock) clickDetectors.get(echoController.echoProcessParameters.clickDetector);
		clickDataBlock = clickDetectors.get(echoController.echoProcessParameters.clickDetector);
		setParentDataBlock(clickDataBlock);
		
		dataSampleRate = (int) clickDataBlock.getSampleRate();
		
		/*
		 * usedChannels will be a combination of what we want and what's availble.
		 */
		usedChannels = clickDataBlock.getChannelMap() & echoController.echoProcessParameters.channelList;
		
		/*
		 * Tell the output data block which channels data may come from.
		 */
		outputDataBlock.setChannelMap(usedChannels);
		
		/**
		 * allocate references to a list of detectors - one for each channel used. 
		 */
		channelDetectors = new ChannelDetector[PamUtils.getHighestChannel(usedChannels)+1];
		for (int i = 0; i <= PamUtils.getHighestChannel(usedChannels); i++) {
			if (((1<<i) & usedChannels) > 0) {
				channelDetectors[i] = new ChannelDetector(i);
			}
		}

		/*
		 * The following could be done every time new data arrive, but 
		 * it's quicker to do them once here - not that it makes a lot of
		 * difference since these preparations are v. quick compared to 
		 * the actual fft calculations. 
		 */
		/*
		 * work out the minimum and maximum times for echo detection check they
		 * are valid. 
		 */
		
		minEchoSample = (int) (echoController.echoProcessParameters.minEchoTime 
				/ 1e3 * clickDataBlock.getSampleRate());
		maxEchoSample = (int) (echoController.echoProcessParameters.maxEchoTime 
				/ 1e3 * clickDataBlock.getSampleRate());
				
		// Figure out how many samples are required to get the right IPI duration
		long targetSamples = (echoController.echoProcessParameters.echoDuration);
		
		// For faster FFT processing make this a power of 2
		echoDuration = PamUtils.getMinFftLength(targetSamples);
		
		/*
		minEchoSampleIndex = Math.min(bin1, fftDataBlock.getFftLength()/2-1);
		minEchoSampleIndex = Math.max(bin1, 0);
		*/
		minEchoSample = Math.min(minEchoSample, echoDuration);
		maxEchoSample = Math.min(maxEchoSample, echoDuration);
		
		
		/*
		 * convert the threshold which was set in dB to a simple energy ratio
		 */
		threshold = Math.pow(10., echoController.echoProcessParameters.threshold/10.);
		
		/*
		 * This should always go back to an Acquisition process by working
		 * back along the chain of data blocks and pamprocesses. 
		 */
		try {
			daqProcess = (AcquisitionProcess) getSourceProcess();
		}
		catch (ClassCastException ex) {
			daqProcess = null;
		}
		
	}
	
	/**
	 * Since the detector may be running on several channels, make a sub class
	 * for the actual detector  code so that multiple instances may be created. 
	 * @author Doug
	 *
	 */
	class ChannelDetector {
		
		/*
		 * Which channel is this detector operating on
		 */
		int channel;
		
		/*
		 * how many times have new data arrived. 
		 */
		int callCount = 0;
		
		/*
		 * some information about each echo detection.
		 */
		long echoFunctionStartSample;
		long echoDelayTime;
		long echoDelayValue;
		
		public ChannelDetector(int channel) {
			this.channel = channel;
		}
		
		public void pamStart() {
			callCount = 0;
		}
		
		/**
		 * Performs the same funciton as newData in the outer class, but this
		 * time it should only ever get called with data for a single channel.
		 * Take the click detection and if appropriate, output an echoDataUnit
		 * @param o
		 * @param arg - a single channel of a click detection
		 */
		public void newData(PamObservable o, ClickDetection arg) {

			int availableSamples = (int) arg.getDuration();
			// Make sure we have enough samples to do the echo detection
			if (availableSamples < echoDuration){
				System.out.println("Oops, " 
						+ echoDuration 	+ " samples are required, but only "
						+ availableSamples  + " are available in this click detection.");
				return;
			}
			
			// Compute the cepstrum from the detection data
			double[] echoData = cepstrum(arg, echoDuration);
			
			long echoTimeMilliseconds = arg.getTimeMilliseconds(); 
			EchoDataUnit edu = new EchoDataUnit(echoTimeMilliseconds, dataSampleRate,
					echoData, minEchoSample, maxEchoSample);
						
			/*
			 * put the unit back into the datablock, at which point all subscribers will
			 * be notified. 
			 */
			outputDataBlock.addPamData(edu);			
		}
		
		/**
		 * This function computes the cepstrum of a signal.
		 * Cepstrum is defined as IFFT(log(abs(FFT(signal))));
		 * Here we simply take the inverse fourier transform of 
		 * the square root of the power spectrum for each detection.
		 */
		private double[] cepstrum(ClickDetection clickDetection, int nSamples) {
			int duration; 
			int i;
			fftManager.FastFFT fastFFT = new fftManager.FastFFT();
			fftManager.FFT fft = new fftManager.FFT();  
			double[] data;
			double[] output;
			Complex[] cepstrum;

			data = clickDetection.getWaveData(channel);

			int minLength = Math.min(nSamples*4, data.length);

			duration = PamUtils.getMinFftLength(minLength);
			
			cepstrum = Complex.allocateComplexArray(duration);
			output = new double [duration/4];
			double[] tempOutput1 = new double [duration/2];
			double[] tempOutput2 = new double [duration/4];

			// put the actual audio data into the array
			for (i = 0; i < minLength; i++) {
				cepstrum[i].real = data[i];
				cepstrum[i].imag = 0;
			}

			/* Pad/truncate number of data samples to a power of 2
			 * Maybe unnecessary because java arrays should be 
			 * initialized to zero?
			 */
			for (i = minLength; i < duration; i++){
				cepstrum[i].real = 0;
				cepstrum[i].imag = 0;
			}
			
			cepstrum = fastFFT.rfft(data, null, FastFFT.log2(duration));
			//fft.recursiveFFT(cepstrum);
			
			for (i = 0; i < duration / 2; i++) {
				cepstrum[i].real = cepstrum[i].mag();
				cepstrum[i].imag = 0;
			}
			
			fastFFT.ifft(cepstrum, FFT.log2(duration/2));
			//fft.recursiveIFFT(cepstrum);
						
			for (i = 0; i < cepstrum.length / 4; i++) 
				output[i] = cepstrum[i].mag();
			
			return output;

		}
		
	}

	/*
	// Used only during development, but useful for debugging
	public void writeEchoData(double[] echoData, String filename){
		PrintWriter out;
		try {
			out = new PrintWriter(filename);
			for (int i = 0; i < echoData.length; i++){
				out.println(echoData[i]);
			}
			out.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	*/
	
}
