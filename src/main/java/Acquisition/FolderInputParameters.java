package Acquisition;

import java.io.File;
import java.io.Serializable;

/**
 * Control parameters for FolderInputSystem
 * @author Doug Gillespie
 * @see FolderInputSystem
 *
 */
public class FolderInputParameters extends FileInputParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 1;
	
	public boolean subFolders;
	
	public boolean mergeFiles;
	
	File[] selectedFiles;

	@Override
	protected FolderInputParameters clone() {
		return (FolderInputParameters) super.clone();
	}
}
