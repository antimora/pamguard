package PamDetection;

import PamguardMVC.PamDataBlock;

/** Basic detection on a single channel for the Ishmael det/loc. 
 * @author Doug Gillespie and Dave Mellinger
 *
 */
public class IshDetection extends PamDetection<PamDetection, IshAnchorGroup> {

	static public final long serialVersionUID = 0;
	String name = "";				//not sure how used
	private double peakHeight;		//...and its height
	protected String callType = "";

	public IshDetection(long startMsec, long endMsec,
			float lowFreq, float highFreq, long peakTimeSam, double peakHeight,
			PamDataBlock parentDataBlock, int channelBitmap, long startSam, long durationSam)
	{
		super(startMsec, channelBitmap, startSam, durationSam);
		setInfo(startMsec, channelBitmap, startSam, durationSam, lowFreq, highFreq, peakTimeSam, peakHeight);
	}
	public String getCallType() { return callType; }
	public void setCallType(String callType) { this.callType = callType; }
	public double getPeakHeight() { return peakHeight; }
	public void setPeakHeight(double peakHeight) { this.peakHeight = peakHeight; }

	/** Set various parameters.
	 * @param startMsec
	 * @param channelBitmap
	 * @param startSam          relative to start of PAMGUARD run
	 * @param durationSam			
	 * @param lowFreq			lower edge of call T/F box, Hz
	 * @param highFreq			upper edge of call T/F box, Hz
	 * @param peakTimeSam		relative to start of PAMGUARD run
	 * @param peakHeight		measure of detection quality; different detectors
	 * 							will scale it differently, so it's only comparable
	 * 							within a detector type
	 */  
	public void setInfo(long startMsec, int channelBitmap, long startSam, 
			long durationSam, float lowFreq, float highFreq, long peakTimeSam, 
			double peakHeight)
	{
		setInfo(startMsec, channelBitmap, startSam, durationSam);
		frequency[0] = lowFreq;
		frequency[1] = highFreq;
		setPeakHeight(peakHeight);
//		setPeakTimeSam(peakTimeSam);     //relative to start of PAMGUARD run
	}
}
