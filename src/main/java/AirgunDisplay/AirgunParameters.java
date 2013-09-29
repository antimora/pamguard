package AirgunDisplay;

import java.awt.Color;
import java.io.Serializable;

public class AirgunParameters implements Serializable, Cloneable {


	static public final long serialVersionUID = 1;
	
	/**
	 * True if guns are on this vessel
	 */
	public boolean gunsThisVessel = true;
	
	/**
	 * mmsi number of vessel if guns are on another vessel
	 */
	public int gunsMMSIVessel = 0;

	/**
	 * distance in m towards the stern from the vessels GPS receiver
	 */
	double dimE = 20;
	
	/**
	 * distance in m towards the starboard side from the vessels GPS receiver
	 */
	double dimF = 0;
	
	/**
	 * Show exclusion zone on the map
	 */
	boolean showExclusionZone = true;
	
	/**
	 * radius of exclusion xone in m
	 */
	int exclusionRadius = 500;
	
	/**
	 * Colour for exclusion zone on map.
	 */
	Color exclusionColor = Color.RED;
	
	/**
	 * predict where we'll be in a certain time
	 */
	boolean predictAhead = false;
	
	/**
	 *  prediction time in seconds
	 */
	int secondsAhead = 600;
	

	@Override
	public AirgunParameters clone() {
		try {
			return (AirgunParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}
}
