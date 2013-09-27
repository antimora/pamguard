/**
 * 
 */
package IshmaelLocator;


import Acquisition.AcquisitionProcess;
import Acquisition.AcquisitionControl;
import Array.ArrayManager;
import Array.PamArray;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import PamController.PamController;
import PamDetection.PamDetection;
import PamDetection.IshDetection;
import PamguardMVC.PamProcess;
import fftManager.Complex;

import Spectrogram.*;

/** Various location algorithms can fail to find a solution.  If so,
 * they just throw this Exception.
 * @author Dave Mellinger
 */
class noLocationFoundException extends Exception {
	static final long serialVersionUID = 0;
}


/** This is a superclass to a "real" localization class. It gathers the
 * necessary data (samples, phone positions, etc.) for the subclass, then calls
 * calcData(), which the subclass should define. Current children include
 * IshLocPairProcess and IshLocHyperbProcess.
 * 
 * @author Dave Mellinger
 */
abstract public class IshLocProcess extends PamProcess implements SpectrogramMarkObserver
{
	double[][] arraygeom;			//index different from hydlist; 2 or 3 cols
	double c;						//speed of sound
	public Complex[] inputData;
	int[] hydlist;					//indices of phones in incoming data 
	Complex[] v1, v2;
	IshLocControl ishLocControl;	//back-pointer for my control
	PamDataBlock<PamDetection> outputDataBlock;
	//PamDataBlock outputDataBlock;
	
	//We don't really *need* a parent process here, as spectrogramNotification()
	//provides everything we need.  Keep the parent case we need it in the future. 
	//FFTDataSource parentFFTProcess;	
	
	public IshLocProcess(IshLocControl ishLocControl) {
		super(ishLocControl, null);
		this.ishLocControl = ishLocControl;
		outputDataBlock = new PamDataBlock<PamDetection>(PamDetection.class,
				this.getName(), this, 0);
		outputDataBlock.setOverlayDraw(new IshOverlayGraphics(outputDataBlock));
		addOutputDataBlock(outputDataBlock);
		
		//Set it up so that whenever a spectrogram mark is made, 
		//this.spectrogramNotification() gets called.
		SpectrogramMarkObservers.addSpectrogramMarkObserver(this);
	}
	
	public abstract String getName();
	
	@Override
	public void setParentDataBlock(PamDataBlock newParentDataBlock) {
		super.setParentDataBlock(newParentDataBlock);
		//PamProcess proc = getParentProcess();
		//parentFFTProcess = (PamFFTProcess)proc;
	}

	/** An IshLocProcess has one input stream (data block).  Return it, or null 
	 * if it's not available.
	 */
	public PamDataBlock getInputDataBlock() {
		IshLocParams p = ishLocControl.ishLocParams;
		
		if (!p.useDetector)
			return null;
		if (p == null || p.inputDataSource == null) 
			return getParentDataBlock();
		else
			return PamController.getInstance().getDataBlock(PamDetection.class, p.inputDataSource);
	}
	
	public void setupConnections() {
		// Find the existing source data block and remove myself from observing it.
		// Then find the new one and subscribe to that instead. 
		if (getParentDataBlock() != null) 
			getParentDataBlock().deleteObserver(this);
		if (ishLocControl == null) 
			return;

		IshLocParams p = ishLocControl.ishLocParams;			//shorthand
		PamDataBlock inputDataBlock = getInputDataBlock();		//might be null
		setParentDataBlock(inputDataBlock);		//in case it wasn't parent already
		if (inputDataBlock != null)
			inputDataBlock.addObserver(this);	//should happen in setParentDataBlock, but doesn't always

		prepareMyParams();
		outputDataBlock.setChannelMap(p.channelList);
		//setSampleRate(sampleRate, true);	//set rate for outputDataBlock
	}

	//Calculate any subsidiary values needed for processing.  These get recalculated
	//whenever the sample rate changes (via setSampleRate, which is also called after 
	//the params dialog box closes).
	//Note that during initialization, this gets called with params.fftDataSource
	//still null.
	protected void prepareMyParams() {
	}
	
