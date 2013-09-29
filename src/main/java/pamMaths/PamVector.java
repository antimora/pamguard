package pamMaths;

import java.io.Serializable;
import java.util.Arrays;

import Jama.Matrix;

/**
 * 
 * @author Doug Gillespie
 * 
 * Vector calculations for spatial orientation. 
 * <br>All vectors have three elements for x,y,z.
 * <br>Package includes basic vector calculations, mostly
 * copied from Stephenson 1961
 *
 */
public class PamVector implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	
	protected double[] vector;
	
	private static final PamVector xAxis = new PamVector(1,0,0);
	private static final PamVector yAxis = new PamVector(0,1,0);
	private static final PamVector zAxis = new PamVector(0,0,1);
	private static final PamVector[] cartesianAxes = {xAxis, yAxis, zAxis};

	public PamVector(double[] vector) {
		if (vector != null) {
			this.vector = vector;
		}
		else {
			this.vector = new double[3];
		}
	}
	
	public PamVector(double x, double y, double z) {
		vector = new double[3];
		vector[0] = x;
		vector[1] = y;
		vector[2] = z;
	}

	public PamVector(PamVector vector) {
		this.vector = Arrays.copyOf(vector.vector, 3);
	}

	public PamVector() {
		vector = new double[3];
	}

	@Override
	public PamVector clone() {
		try {
			PamVector v = (PamVector) super.clone();
			v.vector = Arrays.copyOf(this.vector, 3);
			return v;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @return The magnitude squared of the vector
	 */
	public double normSquared() {
		double a = 0;
		for (int dim = 0; dim < 3; dim++) {
			a += Math.pow(vector[dim],2);
		}
		return a;
	}
	
	/**
	 * 
	 * @param vec other vector to compare. 
	 * @return true if the vectors are equal in all dimensions
	 */
	public boolean equals(PamVector vec) {
		for (int dim = 0; dim < 3; dim++) {
			if (vector[dim] != vec.vector[dim]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Subtract another vector from this vector and return the result in a
	 * new Vector
	 * @param vec vector to subtract
	 * @return new vector
	 */
	public PamVector sub(PamVector vec) {
		PamVector newVec = this.clone();
		for (int dim = 0; dim < 3; dim++) {
			newVec.vector[dim]-=vec.vector[dim];
		}
		return newVec;
	}
	
	/**
	 * Add another vector to this vector and return the result in a
	 * new Vector
	 * @param vec vector to add
	 * @return new vector
	 */
	public PamVector add(PamVector vec) {
		PamVector newVec = this.clone();
		for (int dim = 0; dim < 3; dim++) {
			newVec.vector[dim]+=vec.vector[dim];
		}
		return newVec;
	}
	
	/**
	 * Add any number of vectors together. 
	 * @param pamVectors list of vectors
	 * @return sum of all vectors. 
	 */
	public static PamVector add(PamVector... pamVectors) {
		PamVector vec = new PamVector();
		for (int i = 0; i < pamVectors.length; i++) {
			for (int d = 0; d < 3; d++) {
				vec.vector[d] += pamVectors[i].vector[d];
			}
		}
		return vec;
	}
	
	/**
	 * Add any number of vectors together in quadrature. 
	 * @param pamVectors list of vectors
	 * @return sum of all vectors. 
	 */
	public static PamVector addQuadrature(PamVector... pamVectors) {
		PamVector vec = new PamVector();
		for (int i = 0; i < pamVectors.length; i++) {
			for (int d = 0; d < 3; d++) {
				vec.vector[d] += Math.pow(pamVectors[i].vector[d], 2);
			}
		}
		for (int d = 0; d < 3; d++) {
			vec.vector[d] = Math.sqrt(vec.vector[d]);
		}
		return vec;
	}
	
	/**
	 * convert a series of vectors into a Matrix
	 * @param pamVectors array of vectors
	 * @return Jama Matrix
	 */
	public static Matrix arrayToMatrix(PamVector[] pamVectors) {
		int nRow = pamVectors.length;
		int nCol = 3;
		double[][] data = new double[nRow][];
		for (int i = 0; i < nRow; i++) {
			data[i] = pamVectors[i].vector;
		}
		return new Matrix(data);
	}
	
	/**
	 * Rotate the vector by a matrix. 
	 * @param rotationMatrix rotation matrix
	 * @return rotated vector. 
	 */
	public PamVector rotate(Matrix rotationMatrix) {
		// create a single row matrix
		Matrix thisMat = new Matrix(vector, 1);
		Matrix rotated = thisMat.times(rotationMatrix);
		return new PamVector(rotated.getRowPackedCopy());
	}
	
	/**
	 * Rotate a vector anti-clockwise by an angle. 
	 * <br> the third dimension of the matrix remains untouched. 
	 * The first two dimensions will rotate anti clockwise by the 
	 * given angle. 
	 * @param rotationAngle rotation angle in radians. 
	 * @return new rotated vector. 
	 */
	public PamVector rotate(double rotationAngle) {
		double[] vd = getVector();
		double[] newV = new double[3];
		double sin = Math.sin(rotationAngle);
		double cos = Math.cos(rotationAngle);
		newV[0] = vd[0] * cos - vd[1] * sin;
		newV[1] = vd[0] * sin + vd[1] * cos;
		newV[2] = vd[2];
		return new PamVector(newV);
	}
	
	/**
	 * Rotate a vector using another vector. 
	 * This is basically a surface rotation in the x,y plane
	 * using the same functionality as the rotate(double rotationAngle)
	 * function, but the input are already in vector form
	 * @param rotationVector
	 * @return
	 */
	public PamVector rotate(PamVector rotationVector) {
		double[] vd = getVector();
		double[] newV = new double[3];
		double sin = rotationVector.getElement(1);
		double cos = rotationVector.getElement(0);
		newV[0] = vd[0] * cos - vd[1] * sin;
		newV[1] = vd[0] * sin + vd[1] * cos;
		newV[2] = vd[2];
		return new PamVector(newV);
	}

	/**
	 * Multiply a vector by a scalar
	 * @param scalar scalar value
	 * @return new vector
	 */
	public PamVector times(double scalar) {
		PamVector newVec = new PamVector(this);
		for (int dim = 0; dim < 3; dim++) {
			newVec.vector[dim] *= scalar;
		}
		return newVec;
	}
	
	/**
	 * Get the unit vector, or if the vector is zero, return 
	 * the zero vector (0,0,0). 
	 * @return the unit vector
	 */
	public PamVector getUnitVector() {
		double sz = norm();
		if (sz == 0) {
			return clone();
		}
		return times(1./sz);
	}
	
	/**
	 * Calculate the angle between two vectors
	 * @param vec other vector
	 * @return angle in radians between 0 and pi
	 */
	public double angle(PamVector vec) {
		PamVector v1, v2;
		v1 = getUnitVector();
		v2 = vec.getUnitVector();
		return Math.acos(v1.dotProd(v2));
	}
	
	/**
	 * Calculates the smallest angle between two vectors (0 <= angle < pi/2);
	 * @param vec other vector
	 * @return angle between two vectors (0 < angle < pi/2) 
	 */
	public double absAngle(PamVector vec) {
		double ang = angle(vec);
		if (ang > Math.PI/2) {
			ang = Math.PI-ang;
		}
		return ang;
	}
	
	/**
	 * 
	 * @param vec other vector
	 * @return square of the distance between two vectors
	 */
	public double distSquared(PamVector vec) {
		double d = 0;
		for (int dim = 0; dim < 3; dim++) {
			d += Math.pow(vec.vector[dim]-this.vector[dim], 2);
		}
		return d;
	}
	
	/**
	 * 
	 * @param vec other vector
	 * @return the distance between two vectors
	 */
	public double dist(PamVector vec) {
		return Math.sqrt(distSquared(vec));
	}
	
	/**
	 * 
	 * @return the magnitude of the vector
	 */
	public double norm() {
		return Math.sqrt(normSquared());
	}
	
	/**
	 * 
	 * @param vec a PamVector
	 * @return the dot product of this and vec
	 */
	public double dotProd(PamVector vec) {
		double a = 0;
		for (int dim = 0; dim < 3; dim++) {
			a += vector[dim]*vec.vector[dim];
		}
		return a;
	}
	
	/**
	 * A bit like a dot product, but the three components are added
	 * in quadrature. This is used to estimate errors along a particular
	 * vector component.
	 * @param vec a Pam Vector
	 * @return sum of components in each direction, added in quadrature. 
	 */
	public double sumComponentsSquared(PamVector vec) {
		double a = 0;
		for (int dim = 0; dim < 3; dim++) {
			a += Math.pow(vector[dim]*vec.vector[dim],2);
		}
		return Math.sqrt(a);
	}
	
	/**
	 * 
	 * @param vec
	 * @return vector product of this and vec.
	 */
	public PamVector vecProd(PamVector vec) {
		PamVector newVec = new PamVector();
		/**
		 * for each dimension, take a determinant which 
		 * is made from the 'other' directions. 
		 */
		newVec.vector[0] = this.vector[1]*vec.vector[2]-this.vector[2]*vec.vector[1];
		newVec.vector[1] = -this.vector[0]*vec.vector[2]+this.vector[2]*vec.vector[0];
		newVec.vector[2] = this.vector[0]*vec.vector[1]-this.vector[1]*vec.vector[0];
		return newVec;
	}
	
	/** 
	 * Calculate the triple product of this with v1 and v2
	 * @param v1 other vector
	 * @param v2 other vector
	 * @return triple produce this.(v1^v2);
	 */
	public double tripleDotProduct(PamVector v1, PamVector v2) {
		PamVector v = v1.vecProd(v2);
		return this.dotProd(v);
	}
	
	/**
	 * Tests to see whether two vectors are parallel
	 * @param vec other vector
	 * @return true if parallel to within 1/1000 radian. 
	 */
	public boolean isParallel(PamVector vec) {
		double ang = angle(vec);
		return (ang < 0.001 || Math.PI-ang < 0.001);
	}
	
	/**
	 * Test to see whether two vectors lie in a perfect line
	 * @param vec other vector
	 * @return true if they line up perfectly
	 */
	public boolean isInLine(PamVector vec) {
		// first check they are parallel
		if (isParallel(vec) == false) {
			return false;
		}
		/*
		 *  then construct a new vector being the difference of the two
		 *  and check that this is also parallel
		 */
		PamVector v = vec.sub(this);
		return v.isParallel(vec);
	}

	/**
	 * 
	 * @return the magnitude of the xy components of the vector - generally 
	 * the magnitude across the sea surface if the vector is in normal coordinates. 
	 */
	public double xyDistance() {
		return Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
	}
	
	public double[] getVector() {
		return vector;
	}

	public void setVector(double[] vector) {
		this.vector = vector;
	}

	/**
	 * Get a single vector element
	 * @param iElement element index
	 * @return value
	 */
	public double getElement(int iElement) {
		return vector[iElement];
	}

	/**
	 * Set a single vector element
	 * @param iElement element index
	 * @param val element value
	 */
	public void setElement(int iElement, double val) {
		this.vector[iElement] = val;
	}

	public static PamVector getXAxis() {
		return xAxis;
	}

	public static PamVector getYAxis() {
		return yAxis;
	}

	public static PamVector getZAxis() {
		return zAxis;
	}

	/**
	 * 
	 * @param iAxis
	 * @return a unit vector along a Cartesian axis (x,y,z)
	 */
	public static PamVector getCartesianAxes(int iAxis) {
		return cartesianAxes[iAxis];
	}

	/**
	 * Create a set of vector pairs, which are vectors between all 
	 * the vectors in the input argument
	 * @param vectors array of vectors
	 * @return array of vectors between all the input vectors 
	 * (creates n*(n-1)/2 new vectors)
	 */
	public static PamVector[] getVectorPairs(PamVector[] vectors) {
		int nVecs = vectors.length;
		int nPairs = nVecs * (nVecs-1) / 2;
		PamVector[] vectorPairs = new PamVector[nPairs];
		int iVec = 0;
		for (int i = 0; i < nVecs; i++) {
			for (int j = i+1; j < nVecs; j++) {
				vectorPairs[iVec++] = vectors[j].sub(vectors[i]);
			}
		}
		return vectorPairs;
	}
	/**
	 * 
	 * @return the axis to which the vector is closest. 
	 */
	public int getPrincipleAxis() {
		int ax = 0;
		double minAng = absAngle(xAxis);
		double ang;
		for (int i = 1; i < 3; i++) {
			ang = absAngle(cartesianAxes[i]);
			if (ang < minAng) {
				minAng = ang;
				ax = i;
			}
		}
		return ax;
	}

	@Override
	public String toString() {
		return String.format("(%2g, %2g, %2g) mag = %2g", vector[0], vector[1], vector[2], norm());
	}
	
	/**
	 * Convert an array of Cartesian vectors into bearings from North. Only the x and y parts of the 
	 * vector are used in this and to be a pain, the bearing is left in radians !
	 * @param vectors Cartesian vector (need not be a unit vector) 
	 * @return clockwise bearings from north in radians  
	 */
	public static double[] vectorsToSurfaceBearings(PamVector[] vectors) {
		double[] bearings = new double[vectors.length];
		for (int i = 0; i < vectors.length; i++) {
			bearings[i] = vectorToSurfaceBearing(vectors[i]);
		}
		return bearings;
	}
	/**
	 * Convert a Cartesian vector into a bearing from North. Only the x and y parts of the 
	 * vector are used in this and to be a pain, the bearing is left in radians !
	 * @param vector Cartesian vector (need not be a unit vector) 
	 * @return clockwise bearing from north in radians  
	 */
	public static double vectorToSurfaceBearing(PamVector vector) {
	  double bearing = Math.atan2(vector.getElement(1), vector.getElement(0));
	  bearing = Math.PI/2. - bearing;
	  return bearing;
	}

	/**
	 * Normalise a vector so it has a magnitue of 1.
	 * if the vector has magnitude 0, it is left with this magnitude
	 * and 0 is returned. 
	 * @return 0 if the vector had zero magnitude, a otherwise. 
	 */
	public double normalise() {
		double mag = norm();
		if (mag == 0) {
			return 0.;
		}
		for (int i = 0; i < 3; i++) {
			vector[i] /= mag;
		}
		return 1.;
	}
	
	/**
	 * Make all elements within the vector positive. This can be used to simplify symmetric problems. 
	 * @return The vector but with all elements made positive. 
	 */
	public PamVector absElements(){
		double[] vd = getVector();
		double[] newV = new double[3];
		for (int dim = 0; dim < 3; dim++) {
			newV[dim]=Math.abs(vd[dim]);
		}
		return new PamVector(newV);
	}
	
}
