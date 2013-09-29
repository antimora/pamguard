package PamDetection;

import java.util.ArrayList;

import pamMaths.PamVector;
import Array.ArrayManager;
import Jama.Matrix;
import PamUtils.LatLong;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Class for localisation information. 
 * <p> Each AbstractLocalisation should be uniquely linked
 * to a single PamDataUnit, since this class is abstract, where the actual data
 * are stored (in the data unit or in the localisation) is up to the programmer. 
 * <p>
 * This class has been through a number of iterations during 2007 and may well 
 * go through a few more before we are all happy with it. The main problem has
 * been whether to reference positions purely by a hydrophone number, a time, a distance 
 * and bearing, or simply by a LatLong. It's ended up as a mixture of both. 
 * <p>
 * Things are made worse by ambiguities which means that there may be one or more of each type 
 * of information. The key is to correctly set the types of information held within a particular
 * localisation so that other parts of Pamguard may query it and act accordingly. It is vital that the 
 * PamDataBlock which will hold the PamDataUnits associated with a localisation have their localisationContents
 * member correctly set to the full range of POSSIBLE localisation information types for the data units they
 * will contain. This is important for the database and the various displays, which will all query the 
 * PamDataBlocks before the contain any PamDataUnits in order to see which columns to 
 * put in database tables, whether or
 * not the data within a PamDataBlock may plot on a particular display, etc. 
 * 
 * @author Doug Gillespie
 * @see PamDataUnit
 * @see PamDataBlock
 *
 */
abstract public class AbstractLocalisation {

	/*
	 * Flags to say what each localisation contains
	 */
	static public final int HAS_BEARING = 0x1;
	static public final int HAS_RANGE = 0x2;
	static public final int HAS_DEPTH = 0x4;
	static public final int HAS_BEARINGERROR = 0x8; 
	static public final int HAS_RANGEERROR = 0x10;
	static public final int HAS_DEPTHERROR = 0x20;
	static public final int HAS_LATLONG = 0x40;
	static public final int HAS_XY = 0x80;
	static public final int HAS_XYZ = 0x100;
	static public final int HAS_AMBIGUITY = 0x200;
	/**
	 * Errors parallel and perpendicular to the ships track.
	 */
	static public final int HAS_PERPENDICULARERRORS = 0x400;
	
	static private final double[][] linearArrayGeometry = {{0, 1, 0}, {1, 0, 0}, {0, 0, -1}}; 

	/**
	 * reference to parent PamDetection object
	 */
	private PamDataUnit pamDataUnit;
	
	/**
	 * bitmap of flags saying what's in the localisation information
	 */
	private int locContents = 0;
	
	/**
	 * Type of array, point, line, plane, volume, etc. 
	 */
	private int arrayType = ArrayManager.ARRAY_TYPE_NONE;
	
	/**
	 * Principle axis of the array geometry. 
	 */
	private PamVector[] arrayAxis = null;
	
	/**
	 * All localisation must be relative to at least one hydrophone. If more than one is specified, then 
	 * it's assumed that the data are relative to the mean position of all hydrophones. 
	 */
	private int referenceHydrophones;

	public AbstractLocalisation(PamDataUnit pamDataUnit, int locContents, int referenceHydrophones) {
		super();
		this.pamDataUnit = pamDataUnit;
		this.locContents = locContents;
		this.referenceHydrophones = referenceHydrophones;
	}

	public AbstractLocalisation(PamDataUnit pamDataUnit, int locContents, int referenceHydrophones,
			int arrayType, PamVector[] arrayAxis) {
		super();
		this.pamDataUnit = pamDataUnit;
		this.locContents = locContents;
		this.referenceHydrophones = referenceHydrophones;
	}
	
	/**
	 * 
	 * @return the type of hydrophone sub array used, eg point, planar, line, volumetric.
	 * this can tell us the type of information likely to be available in terms of bearings, etc.
	 * <p> Added DG 6 January 2010 
	 */
	public int getSubArrayType() {
		return arrayType;
	}
	
