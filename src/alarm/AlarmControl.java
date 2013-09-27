package alarm;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamSidePanel;

public class AlarmControl extends PamControlledUnit implements PamSettings {

	private AlarmProcess alarmProcess;
	
	private AlarmSidePanel alarmSidePanel;
	
	protected AlarmParameters alarmParameters = new AlarmParameters();
	
	private int alarmStatus;
	
	/**
	 * @return the alarmStatus
	 */
	public int getAlarmStatus() {
		return alarmStatus;
	}

	/**
	 * @param alarmStatus the alarmStatus to set
	 */
	public void setAlarmStatus(int alarmStatus) {
		this.alarmStatus = alarmStatus;
	}

	public AlarmControl(String unitName) {
		super("Alarm", unitName);
		addPamProcess(alarmProcess = new AlarmProcess(this));
		alarmSidePanel = new AlarmSidePanel(this);
		PamSettingManager.getInstance().registerSettings(this);
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#getSidePanel()
	 */
	@Override
	public PamSidePanel getSidePanel() {
		return alarmSidePanel;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName());
		menuItem.addActionListener(new AlarmMenu(parentFrame));
		return menuItem;
	}

	private class AlarmMenu implements ActionListener {

		private Frame parentFrame;
		
		public AlarmMenu(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			showAlarmDialog(parentFrame);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamController.INITIALIZATION_COMPLETE:
			setupAlarm();
			break;
		}
	}

	private boolean showAlarmDialog(Frame parentFrame) {
		AlarmParameters newParams = AlarmDialog.showDialog(parentFrame, this);
		if (newParams != null) {
			alarmParameters = newParams.clone();
			setupAlarm();
			return true;
		}
		else {
			return false;
		}
	}

	private void setupAlarm() {
		alarmProcess.setupAlarm();
	}

	/**
	 * Received an updated alarm score. 
	 * @param alarmCount
	 */
	public void updateAlarmScore(double alarmCount) {
		int alarmState = 0;
		for (int i = 0; i < AlarmParameters.COUNT_LEVELS; i++) {
			if (alarmCount < alarmParameters.getTriggerCount(i)) {
				break;
			}
			alarmState++;
		}
		setAlarmStatus(alarmState);
		alarmSidePanel.updateAlarmScore(alarmCount);
	}

	@Override
	public Serializable getSettingsReference() {
		return alarmParameters;
	}

	@Override
	public long getSettingsVersion() {
		return AlarmParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		alarmParameters = ((AlarmParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}
}
