/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package PamController;

import generalDatabase.DBControl;
import generalDatabase.DBControlSettings;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import offlineProcessing.OfflineProcessingControlledUnit;
import offlineProcessing.OfflineProcessingProcess;

//XMLSettings
//import org.jdom.Document;
//import org.jdom.Element;
//import org.jdom.JDOMException;
//import org.jdom.input.SAXBuilder;
//import org.jdom.output.XMLOutputter;
//import org.w3c.dom.Node;
//import com.thoughtworks.xstream.XStream;

import java.io.StringWriter;


//import javax.xml.transform.OutputKeys;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerException;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;


//import sun.jdbc.odbc.OdbcDef;
import tipOfTheDay.TipOfTheDayManager;
//import javax.swing.filechooser.FileFilter;
//import javax.swing.filechooser.FileNameExtensionFilter;

import PamUtils.PamCalendar;
import PamUtils.PamFileFilter;
import PamUtils.Splash;

//import PamUtils.PamFileFilter;


/**
 * @author Doug Gillespie
 * 
 * Singleton class for managing Pam settings - where and how they are stored in
 * a persistent way between runs.
 * 
 * Any class that wants is settings saved should register with the
 * PamSettingsManager.
 * <p>
 * When the GUI closes, SaveSettings is called, SaveSettings goes through the
 * list of registered objects and asks each one to give it a reference to an
 * Object containing the settings (this MUST implement serialisable). This can
 * be the object itself, but will more likely be a reference to another object
 * just containing settings parameters. The class implementing PamSettings must
 * also provide functions getUnitType, getUnitName and getSettingsVersion. These
 * four pieces of information are then bundled into a PamControlledUnitSettings
 * which is added to an array list which is then stored in a serialised file.
 * <p>
 * When PAMGUARD starts, after all detectors have been created, the serialised
 * file is reopened. Each PamControlledUnitSettings is taken in turn and
 * compared with the list of registered objects to find one with the same name,
 * type and settings version. Once one is found, it is given the reference to
 * the settings data which t is responsible for casting into whatever class it
 * requires.
 * 
 * 
 */
public class PamSettingManager {

	static public final int LOAD_SETTINGS_OK = 0;
	static public final int LOAD_SETTINGS_CANCEL = 1;
	static public final int LOAD_SETTINGS_NEW = 2; // new settings
	
	private static PamSettingManager pamSettingManager;

	/**
	 * List of modules that have / want PAMGUARD Settings
	 * which get stored in the psf file and / or the database store. 
	 */
	private ArrayList<PamSettings> owners;
	
	/**
	 * List of modules that specifically use settings from the database
	 * storage. 
	 */
	private ArrayList<PamSettings> databaseOwners;

	/**
	 * List of settings used by 'normal' modules. 
	 */
	ArrayList<PamControlledUnitSettings> initialSettingsList;

	/**
	 * List of settings used specifically by databases. 
	 * This list never get's stored anywhere, but is just held
	 * in memory so that the database identified at startup in 
	 * viewer and mixed modes gets reloaded later on . 
	 */
	ArrayList<PamControlledUnitSettings> databaseSettingsList;
	
//	static public final String oldFileEnd = "PamSettingsFiles.ser";
	
	static public final String fileEnd = "psf";
	static public final String fileEndXML = "psfx";
	
	/**
	 * Name of the file that contains a list of recent psf files. 
	 */
	transient private final String settingsListFileName = "PamSettingsFiles";
	
	/**
	 * End of the name - will be joine to the name, but may be changed a bit for funny versions
	 */
	transient private final String settingsListFileEnd = ".psg";
	
	/**
	 * Name of a list of recent database informations (probably just the last one)
	 */
	transient private final String databaseListFile = "recentDatabases.psg";
	
	/**
	 * Identifier for modules that go in the 'normal' list
	 * (everything apart from database modules)
	 */
	public static final int LIST_UNITS = 0x1;
	/**
	 * Identifier for modules which are part of the database system. 
	 */
	public static final int LIST_DATABASESTUFF = 0x2;
	
	/**
	 * Save settings to a psf file
	 */
	static private final int SAVE_PSF = 0x1;
	/**
	 * Save settings to database tables (if available). 
	 */
	static private final int SAVE_DATABASE = 0x2;
	
	/**
	 * running in remote mode, default normal
	 */
	static public boolean RUN_REMOTE = false;
	static public String  remote_psf = null;
	static public String  external_wav = null;
	
	private boolean loadingLocalSettings;
	
//	File currentFile; // always use firstfile from the settingsFileData
	
	private ArrayList<File> recentFiles;
	
	private boolean[] settingsUsed;
//	private boolean userNotifiedAbsentSettingsFile = false;
//	private boolean userNotifiedAbsentDefaultSettingsFile = false;
	
	private boolean programStart = true;
	
	SettingsFileData settingsFileData;

	private PamSettingManager() {
		owners = new ArrayList<PamSettings>();
		databaseOwners = new ArrayList<PamSettings>();
//		setCurrentFile(new File(defaultFile));
	}

	public static PamSettingManager getInstance() {
		if (pamSettingManager == null) {
			pamSettingManager = new PamSettingManager();
		}
		return pamSettingManager;
	}
	
	/**
	 * Clear all settings from the manager
	 */
	public void reset() {
		initialSettingsList = null;
		databaseSettingsList = null;
		owners = new ArrayList<PamSettings>();
		databaseOwners = new ArrayList<PamSettings>();
		
	}

	/*
	 * Flag to indicate that initialisation of PAMGUARD has completed.
	 */
	private boolean initializationComplete = false;
	
	/**
	 * Called everytime anything in the model changes. 
	 * @param changeType type of change
	 */
	public void notifyModelChanged(int changeType) {
		if (changeType == PamControllerInterface.INITIALIZATION_COMPLETE) {
			initializationComplete = true;
		}
	}

