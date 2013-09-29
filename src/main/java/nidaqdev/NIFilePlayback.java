package nidaqdev;

import java.util.ArrayList;
import java.util.Arrays;

import PamDetection.RawDataUnit;
import soundPlayback.FilePlayback;
import soundPlayback.FilePlaybackDevice;
import soundPlayback.PlaybackParameters;

/**
 * PArt of the system for playback from wav files. The wav file playback system
 * has a number of subsystems, currently including sound card and NI card outputs. 
 * <br>This is NOT the class used to play back data actually acquired using NI cards. 
 * @author Doug Gillespie
 *
 */
public class NIFilePlayback implements FilePlaybackDevice {

	
	private volatile boolean prepared;
	
	private FilePlayback filePlayback;
	
	/**
	 * converts usable NI devices (ones with playback channels)
	 * to numbers used by the NI interface to identify the different cards
	 * on the system .
	 */
	private int[] niDeviceLUT = new int[0];
	
	private String[] niDeviceNames = new String[0];
	
	private Nidaq niDaq;
	
	private ArrayList<NIDeviceInfo> deviceInfo;
	
	public NIFilePlayback(FilePlayback filePlayback) {
		super();
		this.filePlayback = filePlayback;
		niDaq = new Nidaq(null);
		getNIDevices();
	}
	
	private void getNIDevices() {
		deviceInfo = niDaq.getDevicesList();
		int nDevs = 0;
		for (int i = 0; i < deviceInfo.size(); i++) {
			if (deviceInfo.get(i).getOutputChannels() > 0) {
				niDeviceNames = Arrays.copyOf(niDeviceNames, niDeviceNames.length+1);
				niDeviceLUT = Arrays.copyOf(niDeviceLUT, niDeviceLUT.length+1);
				niDeviceNames[nDevs] = deviceInfo.get(i).getName();
				if (deviceInfo.get(i).isSimulated()) {
					niDeviceNames[nDevs] += " (simulated)";
				}
				if (deviceInfo.get(i).isExists() == false) {
					niDeviceNames[nDevs] += " (not present)";
				}
				niDeviceLUT[nDevs] = i;
				nDevs++;
			}
		}
		
	}

	@Override
	public String[] getDeviceNames() {
		return niDeviceNames;
	}

	@Override
	public String getName() {
		return "National Instruments Devices";
	}

	@Override
	public int getNumPlaybackChannels(int devNum) {
		return deviceInfo.get(niDeviceLUT[devNum]).getOutputChannels();
	}

	@Override
	synchronized public boolean playData(RawDataUnit[] data) {
		if (!prepared) {
			return false;
		}
		double[] dataBuffer;
		int buffLen = (int) (data.length * data[0].getDuration());
		dataBuffer = new double[buffLen];
		int n = 0;
		int samps;
		RawDataUnit aDataUnit;
		double[] unitData;
		for (int iB = 0; iB < data.length; iB++) {
			aDataUnit = data[iB];
			samps = (int) aDataUnit.getDuration();
			unitData = aDataUnit.getRawData();
			for (int i = 0; i < samps; i++) {
				dataBuffer[n++] = unitData[i];
			}
		}
		
//		long startNanos = System.nanoTime();
		int nSamps =  niDaq.javaPlaybackData(dataBuffer);
//		long endNanos = System.nanoTime() - startNanos;
//		System.out.println(String.format("%d NI Samples written for playback in %dus",
//				nSamps, endNanos/1000));
		return true;
	}

	@Override
	public boolean preparePlayback(PlaybackParameters playbackParameters) {
		int bn = niDeviceLUT[playbackParameters.deviceNumber];
		bn = deviceInfo.get(bn).getDevNumber();
		int[] outchans = {0, 1};
		int ans = niDaq.javaPreparePlayback(bn, (int)playbackParameters.playbackRate, 
				(int)playbackParameters.playbackRate/1, outchans);
		prepared = true;
		return true;
	}

	@Override
	synchronized public boolean stopPlayback() {
		prepared = false;
		return niDaq.javaStopPlayback();
	}

}
