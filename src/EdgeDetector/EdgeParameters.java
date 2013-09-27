package EdgeDetector;

import java.io.Serializable;

public class EdgeParameters implements Serializable, Cloneable {

	static final long serialVersionUID = 0;
	// source
//	String sourceDataBlock;
	int fftBlockIndex;
    // detection
    double detTh = 5;
    double bgTC0 = 16;
    double bgTC1 = 160;
    double analFreqB_1 = 0;
    double analFreqB_2 = 127;
    double maxSoundG = 1;
    // classification
//    int minSoundType ;
    int minSLen = 0;
    int maxSLen = 50;
	
	
	@Override
	protected EdgeParameters clone() {
		try {
			return (EdgeParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			return null;
		}
	}

}
