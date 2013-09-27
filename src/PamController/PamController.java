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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import offlineProcessing.OfflineProcessingControlledUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import pamScrollSystem.AbstractScrollManager;
import soundPlayback.PlaybackControl;

import fftManager.FFTDataUnit;
import generalDatabase.DBControlUnit;
import Array.ArrayManager;
import PamController.command.ExitCommand;
import PamController.command.NetworkController;
import PamDetection.PamDetection;
import PamDetection.RawDataUnit;
import PamModel.PamModel;
import PamModel.PamModelInterface;
import PamModel.PamModuleInfo;
import PamUtils.PamCalendar;
import PamView.GeneralProjector;
import PamView.GuiFrameManager;
import PamView.PamColors;
import PamView.PamGui;
import PamView.PamView;
import PamView.PamViewInterface;
import PamView.PanelOverlayDraw;
import PamView.TopToolBar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;

/**
 * @author Doug Gillespie
 *         <p>
 *         Main Pam Controller class which will communicate with the
 *         PamModelInterface and with the PamViewInterface
 *         <p>
 *         PamController contains a list of PamControlledUnit's each of which
 *         has it's own process,
		simpleMapRef.gpsTextPanel.setPixelsPerMetre(getPixelsPerMetre()); input and output data and display (Tab Panel,
 *         Menus, etc.)
 * @see PamController.PamControlledUnit
 * @see PamView.PamTabPanel
 * 
 */
public class PamController implements PamControllerInterface, PamSettings {

	// status can depend on the run mode !

	public static final int PAM_IDLE = 0;
	public static final int PAM_RUNNING = 1;

	// status' for RunMode = RUN_PAMVIEW
	public static final int PAM_LOADINGDATA = 2;

	public static final int RUN_NORMAL = 0;
	public static final int RUN_PAMVIEW = 1;
	public static final int RUN_MIXEDMODE = 2;
	public static final int RUN_REMOTE = 3;
	public static final int RUN_NOTHING = 4;
	public static final int RUN_NETWORKRECEIVER = 5;

	private int runMode = RUN_NORMAL;

	private PamModelInterface pamModelInterface;

	private ArrayList<PamControlledUnit> pamControlledUnits;

	private int pamStatus = PAM_IDLE;

	public PamViewParameters pamViewParameters = new PamViewParameters();

	//	ViewerStatusBar viewerStatusBar;

	private GuiFrameManager guiFrameManager;

	private boolean initializationComplete = false;

	// PAMGUARD CREATION IS LAUNCHED HERE !!!
	//	private static PamControllerInterface anyController = new PamController();
	private static PamController uniqueController;

	private Timer diagnosticTimer;

	private NetworkController networkController;
	private int nNetPrepared;
	private int nNetStarted;
	private int nNetStopped;

	//	private BinaryStore binaryStore;
	//
	//	public BinaryStore getBinaryStore() {
	//		return binaryStore;
	//	}

	private PamController(int runMode) {
		uniqueController = this;
		this.runMode = runMode;

		sayMemory();
		
		setPamStatus(PAM_IDLE);

		if (pamBuoyGlobals.getNetworkControlPort() != null) {
			networkController = new NetworkController(this);
		}

		//		binaryStore = new BinaryStore(this);
		ToolTipManager.sharedInstance().setDismissDelay(20000);

		guiFrameManager = new GuiFrameManager(this);

		setupPamguard();

		//		diagnosticTimer = new Timer(1000, new DiagnosticTimer());
		//		diagnosticTimer.start();
	}

