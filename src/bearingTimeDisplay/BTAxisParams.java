package bearingTimeDisplay;

import java.io.Serializable;


public class BTAxisParams implements Serializable, Cloneable{

	private static final long serialVersionUID = 1L;

	//axis type;
	public static final int AMPLITUDE=0x1;
	public static final int BEARING=0x2;
	public static final int ICI=0x3;
	
	public int mode=BEARING;
	
	public double bearingMin=0;
	public double bearingMax=180;
	
	public double ampMin=60;
	public double ampMax=250;
	
	//remember bearing is in radians units here
	public double vertMin=0;
	public double vertMax=Math.PI;
	public double vertVisRange=Math.PI;
	
	
	@Override
	public BTAxisParams clone()  {
		try {

			return (BTAxisParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}


}
