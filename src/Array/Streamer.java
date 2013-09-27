package Array;

import java.io.Serializable;

import networkTransfer.receive.BuoyStatusDataUnit;

import pamMaths.PamVector;

/**
 * Contains information on an individual streamer - i.e. something towed 
 * through the water. Each hydrophone element will belong to one streamer. 
 * @author Doug Gillespie
 *
 */
public class Streamer implements Serializable, Cloneable {


	public static final long serialVersionUID = 1L;

	/**
	 * Streamer Id;
	 */
//	private int Id;
	
	/**
	 * Streamer coordinates. 
	 */
	private double[] coordinate = new double[3];
	
	/**
	 * Errors on streamer location in x,y,z
	 */
	private double[] coordinateError = new double[3];
	
	/**
	 * Identifier to use with PAMbuoy. 
	 */
	private Integer buoyId1;
	
	private transient BuoyStatusDataUnit buoyStats;

	/**
	 * 
	 * @param x    x coordinate of streamer (m)
	 * @param dx   estimated error on x location of streamer (m)
	 * @param dy   estimated error on y location of streamer (m)
	 * @param dz   estimated error on z location of streamer (m)
	 */
	public Streamer(double x, double y, double z, double dx, double dy, double dz) {
		super();
		checkAllocations();
		coordinate[0] = x;
		coordinate[1] = y;
		coordinate[2] = z;
		coordinateError[0] = dx;
		coordinateError[1] = dy;
		coordinateError[2] = dz;
	}
	
	private void checkAllocations() {
		if (coordinate == null) {
			coordinate = new double[3];
		}
		if (coordinateError == null) {
			coordinateError = new double[3];
		}
	}

	public Streamer() {
		super();
	}

	// copy constructor 
	public Streamer(Streamer firstStreamer, int buoyId) {
		this.coordinate = firstStreamer.coordinate.clone();
		this.coordinateError = firstStreamer.coordinateError.clone();
		this.buoyId1 = buoyId;
	}

	@Override
	protected Streamer clone()  {
		try {
			Streamer newStreamer =  (Streamer) super.clone();
			newStreamer.checkAllocations();
			return newStreamer;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

//	public int getId() {
//		return Id;
//	}
	
	public void setCoordinates(double[] coordinates) {
		this.coordinate = coordinates;
	}
	
	public void setCoordinate(int iCoordinate, double coordinate) {
		checkAllocations();
		this.coordinate[iCoordinate] = coordinate;
	}
	
	public double[] getCoordinates() {
		checkAllocations();
		return coordinate;
	}
	
	public double getCoordinate(int iCoordinate) {
		checkAllocations();
		return coordinate[iCoordinate];
	}
	
	public double[] getCoordinateError() {
		checkAllocations();
		return coordinateError;
	}
	
	public double getCoordinateError(int iCoordinate) {
		checkAllocations();
		return coordinateError[iCoordinate];
	}
	
	public void setCoordinateErrors(double[] coordinateErrors) {
		this.coordinateError = coordinateErrors;
	}
	
	public void setCoordinateError(int iCoordinate, double error) {
		checkAllocations();
		this.coordinateError[iCoordinate] = error;
	}
	
	public PamVector getCoordinateVector() {
		return new PamVector(coordinate);
	}
	
	public PamVector getErrorVector() {
		return new PamVector(coordinateError);
	}

	public void setX(double x) {
		checkAllocations();
		coordinate[0] = x;
	}
	
	public void setY(double x) {
		checkAllocations();
		coordinate[1] = x;
	}
	
	public void setZ(double x) {
		checkAllocations();
		coordinate[2] = x;
	}

	public void setDx(double dx) {
		checkAllocations();
		coordinateError[0] = dx;
	}

	public void setDy(double dy) {
		checkAllocations();
		coordinateError[1] = dy;
	}

	public void setDz(double dz) {
		checkAllocations();
		coordinateError[2] = dz;
	}

	public double getX() {
		checkAllocations();
		return coordinate[0];
	}
	
	public double getY() {
		checkAllocations();
		return coordinate[1];
	}
	
	public double getZ() {
		checkAllocations();
		return coordinate[2];
	}

	public double getDx() {
		checkAllocations();
		return coordinateError[0];
	}

	public double getDy() {
		checkAllocations();
		return coordinateError[1];
	}

	public double getDz() {
		checkAllocations();
		return coordinateError[2];
	}

	/**
	 * @param buoyId1 the buoyId1 to set
	 */
	public void setBuoyId1(Integer buoyId1) {
		this.buoyId1 = buoyId1;
	}

	/**
	 * @return the buoyId1
	 */
	public Integer getBuoyId1() {
		return buoyId1;
	}

	/**
	 * @param buoyStats the buoyStats to set
	 */
	public void setBuoyStats(BuoyStatusDataUnit buoyStats) {
		this.buoyStats = buoyStats;
		this.buoyId1 = buoyStats.getBuoyId1();
	}

	/**
	 * @return the buoyStats
	 */
	public BuoyStatusDataUnit getBuoyStats() {
		return buoyStats;
	}
	
	
}
