package pamScrollSystem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import PamController.AWTScheduler;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;
import dataMap.DataMapControl;
import dataMap.OfflineDataMap;

public class ViewerScrollerManager extends AbstractScrollManager implements PamSettings {

	private boolean initialisationComplete;

	private boolean intialiseLoadDone;

	private StoredScrollerData oldScrollerData = new StoredScrollerData();

	public ViewerScrollerManager() {
		PamSettingManager.getInstance().registerSettings(this);
	}

	/* (non-Javadoc)
	 * @see pamScrollSystem.AbstractScrollManager#addPamScroller(pamScrollSystem.AbstractPamScroller)
	 */
	@Override
	public void addPamScroller(AbstractPamScroller pamScroller) {
		super.addPamScroller(pamScroller);
		if (oldScrollerData != null && initialisationComplete) {
			PamScrollerData oldData = oldScrollerData.findScrollerData(pamScroller);
			if (oldData != null) {
				pamScroller.scrollerData = oldData.clone();
			}
		}
	}


	@Override
	public void moveInnerScroller(AbstractPamScroller scroller, long newValue) {
		AbstractPamScroller aScroller;
		if (oldScrollerData.coupleAllScrollers) {
			followCoupledScroller(scroller);
		}
		for (int i = 0; i < pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			if (aScroller == scroller) {
				continue;
			}
			aScroller.anotherScrollerMovedInner(newValue);
		}
	}

	@Override
	public void moveOuterScroller(AbstractPamScroller scroller, long newMin,
			long newMax) {
		AbstractPamScroller aScroller;
		for (int i = 0; i < pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			if (aScroller == scroller) {
				continue;
			}
			aScroller.anotherScrollerMovedOuter(newMin, newMax);
		}
		loadData(false);
	}

	private volatile boolean loaderRunning = false;

	private DataLoader dataLoader;

	@Override
	public void reLoad() {
		loadData(false);
	}

	/**
	 * Wait for the data loader to complete. 
	 * 
	 * @param timeOut maximum time to wait for in milliseconds. Enter 0 to wait forever.  
	 * @return true if data loader is not running or stops, false if 
	 * a timeout occurs. 
	 */
//	public boolean waitForLoader(long timeOut) {
//		long now = System.currentTimeMillis();
//		try {
//			while (loaderRunning) {
//				Thread.sleep(10);
//				if (timeOut > 0) {
//					if (System.currentTimeMillis() > now+timeOut) {
//						break;
//					}
//				}	
//			}
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return (!loaderRunning);
//	}
	/**
	 * loads data after a scroller has changed. 
	 * @param immediateLoad 
	 */
	public synchronized void loadData(boolean immediateLoad) {
		/**
		 * will need to stop this getting called multiple times
		 * when several scroll bars move. 
		 */
		if (loaderRunning) {
			//			return;
			//			if (dataLoader != null) {
			//				return;
			//			}
			//			dataLoader.cancel(true);
		}
		// checks what everyone wants and needs, then loads the
		// appropriate data. 
		/*
		 *  first loop through data blocks, then for
		 *  every data block loop through all scrollers and 
		 *  for srollers which use that data block work out
		 *  the min and max times for data in that block then 
		 *  load the data.  
		 */
		ArrayList<PamDataBlock> dataBlocks = PamController.getInstance().getDataBlocks();
		ArrayList<DataLoadQueData> dataLoadQueue = new ArrayList<DataLoadQueData>();
		//		dataLoadQueu.clear();
		for (int i = 0; i < dataBlocks.size(); i++) {
			checkLoadLimits(dataLoadQueue, dataBlocks.get(i));
		}
		if (dataLoadQueue.size() > 0) {
			loaderRunning = true;
			if (immediateLoad) {
				loadDataQueue(dataLoadQueue);
			}
			else {
				scheduleDataQueue(dataLoadQueue);
			}
		}

	}
	
	/**
	 * Load the data queue immediately in the current thread. 
	 * @param dataLoadQueue
	 */
	private void loadDataQueue(ArrayList<DataLoadQueData> dataLoadQueue) {
		int n = dataLoadQueue.size();
		for (int i = 0; i < n; i++) {
			loadDataQueueItem(dataLoadQueue.get(i), i, null);
		}
		loadDone();
	}

