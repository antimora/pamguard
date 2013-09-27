package Array;

import pamMaths.PamVector;
import depthReadout.DepthControl;
import depthReadout.DepthDataBlock;
import depthReadout.DepthDataUnit;
import GPS.GpsData;
import GPS.GpsDataUnit;
import PamController.PamController;
import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

/**
 * Straight Hydrophone Locator, uses GPS heading information from
 * GPS and assumes that the hydrophones are just sticking straight out
 * the back of the boat as though on a rigid stick. 
 * 
 *  <p> 
 *  If depth readout is installed, then the depth will be taken from the depth data for
 *  the appropriate time and also, more interestingly, the y coordinate of each hydrophone
 *  will be reduced according to cable angle based on a simple (straight) model of how the 
 *  hydrophone is lying. 
 * 
 * @author Doug Gillespie
 *
 */
public class StraightHydrophoneLocator extends SimpleHydrophoneLocator implements PamObserver {
	
	PamDataBlock<GpsDataUnit> gpsDataBlock;
	
	PamDataBlock hydrophoneDataBlock;
	
	GpsDataUnit lastGpsUnit;
	
	public StraightHydrophoneLocator(PamArray pamArray) {
		super(pamArray);
		setupGpsMonitor();
	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}
	
	@Override
	public double getPairAngle(long timeMilliseconds, int phone1, int phone2, int angleType) {
		double angle = super.getPairAngle(timeMilliseconds, phone1, phone2, angleType);
		if (angleType == HydrophoneLocator.ANGLE_RE_SHIP) return angle;
		if (angleType == HydrophoneLocator.ANGLE_RE_ARRAY) return angle;
		GpsDataUnit gpsDataUnit = findGpsDataUnit(timeMilliseconds);
		if (gpsDataUnit != null) {
			GpsData gpsData = gpsDataUnit.getGpsData();
			angle += gpsData.getHeading();
		}
		return PamUtils.constrainedAngle(angle);
	}
	
