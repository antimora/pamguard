/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package PamguardMVC;

import generalDatabase.SQLLogging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.media.j3d.TransformGroup;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.vecmath.Point3f;

import pamScrollSystem.ViewLoadObserver;

import dataGram.DatagramProvider;
import dataMap.OfflineDataMap;

import bearingTimeDisplay.DataSymbolProvider;
import bearingTimeDisplay.DefaultDataSymbol;
import binaryFileStorage.BinaryDataSource;

import PamController.OfflineDataStore;
import PamController.PamController;
import PamDetection.AcousticDataUnit;
import PamGraph3D.PamShapes3D;
import PamGraph3D.graphDisplay3D.DataSymbol3DProvider;
import PamGraph3D.graphDisplay3D.DefaultSymbol3D;
import PamUtils.PamCalendar;
import PamView.PamSymbolManager;
import SoundRecorder.RecorderControl;
import SoundRecorder.trigger.RecorderTrigger;

/**
 * 
 * @author Doug Gillespie
 *         <p>
 *         PamDataBlocks manage the data from PamProcesses.
 *         <p>
 *         New data, either from external sources (sound cards, GPS, etc.) or
 *         detector output (clicks or whistles, etc.) are placed in
 *         PamDataUnits. The job of a PamDataBlock is to manage those
 *         PamDataUnits.
 *         <p>
 *         Processes that require the data from a PamDataBlock must implement
 *         PamObserver and subscribe as listeners to the PamDataBlock. When a
 *         new PamDataUnit is added to a data block, all listeners will be
 *         notified and sent references both to the data block and the data
 *         unit.
 *         <p>
 *         Each PamDatablock is also responsible for deleting old data. Since
 *         only the observers of PamDataBlocks know how much historical data is
 *         required, before deleting any data each PamDataBlock asks all the
 *         PamObservers for their required data history in milliseconds and takes
 *         the maximum value returned by all observers. 
 *         The PamDataBlock will then calculate the time of the first required
 *         data unit and delete all preceding units. This operation takes place 
 *         approximately once per second. 
 *         <p>
 *         For example, a whistle detector, while searching for whistles, may
 *         only require the last two or three data units from the data block
 *         containing FFT data, but when it's found a complete whistle, it may
 *         need to go back and look at the FFT data from other channels in order
 *         to calculate a location, or it may require the raw data in order to look at the
 *         original waveform. As another example, the map panel may want to hold 
 *         several hours of data in memory for display purposes.  
 *         <p>
 *         It is essential that PamProcesses are realistic about how much data
 *         they can ask a PamDataBlock to hold - if they consistently ask for too much 
 *         data to be stored, the computer will run out of memory.
 *         
 *         @see PamguardMVC.PamDataUnit
 *         @see PamguardMVC.PamProcess
 */
public class PamDataBlock<Tunit extends PamDataUnit> extends PamObservable {

	/**
	 * When getting a DataUnit from the Datablock, get the absolute data unit,
	 * i.e. the unit number as would be if none had ever been deleted
	 */
	static final public int REFERENCE_ABSOLUTE = 1;

	/**
	 * When getting a DataUnit from the Datablock, get the current data unit,
	 * i.e. the unit number in the current ArrayList
	 */
	static final public int REFERENCE_CURRENT = 2;

	/**
	 * when Pamguard is running in mixed mode, some data are being reanalysed and are being
	 * written back into the database, others are being taken out of the database. <p>
	 * These flags tell each individual datablock what it should do. 
	 */
	private int mixedDirection = MIX_DONOTHING;
	static public final int MIX_DONOTHING = 0;
	static public final int MIX_OUTOFDATABASE = 1;
	static public final int MIX_INTODATABASE = 2;

	protected String dataName;

	/**
	 * No data available for offline loading. 
	 */
	static public final int REQUEST_NO_DATA = 0x1;
	/**
	 * Data loaded for requested time period. 
	 */
	static public final int REQUEST_DATA_LOADED = 0x2;
	/**
	 * Data partially loaded for requested time period
	 */
	static public final int REQUEST_DATA_PARTIAL_LOAD = 0x4;
	/**
	 * this is exactly the same data as requested last time. 
	 * <p>
	 * This flag will be used with one of the other three. 
	 */
	static public final int REQUEST_SAME_REQUEST = 0x8;
	/**
	 * The request was interrupted (in multi thread load)
	 */
	static public final int REQUEST_INTERRUPTED = 0x10;
	/**
	 * The request threw an exception of some sort. 
	 */
	static public final int REQUEST_EXCEPTION = 0x20;

	//	protected DataType dataType;

	// The data units managed by the datablock
	protected List<Tunit> pamDataUnits;

	/**
	 * Only used in viewer mode to store a list of items 
	 * which may need to be deleted from file or the 
	 * databse. 
	 */
	private List<Tunit> removedItems;

	private boolean isViewer = false;

	protected PamProcess parentProcess;

	private BinaryDataSource binaryDataSource;

	/**
	 * Flags from AbstractLocalisation to say what localisation information may
	 * be present in data in this class. This is the maximum data which are likely to 
	 * be found within each data unit and are NOT a guarantee that individul data
	 * units will have that level of localisation content. 
	 */
	private int localisationContents;


	/**
	 * Used in offine analysis when data arebeing reloaded. 
	 * this list gets used to distribute data beingloaded 
	 * from upstream processes. 
	 */
	private Vector<PamObserver> requestingObservers;

	/**
	 * Natural lifetime of data in seconds. 
	 */
	protected int naturalLifetime = 1000; // natural lifetime in milliseconds.

	protected int unitsRemoved = 0;

	protected int unitsAdded = 0;

	protected int unitsUpdated = 0;

	int channelMap;

	private boolean linkGpsData = true;

	private Class unitClass;

	private boolean acousticData;
	
	/**
	 * Flag to say that this data block and trigger sound clip generator. 
	 * NB clips are intended to be very short clips around a single whistle
	 * or similar and are different to automatic recordings which can 
	 * go on for a lot longer.  
	 */
	private boolean canClipGenerate = false;

	/**
	 * Allow recycling within this data block
	 */
	private boolean recycling;

	/**
	 * Max length for the recycling store. 
	 */
	private int recyclingStoreLength = 100;

	private Vector<Tunit> recycledUnits;
	
	private DatagramProvider datagramProvider = null;
	
	private DataSymbolProvider dataSymbolProvider=null;

	/**
	 * Should log data in database if SQL Logging is set. 
	 */
	private  boolean shouldLog = true;
	
	/**
	 * Should store data in binary store if binary storage is available. 
	 */
	private boolean shouldBinary = true;


	/**
	 * PamDataBlock constructor. 
	 * @param unitClass class of data unit to hold in this data block
	 * @param dataName name of data block
	 * @param parentProcess parent PamProcess
	 * @param channelMap bitmap of channels which may be represented in data units in this data block.
	 */
	public PamDataBlock(Class unitClass, String dataName, PamProcess parentProcess, int channelMap) {

		super();

		isViewer = (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW);
		if (isViewer) {
			removedItems = Collections.synchronizedList(new Vector<Tunit>());
		}

		//		pamDataUnits = new Vector<Tunit>();
		pamDataUnits = Collections.synchronizedList(new LinkedList<Tunit>());
		unitsRemoved = 0;
		//		this.dataType = dataType;
		this.unitClass = unitClass;
		this.dataName = dataName;
		this.parentProcess = parentProcess;
		this.channelMap = channelMap;

		autoSetDataBlockMixMode();

		acousticData = AcousticDataUnit.class.isAssignableFrom(unitClass);

		if (PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW) {
			t.start();
		}

	}
	private boolean shouldDelete() {
		switch (PamController.getInstance().getRunMode()) {
		case PamController.RUN_NORMAL:
			return true;
		case PamController.RUN_PAMVIEW:
			return false;
		case PamController.RUN_MIXEDMODE:
			return true;
		}
		return true;
	}

	public void remove() {
		// inform any observers that this data block has been removed from the system
		for (int i = 0; i < countObservers(); i++) {
			pamObservers.get(i).removeObservable(this);
		}
		// also clean up any managed symbol output. 
		if (overlayDraw != null) {
			PamSymbolManager.getInstance().removeManagedSymbol(overlayDraw);
		}
	}

	Timer t = new Timer(1000, new ActionListener() {
		public void actionPerformed(ActionEvent evt) {
			int n;
			if (shouldDelete()) {
				if (acousticData) {
					if (masterClockSample > 0) {
						n = removeOldUnitsS(masterClockSample);
						//						System.out.println(String.format("%d units removed from %s based on sample number at clock sample %d", 
						//								n, getDataName(), masterClockSample));
						return;
					}
				}
				n = removeOldUnitsT(PamCalendar.getTimeInMillis());
				//				System.out.println(String.format("%d units removed from %s based on millisecond time", n, getDataName()));
			}
		}
	});


	/**
	 * @return The total number of PamDataUnits in the block
	 */
	public int getUnitsCount() {
		return pamDataUnits.size();
	}

	synchronized public int getUnitsCountFromTime(long countStart) {
		ListIterator<Tunit> listIterator = pamDataUnits.listIterator(pamDataUnits.size());
		Tunit unit;
		int count = 0;
		while (listIterator.hasPrevious()) {
			unit = listIterator.previous();
			if (unit.getTimeMilliseconds() < countStart) {
				break;
			}
			count++;
		}
		return count;
		//		Tunit unit = this.getFirstUnitAfter(countStart);
		//		if (unit == null) {
		//			return 0;
		//		}
		//		return getUnitsCount() - firstIndex + 1;
	}


