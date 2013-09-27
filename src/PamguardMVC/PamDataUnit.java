/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package PamguardMVC;

import pamMaths.PamVector;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;
import binaryFileStorage.DataUnitFileInformation;

import Acquisition.AcquisitionParameters;
import Acquisition.AcquisitionProcess;
import Array.ArrayManager;
import Array.HydrophoneLocator;
import Array.PamArray;
import GPS.GpsData;
import GPS.GpsDataUnit;
import PamController.PamController;
import PamDetection.AbstractDetectionMatch;
import PamDetection.AbstractLocalisation;
import PamDetection.AcousticDataUnit;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;

/**
 * @author Doug Gillespie
 *         <p>
 *         Class for units of PAM data.
 *         <p>
 *         PamDataUnit's are held in ArrayLists within PamDataBlocks.
 *         <p>
 *         When a PamDataUnit is added to a PamDataBlock any PamProcesses that
 *         subscribe to that PamDataBlock receive a notification and can retrieve the
 *         PamDataUnits from the block.
 *        <p>
 *        Any data derived from acoustic data should subclass from AcousticDataUnit
 * 
 * @see PamguardMVC.PamDataBlock
 * @see AcousticDataUnit
 */
abstract public class PamDataUnit implements Comparable<PamDataUnit>{

	/**
	 * time the NewPamDataUnit was created based using standard Java time
	 */
	protected long timeMilliseconds;
	
	/**
	 * Absolute block index, needed for searches once 
	 * NPDU's start getting deleted off the front of the storage
	 */
	protected int absBlockIndex;
	
	/**
	 * Reference to parent data block
	 */
	private PamDataBlock parentDataBlock;
	
//	public GpsDataUnit gpsDataUnit;

	/**
	 * map of channels used in the data. <p>
	 * N.B the PamDataBlock also has a channelBitmap. The channelBitmap in
	 * a PamDataBlock is a list of channels that might be present in the
	 * data units. The channelBitmap in the data unit represents channels 
	 * that are actually present in that data unit. For instance, if sampling 
	 * 2 channels (ch0 and ch1) of raw audio data, the channelBitmap in the 
	 * data block would equal 3, but the channel maps in the data units (which 
	 * contain one channel of data each) will alternate between 1 and 2. 
	 * <p> note that these are the software channels and that there may not be a 1:1 
	 * relationship between software channels and hydrophones. 
	 * <p>
	 * This parameter is included in PamDataUnit and not in the subclass AcousticDataUnit
	 * since it is sometimes needed by non acoustic data. 
	 */
	protected int channelBitmap;
	
	/**
	 * Counter which increases if the data are altered and re-sent around PAMGUARD
	 */
	private int updateCount = 0;
	
	/**
	 * time of the last update
	 */
	private long lastUpdateTime = 0;
	
	/**
	 * Index of last entry into database - what will happen if the data
	 * are written into > 1 column ?
	 */
	private int databaseIndex;
	
	/**
	 * Index of any database unit that this updated. 
	 */
	private int databaseUpdateOf;
	
	private DataUnitFileInformation dataUnitFileInformation;

	/**
	 * Localisation information
	 */
	protected AbstractLocalisation localisation = null;
	

	public void setAbsBlockIndex(int absBlockIndex) {
		this.absBlockIndex = absBlockIndex;
	}


	public void setParentDataBlock(PamDataBlock parentDataBlock) {
		this.parentDataBlock = parentDataBlock;
	}


	public void setTimeMilliseconds(long timeMilliseconds) {
		this.timeMilliseconds = timeMilliseconds;
	}

	public PamDataUnit(long timeMilliseconds) {
		super();
		this.timeMilliseconds = timeMilliseconds;
		this.parentDataBlock = null;
	}

	public int getAbsBlockIndex() {
		return absBlockIndex;
	}

	public PamDataBlock getParentDataBlock() {
		return parentDataBlock;
	}

	public long getTimeMilliseconds() {
		return timeMilliseconds;
	}
	
	public void updateDataUnit(long updateTime) {
		updateCount++;
		this.lastUpdateTime = updateTime;
	}

