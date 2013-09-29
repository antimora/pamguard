package beakedWhaleProtocol;

import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;



public class ShoreStationDataUnit extends PamDataUnit {

	private LatLong latLong;
	private double height;
	
	private Double measuredAngle;
	
	public ShoreStationDataUnit(long timeMilliseconds, LatLong latLong, double height) {
		super(timeMilliseconds);
		this.latLong = latLong;
		this.height = height;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public LatLong getLatLong() {
		return latLong;
	}

	public void setLatLong(LatLong latLong) {
		this.latLong = latLong;
	}

	public Double getMeasuredAngle() {
		return measuredAngle;
	}

	public void setMeasuredAngle(Double measuredAngle) {
		this.measuredAngle = measuredAngle;
	}
	

}
