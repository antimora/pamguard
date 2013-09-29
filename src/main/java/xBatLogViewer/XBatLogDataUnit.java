package xBatLogViewer;

import PamDetection.PamDetection;

public class XBatLogDataUnit extends PamDetection<PamDetection, PamDetection> {

	private long durationMillis;
	public XBatLogDataUnit(long timeMilliseconds, int channelBitmap,
			long startSample, long durationSamples, long durationMillis) {
		super(timeMilliseconds, channelBitmap, startSample, durationSamples);
		this.setDurationMillis(durationMillis);
	}
	public void setDurationMillis(long durationMillis) {
		this.durationMillis = durationMillis;
	}
	public long getDurationMillis() {
		return durationMillis;
	}

}
