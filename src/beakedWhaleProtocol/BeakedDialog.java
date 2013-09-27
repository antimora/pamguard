package beakedWhaleProtocol;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import angleMeasurement.AngleDataUnit;
import videoRangePanel.VRDataUnit;

import PamUtils.LatLong;
import PamUtils.LatLongDialogStrip;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;

public class BeakedDialog extends PamDialog {

	private static BeakedDialog singleInstance;
	
	private BeakedParameters beakedParameters;
	
	private BeakedControl beakedControl;
	
	private Frame parentFrame;
	
	private JTextField maxAcousticRange, maxExperimentRange;
	private JComboBox distributions;
	
	private LatLongDialogStrip latStrip = new LatLongDialogStrip(true);
	private LatLongDialogStrip longStrip = new LatLongDialogStrip(false);
	private JTextField stationHeight;
	
	
	private BeakedDialog(Frame parentFrame) {
		super(parentFrame, "Beaked whale experiment setup", false);
		JTabbedPane tp = new JTabbedPane();
		JPanel p1 = makePanel1();
		tp.add("General", p1);
		
		JPanel ar = new JPanel();
		ar.setLayout(new BoxLayout(ar, BoxLayout.Y_AXIS));
		ar.add(makeVideoPanel());
		ar.add(makeAnglePanel());
		tp.add("Range and Bearing", ar);
		
		
		setDialogComponent(tp);
		
	}
	
