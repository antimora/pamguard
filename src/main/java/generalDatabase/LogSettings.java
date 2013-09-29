package generalDatabase;

import generalDatabase.pamCursor.NonScrollablePamCursor;
import generalDatabase.pamCursor.PamCursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamSettingsGroup;
import PamUtils.Ascii6Bit;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;

/**
 * Functions for writing Pamguard Settings into any database as character data
 * Runs at DAQ start, goes through the settings manager list and for each
 * set of settings, it serialises the settings data into a binary array, this
 * is then converted from binary data to 6 bit ascii data (using the character set
 * from the AIS standard, which should be compatible with any DBMS). This character
 * string is then broken up into parts < 255 characters long and written to the 
 * Pamguard_Settings table in the database. 
 * <br>
 * This will allow 1) an audit of exactly how Pamguard was configured at any particular
 * time, 2) when looking at data offline, the database will contain all information 
 * required to reconstruct the Pamguard data model and displays, the database thereby
 * becomes a self contained document of operations, there being no need to keep hold
 * of psf settings files. 
 * 
 * @author Doug Gillespie
 * @see LogModules
 *
 */
public class LogSettings extends DbSpecial {

	private PamSettingsTableDefinition tableDef;
	
	static private final int DATA_LENGTH = 255;
	
	private boolean deletePrevious;
		
	public LogSettings(DBControl dbControl, String tableName, boolean deletePrevious) {
		
		super(dbControl);
		
		this.deletePrevious = deletePrevious;

		setUpdatePolicy(UPDATE_POLICY_OVERWRITE);
		
		tableDef = new PamSettingsTableDefinition(tableName, UPDATE_POLICY_OVERWRITE, DATA_LENGTH);
		
		setTableDefinition(tableDef);
	}

	@Override
	void pamStart(Connection con) {
		saveAllSettings();
	}
	
	boolean saveAllSettings() {

		long now = PamCalendar.getTimeInMillis(); // log all at the same time
		now = System.currentTimeMillis();
		
		int errors = 0;
		
		PamSettingManager settingsManager = PamSettingManager.getInstance();
		for (int i = 0; i < settingsManager.getOwners().size(); i++) {
			if (!logSettings(settingsManager.getOwners().get(i), now)) {
				errors ++;
			}
		}
		return errors == 0;
	}
	
	boolean logSettings(PamSettings pamSettings, long logTime) {
		/*
		 * need to serialise the pamSettings data into a byte array, then convert 
		 * that byte array to 6 bit ascii, then chom it up into bits that aren't
		 * too large, then add these to PDU's and send them off to the database. 
		 */

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(buffer);
			out.writeObject(pamSettings.getSettingsReference());
			out.close();
		}
		catch (IOException ex) {
			System.out.println("Error saving settings " + pamSettings.getUnitName());
			ex.printStackTrace();
			return false;
		}
		Ascii6Bit charData = new Ascii6Bit(buffer.toByteArray());
		String dataString = charData.getStringData();
//		System.out.println(charData.getStringData() + " spare bits " + charData.getSpareBits());
		/*
		 * now need to cut that up and turn it into sensible length chunks of ascii
		 * data that will fit into the database. 
		 */  
		int nLines = 1;
		while (nLines * DATA_LENGTH < dataString.length()) nLines++;
		int firstChar = 0;
		int lastChar;
		String subString;
		SettingsDataUnit su;
		int iString = 0;
		int spareBits = 0;
		while (firstChar < dataString.length()) {
			lastChar = Math.min(dataString.length(), firstChar + DATA_LENGTH);
			if (lastChar == dataString.length()) {
				spareBits = charData.getSpareBits();
			}
			subString = dataString.substring(firstChar, lastChar);
			su = new SettingsDataUnit(logTime, pamSettings.getUnitType(), pamSettings.getUnitName(),
					(int) pamSettings.getSettingsVersion(), nLines, iString, subString, spareBits);
			
			this.logData(su);
			
			firstChar = lastChar;
			iString++;
		}
		
		
		