	// speed up the search, since it's likely to get called for
	// the same time for each phone in turn. 
	private GpsDataUnit lastFoundUnit;
	private long lastSearchTime = 0;
	GpsDataUnit findGpsDataUnit(long timeMilliseconds) {
		if (gpsDataBlock == null) return null;
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL) {
//			timeMilliseconds = PamCalendar.getTimeInMillis();
			return gpsDataBlock.getClosestUnitMillis(timeMilliseconds);
		}
		if (lastGpsUnit == null) return null;
		if (lastSearchTime == timeMilliseconds) return lastFoundUnit;
		if (timeMilliseconds >= lastGpsUnit.getTimeMilliseconds()) return lastGpsUnit;
		return gpsDataBlock.getClosestUnitMillis(timeMilliseconds);
	}


	@Override
	public LatLong getReferenceLatLong(long timeMilliseconds) {
		GpsDataUnit gpsDataUnit = findGpsDataUnit(timeMilliseconds);
		if (gpsDataUnit == null) {
			return new LatLong();
		}
		return gpsDataUnit.getGpsData();
	}

	@Override
	public double getArrayHeading(long timeMilliseconds, int phoneNo) {
		GpsDataUnit gpsDataUnit = findGpsDataUnit(timeMilliseconds);
		if (gpsDataUnit == null) {
			return 0;
		}
		return gpsDataUnit.getGpsData().getHeading();
	}

	public LatLong getPhoneLatLong(long timeMilliseconds, int phoneNo) {
		/*
		 * in the array basic settings, x is the distance along the beam and y
		 * is the distance ahead of the vessel - so need to use the vessel heading
		 * to get from the gps position to the positions of individual hydrophones
		 * IF no GPS data are available, then reference to 0,0.
		 */
		GpsData gpsData = null;
		GpsDataUnit gpsDataUnit = findGpsDataUnit(timeMilliseconds);
		if (gpsDataUnit != null) {
			gpsData = gpsDataUnit.getGpsData();
		}
		if (gpsData == null) {
			gpsData = new GpsData();
		}
		PamVector hVec = pamArray.getAbsHydrophoneVector(phoneNo);
//		Hydrophone h = pamArray.getHydrophone(phoneNo);
		if (hVec == null) return null;
		// start by seeing how far beyond the last gps position we're likely
		// to have travelled...
		double beyond = (timeMilliseconds - gpsData.getTimeInMillis()) / 1000.;
		beyond *= gpsData.getSpeedMetric(); 
		/*
		 * N.B. beyond will generally be +ve, h.getY() should be negative for a hydrophone towed 
		 * astern - note that the travelDistanceMeters function is in the direction of the
		 * vessel, so we simply add these two values together. 
		 */
//		double height = getPhoneHeight(timeMilliseconds, phoneNo);
		LatLong latLong = gpsData.travelDistanceMeters(gpsData.getHeading(), 
				beyond + hVec.getElement(1));
		latLong = latLong.travelDistanceMeters(gpsData.getHeading()+90, 
				hVec.getElement(0));
		latLong.setHeight(hVec.getElement(2));
		return latLong;
	}

	@Override
	public void notifyModelChanged(int changeType) {
		setupGpsMonitor();
	}
	
	@Override
	public double getPhoneHeight(long timeMilliseconds, int phoneNo) {
		DepthDataUnit ddu = findDepthDataUnit(timeMilliseconds);
		if (ddu == null) {
			return super.getPhoneHeight(timeMilliseconds, phoneNo);
		}
		DepthControl depthControl = ArrayManager.getArrayManager().getDepthControl();
		int iSensor = depthControl.getSensorForHydrophone(phoneNo);
		double[] depthData = ddu.getDepthData();
		if (iSensor < 0 || depthData == null || iSensor >= depthData.length) {
			return super.getPhoneHeight(timeMilliseconds, phoneNo);
		}
		return depthData[iSensor];
	}
	
	
	protected DepthDataUnit findDepthDataUnit(long timeMillis) {
		DepthDataBlock depthDataBlock = findDepthDataBlock();
		if (depthDataBlock == null) {
			return null;
		}
		return depthDataBlock.getClosestUnitMillis(timeMillis);
	}
	
	private DepthDataBlock findDepthDataBlock() {
		DepthControl depthControl = ArrayManager.getArrayManager().getDepthControl();
		if (depthControl == null) {
			return null;
		}
		return depthControl.getDepthDataBlock();
	}

	/**
	 * Get a corrected distance astern of the vessel based on the hydrophone being at some
	 * depth and the hydrophone cable being basically straight. Generally, this correection will
	 * be small when towing close to the surface, but will get large when the phone sinks. 
	 * @param timeMilliseconds time that the Y is required for
	 * @param phoneNo hydrophone number. 
	 * @return corrected distance astern of vessel. 
	 */
	public double getCorrectedYPos(long timeMilliseconds, int phoneNo) {
		double height = getPhoneHeight(timeMilliseconds, phoneNo);
		Hydrophone h = pamArray.getHydrophone(phoneNo);
		if (h == null) return 0;
		double y = h.getY();
//		depth = Math.max(0, Math.min(depth, Math.abs(y)));
		double y2 = Math.sqrt(y * y - height*height);
		if (y < 0) {
			y2 *= -1;
		}
		return y2;
	}
	
	public void setupGpsMonitor() {
		PamDataBlock newGpsBlock = PamController.getInstance().getDataBlock(GpsDataUnit.class, 0);
		if (newGpsBlock == gpsDataBlock) return; // no need to do anything
		gpsDataBlock = newGpsBlock;
		if (gpsDataBlock != null) {
			gpsDataBlock.addObserver(this);
		}
	}

	public String getObserverName() {
		return "Stright HydrophonePositioner";
	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		return 0;
	}

	public void noteNewSettings() {
		// TODO Auto-generated method stub
		
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

		if (o == gpsDataBlock) {
			lastGpsUnit = (GpsDataUnit) arg;
		}
		
	}

	@Override
	public String toString() {
		return "Straight / rigid hydrophone";
	}
}
