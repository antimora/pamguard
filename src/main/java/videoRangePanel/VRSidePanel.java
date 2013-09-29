package videoRangePanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import PamView.PamBorderPanel;
import PamView.PamColors;
import PamView.PamLabel;
import PamView.PamSidePanel;
import PamView.PamSlider;

public class VRSidePanel implements PamSidePanel {

	private VRControl vrControl;

	private VRPanel vrPanel;
	
	private PamLabel statusText, instruction, mouseLabel, imageNameLabel;
	
	private PamLabel calibrationData;
	private JComboBox calibrations, heights;

	public VRSidePanel(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
		vrPanel = new VRPanel();
		setStatus();
	}

	public JComponent getPanel() {
		return vrPanel;
	}

	public void rename(String newName) {
		vrPanel.panelBorder.setTitle(newName);

	}
	
	public void setStatus() {
		int vrStatus = vrControl.getVrStatus();
		switch (vrStatus) {
		case VRControl.NOIMAGE:
			statusText.setText("No Image");
			break;
		case VRControl.MEASURE_FROM_HORIZON:
			statusText.setText("Measuring from Horizon");
			break;
		case VRControl.MEASURE_FROM_SHORE:
			statusText.setText("Measuring from Shore");
			break;
		case VRControl.CALIBRATE:
			statusText.setText("Calibrating");
			break;
		}
		setImageName();
		setInstruction();
		vrPanel.enableControls();
		vrPanel.selectControls();
	}
	
	public void setInstruction() {
		instruction.setText(vrControl.getInstruction());
	}
	
	void setImageName() {
		String imageName = vrControl.getImageName();
		if (imageName == null) {
			imageNameLabel.setText("No Image");
		}
		else {
			imageNameLabel.setText(imageName);
		}
	}

	void newSettings() {
		vrPanel.scaleStyle.setSelectedIndex(vrControl.vrParameters.imageScaling);
		ArrayList<VRCalibrationData> calData = vrControl.vrParameters.getCalibrationDatas();
		VRCalibrationData currentSelection = vrControl.vrParameters.getCurrentCalibrationData();
		int currIndex = 0;
		calibrations.removeAllItems();
		if (calData != null) {
			for (int i = 0; i < calData.size(); i++) {
				calibrations.addItem(calData.get(i));
				if (calData.get(i) == currentSelection) {
					currIndex = i;
				}
			}
			if (currIndex >= 0) {
				calibrations.setSelectedIndex(currIndex);
			}
		}
		vrControl.vrParameters.setCurrentCalibration(currentSelection);
		
		VRHeightData currentheight = vrControl.vrParameters.getCurrentheightData();
		heights.removeAllItems();
		ArrayList<VRHeightData> heightDatas = vrControl.vrParameters.getHeightDatas();
		for (int i = 0; i < heightDatas.size(); i++) {
			heights.addItem(heightDatas.get(i));
		}
		if (currentheight != null) {
			heights.setSelectedItem(currentheight);
		}
	}
	
	void newCalibration(boolean rebuildList) {
		VRCalibrationData vcd = vrControl.vrParameters.getCurrentCalibrationData();
		if (vcd == null) return;
		calibrations.setSelectedItem(vcd);
		calibrationData.setText(String.format("%.5f\u00B0/px", vcd.degreesPerUnit));
		vrPanel.enableControls();
	}
	
	void newMousePoint(Point mousePoint) {
		if (mousePoint == null) {
			mouseLabel.setText(" ");
		}
		else {
			mouseLabel.setText(String.format("Cursor: (%d,%d)", mousePoint.x, mousePoint.y));
		}
	}


	class VRPanel extends PamBorderPanel {
		JButton pasteButton, fileButton, settingsButton, clearHorizon, clearShorePoint;
//		JRadioButton measureButton, calButton, measureToShoreButton;
		JComboBox scaleStyle;
		JSlider brightness, contrast;
		JSpinner spinBrightness, spinContrast;
		TitledBorder panelBorder;
		JComboBox measurementType;
		
