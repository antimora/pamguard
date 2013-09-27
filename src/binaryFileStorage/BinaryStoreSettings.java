package binaryFileStorage;

import java.io.Serializable;

public class BinaryStoreSettings implements Serializable, Cloneable {
	
	public static final long serialVersionUID = 0L;
	
	private String storeLocation = System.getProperty("user.home");
	
	public boolean datedSubFolders = true;

	public boolean autoNewFiles = true;
	
	public int fileSeconds = 3600;
	
	public boolean limitFileSize = false;
	
	public int maxFileSize = 100; // max file size in megabytes.  
	
	public BinaryStoreSettings() {
		super();
	}

	/**
	 * test to see if it's necessary to open new stores
	 * @param other
	 * @return true if the output folder or sub folders flag have changed 
	 * false for other changes. 
	 */
	boolean isChanged(BinaryStoreSettings other) {
		if (storeLocation == null && other.storeLocation == null) {
			return false;
		}
		if (storeLocation == null || other.storeLocation == null) {
			return true;
		}
		if (storeLocation.equals(other.storeLocation) == false) {
			return true;
		}
		if (datedSubFolders != other.datedSubFolders) {
			return true;
		}
		
		return false;
	}

	@Override
	protected BinaryStoreSettings clone() {
		try {
			return (BinaryStoreSettings) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getMaxSizeMegas() {
		return Math.max(1, maxFileSize) * 1024*1024;
	}
	
	public String getStoreLocation() {
		return storeLocation;
	}
	
	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}
 
	public boolean isDatedSubFolders() {
		return datedSubFolders;
	}
	

	
}
