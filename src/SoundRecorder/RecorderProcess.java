package SoundRecorder;

import java.util.ListIterator;

import javax.sound.sampled.AudioFileFormat;


import PamDetection.RawDataUnit;
import PamUtils.CheckStorageFolder;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import SoundRecorder.trigger.RecorderTriggerData;

/**
 * Process raw audio data prior to storage. Raw data blocks
 * only contain one channel of data each. RecorderProcess stacks
 * up the data from all channels before passing it on the the 
 * RecorderStorage
 * @author Doug
 * @see SoundRecorder.RecorderStorage
 * @see SoundRecorder.RecorderControl
 */
public class RecorderProcess extends PamProcess {
	
	RecorderControl recorderControl;
	
	int collectedChannels;
	
	long sampleStartTime;
	
	private long lastRecordedSample;
	
	double[][] soundData;
	
	RecordingInfo recordingInfo;
	
	PamDataBlock<RecorderDataUnit> recordingData;
	
	/**
	 * Data flowing in from source (i.e. Pam Started)
	 */
	private boolean dataComing;
	
//	private long lastOpening = 0;
	
	/**
	 * Flag that when recording starts the buffer should be grabbed
	 * and inserted at the start of the recording. If recording is 
	 * already running, then this flag will have no effect. It is always
	 * cleared at the end of a recording. 
	 * It's an integer value, so that different recorder triggers can demand 
	 * different amounts of buffer. 
	 */
	double grabBuffer = 0;
	
	String actionTrigger;
	
	public RecorderProcess(RecorderControl recorderControl) {
		
		super(recorderControl, null);
		
		this.recorderControl = recorderControl;
		
		recordingData = new PamDataBlock<RecorderDataUnit>(RecorderDataUnit.class, "Recordings", this, 0);
		
		recordingData.SetLogging(new RecorderLogger(recorderControl, recordingData));
		
		addOutputDataBlock(recordingData);
		
	}

	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		long history = 0;
		if (recorderControl.recorderSettings.enableBuffer) {
			history = (recorderControl.recorderSettings.bufferLength + 1) * 1000;
		}
		history = Math.max(history, 
				(long) (recorderControl.recorderSettings.getLongestHistory() * 1000.));
		return history;
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		
		recorderControl.newData((PamDataBlock) o, arg);
		
		RawDataUnit rawDataUnit = (RawDataUnit) arg;
		
		if (recorderControl.getRecorderStatus() == RecorderControl.IDLE) return;
		
