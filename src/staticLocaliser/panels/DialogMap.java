package staticLocaliser.panels;

import javax.swing.JPanel;

import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;
import targetMotion.TargetMotionLocaliser;
import targetMotion.dialog.TMDialogComponent;
import PamDetection.PamDetection;

public abstract class DialogMap implements StaticDialogComponent{


	public abstract JPanel getPanel();

	protected StaticLocalise staticLocaliser;
	
	protected 	StaticLocalisationMainPanel staticLocalisationDialog;

	/**
	 * @param targetMotionDialog
	 */
	public DialogMap(StaticLocaliserControl  StaticLocaliserControl,
			StaticLocalisationMainPanel staticLocalisationDialog) {
		super();
		this.staticLocaliser = staticLocaliser;
		this.staticLocalisationDialog = staticLocalisationDialog;
	}

	abstract public void notifyNewResults();

	abstract public void settings();

	abstract public void showMap(boolean b);
}
