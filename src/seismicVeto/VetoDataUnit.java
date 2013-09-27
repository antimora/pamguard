package seismicVeto;

import PamDetection.PamDetection;

public class VetoDataUnit extends PamDetection <PamDetection,PamDetection> {

	public VetoDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);

	}
}
