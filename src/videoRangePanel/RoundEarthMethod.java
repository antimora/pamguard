package videoRangePanel;

public class RoundEarthMethod extends VRRangeMethod {

	protected VRControl vrControl;
	
	
	public RoundEarthMethod(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
	}

	@Override
	public void configure() {
		// TODO Auto-generated method stub

	}

	@Override
	public RangeDialogPanel dialogPanel() {
		return null;
	}

	@Override
	public double getRange(double height, double angle) {

		double ha = getHorizonAngle(height);
		double psi = Math.PI / 2 - ha - angle;
		
		return rangeFromPsi(height, psi);
	}
	
	/**
	 * Many calcls, including those in the refractionmethoduse the anlge
	 * from the vertical, so put that as a separate calculation.
	 * <p>
	 * Eq. 1. From Leaper and Gordon.
	 * @param height camera height
	 * @param psi angle up from vertical
	 * @return distance to object. 
	 */
	protected double rangeFromPsi(double height, double psi) {

		double rnh = earthRadius + height;
		double d = (rnh) * Math.cos(psi);
		double sqrtTerm = Math.pow(earthRadius,2) - Math.pow(rnh * Math.sin(psi), 2);
		d -= Math.sqrt(sqrtTerm);
		
		return d;
	}
	

	@Override
	double getAngle(double height, double range) {
		if (range > getHorizonDistance(height)) {
			return -1;
		}
		return Math.PI/2 - getHorizonAngle(height) - psiFromRange(height, range);
	}
	
	/**
	 * Convert a range to an angle up from the vertical
	 * (cosine rule).
	 * @param height platofrm height
	 * @param range range to object
	 * @return angle in radians. 
	 */
	protected double psiFromRange(double height, double range) {
		if (range > getHorizonDistance(height)) {
			return -1;
		}
		double a = earthRadius;
		double b = earthRadius + height;
		double c = range;
		double cosPsi = (b*b + c*c - a*a)/(2*b*c);
		if (Math.abs(cosPsi) > 1) {
			return -1;
		}
		return Math.acos(cosPsi);
	}

	@Override
	String getName() {
		return "Round Earth";
	}

	@Override
	public double getHorizonDistance(double height) {
		// simple pythagorus. 
		return Math.sqrt(Math.pow(height+earthRadius, 2) - Math.pow(earthRadius, 2));
	}
	
	

}
