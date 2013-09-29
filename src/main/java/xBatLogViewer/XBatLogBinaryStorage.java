package xBatLogViewer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import binaryFileStorage.PackedBinaryObject;

public class XBatLogBinaryStorage extends BinaryDataSource {

	float sampleRate = 1;
	String dataName;

	public XBatLogBinaryStorage(PamDataBlock sisterDataBlock) {
		super(sisterDataBlock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] getModuleHeaderData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getModuleVersion() {
		return 0;
	}

	@Override
	public String getStreamName() {
		return "XBat Data";
	}

	@Override
	public int getStreamVersion() {
		return 0;
	}

	@Override
	public void newFileOpened(File outputFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData,
			BinaryHeader bh, int moduleVersion) {

		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);
		
		XBatLogDataUnit du = null;

		try {

			long startSample = dis.readLong();
			int channelMap = dis.readInt();
			float durationSecs = dis.readFloat();
			long durationMillis = (long) (durationSecs * 1000.);
			double f[] = new double[2];
			f[0] = dis.readFloat();
			f[1] = dis.readFloat();
			long nSamples = (long) (durationSecs * sampleRate);
			du = new XBatLogDataUnit(binaryObjectData.getTimeMillis(), 
					channelMap, startSample, nSamples, durationMillis);
			du.setFrequency(f);

			dis.close();
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return du;
	}

	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData,
			BinaryHeader bh, ModuleHeader moduleHeader) {
		return null;
	}

	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData,
			BinaryHeader bh) {		
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);


		try {
			sampleRate = dis.readFloat();
			dataName = dis.readUTF();
			dis.close();
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
