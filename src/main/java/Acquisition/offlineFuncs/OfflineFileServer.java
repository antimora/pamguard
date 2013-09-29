package Acquisition.offlineFuncs;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingWorker;

import pamScrollSystem.ViewLoadObserver;

import clickDetector.ClickTabPanelControl.MapOptions;

import wavFiles.ByteConverter;

import Acquisition.AcquisitionControl;
import Acquisition.DaqSystem;
import Acquisition.FileDate;
import Acquisition.FolderInputSystem;
import Acquisition.StandardFileDate;
import Acquisition.pamAudio.PamAudioSystem;
import PamController.AWTScheduler;
import PamController.OfflineDataStore;
import PamController.OfflineRawDataStore;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.RawDataUnit;
import PamUtils.PamAudioFileFilter;
import PamUtils.PamCalendar;
import PamUtils.PamFileFilter;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RequestCancellationObject;

/**
 * Functionality for handling data from files offline.
 * @author Doug Gillespie
 *
 */
public class OfflineFileServer implements OfflineDataStore, PamSettings {

	private OfflineRawDataStore offlineRawDataStore;

	private OfflineFileParameters offlineFileParameters = new OfflineFileParameters();

	private WavFileDataMap dataMap;

	private FileDate fileDate = new StandardFileDate();

	private PamRawDataBlock rawDataBlock;

	private FileMapMakingdialog mapDialog;

	private MapMaker mapMaker;

	/**
	 * @param acquisitionControl
	 */
	public OfflineFileServer(OfflineRawDataStore offlineRawDataStore) {
		super();
		this.offlineRawDataStore = offlineRawDataStore;
		rawDataBlock = offlineRawDataStore.getRawDataBlock();
		PamSettingManager.getInstance().registerSettings(this);
		dataMap = new WavFileDataMap(this, rawDataBlock);
		rawDataBlock.addOfflineDataMap(dataMap);
	}

	@Override
	public void createOfflineDataMap(Window parentFrame) {
		if ((PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW)) {
			return;
		}
		dataMap.clear();
		/**
		 * then search through all files and folders, checking the 
		 * size and duration of every file (could take quite some time !)
		 */
		if (offlineFileParameters.enable == false) {
			return;
		}
		mapMaker = new MapMaker(this);
		AWTScheduler.getInstance().scheduleTask(mapMaker);

	}

	private class MapMaker extends SwingWorker<Integer, FileMapProgress> {

		private OfflineFileServer fileServer;

		public MapMaker(OfflineFileServer offlineFileServer) {
			fileServer = offlineFileServer;
		}