	/**
	 * Register a PAMGAURD module that wants to store settings in a 
	 * serialised file (.psf file) and / or have those settings stored
	 * in the database settings table. 
	 * <p>Normally, all modules will 
	 * call this for at least one set of settings. Often the PamSettings
	 * is implemented by the class that extends PamControlledunit, but 
	 * it's also possible to have multiple sub modules, processes or displays
	 * implemnt PamSettings so that different settings for different bits of 
	 * a PamControlledUnit are stored separately.
	 * @see PamSettings
	 * @see PamControlledUnit
	 * @param pamUnit Reference to a PamSettings module
	 */
	public boolean registerSettings(PamSettings pamUnit) {
		return registerSettings(pamUnit, LIST_UNITS);
	}
	
	/**
	 * Register modules that have settings information that 
	 * should be stored in serialised form in 
	 * psf files and database Pamguard_Settings tables. 
	 * @param pamUnit Unit containing the settings
	 * @param whichLists which lists to store the settings in. <p>
	 * N.B. These are internal lists and not the external storage. Basically 
	 * any database modules connected with settings should to in LIST_DATABASESTUFF
	 * everything else (including the normal database) should go to LISTS_UNITS 
	 * @return true if settings registered sucessfully. 
	 */
	public boolean registerSettings(PamSettings pamUnit, int whichLists) {
		
		if ((whichLists & LIST_UNITS) != 0) {
			owners.add(pamUnit);
		}
		if ((whichLists & LIST_DATABASESTUFF) != 0) {
			databaseOwners.add(pamUnit);
		}
		
		PamControlledUnitSettings settings = findSettings(pamUnit, whichLists);
		if (settings != null && settings.getSettings() != null) {
			return pamUnit.restoreSettings(settings);
		}
		return false;
	}
	
