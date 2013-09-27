package binaryFileStorage;

import PamUtils.PamCalendar;

/**
 * Class to temporarily hold an objected data that has just been read from 
 * a binary file. Not all types of object use all the fields and data don't
 * tend to stay in this class for long - it's just a convenient wasy of getting
 * data from a to b. 
 * @author Doug Gillespie
 *
 */
public class BinaryObjectData {

	/**
	 * Object type (-1 to -4 for main headers and footers, > 0
	 * for proprietary data formats for data objects)
	 */
	private int objectType;
	
	/**
	 * Object time - for data (not in headers and footers)
	 */
	private long timeMillis;
	
	/**
	 * Binary data - in whatever format
	 */
	private byte[] data;
	
	/**
	 * Length of data (often data.length)
	 */
	private int dataLength;

	/**
	 * Number of the object within the file
	 */
	private int objectNumber;
	
	/**
	 * Version number from the moduleHeader - not the main
	 * pgdf version number or format but something more module 
	 * specific. 
	 */
	private int versionNumber;


	@Override
	public String toString() {
		return String.format("Obj %d, Type %d, Time %s, DataLen %d, arrayLen %d",
				objectNumber, objectType, PamCalendar.formatDate(timeMillis),
				dataLength, data.length);
	}


	public BinaryObjectData(int objectType, byte[] data,
			int dataLength) {
		super();
		this.objectType = objectType;
		this.timeMillis = 0;
		this.data = data;
		this.dataLength = dataLength;
	}


	public BinaryObjectData(int objectType, long timeMillis, int objectNumber,  byte[] data,
			int dataLength) {
		super();
		this.objectType = objectType;
		this.timeMillis = timeMillis;
		this.objectNumber = objectNumber;
		this.data = data;
		this.dataLength = dataLength;
	}


	/**
	 * @return the objectType
	 */
	public int getObjectType() {
		return objectType;
	}


	/**
	 * @return the timeMillis
	 */
	public long getTimeMillis() {
		return timeMillis;
	}


	/**
	 * @return the data. Note that the data array may be longer than the
	 * actual data within it. Use getDataLength() to check.
	 */
	public byte[] getData() {
		return data;
	}


	/**
	 * @return the dataLength
	 */
	public int getDataLength() {
		return dataLength;
	}


	/**
	 * @return the objectNumber
	 */
	public int getObjectNumber() {
		return objectNumber;
	}


	/**
	 * @return the versionNumber
	 */
	public int getVersionNumber() {
		return versionNumber;
	}


	/**
	 * @param versionNumber the versionNumber to set
	 */
	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}
	
	
}