	/**
	 * @return the removedItems list
	 */
	public List<Tunit> getRemovedItems() {
		return removedItems;
	}
	/**
	 * Return the first DataUnit that is on or after
	 * the given time
	 * @param timems Milliseconds - UTC in standard Java epoch
	 * @return a PamDataUnit or null if no data were found
	 */
	public Tunit getFirstUnitAfter(long timems) {
		/**
		 * Iterative method no longer fast with a linked list system. 
		 */
		//		return searchFirstUnitAfter(0, getUnitsCount()-1, timems);
		return searchFirstUnitAfter(timems);
	}


	/**
	 * 
	 * Find a unit that starts at a specific time. 
	 * searchStart may help to speed things up, however, now that
	 * a LinkedList is used in place of a vector, it's likely that
	 * this speed increase will be small.
	 * @param timeMS  start time of data unit
	 * @param channels channel bitmap of data unit, or 0 for any data unit
	 * @param absStartPos start position for search, -1 if you
	 * want to start searching backwards from the end. 
	 * @return data unit (or null if nothing found)
	 */
	synchronized public Tunit findDataUnit(long timeMS, int channels, int absStartPos) {
		if (pamDataUnits == null || pamDataUnits.size() == 0) return null;
		if (absStartPos < 0) {
			return findDataUnitBackwards(timeMS, channels);
		}
		Tunit unit = null;
		ListIterator<Tunit> listIterator = pamDataUnits.listIterator(absStartPos);
		while (listIterator.hasNext()) {
			unit = listIterator.next();
			if (unit.timeMilliseconds == timeMS && (channels == 0 || channels == unit.getChannelBitmap())) {
				return unit;
			}			
		}
		//		if (absStartPos > 0) {
		//			unit = getAbsoluteDataUnit(absStartPos);
		//		}
		//		if (unit == null) unit = pamDataUnits.get(0);
		//		while (unit != null) {
		//			if (unit.timeMilliseconds == timeMS && channels == unit.getChannelBitmap()) {
		//				return unit;
		//			}
		//			unit = getAbsoluteDataUnit(unit.absBlockIndex+1);
		//		}
		return null;
	}
	private Tunit findDataUnitBackwards(long timeMS, int channels) {
		Tunit unit = null;
		ListIterator<Tunit> listIterator = pamDataUnits.listIterator(pamDataUnits.size());
		while (listIterator.hasPrevious()) {
			unit = listIterator.previous();
			if (unit.timeMilliseconds == timeMS && (channels == 0 || channels == unit.getChannelBitmap())) {
				return unit;
			}			
		}
		return null;
	}

	/**
	 * Find a data unit. By default, the search starts at the end
	 * of the list and works backwards on the assumption that we'll be 
	 * more interested in more recent data. 
	 * @param timeMS start time of PamDataUnit
	 * @param channels channel bitmap of PamDataUnit
	 * @return found data unit, or null. 
	 */
	synchronized public Tunit findDataUnit(long timeMS, int channels) {
		return findDataUnit(timeMS, channels, -1);
	}

	public int getUnitIndex(Tunit dataUnit) {
		return pamDataUnits.indexOf(dataUnit);
	}

	/**
	 * Find a dataunit based on it's database index. If there have been no updates, then
	 * database indexes should be in order and a fast find canbe used. If however, there 
	 * have been updates, then things will not be in order so it's necessary to go through 
	 * everything from start to end. 
	 * @param databaseIndex Index to search for. 
	 * @return found unit or null. 
	 */
	synchronized public Tunit findByDatabaseIndex(int databaseIndex) {

		//		if (unitsUpdated == 0) {
		//		  return fastFindByDatabaseIndex(databaseIndex, 0, getUnitsCount()-1);
		//		}
		//		else {
		return slowFindByDatabaseIndex(databaseIndex);
		//		}

	}

	/**
	 * Search all units in reverse order. 
	 * @param databaseIndex Database index to search for
	 * @return found unit
	 */
	synchronized private Tunit slowFindByDatabaseIndex(int databaseIndex) {
		ListIterator<Tunit> listIterator = pamDataUnits.listIterator(pamDataUnits.size());
		Tunit unit;
		while (listIterator.hasPrevious()) {
			unit = listIterator.previous();
			if (unit.getDatabaseIndex() == databaseIndex) {
				return unit;
			}
		}
		//		for (int i = 0; i < getUnitsCount(); i++) {
		//			if (pamDataUnits.get(i).getDatabaseIndex() == databaseIndex) {
		//				return pamDataUnits.get(i);
		//			}
		//		}
//		System.out.println(String.format("Can't find record with database index %d in %s", 
//				databaseIndex, getDataName()));
		return null;
	}
	/**
	 * Recursive search for a data unit between two indexes. 
	 * @param databaseIndex database Index to search for
	 * @param firstUnitIndex first search location in units list
	 * @param lastUnitIndex last search location in units list
	 * @return found unit. 
	 */
	//	private Tunit fastFindByDatabaseIndex(int databaseIndex, int firstUnitIndex, int lastUnitIndex) {
	//		if (lastUnitIndex - firstUnitIndex < 5) {
	//			for (int i = firstUnitIndex; i <= lastUnitIndex; i++) {
	//				if (pamDataUnits.get(i).getDatabaseIndex() == databaseIndex) {
	//					return pamDataUnits.get(i);
	//				}
	//			}
	//			System.out.println(String.format("Can't find record to update with database index", databaseIndex));
	//			return null;
	//		}
	//		// otherwise try the half way point and see if we need
	//		// to go higher or lower (assuming everything is in order)
	//		int halfPoint = (firstUnitIndex + lastUnitIndex) / 2;
	//		if (pamDataUnits.get(halfPoint).getDatabaseIndex() > databaseIndex) {
	//			return fastFindByDatabaseIndex(databaseIndex, firstUnitIndex, halfPoint);
	//		}
	//		else {
	//			return fastFindByDatabaseIndex(databaseIndex, halfPoint, lastUnitIndex);
	//		}
	//	}
	/**
	 * Gets the current index in the DataBlocks array list
	 * of PamDataUnits for the first data unit to come 
	 * after a certain time. 
	 * @param timems Milliseconds - UTC in standard Java epoch
	 * @return The index of the data unit, or -1 if no data were found
	 */
	//	public int getIndexOfFirstUnitAfter(long timems) {
	////		return indexSearchFirstUnitAfter(0, getUnitsCount()-1, timems);
	//		ListIterator<Tunit> listIterator = pamDataUnits.listIterator(pamDataUnits.size());
	//		Tunit unit, prevUnit = null;
	//		int count
	//		while (listIterator.hasPrevious()) {
	//			unit = listIterator.previous();
	//			if (unit.getTimeMilliseconds() < timems) {
	//				return prevUnit.absBlockIndex;
	//			}
	//			prevUnit = unit;
	//		}
	//		return -1;
	//	}

	/**
	 * Search through data units, starting at the last
	 * on the assumption that we'll be more interested in 
	 * more recent data. 
	 * @param timems search time
	 * @return found data unit or null
	 */
	synchronized private Tunit searchFirstUnitAfter(long timems) {
		Tunit unit, prevUnit = null;
		if (getFirstUnit() == null) {
			return null;
		}
		if ((unit = getFirstUnit()).getTimeMilliseconds() >= timems) {
			return unit;
		}
		ListIterator<Tunit> listIterator = pamDataUnits.listIterator(pamDataUnits.size());
		while (listIterator.hasPrevious()) {
			unit = listIterator.previous();
			if (unit.getTimeMilliseconds() < timems) {
				return prevUnit;
			}
			prevUnit = unit;
		}
		if (prevUnit.timeMilliseconds >= timems) {
			return prevUnit;
		}
		else {
			return null;
		}
	}
	
	
	/**
	 * Find a group of data units within a time window. 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	synchronized public ArrayList<Tunit> searchUnitsinInterval(long startTime, long endTime) {
			Tunit unit = null;
			ArrayList<Tunit> unitsInInterval=new ArrayList<Tunit>();
	
			if (getFirstUnit() == null) {
				return null;
			}
					
			//ListIterator<Tunit> listIterator = pamDataUnits.listIterator(pamDataUnits.size());
			ListIterator<Tunit> listIterator = pamDataUnits.listIterator(0);
			while (listIterator.hasNext()) {
				unit = listIterator.next();
				if (unit.getTimeMilliseconds()<endTime && unit.getTimeMilliseconds()>startTime){
					unitsInInterval.add(unit);
				}
			}

		return unitsInInterval;
	}
	
	// recursive search for the correct unit
	//	private Tunit searchFirstUnitAfter(int i1, int i2, long timems) {
	//		/*
	//		 * if there are < 5 units in chain, then just seach for them
	//		 * or give up if nothing was found
	//		 */
	//		if (i2-i1 < 5) {
	//			for (int i = i1; i <= i2; i++) {
	//				if (pamDataUnits.get(i).timeMilliseconds >= timems) {
	//					return pamDataUnits.get(i);
	//				}
	//			}
	//			return null; // nothing was found
	//		}
	//		/*
	//		 * otherwise try the mid point between i1 and i2 and 
	//		 * continue by searching one half of remaining data
	//		 */
	//		int testIndex = (i1 + i2) / 2;
	//		if (pamDataUnits.get(testIndex).timeMilliseconds > timems) {
	//			return searchFirstUnitAfter(i1, testIndex, timems);		
	//		}
	//		else {	
	//			return searchFirstUnitAfter(testIndex, i2, timems);
	//		}
	//	}

