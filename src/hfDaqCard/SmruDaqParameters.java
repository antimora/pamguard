package hfDaqCard;

import java.io.Serializable;

import Acquisition.SoundCardParameters;
import PamguardMVC.PamConstants;

public class SmruDaqParameters extends SoundCardParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 1L;

	public static final int NCHANNELS = 4;
	
	public static final int[] sampleRates = {62500, 250000, 500000, 1000000};
	public static final double[] lineargains = {0, 1, 2, 4, 8, 16, 32, 64};
	private static double[] gains;
	public static final double[] filters = {10., 100., 2000., 20000., 0.};

	public static final int MAXSAMPLERATE = 1000000;

	public static final double VPEAKTOPEAK = 5.64;
	
	public int sampleRateIndex = 1;
	
	public int channelMask;
	public int[] channelGainIndex = new int[PamConstants.MAX_CHANNELS];
	public int[] channelFilterIndex = new int[PamConstants.MAX_CHANNELS];
	
	SmruDaqParameters() {
	}
	
	static double[] getGains() {
		if (gains == null) {
			gains = new double[lineargains.length];
			for (int i = 0; i < lineargains.length; i++) {
				gains[i] = 20.*Math.log10(lineargains[i]);
			}
		}
		return gains;
	}
	
	public float getSampleRate() {
		return sampleRates[sampleRateIndex];
	}
	
	public double getChannelGain(int channel) {
		getGains();
		return gains[channelGainIndex[channel]];
	}
	
	public double getChannelFilter(int channel) {
		return filters[channelFilterIndex[channel]];
	}
	
	@Override
	public SmruDaqParameters clone() {
		SmruDaqParameters newParams = (SmruDaqParameters) super.clone();
		if (newParams.channelFilterIndex == null) {
			newParams = new SmruDaqParameters();
		}
		return newParams;
	}
	
	public int getSampleRateIndex() {
		return sampleRateIndex;
	}
	
	public void setSampleRateIndex(int rateIndex) {
		sampleRateIndex = rateIndex;
	}

	public int getGainIndex(int channel) {
		return channelGainIndex[channel];
	}
	
	public void setGainIndex(int channel, int gainIndex) {
		channelGainIndex[channel] = gainIndex;
	}
	
	public int getFilterIndex(int channel) {
		return channelFilterIndex[channel];
	}
	
	public void setFilterIndex(int channel, int filterIndex) {
		channelFilterIndex[channel] = filterIndex;
	}
}
