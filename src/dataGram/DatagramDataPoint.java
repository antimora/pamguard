package dataGram;

import java.io.Serializable;

public class DatagramDataPoint implements Serializable {

	private static final long serialVersionUID = 1L;

	private Datagram datagram;
	private long startTime;
	private long endTime;
	private float[] data;
	private int nDataUnits;
	
	/**
	 * @param datagram
	 * @param startTime
	 * @param endTime
	 */
	public DatagramDataPoint(Datagram datagram, long startTime, long endTime, int dataLength) {
		super();
		this.datagram = datagram;
		this.startTime = startTime;
		this.endTime = endTime;
		data = new float[dataLength];
	}

	/**
	 * @return the datagram
	 */
	public Datagram getDatagram() {
		return datagram;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @return the data
	 */
	public float[] getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(float[] data, int nDataPoints) {
		this.data = data;
		this.nDataUnits = nDataPoints;
	}
	
	
}
