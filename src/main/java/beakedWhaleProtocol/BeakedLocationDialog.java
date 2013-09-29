package beakedWhaleProtocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import videoRangePanel.VRDataUnit;
import videoRangePanel.VRMeasurement;

import angleMeasurement.AngleDataBlock;
import angleMeasurement.AngleDataUnit;
import Map.MapController;
import PamUtils.LatLong;
import PamUtils.LatLongDialogStrip;
import PamView.PamDialog;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

public class BeakedLocationDialog extends PamDialog implements PamObserver {

	private static BeakedLocationDialog singleInstance;

	private BeakedControl beakedControl;

	private BeakedLocationData beakedLocationData;

	private LatLongDialogStrip latStrip, longStrip;

	private JLabel msgLabel;

	private JComboBox sourceList;

	private JTextArea commentArea;

	private JTextField range, bearing;

	private JRadioButton enterRangeBearing, enterLatLong;

	private JButton updateAngle;

	private JCheckBox autoAngle;

	double latestMeasuredAngle;

	boolean hasMeasuredAngle;

	private ShoreStationDataUnit shoreStationDataUnit;

	AngleDataBlock angleDataBlock;
	
	VRDataUnit vrDataUnit;

	private BeakedLocationDialog() {
		super(null, "Beaked Whale Location", false);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		GridBagConstraints c = new GridBagConstraints();

		ButtonGroup buttonGroup = new ButtonGroup();

		JPanel rb = new JPanel();
		rb.setBorder(new TitledBorder("Range and bearing from shore station"));
		rb.setLayout(new GridBagLayout());
		c.gridx = c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 5;
		addComponent(rb, enterRangeBearing = new JRadioButton(
				"Use range and bearing from shore station"), c);
		buttonGroup.add(enterRangeBearing);
		enterRangeBearing.addActionListener(new DataEntryType());
		c.gridy++;
		c.gridwidth = 1;
		addComponent(rb, new JLabel("Range "), c);
		c.gridx++;
		addComponent(rb, range = new JTextField(6), c);
		c.gridx++;
		addComponent(rb, new JLabel(" m"), c);
		c.gridy++;
		c.gridx = 0;
		addComponent(rb, new JLabel("Bearing "), c);
		c.gridx++;
		addComponent(rb, bearing = new JTextField(6), c);
		c.gridx++;
		addComponent(rb, new JLabel(" \u00B0T "), c);
		c.gridx++;
		addComponent(rb, updateAngle = new JButton("Update"), c);
		c.gridx++;
		addComponent(rb, autoAngle = new JCheckBox("auto"), c);

		range.addKeyListener(new RBTyped());
		bearing.addKeyListener(new RBTyped());
		updateAngle.addActionListener(new UpdateAngle());
		autoAngle.addActionListener(new AutoAngle());
		panel.add(rb);

		latStrip = new LatLongDialogStrip(true);
		longStrip = new LatLongDialogStrip(false);
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Enter whale positions"));
		p.setLayout(new GridBagLayout());
		c.gridx = c.gridy = 0;
		addComponent(p, enterLatLong = new JRadioButton(
				"Enter Lat Long directly"), c);
		buttonGroup.add(enterLatLong);
		enterLatLong.addActionListener(new DataEntryType());
		c.gridy++;
		addComponent(p, msgLabel = new JLabel("Some message or other"), c);
		c.gridy++;
		addComponent(p, latStrip, c);
		c.gridy++;
		addComponent(p, longStrip, c);
		c.gridy++;
		// JPanel q = new JPanel();
		// q.setLayout(new BorderLayout());
		// q.setBorder(new TitledBorder("Additional information"));
		panel.add(p);

		JPanel q = new JPanel();
		q.setLayout(new BorderLayout());
		q.setBorder(new TitledBorder("Other information"));
		// c.gridx = c.gridy = 0;
		// addComponent(q, sourceList = new JComboBox(), c);
		// c.gridy++;
		// addComponent(q, new JLabel("Comment ..."), c);
		// c.gridy++;
		// addComponent(q, commentArea = new JTextArea(5,1), c);
		// c.gridy++;
		q.add(BorderLayout.NORTH, sourceList = new JComboBox());
		q.add(BorderLayout.CENTER, new JLabel("Comment ..."));
		q.add(BorderLayout.SOUTH, commentArea = new JTextArea(5, 1));
		// addComponent(p, q, c);
		commentArea.setPreferredSize(new Dimension(1, 50));
		commentArea.setBorder(BorderFactory.createLoweredBevelBorder());
		commentArea.setWrapStyleWord(true);
		commentArea.setLineWrap(true);
		commentArea
				.setToolTipText(String
						.format(
								"Comments > %d characters long will be truncated in the database",
								BeakedLogging.COMMENT_LENGTH));
		commentArea.addKeyListener(new CommentListener());

		// fill the source list just the once
		String[] list = BeakedLocationData.locationSources;
		for (int i = 0; i < list.length; i++) {
			sourceList.addItem(list[i]);
		}

		panel.add(q);

		setDialogComponent(panel);
	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}
	
