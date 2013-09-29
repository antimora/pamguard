package Array;

import PamUtils.LatLong;

public class StaticHydrophoneLocator extends SimpleHydrophoneLocator {

	/**
	 * for a static hydrophone array, the anlge can only ever be relative to North
	 * so don't need to use anlgeType here. 
	 */
	
	public StaticHydrophoneLocator(PamArray pamArray) {
		super(pamArray);		
	}
	
	@Override
	public LatLong getReferenceLatLong(long timeMilliseconds) {
		return pamArray.getFixedLatLong();
	}

	public LatLong getPhoneLatLong(long timeMilliseconds, int phoneNo) {
		LatLong latLong = pamArray.getFixedLatLong();
		if (latLong == null) return null;
		Hydrophone h = pamArray.getHydrophone(phoneNo);
		latLong = latLong.addDistanceMeters(h.getX(), h.getY());
		latLong.setHeight(h.getCoordinate(2));
		return latLong;
	}

	@Override
	public void notifyModelChanged(int changeType) {

	}

	@Override
	public String toString() {
		return "Sttatic hydrophone array";
	}

	@Override
	public double getArrayHeading(long timeMillieseconds, int phoneNo) {
		// this will always be referenced to zero (North) for a static array. 
		return 0;
	}
}
