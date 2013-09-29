package RightWhaleEdgeDetector;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;

/**
 * Exact implementation of the 2003 Right Whale detector I developed when I was 
 * working for IFAW and is now implemented in the Cornell right whale buoys. 
 * This is not by any means the best right whale detector around any more but 
 * has been implemented purely for backward compatible analysis of old data and 
 * and ease of comparing with newer detectrs as they are developed. 
 * @author Doug Gillespie
 *
 */
public class RWEControl extends PamControlledUnit implements PamSettings {

	protected RWEParameters rweParameters = new RWEParameters();
	
	private RWEProcess rweProcess;
	
	public RWEControl(String unitName) {
		super("RW Edge Detector", unitName);
		rweProcess = new RWEProcess(this);
		addPamProcess(rweProcess);
		PamSettingManager.getInstance().registerSettings(this);
	}


	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamControllerInterface.INITIALIZATION_COMPLETE) {
			rweProcess.setupProcesses();
		}
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " ...");
		menuItem.addActionListener(new DetectionSettings(parentFrame));
		return menuItem;
	}

	private class DetectionSettings implements ActionListener {

		Frame frame;

		public DetectionSettings(Frame frame) {
			super();
			this.frame = frame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			settingsDialog(frame);
		}
		
	}

	public void settingsDialog(Frame frame) {
		RWEParameters newParams = RWEDialog.showDialog(frame, this);
		if (newParams != null) {
			this.rweParameters = newParams.clone();
			rweProcess.setupProcesses();
		}
	}


	@Override
	public Serializable getSettingsReference() {
		return rweParameters;
	}


	@Override
	public long getSettingsVersion() {
		return RWEParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		this.rweParameters = ((RWEParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

}
