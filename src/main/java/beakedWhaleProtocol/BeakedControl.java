package beakedWhaleProtocol;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Random;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import videoRangePanel.VRDataBlock;
import videoRangePanel.VRDataUnit;
import Array.ArrayManager;
import Array.PamArray;
import GPS.GpsDataUnit;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamView.PamSidePanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class BeakedControl extends PamControlledUnit implements PamSettings {

	protected BeakedProcess beakedProcess;
	
	protected BeakedSidePanel beakedSidePanel;
	
	protected BeakedParameters beakedParameters = new BeakedParameters();
	
	protected PamDataBlock<GpsDataUnit> gpsDataBlock;
	
	protected BeakedLocationData lastLocationData;
	
	protected BeakedExperimentData currentExperiment;
	
//	FluxgateWorldAngles fluxgateWorldAngles;

	/**
	 * ETA and cross track error, set from pocess.
	 */
	private double etaSeconds, XTE, DTG;
	
	private BeakedDataUnit lastLocationDataUnit;
	
	private VideoRangeMonitor videoRangeMonitor;
	
	private VRDataBlock vrDataBlock;
	
//	GPSControl gpsControl;
	
	public BeakedControl(String unitName) {
		
		super("Beaked Whale Protocol", unitName);
		
//		fluxgateWorldAngles = new FluxgateWorldAngles("Shore angle", false);
		
		addPamProcess(beakedProcess = new BeakedProcess(this));
		
		addPamProcess(videoRangeMonitor = new VideoRangeMonitor(this));
		
		PamSettingManager.getInstance().registerSettings(this);
		
//		if (beakedParameters.measureAngles) {
//			fluxgateWorldAngles.start();
//		}
		
		beakedSidePanel = new BeakedSidePanel(this);
		
		findGpsData();
		
		beakedProcess.makeShoreDataUnit();
		
		beakedProcess.sortAnglreReadout();
		
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamControllerInterface.ADD_CONTROLLEDUNIT ||
				changeType == PamControllerInterface.REMOVE_CONTROLLEDUNIT ||
				changeType == PamControllerInterface.INITIALIZATION_COMPLETE) {
			findGpsData();
			beakedProcess.sortAnglreReadout();
			videoRangeMonitor.sortVRDataBlock();
		}
	}

	private void findGpsData() {

		if (beakedProcess == null) {
			return;
		}
		
		PamDataBlock<GpsDataUnit> newBlock = PamController.getInstance().getDataBlock(GpsDataUnit.class, 0);
		if (newBlock != gpsDataBlock) {
			gpsDataBlock = newBlock;
			beakedProcess.setParentDataBlock(gpsDataBlock);
			beakedSidePanel.enableControls();
		}
	}
	
	@Override
	public PamSidePanel getSidePanel() {
		return beakedSidePanel;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitType());
		menuItem.addActionListener(new SettingsAction(parentFrame));
		return menuItem;
	}
	
	class SettingsAction implements ActionListener {

		Frame parentFrame;
		
		public SettingsAction(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			settings(parentFrame);
		}
		
	}
	
	protected void settings(Frame parentFrame) {
		BeakedParameters newParams = BeakedDialog.showDialog(parentFrame, this, beakedParameters);
		if (newParams != null) {
			beakedParameters = newParams.clone();
			beakedProcess.makeShoreDataUnit();
//			if (beakedParameters.measureAngles) {
//				fluxgateWorldAngles.start();
//			}
//			else {
//				fluxgateWorldAngles.stop();
//			}
			beakedSidePanel.enableControls();
			beakedProcess.sortAnglreReadout();
		}
	}
	
	
	
	/**
	 * Set up an experiment by taking the current position, then navigating to slightly outside
	 * the edge of the circle and then cutting some shord across it at a sondomised distance from
	 * the last position.
	 *
	 */
	protected void startExperiment() {
		if (gpsDataBlock == null) {
			return;
		}
		if (lastLocationData == null) {
			return;
		}
		GpsDataUnit lastGps = gpsDataBlock.getLastUnit();
		if (lastGps == null) {
			return;
		}
		currentExperiment = new BeakedExperimentData(PamCalendar.getTimeInMillis());
		currentExperiment.experimentLocationData = lastLocationDataUnit;
		currentExperiment.vesselStart = new LatLong(lastGps.getGpsData().getLatitude(), lastGps.getGpsData().getLongitude());
		currentExperiment.perpDistance = generateDistance();
		
		// simple calculation just starts one h lenght outside the circle
		double expRadius;// = beakedParameters.maxRange + getMaxPhoneLength();
		// but if we're approaching at an angle, then can start a bit closer and still
		// get the hydrophone streamed before hitting the circle. 
		double l = Math.sqrt(beakedParameters.maxAcousticRange * beakedParameters.maxAcousticRange - 
				currentExperiment.perpDistance * currentExperiment.perpDistance);
		l += getMaxPhoneLength();
		expRadius = Math.sqrt(l * l + currentExperiment.perpDistance * currentExperiment.perpDistance);
		
		// now need to sail either directly towards, or directly away from the whales to get to the closest starting 
		// point for the experimnet. 
		double bearingtoWhale = currentExperiment.vesselStart.bearingTo(lastLocationData.latLong);
		LatLong startLatLong = lastLocationData.latLong.travelDistanceMeters(bearingtoWhale + 180, expRadius);
		currentExperiment.trackStart = startLatLong;
		
		double attackAngle = Math.asin(currentExperiment.perpDistance / expRadius) * 180 / Math.PI;
		if (random.nextBoolean()) {
			attackAngle = -attackAngle;
		}
		attackAngle = PamUtils.constrainedAngle(attackAngle);
		double halfDist = Math.sqrt(expRadius * expRadius - currentExperiment.perpDistance * currentExperiment.perpDistance);
		currentExperiment.course = bearingtoWhale + attackAngle;
		currentExperiment.course = PamUtils.constrainedAngle(currentExperiment.course);
		LatLong endLatLong = startLatLong.travelDistanceMeters(currentExperiment.course, halfDist * 2);
		currentExperiment.trackEnd = endLatLong.clone();
		
		currentExperiment.alternateCourse = bearingtoWhale - attackAngle;
		currentExperiment.alternateCourse = PamUtils.constrainedAngle(currentExperiment.alternateCourse);
		endLatLong = startLatLong.travelDistanceMeters(currentExperiment.alternateCourse, halfDist * 2);
		currentExperiment.alternateEnd = endLatLong.clone();
		
		currentExperiment.totalTrackLength = halfDist * 2;
		currentExperiment.status = BeakedExperimentData.APPROACH_START;
		
		beakedSidePanel.enableControls();
		beakedSidePanel.sayStatus();
		
		beakedProcess.newExperiment();
	}
	
	protected void setExperimentStatus(int newStatus) {
		if (currentExperiment == null) return;
		currentExperiment.status = newStatus;
		beakedSidePanel.sayStatus();
		if (newStatus == BeakedExperimentData.AUTOCOMPLETE) {
			autoEndExperiment();
		}
		beakedSidePanel.enableControls();
	}
	
	private Random random = new Random();
	private double generateDistance() {
		switch (beakedParameters.distributionType) {
		case BeakedParameters.RANDOM_FLAT:
			return random.nextDouble() * beakedParameters.maxExperimentRange;
		case BeakedParameters.RANDOM_HALFNORMAL:
			double d;
			while ((d = Math.abs(random.nextGaussian())) > 1);
			return d * beakedParameters.maxExperimentRange;
		}
		return 0;
	}
	
	private double getMaxPhoneLength() {
		// get teh distance astern of the furthest hydrophone
		double y = 0;
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		if (array == null) {
			return 0;
		}
		for (int i = 0; i < array.getHydrophoneCount(); i++) {
			y = Math.max(y, -array.getHydrophone(i).getY());
		}
		return y;
	}
	
	protected void setLocation(VRDataUnit vrDataUnit) {
		BeakedLocationData beakedLocationData = BeakedLocationDialog.showDialog(this, vrDataUnit);
		if (beakedLocationData != null) {
			beakedLocationData.videoData = vrDataUnit;
			lastLocationDataUnit = new BeakedDataUnit(PamCalendar.getTimeInMillis(), beakedLocationData);
			beakedProcess.beakedDataBlock.addPamData(lastLocationDataUnit);
			lastLocationData = beakedLocationData;
			beakedSidePanel.enableControls();
		}
	}
