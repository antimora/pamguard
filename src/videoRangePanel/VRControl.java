package videoRangePanel;

import java.awt.Point;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import angleMeasurement.AngleDataBlock;
import angleMeasurement.AngleDataUnit;

import Map.GebcoMapFile;
import Map.MapFileManager;
import Map.MasterReferencePoint;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class VRControl extends PamControlledUnit implements PamSettings {

	protected VRTabPanelControl vrTabPanelControl;
	
	protected VRParameters vrParameters = new VRParameters();

	protected VRSidePanel vrSidePanel;
	
	protected VRRangeMethods rangeMethods;
	
	protected VRProcess vrProcess;
	
	protected AngleListener angleListener;
	
	protected AngleDataBlock angleDataBlock;
	
	protected MapFileManager mapFileManager;
	
	protected ShoreManager shoreManager;
	
	private double[] shoreRanges;
	
	private Double imageAngle;
	
	/*
	 * flags for general status of what we're trying to do
	 */
	protected static final int NOIMAGE = -1;
	protected static final int MEASURE_FROM_HORIZON = 0;
	protected static final int MEASURE_FROM_SHORE = 1;
	protected static final int CALIBRATE = 2;
	
	/*
	 * Flags to say what should be measured next. 
	 */
	protected static final int MEASURE_NONE = -1;
	
	protected static final int MEASURE_HORIZON_1 = 0;
	protected static final int MEASURE_HORIZON_2 = 1;
	protected static final int MEASURE_ANIMAL = 2;
	protected static final int MEASURE_DONE = 3;
	protected static final int MEASURE_SHORE = 4;
	
	protected static final int CALIBRATE_1 = 10;
	protected static final int CALIBRATE_2 = 11;
	protected static final int CALIBRATE_DONE = 12;
	
	protected static final int DBCOMMENTLENGTH = 50;
	
	private int vrStatus = NOIMAGE;
	
	private int vrSubStatus = -1;
	
	private String imageName;
	
	private Point horizonPoint1, horizonPoint2, calibratePoint1, calibratePoint2, shorePoint;
	/*
	 * Horizon tilt in radians. 
	 */
	private double horizonTilt = 0;
	private ArrayList<VRMeasurement> measuredAnimals;
	private VRMeasurement candidateMeasurement;

	public VRControl(String unitName) {

		super("Video Range Measurement", unitName);

		vrTabPanelControl = new VRTabPanelControl(this);

		setTabPanel(vrTabPanelControl);

		setSidePanel(vrSidePanel = new VRSidePanel(this));
		
		rangeMethods = new VRRangeMethods(this);
		
		addPamProcess(vrProcess = new VRProcess(this));

		addPamProcess(angleListener = new AngleListener(this));
		
		PamSettingManager.getInstance().registerSettings(this);
		
		mapFileManager = new GebcoMapFile();
		
		shoreManager = new ShoreManager(mapFileManager);
		
		newSettings();
		
		// load the map file
		mapFileManager.readFileData(vrParameters.shoreFile, false);

	}

	protected void settingsButton(JFrame frame) {
		if (frame == null) {
			frame = getPamView().getGuiFrame();
		}
		VRParameters newParams = VRParametersDialog.showDialog(frame, this);
		if (newParams != null) {
			vrParameters = newParams.clone();
			newSettings();
		}
	}
	
	protected void pasteButton() {

		setImageName("Pasted Image");
		if (vrTabPanelControl.pasteImage()) {
			newImage();
//			setVrStatus(MEASURE_FROM_HORIZON);
		}
	}

	protected void fileButton() {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new ImageFileFilter());
		fileChooser.setCurrentDirectory(vrParameters.imageDirectory);
		fileChooser.setDialogTitle("Select image...");
		//fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileHidingEnabled(true);
		fileChooser.setApproveButtonText("Select");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		javax.swing.filechooser.FileFilter[] filters = fileChooser
		.getChoosableFileFilters();
		for (int i = 0; i < filters.length; i++) {
			fileChooser.removeChoosableFileFilter(filters[i]);
		}
		fileChooser.addChoosableFileFilter(new ImageFileFilter());

//		if (currFile != null) fileChooser.setSelectedFile(new File(currFile));
//		}
		int state = fileChooser.showOpenDialog(vrSidePanel.getPanel());
		if (state == JFileChooser.APPROVE_OPTION) {
			vrParameters.imageDirectory = fileChooser.getCurrentDirectory();
			File currFile = fileChooser.getSelectedFile();
			//System.out.println(currFile);
			loadFile(currFile);
		}
		newImage();
		
	}
	
	/**
	 * Force the video range tab to be selected.
	 *
	 */
	void showVRTab() {
		PamController.getInstance().showControlledUnit(this);
	}
	
