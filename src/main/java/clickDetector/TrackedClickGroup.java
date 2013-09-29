package clickDetector;


import Localiser.bearingLocaliser.GroupDetection;

/**
 * Class for handling data from manual tracking of clicks. 
 * Probably doens't need any functionality that is not already 
 * in ClickGroupDetection, but sub class anyway to make 
 * future changes easy. 
 * 
 * @author Doug Gillespie
 *
 */
public class TrackedClickGroup extends GroupDetection {

	public TrackedClickGroup(ClickDetection click) {
		super(click);
		if (click != null) {
			setEventId(click.getEventId());
		}
	}

	public TrackedClickGroup(long timeMilliseconds, int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
	}


}
