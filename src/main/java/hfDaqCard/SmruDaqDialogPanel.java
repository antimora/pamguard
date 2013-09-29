package hfDaqCard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import Acquisition.AcquisitionDialog;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class SmruDaqDialogPanel {

	private JPanel dialogPanel;
	
	private SmruDaqSystem smruDaqSystem;
	
	JComboBox sampleRate;
	JCheckBox[] channelEnable = new JCheckBox[SmruDaqParameters.NCHANNELS];
	JComboBox[] gains = new JComboBox[SmruDaqParameters.NCHANNELS];
	JComboBox[] filters = new JComboBox[SmruDaqParameters.NCHANNELS];

	private SmruDaqParameters smruDaqParameters;

	private AcquisitionDialog acquisitionDialog;
	
	private JButton[] toggles = new JButton[2];

	/**
	 * @param smruDaqSystem
	 */
	public SmruDaqDialogPanel(SmruDaqSystem smruDaqSystem) {
		super();
		
		this.smruDaqSystem = smruDaqSystem;
		dialogPanel = new JPanel(new GridBagLayout());
		dialogPanel.setBorder(new TitledBorder("Daq card configuration"));
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 3;
		PamDialog.addComponent(dialogPanel,new JLabel("Sample Rate ", SwingConstants.RIGHT),c);
		c.gridx += c.gridwidth;
		c.gridwidth = 3;
		PamDialog.addComponent(dialogPanel, sampleRate = new JComboBox(), c);
		sampleRate.addActionListener(new SampleRate());
		c.gridx += c.gridwidth;
		c.gridwidth = 1;
//		PamDialog.addComponent(dialogPanel,new JLabel(" Hz ", JLabel.LEFT),c);
		for (int i = 0; i < SmruDaqParameters.sampleRates.length; i++) {
			sampleRate.addItem(String.format("%3.1f kHz", SmruDaqParameters.sampleRates[i]/1000.));
		}
		
		for (int i = 0; i < SmruDaqParameters.NCHANNELS; i++) {
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = 1;
			PamDialog.addComponent(dialogPanel,new JLabel("Enable ", SwingConstants.RIGHT),c);
			c.gridx += 1;
			PamDialog.addComponent(dialogPanel, channelEnable[i] = new JCheckBox(), c);
			c.gridx ++;
			PamDialog.addComponent(dialogPanel,new JLabel(", gain ", SwingConstants.RIGHT),c);
			c.gridx ++;
			PamDialog.addComponent(dialogPanel, gains[i] = new JComboBox(), c);
			c.gridx ++;
			PamDialog.addComponent(dialogPanel,new JLabel(" dB, HP filter ", SwingConstants.LEFT),c);
			c.gridx ++;
			PamDialog.addComponent(dialogPanel, filters[i] = new JComboBox(), c);
			c.gridx ++;
			PamDialog.addComponent(dialogPanel,new JLabel(" Hz ", SwingConstants.LEFT),c);
			
			double[] daqGains = SmruDaqParameters.getGains();
			channelEnable[i].addActionListener(new ChannelEnable());
			for (int j = 0; j < daqGains.length; j++) {
				if (Double.isInfinite(daqGains[j])) {
					gains[i].addItem("Off");
				}
				else {
					gains[i].addItem(String.format("%3.1f", daqGains[j]));
				}
			}
			for (int j = 0; j < SmruDaqParameters.filters.length; j++) {
				filters[i].addItem(String.format("%3.1f", SmruDaqParameters.filters[j]));
			}
			
		}
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		for (int i = 0; i < 2; i++) {
			PamDialog.addComponent(dialogPanel, toggles[i] = new JButton("Toggle LED "+i), c);
			toggles[i].addActionListener(new Toggle(i));
			c.gridx += c.gridwidth;
		}
	}
	
	public SmruDaqParameters getParams() {
		smruDaqParameters.setSampleRateIndex(sampleRate.getSelectedIndex());
		smruDaqParameters.channelMask = 0;
		for (int i = 0; i < SmruDaqParameters.NCHANNELS; i++) {
			if (channelEnable[i].isSelected()) {
				smruDaqParameters.channelMask += 1<<i;
			}
			smruDaqParameters.setGainIndex(i, gains[i].getSelectedIndex());
			smruDaqParameters.setFilterIndex(i, filters[i].getSelectedIndex());
		}
		return smruDaqParameters;
	}

	public void setParams(SmruDaqParameters smruDaqParameters) {
		this.smruDaqParameters = smruDaqParameters.clone();
		sampleRate.setSelectedIndex(smruDaqParameters.getSampleRateIndex());
		for (int i = 0; i < SmruDaqParameters.NCHANNELS; i++) {
			channelEnable[i].setSelected((smruDaqParameters.channelMask & 1<<i) != 0);
			gains[i].setSelectedIndex(smruDaqParameters.getGainIndex(i));
			filters[i].setSelectedIndex(smruDaqParameters.getFilterIndex(i));
		}
		
		enableControls();
	}
	
	class ChannelEnable implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			enableControls();
		}
	}

	public void enableControls() {
		boolean b;
		int enabledChannels = 0;
		for (int i = 0; i < SmruDaqParameters.NCHANNELS; i++) {
			b = channelEnable[i].isSelected();
			gains[i].setEnabled(b);
			filters[i].setEnabled(b);
			if (b) {
				enabledChannels++;
			}
		}
		if (acquisitionDialog != null) {
			acquisitionDialog.setChannels(enabledChannels);
		}
	}
	
	class SampleRate implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			saySampleRate();
		}
	}
	
	class Toggle implements ActionListener {

		int led;
		public Toggle(int led) {
			super();
			this.led = led;
			setText();
		}
		public void actionPerformed(ActionEvent e) {
			smruDaqSystem.toggleLED(led);
			setText();
		}
		public void setText() {
			int status = smruDaqSystem.getLED(led);
			String text = String.format("Toggle LED %d", led);
			switch(status) {
			case 0:
				text = String.format("Turn LED %d ON", led);
				break;
			case 1:
				text = String.format("Turn LED %d OFF", led);
				break;
			}
			toggles[led].setText(text);
		}
	}
	

	public void saySampleRate() {
		if (acquisitionDialog != null) {
			int sr = SmruDaqParameters.sampleRates[sampleRate.getSelectedIndex()];
			acquisitionDialog.setSampleRate(sr);
			
			// while here, set the V p-p
			acquisitionDialog.setVPeak2Peak(SmruDaqParameters.VPEAKTOPEAK);
		}
	}

	/**
	 * @return the dialogPanel
	 */
	public JPanel getDialogPanel() {
		return dialogPanel;
	}

	public void setDaqDialog(AcquisitionDialog acquisitionDialog) {
		this.acquisitionDialog = acquisitionDialog;
	}
	
	
}
