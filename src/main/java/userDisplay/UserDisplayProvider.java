package userDisplay;

import java.awt.Component;

/**
 * Provider of displays for the main display panel. 
 * Can be implemented by anything anywhere in PAMGUard
 * and will add the appropriate menu command to 
 * the user display menu, etc. 
 * @author Doug Gillespie
 *
 */
public interface UserDisplayProvider {

	public String getName();
	
	public Component getComponent();
	
	public Class getComponentClass();
		
}