	// recursive search for the correct unit
	//	private int indexSearchFirstUnitAfter(int i1, int i2, long timems) {
	//		/*
	//		 * if there are < 5 units in chain, then just seach for them
	//		 * or give up if nothing was found
	//		 */
	//		if (i2-i1 < 5) {
	//			for (int i = i1; i <= i2; i++) {
	//				if (pamDataUnits.get(i).timeMilliseconds >= timems) {
	//					return i;
	//				}
	//			}
	//			return -1; // nothing was found
	//		}
	//		/*
	//		 * otherwise try the mid point between i1 and i2 and 
	//		 * continue by searching one half of remaining data
	//		 */
	//		int testIndex = (i1 + i2) / 2;
	//		if (pamDataUnits.get(testIndex).timeMilliseconds > timems) {
	//			return indexSearchFirstUnitAfter(i1, testIndex, timems);		
	//		}
	//		else {	
	//			return indexSearchFirstUnitAfter(testIndex, i2, timems);
	//		}
	//	}

	/**
	 * Creates a new PamDataUnit which will be added to this data block
	 * 
	 * @param startSample
	 *            first sample in data unit
	 * @param duration
	 *            Duration of the data unit (samples)
	 * @param channelBitmap
	 *            Bitmap of channels having data in the unit, i.e. if it's
	 *            reading channel 0 only, channelBitmap is 1, if it's channel 1
	 *            only, the channelBitmap is 2 and if it's data from channels 0
	 *            and 1 together, then channelBitmap is 3.
	 * @return Reference to the new PamDataUnit
	 */
	//	public Tunit getNewUnit(long startSample, long duration,
	//			int channelBitmap) {
	//		
	//		long ms;
	//		if (parentProcess != null) {
	//			ms = parentProcess.absSamplesToMilliseconds(startSample);
	//		}
	//		else {
	//			ms = PamCalendar.getTimeInMillis();
	//		}
	//		return createNewUnit(ms);
	////		return new Tunit(startSample, ms, duration, channelBitmap, null);
	//	}


	//	abstract Tunit createNewUnit(long timeMilliseconds);

