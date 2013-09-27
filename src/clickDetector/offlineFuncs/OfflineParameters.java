package clickDetector.offlineFuncs;

import java.io.Serializable;

public class OfflineParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;

	public static final int ANALYSE_LOADEDDATA = 0;
	public static final int ANALYSE_ALLDATA = 1; 
	
	public int analSelection = ANALYSE_LOADEDDATA;
	
	public boolean doClickId = true;
	
	@Override
	protected OfflineParameters clone() {
		try {
			return (OfflineParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
