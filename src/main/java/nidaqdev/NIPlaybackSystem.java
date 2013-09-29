package nidaqdev;

import PamDetection.RawDataUnit;
import soundPlayback.PlaybackControl;
import soundPlayback.PlaybackDialogComponent;
import soundPlayback.PlaybackSystem;

/**
 * Playback system to use when acquiring data from NI cards. 
 * @author Doug Gillespie
 *
 */
public class NIPlaybackSystem extends PlaybackSystem {

	private NIDAQProcess niDaqProcess;

	private Nidaq niDaq;

	private boolean prepared;

	/**
	 * @param niDaqProcess
	 */
	public NIPlaybackSystem(NIDAQProcess niDaqProcess) {
		super();
		this.niDaqProcess = niDaqProcess;
		niDaq = niDaqProcess.getNiDaq();
	}

	@Override
	public PlaybackDialogComponent getDialogComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxChannels() {
		return 2;
	}

	@Override
	public String getName() {
		return "NI Playback";
	}

	@Override
	public boolean playData(RawDataUnit[] data) {
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

		//	long startNanos = System.nanoTime();
		int nSamps =  niDaq.javaPlaybackData(dataBuffer);
		//	long endNanos = System.nanoTime() - startNanos;
		//	System.out.println(String.format("%d NI Samples written for playback in %dus",
		//			nSamps, endNanos/1000));
		return true;
	}

	@Override
	public boolean prepareSystem(PlaybackControl playbackControl,
			int nChannels, float sampleRate) {
		int bn = niDaqProcess.getMasterDevice();
		int[] outchans = new int[nChannels];
		for (int i = 0; i < nChannels; i++) {
			outchans[i] = i;
		}
		int ans = niDaq.javaPreparePlayback(bn, (int)sampleRate, 
				(int)sampleRate/1, outchans);
		prepared = true;
		return true;
	}

	@Override
	public boolean unPrepareSystem() {

		// TODO Auto-generated method stub
		return false;
	}

}
