package likelihoodDetectionModule;

import java.io.Serializable;

/**
 * The Class ConfigurationDialogSettings holds parameters about the
 * configuration dialog box that are persistent from one invocation
 * to the next.
 */
public class ConfigurationDialogSettings implements Serializable {
	
	/** The Constant serialVersionUID. */
	static final long serialVersionUID = 234123;
	
	/** The first column width. */
	public int firstColumnWidth = 75;
	
	/** The second column width. */
	public int secondColumnWidth = 75;
	
	/** The expanded state. */
	String expandedState = new String();
}
