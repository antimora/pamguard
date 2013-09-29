package simulatedAcquisition;

import java.io.Serializable;

import PamUtils.LatLong;

/**
 * Information on a single simulated object
 * @author Doug Gillespie
 *
 */
public class SimObject implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 1L;

	public String name;
	
	public LatLong startPosition = new LatLong();
	
	@Deprecated
	private double depth = 0;
	
	private double height = 0;
	
	public double getHeight() {
		if (height == 0 && depth != 0) {
			height = -depth;
		}
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double course;
	
	public double speed;
	
	public String signalName; 
	
	public double amplitude = 170;
	
	public double meanInterval = 1;
	
	public boolean randomIntervals = false;

	protected transient SimObjectDataUnit simObjectDataUnit;

	@Override
	protected SimObject clone() {
		try {
			SimObject newObject = (SimObject) super.clone();
			/** 
			 * Deal with the switch in coordinates from depth to height. 
			 */
			if (newObject.height == 0 && newObject.depth != 0) {
				newObject.height = -newObject.depth;
			}
			return newObject;
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	

}
