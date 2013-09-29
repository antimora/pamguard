package spectrogramNoiseReduction.medianFilter;

import java.io.Serializable;

public class MedianFilterParams implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	
	public int filterLength = 61;
	

	@Override
	public MedianFilterParams clone() {
		try {
			return (MedianFilterParams) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
