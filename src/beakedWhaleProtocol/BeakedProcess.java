package beakedWhaleProtocol;

import angleMeasurement.AngleDataBlock;
import angleMeasurement.AngleDataUnit;
import GPS.GpsDataUnit;
import PamController.PamController;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class BeakedProcess extends PamProcess {

	BeakedControl beakedControl;
	
	BeakedDataBlock beakedDataBlock;
	
	BeakedExperimentDataBlock beakedExperimentDataBlock;
	
	ShoreStationDataBlock shoreStationDataBlock;
	
	private Double latestAngle;
		
	AngleDataBlock angleDataBlock;
	
	public BeakedProcess(BeakedControl beakedControl) {
		super(beakedControl, null);
		this.beakedControl = beakedControl;
		
		addOutputDataBlock(beakedDataBlock = new BeakedDataBlock("Beaked Whale Experiment Locations", this));
		beakedDataBlock.setOverlayDraw(new BeakedGraphics(beakedControl));
		beakedDataBlock.SetLogging(new BeakedLogging(beakedControl, this));
		
		addOutputDataBlock(beakedExperimentDataBlock = new BeakedExperimentDataBlock("Old Beaked Whale Experiments", this));
		beakedExperimentDataBlock.setOverlayDraw(new BeakedExperimentGraphics(beakedControl));
		beakedExperimentDataBlock.SetLogging(new BeakedExperimentLogging(beakedExperimentDataBlock));
		
		addOutputDataBlock(shoreStationDataBlock = new ShoreStationDataBlock("Beaked Whale Shore Station", this));
		shoreStationDataBlock.setOverlayDraw(new ShoreStationGraphics(beakedControl));
		shoreStationDataBlock.SetLogging(new ShoreStationLogging(shoreStationDataBlock));
		
//		beakedControl.fluxgateWorldAngles.addMeasurementListener(new FluxgateListener());
	}

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub
		
	}
	
	protected void sortAnglreReadout() {
		if (angleDataBlock != null) {
			angleDataBlock.deleteObserver(this);
			angleDataBlock = null;
		}
		angleDataBlock = (AngleDataBlock) PamController.getInstance().getDataBlock(AngleDataUnit.class, 
				beakedControl.beakedParameters.angleDataSource);
		if (angleDataBlock != null) {
			angleDataBlock.addObserver(this);
		}
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {

		if (o == beakedControl.gpsDataBlock) {
			useGpsData((GpsDataUnit) arg);
		}
		if (o == angleDataBlock) {
			newAngles((AngleDataUnit) arg);
		}
	}
	
	private void useGpsData(GpsDataUnit gpsDataUnit) {
		BeakedExperimentData bed;
		if ((bed = beakedControl.currentExperiment) == null) {
			return;
		}
		if (bed.status == BeakedExperimentData.APPROACH_START) {
			double d2Start = gpsDataUnit.getGpsData().distanceToMetres(bed.trackStart);
			if (d2Start > lastDistance) { // now getting further away
				if (d2Start < maxDistFromStart && Math.abs(gpsDataUnit.getGpsData().
						getCourseOverGround() - bed.course) < maxCourseError) {// have 
					beakedControl.setExperimentStatus(BeakedExperimentData.ON_TRACK);
					distance2End = gpsDataUnit.getGpsData().distanceToMetres(bed.trackEnd);
				}
			}
			beakedControl.setEtaSeconds(d2Start / LatLong.MetersPerMile / gpsDataUnit.getGpsData().getSpeed() * 3600);
			lastDistance = d2Start;
			beakedControl.setDTG(lastDistance);
		}
		else if (bed.status == BeakedExperimentData.ON_TRACK) {
			double d2End = gpsDataUnit.getGpsData().distanceToMetres(bed.trackEnd);
			if (d2End > distance2End) { // getting further away
				if (d2End < maxDistFromStart) {
					beakedControl.setExperimentStatus(BeakedExperimentData.AUTOCOMPLETE);
				}
			}
			beakedControl.setEtaSeconds(d2End / LatLong.MetersPerMile / gpsDataUnit.getGpsData().getSpeed() * 3600);
			distance2End = d2End;
			beakedControl.setXTE(gpsDataUnit.getGpsData().getCrossTrackError(bed.trackStart, bed.trackEnd));
			beakedControl.setDTG(distance2End);
		}
	}
	
	// only does this if necessary
	protected void makeShoreDataUnit() {
		if (shoreStationDataBlock.needNew(beakedControl.beakedParameters.shoreStation, beakedControl.beakedParameters.shoreStationHeight)){
			ShoreStationDataUnit sdu = new ShoreStationDataUnit(PamCalendar.getTimeInMillis(),
					beakedControl.beakedParameters.shoreStation, beakedControl.beakedParameters.shoreStationHeight);
			shoreStationDataBlock.addPamData(sdu);
			System.out.println("Create shore station data unit");
		}
	}
	
	long lastUpdate = 0;
	protected void newAngles(AngleDataUnit angleDataUnit) {

		long now = PamCalendar.getTimeInMillis();
		if (now - lastUpdate > beakedControl.beakedParameters.angleUpdateInterval * 1000) {
			// get the latest data unit and update the angle in it. 
			ShoreStationDataUnit sdu = shoreStationDataBlock.getLastUnit();
			sdu.setMeasuredAngle(angleDataUnit.correctedAngle);
			sdu.setTimeMilliseconds(now);
			setLatestAngle(angleDataUnit.correctedAngle);
			shoreStationDataBlock.updatePamData(sdu, now);
			lastUpdate = now;
//			beakedControl.beakedSidePanel.newAngle(angleDataUnit.rawAngle, angleDataUnit.correctedAngle);
		}
	}
	
	private double closest2Start = Double.MAX_VALUE;
	private double maxDistFromStart = 500; 
	private double maxCourseError = 45;
	private double lastDistance;
	private double distance2End;
	protected void newExperiment() {
		closest2Start = Double.MAX_VALUE;
		lastDistance = Double.MAX_VALUE;
		distance2End = Double.MAX_VALUE;
	}

	public Double getLatestAngle() {
		return latestAngle;
	}

	public void setLatestAngle(Double latestAngle) {
		this.latestAngle = latestAngle;
	}

	public AngleDataUnit getHeldAngle() {
		if (angleDataBlock == null) {
			return null;
		}
		return angleDataBlock.getHeldAngle();
	}
}
