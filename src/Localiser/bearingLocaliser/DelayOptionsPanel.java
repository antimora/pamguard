package Localiser.bearingLocaliser;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import Localiser.DelayMeasurementParams;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

import fftFilter.FFTFilterDialog;
import fftFilter.FFTFilterParams;

public class DelayOptionsPanel {

	private JCheckBox filterBearings, envelopeBearings;
	private JButton filterSettings;
	private JLabel filterDescription;
	
	private DelayMeasurementParams delayMeasurementParams;
	
	private Window owner;
	
	private JPanel panel;
	
	/**
	 * @param owner
	 */
	public DelayOptionsPanel(Window owner) {
		super();
		this.owner = owner;
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("Delay measurement options"));
		GridBagConstraints c = new PamGridBagContraints();
		PamDialog.addComponent(mainPanel, filterBearings = new JCheckBox("Filter data before measurement"),  c);
		filterBearings.addActionListener(new FilterBearings());
		c.gridx++;
		PamDialog.addComponent(mainPanel, filterSettings = new JButton("Settings"), c);
		filterSettings.addActionListener(new FilterSettings());
		c.gridx = 0;
		c.gridy++;
		PamDialog.addComponent(mainPanel, filterDescription = new JLabel(" ", SwingConstants.CENTER), c);
		c.gridx = 0;
		c.gridy++;
		PamDialog.addComponent(mainPanel, envelopeBearings = new JCheckBox("Use waveform envelope"),  c);
		
		filterBearings.setToolTipText("Filter data prior to bearing measurement to imporve accuracy");
		filterSettings.setToolTipText("Setup filter options");
		envelopeBearings.setToolTipText("Using the envelope can provide more accurate bearings for some narrowbanc pulses");
		
		panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH, mainPanel);
	}

	/**
	 * @return the mainPanel
	 */
	public JPanel getMainPanel() {
		return panel;
	}

	private void enableControls() {
		filterSettings.setEnabled(filterBearings.isSelected());
		filterDescription.setEnabled(filterBearings.isSelected());
	}
	
	private void describeFilter() {
		if (delayMeasurementParams == null || delayMeasurementParams.delayFilterParams == null) {
			filterDescription.setText("No filter");
			return;
		}
		filterDescription.setText(delayMeasurementParams.delayFilterParams.toString());
	}
	
	public void setParams(DelayMeasurementParams delayMeasurementParams) {
		this.delayMeasurementParams = delayMeasurementParams;
		filterBearings.setSelected(delayMeasurementParams.filterBearings);
		envelopeBearings.setSelected(delayMeasurementParams.envelopeBearings);
		enableControls();
		describeFilter();
	}
	public boolean getParams(DelayMeasurementParams delayMeasurementParams) {
		delayMeasurementParams.delayFilterParams = this.delayMeasurementParams.delayFilterParams;
		delayMeasurementParams.filterBearings = filterBearings.isSelected();
		delayMeasurementParams.envelopeBearings = envelopeBearings.isSelected();
		if (delayMeasurementParams.filterBearings && delayMeasurementParams.delayFilterParams == null) {
			return PamDialog.showWarning(owner, "Delay measurement settings", "Filter parameters have not been set");
		}
		return true;
	}


	private class FilterBearings implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			enableControls();
		}
	}

	private class FilterSettings implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			FFTFilterParams newParams = FFTFilterDialog.showDialog(owner, delayMeasurementParams.delayFilterParams);
			if (newParams != null) {
				delayMeasurementParams.delayFilterParams = newParams.clone();
				describeFilter();
			}
		}
	}
	
	
}
