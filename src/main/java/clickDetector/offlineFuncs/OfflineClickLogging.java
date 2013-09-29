package clickDetector.offlineFuncs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ListIterator;

import pamScrollSystem.ViewLoadObserver;

import binaryFileStorage.DataUnitFileInformation;

import clickDetector.ClickControl;
import clickDetector.ClickDataBlock;
import clickDetector.ClickDetection;
import PamController.PamViewParameters;
import PamDetection.PamDetection;
import PamUtils.PamCalendar;
import PamguardMVC.BinaryFileMatcher;
import PamguardMVC.DataUnitFinder;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.DBControlUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.pamCursor.PamCursor;

public class OfflineClickLogging extends SQLLogging {

	private ClickControl clickControl;

	private ClickDataBlock clickDataBlock;

	protected PamTableItem eventId;

	protected PamTableItem binaryFile;

	protected PamTableItem clickNumber;

	protected PamTableItem amplitude;

	static public final int BINARY_FILE_NAME_LENGTH = 80;

	public OfflineClickLogging(ClickControl clickControl, ClickDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.clickControl = clickControl;
		this.clickDataBlock = pamDataBlock;

		PamTableDefinition tableDef;
		tableDef = new PamTableDefinition(clickControl.getUnitName()+"_OfflineClicks", SQLLogging.UPDATE_POLICY_OVERWRITE);
		tableDef.addTableItem(eventId = new PamTableItem("EventId", Types.INTEGER));
		tableDef.addTableItem(binaryFile = new PamTableItem("BinaryFile", Types.CHAR, BINARY_FILE_NAME_LENGTH));
		tableDef.addTableItem(clickNumber = new PamTableItem("ClickNo", Types.INTEGER));
		tableDef.addTableItem(amplitude = new PamTableItem("Amplitude", Types.DOUBLE));
		tableDef.setUseCheatIndexing(true);

		setTableDefinition(tableDef);
	}


	private Connection lastConnection;

	private DataUnitFinder<ClickDetection> dataFinder;
	private void checkTable() {
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		if (lastConnection == null || lastConnection != dbControl.getConnection()) {
			lastConnection = dbControl.getConnection();
			dbControl.getDbProcess().checkTable(getTableDefinition());
		}
	}

	@Override
	public boolean loadViewerData(long dataStart, long dataEnd, ViewLoadObserver loadObserver) {
		checkTable();
		/*
		 * Want to get a fast find of the click data going, so initialise it
		 * here and clean up after the data are loaded. 
		 */
		OfflineEventDataBlock eventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();
//		System.out.println(String.format("loading clicks between %s and %s, %d events and %d clicks in memory", 
//				PamCalendar.formatDateTime(dataStart), 
//				PamCalendar.formatDateTime(dataEnd), eventDataBlock.getUnitsCount(), 
//				getPamDataBlock().getUnitsCount()));
		dataFinder = new DataUnitFinder<ClickDetection>(getPamDataBlock(), new BinaryFileMatcher());
		boolean ok = super.loadViewerData(dataStart, dataEnd, loadObserver);
		return ok;
	}


	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		ClickDetection clickDetection = (ClickDetection) pamDataUnit;
		DataUnitFileInformation fileInfo = clickDetection.getDataUnitFileInformation();
		binaryFile.setValue(fileInfo.getShortFileName(BINARY_FILE_NAME_LENGTH));
		clickNumber.setValue((int)fileInfo.getIndexInFile());
		OfflineEventDataUnit offlineEvent = (OfflineEventDataUnit) 
		clickDetection.getSuperDetection(OfflineEventDataUnit.class);
		if (offlineEvent == null) {
			eventId.setValue(null);
		}
		else {
			eventId.setValue(offlineEvent.getDatabaseIndex());
		}
		amplitude.setValue(clickDetection.getAmplitudeDB());
	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {
		String fileName = binaryFile.getStringValue();
		long time = getLastTime();
		int clickNum = clickNumber.getIntegerValue();
		int eventId = this.eventId.getIntegerValue();
		int clickIndex = getTableDefinition().getIndexItem().getIntegerValue();
		OfflineEventDataBlock eventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();

		ClickDetection aClick = dataFinder.findDataUnit(fileName, clickNum);
		if (aClick == null) {
//			System.out.println(String.format("Unable to find click %d in %s", clickNum, fileName));
		}
		else {
			aClick.setDatabaseIndex(clickIndex);
			OfflineEventDataUnit event = eventDataBlock.findByDatabaseIndex(eventId);
			if (event != null) {
				event.addSubDetection(aClick, false);
			}
		}
		// amplitude doesn't get read back since it's already known !
		
//		aClick.setDatabaseIndex(databaseIndex);

		return aClick;
	}

