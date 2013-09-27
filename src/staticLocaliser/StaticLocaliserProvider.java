package staticLocaliser;

import java.awt.Window;

import staticLocaliser.panels.AbstractLocaliserControl;

public interface StaticLocaliserProvider {
	
	/**
	 * Any datablock can provide a localiser control panel for the static localiser, however it must be tailored to the specific type of detection. For example clicks would require a control panel which can sort out events, classified clicks etc.
	 * @param staticLocaliserControl
	 * @return
	 */
	public AbstractLocaliserControl getSLControlDialog(StaticLocaliserControl staticLocaliserControl);
	
	}
