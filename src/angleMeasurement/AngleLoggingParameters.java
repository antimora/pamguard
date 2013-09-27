package angleMeasurement;

import java.io.Serializable;

public class AngleLoggingParameters extends Object implements Cloneable,
		Serializable {

	static public final long serialVersionUID = 0;
	
	static public final int LOG_NONE = 0;
	static public final int LOG_ALL = 1;
	static public final int LOG_HELD = 2;
	static public final int LOG_TIMED = 3;
		
	public int logAngles = LOG_NONE;
	
	public double logInterval = 10;
	
	public boolean timedRandom = false;

	@Override
	public AngleLoggingParameters clone()  {

		try {
			return (AngleLoggingParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	
}