	/** Data for localization can arrive from either an upstream data source or
	 * from the user drawing a box on the spectrogram.  This is the routine for
	 * capturing user box-drawing.
	 * 
	 * @param display		spectrogram display; ignored
	 * @param downUp		mouse action (only MOUSE_UP events are used)
	 * @param channel		which channel was drawn on; ignored
	 * @param startMsec		in absolute msec (since 1970)
	 * @param durationMsec
	 * @param f0,f1			frequency range of the selection
	 */
	public void spectrogramNotification(SpectrogramDisplay display, int downUp, 
			int channel, long startMsec, long durationMsec, double f0, double f1)
	{		
		if (downUp != SpectrogramMarkObserver.MOUSE_UP)		//react only on mouse-up
			return;
		
		long startSam, durationSam;
		PamRawDataBlock daqBlock = display.getSourceRawDataBlock();
		PamProcess daqProc1 = daqBlock.getSourceProcess();
		//If it crashes on this line, daqProc1 is not an AcquisitionProcess.
		AcquisitionProcess daqProc = (AcquisitionProcess)daqProc1;
		startSam    = daqProc.absMillisecondsToSamples(startMsec);
		durationSam = daqProc.relMillisecondsToSamples(durationMsec);
		
		doLocalisation(startSam, durationSam, f0, f1, daqBlock);
	}

	/** Data for localization can arrive from either an upstream data source or
	 * from the user drawing a box on the spectrogram.  This is the routine for
	 * data arriving from upstream.
	 * 
	 * @param arg1	data arriving from upstream; type must be PamDetection (or
	 * 				a subclass of it)
	 */
	@Override
	public void newData(PamObservable o, PamDataUnit arg1) {  //called from PamProcess
		PamDetection det = (PamDetection)arg1;
		IshLocParams p = ishLocControl.ishLocParams;			//shorthand
		
		PamProcess daqProc1 = getInputDataBlock().getSourceProcess();
		//If it crashes on this line, daqProc1 is not an AcquisitionProcess.
		AcquisitionProcess daqProc =(AcquisitionProcess)daqProc1; 

		PamRawDataBlock daqBlock = (PamRawDataBlock)daqProc.getOutputDataBlock(0);
		//PamRawDataBlock daqBlock = (PamRawDataBlock)getAncestorDataBlock(PamRawDataBlock.class);
		//PamProcess daqProc1 = daqBlock.getSourceProcess();
		//If it crashes on this line, daqProc1 is not an AcquisitionProcess.
		//AcquisitionProcess daqProc = (AcquisitionProcess)daqProc1;
		float sRate = daqProc.getSampleRate();
		//float sRate = ishLocControl.getProcess().getSampleRate();

		long t0Sam = det.getStartSample() - (long)(p.tBefore * sRate);
		long durSam = det.getDuration() + (long)((p.tBefore + p.tAfter) * sRate);
		double[] freq = det.getFrequency();
		doLocalisation(t0Sam, durSam, freq[0], freq[1], daqBlock);
	}

