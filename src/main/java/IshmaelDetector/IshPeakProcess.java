package IshmaelDetector;


import java.awt.Color;

import PamDetection.IshDetection;
import PamUtils.PamUtils;
import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;


public class IshPeakProcess extends PamProcess
{
	/**
	 * @author Dave Mellinger and Hisham Qayum 
	 */
	//public Complex[] inputData;
	//IshDetCalcIntfc ishDetCalcIntfc;
	//IshDetIoIntfc ishDetIoIntfc;
//	FFTDataSource parentProcess;
	IshDetControl ishDetControl;
	PamDataBlock<IshDetection> outputDataBlock;
	int savedFftLength = -1;		//-1 forces recalculation
	double[] outData;
	long minTimeN, refractoryTimeN;	//time values converted to slice numbers
	private class PerChannelInfo {
		int nOverThresh = 0;		//number of times we've been over threshold
		double peakHeight;			//height of peak within the current event
		long peakTimeSam;			//sample number at which that peak occurred
	}
	PerChannelInfo perChannelInfo[] = new PerChannelInfo[PamConstants.MAX_CHANNELS];
	
	public IshPeakProcess(IshDetControl ishDetControl, 
			PamDataBlock parentDataBlock) 
	{
		super(ishDetControl, null);
		this.ishDetControl = ishDetControl;
		setParentDataBlock(parentDataBlock);
		outputDataBlock = new PamDataBlock<IshDetection>(IshDetection.class, 
				ishDetControl.getUnitName() + " events", this, parentDataBlock.getChannelMap());
		outputDataBlock.setCanClipGenerate(true);
		addOutputDataBlock(outputDataBlock);
		PamDetectionOverlayGraphics overlayGraphics = new PamDetectionOverlayGraphics(outputDataBlock);
		if (overlayGraphics.getPamSymbol() == null) {
			overlayGraphics.setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 10, 10, false, Color.GREEN, Color.GREEN));
		}
		outputDataBlock.setOverlayDraw(overlayGraphics);
		IshLogger ishLogger = new IshLogger(ishDetControl, outputDataBlock);		
		outputDataBlock.SetLogging(ishLogger);
		setupConnections();
	}
	
	@Override
	public void setParentDataBlock(PamDataBlock newParentDataBlock) {
		super.setParentDataBlock(newParentDataBlock);
	}
	
	public int getChannelMap() {
		return ishDetControl.ishDetParams.channelList;
	}
	
	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		super.setSampleRate(sampleRate, notify);
		prepareMyParams();
	}
	
	public void setupConnections() {
		// Find the existing source data block and remove myself from observing it.
		// Then find the new one and subscribe to that instead. 
		//if (getParentDataBlock() != null) 
			//getParentDataBlock().deleteObserver(this);
		
		if (ishDetControl == null) 
			return;
		IshDetParams p = ishDetControl.ishDetParams;  //local reference
		//PamDataBlock detfnDataBlock = 
			//PamController.getInstance().getDetectorDataBlock(p.detfnDataSource);
		//setParentDataBlock(detfnDataBlock);

		prepareMyParams();
		outputDataBlock.setChannelMap(p.channelList);
		//setProcessName("Peak-picker: threshold " + p.thresh);
		//setSampleRate(sampleRate, true);	//set rate for outputDataBlock
	}

	/** Calculate any subsidiary values needed for processing.  These get recalculated
	 * whenever the sample rate changes (via setSampleRate, which is also called after 
	 * the params dialog box closes).
	 */
	protected void prepareMyParams() {
		IshDetParams p = ishDetControl.ishDetParams;  //local reference
		float dRate     = ishDetControl.ishDetFnProcess.getDetSampleRate();
		minTimeN        = Math.max(0, (long)(dRate * p.minTime));
		refractoryTimeN = Math.max(0, (long)(dRate * p.refractoryTime));
	}

	public void prepareForRun() {
		//Refresh all the PerChannelInfo's.  This resets chan.nOverThresh to 0.
		for (int i = 0; i < perChannelInfo.length; i++)
			perChannelInfo[i] = new PerChannelInfo();
	}
	
	/* PeakProcess uses recycled data blocks; the length of the data unit should
	 * correspond to the output of the detector function: Just one double.
	 */
	@Override
	public void newData(PamObservable o, PamDataUnit arg1) {  //called from PamProcess
		IshDetFnDataUnit arg = (IshDetFnDataUnit)arg1;
		double[] inputData = arg.getDetData();

		//See if the channel is one we want before doing anything.
		if ((arg.getChannelBitmap() & ishDetControl.ishDetParams.channelList) == 0)
			return;
		int chanIx = PamUtils.getSingleChannel(arg.getChannelBitmap());
		PerChannelInfo chan = perChannelInfo[chanIx];
		
		//The actual peak-picking calculation.
		if (inputData[0] > ishDetControl.ishDetParams.thresh) {
			if (chan.nOverThresh == 0) { chan.peakHeight = Double.NEGATIVE_INFINITY; }
			chan.nOverThresh++;
			if (inputData[0] >= chan.peakHeight) {
				chan.peakHeight = inputData[0];
				chan.peakTimeSam = arg.getStartSample(); //
			}
		} else {
			//Below thresh. Check to see if we were over enough times, and if so,
			//add an output unit.
			if (chan.nOverThresh > 0 && chan.nOverThresh >= minTimeN) {
				//Append the new data to the end of the data stream.
				long startSam = arg.getStartSample() - chan.nOverThresh;
				long durationSam = chan.nOverThresh;
				durationSam *= sampleRate / (ishDetControl.ishDetFnProcess.getDetSampleRate());
				long startMsec = absSamplesToMilliseconds(startSam - durationSam);
				long endMsec = absSamplesToMilliseconds(startSam) + 
				Math.round(1000.0 * (float)durationSam * ishDetControl.ishDetFnProcess.getDetSampleRate()); 
				float lowFreq = ishDetControl.ishDetFnProcess.getLoFreq();
				float highFreq = ishDetControl.ishDetFnProcess.getHiFreq();

				IshDetection iDet = outputDataBlock.getRecycledUnit();
				if (iDet != null) {                //refurbished
					iDet.setInfo(startMsec, 1 << chanIx, startSam, durationSam, 
							lowFreq, highFreq, chan.peakTimeSam, chan.peakHeight);
				} else {                           //new
					iDet = new IshDetection(startMsec, endMsec, lowFreq, highFreq, chan.peakTimeSam, 
							chan.peakHeight, outputDataBlock, 1 << chanIx, startSam, durationSam);
					iDet.setParentDataBlock(outputDataBlock);
				}
				
				outputDataBlock.addPamData(iDet);
			}
			chan.nOverThresh = 0;
		}
	}

	@Override public void pamStart() {
		//This doesn't get called because we don't do addPamProcess(ishPeakProcess).
		//(Why not?  Then it shows up in the data model.)
	}
	
	//This keeps the compiler happy -- it's abstract in the superclass.
	@Override public void pamStop() { }
}
