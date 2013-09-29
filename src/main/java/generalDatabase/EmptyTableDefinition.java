package generalDatabase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

/**
 * A totally empty table definition.
 * Not really empty, since it includes an Id.
 * @author Doug Gillespie
 *
 */
public class EmptyTableDefinition {

	protected String tableName;

	protected ArrayList<PamTableItem> pamTableItems;

	protected Connection checkedConnection;

	private PamTableItem indexItem;
	
	/**
	 * Cheat when retrieving database indexes
	 * by only retrieving them the first time and
	 * then just counting up.  
	 */
	private boolean useCheatIndexing = false;

	/**
	 * @param tableName
	 */
	public EmptyTableDefinition(String tableName) {
		super();
		this.tableName = tableName;
		pamTableItems = new ArrayList<PamTableItem>();
		pamTableItems.add(indexItem = new PamTableItem("Id", Types.INTEGER));
		indexItem.setPrimaryKey(true);
		indexItem.setCounter(true);
	}

	/**
	 * Function to remove leading and trailing blanks and to
	 * replace spaces in a database table or column name
	 * with the _ character.
	 * @param str database table or column name
	 * @return deblanked string.
	 */
	public static String deblankString(String str) {
		if (str == null) {
			return null;
		}
		String newString = str.trim();
		char c;
		for (int i = 0; i < newString.length(); i++) {
			c = newString.charAt(i);
			if (isValidCharacter(c) == false)  {
				newString = newString.replace(c, '_');
			}
		}
		return newString.replace(' ', '_');
	}
	
	/**
	 * Function to replace the _ character in a database table or column name
	 * with spaces to use with displays.
	 * @param str database table or column name
	 * @return reblanked string.
	 */
	public static String reblankString(String str) {
		return str.replace('_', ' ');
	}

	private static boolean isValidCharacter(char ch) {
		if (ch == ' ') return true;
		if (Character.isLetterOrDigit(ch)) return true;
		return false;
	}

	/**
	 * @return Deblanked database table name
	 */
	public String getTableName() {
		return deblankString(tableName);
		
	}
	
	public void setTableName(String tableName) {
		this.tableName = deblankString(tableName);
	}

	/**
	 * @param itemNumber Table item index (0 indexed)
	 * @return table item
	 * @see PamTableItem
	 */
	public PamTableItem getTableItem(int itemNumber) {
		if (pamTableItems == null || pamTableItems.size() == 0) return null;
		if (itemNumber < 0 || itemNumber >= pamTableItems.size()) return null;
		return pamTableItems.get(itemNumber);
	}
//	
//	/**
//	 * @param itemNumber Table item index (0 indexed)
//	 * @return table item
//	 * @see PamTableItem
//	 */
//	public PamTableItem getTableItem(PamTableItem item) {
//		if (pamTableItems == null || pamTableItems.size() == 0) return null;
//		if (item==null) return null;
//		
////		NICE WAY TO DO THIS BUT NEED TO CHECK SAME OBJECT REFERNCE
////		if (pamTableItems.indexOf(item)==-1) return null;
////		return pamTableItems.get(pamTableItems.indexOf(item));
//		
////		IN THE MEANTIME
//		PamTableItem itemToReturn=null;
//		for (PamTableItem pti:pamTableItems){
//			if (pti.getName()==item.getName()){
//				itemToReturn=pti;
//			}
//			
//		}
////		OR MORE COMPLETE CHECK
////		for (PamTableItem pti:pamTableItems){
////			if (pti.getName()==item.getName()&&pti.getSqlType()==item.getSqlType()&&
////					pti.getLength()==item.getLength()&& pti.isRequired()==item.isRequired()&&
////					pti.isCounter()==item.isCounter()&&pti.isPrimaryKey()==item.isPrimaryKey()){
////				itemToReturn=pti;
////			}
////			
////		}
//		return itemToReturn;
//	}

	/**
	 * 
	 * @return Count of table items (database columns)
	 * @see PamTableItem
	 */
	public int getTableItemCount() {
		if (pamTableItems == null) return 0;
		return pamTableItems.size();
	}

	/**
	 * Adds a new table item
	 * @param pamTableItem new table item object.
	 * @return The index of the new table item. Some columns are automatically
	 * added to every table, so the first column you create yourself may
	 * not be column 0. Since the number of columns added automatically may
	 * change in future versions of Pamguard, you should use this index when 
	 * adding data to the table using the SQLLogging.setColumnData function.
	 * <p>
	 * If an item already exists with the same name (after deblanking) the new
	 * table item will not be added to the list and the index
	 * of the existing table item will be returned. 
	 *
	 */
	public int addTableItem(PamTableItem pamTableItem) {
		if (findTableItem(pamTableItem) != null) {
			/*
			 * Need to issue a warning that an item already 
			 * exists in this table with the same name. 
			 */
			return pamTableItems.indexOf(pamTableItem);
		}
		else {
			pamTableItems.add(pamTableItem);
			return pamTableItems.indexOf(pamTableItem);
		}
	}

	/**
	 * Removes a table item from the table definition.
	 * @param itemIndex index of the table item.
	 * @return the item removed. 
	 */
	public PamTableItem removeTableItem(int itemIndex) {
		return pamTableItems.remove(itemIndex);
	}

	/**
	 * Removes a table item from the table definition.
	 * @param item reference to the item to be removed.
	 * @return true if successful, false if the item could not be found.
	 */
	public boolean removeTableItem(PamTableItem item) {
		return pamTableItems.remove(item);
	}