		return true;
	}
	public DBSettingsStore loadSettings(Connection con) {
			
//		System.out.println(stmt);
		
		DBSettingsStore dbSettingsStore = new DBSettingsStore();
		
		PamCursor pamCursor = new NonScrollablePamCursor(tableDef);
		
		// work through and read in all records to build up the store
		
		ResultSet result = pamCursor.openReadOnlyCursor(con, "ORDER BY Id");
		
		String settingString = null;
		
		String partString;
		
		String unitType, unitName;
		
		int spares;
		
		int nStrings, iString;
		
		long version;
		
		long vLo, vHi;
		
		PamTableItem tableItem;
		
		Timestamp timestamp;
		
		long timeMillis;
		
		long lastTimeMillis = 0;
		
		PamSettingsGroup dbSettingsGroup = null;
		
		PamControlledUnitSettings pamControlledUnitSettings;
		
		boolean haveData;
		if (result != null) try {
			haveData = result.next();
			while (haveData) {

				// transfer data back into the tableItems store.
				transferDataFromResult(result);
				
				tableItem = getTableDefinition().getTimeStampItem();
				timestamp = (Timestamp) tableItem.getValue();
				timeMillis = PamCalendar.millisFromTimeStamp(timestamp);
				
				nStrings = (Integer) tableDef.getGroupTotal().getValue();
				iString = (Integer) tableDef.getGroupIndex().getValue();
				unitType = ((String) tableDef.getType().getValue()).trim();
				unitName = ((String) tableDef.getName().getValue()).trim();
//				this.versionLo = (int) (version & 0xFFFFFFFF);
//				this.versionHi = (int) (version>>32 & 0xFFFFFFFF);
				vLo = (Integer) tableDef.getVersionLo().getValue();
				vHi = (Integer) tableDef.getVersionHi().getValue();
				version = (vHi << 32) | vLo;
				spares = (Integer) tableDef.getSpareBits().getValue();
				partString = ((String) tableDef.getData().getValue()).trim();				
				
//				System.out.println(PamCalendar.formatDateTime(PamCalendar.millisFromTimeStamp(timestamp)));
				// have to allow a bit of slack here, since in some early databases
				// not all settings were written at same time - so can be spread over > 1s.
				if (Math.abs(lastTimeMillis - timeMillis) > 2000) {
					// new group
					dbSettingsGroup = new PamSettingsGroup(timeMillis);
					dbSettingsStore.addSettingsGroup(dbSettingsGroup);
				}
				lastTimeMillis = timeMillis;
				if (iString == 0 || settingString == null) {
					settingString = new String(partString);
				}
				else {
					settingString += partString;
				}
				if (iString == nStrings - 1) {
					Ascii6Bit newData = new Ascii6Bit(settingString, spares);
					byte[] byteData = newData.getByteData(); 
					ByteArrayInputStream inputBuffer = new ByteArrayInputStream(byteData);
					Object readObject = null;
					try {
						ObjectInputStream in = new ObjectInputStream(inputBuffer);
						readObject = in.readObject();
						in.close();
					}
					catch (IOException ex) {
//						return false;
					}
					catch (ClassNotFoundException cx) {
						cx.printStackTrace();
					}
					pamControlledUnitSettings = new PamControlledUnitSettings(unitType, unitName, 
							version, readObject);
					
					dbSettingsGroup.addSettings(pamControlledUnitSettings);
				}
				
				
				haveData = result.next();
			}
		
		}
		catch (SQLException ex) {
			
		}
		
		try {
			result.close();
		}
		catch (SQLException ex) {
			
		}
		
		return dbSettingsStore;
	}
	
	class SettingsDataUnit extends PamDataUnit {

//		private PamTableItem type, name, version, groupTotal, groupIndex, data;
		String type, name, data;
		int versionLo, versionHi, groupTotal, groupIndex, spareBits;
		public SettingsDataUnit(long timeMilliseconds, String type, String name, long version, 
				int groupTotal, int groupIndex, String data, int spareBits) {
			super(timeMilliseconds);
			this.type = type;
			this.name = name;
			this.data = data;
			this.versionLo = (int) (version & 0xFFFFFFFF);
			this.versionHi = (int) (version>>32 & 0xFFFFFFFF);
			this.groupTotal = groupTotal;
			this.groupIndex = groupIndex;
			this.spareBits = spareBits;
		}
		
	}
	

		/* not a functional routine yet - just somewhere to put this demo code. 
		 * 
		 */
		// now try to turn that back into a java object
//		Ascii6Bit newData = new Ascii6Bit(charData.getStringData(), charData.getSpareBits());
//		byte[] byteData = newData.getByteData(); 
//		ByteArrayInputStream inputBuffer = new ByteArrayInputStream(byteData);
//		Object readObject = null;
//		try {
//			ObjectInputStream in = new ObjectInputStream(inputBuffer);
//			readObject = in.readObject();
//			in.close();
//		}
//		catch (IOException ex) {
//			return false;
//		}
//		catch (ClassNotFoundException cx) {
//			cx.printStackTrace();
//		}
//		System.out.println(readObject.toString());
//	}

	@Override
	void pamStop(Connection con) {
		// TODO Auto-generated method stub

	}

//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

//		private PamTableItem type, name, versionLo, versionHi, groupTotal, groupIndex, data, spareBits;
		SettingsDataUnit su = (SettingsDataUnit) pamDataUnit;
		tableDef.getType().setValue(su.type);
		tableDef.getName().setValue(su.name);
		tableDef.getVersionLo().setValue(su.versionLo);
		tableDef.getVersionHi().setValue(su.versionHi);
		tableDef.getGroupTotal().setValue(su.groupTotal);
		tableDef.getGroupIndex().setValue(su.groupIndex);
		tableDef.getData().setValue(su.data);
		tableDef.getSpareBits().setValue(su.spareBits);

	}

}
