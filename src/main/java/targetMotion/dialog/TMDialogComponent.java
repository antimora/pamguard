package targetMotion.dialog;

import javax.swing.JPanel;

public interface TMDialogComponent {
	
	/**
	 * 
	 * @return a panel to include in the main dialog
	 */
	JPanel getPanel();
	
	/**
	 * Current event has been set (possibly in one of the other panels)
	 * @param event
	 * @param sender
	 */
	void setCurrentEventIndex(int eventIndex, Object sender);
	
	/**
	 * Enable controls - based on event selection and other controls 
	 */
	void enableControls();
	
	/**
	 * 
	 * @return true if settigns on this panel think it's possible to start a run
	 */
	boolean canRun();
}
