package generalDatabase;

import generalDatabase.pamCursor.PamCursor;

import java.awt.Component;
import java.sql.Connection;
import java.sql.SQLException;


abstract public class DBSystem {

//	String databaseName;
	protected static transient final String keywords2003="ADD,ALL,ALLOCATE,ALTER,AND,ANY,ARE,ARRAY,AS,ASENSITIVE," +
	"ASYMMETRIC,AT,ATOMIC,AUTHORIZATION,BEGIN,BETWEEN,BIGINT,BINARY,BLOB,BOOLEAN,BOTH,BY,CALL,CALLED,CASCADED," +
	"CASE,CAST,CHAR,CHARACTER,CHECK,CLOB,CLOSE,COLLATE,COLUMN,COMMIT,CONDITION,CONNECT,CONSTRAINT,CONTINUE," +
	"CORRESPONDING,CREATE,CROSS,CUBE,CURRENT,CURRENT_DATE,CURRENT_DEFAULT_TRANSFORM_GROUP,CURRENT_PATH," +
	"CURRENT_ROLE,CURRENT_TIME,CURRENT_TIMESTAMP,CURRENT_TRANSFORM_GROUP_FOR_TYPE,CURRENT_USER,CURSOR,CYCLE," +
	"DATE,DAY,DEALLOCATE,DEC,DECIMAL,DECLARE,DEFAULT,DELETE,DEREF,DESCRIBE,DETERMINISTIC,DISCONNECT,DISTINCT,DO," +
	"DOUBLE,DROP,DYNAMIC,EACH,ELEMENT,ELSE,ELSEIF,END,ESCAPE,EXCEPT,EXEC,EXECUTE,EXISTS,EXIT,EXTERNAL,FALSE,FETCH," +
	"FILTER,FLOAT,FOR,FOREIGN,FREE,FROM,FULL,FUNCTION,GET,GLOBAL,GRANT,GROUP,GROUPING,HANDLER,HAVING,HOLD,HOUR," +
	"IDENTITY,IF,IMMEDIATE,IN,INDICATOR,INNER,INOUT,INPUT,INSENSITIVE,INSERT,INT,INTEGER,INTERSECT,INTERVAL,INTO," +
	"IS,ITERATE,JOIN,LANGUAGE,LARGE,LATERAL,LEADING,LEAVE,LEFT,LIKE,LOCAL,LOCALTIME,LOCALTIMESTAMP,LOOP,MATCH,MEMBER," +
	"MERGE,METHOD,MINUTE,MODIFIES,MODULE,MONTH,MULTISET,NATIONAL,NATURAL,NCHAR,NCLOB,NEW,NO,NONE,NOT,NULL,NUMERIC,OF," +
	"OLD,ON,ONLY,OPEN,OR,ORDER,OUT,OUTER,OUTPUT,OVER,OVERLAPS,PARAMETER,PARTITION,PRECISION,PREPARE,PRIMARY,PROCEDURE," +
	"RANGE,READS,REAL,RECURSIVE,REF,REFERENCES,REFERENCING,RELEASE,REPEAT,RESIGNAL,RESULT,RETURN,RETURNS,REVOKE,RIGHT," +
	"ROLLBACK,ROLLUP,ROW,ROWS,SAVEPOINT,SCOPE,SCROLL,SEARCH,SECOND,SELECT,SENSITIVE,SESSION_USER,SET,SIGNAL,SIMILAR," +
	"SMALLINT,SOME,SPECIFIC,SPECIFICTYPE,SQL,SQLEXCEPTION,SQLSTATE,SQLWARNING,START,STATIC,SUBMULTISET,SYMMETRIC,SYSTEM," +
	"SYSTEM_USER,TABLE,TABLESAMPLE,THEN,TIME,TIMESTAMP,TIMEZONE_HOUR,TIMEZONE_MINUTE,TO,TRAILING,TRANSLATION,TREAT,TRIGGER," +
	"TRUE,UNDO,UNION,UNIQUE,UNKNOWN,UNNEST,UNTIL,UPDATE,USER,USING,VALUE,VALUES,VARCHAR,VARYING,WHEN,WHENEVER,WHERE,WHILE," +
	"WINDOW,WITH,WITHIN,WITHOUT,YEAR";
	
	protected Connection connection;
	
	/**
	 * 
	 * @return The name of the database system
	 */
	abstract String getSystemName();
	
	/**
	 * 
	 * @return true if the system can create new databases. 
	 */
	abstract boolean canCreate();
	
	/**
	 * 
	 * @return the name of the currently open database.
	 */
	abstract String getDatabaseName();
	
	/**
	 * Get a shorter version of the currently open database
	 * name (e.g. without the file path name)
	 * @return a shorter name
	 */
	public String getShortDatabaseName() {
		return getDatabaseName();
//		return new File(getDatabaseName()).getName();
	}

	/**
	 * Get the SQLTypes object which can be used to preform system specific
	 * formatting of SQL strings. 
	 * @return
	 */
	public abstract SQLTypes getSqlTypes();
	

	abstract boolean exists();
	
	/**
	 * Create a new database
	 * <p>
	 * The underlying DBSystem will be responsible for any 
	 * dialogs to chose database names, etc. 
	 * @return true if successful. 
	 */
	abstract boolean create();
//	
//	abstract boolean open();
	
	/**
	 * Open a new database connection
	 */
	abstract Connection getConnection();

	/**
	 * Close the database connection
	 */
	void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
				connection = null;
			}
			catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	
	
	abstract String browseDatabases(Component parent);
	
	/**
	 * Get a database specific dialog panel to include in 
	 * the database select dialog. 
	 * @param parent parent component
	 * @return dialog panel
	 */
	abstract SystemDialogPanel getDialogPanel(Component parent);

	abstract public PamCursor createPamCursor(EmptyTableDefinition tableDefinition);

	/**
	 * 
	 * @return true if the driver for this database system is available on this 
	 * computer. the availability of database system will depend both on the OS 
	 * and whether software are installed.  
	 */
	abstract public boolean hasDriver();

	/**
	 * Get a list of keywords which potentially may not be used for column
	 * names in an SQL statement. 
	 * In reality, this list is complete overkill. Override for each system 
	 * and try to reduce the list as much as possible.  
	 * @return list of restricted keywords. 
	 */
	public String getKeywords() {
		return keywords2003;
	}

	/**
	 * find out if it's possible to open the current databse
	 * with a host application (e.g. MS Access). 
	 * @return true if it's possibe.
	 */
	public boolean canOpenDatabase() {
		return false;
	}
	/**
	 * Open the database with it's host application if available
	 * (e.g. open MS access database with Access).
	 * @return true if opened sucessfully.  
	 */
	public boolean openCurrentDatabase() {
		return false;
	}
	
}
