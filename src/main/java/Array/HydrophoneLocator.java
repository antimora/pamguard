package Array;

import PamUtils.LatLong;

/**
 * HydrophoneLocator is an interface for objects that can work out the true positions of 
 * hydrophones based on any or all of the following information
 * <p>
 * The set positions for each hydrophone from the array dialog
 * <p>
 * The movement of the vessel (e.g. the hydrophone may thread behind the vessel
 * <p>
 * Any sensor information available, e.g. depth, Terella, etc. 
 * 
 * @author Doug Gillespie
 *
 */
public interface HydrophoneLocator {

	static public final int ANGLE_RE_NORTH = 1;
	
	static public final int ANGLE_RE_SHIP = 2;
	
	static public final int ANGLE_RE_ARRAY = 3;
	
	/**
	 * Gets a reference LatLong for the locator at a given time.
	 * For towed hydrophones, this will be the ships gps 
	 * position at that moment. FOr static phones it will
	 * be the static array reference position. 
	 * @param timeMilliseconds time reference needed for 
	 * @return Reference LatLonbg
	 */
	LatLong getReferenceLatLong(long timeMilliseconds);
	
	/**
	 * Get the heading of the array at the given time. 
	 * @param timeMillieseconds time in milliseconds. 
	 * @param phoneNo Hydrophone number (the array may be heading in different directions at different places !)
	 * @return Heading in standard navigation units of degrees clockwise
	 * from North. 
	 */
	double getArrayHeading(long timeMilliseconds, int phoneNo);
	
	/**
	 * Get's the LatLong of a specific hydrophone at a given time. 
	 * @param timeMilliseconds time position needed for
	 * @param phoneNo Hydrophone number
	 * @return Hydrophone LatLong
	 */
	LatLong getPhoneLatLong(long timeMilliseconds, int phoneNo);

	/**
	 * Get's the depth of a specific hydrophone at a given time. 
	 * @param timeMilliseconds time depth needed for
	 * @param phoneNo Hydrophone number
	 * @return Hydrophone depth
	 */
	double getPhoneHeight(long timeMilliseconds, int phoneNo);

	/**
	 * Get's the tilt of a specific hydrophone at a given time. 
	 * @param timeMilliseconds time tilt needed for
	 * @param phoneNo Hydrophone number
	 * @return Hydrophone tilt
	 */
	double getPhoneTilt(long timeMilliseconds, int phoneNo);
	
	/**
	 * Get's the angle between a pair of hydrophones
	 * @param timeMilliseconds time angle is needed for
	 * @param phone1 First hydrophone
	 * @param phone2 Second Hydrophone
	 * @param angleType Type of angle - ANGLE_RE_NORTH; ANGLE_RE_SHIP; ANGLE_RE_ARRAY;
	 * @return angle in radians
	 */
	double getPairAngle(long timeMilliseconds, int phone1, int phone2, int angleType);
	
	/**
	 * Gets the distance between a pair of hydrophones in metres. 
	 * @param timeMilliseconds time angle is needed for
	 * @param phone1 First hydrophone
	 * @param phone2 Second Hydrophone
	 * @return distance in metres.
	 */
	double getPairSeparation(long timeMilliseconds, int phone1, int phone2);
	
	void notifyModelChanged(int changeType);
	
}
