package staticLocaliser.panels;

import javax.media.j3d.Shape3D;

import staticLocaliser.StaticLocalisationResults;

/**
 * A simple extension of Shape3D which holds a localisation results and automatically prepares the SHAPE3D for appearance changes and pick reporting. An integer flag can be stored- this can be used to identify multiple shapes or appearances which correspond to the same localisation
 * @author Jamie Macaulay
 *
 */
public class LocShape3D extends Shape3D{
	
	private StaticLocalisationResults staticLocalisationResults;
	private int flag;
	
	public LocShape3D(Shape3D shape3d, StaticLocalisationResults staticLocalisationResult){
		this.staticLocalisationResults=staticLocalisationResult;
		this.setGeometry(shape3d.getGeometry());
		this.setAppearance(shape3d.getAppearance());
		this.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		this.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		this.setCapability(Shape3D.ENABLE_PICK_REPORTING);
	}
	
	public LocShape3D(Shape3D shape3d, StaticLocalisationResults staticLocalisationResult, int flag){
		this.staticLocalisationResults=staticLocalisationResult;
		this.setGeometry(shape3d.getGeometry());
		this.setAppearance(shape3d.getAppearance());
		this.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		this.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		this.setCapability(Shape3D.ENABLE_PICK_REPORTING);
		this.flag=flag;
	}
	
	public StaticLocalisationResults getStaticLocResult(){
		return staticLocalisationResults;
	}
	
	public void setFlag(int flag){
		this.flag=flag;
	}
	
	public int getFlag(){
		return flag;
	}


}
