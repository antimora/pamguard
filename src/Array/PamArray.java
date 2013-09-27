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

package Array;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import pamMaths.PamVector;

import GPS.GpsData;
import GPS.GpsDataUnit;
import Map.MasterReferencePoint;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;

/**
 * 
 * @author Doug Gillespie
 *         <p>
 *         Contains information on the hydrophone array. Individual hydrophones
 *         ar not given channel numbers - channel numbers are the position of
 *         the hydrophone in the arraylist.
 *         <p>
 *         This class should eventually be extended so that Hydrophones
 *         positions can be updated dynamically and functions added to give
 *         distances / angles between them, interpolate the hydrophones position
 *         based on the ships track, etc.
 * @see Array.Hydrophone
 * @see Array.PamArray
 */
public class PamArray implements Serializable, Cloneable {

	static public final int ARRAY_TYPE_TOWED = 0;
	static public final int ARRAY_TYPE_STATIC = 1;

	public static final int HEADING_TYPE_NONE = 0;
	public static final int HEADING_TYPE_TRUE = 1;
	public static final int HEADING_TYPE_MAGNETIC = 2;
	public static final int HEADING_TYPE_SHIP = 3;

	public static final int TILT_TYPE_NONE = 0;
	public static final int TILT_TYPE_UPDOWN = 1;
		
	public static final long serialVersionUID = 1;
	
	private ArrayList<Streamer> streamers = new ArrayList<Streamer>();
	
	private ArrayList<Hydrophone> hydrophoneArray = new ArrayList<Hydrophone>();

	private double speedOfSound = 1500;
	
	private double speedOfSoundError = 0;
	
	private String arrayName;
	
	private String arrayFile;
		
	private LatLong fixedLatLong = new LatLong();

	private int tiltType = TILT_TYPE_NONE;
	
	private int headingType = HEADING_TYPE_NONE;
	
	private int arrayType = ARRAY_TYPE_TOWED;
	
	/**
	 * Array shape - point, line, plane, etc. 
	 */
	private int arrayShape = -1;
	
	private int arrayLocator;
	
	transient private PamDataBlock<GpsDataUnit> fixedPointReferenceBlock;
	
	transient private GpsDataUnit fixedPointDataUnit;

	transient private HydrophoneLocator hydrophoneLocator;
//	private static PamArray singleInstance;

	public PamArray(String arrayName, int arrayType) {
		super();
		setArrayType(arrayType);
		this.arrayName = arrayName;
	}
	
	public void notifyModelChanged(int changeType) {
		if (hydrophoneLocator != null) {
			hydrophoneLocator.notifyModelChanged(changeType);
		}
	}
//
//	public static PamArray getArray () {
//		if (singleInstance == null) {
//			singleInstance = new PamArray();
//		}
//		return singleInstance;
//	}

	public int addHydrophone(Hydrophone hydrophone) {
		hydrophoneArray.add(hydrophone.getID(), hydrophone);
		checkHydrophoneIds();
		return hydrophoneArray.indexOf(hydrophone);
	}
	
	public boolean removeHydrophone(Hydrophone hydrophone) {
		if (hydrophoneArray.remove(hydrophone)){
			checkHydrophoneIds();
			return true;
		}
		return false;
	}
	
	public int updateHydrophone(int oldIndex, Hydrophone hydrophone) {
		//hydrophoneArray.add(hydrophone);
		// remove it and put it back again
		hydrophoneArray.remove(oldIndex);
		return addHydrophone(hydrophone);
	}

	/**
	 * Get a specific hydrophone from the list. 
	 * @param iHydrophone hydrophone number
	 * @return hydrophone or null if outwith the list. 
	 */
	public Hydrophone getHydrophone(int iHydrophone) {
		if (iHydrophone < 0 || iHydrophone >= hydrophoneArray.size()) {
			return null;
		}
		return hydrophoneArray.get(iHydrophone);
	}

	public void clearArray() {
		hydrophoneArray.clear();
	}

	public ArrayList<Hydrophone> getHydrophoneArray() {
		return hydrophoneArray;
	}
	
	/**
	 * Gets the size of the hydrophone array which is implicitly
	 * the hydrophone count.
	 *  
	 * @return The values reflecting the size of the hydrophone array
	 * with 0 being zero or a null object reference.
	 */
	public int getHydrophoneCount() {
		if (hydrophoneArray == null) return 0;
		return hydrophoneArray.size();
	}
	
