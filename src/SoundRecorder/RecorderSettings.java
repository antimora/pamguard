package SoundRecorder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import PamController.PamController;
import PamguardMVC.PamRawDataBlock;
import SoundRecorder.trigger.RecorderTrigger;
import SoundRecorder.trigger.RecorderTriggerData;

/**
 * Control parameters for sound recorders.
 * @author Doug Gillespie
 *
 */
public class RecorderSettings implements Serializable, Cloneable {

	static public final long serialVersionUID = 3;

	/**
	 * Name of the raw data source
	 */
	public String rawDataSource;
	
	/**
	 * Bitmap of channels to be saved (need not be all available channels)
	 */
	private int channelBitmap = 3;
	
	/**
	 * bit depth (NOT Byte depth) of the recording format.
	 */
	int bitDepth = 16;
	
	/**
	 * Buffer data so that it can be added to the start of a file
	 */
	boolean enableBuffer = false;
	
	/**
	 * Length of the buffered data to store
	 */
	int bufferLength = 30; //seconds.
	
	/**
	 * Output folder for recording files
	 */
	String outputFolder;
	
	/**
	 * Initials to add to the start of a file name, the rest
	 * of which is made up from the date. 
	 */
	String fileInitials = "PAM";
	
	/**
	 * Unfortunately AudioFileFormat.Type is not serialized, so store
	 * as a string and have getters and setters to sort out the mess
	 */
	private String fileType = "WAVE";
	
	/**
	 * Number of seconds between automatic recordings 
	 */
	int autoInterval = 300;

	/**
	 * Duration of automatic recordings in seconds
	 */
	int autoDuration = 10;
	
	/**
	 * Maximum length of a single file in seconds
	 */
	int maxLengthSeconds = 3600;
	
	/**
	 * Limit the maximum length of a single file in seconds
	 */
	boolean limitLengthSeconds = true;
	
	/**
	 * Maximum length of a single file in Mega bytes
	 */
	long maxLengthMegaBytes = 640;

	/**
	 * Limit the maximum length of a single file in Mega bytes
	 */
	boolean limitLengthMegaBytes = true;
	
	/**
	 * Enable triggers (from detectors). It is possible that the
	 * number of triggers will increase, in which case, this array
	 * will get extended before it is next saved.
	 */
	private ArrayList<RecorderTriggerData> recorderTriggerDatas = new ArrayList<RecorderTriggerData>();
	
//	/**
//	 * Get the trigger state for a trigger
//	 * @param iTrigger trigger index
//	 * @return trigger state
//	 */
//	public boolean getEnableTrigger(int iTrigger) {
//		if (enableTrigger == null || enableTrigger.length <= iTrigger) {
//			return false;
//		}
//		return enableTrigger[iTrigger];
//	}
//	
//	/**
//	 * Set the trigger state for a trigger
//	 * @param iTrigger trigger index
//	 * @param state trigger state
//	 */
//	public void setEnableTrigger(int iTrigger, boolean state) {
//		if (recorderTriggerDatas == null || iTrigger >= recorderTriggerDatas.size()) {
//			return;
//		}
//		recorderTriggerDatas.get(iTrigger).setEnabled(true);
//	}
	/**
	 * Check that everything in the recorderTriggers list is also represented 
	 * in the triggerDataList. <p>
	 * Each recorder trigger can provide a set of default data, which is basically 
	 * what the programmer has put in to give an idea of suitable data budgets and 
	 * trigger conditions. These default parameters then get modified
	 * by the user to suit their own requirements. 
	 */
	protected void createTriggerDataList(ArrayList<RecorderTrigger> recorderTriggers) {
		for (RecorderTrigger rt:recorderTriggers) {
			if (findTriggerData(rt.getDefaultTriggerData().getTriggerName()) == null) {
				recorderTriggerDatas.add(rt.getDefaultTriggerData().clone());
			}
		}
	}
	
	/**
	 * Called before settings are saved to remove settings for any module no longer present. 
	 * @param recorderTriggers
	 */
	protected void cleanTriggerDataList(ArrayList<RecorderTrigger> recorderTriggers) {
		boolean[] present = new boolean[recorderTriggerDatas.size()];
		RecorderTriggerData rtData;
		for (RecorderTrigger rt:recorderTriggers) {
			rtData = findTriggerData(rt.getDefaultTriggerData().getTriggerName());
			if (rtData != null) {
				present[recorderTriggerDatas.indexOf(rtData)] = true;
			}
		}
		for (int i = present.length - 1; i >= 0; i--) {
			if (present[i] == false) {
				recorderTriggerDatas.remove(i);
			}
		}
	}
	
