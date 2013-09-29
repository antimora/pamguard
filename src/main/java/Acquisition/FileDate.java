package Acquisition;

import java.io.File;

/**
 * Extract file dates from the file name
 * <p>
 * Should be possible to make many different implementations 
 * of this for handling different file name formats. 
 * 
 * @author Doug Gillespie
 *
 */
public interface FileDate {

	/**
	 * Get a time in milliseconds from a file date. 
	 * @param file file
	 * @return time in milliseconds or 0 if can't work it out. 
	 */
	long getTimeFromFile(File file);
	
	boolean hasSettings();
	
	boolean doSettings();
	
	String getName();
	
	String getDescription();
	
}
