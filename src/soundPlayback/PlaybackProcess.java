package soundPlayback;

import PamController.PamController;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

/**
 * Pam Process for sound playback (corralls data from multiple channels
 * and then passes it on to the PlaybackSystem.
 * <p>
 * For file analysis, playback is through a chosen sound card. For 
 * playback of real time data, the playbackSystem is hanbdled by the real time
 * acquisition system - so samples stay synchronised.
 * @author Doug Gillespie
 * @see PlaybackSystem
 *
 */
public class PlaybackProcess extends PamProcess {

	private PlaybackControl playbackControl;
	
	private RawDataUnit rawDataUnits[];
	
	private int[] channelPos;
	
	int haveChannels;
	
	int runningChannels;
	
	boolean running = false;
	
	public PlaybackProcess(PlaybackControl playbackControl) {
		super(playbackControl, null);
		this.playbackControl = playbackControl;
	}

	@Override
	public void prepareProcess() {
//		super.prepareProcess();
//		System.out.println("Playback prepare process");
		if (playbackControl.playbackSystem != null) {
			playbackControl.playbackSystem.prepareSystem(playbackControl, 
					runningChannels, playbackControl.choseSampleRate());
		}
	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		super.setSampleRate(sampleRate, notify);
//		System.out.println("Playback set sample rate to " + sampleRate);
		if (playbackControl.playbackParameters.defaultSampleRate) {
			playbackControl.playbackParameters.playbackRate = sampleRate;
		}
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#getSampleRate()
	 */
	@Override
	public float getSampleRate() {
		return super.getSampleRate();
	}

	@Override
	public void pamStart() {
		if (playbackControl.playbackSystem == null) return;
		running = true;
//		System.out.println("Playback start");
	}
	
	@Override
	public void pamStop() {
		if (playbackControl.playbackSystem == null) return;
		playbackControl.playbackSystem.unPrepareSystem();
		running = false;
	}

	@Override
	synchronized public void newData(PamObservable o, PamDataUnit arg) {
		if (playbackControl.playbackSystem == null) return;
		int channel = PamUtils.getSingleChannel(arg.getChannelBitmap());
		int pos = channelPos[channel];
		if (pos < 0) return; // it's a channel we don't want
		/*
		 * Need to ensure that channels get grouped in the right order. 
		 * Ordering may get confused if playback was turned off and on 
		 * during a run. 
		 * It should always be true, that if haveChannels is zero, then
		 * pos should also be zero. 
		 */
		if (pos == 0) {
			haveChannels = 0;
//			return;
		}
		rawDataUnits[pos] = (RawDataUnit) arg;
		haveChannels |= arg.getChannelBitmap();
		if (haveChannels == playbackControl.playbackParameters.channelBitmap) {
			// do something with the data
			playbackControl.playbackSystem.playData(rawDataUnits);
			playbackControl.setPlaybackProgress(rawDataUnits[pos].getTimeMilliseconds());
			haveChannels = 0;
			for (int i = 0; i < rawDataUnits.length; i++) {
				rawDataUnits[i] = null;
			}
		}	
	}

	@Override
	synchronized public void noteNewSettings() {
		
		PamDataBlock sourceData = PamController.getInstance().getRawDataBlock(
				playbackControl.playbackParameters.dataSource);
		
		setParentDataBlock(sourceData);

		runningChannels = PamUtils.getNumChannels(playbackControl.playbackParameters.channelBitmap);
		rawDataUnits = new RawDataUnit[runningChannels];
		channelPos = PamUtils.getChannelPositionLUT(playbackControl.playbackParameters.channelBitmap);
		haveChannels = 0;
		
		// if it's running, restart it to get new sample rate, device, etc.

		if (playbackControl.playbackSystem != null && running) {
			playbackControl.playbackSystem.unPrepareSystem();
			playbackControl.playbackSystem.prepareSystem(playbackControl, runningChannels, playbackControl.choseSampleRate());
		}
		
		super.noteNewSettings();
	}


}
