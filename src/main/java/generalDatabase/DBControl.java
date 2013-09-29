package generalDatabase;

import generalDatabase.pamCursor.PamCursor;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import offlineProcessing.DataCopyTask;
import offlineProcessing.OLProcessDialog;
import offlineProcessing.OfflineTaskGroup;

//import java.text.Normalizer;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamSettingsGroup;
import PamController.PamSettingsSource;
import PamController.PamguardVersionInfo;
import PamView.PamSidePanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Database system for accessing data in just about any type of odbc database.
 * <p>
 * This gets used in two slightly different ways in Pamguard. The first is
 * the obvious reading and writing of data to a variety of tables. The second is the
 * loading of settings from the PAmguard_settings and the PamguardModules tables tables
 * in which all program settings were serialised and stored as 6 bit ascii strings each time
 * PAMGUARD started collecting data.
 * <p>
 * So that an instance of DBControl can be made that doesn't load settings, two sub classes
 * have been made: DBContorlUnit for normal use and DBControlSettings for reading in settings
 * information.
 *
 * @author Doug Gillespie
 * @see DBControlSettings
 * @see DBControlUnit
 */

abstract public class DBControl extends PamControlledUnit implements PamSettings,
PamSettingsSource {

	ArrayList<DBSystem> databaseSystems;

	DBSystem databaseSystem;

	private Connection connection;

	DBParameters dbParameters = new DBParameters();

	private DBProcess dbProcess;

	DBSidePanel dbSidePanel;

	private DBSettingsStore dbSettingsStore;

	/**
	 * Do full check of all database tables, not just specieals for
	 * controller.
	 */
	private boolean fullTablesCheck = false;

	static private String dbUnitType = "Pamguard Database";

	private DBControl THIS;

	private JMenuItem openDatabaseMenu;

	public DBControl(String unitName, int settingsStore, boolean openImmediately) {
		super(dbUnitType, unitName);
		THIS = this;
		createDBControl(settingsStore, openImmediately);
	}

	boolean addDatabaseSystem(DBSystem dbSystem) {
		if (dbSystem.hasDriver()) {
			databaseSystems.add(dbSystem);
		}
		else {
			System.out.println(String.format("%s Database system is unavailable on this platform",
					dbSystem.getSystemName()));
			return false;
		}
		return true;
	}

	void createDBControl(int settingsStore, boolean openImmediately) {

		databaseSystems = new ArrayList<DBSystem>();
		addDatabaseSystem(new MySQLSystem(this, settingsStore));
		addDatabaseSystem(new MSAccessSystem(this, settingsStore));

//		if (PamguardVersionInfo.getReleaseType()==PamguardVersionInfo.ReleaseType.SMRU){
				addDatabaseSystem(new OOoDBSystem(this, settingsStore));
//		}

		addPamProcess(dbProcess = new DBProcess(this));

		dbSidePanel = new DBSidePanel(this);

		if (settingsStore != 0) {
			PamSettingManager.getInstance().registerSettings(this, settingsStore);
		}

		//		selectDatabase(null);

		if (databaseSystem == null){
			selectSystem(dbParameters.databaseSystem, openImmediately);
		}

		// not needed - this happens in selectSystem anyway.
//		if (isViewer == false) {
//			if (databaseSystem != null){
//				//			databaseSystem.setDatabaseName(dbParameters.databaseName);
//				connection = databaseSystem.getConnection();
//			}
//		}
	}

	/**
	 * Select a database system
	 * @param systemNumber index of the database system
	 * @param openDatabase flag to immediately open the database
	 * @return true if all ok
	 */
	public boolean selectSystem(int systemNumber, boolean openDatabase) {
		closeConnection();
		if (systemNumber >= databaseSystems.size() || systemNumber < 0) return false;
		databaseSystem = databaseSystems.get(systemNumber);
		if (openDatabase) {
			//			databaseSystem.setDatabaseName(dbParameters.databaseName);
			connection = databaseSystem.getConnection();
			if (connection != null) {
				try {
					System.out.println("Database system     : " + databaseSystem.getSystemName());
					DatabaseMetaData metaData = connection.getMetaData();
					System.out.println("Driver              : " + metaData.getDriverName());
					System.out.println("ANSI92EntryLevelSQL : " + metaData.supportsANSI92EntryLevelSQL());
					System.out.println("Keywords            : " + metaData.getSQLKeywords());
					System.out.println("Add Column          : " + metaData.supportsAlterTableWithAddColumn());
					System.out.println("Auto Commit         : " + connection.getAutoCommit());
					System.out.println("Updatable resultset : " + metaData.supportsResultSetConcurrency(
							ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_UPDATABLE));
				}
				catch (SQLException ex) {

				}
			}
			else {
				System.out.println("Database system     : " + databaseSystem.getSystemName() +
				" is not available");
			}
		}
		if (openDatabaseMenu != null) {
			openDatabaseMenu.setEnabled(databaseSystem != null && databaseSystem.canOpenDatabase());
		}
		dbSidePanel.updatePanel();
		return (connection != null);
	}

	DBSystem getSystem(int systemNumber) {
		if (systemNumber < 0 || systemNumber >= databaseSystems.size()) return null;
		return databaseSystems.get(systemNumber);
	}



	@Override
	public boolean canClose() {
		return true;
	}


	@Override
	public void pamClose() {
		//this is very important for OOoDBs at least databaseSystem.closeConnection(); is
		closeConnection();
	}

	private synchronized void closeConnection() {
		if (databaseSystem != null) {
			databaseSystem.closeConnection();
			databaseSystem.connection = null;
			connection = null;
			databaseSystem = null;
		}
	}

	public String browseDatabases(Component parent) {
		if (databaseSystem == null) {
			return null;
		}
		return databaseSystem.browseDatabases(parent);
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (PamController.getInstance().isInitializationComplete()) {
			switch(changeType) {
			case PamControllerInterface.ADD_CONTROLLEDUNIT:
			case PamControllerInterface.INITIALIZATION_COMPLETE:
			case PamControllerInterface.RENAME_CONTROLLED_UNIT:
				dbProcess.checkTables();
				dbProcess.updateProcessList();
				if (isViewer) {
					fillSettingsStore();
				}
				dbSidePanel.updatePanel();
			}
		}
	}

	/**
	 * Read all the settings in from storage.
	 */
	private void fillSettingsStore() {
		if (dbProcess == null || dbProcess.getLogSettings() == null || connection == null) {
			return;
		}
		dbSettingsStore = dbProcess.getLogSettings().loadSettings(connection);
	}

	public Connection getConnection() {
		return connection;
	}

	public Serializable getSettingsReference() {
		return dbParameters;
	}

	public long getSettingsVersion() {
		return DBParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		DBParameters np = (DBParameters) pamControlledUnitSettings.getSettings();
		dbParameters = np.clone();
		//		selectSystem(dbParameters.databaseSystem, true);
		return true;
	}

	@Override
	public JMenuItem createFileMenu(JFrame parentFrame) {
		JMenu menu = new JMenu("Database");
		JMenuItem fileMenu = new JMenuItem("Database Selection ...");
		fileMenu.addActionListener(new DatabaseFileMenuAction(this, parentFrame));
		menu.add(fileMenu);

		openDatabaseMenu = new JMenuItem("View Database ...");
		openDatabaseMenu.addActionListener(new OpenDatabaseMenu(parentFrame));
		menu.add(openDatabaseMenu);

		if (isViewer) {
			JMenu copyMenu = new JMenu("Export Binary Data ...");
			ArrayList<PamDataBlock> dataBlocks = PamController.getInstance().getDataBlocks();
			int nCopy = 0;
			for (PamDataBlock aBlock:dataBlocks) {
				if (aBlock.getLogging() == null) continue;
				if (aBlock.getBinaryDataSource() == null) continue;
				JMenuItem menuItem = new JMenuItem(aBlock.getDataName());
				menuItem.addActionListener(new ExportDataBlock(parentFrame, aBlock));
				copyMenu.add(menuItem);
				nCopy ++;
			}
			copyMenu.setEnabled(nCopy > 0);
			menu.add(copyMenu);
		}


		if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
			JMenuItem speedMenu = new JMenuItem("Test database speed");
			speedMenu.addActionListener(new SpeedMenu(parentFrame));
			menu.add(speedMenu);
		}
		return menu;
	}


	class DatabaseFileMenuAction implements ActionListener {

		private Frame frame;

		private DBControl dBControl;

		public DatabaseFileMenuAction(DBControl dbControl, Frame frame) {
			this.dBControl = dbControl;
			this.frame = frame;
		}

		public void actionPerformed(ActionEvent e) {

			selectDatabase(frame);

		}

	}

	class OpenDatabaseMenu implements ActionListener {
		private Frame frame;

		public OpenDatabaseMenu(Frame frame) {
			super();
			this.frame = frame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (databaseSystem != null) {
				databaseSystem.openCurrentDatabase();
			}
		}
	}

	class SpeedMenu implements ActionListener {
		private Frame frame;


		public SpeedMenu(Frame frame) {
			super();
			this.frame = frame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			DatabaseSpeedDialog.showDialog(frame);
		}

	}

	/**
	 * Action for general tansfer of data from binary store to
	 * database.
	 * @author Doug Gillespie
	 *
	 */
	class ExportDataBlock implements ActionListener {
		private Frame  frame;
		private PamDataBlock dataBlock;
		private OfflineTaskGroup offlineTaskGroup;
		private OLProcessDialog olProcessDialog;

		public ExportDataBlock(Frame frame, PamDataBlock dataBlock) {
			this.frame = frame;
			this.dataBlock = dataBlock;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// do the processing here so that some stuff can be saved.

			if (offlineTaskGroup == null) {
				offlineTaskGroup = new OfflineTaskGroup(THIS, dataBlock.getDataName());
				offlineTaskGroup.setPrimaryDataBlock(dataBlock);
				offlineTaskGroup.addTask(new DataCopyTask<PamDataUnit>(dataBlock));
//				DbHtSummaryTask task = new DbHtSummaryTask(THIS, dataBlock);
//				offlineTaskGroup.addTask(task);
			}
			if (olProcessDialog == null) {
				olProcessDialog = new OLProcessDialog(getPamView().getGuiFrame(), offlineTaskGroup,
						dataBlock.getDataName() + " Export");
			}
			olProcessDialog.setVisible(true);
		}
	}


	protected void setWriteCount(int dbWriteOKs, int dbWriteErrors) {
		if (dbSidePanel == null) {
			return;
		}
		dbSidePanel.writeCount(dbWriteOKs, dbWriteErrors);
	}

	protected boolean selectDatabase(Frame frame) {

		DBParameters newParams = DBDialog.showDialog(this, frame, dbParameters);
		if (newParams != null) {
			dbParameters = newParams.clone();
			selectSystem(dbParameters.databaseSystem, true);
			dbProcess.checkTables();
			fillSettingsStore();
			return true;
		}
		return false;
	}


	@Override
	public PamSidePanel getSidePanel() {
		if (isViewer) {
			return null;
		}
		else {
			return dbSidePanel;
		}
	}

	public static String getDbUnitType() {
		return dbUnitType;
	}

	public DBProcess getDbProcess() {
		return dbProcess;
	}

	public boolean saveSettingsToDB() {
		return dbProcess.saveSettingsToDB();
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettingsSource#saveStartSettings(long)
	 */
	@Override
	public boolean saveStartSettings(long timeNow) {
		return dbProcess.saveStartSettings();
	}


	@Override
	public int getNumSettings() {
		if (dbSettingsStore == null) {
			return 0;
		}
		return dbSettingsStore.getNumGroups();
	}

	@Override
	public PamSettingsGroup getSettings(int settingsIndex) {
		if (dbSettingsStore == null) {
			return null;
		}
		return dbSettingsStore.getSettingsGroup(settingsIndex);
	}

	@Override
	public String getSettingsSourceName() {
		return getUnitName();
	}

	public boolean isFullTablesCheck() {
		return fullTablesCheck;
	}

	public void setFullTablesCheck(boolean fullTablesCheck) {
		this.fullTablesCheck = fullTablesCheck;
	}
	protected ArrayList<PamDataBlock> getLoggingDataBlocks() {
		ArrayList<PamDataBlock> loggingBlocks = new ArrayList<PamDataBlock>();
		ArrayList<PamDataBlock> allDataBlocks = PamController.getInstance().getDataBlocks();
		for (int i = 0; i < allDataBlocks.size(); i++) {
			if (allDataBlocks.get(i).getLogging() != null) {
				loggingBlocks.add(allDataBlocks.get(i));
			}
		}
		return loggingBlocks;
	}

	/**
	 *
	 * @return the name of the current database
	 */
	public String getDatabaseName() {
		if (databaseSystem == null) {
			return null;
		}
		return databaseSystem.getShortDatabaseName();
	}

	public PamCursor createPamCursor(EmptyTableDefinition tableDefinition) {
		if (databaseSystem == null) {
			return null;
		}
		return databaseSystem.createPamCursor(tableDefinition);
	}

	/**
	 * @return the current databaseSystem
	 */
	public DBSystem getDatabaseSystem() {
		return databaseSystem;
	}

}
