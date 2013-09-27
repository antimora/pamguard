package beakedWhaleProtocol;

import videoRangePanel.VRDataUnit;
import PamUtils.LatLong;

/**
 * BAsic ingormation on beaked whale location returned from BeakedLocationDialog
 * @author Douglas Gillespie
 *
 */
public class BeakedLocationData {
	
	public static final int SOURCE_UNKNOWN = 0;
	public static final int SOURCE_SHORESIGHTING_THEODOLITE = 1;
	public static final int SOURCE_SHORESIGHTING_VIDEO = 2;
	public static final int SOURCE_SHORESIGHTING_RETICULE = 3;
	public static final int SOURCE_SHORESIGHTING_OTHER = 4;
	public static final int SOURCE_VESSELSIGHTING = 5;
	public static final int SOURCE_OTHERVESSELSIGHTING = 6;
	public static final int SOURCE_ACOUSTICTRACKING = 7;
	public static final int SOURCE_OTHER = 8;
	public static String[] locationSources = {"Unknown source", "Shore Sighting (Theodolite)", 
		"Shore Sighting (Video)", "Shore Sighting (Reticule)", "Shore Sighting (OTHER)", 
		"Vessel Sighting", "Other Vessel Sighting", "Acoustic Tracking", "OTHER"}; 

	public LatLong latLong;
	
	//range and bearing from shore station. 
	public double range, bearing;
	
	public int locationSource;
	
	public String comment;
	
	public VRDataUnit videoData;
	
	public static int interpretSource(String sourceName) {
		String sn = sourceName.trim();
		for (int i = 0; i < locationSources.length; i++) {
			if (sn.equals(locationSources[i])) {
				return i;
			}
		}
		
		return SOURCE_UNKNOWN;
	}
	
	public static boolean enterLatLong;
	
}