		VRPanel() {
			this.setLayout(new BorderLayout());
			
			PamLabel lab;
			
			JPanel mp = new PamBorderPanel();
			this.setBorder(panelBorder = new TitledBorder(vrControl.getUnitName()));
			mp.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			
			addComponent(mp, lab = new PamLabel("Image"), c);
			lab.setFont(PamColors.getInstance().getBoldFont());
			c.gridx=0;
			c.gridy++;
			addComponent(mp, pasteButton = new JButton("Paste"), c);
			c.gridx++;
			addComponent(mp, fileButton = new JButton("File ..."), c);
			c.gridx=0;
			c.gridy++;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			addComponent(mp, new PamLabel("Scrolling  "), c);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx++;
			addComponent(mp, scaleStyle = new JComboBox(), c);
			c.gridy++;
			c.gridx=0;
			c.gridwidth = 2;
			addComponent(mp, imageNameLabel = new PamLabel(), c);
//			imageNameLabel.setFont(PamColors.getInstance().getBoldFont());
			c.gridy++;
			c.gridx=0;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.CENTER;
			addComponent(mp, lab = new PamLabel("Brightness"), c);
			c.gridx++;
			addComponent(mp, new PamLabel("Contrast"), c);
			c.gridy++;
			c.gridx=0;
			c.fill = GridBagConstraints.HORIZONTAL;
			// sli
			JPanel bc = new PamBorderPanel();
			bc.setLayout(new BoxLayout(bc, BoxLayout.X_AXIS));
			brightness = new PamSlider(SwingConstants.HORIZONTAL, -20, 20, 0);
			Dimension d = brightness.getPreferredSize();
			d.width = 10;
			brightness.setPreferredSize(d);
//			PamColors.getInstance().registerComponent(brightness, PamColor.BORDER);
			brightness.addChangeListener(new BrightnessChange());
			contrast = new PamSlider(SwingConstants.HORIZONTAL, -20, 20, 0);
			contrast.setPreferredSize(d);
//			PamColors.getInstance().registerComponent(contrast, PamColor.BORDER);
			contrast.addChangeListener(new BrightnessChange());
			bc.add(brightness);
			bc.add(contrast);
//			this.add(BorderLayout.SOUTH, bc);
			c.gridy++;
			c.gridx=0;
			c.gridwidth = 2;
			addComponent(mp, bc, c);

			c.gridy++;
			c.gridx=0;
			c.gridwidth = 1;
			addComponent(mp, lab = new PamLabel("Calibration"), c);
			lab.setFont(PamColors.getInstance().getBoldFont());
			c.gridx++;
//			c.gridx=0;
//			c.gridwidth = 2;
			addComponent(mp, calibrationData = new PamLabel(""), c);
//			calibrationData.setFont(PamColors.getInstance().getBoldFont());
			c.gridy++;
			c.gridx=0;
			c.gridwidth = 2;
			addComponent(mp, calibrations = new JComboBox(), c);
			c.gridy++;
			c.gridx = 0;
			addComponent(mp, new JLabel("Height"), c);
			c.gridy++;
			c.gridwidth = 2;
			addComponent(mp, heights = new JComboBox(), c);
			
			
			c.gridy++;
			c.gridx=0;
			c.gridwidth = 2;
			addComponent(mp, lab = new PamLabel("Analysis"), c);
			lab.setFont(PamColors.getInstance().getBoldFont());
			c.gridy++;
			c.gridx=0;
			addComponent(mp, measurementType = new JComboBox(), c);
			measurementType.addItem("Measure from horizon");
			measurementType.addItem("Measure from shore");
			measurementType.addItem("Calibrate");
			measurementType.addActionListener(new MeasurementType());
			
			c.gridwidth = 2;
			statusText = new PamLabel();
//			addComponent(mp, statusText, c);
			statusText.setFont(PamColors.getInstance().getBoldFont());
			c.gridy++;
			
			addComponent(mp, instruction = new PamLabel(), c);
			instruction.setFont(PamColors.getInstance().getBoldFont());
			c.gridy++;
			
			
			c.gridx++;
			c.gridy++;
			c.gridx =0;
			c.gridwidth = 2;
//			addComponent(mp, measureButton = new PamRadioButton("Measure from Horizon"), c);
//			measureButton.setSelected(true);
//			c.gridy++;
//			addComponent(mp, measureToShoreButton = new PamRadioButton("Measure from Shore"), c);
////			pasteButton.setSelected(true);
//			c.gridy++;
//			addComponent(mp, calButton = new PamRadioButton("Calibrate Camera"), c);
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 2;
			addComponent(mp, clearHorizon = new JButton("Clear Horizon"), c);
			c.gridy++;
			addComponent(mp, clearShorePoint = new JButton("Clear Shore Point"), c);
			c.gridy++;
			c.gridx = 0;
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 2;
			addComponent(mp, mouseLabel = new PamLabel(" "), c);

			c.gridx=0;
			c.gridy++;
			c.gridwidth = 2;
			addComponent(mp, settingsButton = new JButton("Settings"), c);
						
			settingsButton.addActionListener(new SettingsButton());
			pasteButton.addActionListener(new PasteButton());
			fileButton.addActionListener(new FileButton());
//			measureButton.addActionListener(new MeasureButton());
//			calButton.addActionListener(new CalibrateButton());
			clearHorizon.addActionListener(new ClearHorizon());
			clearShorePoint.addActionListener(new ClearShorePoint());
			calibrations.addActionListener(new SelectCalibration());
			heights.addActionListener(new SelectHeight());
			
			ButtonGroup bg = new ButtonGroup();
//			bg.add(measureButton);
//			bg.add(calButton);
			
			for (int i = 0; i < VRParameters.shortScaleNames.length; i++) {
				scaleStyle.addItem(VRParameters.shortScaleNames[i]);
			}
			scaleStyle.addActionListener(new ScaleAction());
					
			
			
			enableControls();
			
			this.add(BorderLayout.CENTER, mp);
			
		}
		
