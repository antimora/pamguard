package Array;

import java.util.ListIterator;

import pamMaths.PamVector;

import GPS.GpsData;
import GPS.GpsDataUnit;
import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;

/**
 * 
 * A threading hydrophone is one that exactly follows the track of the ship.
 * It works by holding GPS information, then for any time, works out where the 
 * ship was, then works back to where the hydrophone would have been. 
 * <p>
 * Extends StrightHydrophoneLocator since that already handled accessing GPS
 * data.  
 * 
 * @author Doug Gillespie
 *
 */
public class ThreadingHydrophoneLocator extends StraightHydrophoneLocator {
	
	private Object headingSynchObject = new Object();
	
	private double storedHeading;
	private long storedHeadingTime;
	private int storedHeadingPhone;

	public ThreadingHydrophoneLocator(PamArray pamArray) {
		super(pamArray);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "Threading hydrophone";
	}

	@Override
	synchronized public LatLong getPhoneLatLong(long timeMilliseconds, int phoneNo) {
		/*
		 * in the array basic settings, x is the distance along the beam and y
		 * is the distance ahead of the vessel - so need to use the vessel heading
		 * to get from the gps position to the positions of individual hydrophones
		 * IF no GPS data are available, then reference to 0,0.
		 */
		GpsData gpsData = null, g = null;
		GpsDataUnit gpsDataUnit = null;
		double distanceToTravel = 0;

		synchronized (headingSynchObject) {
			storedHeadingTime = 0;
			storedHeadingPhone = -1;
			storedHeading = 0;
		}
		
		PamVector hVec;
		if (gpsDataBlock == null) {
			return new LatLong(0,0);
		}
		synchronized (gpsDataBlock) {
			ListIterator<GpsDataUnit> gpsIterator = gpsDataBlock.getListIterator(PamDataBlock.ITERATOR_END);
			// work back until we find the closest gps unit.
			// do all this here, since we want to hold on to the iterator to carry
			// on working backwards.
			gpsDataUnit = gpsDataBlock.getPreceedingUnit(gpsIterator, timeMilliseconds);
			if (gpsDataUnit == null) {
				gpsDataUnit = gpsDataBlock.getClosestUnitMillis(timeMilliseconds);
			}
			if (gpsDataUnit != null) {
				gpsData = gpsDataUnit.getGpsData();
			}
			if (gpsData == null) {
				gpsData = new GpsData();
			}
			/*
			 * That's told us where the ship was, now need to work back and 
			 * find out where the actual hydrophone was at that time
			 * The easiest way to do this is based on vessel speed and hope
			 * it didn't change too much, but a better way, used here, is to 
			 * tot up distances between gps's, which have been conveniently
			 * stored in the GpsData. 
			 */
			hVec = pamArray.getAbsHydrophoneVector(phoneNo);
			if (hVec == null) {
				return null;
			}
//			h = pamArray.getHydrophone(phoneNo);
//			if (h == null) return null;

			if (hVec.getElement(1) > 0 || gpsDataUnit == null) {
				// the hydrophone is in front of the boat, so assume a rigid system. 
				LatLong latLong = gpsData.travelDistanceMeters(gpsData.getCourseOverGround(), 
						hVec.getElement(1));
				latLong = latLong.travelDistanceMeters(gpsData.getCourseOverGround()+90, 
						hVec.getElement(0));

				return latLong;
			}

//			double  = getPhoneHeight(timeMilliseconds, phoneNo);

			// start by seeing how far beyond the last gps position we're likely
			// to have travelled...
			double beyond = (timeMilliseconds - gpsData.getTimeInMillis()) / 1000. * gpsData.getSpeedMetric();
			distanceToTravel = -hVec.getElement(1) - beyond;
			//now work back through gps units until we're at the GPS unit in front of the one we want
			GpsDataUnit gu = gpsDataUnit;
			g = gpsDataUnit.getGpsData();
			while (gpsIterator.hasPrevious()) {
				g = gu.getGpsData();
				if (distanceToTravel > g.getDistanceFromLast()) {
					distanceToTravel -= g.getDistanceFromLast();
					gu = gpsIterator.previous();
				}
				else {
					break;
				}
			}
		}
		// distance to travel should now be a small positive number.
		// need to travel backwards a bit more, so need sign change in next function call ...

		LatLong latLong = g.travelDistanceMeters(g.getCourseOverGround(), -distanceToTravel);
		latLong = latLong.travelDistanceMeters(gpsData.getCourseOverGround()+90, hVec.getElement(0));
		latLong.setHeight(hVec.getElement(2));
		
		synchronized (headingSynchObject) {
			storedHeadingTime = timeMilliseconds;
			storedHeadingPhone = phoneNo;
			storedHeading = g.getHeading();
		}

		return latLong;
	}
	
	
	@Override
	public double getArrayHeading(long timeMilliseconds, int phoneNo) {
		synchronized (this) {
			if (storedHeadingTime != timeMilliseconds || storedHeadingPhone != phoneNo) {
				getPhoneLatLong(timeMilliseconds, phoneNo);
//				storedHeading = super.getArrayHeading(timeMilliseconds, phoneNo);
//				storedHeadingTime = timeMilliseconds;
//				storedHeadingPhone = phoneNo;
			}
			return storedHeading;
		}
	}

