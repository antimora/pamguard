package whistlesAndMoans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import whistlesAndMoans.WhistleToneConnectProcess.ShapeConnector;

import Localiser.bearingLocaliser.BearingLocaliser;
import PamController.PamController;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import binaryFileStorage.PackedBinaryObject;

public class WhistleBinaryDataSource extends BinaryDataSource {

	private String streamName;
	
	public static final int WHISTLE_MOAN_DETECTION = 2000;
	
	private static final int currentVersion = 1;
	
	private WhistleToneConnectProcess wmDetector;
	
	private int runMode;

	public WhistleBinaryDataSource(WhistleToneConnectProcess wmDetector, PamDataBlock sisterDataBlock, String streamName) {
		super(sisterDataBlock);
		this.wmDetector = wmDetector;
		this.streamName = streamName;
		runMode = PamController.getInstance().getRunMode();
	}

	private ByteArrayOutputStream bos;
	private DataOutputStream dos;
	private DataOutputStream headerOutputStream;

	private ByteArrayOutputStream headerBytes;

	private int delayScale = 0;
	
	@Override
	public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit) {
		ConnectedRegionDataUnit crdu = (ConnectedRegionDataUnit) pamDataUnit;
		ConnectedRegion cr = crdu.getConnectedRegion();

		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}
		if (delayScale == 0) {
			delayScale = wmDetector.getDelayScale();
		}
		List<SliceData> sliceDataList;
		SliceData sliceData;
		int[][] peakInfo;
		try {
			dos.writeLong(crdu.getStartSample());
			dos.writeInt(crdu.getChannelBitmap());
			int nSlices = cr.getNumSlices();
			dos.writeShort(nSlices);
			if (currentVersion >= 1) {
//				amplitude = (double) dis.readShort() / 100.;
//				nDelays = dis.readByte();
//				delays = new double[nDelays];
//				for (int i = 0; i < nDelays; i++) {
//					delays[i] = dis.readShort();
//				}
				dos.writeShort((int) (crdu.getAmplitudeDB() * 100));
				int nDelays = 0;
				double[] delays = crdu.getTimeDelaysSeconds();
				double sampleRate = wmDetector.getSampleRate();
				if (delays != null) {
					nDelays = delays.length;
				}
				dos.writeByte(nDelays);
				for (int i = 0; i < nDelays; i++) {
					dos.writeShort((int) (delays[i] * sampleRate * delayScale));
				}
			}
			sliceDataList = cr.getSliceData();
			for (int i = 0; i < nSlices; i++) {
				sliceData = sliceDataList.get(i);
				dos.writeInt(sliceData.sliceNumber);
				dos.writeByte(sliceData.nPeaks);
				peakInfo = sliceData.peakInfo;
				for (int j = 0; j < sliceData.nPeaks; j++) {
					for (int k = 0; k < 4; k++) {
						dos.writeShort(peakInfo[j][k]);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		if (getBinaryStorageStream() != null) {
//			getBinaryStorageStream().storeData(WHISTLE_MOAN_DETECTION, 
//					crdu.getTimeMilliseconds(),
//					bos.toByteArray());
//		}

		PackedBinaryObject pbo = new PackedBinaryObject(WHISTLE_MOAN_DETECTION, bos.toByteArray());
		
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return pbo;
	}

	@Override
	public String getStreamName() {
		return streamName;
	}

	@Override
	public int getStreamVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData, BinaryHeader bh, int moduleVersion) {

		ConnectedRegionDataUnit crdu = null;
		ConnectedRegion cr = null;
		SliceData sliceData;
		ConnectedRegionDataBlock wmDataBlock = wmDetector.getOutputData();
		int fftHop = wmDataBlock.getFftHop();
		int fftLength = wmDataBlock.getFftLength();
		float sampleRate = wmDataBlock.getSampleRate();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);
		
//		long intLength;
		if (delayScale == 0) {
			delayScale = wmDetector.getDelayScale();
		}
		long startSample;
		int channelMap; 
		int singleChannel;
		short nSlices;
		int sliceNum;
		int nPeaks;
		double amplitude = 0;
		double[] delays = null;
		int nDelays;
		long firstSliceSample;
		int[][] peakInfo;
		int firstSliceNum = 0;
		int[] timeBins;
		int[] peakFreqsBins;
		try {
//			intLength = dis.readInt(); // should always be dataLength-4 !
//			firstSliceSample = (long) ((double)(binaryObjectData.getTimeMillis() - bh.getDataDate()) * sampleRate / 1000.);
//			firstSliceSample = binary
			firstSliceSample = startSample = dis.readLong();
			channelMap = dis.readInt();
			if (channelMap != 1) {
				System.out.println("Channel map = " + channelMap);
			}
			singleChannel = PamUtils.getLowestChannel(channelMap);
			nSlices = dis.readShort();
			if (moduleVersion >= 1) {
				amplitude = (double) dis.readShort() / 100.;
				nDelays = dis.readByte();
//				if (nDelays > 1) {
//					System.out.println("Bad number of delays : " + nDelays);
//				}
				delays = new double[nDelays];
				for (int i = 0; i < nDelays; i++) {
					delays[i] = (double) dis.readShort() / delayScale / sampleRate;
				}
			}
			/*
			 * sliceDataList = cr.getSliceData();
			for (int i = 0; i < nSlices; i++) {
				sliceData = sliceDataList.get(i);
				dos.writeInt(sliceData.sliceNumber);
				dos.writeByte(sliceData.nPeaks);
				peakInfo = sliceData.peakInfo;
				for (int j = 0; j < sliceData.nPeaks; j++) {
					for (int k = 0; k < 4; k++) {
						dos.writeShort(peakInfo[j][k]);
					}
				}
			}
			 */
			
			timeBins = new int[nSlices];
			peakFreqsBins = new int[nSlices];
			for (int i = 0; i < nSlices; i++) {
				sliceNum = dis.readInt();
				nPeaks = dis.readByte();
				if (nPeaks < 0) {
					System.out.println("Negative number of peaks: " + nPeaks);
				}
				if (i == 0) {
					firstSliceNum = sliceNum;
					cr = new ConnectedRegion(singleChannel, sliceNum, 0, wmDetector.getFFTLen());
				}
				peakInfo = new int[nPeaks][4];
				for (int j = 0; j < nPeaks; j++) {
					for (int k = 0; k < 4; k++) {
						peakInfo[j][k] = dis.readShort();
					}
				}
				sliceData = new SliceData(sliceNum, firstSliceSample + 
						fftHop * (sliceNum-firstSliceNum), peakInfo);
				cr.addOfflineSlice(sliceData);
				timeBins[i] = sliceData.sliceNumber;
				peakFreqsBins[i] = sliceData.getPeakBin();
			}			
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		catch (Exception ee) {
			ee.printStackTrace();
			return null;
		}
		cr.cleanFragmentedFragment();
//		cr.sett
//		cr.addOfflineSlice(sliceData);
//		cr.condenseInfo();
		crdu = new ConnectedRegionDataUnit(cr, wmDetector);
		crdu.setTimeMilliseconds(binaryObjectData.getTimeMillis());
		crdu.setTimeDelaysSeconds(delays);
		crdu.setCalculatedAmlitudeDB(amplitude);
		crdu.setDuration((nSlices+1) * fftHop);
		/*
		 *  now also need to recalculate bearings using the appropriate bearing localiser.
		 *  These are hidden away in the sub processes and may be different for different 
		 *  hydrophone groups. 
		 *  Only do this here if we're in viewer mode, not network receive mode. 
		 *  If we're n network receive mode, we can't do this until 
		 *  channel numbers have been reassigned.   
		 */
		if (runMode == PamController.RUN_PAMVIEW && delays != null) {
			ShapeConnector shapeConnector = wmDetector.findShapeConnector(channelMap);
			if (shapeConnector != null) {
				BearingLocaliser bl = shapeConnector.getBearingLocaliser();
				if (bl != null) {
					double[][] angles = bl.localise(delays);
					WhistleBearingInfo newLoc = new WhistleBearingInfo(crdu, bl, 
							shapeConnector.getGroupChannels(), angles);
					newLoc.setArrayAxis(bl.getArrayAxis());
					newLoc.setSubArrayType(bl.getArrayType());
					crdu.setTimeDelaysSeconds(delays);
					crdu.setLocalisation(newLoc);
				}
			}
		}
		
		try {
			dis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return crdu;
	}

	@Override
	public int getModuleVersion() {
		return currentVersion;
	}

	@Override
	public byte[] getModuleHeaderData() {
		if (headerOutputStream == null) {
			headerOutputStream = new DataOutputStream(headerBytes = new ByteArrayOutputStream(4));
		}
		headerBytes.reset();
		try {
			headerOutputStream.writeInt(delayScale = wmDetector.getDelayScale());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return headerBytes.toByteArray();
	}


	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData,
			BinaryHeader bh, ModuleHeader moduleHeader) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see binaryFileStorage.BinaryDataSource#sinkModuleHeader(binaryFileStorage.BinaryObjectData, binaryFileStorage.BinaryHeader)
	 */
	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData,
			BinaryHeader bh) {
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData());
		DataInputStream dis = new DataInputStream(bis);
		try {
			delayScale = dis.readInt();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		WhistleBinaryModuleHeader mh = new WhistleBinaryModuleHeader(binaryObjectData.getVersionNumber());
		mh.delayScale = delayScale;
		return mh;
	}

	@Override
	public void newFileOpened(File outputFile) {
		// TODO Auto-generated method stub
		
	}

	
}