	/**
	 * Set the type of array (or sub array) used for this particular localisation
	 * This may not be the entire array if detection was only on some channels. 
	 * @param arrayType
	 */
	public void setSubArrayType(int arrayType) {
		this.arrayType = arrayType;
	}
	/**
	 * 
	 * @return one, two or three orthogonal orientation vectors which tell us the nominal direction the 
	 * array is pointing in. For a simple towed array, this is likely to be (0,1,0), i.e. aligned
	 * with the positive y axis. For a planar array, two vectors should be returned and for 
	 * a volumetric array, three, although the third will just be the cross product of the first two.  
	 */
	public PamVector[] getArrayOrientationVectors() {
		return arrayAxis;
	}
	
	/**
	 * Set the array axis - one, two or three axes defining the principle 
	 * orientations of the array used for the localisation. The angles
	 * in the localisation will be relative to these axes. 
	 * @param arrayAxis array axis vectors. 
	 */
	public void setArrayAxis(PamVector[] arrayAxis) {
		this.arrayAxis = arrayAxis;
	}
		
	/**
	 * returns angles projected onto the surface of a plane defined by the 
	 * first two (or first in the case of a linear array) array axis directions. <p>
	 * In an ideal world, this plane will be aligned with the sea surface, in which case it 
	 * will be relatively easy to convert these angles to a surface angle. However, the choice 
	 * of array primary and secondary axes may vary a lot (depending on hydrophone order) and there
	 * is no implicit guarantee that these angles will be relative to the real x and y axis of the array. 
	 * <p>For a linear array
	 * this will just be +/- the theta angle, or the getBearing(0) angle and
	 * there will be two of them, reflecting lr ambiguity. <br>For a planar array, the
	 * angle will probably be a single angle, but will be projected onto the 
	 * plane of the array. 
	 * <br>For a volumetric array, it will be a projection of the  
	 * two angles. 
	 * <p>To use these data though, it will also be necessary to know the
	 * orientation of the principle axes of the array. For a linear array, this will 
	 * just be the line of the array. For a planar and volumetric array, these may depend
	 * on the type of localiser used. The array axes should be available in arrayAxes.
	 * <p>The planar angles will be correct relative to the two primary array axis directions. 
	 * 
	 * @return array of possible angles. 
	 */
	public double[] getPlanarAngles() {
		double[] newAngles = getAngles();
		double[] planarAngles;
		if (newAngles == null) {
			if (hasLocContent(HAS_AMBIGUITY)) {
				planarAngles = new double[2];
				planarAngles[0] = getBearing(0);
				planarAngles[1] = getBearing(1);
			}
			else {
				planarAngles = new double[1];
				planarAngles[0] = getBearing(0);
			}
		}
		else {
			if (newAngles.length == 2) {
				planarAngles = new double[1];
				planarAngles[0] = Math.atan2(Math.sin(newAngles[0])*Math.cos(newAngles[1]), Math.cos(newAngles[0]));
			}
			else {
				planarAngles = new double[2];
				planarAngles[0] = newAngles[0];		
				planarAngles[1] = -planarAngles[0];
			}
		}
		return planarAngles;
	}
	
	/**
	 * Get a unit vector pointing towards this localisation 
	 * in the coordinate frame of the array geometry. 
	 * 
	 * @return a unit vector in the coordinate frame of the array geometry. <br>
	 * If you want the projection of this vector onto the plane, simply set the third
	 * element to zero. If you do that, do not normalise in case the direction is 
	 * perpendicular to the plane, in which case you'll get a divide by zero.
	 */
	public PamVector getPlanarVector() {
		double[] newAngles = getAngles();
		PamVector planeVec = new PamVector();
		if (newAngles == null) {
			double bearing = getBearing(0);
			planeVec.setElement(0, Math.cos(bearing));
			planeVec.setElement(1, Math.sin(bearing));
		}
		else {
			if (newAngles.length == 1) {
				planeVec.setElement(0, Math.cos(newAngles[0]));
				planeVec.setElement(1, Math.sin(newAngles[0]));
			}
			else {
				planeVec.setElement(0, Math.cos(newAngles[0]));
				double sin0 = Math.sin(newAngles[0]);
				planeVec.setElement(1, sin0*Math.cos(newAngles[1]));
				planeVec.setElement(2, sin0*Math.sin(newAngles[1]));
			}
		}
		return planeVec;
	}
	
