package networkTransfer.receive;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.JMenuItem;

import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.BinaryStore;

import Array.ArrayManager;
import Array.Hydrophone;
import Array.PamArray;
import GPS.GPSParameters;
import GPS.GpsData;
import GPS.GpsDataUnit;
import Localiser.bearingLocaliser.BearingLocaliser;
import Localiser.bearingLocaliser.BearingLocaliserSelector;
import NMEA.NMEADataBlock;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.AcousticDataUnit;
import PamUtils.PamCalendar;
import PamView.PamSidePanel;
import PamView.PamTabPanel;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;

/**
 * Receive near real time data over the network in the form of packaged PamDataUnits. 
 * @author Doug Gillespie
 *
 */
public class NetworkReceiver extends PamControlledUnit implements PamSettings {

	public short dataVersion;

	private ArrayList<PamDataBlock> rxDataBlocks;

	private List<ReceiveThread> receiveThreads;

	//	private Vector<BuoyRXStats> buoyRXStats = new Vector<BuoyRXStats>();

	private int[] quickBlockIds;

	private NetworkReceiveParams networkReceiveParams = new NetworkReceiveParams();

	private NetworkRXTabPanel networkRXTabPanel;

	private NetworkReceiveProcess networkReceiveProcess;

	public NetworkReceiveProcess getNetworkReceiveProcess() {
		return networkReceiveProcess;
	}

	private static final int HEADID = 0xFFEEDDCC;

	public static String unitTypeString = "Network Receiver";

	private BuoyStatusDataBlock buoyStatusDataBlock;

	private ConnectionThread connectionThread;

	private NetworkReceiveSidePanel networkReceiveSidePanel;

	private int recentPackets, recentDataBytes;

	/**
	 * @return the buoyStatusDataBlock
	 */
	public BuoyStatusDataBlock getBuoyStatusDataBlock() {
		return buoyStatusDataBlock;
	}

	/**
	 * Flags for dataType1 - these must match equivalent commands in 
	 * other network C++ code, so don't mess with them !
	 */
	public static final int  NET_PAM_DATA       = 1;
	public static final int  NET_REMOTE_COMMAND = 2;
	public static final int  NET_SPEED_DATA     = 3;
	public static final int  NET_SYSTEM_DATA    = 4;
	public static final int  NET_PAM_COMMAND    = 5;

	public static final int  SYSTEM_GPSDATA     = 1;
	public static final int  SYSTEM_COMPASSDATA = 16; 

	/**
	 * Some very basic commands to send straight through to receiving stations
	 * @param unitName
	 */
	public static final int NET_PAM_COMMAND_STOP = 0;
	public static final int NET_PAM_COMMAND_PREPARE = 1;
	public static final int NET_PAM_COMMAND_START = 2;

	private static final String COMPASSID = "HMC";

	public NetworkReceiver(String unitName) {
		super(unitTypeString, unitName);

		PamSettingManager.getInstance().registerSettings(this);

		networkReceiveProcess = new NetworkReceiveProcess(this);
		addPamProcess(networkReceiveProcess);

		receiveThreads = new Vector<ReceiveThread>();

		networkRXTabPanel = new NetworkRXTabPanel(this);

		networkReceiveSidePanel = new NetworkReceiveSidePanel(this);
	}

	@Override
	public PamTabPanel getTabPanel() {
		return networkRXTabPanel;
	}


