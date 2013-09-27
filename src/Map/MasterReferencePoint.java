package Map;

import PamUtils.LatLong;

/**
 * Master reference point for PAMGAURD. This is 
 * a bit of a stopgap to get some bearing and range
 * info from Landmarks, but may get used later on
 * to contain a list of possible reference marks that
 * can be selected. 
 * <br>
 * Currently gets data from the map whenever GPS updates, or whenever 
 * static hydrophone locations update. 
 * <br>
 * Everything static.
 * 
 * @author Douglas Gillespie
 *
 */
public class MasterReferencePoint {
	
	private static LatLong refLatLong = new LatLong(0,0);
	
	private static String referenceName = "";


	public static void setRefLatLong(LatLong refLatLong, String referenceName) {
		MasterReferencePoint.refLatLong = refLatLong;
		MasterReferencePoint.referenceName = referenceName;
	}
	
	public static LatLong getRefLatLong() {
		return refLatLong;
	}

	public static String getReferenceName() {
		return referenceName;
	}

	
}