//	protected void measureButton() {
//		setVrStatus(MEASURE_TO_HORIZON);
//		clearPoints();
//	}
//	
//	protected void calibrateButton() {
//		setVrStatus(CALIBRATE);
//		clearPoints();
//	}
	
	protected void setMeasurementType(int measuremntType) {
		setVrStatus(measuremntType);
		clearPoints();
	}
	
	protected void clearHorizon() {
		clearPoints();
		setVrStatus(MEASURE_FROM_HORIZON);
	}
	protected void clearShorePoint() {
		clearPoints();
		setVrStatus(MEASURE_FROM_SHORE);
	}
	
	protected void selectCalibration(VRCalibrationData vrCalibrationData) {
		vrParameters.setCurrentCalibration(vrCalibrationData);
		vrSidePanel.newCalibration(false);
		newSettings();
	}
	
	protected void selectHeight(int heightIndex) {
		vrParameters.setCurrentHeightIndex(heightIndex);
		newSettings();
	}
	
	protected void setImageBrightness(float brightness, float contrast) {
		vrTabPanelControl.vrPanel.setImageBrightness(brightness, contrast);
	}

	private void loadFile(File file) {
		if (vrTabPanelControl.loadFile(file)) {
			setImageName(file.getName());
			if (vrParameters.getCurrentCalibrationData() != null && getVrStatus() == CALIBRATE) {
				setVrStatus(MEASURE_FROM_HORIZON);
			}
		}
	}
	
	private void newImage() {
		clearPoints();
		if (vrTabPanelControl.vrPanel.vrImage != null) {
			showVRTab();
		}
		// causes a few other things to get reset too !
		autoSetVrStatus();
	}

	public Serializable getSettingsReference() {
		return vrParameters;
	}

	public long getSettingsVersion() {
		return VRParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {	
		vrParameters = ((VRParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}
	
	void newSettings() {
		vrTabPanelControl.newSettings();
		vrSidePanel.newSettings();
		if (vrTabPanelControl.vrPanel.vrImage == null) {
			setVrStatus(NOIMAGE);
		}
		else if (vrParameters.getCurrentCalibrationData() == null) {
			setVrStatus(CALIBRATE);
		}
		else if (getVrStatus() == CALIBRATE) {
			setVrStatus(MEASURE_FROM_HORIZON);
		}
		rangeMethods.setCurrentMethodId(vrParameters.rangeMethod);
		angleListener.sortAngleMeasurement();
	
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamControllerInterface.ADD_CONTROLLEDUNIT || changeType == PamControllerInterface.REMOVE_CONTROLLEDUNIT) {
			angleListener.sortAngleMeasurement();
		}
	}

	public void autoSetVrStatus() {
		if (vrTabPanelControl.vrPanel.getImageWidth() == 0) {
			setVrStatus(NOIMAGE);
		}
		else if (vrParameters.getCurrentCalibrationData() == null) {
			setVrStatus(CALIBRATE);
		}
		else if (getVrStatus() != MEASURE_FROM_HORIZON && getVrStatus() != MEASURE_FROM_SHORE) {
			setVrStatus(MEASURE_FROM_HORIZON);
		}
		else {
			setVrStatus(getVrStatus());
		}
	}
	
	public int getVrStatus() {
		return vrStatus;
	}

	public void setVrStatus(int vrStatus) {
		this.vrStatus = vrStatus;
		switch (vrStatus) {
		case NOIMAGE:
			setVrSubStatus(MEASURE_NONE);
			break;
		case CALIBRATE:
			setVrSubStatus(CALIBRATE_1);
			break;
		case MEASURE_FROM_HORIZON:
			setVrSubStatus(MEASURE_HORIZON_1);
			break;
		case MEASURE_FROM_SHORE:
			setVrSubStatus(MEASURE_SHORE);
			break;
		default:
			setVrSubStatus(MEASURE_NONE);
		}
		if (vrSidePanel != null) {
			vrSidePanel.setStatus();
		}
		if (vrTabPanelControl != null) {
			vrTabPanelControl.showComponents();
			vrTabPanelControl.enableControls();
		}
	}

	public int getVrSubStatus() {
		return vrSubStatus;
	}

	public void setVrSubStatus(int vrSubStatus) {
		this.vrSubStatus = vrSubStatus;
		if (vrSidePanel != null) {
			vrSidePanel.setStatus();
		}
		vrTabPanelControl.setStatus();
	}
	
	String getInstruction() {

		switch (vrSubStatus) {
		case MEASURE_HORIZON_1:
			return "Click first horizon point";
		case MEASURE_HORIZON_2:
			return "Click second horizon point";
		case MEASURE_ANIMAL:
			return "Click animal";
		case MEASURE_DONE:
			return "Measurement complete";
		case CALIBRATE_1:
			return "Click first cal' point";
		case CALIBRATE_2:
			return "Click second cal' point";
		case CALIBRATE_DONE:
			return "Calibration complete";
		case MEASURE_SHORE:
			return "Click shore behind animal";
		}
		return "";
	}
	
	void newMousePoint(Point mousePoint) {
		// called from mousemove.
		vrSidePanel.newMousePoint(mousePoint);
	}
	
	void mouseClick(Point mouseClick) {
		switch (vrSubStatus) {
		case MEASURE_HORIZON_1:
			horizonPoint1 = new Point(mouseClick);
			setVrSubStatus(MEASURE_HORIZON_2);
			break;
		case MEASURE_HORIZON_2:
			horizonPoint2 = new Point(mouseClick);
			calculateHorizonTilt();
			setVrSubStatus(MEASURE_ANIMAL);
			break;
		case MEASURE_ANIMAL:
//			horizonPoint1 = new Point(mouseClick);
			newAnimalMeasurement(mouseClick);
//			setVrSubStatus(MEASURE_DONE);
			break;
		case CALIBRATE_1:
			calibratePoint1 = new Point(mouseClick);
			setVrSubStatus(CALIBRATE_2);
			break;
		case CALIBRATE_2:
			calibratePoint2 = new Point(mouseClick);
			newCalibration();
			setVrSubStatus(CALIBRATE_DONE);
			break;
		case MEASURE_SHORE:
			shorePoint = new Point(mouseClick);
			horizonPointsFromShore(horizonTilt);
			setVrSubStatus(MEASURE_ANIMAL);
			break;
			
		}
	}
	
	private void horizonPointsFromShore(double tilt) {
		if (vrStatus != MEASURE_FROM_SHORE) {
			return;
		}
		// work out where the horizon should be based on the shore point. 
		horizonPoint1 = horizonPoint2 = null;
		if (shorePoint == null) {
			return;
		}
		int imageWidth = vrTabPanelControl.vrPanel.getImageWidth();
		if (imageWidth == 0) {
			return;
		}
		VRCalibrationData calData = vrParameters.getCurrentCalibrationData();
		VRHeightData heightData = vrParameters.getCurrentheightData();
		VRRangeMethod vrRangeMethod = rangeMethods.getCurrentMethod();
		Double imageBearing = getImageAngle();

		if (vrRangeMethod == null || calData == null || heightData == null || imageBearing == null) {
			return;
		}
		double pointBearing = imageBearing + (shorePoint.x - imageWidth/2) * calData.degreesPerUnit;
		
		double[] ranges = shoreManager.getSortedShoreRanges(MasterReferencePoint.getRefLatLong(), pointBearing);
		
		Double range = getshoreRange(ranges);

		if (range == null) {
			return;
		}
		// dip from horizon to this point. 
		Double angleTo = vrRangeMethod.getAngle(heightData.height, range);
		if (angleTo < 0) {
			// over horizon
			return; 
		}
		int y =  (int) (shorePoint.y - angleTo * 180 / Math.PI / calData.degreesPerUnit);
		double xD = shorePoint.x;
		horizonPoint1 = new Point(0, y + (int) (xD * Math.tan(getHorizonTilt() * Math.PI / 180)));
		xD = imageWidth - shorePoint.x;
		horizonPoint2 = new Point(imageWidth, y - (int) (xD * Math.tan(getHorizonTilt() * Math.PI / 180)));
	}
	
	private boolean newAnimalMeasurement(Point animalPoint) {


		if (measuredAnimals == null) {
			measuredAnimals = new ArrayList<VRMeasurement>();
		}
		
		switch (vrStatus) {
		case MEASURE_FROM_HORIZON:
			return newAnimalMeasurement_Horizon(animalPoint);
		case MEASURE_FROM_SHORE:
			return newAnimalMeasurement_Shore(animalPoint);
		}
		return false;
	}
	/**
	 * Can use horizon calculation since the shore data will have generated an 
	 * artificial horizon. 
	 * @param animalPoint Clicked point on image
	 * @return true if calculation completed and range accepted bu uper. 
	 */
	private boolean newAnimalMeasurement_Shore(Point animalPoint) {
		return newAnimalMeasurement_Horizon(animalPoint);
	}
	/** 
	 * @param animalPoint Clicked point on image
	 * @return true if calculation completed and range accepted bu uper. 
	 */
	private boolean newAnimalMeasurement_Horizon(Point animalPoint) {
		if (horizonPoint1 == null || horizonPoint2 == null) {
			return false;
		}
		VRCalibrationData calData = vrParameters.getCurrentCalibrationData();
		if (calData == null) {
			return false;
		}
		VRHeightData heightData = vrParameters.getCurrentheightData();
		if (heightData == null) {
			return false;
		}
		VRRangeMethod vrMethod = rangeMethods.getCurrentMethod();
		if (vrMethod == null) {
			return false;
		}
		
		candidateMeasurement = new VRMeasurement(horizonPoint1, horizonPoint2, animalPoint);
		candidateMeasurement.imageName = new String(getImageName());
		candidateMeasurement.imageAnimal = measuredAnimals.size();
		candidateMeasurement.calibrationData = calData.clone();
		candidateMeasurement.heightData = heightData.clone();
		candidateMeasurement.rangeMethod = vrMethod;
		

		// try to get an angle
//		AngleDataUnit heldAngle = null;
//		if (angleDataBlock != null) {
//			heldAngle = angleDataBlock.getHeldAngle();
//		}
//		if (heldAngle != null) {
//			candidateMeasurement.cameraAngle = heldAngle.correctedAngle;
//		}
		/*
		 * Held angles whould have been put straight into the imageAngle field. 
		 * Even if they haven't, it's possible that someone will have entered a
		 * value manually into the field, so try using it. 
		 */
		candidateMeasurement.cameraAngle = getImageAngle();
		
		double angle = candidateMeasurement.getAnimalAngleRadians(calData);
		double range = vrMethod.getRange(vrParameters.getCameraHeight(), angle);
		candidateMeasurement.distanceMeters = range;
		// see how accurate the measurement might be based on a single pixel error
		double range1 = vrMethod.getRange(vrParameters.getCameraHeight(), angle + Math.PI/180*calData.degreesPerUnit);
		double range2 = vrMethod.getRange(vrParameters.getCameraHeight(), angle - Math.PI/180*calData.degreesPerUnit);
		double error = Math.abs(range1-range2)/2;
		candidateMeasurement.pixelAccuracy = error;
		
		// calculate the angle correction
		int imageWidth = vrTabPanelControl.vrPanel.getImageWidth();
		double angCorr = (animalPoint.x - imageWidth/2) * calData.degreesPerUnit;
		candidateMeasurement.angleCorrection = angCorr;
		
		VRMeasurement newMeasurement = AcceptMeasurementDialog.showDialog(null, this, candidateMeasurement);
		if (newMeasurement != null) {
			measuredAnimals.add(newMeasurement);
			vrProcess.newRange(newMeasurement);
		}
		candidateMeasurement = null;
		
		return true;
	}
	
	private void newCalibration() {
		if (calibratePoint1 == null || calibratePoint2 == null) {
			return;
		}
		VRCalibrationData newCalibration = VRCalibrationDialog.showDialog(null, this, null);
		if (newCalibration != null){
			vrParameters.setCurrentCalibration(newCalibration);
			newSettings();
		}
		vrTabPanelControl.newCalibration();
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public Point getCalibratePoint1() {
		return calibratePoint1;
	}

	public void setCalibratePoint1(Point calibratePoint1) {
		this.calibratePoint1 = calibratePoint1;
	}

	public Point getCalibratePoint2() {
		return calibratePoint2;
	}

	public void setCalibratePoint2(Point calibratePoint2) {
		this.calibratePoint2 = calibratePoint2;
	}

	public Point getHorizonPoint1() {
		return horizonPoint1;
	}

	public void setHorizonPoint1(Point horizonPoint1) {
		this.horizonPoint1 = horizonPoint1;
	}

	public Point getHorizonPoint2() {
		return horizonPoint2;
	}

	public void setHorizonPoint2(Point horizonPoint2) {
		this.horizonPoint2 = horizonPoint2;
	}
	
	public Point getShorePoint() {
		return shorePoint;
	}

	public void setShorePoint(Point shorePoint) {
		this.shorePoint = shorePoint;
	}

	public ArrayList<VRMeasurement> getMeasuredAnimals() {
		return measuredAnimals;
	}

	private void clearPoints() {
		horizonPoint1 = horizonPoint2 = calibratePoint1 = calibratePoint2 = null;
		shorePoint = null;
		measuredAnimals = null;
		candidateMeasurement = null;
	}

	public VRMeasurement getCandidateMeasurement() {
		return candidateMeasurement;
	}
	
	private class AngleListener extends PamProcess {

		@Override
		public String getProcessName() {
			// TODO Auto-generated method stub
			return "Angle monitor";
		}

		VRControl vrControl;
		public AngleListener(VRControl vrControl) {
			super(vrControl, null);
			this.vrControl = vrControl;
		}
		
		@Override
		public void pamStart() {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void pamStop() {
			// TODO Auto-generated method stub
			
		}
		
		void sortAngleMeasurement () {
			if (angleDataBlock != null) {
				setParentDataBlock(null);
				angleDataBlock = null;
			}
			if (vrParameters.measureAngles) {
				angleDataBlock = (AngleDataBlock) PamController.getInstance().getDataBlock(AngleDataUnit.class, vrParameters.angleDataBlock);
				if (angleDataBlock != null) {
					setParentDataBlock(angleDataBlock);
				}
			}
		}

		@Override
		public void newData(PamObservable o, PamDataUnit arg) {
			if (o == angleDataBlock) {
				newAngles((AngleDataUnit) arg);
			}
		}
		
		private void newAngles(AngleDataUnit angleDataUnit) {
			if (angleDataUnit.held) {
				setImageAngle(angleDataUnit.correctedAngle);
			}
		}
	}

	public Double getImageAngle() {
		return imageAngle;
	}

	public void setImageAngle(Double shoreAngle) {
		this.imageAngle = shoreAngle;
		horizonPointsFromShore(horizonTilt);
		// and work out what the ranges are to the shore for this angle. 
		shoreRanges = shoreManager.getSortedShoreRanges(MasterReferencePoint.getRefLatLong(), shoreAngle);
		vrTabPanelControl.vrPanel.repaint();
		vrTabPanelControl.imageAnglePanel.setAngle(shoreAngle);
	}
	
	protected void stepImageAngle(double step) {
		Double currentAngle = getImageAngle();
		if (currentAngle == null) {
			currentAngle = 0.;
		}
		setImageAngle(PamUtils.constrainedAngle(currentAngle + step));
	}

	public double[] getShoreRanges() {
		return shoreRanges;
	}
	
	/**
	 * Get the shore range we want to use - not necessarily the closest. 
	 * @return shore range to use in VR calculations. 
	 */
	public Double getShoreRange() {
		return getshoreRange(shoreRanges);
	}
	
	Double getshoreRange(double[] ranges) {
		int want = 0;
		if (ranges == null) {
			return null;
		}
		if (vrParameters.ignoreClosest) {
			want = 1;
		}
		if (ranges.length < want+1) {
			return null;
		}
		return ranges[want];
	}

	/**
	 * Gets called when a second horizon point is added and then 
	 * calculates the horizon tilt, based on the two points. 
	 *
	 */
	private void calculateHorizonTilt() {
		if (horizonPoint1 == null ||horizonPoint2 == null) {
			return;
		}
		Point l, r;
		if (horizonPoint1.x < horizonPoint2.x) {
			l = horizonPoint1;
			r = horizonPoint2;
		}
		else {
			l = horizonPoint2;
			r = horizonPoint1;
		}
		double tilt = Math.atan2(-(r.y-l.y), r.x-l.x) * 180 / Math.PI;
		setHorizonTilt(tilt);
		vrTabPanelControl.imageAnglePanel.setTilt(tilt);
	}
	public double getHorizonTilt() {
		return vrTabPanelControl.imageAnglePanel.getTilt();
	}

	public void setHorizonTilt(double horizonTilt) {
		this.horizonTilt = horizonTilt;
		horizonPointsFromShore(horizonTilt);
	}
}
