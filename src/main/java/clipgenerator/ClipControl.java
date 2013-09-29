package clipgenerator;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayProvider;

import clipgenerator.clipDisplay.ClipDisplayPanel;
//import clipgenerator.clipDisplay.ClipTabPanel;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamTabPanel;

/**
 * The clip generator is used to generate short sound clips around detections. 
 * In principle, it can be triggered by just about any dataunit which is a
 * subclass of AcousticDataUnit, i.e. one having both a start time and a 
 * duration. 
 * <p>
 * The clip generator configuration contains a budget of how many clips it
 * can make for each possible trigger and will, based on usage, decide in a semi
 * random way whether or not to make a clip on the arrival of each detection. 
 * <p>
 * Trigger specific settings also include a pre and post sample time.
 * <p>
 * @author Doug Gillespie
 *
 */

public class ClipControl extends PamControlledUnit implements PamSettings {

	protected ClipProcess clipProcess;
	
	protected ClipSettings clipSettings = new ClipSettings();

	private boolean initializationComplete;

//	private ClipTabPanel clipTabPanel;
	
	private UserDisplayProvider userDisplayProvider;
	
	public ClipControl(String unitName) {
		
		super("Clip Generator", unitName);

		addPamProcess(clipProcess = new ClipProcess(this));
		
		PamSettingManager.getInstance().registerSettings(this);
		
//		clipTabPanel = new ClipTabPanel(this);
		
		userDisplayProvider = new ClipDisplayProvider();
		UserDisplayControl.addUserDisplayProvider(userDisplayProvider);
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " settings ...");
		menuItem.addActionListener(new ClipSettingsMenu(parentFrame));
		return menuItem;
	}
	
	private class ClipSettingsMenu implements ActionListener {

		private Frame parentFrame;
		
		/**
		 * @param parentFrame
		 */
		public ClipSettingsMenu(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ClipSettings newSettings = ClipDialog.showDialog(parentFrame, clipSettings);
			if (newSettings != null) {
				clipSettings = newSettings.clone();
				clipProcess.subscribeDataBlocks();
			}
		}
		
	}

	@Override
	public Serializable getSettingsReference() {
		return clipSettings;
	}

	@Override
	public long getSettingsVersion() {
		return ClipSettings.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		clipSettings = ((ClipSettings) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch(changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			initializationComplete = true;
			break;
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
			clipProcess.subscribeDataBlocks();
			break;
		}
	
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("RawDataSource", clipSettings.dataSourceName);
		paramsEl.setAttribute("OutputFolder", clipSettings.outputFolder);
		paramsEl.setAttribute("datedSubFolders", ((Boolean) clipSettings.datedSubFolders).toString());
		paramsEl.setAttribute("storageOption", ((Integer) clipSettings.storageOption).toString());
		int n = clipSettings.getNumClipGenerators();
		paramsEl.setAttribute("NumSettings", ((Integer) n).toString());
		ClipGenSetting clipSet;
		Element setEl;
		for (int i = 0; i < n; i++) {
			 clipSet = clipSettings.getClipGenSetting(i);
			 setEl = doc.createElement("Set"+i);
			 setEl.setAttribute("dataName", clipSet.dataName);
			 setEl.setAttribute("enable", ((Boolean) clipSet.enable).toString());
			 setEl.setAttribute("preSeconds", ((Double) clipSet.preSeconds).toString());
			 setEl.setAttribute("postSeconds", ((Double) clipSet.postSeconds).toString());
			 setEl.setAttribute("channelSelection", ((Integer) clipSet.channelSelection).toString());
			 setEl.setAttribute("clipPrefix", clipSet.clipPrefix);
			 setEl.setAttribute("useDataBudget", ((Boolean) clipSet.useDataBudget).toString());
			 setEl.setAttribute("dataBudget", ((Integer) clipSet.dataBudget).toString());
			 setEl.setAttribute("budgetPeriod", ((Double)clipSet.budgetPeriodHours).toString());
			 
			 paramsEl.appendChild(setEl);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#getTabPanel()
	 */
//	@Override
//	public PamTabPanel getTabPanel() {
//		return clipTabPanel;
//	}

	/**
	 * @return the clipProcess
	 */
	public ClipProcess getClipProcess() {
		return clipProcess;
	}
	
	private class ClipDisplayProvider implements UserDisplayProvider {

		@Override
		public String getName() {
			return getUnitName() + " display";
		}

		@Override
		public Component getComponent() {
			return generateNewPanel();
		}

		@Override
		public Class getComponentClass() {
			return ClipDisplayPanel.class;
		}
		
	}

	public Component generateNewPanel() {
		ClipDisplayPanel aPanel = new ClipDisplayPanel(this);
		return aPanel.getDisplayPanel();
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#removeUnit()
	 */
	@Override
	public boolean removeUnit() {
		UserDisplayControl.removeDisplayProvider(userDisplayProvider);
		return super.removeUnit();
	}

}