	/**
	 * Find settings for a particular user in one or more lists. 
	 * @param user PamSettings user. 
	 * @param whichLists lists to search 
	 * @return settings object. 
	 */
	private PamControlledUnitSettings findSettings(PamSettings user, int whichLists) {
		PamControlledUnitSettings settings = null;
		
		if ((whichLists & LIST_UNITS) != 0) {
			settings = findSettings(initialSettingsList, settingsUsed, user);
		}
		
		if (settings == null && (whichLists & LIST_DATABASESTUFF) != 0) {
			settings = findGeneralSettings(user.getUnitType());
		}
		
		return settings;
	}
	/**
	 * Find settings in a list of settings, ignoring settings which have
	 * already been used by a module. 
	 * @param settingsList settings list
	 * @param usedSettings list of settings that have already been used. 
	 * @param user module that uses the settings. 
	 * @return Settings object. 
	 */
	private PamControlledUnitSettings findSettings(ArrayList<PamControlledUnitSettings> settingsList, 
			boolean[] usedSettings,	PamSettings user) {
		if (settingsList == null) return null;
		// go through the list and see if any match this module. Avoid repeats.
		for (int i = 0; i < settingsList.size(); i++) {
			if (usedSettings != null && usedSettings[i]) continue; 
			if (isSettingsUnit(user, settingsList.get(i))) {
				if (usedSettings != null) {
					usedSettings[i] = true;
				}
				return settingsList.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Searches a list of settings for settings with a
	 * specific type. 
	 * @param unitType
	 * @return PamControlledUnitSettings or null if none found
	 * @see PamControlledUnitSettings
	 */
	public PamControlledUnitSettings findGeneralSettings(String unitType) {
		if (databaseSettingsList == null) {
			return null;
		}
		for (int i = 0; i < databaseSettingsList.size(); i++) {
			if (databaseSettingsList.get(i).getUnitType().equalsIgnoreCase(unitType)) {
				return databaseSettingsList.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Find settings in a list of settings by name and by type. 
	 * @param settingsList settings list to search 
	 * @param unitType unit name
	 * @param unitName unit type
	 * @return settings object
	 */
	public PamControlledUnitSettings findSettings(ArrayList<PamControlledUnitSettings> settingsList,
			String unitType, String unitName) {
		
		if (settingsList == null) {
			return null;
		}
		PamControlledUnitSettings aSet;
		
		
		for (int i = 0; i < settingsList.size(); i++) {
			aSet = settingsList.get(i);
			if (aSet.getUnitType().equals(unitType) & (unitName == null | aSet.getUnitName().equals(unitName))) {
				return aSet;
			}
		}
		
		return null;
	}

	/**
	 * Call just before PAMGUARD exits to save the settings
	 * either to psf and / or database tables. 
	 * @return true if settings saved sucessfully. 
	 */
	public boolean saveFinalSettings() {
		int runMode = PamController.getInstance().getRunMode();
		switch (runMode) {
		case PamController.RUN_NORMAL:
		case PamController.RUN_NETWORKRECEIVER:
			return saveSettings(SAVE_PSF | SAVE_DATABASE);
		case PamController.RUN_PAMVIEW:
			return saveSettings(SAVE_DATABASE);
		case PamController.RUN_MIXEDMODE:
			return saveSettings(SAVE_DATABASE);
		case PamController.RUN_NOTHING:
			return saveSettings(SAVE_PSF);
		}
		return false;
	}

	/**
	 * Save settings to a psf file and / or the database tables. 
	 * @param saveWhere
	 * @return true if sucessful 
	 */
	public boolean saveSettings(int saveWhere) {
		
		if (initializationComplete == false) {
			// if PAMGAURD hasn't finished loading, then don't save the settings
			// or the file will get wrecked (bug tracker 2269579)
			System.out.println("Settings have not yet loaded. Don't save file");
			return false;
		}

//		saveSettingToDatabase();
		
		if ((saveWhere & SAVE_PSF) != 0) {
			saveSettingsToFile();
			saveSettingsFileData();
		}

		if ((saveWhere & SAVE_DATABASE) != 0) {
			saveSettingsToDatabase();
			saveDatabaseFileData();
		}
		
		
		return true;

	}

	/**
	 * Save configuration settings to the default (most recently used) psf file. 
	 * @return true if successful. 
	 */
	public boolean saveSettingsToFile() {

		/*
		 * Create a new list of settings in case they have changed
		 */

		ArrayList<PamControlledUnitSettings> pamSettingsList;
		pamSettingsList = new ArrayList<PamControlledUnitSettings>();
		for (int i = 0; i < owners.size(); i++) {
			pamSettingsList
			.add(new PamControlledUnitSettings(owners.get(i)
					.getUnitType(), owners.get(i).getUnitName(), 
					owners.get(i).getSettingsVersion(), 
					owners.get(i).getSettingsReference()));
		}
		int nUsed = pamSettingsList.size();
		/*
		 * Then go through the initialSettings, that were read in and any that were not used
		 * add to the current settings output so that they may be used next time around incase
		 * a module reappears that was temporarily not used.
		 */
		if (initialSettingsList != null) {
			for (int i = 0; i < initialSettingsList.size(); i++) {
				if (settingsUsed != null && settingsUsed.length > i && settingsUsed[i]) continue;
				pamSettingsList.add(initialSettingsList.get(i));
			}
		}
		/*
		 * then save it to a single serialized file
		 */
		ObjectOutputStream file = openOutputFile();
		if (file == null)
			return false;
		try {
			for (int i = 0; i < pamSettingsList.size(); i++){
				file.writeObject(pamSettingsList.get(i));
			}
			file.close();
		} catch (Exception Ex) {
			Ex.printStackTrace();
			return false;
		}
		
//		try { // experimenting with xml output. 
//			FileOutputStream fos = new FileOutputStream("pamguard.xml");
//			XMLEncoder xe = new XMLEncoder(fos);
//			for (int i = 0; i < nUsed; i++) {
//				xe.writeObject(pamSettingsList.get(i).getUnitName());
//			}
//			xe.flush();
//			xe.close();
//			fos.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// and save the settings file list to that's file
		return true;

	}
	
	/**
	 * Save configuration settings to a PSFX file (XML). 
	 * @return true if successful.
	 */
	public boolean saveSettingsToXMLFile(File file) {

		/*
		 * Create a new list of settings in case they have changed
		 */

//XMLSettings
		ArrayList<PamControlledUnitSettings> pamSettingsList;
		pamSettingsList = new ArrayList<PamControlledUnitSettings>();
		for (int i = 0; i < owners.size(); i++) {
			pamSettingsList
			.add(new PamControlledUnitSettings(owners.get(i)
					.getUnitType(), owners.get(i).getUnitName(), 
					owners.get(i).getSettingsVersion(), 
					owners.get(i).getSettingsReference()));
		}
		int nUsed = pamSettingsList.size();
		/*
		 * Then go through the initialSettings, that were read in and any that were not used
		 * add to the current settings output so that they may be used next time around incase
		 * a module reappears that was temporarily not used.
		 */
		if (initialSettingsList != null) {
			for (int i = 0; i < initialSettingsList.size(); i++) {
				if (settingsUsed != null && settingsUsed.length > i && settingsUsed[i]) continue;
				pamSettingsList.add(initialSettingsList.get(i));
			}
		}
		/*
		 * then save it to a single XML file
		 */
		
		//XML file test
		
		objectToXMLFile(pamSettingsList,file);
		return true;

	}
	
	/**
	 * An object is serializable iff .... TBC
	 */
	public void objectToXMLFile(Object serialisableObject, File file){
		
//		XStream xStream = new XStream();
//	    OutputStream outputStream = null;
//	    Writer writer = null;
//
//	    try {
//	        outputStream = new FileOutputStream(file);
//	        writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
//	        xStream.toXML(serialisableObject, writer);
//	    } catch (Exception exp) {
//	    	exp.printStackTrace();
////	        log.error(null, exp);
////	        return false;
//	    } finally {
//	        try {
//	        	writer.close();
//	        	outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//	        System.out.println("done!");
//	        
//	    }
	    System.out.println("The code for objectToXMLFile(Object serialisableObject, File file) has been commented out!!");
	}

	/**
	 * Load the PAMGAURD settings either from psf file or from 
	 * a database, depending on the run mode and type of settings required. 
	 * @param runMode
	 * @return OK if load was successful. 
	 */
	public int loadPAMSettings(int runMode) {
		int ans = LOAD_SETTINGS_OK;
		switch(runMode) {
		case PamController.RUN_NORMAL:
		case PamController.RUN_NETWORKRECEIVER:
			ans = loadNormalSettings();
			break;
		case PamController.RUN_PAMVIEW:
			ans = loadViewerSettings();
			break;
		case PamController.RUN_MIXEDMODE:
			ans = loadMixedModeSettings();
			break;
		case PamController.RUN_REMOTE:
			PamSettingManager.RUN_REMOTE = true;
			ans = loadNormalSettings();
			break;
		case PamController.RUN_NOTHING:
			ans = loadNormalSettings();
			break;
		default:
			return LOAD_SETTINGS_CANCEL;	
		}
		if (ans == LOAD_SETTINGS_OK) {
			initialiseRegisteredModules();
		}
		return ans;
	}
	
	/**
	 * Load settings perfectly 'normally' from a psf file. 
	 * @return OK whether or not any settings were loaded. 
	 */
	private int loadNormalSettings() {
		return loadPSFSettings();
	}
	
	/**
	 * Load settings for viewer mode. These must come from 
	 * an old PAMGUARD database containing settings information. 
	 * @return true if settings loaded sucessfully. 
	 */
	private int loadViewerSettings() {
		return loadDBSettings();
	}
	
	/**
	 * Load settings for mixed mode. These must come from 
	 * an old PAMGUARD database containing settings information. 
	 * @return true if settings loaded sucessfully. 
	 */
	private int loadMixedModeSettings() {
		return loadDBSettings();
	}
	
	/**
	 * Some modules may have already registered before the 
	 * settings were loaded, so this function is called
	 * as soon as they are loaded which sends settings to 
	 * all modules in the list. 
	 */
	private void initialiseRegisteredModules() {
		if (owners == null) {
			return;
		}
		PamControlledUnitSettings settings = null;
		for (int i = 0; i < owners.size(); i++) {
			settings = findSettings(initialSettingsList, settingsUsed, owners.get(i));
			if (settings != null) {
				try {
					owners.get(i).restoreSettings(settings);
				}
				catch (ClassCastException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Open the file that contains a list of files and optionally open a dialog
	 * giving the list of recent files. 
	 * <p>
	 * Unfortunately, as soon as this gets called the first time, it tries to 
	 * open a database to get more settings information and different database
	 * plug ins all start trying to get more settings and it goes round and round and
	 * round. Need to ensure that these loop around only get given the general settings
	 * information.
	 * @return
	 */
	private int loadPSFSettings() {
		if (PamSettingManager.remote_psf == null) {
			if (settingsFileData == null) {
				loadLocalSettings();
			}
			if (loadingLocalSettings) return LOAD_SETTINGS_OK;
			if (settingsFileData.showFileList && programStart) {
				SettingsFileData newData = SettingsFileDialog.showDialog(null, settingsFileData);
				if (newData != null) {
					settingsFileData = newData.clone();
				}
				else {
					return LOAD_SETTINGS_CANCEL;
				}
				programStart = false;
			}
			File ff = settingsFileData.getFirstFile();
		}
		
		initialSettingsList = loadSettingsFromFile();
//XMLSettings		
//		initialSettingsList = loadSettingsFromXMLFile();
		
		/*TODO FIXME -implement this properly (see also PamGui-line 478) to enable saving menu item
		 * so far it works for some settings- one it doesn't work for is File Folder Acquisition
		 * 
		 * output from loading XML
		 * ------------------------------------
						PAMGUARD Version 1.11.02j branch SMRU
						Revision 1028
						java.version 1.7.0_07
						java.vendor Oracle Corporation
						java.vm.version 23.3-b01
						java.vm.name Java HotSpot(TM) Client VM
						os.name Windows 7
						os.arch x86
						os.version 6.1
						java.library.path lib
						For further information and bug reporting visit www.pamguard.org
						If possible, bug reports and support requests should 
						contain a copy of the full text displayed in this window.
						(Windows users right click on window title bar for edit / copy options)
						
						System memory at 08 January 2013 19:15:31 UTC Max 1037959168, Free 13586536
						Pam Color Manager n:t Pam Color Manager
						Array Manager n:t Array Manager
						PamGUI n:t PamGUI
						Pamguard Controller n:t PamController
						MySQL Database System n:t MySQL Database System
						Database n:t MS Access Database System
						Database n:t OOo Database System
						Database n:t Pamguard Database
						Sound Card System n:t Acquisition System
						Sound Acquisition n:t ASIO Sound System
						Sound Acquisition n:t New ASIO Sound System
		  ****			File Folder Analysis n:t File Folder Acquisition System
						<PamController.PamControlledUnitSettings>
						    <versionNo>1</versionNo>
						    <unitType>File Folder Acquisition System</unitType>
						    <unitName>File Folder Analysis</unitName>
						    <settings class="Acquisition.FolderInputParameters" reference="../../PamController.PamControlledUnitSettings[13]/settings" />
						</PamController.PamControlledUnitSettings>
		 * -------------------------------------
		 * Looks like not ALL information has been stored correctly-might be best to contact XStream about resolution
		 *  
		 */

		
		return (initialSettingsList == null ? LOAD_SETTINGS_NEW : LOAD_SETTINGS_OK);
		
	}
	
	/**
	 * Load data from settings files.
	 * <p>
	 * This is just the general data - the list of recently used
	 * psf files and recent database files.  
	 */
	public boolean loadLocalSettings() {
		
		loadingLocalSettings = true;

		loadSettingsFileData();
		
		if (PamSettingManager.RUN_REMOTE == false) {
		if (settingsFileData != null) {
			TipOfTheDayManager.getInstance().setShowAtStart(settingsFileData.showTipAtStartup);
			if (settingsFileData.showTipAtStartup) {	
				TipOfTheDayManager.getInstance().showTip(null, null);
			}
		}
		}
		boolean ok = true; // always ok if non - database settings are used. 
//
//		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL) {
//			ok = loadDBSettings();		
//		}

		loadingLocalSettings = false;
		
		return ok;

	}
	
	
	/**
	 * Try to get settings information from a valid databse. If none are
	 * loaded, then return null and Pamguard will try to get them from a psf file.
	 */
	public int loadDBSettings() {
		

		if (settingsFileData == null) {
			loadLocalSettings();
		}
		
		loadDatabaseFileData();
		
		// try to find the database settings...
//		PamControlledUnitSettings dbSettings = findGeneralSettings(DBControl.getDbUnitType());
		
		DBControlSettings dbControlSettings = new DBControlSettings();
		
		/*
		 * Get settings from the database from either the Pamguard_Settings_Last
		 * or from the Pamguard_Settings table. 
		 */
		initialSettingsList = dbControlSettings.loadSettingsFromDB();
		
		/**
		 *  now need to get parameters back from the listed modules in databaseOwners
		 *  so that the correct settings can be passed over to the initialSettingsList. 
		 */
		
		if (initialSettingsList == null) {
			return LOAD_SETTINGS_CANCEL;
		}
		else {
			/* reading settings from the database was sucessful. Now the problem we have is that
			*  this database closes, and when the 'real' database opens up later, it won't be pointing 
			*  at the same place !
			*  Two options are 1) try to keep this version of the database alive
			*  2) frig the generalsettings so that the 'real' database gets the same ones.
			*  
			*  Trouble is that there are multiple settings in the settings database stuff.
			*  Copy them all back into the generalSettings list
			*/ 
			PamControlledUnitSettings aSet, generalSet;
			/**
			 * Don't take these out of databaseSettingsList - go throuh 
			 */
			PamSettings dbOwner;
			databaseSettingsList.clear();
			if (databaseOwners != null) {
				for (int i = 0; i < databaseOwners.size(); i++) {
					dbOwner = databaseOwners.get(i);
					aSet = new PamControlledUnitSettings(dbOwner.getUnitType(),
							dbOwner.getUnitName(), dbOwner.getSettingsVersion(), dbOwner.getSettingsReference());
					databaseSettingsList.add(aSet);
					// see if there is any settings with the same type and name
					// in the general list and copy settings object over. 
					generalSet = findSettings(initialSettingsList, aSet.getUnitType(), null);
					if (generalSet != null) {
						generalSet.setSettings(aSet.getSettings());
					}
				}
			}
		}
		if (initialSettingsList == null) {
			return LOAD_SETTINGS_CANCEL;
		}
		else if (initialSettingsList.size() == 0) {
			return LOAD_SETTINGS_NEW;
		}
		else {
			return LOAD_SETTINGS_OK;
		}
	}
	
	/**
	 * See if there is a database module in PAMGUARD and if so, save the 
	 * settings in serialised from in the Pamguard_Settings and Pamguard_Settings_Last
	 * tables. 
	 * @return true if successful. 
	 */
	private boolean saveSettingsToDatabase() {
		// see if there is an existing database module and if there is, then 
		// it will know how to save settings. 
		DBControl dbControl = (DBControl) PamController.getInstance().findControlledUnit(DBControl.getDbUnitType());
		if (dbControl == null) {
			return false;
		}
		return dbControl.saveSettingsToDB();
	}
	
	/**
	 * Find the owner of some PAMGUARD settings. 
	 * @param ownersList which list to search 
	 * @param unitType unit type
	 * @param unitName unit name
	 * @return owner of the settings. 
	 */
	private PamSettings findOwner(ArrayList<PamSettings> ownersList, String unitType, String unitName) {
		PamSettings owner;
		for (int i = 0; i < ownersList.size(); i++) {
			owner = ownersList.get(i);
			if (owner.getUnitType().equals(unitType) == false) continue;
			if (unitName != null && owner.getUnitName().equals(unitName) == false) continue;
			return owner;
		}
		
		return null;
	}
	
	/**
	 * Load PAMGUARD settings from a psf file. 
	 * @return Array list of settings. 
	 */
	private ArrayList<PamControlledUnitSettings> loadSettingsFromFile() {

		ArrayList<PamControlledUnitSettings> newSettingsList = 
			new ArrayList<PamControlledUnitSettings>();
		
		PamControlledUnitSettings newSetting;
				
		ObjectInputStream file = openInputFile();
		
		if (file == null) return null;
		
		Object j;
		while (true) {
			try {
				j = file.readObject();
				newSetting = (PamControlledUnitSettings) j;
				newSettingsList.add(newSetting);
			}
			catch (EOFException eof){
				break;
			}
			catch (IOException io){
//				io.printStackTrace();
				System.out.println(io.getMessage());
//				break;
			}
			catch (ClassNotFoundException Ex){
				// print and continue - there may be other things we can deal with.
//				Ex.printStackTrace();
				System.out.println(Ex.getMessage());
			}
			catch (Exception Ex) {
//				Ex.printStackTrace();
				System.out.println(Ex.getMessage());
			}
		}
		try {
			file.close();
		}
		catch (Exception Ex) {
//			Ex.printStackTrace();
		}

		return newSettingsList;
	}
	
	
//	private String nodeToString(Node node) {
//		StringWriter sw = new StringWriter();
//		try {
//			Transformer t = TransformerFactory.newInstance().newTransformer();
//			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//			t.transform(new DOMSource(node), new StreamResult(sw));
//		} catch (TransformerException te) {
//			System.out.println("nodeToString Transformer Exception");
//		}
//		return sw.toString();
//	}
	
	
//XMLSettings
//	/**
//	 * Load PAMGUARD settings from a psf file. 
//	 * @return Array list of settings. 
//	 */
//	private ArrayList<PamControlledUnitSettings> loadSettingsFromXMLFile() {
//
//		XMLOutputter outp = new XMLOutputter();
//		final ArrayList<PamControlledUnitSettings> newSettingsList = new ArrayList<PamControlledUnitSettings>();
//		final XStream xStream = new XStream();
//		SAXBuilder builder = new SAXBuilder();
//
//		File xmlFile = new File("C:\\Users\\gw\\Desktop\\tidy\\testPsfs\\LoggerTestpx.psfx");
//		String str = "";
//		try {
//
//			Document document = (Document) builder.build(xmlFile);
//			Element rootNode = document.getRootElement();
//			List list = rootNode.getChildren();
//
//			for (int i = 0; i < list.size(); i++) {
//
//				Element node = (Element) list.get(i);
//
//				str = outp.outputString(node);
//				PamControlledUnitSettings pcus = (PamControlledUnitSettings) xStream.fromXML(str);
////				System.out.println(pcus.getUnitName() + " n:t " + pcus.getUnitType());
//				newSettingsList.add(pcus);
//			}
//
//		} catch (IOException io) {
//			System.out.println(io.getMessage());
//		} catch (JDOMException jdomex) {
//			System.out.println(jdomex.getMessage());
//		} catch (Exception e) {
//			System.out.println(str);
//			e.printStackTrace();
//		}
//
//		return newSettingsList;
//	}
	
	
	
	/**
	 * See if a particular PamControlledUnitSettings object is the right one
	 * for a particular module that wants some settings. 
	 * @param settingsUser User of settings
	 * @param settings Settings object. 
	 * @return true if matched. 
	 */
	public boolean isSettingsUnit(PamSettings settingsUser, PamControlledUnitSettings settings) {
		if (settings.getUnitName() == null || settingsUser.getUnitName() == null) return false;
		if (settings.getUnitType() == null || settingsUser.getUnitType() == null) return false;
		if (settings.getUnitName().equals(settingsUser.getUnitName())
				&& settings.getUnitType().equals(settingsUser.getUnitType()) 
				&& settings.versionNo == settingsUser.getSettingsVersion()){
			return true;
		}
		return false;
	}

	/**
	 * Open psf file for settings serialised output. 
	 * @return stream handle. 
	 */
	public ObjectOutputStream openOutputFile() {
		try {
			return new ObjectOutputStream(new FileOutputStream(
					getSettingsFileName()));
			
		} catch (Exception Ex) {
			System.out.println(Ex);
			return null;
		}
	}

	/**
	 * Open psf file for settings input. 
	 * @return stream handle. 
	 */
	private ObjectInputStream openInputFile() {
//		System.out.println("Loading settings from " + getSettingsFileName());
		try {
			return new ObjectInputStream(new FileInputStream(
					getSettingsFileName()));
	
			
		} catch (Exception Ex) {
			//Ex.printStackTrace();
//			if(!userNotifiedAbsentSettingsFile){
//				System.out.println("Serialized settings file not found in JAR, Possibly not being run from standalone JAR file e.g. in Eclipse ?");
//				Splash.setStartupErrors(true);
//				JOptionPane.showMessageDialog(null,
//	                "Cannot Load: " + getSettingsFileName() +"\nAttempting to load defaults!"
//	                +"\nThis is expected on first use."
//	                ,
//	                "PamSettingManager",
//	                JOptionPane.WARNING_MESSAGE);  
//				userNotifiedAbsentSettingsFile= true;
//			}
			String msg = "You are opening new configuration file: " + getSettingsFileName();
			msg += "\nClick OK to continue with blank configuration or Cancel to exit PAMGuard";
			int ans = JOptionPane.showConfirmDialog(null, msg, "PAMGuard settings", JOptionPane.OK_CANCEL_OPTION);
			if (ans == JOptionPane.CANCEL_OPTION) {
				System.exit(0);
			}
			
			
			return null;
		}
	}

//	/**
//	 * Returns total gobbledygook - need to improve the way
//	 * PAMGAURD creates new psf files. 
//	 * @return lies. 
//	 */
//	private ObjectInputStream openInputFileResource() {
//		try {
//			return new ObjectInputStream( //new FileInputStream(
//					ClassLoader.getSystemResourceAsStream("DefaultPamguardSettings.psf"));		
//		} catch (Exception Ex) {
////			//Ex.printStackTrace();
////			System.out.println("Serialized default settings file not found!");
////			if(!userNotifiedAbsentDefaultSettingsFile){
////			JOptionPane.showMessageDialog(null,
////	                "No Default Settings Found",
////	                "PamSettingManager",
////	                JOptionPane.ERROR_MESSAGE);
////			}
////			userNotifiedAbsentDefaultSettingsFile= true;
//			return null;
//		}
//	}
	
	/**
	 * The settings list file is a file containing a list of recently 
	 * used psf files. 
	 * @return The settings list file
	 */
	private File getSettingsListFile() {
		String setFileName = getSettingsFolder() + File.separator + settingsListFileName;
		int runMode = PamController.getInstance().getRunMode();
		switch (runMode) {
		case PamController.RUN_NETWORKRECEIVER:
			setFileName += "_nr";
			break;
		case PamController.RUN_MIXEDMODE:
			setFileName += "m";
			break;
		}
		setFileName += settingsListFileEnd;
		return new File(setFileName);
	}
	
	/**
	 * Get a list of recently used databases. 
	 * @return list of recently used databases
	 */
	private File getDatabaseListFile() {
		String setFileName = getSettingsFolder() + File.separator + databaseListFile;
		return new File(setFileName);
	}
	
	/**
	 * Get the settings folder name and if necessary, 
	 * create the folder since it may not exist. 
	 * @return folder name string, (with no file separator on the end)
	 */
	private String getSettingsFolder() {
		String settingsFolder = System.getProperty("user.home");
		settingsFolder += File.separator + "Pamguard";
		// now check that folder exists
		File f = new File(settingsFolder);
		if (f.exists() == false) {
			f.mkdirs();
		}
		return settingsFolder;
	}

	/**
	 * Now that the database is becoming much more fundamental to settings
	 * storage and retrieval, the latest database settings should go into 
	 * the main settings file. This contains a list of recent databases. The trouble is,
	 * the settings are spread amongst several different settings object (e.g. one that 
	 * tells us what type of database, another that tells us a list of recent databases 
	 * for a specific database type, etc. 
	 * <p>
	 * We therefore need some modules (i.e. database ones) to also store their settings
	 * in a general settings list so that they can be read in before any other settings
	 * are read in. So each unit when it registers, says whether it should be included in
	 * the general list as well as the specific data file. 
	 * 
	 */
	public boolean loadSettingsFileData() {
		ObjectInputStream is = null;
		settingsFileData = new SettingsFileData();
		/*
		 * First do some tests to see if the settingslistfile exists. If it doens't
		 * then create the file (and do a few other things)
		 */
		File slFile = getSettingsListFile();
		if (slFile.exists() == false) {
			createSettingsListFile();
		}
		
		try {
			is = new ObjectInputStream(new FileInputStream(getSettingsListFile()));
			settingsFileData = (SettingsFileData) is.readObject();
			
		} catch (Exception Ex) {
//			System.out.println(Ex);
			System.out.println("Unable to open " + getSettingsListFile() + " this is normal on first use");
		}
		try {
			if (is != null) {
				is.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		if (settingsFileData == null){
			createSettingsListFile();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Create a settings list file. This should only
	 * ever get called once per user. One of the things it
	 * will do is copy all psf files from the installed directory
	 * over into the settingsFolder and then populate the list
	 * in a settings list file so that users get a reasonably
	 * coherent startup experience. 
	 */
	private void createSettingsListFile() {
		/**
		 * List all psf files in the program folder. 
		 * I think that we should already be working in that folder, 
		 * so can just list the files. 
		 */
		settingsFileData = new SettingsFileData();
		PamFileFilter psfFilter = new PamFileFilter("psf files", ".psf");
		psfFilter.setAcceptFolders(false);
		String settingsFolder = getSettingsFolder() + File.separator;
		// list files in the current folder. 
		String userDir = System.getProperty("user.dir");
		File folder = new File(userDir);
		File[] psfFiles = folder.listFiles(psfFilter);
		File aFile;
		if (psfFiles != null) {
			for (int i = 0; i < psfFiles.length; i++) {
				aFile = psfFiles[psfFiles.length-i-1];
				// copy that file over to the settings folder.
				File newFile = new File(settingsFolder + File.separator + aFile.getName());
//				aFile.renameTo(newFile);
				copyFile(aFile, newFile);
				// then add it to the list. 
				settingsFileData.setFirstFile(newFile);
			}
		}
		
		saveSettingsFileData();
		
	}
	
	private boolean copyFile(File source, File dest) {
		FileInputStream fIs;
		FileOutputStream fOs;
		final int BUFFLEN = 1024;
		byte[] buffer = new byte[BUFFLEN];
		int bytesRead;
		try {
			fIs = new FileInputStream(source);
			fOs = new FileOutputStream(dest);
			while ((bytesRead = fIs.read(buffer)) != -1) {
				fOs.write(buffer, 0, bytesRead);
			}
			fIs.close();
			fOs.close();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			return false;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Save the list of recently used settings files. 
	 * @return true if write OK.
	 */
	private boolean saveSettingsFileData() {
		
		if (settingsFileData == null) {
			return false;
		}
		if (PamSettingManager.RUN_REMOTE == false) {
			settingsFileData.showTipAtStartup = TipOfTheDayManager.getInstance().isShowAtStart();
		}
		
		ObjectOutputStream os;
		try {
			os = new ObjectOutputStream(new FileOutputStream(getSettingsListFile()));
			os.writeObject(settingsFileData);
		} catch (Exception Ex) {
			System.out.println(Ex);
			return false;
		}
		try {
			os.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return true;
	}

	/**
	 * Loads the details of the last database to be opened. This will 
	 * probably be in the form of multiple serialised objects since
	 * the database information is spread amongst several plug in sub-modules. 
	 * @return true if settings data loaded ok
	 */
	private boolean loadDatabaseFileData() {
		
		ObjectInputStream is;

		PamControlledUnitSettings newSetting;
		databaseSettingsList = new ArrayList<PamControlledUnitSettings>();
		Object j;
		try {
			is = new ObjectInputStream(new FileInputStream(getDatabaseListFile()));
		} catch (Exception Ex) {
			return false;
		}
		while (true) {
			try {
				j = is.readObject();
				newSetting = (PamControlledUnitSettings) j;
				databaseSettingsList.add(newSetting);
			}
			catch (EOFException eof){
				break;
			}
			catch (IOException io){
				break;
			}
			catch (ClassNotFoundException Ex){
				// print and continue - there may be othere things we can deal with.
				Ex.printStackTrace();
			}
			catch (Exception Ex) {
				Ex.printStackTrace();
			}
		}
		try {
			is.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		
		return true;
	}
	/**
	 * Save the details of the most recently used database. 
	 * @return true if successful. 
	 */
	private boolean saveDatabaseFileData() {

		if (databaseOwners == null || databaseOwners.size() == 0) {
			return false;
		}
		databaseSettingsList.clear();
		PamSettings dbOwner;
		PamControlledUnitSettings aSet;
		for (int i = 0; i < databaseOwners.size(); i++) {
			dbOwner = databaseOwners.get(i);
			aSet = new PamControlledUnitSettings(dbOwner.getUnitType(),
					dbOwner.getUnitName(), dbOwner.getSettingsVersion(), dbOwner.getSettingsReference());
			databaseSettingsList.add(aSet);
		}
		
		ObjectOutputStream os;
		try {
			os = new ObjectOutputStream(new FileOutputStream(getDatabaseListFile()));
		} catch (Exception Ex) {
			return false;
		}

//		write out the settings for all units in the general owners list. 
		ArrayList<PamControlledUnitSettings> generalSettingsList;
		generalSettingsList = new ArrayList<PamControlledUnitSettings>();
		for (int i = 0; i < databaseOwners.size(); i++) {
			generalSettingsList
					.add(new PamControlledUnitSettings(databaseOwners.get(i)
							.getUnitType(), databaseOwners.get(i).getUnitName(), 
							databaseOwners.get(i).getSettingsVersion(), 
							databaseOwners.get(i).getSettingsReference()));
		}
		try {
			for (int i = 0; i < generalSettingsList.size(); i++){
				os.writeObject(generalSettingsList.get(i));
			}
		} catch (Exception Ex) {
			Ex.printStackTrace();
			return false;
		}
		
		
		try {
			os.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Get the most recently used settings file name. We have added a switch in here
	 * to allow for the direct setting of the psf used from the command line. This
	 * can be used in remote on non remote deployments.
	 * @return File name string. 
	 */
	public String getSettingsFileName() {
		if (PamSettingManager.remote_psf != null) {
			System.out.println("Automatically loading settings settings from " + remote_psf);
			return remote_psf;
		} 
		else {
			if (settingsFileData == null || settingsFileData.getFirstFile() == null) {
				return null;
			}
			return settingsFileData.getFirstFile().getAbsolutePath();
		}
	}
	
	public String getDefaultFile() {
		String fn = getSettingsFileName();
		if (fn == null) {
			fn = "PamguardSettings.psf";
		}
		return fn;
	}
	


	/**
	 * saves settings in the current file
	 * @param frame GUI frame (needed for dialog, can be null)
	 */
	public void saveSettings(JFrame frame) {
		saveFinalSettings();
	}
	
	/**
	 * Save settings to a new psf file. 
	 * @param frame parent frame for dialog.
	 */
	public void saveSettingsAs(JFrame frame) {
		/*
		 * get a new file name, set that as the current file
		 * then write all settings to it.
		 */
		File file = null;
		if (settingsFileData != null) {
			file = settingsFileData.getFirstFile();
		}
		JFileChooser jFileChooser = new JFileChooser(file);
//		jFileChooser.setFileFilter(new SettingsFileFilter());
		jFileChooser.setApproveButtonText("Select");
		jFileChooser.addChoosableFileFilter(new PamFileFilter("PAMGUARD Settings files", fileEnd));
//		jFileChooser.setFileFilter(new FileNameExtensionFilter("PAMGUARD Settings files", defaultFile));
		int state = jFileChooser.showSaveDialog(frame);
		if (state != JFileChooser.APPROVE_OPTION) return;
		File newFile = jFileChooser.getSelectedFile();
		if (newFile == null) return;
//		newFile.getAbsoluteFile().
		newFile = PamFileFilter.checkFileEnd(newFile, fileEnd, true);
		
		System.out.println(newFile.getAbsolutePath());
		
		setDefaultFile(newFile.getAbsolutePath());
		
		saveSettings(SAVE_PSF);
		
		PamController.getInstance().getGuiFrameManager().sortFrameTitles();
		
	}
	
	/**
	 * Save settings to a new psf file. 
	 * @param frame parent frame for dialog.
	 */
	public void saveSettingsAsXML(JFrame frame) {
		/*
		 * get a new file name, set that as the current file
		 * then write all settings to it.
		 */
		File file = null;
		if (settingsFileData != null) {
			file = settingsFileData.getFirstFile();
		}
		JFileChooser jFileChooser = new JFileChooser(file);
//		jFileChooser.setFileFilter(new SettingsFileFilter());
		jFileChooser.setApproveButtonText("Select");
		jFileChooser.addChoosableFileFilter(new PamFileFilter("PAMGUARD Settings files (PSFX)", fileEndXML));
//		jFileChooser.setFileFilter(new FileNameExtensionFilter("PAMGUARD Settings files", defaultFile));
		int state = jFileChooser.showSaveDialog(frame);
		if (state != JFileChooser.APPROVE_OPTION) return;
		File newFile = jFileChooser.getSelectedFile();
		if (newFile == null) return;
//		newFile.getAbsoluteFile().
		newFile = PamFileFilter.checkFileEnd(newFile, fileEndXML, true);
		
		System.out.println(newFile.getAbsolutePath());
		
		setDefaultFile(newFile.getAbsolutePath());
		
		saveSettingsToXMLFile(newFile);
		
		PamController.getInstance().getGuiFrameManager().sortFrameTitles();
		
	}
	

	/**
	 * Set the default (first) file in the settings file data. 
	 * @param defaultFile File name string. 
	 */
	public void setDefaultFile(String defaultFile) {
		
		/**
		 * If saving from viewer or mixed mode, then the 
		 * settingsFileData may not have been loaded, in which case
		 * load it now so that old psf names remain in the list. 
		 */
		if (settingsFileData == null) {
			loadSettingsFileData();
			if (settingsFileData == null) {
				settingsFileData = new SettingsFileData();
			}
		}
		settingsFileData.setFirstFile(new File(defaultFile));
		
	}

	
	/**
	 * pop up the dialog that's shown at start up to show
	 * a list of recent settings file and give the opportunity
	 * for browsing for more. IF the new settings file is
	 * different from the current one, then send a command off 
	 * to the Controller to re-do the entire Pamguard system model
	 * @param frame parent frame for dialog (can be null)
	 */
	public void loadSettingsFrom(JFrame frame) {
		/*TODO look at combining XMLversion(psfx)
		 */
		File currentFile = null;
		if (settingsFileData != null) {
			currentFile = settingsFileData.getFirstFile();
		}
		SettingsFileData newData = SettingsFileDialog.showDialog(null, settingsFileData);
		if (newData == null) {
			return;
		}
		settingsFileData = newData.clone();
		if (settingsFileData.getFirstFile() != currentFile) {
			saveSettingsFileData();
			// rebuild the entire model. 
			PamControllerInterface pamController = PamController.getInstance();
			if (pamController == null) return;
			pamController.totalModelRebuild();
		}
		
	}
	
	/**
	 * Import a configuration during viewer mode operation. 
	 * @param frame
	 */
	public void importSettings(JFrame frame) {
		if (settingsFileData == null) {
			loadLocalSettings();
		}
		File currentFile = null;
		if (settingsFileData != null) {
			currentFile = settingsFileData.getFirstFile();
		}
		SettingsFileData newData = SettingsFileDialog.showDialog(null, settingsFileData);
		if (newData == null) {
			return;
		}
		/*
		 * Should now have a valid settings file. Import the data from it. 
		 */
		PamSettingsGroup pamSettingsGroup = new PamSettingsGroup(System.currentTimeMillis());
		/**
		 * Load all the settings from the latest psf and add them to this settings group
		 * then incorporate them into the model in the same way as is done from 
		 * the Dataview settings strip. 
		 */

		ObjectInputStream file = openInputFile();
		
		if (file == null) return;
		PamControlledUnitSettings newSetting;
		
		Object j;
		while (true) {
			try {
				j = file.readObject();
				newSetting = (PamControlledUnitSettings) j;
				pamSettingsGroup.addSettings(newSetting);
//				newSettingsList.add(newSetting);
			}
			catch (EOFException eof){
				break;
			}
			catch (IOException io){
				io.printStackTrace();
				break;
			}
			catch (ClassNotFoundException Ex){
				// print and continue - there may be othere things we can deal with.
				Ex.printStackTrace();
			}
			catch (Exception Ex) {
				Ex.printStackTrace();
			}
		}
		try {
			file.close();
		}
		catch (Exception Ex) {
//			Ex.printStackTrace();
		}

		PamController.getInstance().loadOldSettings(pamSettingsGroup);
		
	}
	public void exportSettings(JFrame frame) {
		
	}

	public ArrayList<PamSettings> getOwners() {
		return owners;
	}
	
	/**
	 * 
	 * @return everything about every set of settings currently loaded. 
	 */
	public PamSettingsGroup getCurrentSettingsGroup() {
		PamSettingsGroup psg = new PamSettingsGroup(PamCalendar.getTimeInMillis());
		PamControlledUnitSettings pcus;
		PamSettings ps;
		for (int i = 0; i < owners.size(); i++) {
			ps = owners.get(i);
			pcus = new PamControlledUnitSettings(ps.getUnitType(), ps.getUnitName(), 
					ps.getSettingsVersion(), ps.getSettingsReference());
			psg.addSettings(pcus);
		}
		return psg;
	}
	
	/**
	 * Load some old settings into all modules.
	 * <p>Currently used in viewer mode to load reloaded settings
	 * from binary files and the database. 
	 * @param settingsGroup settings group to load.
	 * @param send these new settings round to all existing modules.  
	 */
	public void loadSettingsGroup(PamSettingsGroup settingsGroup, boolean notifyExisting) {
		ArrayList<PamControlledUnitSettings> tempSettingsList = settingsGroup.getUnitSettings();
		
		
		/////////////deleteDBsettings
		/* TODO FIXME -better way? TEMPORARY - GW
		 * delete DB settings so when old settings psf is restored over current settings 
		 * the current DB will not be changed!!
		 */
		ArrayList<String> DBsettingTypes = new ArrayList<String>();
		
		DBsettingTypes.add("Pamguard Database");
		DBsettingTypes.add("MySQL Database System");
		DBsettingTypes.add("MS Access Database System");
		DBsettingTypes.add("OOo Database System");
		
		Iterator<PamControlledUnitSettings> it = tempSettingsList.iterator();
		if (it.hasNext()){
			PamControlledUnitSettings current = it.next();
			for (String dbSettingType:DBsettingTypes){
				if (current.getUnitType()==dbSettingType){
					it.remove();
				}
			}
		}
		
		/////////////
		
		initialSettingsList = tempSettingsList;
		if (notifyExisting) {
			initialiseRegisteredModules();
		}
	}
}
