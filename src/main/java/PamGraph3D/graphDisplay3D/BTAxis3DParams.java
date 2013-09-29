package PamGraph3D.graphDisplay3D;

import java.io.Serializable;


public class BTAxis3DParams implements Serializable, Cloneable{

	private static final long serialVersionUID = 1L;

	//axis type;
	public static final int AMPLITUDE=0x1;
	public static final int BEARING=0x2;
	public static final int ICI=0x3;
	
	public int mode=BEARING;
	
	public double bearingMin=0;
	public double bearingMax=180;
	
	public double ampMin=150;
	public double ampMax=300;
	
	//remember bearing is in radians units here
	public double vertMin=0;
	public double vertMax=Math.PI;
	public double vertVisRange=Math.PI;
	
	
	@Override
	public BTAxis3DParams clone()  {
		try {

			return (BTAxis3DParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}


	

}