//	
//	protected void abortExperiment() {
//		
//		String reason = JOptionPane.showInputDialog("Enter reason for aborting experiment or Cancel to continue");
//		
//		if (reason == null) return;
//		
//		currentExperiment.comment = reason;
//		
//		currentExperiment.status = BeakedExperimentData.ABORTED;
//		
//		currentExperiment = null;
//
//		beakedSidePanel.enableControls();
//		
//	}
	
	protected void endExperiment() {

		currentExperiment.endTime = PamCalendar.getTimeInMillis();
		
		String reason = JOptionPane.showInputDialog("Enter reason for aborting experiment or Cancel to continue");
		
		if (reason == null) return;
		
		currentExperiment.comment = reason;

		currentExperiment.status = BeakedExperimentData.COMPLETE;
		
		beakedProcess.beakedExperimentDataBlock.addPamData(currentExperiment);
		
		currentExperiment = null;
		
		beakedSidePanel.enableControls();

		beakedSidePanel.sayStatus();
		
	}
	
	protected void autoEndExperiment() {

		currentExperiment.status = BeakedExperimentData.AUTOCOMPLETE;
		
		currentExperiment.comment = "Auto completed";
		
		beakedProcess.beakedExperimentDataBlock.addPamData(currentExperiment);
		
		currentExperiment = null;
		
		beakedSidePanel.enableControls();

		beakedSidePanel.sayStatus();
	}
	
	/** 
	 * can be called as the start point is being approached. 
	 * Keep the same start point, but swap to the same perp distance 
	 * on the other side of the whale. 	 *
	 */
	protected void swapSides() {
		if (currentExperiment != null) {
			currentExperiment.swapSides();
		}
	}
	
	protected void forceStart(){
		setExperimentStatus(BeakedExperimentData.ON_TRACK);
	}

	public Serializable getSettingsReference() {
		return beakedParameters;
	}

	public long getSettingsVersion() {
		return BeakedParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		beakedParameters = ((BeakedParameters) pamControlledUnitSettings.getSettings()).clone();
		return (beakedParameters != null);
	}

	public double getEtaSeconds() {
		return etaSeconds;
	}

	public void setEtaSeconds(double etaSeconds) {
		this.etaSeconds = etaSeconds;
		beakedSidePanel.sayStatus();
	}

	public double getXTE() {
		return XTE;
	}

	public void setXTE(double xte) {
		XTE = xte;
	}

	public double getDTG() {
		return DTG;
	}

	public void setDTG(double dtg) {
		DTG = dtg;
	}
	
	class VideoRangeMonitor extends PamProcess {

		BeakedControl beakedControl;
		
		public VideoRangeMonitor(BeakedControl beakedControl) {
			super(beakedControl, null);
			this.beakedControl = beakedControl;
			sortVRDataBlock();
		}

		@Override
		public String getProcessName() {
			return "Video range monitor";
		}

		private void sortVRDataBlock() {
			setParentDataBlock(null);
			vrDataBlock = null;
			if (beakedParameters.useVideoRange) {
				vrDataBlock = (VRDataBlock) PamController.getInstance().getDataBlock(VRDataUnit.class, beakedParameters.videoRangeDataSource);
				if (vrDataBlock != null) {
					setParentDataBlock(vrDataBlock);
				}
			}
		}

		@Override
		public void newData(PamObservable o, PamDataUnit arg) {
			/*
			 * When new video range data appear, pop up the beaked new location dialog. 
			 */
			if (beakedParameters.useVideoRange && o == vrDataBlock) {
				VRDataUnit vrd = (VRDataUnit) arg;
				setLocation(vrd);
			}
		}

		@Override
		public void pamStart() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void pamStop() {
			// TODO Auto-generated method stub
			
		}
		
	}


}