	/**
	 * 
	 * @param iStreamer streamer Id
	 * @return number of hydrophone elements associated with that streamer. 
	 */
	public int getStreamerHydrophoneCount(int iStreamer) {
		int n = 0;
		for (int i = 0; i < hydrophoneArray.size(); i++) {
			if (hydrophoneArray.get(i).getStreamerId() == iStreamer) {
				n++;
			}
		}
		return n;
	}
	
	/**
	 * Ensures that hydrophone numbering in in sequential order
	 */
	public void checkHydrophoneIds() {
		if (hydrophoneArray == null) return;
		for (int i = 0; i < hydrophoneArray.size(); i++) {
			hydrophoneArray.get(i).setID(i);
		}
	}

	/**
	 * 
	 * @param a1 phone 1
	 * @param a2 phone 2
	 * @return separation between two hydrophons in metres
	 */
	public double getSeparation(int a1, int a2) {
		PamVector v1 = getAbsHydrophoneVector(a1);
		PamVector v2 = getAbsHydrophoneVector(a2);
		if (v1 == null || v2 == null) return 0;
		return v1.dist(v2);
	}
	
	/**
	 * Get the true location vector for  a hydrophone element 
	 * which is the phone vector + the array vector. 
	 * @param iPhone hydrophone number
	 * @return absolute location. 
	 */
	public PamVector getAbsHydrophoneVector(int iPhone) {
		Hydrophone h = getHydrophone(iPhone);
		if (h == null) {
			return null;
		}
		PamVector v = h.getVector();
		Streamer s = getStreamer(h.getStreamerId());
		if (s != null && s.getCoordinateVector() != null) {
			v = v.add(s.getCoordinateVector());
		}
		return v;
	}


