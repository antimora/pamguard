package clipgenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import binaryFileStorage.PackedBinaryObject;

public class ClipBinaryDataSource extends BinaryDataSource {

	private ClipControl clipControl;
	private ClipDataBlock clipDataBlock;
	
	public ClipBinaryDataSource(ClipControl clipControl, ClipDataBlock clipDataBlock) {
		super(clipDataBlock);
		this.clipControl = clipControl;
		this.clipDataBlock = clipDataBlock;
	}

	@Override
	public byte[] getModuleHeaderData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getModuleVersion() {
		return 1;
	}

	@Override
	public String getStreamName() {
		return "Clips";
	}

	@Override
	public int getStreamVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void newFileOpened(File outputFile) {
		// TODO Auto-generated method stub

	}

	private ByteArrayOutputStream bos;
	private DataOutputStream dos;
	@Override
	public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit) {
		ClipDataUnit cd = (ClipDataUnit) pamDataUnit;
	
		// make a byte array output stream and write the data to that, 
		// then dump that down to the main storage stream
		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}
		int storageOption = clipControl.clipSettings.storageOption;
		if (cd.getRawData() == null) {
			storageOption = ClipSettings.STORE_WAVFILES;
		}
		try {
			dos.writeLong(cd.getStartSample());
			dos.writeInt(cd.getChannelBitmap());
			dos.writeLong(cd.triggerMilliseconds);
			dos.writeInt((int)cd.getDuration());
			dos.writeUTF(cd.fileName);
			dos.writeUTF(cd.triggerName);
			if (storageOption == ClipSettings.STORE_BINARY) {
				writeWaveClip(dos, cd.getRawData());
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
//		getBinaryStorageStream().storeData(1, cd.getTimeMilliseconds(), bos.toByteArray());
		return new PackedBinaryObject(storageOption + 1, bos.toByteArray());

	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData,
			BinaryHeader bh, int moduleVersion) {
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);
		
		long startSample;
		int channelMap;
		long triggerMillis;
		int duration;
		String fileName;
		String triggerName;
		
		int storageOption = binaryObjectData.getObjectType() - 1;
		double[][] rawData = null;
		try {
			startSample = dis.readLong();
			channelMap = dis.readInt();
			triggerMillis = dis.readLong();
			duration = dis.readInt();
			fileName = dis.readUTF();
			triggerName = dis.readUTF();
			if (storageOption == ClipSettings.STORE_BINARY) {
				rawData = readWavClip(dis);
			}
			
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		ClipDataUnit newUnit = new ClipDataUnit(binaryObjectData.getTimeMillis(), triggerMillis, startSample, 
				duration, channelMap, fileName, triggerName, rawData);
		return newUnit;
	}

	/**
	 * Write the wave clip in scaled int8 format. 
	 * @param dos2
	 * @param rawData
	 * @throws IOException 
	 */
	private void writeWaveClip(DataOutputStream dos2, double[][] rawData) throws IOException {
		int nChan = rawData.length;
		int nSamps = rawData[0].length;
		double minVal = 0, maxVal = 0;
		for (int iC = 0; iC < nChan; iC++) {
			double[] chanData = rawData[iC];
			for (int iS = 0; iS < nSamps; iS++) {
				minVal = Math.min(minVal, chanData[iS]);
				maxVal = Math.max(maxVal, chanData[iS]);		
			}
		}
		maxVal = Math.max(maxVal, -minVal);
		float scale = (float) (127./maxVal);
		dos.writeShort(nChan);
		dos.writeInt(nSamps);
		dos.writeFloat(scale);
		for (int iC = 0; iC < nChan; iC++) {
			double[] chanData = rawData[iC];
			for (int iS = 0; iS < nSamps; iS++) {
				dos.writeByte((int) (chanData[iS] * scale));
			}
		}
	}

	private double[][] readWavClip(DataInputStream dis) throws IOException {
		int nChan = dis.readShort();
		int nSamps = dis.readInt();
		double scale = 1./dis.readFloat();
		double[][] rawData = new double[nChan][nSamps];
		for (int iC = 0; iC < nChan; iC++) {
			double[] chanData = rawData[iC];
			for (int iS = 0; iS < nSamps; iS++) {
				chanData[iS] = (double) dis.readByte() * scale;
			}
		}
		return rawData;
	}

	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData,
			BinaryHeader bh, ModuleHeader moduleHeader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData,
			BinaryHeader bh) {
		// TODO Auto-generated method stub
		return null;
	}

}
