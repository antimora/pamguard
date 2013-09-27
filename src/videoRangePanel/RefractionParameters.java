package videoRangePanel;

import java.io.Serializable;

public class RefractionParameters implements Cloneable, Serializable {

	static public final long serialVersionUID = 0;
	
	/**
	 * SST in degress celcius.
	 */
	double seaSurfactCelcius = 10;
	
	/**
	 * temperature gradient (degrees per metre)
	 */
	double tempGradient = -0.0065; // (get's colder as you go higher) 
	
	/**
	 * Atmospheric pressure in millibars. 
	 */
	double atmosphericPressure = 1000;

	@Override
	protected RefractionParameters clone()  {
		try {
			return (RefractionParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
}
