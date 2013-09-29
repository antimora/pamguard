package generalDatabase;


import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;

import pamScrollSystem.ViewLoadObserver;

import PamController.AWTScheduler;
import PamController.OfflineDataStore;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.RequestCancellationObject;

/**
 * Version of DBControl for normal use while PAMGUARD is running 
 * - reading and writing of data
 * @author Doug Gillespie
 * @see DBControl
 *
 */
public class DBControlUnit extends DBControl implements OfflineDataStore {


	
	private DBControlUnit THIS;

	public DBControlUnit(String unitName) {
		super(unitName, whichStore(), true);
		THIS = this;
		setFullTablesCheck(true);
		//		int runMode = PamController.getInstance().getRunMode();
		//		if (runMode == PamController.RUN_MIXEDMODE ||
		//				runMode == PamController.RUN_PAMVIEW) {
		//			PamSettingManager.getInstance().registerSettings(this, PamSettingManager.LIST_DATABASESTUFF);
		//		}
	}

	private static int whichStore() {
		if (PamController.getInstance() == null) {
			return 0;
		}
		int runMode = PamController.getInstance().getRunMode();
		if (runMode == PamController.RUN_MIXEDMODE ||
				runMode == PamController.RUN_PAMVIEW) {
			return PamSettingManager.LIST_DATABASESTUFF;
		}
		else {
			return PamSettingManager.LIST_UNITS;
		}
	}

	/**
	 * GEt a list of keywords which might cause havoc 
	 * in SQL statements if they are used as columnn names. 
	 * These are returned on a dbms by dbms basis since they 
	 * may vary or be overridden through work arounds such 
	 * as wrapping names in "". 
	 * @return
	 */
	private String getKeywords() {
		return getDatabaseSystem().getKeywords();
	}

	/* (non-Javadoc)
	 * @see generalDatabase.DBControl#selectSystem(int, boolean)
	 */
	@Override
	public boolean selectSystem(int systemNumber, boolean openDatabase) {
		boolean ans =  super.selectSystem(systemNumber, openDatabase);
		if (ans && PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			createOfflineDataMap(null);
		}		
		return ans;
	}


	/* (non-Javadoc)
	 * @see generalDatabase.DBControl#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamController.INITIALIZATION_COMPLETE:
			if (isViewer) {
				createOfflineDataMap(null);
			}
		}
	}

	//	/* (non-Javadoc)
	//	 * @see generalDatabase.DBControl#selectDatabase(java.awt.Frame)
	//	 */
	//	@Override
	//	protected boolean selectDatabase(Frame frame) {
	//		// TODO Auto-generated method stub
	//		boolean ans =  super.selectDatabase(frame);
	//		if (ans && PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
	//			createOfflineDataMap(frame);
	//		}
	//		return ans;
	//	}


	@Override
	public void createOfflineDataMap(Window parentFrame) {
		if (getConnection() == null) {
			return;
		}
		if (!isViewer) {
			return;
		}

		AWTScheduler.getInstance().scheduleTask(new CreateDataMap(getLoggingDataBlocks()));
	}
	
	/**
	 * Function to map a single new datablock. 
	 * @param parentFrame parent frame for dialog
	 * @param dataBlock datablock to map. 
	 */
	public void mapNewDataBlock(Window parentFrame, PamDataBlock dataBlock) {
		ArrayList<PamDataBlock> oneDataBlock = new ArrayList<PamDataBlock>();
		oneDataBlock.add(dataBlock);
		AWTScheduler.getInstance().scheduleTask(new CreateDataMap(oneDataBlock));
	}
	
	/**
	 * Map a list of data blocks. 
	 * @param parentFrame parent frame for dialog
	 * @param dataBlocks Array list of datablocks. 
	 */
	public void mapNewDataBlock(Window parentFrame, ArrayList<PamDataBlock> dataBlocks) {
		AWTScheduler.getInstance().scheduleTask(new CreateDataMap(dataBlocks));
	}

	class CreateDataMap extends SwingWorker<Integer, CreateMapInfo> {

		private ArrayList<PamDataBlock> loggingBlocks;
		private DBMapMakingDialog dbMapDialog;

		/**
		 * @param loggingBlocks
		 */
		public CreateDataMap(ArrayList<PamDataBlock> loggingBlocks) {
			super();
			this.loggingBlocks = loggingBlocks;
		}

