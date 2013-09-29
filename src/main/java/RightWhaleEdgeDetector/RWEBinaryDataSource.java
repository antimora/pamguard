package RightWhaleEdgeDetector;

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

public class RWEBinaryDataSource extends BinaryDataSource {

	RWEDataBlock rweDataBlock;
	public RWEBinaryDataSource(RWEDataBlock rweDataBlock) {
		super(rweDataBlock);
		this.rweDataBlock = rweDataBlock;
	}

	@Override
	public byte[] getModuleHeaderData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getModuleVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getStreamName() {
		return "Edges";
	}

	@Override
	public int getStreamVersion() {
		return 0;
	}

	@Override
	public void newFileOpened(File outputFile) {
		// TODO Auto-generated method stub

	}
	private ByteArrayOutputStream bos;
	private DataOutputStream dos;
	@Override

	public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit){
		RWEDataUnit rweDataUnit = (RWEDataUnit) pamDataUnit;
		RWESound aSound = rweDataUnit.rweSound;

		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}

		try {
			dos.writeLong(rweDataUnit.getStartSample());
			dos.writeInt(rweDataUnit.getChannelBitmap());
			int nSlices = aSound.sliceCount;
			dos.writeShort(aSound.soundType);
			dos.writeFloat((float)aSound.signal);
			dos.writeFloat((float)aSound.noise);
			dos.writeShort(nSlices);
			for (int i = 0; i < nSlices; i++) {
				dos.writeShort(aSound.sliceList[i]);
				dos.writeShort(aSound.lowFreq[i]);
				dos.writeShort(aSound.peakFreq[i]);
				dos.writeShort(aSound.highFreq[i]);
				dos.writeFloat((float) aSound.peakAmp[i]);
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		if (getBinaryStorageStream() != null) {
//			getBinaryStorageStream().storeData(0, 
//					rweDataUnit.getTimeMilliseconds(),
//					bos.toByteArray());
//		}
		
		PackedBinaryObject pbo = new PackedBinaryObject(0, bos.toByteArray());

		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pbo;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData,
			BinaryHeader bh, int moduleVersion) {
		RWEDataUnit rweDataUnit;
		RWESound aSound = null;
		RWEDetectionPeak aPeak;

		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);

		long startSample;
		int channelMap;
		int soundType;
		double signal, noise;
		int nSlices;
		int iSlice, lowFreq, peakFreq, highFreq;
		double peakAmp;
		try {
//			dos.writeShort(aSound.soundType);
//			dos.writeFloat((float)aSound.signal);
//			dos.writeFloat((float)aSound.noise);
//			dos.writeShort(nSlices);
//			for (int i = 0; i < nSlices; i++) {
//				dos.writeShort(aSound.sliceList[i]);
//				dos.writeShort(aSound.lowFreq[i]);
//				dos.writeShort(aSound.peakFreq[i]);
//				dos.writeShort(aSound.highFreq[i]);
//				dos.writeFloat((float) aSound.peakAmp[i]);
//			}
			startSample = dis.readLong();
			channelMap = dis.readInt();
			soundType = dis.readShort();
			signal = dis.readFloat();
			noise = dis.readFloat();
			nSlices = dis.readShort();
			for (int i = 0; i < nSlices; i++) {
				iSlice = dis.readShort();
				lowFreq = dis.readShort();
				peakFreq = dis.readShort();
				highFreq = dis.readShort();
				peakAmp = dis.readFloat();
				aPeak = new RWEDetectionPeak(lowFreq);
				aPeak.bin2 = highFreq;
				aPeak.peakBin = peakFreq;
				aPeak.maxAmp = peakAmp;
				if (i == 0) {
					aSound = new RWESound(binaryObjectData.getTimeMillis(), aPeak, 0);
				}
				else {
					aSound.addPeak(iSlice, aPeak, 0);
				}
			}
			aSound.completeSound();
			aSound.signal = signal;
			aSound.noise = noise;
			aSound.soundType = soundType;
			
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		long duration = rweDataBlock.getFftHop() * aSound.duration;
		rweDataUnit = new RWEDataUnit(aSound.timeMilliseconds, channelMap, 
				startSample, duration, aSound);
		double f[] = new double[2];
		f[0] = aSound.minFreq * rweDataBlock.getSampleRate()/rweDataBlock.getFftLength();
		f[1] = aSound.maxFreq * rweDataBlock.getSampleRate()/rweDataBlock.getFftLength();
		rweDataUnit.setFrequency(f);
		return rweDataUnit;
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