	/**
	 * Load the data from a single object in the queue. 
	 * Generally called from within the worker thread. 
	 * @param dataLoadQueData
	 * @param i 
	 */
	private void loadDataQueueItem(DataLoadQueData dataLoadQueData, int queuePosition, ViewLoadObserver loadObserver) {
		if (dataLoadQueData.getDataStart() <= 0 || dataLoadQueData.getDataStart() == Long.MAX_VALUE) {
			return;
		}
		dataLoadQueData.getPamDataBlock().loadViewerData(dataLoadQueData.getDataStart(),
				dataLoadQueData.getDataEnd(), loadObserver);
	}
	/**
	 * Schedule the data queue to be loaded in a separate thread. 
	 * @param dataLoadQueue
	 */
	private void scheduleDataQueue(ArrayList<DataLoadQueData> dataLoadQueue) {
		AWTScheduler.getInstance().scheduleTask(dataLoader = new DataLoader(dataLoadQueue));
	}

	private void loadDone() {
		for (int i = 0; i < pamScrollers.size(); i++) {
			pamScrollers.get(i).notifyRangeChange();
		}
		PamController.getInstance().notifyModelChanged(PamControllerInterface.OFFLINE_DATA_LOADED);
	}

	/**
	 * check the required load limits for a data block which 
	 * may be being used my multiple scrollers. 
	 * @param pamDataBlock datablock to check
	 */
	private void checkLoadLimits(ArrayList<DataLoadQueData> dataLoadQueue, PamDataBlock pamDataBlock) {
		long minTime = Long.MAX_VALUE;
		long maxTime = Long.MIN_VALUE;
		AbstractPamScroller aScroller;
		boolean used = false;
		for (int i = 0; i < this.pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			if (aScroller.isDataBlockUsed(pamDataBlock)) {
				minTime = Math.min(minTime, aScroller.getMinimumMillis());
				maxTime = Math.max(maxTime, aScroller.getMaximumMillis());
				used = true;
			}
		}
		if (used) {
			dataLoadQueue.add(new DataLoadQueData(pamDataBlock, minTime, maxTime));
		}
	}

	class DataLoader extends SwingWorker<Integer, LoadQueueProgressData> implements ViewLoadObserver{

		private ArrayList<DataLoadQueData> dataLoadQueue;

		private LoadingDataDialog loadingDataDialog;

		private volatile boolean emergencyStop = false;

		public DataLoader(ArrayList<DataLoadQueData> dataLoadQueue) {
			super();
			this.dataLoadQueue = dataLoadQueue;
		}

		@Override
		protected Integer doInBackground() throws Exception {
			try {
				for (int i = 0; i < dataLoadQueue.size(); i++) {
					//				System.out.println("Start loading " + 
					//						dataLoadQueu.get(i).getPamDataBlock().getDataName());
					
					LoadQueueProgressData lpd = new LoadQueueProgressData("", 
							dataLoadQueue.get(i).getPamDataBlock().getDataName(), 
							dataLoadQueue.size(), i, 0, 0, 0, 0, 0);
					publish(lpd);
					loadDataQueueItem(dataLoadQueue.get(i), i, this);
				}
				// need to set this here so that waitForLoader() can execute in the AWT thread. 
				loaderRunning = false;
			}
			catch (Exception e){
				System.out.println("Error in Viewer Scroller data loader");
				e.printStackTrace();
			}
			PamController.getInstance().notifyModelChanged(PamControllerInterface.DATA_LOAD_COMPLETE);
			return null;
		}

		@Override
		protected void done() {
			super.done();
			loaderRunning = false;
			if (loadingDataDialog != null) {
				loadingDataDialog.closeLater();
//				loadingDataDialog.setVisible(false);
			}
			loadDone();
		}

