package networkTransfer.send;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import networkTransfer.receive.NetworkReceiver;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.PackedBinaryObject;
import PamController.PamControlledUnit;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class NetworkSendProcess extends PamProcess {

	private BinaryDataSource binarySource;
	private int quickId;
	private NetworkSender networkSender;
	private NetworkObjectPacker networkObjectPacker;
	private boolean commandProcess;

	public NetworkSendProcess(NetworkSender networkSender,
			PamDataBlock parentDataBlock) {
		super(networkSender, parentDataBlock);
		this.networkSender = networkSender;
		if (parentDataBlock != null) {
			binarySource = parentDataBlock.getBinaryDataSource();
			quickId = parentDataBlock.getQuickId();
		}
		networkObjectPacker = new NetworkObjectPacker();
	}


	@Override
	public void prepareProcess() {
		if (commandProcess) {
			sendPamCommand(NetworkReceiver.NET_PAM_COMMAND_PREPARE);
		}
	}

	@Override
	public void pamStart() {
		if (commandProcess) {
			sendPamCommand(NetworkReceiver.NET_PAM_COMMAND_START);
		}
	}

	@Override
	public void pamStop() {
		if (commandProcess) {
			sendPamCommand(NetworkReceiver.NET_PAM_COMMAND_STOP);
		}
	}

	private void sendPamCommand(int command) {
		int id1 = networkSender.networkSendParams.stationId1;
		int id2 = networkSender.networkSendParams.stationId2;
		/*
		 * Need to pack the time that this command was sent so that the Network Receiver can know when to start
		 * even if there is a delay in sending the data.  
		 */
		ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
		DataOutputStream dos = new DataOutputStream(bos);
		byte[] timeData = null;
		try {
			dos.writeLong(PamCalendar.getTimeInMillis());
			timeData = bos.toByteArray();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] data = networkObjectPacker.packData(id1, id2, (short) NetworkReceiver.NET_PAM_COMMAND, command, timeData);
		NetworkQueuedObject qo = new NetworkQueuedObject(id1, id2, NetworkReceiver.NET_PAM_COMMAND, command, data);
		networkSender.queueDataObject(qo);
	}

	@Override
	public String getProcessName() {
		if (commandProcess) {
			return "Command";
		}
		if (getParentDataBlock() != null) {
			return getParentDataBlock().getDataName();
		}
		return null;
	}

	@Override
	public void newData(PamObservable dataBlock, PamDataUnit dataUnit) {
		/*
		 * Pack and send the data off to the queue in the Controller
		 */
		int id1 = networkSender.networkSendParams.stationId1;
		int id2 = networkSender.networkSendParams.stationId2;
		//		PackedBinaryObject data = binarySource.getPackedData(dataUnit);
		byte[] data = networkObjectPacker.packDataUnit(id1,id2, (PamDataBlock) dataBlock, dataUnit);
		NetworkQueuedObject qo = new NetworkQueuedObject(id1, id2, NetworkReceiver.NET_PAM_DATA, quickId, data);
		networkSender.queueDataObject(qo);
	}


	/**
	 * @param commandProcess the commandProcess to set
	 */
	public void setCommandProcess(boolean commandProcess) {
		this.commandProcess = commandProcess;
	}


	/**
	 * @return the commandProcess
	 */
	public boolean isCommandProcess() {
		return commandProcess;
	}



}