		@Override
		protected Integer doInBackground() throws Exception {
			try {
				mapDialog = FileMapMakingdialog.showDialog(PamController.getMainFrame(), fileServer);
				addToMap(new File(offlineFileParameters.folderName), offlineFileParameters.includeSubFolders);
				dataMap.sortMapPoints();
				getMapTimes();
				mapDialog.setVisible(false);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
		
		public void pPublish(FileMapProgress mapProgress) {
			publish(mapProgress);
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}

		@Override
		protected void done() {
			if (mapDialog != null) {
				mapDialog.setVisible(false);
			}
		}

		@Override
		protected void process(List<FileMapProgress> chunks) {
			//			for (int i = 0; i < chunks.size(); i++) {
			//				mapDialog.setProgress(chunks.get(i));
			//			}
			mapDialog.setProgress(chunks.get(chunks.size()-1));
		}

	}

	private void addToMap(File folderName, boolean includeSubFolders) {
		PamFileFilter audioFileFilter = new PamAudioFileFilter();
		File[] files = folderName.listFiles(audioFileFilter);
		if (files == null) return;
		File file;
		for (int i = 0; i < files.length; i++) {
			file = files[i];
			if (file.isDirectory() && includeSubFolders) {
				System.out.println(file.getAbsoluteFile());
				addToMap(file.getAbsoluteFile(), includeSubFolders);
			}
			else if (file.isFile()) {
				addToMap(file);
			}
		}
	}

	/**
	 * Add a single sound file to the data map 
	 * @param file
	 */
	private void addToMap(File file) {
		/* first need to try to get the time from the file name...
		 * Don't do this here - just build the list of files
		 * first, then go through and open all the files to get their durations 
		 * 
		 */
		long startTime = 0;
//		DaqSystem daqSystem = offlineRawDataStore.findDaqSystem(null);

		startTime = fileDate.getTimeFromFile(file);

		WavFileDataMapPoint mapPoint = new WavFileDataMapPoint(file, startTime, 
				startTime);
		dataMap.addDataPoint(mapPoint);

		mapMaker.pPublish(new FileMapProgress(true, dataMap.getNumMapPoints(), 0, file.getName()));

	}

	private void getMapTimes() {
		Iterator<WavFileDataMapPoint> it = dataMap.getListIterator();
		WavFileDataMapPoint mapPoint;
		File file;
		int totalPoints = dataMap.getNumMapPoints();
		int opened = 0;
		while (it.hasNext()) {
			mapPoint = it.next();
			file = mapPoint.getSoundFile();/*
			 * Now need to open file file as an input stream (what a bore !)
			 */
			AudioInputStream audioStream;
			long fileSamples = 0;
			long fileMillis = 0;
			try {
				audioStream = PamAudioSystem.getAudioInputStream(file);
				AudioFormat audioFormat = audioStream.getFormat();
				audioStream.close();
				fileSamples = audioStream.getFrameLength();
				float sampleRate = audioFormat.getSampleRate();
				fileMillis = (long) (fileSamples*1000/sampleRate);
				mapPoint.setEndTime(mapPoint.getStartTime() + fileMillis);
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mapMaker.pPublish(new FileMapProgress(false, totalPoints, ++opened, file.getName()));
		}

	}

	@Override
	public String getDataSourceName() {
		// TODO Auto-generated method stub
		return "Sound Files";
	}

	private AudioInputStream audioInputStream;
	private AudioFormat audioFormat;

	private ByteConverter byteConverter;
	@Override
	public boolean loadData(PamDataBlock dataBlock, long dataStart, long dataEnd, 
			RequestCancellationObject cancellationObject, ViewLoadObserver loadObserver) {
//		System.out.println(String.format("Request raw data from %s to %s", PamCalendar.formatDateTime(dataStart),
//				PamCalendar.formatTime(dataEnd)));
		/*
		 * Find the data mapped files, work through them and load data as appropriate, forming into 
		 * standard sized data units and passing off into the dataBlock.
		 */
		Iterator<WavFileDataMapPoint> mapIt = dataMap.getListIterator();
		WavFileDataMapPoint mapPoint;
		File soundFile;
		mapPoint = findFirstMapPoint(mapIt, dataStart, dataEnd);
		if (mapPoint == null) {
			return false;
		}
		if (openSoundFile(mapPoint.getSoundFile()) == false) {
			return false;
		}
		byteConverter = ByteConverter.createByteConverter(audioFormat);
		long currentTime = mapPoint.getStartTime();
		long prevFileEnd = mapPoint.getEndTime();
		boolean fileGap = false;
		int newSamples; 
		double[][] doubleData;
		int nChannels = audioFormat.getChannels();
		int blockSamples = Math.max((int) audioFormat.getSampleRate() / 10, 1000);
		byte[] inputBuffer = new byte[blockSamples * audioFormat.getFrameSize()];
		int bytesRead = 0;
		long totalSamples = 0;
//		long fileSamples = 0;
		long millisecondsGaps = 0;
		long ms;

		RawDataUnit newDataUnit;
		if (currentTime < dataStart) {
			// need to fast forward in current file. 
			long skipBytes = (long) (((dataStart-currentTime)*audioFormat.getSampleRate()*audioFormat.getFrameSize())/1000.);
			try {
				audioInputStream.skip(skipBytes);
			} catch (IOException e) {
				System.out.println("End of audio file " + mapPoint.getSoundFile().getName());
				//				e.printStackTrace();
			}
			currentTime = dataStart;
		}
		ms = currentTime;
		while (ms < dataEnd && currentTime < dataEnd) {
			if (cancellationObject != null && cancellationObject.cancel) {
				break;
			}
			try {
				bytesRead = audioInputStream.read(inputBuffer);
			} catch (IOException e) {
				e.printStackTrace();
			}			
			if (bytesRead <= 0) {
				/*
				 *  that's the end of that file, so get the next one if there
				 *  is one, if not then break.
				 */
				if (mapIt.hasNext() == false) {
					break;
				}
				mapPoint = mapIt.next();
				fileGap = (mapPoint.getStartTime() - prevFileEnd) > 1000;
				if (fileGap) {
					System.out.println(String.format("Sound file gap %3.3fs from %s to %s", 
							(double) (mapPoint.getStartTime()-prevFileEnd) / 1000.,
							PamCalendar.formatTime(prevFileEnd), PamCalendar.formatTime(mapPoint.getStartTime())));
				}
				prevFileEnd = mapPoint.getEndTime();
				if (!fileGap) { // don't carry on if there is a file gap
					if (openSoundFile(mapPoint.getSoundFile()) == false) {
						break;
					}
					// try again to read data. 
					try {
						bytesRead = audioInputStream.read(inputBuffer);
					} catch (IOException e) {
						e.printStackTrace();
					}		
					if (bytesRead <= 0) {
						break;
					}
				}
			}
			newSamples = bytesRead / audioFormat.getFrameSize();
			doubleData = new double[nChannels][newSamples];
			int convertedSamples = byteConverter.bytesToDouble(inputBuffer, doubleData, bytesRead);
			ms = offlineRawDataStore.getParentProcess().absSamplesToMilliseconds(totalSamples);
			ms = currentTime + (long)(totalSamples * 1000 / (double) audioFormat.getSampleRate());

			for (int ichan = 0; ichan < nChannels; ichan++) {

				newDataUnit = new RawDataUnit(ms, 1 << ichan, totalSamples, newSamples);
				newDataUnit.setRawData(doubleData[ichan], true);

				rawDataBlock.addPamData(newDataUnit);
			}
			if (fileGap) {
				currentTime = mapPoint.getStartTime();
				totalSamples = 0;
//				fileSamples = 0;
			}

			totalSamples += newSamples;
//			fileSamples += newSamples;
		}


		return true;
	}
	private boolean openSoundFile(File soundFile) {

		try {
			audioInputStream = PamAudioSystem.getAudioInputStream(soundFile);
			audioFormat = audioInputStream.getFormat();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private WavFileDataMapPoint findFirstMapPoint(Iterator<WavFileDataMapPoint> mapIterator, long startMillis, long endMillis) {
		WavFileDataMapPoint mapPoint;
		while (mapIterator.hasNext()) {
			mapPoint = mapIterator.next();
			if (mapPoint.getEndTime() < startMillis) {
				continue;
			}
			else if (mapPoint.getStartTime() > endMillis) {
				return null;
			}
			return mapPoint;
		}
		return null;
	}

	@Override
	public boolean saveData(PamDataBlock dataBlock) {
		// will never have to do this
		return false;
	}

	@Override
	public Serializable getSettingsReference() {
		return offlineFileParameters;
	}

	@Override
	public long getSettingsVersion() {
		return OfflineFileParameters.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return offlineRawDataStore.getUnitName();
	}

	@Override
	public String getUnitType() {
		return "Offline sound file server";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		OfflineFileParameters n = (OfflineFileParameters) pamControlledUnitSettings.getSettings();
		offlineFileParameters = n.clone();
		return true;
	}

	/**
	 * @return the offlineFileParameters
	 */
	public OfflineFileParameters getOfflineFileParameters() {
		return offlineFileParameters;
	}

	/**
	 * @param offlineFileParameters the offlineFileParameters to set
	 */
	public void setOfflineFileParameters(OfflineFileParameters offlineFileParameters) {
		this.offlineFileParameters = offlineFileParameters;
	}

}
