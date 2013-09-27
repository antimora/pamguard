package generalDatabase;

import java.util.ArrayList;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettingsGroup;

/**
 * Version of DBControl for loading settings at program startup.
 * @author Doug Gillespie
 * @see DBControl
 *
 */
public class DBControlSettings extends DBControl {

//	LogSettings logSettings;
	
	public DBControlSettings() {
		
		super("Settings database", PamSettingManager.LIST_DATABASESTUFF, false);
		
//		logSettings = new LogSettings(this);
		
	}
	
	public ArrayList<PamControlledUnitSettings> loadSettingsFromDB(PamControlledUnitSettings pamControlledUnitSettings) {
		
		if (pamControlledUnitSettings != null) {
			DBParameters np = (DBParameters) pamControlledUnitSettings.getSettings();
			dbParameters = np.clone();
		}
		
		DBParameters newParams = DBDialog.showDialog(this, null, dbParameters);
		if (newParams != null) {
			dbParameters = newParams.clone();
			selectSystem(dbParameters.databaseSystem, true);
			return loadSettingsFromDB();
		}
		else {
			return null;
		}
	}
	
	/**
	 * Try to load the serialised settings from the database.
	 * <p> First go for the last settings which are in a separate
	 * table, then if that's empty (which it will in many cases since
	 * the last settings table (Pamguard_Settings_Last) only appeared
	 * in February 2009), get the last entry in the cumulative settings 
	 * stored in the Pamguard_Settings table.  
	 * @return Array list of PAMGUARD settings. 
	 */
	public ArrayList<PamControlledUnitSettings> loadSettingsFromDB() {
		
		/*
		 * The database should have been opened when the dialog closed
		 * 
		 */
		// open the database dialog.
		if (selectDatabase(null) == false) {
			return null;
		}
		
		if (getConnection() == null) return null;

		DBSettingsStore dbSettingsStore = getDbProcess().getLogLastSettings().loadSettings(getConnection());
		
		if (dbSettingsStore == null | dbSettingsStore.getNumGroups() == 0) {
			dbSettingsStore = getDbProcess().getLogSettings().loadSettings(getConnection());
		}
		
		if (dbSettingsStore != null) {
			PamSettingsGroup lastGroup = dbSettingsStore.getLastSettingsGroup();
			if (lastGroup != null) {
				return lastGroup.getUnitSettings();
			}
		}
		/*
		 * by default return an empty array list which 
		 * will let it know that we do at least have a database
		 * we want. 
		 */
		return new ArrayList<PamControlledUnitSettings>();
	}

	@Override
	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub
//		super.notifyModelChanged(changeType);
	}
	
}