		@Override
		protected void process(List<LoadQueueProgressData> chunks) {
			if (loadingDataDialog == null) {
				loadingDataDialog = LoadingDataDialog.showDialog(PamController.getMainFrame());
			}
			if (loadingDataDialog != null) {
				for (LoadQueueProgressData lpd:chunks) {
					loadingDataDialog.setData(lpd);
				}
				emergencyStop = loadingDataDialog.shouldStop();
			}
		}

		@Override
		public void sayProgress(int state, long loadStart, long loadEnd,
				long lastTime, int nLoaded) {
			LoadQueueProgressData lpd = new LoadQueueProgressData(null, 
						null, 0, 0, state, loadStart, loadEnd, lastTime, nLoaded);
				publish(lpd);
			
		}

		/* (non-Javadoc)
		 * @see pamScrollSystem.ViewLoadObserver#cancelLoad()
		 */
		@Override
		public boolean cancelLoad() {
			return emergencyStop ;
		}


	}

	private DataMapControl findDataMapControl() {
		return DataMapControl.getDataMapControl();
	}

	@Override
	public long checkMaximumTime(long requestedTime) {
		DataMapControl dmc = findDataMapControl();
		if (dmc == null) {
			return 0;
		}
		return Math.min(dmc.getLastTime(), requestedTime);
	}

	@Override
	public long checkMinimumTime(long requestedTime) {
		DataMapControl dmc = findDataMapControl();
		if (dmc == null) {
			return 0;
		}
		return Math.max(dmc.getFirstTime(), requestedTime);
	}

	@Override
	public void notifyModelChanged(int changeType) {
		switch (changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			initialisationComplete = true;
			break;
		case PamControllerInterface.INITIALIZE_LOADDATA:
			intialiseLoadDone = true;
		case PamControllerInterface.CHANGED_OFFLINE_DATASTORE:
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
			if (initialisationComplete && intialiseLoadDone) {
				initialiseScrollers();
			}
			break;
		}	
	}


	/**
	 * Called once at the start, and possibly after 
	 * any changes to the database or binary store. 
	 * Initialises scroll bars and calls for a data load. 
	 */
	private void initialiseScrollers() {
		DataMapControl dmc = findDataMapControl();
		if (dmc == null) {
			return;
		}
		AbstractPamScroller aScroller;
		PamScrollerData oldData;
		for (int i = 0; i < pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			if (oldScrollerData != null) {
				oldData = oldScrollerData.findScrollerData(aScroller);
			}
			else {
				oldData = null;
			}
			if (oldData == null) {
				aScroller.setRangeMillis(dmc.getFirstTime(), 
						dmc.getFirstTime() + aScroller.getDefaultLoadtime(), true);
			}
			else {
				aScroller.scrollerData = oldData.clone();
				aScroller.rangesChanged(0);
			}
		}


		loadData(false);
	}

	@Override
	public Serializable getSettingsReference() {
		StoredScrollerData sd = new StoredScrollerData();
		for (int i = 0; i < pamScrollers.size(); i++) {
			sd.addScrollerData(pamScrollers.get(i));
		}
		return sd;
	}

	@Override
	public long getSettingsVersion() {
		return StoredScrollerData.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return "Viewer Scroll Manager";
	}

	@Override
	public String getUnitType() {
		return "Viewer Scroll Manager";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		oldScrollerData = (StoredScrollerData) pamControlledUnitSettings.getSettings();
		return true;
	}

	@Override
	public void centreDataAt(PamDataBlock dataBlock, long menuMouseTime) {
		// centre all scroll bars as close to the above as is possible.
		AbstractPamScroller aScroller;
		long scrollRange, newMax, newMin;
		for (int i = 0; i < pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			scrollRange = aScroller.getMaximumMillis() - aScroller.getMinimumMillis();
			newMin = checkMinimumTime(menuMouseTime - scrollRange / 2);
			newMax = checkMaximumTime(newMin + scrollRange);
			newMin = newMax-scrollRange;
			newMin = checkGapPos(dataBlock, newMin, newMax);
			newMax = newMin + scrollRange;
			aScroller.setRangeMillis(newMin, newMax, false);
			//			aScroller.setValueMillis(menuMouseTime - scrollRange/2);
		}
		loadData(false);
	}

