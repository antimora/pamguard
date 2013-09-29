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

/**
 *
 * Facilitates data logging to database 
 *
 * @author David J McLaren, Douglas Gillespie, Paul Redmond
 *
 */

package generalDatabase;

import generalDatabase.pamCursor.CursorFinder;
import generalDatabase.pamCursor.PamCursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
//import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import pamScrollSystem.ViewLoadObserver;


import PamController.PamController;
import PamController.PamViewParameters;
import PamUtils.PamCalendar;
import PamguardMVC.DataUnitFinder;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.SaveRequirements;

/**
 * 
 * SQLLogging performs basic SQL actions to write and read data from a database.
 * <p>
 * Each SQLLogging object is Associated with a PamDataBlock.
 * <p>
 * When a database is first connected, the last values entered into the database
 * will be read back into Pamguard.
 * <p>
 * When a PamDataUnit is added to a PamDataBlock, Pamguard will automatically
 * call the fillTableData function. This will automatically fill in any database
 * columns that are cross referenced to data from other tables. It will then
 * call the abstract function setTableData where the programmer is responsible
 * for filling data for other columns.
 * 
 * 
 * @author Doug Gillespie
 * @see PamguardMVC.PamDataBlock
 * @see generalDatabase.PamTableDefinition
 * @see generalDatabase.PamTableItem
 * 
 */
public abstract class SQLLogging {

	private PreparedStatement selectStatement;

	private Connection selectConnection;

	/**
	 * Reference to the table definition object.
	 * This MUST be set from within the concrete logging class.  
	 */
	private PamTableDefinition pamTableDefinition;

	//	private long selectT1, selectT2;
	private PamViewParameters currentViewParameters;

	private PamDataBlock pamDataBlock;
	
	private ArrayList<SQLLoggingAddon> loggingAddOns;

	/**
	 * time of last item loaded offline or mixed mode
	 */
	private long lastTime;

	/**
	 * Database index of last item loaded offlien or mixed mode
	 */
	private int lastLoadIndex;

	private Statement dbStatement;

	private boolean canView = false;

	private boolean loadViewData = false;
	
	private CursorFinder loggingCursorFinder = new CursorFinder();
	private CursorFinder viewerCursorFinder = new CursorFinder();

	public CursorFinder getViewerCursorFinder() {
		return viewerCursorFinder;
	}

	public static final int UPDATE_POLICY_OVERWRITE = 1;
	public static final int UPDATE_POLICY_WRITENEW = 2;

	private int updatePolicy = UPDATE_POLICY_WRITENEW;

	/**
	 * SQLLogging constructor.
	 */
	protected SQLLogging(PamDataBlock pamDataBlock) {
		this.pamDataBlock = pamDataBlock;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (dbStatement != null) {
			dbStatement.close();
		}
		if (resultSet != null) {
			resultSet.close();
		}
	}

	/**
	 * Searches the Pamguard system for a SQLLogger with a given table name.
	 * Table name blanks are replaced with the _ character during the search.
	 * 
	 * @param tableName
	 *            table name to search for.
	 * @return reference to a SQLLogging
	 */
	final public static SQLLogging findLogger(String tableName) {
		String searchName = EmptyTableDefinition.deblankString(tableName);
		ArrayList<PamDataBlock> blockList = PamController.getInstance()
		.getDataBlocks();
		SQLLogging logger;
		for (int i = 0; i < blockList.size(); i++) {
			if ((logger = blockList.get(i).getLogging()) != null) {
				if (logger.getTableDefinition().getTableName()
						.equalsIgnoreCase(searchName)) {
					return logger;
				}
			}
		}
		return null;
	}

	/**
	 * Data values going in and out of the database are stored with their
	 * respective PamTableItems. This function is used to set data for a
	 * particular column before it is written to the database.
	 * <p>
	 * It is more efficient to maintain references to each PamTableItem and to
	 * set the values directly in each PamTableItem in the setTableData
	 * function.
	 * 
	 * @param iCol
	 *            Database item index
	 * @param data
	 *            Data object (can be null, otherwise must be correct type for
	 *            the column)
	 * @see PamTableDefinition
	 * @see PamTableItem
	 */
	public void setColumnData(int iCol, Object data) {
		getTableDefinition().getTableItem(iCol).setValue(data);
	}

