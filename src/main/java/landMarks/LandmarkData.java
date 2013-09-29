package landMarks;

import java.io.Serializable;

import Map.MasterReferencePoint;
import PamUtils.LatLong;
import PamView.PamSymbol;

public class LandmarkData extends Object implements Serializable, Cloneable{

	static public final long serialVersionUID = 0;

	public LatLong latLong;
	
	public double height;
	
	public PamSymbol symbol;
	
	public String name;

	@Override
	protected LandmarkData clone() {
		try {
			return (LandmarkData) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public double rangeToReference() {
		LatLong refLatLong = MasterReferencePoint.getRefLatLong();
		if (refLatLong == null) {
			return 0;
		}
		return latLong.distanceToMetres(refLatLong);
	}
	
	public double bearingToReference() {
		LatLong refLatLong = MasterReferencePoint.getRefLatLong();
		if (refLatLong == null) {
			return 0;
		}
		return latLong.bearingTo(refLatLong);		
	}
}
