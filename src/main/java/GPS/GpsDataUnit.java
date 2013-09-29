package GPS;

import PamguardMVC.PamDataUnit;

public class GpsDataUnit extends PamDataUnit {

	/** 
	 * We could just include all the members of GpsData here, 
	 * but since the GpsData
	 * class works OK already, just leave it alone. 
	 */
	private GpsData gpsData;
	
	public GpsDataUnit(long timeMilliseconds, GpsData gpsData) {
		super(timeMilliseconds);
		this.gpsData = gpsData;
	}

	/**
	 * @return Returns the gpsData.
	 */
	public GpsData getGpsData() {
		return gpsData;
	}
	
	public void setGpsData(GpsData gpsData) {
		this.gpsData = gpsData;
		this.timeMilliseconds = gpsData.getTimeInMillis();
	}

}
