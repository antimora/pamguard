package networkTransfer.send;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import networkTransfer.emulator.NetworkEmulator;
import networkTransfer.receive.NetworkReceiver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamguardVersionInfo;
import PamView.PamSidePanel;
import PamguardMVC.PamDataBlock;

/**
 * Send near real time data over the network to another PAMGUARD configuration.
 * <p>Not currently configured in Java.  
 * @author Doug Gillespie
 *
 */
public class NetworkSender extends PamControlledUnit implements PamSettings {

	protected NetworkSendParams networkSendParams = new NetworkSendParams();
	private NetworkEmulator networkEmulator;
	private boolean initialisationComplete = false;
	List<NetworkQueuedObject> objectList;
	int totalQueueSize = 0;
	private NetworkSendSidePanel sidePanel;
	private QueueWorkerThread queueWorkerThread;
	private NetworkObjectPacker objectPacker;
	private Socket tcpSocket;
	private String currStatus = "Closed";
	private DataOutputStream tcpWriter;
	private NetworkSendProcess commandProcess;
	
	public NetworkSender(String unitName) {
		super("Network Sender", unitName);
		commandProcess = new NetworkSendProcess(this, null);
		commandProcess.setCommandProcess(true);
		addPamProcess(commandProcess);
		PamSettingManager.getInstance().registerSettings(this);
		objectList = Collections.synchronizedList(new LinkedList<NetworkQueuedObject>());
		sidePanel = new NetworkSendSidePanel(this);
		objectPacker = new NetworkObjectPacker();
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " Settings ...");
		menuItem.addActionListener(new SenderSettings(parentFrame));
		if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU && isViewer) {
			JMenu menu = new JMenu(getUnitName());
			menu.add(menuItem);
			menuItem = new JMenuItem("Emulate PamBuoy Mitigate Data ...");
			menuItem.addActionListener(new MitigateEmulateMenu(parentFrame));
			menu.add(menuItem);
			return menu;
		}
		else {
			return menuItem;
		}
	}
	
	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#getSidePanel()
	 */
	@Override
	public PamSidePanel getSidePanel() {
		return sidePanel;
	}

	private class SenderSettings implements ActionListener {

		private Frame parentFrame;

		public SenderSettings(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			senderSettings(parentFrame);			
		}
		
	}

	public void senderSettings(Frame parentFrame) {
		NetworkSendParams p = NetworkSendDialog.showDialog(parentFrame, this, networkSendParams);
		if (p != null) {
			networkSendParams = p.clone();
			sortDataSources();
		}
	}

	private class MitigateEmulateMenu implements ActionListener {
		
		private Frame parentFrame;

		public MitigateEmulateMenu(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			mitigateEmulate(parentFrame);			
		}
	}
	
	/**
	 * Call the emulator to pop up a dialog which willcontrol everything. 
	 * @param parentFrame
	 */
	public void mitigateEmulate(Frame parentFrame) {
		getNetworkEmulator().showEmulateDialog(parentFrame);
	}
	
	/**
	 * Get  / create the NetworkEmulator. 
	 * @return
	 */
	private NetworkEmulator getNetworkEmulator() {
		if (networkEmulator == null) {
			networkEmulator = new NetworkEmulator(this);
		}
		return networkEmulator;
	}

	@Override
	public Serializable getSettingsReference() {
		NetworkSendParams p = networkSendParams.clone();
		if (p.savePassword == false) {
			p.password = null;
		}
		return p;
	}

	@Override
	public long getSettingsVersion() {
		return NetworkSendParams.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		networkSendParams = ((NetworkSendParams) pamControlledUnitSettings.getSettings()).clone();
		return (networkSendParams != null);
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("ipAddress", networkSendParams.ipAddress);
		paramsEl.setAttribute("portNumber", String.format("%d",networkSendParams.portNumber));
		paramsEl.setAttribute("userId", networkSendParams.userId);
		if (networkSendParams.savePassword) {
			paramsEl.setAttribute("password", networkSendParams.password);
		}
		else {
			paramsEl.setAttribute("password", "**********");
		}
		return true;
	}

	/**
	 * @return the networkSendParams
	 */
	public NetworkSendParams getNetworkSendParams() {
		return networkSendParams;
	}


	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamController.INITIALIZATION_COMPLETE:
			sortDataSources();
			initialisationComplete  = true;
			break;
		case PamController.REMOVE_CONTROLLEDUNIT:
		case PamController.ADD_CONTROLLEDUNIT:
			if (initialisationComplete) {
				sortDataSources();
			}
		}
	}

	private void sortDataSources() {
		ArrayList<PamDataBlock> wanted = listWantedDataSources();
		int nProcess = getNumPamProcesses();
		for (int i = nProcess - 1; i >= 1; i--) {
			removePamProcess(getPamProcess(i));
		}
		for (PamDataBlock aBlock:wanted) {
			addPamProcess(new NetworkSendProcess(this, aBlock));
		}
	}

	public ArrayList<PamDataBlock> listWantedDataSources() {
		ArrayList<PamDataBlock> possibles = listPossibleDataSources();
		ArrayList<PamDataBlock> wants = new ArrayList<PamDataBlock>();
		for (PamDataBlock aBlock:possibles) {
			if (networkSendParams.findDataBlock(aBlock) != null) {
				wants.add(aBlock);
			}
		}
		return wants;
	}
	
	
	public ArrayList<PamDataBlock> listPossibleDataSources() {
		ArrayList<PamDataBlock> possibles = new ArrayList<PamDataBlock>();
		ArrayList<PamDataBlock> allDataBlocks = PamController.getInstance().getDataBlocks();
		for (PamDataBlock aBlock:allDataBlocks) {
			if (aBlock.getBinaryDataSource() != null) {
				possibles.add(aBlock);
			}
		}
		return possibles;
	}

	/** 
	 * Put data objects in a queue. A different thread will handle
	 * emptying the queue, opening the connection, sending the data, etc. 
	 * @param networkQueuedObject Object to add to the queue
	 */
	public synchronized void queueDataObject(NetworkQueuedObject networkQueuedObject) {
		objectList.add(networkQueuedObject);
		totalQueueSize += networkQueuedObject.dataLength;
		checkQueueSize();
	}
	
	/**
	 * Get the size of the queue as a number of objects. 
	 * @return number of objects in queue.
	 */
	public synchronized int getQueueLength() {
		return objectList.size();
	}
	
	/**
	 * Get the current queue size in kilobytes
	 * @return
	 */
	public synchronized int getQueueSize() {
		return totalQueueSize / 1024;
	}

	/**
	 * Check the queue size is no greater than the permitted maximum. If it is, start to throw 
	 * away the oldest data units. 
	 */
	private void checkQueueSize() {
		NetworkQueuedObject removedObject;
		while (totalQueueSize > networkSendParams.maxQueueSize * 1024 || objectList.size() > networkSendParams.maxQueuedObjects) {
			 removedObject = objectList.remove(0);
			 totalQueueSize -= removedObject.dataLength;
		}
	}

	public String getStatus() {
		return currStatus;
	}
	
	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#pamClose()
	 */
	@Override
	public void pamClose() {
		// TODO Auto-generated method stub
		super.pamClose();
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#pamHasStopped()
	 */
	@Override
	public void pamHasStopped() {
		if (queueWorkerThread != null) {
			queueWorkerThread.stopThread(true, true);
			queueWorkerThread = null;
		}
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#pamToStart()
	 */
	@Override
	public void pamToStart() {
		super.pamToStart();
		queueWorkerThread = new QueueWorkerThread();
		queueWorkerThread.execute();
	}

	/**
	 * Called from within the swing worker thread to send data from the queue. 
	 * @return The number of objects sent. 
	 */
	public int sendQueuedData() {
		int objectsSent = 0;
		NetworkQueuedObject anObject;
		while (objectList.size() > 0) {
			anObject = objectList.get(0);
			if (!sendObject(anObject)) {
				return objectsSent;				
			}
			totalQueueSize -= anObject.dataLength;
			objectList.remove(0);
		}
		return objectsSent;
	}

	/**
	 * Final flushing of the data queue once DAQ has stopped. 
	 * @param flushData
	 * @param flushCommands
	 * @return number of objects sent. 
	 */
	private int flushQueue(boolean flushData, boolean flushCommands) {
		int objectsSent = 0;
		NetworkQueuedObject anObject;
		while (objectList.size() > 0) {
			anObject = objectList.get(0);
			if (shouldFlush(anObject, flushData, flushCommands)) {
				if (!sendObject(anObject)) {
					return objectsSent;				
				}
			}
			totalQueueSize -= anObject.dataLength;
			objectList.remove(0);
		}	
		return objectsSent;
	}
	
	/**
	 * Delete everything left in the queue.
	 * @return number of items removed. 
	 */
	private int deleteQueue() {
		// now just empty anything left...
		int n = objectList.size();
		objectList.clear();
		totalQueueSize = 0;		
		return n;
	}
	
	boolean shouldFlush(NetworkQueuedObject anObject, boolean flushData, boolean flushCommands) {
		switch(anObject.dataType2) {
		case NetworkReceiver.NET_PAM_DATA:
			return flushData;
		default:
			return flushCommands;
		}
	}

	private boolean sendObject(NetworkQueuedObject anObject) {
		// first, re-pack the object ...
		byte[] data = anObject.data;
		// open a writer if necessary. 
		if (tcpWriter == null) {
			if (!openConnection()) {
				return false;
			}
		}
		/*
		 * Don't we here need to write out a whole load of stuff first to make
		 * the header before the actual data ? 
		 */
		boolean wrote = writeData(data);
		if (wrote == false) {
			closeConnection();
			if (!openConnection()) {
				return false;
			}
			if (!writeData(data)) {
				closeConnection();
				return false;
			}
		}
		
		return true;
	}
	
	private boolean writeData(byte[] data) {
		try {
			tcpWriter.write(data);
//			System.out.println(String.format("Wrote %d bytes to socket", data.length));
			return true;
		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println(e.getMessage());
			currStatus = "Socket Closed";
			return false;
		}
	}
	
	boolean openConnection() {
		try {
			tcpSocket = new Socket(networkSendParams.ipAddress, networkSendParams.portNumber);
		} catch (UnknownHostException e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//			currStatus = e.getMessage();
			currStatus = "Unknown host";
			tcpSocket = null;
			tcpWriter = null;
			return false;
		} catch (IOException e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//			currStatus = e.getMessage();
			currStatus = "IO Exception";
			tcpSocket = null;
			tcpWriter = null;
			return false;
		}
		try {
			tcpWriter = new DataOutputStream(tcpSocket.getOutputStream());
		} catch (IOException e) {
//			currStatus = "TCP IO Exception";
			currStatus = e.getMessage();
			tcpSocket = null;
			tcpWriter = null;
			return false;
		}
		currStatus = "Open";
		return true;
	}
	
	void closeConnection() {
		if (tcpSocket == null) {
			return;
		}
		try {
			tcpSocket.close();
			tcpWriter.close();
		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		tcpSocket = null;
		tcpWriter = null;
	}

	private class QueueWorkerThread extends SwingWorker<Integer, QueueStatusData> {

		private volatile boolean stopNow;
		private boolean flushData;
		private boolean flushCommands;

		@Override
		protected Integer doInBackground() {
			while (stopNow == false) {
				if (sendQueuedData() == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			flushQueue(flushData, flushCommands);
			deleteQueue();
			return null;
		}

		public void stopThread(boolean flushData, boolean flushCommands) {
			stopNow = true;
			this.flushData = flushData;
			this.flushCommands = flushCommands;
		}

		@Override
		protected void done() {
			// TODO Auto-generated method stub
			super.done();
		}

		@Override
		protected void process(List<QueueStatusData> chunks) {
			// TODO Auto-generated method stub
			super.process(chunks);
		}
		
	}
}
