package soundPlayback;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import Acquisition.SoundCardSystem;
import PamDetection.RawDataUnit;

public class SoundCardPlayback extends PlaybackSystem {
	
	private soundCardDialogComponent soundCardDialogComponent;
	
	private AudioFormat audioFormat;
	
	private SourceDataLine sourceDataLine;
	
	byte[] rawAudio;

	public SoundCardPlayback(SoundCardSystem soundCardSystem) {
		soundCardDialogComponent = new soundCardDialogComponent(soundCardSystem);
	}

	@Override
	public PlaybackDialogComponent getDialogComponent() {
		return soundCardDialogComponent;
	}

	@Override
	public int getMaxChannels() {
		return 2;
	}

	@Override
	public String getName() {
		return "Sound Card Playback";
	}

	synchronized public boolean prepareSystem(PlaybackControl playbackControl,
			int nChannels, float sampleRate) {

		unPrepareSystem();
		
		if (nChannels <= 0 || nChannels > getMaxChannels()) return false;
		
		audioFormat = new AudioFormat(sampleRate, 16, nChannels, true, true);

		ArrayList<Mixer.Info> mixerinfos = SoundCardSystem.getOutputMixerList();
		Mixer.Info thisMixerInfo = mixerinfos.get(playbackControl.playbackParameters.deviceNumber);
		Mixer thisMixer = AudioSystem.getMixer(thisMixerInfo);
		if (thisMixer.getSourceLineInfo().length <= 0){
			thisMixer.getLineInfo();
			return false;
		}
		
		try {
			// try to get the device of choice ...
			sourceDataLine = (SourceDataLine) thisMixer.getLine(thisMixer.getSourceLineInfo()[0]);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();
		} catch (Exception Ex) {
			Ex.printStackTrace();
			sourceDataLine = null;
			return false;
		}
		
		return true;
	}

	synchronized public boolean unPrepareSystem() {
		if (sourceDataLine == null) return false;
		sourceDataLine.stop();
		sourceDataLine.close();
		sourceDataLine = null;
		return true;
	}
	
	public boolean playData(RawDataUnit[] data) {

		if (sourceDataLine == null) return false;
		/*
		 * need to check the buffer size - will be wrong the first time, but can then write easily
		 * to it. then write data into the buffer then write buffer to dataline
		 */
		int nChan = data.length;
		int sampleSize = 2;
		RawDataUnit dataUnit = data[0];
		int nSamples = (int) dataUnit.getDuration();
		int bufferSize = nSamples * sampleSize * nChan; 
		if (rawAudio == null || rawAudio.length != bufferSize) {
			rawAudio = new byte[bufferSize];
		}
		// now write the data to the buffer, packing as we go.
		int byteNo;
		double[] rawData;
		short int16Data;
		for (int iChan = 0; iChan < nChan; iChan++) {
			byteNo = iChan * sampleSize;
			rawData = data[iChan].getRawData();
			for (int i = 0; i < rawData.length; i++) {
				int16Data = (short) (rawData[i] * 32768);
				rawAudio[byteNo+1] = (byte) (int16Data & 0xFF);
				rawAudio[byteNo] = (byte) (int16Data>>>8 & 0xFF);
				byteNo += nChan * sampleSize;
			}
		}
		sourceDataLine.write(rawAudio, 0, bufferSize);
		
		return false;
	}

}
