/**
 * 
 */
package IshmaelDetector;

/**
 * @author Dave Mellinger
 */

import java.io.Serializable;
import java.util.ArrayList;

public class MatchFiltParams extends IshDetParams implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
	/** This is a list of file names that the user has picked recently.
	 * The 0th element of it is the matched filter kernel; later items
	 * are there only to make it easy for the user to re-pick them.
	 */ 
	ArrayList<String> kernelFilenameList = new ArrayList<String>(0);
	static public final int MAX_FILENAME_LIST_SIZE = 10;

	@Override
	protected MatchFiltParams clone() {
		return (MatchFiltParams) super.clone();
	}
	
	/** Return the name of the kernel file, or "" if none.
	 */
	public String getKernelFilename() {
		return (kernelFilenameList.size() > 0) ? kernelFilenameList.get(0) : "";
	}
}
