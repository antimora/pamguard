package generalDatabase.pamCursor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import generalDatabase.DBControlUnit;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLTypes;

/**
 * Class to generate Cursors for databases which can directly support
 * scrollable cursors and ones which cannot.
 * <p>
 * This will directly implement a lot of the functions of the 
 * ResultSet interface, but not all of them - life is too short. It will 
 * however use the same names and behave in the same way. 
 * 
 * @see NonScrollablePamCursor
 * @see ScrollablePamCursor 
 * @author Doug Gillespie
 *
 */
abstract public class PamCursor {

	private EmptyTableDefinition tableDefinition;

	private Connection currentConnection;

	private PreparedStatement preparedInsertStatement;
	
	private PreparedStatement preparedUpdateStatement;
	
	/**
	 * Internal index to use when writing data for certain tables
	 * in order to save the time taken to read indexes back from
	 * the database. Not suitable for all tables. See longer comment
	 * in EmptyTableDefinition
	 * @see EmptyTableDefinition 
	 */
	private int cheatsIndex = 0;

	/**
	 * Remember the connection - will have to reset the cheats index if 
	 * the connection changes. 
	 */
	private Connection cheatsConnection;

	private SQLTypes sqlTypes;

	public PamCursor(EmptyTableDefinition tableDefinition) {
		super();
		this.tableDefinition = tableDefinition;
	}

	@Override
	protected void finalize() throws Throwable {
		closeScrollableCursor();
		super.finalize();
	}

	public EmptyTableDefinition getTableDefinition() {
		return tableDefinition;
	}

//	public void setTableDefinition(EmptyTableDefinition tableDefinition) {
//		this.tableDefinition = tableDefinition;
//	}

	public Connection getCurrentConnection() {
		return currentConnection;
	}

	public void setCurrentConnection(Connection currentConnection) {
		this.currentConnection = currentConnection;
	}

	/**
	 * Generate a standard SQL select string for everything in the 
	 * table. <p>
	 * No ordering of selecting at this stage.  
	 * @param includeKeys include items which are primary keys (i.e. the index)
	 * @param includeCounters include items which are counters
	 * @param orderById add a standard ORDER BY Id at the end of the string
	 * @return SQL string
	 */
	public String getSelectString(SQLTypes sqlTypes, boolean includeKeys, boolean includeCounters, boolean orderById) {
		String str = "SELECT ";
		int nItems = tableDefinition.getTableItemCount();
		PamTableItem tableItem;
		boolean first = true;
		for (int i = 0; i < nItems; i++) {
			tableItem = tableDefinition.getTableItem(i);
			if (tableItem.isCounter() && !includeCounters) {
				continue;
			}
			if (tableItem.isPrimaryKey() && !includeKeys) {
				continue;
			}
			if (first == false) {
				str+=", ";
			}
			else {
				first = false;
			}
			str += sqlTypes.formatColumnName(tableItem.getName());
		}
		str += " FROM " + tableDefinition.getTableName();
		if (orderById) {
			str += " ORDER BY Id";
		}
		return str;
	}

	public String getUpdateString(SQLTypes sqlTypes) {
		String str = "UPDATE " + tableDefinition.getTableName() + " SET ";
		int nItems = tableDefinition.getTableItemCount();
		PamTableItem tableItem;
		boolean first = true;
		for (int i = 0; i < nItems; i++) {
			tableItem = tableDefinition.getTableItem(i);
			if (tableItem.isCounter()) {
				continue;
			}
			if (tableItem.isPrimaryKey()) {
				continue;
			}
			if (first == false) {
				str+=", ";
			}
			else {
				first = false;
			}
			str += sqlTypes.formatColumnName(tableItem.getName()) + " = ?";
		}
		str += " WHERE Id = ?";
		return str;
	}

	public String getInsertString(SQLTypes sqlTypes) {
		return tableDefinition.getSQLInsertString(sqlTypes);
	}

	/**
	 * Generate an SQL select string with an optional clause which 
	 * may include WHERE and ORDER 
	 * @param includeKeys include items which are primary keys (i.e. the index)
	 * @param includeCounters include items which are counters
	 * @param clause WHERE ... and ORDER BY clause. 
	 * @return SQL string
	 */
	public String getSelectString(SQLTypes sqlTypes, boolean includeKeys, boolean includeCounters, String clause) {
		String str = getSelectString(sqlTypes, includeKeys, includeCounters, false);
		str += " " + clause;
		return str;
	}

