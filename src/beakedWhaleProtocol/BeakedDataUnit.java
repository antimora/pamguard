package beakedWhaleProtocol;

import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;

public class BeakedDataUnit extends PamDataUnit {

	private BeakedLocationData beakedLocationData;
	
	public BeakedDataUnit(long timeMilliseconds, BeakedLocationData beakedLocationData) {
		super(timeMilliseconds);
		this.beakedLocationData = beakedLocationData;
	}

	public LatLong getBeakedLatLong() {
		return beakedLocationData.latLong;
	}
	
	public String getBeakedComment() {
		return beakedLocationData.comment;
	}

	public int getLocationSource() {
		return beakedLocationData.locationSource;
	}
	
	public String getLocationName() {
		return BeakedLocationData.locationSources[beakedLocationData.locationSource];
	}
	
	public BeakedLocationData getBeakedLocationData() {
		return beakedLocationData;
	}

	public void setBeakedLocationData(BeakedLocationData beakedLocationData) {
		this.beakedLocationData = beakedLocationData;
	}


}
