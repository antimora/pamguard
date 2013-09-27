package binaryFileStorage;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pamScrollSystem.ViewLoadObserver;

import com.mysql.jdbc.NdbLoadBalanceExceptionChecker;

import dataGram.Datagram;
import dataGram.DatagramManager;
import dataMap.OfflineDataMap;
import PamController.AWTScheduler;
import PamController.OfflineDataStore;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamSettingsGroup;
import PamController.PamSettingsSource;
import PamController.StorageOptions;
import PamUtils.FileParts;
import PamUtils.PamCalendar;
import PamUtils.PamFileFilter;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;
import PamguardMVC.RequestCancellationObject;

/**
 * The binary store will work very much like the database in that it 
 * monitors the output of data streams and when data is added to them
 * it writes it to the binary store. 
 * <p>
 * The binary store has a number of advantages over database storage, particularly
 * when it comes to writing objects with varying record lengths such as clicks and 
 * whistle contours. 
 * <p>
 * Further information about binary storage and information on formats can be found 
 * <a href="../books/BinaryFileStructure.html">here</a>.
 * 
 * @author Doug Gillespie
 *
 */
public class BinaryStore extends PamControlledUnit implements PamSettings, 
PamSettingsSource, OfflineDataStore {

	public static final String fileType = "pgdf";

	public static final String indexFileType = "pgdx";

	public static final String settingsFileType = "psfx";

	public static int CURRENT_FORMAT = 0;

	private PamController pamController;

	protected BinaryStoreSettings binaryStoreSettings = new BinaryStoreSettings();

	private Vector<BinaryOutputStream> storageStreams = new Vector<BinaryOutputStream>();

	private BinaryStoreProcess binaryStoreProcess;

	private BinarySettingsStorage binarySettingsStorage;

	public static final String unitType = "Binary Storage";
	
	private DatagramManager datagramManager;

	private DataMapSerialiser dataMapSerialiser;
	
	private boolean isNetRx;

	private boolean storesOpen;

	public BinaryStore(String unitName) {

		super(unitType, unitName);
		this.pamController = PamController.getInstance();
		addPamProcess(binaryStoreProcess = new BinaryStoreProcess(this));
		isNetRx = pamController.getRunMode() == PamController.RUN_NETWORKRECEIVER;

		binarySettingsStorage = new BinarySettingsStorage(this);

		PamSettingManager.getInstance().registerSettings(this);
		
		datagramManager = new DatagramManager(this);
		
		dataMapSerialiser = new DataMapSerialiser(this);
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			doInitialStoreChecks();
			break;
		case PamControllerInterface.CHANGED_OFFLINE_DATASTORE:
			checkDatagrams();
			break;
		}
	}

	@Override
	public void pamToStart() {
		super.pamToStart();
		StorageOptions.getInstance().setBlockOptions();
		prepareStores();
		openStores();
	}

	@Override
	public void pamHasStopped() {
		super.pamHasStopped();
		closeStores();
	}

	private void prepareStores() {
		closeStores();
		createNewStores();
	}

	/**
	 * Called from the process to close and reopen each datastream in 
	 * a new file. Probably gets called about once an hour on the hour. 
	 */
	protected void reOpenStores(int endReason) {

		long dataTime = PamCalendar.getTimeInMillis();
		long analTime = System.currentTimeMillis();
		BinaryOutputStream dataStream;
		for (int i = 0; i < storageStreams.size(); i++) {
			dataStream = storageStreams.get(i);
			dataStream.reOpen(dataTime, analTime, endReason);
		}
	}

	private void openStores() {
		long dataTime = PamCalendar.getTimeInMillis();
		long analTime = System.currentTimeMillis();
		BinaryOutputStream outputStream;
		if (!checkOutputFolder()) {
			storesOpen = false;
			return;
		}
		for (int i = 0; i < storageStreams.size(); i++) {
			outputStream = storageStreams.get(i);
			outputStream.openFile(dataTime);
			outputStream.writeHeader(dataTime, analTime);
			outputStream.writeModuleHeader();
		}
		storesOpen = true;
	}

	private boolean checkOutputFolder() {
		/*
		 * Check the output folder exists and if it doesn't, then throw up the dialog to select a folder. 
		 */
		File folder = new File(binaryStoreSettings.getStoreLocation());
		if (folder.exists()) {
			return true;
		}

		BinaryStoreSettings newSettings = BinaryStorageDialog.showDialog(null, binaryStoreSettings);
		if (newSettings != null) {
			binaryStoreSettings = newSettings.clone();
			folder = new File(binaryStoreSettings.getStoreLocation());
			return folder.exists();
		}
		
		return false;
	}

	private void closeStores() {
		long dataTime = PamCalendar.getTimeInMillis();
		long analTime = System.currentTimeMillis();
		BinaryOutputStream outputStream;
		for (int i = 0; i < storageStreams.size(); i++) {
			outputStream = storageStreams.get(i);
			outputStream.writeModuleFooter();
			outputStream.writeFooter(dataTime, analTime, BinaryFooter.END_RUNSTOPPED);
			outputStream.closeFile();
			outputStream.createIndexFile();
		}
		storesOpen = false;
	}

	private void createNewStores() {

		storageStreams.clear();

		//		ArrayList<BinaryDataSource> dataSources;
		ArrayList<PamDataBlock> streamingDataBlocks = getStreamingDataBlocks(true);
		if (streamingDataBlocks == null) {
			return;
		}
		int n = streamingDataBlocks.size();
		for (int i = 0; i < n; i++) {
			storageStreams.add(new BinaryOutputStream(this, streamingDataBlocks.get(i)));
		}

	}


	/**
	 * Get a list of data blocks with binary storage. If input parameter is true, then
	 * stores which have their binary storage disabled will NOT be included in the list. 
	 * @param binaryStore true if binary storage - blocks with binaryStore flag set false will not 
	 * be included. 
	 * @return list of data blocks which have binary storage. 
	 */
	public static ArrayList<PamDataBlock> getStreamingDataBlocks(boolean binaryStore) {
		ArrayList<PamDataBlock> streamingDataBlocks = new ArrayList<PamDataBlock>();
		PamDataBlock pamDataBlock;
		PamControlledUnit pcu;
		PamProcess pp;
		int nP;
		PamController pamController = PamController.getInstance();
		int nUnits = pamController.getNumControlledUnits();
		for (int i = 0; i < nUnits; i++) {
			pcu = pamController.getControlledUnit(i);
			nP = pcu.getNumPamProcesses();
			for (int j = 0; j < nP; j++) {
				pp = pcu.getPamProcess(j);
				int nDataBlocks = pp.getNumOutputDataBlocks();
				for (int d = 0; d < nDataBlocks; d++) {
					pamDataBlock = pp.getOutputDataBlock(d);
					if (pamDataBlock.getBinaryDataSource() != null ) {
						if (binaryStore == false || 
								(pamDataBlock.getBinaryDataSource().isDoBinaryStore() && pamDataBlock.getShouldBinary(null))) {
							streamingDataBlocks.add(pamDataBlock);
						}
					}
				}
			}
		}
		return streamingDataBlocks;
	}

	@Override
	public Serializable getSettingsReference() {
		return binaryStoreSettings;
	}

	@Override
	public long getSettingsVersion() {
		return BinaryStoreSettings.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return "Binary Store";
	}

	@Override
	public String getUnitType() {
		return unitType;
	}

	/**
	 * Get the name of the current storage folder and 
	 * check the folder exists, if necessary, creating it. 
	 * @param timeStamp current time in milliseconds. 
	 * @param addSeparator if true, add a path separator character to the
	 * end of the path. 
	 * @return true if folder exists (or has been created)
	 */
	public String getFolderName(long timeStamp, boolean addSeparator) {
		String fileSep = FileParts.getFileSeparator();
		String folderName = new String(binaryStoreSettings.getStoreLocation());
		if (binaryStoreSettings.datedSubFolders) {
			folderName += fileSep + PamCalendar.formatFileDate(timeStamp);

			// now check that that folder exists. 
			File folder = new File(folderName);
			if (folder.exists() == false) {
				folder.mkdirs();
				if (folder.exists() == false) {
					return null;
				}
			}
		}
		if (addSeparator) {
			folderName += fileSep;
		}
		return folderName;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		binaryStoreSettings = ((BinaryStoreSettings) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	@Override
	public JMenuItem createFileMenu(JFrame parentFrame) {
		JMenuItem m = new JMenuItem("Binary storage options...");
		m.addActionListener(new BinaryStorageOptions(parentFrame));
		return m;
	}

	class BinaryStorageOptions implements ActionListener {

		JFrame parentFrame;

		public BinaryStorageOptions(JFrame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			BinaryStoreSettings newSettings = BinaryStorageDialog.showDialog(parentFrame, binaryStoreSettings);
			if (newSettings != null) {
				boolean immediateChanges = binaryStoreSettings.isChanged(newSettings);
				binaryStoreSettings = newSettings.clone();
				/*
				 *  possible that storage location will have changed, so depending on mode, may have to close
				 *  and reopen some files. 
				 */
				if (immediateChanges) {
					if (storesOpen) {
						reOpenStores(BinaryFooter.END_UNKNOWN);
					}
				}
				
			}
		}
	}

	class NewFileTask extends TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}

	}


	@Override
	public boolean saveStartSettings(long timeNow) {
		return binarySettingsStorage.saveStartSettings(timeNow);
	}

	@Override
	public int getNumSettings() {
		return binarySettingsStorage.getNumSettings();
	}

	@Override
	public PamSettingsGroup getSettings(int settingsIndex) {
		return binarySettingsStorage.getSettings(settingsIndex);
	}

	@Override
	public String getSettingsSourceName() {
		return getUnitName();
	}

	/**
	 * @return the binaryStoreSettings
	 */
	public BinaryStoreSettings getBinaryStoreSettings() {
		return binaryStoreSettings;
	}

	/**
	 * @param binaryStoreSettings the binaryStoreSettings to set
	 */
	public void setBinaryStoreSettings(BinaryStoreSettings binaryStoreSettings) {
		this.binaryStoreSettings = binaryStoreSettings;
	}

	private void doInitialStoreChecks() {
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			makeInitialDataMap();
		}
