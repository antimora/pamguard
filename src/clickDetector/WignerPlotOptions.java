package clickDetector;

import java.io.Serializable;

public class WignerPlotOptions implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	public boolean limitLength = true;
	
	public int manualLength = 128;

	@Override
	protected WignerPlotOptions clone() {
		try {
			return (WignerPlotOptions) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
