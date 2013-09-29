package dataMap;

import java.io.Serializable;

/**
 * Map points to go into an OfflineDataMap. 
 * @author Doug Gillespie
 *
 */
abstract public class OfflineDataMapPoint implements Comparable<OfflineDataMapPoint>, Serializable,Cloneable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @return a name for the map point
	 */
	abstract public String getName();
	
	/**
	 * Start time of map point data
	 */
	private long startTime;
	
	/**
	 * End time of map point data
	 */
	private long endTime;
	
	/**
	 * Number of data points. 
	 */
	private int nDatas;
	
	public OfflineDataMapPoint(long startTime, long endTime, int nDatas) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.nDatas = nDatas;
	}

	@Override
	public OfflineDataMapPoint clone(){
		OfflineDataMapPoint a;
		try{
			a= (OfflineDataMapPoint) super.clone();
		}catch(Exception e){
			a= null;
			e.printStackTrace();
		}
		return a;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the nDatas
	 */
	public int getNDatas() {
		return nDatas;
	}

	/**
	 * @param datas the nDatas to set
	 */
	public void setNDatas(int datas) {
		nDatas = datas;
	}
	
	/**
	 * Test whether or not this map point overlaps with 
	 * a pair of times. 
	 * @param startTime start time in milliseconds
	 * @param endTime end time in milliseconds. 
	 * @return true if there is any overlap.
	 */
	public boolean coincides(long startTime, long endTime) {
		if (endTime < getStartTime() || startTime > getEndTime()) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(OfflineDataMapPoint o) {
		/**
		 * Need to return an int - avoid long wrap around, so
		 * don't just do a cast !
		 */
		long tDiff = startTime - o.getStartTime();
		if (tDiff < 0) {
			return -1;
		}
		else if (tDiff > 0) {
			return 1;
		}
		else {
			return 0;
		}
	}
	
}