		void enableControls() {
			boolean hasHeight = (vrControl.vrParameters.getCameraHeight() > 0);
			int status = vrControl.getVrStatus();
			if (vrControl.vrParameters.getCurrentCalibrationData() == null) {
				measurementType.setSelectedIndex(VRControl.CALIBRATE);
				measurementType.setEnabled(false);
			}
			else {
				measurementType.setEnabled(hasHeight && status != VRControl.NOIMAGE);
			}
//			measureButton.setEnabled(hasHeight && status != VRControl.NOIMAGE && vrControl.vrParameters.getCurrentCalibrationData() != null);
//			calButton.setEnabled(hasHeight && status != VRControl.NOIMAGE);
//			scaleStyle.setEnabled(hasHeight && status != VRControl.NOIMAGE);
			clearHorizon.setVisible(status != VRControl.MEASURE_FROM_SHORE);
			clearHorizon.setEnabled(hasHeight && vrControl.getVrSubStatus() != VRControl.MEASURE_HORIZON_1);
			clearShorePoint.setVisible(status == VRControl.MEASURE_FROM_SHORE);
			clearShorePoint.setEnabled(hasHeight && vrControl.getVrSubStatus() != VRControl.MEASURE_SHORE);
			fileButton.setEnabled(hasHeight);
			pasteButton.setEnabled(hasHeight);
			
		}
		void selectControls() {
			int status = vrControl.getVrStatus();
			measurementType.setSelectedIndex(status);
//			measureButton.setSelected(status == VRControl.MEASURE_TO_HORIZON);
//			calButton.setSelected(status == VRControl.CALIBRATE);
		}

		
		class PasteButton implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				vrControl.pasteButton();
			}
			
		}

		class SettingsButton implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				vrControl.settingsButton(null);
			}
			
		}
		class FileButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				vrControl.fileButton();
			}
		}
//		class MeasureButton implements ActionListener {
//			public void actionPerformed(ActionEvent e) {
//				vrControl.measureButton();
//			}
//		}
//		class CalibrateButton implements ActionListener {
//			public void actionPerformed(ActionEvent e) {
//				vrControl.calibrateButton();
//			}
//		}
		class MeasurementType implements ActionListener {
			private int lastSelection = -1;
			public void actionPerformed(ActionEvent e) {
				if (lastSelection != measurementType.getSelectedIndex()) {
					lastSelection = measurementType.getSelectedIndex();
					vrControl.setMeasurementType(lastSelection);
				}
			}
		}
		class ScaleAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				vrControl.vrParameters.imageScaling = scaleStyle.getSelectedIndex();
				vrControl.newSettings();
			}
			
		}
		class ClearHorizon implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				vrControl.clearHorizon();
			}
		}
		class ClearShorePoint implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				vrControl.clearShorePoint();
			}
		}
		class SelectCalibration implements ActionListener {
			public void actionPerformed(ActionEvent e) {
//				System.out.println(e.getActionCommand());
//				VRCalibrationData selectedCalibration = (VRCalibrationData) calibrations.getSelectedItem();
//				vrControl.vrParameters.setCurrentCalibration(selectedCalibration);
				vrControl.selectCalibration((VRCalibrationData) calibrations.getSelectedItem());
			}
		}

		class SelectHeight implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				vrControl.selectHeight(heights.getSelectedIndex());
//				vrControl.vrParameters.setCurrentHeightIndex(heights.getSelectedIndex());
			}
		}
		
		class BrightnessChange implements ChangeListener {
			public void stateChanged(ChangeEvent e) {
				// brightness value must be between 0 and 2
				float brightVal = 2 * (float) (brightness.getValue() - brightness.getMinimum()) / 
				((float) brightness.getMaximum() - (float) brightness.getMinimum());
				
				// contrast value must be between 0 and 255 (log scale ? )
				// first cal on a scale of -1 to + 1.
				
				// first get a number between 0 and 8.
				float contVal = (float) 8 * (contrast.getValue() - contrast.getMinimum()) / 
				((float) contrast.getMaximum() - (float) contrast.getMinimum());
				// then convert to a log scale between 0 and 255.
				contVal = (float) Math.pow(2, contVal) - 1;
						
//				System.out.println(String.format("Brightness %.2f, Contrast %.1f", brightVal, contVal));
				
				vrControl.setImageBrightness(brightVal, contVal);
			}
		}
		
	}
	


}