	public boolean saveViewerData() {
		checkTable();
		PamDataBlock pamDataBlock = getPamDataBlock();
//		long dataStart = pamDataBlock.getCurrentViewDataStart();
//		long dataEnd = pamDataBlock.getCurrentViewDataEnd();
		if (pamDataBlock.getUnitsCount() == 0) {
			return true;
		}
		long dataStart = pamDataBlock.getFirstUnit().getTimeMilliseconds() - 1000;
		long dataEnd = pamDataBlock.getLastUnit().getTimeMilliseconds() + 1000;
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		Connection con = DBControlUnit.findConnection();
		PamCursor pamCursor = dbControl.createPamCursor(getTableDefinition());
		PamViewParameters pvp = new PamViewParameters();
		pvp.viewStartTime = dataStart;
		pvp.viewEndTime = dataEnd;
		pvp.useAnalysisTime = false;
		String clause = getTableDefinition().getBetweenString(pvp) + " ORDER BY UTC";
		boolean ok = pamCursor.openScrollableCursor(con, true, true, clause);
		if (ok == false) {
			System.out.println("OfflineClickLogging.SaveViewerData():  Unable to open scrollable cursor for " + getTableDefinition().getTableName());
			return false;
		}
		/*
		 * Now scroll through the data trying to match each point from the database to a data unit. 
		 * If a data unit cannot be found it's an error - all loaded data should have a click from 
		 * the binary store in memory. 
		 * IF the click has no super detection, then it will need deleting from the database
		 * IF the click HAS a super detection, then it will need updating in the database. 
		 * Finally go through all clicks and add any which don't have a database index number. 
		 */
		PamDetection dataUnit;
		PamDataUnit superDetection;
		int dbId;
		pamCursor.beforeFirst();
		try {
			/*
			 * First go through the cursor data and try to match with 
			 * loaded data. then either update or delete. 
			 */
			while (pamCursor.next()) {
				dbId = pamCursor.getInt(1);
				dataUnit = (PamDetection) pamDataBlock.findByDatabaseIndex(dbId);
				if (dataUnit == null) {
					System.out.println(pamDataBlock.getDataName() + "OfflineClickLogging.SaveViewerData(): Unable to find data unit for database index " + dbId);
					continue;
				}
				superDetection = dataUnit.getSuperDetection(OfflineEventDataUnit.class);
				if (superDetection == null) {
					pamCursor.deleteRow();
				}
				else {
					fillTableData(dataUnit);
					pamCursor.moveDataToCursor(true);
					pamCursor.updateRow();
				}
			}
			/*
			 * Then go through loaded data, and anything which isn't matched to 
			 * data in the database can be used to make a new record. 
			 */
			ListIterator<PamDataUnit> listIt = pamDataBlock.getListIterator(0);
			while (listIt.hasNext()) {
				dataUnit = (PamDetection) listIt.next();
				superDetection = dataUnit.getSuperDetection(OfflineEventDataUnit.class);
				if (superDetection == null) {
					continue;
				}
				if (dataUnit.getDatabaseIndex() == 0) {
					pamCursor.moveToInsertRow();
					fillTableData(dataUnit);
					pamCursor.moveDataToCursor(true);
					dbId = pamCursor.insertRow(true);
					dataUnit.setDatabaseIndex(dbId);
				}
			}
			pamCursor.updateDatabase();
			pamCursor.closeScrollableCursor();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
	}

	public void deleteEventClicks(Connection connection, int[] deleteIndexes) {
		String sqlString = String.format("DELETE FROM %s WHERE EventId %s", 
				getTableDefinition().getTableName(), createInClause(deleteIndexes));
		try {
			Statement s = connection.createStatement();
			s.execute(sqlString);
			s.close();
		} catch (SQLException e) {
			System.out.println("Delete failed with " + sqlString);
			e.printStackTrace();
		}
	}

	/**
	 * Check suspect offline event times for an event data block. 
	 * @param offlineEventDataBlock
	 */
	public void checkSuspectEventTimes(
			OfflineEventDataBlock offlineEventDataBlock) {
		ListIterator<OfflineEventDataUnit> li = offlineEventDataBlock.getListIterator(0);
		OfflineEventDataUnit event;
		while (li.hasNext()) {
			event = li.next();
			if (event.isSuspectEventTimes()) {
				checkSuspectEventTimes(event);
			}
		}
		
	}
	/**
	 * Update suspect event times for a single event. 
	 * @param event
	 */
	private void checkSuspectEventTimes(OfflineEventDataUnit event) {
		String criteria = String.format("WHERE EventId = %d ORDER BY UTC, UTCMilliseconds", 
				event.getDatabaseIndex());
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		PamCursor pamCursor = dbControl.createPamCursor(getTableDefinition());
		ResultSet resultSet = pamCursor.openReadOnlyCursor(dbControl.getConnection(), criteria);
		long newStart = 0, newEnd = 0;
		Timestamp ts;
		Integer millis;
		try {
			if (resultSet.first()) {
				transferDataFromResult(resultSet);
				ts = (Timestamp) getTableDefinition().getTimeStampItem().getValue();
				newStart = PamCalendar.millisFromTimeStamp(ts);
				if (newStart%1000 == 0) {
					millis = (Integer) getTableDefinition().getTimeStampMillis().getValue();
					if (millis != null) {
						newStart += millis;
					}
				}
			}
			if (resultSet.last()) {
				transferDataFromResult(resultSet);
				ts = (Timestamp) getTableDefinition().getTimeStampItem().getValue();
				newEnd = PamCalendar.millisFromTimeStamp(ts);
				millis = (Integer) getTableDefinition().getTimeStampMillis().getValue();
				if (millis != null) {
					newEnd += millis;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (newStart != 0 && newEnd != 0) {
			event.setTimeMilliseconds(newStart);
			event.setEventEndTime(newEnd);
			event.setSuspectEventTimes(false);
		}
	}

}
