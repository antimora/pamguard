package PamDetection;

import java.util.ArrayList;

import PamguardMVC.PamDataUnit;

public interface DetectionMatchModel {
	
	public ArrayList<PamDataUnit> getDetectionGroup();
	
	/**
	 * Gets the number of non null time delays for this pamDetection. Note: this is not the same number of possible timedelay possibilities. For any detection, if there is more than one detection that might correpsond to the primary detection on any channel, then there is more than one combination of time delays. However, although there might be 
	 * more than one detection on a channel, there also may be a channel with no possible corresponding detections at all. In this case, depending on the number of channels without a detection, there will be set number of null time delays. This equivalent to simply removing a hydrophone from the array. 
	 * @return the number of non null time delays for the current pamDetection. Holds for every possible combination.  
	 */
	public int getNumberofNonNullDelays();
	
	/**
	 * To group detections run through every detection on the primary hydrophone. Use the absolute distance of each hydrophone element from the primary hydrophone to create a time window before and after the primary detection. All detections located within the time window of 
	 * each hydrophone possibly correspond to the primary detection. Work out all the possible combination of detections and the corresponding time delays.  
	 * @return a set of time delays. Each ArrayList<Double> represents a set of time delays between ALL hydrophones using the indexM1 and indexM2 rules. Multiple combinations are then added to ArrayList<ArrayList<Double>> and returned by this function;
	
	 */
	public ArrayList<ArrayList<Double>> getGroupTimeDelays();
	
	/**Quickly calculate the number of possible time delay possibilities there will be for this localisation. Remember to include flags,e.g. the number of possibilities for a only event clicks 
	 * 
	 * @return
	 */
	public int getNPossibilities();

	
	
}
