package depthReadout;

import java.io.Serializable;

public class DepthParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 0;

	public int systemNumber;

	public double pollTime = 2;

	public int[] hydrophoneMaps;
	
	public double[] hydrophoneY;
	
	public int nSensors = 2;

	@Override
	public DepthParameters clone() {
		try {
			return (DepthParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
