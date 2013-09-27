package PamView;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.Serializable;

public class GuiParameters implements Serializable, Cloneable {

	static final long serialVersionUID = 1;
	
	int selectedTab = 0;
	
	boolean isZoomed = true;
	
	int state, extendedState;
	
	Dimension size;
	
	Rectangle bounds;
	
	boolean hideSidePanel;
	

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected GuiParameters clone() {
		try {
			return (GuiParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
		
}
