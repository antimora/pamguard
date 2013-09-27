package videoRangePanel;

abstract class VRRangeMethod {

	protected static final double earthRadius = 6356766;
	protected static final double gravity = 9.80665;
	
	/**
	 * Converts a height and an angle below the horizon to a distance in metres. 
	 * @param height platform height (metres)
	 * @param angle angle below the horizon (radians)
	 * @return distance in metres. 
	 */
	abstract double getRange(double height, double angle);
	
	/**
	 * Converts a range into an angle below the horizon. 
	 * <p>
	 * Or returns -1 if the range is beyond the horizon.
	 * @param height platofrm height (metres)
	 * @param range range to object. 
	 * @return angle in radians. 
	 */
	abstract double getAngle(double height, double range);
	
	abstract void configure();
		
	abstract RangeDialogPanel dialogPanel();
	
	abstract String getName();
	
	/**
	 * Calculate the horizon dip angle from the horizontal
	 * @param height Platform height
	 * @return dip angle in radians. 
	 */
	protected double getHorizonAngle(double height) {
		return Math.acos(earthRadius / (earthRadius + height));
	}
	
	/**
	 * Calculate the distance to the horizon from a given height. 
	 * @param height
	 * @return distnace to horizon in metres. 
	 */
	abstract public double getHorizonDistance(double height);
}
