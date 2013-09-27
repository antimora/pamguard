package PamGraph3D.graphDisplay3D;

import javax.media.j3d.Shape3D;

import PamguardMVC.PamDataUnit;

/**
 * Simple class which holds a 3D shape and 
 * @author spn1
 *
 */
public class DataUnitShape3D extends Shape3D {
	
	PamDataUnit dataUnit;
	
	DataUnitShape3D(PamDataUnit dataUnit, Shape3D shape3D){
		this.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		this.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		this.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		this.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);


		this.dataUnit=dataUnit;
		this.setGeometry(shape3D.getGeometry());
		this.setAppearance(shape3D.getAppearance());
	}
	
	public PamDataUnit getdataUnit(){
		return dataUnit;
	}
	
	public void setdataUnit(PamDataUnit dataUnit){
		this. dataUnit=dataUnit;
	}

}
