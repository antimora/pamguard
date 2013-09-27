package PamGraph3D.graphDisplay3D;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import PamDetection.PamDetection;
import PamguardMVC.PamDataUnit;

public interface GraphDisplay3DAxisProvider {
	
	/**
	 * Get the 
	 * @return
	 */
	 JPanel getPanel();
		
	 double getAxisMax();
	
	 double getAxisMin();
	
	 double getAxisValue();
	
	 double getAxisVisibleRange();
	 
	 double getScrollBarValue();
	 
	 double getScrollBarMax();
	 
	 double getScrollBarMin();
	 
	 /***
	  * Add menu items to the JPopUpMenu() in the diplay. Settings with potential dialog boxes will be here along with all other options for this axis. 
	  * @param popUpMenu
	  */
	 void addMenuItems(JPopupMenu popUpMenu);
	 
	 /**
	  * A name for this axis
	  * @return
	  */
	 String getAxisName();
		 
	 
	 /**
	  * Any PamDataBlock can be added to a graph3D. The axis defines what measurement should be taken from each PamDataUnit. For example a time axis would return the timeMillis from a pamDetection. 
	  *
	  * @param pamDetection
	  * @return the measured value to be used on the geaph
	  */
	 double getMeasurment(PamDataUnit pamDataUnit);
		
}
