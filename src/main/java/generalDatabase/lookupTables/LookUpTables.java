package generalDatabase.lookupTables;

import java.awt.Color;
import java.awt.Window;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ListIterator;

import generalDatabase.DBControlUnit;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.pamCursor.PamCursor;
import generalDatabase.pamCursor.PamCursorManager;

/**
 * Singleton class for managing a common lookup table for 
 * many PAMGUARD modules (following from Logger format)
 * @author Doug Gillespie
 *
 */
public class LookUpTables {

	private static LookUpTables singleInstance;

	private EmptyTableDefinition lutTableDef;
	
	/**
	 * Maximum length of topic text for a lookup collection
	 */
	static public final int TOPIC_LENGTH = 50;
	/**
	 * Maximum length of a lookup code
	 */
	static public final int CODE_LENGTH = 12;
	/**
	 * Maximum length of a lookup item text
	 */
	static public final int TEXT_LENGTH = 50;

	private PamTableItem topicItem, codeItem, textItem, 
	selectableItem, borderColourItem, fillcolourItem, orderItem, symbolItem;

	private LookUpTables() {
		lutTableDef = new EmptyTableDefinition("Lookup");
		lutTableDef.addTableItem(topicItem = new PamTableItem("Topic", Types.CHAR, TOPIC_LENGTH));
		lutTableDef.addTableItem(orderItem = new PamTableItem("DisplayOrder", Types.INTEGER));
		lutTableDef.addTableItem(codeItem = new PamTableItem("Code", Types.CHAR, CODE_LENGTH));
		lutTableDef.addTableItem(textItem = new PamTableItem("ItemText", Types.CHAR, TEXT_LENGTH));
		selectableItem = new PamTableItem("isSelectable", Types.BIT);
		lutTableDef.addTableItem(selectableItem);
		lutTableDef.addTableItem(fillcolourItem = new PamTableItem("FillColour", Types.CHAR, 20));
		lutTableDef.addTableItem(borderColourItem = new PamTableItem("BorderColour", Types.CHAR, 20));
		lutTableDef.addTableItem(symbolItem = new PamTableItem("Symbol", Types.CHAR, 2));

		checkTable();
	}

	/**
	 * Access the LookUpTables class
	 * @return reference to a single instance of the Look up table manager. 
	 */
	synchronized public static LookUpTables getLookUpTables() {
		if (singleInstance == null) {
			singleInstance = new LookUpTables();
		}
		return singleInstance;
	}


	private Connection checkedTableConnection;
	/**
	 * Check the database module is present and that 
	 * the lookup table exists. 
	 * @return true if the table exists and is correctly formatted with all the 
	 * right columns. 
	 */
	public boolean checkTable() {
		DBControlUnit dbControlUnit = DBControlUnit.findDatabaseControl();
		if (dbControlUnit == null) {
			return false;
		}
		Connection con = DBControlUnit.findConnection();
		if (con == null) {
			return false;
		}
		if (con == checkedTableConnection) {
			return true;
		}
		checkedTableConnection = null;
		if (dbControlUnit.getDbProcess().checkTable(lutTableDef)) {
			checkedTableConnection = con;
			return true;
		}
		return false;
	}

