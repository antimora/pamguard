package soundPlayback;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;

public class FilePlaybackDialogComponent extends PlaybackDialogComponent {

	private JPanel panel;
	
	private PlaybackControl playbackControl;
	
	private JComboBox deviceTypes;
	
	private JComboBox soundCards;
	
	private JCheckBox defaultRate;
	
	private JTextField sampleRate;

	private FilePlayback filePlayback;

	private PlaybackParameters playbackParameters;
	
	/**
	 * Dialog component for sound playback when input is from a file. 
	 * <p>
	 * Have now implemented a system whereby playback can be over 
	 * a number of device types. For now this will be sound cards and NI 
	 * cards so that we can generate real audio data at V high frequency
	 * for some real time testing. 
	 * <p>
	 * Playback from file is easy since there is no need to synchronise dound input with 
	 * sound output. 
	 * @param playbackControl
	 */
	public FilePlaybackDialogComponent(FilePlayback filePlayback) {
		this.filePlayback = filePlayback;
		this.playbackControl = filePlayback.getPlaybackControl();
		
		panel = new JPanel();
		panel.setBorder(new TitledBorder("Options"));
		deviceTypes = new JComboBox();
		deviceTypes.addActionListener(new NewDeviceType());
		soundCards = new JComboBox();
		soundCards.addActionListener(new NewSoundCard());

		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		PamDialog.addComponent(panel, new JLabel("Output device type ..."), c);
		c.gridy++;
		PamDialog.addComponent(panel, deviceTypes, c);
		c.gridy++;
		PamDialog.addComponent(panel, new JLabel("Output device name ..."), c);
		c.gridy++;
		PamDialog.addComponent(panel, soundCards, c);

		c.gridy++;
		PamDialog.addComponent(panel, defaultRate = new JCheckBox("Use default playback rate"), c);
		c.gridwidth = 1;
		c.gridy++;
		PamDialog.addComponent(panel, new JLabel("Playback rate "), c);
		c.gridx++;
		PamDialog.addComponent(panel, sampleRate = new JTextField(9), c);
		c.gridx++;
		PamDialog.addComponent(panel, new JLabel(" Hz"), c);
		
		defaultRate.addActionListener(new DefSampleRateAction());
		
	}

	class DefSampleRateAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {

//			if (defaultRate.isSelected()) {
				saySampleRate();
//			}
			
		}
		
	}
	
	
	
	@Override
	Component getComponent() {
		return panel;
	}

	@Override
	PlaybackParameters getParams(PlaybackParameters playbackParameters) {
		playbackParameters.deviceType = deviceTypes.getSelectedIndex();
		playbackParameters.deviceNumber = soundCards.getSelectedIndex();
		playbackParameters.defaultSampleRate = defaultRate.isSelected();
		try {
			playbackParameters.playbackRate = Float.valueOf(sampleRate.getText());
		}
		catch (NumberFormatException ex) {
			return null;
		}
		return playbackParameters;
	}

	@Override
	void setParams(PlaybackParameters playbackParameters) {
		this.playbackParameters = playbackParameters;
		deviceTypes.removeAllItems();
		for (int i = 0; i < filePlayback.filePBDevices.size(); i++) {
			deviceTypes.addItem(filePlayback.filePBDevices.get(i).getName());
		}
		deviceTypes.setSelectedIndex(playbackParameters.deviceType);
//		
//		soundCards.removeAllItems();
//		ArrayList<Info> mixers = SoundCardSystem.getOutputMixerList();
//		for (int i = 0; i < mixers.size(); i++) {
//			soundCards.addItem(mixers.get(i).getName());
//		}
//		soundCards.setSelectedIndex(playbackParameters.deviceNumber);
		
		defaultRate.setSelected(playbackParameters.defaultSampleRate);
		
		saySampleRate();
		
		fillDeviceSpecificList();
	}
	
	private class NewDeviceType implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fillDeviceSpecificList();
		}

	}

	private class NewSoundCard implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (filePlayback != null) {
				filePlayback.notifyObservers();
			}
			
		}
	}
	
	public void fillDeviceSpecificList() {
		int deviceType = deviceTypes.getSelectedIndex();
		deviceType = Math.max(0, deviceType);
		FilePlaybackDevice selectedDeviceType = filePlayback.filePBDevices.get(deviceType);
		soundCards.removeAllItems();
		String[] devList = selectedDeviceType.getDeviceNames();
		for (int i = 0; i < devList.length; i++) {
			soundCards.addItem(devList[i]);
		}
		if (playbackParameters.deviceNumber < devList.length) {
			soundCards.setSelectedIndex(playbackParameters.deviceNumber);
		}
	}
	
	void saySampleRate() {
		float playbackRate;
		try {
			playbackRate = Float.valueOf(sampleRate.getText());
		}
		catch (NumberFormatException ex) {
			playbackRate = 0;
		}
		
		playbackRate = playbackControl.playbackParameters.playbackRate;
		
		if (defaultRate.isSelected() || playbackRate == 0) {
			playbackRate = playbackControl.playbackProcess.getSampleRate();
		}
		
		sampleRate.setText(String.format("%.0f", playbackRate));
		
		sampleRate.setEnabled(defaultRate.isSelected() == false);
	}
	
	
	
	
	
}
