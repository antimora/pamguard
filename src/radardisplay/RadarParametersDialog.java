package radardisplay;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import Array.ArrayManager;
import PamController.PamController;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.GeneralProjector.ParameterType;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class RadarParametersDialog extends PamDialog {

	private static RadarParameters radarParameters;
	private static RadarParametersDialog singleInstance;
	StylePanel stylePanel;
	ScalePanel scalePanel;
	RadarProjector radarProjector;
	DetectorsPanel detectorsPanel;
	public int maxRadialIndex;
	
	public RadarParametersDialog(Frame parentFrame) {
		
		super(parentFrame, "Radar Display Parameters", true);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Scales", new FirstTab());
		detectorsPanel = new DetectorsPanel();
		tabbedPane.add("Detectors", detectorsPanel);
		setDialogComponent(tabbedPane);
		
		setHelpPoint("displays.radarDisplayHelp.docs.UserDisplay_Radar_Configuring");
		
	}
	
	public static RadarParameters showDialog(Frame parentFrame, 
			RadarParameters radarParameters, RadarProjector radarProjector) {
		
		if (singleInstance == null || parentFrame != singleInstance.getOwner()) {
			singleInstance = new RadarParametersDialog(parentFrame);
		}
		RadarParametersDialog.radarParameters = radarParameters.clone();
		singleInstance.radarProjector = radarProjector;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return RadarParametersDialog.radarParameters;
	}
	
	void setParams() {
		stylePanel.setParams();
		scalePanel.setParams();
		detectorsPanel.setParams();
	}

	@Override
	public void cancelButtonPressed() {
		
		radarParameters = null;
		
	}

	@Override
	public boolean getParams() {
		if (stylePanel.getParams() == false) return false;
		if (scalePanel.getParams() == false) return false;
		if (detectorsPanel.getParams() == false) return false;
		return true;
	}

	@Override
	public void restoreDefaultSettings() {

		RadarParametersDialog.radarParameters = new RadarParameters();
		
		setParams();
		
	}

	class FirstTab extends JPanel {

		public FirstTab() {
			super();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(stylePanel = new StylePanel());
			add(scalePanel = new ScalePanel());
		}
		
	}
	class StylePanel extends JPanel {
		
		JTextField windowName;
		JComboBox sides;
		JComboBox radialAxis;
		JComboBox orientation;
		
		public StylePanel () {
			super();
						
			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			GridBagConstraints constraints = new PamGridBagContraints();
//			constraints.anchor = GridBagConstraints.WEST;
//			constraints.fill = GridBagConstraints.HORIZONTAL;
//			constraints.ipadx = 2;
//			constraints.ipady = 3;

			setBorder(new TitledBorder("Plot layout"));
			constraints.gridx = 0;
			constraints.gridy = 0;
			addComponent(this, new JLabel("Name ", JLabel.RIGHT), constraints);
			constraints.gridx ++;
			constraints.gridwidth = 2;
			addComponent(this, windowName = new JTextField(20), constraints);
			constraints.gridwidth = 1;
			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(this, new JLabel("Style ", JLabel.RIGHT), constraints);
			constraints.gridx ++;
			addComponent(this, sides = new JComboBox(), constraints);

			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(this, new JLabel("Orientation ", JLabel.RIGHT), constraints);
			constraints.gridx ++;
			addComponent(this, orientation = new JComboBox(), constraints);
			orientation.addItem("Heading Up");
			orientation.addItem("North Up");
			
			
			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(this, new JLabel("Radial Axis ", JLabel.RIGHT), constraints);
			constraints.gridx ++;
			addComponent(this, radialAxis = new JComboBox(), constraints);
			radialAxis.addActionListener(new ChangeListener());
		}
		
		void setParams() {
			if (radarParameters.windowName != null)
				windowName.setText(radarParameters.windowName);
			
			sides.removeAllItems();
			sides.addItem("Full display");
			sides.addItem("Right half only");
			sides.addItem("Left half only");
			sides.addItem("Front half only");
			sides.addItem("Back half only");
			sides.setSelectedIndex(radarParameters.sides);
			
			radialAxis.removeAllItems();
			radialAxis.addItem("Amplitude scale");
			radialAxis.addItem("Distance scale");
			maxRadialIndex = 1;
			if (ArrayManager.getArrayManager().getCurrentArray().getArrayShape() >= ArrayManager.ARRAY_TYPE_PLANE) {
				radialAxis.addItem("Slant Angle");
				maxRadialIndex = 2;
			}
			if (radarParameters.radialAxis <= maxRadialIndex) {
				radialAxis.setSelectedIndex(radarParameters.radialAxis);
			}
			else {
				radialAxis.setSelectedIndex(0);
			}
			
			orientation.setSelectedIndex(radarParameters.orientation);
		}
		
		boolean getParams() {
			radarParameters.windowName = windowName.getText();
			radarParameters.sides = sides.getSelectedIndex();
			radarParameters.radialAxis = radialAxis.getSelectedIndex();
			radarParameters.orientation = orientation.getSelectedIndex();
			return true;
		}
		
		int getRadialAxisSelection() {
			return radialAxis.getSelectedIndex();
		}
	}
	
	class ChangeListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
			if (scalePanel != null) {
				scalePanel.setParams();
				detectorsPanel.setParams();
			}
			
		}
		
	}
	
	class ScalePanel extends JPanel {

		TitledBorder titledBorder;
		JTextField minValue, maxValue;
		JLabel minLabel, maxLabel;
		public ScalePanel () {
			super();
						
			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.ipadx = 2;
			constraints.ipady = 3;

			setBorder(titledBorder = new TitledBorder("Scales"));
			constraints.gridx = 0;
			constraints.gridy = 0;
			addComponent(this, new JLabel("Min "), constraints);
			constraints.gridx ++;
			addComponent(this, minValue = new JTextField(6), constraints);
			constraints.gridx ++;
			addComponent(this, minLabel = new JLabel("m"), constraints);
			constraints.gridy++;
			constraints.gridx = 0;
			addComponent(this, new JLabel("Max "), constraints);
			constraints.gridx ++;
			addComponent(this, maxValue = new JTextField(6), constraints);
			constraints.gridx ++;
			addComponent(this, maxLabel = new JLabel("m"), constraints);
		}
		void setParams() {
			if (stylePanel == null) return;
			int axisSelection = stylePanel.getRadialAxisSelection();
			switch (axisSelection) {
			case RadarParameters.RADIAL_AMPLITIDE:
//				titledBorder.setTitle("Amplitude range");
				setBorder(titledBorder = new TitledBorder("Amplitude range"));
				minValue.setText(String.format("%d", radarParameters.rangeStartdB));
				maxValue.setText(String.format("%d", radarParameters.rangeEnddB));
				minLabel.setText(" dB");
				maxLabel.setText(" dB");
				break;
			case RadarParameters.RADIAL_DISTANCE:
//				titledBorder.setTitle("Distance range");
				setBorder(titledBorder = new TitledBorder("Distance range"));
				minValue.setText(String.format("%d", radarParameters.rangeStartm));
				maxValue.setText(String.format("%d", radarParameters.rangeEndm));
				minLabel.setText(" metres");
				maxLabel.setText(" metres");
				break;
			case RadarParameters.RADIAL_SLANT_ANGLE:
				setBorder(titledBorder = new TitledBorder("Slant Angle"));
				minValue.setText("90");
				maxValue.setText("0");
				minLabel.setText(" degrees");
				maxLabel.setText(" degrees");
				break;
			}
			enableControls();
		}
		
		void enableControls() {
			int axisSelection = stylePanel.getRadialAxisSelection();
			minValue.setEnabled(axisSelection != RadarParameters.RADIAL_SLANT_ANGLE);
			maxValue.setEnabled(axisSelection != RadarParameters.RADIAL_SLANT_ANGLE);
		}
		
		boolean getParams() {
			if (stylePanel == null) return false;
			int axisSelection = stylePanel.getRadialAxisSelection();
			try {
				switch (axisSelection) {
				case RadarParameters.RADIAL_AMPLITIDE:
					radarParameters.rangeStartdB = Integer.valueOf(minValue.getText());
					radarParameters.rangeEnddB = Integer.valueOf(maxValue.getText());
					break;
				case RadarParameters.RADIAL_DISTANCE:
					radarParameters.rangeStartm = Integer.valueOf(minValue.getText());
					radarParameters.rangeEndm = Integer.valueOf(maxValue.getText());
					break;
				}
			}
			catch (Exception Ex) {
				return false;
			}
			return true;
		}
	}
	class DetectorsPanel extends JPanel {
		
		JCheckBox[] checkBoxes;
		
		JTextField[] textFields;
		
		void setParams() {
			fillPanel();
		}
		
		boolean getParams() {
			if (checkBoxes == null || textFields == null) return true;
			for (int i = 0; i < checkBoxes.length; i++) {
				if (checkBoxes[i] == null) {
					radarParameters.showDetector[i] = false;
					continue;
				}
				try {
					radarParameters.showDetector[i] = checkBoxes[i].isSelected();
					radarParameters.detectorLifetime[i] = Integer.valueOf(textFields[i].getText());
				}
				catch (Exception Ex) {
					return false;
				}
			}
			return true;
		}
		
		void fillPanel() {

			int axisSelection = stylePanel.getRadialAxisSelection();
			if (axisSelection == RadarParameters.RADIAL_AMPLITIDE)
				radarProjector.setParmeterType(1, ParameterType.AMPLITUDE);
			else if (axisSelection == RadarParameters.RADIAL_DISTANCE)
				radarProjector.setParmeterType(1, ParameterType.RANGE);
			else if (axisSelection == RadarParameters.RADIAL_SLANT_ANGLE)
				radarProjector.setParmeterType(1, ParameterType.SLANTANGLE);
			
			
			ArrayList<PamDataBlock> detectorDataBlocks = 
				PamController.getInstance().getDataBlocks(PamDataUnit.class, true);
			
			this.removeAll();

			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			GridBagConstraints constraints = new PamGridBagContraints();
			constraints.anchor = GridBagConstraints.WEST;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.ipadx = 2;
			constraints.ipady = 3;

			setBorder(new TitledBorder("Show Detector Data"));
			constraints.gridx = 0;
			constraints.gridy = 0;

			addComponent(this, new JLabel("Detector"), constraints);
			constraints.gridx++;
			addComponent(this, new JLabel("Lifetime (s)"), constraints);
			
			if (detectorDataBlocks != null) {
				if (radarParameters.showDetector == null ||
						detectorDataBlocks.size() > radarParameters.showDetector.length) {
					radarParameters.showDetector = new boolean[detectorDataBlocks.size()];
					radarParameters.detectorLifetime = new int[detectorDataBlocks.size()];
				}
				checkBoxes = new JCheckBox[detectorDataBlocks.size()];
				textFields = new JTextField[detectorDataBlocks.size()];
			}
			PamDataBlock dataBlock;
			for (int i = 0; i < detectorDataBlocks.size(); i++) {
				dataBlock = detectorDataBlocks.get(i);
				if (dataBlock.canDraw(radarProjector) == false) continue;
				constraints.gridx = 0;
				constraints.gridy ++;
				addComponent(this, checkBoxes[i] = new JCheckBox(dataBlock.getDataName()), constraints);
				checkBoxes[i].setSelected(radarParameters.showDetector[i]);
				constraints.gridx++;
				addComponent(this, textFields[i] = new JTextField(6), constraints);
				textFields[i].setText(String.format("%d", radarParameters.detectorLifetime[i]));
			}
			pack();
		}
	}
}
