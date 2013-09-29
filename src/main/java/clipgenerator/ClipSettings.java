package clipgenerator;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * General clip gen settings (file location, etc.
 * and list of more detector specific settings. 
 * @author Doug Gillespie
 *
 */
public class ClipSettings implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	public static final int STORE_WAVFILES = 0;
	public static final int STORE_BINARY = 1;

	/**
	 * Raw Audio Data source. 
	 */
	public String dataSourceName;
	
	/**
	 * output file folder. 
	 */
	public String outputFolder;
	
	public boolean datedSubFolders = true;
	
	public int storageOption = STORE_WAVFILES;
	
	/**
	 * List of clip generator settings. 
	 */
	private ArrayList<ClipGenSetting> clipGenSettings;

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ClipSettings clone() {
		try {
			ClipSettings newSettings =  (ClipSettings) super.clone();
			int n = getNumClipGenerators();
			newSettings.clipGenSettings = new ArrayList<ClipGenSetting>();
			for (int i = 0; i < n; i++) {
				newSettings.clipGenSettings.add(getClipGenSetting(i).clone());
			}
			return newSettings;
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 
	 * @return the number of clip generators. 
	 */
	public int getNumClipGenerators() {
		if (clipGenSettings == null) {
			return 0;
		}
		return clipGenSettings.size();
	}

	/**
	 * Get a clip generator setting objecct
	 * @param i number of the clip generator
	 * @return the slip generator settings
	 */
	public ClipGenSetting getClipGenSetting(int i) {
		if (clipGenSettings == null) {
			return null;
		}
		return clipGenSettings.get(i);
	}

	/**
	 * Find the clip generator settings for a specific data stream. 
	 * 
	 * @param dataName data name for the data block. 
	 * @return clip generator settings, or null if none active. 
	 */
	public ClipGenSetting findClipGenSetting(String dataName) {
		int n = getNumClipGenerators();
		for (int i = 0; i < n; i++) {
			if (clipGenSettings.get(i).dataName.equals(dataName)) {
				return clipGenSettings.get(i);
			}
		}
		return null;
	}

	/**
	 * Clear all clip generator settings. 
	 */
	public void clearClipGenSettings() {
		clipGenSettings.clear();
	}

	/**
	 * Add a new clip gen setting to the list. 
	 * @param clipGenSetting
	 */
	public void addClipGenSettings(ClipGenSetting clipGenSetting) {
		if (clipGenSettings == null) {
			clipGenSettings = new ArrayList<ClipGenSetting>();
		}
		clipGenSettings.add(clipGenSetting);
	}

}
