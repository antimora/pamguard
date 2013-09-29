package clickDetector.echoDetection;

import java.io.Serializable;

public class SimpleEchoParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	public double maxIntervalSeconds = 0.1;

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected SimpleEchoParams clone() {
		try {
			return (SimpleEchoParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
