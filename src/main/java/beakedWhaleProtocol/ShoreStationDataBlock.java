package beakedWhaleProtocol;

import PamUtils.LatLong;
import PamguardMVC.PamProcess;
import PamguardMVC.SingletonDataBlock;

public class ShoreStationDataBlock extends SingletonDataBlock<ShoreStationDataUnit> {

	public ShoreStationDataBlock(String dataName, PamProcess parentProcess) {
		super(ShoreStationDataUnit.class, dataName, parentProcess, 0);
		setNaturalLifetime(1000*3600*24*365); // set it to a year. 
	}

	/**
	 * Say if a new unit is needed - i.e. if there isn't already one in 
	 * memory, or if the position or height have changed. 
	 * @param newLatlong
	 * @param newHeight
	 * @return true if something has chnaged which makes us need to create a new data unit
	 */
	protected boolean needNew(LatLong newLatlong, double newHeight) {
		ShoreStationDataUnit lastUnit = getLastUnit();
		if (lastUnit == null) return true;
		if (lastUnit.getDatabaseIndex() <= 0) return true;
		if (lastUnit.getLatLong().getLatitude() != newLatlong.getLatitude()) return true;
		if (lastUnit.getLatLong().getLongitude() != newLatlong.getLongitude()) return true;
		if (lastUnit.getHeight() != newHeight) return true;
		return false;
	}
	
}
