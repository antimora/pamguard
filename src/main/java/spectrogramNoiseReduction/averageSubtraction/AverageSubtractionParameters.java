package spectrogramNoiseReduction.averageSubtraction;

import java.io.Serializable;

public class AverageSubtractionParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	
	public double updateConstant = 0.02;
	

	@Override
	public AverageSubtractionParameters clone() {
		try {
			return (AverageSubtractionParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
