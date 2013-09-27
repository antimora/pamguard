package alarm;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamDetection.PamDetection;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class AlarmDialog extends PamDialog {

	private static AlarmDialog singleInstance;
	private AlarmParameters alarmParameters;
	
	private SourcePanel sourcePanel;
	
	private JComboBox<String> countType;
	
	private JTextField alarmTime;
	
	private JTextField[] alarmCount = new JTextField[AlarmParameters.COUNT_LEVELS];
	
	private JButton settingsButton;
	
	private AlarmDialog(Window parentFrame, AlarmControl alarmControl) {
		super(parentFrame, alarmControl.getUnitName() + " Settings", false);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		sourcePanel = new SourcePanel(this, PamDataUnit.class, false, true);
		sourcePanel.addSelectionListener(new SourceSelection());
		JPanel northPanel = new JPanel(new GridBagLayout());
		northPanel.setBorder(new TitledBorder("Trigger source"));
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 2;
		addComponent(northPanel, sourcePanel.getPanel(), c);
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridy++;
		c.gridx = 1;
		addComponent(northPanel, settingsButton = new JButton("Settings..."), c);
		settingsButton.addActionListener(new SettingsButton());
		
		mainPanel.add(BorderLayout.NORTH, northPanel);
		
		JPanel southPanel = new JPanel(new GridBagLayout());
		southPanel.setBorder(new TitledBorder("Alarm Count"));
		c = new PamGridBagContraints();
		addComponent(southPanel, new JLabel("Count Type ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 2;
		addComponent(southPanel, countType = new JComboBox<String>(), c);
		countType.addItem("Simple Counts");
		countType.addItem("Scored Values");
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy++;
		addComponent(southPanel, new JLabel("Count Time (s) ", JLabel.RIGHT), c);
		c.gridx++;
		addComponent(southPanel, alarmTime = new JTextField(5), c);
		for (int i = 0; i < AlarmParameters.COUNT_LEVELS; i++) {
			c.gridx = 0;
			c.gridy++;
			addComponent(southPanel, new JLabel(AlarmParameters.levelNames[i] + " Count ", JLabel.RIGHT), c);
			c.gridx++;
			addComponent(southPanel, alarmCount[i] = new JTextField(5), c);
		}

		mainPanel.add(BorderLayout.SOUTH, southPanel);
		
		
		setDialogComponent(mainPanel);
		
	}

	public static final AlarmParameters showDialog(Window parentFrame, AlarmControl alarmControl) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
			singleInstance = new AlarmDialog(parentFrame, alarmControl);
		}
		singleInstance.alarmParameters = alarmControl.alarmParameters;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.alarmParameters;
	}
	
	private void setParams() {
		sourcePanel.setSourceList();
		sourcePanel.setSource(alarmParameters.dataSourceName);
		countType.setSelectedIndex(alarmParameters.countType);
		alarmTime.setText(String.format("%3.1f", (double) alarmParameters.countIntervalMillis / 1000.));
		for (int i = 0; i < AlarmParameters.COUNT_LEVELS; i++) {
			alarmCount[i].setText(String.format("%3.1f", alarmParameters.getTriggerCount(i)));
		}
		enableControls();
	}

	@Override
	public void cancelButtonPressed() {
		alarmParameters = null;
	}

	@Override
	public boolean getParams() {
		alarmParameters.dataSourceName = sourcePanel.getSourceName();
		if (alarmParameters.dataSourceName == null) {
			return showWarning("No data source selected");
		}
		try {
			alarmParameters.countType = countType.getSelectedIndex();
			alarmParameters.countIntervalMillis = (long) (Double.valueOf(alarmTime.getText()) * 1000.);
			for (int i = 0; i < AlarmParameters.COUNT_LEVELS; i++) {
				alarmParameters.setTriggerCount(i, Double.valueOf(alarmCount[i].getText()));
			}
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid number");
		}
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}
	
	private void enableControls() {
		AlarmCounter ac = findAlarmCounter();
		if (ac == null || ac.hasOptions() == false) {
			settingsButton.setEnabled(false);
		}
		else {
			settingsButton.setEnabled(true);
		}
	}

	private class SettingsButton implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			AlarmCounter ac = findAlarmCounter();
			if (ac != null) {
				ac.showOptions(getOwner());
			}
		}
		
	}
	
	class SourceSelection implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			enableControls();
		}
		
	}
	
	private AlarmCounter findAlarmCounter() {
		PamDataBlock alarmSource = sourcePanel.getSource();
		if (alarmSource == null) {
			return null;
		}
		if (AlarmDataSource.class.isAssignableFrom(alarmSource.getClass())) {
			return ((AlarmDataSource) alarmSource).getAlarmCounter();
		}
		return null;
		
	}
}