	/**
	 * Get the separation of two hydrophones - hopefully, channelMap
	 * will contain just two set bits !
	 * @param channelMap
	 * @return the separation between two hydrophones in milliseconds
	 */
	public double getSeparationInSeconds(int channelMap) {
		int[] channels = new int[2];
		int found = 0;
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			if ((1<<i & channelMap) > 0) {
				channels[found] = i;
				if (++found >= 2) break;
			}
		}
		return getSeparationInSeconds(channels[0], channels[1]);
	}
	
	/**
	 * 
	 * @param a1 phone 1
	 * @param a2 phone 2
	 * @return separation of two hydrophones in seconds. 
	 */
	public double getSeparationInSeconds(int a1, int a2) {
		return getSeparation(a1, a2) / speedOfSound;
	}
	/**
	 * Get the max phone separation in metres for all hydrophones in the array.  
	 * @return max phone separation in metres. 
	 */
	public double getMaxPhoneSeparation(){
		return getMaxPhoneSeparation(0xFFFFFFFF);
	}
			
	/**
	 * Get the max phone separation in metres. 
	 * @param phoneMap bit map of used hydrophones. 
	 * @return max phone separation in metres. 
	 */
	public double getMaxPhoneSeparation(int phoneMap) {
		double maxSep = 0, sep;
		int nH = getHydrophoneCount();
		for (int i = 0; i < nH; i++) {
			if ((1<<i & phoneMap) == 0) continue;
			for (int j = i+1; j < nH; j++) {
				if ((1<<j & phoneMap) == 0) continue;
				sep = hydrophoneArray.get(i).getVector().dist(hydrophoneArray.get(j).getVector());
				maxSep = Math.max(sep, maxSep);
			}
		}
		
		return maxSep;
	}

	/**
	 * Get the error in the separation between two hydrophones along
	 * the axis joining the two phones
	 * @param a1 phone 1
	 * @param a2 phone 1
	 * @return error in m. 
	 */
	public double getSeparationError(int a1, int a2) {
		PamVector v1 = getAbsHydrophoneVector(a1);
		PamVector v2 = getAbsHydrophoneVector(a2);
		if (v1 == null || v2 == null) {
			return 0;
		}
		else {
			return getSeparationError(a1, a2, v1.sub(v2));
		}
	}
	
	/**
	 * Get the separation error in a given direction
	 * @param a1 phone 1
	 * @param a2 phone 2 
	 * @param direction direction of interest. 
	 * @return error in that direction
	 */
	public double getSeparationError(int a1, int a2, PamVector direction) {
		PamVector errorVector = getSeparationErrorVector(a1, a2);
		direction = direction.getUnitVector();
		return Math.abs(errorVector.dotProd(direction));
	}
	
	public double getWobbleRadians(int a1, int a2) {
		double perpError = getPerpendicularError(a1, a2);
		double separation = getSeparation(a1, a2);
		double wobble = Math.abs(Math.atan2(perpError, separation));
		return wobble;
	}
	
	/**
	 * 
	 * @param a1 phone number 1
	 * @param a2 phone number 2
	 * @param timeMillis
	 * @return
	 */
	public double getTimeDelayError(int a1, int a2) {
		
		PamVector sepError= getSeparationErrorVector( a1,  a2); 
		
		double totSqrd=0;
		
		for (int i=0; i<3; i++){
			totSqrd+=Math.pow(sepError.getVector()[i],2);
		}
		
		double tdError=(Math.sqrt(totSqrd))/speedOfSound; 
		
		double soundError=Math.abs(((Math.sqrt(totSqrd))/(speedOfSound+ speedOfSoundError))-tdError);
		
		return Math.sqrt(Math.pow(tdError,2)+Math.pow(soundError,2));
	}

	/**
	 * Get the error in the plane perpendicular to the line separating 
	 * two hydrophones. 
	 * @param a1 hydrophone 1
	 * @param a2 hydrophone 2
	 * @return magnitude of error perpendicular to the line between them. 
	 */
	public double getPerpendicularError(int a1, int a2) {
		PamVector v1 = getAbsHydrophoneVector(a1);
		PamVector v2 = getAbsHydrophoneVector(a2);
		if (v1 == null || v2 == null) {
			return 0;
		}
		else {
			return getPerpendicularError(a1, a2, v1.sub(v2));
		}
	}
	
	/**
	 * 
	 * @param a1
	 * @param a2
	 * @param direction
	 * @return
	 */
	public double getPerpendicularError(int a1, int a2, PamVector direction) {
		if (direction.normalise() == 0) {
			return 0;
		}

		Hydrophone h = getHydrophone(a1);
		if (h == null) {
			return 0;
		}
		PamVector v1 = h.getErrorVector();
		h = getHydrophone(a2);
		if (h == null) {
			return 0;
		}
		PamVector v2 = h.getErrorVector();
		PamVector errVec = PamVector.addQuadrature(v1, v2);
		errVec = errVec.vecProd(direction);
		double error = errVec.norm();
		
		return error;
	}
	
	/**
	 * Gets the error of the separation between two hydrophones as a vector. 
	 * <p>
	 * If the hydrophones are in the same streamer, then the error is the error
	 * on the coordinate of each phone added in quadrature, and the streamer errors
	 * are ignored. 
	 * <p>
	 * If the hydrophones are in different streamers, then the streamer errors are
	 * also incorporated. 
	 * @param a1 phone number 1
	 * @param a2 phone number 2
	 * @return vector representing components of error in each direction. 
	 */
	public PamVector getSeparationErrorVector(int a1, int a2) {
		Hydrophone h1 = getHydrophone(a1);
		Hydrophone h2 = getHydrophone(a2);
		PamVector e1 = h1.getErrorVector();
		PamVector e2 = h2.getErrorVector();
		if (h1.getStreamerId() == h2.getStreamerId()) {
			return PamVector.addQuadrature(e1, e2);
		}
		PamVector e3 = getStreamer(h1.getStreamerId()).getErrorVector();
		PamVector e4 = getStreamer(h2.getStreamerId()).getErrorVector();
		return PamVector.addQuadrature(e1, e2, e3, e4);
	}
	
	/**
	 * Create a simple linear array. 
	 * @param arrayName name
	 * @param y0 y position of first hydrophone
	 * @param depth depth
	 * @param separation separation between each element
	 * @param nElements number of eleemnts
	 * @param sensitivity hydrophone sensitivity
	 * @param gain preamp gain
	 * @param bandwidth preamp bandwidth
	 * @return new array 
	 */
	public static PamArray createSimpleArray(String arrayName, double y0, double depth, double separation,
			int nElements, double sensitivity, double gain, double[] bandwidth) {
		PamArray newArray = new PamArray(arrayName, ARRAY_TYPE_TOWED);
		for (int i = 0; i < nElements; i++) {
			newArray.addHydrophone(new Hydrophone(i, 0, y0 - separation * i, -depth, "Unknown",
					sensitivity, bandwidth, 0));
		}
		
		return newArray;
	}

	/**
	 * 
	 * @return Speed of sound in m/s
	 */
	public double getSpeedOfSound() {
		return speedOfSound;
	}

	/**
	 * 
	 * @param speedOfSound speed of sound in m/s
	 */
	public void setSpeedOfSound(double speedOfSound) {
		this.speedOfSound = speedOfSound;
	}
	public double getSpeedOfSoundError() {
		return speedOfSoundError;
	}

	public void setSpeedOfSoundError(double speedOfSoundError) {
		this.speedOfSoundError = speedOfSoundError;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return arrayName;
	}
	
	public String getArrayFileName() {
		if (arrayFile != null) return arrayFile;
		return arrayName + "." + ArrayManager.getArrayFileType();
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public PamArray clone() {
		try {
			PamArray pa = (PamArray) super.clone();
			if (pa.arrayFile != null) {
				pa.arrayFile = new String(pa.arrayFile);
			}
			if (pa.arrayName != null) {
				pa.arrayName = new String(pa.arrayName);
			}
			ArrayList<Hydrophone> hl = new ArrayList<Hydrophone>();
			for (int i = 0; i < pa.hydrophoneArray.size(); i++) {
				hl.add(pa.hydrophoneArray.get(i).clone());
			}
			pa.hydrophoneArray = hl;
			return pa;
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
			return null;
		}
	}
	/**
	 * @return Returns the arrayFile.
	 */
	public String getArrayFile() {
		return arrayFile;
	}
	/**
	 * @param arrayFile The arrayFile to set.
	 */
	public void setArrayFile(String arrayFile) {
		this.arrayFile = arrayFile;
	}
	
	/**
	 * @return Returns the fixedLatLong.
	 */
	public LatLong getFixedLatLong() {
		return fixedLatLong;
	}
	/**
	 * @param fixedLatLong The fixedLatLong to set.
	 */
	public void setFixedLatLong(LatLong fixedLatLong) {
		this.fixedLatLong = fixedLatLong.clone();
		setStaticMasterReference();
	}
	
	private void setStaticMasterReference() {
		MasterReferencePoint.setRefLatLong(fixedLatLong, "Static Array Loc");		
	}
	/**
	 * @return Returns the arrayName.
	 */
	public String getArrayName() {
		return arrayName;
	}
	/**
	 * @param arrayName The arrayName to set.
	 */
	public void setArrayName(String arrayName) {
		this.arrayName = arrayName;
	}
	/**
	 * @return Returns the arrayType. Either ARRAY_TYPE_TOWED or ARRAY_TYPE_STATIC
	 */
	public int getArrayType() {
		return arrayType;
	}
	/**
	 * @param arrayType Set the array type - Either ARRAY_TYPE_TOWED or ARRAY_TYPE_STATIC.
	 */
	public void setArrayType(int arrayType) {
		this.arrayType = arrayType;
		switch (arrayType) {
		case ARRAY_TYPE_STATIC:
			hydrophoneLocator = new StaticHydrophoneLocator(this);
			setStaticMasterReference();
			break;
		case ARRAY_TYPE_TOWED:
			hydrophoneLocator = HydrophoneLocators.getInstance().get(arrayLocator, this);
			break;
		}
	}
	/**
	 * @return Returns the headingType.
	 */
	public int getHeadingType() {
		return headingType;
	}

	/**
	 * @param headingType The headingType to set.
	 */
	public void setHeadingType(int headingType) {
		this.headingType = headingType;
	}

	/**
	 * @return Returns the tiltType.
	 */
	public int getTiltType() {
		return tiltType;
	}

	/**
	 * @param tiltType The tiltType to set.
	 */
	public void setTiltType(int tiltType) {
		this.tiltType = tiltType;
	}
	/**
	 * @return Returns the fixedPointReferenceBlock.
	 */
	public PamDataBlock<GpsDataUnit> getFixedPointReferenceBlock() {
		if (fixedPointReferenceBlock == null || fixedPointReferenceBlock.getUnitsCount() == 0) {
			setupFixedPointReferenceBlock();
		}
		return fixedPointReferenceBlock;
	}
	public void setupFixedPointReferenceBlock() {
		if (fixedPointReferenceBlock != null & arrayType == ARRAY_TYPE_TOWED) {
			fixedPointReferenceBlock = null;
			fixedPointDataUnit = null;
			return;
		}
		if (fixedPointReferenceBlock == null) {
			fixedPointReferenceBlock = new PamDataBlock<GpsDataUnit>(GpsDataUnit.class, "Fixed Data", null, 0);
			fixedPointReferenceBlock.setLinkGpsData(false);
			fixedPointReferenceBlock.setNaturalLifetime(Integer.MAX_VALUE);
		}
		fixedPointReferenceBlock.clearAll();
		GpsData positionData = new GpsData();
		if (fixedLatLong != null) {
			positionData.setLatitude(fixedLatLong.getLatitude());
			positionData.setLongitude(fixedLatLong.getLongitude());
			setStaticMasterReference();
		}
		fixedPointDataUnit = new GpsDataUnit(PamCalendar.getTimeInMillis(), positionData);
		fixedPointReferenceBlock.addPamData(fixedPointDataUnit);
	}

	public HydrophoneLocator getHydrophoneLocator() {
		if (hydrophoneLocator == null) {
			setArrayType(arrayType);
		}
		return hydrophoneLocator;
	}

	/**
	 * @param hydrophoneLocator the hydrophoneLocator to set
	 */
	public void setHydrophoneLocator(HydrophoneLocator hydrophoneLocator) {
		this.hydrophoneLocator = hydrophoneLocator;
	}

	public int getArrayLocator() {
		return arrayLocator;
	}

	public void setArrayLocator(int arrayLocator) {
		this.arrayLocator = arrayLocator;
		hydrophoneLocator = HydrophoneLocators.getInstance().get(arrayLocator, this);
		if (hydrophoneLocator.getClass() == StaticHydrophoneLocator.class) {
			setStaticMasterReference();
		}
	}

	/**
	 * 
	 * Gets a local array geometry in metres, based on the
	 * reference location and on the locator. 
	 * <p>
	 * The coordinate system is referenced to the reference 
	 * latLong of the array locator at timeMilliseconds. 
	 * 
	 * @param timeMilliseconds time geometry required for
	 * @return double array of arrays of positions
	 */
	public double[][] getLocalGeometry(long timeMilliseconds) {
		LatLong refLatLong = getHydrophoneLocator().getReferenceLatLong(timeMilliseconds);
		return getLocalGeometry(refLatLong, timeMilliseconds);
	}
	/**
	 * Gets a local array geometry in metres, based on the
	 * reference location and on the locator. 
	 * <p>
	 * If the array is static, it will generally just return 
	 * the geometry set in the array dialog
	 * <p>
	 * If the array is towed, then it will work out the latlongs 
	 * of the hydrophones bases on the arraylocator type, it will
	 * then convert these back into geometric coordinates relative
	 * to the given reference position.  
	 * 
	 * @param referenceLatLong reference position for x,y system
	 * @param timeMilliseconds time geometry required for
	 * @return double array of arrays of positions
	 */
	public double[][] getLocalGeometry(LatLong referenceLatLong, long timeMilliseconds) {
		int nPhones = hydrophoneArray.size();
		double[][] localGeom = new double[nPhones][3];
		Hydrophone h;
		Streamer s;
		for (int i = 0; i < nPhones; i++) {
			h = hydrophoneArray.get(i);
			s = getStreamer(h.getStreamerId());
			for (int c = 0; c < 3; c++) {
				localGeom[i][c] = h.getCoordinate(c);
				if (s != null) {
					localGeom[i][c] += s.getCoordinate(c);
				}
			}
		}
		if (referenceLatLong == null) {
			referenceLatLong = getHydrophoneLocator().getReferenceLatLong(timeMilliseconds);
		}
		
		if (getArrayType() == ARRAY_TYPE_STATIC) {
			LatLong arrayRefPos = this.fixedLatLong;
			if (arrayRefPos == null) {
				arrayRefPos = new LatLong(0,0);
			}
			double xOff = arrayRefPos.distanceToMetresX(arrayRefPos);
			double yOff = arrayRefPos.distanceToMetresY(arrayRefPos);

			// add the offset to get the updated reference. 
			for (int i = 0; i < nPhones; i++) {
				localGeom[i][0] += xOff;
				localGeom[i][1] += yOff;
			}
		}
		else { // towed array, so get the locations as lat long and calc distances from ref.
			HydrophoneLocator hl = getHydrophoneLocator();
			LatLong hLatLong;
			for (int i = 0; i < nPhones; i++) {
				hLatLong = hl.getPhoneLatLong(timeMilliseconds, i);
				localGeom[i][0] = referenceLatLong.distanceToMetresX(hLatLong);
				localGeom[i][1] = referenceLatLong.distanceToMetresY(hLatLong);
			}
		}

		return localGeom;
	}
	
	/**
	 * 
	 * @return the number of streamers in the system.
	 */
	int getNumStreamers() {
		checkDefStreamer();
		return streamers.size();
	}

	private void checkDefStreamer() {
		if (streamers == null) {
			streamers = new ArrayList<Streamer>();
		}
		if (streamers.size() == 0) {
			streamers.add(new Streamer(0, 0, 0, 0.1, 0.1, 0.1));
		}
	}

	public Streamer getStreamer(int row) {
		checkDefStreamer();
		if (row < streamers.size()) {
			return streamers.get(row);
		}
		return null;
	}
	
	/**
	 * Find a streamer with a given buoy Id. 
	 * @param buoyId integer buoy id
	 * @return streamer or null if none found. 
	 */
	public Streamer findBuoyStreamer(int buoyId) {
		for (Streamer s:streamers) {
			if (s.getBuoyId1() != null && s.getBuoyId1() == buoyId) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * Get the limits of the dimensions of the entire array. 
	 * 
	 * @return a 3 by 2 double array of the min and max of x,y,z.
	 */
	public double[][] getDimensionLimits() {
		if (hydrophoneArray.size() == 0) {
			return null;
		}
		Hydrophone h = hydrophoneArray.get(0);
		Streamer s = getStreamer(h.getStreamerId());
		double[][] lims = new double[3][2];
		for (int c = 0; c < 3; c++) {
			for (int i = 0; i < 2; i++) {
				lims[c][i] = h.getCoordinate(c);
				if (s != null) {
					lims[c][i] += s.getCoordinate(c);
				}
			}
		}
		double[] hCoordinate;
		double[] sCoordinate;
		for (int iH = 1; iH < hydrophoneArray.size(); iH++) {
			h = hydrophoneArray.get(iH);
			hCoordinate = Arrays.copyOf(h.getCoordinates(),3);
			s = getStreamer(h.getStreamerId());
			if (s != null) {
				sCoordinate = s.getCoordinates();
				for (int i = 0; i < 3; i++) {
					hCoordinate[i] += sCoordinate[i];
				}
			}
			for (int c = 0; c < 3; c++) {
				lims[c][0] = Math.min(lims[c][0], hCoordinate[c]);
				lims[c][1] = Math.max(lims[c][1], hCoordinate[c]);	
			}
		}
		return lims;
	}
	
	/**
	 * Get a bitmap of hydrophones used with a particular streamer. 
	 * @param streamer Hydrophone streamer. 
	 * @return bitmap of used hydrophones. 
	 */
	public int getPhonesForStreamer(Streamer streamer) {
		int streamerIndex = streamers.indexOf(streamer);
		return getPhonesForStreamer(streamerIndex);
	}
	
	/**
	 * Get a bitmap of hydrophones used with a particular streamer. 
	 * @param streamerId Hydrophone streamer index. 
	 * @return bitmap of used hydrophones.
	 */
	public int getPhonesForStreamer(int streamerId) {
		int phones = 0;
		int ih = 0;
		for (Hydrophone h:hydrophoneArray) {
			if (h.getStreamerId() == streamerId) {
				phones |= 1<<ih;
			}
			ih++;
		}
		return phones;
	}

	/**
	 * Add a streamer and return the streamer id which will be one less than the number of streamers. 
	 * @param streamer
	 * @return
	 */
	public int addStreamer(Streamer streamer) {
		streamers.add(streamer);
		return streamers.size()-1;
	}
	
	public void updateStreamer(int iStreamer, Streamer newStreamer) {
		streamers.remove(iStreamer);
		streamers.add(iStreamer, newStreamer);
	}

	public void removeStreamer(int row) {
		streamers.remove(row);
	}

	/**
	 * 
	 * @return the array shape, this is either point, line, plane or volumetric
	 */
	public int getArrayShape() {
		if (arrayShape < 0) {
			arrayShape = ArrayManager.getArrayManager().getArrayShape(this);
		}
		return arrayShape;
	}

	/**
	 * 
	 * @param arrayShape set the array shape - point, line, plane or volumetric. 
	 */
	public void setArrayShape(int arrayShape) {
		this.arrayShape = arrayShape;
	}
	
}
