package Localiser.timeDelayLocalisers;

import java.io.Serializable;

import PamView.ColourArray.ColourArrayType;

public class LikelihoodSurfaceParams implements Serializable, Cloneable  {
	
	public ColourArrayType colourMap = ColourArrayType.HOT;
	
	public void setColourMap(ColourArrayType colourMap) {
		this.colourMap = colourMap;
	}
	
	public ColourArrayType getColourMap() {
		if (colourMap == null) {
			colourMap = ColourArrayType.GREY;
		}
		return colourMap;
	}

	public boolean is3D=true;
	
	// the max chi value to show on surface, any values larger than this maximum value are reperesented as being the maxChiValue;
	public float maxChiValue=10000000;
	
	//this is the size of the surface, in meters, around the simplex loacisation. 
	public float gridRange=100;
	
	// this is essentially the surgface meshSize...Note if the the mesh size is very small and the grid size is very large the surface will take a long time to draw. In general this should 1/100 over the gridRange. 
	public float gridMeshSize=2; 
	
	int dim=2;

	@Override
	public LikelihoodSurfaceParams clone() {
		try {
			return (LikelihoodSurfaceParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
