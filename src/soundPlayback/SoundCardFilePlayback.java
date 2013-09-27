package soundPlayback;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Mixer.Info;

import Acquisition.SoundCardSystem;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;

public class SoundCardFilePlayback implements FilePlaybackDevice {

	private FilePlayback filePlayback;

	private String[] soundCardNames;

	private AudioFormat audioFormat;

	private SourceDataLine sourceDataLine;

	private byte[] rawAudio;

	public SoundCardFilePlayback(FilePlayback filePlayback) {
		super();
		this.filePlayback = filePlayback;

		ArrayList<Info> mixers = SoundCardSystem.getOutputMixerList();
		soundCardNames = new String[mixers.size()];
		for (int i = 0; i < mixers.size(); i++) {
			soundCardNames[i] = mixers.get(i).getName();
		}
	}

	@Override
	public String getName() {
		return "Sound Card";
	}

	@Override
	public String[] getDeviceNames() {
		return soundCardNames;
	}

	@Override
	public int getNumPlaybackChannels(int devNum) {
		return 2;
	}

	@Override
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
//		int availale = sourceDataLine.available();
//		long bN = System.currentTimeMillis();
		int written = sourceDataLine.write(rawAudio, 0, bufferSize);
//		long aN = System.currentTimeMillis() - bN;
//		System.out.println(String.format("T %3.2f Wrote %d of %d into buffer with %d available in %d millis", 
//				(double)(bN-startPlayback)/1000., written, bufferSize, availale, aN));
		
		return true;
	}
	
	long startPlayback;

	@Override
	public boolean preparePlayback(PlaybackParameters playbackParameters) {


		audioFormat = new AudioFormat(playbackParameters.playbackRate, 16, 
				PamUtils.getNumChannels(playbackParameters.channelBitmap), true, true);

		ArrayList<Mixer.Info> mixerinfos = SoundCardSystem.getOutputMixerList();
		Mixer.Info thisMixerInfo = mixerinfos.get(playbackParameters.deviceNumber);
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
		startPlayback = System.currentTimeMillis();

		return true;
	}

	@Override
	public boolean stopPlayback() {
		if (sourceDataLine == null) return false;
		sourceDataLine.stop();
		sourceDataLine.close();
		sourceDataLine = null;
		return true;
	}

}