	/**
	 * Open a cursor for inserting data using a non-scrollable cursor. 
	 * <p>These are generally faster than scrollable cursors, so this is
	 * in the abstract super class PamCursor. Should for any reason someone
	 * convince me that it's better done in a different way for some 
	 * databases, this function can be easily overridden. 
	 * <p>
	 * One thing which is more database specific is the way in which 
	 * index material are retrieved for different database types. 
	 * @param connection Database connection
	 * @param includeCounters include counters in query
	 * @return true if cursor created sucessfully. 
	 */
	public boolean openInsertCursor(Connection connection) {
		if (preparedInsertStatement != null &&
				connection == currentConnection) {
			return true;
		}
		if (connection == null) {
			return false;
		}
		/*
		 * Otherwise, make a new prepared insert statement. 
		 */
//		sqlTypes = DBControlUnit.findDatabaseControl().getDatabaseSystem().getSqlTypes();
		
		closeCursors();
		setCurrentConnection(connection);
		String insertString = getInsertString(getSQLTypes(connection));
		try {
			preparedInsertStatement = connection.prepareStatement(insertString,ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
//			preparedInsertStatement.setMaxRows(1);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Move data from the table definition down to the cursor
	 * @param includeCounters counters are included in the query
	 * @throws SQLException
	 */
	public void moveDataToCursor(boolean includeCounters) throws SQLException {
		int dbCol = 0;
		EmptyTableDefinition tableDef = getTableDefinition();
		PamTableItem tableItem;
		int nCol = tableDef.getTableItemCount();
		for (int i = 0; i < nCol; i++) {
			tableItem = tableDef.getTableItem(i);
			if (tableItem.isCounter() || tableItem.isPrimaryKey()) {
				if (includeCounters) {
					updateObject(++dbCol, tableItem.getValue());
				}
			}
			else {
				updateObject(++dbCol, tableItem.getValue());
			}
		}
	}

//	/**
//	 * Clears 
//	 */
//	public void clearCursor() {
//		EmptyTableDefinition tableDef = getTableDefinition();
//		PamTableItem tableItem;
//		int nCol = tableDef.getTableItemCount();
//		for (int i = 0; i < nCol; i++) {
//			tableItem = tableDef.getTableItem(i);
//			tableItem.setValue(null);
//		}		
//	}
	/**
	 * Move data from the cursor to the table definition
	 * @param includeCounters include counter data
	 * @throws SQLException
	 */
	public void moveDataToTableDef(boolean includeCounters) throws SQLException {
		int dbCol = 0;
		EmptyTableDefinition tableDef = getTableDefinition();
		PamTableItem tableItem;
		int nCol = tableDef.getTableItemCount();
		for (int i = 0; i < nCol; i++) {
			tableItem = tableDef.getTableItem(i);
			if (tableItem.isCounter() || tableItem.isPrimaryKey()) {
				if (includeCounters) {
					tableItem.setValue(getObject(++dbCol));
				}
				else {
					tableItem.setValue(null);
				}
			}
			else {
				tableItem.setValue(getObject(++dbCol));
			}
		}		
	}
	
	private Connection typesConnection;
	protected SQLTypes getSQLTypes(Connection con) {
		if (sqlTypes == null || con != typesConnection) {
			DBControlUnit dbcu = DBControlUnit.findDatabaseControl();
			if (dbcu != null && dbcu.getDatabaseSystem() != null) {
				sqlTypes = dbcu.getDatabaseSystem().getSqlTypes();
			}
		}
		return sqlTypes;
	}
	
	/**
	 * Do an immediate insert of data which should already have 
	 * been put into the data objects of the table definition. 
	 * <p>Primarily used in real time data collection, called from
	 * SQLLogging.
	 * @param connection Database connection
	 * @return the new database Index number
	 */
	public int immediateInsert(Connection connection) {
		// check the cursor is on the right connection.
		if (openInsertCursor(connection) == false) {
			return -1;
		}
		// transfer the data from the table definition to the result set. 
		EmptyTableDefinition tableDef = getTableDefinition();
		PamTableItem tableItem = null;

		int dbIndex = 0;
		int i = 0;

		Object dataObject;
		int iCol = 0;
		try {
			/**
			 * Update the referencing in the bindings to the database statement.
			 * NB. Statement bindings are 1 indexed. tableItems are 0 indexed
			 */
			for (i = 0; i < tableDef.getTableItemCount(); i++) {
				tableItem = tableDef.getTableItem(i);
				if (tableItem.isCounter()) {
					continue;
//					preparedInsertStatement.set
					//					preparedInsertStatement.setNull(iCol+1, tableItem.getSqlType());
					//					preparedInsertStatement.setObject(iCol + 1, new Integer(0), tableItem.getSqlType());
				}
				else {
					dataObject = tableItem.getValue();
					if (dataObject == null) {
						preparedInsertStatement.setNull(iCol+1, tableItem.getSqlType());
//						preparedInsertStatement.setObject(iCol+1, null);
					}
					else {
						preparedInsertStatement.setObject(iCol + 1, tableItem.getValue(), tableItem
								.getSqlType());
					}
				}
				iCol++;
			}
			preparedInsertStatement.executeUpdate();
		}
		catch (SQLException e) {
			System.out.println("Database error logging data for " + tableDef.getTableName());
			System.out.println("Insert string: " + getInsertString(getSQLTypes(connection)));
			System.out.println(String.format("item %d, SQL data type = %d, data = %s", i, tableItem.getSqlType(), tableItem.getValue()));
			e.printStackTrace();
			return -1;
		}
		if (tableDef.isUseCheatIndexing()) {
			return getCheatsIndex(connection, preparedInsertStatement);
		}
		else {
			return getLastDatabaseIndex(connection, preparedInsertStatement);
		}
	}
	
	private int getCheatsIndex(Connection connection, PreparedStatement preparedInsertStatement2) {
		if (cheatsConnection != connection || cheatsIndex == 0) {
			cheatsIndex = getLastDatabaseIndex(connection, preparedInsertStatement);
			cheatsConnection = connection;
			return cheatsIndex;
		}
		else {
			return ++cheatsIndex;
		}
	}

	/**
	 * Prepare a prepared update statement for the cursor which can 
	 * be reused so long as the connection doesn't change. 
	 * @param connection database connection
	 * @return true if all seems OK
	 */
	private boolean prepareUpdateStatement(Connection connection) {
		if (preparedUpdateStatement != null && 
				connection == currentConnection) {
			return true;
		}
		if (preparedUpdateStatement != null) {
			try {
				preparedUpdateStatement.close();
			} catch (SQLException e) {
			}
		}
		String updateString = getUpdateString(getSQLTypes(connection));
		try {
			preparedUpdateStatement = connection.prepareStatement(updateString);
		} catch (SQLException e) {
			System.out.println("Unable to prepare update statement " + updateString);
			e.printStackTrace();
			return false;
		}
		return true;
		
	}
	
	/**
	 * Do an immediate update of a single item which already has
	 * it's data in the table definition fields. 
	 * <p>Use a single parameterised cursor statement. 
	 * @param connection
	 * @return true if successful. 
	 */
	public boolean immediateUpdate(Connection connection) {
		// check the cursor is on the right connection.
		if (prepareUpdateStatement(connection) == false) {
			return false;
		}
		// transfer the data from the table definition to the result set. 
		EmptyTableDefinition tableDef = getTableDefinition();
		PamTableItem tableItem;

		try {
			/**
			 * Update the referencing in the bindings to the database statement.
			 * NB. Statement bindings are 1 indexed. tableItems are 0 indexed
			 */
			int iCol = 0;
			for (int i = 0; i < tableDef.getTableItemCount(); i++) {
				tableItem = tableDef.getTableItem(i);
				if (tableItem.isCounter()) {
					continue;
				}
				preparedUpdateStatement.setObject(iCol + 1, tableItem.getValue(), tableItem
						.getSqlType());
				iCol++;
			}
			preparedUpdateStatement.setObject(iCol + 1, tableDef.getIndexItem().getValue());
			preparedUpdateStatement.executeUpdate();
		}
		catch (SQLException e) {
			System.out.println("Database error logging data for " + tableDef.getTableName());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Gets called from ImmediateInsert to get the last database index
	 * using a couple of alternate methods. 
	 * @param connection
	 * @param preparedInsertStatement
	 * @return
	 */
	abstract int getLastDatabaseIndex(Connection connection,
			PreparedStatement preparedInsertStatement);

	private void closeCursors() {
		if (preparedInsertStatement != null) {
			try {
				preparedInsertStatement.close();
				preparedInsertStatement = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		closeScrollableCursor();
	}
	

	public ResultSet openReadOnlyCursor(Connection connection, String clause) {
		ResultSet readOnlyResult;
		SQLTypes sqlTypes = getSQLTypes(connection);
		if (sqlTypes == null) {
			sqlTypes = new SQLTypes();
		}
		String sqlString = this.getSelectString(sqlTypes, true, true, clause);
		try {
			/*
			 * TYPE_SCROLL_INSENSITIVE makes it go incredibly slowly. 
			 * Get orders of magnitude increase in speed if it's
			 * set to TYPE_FORWARD_ONLY
			 */
			Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY);
			readOnlyResult = stmt.executeQuery(sqlString);
		}
		catch (SQLException e) {
			System.out.println("Error in PamCursor.openReadOnlyCursor() " + tableDefinition.getTableName());
			e.printStackTrace();
			return null;
		}
		return readOnlyResult;
	}
	
	

	/**
	 * Open a scrollable cursor
	 * @param connection database connection
	 * @param includeKeys include keys
	 * @param includeCounters inlcude counters
	 * @param clause selection and ordering clause. 
	 * @return true if successfully opened. 
	 */
	public abstract boolean openScrollableCursor(Connection connection, 
			boolean includeKeys, boolean includeCounters, String clause);	

	/**
	 * Close the scrollable cursor. 
	 * @return true if no exception.
	 */
	public abstract boolean closeScrollableCursor();

	/**
	 * Push everything down onto the database
	 * @return true if no errors or exceptions.
	 */
	public abstract boolean updateDatabase();

	/**
	 * Go t0 an absolute row number
	 * <p>
	 * Note that row numbers are 1 indexed. 
	 * @param row Row number
	 * @return true if the row is accessed. 
	 */
	public abstract boolean absolute(int row);

	public abstract void afterLast();

	public abstract void beforeFirst();

	public abstract void close();

	public abstract void deleteRow();

	public abstract int findColumn(String columnLabel);

	public abstract boolean first();

	/**
	 * Retrieves the current row number. The first row is number 1, the second number 2, and so on.
	 * @return the current Row number or 0 if there is no current row 
	 * @throws SQLException if a database error occurs
	 */
	public abstract int getRow() throws SQLException;

	public abstract Object getObject(int columnIndex) ;

	public abstract boolean getBoolean(int columnIndex) ;

	public abstract byte getByte(int columnIndex);

	public abstract Date getDate(int columnIndex, Calendar cal) ;

	public abstract Date getDate(int columnIndex);

	public abstract double getDouble(int columnIndex);

	public abstract float getFloat(int columnIndex);

	public abstract int getInt(int columnIndex);

	public abstract long getLong(int columnIndex);

	public abstract RowId getRowId(int columnIndex) ;

	public abstract String getString(int columnIndex) ;

	public abstract Time getTime(int columnIndex) ;

	public abstract Timestamp getTimestamp(int columnIndex) ;

	public abstract boolean isAfterLast() ;

	public abstract boolean isBeforeFirst() ;

	public abstract boolean isClosed() ;

	public abstract boolean isFirst() ;

	public abstract boolean isLast() ;

	public abstract boolean last() ;

	public abstract void moveToCurrentRow() ;

	public abstract void moveToInsertRow() ;

	/**
	 * Insert row statement
	 * @param getIndex set true if you want to return the new database index. 
	 * Otherwise 0 will be returned. 
	 * @return return the database iD for the newly inserted item, -1 for an
	 * error or 0 if no index requested.  
	 */
	public abstract int insertRow(boolean getIndex);

	public abstract boolean next() ;

	public abstract boolean previous() ;

	public abstract void refreshRow() ;

	public abstract boolean rowDeleted() ;

	public abstract boolean rowInserted() ;

	public abstract boolean rowUpdated() ;

	public abstract void updateObject(int columnIndex, Object x) throws SQLException;

	public abstract void updateBoolean(int columnIndex, boolean x) throws SQLException;

	public abstract void updateByte(int columnIndex, byte x) throws SQLException;

	public abstract void updateInt(int columnIndex, int x) throws SQLException;

	public abstract void updateLong(int columnIndex, long x) throws SQLException;

	public abstract void updateNull(int columnIndex) throws SQLException;

	public abstract void updateRow() throws SQLException;

	public abstract void updateString(int columnIndex, String x) throws SQLException;

	public abstract void updateShort(int columnIndex, short x) throws SQLException;

	public abstract void updateTimestamp(int columnIndex, Timestamp x) throws SQLException;

}
