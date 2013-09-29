package clickDetector;

import PamDetection.PamDetection;

public class TriggerLevelDataUnit extends PamDetection {

	private TriggerHistogram[] triggerHistograms;
	
	public TriggerLevelDataUnit(long timeMilliseconds, 
			int channelBitmap, long startSample, TriggerHistogram triggerHistograms[]) {
		super(timeMilliseconds, channelBitmap, startSample, 0);
		this.triggerHistograms = triggerHistograms;
	}

	public TriggerHistogram[] getTriggerHistogram() {
		return triggerHistograms;
	}

	public void setTriggerHistogram(TriggerHistogram[] triggerHistograms) {
		this.triggerHistograms = triggerHistograms;
	}

}