	/**
	 * Get vectors pointing at this localisation in a real world coordinate frame. 
	 * <br>N.B. Real world in this instance means relative to the xyz coordinate
	 * frame of the hydrophone array. To get real real world vectors relative to the 
	 * planet, the vectors will need to be further rotated by the course of the 
	 * vessel.  
	 * the hydrophone array
	 * @return vectors pointing at this localisation in a real world coordinate frame.
	 * For a volumetric array, there should be a single vector since the geometry gives an
	 * unambiguous result. For a planar array we will get two vectors, one either side of
	 * the plane.    
	 */
	public PamVector[] getWorldVectors() {
		PamVector singleVec = getPlanarVector();
		Matrix pointer = new Matrix(3, 1);
		for (int i = 0; i < 3; i++) {
			pointer.set(i, 0, singleVec.getElement(i));
		}
		Matrix rotatedPointer;
		Matrix coordMatrix;
		Matrix invCoordMatrix;
		if (arrayType == ArrayManager.ARRAY_TYPE_VOLUME) {
			PamVector[] vecs = new PamVector[1];
			coordMatrix = getCoordinateMatrix(false);
			invCoordMatrix = coordMatrix.inverse();
			rotatedPointer = invCoordMatrix.times(pointer);
			vecs[0] = new PamVector(rotatedPointer.getColumnPackedCopy());
			return vecs;
		}
		else if (arrayType == ArrayManager.ARRAY_TYPE_PLANE) {
			PamVector[] vecs = new PamVector[2];
			coordMatrix = getCoordinateMatrix(false);
			invCoordMatrix = coordMatrix.inverse();
			rotatedPointer = invCoordMatrix.times(pointer);
			vecs[0] = new PamVector(rotatedPointer.getColumnPackedCopy());
			coordMatrix = getCoordinateMatrix(true);
			invCoordMatrix = coordMatrix.inverse();
			rotatedPointer = invCoordMatrix.times(pointer);
			vecs[1] = new PamVector(rotatedPointer.getColumnPackedCopy());
			return vecs;
		}
		else {
			PamVector[] vecs = new PamVector[2];
			coordMatrix = getCoordinateMatrix(false);
			invCoordMatrix = coordMatrix.inverse();
			rotatedPointer = invCoordMatrix.times(pointer);
			vecs[0] = new PamVector(rotatedPointer.getColumnPackedCopy());
			pointer.set(1,0,-pointer.get(1,0));
			rotatedPointer = invCoordMatrix.times(pointer);
			vecs[1] = new PamVector(rotatedPointer.getColumnPackedCopy());
			
//			vecs[0] = singleVec;
//			
//			vecs[1] = singleVec.clone();
//			vecs[1].setElement(1, -vecs[1].getElement(1));
			return vecs;
		}
	}
	
