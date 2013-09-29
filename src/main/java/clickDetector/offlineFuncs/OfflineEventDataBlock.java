package clickDetector.offlineFuncs;

import java.util.Collections;
import java.util.Vector;

import pamScrollSystem.ViewLoadObserver;

import staticLocaliser.StaticLocaliserControl;
import staticLocaliser.StaticLocaliserProvider;
import staticLocaliser.panels.AbstractLocaliserControl;
import staticLocaliser.panels.ClickEventLocaliserControl;

import clickDetector.ClickDetector;
import dataMap.OfflineDataMap;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * PamDataBlock for offline events. 
 * <p>
 * As with RainbowClick, this will (probably) hold all offline events from the 
 * database so that the operator can navigate through them with ease. However, only
 * clicks associated with the currently loaded period will be loaded into memory and associated
 * with the OfflineEventDataUnits. 
 * @author Doug
 *
 */
public class OfflineEventDataBlock extends PamDataBlock<OfflineEventDataUnit> {

	private ClickDetector clickDetector;
	
	public OfflineEventDataBlock(String dataName,
			ClickDetector parentProcess, int channelMap) {
		super(OfflineEventDataUnit.class, dataName, parentProcess, channelMap);
		this.clickDetector = parentProcess;
		/*
		 * Use a vector rather than the default linked list since
		 * this will be faster for sorting and insert / retrieval. 
		 */
		pamDataUnits = new Vector<OfflineEventDataUnit>();
		
		setOverlayDraw(new OfflineEventGraphics(this));
		
	}
	
	@Override
	public void addPamData(OfflineEventDataUnit pamDataUnit) {
		super.addPamData(pamDataUnit);
		Collections.sort(pamDataUnits);
		/*
		 *  also need to immediately save the event, so that it picks up it's 
		 *  event id number right away
		 */
		if (pamDataUnit.getDatabaseIndex() == 0) {
			//		getLogging().saveOfflineData(dbControlUnit, connection)
			saveViewerData();
		}
	}

	@Override
	public boolean getShouldLog(PamDataUnit pamDataUnit) {
		return (pamDataUnit.getDatabaseIndex() == 0);
	}

	/**
	 * LoadViewerData works very differently for offline events since all events for 
	 * the entire data set are always held in memory. <p>
	 * Therefore, they only need be loaded once at the start of analysis and should
	 * never be deleted.  They should however be saved as often as is reasonably possible. 
	 */
	@Override
	public boolean loadViewerData(long dataStart, long dataEnd, ViewLoadObserver loadObserver) {
		// if no data are in memory, then load ...
		if (getUnitsCount() == 0) {
			// need to find the data map and load all data from the entire
			// data set. 
			OfflineDataMap dataMap = getPrimaryDataMap();
			if (dataMap == null) {
				return false;
			}
			dataStart = Math.min(dataStart, dataMap.getFirstDataTime() - 3600000L);
			dataEnd = Math.max(dataEnd, dataMap.getLastDataTime() + 3600000L);
			boolean retVal = super.loadViewerData(dataStart, dataEnd, loadObserver);
//			System.out.println(String.format("%d events loaded between %s and %s", 
//					getUnitsCount(), PamCalendar.formatDateTime(dataStart), 
//					PamCalendar.formatDateTime(dataEnd)));
			return retVal;
		}
		else {
			saveViewerData();
			return true;
		}
	}
	
	@Override
	public int getChannelMap(){
		return clickDetector.getClickDataBlock().getChannelMap();
	}


	/**
	 * This is generally only called from loadViwerData and since
	 * LoadviewerData only ever operates once, it should never get called !
	 */
	@Override
	public synchronized void clearAll() {
		super.clearAll();
	}

	@Override
	public boolean saveViewerData() {

//		/*
//		 * First it is necessary to save all the updated click information
//		 * to the database since some of the events may need to check some 
//		 * information from the updated click table before they save themselves. 
//		 */
		OfflineClickLogging offlineClickLogging = 
			clickDetector.getClickDataBlock().getOfflineClickLogging();
//		boolean ok = offlineClickLogging.saveViewerData();
//		
		offlineClickLogging.checkSuspectEventTimes(this);
				
		return super.saveViewerData();
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#remove(PamguardMVC.PamDataUnit)
	 */
	@Override
	public synchronized boolean remove(OfflineEventDataUnit aDataUnit) {
		// TODO Auto-generated method stub
		return super.remove(aDataUnit);
	}

	

	
}
