package PamView;

import java.awt.GridBagConstraints;
import java.io.Serializable;

public class CornerLayoutContraint  implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	

	static public final int PAGE_END = GridBagConstraints.PAGE_END;
	static public final int PAGE_START = GridBagConstraints.PAGE_START;
	static public final int LINE_END = GridBagConstraints.LINE_END;
	static public final int LINE_START = GridBagConstraints.LINE_START;
	static public final int FIRST_LINE_START = GridBagConstraints.FIRST_LINE_START;
	static public final int FIRST_LINE_END = GridBagConstraints.FIRST_LINE_END;
	static public final int LAST_LINE_END = GridBagConstraints.LAST_LINE_END;
	static public final int LAST_LINE_START = GridBagConstraints.LAST_LINE_START;
	static public final int CENTER = GridBagConstraints.CENTER;
	
	public int anchor = FIRST_LINE_START;

	@Override
	protected CornerLayoutContraint clone() {
		try {
			return (CornerLayoutContraint) super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}

	
}