	/**
	 * Get vectors to the detection in real world (relative to earths surface)
	 * coordinated. 
	 * <br> This is the output of GetWorldVectors rotated by the reference bearing from 
	 * the GPS data. 
	 * @return real world vectors. 
	 */
	public PamVector[] getRealWorldVectors() {
		PamVector[] v = getWorldVectors();
		if (v == null) {
			return null;
		}
		double referenceBearing = getBearingReference();
		/*
		 *  standard rotation is anti-clockwise, so need
		 *  to rotate by - the bearing angle. 
		 */
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i].rotate(-referenceBearing);
		}
		return v;
	}
	
	/**
	 * Get the coordinate matrix which describes the principle coordinates of the array. 
	 * Ultimately, the matrix must be three orthogonal vectors. Generally only two will 
	 * have been defined thus far, and the third is easily calculated. 
	 * <br>
	 * For some old geometries, or for linear arrays, there will be no data, so in this
	 * case, set the first axis in lie with the array, the second in the horizontal, at right
	 * angles to the first and the third as the orthogonal to both the others. 
	 * @param flipZ
	 * @return
	 */
	private Matrix getCoordinateMatrix(boolean flipZ) {
		if (arrayAxis == null || arrayAxis.length < 1) {
			return new Matrix(linearArrayGeometry);
		}
		else if (arrayAxis.length == 1) {
			return linearCoordinateMatrix(flipZ);
		}
		double[][] m = new double[3][];
		m[0] = arrayAxis[0].getVector();
		m[1] = arrayAxis[1].getVector();
		if (flipZ) {
			m[2] = arrayAxis[1].vecProd(arrayAxis[0]).getVector();
		}
		else {
			m[2] = arrayAxis[0].vecProd(arrayAxis[1]).getVector();
		}
		return new Matrix(m);
	}
	
	/**
	 * Special case for linear arrays. In principle, these can be at any orientation, 
	 * but will most probably be in line with one of the principle axis. If there is ANY horisontal
	 * component at all, then set the second component perpendicular to that but also 
	 * in the horizontal plane. If the array is perfectly vertical, then set the second componet
	 * to be (1,0,0)
	 * @param flipZ
	 * @return
	 */
	private Matrix linearCoordinateMatrix(boolean flipZ) {
		double[][] m = new double[3][];
		m[0] = arrayAxis[0].getVector();
		PamVector m1 = null;
		if (m[0][0] == 0 && m[0][1] == 0) {
			m1 = PamVector.getXAxis();
		}
		else {
			m1 = PamVector.getZAxis().vecProd(arrayAxis[0]);
		}
		m[1] = m1.getVector();
		if (flipZ) {
			m[2] = m1.vecProd(arrayAxis[0]).getVector();
		}
		else {
			m[2] = arrayAxis[0].vecProd(m1).getVector();
		}

		return new Matrix(m);
	}

	/**
	 * 
	 * @return Angles to detection in radians. The number of angles will be 0, 1 or 2. 
	 * <p> For a point array, null should be returned. 
	 * <p>For a line array
	 * a single angle is returned which is the angle relative to the first
	 * orientation vector with 0 being in line with the orientation vector 
	 * (can be thought of as colatitude). 
	 * <p>For a planar or volumetric array, two angles should be returned, the first
	 * being the colatitude, the second being the longitude which will be between 0 and pi 
	 * for a planar array and either -pi to pi or 0 to 2pi for a volumetric array. 
	 */
	public double[] getAngles() {
		return null;
	}
	/**
	 * 
	 * @return Angle errors to detection in radians. The number of angles will be 0, 1 or 2. 
	 * <p> For a point array, null should be returned. 
	 * <p>For a line array
	 * a single error is returned which is the angle relative to the first
	 * orientation vector with 0 being in line with the orientation vector 
	 * (can be thought of as colatitude). 
	 * <p>For a planar or volumetric array, two errors should be returned, the first
	 * being the error on colatitude, the second being the longitude which will be between 0 and pi 
	 * for a planar array and either -pi to pi or 0 to 2pi for a volumetric array. 
	 */
	public double[] getAngleErrors() {
		return null;
	}
	
	
	/**IndexM1 and IndexM2 specify which hydrophones to calculate time delays between. In the case of a paired array this will simply be the hydrophones in pairs 
	 * so Index M1 will be 0 and even numbers and IndexM2 will be odd numbers. 
	 * @return
	 */
	public static ArrayList<Integer> indexM2(int numberOfHydrophones){
		ArrayList<Integer> IndexM1=new ArrayList<Integer>();
		for (int i=0; i<numberOfHydrophones; i++){
			for (int j=0; j<numberOfHydrophones-(i+1);j++){
				int HN=j+i+1;
		IndexM1.add(HN);	
			}
		}
		return IndexM1;
	}

	
	public static ArrayList<Integer> indexM1(int numberOfHydrophones){
		ArrayList<Integer> IndexM2=new ArrayList<Integer>();
		for (int i=0; i<numberOfHydrophones; i++){
			for (int j=0; j<numberOfHydrophones-(i+1);j++){
				int HN=i;
		IndexM2.add(HN);	
			}
		}
		return IndexM2;
	}
	
	
	/**
	 * Time delays in seconds. <br>These are calculated between every hydrophone pair in a group. To get the pairs use indexM1 and indexM2 functions. For example for four hydrophones in an array;
	 * <br>IndexM1=0 0 0 1 1 2 
	 * <br>IndexM2=1 2 3 2 3 3
	 * <br>So the first time delay will be between hydrophones 0 and 1, the second time delay between hydrophones 0 and 2 and so on.
	 * Time delay convention. The time delay is positive if it hits the indexM1 hydrophone BEFORE hitting the indexM2 hydrophone.
	 * @return array of time delays in seconds. 
	 */
	public double[] getTimeDelays(){
		return null;
	}
	
	/**
	 * Time delay  
	 * @return
	 */
	public double[] getTimeDelayErrors(){
		return null;
	}


	/**
	 * @return a set of flags specifying which data are available within this localisation object. 
	 */
	public int getLocContents() {
		return locContents;
	}

	/**
	 * 
	 * @param locContents a set of flags specifying which data are available within this localisation object.
	 */
	public void setLocContents(int locContents) {
		this.locContents = locContents;
	}
	
	/**
	 * 
	 * @param flagsToAdd localisation flags to add to existing flags. 
	 */
	public void addLocContents(int flagsToAdd) {
		locContents |= flagsToAdd;
	}
	
	/**
	 * 
	 * @param flagsToRemove bitmap of localisation flags to remove. 
	 * @return new or remaining localisation content flags. 
	 */
	public int removeLocContents(int flagsToRemove) {
		locContents &= (~flagsToRemove);
		return locContents;
	}

	/**
	 * Check that the localisation has specific content. 
	 * @param requiredContent specified content
	 * @return true if specified content exists, false otherwise. 
	 */
	public boolean hasLocContent(int requiredContent) {
		return ((requiredContent & locContents) >= requiredContent);
	}
	/**
	 * 
	 * @return Parent detection containing this localisation information
	 */
	public PamDataUnit getParentDetection() {
		return pamDataUnit;
	}

	/**
	 * 
	 * @param parentDetection Parent detection containing this localisation information
	 */
	public void setParentDetection(PamDetection parentDetection) {
		this.pamDataUnit = parentDetection;
	} 
	
	/**
	 * 
	 * @return a bitmap of hydrophone numbers that form a reference position for this localisation
	 */
	public int getReferenceHydrophones() {
		return referenceHydrophones;
	}

	/**
	 * 
	 * @param referenceHydrophones a bitmap of hydrophone numbers that form a reference position for this localisation
	 */
	public void setReferenceHydrophones(int referenceHydrophones) {
		this.referenceHydrophones = referenceHydrophones;
	}

	/**
	 * 
	 * @return The bearing to the localisation (in radians)
	 */
	@Deprecated 
	public double getBearing() {
		return getBearing(0);
	}

	/**
	 * Get the bearing in radians, relative to the bearing reference
	 * @param iSide which side is the bearing on 
	 * @return bearing in radians
	 * @see getBearingReference
	 */
	@Deprecated
	public double getBearing(int iSide) {
		return Double.NaN;
	}
	
	/**
	 * Get the reference bearing in radians. This is relative to North, 
	 * moving in a clockwise direction as would other bearings.
	 * <br> now that the general code for localisation using vectors has been 
	 * sorted out so that the getWorldVectors now returns vectors which are 
	 * correct in the general xyz frame of the array geometry, all that is actually
	 * needed here now is the array heading at the time of the event, while in previous
	 * versions, this required the actual bearing between two hydrophones (which was 
	 * the same as the array heading for linear arrays which is why it all worked).
	 * @return Reference bearing in radians. 
	 */
	public double getBearingReference() {
		return Math.toRadians(pamDataUnit.getHydrophoneHeading(false));
//		return pamDataUnit.getPairAngle(0, false) * Math.PI / 180.;
	}
	
	/**
	 * 
	 * @return true if the bearing is subject to a left right 
	 * (or rotational) ambiguity about the reference bearing. 
	 */
	public boolean bearingAmbiguity() {
//		double[] newAngles = getAngles();
//		if (newAngles != null) {
//			return (newAngles.length == 1 ? false : true);
//		}
		return ((locContents & HAS_AMBIGUITY) != 0);
	}

	/**
	 * 
	 * @return The range to the detection in meters. 
	 */
	@Deprecated 
	public double getRange() {
		return getRange(0);
	}
	
	/**
	 * Get the range for a specific side (where ambiguity exists)
	 * @param iSide
	 * @return range
	 */
	public double getRange(int iSide) {
		return Double.NaN;
	}

	/**
	 * 
	 * @return The depth of the detection in meters
	 */
	@Deprecated 
	public double getDepth() {
		return getDepth(0);
	}
	/**
	 * 
	 * @return The depth of the detection in meters
	 */
	public double getDepth(int iSide) {
		return Double.NaN;
	}

	/**
	 * 
	 * @return The error on the bearing estimation in radians
	 */
	@Deprecated 
	public double getBearingError() {
		return getBearingError(0);
	}
	/**
	 * 
	 * @param iSide
	 * @return The error on the bearing estimation in radians for the given side
	 */
	public double getBearingError(int iSide) {
		return Double.NaN;
	}
	
	/**
	 * 
	 * @return The error on the range estimation in meters. 
	 */
	@Deprecated 
	public double getRangeError() {
		return getRangeError(0);
	}
	/**
	 * 
	 * @return The error on the range estimation in meters. 
	 */
	public double getRangeError(int iSide) {
		return Double.NaN;
	}

	/**
	 * 
	 * @return The error on the depth estimation in meters. 
	 */
	@Deprecated 
	public double getDepthError() {
		return getDepthError(0);
	}
	/**
	 * 
	 * @return The error on the depth estimation in meters. 
	 */
	public double getDepthError(int iSide) {
		return Double.NaN;
	}
	
	/**
	 * 
	 * @return the latlong of the centre of the hydrophones associated with 
	 * the channels used in this detection. If no channels are set, then it returns
	 * the GPS location for the time of the detection.
	 */
	public LatLong getOriginLatLong() {
		return pamDataUnit.getOriginLatLong(false);
	}
	
	/**
	 * Return the latlong for a location. There may be more than one of them
	 * if there is side to side ambiguity.
	 * @param iSide 0, 1, 2, etc. 
	 * @return LatLong information
	 */
	public LatLong getLatLong(int iSide) {
		return null;
	}
	
	/**
	 * 
	 * @return The number of LatLongs (generally 0 to 2)
	 */
	public int getNumLatLong() {
		if (bearingAmbiguity()) {
			return 2;
		}
		else {
			return 1;
		}
	}
	
	/**
	 * Get the error perpedicular to the track line (in meters)
	 * @param iSide 0, 1, 2, etc.
	 * @return the error in metres. 
	 */
	public double getPerpendiculaError(int iSide) {
		return Double.NaN;
	}
	/**
	 * Get the error parallel to the trackline (in meters)
	 * @param iSide 0, 1, 2, etc.
	 * @return the error in metres. 
	 */
	public double getParallelError(int iSide) {
		return Double.NaN;
	}
	
	/**
	 * Get an angle that the errors are to be plotted relative to. 
	 * @param iSide
	 * @return Error direction (radians)
	 */
	public double getErrorDirection(int iSide) {
		return Double.NaN;
	}
	

}
