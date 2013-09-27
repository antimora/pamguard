package soundPlayback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import asiojni.ASIOFilePlaybackSystem;

import nidaqdev.NIFilePlayback;
import Acquisition.DaqSystem;
import PamDetection.RawDataUnit;

/**
 * Playback of sound from wav files. 
 * Other real time sound sources must handle their own playback so that 
 * timing of in and out is synchronised correctly. 
 * @author Doug Gillespie
 * @see DaqSystem
 *
 */
public class FilePlayback extends PlaybackSystem {

	private PlaybackControl playbackControl;
			
	private FilePlaybackDialogComponent filePlaybackDialogComponent;
	
	protected ArrayList<FilePlaybackDevice> filePBDevices = new ArrayList<FilePlaybackDevice>();
	
	private FilePlaybackDevice currentDevice;

	private boolean realTimePlayback;
	
	List<RawDataUnit[]> realTimeQueue;

	private int maxQueueLength;

	private RealTimeQueueReader realTimeQueueReader;
	
	public FilePlayback(PlaybackControl playbackControl) {
		this.playbackControl = playbackControl;
		filePBDevices.add(currentDevice = new SoundCardFilePlayback(this));
		filePBDevices.add(new NIFilePlayback(this));
		filePBDevices.add(new ASIOFilePlaybackSystem(this));
		filePlaybackDialogComponent = new FilePlaybackDialogComponent(this);
	}

	public int getMaxChannels() {
		FilePlaybackDevice device = filePBDevices.get(playbackControl.playbackParameters.deviceType);
		return device.getNumPlaybackChannels(getDeviceNumber());
	}
	
	public int getDeviceNumber() {
		return playbackControl.playbackParameters.deviceNumber;
	}

	@Override
	synchronized public boolean prepareSystem(PlaybackControl playbackControl,
			int nChannels, float sampleRate) {

		unPrepareSystem();
		
		realTimePlayback = playbackControl.isRealTimePlayback();
		if (realTimePlayback) {
			realTimeQueue = Collections.synchronizedList(new LinkedList<RawDataUnit[]>());
			maxQueueLength = 10; // hold about a second of data. 
			realTimeQueueReader = new RealTimeQueueReader();
			Thread t = new Thread(realTimeQueueReader);
			t.start();
		}
		
		currentDevice = filePBDevices.get(playbackControl.playbackParameters.deviceType);
		return currentDevice.preparePlayback(playbackControl.playbackParameters);
		

	}

	synchronized public boolean unPrepareSystem() {
		if (currentDevice != null) {
			return currentDevice.stopPlayback();
		}
		return false;
	}

	public boolean playData(RawDataUnit[] data) {
		
		if (currentDevice == null) {
			return false;
		}
		if (realTimePlayback) {
			if (realTimeQueue.size() < maxQueueLength) {
				realTimeQueue.add(data.clone());
			}
			else {
				System.out.println("Dumping playback data since output running too slow for input");
				realTimeQueue.clear();
			}
			return true;
		}
		else {
			return currentDevice.playData(data.clone());
		}

	}
	
	class RealTimeQueueReader implements Runnable {

		private boolean keepRunning = true;
		
		@Override
		public void run() {
			RawDataUnit[] data;
			while (keepRunning) {
				try {
					while (realTimeQueue.size() > 0) {
						data = realTimeQueue.remove(0);
						currentDevice.playData(data);
					}
				}
				catch (IndexOutOfBoundsException e) {
					// can happen if all data were deleted at just that moment due to queue getting 
					// too large. 
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		public void stopThread() {
			keepRunning = false;
		}
		
	}

	public PlaybackDialogComponent getDialogComponent() {
		return filePlaybackDialogComponent;
	}

	@Override
	public String getName() {
		return "Soundcard Playback";
	}

	public PlaybackControl getPlaybackControl() {
		return playbackControl;
	}


}
