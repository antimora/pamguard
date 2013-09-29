package staticLocaliser;

import java.io.Serializable;
import java.util.ArrayList;

import PamController.PamSettings;


public class StaticLocaliserParams implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;

	//Batch processing
	public int minNumberofTDs=4;
	
	public boolean firstOnly=false;
	
	public boolean primaryHydrophoneSel=false;
	
	public int primaryHydrophone=0;
	
	//General
	
	public int maximumNumberofPossibilities=200;
	
	public boolean showOnlyLowestChiValueDelay=false;

	//Map
	public boolean mapAxisVis=false;
	
	//Export Delays
	
	public ArrayList<String> tdExportFiles=new ArrayList<String>();
	
	public boolean appendDelays=true;
	;
	//channels  to use in localisation
	public int [] channels=null;
	
	//use high resolution map symbols
	public boolean useHiResSymbols=false;

	
	
	@Override
	public StaticLocaliserParams clone()  {
		try {

			return (StaticLocaliserParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}


	

}