	/**
	 * Each SQLLogging class must provide a valid Pamguard database definition
	 * object
	 * 
	 * @return a Pamguard database table definition object
	 * @see PamTableDefinition
	 */
	public final PamTableDefinition getTableDefinition() {
		return pamTableDefinition;
	}


	/**
	 * 
	 * @param pamTableDefinition PamTableDefinition to set
	 */
	public void setTableDefinition(PamTableDefinition pamTableDefinition) {
		this.pamTableDefinition = pamTableDefinition;
		if (loggingAddOns != null) {
			for (int i = 0; i < loggingAddOns.size(); i++) {
				loggingAddOns.get(i).addTableItems(pamTableDefinition);
			}
		}
	}

	/**
	 * Callback function when new data are created that allows the user to set
	 * the data for each column. Columns that have data which can be filled
	 * automatically (counters, primary keys and columns cross referenced to data
	 * in other tables) are filled automatically in fillTableData()
	 * 
	 * @param pamDataUnit
	 */
	abstract public void setTableData(PamDataUnit pamDataUnit);

	/**
	 * Automatically fills table data columns that can be done automatically
	 * (counters, primary keys and columns cross referenced to data in other
	 * tables). The abstract function setTableData is then called to fill in the
	 * other columns with detector specific data.
	 * 
	 * @param pamDataUnit
	 */
	protected final void fillTableData(PamDataUnit pamDataUnit) {

		PamTableDefinition tableDef = getTableDefinition();
		PamTableItem tableItem;

		tableDef.getIndexItem().setValue(pamDataUnit.getDatabaseIndex());
		/*
		 * All tables have a timestamp near the front of the table. And all data
		 * units have a time in milliseconds, so always fill this in !
		 */
		tableDef.getTimeStampItem().setValue(
				PamCalendar.getTimeStamp(pamDataUnit.getTimeMilliseconds()));

		tableDef.getTimeStampMillis().setValue((int) (pamDataUnit.getTimeMilliseconds()%1000));

		tableDef.getLocalTimeItem().setValue(PamCalendar.getLocalTimeStamp(pamDataUnit.getTimeMilliseconds()));
		
		tableDef.getPCTimeItem().setValue(PamCalendar.getTimeStamp(System.currentTimeMillis()));
		
		

		if (tableDef.getUpdateReference() != null) {
			tableDef.getUpdateReference().setValue(pamDataUnit.getDatabaseIndex());
		}

		for (int i = 0; i < tableDef.getTableItemCount(); i++) {

			tableItem = tableDef.getTableItem(i);
			// if (tableItem.isCounter()) {
			// tableItem.setValue(1);
			// }
			// else
			if (tableItem.getCrossReferenceItem() != null) {
				tableItem.setValue(tableItem.getCrossReferenceItem().getValue());
			}
		}

		setTableData(pamDataUnit);
		
		if (loggingAddOns != null) {
			for (int i = 0; i < loggingAddOns.size(); i++) {
				loggingAddOns.get(i).saveData(getTableDefinition(), pamDataUnit);
			}
		}
	}

	/**
	 * Called when a new PamDataUnit is added to a PamDataBlock to write those
	 * data to the database. Functionality moved down to PamCursor so that 
	 * exact writing method can become database specific if necessary. 
	 * 
	 * @param con
	 *            Database Connection
	 * @param dataUnit
	 *            Pamguard Data unit.
	 * @return true if written and new index of dataUnit retrieved OK
	 * @see PamDataUnit
	 */
	public synchronized boolean logData(Connection con, PamDataUnit dataUnit) {

		if (con == null) {
			return false;
		}
		
		fillTableData(dataUnit);

		PamCursor pamCursor = loggingCursorFinder.getCursor(con, pamTableDefinition);
		if (pamCursor == null) {// null for oodb
			return false;
		}
		
		int newIndex = pamCursor.immediateInsert(con);
		dataUnit.setDatabaseIndex(newIndex);
		return newIndex > 0;
		
	}

	
	/**
	 * Called when an old PamDataUnit is updated. The record is either 
	 * updated or a new record is written, but cross referenced to the old
	 * unit for bookkeeping purposes based on the updatePolicy flag. 
	 * 
	 * @param con
	 *            Database Connection
	 * @param dataUnit
	 *            Pamguard Data nit.
	 * @return the number of rows written to the database.
	 * @see PamDataUnit
	 */
	public synchronized boolean reLogData(Connection con, PamDataUnit dataUnit) {


		if (con == null) {
			return false;
		}
		if (dataUnit.getDatabaseIndex() <= 0) {
			System.out.println("Cannot update data unit with invalid database index in " +
					getTableDefinition().getTableName());
			return false;
		}
		/* 
		 * If the update policy is to overwrite, then need to update the old database
		 * record. Otherwise, need to update the existing record.
		 */
		if (updatePolicy == UPDATE_POLICY_WRITENEW) {
			return logData(con, dataUnit);
		}

		fillTableData(dataUnit);

		PamCursor pamCursor = loggingCursorFinder.getCursor(con, pamTableDefinition);

		return pamCursor.immediateUpdate(con);
	}

