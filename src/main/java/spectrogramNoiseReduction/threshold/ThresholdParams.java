package spectrogramNoiseReduction.threshold;

import java.io.Serializable;

public class ThresholdParams implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	
	public double thresholdDB = 8;
	
	public int finalOutput = SpectrogramThreshold.OUTPUT_RAW;
	

	@Override
	public ThresholdParams clone() {
		try {
			return (ThresholdParams) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