	/**
	 * Find the active trigger data for a trigger of a given name. 
	 * <p>If the trigger data cannot be found, add the default set. 
	 * @param recorderTrigger
	 * @return Active trigger data (started as the default, then got modified by the user)
	 */
	public RecorderTriggerData findTriggerData(RecorderTrigger recorderTrigger) {
		if (findTriggerData(recorderTrigger.getName()) == null) {
			recorderTriggerDatas.add(recorderTrigger.getDefaultTriggerData().clone());
		}
		return findTriggerData(recorderTrigger.getName());
	}

	/**
	 * find a set of trigger data by name. 
	 * @param triggerName trigger name
	 * @return Active trigger data. 
	 */
	public RecorderTriggerData findTriggerData(String triggerName) {
		for (RecorderTriggerData td:recorderTriggerDatas) {
			if (td.getTriggerName().equals(triggerName)) {
				return td;
			}
		}
		return null;
	}
	
	/**
	 * Get the largest (enabled) pre trigger time
	 * @return longest time in seconds. 
	 */
	public double getLongestHistory() {
		double t = 0;
		for (RecorderTriggerData td:recorderTriggerDatas) {
			if (td.isEnabled() == false) {
				continue;
			}
			t = Math.max(t, td.getSecondsBeforeTrigger());
		}
		return t;
	}
	
	/**
	 * If PAMGAURD stops and starts, automatically put the recorder back into
	 * the same mode it was in when acquisition stopped. 
	 */
	boolean autoStart = false;
	
	/**
	 * Memorised status for autoStart
	 * @see autoStart
	 */
	int oldStatus;
	

	RecorderSettings() {
		/*
		 * By default set the output directory as the current directory 
		 * and the data source as the first data source. The second time
		 * PG runs, these will be overwritten by whatever they've been
		 * set to from data in the set file. 
		 */
		try {
			outputFolder = new File(".").getCanonicalPath();
		}
		catch (IOException Ex) {
			
		}
		PamRawDataBlock prd = PamController.getInstance().getRawDataBlock(0);
		if (prd != null) {
			rawDataSource = prd.getDataName();
		}
	}
	
	@Override
	public RecorderSettings clone(){
		try {
			RecorderSettings newSettings = (RecorderSettings) super.clone();
			if (newSettings.recorderTriggerDatas == null) {
				newSettings.recorderTriggerDatas = new ArrayList<RecorderTriggerData>();
			}
			return newSettings;
		}
		catch (CloneNotSupportedException Ex) {
			return null;
		}
	}

	/**
	 * Since AudioFileFormat.Type is not serialized, fileType
	 * is stored as a sting. The getter therefore needs to search
	 * available file types and return the appropriate one.
	 * @return Format type for the audio file
	 * @see javax.sound.sampled.AudioFormat
	 */
	public AudioFileFormat.Type getFileType() {

		AudioFileFormat.Type types[] = AudioSystem.getAudioFileTypes();
		for (int i = 0; i < types.length; i++) {
			if (types[i].toString().equals(fileType)) {
				return types[i];
			}
		}
		return null;
	}

	//transient AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
	public void setFileType(AudioFileFormat.Type fileType) {
		this.fileType = fileType.toString();
	}

	/**
	 * Find a trigger data object with the same name and replace it. 
	 * @param newData
	 */
	public void replaceTriggerData(RecorderTriggerData newData) {
		RecorderTriggerData td = findTriggerData(newData.getTriggerName());
		int ind = recorderTriggerDatas.indexOf(td);
		recorderTriggerDatas.remove(ind);
		recorderTriggerDatas.add(ind, newData);
	}

	/**
	 * get the channel map, but tell it what channels are available !
	 * @param availableChannels available cahnnels (channel map of parent process)
	 * @return channel bitmap
	 */
	public synchronized int getChannelBitmap(int availableChannels) {
		return (channelBitmap & availableChannels);
	}

	/**
	 * @param channelBitmap the channelBitmap to set
	 */
	public synchronized void setChannelBitmap(int channelBitmap) {
		this.channelBitmap = channelBitmap;
	}
	
	/**
	 * Set the bitmap for a given channel. 
	 * @param iChannel channel number
	 * @param state on or of (true or false)
	 * @return channel bitmap
	 */
	public int setChannelBitmap(int iChannel, boolean state) {
		if (state) {
			channelBitmap |= (1<<iChannel);
		}
		else {
			channelBitmap &= ~(1<<iChannel);
		}
		
		return channelBitmap;
	}
	
	/**
	 * Get the state of a single channel. 
	 * @param availableChannels available channels
	 * @param iChannel channel number
	 * @return true or false.
	 */
	public boolean getChannelBitmap(int availableChannels, int iChannel) {
		channelBitmap &= availableChannels;
		return ((channelBitmap & (1<<iChannel)) != 0);
	}
	
	
}
