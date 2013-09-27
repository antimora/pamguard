package generalDatabase;

import java.sql.Types;

//http://java.sun.com/j2se/1.4.2/docs/api/java/sql/Types.html
	
/**
 * Defines a single item (column) for a Pamguard database table
 * These are listed in PamTableDefinition for each table.
 * 
 * @author Doug Gillespie
 * @see generalDatabase.PamTableDefinition
 * 
 */
public class PamTableItem {

	/**
	 * name of the database column
	 */
	private String name;
	
	/**
	 * the SQL type (as defined in java.sql.Types) for the database column
	 */
	private int sqlType;
	
	/**
	 * lengh of character type fields.
	 */
	private int length;
	
	/**
	 * required field (cannot be null)
	 */
	private boolean required;
	
	/**
	 * Is a primary key
	 */
	private boolean primaryKey = false;
	
	/** 
	 * IS an autoincrementing counter
	 * can only be used if sqlType is integer
	 */
	private boolean isCounter = false;
	
	/**
	 * Contains the last value logged to or read from the database. 
	 */
	private Object value;
	
	/*
	 * Reference to another PamTableItem in a different
	 * table. This must be of the same sqlType as this
	 * PamTableItem. When this PamTableItem is written 
	 * to the database, it's value will automatically
	 * be taken as the last value referenced by the
	 * crossREferenceItm.
	 */
	private PamTableItem crossReferenceItem;

	public PamTableItem(String name, int sqlType) {
		super();
		// TODO Auto-generated constructor stub
		this.name = name;
		this.sqlType = sqlType;
		this.length = 0;
		this.required = false;
	}
	
	public PamTableItem(String name, int sqlType, int length) {
		super();
		// TODO Auto-generated constructor stub
		this.name = name;
		this.sqlType = sqlType;
		this.length = length;
		this.required = false;
	}

	public PamTableItem(String name, int sqlType, int length, boolean required) {
		super();
		// TODO Auto-generated constructor stub
		this.name = name;
		this.sqlType = sqlType;
		this.length = length;
		this.required = required;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getName() {
		return EmptyTableDefinition.deblankString(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public int getSqlType() {
		return sqlType;
	}

	public void setSqlType(int sqlType) {
		this.sqlType = sqlType;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isCounter() {
		return isCounter;
	}

	public void setCounter(boolean isCounter) {
		this.isCounter = isCounter;
	}

	/**
	 * Gets the most recently used value written to or read
	 * from the database.
	 * @return data read from or written to the database column
	 */
	public Object getValue() {
		return value;
	}
	
	public String getDeblankedStringValue() {
		if (sqlType != Types.CHAR || value == null) {
			return null;
		}
		return ((String) value).trim();
	}

	/**
	 * Sets the value of data to be written to the
	 * database column.
	 * @param value
	 */
	public void setValue(Object value) {
		/*
		 * It doesn't seem to like empty strings - causes a total SQL crash !!!
		 * also need to check double types for NaN - which should be changed to null. 
		 */
		if (value == null) {
			this.value = null;
			return;
		}
		switch (sqlType) {
		case Types.CHAR:
			if (value != null) {
				String charObj = (String) value;
				if (charObj.length() == 0) {
//					this.value = new String(" ");
					this.value = null;
					return;
				}
				if (charObj.length() > this.length) {
					// string is too long, so truncate it.
					value = charObj.substring(0, this.length-1);
				}
			}
			break;
		case Types.DOUBLE:
			Double d = (Double) value;
			if (d == Double.NaN || d == Double.NEGATIVE_INFINITY || d == Double.POSITIVE_INFINITY) {
				this.value = null;
				return;
			}
			break;
		}
		this.value = value;
	}

	private String xRefTable, xRefColumn;
	/**
	 * Gets the cross reference item. If the item reference is null
	 * then the function searches for it based on previously set
	 * table and column names. 
	 * @return Table item the current item is cross referenced to
	 */
	public PamTableItem getCrossReferenceItem() {
		if (crossReferenceItem != null) return crossReferenceItem;
		else if (xRefTable != null && xRefColumn != null) {
			return setCrossReferenceItem(xRefTable, xRefColumn);
		}
		else return null;
	}

	/**
	 * Sets the cross reference item. Data from the crossREferenceItem
	 * will automatically be used when data are written to the database. 
	 * @param crossReferenceItem
	 * @return reference to the crossREferenceItem.
	 */
	public PamTableItem setCrossReferenceItem(PamTableItem crossReferenceItem) {
		this.crossReferenceItem = crossReferenceItem;
		return crossReferenceItem;
	}

	/**
	 * Sets the cross reference item. Data from the crossREferenceItem
	 * will automatically be used when data are written to the database.
	 * @param tableName name of the table to cross reference to
	 * @param columnName name of the column to cross reference to
	 * @return reference to the PamTableItem, or null if it can't be found. 
	 * If the cross reference item cannot be found it will be searched for 
	 * again when data are next required in getCrossReferenceItem
	 */
	public PamTableItem setCrossReferenceItem(String tableName, String columnName) {
		setCrossReferenceItem(findTableItem(tableName, columnName));
		if (crossReferenceItem == null) {
			xRefTable = tableName;
			xRefColumn = columnName;
		}
		return crossReferenceItem;
	}
//	
//	public void findCrossReferenceItem() {
//		
//		if (xRefTable == null || xRefColumn == null) return;
//		
//		setCrossReferenceItem(xRefTable, xRefColumn);
//		
//	}
	
	/**
	 * Searches all Pamguard datablocks and SQLLoggers for a named table and
	 * column for use in cross referencing. 
	 */
	public static PamTableItem findTableItem(String tableName, String columnName) {
		PamTableDefinition tableDef = EmptyTableDefinition.
			findTableDefinition(EmptyTableDefinition.deblankString(tableName));
		if (tableDef == null) return null;
		return tableDef.findTableItem(EmptyTableDefinition.deblankString(columnName));
	}

	public Short getShortValue() {
		if (value == null) {
			return null;
		}
		if (value.getClass() == Short.class) {
			return (Short) value;
		}
		else if (value.getClass() == Integer.class) {
			int intVal = (Integer) value;
			return new Short((short) intVal);
		}
		return (Short) value;
	}
	
	public int getIntegerValue() {
		if (value == null) {
			return 0;
		}
		return (Integer) value;
	}
	

	public double getDoubleValue() {
		if (value == null) {
			return Double.NaN;
		}
		return (Double) value;
	}
	
	
	public float getFloatValue() {
		if (value == null) {
			return Float.NaN;
		}
		return (Float) value;
	}

	public boolean getBooleanValue() {
		if (value == null) {
			return false;
		}
//		System.out.println("Value class = " + value.getClass());
		if (value.getClass() == Integer.class) {
			return ((Integer)value > 0);
		}
		else if (value.getClass() == Boolean.class) {
			return (Boolean) value;
		}
		else if (value.getClass() == String.class) {
			String v = (String) value;
			if (v.length() < 1) {
				return false;
			}
			Character c = v.charAt(0);
			int i = c;
			return (i != 0);
		}
		else {
			return (value != null);
		}
	}
	
	public String getStringValue() {
		if (value == null) {
			return null;
		}
		return ((String) value).trim();
	}
	
	
}