	@Override
	public void startDataAt(PamDataBlock dataBlock, long menuMouseTime, boolean immediateLoad) {
		AbstractPamScroller aScroller;
		long scrollRange, newMax, newMin;
		for (int i = 0; i < pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			scrollRange = aScroller.getMaximumMillis() - aScroller.getMinimumMillis();
			newMin = checkMinimumTime(menuMouseTime);
			newMax = checkMaximumTime(newMin + scrollRange);
			newMin = newMax-scrollRange;
			newMin = checkGapPos(dataBlock, newMin, newMax);
			newMax = newMin + scrollRange;
			aScroller.setRangeMillis(newMin, newMax, false);
		}
		loadData(immediateLoad);
	}

	@Override
	public JPopupMenu getStandardOptionsMenu(AbstractPamScroller pamScroller) {
		JPopupMenu menu = new JPopupMenu();
		JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem("Couple all scrollers");
		cbItem.setSelected(oldScrollerData.coupleAllScrollers);
		cbItem.addActionListener(new CoupleScrollersMenuItem(pamScroller, oldScrollerData.coupleAllScrollers));
		menu.add(cbItem);
		return menu;
	}

	class CoupleScrollersMenuItem implements ActionListener {
		private boolean currentState;
		private AbstractPamScroller pamScroller;

		public CoupleScrollersMenuItem(AbstractPamScroller pamScroller, boolean currentState) {
			super();
			this.currentState = currentState;
			this.pamScroller = pamScroller;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			setCoupleAllScrollers(pamScroller, !currentState);
		}
	}

	private void setCoupleAllScrollers(AbstractPamScroller pamScroller, boolean newState) {
		oldScrollerData.coupleAllScrollers = newState;
		if (newState) {
			followCoupledScroller(pamScroller);
		}
	}

	private void followCoupledScroller(AbstractPamScroller pamScroller) {
		AbstractPamScroller aScroller;
		long value = pamScroller.getValueMillis();
		for (int i = 0; i < pamScrollers.size(); i++) {
			aScroller = pamScrollers.get(i);
			if (aScroller == pamScroller) {
				continue;
			}
			aScroller.setValueMillis(value);
		}
	}

	@Override
	public long checkGapPos(AbstractPamScroller scroller,
			long oldMin, long oldMax, long newMin, long newMax, int direction) {
		int nDataBlocks = scroller.getNumUsedDataBlocks();
		if (nDataBlocks == 0) {
			return newMin;
		}
		int oldMinGap = 0, oldMaxGap = 0, newMinGap = 0, newMaxGap = 0;
		PamDataBlock dataBlock;
		long range = oldMax-oldMin;

		/*
		 * Get a series of flags for all four positions indicating
		 * exactly where the data are / want to be relative to 
		 * gaps in the datamap. 
		 * These are defined in OfflineDataMap	 
		 * <p>NO_DATA
		 * <p>BEFORE_FIRST
		 * <p>AFTER_LAST
		 * <p>POINT_START
		 * <p>POINT_END
		 * <P>IN_GAP
		 */
		for (int i = 0; i < nDataBlocks; i++) {
			dataBlock = scroller.getUsedDataBlock(i);
			oldMinGap |= isInGap(dataBlock, oldMin);
			oldMaxGap |= isInGap(dataBlock, oldMax);
			newMinGap |= isInGap(dataBlock, newMin);
			newMaxGap |= isInGap(dataBlock, newMax);
		}

		/**
		 * Can now think about what to do based on gap status
		 * of the four times.
		 */
		if ((newMinGap & OfflineDataMap.IN_DATA) > 0 && (newMaxGap & OfflineDataMap.IN_DATA) > 0) {
			return newMin;
		}
		long newStart;
		if (direction > 0) { // going forward. 
			/*
			 *  if the old start was in a gap, move so the new start is at the edge of
			 *  the next data.  
			 */
			if ((oldMinGap == OfflineDataMap.IN_GAP)) {
				newStart = getNextDataStart(scroller, oldMin);
				if (newStart != Long.MAX_VALUE) {
					return newStart;
				}
			}
			/*
			 * If the old end was on the end of a point, then jump so that the new start
			 * is at the start of the next map point AFTER the old end.
			 */
			if ((oldMaxGap & OfflineDataMap.POINT_END) != 0) {
				newStart = getNextDataStart(scroller, oldMax);
				if (newStart != Long.MAX_VALUE) {
					return newStart;
				}
			}

			/*
			 * If the old end wasn't in gap and the new end is in a gap, then align the
			 * data so that the end is on the end of the data.  
			 */
			if ((oldMaxGap & OfflineDataMap.IN_DATA) != 0 &&
					(newMaxGap == OfflineDataMap.IN_GAP)) {
				newStart = getPrevDataEnd(scroller, newMax);
				if (newStart != Long.MAX_VALUE) {
					return newStart - range;
				}
			}			
		}
		else if (direction < 0) { // going backwards. 
			/*
			 *  if the old end was in a gap, move so the new end is at the edge of
			 *  the previous data.  
			 */
			if ((oldMaxGap == OfflineDataMap.IN_GAP)) {
				newStart = getPrevDataEnd(scroller, oldMax);
				if (newStart != Long.MIN_VALUE) {
					return newStart-range;
				}
			}
			/*
			 * If the old start was on the start of a point, then jump so that the new end
			 * is at the end of the previous map point BEFORE the old start.
			 */
			if ((oldMinGap & OfflineDataMap.POINT_START) != 0) {
				newStart = getPrevDataEnd(scroller, oldMin);
				if (newStart != Long.MIN_VALUE) {
					return newStart-range;
				}
			}

			/*
			 * If the old start wasn't in gap and the new start is in a gap, then align the
			 * data so that the start is on the start of the data.  
			 */
			if ((oldMinGap & OfflineDataMap.IN_DATA) != 0 &&
					(newMinGap == OfflineDataMap.IN_GAP)) {
				newStart = getNextDataStart(scroller, newMin);
				if (newStart != Long.MAX_VALUE) {
					return newStart;
				}
			}			
		}
		return newMin;
	}

