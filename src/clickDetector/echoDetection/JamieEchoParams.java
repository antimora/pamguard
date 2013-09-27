package clickDetector.echoDetection;

import java.io.Serializable;

public class JamieEchoParams implements Serializable, Cloneable {
	
public static final long serialVersionUID = 3L;
	
//these are default settings for a porpoise;

	public double maxAmpDifference=0.5;
	public double maxICIDifference=0.2;
	public double maxIntervalSeconds = 0.0012;


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected JamieEchoParams clone() {
		try {
			return (JamieEchoParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
}