	class UpdateAngle implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			if (hasMeasuredAngle == true) {
				setBearing(latestMeasuredAngle);
			}
		}

	}

	class AutoAngle implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			enableAngleControls();
		}

	}

	class CommentListener implements KeyListener {
		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
			checkCommentLength();
		}
	}

	private void checkCommentLength() {
		int commentLength = 0;
		String txt = commentArea.getText();
		if (txt != null) {
			commentLength = txt.length();
		}
		if (commentLength < BeakedLogging.COMMENT_LENGTH) {
			commentArea.setBackground(Color.WHITE);
		} else {
			commentArea.setBackground(Color.PINK);
		}
	}

	public static BeakedLocationData showDialog(BeakedControl beakedControl, VRDataUnit vrDataUnit) {
		if (singleInstance == null) {
			singleInstance = new BeakedLocationDialog();
		}
		singleInstance.beakedControl = beakedControl;
		singleInstance.vrDataUnit = vrDataUnit;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.beakedLocationData;
	}

	@Override
	public void setVisible(boolean b) {
		angleDataBlock = beakedControl.beakedProcess.angleDataBlock;
		if (angleDataBlock != null && b) {
			angleDataBlock.addObserver(this);
			hasMeasuredAngle = false;
		} else if (angleDataBlock != null) {
			angleDataBlock.deleteObserver(this);
		}
		super.setVisible(b);
	}

	@Override
	public void cancelButtonPressed() {

		beakedLocationData = null;

	}

	private void setParams() {
		// see if the shore station data exist.
		ShoreStationDataBlock sdb = beakedControl.beakedProcess.shoreStationDataBlock;
		shoreStationDataUnit = sdb.getLastUnit();
		if (shoreStationDataUnit == null) {
			BeakedLocationData.enterLatLong = true;
			enterRangeBearing.setEnabled(false);
		}

		LatLong mapLatLong = MapController.getMouseClickLatLong();
		if (mapLatLong == null) {
			msgLabel.setText("Enter Lat Long of beaked whales");
			latStrip.clearData();
			longStrip.clearData();
		} else {
			msgLabel.setText("LatLong taken from last click on map");
			latStrip.sayValue(mapLatLong.getLatitude(), false);
			longStrip.sayValue(mapLatLong.getLongitude(), false);
		}
		commentArea.setText("");
		checkCommentLength();
		enterRangeBearing.setSelected(!BeakedLocationData.enterLatLong);
		enterLatLong.setSelected(BeakedLocationData.enterLatLong);

		range.setText("");
		bearing.setText("");
		boolean hasVRAngle = false;
		if (vrDataUnit != null) {
			VRMeasurement vrm = vrDataUnit.getVrMeasurement();
			range.setText(String.format("%.1f", vrm.distanceMeters));
			hasVRAngle = (vrm.cameraAngle != null);
			if (hasVRAngle) {
				bearing.setText(String.format("%.1f", vrm.totalAngle));
			}
			sourceList.setSelectedIndex(BeakedLocationData.SOURCE_SHORESIGHTING_VIDEO);
		}
		if (hasVRAngle == false && beakedControl.beakedParameters.measureAngles) {
			AngleDataUnit heldAngle = beakedControl.beakedProcess.getHeldAngle();
			if (heldAngle != null) {
				bearing.setText(String.format("%.1f", heldAngle.correctedAngle));
			}
		}

		enableControls();
	}

	@Override
	public boolean getParams() {

		if (enterRangeBearing.isSelected()) {
			// call this just to ensure that correct lat long will be read.
			if (newRangeOrBearing() == false) {
				return false;
			}
		}

		beakedLocationData = new BeakedLocationData();
		double lat = latStrip.getValue();
		double longi = longStrip.getValue();
		if (Double.isNaN(lat) || Double.isNaN(longi)) {
			return false;
		}
		beakedLocationData.latLong = new LatLong(lat, longi);
		beakedLocationData.locationSource = sourceList.getSelectedIndex();
		beakedLocationData.comment = commentArea.getText();
		beakedLocationData.range = shoreStationDataUnit.getLatLong().distanceToMetres(beakedLocationData.latLong);
		beakedLocationData.bearing = shoreStationDataUnit.getLatLong().bearingTo(beakedLocationData.latLong);
		BeakedLocationData.enterLatLong = enterLatLong.isSelected();
		return true;
	}

	private void enableControls() {
		enterRangeBearing.setEnabled(hasShore());
		if (!hasShore()) {
			enterRangeBearing.setSelected(false);
			enterLatLong.setSelected(true);
		}
		boolean useRB = enterRangeBearing.isSelected();
		range.setEnabled(useRB && hasShore());
		bearing.setEnabled(useRB && hasShore());
		latStrip.setEnabled(!useRB);
		longStrip.setEnabled(!useRB);
	}
	
	private boolean hasShore() {
		if (shoreStationDataUnit == null || shoreStationDataUnit.getLatLong() == null) {
			return false;
		}
		return true;
	}

	private void enableAngleControls() {
		Double lastAngle = beakedControl.beakedProcess.getLatestAngle();
		updateAngle.setEnabled(lastAngle != null
				&& beakedControl.beakedParameters.measureAngles
				&& autoAngle.isSelected() == false);
		autoAngle.setEnabled(lastAngle != null
				&& beakedControl.beakedParameters.measureAngles);
	}

	public String getObserverName() {
		return "BEaked Whale dialog";
	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		return 0;
	}

	public void noteNewSettings() {

	}

	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub

	}

	public void setSampleRate(float sampleRate, boolean notify) {
		// TODO Auto-generated method stub

	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub
		
	}

	public void update(PamObservable o, PamDataUnit arg) {

		AngleDataUnit angleDataUnit = (AngleDataUnit) arg;
		newAngle(angleDataUnit.rawAngle, angleDataUnit.correctedAngle);

	}

	public void newAngle(Double rawAngle, Double correctedAngle) {
		if (autoAngle.isSelected()) {
			setBearing(correctedAngle);
		}
		latestMeasuredAngle = correctedAngle;
		if (hasMeasuredAngle == false) {
			enableAngleControls();
			hasMeasuredAngle = true;
		}
	}

	private void setBearing(double newBearing) {
		bearing.setText(String.format("%.1f", newBearing));
	}

	private class DataEntryType implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			enableControls();
		}

	}

	/**
	 * automatically fill out the lat long information based on the range and
	 * bearing.
	 * 
	 */
	private boolean newRangeOrBearing() {
		if (shoreStationDataUnit == null || shoreStationDataUnit.getLatLong() == null) {
			return false;
		}
		
		double newRange, newBearing;
		try {
			newRange = Double.valueOf(range.getText());
			newBearing = Double.valueOf(bearing.getText());
		} catch (NumberFormatException e) {
			return false;
		}
		LatLong newLatlong = shoreStationDataUnit.getLatLong()
				.travelDistanceMeters(newBearing, newRange);
		latStrip.sayValue(newLatlong.getLatitude());
		longStrip.sayValue(newLatlong.getLongitude());
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

	class RBTyped implements KeyListener {

		public void keyPressed(KeyEvent e) {
			newRangeOrBearing();
		}

		public void keyReleased(KeyEvent e) {
			newRangeOrBearing();
		}

		public void keyTyped(KeyEvent e) {
			newRangeOrBearing();
		}

	}

}