	/** Do the localization for the call delineated by startMsec, durationMsec, f0,
	 * and f1.
	 * 
	 * @param startSam		in absolute msec (since 1970)
	 * @param durationSam
	 * @param f0,f1			frequency range to use in calculating the loc
	 * @param daqProcess	the PamProcess producing raw audio data, needed for getting
	 * 						hydrophone info
	 */
	public void doLocalisation(long startSam, long durationSam, double f0, double f1,
			PamRawDataBlock daqBlock)    //AcquisitionProcess daqProcess)
	{	
		//Need the geometry -- the location of the hydrophone elements 
		//relative to the ship.  First find the available hydrophones (i.e., the
		//ones whose signals are captured by software) get the hydrophone list
		//for this digitizing PamProcess --should give actual phone element numbers
		//versus channels.  We will send the hydrophone list to subclass
		//processes: user chooses phones; Pam finds channels.
		//AcquisitionProcess daqProcess = (AcquisitionProcess)outputDataBlock.getSourceProcess();
		AcquisitionProcess daqProcess = (AcquisitionProcess)daqBlock.getParentProcess();
		AcquisitionControl ctrl = daqProcess.getAcquisitionControl();
		
		//I don't think hydlist is used anymore.  See channelMap below.
		hydlist = ctrl.getHydrophoneList();
		int nPhones = ctrl.acquisitionParameters.getNChannels();
		
		if (hydlist == null || nPhones < 2) {
			//For some reason hydlist is bad. Assume that channels are phones 0..n-1.
			hydlist = new int[ctrl.acquisitionParameters.getNChannels()];
			for (int i = 0; i < nPhones; i++)
				hydlist[i] = i;
		}

		//Get coordinates of hydrophones on this list for the subclass.
		int channelMap = daqBlock.getChannelMap();
		
		//Get the array geometry that was in effect at the time of this selection.
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
//		for (int i = 0; i < nPhones; i++)
//			arraygeom[i] = array.getHydrophone(hydlist[i]).getCoordinate();
		arraygeom = array.getLocalGeometry(daqProcess.absSamplesToMilliseconds(startSam));

		c = array.getSpeedOfSound();
		
		/**
		 * Doug.
		 * If the locator is downstream of a decimator, this doesn't work since the 
		 * sample rate will be the acquisition sample rate, not the sample rate in the 
		 * incoming data. Or does a decimator output RawDataUnits, and getAncestorDataBlock 
		 * will find it correctly?  I think it should all work OK if sample rate, etc. are 
		 * all just taken from the process just before the FFT. 
		 */
		double[][] selectionSams = null;
		try {
			selectionSams =	daqBlock.getSamples(startSam, (int)durationSam, channelMap);
		}
		catch (RawDataUnavailableException e) {
			System.out.println(e.getMessage());
		}
		if (selectionSams == null) {
			System.out.println("ishLocProcess: Unable to find source audio data.");
			return;
		}
		//PamProcess rawProcess = getRawDataProcess();
		long startMsec = daqProcess.absSamplesToMilliseconds(startSam); 
		long endMsec   = daqProcess.absSamplesToMilliseconds(startSam + durationSam);
		long midSam = startSam + durationSam / 2;

		//RecycledDataUnit outputUnit = getOutputDataBlock(0).getNewUnit(startSample,
		//		durationInSams, channelMap);
		PamDetection outputUnit = outputDataBlock.getRecycledUnit();
		if (outputUnit != null)                 //refurbished outputUnit
			outputUnit.setInfo(startSam, channelMap, startSam, durationSam);
		else {                                  //new outputUnit
			outputUnit = new IshDetection(startMsec, endMsec, (float)f0, (float)f1, 
					midSam, 1.0, outputDataBlock, channelMap, startSam, durationSam);
		}

		////////////////////////////////// Do it! ////////////////////////////////////////
		//Here's where we call the subclass to run the loc algorithm.
		//The order of entries in arraygeom matches the order in selectionSams.
		//The result is installed in outputUnit.
		try {
			IshLocalisation iLoc = calcData(outputUnit, outputDataBlock.getChannelMap(),
					selectionSams, daqProcess.getSampleRate(), f0, f1);
			 outputUnit.setLocalisation(iLoc);
		} catch (noLocationFoundException ex) {
			return;				//error; just ignore the loc
		}
		////////////////////////////////// Done! ////////////////////////////////////////

		//Produce an output PamDataUnit with the result.
		outputDataBlock.addPamData(outputUnit);
	}
	
	public String getMarkObserverName() {
		return getProcessName();
	}

	/** calcData, which is declared here but defined only in subclasses,
	 * uses the selectionSams to calculate a location, which is returned.
	 * Note that the class variables arraygeom[][] and c are available
	 * for use by the subclass.
	 */
	abstract IshLocalisation calcData(PamDetection outputUnit, int referencePhones,
			double[][] selectionSams, double rawSampleRate, double f0, double f1)
		throws noLocationFoundException;

	@Override
	public void pamStart() {}

	@Override
	public void pamStop() {}
}