	class DiagnosticTimer implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			sayMemory();
		}
	}
	private void sayMemory() {
		Runtime r = Runtime.getRuntime();
		System.out.println(String.format("System memory at %s Max %d, Free %d", 
				PamCalendar.formatDateTime(System.currentTimeMillis()), 
				r.maxMemory(), r.freeMemory()));
	}

	public static void create(int runMode) {
		if (uniqueController == null) {
			new PamController(runMode);
		}
	}


	private void setupPamguard() {
		
		
		// create the array list to hold multiple views
		
		pamControlledUnits = new ArrayList<PamControlledUnit>();

		/**
		 * Set Locale to English so that formated writes to text fields
		 * in dialogs use . and not , for the decimal. 
		 */
		Locale.setDefault(Locale.ENGLISH);

		
		/*
		 * 9 February 2009
		 * Trying to sort out settings file loading. 
		 * Was previously done when the first modules registered itself
		 * with the settings manager. Gets very confusing. Will be much easier 
		 * to load up the settings first, depending on the type of module
		 * and then have them ready when the modules start asking for them.   
		 */
		int loadAns = PamSettingManager.getInstance().loadPAMSettings(runMode);
		if (loadAns == PamSettingManager.LOAD_SETTINGS_NEW) {
//			if (runMode == RUN_PAMVIEW) {
//				// no model, no gui, so PAMGAURD will simply exit.
//				String str = String.format("PAMGUARD cannot run in %s mode without a valid database\nPAMGUARD will exit.",
//						getRunModeName());
//				str = "You have opened a database in viewer mode that contains no settings\n" +
//				"Either load settings from the binary store, import a psf settings file or create modules by hand.\n" +
//				"Press OK to continue or Cancel to exit the viewer";
//
//				int ans = JOptionPane.showConfirmDialog(null, str, "PAMGuard viewer", JOptionPane.OK_CANCEL_OPTION);
//				if (ans == JOptionPane.CANCEL_OPTION) {
//					System.exit(0);
//				}
//			}
//			else if (loadAns == ){
//				// normal settings will probably return an error, but it's OK still !
////				System.exit(0);
//			}
			//			return;
		}
		else if (loadAns == PamSettingManager.LOAD_SETTINGS_CANCEL) {
			JOptionPane.showMessageDialog(null, "No settings loaded. PAMGuard will exit", "PAMGuard", JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);
		}
		
		if (getRunMode() == RUN_NOTHING) {
			return;
		}


		/*
		 * 15/8/07 Changed creation order of model and view. 
		 * Need to be able to create a database pretty early on 
		 * (in the Model) in order to read back settings that 
		 * the GUI may require. 
		 * 
		 */
		// create the model
		pamModelInterface = new PamModel(this);

		// get the general settings out of the file immediately.
		//		PamSettingManager.getInstance().loadSettingsFileData();

		/*
		 * prepare to add a database to the model. 
		 * this will then re-read it's settings from the 
		 * settings file - which we dont' want yet !!!!!
		 * But now we have the database, it should be possible to
		 * alter the code that reads in all settings from a selected
		 * file and alter it so it gets them from the db instead.
		 * Then remove this database module immediately
		 * and let Pamguard create a new one based on the settings !
		 */
		//		PamModuleInfo mi = PamModuleInfo.findModuleInfo("generalDatabase.DBControl");
		//		PamControlledUnitSettings dbSettings = PamSettingManager.getInstance().findGeneralSettings(DBControl.getDbUnitType());
		//		if (mi != null) {
		//			addModule(mi, "Temporary Database");	
		//		}

		ArrayManager.getArrayManager(); // create the array manager so that it get's it's settings

		// create the view
		/*
		 * We are running as a remote application, start process straight away!
		 */
		if (PamSettingManager.RUN_REMOTE == false) {
			addView(new PamGui(this, pamModelInterface, 0));
		}

		PamSettingManager.getInstance().registerSettings(this);

		pamModelInterface.startModel();

		setupProcesses();
						
				if (getRunMode() == RUN_PAMVIEW) {
//					createViewerStatusBar();
					
					
//					pamControlledUnits.add(new OfflineProcessingControlledUnit("OfflineProcessing"));
				}


		/*
		 * We are running as a remote application, start process straight away!
		 */
		if (getRunMode() == RUN_NOTHING) {

		}else if (PamSettingManager.RUN_REMOTE == true) {
			// Initialisation is complete.
			initializationComplete = true;
			notifyModelChanged(PamControllerInterface.INITIALIZATION_COMPLETE);
			System.out.println("Starting Pamguard in REMOTE execution mode.");
			pamStart();
		}else{

			//			if (getRunMode() == RUN_PAMVIEW) {
			//				createViewerStatusBar();
			//			}

			// call before initialisation complete, so that processes can re-do. 
			createAnnotations();

			organiseGUIFrames();

			guiFrameManager.sortFrameTitles();

			initializationComplete = true;
			notifyModelChanged(PamControllerInterface.INITIALIZATION_COMPLETE);
		}
		if (getRunMode() == RUN_PAMVIEW) {
			/**
			 * Tell any modules implementing OfflineDataSource to check
			 * their maps. 
			 */
			AWTScheduler.getInstance().scheduleTask(new DataInitialised());
			//			PamControlledUnit pcu;
			//			OfflineDataSource offlineDataSource;
			//			for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			//				pcu = pamControlledUnits.get(iU);
			//				if (OfflineDataSource.class.isAssignableFrom(pcu.getClass())) {
			//					offlineDataSource = (OfflineDataSource) pcu;
			//					offlineDataSource.createOfflineDataMap(null);
			//				}
			//			}

			//			PamSettingManager.getInstance().registerSettings(new ViewTimesSettings());
			//			getNewViewTimes(null);
		}
	}


	class DataInitialised implements Runnable {
		@Override
		public void run() {
			notifyModelChanged(PamControllerInterface.INITIALIZE_LOADDATA);	
			// tell all scrollers to reload their data. 
			//			loadViewerData();
		}
	}


	/**
	 * Called when the number of Networked remote stations changes so that the
	 * receiver can make a decision as to what to do in terms of 
	 * preparing detectors, opening files, etc.  
	 * @param timeMilliseconds 
	 * @param nPrepared number of remote stations currently prepared (called just before start)
	 * @param nStarted number of remote stations currently started
	 * @param nStopped number of remote stations currently stopped 
	 */
	public void netReceiveStatus(long timeMilliseconds, int nPrepared, int nStarted, int nStopped) {
		if (this.nNetStarted == 0 && nStarted >= 1) {
			System.out.println("Starting processing Received network data");
			this.pamStart(true, timeMilliseconds);
		}
		if (this.nNetStarted >= 1 && nStarted == 0) {
			System.out.println("Stopping processing Received network data");
			this.pamStop();
		}
		
		this.nNetPrepared = nPrepared;
		this.nNetStarted = nStarted;
		this.nNetStopped = nStopped;
	}
	/**
	 * Loop through all controllers and processes and datablocks and set up all 
	 * of their annotations. 
	 */
	private void createAnnotations() {
		PamControlledUnit pcu;
		PamProcess pp;
		PamDataBlock pdb;
		int nPcu, nPp, nPdb;
		nPcu = getNumControlledUnits();
		for (int iPcu = 0; iPcu < nPcu; iPcu++) {
			pcu = getControlledUnit(iPcu);
			nPp = pcu.getNumPamProcesses();
			for (int iPp = 0; iPp < nPp; iPp++) {
				pp = pcu.getPamProcess(iPp);
				// only do top processes (ones which have no source datablock
				if (pp.getSourceDataBlock() == null) {
					pp.createAnnotations(true);
				}
				//				nPdb = pp.getNumOutputDataBlocks();
				//				for (int iPdb = 0; iPdb < nPdb; iPdb++) {
				//					pdb = pp.getOutputDataBlock(iPdb);
				//					pdb.createAnnotations(pp.getSourceDataBlock(), pp);
				//				}
			}
		}

	}

	/**
	 * Organise the GUI frames on start up or after a module was added 
	 * or after the frames menus have changed. 
	 */
	private void organiseGUIFrames() {

	}


	//	private void createViewerStatusBar() {
	//		
	//		viewerStatusBar = new ViewerStatusBar(this);
	//		PamStatusBar.getStatusBar().getToolBar().setLayout(new BorderLayout());
	//		PamStatusBar.getStatusBar().getToolBar().add(BorderLayout.CENTER, 
	//				viewerStatusBar.getStatusBarComponent());
	//	}

	void setupProcesses() {
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			pamControlledUnits.get(i).setupControlledUnit();
		}
	}

	/**
	 * Can PAMGUARD shut down. This question is asked in turn to 
	 * every module. Each module should attempt to make sure it can 
	 * answer true, e.g. by closing files, but if any module
	 * returns false, then canClose() will return false;
	 * @return whether it's possible to close PAMGUARD 
	 * without corrupting or losing data. 
	 */
	public boolean canClose() {
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			if (pamControlledUnits.get(i).canClose() == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Called after canClose has returned true to finally tell 
	 * all modules that PAMGUARD is definitely closing down.so they
	 * can free any resources, etc.  
	 */
	public void pamClose() {
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			pamControlledUnits.get(i).pamClose();
		}
	}

	/**
	 * Go through all data blocks in all modules and tell them to save. 
	 * This has been built into PamProcess and PamDataBlock since we want
	 * it to be easy to override this for specific modules / processes / data blocks. 
	 */
	public void saveViewerData() {
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			pamControlledUnits.get(i).saveViewerData();
		}
	}

	public void addControlledUnit(PamControlledUnit controlledUnit) {
		pamControlledUnits.add(controlledUnit);

		guiFrameManager.addControlledUnit(controlledUnit);

		notifyModelChanged(PamControllerInterface.ADD_CONTROLLEDUNIT);
	}

	public boolean addModule(Frame parentFrame, PamModuleInfo moduleInfo) {
		// first of all we need to get a name for the new module
		//		String question = "Enter a name for the new " + moduleInfo.getDescription();
		//		String newName = JOptionPane.showInputDialog(null, question, 
		//				"New " + moduleInfo.getDescription(), JOptionPane.OK_CANCEL_OPTION);
		String newName = NewModuleDialog.showDialog(parentFrame ,moduleInfo,null);
		if (newName == null) return false;
		return addModule(moduleInfo, newName);
	}

	private boolean addModule(PamModuleInfo moduleInfo, String moduleName) {

		PamControlledUnit pcu = moduleInfo.create(moduleName);

		if (pcu == null) return false;
		addControlledUnit(pcu);

		if (initializationComplete) {
			pcu.setupControlledUnit();
		}


		return true;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamControllerInterface#RemoveControlledUnt(PamguardMVC.PamControlledUnit)
	 */
	public void removeControlledUnt(PamControlledUnit controlledUnit) {

		guiFrameManager.removeControlledUnt(controlledUnit);

		while (pamControlledUnits.contains(controlledUnit)) {
			pamControlledUnits.remove(controlledUnit);
			notifyModelChanged(PamControllerInterface.REMOVE_CONTROLLEDUNIT);
		}

	}


	/* (non-Javadoc)
	 * @see PamController.PamControllerInterface#orderModules()
	 */
	public boolean orderModules(Frame parentFrame) {
		int[] newOrder = ModuleOrderDialog.showDialog(this, parentFrame); 
		if (newOrder != null) {
			// re-order the modules according the new list.
			reOrderModules(newOrder);

			notifyModelChanged(PamControllerInterface.REORDER_CONTROLLEDUNITS);

			return true;
		}

		return false;
	}

	private boolean reOrderModules(int[] newOrder) {

		if (pamControlledUnits.size() != newOrder.length) return false;

		ArrayList<PamControlledUnit> newList = new ArrayList<PamControlledUnit>();

		for (int i = 0; i < newOrder.length; i++) {

			newList.add(pamControlledUnits.get(newOrder[i]));

		}

		pamControlledUnits = newList;

		return true;
	}

	/**
	 * Swaps the positions of two modules in the main list of modules and 
	 * also swaps their tabs (if they have them). 
	 * @param m1 First PamControlledUnit to swap
	 * @param m2 Second PamControlledUnit to swap.
	 */
	private void switchModules(PamControlledUnit m1, PamControlledUnit m2) {

	}

	/**
	 * Sets the position of a particular PamControlledUnit in the list. 
	 * Also sets the right tab position, to match that order. 
	 * @param pcu
	 * @param position
	 * @return
	 */
	private boolean setModulePosition(PamControlledUnit pcu, int position) {

		return false;
	}

	public PamControlledUnit getControlledUnit(int iUnit) {
		if (iUnit < getNumControlledUnits()) {
			return pamControlledUnits.get(iUnit);
		}
		return null;
	}

	public PamControlledUnit findControlledUnit(String unitType) {
		for (int i = 0; i < getNumControlledUnits(); i++) {
			if (pamControlledUnits.get(i).getUnitType().equalsIgnoreCase(unitType)) {
				return pamControlledUnits.get(i);
			}
		}
		return null;
	}

	/**
	 * Get a list of PamControlledUnit units of a given type
	 * @param unitType Controlled unit type
	 * @return list of units. 
	 */
	public ArrayList<PamControlledUnit> findControlledUnits(String unitType) {
		ArrayList<PamControlledUnit> l = new ArrayList<PamControlledUnit>();
		int n = getNumControlledUnits();
		PamControlledUnit pcu;
		for (int i = 0; i < n; i++) {
			pcu = getControlledUnit(i);
			if (pcu.getUnitType().equals(unitType)) {
				l.add(pcu);
			}
		}

		return l;
	}

	public PamControlledUnit findControlledUnit(String unitType, String unitName) {
		for (int i = 0; i < getNumControlledUnits(); i++) {
			if (pamControlledUnits.get(i).getUnitType().equalsIgnoreCase(unitType) &&
					pamControlledUnits.get(i).getUnitName().equalsIgnoreCase(unitName)) {
				return pamControlledUnits.get(i);
			}
		}
		return null;
	}
	public PamControlledUnit findControlledUnit(Class unitClass, String unitName) {
		for (int i = 0; i < getNumControlledUnits(); i++) {
			if (pamControlledUnits.get(i).getClass() == unitClass && (unitName == null ||
					pamControlledUnits.get(i).getUnitName().equalsIgnoreCase(unitName))) {
				return pamControlledUnits.get(i);
			}
		}
		return null;
	}

	public int getNumControlledUnits() {
		return pamControlledUnits.size();
	}

	static public PamController getInstance() {
		return uniqueController;
	}

	public PamModelInterface getModelInterface() {
		return pamModelInterface;
	}

	public void addView(PamViewInterface newView) {
		guiFrameManager.addView(newView);
	}

	public void showControlledUnit(PamControlledUnit unit) {
		guiFrameManager.showControlledUnit(unit);
	}

	/**
	 * calls pamStart using the SwingUtilities
	 * invokeLater command to start PAMGAURD 
	 * later in the AWT event queue. 
	 */
	public void startLater() {
		SwingUtilities.invokeLater(new StartLater(true));
	}

	public void startLater(boolean saveSettings) {
		SwingUtilities.invokeLater(new StartLater(saveSettings));
	}
	/**
	 * Runnable for use with startLater. 
	 * @author Doug
	 *
	 */
	private class StartLater implements Runnable {

		boolean saveSettings;

		/**
		 * @param saveSettings
		 */
		public StartLater(boolean saveSettings) {
			super();
			this.saveSettings = saveSettings;
		}

		@Override
		public void run() {
			pamStart(saveSettings);
		}
	}

	/**
	 * calls pamStop using the SwingUtilities
	 * invokeLater command to stop PAMGAURD 
	 * later in the AWT event queue. 
	 */
	public void stopLater() {
		SwingUtilities.invokeLater(new StopLater());
	}

	/**
	 * Runnable to use with the stopLater() command
	 * @author Doug Gillespie
	 *
	 */
	private class StopLater implements Runnable {
		@Override
		public void run() {
			pamStop();
		}
	}


	/**
	 * Start PAMGUARD. This function also gets called from the 
	 *  GUI menu start button and from the Network control system.
	 *  <p>As well as actually starting PAMGUARD it will write
	 *  settings to the database and to the binary data store. 
	 * @return true if all modules start successfully
	 */
	public boolean pamStart() {
		return pamStart(true);
	}

	/**
	 * Start PAMGuard with an option on saving settings. 
	 * @param saveSettings flag to save settings to database and binary store
	 * @return true if all modules start successfully
	 */
	public boolean pamStart(boolean saveSettings) {
		return pamStart(saveSettings, PamCalendar.getTimeInMillis());
	}

	/**
	 * Starts PAMGuard, but with the option to save settings (to binary and to database)
	 * and also to give a specific start time for the session. When data are being received over
	 * the network, this may be in the past !
	 * @param saveSettings flag to say whether or not settings should be saved. 
	 * @param startTime start time in millis
	 * @return true if all modules start successfully
	 */
	public boolean pamStart(boolean saveSettings, long startTime) {

		PamCalendar.setSessionStartTime(startTime);
		setPamStatus(PAM_RUNNING);

		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			for (int iP = 0; iP < pamControlledUnits.get(iU)
			.getNumPamProcesses(); iP++) {
				pamControlledUnits.get(iU).getPamProcess(iP).clearOldData();
				pamControlledUnits.get(iU).getPamProcess(iP).prepareProcess();
			}
		}

		if (saveSettings) {
			saveSettings(PamCalendar.getSessionStartTime());
		}

		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			pamControlledUnits.get(iU).pamToStart();
		}
		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			for (int iP = 0; iP < pamControlledUnits.get(iU)
			.getNumPamProcesses(); iP++) {
				pamControlledUnits.get(iU).getPamProcess(iP).pamStart();
			}
		}

		// starting the DAQ may take a little while, so recheck and reset the 
		// start time.
		long startDelay = PamCalendar.getTimeInMillis() - PamCalendar.getSessionStartTime();
		System.out.println(String.format("PAMGUARD Startup took %d milliseconds at time %s", startDelay, PamCalendar.formatDateTime(System.currentTimeMillis())));
		if (PamCalendar.isSoundFile() == false) {
			PamCalendar.setSessionStartTime(PamCalendar.getTimeInMillis());
		}

		guiFrameManager.pamStart();

		return true;
	}

	/**
	 * Stopping PAMGUARD. Harder than you might think !
	 * First a pamStop() is sent to all processes, then once
	 * that's done, a pamHasStopped is sent to all Controllers. 
	 * <p>This is necessary when running in a multi-thread mode
	 * since some processes may still be receiving data and may still 
	 * pass if on to other downstream processes, storage, etc. 
	 * 
	 */
	public void pamStop() {

		setPamStatus(PAM_IDLE);

		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			for (int iP = 0; iP < pamControlledUnits.get(iU)
			.getNumPamProcesses(); iP++) {
				pamControlledUnits.get(iU).getPamProcess(iP).pamStop();
			}
		}

		/*
		 * If it's running in multithreading mode, then at this point
		 * it is necessary to make sure that all internal datablock 
		 * buffers have had time to empty.
		 */
		if (PamModel.getPamModel().isMultiThread()) {
			for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
				pamControlledUnits.get(iU).flushDataBlockBuffers(2000);
			}
		}

		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			pamControlledUnits.get(iU).pamHasStopped();
		}
		guiFrameManager.pamStop();
	}


	/**
	 * Gets called in pamStart and may / will attempt to store all
	 * PAMGUARD settings via the database and binary storage modules. 
	 */
	private void saveSettings(long timeNow) {
		PamControlledUnit pcu;
		PamSettingsSource settingsSource;
		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			pcu = pamControlledUnits.get(iU);
			if (PamSettingsSource.class.isAssignableFrom(pcu.getClass())) {
				settingsSource = (PamSettingsSource) pcu;
				settingsSource.saveStartSettings(timeNow);
			}
			//			pamControlledUnits.get(iU).pamHasStopped();
		}
	}
	
	
	/**
	 * Export certain settings into an XML file
	 * which will have the current date encoded in its
	 * name. Return the file name since it may
	 * be needed by the calling function.
	 * @return full path to output file. 
	 * 
	 */
	public String exportXMLSettings() {
		long now = System.currentTimeMillis();
		/*
		 * Get the psf or database name to start the string - 
		 * then append the date - will make 
		 * management easier. 
		 */
		String name = getPSFName();
//		File f = new File(name);
//		name = f.getName();
		// strip of the .
		int dot = name.indexOf('.');
		if (dot > 0) {
			name = name.substring(0, dot);
		}
		
		String fileName = String.format("%s_%s.xml", name, PamCalendar.formatFileDateTime(now));
		File file = new File(fileName);
		System.out.println("Writing XML data to " + file.getAbsolutePath());


		Document doc = new DocumentImpl();
		Element root = doc.createElement("PAMGUARD");
		// start with the basic version information about PAMGAURD.
		Element vInfo = doc.createElement("VERSIONINFO");
		root.appendChild(vInfo);
		vInfo.setAttribute("Created", PamCalendar.formatDateTime(System.currentTimeMillis()));
		vInfo.setAttribute("Version", PamguardVersionInfo.version);
		vInfo.setAttribute("Release", PamguardVersionInfo.getReleaseType().toString());
		
		Element array = doc.createElement("Array");
		ArrayManager.getArrayManager().fillXMLData(doc, array);
		root.appendChild(array);
		
		Element modules = doc.createElement("MODULES");
		Element moduleData;
		int nModules = getNumControlledUnits();
		PamControlledUnit pC;
		boolean hasXML;
		for (int i = 0; i < nModules; i++) {
			moduleData = doc.createElement("MODULE");
			pC = getControlledUnit(i);
			hasXML = pC.fillXMLElement(doc, moduleData);
			if (hasXML) {
				modules.appendChild(moduleData);
			}
		}

		doc.appendChild(root);
		root.appendChild(modules);
		/**
		 * XML document now created - output it to file. 
		 */
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file.getAbsolutePath());
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setLineSeparator("\r\n");
		of.setIndenting(true);
		of.setDoctype(null,"pamguard.dtd");
		XMLSerializer serializer = new XMLSerializer(fos,of);
		// As a DOM Serializer
		try {
			serializer.asDOMSerializer();
			serializer.serialize( doc.getDocumentElement() );
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return file.getAbsolutePath();
	}

	/**
	 * 
	 * @return a list of PamControlledUnits which implements the 
	 * PamSettingsSource interface
	 * @see PamSettingsSource
	 */
	public ArrayList<PamSettingsSource> findSettingsSources() {
		ArrayList<PamSettingsSource> settingsSources = new ArrayList<PamSettingsSource>();
		PamControlledUnit pcu;
		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			pcu = pamControlledUnits.get(iU);
			if (PamSettingsSource.class.isAssignableFrom(pcu.getClass())) {
				settingsSources.add((PamSettingsSource) pcu);
			}
		}
		return settingsSources;
	}

	public boolean modelSettings(JFrame frame) {
		return pamModelInterface.modelSettings(frame);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see PamguardMVC.PamControllerInterface#PamStarted()
	 */
	public void pamStarted() {
	}

	public void pamEnded() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see PamModel.PamModelInterface#GetFFTDataBlocks() Goes through all
	 *      processes and makes an array list containing only data blocks of FFT
	 *      data
	 */
	public ArrayList<PamDataBlock> getFFTDataBlocks() {
		return makeDataBlockList(FFTDataUnit.class, true);
	}

	public PamDataBlock getFFTDataBlock(int id) {
		return getDataBlock(FFTDataUnit.class, id);
	}

	public PamDataBlock getFFTDataBlock(String name) {
		return getDataBlock(FFTDataUnit.class, name);
	}

	public ArrayList<PamDataBlock> getRawDataBlocks() {
		return makeDataBlockList(RawDataUnit.class, true);
	}

	public PamRawDataBlock getRawDataBlock(int id) {
		return (PamRawDataBlock) getDataBlock(RawDataUnit.class, id);
	}

	public PamRawDataBlock getRawDataBlock(String name) {
		return (PamRawDataBlock) getDataBlock(RawDataUnit.class, name);
	}

	public ArrayList<PamDataBlock> getDetectorDataBlocks() {
		return makeDataBlockList(PamDetection.class, true);
	}

	public PamDataBlock getDetectorDataBlock(int id) {
		return getDataBlock(PamDetection.class, id);
	}

	public PamDataBlock getDetectorDataBlock(String name) {
		return getDataBlock(PamDetection.class, name);
	}

	public ArrayList<PamDataBlock> getDetectorEventDataBlocks() {
		//		return makeDataBlockList(PamguardMVC.DataType.DETEVENT);
		return null;
	}

	public PamDataBlock getDetectorEventDataBlock(int id) {
		//		return getDataBlock(PamguardMVC.DataType.DETEVENT, id);
		return null;
	}

	public PamDataBlock getDetectorEventDataBlock(String name) {
		//		return (PamDataBlock) getDataBlock(PamguardMVC.DataType.DETEVENT, name);
		return null;
	}

	public ArrayList<PamDataBlock> getDataBlocks(Class blockType, boolean includeSubClasses) {
		return makeDataBlockList(blockType, includeSubClasses);
	}

	public ArrayList<PamDataBlock> getDataBlocks() {
		return makeDataBlockList(PamDataUnit.class, true);
	}

	public ArrayList<PamDataBlock> getPlottableDataBlocks(GeneralProjector generalProjector) {

		ArrayList<PamDataBlock> blockList = new ArrayList<PamDataBlock>();
		PamProcess pP;
		Class unitClass;
		PanelOverlayDraw panelOverlayDraw;

		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			for (int iP = 0; iP < pamControlledUnits.get(iU)
			.getNumPamProcesses(); iP++) {
				pP = pamControlledUnits.get(iU).getPamProcess(iP);
				for (int j = 0; j < pP.getNumOutputDataBlocks(); j++) {
					if(pP.getOutputDataBlock(j).canDraw(generalProjector)) {
						blockList.add(pP.getOutputDataBlock(j));
					}
				}
			}
		}
		return blockList;
	}

	/**
	 * Makes a list of data blocks for all processes in all controllers for a
	 * given DataType or for all DataTypes
	 * 
	 * @param blockType -- PamguardMVC.DataType.FFT, .RAW, etc., or <b>null</b> to
	 *        get all extant blocks
	 * @return An ArrayList of data blocks
	 */
	//	private ArrayList<PamDataBlock> makeDataBlockList(Enum blockType) {
	//		ArrayList<PamDataBlock> blockList = new ArrayList<PamDataBlock>();
	//		PamProcess pP;
	//
	//		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
	//			for (int iP = 0; iP < pamControlledUnits.get(iU)
	//					.getNumPamProcesses(); iP++) {
	//				pP = pamControlledUnits.get(iU).getPamProcess(iP);
	//				for (int j = 0; j < pP.getNumOutputDataBlocks(); j++) {
	//					if (blockType == null
	//							|| pP.getOutputDataBlock(j).getDataType() == blockType) {
	//						blockList.add(pP.getOutputDataBlock(j));
	//					}
	//				}
	//			}
	//		}
	private ArrayList<PamDataBlock> makeDataBlockList(Class classType, boolean includSubClasses) {

		ArrayList<PamDataBlock> blockList = new ArrayList<PamDataBlock>();
		PamProcess pP;
		Class unitClass;

		for (int iU = 0; iU < pamControlledUnits.size(); iU++) {
			for (int iP = 0; iP < pamControlledUnits.get(iU)
			.getNumPamProcesses(); iP++) {
				pP = pamControlledUnits.get(iU).getPamProcess(iP);
				for (int j = 0; j < pP.getNumOutputDataBlocks(); j++) {
					if ((unitClass = pP.getOutputDataBlock(j).getUnitClass()) == classType) {
						blockList.add(pP.getOutputDataBlock(j));
					}
					else if (includSubClasses) {
						if (classType != null && classType.isAssignableFrom(unitClass)) {
							blockList.add(pP.getOutputDataBlock(j));
						}
//						while ((unitClass = unitClass.getSuperclass()) != null) {
//							if (unitClass == classType) {
//								blockList.add(pP.getOutputDataBlock(j));
//								break;
//							}
//						}
					}
				}
			}
		}

		return blockList;
	}

	/** 
	 * Find a block of a given type with the id number, or null if the number
	 * is out of range.
	 * 
	 * @param  blockType
	 * @param  id -- the block id number
	 * @return  block, which you may want to cast to a subtype
	 */
	public PamDataBlock getDataBlock(Class blockType, int id) {

		ArrayList<PamDataBlock> blocks = getDataBlocks(blockType, true);
		if (id >= 0 && id < blocks.size()) 
			return blocks.get(id);
		return null;
	}

	/** 
	 * Find a block of a given type with the given name, or null if it
	 * doesn't exist.
	 * @param  blockType -- RAW, FFT, DETECTOR, null, etc.
	 * @param  name -- the block name
	 * @return  block, which you may want to cast to a subtype
	 */
	public PamDataBlock getDataBlock(Class blockType, String name) {
		if (name == null) return null;
		ArrayList<PamDataBlock> blocks = getDataBlocks(blockType, true);
		for (int i = 0; i < blocks.size(); i++) {
			if (name.equals(blocks.get(i).toString())) 
				return blocks.get(i);
		}
		return null;
	}

	/**
	 * 
	 * @return a list of offline data sources.  
	 */
	public ArrayList<OfflineDataStore> findOfflineDataStores() {
		ArrayList<OfflineDataStore> ods = new ArrayList<OfflineDataStore>();
		int n = getNumControlledUnits();
		PamControlledUnit pcu;
		for (int i = 0; i < n; i++) {
			pcu = getControlledUnit(i);
			if (OfflineDataStore.class.isAssignableFrom(pcu.getClass())) {
				ods.add((OfflineDataStore) pcu);
			}
		}
		return ods;
	}

	public OfflineDataStore findOfflineDataStore(Class sourceClass) {
		ArrayList<OfflineDataStore> odss  = findOfflineDataStores();
		for (int i = 0; i < odss.size(); i++) {
			if (sourceClass.isAssignableFrom(odss.get(i).getClass())) {
				return odss.get(i);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamControllerInterface#NotifyModelChanged()
	 */
	@Override
	public void notifyModelChanged(int changeType) {

		if (changeType == CHANGED_MULTI_THREADING) {
			changedThreading();
		}

		ArrayManager.getArrayManager().notifyModelChanged(changeType);

		guiFrameManager.notifyModelChanged(changeType);

		// also tell all PamControlledUnits since they may want to find their data source 
		// it that was created after they were - i.e. dependencies have got all muddled
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			pamControlledUnits.get(i).notifyModelChanged(changeType);
		}

		PamSettingManager.getInstance().notifyModelChanged(changeType);

		PamColors.getInstance().notifyModelChanged(changeType);

		if (getRunMode() == PamController.RUN_PAMVIEW) {
			AbstractScrollManager.getScrollManager().notifyModelChanged(changeType);
		}

		if (networkController != null) {
			networkController.notifyModelChanged(changeType);
		}

	}

	public void notifyArrayChange() {
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			pamControlledUnits.get(i).notifyArrayChanged();
		}
	}

	/**
	 * loop over all units and processes, telling them to 
	 * re-subscribe to their principal data source 
	 */
	private void changedThreading() {
		PamProcess pamProcess;
		int nP;
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			nP = pamControlledUnits.get(i).getNumPamProcesses();
			for (int iP = 0; iP < nP; iP++) {
				pamProcess = pamControlledUnits.get(i).getPamProcess(iP);
				pamProcess.changedThreading();
			}
		}
	}

	public Serializable getSettingsReference() {
		ArrayList<UsedModuleInfo> usedModules = new ArrayList<UsedModuleInfo>();
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			usedModules.add(new UsedModuleInfo(pamControlledUnits.get(i).getClass().getName(), 
					pamControlledUnits.get(i).getUnitType(),
					pamControlledUnits.get(i).getUnitName()));
		}
		return usedModules;
	}

	public long getSettingsVersion() {
		return 0;
	}

	public String getUnitName() {
		return "Pamguard Controller";
	}

	public String getUnitType() {
		return "PamController";
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		if (loadingOldSettings) {
			return false;
		}
		ArrayList<UsedModuleInfo> usedModules = (ArrayList<UsedModuleInfo>) pamControlledUnitSettings.getSettings();
		UsedModuleInfo umi;
		PamModuleInfo mi;
		for (int i = 0; i < usedModules.size(); i++) {
			umi = usedModules.get(i);
			mi = PamModuleInfo.findModuleInfo(umi.className);
			if (mi == null) continue;
			addModule(mi, umi.unitName);
		}
		return true;
	}

	public void destroyModel() {
		pamStop();

		guiFrameManager.destroyModel();

		// also tell all PamControlledUnits since they may want to find their data source 
		// it that was created after they were - i.e. dependencies have got all muddled
		for (int i = 0; i < pamControlledUnits.size(); i++) {
			pamControlledUnits.get(i).notifyModelChanged(DESTROY_EVERYTHING);
		}
		pamControlledUnits = null;

		PamSettingManager.getInstance().reset();

	}

	public void totalModelRebuild() {

		destroyModel();

		setupPamguard();		
	}
	/**
	 * returns the status of Pamguard. The available status' will
	 * depend on the run mode. For instance, if run mode is RUN_NORMAL
	 * then status can be either PAM_IDLE or PAM_RUNNING.
	 * @return Pamguard status
	 */
	public int getPamStatus() {
		return pamStatus;
	}
	public void setPamStatus(int pamStatus) {
		this.pamStatus = pamStatus;
		if (getRunMode() == RUN_NORMAL) {
			TopToolBar.enableStartButton(pamStatus == PAM_IDLE);
			TopToolBar.enableStopButton(pamStatus == PAM_RUNNING);
		}
	}
	/**
	 * Gets the Pamguard running mode. This is set at startup (generally
	 * through slightly different versions of the main class). It will be
	 * one of 
	 * RUN_NORMAL
	 * RUN_PAMVIEW
	 * RUN_MIXEDMODE
	 * @return Pamguards run mode
	 */
	public int getRunMode() {
		return runMode;
	}

	public String getRunModeName() {
		switch (runMode) {
		case RUN_NORMAL:
			return "Normal";
		case RUN_PAMVIEW:
			return "Viewer";
		case RUN_MIXEDMODE:
			return "Mixed";
		default:
			return "Unknown";
		}
	}

	//	public void getNewViewTimes(Frame frame) {
	//
	//		PamViewParameters newParams = ViewTimesDialog.showDialog(null, pamViewParameters);
	//		if (newParams != null) {
	//			pamViewParameters = newParams.clone();	
	//			useNewViewTimes();
	//		}	
	//	}

	/**
	 * Class to do some extra saving of view times. 
	 * @author Douglas Gillespie
	 *
	 */
	class ViewTimesSettings implements PamSettings {

		public Serializable getSettingsReference() {
			return pamViewParameters;
		}

		public long getSettingsVersion() {
			return PamViewParameters.serialVersionUID;
		}

		public String getUnitName() {
			return "PamViewParameters";
		}

		public String getUnitType() {
			return "PamViewParameters";
		}

		public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
			pamViewParameters = ((PamViewParameters) pamControlledUnitSettings.getSettings()).clone();
			//			useNewViewTimes();
			return true;
		}

	}



	//	private long trueStart;
	//	private long trueEnd;
	//
	//	public void useNewViewTimes() {
	//		AWTScheduler.getInstance().scheduleTask(new LoadViewerData());	
	//	}

	//	public class LoadViewerData extends SwingWorker<Integer, ViewerLoadProgress> {
	//
	//		/* (non-Javadoc)
	//		 * @see javax.swing.SwingWorker#doInBackground()
	//		 */
	//		@Override
	//		protected Integer doInBackground() throws Exception {
	//			loadViewData();
	//			
	//			return null;
	//		}
	//
	//		private void loadViewData() {
	//			setPamStatus(PAM_LOADINGDATA);
	//			PamCalendar.setViewTimes(pamViewParameters.viewStartTime, pamViewParameters.viewEndTime);	
	//			// need to tell all datablocks to dump existing data and read in new.
	//			ArrayList<PamDataBlock> pamDataBlocks = getDataBlocks();
	//			PamDataBlock pamDataBlock;
	//			/*
	//			 * also need to get the true max and min load times of the data
	//			 *  
	//			 */
	//			PamDataUnit pdu;
	//			trueStart = Long.MAX_VALUE;
	//			trueEnd = Long.MIN_VALUE;
	//			for (int i = 0; i < pamDataBlocks.size(); i++){
	//				pamDataBlock = pamDataBlocks.get(i);
	//				pamDataBlock.clearAll();
	//				pamDataBlock.loadViewData(this, pamViewParameters);
	//				pdu = pamDataBlock.getFirstUnit();
	//				if (pdu != null) {
	//					trueStart = Math.min(trueStart, pdu.getTimeMilliseconds());
	//				}
	//				pdu = pamDataBlock.getLastUnit();
	//				if (pdu != null) {
	//					trueEnd = Math.max(trueEnd, pdu.getTimeMilliseconds());
	//				}
	//			}
	//		}
	//
	//		/* (non-Javadoc)
	//		 * @see javax.swing.SwingWorker#done()
	//		 */
	//		@Override
	//		protected void done() {
	//			if (trueStart != Long.MAX_VALUE) {
	//				pamViewParameters.viewStartTime = trueStart;
	//				pamViewParameters.viewEndTime = trueEnd;			
	//				PamCalendar.setViewTimes(trueStart, trueEnd);
	////				viewerStatusBar.newShowTimes();
	//			}
	//			newViewTime();
	//			setPamStatus(PAM_IDLE);
	//		}
	//
	//		/* (non-Javadoc)
	//		 * @see javax.swing.SwingWorker#process(java.util.List)
	//		 */
	//		@Override
	//		protected void process(List<ViewerLoadProgress> vlp) {
	//			// TODO Auto-generated method stub
	//			for (int i = 0; i < vlp.size(); i++) {
	////				displayProgress(vlp.get(i));
	//			}
	//		}
	//
	//		/**
	//		 * Callback from SQLLogging in worker thread. 
	//		 * @param viewerLoadProgress
	//		 */
	//		public void sayProgress(ViewerLoadProgress viewerLoadProgress) {
	//			this.publish(viewerLoadProgress);
	//		}
	//		
	//	}

	//	public void tellTrueLoadTime(long loadTime) {
	//		trueStart = Math.min(trueStart, loadTime);
	//		trueEnd = Math.max(trueEnd, loadTime);
	//	}
	//	
	//	public void newViewTime() {
	//		// view time has changed (probably from the slider)
	//		notifyModelChanged(PamControllerInterface.NEW_VIEW_TIME);
	//	}

	//public void displayProgress(ViewerLoadProgress viewerLoadProgress) {
	//	if (viewerStatusBar == null) {
	//		return;
	//	}
	////	if (viewerLoadProgress.getTableName() != null) {
	//		viewerStatusBar.setupLoadProgress(viewerLoadProgress.getTableName());
	////	}
	//	
	//}

	//	public void setupDBLoadProgress(String name) {
	//
	//		if (viewerStatusBar != null) {
	//			viewerStatusBar.setupLoadProgress(name);
	//		}
	//	}
	//	public void setDBLoadProgress(long t) {
	//
	//		if (viewerStatusBar != null) {
	//			viewerStatusBar.setLoadProgress(t);
	//		}
	//	}

	public boolean isInitializationComplete() {
		return initializationComplete;
	}

	@Override
	public GuiFrameManager getGuiFrameManager() {
		return guiFrameManager;
	}

	/**
	 * GEt the main frame if there is one. 
	 * Can be used by dialogs when no one else has
	 * sorted out a frame reference to pass to them. 
	 * @return reference to main gui frame. 
	 */
	public static Frame getMainFrame() {
		PamController c = getInstance();
		if (c.guiFrameManager == null) {
			return null;
		}
		if (c.guiFrameManager.getNumFrames() <= 0) {
			return null;
		}
		return c.guiFrameManager.getFrame(0);
	}
	/**
	 * Called from PamDialog whenever the OK button is pressed. 
	 * Don't do anything immediately to give the module that opened
	 * the dialog time to respond to it's closing (e.g. make the new
	 * settings from the dialog it's default). 
	 * Use invokeLater to send out a message as soon as the awt que is clear. 
	 */
	public void dialogOKButtonPressed() {

		SwingUtilities.invokeLater(new DialogOKButtonPressed());

	}

	/**
	 * Invoked later every time a dialog OK button is pressed. Sends 
	 * out a message to all modules to say settings have changed. 
	 * @author Doug
	 *
	 */
	class DialogOKButtonPressed implements Runnable {
		@Override
		public void run() {
			notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);	
		}
	}

	/**
	 * Enables / Disables GUI for input. This is used when data are being loaded 
	 * in viewer mode to prevetn impatient users from clicking on extra things while
	 * long background processes take place. 
	 * <p>
	 * Many of the processes loading data are run in the background in SwingWorker threads
	 * scheduled with the AWTScheduler so that they are able to update progress on teh screen
	 * @param enable enable or disable the GUI. 
	 */
	public void enableGUIControl(boolean enable) {
//		System.out.println("Enable GUI Control = " + enable);
		guiFrameManager.enableGUIControl(enable);
	}

	//	/**
	//	 * Load viewer data into all the scrollers.
	//	 */
	//	public void loadViewerData() {
	//		// TODO Auto-generated method stub
	//		AbstractScrollManager scrollManager = AbstractScrollManager.getScrollManager();
	//		scrollManager.reLoad();
	//		
	//	}

	boolean loadingOldSettings = false;
	/**
	 * Called to load a specific set of PAMGUARD settings in 
	 * viewer mode, which were previously loaded in from a 
	 * database or binary store. 
	 * @param settingsGroup settings information
	 */
	public void loadOldSettings(PamSettingsGroup settingsGroup) {
		/**
		 * Three things to do:
		 * 1. consider removing modules which exist but are no longer needed
		 * 2. Add modules which aren't present but are needed
		 * 3. re-order modules
		 * 4. Load settings into modules
		 * 5. Ping round an initialisation complete message. 
		 */
		// 1. get a list of current modules no longer needed. 
		PamControlledUnit pcu;
		ArrayList<PamControlledUnit> toRemove = new ArrayList<PamControlledUnit>();
		for (int i = 0 ; i < getNumControlledUnits(); i++) {
			pcu = getControlledUnit(i);
			if (settingsGroup.findUnitSettings(pcu.getUnitType(), pcu.getUnitName()) == null) {
				toRemove.add(pcu);
			}
		}
		ArrayList<UsedModuleInfo> usedModuleInfo = settingsGroup.getUsedModuleInfo();
		ArrayList<UsedModuleInfo> toAdd = new ArrayList<UsedModuleInfo>();
		UsedModuleInfo aModuleInfo;
		Class moduleClass = null;
		for (int i = 0; i < usedModuleInfo.size(); i++) {
			aModuleInfo = usedModuleInfo.get(i);
			try {
				moduleClass = Class.forName(aModuleInfo.className);
			} catch (ClassNotFoundException e) {
				System.out.println(String.format("The module with class %s is not available",
						aModuleInfo.className));
				continue;
			}
			if (findControlledUnit(moduleClass, aModuleInfo.unitName) == null) {
				toAdd.add(aModuleInfo);
			}
		}

		PamModuleInfo mi;
		// remove unwanted modules
		if (toRemove.size() > 0) {
			System.out.println(String.format("%d existing modules are not needed", toRemove.size()));
			for (int i = 0; i < toRemove.size(); i++) {
				mi = PamModuleInfo.findModuleInfo(toRemove.get(i).getClass().getName());
				if (mi != null && mi.canRemove() == false) {
					continue;
				}
				System.out.println("Remove module " + toRemove.get(i).toString());
				removeControlledUnt(toRemove.get(i));
			}
		}

		initializationComplete = false;
		loadingOldSettings = true;
		PamSettingManager.getInstance().loadSettingsGroup(settingsGroup, false);

		// add required modules
		if (toAdd.size() > 0) {
			System.out.println(String.format("%d additional modules are needed", toAdd.size()));
			for (int i = 0; i < toAdd.size(); i++) {
				aModuleInfo = toAdd.get(i);
				System.out.println("   Add module " + aModuleInfo.toString());
				mi = PamModuleInfo.findModuleInfo(aModuleInfo.className);
				if (mi == null || mi.canCreate() == false) {
					continue;
				}
				addModule(mi, aModuleInfo.unitName);
			}
		}
		/*
		 *  try to get everything in the right order
		 *  Needs a LUT which converts the current order
		 *  into the required order, i.e. the first element
		 *  of the LUT will be the current position of the 
		 *  unit we want to come first. 
		 */
		int[] orderLUT = new int[getNumControlledUnits()];
		PamControlledUnit aUnit;
		int currentPos;
		int n = Math.min(orderLUT.length, usedModuleInfo.size());
		int nFound = 0;
		for (int i = 0; i < orderLUT.length; i++) {
			orderLUT[i] = i;
		}
		int temp;
		for (int i = 0; i < n; i++) {
			aModuleInfo = usedModuleInfo.get(i);
			try {
				moduleClass = Class.forName(aModuleInfo.className);
			} catch (ClassNotFoundException e) {
				System.out.println(String.format("The module with class %s is not available",
						aModuleInfo.className));
				continue;
			}
			aUnit = findControlledUnit(moduleClass, aModuleInfo.unitName);
			currentPos = pamControlledUnits.indexOf(aUnit);
			if (currentPos >= 0) {
				temp = orderLUT[nFound];
				orderLUT[nFound] = currentPos;
				orderLUT[currentPos] = temp;
				nFound++;
			}
		}
		//		reOrderModules(orderLUT);

		/*
		 * Now try to give each module it's settings. 
		 */
		initializationComplete = true;

		notifyModelChanged(INITIALIZATION_COMPLETE);
		PamSettingManager.getInstance().loadSettingsGroup(settingsGroup, true);
		loadingOldSettings = false;

	}

	/**
	 * Get the name of the psf or database used to contain settings
	 * for this run. 
	 * @return name of psf or database
	 */
	public String getPSFName() {
		switch (runMode) {
		case RUN_NORMAL:
		case RUN_REMOTE:
		case RUN_NETWORKRECEIVER:
			String fn = PamSettingManager.getInstance().getSettingsFileName();
			if (fn == null) {
				return null;
			}
			File aFile = new File(fn);
			return aFile.getAbsolutePath();
		case RUN_MIXEDMODE:
		case RUN_PAMVIEW:
			DBControlUnit dbc = DBControlUnit.findDatabaseControl();
			if (dbc == null) {
				return null;
			}
			return dbc.getDatabaseName();
		}
		return null;
	}

	public void toolBarStartButton(PamControlledUnit currentControlledUnit) {
		if (getRunMode() == RUN_PAMVIEW) {
		}
		else {
			pamStart();
		}
	}

	public void toolBarStopButton(PamControlledUnit currentControlledUnit) {
		if (getRunMode() == RUN_PAMVIEW) {
			PlaybackControl.getViewerPlayback().stopViewerPlayback();
		}
		else {
			pamStop();
		}
	}

	/**
	 * Respond to storage options dialog. 
	 * @param parentFrame 
	 */
	public void storageOptions(JFrame parentFrame) {
		StorageOptions.getInstance().showDialog(parentFrame);
	}


}
