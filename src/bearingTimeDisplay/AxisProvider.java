package bearingTimeDisplay;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import Layout.PamAxis;
import PamDetection.PamDetection;
import PamguardMVC.PamDataUnit;

public interface AxisProvider {
	
	/**
	 * Get the panel containing the axis, scroll bars, range spinners etc
	 * @return
	 */
	 public JPanel getAxisControlPanel();
	 
	 
	 /**
	  * Get the PamAxis. BY default the PamAxis will be opposite 
	  */
	 public PamAxis getPamAxis();
	
	 /**
	  * Get the maximum value of the axis.
	  * @return
	  */
	 double getAxisMax();
	
	 /**
	  * Get the absolute minimum value of the axis (often zero)
	  * @return
	  */
	 double getAxisMin();
	
	
	 /**
	  * Get the current visible range of the axis. Without the JSpinners the visible range is simply the axis min-axis max. Otherwise the visible range is the the range of the data on this axis displayed to the user. 
	  * @return
	  */
	 double getAxisVisibleRange();
	 
	 //scroll bar functions are not required and can be set to  null in the abstractaxis class.
	 
	 /**
	  * Get the current scroll bar value
	  * @return
	  */
	 double getScrollBarValue();
	 
	 /**
	  * Get the maximum value of the scroll bar
	  * @return
	  */
	 double getScrollBarMax();
	 
	 /**
	  * Get the minimum of the scroll bar
	  * @return
	  */
	 double getScrollBarMin();
	 
	 /***
	  * Add menu items to the JPopUpMenu() in the display. Settings with potential dialog boxes will be here along with all other options for this axis. 
	  * @param popUpMenu
	  */
	 void addMenuItems(JPopupMenu popUpMenu);
	 
	 /**
	  * A name for this axis
	  * @return
	  */
	 String getAxisName();
		 
	 
	 /**
	  * Any PamDataBlock can be added to a GraphDisplay. The axis defines what measurement should be taken from each PamDataUnit. For example a time axis would return the timeMillis from a pamDetection. 
	  *
	  * @param pamDetection
	  * @return the measured value to be used on the graph
	  */
	 double getMeasurment(PamDataUnit pamDataUnit);
	 
	 /**
	  * Invert the axis; true-min is at pixel zero, max at maxPixel. false-min is at max pixel, max at min pixel. e.g: bearing axis is inverted because convention in pamguard has 0 at the top of the graph and 180 at the bottom. 
	  * @return boolean indicating whether to invert axis.
	  */
	 boolean invert();
		
}