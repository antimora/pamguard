package RightWhaleEdgeDetector;

import java.io.Serializable;

public class RWEParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 0L;

	public String dataSourceName;
	
	public int channelMap;

	public double startFreq = 0;
	
	public double endFreq = 1000;
	
	public double[] backgroundConst = {16, 160};
	
	public double threshold = 4;
	
	public int maxSoundGap = 1;

	public int maxFrequencyGap = 1;
	
	public int minSoundType = 5;
	
	@Override
	protected RWEParameters clone() {
		try {
			return (RWEParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
