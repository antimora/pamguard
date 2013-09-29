package Acquisition;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Used by FileInputSystem
 * @author Doug Gillespie
 * @see Acquisition.FileInputSystem
 *
 */
public class FileInputParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 1;
	
	static public final int MAX_RECENT_FILES = 20;
	
	public ArrayList<String> recentFiles = new ArrayList<String>();
	
	public boolean realTime;

	/**
	 * repeat in an infinite loop
	 */
	public boolean repeatLoop; 
	
	@Override
	protected FileInputParameters clone() {
		try{
			return (FileInputParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}
	
}
