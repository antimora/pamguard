package generalDatabase;

import java.sql.Types;

/**
 * Utilities for converting between java.sql.types numeric
 * and text formats.
 * <p>
 * SQL format can be slightly different between different databases. 
 * For example, MS Access allows you to put column names in "" which 
 * then enables you to use otherwise reserved words. MySQL on the 
 * other hand, will not let you do this.  
 * <p>
 * This base class contains some default behaviours, but expect them 
 * to be overridden in many instances.  
 * @author Doug Gillespie
 *
 * @see java.sql.Types
 *
 */
public class SQLTypes {
	
	public String typeToString(PamTableItem tableItem){
		return typeToString(tableItem.getSqlType(),tableItem.getLength(),tableItem.isCounter());
	}
	
	/**
	 * Converts a numeric SQL type and length to a text string
	 * that can be used in SQL statements. 
	 * The length parameter is generally only required by text and
	 * character types. 
	 * @param sqlType SQL type as defined in java.sql.Types 
	 * @param length length of character and text fields 
	 * @return string representation of the type
	 */
	public String typeToString(int sqlType, int length) {
		return typeToString(sqlType, length, false);
	}
	
	
	
	public String typeToString(int sqlType, int length, boolean counter) {
		switch (sqlType) {
		case Types.ARRAY:
			return "ARRAY";
		case Types.BIGINT:
			return "BIGINT";
		case Types.BINARY:
			return "BINARY";
		case Types.BIT:
			return "BIT";
		case Types.BLOB:
			return "BLOB";
		case Types.BOOLEAN:
			return "BOOLEAN";
		case Types.CHAR:
			return "CHAR(" + length + ")";
		case Types.CLOB:
			return "CLOB";
		case Types.DATALINK:
			return "DATALINK";
		case Types.DATE:
			return "DATE";
		case Types.DECIMAL:
			return "DECIMAL";
		case Types.DISTINCT:
			return "DISTINCT";
		case Types.DOUBLE:
			return "DOUBLE";
		case Types.FLOAT:
			return "DOUBLE";
		case Types.INTEGER:
			if (counter) {
				return "COUNTER";
			}
			return "INTEGER";
		case Types.JAVA_OBJECT:
			return "JAVA_OBJECT";
		case Types.LONGVARBINARY:
			return "LONGVARBINARY(" + length + ")";
		case Types.LONGVARCHAR:
			return "LONGVARCHAR(" + length + ")";
		case Types.NULL:
			return "NULL";
		case Types.NUMERIC:
			return "NUMERIC";
		case Types.OTHER:
			return "OTHER";
		case Types.REAL:
			return "REAL";
		case Types.REF:
			return "REF";
		case Types.SMALLINT:
			return "SMALLINT";
		case Types.STRUCT:
			return "STRUCT";
		case Types.TIME:
			return "TIME";
		case Types.TIMESTAMP:
			return "TIMESTAMP";
		case Types.TINYINT:
			return "TINYINT";
		case Types.VARBINARY:
			return "VARBINARY(" + length + ")";
		case Types.VARCHAR:
			return "VARCHAR(" + length + ")";
		}
		return null;
	}
	
	/**
	 * Format the column name. Formats may be slightly different for 
	 * different DBMS's. e.g. MS Access can put quotes around names. 
	 * OODB requires them to be all upper case, etc. 
	 * @param columnName
	 * @return formatted column name. 
	 */
	public synchronized String formatColumnName(String columnName) {
		return columnName;
	}
}