	public LookupList createLookupList(PamCursor resultSet, String topic) {
		LookupList lookupList = new LookupList(topic);
		LookupItem lutItem;
		try {
			while (resultSet.next()) {
				for (int i = 0; i < lutTableDef.getTableItemCount(); i++) {
					lutTableDef.getTableItem(i).setValue(resultSet.getObject(i+1));
				}
				lookupList.addItem(lutItem = new LookupItem(lutTableDef.getIndexItem().getIntegerValue(),
						resultSet.getRow(),
						topic, orderItem.getIntegerValue(), codeItem.getDeblankedStringValue(),
						textItem.getDeblankedStringValue(), selectableItem.getBooleanValue(),
						getColour(fillcolourItem.getDeblankedStringValue()),
						getColour(borderColourItem.getDeblankedStringValue()),
						symbolItem.getDeblankedStringValue()));
				//				System.out.println("createLookupList New Item " + lutItem);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return lookupList;
	}

	/**
	 * write back to database
	 * @param newList
	 * @param resultSet
	 * @return
	 */
	private boolean reWriteList(LookupList newList, PamCursor resultSet) {
		//		newList.sortItemsById();
		LookupItem item;
		//		Connection con = DBControlUnit.findConnection();
		ListIterator<LookupItem> lutList = newList.getList().listIterator();
		while (lutList.hasNext()) {
			item = lutList.next();
			//			setTableData(item);
			if (item.getResultSetRow() == 0) {
				resultSet.moveToInsertRow();
				setTableData(resultSet, item);
				//					resultSet.updateRow();
				resultSet.insertRow(false);
			}
			else {
				try {
					boolean b = resultSet.absolute(item.getResultSetRow());
					if (b) {
						setTableData(resultSet, item);
						resultSet.updateRow();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		// now check for deleted items. 
		if (newList.getDeletedItems() != null) {
			lutList = newList.getDeletedItems().listIterator();
			while (lutList.hasNext()) {
				item = lutList.next();
				if (item.getResultSetRow() > 0 && item.getDatabaseId() > 0) {
					resultSet.absolute(item.getResultSetRow());
					resultSet.deleteRow();
				}
			}
		}
		resultSet.updateDatabase();


		return true;
	}


	/**
	 * Set data in the result set. 
	 * It's slightly easier for this to be done by copying the data back into 
	 * the table items since they are accessible by named objects, these 
	 * then get copied in the correct order into the PamCursor.<p>
	 * they could be copied straight into the cursor, but this would have to
	 * be done in the exact right order. 
	 * @param resultSet Pamguard cursor object
	 * @param lookupItem lookup item
	 */
	private void setTableData(PamCursor resultSet, LookupItem lookupItem) {
		if (lookupItem.getDatabaseId() <= 0) {
			lutTableDef.getIndexItem().setValue(null);
		}
		else {
			lutTableDef.getIndexItem().setValue(lookupItem.getDatabaseId());
		}
		topicItem.setValue(lookupItem.getTopic());
		codeItem.setValue(lookupItem.getCode());
		textItem.setValue(lookupItem.getText());
		selectableItem.setValue(lookupItem.isSelectable() ? 1 : 0);
		borderColourItem.setValue(getColourString(lookupItem.getBorderColour()));
		fillcolourItem.setValue(getColourString(lookupItem.getFillColour()));
		orderItem.setValue(lookupItem.getOrder());
		symbolItem.setValue(lookupItem.getSymbolType());
		try {
			for (int i = 0; i < lutTableDef.getTableItemCount(); i++) {
				resultSet.updateObject(i+1, lutTableDef.getTableItem(i).getValue());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Interpret colour strings from the lookup table
	 * @param deblankedStringValue
	 * @return a color, or null if the string cannot be interpreted.
	 */
	private Color getColour(String colString) {
		if (colString == null) {
			return null;
		}
		if (colString.equalsIgnoreCase("red")) {
			return Color.red;
		}else if (colString.equalsIgnoreCase("green")) {
			return Color.green;
		}else if (colString.equalsIgnoreCase("blue")) {
			return Color.blue;
		}else if (colString.equalsIgnoreCase("white")) {
			return Color.white;
		}else if (colString.equalsIgnoreCase("black")) {
			return Color.black;
		}else if (colString.equalsIgnoreCase("gray")) {
			return Color.gray;
		}else if (colString.equalsIgnoreCase("darkgray")) {
			return Color.darkGray;
		}else if (colString.equalsIgnoreCase("orange")) {
			return Color.orange;
		}else if (colString.equalsIgnoreCase("cyan")) {
			return Color.cyan;
		}else if (colString.equalsIgnoreCase("magenta")) {
			return Color.magenta;
		}else if (colString.equalsIgnoreCase("lightgray")) {
			return Color.lightGray;
		}else if (colString.equalsIgnoreCase("pink")) {
			return Color.pink;
		}else if (colString.equalsIgnoreCase("yellow")) {
			return Color.yellow;
		}
		else if (colString.substring(0, 3).equalsIgnoreCase("RGB")) {
			int[] rgb = new int[3];
			int[] divPos = new int[4];
			divPos[0] = colString.indexOf('(');
			divPos[1] = colString.indexOf(',');
			if (divPos[1] < 0) {
				return null;
			}
			divPos[2] = colString.indexOf( ',', divPos[1]+1);
			if (divPos[2] < 0) {
				return null;
			}
			divPos[3] = colString.indexOf(')');
			if (divPos[3] < 0) {
				return null;
			}
			try {
				for (int i = 0; i < 3; i++) {
					rgb[i] = Integer.valueOf(colString.substring(divPos[i]+1, divPos[i+1]));
				}
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
				return null;
			}
			return new Color(rgb[0], rgb[1], rgb[2]);
		}
		return null;
	}

	/**
	 * Convert a colour into a string that can be written to the table
	 * @param colour Colour
	 * @return String representation in the format RGB(%d,%d,%d).
	 */
	private String getColourString(Color colour) {
		if (colour == null) {
			return null;
		}
		return String.format("RGB(%d,%d,%d)", colour.getRed(), colour.getGreen(), colour.getBlue());
	}

	/**
	 * Query all LUT items with the given topic name. 
	 * display these in a table / list (which might be empty)
	 * and provide facilities for the user to add to and remove 
	 * items from this list
	 * @param window 
	 * @param topic LUT topic
	 * @return a new list, or null if no new list created.
	 */
	public LookupList editLookupTopic(Window window, String topic) {

		PamCursor cursor = createPamCursor(topic);
		if (cursor == null) {
			System.out.println("Unable to access database lookup table");
			return null;
		}
		LookupList lookupList = createLookupList(cursor, topic);
		//		lookupList.sortItemsByOrder();
		LookupList newList = LookupEditDialog.showDialog(window, lookupList);
		if (newList != null) {
			reWriteList(newList, cursor);
			return newList;
		}
		return null;
	}

	public LookupList getLookupList(String topic) {
		PamCursor cursor = createPamCursor(topic); 
		if (cursor == null) {
			System.out.println("Unable to access database lookup table for topic " + topic);
			return null;
		}
		LookupList lookupList = createLookupList(cursor, topic);
		cursor.closeScrollableCursor();
		return lookupList;
	}

	private PamCursor createPamCursor(String topic) {
		PamCursor c = PamCursorManager.createCursor(lutTableDef);
		Connection con = DBControlUnit.findConnection();
		c.openScrollableCursor(con, true, true, String.format("WHERE Topic = '%s' ORDER BY DisplayOrder", topic));
		return c;
	}

	}
