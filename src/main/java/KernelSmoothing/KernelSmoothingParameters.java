package KernelSmoothing;

import java.io.Serializable;

public class KernelSmoothingParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	
	public int fftBlockIndex = 0;
	
	public int channelList = 1;

	@Override
	protected KernelSmoothingParameters clone() {
		try {
			return (KernelSmoothingParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			return null;
		}
	}
	
}
