package simulatedAcquisition;

import PamUtils.LatLong;

/**
 * Class to hold information on a single sound. 
 * These will be held in a list for each simulated object
 * @author Doug Gillespie
 *
 */
public class SimSound {

	double[] waveform;
	
	/**
	 * first sample - at the animal. It will get to the hydrophone later. 
	 */
	long startSample;
	
	long startTimeMillis;
	
	long endSample;
	
	boolean started;
	
	int completeChannels = 0;
	
	LatLong latLong;
	
	double height;
	
	double[][] hydrophoneDelays;
	
	double[][] transmissionGains;
	
	/**
	 * Signal level of the sound 1m from the hydrophone
	 */
	double[] soundAmplitude; 
	
	public SimSound(long startSample, long startTimeMillis, LatLong latLong, double height, double[] waveform) {
		this.startSample = startSample;
		this.startTimeMillis = startTimeMillis;
		this.latLong = latLong.clone();
		this.height = height;
		this.waveform = waveform;
		this.endSample = startSample + waveform.length;
		started = false;
	}
	
	public void setCompleteChannel(int iChan) {
		completeChannels |= 1<<iChan;
	}
	
	public boolean isComplete(int channelMap) {
		return ((channelMap & completeChannels) == channelMap);
	}

	public double[][] getHydrophoneDelays() {
		return hydrophoneDelays;
	}

	public void setHydrophoneDelays(double[][] delays) {
		this.hydrophoneDelays = delays;
	}
	
	public double getHydrophoneDelay(int iPhone, int iDelay) {
		return hydrophoneDelays[iPhone][iDelay];
	}
	
	public int getNumDelays() {
		return hydrophoneDelays[0].length;
	}
	public double[][] getTransmissionGains() {
		return transmissionGains;
	}

	public void setTransmissionGains(double[][] gains) {
		this.transmissionGains = gains;
	}
	
	public double getTranmissionGain(int iPhone, int iGain) {
		return transmissionGains[iPhone][iGain];
	}

	public double[] getSoundAmplitude() {
		return soundAmplitude;
	}
	
	public double getSoundAmplitude(int iChan) {
		return soundAmplitude[iChan];
	}

	public void setSoundAmplitude(double[] soundAmplitude) {
		this.soundAmplitude = soundAmplitude;
	}
	
}
