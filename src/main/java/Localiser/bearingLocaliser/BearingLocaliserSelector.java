package Localiser.bearingLocaliser;

import Array.ArrayManager;
import Array.PamArray;

/**
 * Class to automatically select  / create the most appropriate type of 
 * bearing localiser. 
 * @author Doug Gillespie
 *
 */
public class BearingLocaliserSelector {

	public static BearingLocaliser createBearingLocaliser(int hydrophoneMap, double timingError) {
		ArrayManager arrayManager = ArrayManager.getArrayManager();
		PamArray currentArray = arrayManager.getCurrentArray();
		int arrayType = arrayManager.getArrayType(hydrophoneMap);
		switch(arrayType) {
		case ArrayManager.ARRAY_TYPE_NONE:
		case ArrayManager.ARRAY_TYPE_POINT:
			return null;
		case ArrayManager.ARRAY_TYPE_LINE:
			return new PairBearingLocaliser(hydrophoneMap, timingError);
		case ArrayManager.ARRAY_TYPE_PLANE:
		case ArrayManager.ARRAY_TYPE_VOLUME:
			return new MLGridBearingLocaliser(hydrophoneMap, timingError);
		}
		return null;
	}
	
	
}
