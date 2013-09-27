package dataGram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.SwingWorker;

import binaryFileStorage.BinaryOfflineDataMap;
import binaryFileStorage.BinaryOfflineDataMapPoint;
import binaryFileStorage.BinaryStore;

import PamController.AWTScheduler;
import PamController.PamController;
import PamView.CancelObserver;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class DatagramManager {


	private DatagramSettings datagramSettings = new DatagramSettings();

	private BinaryStore binaryStore;

	/**
	 * @param binaryOfflineDataMap
	 */
	public DatagramManager(BinaryStore binaryStore) {
		super();
		this.binaryStore = binaryStore;
	}

	/**
	 * 
	 * @return a list of data blocks which have a DatagramProvider
	 */
	public ArrayList<PamDataBlock> getDataBlocks() {
		ArrayList<PamDataBlock> allDataBlocks = PamController.getInstance().getDataBlocks();
		int n = allDataBlocks.size();
		ArrayList<PamDataBlock> usedBlocks = new ArrayList<PamDataBlock>();
		for (int i = 0; i < n; i++) {
			if (allDataBlocks.get(i).getDatagramProvider() != null) {
				usedBlocks.add(allDataBlocks.get(i));
			}
		}
		return usedBlocks;
	}

	/**
	 * 
	 * @return a list of datablocks which need their datagrams updated. 
	 */
	public ArrayList<PamDataBlock> checkAllDatagrams() {
		ArrayList<PamDataBlock> grammedDataBlocks = getDataBlocks();
		ArrayList<PamDataBlock> updateBlocks = new ArrayList<PamDataBlock>();
		for (int i = 0; i < grammedDataBlocks.size(); i++) {
			if (checkDatagram(grammedDataBlocks.get(i))) {
				updateBlocks.add(grammedDataBlocks.get(i));
			}
		}
		return updateBlocks;
	}

	/**
	 * Check that every data map point associated with this datablock has 
	 * a datagram and that the datagram entries are of the right length. 
	 * Return true if the datagram needs updating
	 * @param pamDataBlock datablock to check
	 * @return true if Datagram needs updating, false otherwise. 
	 */
	public boolean checkDatagram(PamDataBlock pamDataBlock) {
		DatagramProvider datagramProvider = pamDataBlock.getDatagramProvider();
		if (datagramProvider == null) {
			return false;
		}
		BinaryOfflineDataMap dm = (BinaryOfflineDataMap) pamDataBlock.getOfflineDataMap(binaryStore);
		if (dm == null) {
			return false;
		}
		int nMapPoints = dm.getNumMapPoints();
		BinaryOfflineDataMapPoint dmp;
		Datagram datagram;
		Iterator<BinaryOfflineDataMapPoint> it = dm.getListIterator();
		while (it.hasNext()) {
			dmp = it.next();
			datagram = dmp.getDatagram();
			if (datagram == null || datagram.getIntervalSeconds() != datagramSettings.datagramSeconds) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Update all datagrams (which need it)
	 */
	public void updateDatagrams() {
		ArrayList<PamDataBlock> updateList = checkAllDatagrams();
		if (updateList != null && updateList.size() > 0) {
			updateDatagrams(updateList);
		}
	}

	private volatile DatagramProgressDialog datagramProgressDialog;
	/**
	 * update a list of datagrams. This should be done in a 
	 * worker thread ...
	 * @param updateList
	 */
	public void updateDatagrams(ArrayList<PamDataBlock> updateList) {
		DatagramCreator datagramCreator = new DatagramCreator(updateList);
		AWTScheduler.getInstance().scheduleTask(datagramCreator);
	}

	class DatagramCreator extends SwingWorker<Integer, DatagramProgress> implements CancelObserver {

		private ArrayList<PamDataBlock> updateList;

		private volatile boolean cancelNow = false;
		/**
		 * @param updateList
		 */
		public DatagramCreator(ArrayList<PamDataBlock> updateList) {
			super();
			this.updateList = updateList;
		}

		@Override
		protected Integer doInBackground() {
			try {
				publish(new DatagramProgress(DatagramProgress.STATUS_BLOCKCOUNT, updateList.size()));

				for (int i = 0; i < updateList.size(); i++) {
					processDataBlock(updateList.get(i));
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}

		private void processDataBlock(PamDataBlock pamDataBlock) {
			DatagramProvider datagramProvider = pamDataBlock.getDatagramProvider();
			if (datagramProvider == null) {
				return;
			}
			BinaryOfflineDataMap dm = (BinaryOfflineDataMap) pamDataBlock.getOfflineDataMap(binaryStore);
			if (dm == null) {
				return;
			}

			int nMapPoints = dm.getNumMapPoints();
			BinaryOfflineDataMapPoint dmp;
			Datagram datagram;
			/**
			 * Loop through a first time to see now many map points need updating. 
			 */
			Iterator<BinaryOfflineDataMapPoint> it = dm.getListIterator();
			int nToUpdate = 0;
			while (it.hasNext()) {
				dmp = it.next();
				datagram = dmp.getDatagram();
				if (datagram == null || datagram.getIntervalSeconds() != datagramSettings.datagramSeconds) {
					nToUpdate++;;
				}
			}
			publish(new DatagramProgress(DatagramProgress.STATUS_STARTINGBLOCK, pamDataBlock, nToUpdate));
			/*
			 * then loop through again and update the actual map points...
			 */
			it = dm.getListIterator();
			int iPoint = 0;
			while (it.hasNext()) {
				dmp = it.next();
				datagram = dmp.getDatagram();
				if (datagram == null || datagram.getIntervalSeconds() != datagramSettings.datagramSeconds) {
					publish(new DatagramProgress(DatagramProgress.STATUS_STARTINGFILE, dmp, ++iPoint));
					processDataMapPoint(pamDataBlock, dmp);
					publish(new DatagramProgress(DatagramProgress.STATUS_ENDINGFILE, dmp, iPoint));
				}
				if (cancelNow) {
					break;
				}
			}

			publish(new DatagramProgress(DatagramProgress.STATUS_ENDINGBLOCK, pamDataBlock, nToUpdate));
		}

		/**
		 * Process a single data map point
		 * @param dataBlock
		 * @param dmp
		 */
		private void processDataMapPoint(PamDataBlock dataBlock, BinaryOfflineDataMapPoint dmp) {
			long startTime = dmp.getStartTime();
			long endTime = dmp.getEndTime();
			DatagramProvider datagramProvider = dataBlock.getDatagramProvider();
			int nPoints = datagramProvider.getNumDataGramPoints();
			double[] tempData = new double[nPoints]; // temp holder - gets converted to float later on
			float[] gramData;

			/**
			 * Create a new datagram class which will be given to this individual dmp
			 */
			Datagram datagram = new Datagram(datagramSettings.datagramSeconds);
			long datagramMillis = datagramSettings.datagramSeconds*1000;
			long currentStart = startTime;
			long currentEnd = currentStart + datagramMillis;
			DatagramDataPoint datagramPoint;
			/*
			 * first load all the data from a single file ...
			 */
			dataBlock.clearAll();
			Runtime.getRuntime().gc();
			dataBlock.loadViewerData(startTime, endTime, null);
			ListIterator<PamDataUnit> li = dataBlock.getListIterator(0);
			PamDataUnit dataUnit;
			int usedDataUnits = 0;
			while (currentStart <= endTime) {
				datagramPoint = new DatagramDataPoint(datagram, currentStart, currentStart+datagramMillis, nPoints);
				usedDataUnits = 0;
				while (li.hasNext()) {
					dataUnit = li.next();
					if (dataUnit.getTimeMilliseconds() >= currentEnd) {
						break;
					}
					datagramProvider.addDatagramData(dataUnit, tempData);
					usedDataUnits++;
					
					if (cancelNow) {
						return;
					}
				}
				/**
				 * end the current datagram
				 */
				if (usedDataUnits > 0) {
					gramData = datagramPoint.getData(); 
					for (int i = 0; i < nPoints; i++) {
						gramData[i] = (float) tempData[i];
						tempData[i] = 0;
					}
					datagramPoint.setData(gramData, usedDataUnits);
				}
				datagram.addDataPoint(datagramPoint);
				currentStart = currentEnd;
				currentEnd = currentStart+datagramMillis;
			}
			dmp.setDatagram(datagram);
			
			binaryStore.rewriteIndexFile(dataBlock, dmp);

		}


		@Override
		protected void process(List<DatagramProgress> chunks) {
			for (int i = 0; i < chunks.size(); i++) {
				showProgress(chunks.get(i), this);
			}
		}

		@Override
		protected void done() {
			if (datagramProgressDialog != null) {
				datagramProgressDialog.setVisible(false);
				datagramProgressDialog = null;
			}
		}

		@Override
		public boolean cancelPressed() {
			cancelNow = true;
			return true;
		}

	}

	public void showProgress(DatagramProgress datagramProgress, DatagramCreator datagramCreator) {
		if (datagramProgressDialog == null) {
			datagramProgressDialog = DatagramProgressDialog.showDialog(null, datagramCreator);
		}
		datagramProgressDialog.setProgress(datagramProgress);
	}

	/**
	 * @return the datagramSettings
	 */
	public DatagramSettings getDatagramSettings() {
		return datagramSettings;
	}

	/**
	 * @param datagramSettings the datagramSettings to set
	 */
	public void setDatagramSettings(DatagramSettings datagramSettings) {
		this.datagramSettings = datagramSettings;
	}

	/**
	 * Create the raw data to go into a dataGram. 
	 * Fill with NaN wherever data are unavailable.  
	 * @param dataBlock
	 * @param startTimeMills
	 * @param endTimeMillis
	 * @param maxPixels
	 * @return a 2D array of double precision data to go into an image. 
	 */
	public synchronized DatagramImageData getImageData(PamDataBlock dataBlock, long startTimeMillis, long endTimeMillis, int maxPixels) {
		BinaryOfflineDataMap binaryDataMap = (BinaryOfflineDataMap) dataBlock.getOfflineDataMap(binaryStore);
		if (binaryDataMap == null) {
			return null;
		}
		long datagramMillis = datagramSettings.datagramSeconds * 1000;
		// round the start and end times up and down a little ...
		startTimeMillis /= datagramMillis;
		startTimeMillis *= datagramMillis;
		endTimeMillis += datagramMillis - 1;
		endTimeMillis /= datagramMillis;
		endTimeMillis *= datagramMillis;
		
		// first work out how many points there should naturally be ...
		int nTimePoints = (int) ((endTimeMillis-startTimeMillis)/datagramMillis);
		if (nTimePoints > maxPixels) {
			nTimePoints = maxPixels;
		}
		if (nTimePoints < 1) {
			nTimePoints = 1;
		}
		double xScale = (endTimeMillis - startTimeMillis) / nTimePoints;
		// work out the number of points in y
		DatagramProvider dp = dataBlock.getDatagramProvider();
		if (dp == null) return null;
		int nYPoints = dp.getNumDataGramPoints();
		double[][] datagramArray = new double[nTimePoints][nYPoints];
		int[][] scaleCount = new int[nTimePoints][nYPoints];
		

		Iterator<BinaryOfflineDataMapPoint> iterator = binaryDataMap.getListIterator();
		BinaryOfflineDataMapPoint mapPoint;
		long pointStart, pointEnd;
		int xBin;
		Datagram datagram;
		DatagramDataPoint dataPoint;
		int nDataPoints;
		float[] data;
		while (iterator.hasNext()) {
			mapPoint = iterator.next();
			pointStart = mapPoint.getStartTime();
			pointEnd = mapPoint.getEndTime();
			if (pointEnd < startTimeMillis - datagramSettings.datagramSeconds*5000) {
				continue;
			}
			if (pointStart > endTimeMillis) {
				break;
			}
			datagram = mapPoint.getDatagram();
			if (datagram == null) {
				continue;
			}
//			System.out.println("Add map point starting at " + PamCalendar.formatDateTime(mapPoint.getStartTime()));
			nDataPoints = datagram.getNumDataPoints();
			for (int i = 0; i < nDataPoints; i++) {
				dataPoint = datagram.getDataPoint(i);
//				System.out.println("   .... add Datagram point starting at " + 
//						PamCalendar.formatDateTime(dataPoint.getStartTime()));
				xBin = (int)((dataPoint.getStartTime()-startTimeMillis)/xScale);
				if (xBin < 0) continue;
				if (xBin >= nTimePoints) break;
				data = dataPoint.getData();
				for (int j = 0; j < Math.min(nYPoints, data.length); j++) {
					datagramArray[xBin][j] += data[j];
					scaleCount[xBin][j] ++;
				}
			}
		}
		/**
		 * now scale any bins with > 1 entry and set empty ones to -1.
		 */
		int n;
		for (int i = 0; i < nTimePoints; i++) {
			for (int j = 0; j < nYPoints; j++) {
				n = scaleCount[i][j];
				if (n == 0) {
					datagramArray[i][j] = -1;
				}
				else if (n > 0) {
					datagramArray[i][j] = datagramArray[i][j]/n;
				}
			}
		}
		return new DatagramImageData(startTimeMillis, endTimeMillis, datagramArray);
	}

}
