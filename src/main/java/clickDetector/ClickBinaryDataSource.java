package clickDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import clickDetector.ClickDetector.ChannelGroupDetector;

import dataMap.DataMapDrawing;

import Acquisition.AcquisitionProcess;
import Array.ArrayManager;
import Localiser.bearingLocaliser.BearingLocaliser;
import Localiser.bearingLocaliser.BearingLocaliserSelector;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import binaryFileStorage.PackedBinaryObject;

/**
 * Class for storing clicks to binary store. 
 * @author Doug Gillespie
 *
 */
public class ClickBinaryDataSource extends BinaryDataSource {

	private String streamName;

	private static final int bytesPerSamples = 1;

	private static final int currentVersion = 3;
	/**
	 * Module version changes
	 * Version 2: Add int clickFlags after short clickType
	 */

	public static final int CLICK_DETECTOR_CLICK = 1000;
	
	private ClickDetector clickDetector;
	
	private ClickBinaryModuleFooter clickFooter;
	
	private AcquisitionProcess acquisitionProcess;

	public ClickBinaryDataSource(ClickDetector clickDetector, PamDataBlock sisterDataBlock, String streamName) {
		super(sisterDataBlock);
		this.clickDetector = clickDetector;
		this.streamName = streamName;
		clickMapDrawing = new ClickMapDrawing(clickDetector);
	}

	
	@Override
	public void newFileOpened(File outputFile) {
		clickFooter = new ClickBinaryModuleFooter(clickDetector);
	}

	@Override
	public int getModuleVersion() {
		return currentVersion;
	}

	@Override
	public byte[] getModuleHeaderData() {
		return null;
	}

	@Override
	public byte[] getModuleFooterData() {
		return clickFooter.getByteArray();
	}

	@Override
	public String getStreamName() {
		return streamName;
	}

	@Override
	public int getStreamVersion() {
		return 1;
	}

	private ByteArrayOutputStream bos;
	private DataOutputStream dos;

	private ClickMapDrawing clickMapDrawing;

	/**
	 * Save a click to the binary data store
	 * @param cd click detection
	 */
	public PackedBinaryObject getPackedData(PamDataUnit pamDataUnit) {
		ClickDetection cd = (ClickDetection) pamDataUnit;
		// make a byte array output stream and write the data to that, 
		// then dump that down to the main storage stream
		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}
		try {
			dos.writeLong(cd.getStartSample());
			dos.writeInt(cd.getChannelBitmap());
			dos.writeInt(cd.triggerList);
			dos.writeShort(cd.getClickType());
			dos.writeInt(cd.getClickFlags());
			double[] delays = cd.getDelays();
			if (delays != null) {
				dos.writeShort(delays.length);
				for (int i = 0; i < delays.length; i++) {
					dos.writeFloat((float) delays[i]);
				}
			}
			else {
				dos.writeShort(0);
			}
			double[] angles = null;
			double[] angleErrors = null;
			if (cd.getLocalisation() != null) {
				angles = cd.getLocalisation().getAngles();
				angleErrors = cd.getLocalisation().getAngleErrors();
			}
			if (angles != null) {
				dos.writeShort(angles.length);
				for (int i = 0; i < angles.length; i++) {
					dos.writeFloat((float)angles[i]);
				}
			}
			else {
				dos.writeShort(0);
			}
			if (angleErrors != null) {
				dos.writeShort(angleErrors.length);
				for (int i = 0; i < angleErrors.length; i++) {
					dos.writeFloat((float)angleErrors[i]);
				}
			}
			else {
				dos.writeShort(0);
			}
			int duration = (int)cd.getDuration();
			dos.writeShort(duration);
			byte[][] waveData = cd.getCompressedWaveData();
			double maxVal = cd.getWaveAmplitude();
			// write the scale factor. 
			dos.writeFloat((float) maxVal);
			for (int i = 0; i < cd.getNChan(); i++) {
				dos.write(waveData[i]);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		PackedBinaryObject packedData = new PackedBinaryObject(CLICK_DETECTOR_CLICK, bos.toByteArray());

		if (clickFooter != null) {
			clickFooter.newClick(cd);
		}
		
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return packedData;
	}

	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData, BinaryHeader bh) {
		ClickBinaryModuleHeader mh = new ClickBinaryModuleHeader(binaryObjectData.getVersionNumber());
		if (mh.createHeader(binaryObjectData, bh) == false) {
			return null;
		}
		return mh;
	}

	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData,
			BinaryHeader bh, ModuleHeader mh) {
		ClickBinaryModuleFooter mf = new ClickBinaryModuleFooter(clickDetector);
		if (mf.createFooter(binaryObjectData, bh, mh) == false) {
			return null;
		}
		return mf;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData, BinaryHeader bh, int moduleVersion) {
		// Turn the binary data back into a click. 
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);
		
		/**
		 * Can't work out what on earth this line was doing !
		 */
