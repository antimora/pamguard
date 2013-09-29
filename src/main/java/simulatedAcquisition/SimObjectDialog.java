package simulatedAcquisition;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamUtils.LatLongDialogStrip;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class SimObjectDialog extends PamDialog {

	private static SimObjectDialog singleInstance;
	
	private SimObject simObject;
	private SimProcess simProcess;
	
	private JTextField name;
	private LatLongDialogStrip latDialogStrip, longDialogStrip;
	private JTextField cog, spd, depth, amplitude;
	private JComboBox soundTypes;
	private JTextField meanInterval;
	private JCheckBox randomIntervals;
	
	private SimObjectDialog(Frame parentFrame, SimProcess simProcess) {
		super(parentFrame, "Simulated Object", false);
		this.simProcess = simProcess;
		
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Object parameters"));
		p.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		addComponent(p, new JLabel("Name "), c);
		c.gridx++;
		c.gridwidth = 5;
		addComponent(p, name = new JTextField(10), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		addComponent(p, new JLabel("Sound Type "), c);
		c.gridx++;
		c.gridwidth = 5;
		addComponent(p, soundTypes = new JComboBox(), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		addComponent(p, new JLabel("Amplitude"), c);
		c.gridx++;
		addComponent(p, amplitude = new JTextField(5), c);
		c.gridx++;
		addComponent(p, new JLabel("<html>dB re.1&mu;Pa p-p</html>"), c);
		c.gridy++;
		c.gridx= 0;
		c.gridwidth = 1;
		addComponent(p, new JLabel("Mean Interval "), c);
		c.gridx++;
		addComponent(p, meanInterval = new JTextField(5), c);
		c.gridx++;
		addComponent(p, new JLabel(" s   "), c);
		c.gridx++;
		c.gridwidth = 2;
		addComponent(p, randomIntervals = new JCheckBox("Randomise"), c);
		
		
		c.gridx = 0;
		c.gridwidth = 6;
		c.gridy++;
		latDialogStrip = new LatLongDialogStrip(true);
		addComponent(p, latDialogStrip, c);
		c.gridy++;
		longDialogStrip = new LatLongDialogStrip(false);
		addComponent(p, longDialogStrip, c);
		c.gridy++;
		c.gridwidth = 1;
		addComponent(p, new JLabel("Depth  "), c);
		c.gridx++;
		addComponent(p, depth = new JTextField(3), c);
		c.gridx++;
		addComponent(p, new JLabel(" m"), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		addComponent(p, new JLabel("Course  "), c);
		c.gridx++;
		addComponent(p, cog = new JTextField(3), c);
		c.gridx++;
		addComponent(p, new JLabel(" degrees   "), c);
		c.gridx++;
		addComponent(p, new JLabel("Speed  "), c);
		c.gridx++;
		addComponent(p, spd = new JTextField(3), c);
		c.gridx++;
		addComponent(p, new JLabel(" m/sec"), c);
		c.gridy++;
		c.gridx = 0;
		
		// fill the types comboBox
		SimSignals simSignals = simProcess.simSignals;
		int n = simSignals.getNumSignals();
		for (int i = 0; i < n; i++) {
			soundTypes.addItem(simSignals.getSignal(i));
		}
		
		setDialogComponent(p);
		setModal(true);
		
	}
	
	
	public static SimObject showDialog(Frame parentFrame, SimProcess simProcess, SimObject simObject) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame ||
				singleInstance.simProcess != simProcess) {
			singleInstance = new SimObjectDialog(parentFrame,simProcess);
		}
		singleInstance.simObject = simObject;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.simObject;
	}

	@Override
	public void cancelButtonPressed() {
		simObject = null;
	}
	
	private void setParams() {
		name.setText(simObject.name);
		latDialogStrip.sayValue(simObject.startPosition.getLatitude());
		longDialogStrip.sayValue(simObject.startPosition.getLongitude());
		amplitude.setText(String.format("%3.1f", simObject.amplitude));
		meanInterval.setText(String.format("%3.1f", simObject.meanInterval));
		randomIntervals.setSelected(simObject.randomIntervals);
		depth.setText(String.format("%3.1f", -simObject.getHeight()));
		spd.setText(String.format("%3.1f", simObject.speed));
		cog.setText(String.format("%3.1f", simObject.course));
		soundTypes.setSelectedItem(simProcess.simSignals.findSignal(simObject.signalName));
	}

	@Override
	public boolean getParams() {
		simObject.name = name.getText();
		simObject.startPosition.setLatitude(latDialogStrip.getValue());
		simObject.startPosition.setLongitude(longDialogStrip.getValue());
		simObject.randomIntervals = randomIntervals.isSelected();
		try {
			simObject.amplitude = Double.valueOf(amplitude.getText());
			simObject.meanInterval = Double.valueOf(meanInterval.getText());
			simObject.setHeight(-Double.valueOf(depth.getText()));
			simObject.speed = Double.valueOf(spd.getText());
			simObject.course = Double.valueOf(cog.getText());
		}
		catch (NumberFormatException e) {
			return false;
		}
		if (simObject.meanInterval <= 0) {
			JOptionPane.showMessageDialog(this, "Mean interval must be > 0");
			return false;
		}
		SimSignal simSignal = (SimSignal) soundTypes.getSelectedItem();
		if (simSignal == null) {
			return false;
		}
		simObject.signalName = simSignal.getName();
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
