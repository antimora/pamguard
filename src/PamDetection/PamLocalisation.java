package PamDetection;

/**
 * General class for handling localisation information for 
 * a PamDetection. Note that PamDetections may have lists of 
 * subDetections and superDetections (e.g. a click is a sub detection 
 * of a click train, a click train is a super detection of a click; 
 * a Whistle Peak is a sub detection of a Whistle Shape, etc.)
 * <p>
 * It's quite possible that the sub detections and the super detection 
 * will all have, or not have localisation information and the amount of
 * information will vary depending on the detection. 
 * <p> 
 * This class therefore has a lot of has? type functions to see what
 * types of information are available. Of course, these meed not always 
 * be called by many detectors, for instance, the simple IFAW 2 channel 
 * click detector knows that there will always be bearing information 
 * and no other information for every click. Generic plotting routines 
 * may find this stuff really useful though as might detectors / localisers 
 * that may be able to extract varying degrees of information from a 
 * detection. 
 * 
 * @author Doug Gillespie
 *
 */
@Deprecated
public class PamLocalisation extends PamDetection {

	/*
	 * Am hoping that you guys who have more experience of the types
	 * of localisation information available may be able to compile this
	 * list. 
	 */
	boolean hasAngle = false;
	
	double angle[];
	
	boolean hasAngleAmbiguity = true;
	
	boolean hasRange = false;
	
	double range[]; // meters !
	
	boolean hasDepth;
	
	double depth;
	
	double heading[],tilt[];

	/**
	 * 
	 * @param timeMilliseconds Detection time in milliseconds (Java Callender)
	 * @param istation 
	 * @param startSample Detectin start sample (from acquisition start)
	 * @param duration Detection duration in samples
	 * @param hasAngle Localisation has an angle
	 * @param angle Localisation angle
	 * @param hasAngleAmbiguity Localisation has angle ambiguity
	 * @param hasRange Localisation has range
	 * @param range Localisation range
	 * @param hasDepth Localisation has depth
	 * @param depth Localisation depth
	 */
	public PamLocalisation(long timeMilliseconds, int istation, long startSample, long duration, boolean hasAngle, double angle, 
			boolean hasAngleAmbiguity, boolean hasRange, double range, boolean hasDepth, double depth) {
		super(timeMilliseconds, istation, startSample, duration);
		
		this.angle=new double[istation+1];
		this.range=new double[istation+1];
		this.heading=new double[istation+1];
		this.tilt=new double[istation+1];
		for (int i=0;i<=istation; i++){
			this.angle[i]=0;
			this.heading[i]=0;
			this.tilt[i]=0;
			this.range[i]=0;
		}
		this.hasAngle = hasAngle;
		this.angle[istation] = angle;
		this.hasAngleAmbiguity = hasAngleAmbiguity;
		this.hasRange = hasRange;
		this.range[istation] = range;
		this.hasDepth = hasDepth;
		this.depth = depth;
	}

	public double[] getAngle() {
		return angle;
	}

	public void setAngle(double angle, int i) {
		this.angle[i] = angle;
	}

	public double getDepth() {
		return depth;
	}

	public void setDepth(double depth) {
		this.depth = depth;
	}

	public double[] getRange() {
		return range;
	}

	public void setRange(double range, int i) {
		this.range[i] = range;
	}

	public double[] getHeading() {
		return heading;
	}

	public void setHeading(double heading, int i) {
		this.heading[i] = heading;
	}

	public double[] getTilt() {
		return tilt;
	}

	public void setTilt(double tilt, int i) {
		this.tilt[i] = tilt;
	}

	
	
}
