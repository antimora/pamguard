package PamDetection;

/**
 * Groups togetehr a series of IshDeections. Most functinality for doing
 * the grouping, adding removing elements, etc should be in PamDetection.
 * 
 * @author Doug
 *
 */
public class IshDetWrapper extends PamDetection <IshDetection, IshAnchorGroup> {

	public IshDetWrapper(long timeMilliseconds, int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
		// TODO Auto-generated constructor stub
	}

}
