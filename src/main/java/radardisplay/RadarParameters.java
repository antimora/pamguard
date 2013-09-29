package radardisplay;

import java.io.Serializable;

import userDisplay.UserFrameParameters;

public class RadarParameters extends UserFrameParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 1;
	
	static public final int SIDES_ALL = 0;
	static public final int SIDES_RIGHTHALF = 1;
	static public final int SIDES_LEFTHALF = 2;
	static public final int SIDES_FRONTHALF = 3;
	static public final int SIDES_BACKHALF = 4;
	
	static public final int RADIAL_AMPLITIDE = 0;
	static public final int RADIAL_DISTANCE = 1;
	static public final int RADIAL_SLANT_ANGLE = 2;
	
	static public final int HEAD_UP = 0;
	static public final int NORTH_UP = 1;
	
	String windowName;
	
	int sides = SIDES_ALL;
	
	int radialAxis = RADIAL_AMPLITIDE;
	
	int rangeStartm = 0;
	
	int rangeEndm = 1000;
	
	int orientation = HEAD_UP;
	
	/**
	 * Ranges in dB are opposite way round to normal so that 
	 * small amplitudes are on the outside and large amplitudes are in the middle. 
	 */
	int rangeStartdB = 160;
	
	int rangeEnddB = 100;
	
	int angleGrid = 30;
	
	boolean[] showDetector;
	
	int[] detectorLifetime;
	
	// some stuff for the viewer ...
	long scrollMinMillis, scrollMaxMillis; // current 
	double viewRangeSeconds; // range off spinner. 
	public long scrollValue;
	
	public String getScaleName() {
		switch(radialAxis) {
		case RADIAL_AMPLITIDE:
			return "Amplitude";
		case RADIAL_DISTANCE:
			return "Distance";
		case RADIAL_SLANT_ANGLE:
			return "Slant Angle";
		}
		return "Unknown";
	}

	@Override
	public RadarParameters clone()  {
		try {
			RadarParameters newParams = (RadarParameters) super.clone();
			if (newParams.viewRangeSeconds == 0) {
				viewRangeSeconds = 600000L;
			}
			return newParams;
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}
	
}
