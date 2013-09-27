package GPS;

import java.util.ListIterator;

import nmeaEmulator.EmulatedData;
import nmeaEmulator.NMEAEmulator;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

/**
 * Add a bit of extra functionality to GPSDataBlock so 
 * that it can check new GPS data are 'reasonable'
 * and flag bad ones. 
 * 
 * @author Doug
 *
 */
public class GPSDataBlock extends PamDataBlock<GpsDataUnit> implements NMEAEmulator {

	private ProcessNmeaData nmeaProcess;

	/**
	 * Max reasonable speed in km per hour
	 */
	private double reasonableSpeed = 100; 

	/**
	 * Reasonable time to wait before believing anything
	 */
	private double reasonableResetTime = 120;

	/**
	 * Max number of objects to look at before deciding it's OK anyway. 
	 */
	private int reasonableTries = 10;

	public GPSDataBlock(PamProcess process) {
		super(GpsDataUnit.class, "GPS Data", process, 1);
	}

	@Override
	public void addPamData(GpsDataUnit gpsDataUnit) {
		boolean r = isReasonable(gpsDataUnit);
		if (r == false) {
			gpsDataUnit.getGpsData().setDataOk(false);
		}
		super.addPamData(gpsDataUnit);
	}

	/**
	 * Check a GPS entry is reasonable - i.e. doesn't jump a huge
	 * distance from the preceeding entry.
	 * @param gpsDataUnit
	 * @return true if reasonable. 
	 */
	public boolean isReasonable(GpsDataUnit gpsDataUnit) {

		GpsData thisData, thatData;
		thisData = gpsDataUnit.getGpsData();

		if (thisData.isDataOk() == false) {
			return false;
		}
		
		synchronized (this) {
			int nTries = 0;
			double speed, timeSecs;
			ListIterator<GpsDataUnit> gpsIterator = getListIterator(PamDataBlock.ITERATOR_END);
			while (gpsIterator.hasPrevious()) {
				thatData = gpsIterator.previous().getGpsData();
				// ok if we've tried to many data. 
				if (nTries++ > reasonableTries) {
					return true;
				}
				// ignore gps points flagged as bad. 
				if (thatData.isDataOk() == false) {
					continue;
				}
				// OK if it's gone too far back in time
				timeSecs = (thisData.getTimeInMillis() - thatData.getTimeInMillis()) / 1000.; 
				if (timeSecs > reasonableResetTime) {
					return true;
				}
				speed = thisData.distanceToMetres(thatData) / timeSecs * 3.6; // speed in km / hr
				if (speed > reasonableSpeed) {
					return false;
				}
				else {
					return true;
				}
			}
		}
		return true;
	}
	
	private ListIterator<GpsDataUnit> emulatorIterator;
	private long emulatorTimeOffset;
	private EmulatedData readyGGAData;
	@Override
	public EmulatedData getNextData() {
		/*
		 * If the last time this was called, it returned RMC data
		 * this time it will return the pre prepared GGA data before
		 * the next call which will return RMC data again. 
		 */
		if (readyGGAData != null) {
			EmulatedData dd = readyGGAData;
			readyGGAData = null;
			return dd;
		}
		if (emulatorIterator.hasNext() == false) {
			return null;
		}
		GpsDataUnit gpsUnit =  emulatorIterator.next();
		GpsData gpsData = gpsUnit.getGpsData().clone();
		long dataTime = gpsData.getTimeInMillis();
		long simTime = dataTime + emulatorTimeOffset;
		gpsData.setTimeInMillis(simTime);
		readyGGAData = new EmulatedData(dataTime, simTime, gpsData.gpsDataToGGA(4));
		return new EmulatedData(dataTime, simTime, gpsData.gpsDataToRMC(4));
	}

	@Override
	public boolean prepareDataSource(long[] timeLimits, long timeOffset) {
		emulatorIterator = this.getListIterator(0);
		emulatorTimeOffset = timeOffset;
		return true;
	}

}