//		else if (isNetRx) {
//			// act as though acquisition is about to start, i.e. open some stores. 
//			pamToStart(); 
//			binaryStoreProcess.pamStart();
//		}
	}

	private void makeInitialDataMap() {

		// this first operation should be fast enough that it doesn't
		// need rethreading. 

		BinaryStoreSettings newSettings = BinaryStorageDialog.showDialog(null, binaryStoreSettings);
		if (newSettings != null) {
			binaryStoreSettings = newSettings.clone();
		}
		binarySettingsStorage.makeSettingsMap();
		createOfflineDataMap(null);
	}

	/* (non-Javadoc)
	 * @see PamController.OfflineDataSource#createDataMap()
	 */
	@Override
	public void createOfflineDataMap(Window parentFrame) {
		if (!isViewer) {
			return;
		}
		/*
		 *  do this by opening a dialog in the middle of the frame
		 *  which is modal so nothing else can be executed by the 
		 *  operator. Then do the work in a SwingWorker, which sends 
		 *  updates to the dialog to display progress, then close the 
		 *  dialog. 
		 */

		BinaryDataMapMaker bdmm = new BinaryDataMapMaker(this);
		AWTScheduler.getInstance().scheduleTask(bdmm);

	}

	private BinaryMapMakingDialog binaryMapDialog;

	class BinaryDataMapMaker extends SwingWorker<Integer, BinaryMapMakeProgress> {

		private BinaryStore binaryStore;
		
		private BinaryStoreSettings viewerSettings;
		
		
		/**
		 * @param binaryStore
		 */
		public BinaryDataMapMaker(BinaryStore binaryStore) {
			super();
			this.binaryStore = binaryStore;
			viewerSettings = binaryStore.binaryStoreSettings;
		}


		@Override
		protected Integer doInBackground() {
			try {
				if (isViewer) {
//					for some reason, settings get reloaded before this gets to run which reverts it back to old settings. 
					binaryStore.binaryStoreSettings = viewerSettings;
				}
				ArrayList<PamDataBlock> streams = getStreamingDataBlocks(true);
				
				File serFile = new File(binaryStoreSettings.getStoreLocation() + FileParts.getFileSeparator() + "serialisedBinaryMap.data");

				createBinaryDataMaps(streams);

				publish(new BinaryMapMakeProgress(BinaryMapMakeProgress.STATUS_DESERIALIZING, null, 0, 0));
				dataMapSerialiser.loadDataMap(streams, serFile);

				BinaryHeaderAndFooter bhf;
				int nStreams = streams.size();
				if (nStreams == 0) {
					return null;
				}
				publish(new BinaryMapMakeProgress(BinaryMapMakeProgress.STATUS_COUNTING_FILES, null, 0, 0));
				
				List<File> fileList = listAllFiles();
				if (fileList == null) {
					return null;
				}
				int nFiles = fileList.size();
				int updateAt = Math.max(nFiles/100,1);
				int fileIndex;
				int nNew = 0;
				for (int i = 0; i < nFiles; i++) {
					if (i%updateAt == 0){
						publish(new BinaryMapMakeProgress(BinaryMapMakeProgress.STATUS_ANALYSING_FILES, 
								fileList.get(i).toString(), nFiles, i));
					}
					//				Thread.sleep(10);
					/*
					 * go through the real data files and check that each one is included in the map
					 */
					fileIndex = dataMapSerialiser.findFile(fileList.get(i));
					if (fileIndex >= 0) {
						// consider updating some information in the appropriate data map point in 
						// order to ensure the file has the correct path., 
						continue;
					}
					bhf = getFileHeaderAndFooter(fileList.get(i));
					createMapPoint(fileList.get(i), bhf, streams);
					nNew++;
				}

				sortBinaryDataMaps(streams);

				if (nNew > 0) {
					dataMapSerialiser.setHasChanges(true);
				}
				// don't save the datamap here - need to wait until the datagrams have been created too. 
				// always save in case the datagrams have changed. 
//					publish(new BinaryMapMakeProgress(BinaryMapMakeProgress.STATUS_SERIALIZING, null, 0, 0));
//					System.out.println(String.format("Added %d new files to data map. Total now %d", nNew, nFiles));
//					dataMapSerialiser.saveDataMaps(binaryStore, streams, serFile);
//				}

				publish(new BinaryMapMakeProgress(BinaryMapMakeProgress.STATUS_IDLE, "Done", nStreams, nStreams));

			}
			catch (Exception e){
				System.out.println("Error in BinaryDataMapMaker SwingWorker thread");
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void done() {
			System.out.println("BinaryDataMapMaker done " + this);
			super.done();
			if (binaryMapDialog != null) {
				binaryMapDialog.setVisible(false);
				binaryMapDialog = null;
			}
			PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_OFFLINE_DATASTORE);
			System.out.println("BinaryDataMapMaker really done " + this);
		}

		@Override
		protected void process(List<BinaryMapMakeProgress> chunks) {
			if (binaryMapDialog == null) {
				binaryMapDialog = BinaryMapMakingDialog.showDialog(getPamView().getGuiFrame());
			}
			super.process(chunks);
			for (int i = 0; i < chunks.size(); i++) {
				binaryMapDialog.setProgress(chunks.get(i));
			}
		}

	}


	/**
	 * Make a set of empty datamaps. One for each datablock
	 * @param streams list of data blocks to map
	 */
	private void createBinaryDataMaps(ArrayList<PamDataBlock> streams) {
		for (int i = 0; i < streams.size(); i++) {
			BinaryOfflineDataMap dm = new BinaryOfflineDataMap(this, streams.get(i));
			BinaryDataSource bds = streams.get(i).getBinaryDataSource();
			dm.setSpecialDrawing(bds.getSpecialDrawing());
			streams.get(i).addOfflineDataMap(dm);
		}
	}
	
	/**
	 * This should get called when the map maker has completed and will check the status of all 
	 * the datagrams. 
	 */
	private void checkDatagrams() {
		ArrayList<PamDataBlock> updateList = datagramManager.checkAllDatagrams();
		if (updateList != null && updateList.size() > 0) {
			dataMapSerialiser.setHasChanges(true);
			datagramManager.updateDatagrams(updateList);
		}
		/*
		 *  still can't save the serialised data here since the datatram Manager is going
		 *  to make the datagrams in a different thread so they won't be ready yet !  
		 */
		
		if (dataMapSerialiser.isHasChanges()) {
			AWTScheduler.getInstance().scheduleTask(new SaveDataMap());
		}
	}
	
	class SaveDataMap implements Runnable {
		public void run() {
//			System.out.println("Save changed serialised data map to " + dataMapSerialiser.getSerialisedFile().getAbsolutePath());
			dataMapSerialiser.saveDataMaps();
		}
	}

	private void sortBinaryDataMaps(ArrayList<PamDataBlock> streams) {
		OfflineDataMap dm;
		for (int i = 0; i < streams.size(); i++) {
			dm = streams.get(i).getOfflineDataMap(this);
			if (dm == null) {
				continue;
			}
			dm.sortMapPoints();
		}
	}

//	/**
//	 * Dump all the map data into a serialised file which can be easily read back 
//	 * in next time PAMGAURD starts. That way, it will only be necessary to 
//	 * unpack new data map points which should massively speed up data loading
//	 * for large data sets. 
//	 * @param streams
//	 */
//	public boolean serialiseDataMaps(ArrayList<PamDataBlock> streams) {
//		File mapFile = new File(binaryStoreSettings.storeLocation + FileParts.getFileSeparator() + "serialisedBinaryMap.data");
//		OutputStream os;
//		try {
//			os = new FileOutputStream(mapFile);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			return false;
//		}
//		try {
//			ObjectOutputStream oos = new ObjectOutputStream(os);
//			OfflineDataMap dm;
//			Object o;
//			for (int i = 0; i < streams.size(); i++) {
//				dm = streams.get(i).getOfflineDataMap(this);
//				if (dm == null) {
//					continue;
//				}
//				oos.writeObject(streams.get(i).getDataName());
////				o = ((BinaryOfflineDataMapPoint) dm.getMapPoints().get(0)).getBinaryHeader();
////				o = dm.getMapPoints();
//				oos.writeObject(dm.getMapPoints());
//			}
//			oos.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}


	/**
	 * List all the files in the binary storage folder
	 * @return List of files. 
	 */
	public List<File> listAllFiles() {
		ArrayList<File> fileList = new ArrayList<File>();
		String root = binaryStoreSettings.getStoreLocation();
		if (root == null) {
			return null;
		}
		File rootFolder = new File(root);
		PamFileFilter binaryDataFilter = new PamFileFilter("Binary Data Files", fileType);
		binaryDataFilter.setAcceptFolders(true);

		listDataFiles(fileList, rootFolder, binaryDataFilter);

		return fileList;
	}

	/**
	 * Run checks on each file. 
	 * @param file file to check. 
	 */
	public BinaryHeaderAndFooter getFileHeaderAndFooter(File file) {
		File indexFile = findIndexFile(file, true);
		BinaryHeaderAndFooter bhf = null;
		if (indexFile != null) {
			bhf = readHeadAndFoot(indexFile);
		}
		if (bhf == null || bhf.binaryHeader == null || bhf.binaryFooter == null) {
			bhf = readHeadAndFoot(file);
		}


		return bhf;
	}

	String lastFailedStream = null;

	/**
	 * Create a data map point and add it to the map. 
	 * <p>
	 * First the correct data stream (PamDataBlock) must be located
	 * then the correct map within that stream. All this searching is
	 * necessary since the files will not be read in order so we've no 
	 * idea up until now which stream it's going to be. 
	 * @param bhf binary header and footer. 
	 * @param streams list of data streams
	 * @return true if stream and map found and map point added. 
	 */
	public boolean createMapPoint(File aFile, BinaryHeaderAndFooter bhf,
			ArrayList<PamDataBlock> streams) {
		if (bhf.binaryHeader == null) {
			return false;
		}
		PamDataBlock parentStream = findDataStream(bhf.binaryHeader, streams);
		if (parentStream == null) {
			if (lastFailedStream == null || lastFailedStream.equals(bhf.binaryHeader.getModuleName()) == false) {
				System.out.println(String.format("No internal data stream for %s %s %s",
						bhf.binaryHeader.getModuleType(), bhf.binaryHeader.getModuleName(), 
						bhf.binaryHeader.getStreamName()));
				lastFailedStream = bhf.binaryHeader.getModuleName();
			}
			return false;
		}
		BinaryDataSource binaryDataSouce = parentStream.getBinaryDataSource();
		BinaryOfflineDataMap dm = (BinaryOfflineDataMap) parentStream.getOfflineDataMap(this);
		if (dm == null) {
			System.out.println(String.format("Cannot locate binary offline data map for %s %s %s",
					bhf.binaryHeader.getModuleType(), bhf.binaryHeader.getModuleName(), 
					bhf.binaryHeader.getStreamName()));
			return false;
		}
		ModuleHeader mh = null;
		ModuleFooter mf = null;
		if (bhf.moduleHeaderData != null) {
			mh = binaryDataSouce.sinkModuleHeader(bhf.moduleHeaderData, bhf.binaryHeader);
		}
		if (bhf.moduleFooterData != null) {
			mf = binaryDataSouce.sinkModuleFooter(bhf.moduleFooterData, bhf.binaryHeader, mh);
		}
		BinaryOfflineDataMapPoint mapPoint = new BinaryOfflineDataMapPoint(this,aFile, bhf.binaryHeader,
				bhf.binaryFooter, mh, mf, bhf.datagram);
		if (bhf.binaryFooter == null) {
			mapPoint.setEndTime(bhf.lastDataTime);
			mapPoint.setNDatas(bhf.nDatas);
		}
		dm.addDataPoint(mapPoint);

		return true;
	}

	/**
	 * Find the datastream to go with a particular file header. 
	 * @param binaryHeader binary header read in from a file
	 * @param streams Array list of available data streams
	 * @return single stream which matches the header. 
	 */
	private PamDataBlock findDataStream(BinaryHeader binaryHeader,
			ArrayList<PamDataBlock> streams) {
		int nStreams = streams.size();
		PamDataBlock aBlock;
		BinaryDataSource binaryDataSource;
		for (int i = 0; i < nStreams; i++) {
			aBlock = streams.get(i);
			binaryDataSource = aBlock.getBinaryDataSource();
			if (binaryDataSource.getModuleName().equalsIgnoreCase(binaryHeader.getModuleName()) &&
					binaryDataSource.getModuleType().equals(binaryHeader.getModuleType()) &&
					binaryDataSource.getStreamName().equals(binaryHeader.getStreamName())){
				return aBlock;
			}
		}
		return null;
	}

	/**
	 * Read the header and footer from a binary file. 
	 * @param file
	 * @return header and footer packed up together. 
	 */
	private BinaryHeaderAndFooter readHeadAndFoot(File file) {
		/**
		 * reading in is a pain since you don't know what anything is until
		 * you've read the first two words. 
		 * Read everything into byte buffers, then can skip easily if
		 * necessary or interpret if it's interesting. 
		 */		
		BinaryHeaderAndFooter bhf = new BinaryHeaderAndFooter();
		int objectLength;
		int objectType;
		byte[] dataBytes;
		DataInputStream fileStream = null;
		try {
			fileStream = new DataInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		BinaryHeader bh = new BinaryHeader();
		if (file.getAbsolutePath().equals("C:\\CromartyEars\\BinaryData\\Deployment06\\20100729\\Click_Detector_Click_Detector_Clicks_20100729_100041.pgdx")) {
			System.out.println(file.getAbsolutePath());
		}
		if (bh.readHeader(fileStream)) {
			bhf.binaryHeader = bh;
		}
		else {
			reportError("No valid header in file " + file.getAbsolutePath());
		}
		bhf.lastDataTime = bh.getDataDate();

		BinaryFooter bf = new BinaryFooter();
//		bhf.binaryFooter = bf;
		byte[] byteData;
		int objectDataLength = 0;
		int moduleVersion = 0;
		try {
			while (true) {
				objectLength = fileStream.readInt();
				objectType = fileStream.readInt();
				if (objectType == BinaryTypes.FILE_FOOTER) {
					if (bf.readFooterData(fileStream)) {
						bhf.binaryFooter = bf;
					}
				}
				else if (objectType == BinaryTypes.MODULE_HEADER) {
					moduleVersion = fileStream.readInt();
					objectDataLength = fileStream.readInt();
					if (objectDataLength > 0) {
						byteData = new byte[objectDataLength];
						fileStream.read(byteData);
						bhf.moduleHeaderData = new BinaryObjectData(objectType, byteData, objectDataLength);
					}
				}
				else if (objectType == BinaryTypes.MODULE_FOOTER) {
					objectDataLength = fileStream.readInt();
					if (objectDataLength > 0) {
						byteData = new byte[objectDataLength];
						fileStream.read(byteData);
						bhf.moduleFooterData = new BinaryObjectData(objectType, byteData, objectDataLength);
					}
				}
				else if (objectType == BinaryTypes.DATAGRAM) {
					bhf.datagram = new Datagram(0);
					bhf.datagram.readDatagramData(fileStream, objectLength);
				}
				else {
					bhf.lastDataTime = fileStream.readLong();
					fileStream.skipBytes(objectLength-8-8);
					bhf.nDatas++;
				}
			}
		} 
		catch (EOFException eof) {

		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (bhf.binaryFooter == null) {
			bf.setnObjects(bhf.nDatas);
			bf.setDataDate(bhf.lastDataTime);
			bf.setAnalysisDate(System.currentTimeMillis());
			bf.setFileEndReason(BinaryFooter.END_CRASHED);
			String errStr = String.format("No valid file footer in %s. Creating default with %d objects end time %s", 
					file.getName(), bf.getNObjects(), PamCalendar.formatDateTime(bf.getDataDate()));
			System.out.println(errStr);
			bhf.binaryFooter = bf;
		}
		return bhf;
	}

	/**
	 * Find the index file to match a given data file. 
	 * @param dataFile data file. 
	 * @param checkExists check teh file exists and if it doens't return null
	 * @return index file to go with the data file. 
	 */
	public File findIndexFile(File dataFile, boolean checkExists) {
		String filePath = dataFile.getAbsolutePath();
		// check that the last 4 characters are "pgdf"
		int pathLen = filePath.length();
		String newPath = filePath.substring(0, pathLen-4) + indexFileType;
		File newFile = new File(newPath);
		if (checkExists) {
			if (newFile.exists() == false) {
				return null;
			}
		}
		return newFile;
	}
	
	/**
	 * rewrite the index file. 
	 * @param dmp 
	 * @param dataBlock 
	 */
	public boolean rewriteIndexFile(PamDataBlock dataBlock, BinaryOfflineDataMapPoint dmp) {
		/*
		 * First work out the index file name from the binaryFile;
		 */
		BinaryDataSource binaryDataSource = dataBlock.getBinaryDataSource();
		if (binaryDataSource == null) {
			return false;
		}
		BinaryOutputStream outputStream = new BinaryOutputStream(this, dataBlock);
		
		String fileName = dmp.getBinaryFile(this).getAbsolutePath();
		String endBit = fileName.substring(fileName.length()-4);
		String indexName = fileName.substring(0, fileName.length()-4) + "pgdx";
//		System.out.println(fileName + " change to  " + indexName);
		File indexFile = new File(indexName);
		

		BinaryHeader header = dmp.getBinaryHeader();
		BinaryFooter footer = dmp.getBinaryFooter();
		ModuleHeader moduleHeader = dmp.getModuleHeader();
		ModuleFooter moduleFooter = dmp.getModuleFooter();
		Datagram datagram = dmp.getDatagram();
		
		if (header == null) {
			return false; 
			// can't do anything if there isn't at least a header file !
		}

		outputStream.openFile(indexFile);
		outputStream.writeHeader(header.getDataDate(), header.getAnalysisDate());
		
		/*
		 * Always write a module header. 
		 */
		outputStream.writeModuleHeader();

		if (moduleFooter != null) {
			outputStream.writeModuleFooter(moduleFooter);
		}
		
		if (datagram != null) {
			outputStream.writeDatagram(datagram);
		}
		
		/**
		 * If the file crashed, then there is a strong possibility that the 
		 * footer won't exist, so make one and flag it as crashed,
		 */
		if (footer == null) {
			footer = new BinaryFooter(dmp.getEndTime(), System.currentTimeMillis(), 
					dmp.getNDatas(), outputStream.getFileSize());
			footer.setFileEndReason(BinaryFooter.END_CRASHED);
		}
		outputStream.writeFileFooter(footer);

		outputStream.closeFile();

		return true;
	}


	/**
	 * List all data files - get's called recursively
	 * @param fileList current fiel list - get's added to
	 * @param folder folder to search
	 * @param filter file filter
	 */
	private void listDataFiles(ArrayList<File> fileList, File folder, PamFileFilter filter) {
		File[] newFiles = folder.listFiles(filter);
		if (newFiles == null) {
			return;
		}
		for (int i = 0; i < newFiles.length; i++) {
			if (newFiles[i].isFile()) {
				fileList.add(newFiles[i]);
			}
			else if (newFiles[i].isDirectory()) {
				listDataFiles(fileList, newFiles[i].getAbsoluteFile(), filter);
			}
		}
	}

	@Override
	public String getDataSourceName() {
		return getUnitName();
	}

	@Override
	public boolean loadData(PamDataBlock dataBlock, long dataStart, 
			long dataEnd, RequestCancellationObject cancellationObject, ViewLoadObserver loadObserver) {
		/**
		 * First use the map to identify a list of files which will have data 
		 * between these times. 
		 */
		BinaryOfflineDataMap dataMap = (BinaryOfflineDataMap) dataBlock.getOfflineDataMap(this);
		if (dataMap == null) {
			return false;
		}
		ArrayList<BinaryOfflineDataMapPoint> mapPoints = dataMap.getFileList(dataStart, dataEnd);
		if (mapPoints.size() == 0) {
//			System.out.println("No data to load from " + dataBlock.getDataName() + " between " +
//					PamCalendar.formatDateTime(dataStart) + " and " + PamCalendar.formatDateTime(dataEnd));
			return true;
		}
		else {
			//			System.out.println(String.format("Loading data for %s from %d files", dataBlock.getDataName(),
			//					mapPoints.size()));
			for (int i = 0; i < mapPoints.size(); i++) {
				//				System.out.println(mapPoints.get(i).getBinaryFile().getPath());
				if (loadData(dataBlock, mapPoints.get(i), dataStart, dataEnd) == false) {
					break;
				}
			}
		}
		dataBlock.sortData();

		return true;
	}

	/**
	 * Load the data from a single file.
	 * <p>
	 * Generally, PAMGUARD will use the above function which loads between
	 * two times, but for some offline analysis tasks, it's convenient
	 * to scroll through file at a time in which case this function
	 * can be used. 
	 * @param dataBlock PamDataBlock to receive the data
	 * @param binaryOfflineDataMapPoint data map point
	 * @param dataStart data start
	 * @param dataEnd data end
	 * @return true if data were loaded. false if out of memory in which case
	 * the calling load function will drop out of the loop over files. 
	 */
	public boolean loadData(PamDataBlock dataBlock, BinaryOfflineDataMapPoint mapPoint,
			long dataStart, long dataEnd) {

		BinaryHeaderAndFooter bhf = new BinaryHeaderAndFooter();
		BinaryInputStream inputStream = new BinaryInputStream(this, dataBlock);
		if (inputStream.openFile(mapPoint.getBinaryFile(this)) == false) {
			return false;
		}
		BinaryDataSource binarySource = dataBlock.getBinaryDataSource();
		BinaryHeader bh = inputStream.readHeader();
		BinaryFooter bf = null;
		ModuleHeader mh = null;
		ModuleFooter mf = null;
		long objectTime;
		PamDataUnit createdUnit;
		int moduleVersion = 0;

//				System.out.println(String.format("Loading data from file %s", mapPoint.getBinaryFile().getAbsolutePath()));

		if (bh == null) {
			return false;
		}
		BinaryObjectData binaryObjectData;
		while ((binaryObjectData = inputStream.readNextObject()) != null) {
			//			if (binaryObjectData.getObjectNumber() == 371){
			//				System.out.println(binaryObjectData.toString());
			//			}
			switch(binaryObjectData.getObjectType()){
			case  BinaryTypes.MODULE_HEADER:
				mh = binarySource.sinkModuleHeader(binaryObjectData, bh);
				moduleVersion = binaryObjectData.getVersionNumber();
				break;
			case  BinaryTypes.MODULE_FOOTER:
				mf = binarySource.sinkModuleFooter(binaryObjectData, bh, mh);
				break;
			default:
				objectTime = binaryObjectData.getTimeMillis();
				if (objectTime < dataStart) {
					continue;
				}
				if (objectTime > dataEnd) {
					//					System.out.println("Break on object at " +
					//							PamCalendar.formatDateTime(objectTime));
					break;
				}
				createdUnit = binarySource.sinkData(binaryObjectData, bh, moduleVersion);
				if (createdUnit != null) {
					createdUnit.setDataUnitFileInformation(
							new DataUnitFileInformation(mapPoint.getBinaryFile(this), binaryObjectData.getObjectNumber()));
					dataBlock.addPamData(createdUnit);
				}
				if (checkMemory(dataBlock) == false) {
					return false;
				}
			}
		}
		bf = inputStream.getBinaryFooter();
		inputStream.closeFile();


		return true;
	}

	/**
	 * Saves data in a PamDataBlock. 
	 * <p>
	 * First scans all data in the pamDataBlock and works out which 
	 * files actually need updating based on info in their DataUnitFileInformation
	 * then rewrites those files.
	 * <p>
	 * Note that data from many files may be in memory and it's also possible 
	 * that files are only partially read in, in which case, it will be 
	 * necessary to partially take data from the old file and partially 
	 * from the stuff in memory !
	 * @param pamDataBlock data block holding the data. 
	 * @return true if saved Ok. 
	 */
	@Override
	public boolean saveData(PamDataBlock pamDataBlock) {
		ArrayList<File> changedFileList = createChangedFileList(pamDataBlock);
		for (int i = 0; i < changedFileList.size(); i++) {
			if (saveData(changedFileList.get(i), pamDataBlock) == false) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Rewrite a specific file using data from pamDataBlock.
	 * <p> N.B. Not all data from the original file will have 
	 * been read in, so it will be necessary to take data both from
	 * the original file and from the PamDataBlock
	 * @param file file to recreate
	 * @param pamDataBlock 
	 * @return true if file recreated OK, false otherwise. 
	 */
	private boolean saveData(File file, PamDataBlock pamDataBlock) {

		BinaryDataSource binarySource = pamDataBlock.getBinaryDataSource();
		BinaryOutputStream outputStream = new BinaryOutputStream(this, pamDataBlock);
		binarySource.setBinaryStorageStream(outputStream);
		File tempFile = new File(file.getAbsolutePath() + ".tmp");

		BinaryInputStream inputStream = new BinaryInputStream(this, pamDataBlock);

		BinaryHeader binaryHeader;
		BinaryFooter binaryFooter;
		ModuleHeader mh = null;
		ModuleFooter mf = null;
		BinaryObjectData binaryObjectData;
		int oldModuleVersion = binarySource.getModuleVersion();

		if (outputStream.openFile(tempFile) ==  false) {
			return reportError("Unable to open temp output file for rewriting " + 
					tempFile.getAbsolutePath()); 
		}

		if (inputStream.openFile(file) == false) {
			return reportError("Unable to open data file for rewriting " + 
					file.getAbsolutePath()); 
		}
		/**
		 * Read the header from the main file, then write to the temp file. 
		 * The appropriate module will handle the Module Header since this
		 * may change. 
		 */
		binaryHeader = inputStream.readHeader();
		if (binaryHeader == null) {
			return reportError("Unable to read header from " +
					file.getAbsolutePath());
		}
		outputStream.writeHeader(binaryHeader.getDataDate(), binaryHeader.getAnalysisDate());
		//		ModuleHeader mh = 
		byte[] moduleHeaderData = binarySource.getModuleHeaderData();
		outputStream.writeModuleHeader(moduleHeaderData);

		/**
		 * Now comes the clever bit where we have to read from the file, 
		 * but stay in synch with units which are in storage and 
		 * if any of the stored unit are flagged as changed, then 
		 * use their data instead of the data we just read from the 
		 * original file. 
		 * <p>
		 * Even if data is being taken from file, convert back into a 
		 * data unit so that the binary source can know about everything
		 * being written into the file in order to create the correct
		 * module footer. This is also extrememly important if module 
		 * data formats change - we're assuming that data will always
		 * be written in the absolutely latest format. 
		 */
		StoredUnitServer storedUnitServer = new StoredUnitServer(file, pamDataBlock);
		PamDataUnit aDataUnit;
		while((binaryObjectData = inputStream.readNextObject()) != null) {
			switch(binaryObjectData.getObjectType()) {
			case BinaryTypes.FILE_HEADER:
				break;
			case BinaryTypes.FILE_FOOTER:
				break;
			case BinaryTypes.MODULE_HEADER:
				mh = binarySource.sinkModuleHeader(binaryObjectData, binaryHeader);
				oldModuleVersion = binaryObjectData.getVersionNumber();
				break;
			case BinaryTypes.MODULE_FOOTER:
				break;
			default:// it's data !
				aDataUnit = storedUnitServer.findStoredUnit(binaryObjectData);
				if (aDataUnit != null) {
					binarySource.saveData(aDataUnit);
					aDataUnit.getDataUnitFileInformation().setNeedsUpdate(false);
				}
				else {
					aDataUnit = binarySource.sinkData(binaryObjectData, binaryHeader, oldModuleVersion);
					binarySource.saveData(aDataUnit);
					//					outputStream.storeData(binaryObjectData);
				}

			}
		}

		byte[] moduleFooterData = binarySource.getModuleFooterData();
		outputStream.writeModuleFooter(moduleFooterData);

		binaryFooter = inputStream.getBinaryFooter();
		if (binaryFooter == null) {
			outputStream.writeFooter(inputStream.getLastObjectTime(), binaryHeader.getAnalysisDate(),
					BinaryFooter.END_CRASHED);
		}
		else {
			outputStream.writeFooter(binaryFooter.getDataDate(), binaryFooter.getAnalysisDate(), 
					binaryFooter.getFileEndReason());
		}
		outputStream.closeFile();
		inputStream.closeFile();

		/*
		 * Now file final stage - copy the temp file in place of the 
		 * original file. 
		 */
		boolean deletedOld = false;
		try {
			deletedOld = file.delete();
		}
		catch (SecurityException e) {
			System.out.println("Error deleting old psf file: " + file.getAbsolutePath());
			e.printStackTrace();
		}

		boolean renamedNew = false;
		try {
			renamedNew = tempFile.renameTo(file);
		}
		catch (SecurityException e) {
			System.out.println("Error renaming new psf file: " + tempFile.getAbsolutePath() + 
					" to " + file.getAbsolutePath());
			e.printStackTrace();
		}
		if (renamedNew == false) {
			if (deletedOld == false) {
				reportError("Unable to delete " + file.getAbsolutePath());
			}
			return reportError(String.format("Unable to rename %s to %s", 
					tempFile.getAbsolutePath(), file.getAbsolutePath()));
		}

		/*
		 * Finally, write a new index file. 
		 */
		File indexFile = findIndexFile(file, false);
		outputStream.createIndexFile(indexFile);

		/*
		 * And try to find and update the data map point
		 * Will need to make a module header in order to do this. 
		 */
		BinaryObjectData moduleData;
		if (moduleHeaderData != null) {
			moduleData = new BinaryObjectData(BinaryTypes.MODULE_HEADER,
					moduleHeaderData, moduleHeaderData.length);
			moduleData.setVersionNumber(binarySource.getModuleVersion());
			mh = binarySource.sinkModuleHeader(moduleData, binaryHeader);
		}

		if (moduleFooterData != null) {
			moduleData = new BinaryObjectData(BinaryTypes.MODULE_FOOTER,
					moduleFooterData, moduleFooterData.length);
			mf = binarySource.sinkModuleFooter(moduleData, binaryHeader, mh);
		}

		BinaryOfflineDataMapPoint mapPoint = findMapPoint(pamDataBlock, file);
		if (mapPoint != null) {
			Datagram oldDataGram = mapPoint.getDatagram();
			mapPoint.update(this,file, binaryHeader, binaryFooter, mh, mf, oldDataGram);
			dataMapSerialiser.setHasChanges(true);
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#pamClose()
	 */
	@Override
	public void pamClose() {
		super.pamClose();
		/**
		 * Save the datamap which may have changed as a result of offline processing. 
		 */
		if (dataMapSerialiser != null && dataMapSerialiser.isHasChanges()) {
			System.out.println("Save serialised binary data map");
			dataMapSerialiser.saveDataMaps();
		}
	}

	/**
	 * Find a data map point for a specific file. 
	 */
	private BinaryOfflineDataMapPoint findMapPoint(PamDataBlock pamDataBlock, File file) {
		BinaryOfflineDataMap dataMap = (BinaryOfflineDataMap) pamDataBlock.getOfflineDataMap(this);
		if (dataMap == null) {
			return null;
		}
		return dataMap.findMapPoint(file);
	}

	/**
	 * Class to serve up the next data unit 
	 * that needs to be stored from a particular file.
	 * @author Doug Gillespie
	 *
	 */
	class StoredUnitServer {
		private PamDataBlock pamDataBlock;
		private File file;
		private ListIterator<PamDataUnit> dataUnitIterator;
		private PamDataUnit currentUnit = null;

		public StoredUnitServer(File file, PamDataBlock pamDataBlock) {
			super();
			this.file = file;
			this.pamDataBlock = pamDataBlock;
			dataUnitIterator = pamDataBlock.getListIterator(0);
			if (dataUnitIterator.hasNext()) {
				currentUnit = dataUnitIterator.next();
			}
		}
		/**
		 * Finds a data unit which needs storing. 
		 * @param binaryObjectData
		 * @return
		 */
		public PamDataUnit findStoredUnit(BinaryObjectData binaryObjectData) {
			if (currentUnit == null) {
//				return null;
				/**
				 * Need to reset this. When loading data which didn't start at the beginning 
				 * of a file, then when it comes to resave that file, the first clicks 
				 * (before the load period) will not be in memory, so this iterator will run to 
				 * it's end and currentUnit will be null. As a consequence, subsequent clicks never
				 * get found either if this returns here as it did in previous versions.  
				 */
				dataUnitIterator = pamDataBlock.getListIterator(0);
				if (dataUnitIterator.hasNext()) {
					currentUnit = dataUnitIterator.next();
				}
			}
			DataUnitFileInformation fileInfo;
			PamDataUnit foundUnit = null;
			while (currentUnit != null) {
				fileInfo = currentUnit.getDataUnitFileInformation();
				if (fileInfo.getIndexInFile() == binaryObjectData.getObjectNumber() &&
						fileInfo.getFile() != null && fileInfo.getFile().equals(file)) {
					if (currentUnit.getTimeMilliseconds() != binaryObjectData.getTimeMillis()) {
						System.out.println("Non matching time on stored object");
						return null;
					}
					foundUnit = currentUnit;
				}
				if (dataUnitIterator.hasNext()) {
					currentUnit = dataUnitIterator.next();
				}
				else {
					currentUnit = null;
				}
				if (foundUnit != null) {
					if (foundUnit.getDataUnitFileInformation().isNeedsUpdate()) {
						return foundUnit;
					}
					else {
						return null;
					}
				}
			}
			return null;
		}

	}

	private boolean reportError(String string) {
		System.out.println(string);
		return false;
	}

	/**
	 * Create a list of files which have data in that datablock 
	 * that has been flagged as changed. 
	 * @param pamDataBlock
	 * @return ArrayList of files that have changed data 
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<File> createChangedFileList(PamDataBlock pamDataBlock) {
		ArrayList<File> changedFiles = new ArrayList<File>();
		/*
		 *  go through the data in the data block and see which have 
		 *  data that has been flagged as changed.  
		 */
		synchronized (pamDataBlock) {
			ListIterator<PamDataUnit> iterator = pamDataBlock.getListIterator(0);
			PamDataUnit aDataUnit;
			DataUnitFileInformation fileInfo;
			File lastFile = null;
			File aFile;
			while (iterator.hasNext()) {
				aDataUnit = iterator.next();
				fileInfo = aDataUnit.getDataUnitFileInformation();
				if (fileInfo == null) {
					continue;
				}
				if (fileInfo.isNeedsUpdate() == false) {
					continue;
				}
				aFile = fileInfo.getFile();
				if (aFile == lastFile) {
					continue;
				}
				if (changedFiles.indexOf(aFile) >= 0) {
					continue;
				}
				/*
				 * If it gets here it's a new file for our list. 
				 */
				lastFile = aFile;
				changedFiles.add(aFile);
			}
		}
		return changedFiles;
	}

	/**
	 * Check available memory. 
	 * If it's less than 10 megabytes, return false since we're about to 
	 * run out. 
	 * @return true if there is > 1o Meg of memory left. 
	 */
	private boolean checkMemory(PamDataBlock dataBlock) {
		return checkMemory(dataBlock, 10000000L); // standard to test to check still have 10MBytes. 
	}

	private boolean checkMemory(PamDataBlock dataBlock, long minAmount) {
		Runtime r = Runtime.getRuntime();
		long totalMemory = r.totalMemory();
		long maxMemory = r.maxMemory();
		long freeMemory = r.freeMemory();
		if (freeMemory > minAmount) {
			return true;
		}
		else if (freeMemory + totalMemory < maxMemory - minAmount) {
			return true;
		}
		// run the garbage collector and try again ...
		r.gc();
		
		totalMemory = r.totalMemory();
		maxMemory = r.maxMemory();
		freeMemory = r.freeMemory();
		if (freeMemory > minAmount) {
			return true;
		}
		else if (freeMemory + totalMemory < maxMemory - minAmount) {
			return true;
		}
		
		// not enoughmemory, so throw a warning. 
		JOptionPane.showMessageDialog(null, "System memory is getting low and no more " +
				"\ndata can be loaded. Select a shorter load time for offline data", 
				dataBlock.getDataName(), JOptionPane.ERROR_MESSAGE);

		return false;
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("storeLocation", binaryStoreSettings.getStoreLocation());
		paramsEl.setAttribute("datedSubFolders", new Boolean(binaryStoreSettings.datedSubFolders).toString());
		paramsEl.setAttribute("autoNewFiles", new Boolean(binaryStoreSettings.autoNewFiles).toString());
		paramsEl.setAttribute("fileSeconds", String.format("%d", binaryStoreSettings.fileSeconds));
		paramsEl.setAttribute("limitFileSize", new Boolean(binaryStoreSettings.limitFileSize).toString());
		paramsEl.setAttribute("maxFileSize", String.format("%d", binaryStoreSettings.maxFileSize));

		return true;
	}

	/**
	 * @return the datagramManager
	 */
	public DatagramManager getDatagramManager() {
		return datagramManager;
	}

}
