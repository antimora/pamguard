package Map;

import java.io.Serializable;

import PamguardMVC.PamDataBlock;

public class MapDetectionData implements Serializable, Cloneable {
	
	static public final long serialVersionUID = 0;
	
	public String dataName;
	
	public boolean shouldPlot;
	
	public long displaySeconds;
	
//	public long forcedStart;
	
	public boolean allAvailable;
	
	transient public PamDataBlock dataBlock;

	@Override
	public MapDetectionData clone() {

		try {
			return (MapDetectionData) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
			return null;
		}
		
	}
		

}