	/*
	 * Needs to be a bit more sophisticated too
	 * in order to cope with the threading angle
	 *  (non-Javadoc)
	 * @see Array.HydrophoneLocator#getPairAngle(long, int, int, int)
	 */
	@Override
	public double getPairAngle(long timeMilliseconds, int phone1, int phone2, int angleType) {
		/*
		 * One way would be to get the latlong of each hydrophone and then recaluclate 
		 * the bearing. 
		 * What I'm going to do is take the mean y position of the two phones 
		 * and get the hydrophone heading at that position.
		 */

		// first get the angle relative to the array - this is dead easy. 
		double angle = super.getPairAngle(timeMilliseconds, phone1, phone2, HydrophoneLocator.ANGLE_RE_ARRAY);


		// all we need to do for ANGLE_RE_ARRAY option, so just return
		if (angleType == HydrophoneLocator.ANGLE_RE_ARRAY) return angle;
		GpsDataUnit arrayGpsUnit;
		GpsData arrayGpsData, shipGpsData;
		if (gpsDataBlock == null) {
			return angle;
		}
		synchronized (gpsDataBlock) {
			// now we need the location of the ship. 
			ListIterator<GpsDataUnit> gpsIterator = gpsDataBlock.getListIterator(PamDataBlock.ITERATOR_END);
			GpsDataUnit shipGpsDataUnit = gpsDataBlock.getPreceedingUnit(timeMilliseconds);
			if (shipGpsDataUnit == null) return angle;
			shipGpsData = shipGpsDataUnit.getGpsData();

			Hydrophone h1 = pamArray.getHydrophone(phone1);
			if (h1 == null) return angle;
			Hydrophone h2 = pamArray.getHydrophone(phone1);
			if (h2 == null) return angle;
			//		DepthDataUnit ddu = findDepthDataUnit(timeMilliseconds);

			double meanY = (h1.getY() + h2.getY()) / 2.;
			if (meanY > 0) {
				// it's in front of the ship so use the rigid locator
				if (angleType == HydrophoneLocator.ANGLE_RE_SHIP) return angle;
				else return angle + shipGpsData.getCourseOverGround();
			}
			// otherwise work back to closest ...
			double beyond = (timeMilliseconds - shipGpsData.getTimeInMillis()) / 1000. * shipGpsData.getSpeedMetric();
			double distanceToTravel = -meanY - beyond;
			//now work back through gps units until we're at the GPS unit in front of the one we want
			arrayGpsUnit = shipGpsDataUnit;
			arrayGpsData = shipGpsDataUnit.getGpsData();
			while (gpsIterator.hasPrevious()) {
				//		while (arrayGpsUnit != null) {
				arrayGpsData = arrayGpsUnit.getGpsData();
				if (distanceToTravel > arrayGpsData.getDistanceFromLast()) {
					distanceToTravel -= arrayGpsData.getDistanceFromLast();
					arrayGpsUnit = gpsIterator.previous();
				}
				else {
					break;
				}
			}
		}

		if (angleType == HydrophoneLocator.ANGLE_RE_SHIP) {
			// add in the difference between where the ship is heading and where the array is heading
			return angle + arrayGpsData.getCourseOverGround() - shipGpsData.getCourseOverGround();
		}
		else {
			// return angle relative to North. 
			return angle + arrayGpsData.getCourseOverGround();
		}

	}
}