		// check the file length
		if (recorderControl.recorderStorage.getFileName() != null) {
			if (recorderControl.recorderSettings.limitLengthMegaBytes && 
					recorderControl.recorderSettings.maxLengthMegaBytes > 0 &&
					(recorderControl.recorderStorage.getFileBytes() / (1<<20)) >=
						recorderControl.recorderSettings.maxLengthMegaBytes) {
//				recorderControl.recorderStorage.closeStorage();
				recorderControl.recorderStorage.reOpenStorage(arg.getTimeMilliseconds());
			}
		}
		if (recorderControl.recorderStorage.getFileName() != null) {
			if (recorderControl.recorderSettings.limitLengthSeconds && 
					recorderControl.recorderSettings.maxLengthSeconds > 0 &&
					recorderControl.recorderStorage.getFileMilliSeconds() / 1000 >=
						recorderControl.recorderSettings.maxLengthSeconds) 
			{
				// a new file will immediately be reopend
//				recorderControl.recorderStorage.closeStorage();
				recorderControl.recorderStorage.reOpenStorage(arg.getTimeMilliseconds());
			} 
		}
		/*
		 * at this point, we may need to start a recording - the controller
		 * tells us we want one - if there isn't one, start one, getting the buffer
		 * if necessary before starting to throw new data in. This is all done here so
		 * ensure that any data in the buffer is synchronised with anything here. 
		 */ 
		if (recorderControl.recorderStorage.getFileName() == null) {
			/* need to set up and start a recording, and to use all the data in 
			*  the buffer if desired. 
			*  the file name is based on time, so the first thing to do is to work
			*  this out based on the time of the first data block that will actually be used. 
			*  This may be the current one, or it may be earler in the buffered data.   
			*/
			PamRawDataBlock b = (PamRawDataBlock) o;
			RawDataUnit dataUnit;
			long timeNow = arg.getTimeMilliseconds();
			long recordingStart = timeNow;
			if (recorderControl.recorderSettings.enableBuffer && 
					grabBuffer > 0) {
				synchronized (b) {
					ListIterator<RawDataUnit> rawIterator = b.getListIterator(0);
					dataUnit = b.getFirstUnit();
					if (dataUnit != null) {
						recordingStart -= (grabBuffer * 1000.);
						recordingStart = Math.max(recordingStart, dataUnit.getTimeMilliseconds());
						// also, since the buffer isn't emptied, check that we don't ever overlap
						// recordings. 
						recordingStart = Math.max(recordingStart, lastRecordedSample);
					}
					
				}
			}
			if (recorderControl.recorderSettings.getFileType() == null) {
				recorderControl.recorderSettings.setFileType(AudioFileFormat.Type.WAVE);
			}
//			if (true) {
//			System.out.println("Opening storage at sample " + rawDataUnit.getStartSample() +
//					" Samples since last opening = " + (rawDataUnit.getStartSample() - lastOpening));
//			lastOpening = rawDataUnit.getStartSample();
//			}
			int chanMap = recorderControl.recorderSettings.getChannelBitmap(b.getChannelMap());
			recorderControl.recorderStorage.openStorage(recorderControl.recorderSettings.getFileType(),
					recordingStart, getSampleRate(), 
					PamUtils.getNumChannels(chanMap), 
					recorderControl.recorderSettings.bitDepth);
			recordingInfo = new RecordingInfo(recorderControl.recorderStorage.getFileName(),
					getSampleRate(), chanMap,
					recorderControl.recorderSettings.bitDepth, recordingStart, 
					rawDataUnit.getStartSample(), actionTrigger);
			synchronized (b) {
				ListIterator<RawDataUnit> rawIterator = b.getListIterator(0);
				while (rawIterator.hasNext()) {
					dataUnit = rawIterator.next();
					if (dataUnit == null) break;
					if (dataUnit.getTimeMilliseconds() < recordingStart) continue;
					recordData(o, dataUnit);
				}
			}
			recorderControl.sayRecorderStatus();
		}
		else {
			/*
			 * If this file is already open, then can just get on with it.
			 * If it just went through the file opening process, then this unit
			 * will already have been recorded at the end of the loop above which
			 * is why this is contained within the else{}.
			 */
			recordData(o, rawDataUnit);
		}
	}
	
	private void recordData(PamObservable o, RawDataUnit rawDataUnit) {		
		
		int wantedChannels = recorderControl.recorderSettings.getChannelBitmap(0xFFFFFFFF);
		
		if ((rawDataUnit.getChannelBitmap() & wantedChannels) == 0) return;
		
		int nChannels = PamUtils.getNumChannels(wantedChannels);
		if (soundData == null || soundData.length != nChannels) {
			soundData = new double[nChannels][];
		}
		/*
		 * there is a mapping of channel numbers if not all are being used
		 * which needs to be sorted out here. For instance, if there were 
		 * two channels 0 and 1, and we're only recording channel 1, then 
		 * the data from channel one needs to go into position 0 in the soundData
		 */
		int thisChannel = PamUtils.getSingleChannel(rawDataUnit.getChannelBitmap());
		int channelPos = PamUtils.getChannelPos(thisChannel, wantedChannels);
		if (channelPos < 0) return;
		if (collectedChannels > 0 && sampleStartTime != rawDataUnit.getStartSample()) {
			// data coming from a different sample start time - reset
			collectedChannels = 0;
		}
		sampleStartTime = rawDataUnit.getStartSample();
		collectedChannels = PamUtils.SetBit(collectedChannels, thisChannel, true);
		soundData[channelPos] = rawDataUnit.getRawData();
		if (collectedChannels == wantedChannels) {
			lastRecordedSample = rawDataUnit.getTimeMilliseconds();
			recordSoundData(lastRecordedSample, soundData);
			collectedChannels = 0;
			recordingInfo.endTimeMillis = lastRecordedSample;
		}
	}
	private boolean recordSoundData(long dataTimeMillis, double[][] soundData){
		
		if (soundData == null) return false;
		
		return recorderControl.recorderStorage.addData(dataTimeMillis, soundData);
		
	}
	protected void setRecordStatus(int status, String actionTrigger) {
		this.actionTrigger = actionTrigger;
		if (status == RecorderControl.RECORDING) {
			startRecording(false);
		}
		else {
			stopRecording();
		}
	}
	
	private boolean startRecording(boolean forceStart) {
		// if it's already recording, then there is nothing to do.
		if (forceStart) {
			stopRecording();
		}
		return false;
	}
	
	protected boolean stopRecording() {
		grabBuffer = 0;
		recorderControl.recorderStorage.closeStorage();
		return false;	
	}
	
	protected void storageClosed() {
		RecorderDataUnit newDataUnit = new RecorderDataUnit(recordingInfo.startTimeMillis, recordingInfo);
		recordingData.addPamData(newDataUnit);		
	}
	

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		super.setSampleRate(sampleRate, notify);
		recorderControl.setSampleRate(sampleRate);
	}

	@Override
	public void prepareProcess() {
		CheckStorageFolder scf = new CheckStorageFolder(recorderControl.getUnitName());
		boolean ok = scf.checkPath(recorderControl.recorderSettings.outputFolder, true);
		recorderControl.setFolderStatus(ok);
	}

	@Override
	public void pamStart() {
		
		dataComing = true;
		if (getParentDataBlock() != null) {
			recorderControl.recorderSettings.getChannelBitmap(getParentDataBlock().getChannelMap());
		}
			
		recorderControl.enableRecording();

		// press whichever button was last pressed...
		if (recorderControl.recorderSettings.autoStart) {
			recorderControl.buttonCommand(recorderControl.recorderSettings.oldStatus);
		}
		
	}

	@Override
	public void pamStop() {
		/*
		 * If it's set to autoStart, get the current status
		 */
//		if (recorderControl.recorderSettings.autoStart &&
//				PamController.getInstance().getPamStatus() == PamController.PAM_RUNNING) {
//			recorderControl.recorderSettings.oldStatus = recorderControl.pressedButton;
//		}
//		else {
//			recorderControl.recorderSettings.oldStatus = RecorderView.BUTTON_OFF;
//		}
		// Close off any current file.
		recorderControl.buttonCommand(RecorderView.BUTTON_OFF);
		//recorderControl.recorderStorage.closeStorage();
		dataComing = false;
		recorderControl.enableRecording();
		soundData = null;
	}


	public boolean isDataComing() {
		return dataComing;
	}


}