//		long fileOffsetSamples = (long)((bh.getDataDate()-binaryObjectData.getTimeMillis()) * 
//				(double)clickDetector.getSampleRate() / 1000.) -
//				bh.getFileStartSample();

//		int intLength;
		long startSample;
		int channelMap;
		int triggerList;
		short clickType;
		short nDelays;
		double[] delays = null;
		short nAngles;
		double[] angles = null;
		short nAngleErrors;
		double[] angleErrors = null;
		short duration;
		double waveMax;
		double waveScale;
		int clickFlags = 0;
		int nChan;
		int maxDelays;
		byte[][] waveData;
		double channelMax, aVal, allMax = 0;
		ClickDetection newClick = null;
		ClickLocalisation clickLocalisation;
		ChannelGroupDetector channelGroupDetector = null;
		BearingLocaliser bearingLocaliser = null;
		try {
//			intLength = dis.readInt(); // should always be dataLength-4 !
//			System.out.println("Read click at " + PamCalendar.formatDateTime(millis));
			startSample = dis.readLong();
//			startSample += fileOffsetSamples;
			/*
			 * Should be able to do some check here whereby the millis time
			 * is the same as dataStart + startSample.sampleRate*1000
			 */
//			long millis2 = dataStart + (long) (startSample / clickDetector.getSampleRate() * 1000.);
//			millis2 = clickDetector.absSamplesToMilliseconds(startSample);
//			if (Math.abs(millis2-timeMillis) > 1000) {
//				System.out.println(String.format("Time offset in read %d", + millis2 - timeMillis));
//			}
			
			
			
			channelMap = dis.readInt();
			nChan = PamUtils.getNumChannels(channelMap);
			channelGroupDetector = clickDetector.findChannelGroupDetector(channelMap);
			maxDelays = (nChan*(nChan-1))/2;
			triggerList = dis.readInt();
			clickType = dis.readShort();
			if (moduleVersion >= 2) {
				clickFlags = dis.readInt();
			}
			nDelays = dis.readShort();
			if (nDelays > maxDelays) {
				System.out.println("Too many delays in click: " + nDelays);
			}
			if (nDelays > 0) {
				delays = new double[nDelays];
				for (int i = 0; i < nDelays; i++) {
					delays[i] = dis.readFloat();
				}
			}
			nAngles = dis.readShort();
			if (nAngles > 0) {
				angles = new double[nAngles];
				for (int i = 0; i < nAngles; i++) {
					angles[i] = dis.readFloat();
				}
			}
			if (moduleVersion >= 3) {
				nAngleErrors = dis.readShort();
				if (nAngleErrors > 0) {
					angleErrors = new double[nAngleErrors];
					for (int i = 0; i < nAngleErrors; i++) {
						angleErrors[i] = dis.readFloat();
					}
				}
			}
			duration = dis.readShort();
			
			newClick = new ClickDetection(channelMap, startSample, duration, clickDetector, null, triggerList);
			newClick.setClickType((byte) clickType);
			newClick.setClickFlags(clickFlags);
			newClick.setChannelGroupDetector(channelGroupDetector);
			for (int i = 0; i < nDelays; i++) {
				newClick.setDelay(i, delays[i]);
			}
			
			waveMax = dis.readFloat();
			waveScale = waveMax / 127.;
			if (nChan <= 0 || duration <= 0) {
				System.out.println("Invalid click in file");
				return null;
			}
			waveData = new byte[nChan][duration];
			for (int iChan = 0; iChan < nChan; iChan++) {
				dis.read(waveData[iChan]);
				channelMax = 0;
				for (int iSamp = 0; iSamp < duration; iSamp++) {
//					waveData[iChan][iSamp] = (aVal = dis.readByte() * waveScale);
					channelMax = Math.max(channelMax, waveData[iChan][iSamp]);
				}
				newClick.setAmplitude(iChan, channelMax*waveScale);
				allMax = Math.max(channelMax, allMax*waveScale);
			}
			int firstChan = PamUtils.getLowestChannel(newClick.getChannelBitmap());
//			newClick.setMeasuredAmplitude(allMax/nChan);
//			newClick.setMeasuredAmplitudeType(AcousticDataUnit.AMPLITUDE_SCALE_LINREFSD);
			if (acquisitionProcess == null) {
				acquisitionProcess = (AcquisitionProcess) clickDetector.getSourceProcess();
			}
			if (acquisitionProcess != null) {
				newClick.setCalculatedAmlitudeDB(acquisitionProcess.
						rawAmplitude2dB(newClick.getMeanAmplitude(), firstChan, false));
			}
			clickLocalisation = newClick.getClickLocalisation();
			if (clickLocalisation != null && angles != null) {
				clickLocalisation.setAngles(angles);
//				 needs another call into the localisation to set the correct array axis. 
				clickLocalisation.setSubArrayType(getArrayType(channelMap, nAngles));
				if (channelGroupDetector != null) {
					bearingLocaliser = channelGroupDetector.getBearingLocaliser();
				}
				if (bearingLocaliser == null) {
					// this should be called with hydrophone map, not channel map. 
					bearingLocaliser = BearingLocaliserSelector.createBearingLocaliser(channelMap, 0);
				}
				clickLocalisation.setSubArrayType(bearingLocaliser.getArrayType());
				clickLocalisation.setArrayAxis(bearingLocaliser.getArrayAxis());


			}
			if (angleErrors != null) {
					clickLocalisation.setAngleErrors(angleErrors);
			}
			newClick.setTimeMilliseconds(binaryObjectData.getTimeMillis());
			newClick.setCompressedData(waveData, waveMax);
//			clickDetector.getClickDataBlock().addPamData(newClick);
			
		} catch (IOException e1) {
			System.out.println("Error in file: " + e1.getMessage());
			return null;
		}

		try {
			bis.close();
		} catch (IOException e) {
			System.out.println("Error in file: " + e.getMessage());
			return null;
		}
		
		//if (newClick.getAmplitudeDB()>150) System.out.println("SinkData: ClickType: "+newClick.getClickType()+" channel: "+PamUtils.getSingleChannel(newClick.getChannelBitmap())+ " Amplitude: "+  newClick.getAmplitudeDB());

		
		return newClick;
	}
	
	/**
	 * Try to work out from the click detector what on earth the array type is for this
	 * localisation. 
	 * For now, cheat and just base the decision on the number of channels !
	 * @param channelMap
	 * @return
	 */
	private int getArrayType(int channelMap, int nAngles) {
		int nChan = PamUtils.getNumChannels(channelMap);
		if (nChan <= 1) {
			return ArrayManager.ARRAY_TYPE_POINT;
		}
		else if (nChan == 2)  {
			return ArrayManager.ARRAY_TYPE_LINE;
		}
		else if (nAngles == 1) {
			return ArrayManager.ARRAY_TYPE_LINE;
		}
		else if (nChan == 3) {
			return ArrayManager.ARRAY_TYPE_PLANE;
		}
		else if (nChan >= 4) {
			return ArrayManager.ARRAY_TYPE_VOLUME;
		}
		return ArrayManager.ARRAY_TYPE_POINT;
	}



	@Override
	public DataMapDrawing getSpecialDrawing() {
		return clickMapDrawing;
	}

	/*
	 * version 0 format. 
	public void saveData(PamDataUnit pamDataUnit) {
		if (getBinaryStorageStream() == null) {
			return;
		}
		ClickDetection cd = (ClickDetection) pamDataUnit;
		// make a byte array output stream and write the data to that, 
		// then dump that down to the main storage stream
		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}
		try {
			dos.writeLong(cd.getTimeMilliseconds());
			dos.writeLong(cd.getStartSample());
			dos.writeInt(cd.getChannelBitmap());
			dos.writeInt(cd.triggerList);
			dos.writeShort(cd.getClickType());
			double[] delays = cd.getDelays();
			if (delays != null) {
				dos.writeShort(delays.length);
				for (int i = 0; i < delays.length; i++) {
					dos.writeFloat((float) delays[i]);
				}
			}
			else {
				dos.writeShort(0);
			}
			double[] angles = cd.getLocalisation().getAngles();
			if (angles != null) {
				dos.writeShort(angles.length);
				for (int i = 0; i < angles.length; i++) {
					dos.writeFloat((float)angles[i]);
				}
			}
			else {
				dos.writeShort(0);
			}
			int duration = (int)cd.getDuration();
			dos.writeShort(duration);
			double[][] waveData = cd.getWaveData();
			for (int i = 0; i < cd.getNChan(); i++) {
				for (int j = 0; j < duration; j++) {
					dos.writeShort((int)(waveData[i][j]*32768.));
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		getBinaryStorageStream().storeData(BinaryTypes.CLICK_DETECTOR_CLICK, bos.toByteArray());
	}
	 */

}
