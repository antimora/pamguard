/**
 * This package contains files for the Inter-pulse-interval (IPI) computation plugin
 */
package ipiDemo;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;

/**
 * Controller for sperm whale IPI computation, parameters, and displays.
 * 
 * @author Brian Miller
 */
public class IpiController extends PamControlledUnit implements PamSettings {
	/**
	 * ipiProcess does all of the actual calculation of IPI
	 */
	IpiProcess ipiProcess;

	/**
	 * User adjustable and fixed parameters for IPI stuff are stored here
	 */
	IpiProcessParameters ipiProcessParameters;

	/**
	 * Displays summary information about IPI calculations in a panel on the 
	 * left hand side of the screen.
	 */
	protected IpiSidePanel ipiSidePanel;

	/**
	 * Displays IPI signal processing results beneath a user spectrogram
	 * display. 
	 */
	protected IpiPluginPanelProvider ipiPluginPanelProvider;

	public IpiController(String unitName) {

		super("Ipi module", unitName);

		/*
		 * create the parameters that will control the process. (do this before
		 * crating the process in case the process tries to access them from
		 * it's constructor).
		 */
		ipiProcessParameters = new IpiProcessParameters();

		addPamProcess(ipiProcess = new IpiProcess(this));

		PamSettingManager.getInstance().registerSettings(this);

		setSidePanel(ipiSidePanel = new IpiSidePanel(this));

		ipiPluginPanelProvider = new IpiPluginPanelProvider(this);

	}

	@Override
	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub
		super.notifyModelChanged(changeType);

		/*
		 * This gets called every time a new module is added - make sure that
		 * the ipiProcess get's a chance to look around and see if there is data
		 * it wants to subscribe to.
		 */
		switch (changeType) {
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
		case PamControllerInterface.ADD_DATABLOCK:
		case PamControllerInterface.REMOVE_DATABLOCK:
			ipiProcess.prepareProcess();
		}
	}

	/*
	 * Menu item and action for detection parameters... (non-Javadoc)
	 * 
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " Parameters");
		menuItem.addActionListener(new SetParameters(parentFrame));
		return menuItem;
	}

	class SetParameters implements ActionListener {

		Frame parentFrame;

		public SetParameters(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {

			IpiProcessParameters newParams = IpiParametersDialog.showDialog(
					parentFrame, ipiProcessParameters);
			/*
			 * The dialog returns null if the cancel button was set. If it's not
			 * null, then clone the parameters onto the main parameters
			 * reference and call preparePRocess to make sure they get used !
			 */
			if (newParams != null) {
				ipiProcessParameters = newParams.clone();
				ipiProcess.prepareProcess();
			}

		}

	}

	/**
	 * These next three functions are needed for the PamSettings interface which
	 * will enable Pamguard to save settings between runs
	 */
	public Serializable getSettingsReference() {
		return ipiProcessParameters;
	}

	public long getSettingsVersion() {
		return IpiProcessParameters.serialVersionUID;
	}

	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		ipiProcessParameters = (IpiProcessParameters) pamControlledUnitSettings
				.getSettings();
		return true;
	}
}
