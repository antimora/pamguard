package staticLocaliser.panels;

import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.swing.JPanel;

import PamDetection.PamDetection;

/**
 *Any control panel for the static localiser must use this basic interface and make sure all these functions are implemented properly. The control essentially allows the user to pick a single pamdetection to pass to the localiser. It also specifies what type of detection the user wishes to localise by passing an int flag to 
 *the detectionMatch class for the pamDetection. For example, in the case of a clickDetection, the user can pass flags telling the detection match class only to include classified clicks in it's calculation of time delays. 
 * @author Jamie Macaulay
 *
 */
public interface LocaliserControlModel {
	
	/**
	 * 
	 * @return the current selected detection in the control panel. 
	 */
	public PamDetection getCurrentDetection();
		
	/**
	 * 
	 * @return a list of all the detection shown in the control panel. 
	 */
	public ArrayList<PamDetection> getCurrentDetections();
	
	
	/**
	 * return a flag to calculate what subset of detections to classify. For example, clicks have flags which allow only classified or event type clicks to be considered for time calculations
	 * @return
	 */
	public Integer getDetectionType();
	
	/**
	 * Refresh all the data in the control panel. This is called when offline data is loaded, a datblock is changed, channels changed, or data is added to a datablock.  
	 */
	public void refreshData();
	
	/**
	 * Checks to see if this pamDetection can be localised. This will depend on the settings in the control panel. For example, if a control panel selects that only clicks classified as a certain species can be used then this function should return true if the pamDetection belongs to this species.
	 * This function is mainly used to test pamDetection outwith loaded data i.e. the input pamDetection does NOT necassarily have to be in the current set of loaded data.  
	 * @return
	 */
	public boolean canLocalise(PamDetection pamDetection);
	
	/**
	 * Had to implement this as offline datablocks had problems with channel maps.
	 */
	public int[] getChannelMap();
	


}