	/**
	 * Do a clear of update count after a database save.
	 */
	public void clearUpdateCount() {
		updateCount = 0;;
	}
	
	/**
	 * @return the number of times the data unit has been updated. 
	 */
	public int getUpdateCount() {
		return updateCount;
	}


	public int getChannelBitmap() {
		return channelBitmap;
	}


	public void setChannelBitmap(int channelBitmap) {
		this.channelBitmap = channelBitmap;
	}

	

	// return the reference position for this detection. 
	// Only ever calculate this one, unless recaluclate 
	// flag is set to true. 
	protected GpsData oLL = null;
	private double[] pairAngles = null;
	
	/**
	 * Get the latlong of the mean hydrophone position at the time of 
	 * this detection. 
	 * @param recalculate
	 * @return Lat long of detection origin (usually the position of the reference hydrophone at time of detection)
	 */
	public GpsData getOriginLatLong(boolean recalculate) {
		if (oLL == null || recalculate) {
			calcOandAngles();
		}
		return oLL;
	}
	
	public void setOriginLatLong(GpsData oll) {
		oLL = oll;
	}


	/**
	 * Return the angle between pairs of hydrophones. For a n channel detection, 
	 * n-1 pair angles are calculated, each being the bearing, realtive to north, from the 
	 * pair+1th hydrophone TO the 0th hydrophone (i.e. for a 2 channel array, there is 
	 * one pair calculated and it's from channel 1 to channel 0, which is the most
	 * useful. If other inter-pair angles are required, then it should easy to
	 * calculate them from these values.   
	 * @param pair
	 * @param recalculate
	 * @return angle clockwise from North in degrees. 
	 */
	public double getPairAngle(int pair, boolean recalculate) {
		if (pairAngles == null || recalculate) {
			calcOandAngles();
		}
		if (pairAngles.length > pair) {
			return pairAngles[pair];
		}
		return Double.NaN;
	}
	
	/**
	 * Get the hydrophone heading for the first hydrophone indluded in the 
	 * detection. 
	 * @param reacalculate force recalculation
	 * @return hydrophone heading
	 */
	public double getHydrophoneHeading(boolean recalculate) {
		if (hydrophoneHeading == null || recalculate) {
			clacHeadingandOrigin();
		}
		if (hydrophoneHeading != null) {
			return hydrophoneHeading;
		}
		return Double.NaN;
	}
	
	/**
	 * Do the actual calculation of hydrophone heading and 
	 * it's position at the time of the detection. 
	 */
	private void clacHeadingandOrigin() {
		// TODO Auto-generated method stub
		int nPhones = PamUtils.getNumChannels(channelBitmap);
		GpsData gpsData;
		if (nPhones == 0) {
			oLL = gpsData = getGpsPosition();
			if (gpsData != null) {
				hydrophoneHeading = gpsData.getHeading();
			}
			return;
		}
		ArrayManager arrayManager = ArrayManager.getArrayManager();
		PamArray array = arrayManager.getCurrentArray();
		HydrophoneLocator hydrophoneLocator = array.getHydrophoneLocator();
		// turn channel numbers into hydrophone numbes.
		// to do this we need the parameters from the acquisition process !
		// if this is not available, then assume 1:1 mapping and get on with it.
		AcquisitionProcess daqProcess;
		AcquisitionParameters daqParams = null;
		try {
			daqProcess = (AcquisitionProcess) this.getParentDataBlock().getParentProcess().getSourceProcess();
			daqParams = daqProcess.getAcquisitionControl().acquisitionParameters;
		}
		catch (Exception ex) {
			daqProcess = null;
			daqParams = null;
		}
//		int nChan = PamUtils.getNumChannels(channelBitmap);
		
		int phone = PamUtils.getNthChannel(0, channelBitmap);
		if (daqParams != null) {
			phone = daqParams.getHydrophone(phone);
		}
		if (phone < 0) return;
		// seems like we've found a hydrophone ...
		LatLong phoneLatLong = hydrophoneLocator.getPhoneLatLong(getTimeMilliseconds(), phone);
		hydrophoneHeading = hydrophoneLocator.getArrayHeading(getTimeMilliseconds(), phone);
	}

	Double hydrophoneHeading = null;
	
