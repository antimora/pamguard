package generalDatabase;

import generalDatabase.pamCursor.PamCursor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import whistlesAndMoans.ConnectedRegionDataBlock;

import loggerForms.UDFTableDefinition;

import PamController.PamController;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class DBProcess extends PamProcess {

	private DBControl databaseControll;
	
	private ArrayList<PamDataBlock> dataBlocks;
	
	private javax.swing.Timer timer;
	
	private int dbWriteOKs;
	
	private int dbWriteErrors;

	private ArrayList<DbSpecial> dbSpecials = new ArrayList<DbSpecial>();
	
	private Timer viewTimer;
	
	private LogModules logModules;
	private LogSettings logSettings, logLastSettings;
	
	private PamCursor cursor;
	
	
	public DBProcess(DBControl databaseControll) {
		super(databaseControll, null);
		this.databaseControll = databaseControll;
		
		timer = new Timer(1000, new TimerAction());
		timer.start();
		
		viewTimer = new Timer(500, new ViewTimerAction());
		
		dbSpecials.add(logModules = new LogModules(databaseControll));
		dbSpecials.add(logSettings = new LogSettings(databaseControll, "Pamguard Settings", false));
		dbSpecials.add(logLastSettings = new LogSettings(databaseControll, "Pamguard Settings Last", true));
		
	}

	@Override
	public void pamStart() {
		
		if (PamController.getInstance().getRunMode() == PamController.RUN_MIXEDMODE) {
			prepareForMixedMode();
		}
	}
	
	protected boolean saveStartSettings() {
		Connection con = databaseControll.getConnection();
		if (con != null) {
			for (int i = 0; i < dbSpecials.size(); i++) {
				dbSpecials.get(i).pamStart(con);
			}
			return true;
		}
		return false;
	}

	@Override
	public void pamStop() {
		Connection con = databaseControll.getConnection();
		if (con != null) {
			for (int i = 0; i < dbSpecials.size(); i++) {
				dbSpecials.get(i).pamStop(con);
			}
		}		
		viewTimer.stop();
	}
	
	/**
	 * Called from the settings manager whenever settings would normally be saved to 
	 * file. Just saves the latest of all PAMGUARD settings, first deleting any 
	 * other settings in the logLastSettings table. 
	 * <p>
	 * The logSettings object does a slightly different task of always storing 
	 * the current PAMGAURD settings in a table which grows and grows, giving a
	 * permanent record of PAMGUARD settings over time. 
	 * <p>
	 * Unlike the settings in the growing table of logSettings, the settings
	 * stored from logLastSettings are also stored when viewer or mixed mode
	 * is exited.   
	 * @return true if successful. 
	 */
	public boolean saveSettingsToDB() {
		if (clearTable(logLastSettings.getTableDefinition())) {
			return logLastSettings.saveAllSettings();
		}
		else {
			return false;
		}
	}
	private void prepareForMixedMode() {
		PamDataBlock dataBlock;
		for (int i = 0; i < dataBlocks.size(); i++) {
			dataBlock = dataBlocks.get(i);
			if (dataBlock.getCanLog() && dataBlock.getMixedDirection() == PamDataBlock.MIX_OUTOFDATABASE) {
				dataBlock.getLogging().prepareForMixedMode(databaseControll.getConnection());
			}
		}
		viewTimer.start();
	}
	
	private void viewTimerAction() {
		PamDataBlock dataBlock;
		long timeTo = PamCalendar.getTimeInMillis();
		for (int i = 0; i < dataBlocks.size(); i++) {
			dataBlock = dataBlocks.get(i);
			if (dataBlock.getCanLog() && dataBlock.getMixedDirection() == PamDataBlock.MIX_OUTOFDATABASE) {
				dataBlock.getLogging().readMixedModeData(timeTo);
			}
		}
	}
	

	public synchronized void checkTables() {
		
		Connection dbCon = databaseControll.getConnection();
		if (dbCon == null) {
			return;
		}
		//get rid of spaces in table names
		// this doesn't currently work and also leaves the tables in a locked state which stops other stuff from working. 
//		try {
//			DatabaseMetaData dbmd = dbCon.getMetaData();
//			String[] types = {"TABLE"};
////			getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) 
//			ResultSet resultSet = dbmd.getTables(null, null, "%", types);
//			
//			//Loop through database tables
//			while (resultSet.next()){
//				String tableName = resultSet.getString(3);
//				//If contains a space deblank it
//				if(  tableName.contains(" ".subSequence(0, 1))){
//					String newTableName=EmptyTableDefinition.deblankString(tableName);
//					if (renameTable(tableName, newTableName)){
//						tableName=newTableName;
//					}
//				}
//				
//				// Create a result set
//			    Statement stmt = dbCon.createStatement();
//			    ResultSet rs = stmt.executeQuery("SELECT * FROM "+tableName);
//
//			    // Get result set meta data
//			    ResultSetMetaData rsmd = rs.getMetaData();
//			    int numColumns = rsmd.getColumnCount();
//
//			    // Get the column names; column indices start from 1
//			    for (int i=1; i<numColumns+1; i++) {
//			        String columnName = rsmd.getColumnName(i);
//
//			        if (columnName.contains(" ".subSequence(0, 1))){
//			        	String newColumnName= EmptyTableDefinition.deblankString(columnName);
//			        	renameColumn(tableName, columnName, newColumnName);
//			        }
//			    }
//			}
//		} catch (SQLException e) {
////			e.printStackTrace();
//			System.out.println(e.getMessage());
//		}
		

		
		dataBlocks = PamController.getInstance().getDataBlocks();
		PamTableDefinition tableDefinition;
		SQLLogging logging;
		
		// for each datablock, check that the process can log (ignoring GPS process)
		if (databaseControll.isFullTablesCheck()) {
		for (int i = 0; i < dataBlocks.size(); i++) {
			logging = dataBlocks.get(i).getLogging();
			if (logging != null) {
				if ((tableDefinition = logging.getTableDefinition()) != null) {
					if (tableDefinition.getCheckedConnection() != databaseControll.getConnection()) {
						if (logging.doExtraChecks(this, databaseControll.getConnection()) == false){
							continue;
						}
						if (checkTable(tableDefinition)) {
							tableDefinition.setCheckedConnection(databaseControll.getConnection());
						}
					}
				}
			}
		}
		}

		Connection con = databaseControll.getConnection();
		if (con != null) {
			for (int i = 0; i < dbSpecials.size(); i++) {
				logging = dbSpecials.get(i);
				if ((tableDefinition = logging.getTableDefinition()) != null) {
					if (tableDefinition.getCheckedConnection() != databaseControll.getConnection()) {
						if (checkTable(tableDefinition)) {
							tableDefinition.setCheckedConnection(databaseControll.getConnection());
						}
					}
				}
			}
		}
	}
	
	/**
	 * Check a database table. If it does not exist, create it. 
	 * <p>Then check all columns and if a column does not exist, 
	 * create that too.
	 * @param tableDef table definition
	 * @return true if table is OK, i.e. table and all columns either
	 * existed or were successfully created. 
	 */
	public boolean checkTable(EmptyTableDefinition tableDef) {
		// check the table exists and column format for a particular table. 
		if (databaseControll.getConnection() == null) return false;
		
//		System.out.println("Checking table " + tableDef.getTableName());

		if (tableExists(tableDef) == false) {
			createTable(tableDef);
		}
		if (tableExists(tableDef) == false) return false;
		
		
		
		if (tableDef.tableName.startsWith("UDF_")){
		//	fixUDFTableColumnNames(tableDef);
		} else { //this should now be PamTable
			fixLocalTimeProblem(tableDef);
		}
		
		int columnErrors = 0;
		
		for (int i = 0; i < tableDef.getTableItemCount(); i++) {
			if (checkColumn(tableDef, tableDef.getTableItem(i)) == false) {
				columnErrors++;
			}
		}
		
		return (columnErrors == 0);
	}
	
	/**
	 * Check a database table exists. 
	 * @param tableDef table definition
	 * @return true if the table exists
	 */
	public boolean tableExists(EmptyTableDefinition tableDef) {
		try {
			DatabaseMetaData dbm = databaseControll.getConnection().getMetaData();
			ResultSet tables = dbm.getTables(null, null, tableDef.getTableName(), null);
			
			if (tables.next()){
				 return true;
			}
			if (databaseControll.databaseSystem.getSystemName().equals(OOoDBSystem.SYSTEMNAME)){
				ResultSet oodbTables = dbm.getTables(null, null, /*tableDef.getTableName().toUpperCase()*/null, null);
				
				while (oodbTables.next()){
					
					if (oodbTables.getString(3).trim().equalsIgnoreCase(tableDef.getTableName())){
//						System.out.println("Table Found: "+oodbTables.getString(3));
						tableDef.setTableName(oodbTables.getString(3).trim().toUpperCase());
						return true;
					}
					
				}
				
				System.out.println("Table Not Found: "+tableDef.getTableName().toUpperCase());
			}
			
		}
		catch (SQLException e) {
			System.out.println("Invalid table name " + tableDef.getTableName());
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * From early 2008 (release 1.1) until August 2008 (release 1.1.1)
	 * A LocalTime column had been added to database tables to store the local computer time
	 * with each record as well as UTC. This feature was only tested with MS Access.
	 * <p>Unfortunately, it transpires that LocalTime is a reserved word in MySQL, so 
	 * while MS Access databases worked OK, it
	 * became impossible to create tables with MySQL.
	 * <p>The column name has now been changed from LocalTime to PCLocalTime. New databases
	 * will not be affected, however, there are now old MS Access databases out there 
	 * that have a LocalTime column, where we need a PCLocalTime column. 
	 * <p>This function attempts to fix this automatically.  
	 * @param tableDef table def of table to fix. 
	 */
	private static String expMessage = "<html>From V 1.1.1 the column LocalTime was renamed to " +
	"PCLocalTime for compatibility with MySQL. "+
	"<p>A new PCLocalTime columnn will be created and data in the LocalTime column will be duplicated</html>";
	void fixLocalTimeProblem(EmptyTableDefinition tableDef) {
		/*
		 * If the DB is NOT MS Access, not need to do anything. while the error occurred
		 * it would have been impossible to create any other type of database. 
		 */
		if (databaseControll.databaseSystem.getSystemName().equals(MSAccessSystem.SYSTEMNAME) == false) {
			return;
		}
		boolean hasPCLocalTime = columnExists(tableDef, "PCLocalTime", Types.TIMESTAMP);
		boolean hasLocalTime = columnExists(tableDef, "LocalTime", Types.TIMESTAMP);
		
		/*
		 * If it already has a PCLocalTime column, then we've probably been here before 
		 * and all is OK, 
		 */
		if (hasLocalTime == true && hasPCLocalTime == false) {
			// we have a problem !
			int ans = JOptionPane.showConfirmDialog(null, expMessage, 
					"Database table " + tableDef.getTableName(), JOptionPane.OK_CANCEL_OPTION);
			if (ans == JOptionPane.CANCEL_OPTION) {
				return; // user doens't want to do anything. 
			}
			// create the new column, then try to duplicate the data
			PamTableItem ti = tableDef.findTableItem("PCLocalTime");
			addColumn(tableDef, ti);
			
			String renameStr = String.format("UPDATE \"%s\" SET \"PCLocalTime\" = \"LocalTime\"", tableDef.getTableName());

			Statement stmt;
			
			try {
				stmt = databaseControll.getConnection().createStatement();   
				ans = stmt.executeUpdate(renameStr); 
			}
			catch (SQLException ex) {
				System.out.println(String.format("Error %d in %s", ex.getErrorCode(), renameStr));
				ex.printStackTrace();
				return;
			}
		}
	}

	/**
	 * 
	 * @param oldName
	 * @param newName
	 * @return true if worked
	 */
	public boolean renameTable(String oldName,String newName){
		
		
		//TODO for mysql update to use ALTER TABLE `dbname`.`tablename` RENAME TO  `dbname`.`newtablename` ;
//		String renameString= "ALTER TABLE \""+oldName+"\" RENAME TO \""+newName+"\"";
		
		
		String copyString ="SELECT * INTO \""+newName+"\" FROM \""+oldName+"\"";//SELECT * may not work at times also quotes
		String dropString ="DROP TABLE \""+oldName+"\"";
		
		Statement stmt;
		
		try {
			stmt = databaseControll.getConnection().createStatement();
			int renameResult;
			renameResult = stmt.executeUpdate(copyString);
//			System.out.println("renRes:"+renameResult);
			
			copyString=dropString;
			renameResult=stmt.executeUpdate(copyString);
			
		}
		catch (SQLException ex) {
			System.out.println(copyString);
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param tableName
	 * @param oldName
	 * @param newName
	 * @return true if worked
	 */
	boolean renameColumn(String tableName,String oldName,String newName){
		
		String renameString= "ALTER TABLE \""+tableName +"\" RENAME COLUMN \""+oldName+"\" TO \""+newName+"\"";
		
		String newColumnStrin="ALTER TABLE NEW COLUMN \""+newName+"\"";
		String copyDataString="UPDATE Table1 SET Table1.Field2 = [Table1]![Field1]";
		
		
		Statement stmt;
		
		try {
			stmt = databaseControll.getConnection().createStatement();
			int renameResult; 
			System.out.println(renameString);
			renameResult = stmt.executeUpdate(renameString); 
		}
		catch (SQLException ex) {
//			System.out.println(renameString);
			System.out.println(ex.getMessage() + " " +renameString);
//			ex.printStackTrace();
			return false;
		}
		return true;
	}

	
	
//	private void fixLocalTimeProblem1(EmptyTableDefinition tableDef){
//		
//		//If the DB is NOT MS Access, not need to do anything. while the error occurred
//		//it would have been impossible to create any other type of database. 
//		
//		
//		if (databaseControll.databaseSystem.getSystemName().equals(MSAccessSystem.SYSTEMNAME) == false) {
//			return;
//		}
//		renameTableColumn(tableDef, "LocalTime","PCLocalTime",false);
//	}

//	private void fixUDFTableColumnNames(EmptyTableDefinition tableDef){
//		renameTableColumnIfNecessary(tableDef,"NMEA String"	,"NMEA_String" ,  true);//Access handles spaces ok.
//		renameTableColumn(tableDef,"Default"		,"DefaultField",  true);
		
//	}
	
	
	/**
	 * Check a database table column exists. If it doesn't
	 * exist, attempt to create it. 
	 * @param tableDef table definition
	 * @param tableItem table item
	 * @return true if column existed or was created. 
	 */
	public boolean checkColumn(EmptyTableDefinition tableDef, PamTableItem tableItem) {
		if (columnExists(tableDef, tableItem) == false) {
			if (addColumn(tableDef, tableItem) == false) return false;
		}
		if (tableItem.isPrimaryKey()){
			
		}
		if (tableItem.isRequired()){
			
		}
		
		return true;
	}
	
	/**
	 * Check that a specific table column exists
	 * @param tableDef table definition
	 * @param tableItem table item
	 * @return true if the column exists
	 */
	public boolean columnExists(EmptyTableDefinition tableDef, PamTableItem tableItem) {
		return columnExists(tableDef, tableItem.getName(), tableItem.getSqlType());
	}

	/**
	 * Check a specific table column exists
	 * @param tableDef table definition
	 * @param columnName column name
	 * @param sqlType column sql type
	 * @return true if the column exists and has the correct format. 
	 */
	private boolean columnExists(EmptyTableDefinition tableDef, String columnName, int sqlType) {
		return columnExists(tableDef.getTableName(), columnName, sqlType);
	}
	/**
	 * Check a specific table column exists
	 * @param tableName table name
	 * @param columnName column name
	 * @param sqlType column sql type
	 * @return true if the column exists and has the correct format. 
	 */
//	private boolean columnExists(String tableName, String columnName, int sqlType) {
//
//		try {
//			DatabaseMetaData dbm = databaseControll.getConnection().getMetaData();
//			ResultSet columns = dbm.getColumns(null, null, tableName, columnName);
//			if (columns.next() == false) return false; 
//			// now check the format
//			String colName = columns.getString(4);
//			int colType = columns.getInt(5);
////			if (colType == tableItem.getSqlType()) return true;
////			//String strColType = columns.getString(6);
////			return false;
//		}
//		catch (SQLException e) {
//			e.printStackTrace();
//			return false;
//		}
//		
//		return true;
//	}
	private boolean columnExists(String tableName, String columnName, int sqlType) {

		try {
			DatabaseMetaData dbm = databaseControll.getConnection().getMetaData();
			ResultSet columns = dbm.getColumns(null, null, tableName, columnName);
			while (columns.next()) {
				
				// now check the format
				String colName = columns.getString(4);
				int colType = columns.getInt(5);
				//			if (colType == tableItem.getSqlType()) return true;
				//			//String strColType = columns.getString(6);
				if (columnName.equalsIgnoreCase(colName)) {
					return true;
				}
			}
			if (databaseControll.databaseSystem.getSystemName().equals(OOoDBSystem.SYSTEMNAME)){
				columns = dbm.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase());
				
				while (columns.next()) {
					// now check the format
					String colName = columns.getString(4);
					int colType = columns.getInt(5);
					int colLength= columns.getInt(7);
//					System.out.println("collength="+colLength);
//					colLength= columns.getInt(9);
					
					
					
					
					
					//			if (colType == tableItem.getSqlType()) return true;
					//			//String strColType = columns.getString(6);
					if (columnName.equalsIgnoreCase(colName)) {
//						String colTypeString=databaseControll.databaseSystem.getSqlTypes().typeToString(colType, colLength);
//						System.out.println("Found column " +colName +" type: "+colTypeString+" in "+tableName);
						
						
						return true;
					}
//					System.out.println("Col not found");
				}
			}
			
			
//			return false;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return false;
	}

	/**
	 * Change the format of a column. 
	 * @return true if change sucessful
	 */
	public boolean changeColumnFormat(String tableName, PamTableItem tableItem) {
		/*
		 * Create a temp column, copy the data over, then rename the column.
		 */
		SQLTypes sqlTypes = databaseControll.databaseSystem.getSqlTypes();
		String tempColName = "tttteeeemmmmmpppp";
		String sqlCmd;
		if (columnExists(tableName, tempColName, tableItem.getSqlType())) {
			sqlCmd = String.format("ALTER TABLE %s DROP COLUMN %s", tableName, tempColName);
			if (runStmt(sqlCmd) == false) {
				return false;
			}
		}

		// create the temp column
		sqlCmd = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, tempColName,
				sqlTypes.typeToString(tableItem.getSqlType(), tableItem.getLength()));
		if (runStmt(sqlCmd) == false) {
			return false;
		}
		
		// copy over the data
		sqlCmd = String.format("UPDATE %s SET %s = %s", tableName,
				tempColName, sqlTypes.formatColumnName(tableItem.getName()));
		if (runStmt(sqlCmd) == false) {
			return false;
		}
		
		// delete the original column
		sqlCmd = String.format("ALTER TABLE %s DROP COLUMN %s", tableName, 
				sqlTypes.formatColumnName(tableItem.getName()));
		if (runStmt(sqlCmd) == false) {
			return false;
		}

		// create the temp column
		sqlCmd = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, 
				sqlTypes.formatColumnName(tableItem.getName()),
				sqlTypes.typeToString(tableItem.getSqlType(), tableItem.getLength()));
		if (runStmt(sqlCmd) == false) {
			return false;
		}

		// copy over the data back to the replaced original column
		sqlCmd = String.format("UPDATE %s SET %s = %s", tableName,
				sqlTypes.formatColumnName(tableItem.getName()), tempColName);
		if (runStmt(sqlCmd) == false) {
			return false;
		}
		
		// delete the original column
		sqlCmd = String.format("ALTER TABLE %s DROP COLUMN %s", tableName, tempColName);
		if (runStmt(sqlCmd) == false) {
			return false;
		}

			
		return true;
	}

	private boolean runStmt(String str) {
		Connection con = databaseControll.getConnection();
		Statement stmt;
		try {
			stmt = con.createStatement();   
			int addResult;
			addResult = stmt.executeUpdate(str); 
		}
		catch (SQLException ex) {
			System.out.println(str);
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Create the entire table from scratch using a single SQL command
	 * @param tableDef Table definition structure
	 * @return true if table created successfully
	 * @see PamTableDefinition
	 */
	private synchronized boolean createTable(EmptyTableDefinition tableDef) {
		
		if (databaseControll.getConnection() == null) return false;
		SQLTypes sqlTypes = databaseControll.databaseSystem.getSqlTypes();
		PamTableItem tableItem;
		int nPrimaryKey = 0;
		String createString = "CREATE TABLE " + sqlTypes.formatColumnName(tableDef.getTableName()) + " (";
		for (int i = 0; i < tableDef.getTableItemCount(); i++) {
			tableItem = tableDef.getTableItem(i);
			createString += sqlTypes.formatColumnName(tableItem.getName()) + " ";
			// command for making counters seems to be different for Access and MySQL !
			// Access uses counter and mysql uses integer auto_increment
			createString += sqlTypes.typeToString(tableItem.getSqlType(), tableItem.getLength(), tableItem.isCounter());
			if (tableItem.isCounter() && tableItem.getSqlType() == Types.INTEGER) {
//				createString += " ";
//				createString += " NOT NULL ";
			}
			if (i < tableDef.getTableItemCount() - 1) {
				createString += ", ";
			}
			if (tableItem.isPrimaryKey()) {
				nPrimaryKey++;
			}
		}
		if (nPrimaryKey > 0) {
			int usedPrimaryKeys = 0;
			createString += ", PRIMARY KEY (";
			for (int i = 0; i < tableDef.getTableItemCount(); i++) {
				tableItem = tableDef.getTableItem(i);
				if (tableItem.isPrimaryKey()) {
					createString += sqlTypes.formatColumnName(tableItem.getName());
					usedPrimaryKeys++;
					if (usedPrimaryKeys < nPrimaryKey) {
						createString += ", ";
					}
				}
			}
			createString += ")";
		}
		createString += " )";
		
//		createString = "CREATE TABLE testData45 (\"testCol\" TIMESTAMP, otherCol INTEGER)";
		
		System.out.println(createString);
		Statement stmt;

		try {
			stmt = databaseControll.getConnection().createStatement();   
			int createResult;
			createResult = stmt.executeUpdate(createString); 
		}
		catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
//		databaseController.registerTables();
		return true;
	}


	/**
	 * Deletes column from table,
	 * @param tableDef tableDef identifying the table.
	 * @param tableItem tableItem to delete if not isRequired or isPrimaryKey.
	 * @return true if successful.
	 */
	private boolean deleteColumn(EmptyTableDefinition tableDef, String columnName) { //Was Based on add column method
		//SQLTypes sqlTypes = databaseControll.databaseSystem.getSqlTypes();
		String delString = "ALTER TABLE " + tableDef.getTableName();
		delString += " DROP COLUMN " + columnName ;
		//addString += sqlTypes.typeToString(tableItem.getSqlType(), tableItem.getLength());
	//	if (tableItem.isRequired() || tableItem.isPrimaryKey()) {
	//		System.out.println(tableDef.getTableName()+" "+tableItem.getName()+" will not be deleted as isRequired or isPrimaryKey");
	//		return false;
	//	}
	
		
		//System.out.println(addString);
	
		Statement stmt;
		
		try {
			stmt = databaseControll.getConnection().createStatement();   
			int delResult;
			delResult = stmt.executeUpdate(delString); 
		}
		catch (SQLException ex) {
			System.out.println(delString);
			ex.printStackTrace();
			return false;
		}
	
		return true;
	}

	
	
	
	private boolean addColumn(EmptyTableDefinition tableDef, PamTableItem tableItem) {
	
		SQLTypes sqlTypes = databaseControll.databaseSystem.getSqlTypes();
		String addString = "ALTER TABLE " + tableDef.getTableName();
		
		
		if (tableItem.isCounter() && databaseControll.databaseSystem.getSystemName().equals(MSAccessSystem.SYSTEMNAME)) {
			//Access and MySQL may handle these differently as per create table
			
			addString += " ADD " + sqlTypes.formatColumnName(tableItem.getName()) + " COUNTER ";
			
			
		}else {
			addString += " ADD COLUMN " + sqlTypes.formatColumnName(tableItem.getName()) + " ";
			
			addString += sqlTypes.typeToString(tableItem.getSqlType(), tableItem.getLength(), tableItem.isCounter());
		}
		
		Statement stmt;
		
		try {
			stmt = databaseControll.getConnection().createStatement();
			int addResult;
			addResult = stmt.executeUpdate(addString);
			stmt.close();
		}
		catch (SQLException ex) {
			System.out.println(addString);
			ex.printStackTrace();
			return false;
		}
		
		if (tableItem.isPrimaryKey()) {
			String primaryString = "ALTER TABLE "+tableDef.getTableName()+" ADD PRIMARY KEY ( " + 
			sqlTypes.formatColumnName(tableItem.getName()) + " )";;
		
			try {
				stmt = databaseControll.getConnection().createStatement();   
				int primaryResult;
				System.out.println(primaryString);
				primaryResult = stmt.executeUpdate(primaryString); 
			}
			catch (SQLException ex) {
				System.out.println("Column added but could not be made primary key");
				System.out.println(primaryString);
				ex.printStackTrace();
				
				return false;
			}
		}
		return true;
		
	}
	
	/**
	 * Completely clear the contents of a table
	 * @param tableDef tabledef identifying the table. 
	 * @return true if successful.
	 */
	public boolean clearTable(EmptyTableDefinition tableDef) {
		return clearTable(tableDef.getTableName());
	}
	
	public boolean clearTable(String tableName) {
		
		String deleteStr = String.format("DELETE FROM %s", tableName);

		Statement stmt;
		
		if (databaseControll.getConnection() == null) {
			return false;
		}
		
		try {
			stmt = databaseControll.getConnection().createStatement();   
			int addResult;
			addResult = stmt.executeUpdate(deleteStr); 
		}
		catch (SQLException ex) {
			System.out.println(deleteStr);
			ex.printStackTrace();
			return false;
		}
		
		return true;
	}

	public void updateProcessList(){
		ArrayList<PamDataBlock> dataBlocks = PamController.getInstance().getDataBlocks();
		PamDataBlock dataBlock;
		for (int i = 0; i < dataBlocks.size(); i++) {
			dataBlock = dataBlocks.get(i);
			if (dataBlock.getCanLog()) {
				dataBlock.addObserver(this);
			}
		}
	}

	@Override
	public void newData(PamObservable o, PamDataUnit dataUnit) {
		PamDataBlock dataBlock = (PamDataBlock) o;

		if (dataBlock.getLogging() != null && shouldLog(dataBlock, dataUnit)) {
			logData(dataBlock, dataUnit);
		}
	}

	@Override
	public void updateData(PamObservable o, PamDataUnit dataUnit) {
		/*
		 * Some detectors may have inadvertently set the updateCount in the data
		 * unit > 0 in which case the notification of new data will end up here. 
		 * Make the decision as to whether to update or write new based on existing 
		 * database index information
		 */
		if (dataUnit.getDatabaseIndex() == 0) {
			newData(o, dataUnit);
			return;
		}
//		System.out.println("Updating database record for " + dataUnit);
		PamDataBlock dataBlock = (PamDataBlock) o;
		if (dataBlock.getLogging() != null && shouldLog(dataBlock, dataUnit)) {
			reLogData(dataBlock, dataUnit);
		}
	}

	/**
	 * Hope this doesn't happen during PamguardViewer. 
	 * May sometimes happen during mixed operation. May need
	 * some fudges to make sure the right data are logged. 
	 * @param block
	 * @param unit
	 * @return
	 */
	private boolean logData(PamDataBlock block, PamDataUnit unit) {
		SQLLogging logger = block.getLogging();
		boolean ok = logger.logData(databaseControll.getConnection(), unit);
		if (ok) {
			dbWriteOKs++;
		}
		else {
			dbWriteErrors++;
		}
		return ok;
	}
	
	/**
	 * Hope this doesn't happen during PamguardViewer. 
	 * May sometimes happen during mixed operation. May need
	 * some fudges to make sure the right data are logged. 
	 * @param block
	 * @param unit
	 * @return
	 */
	private boolean reLogData(PamDataBlock block, PamDataUnit unit) {
		SQLLogging logger = block.getLogging();
		boolean ok = logger.reLogData(databaseControll.getConnection(), unit);
		if (ok) {
			dbWriteOKs++;
		}
		else {
			dbWriteErrors++;
		}
		return ok;
	}
	
	private boolean shouldLog(PamDataBlock pamDataBlock, PamDataUnit pamDataUnit) {
		switch (PamController.getInstance().getRunMode()) {
		case PamController.RUN_NORMAL:
		case PamController.RUN_NETWORKRECEIVER:
			return pamDataBlock.getShouldLog(pamDataUnit);
		case PamController.RUN_MIXEDMODE:
			return (pamDataBlock.getMixedDirection() == PamDataBlock.MIX_INTODATABASE && pamDataBlock.getShouldLog(pamDataUnit));
		case PamController.RUN_PAMVIEW:
			return pamDataBlock.getShouldLog(pamDataUnit);
		}
		return false;
	}
	
	class TimerAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			databaseControll.setWriteCount(dbWriteOKs, dbWriteErrors);
			dbWriteOKs = dbWriteErrors = 0;
		}
		
	}
	
	class ViewTimerAction implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {
			viewTimerAction();
		}
		
	}

	public LogSettings getLogSettings() {
		return logSettings;
	}

	public LogSettings getLogLastSettings() {
		return logLastSettings;
	}
}
