package clickDetector;

import java.awt.Window;
import java.util.ListIterator;

import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;

import pamScrollSystem.ViewLoadObserver;

import staticLocaliser.StaticLocaliserControl;
import staticLocaliser.StaticLocaliserProvider;
import staticLocaliser.panels.AbstractLocaliserControl;
import staticLocaliser.panels.ClickLocaliserControl;

import alarm.AlarmCounter;
import alarm.AlarmDataSource;
import bearingTimeDisplay.DataSymbolProvider;
import bearingTimeDisplay.DefaultDataSymbol;
import binaryFileStorage.BinaryStore;
import clickDetector.offlineFuncs.OfflineClickLogging;
import clickDetector.offlineFuncs.OfflineEventDataBlock;
import dataMap.OfflineDataMap;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamDetection.AbstractLocalisation;
import PamGraph3D.PamShapes3D;
import PamguardMVC.AcousticDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;

public class ClickDataBlock extends AcousticDataBlock<ClickDetection>  implements AlarmDataSource {

	protected ClickControl clickControl;
	
	private OfflineClickLogging offlineClickLogging;
	
	private boolean isViewer;
	
	private ClickSymbol3D clickSymbol3D;
	
	public ClickDataBlock(ClickControl clickControl, PamProcess parentProcess, int channelMap) {

		super(ClickDetection.class, clickControl.getDataBlockPrefix() + "Clicks", parentProcess, channelMap);

		this.clickControl = clickControl;
		addLocalisationContents(AbstractLocalisation.HAS_BEARING);
		isViewer = (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW);
		if (isViewer) {
			offlineClickLogging = new OfflineClickLogging(clickControl, this);
		}
	}
//
//	@Override
//	public boolean getShouldLog(PamDataUnit pamDataUnit) {
//		return (super.getShouldLog(pamDataUnit) && clickControl.clickParameters.saveAllClicksToDB);
//	}

	private boolean firstLoad = true;

	private DataSymbolProvider clickSymbolProvider;
	/**
	 * Click detector loading has to be a bit different to normal - first 
	 * data are loaded from the binary store, then a subset of these data
	 * are loaded from the Offline database. These database clicks are then
	 * matched to the data in the  
	 */
	@Override
	public boolean loadViewerData(long dataStart, long dataEnd, ViewLoadObserver loadObserver) {
		if (firstLoad) {
			// make sure that offline events are already loaded. 
			OfflineEventDataBlock eventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();
			if (eventDataBlock != null) {
				eventDataBlock.loadViewerData(dataStart, dataEnd, loadObserver);
				firstLoad = false;
			}
		}
		
		/*
		 * default load should always load the binary data. 
		 */
		boolean loadOk = super.loadViewerData(dataStart, dataEnd, loadObserver);
		/*
		 * then force it to load the database stuff too. 
		 */
		OfflineClickLogging offlineClickLogging = 
			clickControl.getClickDetector().getClickDataBlock().getOfflineClickLogging();
		if (offlineClickLogging != null) {
			offlineClickLogging.loadViewerData(dataStart, dataEnd, null);
		}
		
//		matchDatabaseAndBinaryData();
		return loadOk; 
	}

	@Override
	public boolean saveViewerData() {

		/**
		 * Save of data to database.
		 * Teh events MUST be saved first so that they get the correct event id's from the database. 
		 */
		OfflineEventDataBlock offlineEventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();
		offlineEventDataBlock.saveViewerData();
		boolean ok = offlineClickLogging.saveViewerData();
		offlineClickLogging.checkSuspectEventTimes(offlineEventDataBlock);
		
		/**
		 * Normal save of data to binary tore
		 */
		return super.saveViewerData();
	}

	@Override
	public OfflineDataMap getPrimaryDataMap() {
		/*
		 * Try really hard to get the binary data source, not the database one. 
		 */
		int n = getNumOfflineDataMaps();
		OfflineDataMap aMap;
		for (int i = 0; i < n; i++) {
			aMap = getOfflineDataMap(i);
			if (aMap.getOfflineDataSource().getClass() == BinaryStore.class) {
				return aMap;
			}
		}
		return super.getPrimaryDataMap();
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamControllerInterface.DATA_LOAD_COMPLETE:
			matchClickstoEvents();
		}
	}

	private void matchClickstoEvents() {
//		System.out.println("Match clicks to events");		
		/**
		 * Now also load all the data from the offlineclickLogging
		 * (which is not registered as the official logger for click 
		 * data)
		 */
		offlineClickLogging.loadViewerData(getCurrentViewDataStart(), getCurrentViewDataEnd(), null);
	}

	/**
	 * @return the offlineClickLogging
	 */
	public OfflineClickLogging getOfflineClickLogging() {
		return offlineClickLogging;
	}

	@Override
	protected synchronized int removeOldUnitsS(long mastrClockSample) {
		int r =  super.removeOldUnitsS(mastrClockSample);
		int n = getUnitsCount();
		ClickDetection click;
		if (n > 200) {
			ListIterator<ClickDetection> iter = getListIterator(n-50);
			while (iter.hasPrevious()) {
				click = iter.previous();
				if (click.hasComplexSpectrum() == false) {
					break; // probably no need to go further down the list 
				}
				click.freeClickMemory();
			}
		}
		return r;
	}

	
	
	@Override
	public DataSymbolProvider get2DPlotProvider(){
		if (clickSymbolProvider==null){
			clickSymbolProvider=new ClickDataSymbol(this);
		}
		return clickSymbolProvider;
	}

	/* (non-Javadoc)
	 * @see alarm.AlarmDataSource#getAlarmCounter()
	 */
	@Override
	public AlarmCounter getAlarmCounter() {
		return clickControl.getAlarmCounter();
	}


}