	@Override
	public PamSidePanel getSidePanel() {
		return networkReceiveSidePanel;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			findDataSources();
			networkRXTabPanel.notifyModelChanged(changeType);
			startConnectionThread();
		}
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " Settings ...");
		menuItem.addActionListener(new DetectionMenu(parentFrame));
		return menuItem;
	}

	private class DetectionMenu implements ActionListener {
		private Frame parentFrame;

		public DetectionMenu(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			detectionDialog(parentFrame);
		}

	}

	/**
	 * Display the network dialog and act if things change.
	 * @param parentFrame parent frame to own dialog
	 */
	public void detectionDialog(Frame parentFrame) {
		NetworkReceiveParams newParams = NetworkReceiveDialog.showDialog(parentFrame, this);
		if (newParams != null) {
			networkReceiveParams = newParams.clone();
			newParams();
		}
	}

	/**
	 * Called when parameters change
	 */
	private void newParams() {
		if (connectionThread == null) {
			startConnectionThread();
			return;
		}
		if (connectionThread.currentParams.receivePort == networkReceiveParams.receivePort) {
			return; // no need to restart if port number is the same. 
		}
		/*
		 * Now need to stop the existing connection thread and restart with the new port number
		 */
		connectionThread.stopConnectionThread();
		/*
		 * And all the client threads too !
		 */
		ListIterator<ReceiveThread> rxIt = receiveThreads.listIterator();
		ReceiveThread rxThread;
		while (rxIt.hasNext()) {
			rxThread = rxIt.next();
			rxIt.remove();
			rxThread.stopThread();
		}
		/*
		 * Then start a new connection thread. 
		 */
		startConnectionThread();
	}

	private void startConnectionThread() {
		if (PamController.getInstance().getRunMode() != PamController.RUN_NETWORKRECEIVER) {
			return;
		}
		connectionThread = new ConnectionThread(networkReceiveParams);
		Thread t = new Thread(connectionThread);
		t.start();		
	}

	/**
	 * Find all data sources / sinks which may be about to use data from the network. 
	 */
	private void findDataSources() {
		rxDataBlocks = BinaryStore.getStreamingDataBlocks(false);
		quickBlockIds = new int[rxDataBlocks.size()];
		for (int i = 0; i < rxDataBlocks.size(); i++) {
			quickBlockIds[i] = rxDataBlocks.get(i).getQuickId();
		}
		//		initRXStats();
	}

	private int findStreamDataBlock(int quickId) {
		if (quickBlockIds == null) {
			return -1;
		}
		for (int i = 0; i < quickBlockIds.length; i++) {
			if (quickBlockIds[i] == quickId) {
				return i;
			}
		}
		return -1;
	}


	//	/**
	//	 * Check the array has enough hydrophones.
	//	 * If it hasn't, then make them ! 
	//	 * @param channel
	//	 */
	//	private void checkArrayChannel(int channel) {
	//		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
	//		ArrayList<Hydrophone> phones = array.getHydrophoneArray();
	//		Hydrophone newPhone;
	//		Hydrophone anyPhone = array.getHydrophone(0);
	//		if (anyPhone == null) {
	//			double[] bw = {0., 500000.};
	//			anyPhone = new Hydrophone(0, 0, 0, 0, "Made up", -170, bw, 0);
	//		}
	//		while (phones.size() < channel+1) {
	//			newPhone = new Hydrophone(phones.size(), channel*10., channel*10., anyPhone.getDepth(), 
	//					anyPhone.getType(), anyPhone.getSensitivity(), anyPhone.getBandwidth(), anyPhone.getPreampGain());
	//			array.addHydrophone(newPhone);
	//		}
	//	}

	/**
	 * Called whenever the command status of a buoy changes. 
	 * May require us to open or close data files, etc. 
	 * @param changeTime 
	 * @param bsdu
	 */
	private synchronized void commandStateChanged(long changeTime, BuoyStatusDataUnit bsdu) {
		int nStopped = 0;
		int nPrepared = 0;
		int nStarted = 0;
		ListIterator<BuoyStatusDataUnit> it = buoyStatusDataBlock.getListIterator(0);
		BuoyStatusDataUnit b;
		while (it.hasNext()) {
			b = it.next();
			switch(b.getCommandStatus()) {
			case NET_PAM_COMMAND_PREPARE:
				nPrepared++;
				break;
			case NET_PAM_COMMAND_START:
				nStarted++;
				break;
			case NET_PAM_COMMAND_STOP:
				nStopped++;
				break;
			}
		}

		/**
		 * May now need to tell the overall pam controller that things are starting or
		 * stopping !
		 */
		PamController.getInstance().netReceiveStatus(changeTime, nPrepared, nStarted, nStopped);
	}

	private synchronized void addPacketStats(int packetSize) {
		recentPackets++;
		recentDataBytes += packetSize;
	}

	/**
	 * @return the number of data packets received since the last call to this function
	 */
	public synchronized int getRecentPackets() {
		int n = recentPackets;
		recentPackets = 0;
		return n;
	}

	/**
	 * 
	 * @return The total number of bytes received since the last call to this function
	 */
	public synchronized int getRecentDataBytes() {
		int n = recentDataBytes;
		recentDataBytes = 0;
		return n;
	}

	/**
	 * The connection thread does not receive data, it just listens 
	 * for socket requests when clients attempt to open a connection. 
	 * When it get's a connection, it then opens a new ReceiveThread
	 * which will receive the actual data. 
	 * @author Doug Gillespie
	 *
	 */
	class ConnectionThread implements Runnable {

		protected NetworkReceiveParams currentParams;

		private volatile boolean keepRunning = true;

		ServerSocket serverSocket;

		/**
		 * @param currentParams
		 */
		public ConnectionThread(NetworkReceiveParams currentParams) {
			super();
			this.currentParams = currentParams.clone();
		}

		/**
		 * Stop the connection thread by closing the
		 * server socket and telling it to break out of the loop. 
		 */
		public void stopConnectionThread() {
			keepRunning = false;
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			runConnectionThread();

		}

		/**
		 * Connection thread - sits in loop waiting for connections. 
		 */
		public void runConnectionThread() {
			int bufferSize = 1024;
			byte[] dgBuffer = new byte[21];

			Socket clientSocket;
			ReceiveThread rxThread;
			try {
				serverSocket = new ServerSocket(networkReceiveParams.receivePort);
				while (keepRunning) {
					System.out.println("Waiting for client contact on port " + networkReceiveParams.receivePort);
					/*
					 * This next function blocks until either something connects, or 
					 * the serverSocket is closed. 
					 */
					clientSocket = serverSocket.accept();

					System.out.println("Handling client at " +
							clientSocket.getInetAddress().getHostAddress() + " on port " +
							clientSocket.getPort());

					rxThread = new ReceiveThread(clientSocket);
					receiveThreads.add(rxThread);
					Thread t = new Thread(rxThread);
					t.start();

				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}

		}

	}

	class ReceiveThread implements Runnable {
		private Socket clientSocket;
		private InputStream inStream;
		private byte[] duBuffer = new byte[0];
		private int duBufferPos;
		private int duSize;
		private short buoyId1;
		private short buoyId2;
		private short dataId1;
		private int dataId2;
		private int dataLen;
		private int unitBytesRead;
		private volatile boolean keepRunning = true;

		public ReceiveThread(Socket clientSocket) {
			super();
			this.clientSocket = clientSocket;
		}

		public void stopThread() {
			keepRunning = false;
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {	
			receiveData();
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			/*
			 * Once the thread has ended, tell the stats objects that
			 * they are no longer connected to anything. 
			 */
			ListIterator<BuoyStatusDataUnit> it = buoyStatusDataBlock.getListIterator(0);
			BuoyStatusDataUnit b;
			while (it.hasNext()) {
				b = it.next();
				if (b.getSocket() == clientSocket) {
					b.setSocket(null);
				}
			}
		}

		public void receiveData() {
			byte[] receiveBuffer = new byte[1024*20];
			int bytesRead;
			int bytesLeft;
			DataInputStream dis;
			int headInt;
			System.out.println(String.format("Socket opened on input stream %s", this.toString()));
			try {
				inStream = clientSocket.getInputStream();
				dis = new DataInputStream(inStream);
				int badHeads;
				while (keepRunning) {
					/**
					 * Ensure (or try to) that we're always starting on an object boundary, beginning with 
					 * 0xFEDC 
					 */
					badHeads = 0;
					while (true) {
						/*
						 * This shouldn't normally happen, but does happen if data were
						 * interrupted in some way due to network problems. 
						 * Keep reading until it's all correctly lined up again !
						 */
						headInt = dis.readInt();
						if (headInt == HEADID) {
							break;
						}
						badHeads++;
						System.out.println(String.format("Unable to find head in stream try %d got 0x%x %s", badHeads, headInt, this.toString()));

					}
					// now must be aligned on the start of real data. 
					duSize = dis.readInt();
					dataVersion = dis.readShort();
					buoyId1 = dis.readShort();
					buoyId2 = dis.readShort();
					dataId1 = dis.readShort();
					dataId2 = dis.readInt();
					dataLen = dis.readInt();
					bytesLeft = duSize - 24;
					if (receiveBuffer.length < bytesLeft) {
						receiveBuffer = new byte[bytesLeft];
					}		
					bytesRead = 0;
					// use readFully - will block until all data have arrived. Important with large packets. 
					dis.readFully(receiveBuffer, 0, bytesLeft);
					bytesRead = bytesLeft;
					//					while (bytesRead < bytesLeft) {
					//						bytesRead += dis.read(receiveBuffer, bytesRead, bytesLeft-bytesRead);
					//					}
					//					if (bytesRead< 0) {
					//						break;
					//					}
					addPacketStats(bytesRead);

					interpretData(clientSocket, dataVersion, buoyId1, buoyId2, dataId1, dataId2, bytesRead, receiveBuffer);
					//					interpretReadData(receiveBuffer, bytesRead);
					//					System.out.println(String.format("Data received from network = " + bytesRead + " bytes"));

				}

			} catch (IOException e) {
				System.out.println(String.format("Socket closed on input stream %s", this.toString()));
				receiveThreads.remove(this);
			}
			catch (Exception e) {
				System.out.println(String.format("General exception in input stream %s", this.toString()));
				e.printStackTrace();
			}
			System.out.println("Receive thread terminated");
			//			clientSocket.close();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("Receiver Thread address %s, channel %s, port %d", clientSocket.getInetAddress(), 
					clientSocket.getChannel(), clientSocket.getPort());
		}
	}

	int findErrors = 0;
	public synchronized void interpretData(Socket socket, short dataVersion2, short buoyId1, short buoyId2,
			short dataId1, int dataId2, int dataLen, byte[] duBuffer) {
		BuoyStatusDataUnit buoyStatusDataUnit = findBuoyStatusDataUnit(buoyId1, buoyId2, true);
		buoyStatusDataUnit.setSocket(socket);
		if (dataId1 != NET_PAM_DATA) {
			System.out.println(String.format("Buoy data arrived id %d / %d, length %d", dataId1, dataId2, dataLen));
		}
		switch(dataId1) {
		case NET_PAM_DATA:
			interpretPamData(socket, dataVersion2, buoyStatusDataUnit, dataId1, dataId2, dataLen, duBuffer);
			break;
		case NET_SYSTEM_DATA:
			interpretSystemData(socket, dataVersion2, buoyStatusDataUnit, dataId1, dataId2, dataLen, duBuffer);
			break;
		case NET_PAM_COMMAND:
			interpretPamCommand(socket, dataVersion2, buoyStatusDataUnit, dataId1, dataId2, dataLen, duBuffer);
			break;
		}
	}
	public  void interpretSystemData(Socket socket, short dataVersion2, BuoyStatusDataUnit buoyStatusDataUnit,
			short dataId1, int dataId2, int dataLen, byte[] duBuffer) {
		switch (dataId2) {
		case SYSTEM_GPSDATA:
			interpretGpsData(buoyStatusDataUnit, dataLen, duBuffer);
			return;
		case SYSTEM_COMPASSDATA:
			interpretCompassData(buoyStatusDataUnit, dataLen, duBuffer);
			return;
		default:
			System.out.println(String.format("Unidentified system data id %d, length %d", dataId2, dataLen));
		}

	}
	private void interpretCompassData(BuoyStatusDataUnit buoyStatusDataUnit,
			int dataLen, byte[] duBuffer) {
		String compassString = new String(duBuffer, 0, dataLen);
		// strin gstarts with HMC:, no ',' !
		int compInd = compassString.indexOf(COMPASSID);
		if (compInd < 0) {
			System.out.println(String.format("Invalid compass data %s : %s", 
					PamCalendar.formatDateTime(PamCalendar.getTimeInMillis()), compassString));
			return;
		}
		System.out.println(String.format("Compass data %s : %s", 
				PamCalendar.formatDateTime(PamCalendar.getTimeInMillis()), compassString));
		StringBuffer sb = new StringBuffer(compassString.substring(COMPASSID.length()+1));
		double[] compassData = new double[3];
		try {
			for (int i = 0; i < 3; i++) {
				String subStr = NMEADataBlock.getSubString(sb, i+1);
				compassData[i] = Double.valueOf(subStr);
			}
		}
		catch (NumberFormatException e) {
			System.out.println(String.format("Invalid compass data %s : %s", 
					PamCalendar.formatDateTime(PamCalendar.getTimeInMillis()), compassString));
			return;
		}
		buoyStatusDataUnit.setCompassData(PamCalendar.getTimeInMillis(), compassData);
	}

	private void interpretGpsData(BuoyStatusDataUnit buoyStatusDataUnit, int dataLen,
			byte[] duBuffer) {
		//		System.out.println("GPS String: " + new String(duBuffer));
		String gpsString = new String(duBuffer, 0, dataLen);
		StringBuffer sb = new StringBuffer(gpsString);
		GpsData gpsData = null;
		if (gpsString.contains("GPRMC")) {
			gpsData = new GpsData(sb, GPSParameters.READ_RMC, false);
		}
		else if (gpsString.contains("GPGGA")) {
			gpsData = new GpsData(sb, GPSParameters.READ_GGA, false);
		}
		if (gpsData == null) {
			System.out.println("Invalid GPS String type. Must be GGA or RMC: " + gpsString);
			return;
		}
		if (gpsData.isDataOk() == false) {
			System.out.println("Invalid GPS string from buoy " + buoyStatusDataUnit.getBuoyId1() + ": " + gpsString);
			return;
		}
		//		System.out.println("Good GPS string from buoy " + buoyId1 + ": " + gpsString);

		//		buoyStats.setSocket(socket);
		//		buoyStats.newDataObject(dataBlock, dataUnit, dataBlockSeqNumber);
		GpsDataUnit gpsDataUnit = new GpsDataUnit(System.currentTimeMillis(), gpsData);
		if (gpsData.isDataOk()) {
			gpsDataUnit.setChannelBitmap(1<<buoyStatusDataUnit.getChannel());
			buoyStatusDataUnit.setGpsData(gpsDataUnit.getTimeMilliseconds(), gpsData);
			buoyStatusDataBlock.updatePamData(buoyStatusDataUnit, System.currentTimeMillis());
		}
	}

	public  void interpretPamData(Socket socket, short dataVersion2, BuoyStatusDataUnit buoyStatusDataUnit,
			short dataId1, int dataId2, int dataLen, byte[] duBuffer) {
		/*
		 * Still need to do some unpacking,  
		 */
		int dataBlockSeqNumber = findStreamDataBlock(dataId2);
		if (dataBlockSeqNumber < 0) {
			if (++findErrors < 10) {
				System.out.println(String.format("Unable to find datablock for network data received from buoy %d(%d) with data Id %d(%d)",
						buoyStatusDataUnit.getBuoyId1(), buoyStatusDataUnit.getBuoyId2(), dataId1, dataId2));
			}
			return;
		}
		PamDataBlock dataBlock =  rxDataBlocks.get(dataBlockSeqNumber);
		BinaryDataSource dataSource = dataBlock.getBinaryDataSource();
		PamProcess parentProcess = dataBlock.getParentProcess();

		// now need to pack that up into something very similar to the data we'd read from a binary file. 
		ByteArrayInputStream bis = new ByteArrayInputStream(duBuffer);
		DataInputStream ds = new DataInputStream(bis);
		// unpack the header information
		int objectId = 0; 
		int moduleVersion = 0;
		long millis = 0;
		int dataLength = 0;
		byte[] data = null;
		try {
			objectId = ds.readInt();
			moduleVersion = ds.readInt();
			millis = ds.readLong();
			dataLength = ds.readInt();
			data = new byte[dataLength];
			ds.read(data);
			ds.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		BinaryObjectData bod = new BinaryObjectData(objectId, millis, 0, data, dataLength);

		//		String str = String.format("Net data received a %s from buoy %d(%d) type %d(%d), data length = %d for %s",
		//				PamCalendar.formatDateTime(millis),buoyId1, buoyId2, dataId1, dataId2, dataLen, dataBlock.getDataName());
		//		System.out.println(str);

		PamDataUnit dataUnit = dataSource.sinkData(bod, null, moduleVersion);
		if (dataUnit == null) {
			System.out.println("Null data unit from dataSource: " + dataSource.getModuleName());
			return;
		}
		//		
		//		BuoyStatusDataUnit buoyStats = findBuoyStatusDataUnit(buoyId1, buoyId2, true);
		//		if (dataUnit.getClass().)

		buoyStatusDataUnit.setTimeMilliseconds(dataUnit.getTimeMilliseconds());
		buoyStatusDataUnit.setSocket(socket);
		buoyStatusDataUnit.newDataObject(dataBlock, dataUnit, dataBlockSeqNumber);
		if (buoyStatusDataUnit.getCommandStatus() != NET_PAM_COMMAND_START) {
			buoyStatusDataUnit.setCommandStatus(NET_PAM_COMMAND_START);
			commandStateChanged(dataUnit.getTimeMilliseconds(), buoyStatusDataUnit);
		}

		if (networkReceiveParams.channelNumberOption == NetworkReceiveParams.CHANNELS_RENUMBER) {
			dataUnit.setChannelBitmap(1<<buoyStatusDataUnit.getChannel());
		}
		long sampleNumber = 0;
		if (AcousticDataUnit.class.isAssignableFrom(dataUnit.getClass())) {
			sampleNumber = ((AcousticDataUnit) dataUnit).getStartSample();
			checkAcousticBuoyStats(buoyStatusDataUnit);
		}
		dataBlock.masterClockUpdate(dataUnit.getTimeMilliseconds(), sampleNumber);

		//		System.out.println("New data from " + dataSource.getModuleName() + " on channels " + dataUnit.getChannelBitmap());

		/* 
		 * GPS data get handled in a special way ...
		 * This method is no longer used since gps data get sent direct interpreted 
		 * with other system data direct from the buoy control systems and don't generally come in as 
		 * data units. 
		 * This could still get used if GPS data were sent from PAMGuard rather than PAMBuoy. 
		 */
		if (dataUnit.getClass() == GpsDataUnit.class) {
			buoyStatusDataUnit.setGpsData(dataUnit.getTimeMilliseconds(), ((GpsDataUnit) dataUnit).getGpsData());
			//			networkReceiveProcess.newGpsData((GpsDataUnit) dataUnit);
		}
		else {
			parentProcess.processNewBuoyData(buoyStatusDataUnit, dataUnit);
			dataBlock.addPamData(dataUnit);
			//			System.out.println("DAta added to data block " + dataBlock.getDataName());
		}
	}

	/**
	 * Check the hydrophone array configuration for this status information.<p> 
	 * What happens is the following:
	 * <br> Each buoy associates itself with one streamer in the array manager. 
	 * <br> The buoy stats searches for a streamer with it's own identifier and links 
	 * to it. 
	 * <br> if it can't find a streamer with it's own id, it clones the new one 
	 * and any associated hydrophones. The exception to this is if the first streamer 
	 * does not have a buoy id, in which case it steals it. 
	 * <br> Streamers must either be set up explicitly for each buoy in a system or must
	 * at least have the same number of channels and hydrophone layout as the first streamer. 
	 * @param buoyStats buoy status data. 
	 */
	private void checkHydrophoneArray(BuoyStatusDataUnit buoyStats) {
		ArrayManager.getArrayManager().checkBuoyHydropneStreamer(buoyStats);
	}

	private void interpretPamCommand(Socket socket, short dataVersion2,
			BuoyStatusDataUnit buoyStatusDataUnit, short dataId1, int dataId2,
			int dataLen, byte[] duBuffer) {
		//		System.out.println("Network command received: " + getPamCommandString(dataId2));
		buoyStatusDataUnit.setCommandStatus(dataId2);
		long time = PamCalendar.getTimeInMillis();
		if (duBuffer != null && dataLen >= 8) {
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(duBuffer));
			try {
				time = dis.readLong();
				dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		buoyStatusDataBlock.updatePamData(buoyStatusDataUnit, time);
		/**
		 * Now have a wee think about what to do based on the overall status
		 * of multiple buoys 
		 */
		commandStateChanged(time, buoyStatusDataUnit);
	}

	public static String getPamCommandString(int command) {
		switch(command) {
		case NET_PAM_COMMAND_PREPARE:
			return "Prepare";
		case NET_PAM_COMMAND_START:
			return "Start";
		case NET_PAM_COMMAND_STOP:
			return "Stop";
		}
		return null;
	}

	/**
	 * Find buoy status data and optionally create it if it doesn't exist. 
	 * @param buoyId1 buoy Id 1
	 * @param buoyId2 buoy Id 2
	 * @param create automatically create
	 * @return status data unit or null if not found. 
	 */
	public BuoyStatusDataUnit findBuoyStatusDataUnit(int buoyId1, int buoyId2, boolean create) {
		BuoyStatusDataUnit du = buoyStatusDataBlock.findBuoyStatusData(buoyId1, buoyId2);
		if (du != null) {
			du.setNetworkReceiver(this);
			return du;
		}
		if (create == false) {
			return null;
		}
		du = new BuoyStatusDataUnit(this, buoyId1, buoyId2, findFirstFreeChannel());


		buoyStatusDataBlock.addPamData(du);
		return du;
	}

	/**
	 * If data units coming out of this buoy are acoustic, then it will need
	 * hydrophones. Note that all this code needs to work with non acoustic 
	 * data too - such as logger forms, in which case it wn' tbe necessary
	 * to have hydrophones. 
	 * @param buoyStatusDataUnit status data. 
	 */
	public void checkAcousticBuoyStats(BuoyStatusDataUnit buoyStatusDataUnit) {
		// check that the buoy id can be associated with the right hydrophones
		// and create a bearing localiser.  
		if (buoyStatusDataUnit.getHydrophoneStreamer() != null) {
			return;
		}
		checkHydrophoneArray(buoyStatusDataUnit);
		int phones = ArrayManager.getArrayManager().getCurrentArray().getPhonesForStreamer(buoyStatusDataUnit.getHydrophoneStreamer());
		BearingLocaliser bearingLocaliser = BearingLocaliserSelector.createBearingLocaliser(phones, 1e-5);
	}

	/**
	 * find the first free channel number for a new Buoy status object.
	 * @return new unassigned channel
	 */
	private int findFirstFreeChannel() {
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			if (buoyStatusDataBlock.findDataUnit(1<<i) == null) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @return the rxDataBlocks
	 */
	public ArrayList<PamDataBlock> getRxDataBlocks() {
		return rxDataBlocks;
	}

	private class NetworkReceiveProcess extends PamProcess {

		public NetworkReceiveProcess(NetworkReceiver networkReceiver) {
			super(networkReceiver, null);
			buoyStatusDataBlock = new BuoyStatusDataBlock(this);
			addOutputDataBlock(buoyStatusDataBlock);
			buoyStatusDataBlock.setOverlayDraw(new NetworkGPSDrawing(networkReceiver));
		}

		@Override
		public void pamStart() {

		}

		@Override
		public void pamStop() {

		}

	}

	/**
	 * @return the networkReceiveParams
	 */
	public NetworkReceiveParams getNetworkReceiveParams() {
		return networkReceiveParams;
	}

	@Override
	public Serializable getSettingsReference() {
		return networkReceiveParams;
	}

	@Override
	public long getSettingsVersion() {
		return NetworkReceiveParams.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		networkReceiveParams = ((NetworkReceiveParams) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}
}