	private JPanel  makePanel1() {
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Track Rules"));
		p.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
//		c.anchor = GridBagConstraints.CENTER;
		addComponent(p, new JLabel("Maximum acoustic range  "), c);
		c.gridx+=c.gridwidth;
		addComponent(p, maxAcousticRange = new JTextField(6), c);
		c.gridx++;
		addComponent(p, new JLabel(" m "), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(p, new JLabel("Maximum experiment range  "), c);
		c.gridx+=c.gridwidth;
		addComponent(p, maxExperimentRange = new JTextField(6), c);
		c.gridx++;
		addComponent(p, new JLabel(" m "), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		addComponent(p, new JLabel(" "), c);
		c.gridy++;
		addComponent(p, new JLabel("Distribution for randomise passing distance"), c);
		c.gridy++;
		addComponent(p, distributions = new JComboBox(), c);
		
		// shore station setup
		JPanel q = new JPanel();
		q.setBorder(new TitledBorder("Shore Station"));
		q.setLayout(new GridBagLayout());
		c.gridx = c.gridy = 0;
		c.gridwidth = 3;
		addComponent(q,latStrip,c);
		c.gridy++;
		addComponent(q,longStrip,c);
		c.gridy++;
		c.gridwidth = 1;
		addComponent(q, new JLabel("Station height "), c);
		c.gridx++;
		addComponent(q, stationHeight = new JTextField(6), c);
		c.gridx++;
		addComponent(q, new JLabel(" m "), c);

		
		
		JPanel r = new JPanel();
		r.setLayout(new BoxLayout(r, BoxLayout.Y_AXIS));
		r.add(p);
		r.add(q);
		
		return r;
	}
	
	JCheckBox monitorVideoRange;
	SourcePanel videoSource;
	JPanel makeVideoPanel() {
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Video Range"));
		p.setLayout(new BorderLayout());
		p.add(BorderLayout.NORTH, monitorVideoRange = new JCheckBox("Automatically use video ranges"));
		monitorVideoRange.addActionListener(new MonitorVideo());
		videoSource = new SourcePanel(this, VRDataUnit.class, false, true);
		p.add(BorderLayout.CENTER, videoSource.getPanel());
		return p;
	}
	
	JCheckBox measureAngles, showLine;
	JTextField lineLength, updateInterval;
//	JButton angleSettings;
	SourcePanel angleSource;
	JPanel makeAnglePanel() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JPanel a = new JPanel();
		a.setBorder(new TitledBorder("Angle measurement"));
		a.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridx = c.gridy = 0;
		c.gridwidth = 3;
		addComponent(a, measureAngles = new JCheckBox("Measure angles using Fluxgate"), c);
//		c.gridx+= c.gridwidth;
//		c.gridwidth = 1;
//		addComponent(a, angleSettings = new JButton("Settings"), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(a, new JLabel("Angle data source"), c);
		c.gridx = 0;
		c.gridy++;
		angleSource = new SourcePanel(this, AngleDataUnit.class, false, false);
		addComponent(a, angleSource.getPanel(), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		addComponent(a, new JLabel("Min update interval "), c);
		c.gridx++;
		addComponent(a, updateInterval = new JTextField(5), c);
		c.gridx++;
		addComponent(a, new JLabel(" s"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		addComponent(a, showLine = new JCheckBox("Show line on map"), c);
		c.gridy++;
		c.gridwidth = 1;
		addComponent(a, new JLabel("Line length "), c);
		c.gridx++;
		addComponent(a, lineLength = new JTextField(5), c);
		c.gridx++;
		addComponent(a, new JLabel(" m"), c);
		
		measureAngles.addActionListener(new MeasureAngles());
		showLine.addActionListener(new ShowLine());
		
		p.add(BorderLayout.NORTH, a);
		
		
		return p;
	}

	public static BeakedParameters showDialog(Frame parentFrame, BeakedControl beakedControl, BeakedParameters beakedParameters) {
		if (singleInstance == null || parentFrame != singleInstance.getOwner()) {
			singleInstance = new BeakedDialog(parentFrame);
		}
		singleInstance.beakedParameters = beakedParameters.clone();
		singleInstance.beakedControl = beakedControl;
		singleInstance.parentFrame = parentFrame;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.beakedParameters;
	}
	
	public void setParams() {
		distributions.removeAllItems();
		String[] dl = BeakedParameters.distributions;
		for (int i = 0; i < dl.length; i++) {
			distributions.addItem(dl[i]);
		}
		distributions.setSelectedIndex(beakedParameters.distributionType);
		
		maxAcousticRange.setText(String.format("%d", beakedParameters.maxAcousticRange));
		maxExperimentRange.setText(String.format("%d", beakedParameters.maxExperimentRange));
		
		if (beakedParameters.shoreStation != null) {
			latStrip.sayValue(beakedParameters.shoreStation.getLatitude());
			longStrip.sayValue(beakedParameters.shoreStation.getLongitude());
		}
		stationHeight.setText(String.format("%.0f", beakedParameters.shoreStationHeight));
		
		monitorVideoRange.setSelected(beakedParameters.useVideoRange);
		videoSource.setSource(beakedParameters.videoRangeDataSource);
		
		measureAngles.setSelected(beakedParameters.measureAngles);
		showLine.setSelected(beakedParameters.showLine);
		lineLength.setText(String.format("%.1f", beakedParameters.lineLength));
		updateInterval.setText(String.format("%.1f", beakedParameters.angleUpdateInterval));
		
		angleSource.setSourceList();
		angleSource.setSource(beakedParameters.angleDataSource);
		
		enableControls();
		
	}
	
	@Override
	public void cancelButtonPressed() {

		beakedParameters = null;

	}

	@Override
	public boolean getParams() {

		try {
			beakedParameters.maxAcousticRange = Integer.valueOf(maxAcousticRange.getText());
			beakedParameters.maxExperimentRange = Integer.valueOf(maxExperimentRange.getText());
			beakedParameters.shoreStationHeight = Double.valueOf(stationHeight.getText());
		}
		catch (NumberFormatException ex) {
			return false;
		}
		
		beakedParameters.distributionType = distributions.getSelectedIndex();
		LatLong sll = new LatLong(latStrip.getValue(), longStrip.getValue());
		beakedParameters.shoreStation = sll;
		
		beakedParameters.useVideoRange = monitorVideoRange.isSelected();
		PamDataBlock videoSourceBlock = videoSource.getSource();
		if (videoSourceBlock != null) {
			beakedParameters.videoRangeDataSource = videoSourceBlock.getDataName();
		}

		beakedParameters.measureAngles = measureAngles.isSelected();
		beakedParameters.showLine = showLine.isSelected();
		try {
			beakedParameters.lineLength = Double.valueOf(lineLength.getText());
			beakedParameters.angleUpdateInterval = Double.valueOf(updateInterval.getText());
		}
		catch (NumberFormatException ex) {
			return false;
		}
		
		PamDataBlock angleBlock = angleSource.getSource();
		if (angleBlock != null) {
			beakedParameters.angleDataSource = angleBlock.getDataName();
		}
	
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}
	
	private void enableControls() {
		showLine.setEnabled(measureAngles.isSelected());
		lineLength.setEnabled(measureAngles.isSelected() && showLine.isSelected());
		angleSource.setEnabled(measureAngles.isSelected());
		updateInterval.setEnabled(measureAngles.isSelected());
		videoSource.setEnabled(monitorVideoRange.isSelected());
		int nVid = videoSource.getSourceCount();
		monitorVideoRange.setEnabled(nVid > 0);
		if (nVid == 0) {
			monitorVideoRange.setSelected(false);
		}
		int nAng = angleSource.getSourceCount();
		measureAngles.setEnabled(nAng > 0);
		if (nAng <= 0) {
			measureAngles.setSelected(false);
		}
	}
	
	class ShowLine implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			enableControls();
		}
		
	}
	
	class MeasureAngles implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			enableControls();
		}
		
	}
	class MonitorVideo implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			enableControls();
		}
		
	}
//	class AngleSettings implements ActionListener {
//
//		public void actionPerformed(ActionEvent e) {
//			beakedControl.fluxgateWorldAngles.settings(null);
//			
//		}
//		
//	}

}
