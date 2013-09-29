package clipgenerator;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import wavFiles.WavFile;

import PamController.PamController;
import PamDetection.AcousticDataUnit;
import PamUtils.FileParts;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import Spectrogram.SpectrogramDisplay;
import Spectrogram.SpectrogramMarkProcess;

/**
 * Process for making short clips of audio data. 
 * <br> separate subscriber processes for each triggering data block, but these all send clip requests
 * back into the main observer of the actual raw data - so that all clips are made from the 
 * same central thread. 
 * <br> Let the request queue trigger off the main clock signal. 
 *  
 * @author Doug Gillespie
 *
 */
public class ClipProcess extends SpectrogramMarkProcess {

	private ClipControl clipControl;
	
	private PamDataBlock[] dataSources;

	private ClipBlockProcess[] clipBlockProcesses;
	
	private List<ClipRequest> clipRequestQueue;
	
	private Object clipRequestSynch = new Object();
	
	private PamRawDataBlock rawDataBlock;
	
	private ClipDataBlock clipDataBlock;

	private long specMouseDowntime;

	private boolean specMouseDown;

	private long masterClockTime;
	
	public ClipProcess(ClipControl clipControl) {
		super(clipControl);
		this.clipControl = clipControl;
		clipRequestQueue = new LinkedList<ClipRequest>();
		clipDataBlock = new ClipDataBlock(clipControl.getUnitName() + " Clips", this);
		clipDataBlock.setBinaryDataSource(new ClipBinaryDataSource(clipControl, clipDataBlock));
		ClipOverlayGraphics cog = new ClipOverlayGraphics(clipControl);
		clipDataBlock.setOverlayDraw(cog);
		addOutputDataBlock(clipDataBlock);
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		super.newData(o, arg);
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL) {
			return;
		}
		processRequestList();
	}

	@Override
	public void masterClockUpdate(long timeMilliseconds, long sampleNumber) {
		super.masterClockUpdate(timeMilliseconds, sampleNumber);
		masterClockTime = timeMilliseconds;
	}

	/**
	 * Process the queue of clip request - these are passed straight back
	 * into the ClipBlockProcesses which started them since there is a 
	 * certain amount of bookkeeping which needs to be done at the
	 * individual block level.  
	 */
	private void processRequestList() {
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL) {
			return;
		}
		if (clipRequestQueue.size() == 0) {
			return;
		}
		synchronized(clipRequestSynch) {
			ClipRequest clipRequest;
			ListIterator<ClipRequest> li = clipRequestQueue.listIterator();
			int clipErr;
			while (li.hasNext()) {
				clipRequest = li.next();
				clipErr = clipRequest.clipBlockProcess.processClipRequest(clipRequest);
				switch (clipErr) {
				case 0: // no error - clip should have been created. 
				case RawDataUnavailableException.DATA_ALREADY_DISCARDED:
				case RawDataUnavailableException.INVALID_CHANNEL_LIST:
					li.remove();
				case RawDataUnavailableException.DATA_NOT_ARRIVED:
					continue; // hopefully, will get this next time !
				}
			}
		}
	}

	@Override
	public void pamStart() {
		super.pamStart();
		clipRequestQueue.clear(); // just in case anything hanging around from previously. 
		// if there is it may crash since the ClipblockProcess will probably have been replaced anyway. 
	}

	/**
	 * Find the wav file to go with a particular clip
	 * @param clipDataUnit data unit to find the file for. 
	 * @return file, or null if not found. 
	 */
	public File findClipFile(ClipDataUnit clipDataUnit) {
		String path = getClipFileFolder(clipDataUnit.getTimeMilliseconds(), true);
		path += clipDataUnit.fileName;
		File aFile = new File(path);
		if (aFile.exists() == false) {
			return null;
		}
		return aFile;
	}
	
	/**
	 * Get the output folder, based on time and sub folder options. 
	 * @param timeStamp
	 * @param addSeparator
	 * @return
	 */
	private String getClipFileFolder(long timeStamp, boolean addSeparator) {
		String fileSep = FileParts.getFileSeparator();
		String folderName = new String(clipControl.clipSettings.outputFolder);
		if (clipControl.clipSettings.datedSubFolders) {
			folderName += fileSep + PamCalendar.formatFileDate(timeStamp);

			// now check that that folder exists. 
			File folder = new File(folderName);
			if (folder.exists() == false) {
				folder.mkdirs();
				if (folder.exists() == false) {
					return null;
				}
			}
		}
		if (addSeparator) {
			folderName += fileSep;
		}
		return folderName;
	}

	@Override
	public boolean flushDataBlockBuffers(long maxWait) {
		boolean ans = super.flushDataBlockBuffers(maxWait);
		processRequestList(); // one last go at processing the clip request list before stopping.
		
		return ans;
	}

	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		long minH = 0;
		if (clipBlockProcesses == null || clipBlockProcesses.length == 0) {
			return 0;
		}
		for (int i = 0; i < clipBlockProcesses.length; i++) {
			if (clipBlockProcesses[i] == null) {
				continue;
			}
			minH = Math.max(minH, clipBlockProcesses[i].getRequiredDataHistory(o, arg));
		}
		minH += Math.max(3000, 192000/(long)getSampleRate());
		if (specMouseDown) {
			minH = Math.max(minH, masterClockTime-specMouseDowntime);
		}
		return minH;
	}

	private void addClipRequest(ClipRequest clipRequest) {
		synchronized (clipRequestSynch) {
			clipRequestQueue.add(clipRequest);
		}
	}
	
	@Override
	public void spectrogramNotification(SpectrogramDisplay display, int downUp, int channel, 
			long startMilliseconds, long duration, double f1, double f2) {
		/**
		 * Called when a manual mark is made on the spectrogram display. 
		 */
		if (downUp == SpectrogramMarkProcess.MOUSE_DOWN) {
			specMouseDown = true;
			specMouseDowntime = startMilliseconds;
			return;
		}
		else {
			specMouseDown = false;
		}
		if (duration == 0) {
			// no duration or extent in time. Mouse was probably just clicked on spectrogram
			// so don't bother. 
			return;
		}
		// TODO Auto-generated method stub
//		super.spectrogramNotification(display, downUp, channel, startMilliseconds,
//				duration, f1, f2);
//		String str = new String();
//		str = String.format("Spec mark at %s, frequency %3.1f to %3.1f Hz, duration %2.1fs", 
//				PamCalendar.formatDateTime(startMilliseconds), f1, f2, duration / getSampleRate() * 1000.);
//		System.out.println(str);
		long startSample = absMillisecondsToSamples(startMilliseconds);
//		startSample -
		int numSamples = (int) relMillisecondsToSamples(duration);
		int channelMap;
		channelMap = PamUtils.SetBit(0, channel, 1); // just the channel that had the mark
		channelMap = rawDataBlock.getChannelMap(); // all channels in the raw data block 
		double[][] rawData = null;
		try {
			rawData = rawDataBlock.getSamples(startSample, numSamples, channelMap);
		} catch (RawDataUnavailableException e) {
			System.out.println(e.getMessage());
			return;
		}
		if (rawData == null) {
			return;
		}
		ClipDataUnit clipDataUnit;
		if (clipControl.clipSettings.storageOption == ClipSettings.STORE_WAVFILES) {
			String folderName = getClipFileFolder(startMilliseconds, true);
			String fileName = PamCalendar.createFileName(startMilliseconds, "Clip", ".wav");
			WavFile wavFile = new WavFile(folderName+fileName, "w");
			wavFile.write(getSampleRate(), rawData.length, rawData);
			// make a data unit to go with it. 
			clipDataUnit = new ClipDataUnit(startMilliseconds, startMilliseconds, startSample,
					(int)(numSamples), channelMap, fileName, "Manual Clip", null);
		}
		else {
			clipDataUnit = new ClipDataUnit(startMilliseconds, startMilliseconds, startSample,
					(int)(numSamples), channelMap, "", "Manual Clip", rawData);
		}
//		lastClipDataUnit = clipDataUnit;
		clipDataBlock.addPamData(clipDataUnit);
	}

	/**
	 * Called at end of setup of after settings dialog to subscribe data blocks. 
	 */
	public synchronized void subscribeDataBlocks() {
		unSubscribeDataBlocks();
		rawDataBlock = PamController.getInstance().getRawDataBlock(clipControl.clipSettings.dataSourceName);
		setParentDataBlock(rawDataBlock, true);
		
		int nBlocks = clipControl.clipSettings.getNumClipGenerators();
		clipBlockProcesses = new ClipBlockProcess[nBlocks];
		PamDataBlock aDataBlock;
		ClipGenSetting clipGenSetting;
		for (int i = 0; i < nBlocks; i++) {
			clipGenSetting = clipControl.clipSettings.getClipGenSetting(i);
			if (clipGenSetting.enable == false) {
				continue;
			}
			aDataBlock = PamController.getInstance().getDetectorDataBlock(clipGenSetting.dataName);
			if (aDataBlock == null) {
				continue;
			}
			clipBlockProcesses[i] = new ClipBlockProcess(this, aDataBlock, clipGenSetting);
		}
	}
	
	/**
	 * Kill off the old ClipBlockProcesses before creating new ones. 
	 */
	private void unSubscribeDataBlocks() {
		if (clipBlockProcesses == null) {
			return;
		}
		for (int i = 0; i < clipBlockProcesses.length; i++) {
			if (clipBlockProcesses[i] == null) {
				continue;
			}
			clipBlockProcesses[i].disconnect();
		}
	}
	
	public class ClipBlockProcess implements PamObserver {
		
		private PamDataBlock dataBlock;
		
		protected ClipGenSetting clipGenSetting;

		protected ClipProcess clipProcess;

		private ClipDataUnit lastClipDataUnit;

		private WavFile wavFile;
		
		private StandardClipBudgetMaker clipBudgetMaker;
		
		/**
		 * @param dataBlock
		 * @param clipGenSetting
		 */
		public ClipBlockProcess(ClipProcess clipProcess, PamDataBlock dataBlock,
				ClipGenSetting clipGenSetting) {
			super();
			this.clipProcess = clipProcess;
			this.dataBlock = dataBlock;
			this.clipGenSetting = clipGenSetting;
			clipBudgetMaker = new StandardClipBudgetMaker(this);
			dataBlock.addObserver(this, true);
		}
		
		/**
		 * Process a clip request, i.e. make an actual clip from the raw data. This is called back 
		 * from the main thread receiving raw audio data and is called only after any decisions regarding
		 * whether or not a clip should be made have been taken - to get on and make the clip in the
		 * output folder. 
		 * @param clipRequest clip request information
		 * @return 0 if OK or the cause from RawDataUnavailableException if data are not available. 
		 */
		private int processClipRequest(ClipRequest clipRequest) {
			AcousticDataUnit dataUnit = (AcousticDataUnit) clipRequest.dataUnit;
			long rawStart = dataUnit.getStartSample();
			long rawEnd = rawStart + dataUnit.getDuration();
			rawStart -= (clipGenSetting.preSeconds * getSampleRate());
			rawEnd += (clipGenSetting.postSeconds * getSampleRate());
			int channelMap = decideChannelMap(dataUnit.getChannelBitmap());
			
			boolean append = false;
			if (lastClipDataUnit != null) {
				if (rawStart < (lastClipDataUnit.getStartSample()+lastClipDataUnit.getDuration()) &&
						channelMap == lastClipDataUnit.getChannelBitmap()) {
					append = true;
					rawStart = lastClipDataUnit.getStartSample()+lastClipDataUnit.getDuration();
					if (rawEnd < rawStart) {
						return 0; // nothing to do !
					}
				}
			}
			
			double[][] rawData = null;
			try {
				rawData = rawDataBlock.getSamples(rawStart, (int) (rawEnd-rawStart), channelMap);
			}
			catch (RawDataUnavailableException e) {
				return e.getDataCause();
			}
			if (append && clipControl.clipSettings.storageOption == ClipSettings.STORE_WAVFILES) {
				wavFile.append(rawData);
				lastClipDataUnit.setDuration(rawEnd-lastClipDataUnit.getStartSample());
				clipDataBlock.updatePamData(lastClipDataUnit, dataUnit.getTimeMilliseconds());
//				System.out.println(String.format("%d samples added to file", rawData[0].length));
			}
			else {
				ClipDataUnit clipDataUnit;
				long startMillis = dataUnit.getTimeMilliseconds() - (long) (clipGenSetting.preSeconds*1000.);
				if (clipControl.clipSettings.storageOption == ClipSettings.STORE_WAVFILES) {
					String folderName = getClipFileFolder(dataUnit.getTimeMilliseconds(), true);
					String fileName = getClipFileName(startMillis);
					wavFile = new WavFile(folderName+fileName, "w");
					wavFile.write(getSampleRate(), rawData.length, rawData);
					// make a data unit to go with it. 
					clipDataUnit = new ClipDataUnit(startMillis, dataUnit.getTimeMilliseconds(), rawStart,
							(int)(rawEnd-rawStart), channelMap, fileName, dataBlock.getDataName(), null);
				}
				else {
					clipDataUnit = new ClipDataUnit(startMillis, dataUnit.getTimeMilliseconds(), rawStart,
							(int)(rawEnd-rawStart), channelMap, "", dataBlock.getDataName(), rawData);
				}
				lastClipDataUnit = clipDataUnit;
				clipDataBlock.addPamData(clipDataUnit);
			}
			
			return 0; // no error. 
		}

		private String getClipFileName(long timeStamp) {
			return PamCalendar.createFileNameMillis(timeStamp, clipGenSetting.clipPrefix, ".wav");
		}
	

		/**
		 * Decide which channels should actually be used. 
		 * @param channelBitmap
		 * @return
		 */
		protected int decideChannelMap(int channelBitmap) {
			switch (clipGenSetting.channelSelection) {
			case ClipGenSetting.ALL_CHANNELS:
				return rawDataBlock.getChannelMap();
			case ClipGenSetting.DETECTION_CHANNELS_ONLY:
				return channelBitmap;
			case ClipGenSetting.FIRST_DETECTION_CHANNEL_ONLY:
				int overlap = channelBitmap & rawDataBlock.getChannelMap();
				int first = PamUtils.getLowestChannel(overlap);
				return 1<<first;
			}
			return 0;
		}

		/**
		 * disconnect from it's data source. 
		 */
		public void disconnect() {
			dataBlock.deleteObserver(this);
		}
		@Override
		public String getObserverName() {
			return clipProcess.getObserverName();
		}
		@Override
		public PamObserver getObserverObject() {
			return clipProcess.getObserverObject();
		}
		@Override
		public long getRequiredDataHistory(PamObservable o, Object arg) {
			return (long) ((clipGenSetting.preSeconds+clipGenSetting.postSeconds) * 1000.);
		}
		
		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void noteNewSettings() {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void removeObservable(PamObservable o) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void setSampleRate(float sampleRate, boolean notify) {			
		}
		
		@Override
		public void update(PamObservable o, PamDataUnit dataUnit) {
			/**
			 * This one should get updates from the triggering datablock. 
			 */
			if (shouldMakeClip((AcousticDataUnit) dataUnit)) {
				addClipRequest(new ClipRequest(this, dataUnit));
			}
		}

		/**
		 * Function to decide whether or not a clip should be made. 
		 * Might be set to all clips, might be working to a budget. 
		 * Will ultimately be calling into quite a long winded decision
		 * making process. 
		 * @param arg
		 * @return true if a clip should be made, false otherwsie. 
		 */
		private boolean shouldMakeClip(AcousticDataUnit dataUnit) {
			return clipBudgetMaker.shouldStore(dataUnit);
		}
		
	}
	/**
	 * Data needed for a clip request. 
	 * @author Doug Gillespie
	 *
	 */
	public class ClipRequest {

		/**
		 * @param clipBlockProcess
		 * @param dataUnit
		 */
		public ClipRequest(ClipBlockProcess clipBlockProcess,
				PamDataUnit dataUnit) {
			super();
			this.clipBlockProcess = clipBlockProcess;
			this.dataUnit = dataUnit;
		}

		protected ClipBlockProcess clipBlockProcess;
		
		protected PamDataUnit dataUnit;

	}
	/**
	 * @return the clipControl
	 */
	public ClipControl getClipControl() {
		return clipControl;
	}

	/**
	 * @return the clipDataBlock
	 */
	public ClipDataBlock getClipDataBlock() {
		return clipDataBlock;
	}
}
