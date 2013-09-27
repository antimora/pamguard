package fftFilter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import Filters.FilterBand;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class FFTFilterDialog extends PamDialog {

	private static FFTFilterDialog singleInstance = null;

	private FFTFilterParams fftFilterParams;

	//	private JComboBox filterBands;
	private JRadioButton highPass;

	private JRadioButton bandPass;

	private JRadioButton lowPass;

	private JRadioButton bandStop;

	private JTextField lowPassFreq;

	private JTextField highPassFreq;

	private FFTFilterDialog(Window parentFrame) {
		super(parentFrame, "FFT Filter Settings", false);
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 3;
		addComponent(p, highPass = new JRadioButton("High Pass"),  c);
		c.gridy++;
		addComponent(p, lowPass = new JRadioButton("Low Pass"),  c);
		c.gridy++;
		addComponent(p, bandPass = new JRadioButton("Band Pass"),  c);
		c.gridy++;
		addComponent(p, bandStop = new JRadioButton("Band Stop"),  c);
		c.gridy++;
		c.gridwidth = 1;
		c.gridy++;
		c.gridx = 0;
		addComponent(p, new JLabel("High pass cut off frequency "), c);
		c.gridx++;
		addComponent(p, highPassFreq = new JTextField(6), c);
		c.gridx++;
		addComponent(p, new JLabel(" Hz"), c);
		c.gridy++;
		c.gridx = 0;
		addComponent(p, new JLabel("Low pass cut off frequency "), c);
		c.gridx++;
		addComponent(p, lowPassFreq = new JTextField(6), c);
		c.gridx++;
		addComponent(p, new JLabel(" Hz"), c);

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(highPass);
		buttonGroup.add(lowPass);
		buttonGroup.add(bandPass);
		buttonGroup.add(bandStop);
		BandChanged bc = new BandChanged();
		highPass.addActionListener(bc);
		lowPass.addActionListener(bc);
		bandPass.addActionListener(bc);
		bandStop.addActionListener(bc);


		setDialogComponent(p);
	}

	@Override
	public void cancelButtonPressed() {
		fftFilterParams = null;
	}

	private  void setParams() {
		highPass.setSelected(fftFilterParams.filterBand == FilterBand.HIGHPASS);
		lowPass.setSelected(fftFilterParams.filterBand == FilterBand.LOWPASS);
		bandPass.setSelected(fftFilterParams.filterBand == FilterBand.BANDPASS);
		bandStop.setSelected(fftFilterParams.filterBand == FilterBand.BANDSTOP);
		highPassFreq.setText(String.format("%3.1f", fftFilterParams.highPassFreq));
		lowPassFreq.setText(String.format("%3.1f", fftFilterParams.lowPassFreq));
		enableControls();
	}

	@Override
	public boolean getParams() {
		fftFilterParams.filterBand = getBand();
		try {
			if (fftFilterParams.filterBand != FilterBand.HIGHPASS) {
				fftFilterParams.lowPassFreq = Double.valueOf(lowPassFreq.getText());
			}
			if (fftFilterParams.filterBand != FilterBand.LOWPASS) {
				fftFilterParams.highPassFreq = Double.valueOf(highPassFreq.getText());
			}
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid frequency parameter");
		}
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}
	private FilterBand getBand() {
		if (highPass.isSelected()) return FilterBand.HIGHPASS;
		if (lowPass.isSelected()) return FilterBand.LOWPASS;
		if (bandPass.isSelected()) return FilterBand.BANDPASS;
		if (bandStop.isSelected()) return FilterBand.BANDSTOP;
		return null;
	}
	private void enableControls() {
		FilterBand b = getBand();
		highPassFreq.setEnabled(b != FilterBand.LOWPASS);
		lowPassFreq.setEnabled(b != FilterBand.HIGHPASS);
	}

	public static FFTFilterParams showDialog(Window owner,
			FFTFilterParams fftFilterParams) {
		if (singleInstance == null || singleInstance.getOwner() != owner) {
			singleInstance = new FFTFilterDialog(owner);
		}
		if (fftFilterParams == null) {
			singleInstance.fftFilterParams = new FFTFilterParams();
		}
		else {
			singleInstance.fftFilterParams = fftFilterParams.clone();
		}
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.fftFilterParams;
	}

	class BandChanged implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			enableControls();
		}

	}
}