	/**
	 * Searches the existing table defnition to see if a table item
	 * already exists with a given name. Returns the reference to the 
	 * PamTableItem if it exists, null otherwise. 
	 * @param itemName
	 * @return Pamguard table item with given name
	 */
	public PamTableItem findTableItem(String itemName) {
		for (int i = 0; i < pamTableItems.size(); i++) {
			if (itemName.compareToIgnoreCase(pamTableItems.get(i).getName()) == 0) {
				return pamTableItems.get(i);
			}
		}
		return null;
	}

	/**
	 * Searches the table definition to see if a TableItem already exists
	 * with the same name as tableITem. Returns the reference to the 
	 * existing tableItem.
	 * @param tableItem
	 * @return reference to the database item, or null if no item found
	 */
	public PamTableItem findTableItem(PamTableItem tableItem) {
		return findTableItem(tableItem.getName());
	}

	/**
	 * gets an sql insert string for the table that selects all fields.
	 * Note that some databases don't support the " around a column name, so this
	 * has been omitted, making it impossible to use fields with spaces. 
	 * <p>
	 * If skipCounters is true, then counters are not included in the statement.
	 * Generally, this is the sensible ting to do.
	 *  
	 * @return SQL Insert string
	 */
	public String getSQLInsertString(SQLTypes sqlTypes) {
		String str = "INSERT INTO " + getTableName() + " (";
		String valsBit = ") VALUES (";
		int nItems = getTableItemCount();
		PamTableItem tableItem;
		boolean first = true;
		for (int i = 0; i < nItems; i++) {
			tableItem = getTableItem(i);
			if (tableItem.isCounter()) {
				continue;
			}
			if (tableItem.isPrimaryKey()) {
				continue;
			}
			if (first == false) {
				str+=", ";
				valsBit+=", ";
			}
			else {
				first = false;
			}
			str += sqlTypes.formatColumnName(tableItem.getName());
			valsBit += "?";
		}
		valsBit += ")";
		str += valsBit;
		return str;
	}

	/**
	 * Get a very basic select string which queries for all items in the 
	 * table, no ordering or selection
	 * @return an SQL string. 
	 */
	public String getSQLSelectString(SQLTypes sqlTypes) {
		return getBasicSelectString(sqlTypes);
	}
	
	public final String getBasicSelectString(SQLTypes sqlTypes) {
		PamTableItem tableItem;

		String sqlString = "SELECT ";
		for (int i = 0; i < getTableItemCount(); i++) {
//			if (getTableItem(i).isCounter()) continue;
			sqlString +=  sqlTypes.formatColumnName(getTableItem(i).getName());
			if (i < getTableItemCount()-1) {
				sqlString += ", ";
			}
		}
		sqlString += " FROM " + getTableName();
		return sqlString;
	}

	/**
	 * Searches the Pamguard system for a table with a particular name.
	 * Table names used in the seach are deblanked. 
	 * @param tableName
	 * @return reference to the database deinition if it exists, or null 
	 */
	static PamTableDefinition findTableDefinition(String tableName) {
		String searchName = EmptyTableDefinition.deblankString(tableName);
		SQLLogging log = SQLLogging.findLogger(searchName);
		if (log == null) return null;
		return log.getTableDefinition();
	}

	/**
	 * Move the data out of a result set into the holding places in the table items. 
	 * @param resultSet result set to unpack
	 * @return true if OK, false if any exceptions thrown 
	 */
	public boolean unpackResultSet(ResultSet resultSet) {
		PamTableItem anItem;
		for (int i = 0; i < getTableItemCount(); i++) {
			anItem = getTableItem(i);
			try {
				anItem.setValue(resultSet.getObject(i+1));
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	public PamTableItem getIndexItem() {
		return indexItem;
	}

	/**
	 * Cheat at database indexing. If cheat indexing is used, then
	 * the index will only be read back from the database table when 
	 * the first record is written. After that, the PAMCursor working 
	 * with this table will keep a count of the index and increment it by one
	 * each time a record is written. 
	 * <p>
	 * This could go badly wrong if more than one program or different bits of the 
	 * same programme are writing to the same table. However, in many cases this 
	 * will not be important since the indexes are not actually used in real time
	 * operation. i.e. it's safe to do this for things like GPS and whistle data. 
	 * However, it would not be recommended for things where it's more likely that 
	 * multiple users might write to the same database or where indexing is really critical
	 * such as in logger forms (the index is used to update data) or Click offline 
	 * events (the index is used for cross referencing from the clicks).
	 * 
	 * @return the useCheatIndexing
	 */
	public boolean isUseCheatIndexing() {
		return useCheatIndexing;
	}

	/**
	 * Cheat at database indexing. If cheat indexing is used, then
	 * the index will only be read back from the database table when 
	 * the first record is written. After that, the PAMCursor working 
	 * with this table will keep a count of the index and increment it by one
	 * each time a record is written. 
	 * <p>
	 * This could go badly wrong if more than one program or different bits of the 
	 * same programme are writing to the same table. However, in many cases this 
	 * will not be important since the indexes are not actually used in real time
	 * operation. i.e. it's safe to do this for things like GPS and whistle data. 
	 * However, it would not be recommended for things where it's more likely that 
	 * multiple users might write to the same database or where indexing is really critical
	 * such as in logger forms (the index is used to update data) or Click offline 
	 * events (the index is used for cross referencing from the clicks).
	 * @param useCheatIndexing the useCheatIndexing to set
	 */
	public void setUseCheatIndexing(boolean useCheatIndexing) {
		this.useCheatIndexing = useCheatIndexing;
	}
	
}
