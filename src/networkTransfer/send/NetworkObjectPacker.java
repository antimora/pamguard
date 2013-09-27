package networkTransfer.send;

import java.io.DataOutputStream;
import java.io.IOException;

import networkTransfer.receive.NetworkReceiver;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.PackedBinaryObject;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

/**
 * Functions to pack up data for sending over the network. 
 * @author Doug Gillespie
 *
 */
public class NetworkObjectPacker {

	public static final int DATASTARTFLAG = 0xFFEEDDCC;
	
	public byte[] packData(int buoyId1, int buoyId2, short dataType1, int dataType2, byte[] data) {
		if (data == null) {
			return packData(buoyId1, buoyId2, dataType1, dataType2, data, 0);
		}
		else {
			return packData(buoyId1, buoyId2, dataType1, dataType2, data, data.length);
		}
	}
	
	/**
	 * Pack a data unit for TCPIP transfer. 
	 * Note that data units get additional head information comprising 
	 * an objectid, a time in millis and the actual datalength. 
	 * @param buoyId1
	 * @param buoyId2
	 * @param dataBlock
	 * @param dataUnit
	 * @return
	 */
	public byte[] packDataUnit(int buoyId1, int buoyId2, PamDataBlock dataBlock, PamDataUnit dataUnit) {
		short dataType1 = NetworkReceiver.NET_PAM_DATA;
		BinaryDataSource binarySource = dataBlock.getBinaryDataSource();
		int dataType2 = dataBlock.getQuickId();
		PackedBinaryObject packedObject = binarySource.getPackedData(dataUnit);
		byte[] data = packedObject.data;
		int duDataLength = data.length + 20;
		ByteOutputStream bos = new ByteOutputStream(duDataLength);
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeInt(packedObject.objectId);
			dos.writeInt(binarySource.getModuleVersion());
			dos.writeLong(dataUnit.getTimeMilliseconds());
			dos.writeInt(data.length);
			dos.write(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		data = packData(buoyId1, buoyId2, dataType1, dataType2, bos.getBytes(), duDataLength);
		try {
			dos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * Pack any type of data for TCP transfer between programmes. 
	 * @param buoyId1 buoy id1
	 * @param buoyId2 buoy id2
	 * @param dataType1 first data identifier
	 * @param dataType2 second data identifier
	 * @param data data 
	 * @param dataLength length of data in bytes. 
	 * @return byte packed array
	 */
	public byte[] packData(int buoyId1, int buoyId2, short dataType1, int dataType2, byte[] data, int dataLength) {
		int totalSize = dataLength + 24;
		int rem = totalSize%4;
		if (rem != 0) {
			totalSize += 4-rem;
		}
		ByteOutputStream bos = new ByteOutputStream(totalSize);
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeInt(DATASTARTFLAG);
			dos.writeInt(totalSize);
			dos.writeShort(1); // header version
			dos.writeShort(buoyId1);
			dos.writeShort(buoyId2);
			dos.writeShort(dataType1);
			dos.writeInt(dataType2);
			dos.writeInt(dataLength);
			if (data != null) {
				dos.write(data, 0, dataLength);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return bos.getBytes();
	}

//	/**
//	 * Pack an object so it can be sent via TCP as a single lump of data. 
//	 * @param anObject object to pack
//	 * @return byte array to send 
//	 */
//	public byte[] packObject(NetworkQueuedObject anObject) {
//		return packData(anObject.buoyId1, anObject.buoyId2, (short) anObject.dataType1, 
//				anObject.dataType2, anObject.data, anObject.dataLength);
//	}
}
