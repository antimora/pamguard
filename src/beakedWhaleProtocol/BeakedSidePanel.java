package beakedWhaleProtocol;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;

import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.PamBorderPanel;
import PamView.PamColors;
import PamView.PamLabel;
import PamView.PamSidePanel;
import PamView.PamColors.PamColor;

public class BeakedSidePanel implements PamSidePanel {

	BeakedControl beakedControl;
	
	BeakedPanel beakedPanel;
	
	JButton setLocButton, startButton, settingsButton, endButton, swapSides, forceStart;
	PamLabel latLabel, longLabel;
	//angleLabel;
	
	PamLabel passingDistance, distributionType, navData, statusData, etaData, trackData;
	
	
	public BeakedSidePanel(BeakedControl beakedControl) {
		super();
		this.beakedControl = beakedControl;
		beakedPanel = new BeakedPanel();
		enableControls();
	}

	public JComponent getPanel() {
		return beakedPanel;
	}

	public void rename(String newName) {
		// TODO Auto-generated method stub

	}
	
	public void enableControls() {
		setLocButton.setEnabled(true);
		startButton.setEnabled(beakedControl.gpsDataBlock != null &&
				beakedControl.lastLocationData != null && 
				beakedControl.currentExperiment == null);
		settingsButton.setEnabled(beakedControl.currentExperiment == null);
		endButton.setEnabled(beakedControl.currentExperiment != null);
		swapSides.setEnabled(beakedControl.currentExperiment != null && 
				beakedControl.currentExperiment.status == BeakedExperimentData.APPROACH_START);
//		angleLabel.setVisible(beakedControl.beakedParameters.measureAngles);
	}
	
	public void sayStatus() {
		if (beakedControl.currentExperiment == null) {
			passingDistance.setText("");
			distributionType.setText("No active experiment");
			navData.setText("");
			statusData.setText("");
			etaData.setText("");
			trackData.setText("");
		}
		else {
			passingDistance.setText(String.format("Passing distance %.0fm", beakedControl.currentExperiment.perpDistance));
			distributionType.setText(String.format("%s distribution", beakedControl.beakedParameters.getDistributionName()));
			
			String navText = String.format("CSE %.0f\u00B0; DTG %.0fm", beakedControl.currentExperiment.course,
					beakedControl.getDTG());
			if (beakedControl.currentExperiment.status == BeakedExperimentData.ON_TRACK) {
				navText += String.format("; XTE %.0fm", beakedControl.getXTE());
			}
			navData.setText(navText);
			etaData.setText(formatETA(beakedControl.getEtaSeconds()));
			trackData.setText(String.format("Track len' %.0fm (%.1fnmi)", beakedControl.currentExperiment.totalTrackLength,
					(int) beakedControl.currentExperiment.totalTrackLength / LatLong.MetersPerMile));
			statusData.setText(beakedControl.currentExperiment.getStatusString());
		}
	}

//	public void newAngle(Double rawAngle, Double correctedAngle) {
//		String as = String.format(" %.1f\u00B0", correctedAngle);
////		angleLabel.setText(as);
//		
//	}
	private String formatETA(double etaSeconds) {
		return String.format("ETA %s (%.0f s)",  
				PamCalendar.formatTime(PamCalendar.getTimeInMillis() + (int) (etaSeconds * 1000.)), etaSeconds);
	}
	
	class BeakedPanel extends PamBorderPanel {
		
		TitledBorder titledBorder;

		public BeakedPanel() {
			super();
			setBorder(titledBorder = new TitledBorder(beakedControl.getUnitName()));
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			addComponent(this, setLocButton = new JButton("Set loc'"), c);
			c.gridx++;
			addComponent(this, startButton = new JButton("Start exp'"), c);
			c.gridx = 0;
			c.gridy++;
			addComponent(this, settingsButton = new JButton("Settings"), c);
			c.gridx++;
//			addComponent(this, abortButton = new JButton("Abort experiment"), c);
//			c.gridy++;
			addComponent(this, endButton = new JButton("End exp'"), c);
			c.gridx = 0;
			c.gridy++;
			addComponent(this, swapSides = new JButton("Swap Sides"), c);
			c.gridx++;
			addComponent(this, forceStart = new JButton("Force Start"), c);
			c.gridx = 0;
			c.gridwidth = 2;
			c.gridy++;
			addComponent(this, statusData = new PamLabel(), c);
			c.gridy++;
			addComponent(this, trackData = new PamLabel(), c);
			c.gridy++;
			addComponent(this, passingDistance = new PamLabel(), c);
			c.gridy++;
			addComponent(this, distributionType = new PamLabel(), c);
			c.gridy++;
			addComponent(this, navData = new PamLabel(), c);
			c.gridy++;
			addComponent(this, etaData = new PamLabel(), c);
			
			
			setLocButton.addActionListener(new SetLocAction());
			startButton.addActionListener(new StartAction());
			settingsButton.addActionListener(new SettingsAction());
//			abortButton.addActionListener(new AbortAction());
			endButton.addActionListener(new EndAction());
			swapSides.addActionListener(new SwapSidesAction());
			forceStart.addActionListener(new ForceStartAction());
		}
		
		class SetLocAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				beakedControl.setLocation(null);
			}
		}
		
		class StartAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				beakedControl.startExperiment();
			}
		}
		
		class SettingsAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				beakedControl.settings(null);
			}
		}
//		class AbortAction implements ActionListener {
//			public void actionPerformed(ActionEvent e) {
//				beakedControl.abortExperiment();
//			}
//		}
		class EndAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				beakedControl.endExperiment();
			}
		}
		class SwapSidesAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				beakedControl.swapSides();
			}
		}
		
		class ForceStartAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				beakedControl.forceStart();
			}
		}
		
		@Override
		public void setBackground(Color bg) {
			super.setBackground(bg);
			if (titledBorder != null) {
				titledBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
			}
		}
	}

}