	private void calcOandAngles() {


		int nPhones = PamUtils.getNumChannels(channelBitmap);
		if (nPhones == 0) {
			oLL = getGpsPosition();
			return;
		}
		
		ArrayManager arrayManager = ArrayManager.getArrayManager();
		PamArray array = arrayManager.getCurrentArray();
		HydrophoneLocator hydrophoneLocator = array.getHydrophoneLocator();
		// turn channel numbers into hydrophone numbes.
		// to do this we need the parameters from the acquisition process !
		// if this is not available, then assume 1:1 mapping and get on with it.
		AcquisitionProcess daqProcess;
		AcquisitionParameters daqParams = null;
		try {
			daqProcess = (AcquisitionProcess) this.getParentDataBlock().getParentProcess().getSourceProcess();
			daqParams = daqProcess.getAcquisitionControl().acquisitionParameters;
		}
		catch (Exception ex) {
			daqProcess = null;
			daqParams = null;
		}
		int nChan = PamUtils.getNumChannels(channelBitmap);
		pairAngles = new double[nChan - 1];
		int phone;
		double totalLat = 0, totalLong = 0, totalHeight = 0;
		LatLong phoneLatLong;
		LatLong firstLatLong = null;
		int firstPhone;
		for (int i = 0; i < nChan; i++) {
			phone = PamUtils.getNthChannel(i, channelBitmap);
			if (daqParams != null) {
				phone = daqParams.getHydrophone(phone);
			}
			if (phone < 0) {
				continue;
			}
			// seems like we've found a hydrophone ...
			phoneLatLong = hydrophoneLocator.getPhoneLatLong(getTimeMilliseconds(), phone);
			if (phoneLatLong == null) {
//				System.out.println("Can't find phone lat long for time " + 
//						PamCalendar.formatDateTime(getTimeMilliseconds()));
				phoneLatLong = hydrophoneLocator.getPhoneLatLong(getTimeMilliseconds(), phone);
				return;
			}
			if (i == 0) {
				firstPhone = phone;
				firstLatLong = phoneLatLong;
			}
			else{
				pairAngles[i-1] = phoneLatLong.bearingTo(firstLatLong);
//				pairAngles[i-1] = hydrophoneLocator.getPairAngle(getTimeMilliseconds(), phone, 
//						firstPhone, HydrophoneLocator.ANGLE_RE_NORTH);
			}
			totalLat += phoneLatLong.getLatitude();
			totalLong += phoneLatLong.getLongitude();
			totalHeight += phoneLatLong.getHeight();
		}
		if (nPhones == 0) {
			// return the ship GPS for that time.
			oLL = getGpsPosition();
		}
		else {
			oLL = new GpsData(totalLat / nPhones, totalLong / nPhones, totalHeight / nPhones, getTimeMilliseconds());
		}
//		if (nPhones >= 2) {
//			pairAngles = new double[nPhones - 1];
//			phone = PamUtils.getNthChannel(0, channelBitmap);
//			int firstPhone = daqParams.getHydrophone(phone);
//			for (int i = 1; i < nChan; i++) {
//				phone = PamUtils.getNthChannel(0, channelBitmap);
//				phone = daqParams.getHydrophone(phone);
//				pairAngles[i-1] = hydrophoneLocator.getPairAngle(timeMilliseconds, phone, firstPhone, HydrophoneLocator.ANGLE_RE_NORTH);
//			}
//		}
	}
	/**
	 * Used when no hydrophone information is specified to get the nearest ships GPS position.
	 * @return GPS data closest to the time of the detection 
	 */
	protected GpsData getGpsPosition() {
		PamDataBlock<GpsDataUnit> gpsDataBlock = PamController.getInstance().getDataBlock(GpsDataUnit.class, 0);
		if (gpsDataBlock == null) return null;
		GpsDataUnit gpsDataUnit =  gpsDataBlock.getPreceedingUnit(getTimeMilliseconds());
		if (gpsDataUnit == null) { // get the first one anyway - it may be close enough !
			gpsDataUnit = gpsDataBlock.getFirstUnit();
		}
		if (gpsDataUnit == null) {
			return null;
		}
		return gpsDataUnit.getGpsData();
	}


