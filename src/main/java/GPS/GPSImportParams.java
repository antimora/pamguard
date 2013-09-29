package GPS;

import java.io.Serializable;
import java.util.ArrayList;

public class GPSImportParams implements Serializable, Cloneable  {
	
	public ArrayList<String> path=new ArrayList<String>();
	
	//for GGA strings
	//final launch of block 1 GPS satellites
	public int month=9; ///not months are from zero; 

	public int year=1985;

	public int day=9;

	public boolean useGGA=true; 

	
	
	
	//public boolean deletePrev=false; 
	
	@Override
	public GPSImportParams clone()  {
		try {

			return (GPSImportParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
