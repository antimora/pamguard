package dataGram;

import dataMap.OfflineDataMapPoint;
import PamguardMVC.PamDataBlock;

public class DatagramProgress {

	public static final int STATUS_BLOCKCOUNT = 0;
	public static final int STATUS_STARTINGBLOCK  = 1;
	public static final int STATUS_ENDINGBLOCK  = 2;
	public static final int STATUS_STARTINGFILE = 3;
	public static final int STATUS_ENDINGFILE = 4;
	
	public int nDataBlocks;
	
	public int statusType;
	
	public int pointsToUpdate;
	
	public int currentPoint;
	
	public PamDataBlock dataBlock;
	
	public OfflineDataMapPoint dataMapPoint;

	/**
	 * @param statusType
	 * @param nDataBlocks
	 */
	public DatagramProgress(int statusType, int nDataBlocks) {
		super();
		this.statusType = statusType;
		this.nDataBlocks = nDataBlocks;
	}

	/**
	 * @param statusType
	 * @param dataBlock
	 */
	public DatagramProgress(int statusType, PamDataBlock dataBlock, int pointsToUpdate) {
		super();
		this.statusType = statusType;
		this.dataBlock = dataBlock;
		this.pointsToUpdate = pointsToUpdate;
	}

	/**
	 * @param statusType
	 * @param dataMapPoint
	 */
	public DatagramProgress(int statusType, OfflineDataMapPoint dataMapPoint, int currentPoint) {
		super();
		this.statusType = statusType;
		this.dataMapPoint = dataMapPoint;
		this.currentPoint = currentPoint;
	}
	
	
	
}