	/**
	 * Very similar to the function checking gap pos for a whole scroller, 
	 * but only does it for one datablock. 
	 * @param dataBlock data block
	 * @param newMin new min time
	 * @param newMax new max time
	 * @return adjusted new min time
	 */
	private long checkGapPos(PamDataBlock dataBlock, long newMin, long newMax) {
		int newMinGap = 0, newMaxGap = 0;
		newMinGap |= isInGap(dataBlock, newMin);
		newMaxGap |= isInGap(dataBlock, newMax);
		long newT;
		if ((newMinGap & OfflineDataMap.IN_DATA) > 0 && (newMaxGap & OfflineDataMap.IN_DATA) > 0) {
			return newMin;
		}
		else if (newMinGap == OfflineDataMap.IN_GAP) {
			/*
			 * Move forward
			 */
			newT = dataBlock.getNextDataStart(newMin);
			if (newT > 0xFF) {
				return newT;
			}
		}
		else if (newMinGap == OfflineDataMap.IN_DATA && newMaxGap == OfflineDataMap.IN_GAP) {
			/**
			 * Move back a bit, so end of data aligns. 
			 */
			newT = dataBlock.getPrevDataEnd(newMax);
			if (newT > 0xFF) {
				return newT - (newMax-newMin);
			}

		}
		return newMin;
	}

	public long getNextDataStart(AbstractPamScroller scroller, long timeMillis) {
		long time = Long.MAX_VALUE;
		long t;
		for (int i = 0; i < scroller.getNumUsedDataBlocks(); i++) {
			t = scroller.getUsedDataBlock(i).getNextDataStart(timeMillis);
			if (t > 0xFF) {
				time = Math.min(time, t);
			}
		}
		return time;
	}

	public long getPrevDataEnd(AbstractPamScroller scroller, long timeMillis) {
		long time = Long.MIN_VALUE;
		long t;
		for (int i = 0; i < scroller.getNumUsedDataBlocks(); i++) {
			t = scroller.getUsedDataBlock(i).getPrevDataEnd(timeMillis);
			if (t > 0xFF) {
				time = Math.max(time, t);
			}
		}
		return time;
	}


}
