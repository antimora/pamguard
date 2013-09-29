package soundPlayback;

import java.util.ArrayList;

import PamDetection.RawDataUnit;

/**
 * Interface for soundplayback systems. 
 * 
 * @author Doug Gillespie
 *
 */
public abstract class PlaybackSystem {

	public ArrayList<PlaybackChangeObserver> changeObservers = new ArrayList<PlaybackChangeObserver>();
	
	public abstract boolean prepareSystem(PlaybackControl playbackControl, int nChannels, float sampleRate);
	
	public abstract boolean unPrepareSystem();
	
	public abstract int getMaxChannels();
	
	public abstract boolean playData(RawDataUnit[] data);
	
	public abstract PlaybackDialogComponent getDialogComponent();
	
	public abstract String getName();
	
	public void addChangeObserver(PlaybackChangeObserver playbackChangeObserver) {
		if (changeObservers.contains(playbackChangeObserver)) return;
		changeObservers.add(playbackChangeObserver);
	}
	
	public void removeChangeObserver(PlaybackChangeObserver playbackChangeObserver) {
		changeObservers.remove(playbackChangeObserver);
	}
	
	public void notifyObservers() {
		for (PlaybackChangeObserver pbo:changeObservers) {
			pbo.playbackChange();
		}
	}
}
