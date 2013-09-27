package xBatLogViewer;

import java.io.Serializable;

public class XBatLogSettings implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;


	@Override
	protected XBatLogSettings clone() {
		try {
			return (XBatLogSettings) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