		@Override
		protected Integer doInBackground() throws Exception {
			try {
				String dbName = databaseSystem.getShortDatabaseName();
				publish(new CreateMapInfo(loggingBlocks.size(), dbName));
				for (int i = 0; i < loggingBlocks.size(); i++) {
					mapDataBlock(i);
				}
			}
			catch (Exception e){
				System.out.println("Error in Database CreateDataMap SwingWorker thread");
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * Make a data map of a datablock using one hour time intervals. 
		 * @param iBlock index of datablock in a list of logging data blocks.
		 */
		private void mapDataBlock(int iBlock) {

			PamDataBlock pamDataBlock = loggingBlocks.get(iBlock);
			if (pamDataBlock.getOfflineDataMap(THIS) != null) {
				/*
				 * Already have a data map of this type, so don't create. 
				 */
				return; 
			}
			DBOfflineDataMap dataMap = new DBOfflineDataMap(THIS, pamDataBlock); 
			pamDataBlock.addOfflineDataMap(dataMap);
			SQLLogging sqlLogging = pamDataBlock.getLogging();
			if (sqlLogging == null) {
				System.out.println("null SQLLogging in " + pamDataBlock.getDataName());
				return;
			}
			if (sqlLogging.getTableDefinition() == null) {
				System.out.println("null table definition in " + pamDataBlock.getDataName());
				return;				
			}
			if (sqlLogging.getTableDefinition().getTableName() == null) {
				System.out.println("null table name in " + pamDataBlock.getDataName());
				return;				
			}
			publish(new CreateMapInfo(iBlock, pamDataBlock, sqlLogging.getTableDefinition().getTableName()));
			String sql = String.format("SELECT UTC FROM %s WHERE UTC IS NOT NULL ORDER BY UTC ",
					sqlLogging.getTableDefinition().getTableName());
			//			System.out.println("Mapping database " + sql);
			Connection con = getConnection();
			Timestamp timestamp;
			DBOfflineDataMapPoint dataMapPoint = null;
			long mapInterval = 3600000L;
			long utcMillis;
			try {
				PreparedStatement stmt = con.prepareStatement(sql);
				ResultSet resultSet = stmt.executeQuery();
				while (resultSet.next()) {
					timestamp = resultSet.getTimestamp(1);
					utcMillis = PamCalendar.millisFromTimeStamp(timestamp);
					if (dataMapPoint == null) {
						dataMapPoint = new DBOfflineDataMapPoint(utcMillis, utcMillis, 1);
					}
					else if (utcMillis - dataMapPoint.getStartTime() > mapInterval) {
						dataMap.addDataPoint(dataMapPoint);
						dataMapPoint = new DBOfflineDataMapPoint(utcMillis, utcMillis, 1);
					}
					else {
						dataMapPoint.addNewEndTime(utcMillis);
					}
				}
				if (dataMapPoint != null) {
					dataMap.addDataPoint(dataMapPoint);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#done()
		 */
		@Override
		protected void done() {
			super.done();
			if (dbMapDialog != null) {
				dbMapDialog.setVisible(false);
			}
			PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_OFFLINE_DATASTORE);
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#process(java.util.List)
		 */
		@Override
		protected void process(List<CreateMapInfo> dataList) {
			if (dbMapDialog == null) {
				dbMapDialog = DBMapMakingDialog.showDialog(null);
			}
			for (int i = 0; i < dataList.size(); i++) {
				dbMapDialog.newData(dataList.get(i));
			}
		}

	}

	@Override
	public String getDataSourceName() {
		return getUnitName();
	}

	@Override
	public boolean loadData(PamDataBlock dataBlock, long dataStart, long dataEnd, 
			RequestCancellationObject cancellationObject, ViewLoadObserver loadObserver) {
		SQLLogging logging = dataBlock.getLogging();
		if (logging == null) {
			return false;
		}
		return logging.loadViewerData(dataStart, dataEnd, loadObserver);
	}

	@Override
	public boolean saveData(PamDataBlock dataBlock) {
		SQLLogging logging = dataBlock.getLogging();
		if (logging == null) {
			return false;
		}
		return logging.saveOfflineData(this, getConnection());
	}

	/**
	 * Find the database connection
	 * @return the database connection or null if there is either no database
	 * module loaded or no open database. 
	 */
	public static Connection findConnection() {
		DBControlUnit dbControl = findDatabaseControl();
		if (dbControl == null) {
			return null;
		}
		return dbControl.getConnection();
	}

	/**
	 * Find the database controller
	 * @return database controller, or null if no database module loaded. 
	 */
	public static DBControlUnit findDatabaseControl() {
		
		DBControlUnit dbc = (DBControlUnit) PamController.getInstance().findControlledUnit(DBControl.getDbUnitType());
		
		
		return dbc;
	}


}
