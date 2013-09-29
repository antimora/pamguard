package Acquisition.offlineFuncs;

import java.io.Serializable;

public class OfflineFileParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	/**
	 * Enable offline file access
	 */
	public boolean enable;
	
	/**
	 * include sub folders
	 */
	public boolean includeSubFolders;
	
	/**
	 * Reference to wherever the offline files are. 
	 */
	public String folderName;

	@Override
	protected OfflineFileParameters clone()  {
		try {
			return (OfflineFileParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
}