	/**
	 * Clears all PamDataUnits from memory
	 * <p>
	 * In viewer mode, data are also re-saved. 
	 */
	public synchronized void clearAll() {
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			if (getUnitsCount() > 0) {
				saveViewerData();
			}
		}
		pamDataUnits.clear();
		if (isViewer) {
			removedItems.clear();
		}
		unitsRemoved = 0;
		unitsAdded = 0;
		unitsUpdated = 0;
		if (recycledUnits != null) {
			recycledUnits.clear();
		}
	}
	
	/**
	 * Reset a datablock. This is called at PamStart from PamController
	 * It's been added in mostly to help with some issues surrounding
	 * sample numbering and timing when receiving Network data in which  
	 * case the PamCalendar.getSessionStartTime may have been initialised
	 * when the sample count is already up in the zillions, in which 
	 * case a lot of the timing functions won't work. 
	 */
	public synchronized void reset() {
		if (binaryDataSource != null) {
			binaryDataSource.reset();
		}
		if (logging != null) {
			logging.reset();
		}
	}

	private long currentViewDataStart, currentViewDataEnd;

	/**
	 * 
	 * @return the start time of data currently loaded by the viewer.  
	 */
	public long getCurrentViewDataStart() {
		return currentViewDataStart;
	}

	/**
	 * 
	 * @return the end time of data currently loaded by the viewer.  
	 */
	public long getCurrentViewDataEnd() {
		return currentViewDataEnd;
	}
	/**
	 * Instruction from the viewer scroll manager to load new data. 
	 * @param dataStart data start time in millis
	 * @param dataEnd data end time in millis. 
	 */
	public boolean loadViewerData(long dataStart, long dataEnd, ViewLoadObserver loadObserver) {
		if (dataStart == currentViewDataStart && dataEnd == currentViewDataEnd) {
			return true;
		}

		clearAll();

		currentViewDataStart = dataStart;
		currentViewDataEnd = dataEnd;

		/*
		 * No need to call save since it's done anyway in cleaAll()
		 */
		//		saveOfflineData();


		// run the garbage collector immediately. 
		Runtime.getRuntime().gc();

		OfflineDataMap dataMap = getPrimaryDataMap();
		if (dataMap == null) {
			return false;
		}
		OfflineDataStore dataSource = dataMap.getOfflineDataSource();
		if (dataSource == null) {
			return false;
		}
		return dataSource.loadData(this, dataStart, dataEnd, null, loadObserver);

	}

	/**
	 * Saves data in this data block in offline viewer mode. 
	 * @return true if data found and saved. 
	 */
	public boolean saveViewerData() {

		OfflineDataMap dataMap = getPrimaryDataMap();
		if (dataMap == null) {
			return false;
		}

		OfflineDataStore dataSource = dataMap.getOfflineDataSource();
		if (dataSource == null) {
			return false;
		}

		return dataSource.saveData(this);
	}


	/**
	 * if possible, loads old data back in from a database or other storage space.
	 * @param loadViewerData 
	 *
	 */
	//	public void loadViewData(LoadViewerData loadViewerData, PamViewParameters pamViewParameters) {
	//		// find the database module and use it's connection 
	//
	//		if (logging != null) {
	//			generalDatabase.DBControl dbControl = 
	//				(DBControl) PamController.getInstance().findControlledUnit(DBControl.getDbUnitType());
	//			if (dbControl != null ) {
	//				logging.loadViewData(loadViewerData, dbControl.getConnection(), pamViewParameters);
	//			}
	//		}
	//	}


	static public final int NOTIFY_NEW_DATA = 1;
	static public final int NOTIFY_UPDATE_DATA = 2;
	/**
	 * Adds a new PamDataUnit to the PamDataBlock. When the data unit is added,
	 * PamObservers that have subscribed to the block will be notified.
	 * 
	 * @param pamDataUnit
	 *            Reference to a PamDataUnit
	 */
	public void addPamData(Tunit pamDataUnit) {
		synchronized (this) {
			pamDataUnit.absBlockIndex = unitsAdded++;
			pamDataUnit.setParentDataBlock(this);
			pamDataUnits.add(pamDataUnit);
			if (shouldBinary && binaryDataSource != null && !isViewer) {
				binaryDataSource.saveData(pamDataUnit);
			}
		}
		/*
		 * Do not synchronise notification since this will lock up 
		 * the whole thing if anything notified tries to access the
		 * data in a different thread.
		 */
		if (recorderTrigger != null) {
			RecorderControl.actionRecorderTrigger(recorderTrigger, pamDataUnit, 
					pamDataUnit.getTimeMilliseconds());
		}
		if (shouldNotify()) {
			setChanged();
			notifyObservers(pamDataUnit);
		}
		notifyOfflineObservers(pamDataUnit);

	}


	private void notifyOfflineObservers(Tunit pamDataUnit) {
		if (requestingObservers != null) {
			for (int i = 0; i < requestingObservers.size(); i++) {
				requestingObservers.get(i).update(this, pamDataUnit);
			}
		}

	}
	
	/**
	 * update a dataunit. Does little except flag that the data unit is updated 
	 * (so it will get saved), and sends notifications to other modules. 
	 * @param pamDataUnit
	 * @param updateTimeMillis
	 */
	public void updatePamData(Tunit pamDataUnit, long updateTimeMillis) {
		pamDataUnit.updateDataUnit(updateTimeMillis);
		setChanged();
		if (PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW) {
			if (binaryDataSource != null) {
				binaryDataSource.saveData(pamDataUnit);
			}
		}
		notifyObservers(pamDataUnit);
		unitsUpdated++;
	}

	public boolean shouldNotify() {
		switch (PamController.getInstance().getRunMode()) {
		case PamController.RUN_NORMAL:
			return true;
		case PamController.RUN_PAMVIEW:
			return false;
			//			switch (PamController.getInstance().getPamStatus()) {
			//			case PamController.PAM_LOADINGDATA:
			//				return false;
			//			case PamController.
			//			}
		case PamController.RUN_MIXEDMODE:
			return true;
			//			return (mixedDirection == MIX_OUTOFDATABASE);
		}
		return true;
	}

	synchronized public boolean remove(Tunit aDataUnit) {
		boolean rem = pamDataUnits.remove(aDataUnit);
		if (isViewer && rem) {
			removedItems.add(aDataUnit);
		}
		return rem;
	}

	/**
	 * Removes olderPamDataUnits from memory, starting at the first unit and
	 * continuing until if finds one with data coming earlier than the given
	 * time in milliseconds. 
	 * <p>
	 * If the data are acoustic, it tries to find the data source and looks to
	 * see how much data has been placed in the source data unit and does the 
	 * calculation in samples. 
	 * 
	 * @param currentTimeMS
	 *            Time in milliseconds of the first data which must be kept
	 * @return the number of units removed
	 */
	synchronized protected int removeOldUnitsT(long currentTimeMS) {
		// will have to do something to see if any of the blocks are still
		// referenced !
		if (pamDataUnits.isEmpty())
			return 0;
		Tunit pamUnit;
		long firstWantedTime = currentTimeMS - this.naturalLifetime;
		firstWantedTime = Math.min(firstWantedTime, currentTimeMS - getRequiredHistory());


		int unitsJustRemoved=0;
		while (!pamDataUnits.isEmpty()) {
			pamUnit = pamDataUnits.get(0);
			if (pamUnit.timeMilliseconds > firstWantedTime) {
				break;
			}
			pamDataUnits.remove(0);
			if (recycling && recycledUnits.size() < recyclingStoreLength) {
				recycledUnits.add(pamUnit);
			}
//			unitsRemoved++;
			unitsJustRemoved++;
		}
		unitsRemoved+=unitsJustRemoved;
		//TODO check this.
//		return unitsRemoved;  //should this be the count of unitsJustRemoved not total units removed?
		return unitsJustRemoved;
	}

	synchronized protected int removeOldUnitsS(long mastrClockSample) {

		if (pamDataUnits.isEmpty())
			return 0;
		AcousticDataUnit pamUnit;
		long firstWantedTime = (long) (this.naturalLifetime/1000. * getSampleRate());
		long keepTime = (long) (getRequiredHistory() / 1000. * getSampleRate());
		firstWantedTime = masterClockSample - Math.max(firstWantedTime, keepTime);

		int unitsJustRemoved=0;
		while (!pamDataUnits.isEmpty()) {
			pamUnit = (AcousticDataUnit) pamDataUnits.get(0);
			if (pamUnit.getStartSample() > firstWantedTime) {
				break;
			}
			pamDataUnits.remove(0);
			if (recycling && recycledUnits.size() < recyclingStoreLength) {
				recycledUnits.add((Tunit)pamUnit);
			}
//			unitsRemoved++;
			unitsJustRemoved++;
		}
		unitsRemoved+=unitsJustRemoved;
		return unitsJustRemoved;
	}

	/**
	 * Gets a recycled data unit if one is available. 
	 * @return recycled unit, or null
	 */
	public Tunit getRecycledUnit() {
		int n;
		if (recycledUnits == null) {
			return null;
		}
		synchronized (recycledUnits) {
			if ((n=recycledUnits.size()) > 0) {
				return recycledUnits.remove(n-1);
			}
		}
		return null;
	}


	public PamDataBlock getSourceDataBlock() {
		PamDataBlock<PamDataUnit> sourceBlock = parentProcess.getSourceDataBlock();
		if (sourceBlock == null) {
			return this;
		}
		else {
			return sourceBlock;
		}
	}

	//	/**
	//	 * Gets the next data unit in the list
	//	 * 
	//	 * @param lastObject
	//	 *            Current unit (you get back the one after this)
	//	 * @return The next data unit.
	//	 */
	//	public Tunit getNextUnit(Object lastObject) {
	//		int nextIndex = pamDataUnits.lastIndexOf(lastObject) + 1;
	//		Tunit pu = null;
	//		if (nextIndex < pamDataUnits.size()) {
	//			pu = (Tunit) pamDataUnits.get(nextIndex);
	//		}
	//		return pu;
	//	}
	//	public Tunit getNextUnit(PamDataUnit pamDataUnit) {
	//		return getAbsoluteDataUnit(pamDataUnit.getAbsBlockIndex() + 1);
	//	}
	//	
	//	public Tunit getPreceedingUnit(PamDataUnit pamDataUnit) {
	//		return getAbsoluteDataUnit(pamDataUnit.getAbsBlockIndex() - 1);
	//	}
	//
	/**
	 * Gets a reference to a data unit.
	 * 
	 * @param ref
	 *            number of the data unit
	 * @param refType
	 *            REFERENCE_ABSOLUTE or REFERENCE_CURRENT
	 * @return DataUnit \n If refType is REFERENCE_ABSOLUTE then the data unit
	 *         with the absolute position ref is returned (if it has not yet
	 *         been deleted). This might be used to re-access a specific unit or
	 *         to access the unit coming directly before or after a previously
	 *         accessed unit. \n If refType is REFERENCE_CURRENT then the data
	 *         unit at that position in the current ArrayList is returned.
	 */
	synchronized public Tunit getDataUnit(int ref, int refType) {
		switch (refType) {
		case REFERENCE_ABSOLUTE:
			return getAbsoluteDataUnit(ref);
		case REFERENCE_CURRENT:
			return getCurrentDataUnit(ref);
		}
		return null;
	}

	/**
	 * Gets a specific data unit using an absolute reference system which keeps
	 * track of data units that have been removed. Returns null if the specified
	 * unit is no longer available.
	 * 
	 * @param absReference
	 *            Absolute reference to the data unit
	 * @return Requested PamDataUnit
	 */
	synchronized protected Tunit getAbsoluteDataUnit(int absReference) {
		int trueReference = absReference - unitsRemoved;
		if (trueReference >= 0 && trueReference < pamDataUnits.size()) {
			return pamDataUnits.get(trueReference);
		}
		return null;
	}

	synchronized private Tunit getCurrentDataUnit(int ref) {
		if (ref >= pamDataUnits.size()) return null;
		return pamDataUnits.get(ref);
	}

	/**
	 * Gets the last data unit stored
	 * @return data unit or null
	 */
	synchronized public Tunit getLastUnit() {
		if (pamDataUnits == null || pamDataUnits.size() == 0) return null;
		return pamDataUnits.get(pamDataUnits.size() - 1);
	}
	/**
	 * Gets the first data unit stored
	 * @return data unit or null
	 */
	synchronized public Tunit getFirstUnit() {
		if (pamDataUnits == null || pamDataUnits.size() == 0) return null;
		return pamDataUnits.get(0);
	}

	/**
	 * Finds the data unit before the given start time. 
	 * <p>
	 * This implementation is passed an iterator which has already been 
	 * initialised to be at the END of the list. In this way, the calling
	 * function has access to the iterator and can access nearby elements. 
	 * @param listIterator pre initialised ListIterator
	 * @param startTime search time in milliseconds
	 * @return data unit at or following the given time. 
	 * @see ListIterator
	 */
	synchronized public Tunit getPreceedingUnit(ListIterator<Tunit> listIterator, long startTime) {
		Tunit unit;
		while (listIterator.hasPrevious()) {
			unit = listIterator.previous();
			if (unit.timeMilliseconds < startTime) {
				return unit;
			}
		}
		return null;
	}
	/**
	 * Finds the data unit before the given start time that has the same channel map. 
	 * <p>
	 * This implementation is passed an iterator which has already been 
	 * initialised to be at the END of the list. In this way, the calling
	 * function has access to the iterator and can access nearby elements. 
	 * @param listIterator pre initialised ListIterator
	 * @param startTime search time in milliseconds
	 * @param channelMap Channel bitmap
	 * @return data unit at or following the given time. 
	 * @see ListIterator
	 */
	synchronized public Tunit getPreceedingUnit(ListIterator<Tunit> listIterator, long startTime, int channelMap) {
		Tunit unit;
		while (listIterator.hasPrevious()) {
			unit = listIterator.previous();
			if (unit.channelBitmap != channelMap) {
				continue;
			}
			if (unit.timeMilliseconds <= startTime) {
				return unit;
			}
		}
		return null;
	}
	/**
	 * Simple function to find the data unit at or before the given
	 * start time. 
	 * @param startTime search time in milliseconds
	 * @return data unit at or following the given time. 
	 */
	synchronized public Tunit getPreceedingUnit(long startTime) {
		ListIterator<Tunit> listIterator = getListIterator(ITERATOR_END);
		return getPreceedingUnit(listIterator, startTime);
	}
	/**
	 * Simple function to find the data unit at or before the given
	 * start time that has a given channel bitmap 
	 * @param startTime search time in milliseconds
	 * @param channelMap Channel bitmap
	 * @return data unit at or following the given time. 
	 */
	synchronized public Tunit getPreceedingUnit(long startTime, int channelMap) {
		ListIterator<Tunit> listIterator = getListIterator(ITERATOR_END);
		return getPreceedingUnit(listIterator, startTime, channelMap);
	}

	/**
	 * Finds the data unit after the given start time that has the same channel map.
	 * <p>
	 * This implementation is passed an iterator which has already been
	 * initialised to be at the END of the list. In this way, the calling
	 * function has access to the iterator and can access nearby elements.
	 * @param listIterator pre initialised ListIterator
	 * @param startTime search time in milliseconds
	 * @param channelMap Channel bitmap
	 * @return data unit at or following the given time.
	 * @see ListIterator
	 */
	synchronized public Tunit getNextUnit(ListIterator<Tunit> listIterator, long startTime, int channelMap) {
		Tunit unit;
		while (listIterator.hasNext()) {
			unit = listIterator.next();
			if (unit.channelBitmap != channelMap) {
				continue;
			}
			if (unit.timeMilliseconds >= startTime) {
				return unit;
			}
		}
		return null;
	}
	/**
	 * Simple function to find the data unit at or following the given
	 * start time that has a given channel bitmap
	 * @param startTime search time in milliseconds
	 * @param channelMap Channel bitmap
	 * @return data unit at or following the given time.
	 */
	synchronized public Tunit getNextUnit(long startTime, int channelMap) {
		ListIterator<Tunit> listIterator = getListIterator(0);
		return getNextUnit(listIterator, startTime, channelMap);
	}

	/**
	 * Find the closest data unit to a given time. 
	 * @param startTime Start time of data unit (milliseconds)
	 * @return closest data unit
	 */
	synchronized public Tunit getClosestUnitMillis(long startTime) {
		return getClosestUnitMillis(startTime, 0xFFFFFFFF);
//		if (pamDataUnits.size() == 0)
//			return null;
//		/*
//		 * start at the last unit, the work back and if the interval starts
//		 * getting bigger again, stop
//		 */
//		Tunit unit = null;
//		Tunit preceedingUnit = null;
//		long newdifference;
//		long difference;
//
//		ListIterator<Tunit> listIterator = getListIterator(ITERATOR_END);
//		if (listIterator.hasPrevious() == false) {
//			return null;
//		}
//		unit = listIterator.previous();
//		difference = Math.abs(startTime	- unit.timeMilliseconds);
//		while (listIterator.hasPrevious()) {
//			preceedingUnit = listIterator.previous();
//			newdifference = Math.abs(startTime- preceedingUnit.timeMilliseconds);
//			if (newdifference > difference) {
//				return unit;
//			}
//			else {
//				unit = preceedingUnit;
//				difference = newdifference;
//			}
//		}
//		return unit;
	}
	/**
	 * Find the closest data unit to a given time. 
	 * @param startTime Start time of data unit (milliseconds)
	 * @param channelMap Channel map - must be some overlap, not an exact match. 
	 * @return closest data unit
	 */
	synchronized public Tunit getClosestUnitMillis(long startTime, int channelMap) {
		if (pamDataUnits.size() == 0)
			return null;
		/*
		 * start at the last unit, the work back and if the interval starts
		 * getting bigger again, stop
		 */
		Tunit unit = null;
		Tunit preceedingUnit = null;
		long newdifference;
		long difference;

		ListIterator<Tunit> listIterator = getListIterator(ITERATOR_END);
		if (listIterator.hasPrevious() == false) {
			return null;
		}
		unit = listIterator.previous();
		difference = Math.abs(startTime	- unit.timeMilliseconds);
		while (listIterator.hasPrevious()) {
			preceedingUnit = listIterator.previous();
			if (preceedingUnit.getChannelBitmap() != 0 && (preceedingUnit.getChannelBitmap() & channelMap) == 0) {
				continue;
			}
			newdifference = Math.abs(startTime- preceedingUnit.timeMilliseconds);
			if (newdifference > difference) {
				return unit;
			}
			else {
				unit = preceedingUnit;
				difference = newdifference;
			}
		}
		return unit;
	}

	/**
	 * @return The sample rate of the data contained in the block
	 */
	public float getSampleRate() {
		return parentProcess.getSampleRate();
	}

	/**
	 * @return The DataType of the data in the block (RAW, FFT or DETECTOR)
	 */
	//	public DataType getDataType() {
	//		return dataType;
	//	}

	/**
	 * @return Name of the data in the block.
	 */
	public String getDataName() {
		return dataName;
	}

	public String getLongDataName() {
		return getParentProcess().getProcessName() + ", " + getDataName();
	}

	/**
	 * Sets the sample rate for the block (e.g. call this after opening a sound
	 * file and reading the sample rate from the file header or after altering
	 * sound card settings). All observers of this block (PamProcesses and some
	 * views) are notified, they in turn should tell their own output
	 * PamDataBlocks about the change.
	 * 
	 * @param sampleRate
	 *            The new sample rate in Hz.
	 * @param notify
	 *            set true if Observers should be notified
	 */
	public void setSampleRate(float sampleRate, boolean notify) {
//				this.sampleRate = sampleRate;
		/*
		 * Doesnt notify it's own parent to avoid an infinite loop
		 */
		if (notify) {
			for (int i = 0; i < countObservers(); i++) {
				if (pamObservers.get(i).getObserverObject() != parentProcess) {
					pamObservers.get(i).setSampleRate(sampleRate, notify);
					//					pamObservers.get(i).getObserverObject().setSampleRate(sampleRate, notify);
				}
			}
		}
	}

	public void masterClockUpdate(long milliSeconds, long clockSample) {
		masterClockSample = clockSample;
		for (int i = 0; i < pamObservers.size(); i++) {
			if (pamObservers.get(i) != parentProcess) {
				pamObservers.get(i).masterClockUpdate(milliSeconds, clockSample);
			}
		}
		if (acousticData && shouldDelete()) {
			removeOldUnitsS(clockSample);
		}
	}

	/**
	 * Tell all observers of this datablock that some 
	 * control parameters have changed. 
	 * Modified July 09 to make sure it doesn't loop
	 * through itself when using threaded observers. 
	 */
	public void noteNewSettings() {
		for (int i = 0; i < countObservers(); i++) {
			if (pamObservers.get(i).getObserverObject() != parentProcess) {
				pamObservers.get(i).noteNewSettings();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getDataName();
	}


	/**
	 * @return Finds the original data block, i.e. the one that has a PamProcess
	 *         with no source data block.
	 */
	//	PamDataBlock FindMotherDataBlock() {
	//		/*
	//		 * This can be called by ANY PamDataBlock and it will wiggle it's way
	//		 * back through the different detectors and data blocks until it finds
	//		 * one which has no parent
	//		 */
	//		if (parentProcess == null) {
	//			return this;
	//		}
	//		PamDataBlock parentDataBlock = parentProcess.GetSourceDataBlock();
	//		if (parentDataBlock == null) {
	//			return this;
	//		}
	//		else {
	//			return parentDataBlock.FindMotherDataBlock();
	//		}
	//	}

	public int getNaturalLifetime() {
		return naturalLifetime/1000;
	}

	public void setNaturalLifetimeMillis(int naturalLifetime) {
		this.naturalLifetime = naturalLifetime;
	}

	public int getNaturalLifetimeMillis() {
		return naturalLifetime;
	}

	/**
	 * Set the natural lifetime of the data if there are no observers asking to 
	 * keep it for longer
	 * @param naturalLifetime in seconds (NOT milliseconds)
	 */
	public void setNaturalLifetime(int naturalLifetime) {
		this.naturalLifetime = naturalLifetime*1000;
	}

	public boolean isLinkGpsData() {
		return linkGpsData;
	}

	public void setLinkGpsData(boolean linkGpsData) {
		this.linkGpsData = linkGpsData;
	}

	public int getChannelMap() {
		//		System.out.println("PamDataBlock.java->getChannelMap->channelMap:"+channelMap);
		return channelMap;
	}

	/**
	 * Return the gain applied to any data created into this
	 * datablock. 
	 * <p> Example 1: The amplifier module will just return it's gain
	 * <p> Example 2: The FFT module will return the loss due to windowing the data.  
	 * <p> To convert to dB use 20*log10(Math.abs(getDataGain()));
	 * @param iChan channel number
	 * @return gain as a factor (to allow for negative gains)
	 */
	public double getDataGain(int iChan) {
		return 1;
	}

	public void setChannelMap(int channelMap) {
		this.channelMap = channelMap;
	}

	/**
	 * @param dataName The dataName to set.
	 */
	public void setDataName(String dataName) {
		this.dataName = dataName;
	}

	public String getLoggingName() {
		return getParentProcess().getPamControlledUnit().getUnitName() + "_" + dataName;
	}

	/**
	 * @return Returns the parentProcess.
	 */
	public PamProcess getParentProcess() {
		return parentProcess;
	}

	/**
	 * @return Returns the sourceProcess. 
	 */
	public PamProcess getSourceProcess() {
		//		if (sourceProcess != null) return sourceProcess;
		//		else return parentProcess.GetSourceProcess();
		return parentProcess.getSourceProcess();
	}

	public PamRawDataBlock getRawSourceDataBlock() {
		PamDataBlock dataBlock = getSourceDataBlock();
		if (PamRawDataBlock.class.isAssignableFrom(dataBlock.getClass())) {
			return (PamRawDataBlock) dataBlock;
		}
		return null;
	}
	
	/**
	 * Tries to find the raw data block source of the current data block.  It does this by bouncing back and forth from
	 * ParentProcess to ParentDataBlock and back again, until either the ParentProcess or the ParentDataBlock is null.
	 * <p>
	 * Note that this does not search specifically for a PamRawDataBlock; it searches for the first data block in the chain,
	 * and then tests to see whether that data block is of class PamRawDataBlock.
	 * 
	 * @return the PamRawDataBlock that serves as the source data, or null if no data block is found
	 */
	public PamRawDataBlock getRawSourceDataBlock2(){
		PamDataBlock<Tunit> parentDB = this;
		PamProcess parentProc;
		while(true) {
			parentProc = parentDB.getParentProcess();
			if (parentProc==null) {
				break;
			}
			
			PamDataBlock<Tunit> prevDB = parentProc.getParentDataBlock();
			if (prevDB==null) {
				break;
			} else {
				parentDB = prevDB;
			}
			
		}
		if (PamRawDataBlock.class.isAssignableFrom(parentDB.getClass())) {
			return (PamRawDataBlock) parentDB;
		}
		return null;
	}
	
	@Override
	public void addObserver(PamObserver o) {
		super.addObserver(o);
		if (parentProcess != null) {
			o.setSampleRate(parentProcess.getSampleRate(), true);
		}
	}

	@Override
	public void addObserver(PamObserver o, boolean reThread) {
		super.addObserver(o, reThread);
		if (parentProcess != null) {
			o.setSampleRate(parentProcess.getSampleRate(), true);
		}
	}
	/**
	 * 
	 * @return Class type for the sotred data units in this data block. 
	 */
	public Class getUnitClass() {
		return unitClass;
	}

	public void autoSetDataBlockMixMode() {
		if (PamDetection.AcousticDataUnit.class.isAssignableFrom(unitClass)) {
			//			System.out.println(unitClass + " is acoustic data" );
			setMixedDirection(MIX_INTODATABASE);
		}
		else {
			//			System.out.println(unitClass + " is NOT acoustic data" );
			setMixedDirection(MIX_OUTOFDATABASE);
		}
	}

	public int getMixedDirection() {
		return mixedDirection;
	}

	public void setMixedDirection(int mixedDirection) {
		this.mixedDirection = mixedDirection;
	}
	public int getLocalisationContents() {
		return localisationContents;
	}

	public void setLocalisationContents(int localisationContents) {
		this.localisationContents = localisationContents;
	}

	public void addLocalisationContents(int localisationContents) {
		this.localisationContents |= localisationContents;
	}

	public static final int ITERATOR_END = -1;
	public ListIterator<Tunit> getListIterator(int whereFrom) {
		if (whereFrom < 0) {
			return pamDataUnits.listIterator(pamDataUnits.size());
		}
		else {
			return pamDataUnits.listIterator(whereFrom);
		}
	}

	/**
	 * Only accept an iterator for a unit that matches the time exactly
	 */
	static final public int MATCH_EXACT = 1;
	/**
	 * If there is not exact time match, set the iterator so that the
	 * first element it returns will be the element before the requested time. 
	 */
	static final public int MATCH_BEFORE = 2;
	/**
	 * If there is not exact time match, set the iterator so that the
	 * first element it returns will be the element after the requested time. 
	 */
	static final public int MATCH_AFTER = 3;
	/**
	 * Set the iterator so that a call to previous() will give the first wanted element
	 */
	static final public int POSITION_BEFORE = 1;
	/**
	 * Set the iterator so that a call to next() will give the first wanted element
	 */
	static final public int POSITION_AFTER = 2;
	public ListIterator<Tunit> getListIterator(long startTimeMillis, int channels, int match, int position) {
		if (pamDataUnits.size() == 0) {
			return null;
		}
		// see if were closer to the start or the end
		long firstTime = getFirstUnit().timeMilliseconds;
		long lastTime = getLastUnit().timeMilliseconds;
		if (lastTime - startTimeMillis < startTimeMillis - firstTime) {
			return getListIteratorFromEnd(startTimeMillis, channels, match, position);
		}
		else {
			return getListIteratorFromStart(startTimeMillis, channels, match, position);
		}
	}
	public ListIterator<Tunit> getListIteratorFromStart(long startTime, int channels, int match, int position) {
		ListIterator<Tunit> iterator = getListIterator(0);
		Tunit thisOne = null;
		try {
			while (iterator.hasNext()) {
				thisOne = iterator.next();
				if (thisOne.channelBitmap != channels) {
					continue;
				}
				if (thisOne.timeMilliseconds >= startTime) {
					/* 
					 * so we know what's going on, get the pointer
					 * positioned after the one we want. That is where it 
					 * will be if we got an exact match of if
					 * we wanted a time lower or equal to that requests. 
					 */
					if (thisOne.timeMilliseconds > startTime) {
						if (match == MATCH_EXACT) {
							return null;	
						}
						else if (match == MATCH_AFTER) {
							// we want the one before this
							//						iterator.next();
						}
						else { // match == MATCH_BEFORE
							iterator.previous();
						}
					}
					else {
					}
					if (position == POSITION_BEFORE) {
						// need to move back one
						iterator.previous();
					}
					return iterator;
				}
			}
		}
		catch (NoSuchElementException ex) {

		}
		return null;
	}

	synchronized public void dumpBlockContents() {
		ListIterator<Tunit> listIterator = getListIterator(0);
		PamDataUnit unit;
		System.out.println(String.format("***** Data Dump from %s *****", getDataName()));
		while (listIterator.hasNext()) {
			unit = listIterator.next();
			System.out.println(String.format("Object %d, Index %d, Time %d, Channels %d",
					unit.hashCode(),
					unit.getAbsBlockIndex(),
					unit.getTimeMilliseconds(), unit.getChannelBitmap()));
		}
	}

	public ListIterator<Tunit> getListIteratorFromEnd(long startTime, int channels, int match, int position) {
		ListIterator<Tunit> iterator = getListIterator(ITERATOR_END);
		Tunit thisOne = null;
		try {
			while (iterator.hasPrevious()) {
				thisOne = iterator.previous();
				if (thisOne.channelBitmap != channels) {
					continue;
				}
				if (thisOne.timeMilliseconds <= startTime) {
					if (thisOne.timeMilliseconds < startTime)
						if (match == MATCH_EXACT) {
							return null;
						}
						else if (match == MATCH_BEFORE) {

						}
						else { // match == MATCH_AFTER
							iterator.next();
						}
					if (position == POSITION_AFTER) {
						iterator.next();
					}
					return iterator;
				}
			}
		}
		catch (NoSuchElementException ex) {

		}
		return null;
	}

	private Vector<Annotation> annotations = new Vector<Annotation>();

	private Vector<OfflineDataMap> offlineDataMaps = null;

	private SQLLogging logging;

	public Vector<Annotation> getAnnotations() {
		return annotations;
	}
	/**
	 * Gets all the annotations from the parent process and all upstream processes. 
	 * @return total number of annotations
	 */
	private int createAllAnnotations() {

		return annotations.size();
	}


	/**
	 * Copies all annotations over from the source DataBlock, then adds in the new ones
	 * from the new datablock. 
	 * @param sourceData source Datablock
	 * @param newAnnotations source of new annotations
	 * @return total number of annotations
	 */
	public int createAnnotations(PamDataBlock sourceData, Annotator newAnnotations) {
		return createAnnotations(sourceData, newAnnotations, false);
	}
	/**
	 * Copies all annotations over from the source DataBlock, then adds in the new ones
	 * from the new datablock. 
	 * @param sourceData source Datablock
	 * @param newAnnotations source of new annotations
	 * @param notifydownstream notify downstream modules. 
	 * @return total number of annotations
	 */
	public int createAnnotations(PamDataBlock sourceData, Annotator newAnnotations, 
			boolean notifyDownstream) {
		if (sourceData != null && sourceData.getAnnotations() != null) {
			annotations = (Vector<Annotation>) sourceData.getAnnotations().clone();
		}
		else {
			annotations = new Vector<Annotation>();
		}
		int n = newAnnotations.getNumAnnotations(this);
		for (int i = 0; i < n; i++) {
			annotations.add(newAnnotations.getAnnotation(this, i));
		}

		//		for (int i = 0; i < countObservers(); i++) {
		//			if (pamObservers.get(i).getObserverObject() != parentProcess) {
		//				pamObservers.get(i).getObserverObject().setSampleRate(sampleRate, notify);
		//			}
		//		}
		if (notifyDownstream) {
			PamObserver pamObserver;
			for (int i = 0; i < this.pamObservers.size(); i++) {
				pamObserver = pamObservers.get(i).getObserverObject();
				if (pamObserver == parentProcess) {
					continue;
				}
				if (PamProcess.class.isAssignableFrom(pamObserver.getClass())) {
					((PamProcess) pamObserver).createAnnotations(true);
				}
			}
		}

		return annotations.size();
	}

	/**
	 * Finds an annotation with the given type and name
	 * @param type annotation type
	 * @param name annotation name
	 * @return annotation object
	 */
	public Annotation findAnnotation(String type, String name) {
		if (annotations == null) {
			return null;
		}
		Annotation a;
		for (int i = 0; i < annotations.size(); i++) {
			a = annotations.get(i);
			if (a.getName().equals(name) && a.getType().equals(type)) {
				return a;
			}
		}
		return null;
	}

	/**
	 * Finds an annotation with the same type and name as the template annotation
	 * @param template template annotation
	 * @return annotation object
	 */
	public Annotation findAnnotation(Annotation template) {
		return findAnnotation(template.getType(), template.getName());
	}
	/**
	 * @param recycling the recycling to set
	 */
	public void setRecycling(boolean recycling) {
		this.recycling = recycling;
		recycledUnits = new Vector<Tunit>();
		setRecyclingStoreLength(getRecyclingStoreLength());
	}
	/**
	 * @return the recycling
	 */
	public boolean isRecycling() {
		return recycling;
	}
	/**
	 * @param recyclingStoreLength the recyclingStoreLength to set
	 */
	public void setRecyclingStoreLength(int recyclingStoreLength) {
		this.recyclingStoreLength = recyclingStoreLength;
		recycledUnits.setSize(recyclingStoreLength);
	}
	/**
	 * @return the recyclingStoreLength
	 */
	public int getRecyclingStoreLength() {
		return recyclingStoreLength;
	}

	/**
	 * Receive notifications from the main PamController. 
	 * @param changeType
	 */
	public void notifyModelChanged(int changeType) {

	}
	/**
	 * @param siterBinaryDataSource the siterBinaryDataSource to set
	 */
	public void setBinaryDataSource(BinaryDataSource binaryDataSource) {
		this.binaryDataSource = binaryDataSource;
		//		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
		//			binaryDataMap = new OfflineDataMap<Tunit>(this);
		//		}
	}
	/**
	 * @return the siterBinaryDataSource
	 */
	public BinaryDataSource getBinaryDataSource() {
		return binaryDataSource;
	}

	public void SetLogging(SQLLogging logging) {


		this.logging = logging;

		//		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
		//			databaseDataMap = new OfflineDataMap<Tunit>(this);
		//		}

		//		System.out.println(this.getClass().toString() + ", set logging,  " + this.logging.toString());

	}

	public SQLLogging getLogging() {
		return logging;
	}

	final public boolean getCanLog() {
		return (logging != null);
	}


	/**
	 * Should log the data unit to the database ? 
	 * @param pamDataUnit dataunit to consider
	 * @return true if data should be logged. 
	 */
	public boolean getShouldLog(PamDataUnit pamDataUnit) {
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			return false;
		}
		else {
			return shouldLog;
		}
	}

	/**
	 * Set if data should be logged to the database. 
	 * @param shouldLog flag to log data. 
	 */
	final public void setShouldLog(boolean shouldLog) {
		this.shouldLog = (shouldLog && getCanLog());
	}
	
	/**
	 * Get flag to say whether data should be stored in the binary store
	 * @param pamDataUnit data unit
	 * @return true if the data unit shoule be saved. 
	 */
	public boolean getShouldBinary(PamDataUnit pamDataUnit) {
		return (shouldBinary && binaryDataSource != null);
	}

	/**
	 * Set flag to say if data should be stored in the binary store. 
	 * @param shouldBinary flag to say data shoule be stored. 
	 */
	public void setShouldBinary(boolean shouldBinary) {
		this.shouldBinary = shouldBinary;
	}
	/**
	 * Adds a new offline datamap to the data block
	 * <p>
	 * Note that there may be several maps from different types
	 * of storage (although only one should have anything in it 
	 * in a sensible world). 
	 * <p>
	 * It's also possible that these will be recreated during a 
	 * run, and we don't want two of the same type in the same 
	 * datablock, so check that there isn't already one and 
	 * remove it 
	 * @param offlineDataMap
	 */
	public void addOfflineDataMap(OfflineDataMap offlineDataMap) {
		OfflineDataStore dataSource = offlineDataMap.getOfflineDataSource();
		removeOfflineDataMap(dataSource);
		if (offlineDataMaps == null) {
			offlineDataMaps = new Vector<OfflineDataMap>();
		}
		offlineDataMaps.add(offlineDataMap);
	}

	/**
	 * 
	 * @return the number of different offline data maps
	 */
	public int getNumOfflineDataMaps() {
		if (offlineDataMaps == null) {
			return 0;
		}
		return offlineDataMaps.size();
	}

	/**
	 * 
	 * @param iMap index of map (see getNumOfflineDataMaps)
	 * @return an OfflineDataMap from that index in the list. 
	 */
	public OfflineDataMap getOfflineDataMap(int iMap) {
		return offlineDataMaps.get(iMap);
	}

	/**
	 * 
	 * @param dataSource an offline data source (e.g. binaray storage, database storage, etc. 
	 * @return the offline data map for a specific OfflineDataSource or
	 * null
	 */
	public OfflineDataMap getOfflineDataMap(OfflineDataStore dataSource) {
		if (offlineDataMaps == null) {
			return null;
		}
		Iterator<OfflineDataMap> mapsIterator = offlineDataMaps.iterator();
		OfflineDataMap aMap;
		while (mapsIterator.hasNext()) {
			aMap = mapsIterator.next();
			if (aMap.getOfflineDataSource() == dataSource) {
				return aMap;
			}
		}
		return null;
	}

	/**
	 * New plan - always use teh binary store if it has any data at all. 
	 * If the binary store doesn't exist or is empty, only then use the database. 
	 * @return a data map
	 */
	public OfflineDataMap getPrimaryDataMap() {
		if (offlineDataMaps == null) {
			return null;
		}
		Iterator<OfflineDataMap> mapsIterator = offlineDataMaps.iterator();
		OfflineDataMap aMap, bestMap = null;
		int mostData = 0;
		int dataHeight;
		while (mapsIterator.hasNext()) {
			aMap = mapsIterator.next();
			dataHeight = aMap.getDataCount();
			//			System.out.println(aMap.getOfflineDataSource().getClass());
			if (aMap.getOfflineDataSource().getClass() == binaryFileStorage.BinaryStore.class &&
					dataHeight > 0) {
				return aMap;
			}
			if (dataHeight > mostData || bestMap == null) {
				bestMap = aMap;
				mostData = dataHeight;
			}
		}
		return bestMap;
	}

	/**
	 * remove a no longer needed offline data map. 
	 * @param dataSource data source (e.g. binary, database, raw data, etc. 
	 */
	private void removeOfflineDataMap(OfflineDataStore dataSource) {
		if (offlineDataMaps == null) {
			return;
		}
		Iterator<OfflineDataMap> mapsIterator = offlineDataMaps.iterator();
		OfflineDataMap aMap;
		while (mapsIterator.hasNext()) {
			aMap = mapsIterator.next();
			if (aMap.getOfflineDataSource() == dataSource) {
				mapsIterator.remove();
			}
		}
	}

	/**
	 * Work out some basic information about the elements that need saving from these data. 
	 * @param dataStore data source that want's to save the data. 
	 * @return an object of information
	 */
	public SaveRequirements getSaveRequirements(OfflineDataStore dataStore) {
		if (dataStore != getPrimaryDataMap().getOfflineDataSource()) {
			return null;
		}
		SaveRequirements sr = new SaveRequirements(this);
		ListIterator<Tunit> li = getListIterator(0);
		Tunit aUnit;
		/*
		 * First go through the main list and see what's been added (0 index)
		 * or updated. 
		 */
		while (li.hasNext()) {
			aUnit = li.next();
			if (aUnit.getDatabaseIndex() == 0) {
				sr.numAdditions++;
				continue;
			}
			if (aUnit.getUpdateCount() > 0) {
				sr.addUpdateUnit(aUnit);
			}
		}
		/**
		 * Then go through the list of deleted items and get their indexes. 
		 */
		li = removedItems.listIterator();
		while (li.hasNext()) {
			aUnit = li.next();
			if (aUnit.getDatabaseIndex() > 0) {
				sr.addDeletedUnit(aUnit);
			}
		}

		return sr;
	}

	/**
	 * Similar functionality to getOfflineData, but this will launch a separate worker thread to 
	 * do the actual work getting the data. The worker thread will call getOfflineData. <p>
	 * getOfflineData will probably (if not overridden) be sending data to the update member function of the
	 * observer, but from the worker thread. Once it's complete, it will send a message to say that data are
	 * loaded. <p>
	 * It's possible that the user will do something which causes this to be called again before the previous
	 * thread completed execution, in which case there are three options:
	 * <p>OFFLINE_DATA_INTERRUPT - previous thread will be terminated
	 * <p>OFFLINE_DATA_WAIT - wait for previous thread to terminate, then start this load
	 * <p>OFFLINE_DATA_CANCEL - give up and return
	 * @param dataObserver observer of the loaded data
	 * @param loadObserver observer to get status information on the load. 
	 * @param startMillis data start time in imilliseconds
	 * @param endMillis data end time in milliseconds. 
	 * @param interrupt interrupt options. 
	 */
	public void orderOfflineData(PamObserver dataObserver, LoadObserver loadObserver,
			long startMillis, long endMillis, int interrupt) {
		orderOfflineData(dataObserver, loadObserver, startMillis, endMillis, interrupt, false);

	}	
	/**
	 * Similar functionality to getOfflineData, but this will launch a separate worker thread to 
	 * do the actual work getting the data. The worker thread will call getOfflineData. <p>
	 * getOfflineData will probably (if not overridden) be sending data to the update member function of the
	 * observer, but from the worker thread. Once it's complete, it will send a message to say that data are
	 * loaded. <p>
	 * It's possible that the user will do something which causes this to be called again before the previous
	 * thread completed execution, in which case there are three options:
	 * <p>OFFLINE_DATA_INTERRUPT - previous thread will be terminated
	 * <p>OFFLINE_DATA_WAIT - wait for previous thread to terminate, then start this load
	 * <p>OFFLINE_DATA_CANCEL - give up and return
	 * @param dataObserver observer of the loaded data
	 * @param loadObserver observer to get status information on the load. 
	 * @param startMillis data start time in imilliseconds
	 * @param endMillis data end time in milliseconds. 
	 * @param interrupt interrupt options. 
	 * @param allowRepeats allow repeated loads of exactly the same data. 
	 * 
	 */
	public void orderOfflineData(PamObserver dataObserver, LoadObserver loadObserver,
			long startMillis, long endMillis, int interrupt, boolean allowRepeats) {
		//		System.out.println("Start order lock");
		synchronized (orderLock) {
			//			System.out.println("Past order lock");
			long t1 = System.currentTimeMillis();
			long t2 = System.currentTimeMillis();
			long t3 = t2;
			long t4 = t2;
			//			String orderDates = String.format(" %s to %s", 
			//					PamCalendar.formatDateTime(startMillis), PamCalendar.formatDateTime(endMillis));
			if (orderData != null) {
				//				System.out.println("order Data is not null");
				if (orderData.isDone() == false) {
					switch (interrupt) {
					case OFFLINE_DATA_INTERRUPT:
						//						System.out.println("Request order cancelling");
						//						requestCancellationObject.cancel = true;
						if (orderData.cancelOrder()) {
							//							System.out.println(getDataName() + " Cancel old order " + orderDates);
							while (orderData != null) {
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
//									e.printStackTrace();
									return;
								}
							}
						}
						else {
							//							System.out.println("Old order could not be cancelled");
						}
						break;
					case OFFLINE_DATA_CANCEL:
						//						System.out.println("Don't order new data " + orderDates);
						return;
					case OFFLINE_DATA_WAIT:
						int waitCount = 0;
						t3 = System.currentTimeMillis();
						//						System.out.println("Wait for old lot to complete " + orderDates);
						while (true) {
							if (orderData.isDone() || orderData.isCancelled()) {
								break;
							}
							waitCount++;
							try {
								Thread.sleep(10, 0);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					t4 = System.currentTimeMillis() - t3;
				}
			}
			//			}
			long t5 = System.currentTimeMillis();
			//			System.out.println(String.format("%s waited %d, %d, %d, %d during ordering load", 
			//					getDataName(), t2-t1, t3-t2, t4-t3, t5-t4));

			//			System.out.println(getDataName() + " Launch new load thread " + orderDates);
			orderData = new OrderData(dataObserver, loadObserver, startMillis, 
					endMillis, allowRepeats, null);
			//			requestCancellationObject.cancel = true;
			orderData.execute();

			//			t = new Timer(1000, new StartOrderOnTimer(orderData));

		} // end of order lock
	}

	public void cancelDataOrder() {
		synchronized (orderLock) {
			if (orderData != null) {
				orderData.cancelOrder();
			}
		}
	}

	//	private class StartOrderOnTimer implements ActionListener {
	//		
	//		OrderData orderData;
	//
	//		public StartOrderOnTimer(OrderData orderData) {
	//			// TODO Auto-generated constructor stub
	//		}
	//
	//		@Override
	//		public void actionPerformed(ActionEvent arg0) {
	//			// TODO Auto-generated method stub
	//			
	//		}
	//		
	//	}

	public static final int OFFLINE_DATA_INTERRUPT = 0x1;
	public static final int OFFLINE_DATA_WAIT = 0x2;
	public static final int OFFLINE_DATA_CANCEL = 0x4;

	/**
	 * 
	 * @return true if an order for data is currently still being processed. 
	 */
	public boolean getOrderStatus() {
		return (orderData != null);
		//		if (orderData == null) {
		//			return false;
		//		}
		//		if (orderData.isDone() || orderData.isCancelled()) {
		//			return false;
		//		}
		//		return true;
	}

	volatile private OrderData orderData;

	public Object orderLock = new Object();


	class OrderData extends SwingWorker<Integer, Integer> {
		PamObserver observer;
		LoadObserver loadObserver;
		long startMillis;
		long endMillis;
		boolean allowRepeats;
		String dateStr;
		RequestCancellationObject cancellationObject;
		OrderData(PamObserver observer, LoadObserver loadObserver, long startMillis, 
				long endMillis, boolean allowRepeats, RequestCancellationObject requestCancellationObject) {
			this.observer = observer;
			this.loadObserver = loadObserver;
			this.startMillis = startMillis;
			this.endMillis = endMillis;
			this.allowRepeats = allowRepeats;
			dateStr = String.format(" %s to %s", 
					PamCalendar.formatDateTime(startMillis), PamCalendar.formatDateTime(endMillis));
			cancellationObject = new RequestCancellationObject();
		}

		public boolean cancelOrder() {
			cancellationObject.cancel = true;
			return cancel(true);
		}

		@Override
		protected Integer doInBackground()  {
			try {
				//				System.out.println("Enter get offline data " + getDataName());
				int ans = getOfflineData(observer, observer, startMillis, endMillis, 
						allowRepeats, cancellationObject);
				//				System.out.println("Leave get offline data " + getDataName());

				return ans;
			}
			catch (Exception e) {
				e.printStackTrace();
				return 0;
			}

		}

		@Override
		protected void done() {
			if (this == orderData) {
				orderData = null;
			}
			else {
				//				System.out.println("Data order ending which wasn't the latest created");
			}
			Integer status = 0;
			try {
				if (isCancelled()) {
					status = REQUEST_INTERRUPTED;
					//					System.out.println(getDataName() + " Order done - cancelled " + dateStr);
				}
				else {
					status = get();
					//					System.out.println(getDataName() + " Order done - not cancelled " + dateStr + " status " + status);
				}
			} catch (InterruptedException e1) {
				status = REQUEST_INTERRUPTED;
				System.out.println(getDataName() + " Order done - REQUEST_INTERRUPTED " + dateStr);
			} catch (ExecutionException e1) {
				status = REQUEST_EXCEPTION;
				System.out.println(getDataName() + " Order done - ExecutionException " + dateStr);
			} catch (CancellationException e) {
				status = REQUEST_INTERRUPTED;
				System.out.println(getDataName() + " Order done - CancellationException " + dateStr);
			}
			if (loadObserver != null) {
				loadObserver.setLoadStatus(status);
			}
		}

		@Override
		protected void process(List<Integer> chunks) {
			// TODO Auto-generated method stub
			super.process(chunks);
		}

	}
//	/**
//	 * Gets data for offline display, playback, etc.<p>
//	 * This is used to get data from some upstream process
//	 * which is quite different to the function
//	 * loadViewerData(...) which loads data directly associated
//	 * with this data block. 
//	 * <p>For example, this might be called in the FFT data block by 
//	 * the spectrogram which wants some data to display. The FFT data block 
//	 * does not have this data, so it passes the request up to it's process
//	 * which will in turn pass the request on until it reaches a module which 
//	 * is capable of loading data into data units and sending them back 
//	 * down the line.   
//	 * <p>
//	 * Thsi will not cause the same data to be reloaded for the same observer
//	 * a second time - use the full version with extra paramter if you need this. 
//	 * 
//	 * @param observer data observer which will receive the data
//	 * @param startMillis start time in milliseconds
//	 * @param endMillis end time in milliseconds
//	 * @param allowRepeats allow the same data to be loaded a second time. 
//	 * @return true if request satisfied or partially satisfied. 
//	 */
//	synchronized public int getOfflineData(PamObserver observer, PamObserver endUser, 
//			long startMillis, long endMillis, boolean allowRepeats, RequestCancellationObject cancellationObject) {
//		return getOfflineData(observer, endUser, startMillis, endMillis, allowRepeats, cancellationObject);
//	}

	/**
	 * Gets data for offline display, playback, etc.<p>
	 * This is used to get data from some upstream process
	 * which is quite different to the function
	 * loadViewerData(...) which loads data directly associated
	 * with this data block. 
	 * <p>For example, this might be called in the FFT data block by 
	 * the spectrogram which wants some data to display. The FFT data block 
	 * does not have this data, so it passes the request up to it's process
	 * which will in turn pass the request on until it reaches a module which 
	 * is capable of loading data into data units and sending them back 
	 * down the line.   
	 * 
	 * @param observer data observer which will receive the data
	 * @param startMillis start time in milliseconds
	 * @param endMillis end time in milliseconds
	 * @param allowRepeats allow the same data to be loaded a second time. 
	 * @param cancellationObject 
	 * @return answer: . 
	 */
	synchronized public int getOfflineData(PamObserver observer, PamObserver endUser, 
			long startMillis, long endMillis, boolean allowRepeats, RequestCancellationObject cancellationObject) {
		if (allowRepeats == false &&
				lastRequestStart == startMillis &&
				lastRequestEnd == endMillis &&
				lastRequestObserver == observer &&
				lastEndUser == endUser) {
			//			System.out.println(String.format("Don't load repeated data %s to %s",
			//					PamCalendar.formatDateTime(startMillis), PamCalendar.formatDateTime(endMillis)));
			return lastRequestAnswer | REQUEST_SAME_REQUEST;
		}
		addRequestingObserver(observer);
		clearAll();
		//		System.out.println(String.format("getOfflineData %s from %s to %s ",
		//				getDataName(), PamCalendar.formatDateTime(startMillis), PamCalendar.formatDateTime(endMillis)));
		lastRequestAnswer = parentProcess.getOfflineData(this, endUser, startMillis, endMillis,
				 cancellationObject);
		//		System.out.println(String.format("getOfflineData %s has %d units ",
		//				getDataName(), getUnitsCount()));
		removeRequestingObserver(observer);
		lastRequestStart = startMillis;
		lastRequestEnd = endMillis;
		lastRequestObserver = observer;
		lastEndUser = endUser;
		return lastRequestAnswer;
	}
	private int lastRequestAnswer;
	private long lastRequestStart = 0;
	private long lastRequestEnd = 0;
	private PamObserver lastRequestObserver = null;
	private PamObserver lastEndUser = null;

	private RecorderTrigger recorderTrigger;

	
	/**
	 * Add observer to requesting observer list which is 
	 * used to distribute data to selected observers when it's
	 * reloaded in offline viewer mode. 
	 * @param observer observer
	 */
	private void addRequestingObserver(PamObserver observer) {
		if (requestingObservers == null) {
			requestingObservers = new Vector<PamObserver>();
		}
		if (requestingObservers.contains(observer) == false) {
			requestingObservers.add(observer);
		}
	}

	/**
	 * Remove observer from requesting observer list which is 
	 * used to distribute data to selected observers when it's
	 * reloaded in offline viewer mode. 
	 * @param observer observer
	 */
	private void removeRequestingObserver(PamObserver observer) {
		if (requestingObservers == null) {
			return;
		}
		requestingObservers.remove(observer);
	}

	public void clearDeletedList() {
		this.removedItems.clear();
	}

	/**
	 * Get the next data start point. i.e. 
	 * the time of the start of a map point which is 
	 * > timeMillis
	 * @param timeMillis current time in milliseconds
	 * @return start time of the next data start. 
	 */
	public long getNextDataStart(long timeMillis) {
		OfflineDataMap offlineMap = getPrimaryDataMap();
		if (offlineMap == null) {
			return OfflineDataMap.NO_DATA;
		}
		return offlineMap.getNextDataStart(timeMillis);
	}

	/**
	 * Get the previous data end point. i.e. 
	 * the time of the end of a map point which is 
	 * < timeMillis
	 * @param timeMillis current time in milliseconds
	 * @return start time of the next data start. 
	 */
	public long getPrevDataEnd(long timeMillis) {
		OfflineDataMap offlineMap = getPrimaryDataMap();
		if (offlineMap == null) {
			return OfflineDataMap.NO_DATA;
		}
		return offlineMap.getPrevDataEnd(timeMillis);
	}
	public void setDatagramProvider(DatagramProvider datagramProvider) {
		this.datagramProvider = datagramProvider;
	}
	public DatagramProvider getDatagramProvider() {
		return datagramProvider;
	}
	/**
	 * @return the canClipGenerate
	 */
	public boolean isCanClipGenerate() {
		return canClipGenerate;
	}
	/**
	 * @param canClipGenerate the canClipGenerate to set
	 */
	public void setCanClipGenerate(boolean canClipGenerate) {
		this.canClipGenerate = canClipGenerate;
	}
	
	/**
	 * Quick integer datablock id which is based on the dataname
	 * 
	 * @return
	 */
	public int getQuickId() {
		String dataName = getDataName();
		if (dataName == null) return 0;
		int n = dataName.length();
		if (n < 2) return 0;
		int id = dataName.charAt(0) << 24;
		id += dataName.charAt(1) << 16;
		id += dataName.charAt(n-1) << 8;
		char lastChar = 0;
		for (int i = 0; i < n; i++) {
			lastChar ^= dataName.charAt(i);
		}
		id += lastChar;
		return id;
	}
	
	// used in offline reprocessing of clicks to get them all back into time order. 
	public void sortData() {
		Collections.sort(pamDataUnits);
	}
	
	/**
	 * Get the symbol class for a detection. The default is a blue sphere which highlight red when selected.
	 * @return
	 */
	public  DataSymbolProvider get2DPlotProvider(){
		//don't want to create a new class everytime this function is called
		if (dataSymbolProvider==null){
			dataSymbolProvider=new DefaultDataSymbol();
		}
		return dataSymbolProvider;
	}
	
	public void setRecordingTrigger(
			RecorderTrigger recorderTrigger) {
		this.recorderTrigger = recorderTrigger;
		RecorderControl.registerRecorderTrigger(recorderTrigger);
	}
	
}
