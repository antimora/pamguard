package binaryFileStorage;

import java.io.File;

/**
 * Little bit of data to add to all PamDataUnits
 * when they are read back from a file which will 
 * be used to say which file they were from, whether they
 * need updating, etc. 
 * @author Doug Gillespie
 *
 */
public class DataUnitFileInformation {

	/**
	 * File this data unit was read from
	 */
	private File file;
	
	/**
	 * Set if the unit has changes offline, in which case
	 * the binary file will probably need rewriting
	 */
	private boolean needsUpdate;
	
	/**
	 * Objects index in the file. 
	 */
	private long indexInFile;

	public DataUnitFileInformation(File file, long indexInFile) {
		super();
		this.file = file;
		this.indexInFile = indexInFile;
	}
	
	/**
	 * Get a file name, but shortened to a maximum number of 
	 * characters. 
	 * <p>Since the most file specific information will always come at the 
	 * end of the file, the beginning of the name is stripped off and the 
	 * end left alone. 
	 * @param maxChars max length of file name to be returned. 
	 * @return a shortened file name. 
	 */
	public String getShortFileName(int maxChars) {
		String fileName = file.getName();
		if (fileName == null || fileName.length() <= maxChars) {
			return fileName;
		}
		return fileName.substring(fileName.length()-maxChars);
	}

	/**
	 * @return the needsUpdate
	 */
	public boolean isNeedsUpdate() {
		return needsUpdate;
	}

	/**
	 * @param needsUpdate the needsUpdate to set
	 */
	public void setNeedsUpdate(boolean needsUpdate) {
		this.needsUpdate = needsUpdate;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return the indexInFile
	 */
	public long getIndexInFile() {
		return indexInFile;
	}
	
}
