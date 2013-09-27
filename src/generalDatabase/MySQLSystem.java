package generalDatabase;

import generalDatabase.pamCursor.PamCursor;
import generalDatabase.pamCursor.PamCursorManager;
import generalDatabase.pamCursor.ScrollablePamCursor;

import java.awt.Component;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;


import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;

public class MySQLSystem extends DBSystem implements PamSettings {


	public static final String SYSTEMNAME = "MySQL Databases";
	
	private MySQLDialogPanel mySQLDialogPanel;
	
	protected MySQLParameters mySQLParameters = new MySQLParameters();
	
	Connection serverConnection;
	
	final String schemaName = "information_schema";
	
	SQLTypes sqlTypes = new MySQLSQLTypes();
	
	private final String driverClass = "com.mysql.jdbc.Driver";	//Driver string
	
	private String openDatabase = "";
	
	public MySQLSystem(DBControl dbControl, int settingsStore) {
		super();
		PamSettingManager.getInstance().registerSettings(this, settingsStore);
	}

	@Override
	String browseDatabases(Component parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	boolean canCreate() {
		return true;
	}

	@Override
	boolean create() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	Connection getConnection() {
		if (connection == null || openDatabase.equalsIgnoreCase(mySQLParameters.databaseName) == false) {
			if (serverConnect(mySQLParameters) == false) return null;
			if (mySQLParameters.databaseName == null) return null;

			String databaseURL = buildDatabaseUrl(mySQLParameters.ipAddress, mySQLParameters.portNumber, mySQLParameters.databaseName);
			try {
				connection = DriverManager.getConnection(
						databaseURL, 
						mySQLParameters.userName, 
						mySQLParameters.passWord);
				connection.setAutoCommit(true);
			}
			catch (SQLException e) {
				e.printStackTrace();
				connection = null;
			}
			openDatabase = mySQLParameters.databaseName;
			PamCursorManager.setCursorType(PamCursorManager.SCROLLABLE);
		}
		return connection;
	}

	@Override
	String getSystemName() {
		return SYSTEMNAME;
	}

	@Override
	public boolean hasDriver() {

		try {
			Class.forName(driverClass).newInstance();
		} catch (InstantiationException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}

	@Override
	public SystemDialogPanel getDialogPanel(Component parent) {
		if (mySQLDialogPanel == null) {
			mySQLDialogPanel = new MySQLDialogPanel(parent, this);
		}
		return mySQLDialogPanel;
	}
	
	boolean serverConnect(MySQLParameters params) {
		return serverConnect(params.ipAddress, params.portNumber, params.userName, params.passWord);
	}
	
	boolean serverConnect(String ipAddress, int portNumber, String userName, String userPassword) {

		if (serverConnection != null) return true;
		
		try {
			Class.forName(driverClass).newInstance();	//atempt to load the driver
		}
		catch( Exception e ) {
			e.printStackTrace( );
			JOptionPane.showMessageDialog(null,	"Cannot Load: " + driverClass, mySQLParameters.databaseName,	JOptionPane.ERROR_MESSAGE);
//			serverConnected = false;
			return false;
		}
		String databaseURL = buildDatabaseUrl(ipAddress, portNumber, schemaName);
		
		
		try {
			// attempt to connect to the server
			serverConnection = DriverManager.getConnection(
					databaseURL, userName, userPassword);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
//			JOptionPane.showMessageDialog(null,"Database server connection error",(name + " error 2"),JOptionPane.ERROR_MESSAGE);   
//			databaseParams.databaseSettings.setConnectionValid(false);
			System.out.println("Cannot connect to " + databaseURL);
			serverConnection = null;
			return false;
		}
//		serverConnected = true;
		return true;
	}
	
	boolean createNewDatabase(String name) {

		try {
			Statement stmt = serverConnection.createStatement();
			//createResult = 
			stmt.executeUpdate("CREATE DATABASE " + name);
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
		mySQLParameters.databaseName = name;
		return true;
	}

	private ArrayList<String> availableDatabases;
	ArrayList<String> getAvailableDatabases(boolean doUpdate) {
		if (serverConnection == null) return null;
		if (doUpdate == false && availableDatabases != null) {
			return availableDatabases;
		}
		availableDatabases = new ArrayList<String>();
		try {
			Statement stmt = serverConnection.createStatement();
			ResultSet result = stmt.executeQuery("SELECT SCHEMA_NAME AS `Database` FROM INFORMATION_SCHEMA.SCHEMATA");
			while(result.next()) { // process results one row at a time
				String val;
				val = result.getString(1);
				//Ignore the "information_schema" database, as this holds metadata on all the server's databases
				if((!val.equalsIgnoreCase(schemaName) & (!val.equalsIgnoreCase("mysql")))){  	
					availableDatabases.add(val);
				}	
			} 
			stmt.close();
		}
		catch (SQLException e) {
			
		}
		
		return availableDatabases;
	}
	public String buildDatabaseUrl(String ipAddress, int portNumber, String databaseName){	
		return "jdbc:mysql://" + ipAddress + ":" + portNumber + "/" + databaseName +
		"?jdbcCompliantTruncation=true";
	}
	
	void serverDisconnect() {
		if (serverConnection == null) return;
		try {
			serverConnection.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		serverConnection = null;
	}

	public boolean isServerConnected() {
		return (serverConnection != null);
	}

	@Override
	String getDatabaseName() {
		return openDatabase;
	}

	public Serializable getSettingsReference() {
		return mySQLParameters;
	}

	public long getSettingsVersion() {
		return MySQLParameters.serialVersionUID;
	}

	public String getUnitName() {
		return "MySQL Database System";
	}

	public String getUnitType() {
		return "MySQL Database System";
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		mySQLParameters = ((MySQLParameters) pamControlledUnitSettings.getSettings()).clone();
		return (mySQLParameters != null);
	}

	@Override
	public PamCursor createPamCursor(EmptyTableDefinition tableDefinition) {
		return new ScrollablePamCursor(tableDefinition);
	}

	@Override
	public SQLTypes getSqlTypes() {
		return sqlTypes;
	}
}
