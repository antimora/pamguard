package Map;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * PArameters for MapDetectionsManager which 
 * can be easily serialised and stored in pamsettings
 * 
 * @author Douglas Gillespie
 * @see MapDetectionsManager
 *
 */
public class MapDetectionsParameters implements Serializable, Cloneable {
	
	static public final long serialVersionUID = 0;
	
	protected ArrayList<MapDetectionData> mapDetectionDatas = new ArrayList<MapDetectionData>();;

	@Override
	public MapDetectionsParameters clone() {

		try {
			return (MapDetectionsParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
			return null;
		}
	}
		
}
