package generalDatabase;

import dataMap.OfflineDataMapPoint;
import PamUtils.PamCalendar;

public class DBOfflineDataMapPoint extends OfflineDataMapPoint {

	public DBOfflineDataMapPoint(long startTime, long endTime, int datas) {
		super(startTime, endTime, datas);
	}

	/**
	 * Add a new end time to the data map
	 * @param utcMillis time in millis
	 */
	public void addNewEndTime(long utcMillis) {
		setEndTime(utcMillis);
		setNDatas(getNDatas()+1);
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.OfflineDataMapPoint#getName()
	 */
	@Override
	public String getName() {
		return String.format("Database %s to %s", 
				PamCalendar.formatDateTime(getStartTime()), 
				PamCalendar.formatTime(getEndTime()));
	}

}
