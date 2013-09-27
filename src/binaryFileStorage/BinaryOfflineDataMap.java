package binaryFileStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import dataMap.OfflineDataMap;

import PamguardMVC.PamDataBlock;

/**
 * A binary offline data map is a datamap for a single data stream (i.e. the output 
 * of a PamDataBlock). <br>
 * For each binary file, one map point will be created in a list within the datamap 
 * which gives basic information about the data within that time period. The data
 * include the headers and footers read from either the data file or the index file and 
 * possibly also a Datagram - which gives more detail of the data than the simple counts
 * of detections. 
 * <br>Individual modules may also override the data map points within the datamap in order
 * to provide more detailed information, such as numbers of clicks splitup by species. 
 * @author Doug Gillespie
 *
 */
public class BinaryOfflineDataMap extends OfflineDataMap<BinaryOfflineDataMapPoint> {

	transient private BinaryDataSource binaryDataSource;
	transient private BinaryStore binaryStore;

	public BinaryOfflineDataMap(BinaryStore binaryStore, PamDataBlock parentDataBlock) {
		super(binaryStore, parentDataBlock);
		this.binaryStore = binaryStore;
		binaryDataSource = parentDataBlock.getBinaryDataSource();
	}

	/**
	 * Get a list of map points which have data between the two times. 
	 * @param dataStart start time in milliseconds
	 * @param dataEnd end time in milliseconds
	 * @return Array list of map points
	 */
	protected ArrayList<BinaryOfflineDataMapPoint> getFileList(long dataStart, long dataEnd) {
		ArrayList<BinaryOfflineDataMapPoint> fileList = new ArrayList<BinaryOfflineDataMapPoint>();
		Iterator<BinaryOfflineDataMapPoint> list = getListIterator();
		BinaryOfflineDataMapPoint aMapPoint;
		while (list.hasNext()) {
			aMapPoint = list.next();
			if (aMapPoint.coincides(dataStart, dataEnd)) {
				fileList.add(aMapPoint);
			}
		}
		return fileList;
	}
	
	
	
	/**
	 * Finds the mapPoint of a specific binary file 
	 * @param file
	 * @return
	 */
	public BinaryOfflineDataMapPoint findMapPoint(File file) {
		if (file == null) {
			return null;
		}
		Iterator<BinaryOfflineDataMapPoint> list = getListIterator();
		BinaryOfflineDataMapPoint aMapPoint;
		while (list.hasNext()) {
			aMapPoint = list.next();
			if (file.getName().equals(aMapPoint.getName())) {
				return aMapPoint;
			}
		}	
		return null;
	}

}
