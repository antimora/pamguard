package beakedWhaleProtocol;

import java.io.Serializable;

import PamUtils.LatLong;

public class BeakedParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 1;
	
	public int maxAcousticRange = 5000;
	
	public int maxExperimentRange = 3000;
	
	LatLong shoreStation;
	
	double shoreStationHeight;
	
	public static final int RANDOM_FLAT = 0;
	public static final int RANDOM_HALFNORMAL = 1;
	public static final String[] distributions = {"Flat", "Half normal"};
	
	public int distributionType = RANDOM_FLAT;
	
//	FluxgateWorldParameters fluxgateWorldParameters = new FluxgateWorldParameters();
	
	public boolean measureAngles, showLine;
	
	public String angleDataSource;
	
	public boolean useVideoRange;
	
	public String videoRangeDataSource;
	
	public double lineLength; // angle line length in meters
	
	public double angleUpdateInterval = 1; // update interval in seconds. 
		
	public String getDistributionName() {
		return distributions[distributionType];
	}
	
//	public int navigationStrategy;

	@Override
	public BeakedParameters clone() {

		try {
			return (BeakedParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
			return null;
		}
	}

}