	public long getLastUpdateTime() {
		return lastUpdateTime;
	}


	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}


	public int getDatabaseIndex() {
		return databaseIndex;
	}


	public void setDatabaseIndex(int databaseIndex) {
		this.databaseIndex = databaseIndex;
	}


	public int getDatabaseUpdateOf() {
		return databaseUpdateOf;
	}


	public void setDatabaseUpdateOf(int databaseUpdateOf) {
		this.databaseUpdateOf = databaseUpdateOf;
	}


	public DataUnitFileInformation getDataUnitFileInformation() {
		return dataUnitFileInformation;
	}


	public void setDataUnitFileInformation(
			DataUnitFileInformation dataUnitFileInformation) {
		this.dataUnitFileInformation = dataUnitFileInformation;
	}

	@Override
	public int compareTo(PamDataUnit o) {

		/**
		 * Can't just to minus since long might wrap when compared to 
		 * Integer. 
		 * So use the compareTo embedded in the Long class
		 */
		Long thisTime = new Long(timeMilliseconds);
		return thisTime.compareTo(o.getTimeMilliseconds());
	}
	
	
	//localisation capability
	
	/**
	 * @return Returns the localisation.
	 */
	public AbstractLocalisation getLocalisation() {
		return localisation;
	}
	
	/**
	 * @return Returns the large aperture localisation.
	 */
	public AbstractDetectionMatch getDetectionMatch() {
		return null;
	}
	
	/**
	 * @return Returns the large aperture localisation.
	 */
	public AbstractDetectionMatch getDetectionMatch(int type) {
		return null;
	}
	
	/**
	 * @param localisation The localisation to set.
	 */
	public void setLocalisation(AbstractLocalisation localisation) {
		this.localisation = localisation;
//		updateCount++;
	}
	
	/**
	 * Return an html formatted summary string
	 * describing the detection which can be 
	 * used in tooltips anywhere in PAMGuard. 
	 * @return summary string 
	 */
	public String getSummaryString() {
		String str = "<html>";
		if (parentDataBlock != null) {
			str += parentDataBlock.getDataName() + "<p>";
		}
		str += PamCalendar.formatDateTime(timeMilliseconds) + "<p>";
		if (channelBitmap > 0) {
			str += "Channels: " + PamUtils.getChannelList(channelBitmap) + "<p>";
		}
		if (databaseIndex > 0) {
			str += "Database Index : " + databaseIndex + "<p>";
		}
		if (localisation != null) {
//			double[] angles = localisation.getAngles();
//			double bearingRef = localisation.getBearingReference();
			PamVector[]	worldVecs =	localisation.getWorldVectors();
			if (worldVecs != null && worldVecs.length > 0) {
				double angle = 90.-Math.toDegrees(Math.atan2(worldVecs[0].getElement(1), Math.atan(worldVecs[0].getElement(0))));
				str += String.format("Angle %3.1f\u00B0", angle);
				if (worldVecs.length >= 2) {
					double angle2 = 90.-Math.toDegrees(Math.atan2(worldVecs[1].getElement(1), Math.atan(worldVecs[1].getElement(0))));
					if (Math.abs(angle2-angle) > 0.1) {
						str += String.format(" (or %3.1f\u00B0)", angle);
					}
				}
				str += " re. Array<p>";
			}
			worldVecs = localisation.getRealWorldVectors();
			if (worldVecs != null && worldVecs.length > 0) {
				double angle = 90.-Math.toDegrees(Math.atan2(worldVecs[0].getElement(1), Math.atan(worldVecs[0].getElement(0))));
				str += String.format("Bearing %3.1f\u00B0", angle);
				if (worldVecs.length >= 2) {
					double angle2 = 90.-Math.toDegrees(Math.atan2(worldVecs[1].getElement(1), Math.atan(worldVecs[1].getElement(0))));
					if (Math.abs(angle2-angle) > 0.1) {
						str += String.format(" (or %3.1f\u00B0)", angle);
					}
				}
				str += " re. North<p>";
			}
		}
		
		return str;
	}
	
	
	
	/**
	 * Add in this same capability for target motion analysis here.
	 */

}