	private Connection statementCon;
	private ResultSet resultSet;

	private SQLTypes sqlTypes;

	private ResultSet getResultSet(Connection con) {
		
		if (statementCon != con) {
			if (dbStatement != null) {
				try {
					dbStatement.close();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				dbStatement = null;
			}
			statementCon = con;
		}
		if (dbStatement == null) {
			try {
				//				dbStatement = con.prepareStatement(getTableDefinition().getSQLSelectString(),
				//				ResultSet.TYPE_SCROLL_SENSITIVE,
				//				ResultSet.CONCUR_UPDATABLE);
				//				dbStatement.setFetchDirection(ResultSet.FETCH_UNKNOWN);
				dbStatement = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
				//				dbStatement = insertStatement;
				//				dbStatement.setFetchDirection(ResultSet.FETCH_UNKNOWN);
				resultSet = null;
				sqlTypes = DBControlUnit.findDatabaseControl().getDatabaseSystem().getSqlTypes();
			} catch (SQLException ex) {
				ex.printStackTrace();
				return null;
			}
		}
		// now put some sql into the statement
		//		if (resultSet == null) {
		PamTableDefinition tableDef = getTableDefinition();
		String sqlString = tableDef.getSQLSelectString(sqlTypes);
		// sqlString = "select \"comment\" from userinput";
		try {
			resultSet = dbStatement.executeQuery(sqlString);
			//			resultSet = dbStatement.getResultSet();
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
		//		}
		//		else {
		//		try {
		//		dbStatement.getMoreResults();
		//		} catch (SQLException ex) {
		//		ex.printStackTrace();
		//		return null;
		//		}
		//		}
		return resultSet;
	}

	/**
	 * Called when a new database is connected to read the last values back in
	 * from the database.
	 * 
	 * @param con
	 *            Database connection handle.
	 * @return true is successful or false if no data available or an error
	 *         occurred
	 */
	public boolean readLastData(Connection con) {

		PamCursor pamCursor = loggingCursorFinder.getCursor(con, pamTableDefinition);

		ResultSet result = pamCursor.openReadOnlyCursor(con, "ORDER BY Id");

		PamTableItem tableItem;
		try {
			if (result.last()) {
				for (int i = 0; i < pamTableDefinition.getTableItemCount(); i++) {
					tableItem = pamTableDefinition.getTableItem(i);
					tableItem.setValue(result.getObject(i + 1));
				}
				return true;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
		return false;
	}


	public boolean loadViewerData(long dataStart, long dataEnd, ViewLoadObserver loadObserver) {
		//		generalDatabase.DBControl dbControl = 
		//			(DBControl) PamController.getInstance().findControlledUnit(DBControl.getDbUnitType());
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		if (dbControl == null ) {
			return false;
		}
		PamViewParameters vp = new PamViewParameters();
		vp.useAnalysisTime = false;
		vp.viewStartTime = dataStart;
		vp.viewEndTime = dataEnd;
		return loadViewData(dbControl.getConnection(), vp, loadObserver);
	}

	/**
	 * Load viewer data for a single datablock. <p>
	 * this executes in a Swing Worker thread, so needs to send 
	 * notification objects to that thread, and not direct to the 
	 * Controller so that they can be published back in the AWT
	 * thread. 
	 * @param loadViewerData Swing Worker object
	 * @param con database connection 
	 * @param pamViewParameters viewer parameters. 
	 * @param loadObserver 
	 * @return
	 */
	public boolean loadViewData(Connection con, PamViewParameters pamViewParameters, ViewLoadObserver loadObserver) {

		//		if (isLoadViewData() == false) {
		//			return true; // could, but don't want to !
		//		}

		long lastProgUpdate = System.currentTimeMillis();
		long now;
		if (loadObserver != null && pamViewParameters != null) {
			loadObserver.sayProgress(1, pamViewParameters.viewStartTime, pamViewParameters.viewEndTime, 
					pamViewParameters.viewStartTime, 0);
		}

		ResultSet resultSet = createViewResultSet(con, pamViewParameters);
		if (resultSet == null) return false;
		//		resultSet.
		//		PamController pc = PamController.getInstance();
		//		pc.setupDBLoadProgress(getTableDefinition().getTableName());
		//		loadViewerData.sayProgress(new ViewerLoadProgress(getTableDefinition().getTableName(), -1, -1));
		//		int iCount = 0;
		boolean first = true;
		PamDataUnit newDataUnit = null;
		int nAddOns = 0;
		int nUnits = 0;
		if (loggingAddOns != null) {
			nAddOns = loggingAddOns.size();
		}
		// some bookkeeping stuff used during debug, not needed any more
//		long start = System.currentTimeMillis();
//		int[] tPoints = new int[100];
//		int iPoint = 0;
//		int interval = 1000;
//		long end = 0;
		try {
			
			while (resultSet.next()) {
				transferDataFromResult(resultSet);
				newDataUnit = createDataUnit(lastTime, lastLoadIndex);
				if (newDataUnit != null) {
					for (int i = 0; i < nAddOns; i++) {
						loggingAddOns.get(i).loadData(getTableDefinition(), newDataUnit);
					}
				}
				if ((now = System.currentTimeMillis()) > lastProgUpdate + 500) {
					lastProgUpdate = now;
					if (loadObserver != null) {
						if (pamViewParameters != null && newDataUnit != null) {
							loadObserver.sayProgress(1, pamViewParameters.viewStartTime, pamViewParameters.viewEndTime, 
									newDataUnit.getTimeMilliseconds(), nUnits);
						}
						if (loadObserver.cancelLoad()) {
							break;
						}
					}
				}
				nUnits++;
//				if (nUnits % interval == 0 && iPoint < tPoints.length) {
//					tPoints[iPoint] = (int) (System.currentTimeMillis() - start);
//					iPoint++;
//				}
			}
//			resultSet.getStatement().close();
			resultSet.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
//		end = System.currentTimeMillis();
//		System.out.println(String.format("%s loaded %d datas in %d milliseconds", pamDataBlock.getDataName(), 
//				nUnits, (int) (end-start)));
		//		loadViewerData.sayProgress(new ViewerLoadProgress(null, 0, 0));
		//		pc.setupDBLoadProgress(null);

		return true;
	}

	public String getViewerLoadClause(PamViewParameters pvp) {
		return pamTableDefinition.getBetweenString(pvp) + " ORDER BY UTC, UTCMilliseconds";
	}


	public void deleteData(long dataStart, long dataEnd) {
		PamViewParameters pvp = new PamViewParameters();
		pvp.viewStartTime = dataStart;
		pvp.viewEndTime = dataEnd;
		pvp.useAnalysisTime = false;
		String sqlStr = String.format("DELETE FROM %s %s", 
				pamTableDefinition.getTableName(), 
				pamTableDefinition.getBetweenString(pvp));
		System.out.println(sqlStr);
		Connection con = DBControlUnit.findConnection();
		try {
			Statement stmt = con.createStatement();
			stmt.execute(sqlStr);
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Turn the data, which have been transferred back into the PamTableItems back 
	 * into a useable data unit and put it into the datablock.
	 * @return true if a data unit was sucessfully created
	 */
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {
		return null;
	}

	protected ResultSet createViewResultSet(Connection con, PamViewParameters pamViewParameters) {
		PamCursor pamCursor = viewerCursorFinder.getCursor(con, pamTableDefinition);
		String viewerClause = getViewerLoadClause(pamViewParameters);
		ResultSet resultSet = pamCursor.openReadOnlyCursor(con, viewerClause);

		return resultSet;
	}

	public final boolean transferDataFromResult(ResultSet resultSet) {

		PamTableDefinition tableDef = getTableDefinition();
		PamTableItem tableItem;
		try {
			for (int i = 0; i < tableDef.getTableItemCount(); i++) {
				tableItem = tableDef.getTableItem(i);
				tableItem.setValue(resultSet.getObject(i + 1));
			}
			Timestamp ts = (Timestamp) getTableDefinition().getTimeStampItem().getValue();
			lastTime = PamCalendar.millisFromTimeStamp(ts);
			if (lastTime%1000 == 0) {
				// some databases may have stored the milliseconds, in which 
				// case this next bit is redundant. 
				lastTime += getTableDefinition().getTimeStampMillis().getIntegerValue();
			}
			lastLoadIndex = getTableDefinition().getIndexItem().getIntegerValue();
			return true;

		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public PamDataBlock getPamDataBlock() {
		return pamDataBlock;
	}

	/**
	 * Written to prepare the AIS module for general data emulation - will try
	 * to put it in this higher level class, but will move to 
	 * AISLogger if there are any problems with it. 
	 * @param times time limits for the emulation. 
	 * @return true if statement prepared OK/ 
	 */
	public boolean prepareEmulation(long times[]) {

		Connection con = DBControlUnit.findConnection();
		if (con == null ) {
			return false;
		}
		currentViewParameters = new PamViewParameters();
		currentViewParameters.viewStartTime = times[0];
		currentViewParameters.viewEndTime = times[1];
		currentViewParameters.useAnalysisTime = false;
		mixedModeResult = createViewResultSet(con, currentViewParameters);
		return (mixedModeResult != null);
	}

	public boolean readNextEmulation() {
		if (mixedModeResult == null) {
			return false;
		}
		boolean ans;
		try {
			ans = mixedModeResult.next();
			if (ans == false) {
				return false;
			}
			return transferDataFromResult(mixedModeResult);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	private PreparedStatement mixedModeStatement;

	private ResultSet mixedModeResult;

	public boolean prepareForMixedMode(Connection con) {
		long history = pamDataBlock.getRequiredHistory();
		long now = PamCalendar.getTimeInMillis();
		long queryStart = now - history;
		System.out.println("now and start = " + PamCalendar.formatDateTime(now) + "     " + PamCalendar.formatDateTime(queryStart));
		currentViewParameters = new PamViewParameters();
		currentViewParameters.viewStartTime = queryStart;
		currentViewParameters.viewEndTime = 0;
		currentViewParameters.useAnalysisTime = false;
		mixedModeResult = createViewResultSet(con, currentViewParameters);
		//		mixedModeStatement = getSelectStatement(con, currentViewParameters);
		//		try {
		//			if (mixedModeResult != null) {
		//				mixedModeResult.close();
		//			}
		//			mixedModeResult = mixedModeStatement.executeQuery();
		//		}
		//		catch (SQLException ex) {
		//			mixedModeResult = null;
		//			return false;
		//		}
		mixedDataWaiting = false;
		lastTime = 0;
		return readMixedModeData(now);
	}
	private boolean mixedDataWaiting = false;

	/**
	 * always creates the data unit on the next pass through each loop so that they are only
	 * created AFTER the tiem cut off ahs passed. 
	 * @param timeTo
	 * @return true if data were read and used
	 */
	public boolean readMixedModeData(long timeTo) {
		if (mixedModeResult == null) return false;
		if (lastTime > timeTo) {
			return false;
		}
		PamDataUnit newDataUnit;
		int nAddOns = 0;
		if (loggingAddOns != null) {
			nAddOns = loggingAddOns.size();
		}
		try {
			while (true) {
				if (mixedDataWaiting) {
					newDataUnit = createDataUnit(lastTime, lastLoadIndex);
					if (newDataUnit != null) {
						for (int i = 0; i < nAddOns; i++) {
							loggingAddOns.get(i).loadData(getTableDefinition(), newDataUnit);
						}
					}
				}
				if (mixedModeResult.next() == false) break;

				transferDataFromResult(mixedModeResult);
				mixedDataWaiting = true;
				//				System.out.println("last time, time to, " + PamCalendar.formatDateTime(lastTime) + " : " +
				//				PamCalendar.formatDateTime(timeTo));
				if (lastTime > timeTo) {
					break;
				}
			}
		}
		catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public int getUpdatePolicy() {
		return updatePolicy;
	}

	public void setUpdatePolicy(int updatePolicy) {
		this.updatePolicy = updatePolicy;
	}

	public boolean isCanView() {
		return canView;
	}

	public void setCanView(boolean canView) {
		this.canView = canView;
	}

	public boolean isLoadViewData() {
		return loadViewData;
	}

	public void setLoadViewData(boolean loadViewData) {
		this.loadViewData = loadViewData;
	}

	/**
	 * Recheck the databse tables associated with this 
	 * Logger. This only needs to be called if the content of 
	 * tables has changed during PAMGUARD operations.  
	 * @return true if checks sucessful. 
	 * <p>Note that if no databse is present, false will be returned
	 */
	public boolean reCheckTable() {
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		if (dbControl == null) {
			return false;
		}
		return dbControl.getDbProcess().checkTable(getTableDefinition());
	}

	/**
	 * Gives each module a chance to do additional checks and alterations
	 * to the database table at connection time. e.g. the GPS module may
	 * want to check that the table is not an old Logger table and if
	 * necessary alter the columns into the PAMGUARD format. 
	 * @param connection
	 * @return true if tests and alterations OK. 
	 */
	public boolean doExtraChecks(DBProcess dbProcess, Connection connection) {
		return true;
	}

	/**
	 * 
	 * @return the last time of data read in during mixed mode or
	 * NMEA emulations. 
	 */
	public long getLastTime() {
		return lastTime;
	}

	/**
	 * Save offline data in viewer mode.
	 * <p>
	 * This is a pretty basic function which assumes pretty much a 
	 * 1:1 correspondence between data loaded into the viewer and 
	 * data to be saved. 
	 * <p>
	 * Three types of saving to do 
	 * <ol>
	 * <li>Pre existing 1:1 correspondence between data in memory and database
	 * <li>Viewer will have added or removed data  (e.g. map comments may easily have added data)
	 * <li>Weird stuff like in the click detector where the database is only holding information about
	 * a subset of clicks held in the binary data files 
	 * </ol>
	 * <p>Should be able to deal with first tow here, but functions must override for 3. 
	 * @param dbControlUnit 
	 * @param connection 
	 * @return 
	 */
	public boolean saveOfflineData(DBControlUnit dbControlUnit, Connection connection) {
		return standardOfflineSave(dbControlUnit, connection);
	}

	private boolean standardOfflineSave(DBControlUnit dbControlUnit, Connection connection) {
		/**
		 * first work out what needs doing. 
		 */
		SaveRequirements sr = getPamDataBlock().getSaveRequirements(dbControlUnit);
		if (sr.getTotalChanges() == 0) {
			return true;
		}
		System.out.println("Update requirements for " + getPamDataBlock().getDataName());
		System.out.println("Updates   = " + sr.getNumUpdates());
		System.out.println("Additions = " + sr.getNumAdditions());
		System.out.println("Deletions = " + sr.getNumDeletions());

		int nUpdates = 0;
		int nAdditions = 0;
		String clause;
		//		DataUnitFinder<PamDataUnit> deleteFinder = new DeletedDataUnitFinder(getPamDataBlock());
		PamCursor pamCursor;
		PamDataUnit aUnit;
		int cursorId;
		if (sr.getNumUpdates()+sr.getNumAdditions() > 0) {
			try {
				DataUnitFinder<PamDataUnit> updateFinder = new DataUnitFinder<PamDataUnit>(getPamDataBlock(), 
						new DatabaseIndexUnitFinder());
								
				if (sr.getNumUpdates() > 0) {
					clause = getViewerUpdateClause(sr);
				}
				else {
					clause = "WHERE Id < 0"; // generate a null set. 
				}
				pamCursor = dbControlUnit.createPamCursor(getTableDefinition());
				if (pamCursor.openScrollableCursor(connection, true, true, clause) == false) {
					System.out.println("Error opening update cursor " + pamDataBlock.getDataName());
				}
				if (sr.getNumUpdates() > 0) while(pamCursor.next()) {
					cursorId = pamCursor.getInt(1);
					aUnit = updateFinder.findDataUnit(cursorId);
					if (aUnit == null) {
						System.out.println(String.format("Unable to find data unit for index %d in %s",
								cursorId, pamDataBlock.getDataName()));
						continue;
					}
					if (aUnit.getUpdateCount() == 0) {
						continue;
					}
					fillTableData(aUnit);
					pamCursor.moveDataToCursor(true);
					pamCursor.updateRow();
					aUnit.clearUpdateCount();
					nUpdates++;
				}
				if (sr.getNumAdditions() > 0) {
					ListIterator<PamDataUnit> li = pamDataBlock.getListIterator(0);
					while (li.hasNext()) {
						aUnit = li.next();
						if (aUnit.getDatabaseIndex() > 0) {
							continue;
						}
						pamCursor.moveToInsertRow();
						fillTableData(aUnit);
						pamCursor.moveDataToCursor(true);
						cursorId = pamCursor.insertRow(true);
						if (cursorId < 0) {
							System.out.println("Unable to get cursor Id in " + pamDataBlock.getDataName());
						}
						else {
							nAdditions ++;
							aUnit.setDatabaseIndex(cursorId);
						}
					}
				}
				pamCursor.updateDatabase();
				pamCursor.closeScrollableCursor();
			}
			catch (SQLException e) {
				System.out.println("Error updating database in " + pamDataBlock.getDataName());
				e.printStackTrace();
			}
		}
		if (sr.getNumUpdates() != nUpdates) {
			System.out.println(String.format("The number of updates (%d) does not match " +
					"the number of records which require updating (%d) in %s", 
					nUpdates, sr.getNumUpdates(), pamDataBlock.getDataName()));
		}
		if (sr.getNumAdditions() != nAdditions) {
			System.out.println(String.format("The number of inserts (%d) does not match " +
					"the number of records which require inserting (%d) in %s", 
					nUpdates, sr.getNumUpdates(), pamDataBlock.getDataName()));
		}
		if (sr.getNumDeletions() > 0) {
			String sqlString = String.format("DELETE FROM %s WHERE Id %s", 
					pamTableDefinition.getTableName(), createInClause(sr.getDeleteIndexes()));
			try {
				Statement s = connection.createStatement();
				s.execute(sqlString);
				s.close();
				pamDataBlock.clearDeletedList();
			} catch (SQLException e) {
				System.out.println("Delete failed with " + sqlString);
				e.printStackTrace();
			}
		}
		
		return true;
	}


	/**
	 * Get a select clause for viewer updates. this may be overridden for some
	 * data types depending on what' most optimal for date retrieval.
	 * <p> A couple of examples you may want to use are in 
	 * getTimesUpdateClause and getIdListUpdatClause
	 * <p>getIdListUpdatClause is currently the default. 
	 * @param sr requirements extracted from loaded data 
	 * @return clause string (including any sort)
	 */
	public String getViewerUpdateClause(SaveRequirements sr) {
		return getIdListUpdatClause(sr);
	}	
	
	/**
	 * Get a select clause for viewer updates. this may be overridden for some
	 * data types depending on what' most optimal for date retrieval.
	 * <p>for example, the default selection is based on time - which won't work 
	 * if the event times may have changed - ok for things which will be fixed in time.   
	 * @param sr requirements extracted from loaded data 
	 * @return clause string (including any sort)
	 */
	public String getTimesUpdateClause(SaveRequirements sr) {
		PamViewParameters pvp = new PamViewParameters();
		pvp.setViewStartTime(sr.getFirstUpdateTime());
		pvp.setViewEndTime(sr.getLastUpdateTime());
		pvp.useAnalysisTime = false;
		return pamTableDefinition.getBetweenString(pvp);
	}
	
	public String getIdListUpdatClause(SaveRequirements sr) {
		return "WHERE Id " + createInClause(sr.getUpdatedIndexes());
	}
	
	/**
	 * Make an SQL clause in the from IN ( ... )
	 * @param idList
	 * @return string clause. 
	 */
	public String createInClause(int[] idList) {
		if (idList == null) {
			return null;
		}
		Arrays.sort(idList);
		String clause = "IN (";
		boolean first = true;
		for (int i = 0; i < idList.length; i++) {
			if (first) {
				first = false;
			}
			else {
				clause += ", ";
			}
			clause += idList[i];
		}
		clause += ")";
		return clause;
	}

	/**
	 * Add an SQL Logging addon - something which adds some standard colums
	 * to what it probably a non standard table, e.g. adding target motion 
	 * results to a click event detection. 
	 * @param sqlLoggingAddon
	 */
	public void addAddOn(SQLLoggingAddon sqlLoggingAddon) {
		if (loggingAddOns == null) {
			loggingAddOns = new ArrayList<SQLLoggingAddon>();
		}
		loggingAddOns.add(sqlLoggingAddon);
		if (pamTableDefinition != null) {
			sqlLoggingAddon.addTableItems(pamTableDefinition);
		}
	}
	
	/**
	 * Reset anything needing resetting in the binary data source. 
	 * This get's called just before PamStart(). 
	 */
	public void reset() {		
	}

}
