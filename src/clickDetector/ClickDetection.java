package clickDetector;

import java.util.Arrays;

import pamMaths.PamVector;

import clickDetector.ClickClassifiers.basic.BasicClickIdentifier;
import clickDetector.ClickDetector.ChannelGroupDetector;
import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionProcess;
import Localiser.bearingLocaliser.BearingLocaliser;
import PamDetection.AbstractDetectionMatch;
import PamDetection.AbstractLocalisation;
import PamDetection.PamDetection;
import PamUtils.PamUtils;
import PamguardMVC.ChannelListManager;
import PamguardMVC.PamRawDataBlock;
import fftFilter.FFTFilter;
import fftFilter.FFTFilterParams;
import fftManager.Complex;
import fftManager.FastFFT;

/**
 * Class for Click Detector clicks. 
 * @author Doug Gillespie
 *
 */
public class ClickDetection extends PamDetection<PamDetection, PamDetection> {

//	private Click click;
	
	static public final int CLICK_CLICK = 0;
	static public final int CLICK_NOISEWAVE = 5; // compatibility with RC
	
	/*
	 * Bitwise flags to go into the clickFlags variable. 
	 */
	static public final int CLICK_FLAG_ECHO = 0x1;

	/**
	 * Click number in list. 
	 */
	long clickNumber;

	int triggerList;

	/**
	 * Wave data is the original double waveform from the
	 * sound source of the click detector. 
	 */
	private double[][] waveData;
	
	private double[][] filteredWaveData;
	
	/**
	 * Compressed wave data is signed integer 8 bit data, scaled so that
	 * the maximum value is 127 or -127, this is related to 
	 * the original waveData by the scaling constant waveAmplitude
	 * where compressedWaveData = waveData * 127. / waveAmplitude;
	 */
	private byte[][] compressedWaveData;
	
	private double waveAmplitude;

	private double[] amplitude;

	boolean tracked;

	// these next three are mainly to do with saving in RC type files.
	int flags;

	/**
	 * Click data type
	 */
	byte dataType = CLICK_CLICK;

	protected long filePos;

	/**
	 * Click species. 
	 */
	private byte clickType; // click species. 

	private ClickDetector clickDetector;

	int eventId;
	
	boolean discard = false;
	
	private double ICI = -1; // filled in by click train id stuff.
	
	private double tempICI; // used when no event information available.  

	// some stuff used many times, so held internally to avoid repeats
	private double[][] powerSpectra;

	private double[] totalPowerSpectrum;

	private Complex[][] complexSpectrum;
	
	private int currentSpectrumLength = 0;
	
	private double[][] analyticWaveform;
	
	private int nChan;
	
	private int shortestFFTLength = 0;
	
	public int getShortestFFTLength() {
		if (shortestFFTLength > 0) {
			return shortestFFTLength;
		}
		shortestFFTLength = PamUtils.getMinFftLength(duration);
		return shortestFFTLength;
	}

	private ClickLocalisation clickLocalisation;
	private int nDelays;
	private double[] delays;
	private ChannelGroupDetector channelGroupDetector;
	
	private ClickDetectionMatch clickDetectionMatch;
	
	/**
	 * click flags continas bitwise information about the click - such as whether it's an echo.
	 */
	private int clickFlags;

//	public Click(ClickDetector clickDetector, long startSample, int nChan,
//			long duration, int channelList, int triggerList) {
	public ClickDetection(int channelBitmap, long startSample, long duration, 
			ClickDetector clickDetector, ChannelGroupDetector channelGroupDetector, int triggerList) {
		
		super(clickDetector.absSamplesToMilliseconds(startSample), channelBitmap, startSample, duration);
		this.setChannelGroupDetector(channelGroupDetector);
		nChan = PamUtils.getNumChannels(channelBitmap);
		nDelays = nChan*(nChan-1)/2;
		delays = new double[nDelays];
		this.setClickDetector(clickDetector);
		amplitude = new double[nChan];
		this.triggerList = triggerList;
		if (clickDetector != null && channelGroupDetector == null) {
			this.setChannelGroupDetector(clickDetector.findChannelGroupDetector(channelBitmap));
		}
//		this.click = click;
	}
	
	
	public ClickDetection() {
		super(0, 0, 0, 0);
	}

//	public Click getClick() {
//		return click;
//	}
//
//	public void setClick(Click click) {
//		this.click = click;
//	}

	public boolean isTracked() {
		return tracked;
	}

	public void setTracked(boolean tracked) {
		this.tracked = tracked;
	}

	public int getEventId() {
		return eventId;
	}

	public void setEventId(int eventId) {
		this.eventId = eventId;
	}
	
	/**
	 * 
	 * @return true if the echo flag is set. 
	 */
	public boolean isEcho() {
		return ((clickFlags & CLICK_FLAG_ECHO) == CLICK_FLAG_ECHO);
	}
	
	/**
	 * 
	 * @param isEcho set the click echo flag. 
	 */
	public void setEcho(boolean isEcho) {
		if (isEcho) {
			clickFlags |= CLICK_FLAG_ECHO;
		}
		else {
			clickFlags &= ~CLICK_FLAG_ECHO;
		}
	}

	/**
	 * Gets the angle to a click from the time delay on two hydrophones based on
	 * sound speed.
	 * 
	 * @param sampleRate
	 * @return The angle to the click (ahead = 0 degrees)
	 */
//	public double getAngle(float sampleRate) {
//		// need to know which hydrophones are being used. 
//		// just pass the array the channel bitmap. 
//		// first need to converrt the channel map to a hydrophone map
//		// and there is no way to do that here !
//		int hydrophoneList = ((AcquisitionProcess) clickDetector.getSourceProcess()).
//			getAcquisitionControl().ChannelsToHydrophones(channelList);
//		double ang = (double) delay / sampleRate
//				/ ArrayManager.getArrayManager().getCurrentArray().getSeparationInMillis(hydrophoneList);
//		ang = Math.min(1., Math.max(ang, -1.));
//		return Math.acos(ang) * 180. / Math.PI;
//	}

	/**
	 * Returns the complex spectrum for a given channel using the shortest
	 * possible FFT length
	 * 
	 * @param channel
	 * @return The complex spectrum
	 */
	public Complex[] getComplexSpectrum(int channel) {
		return getComplexSpectrum(channel, PamUtils.getMinFftLength(duration));
	}
	
	public void setComplexSpectrum(){
		currentSpectrumLength = PamUtils.getMinFftLength(duration);
	}

	/**
	 * 
	 * Returns the complex spectrum for a given channel using a set FFT length
	 * 
	 * @param channel
	 * @param fftLength
	 * @return the complex spectrum
	 */
	public Complex[] getComplexSpectrum(int channel, int fftLength) {
		double[] paddedRawData;
		double[] rawData;
		int i, mn;
		if (complexSpectrum == null) {
			complexSpectrum = new Complex[nChan][];
		}
		if (complexSpectrum[channel] == null
				|| complexSpectrum.length != fftLength / 2) {
			paddedRawData = new double[fftLength];
			rawData = getWaveData(channel);
			mn = Math.min(fftLength, (int)duration);
			for (i = 0; i < mn; i++) {
				paddedRawData[i] = rawData[i];
			}
			for (i = mn; i < fftLength; i++) {
				paddedRawData[i] = 0;
			}
			complexSpectrum[channel] = getClickDetector().fastFFT.rfft(paddedRawData, null,
					PamUtils.log2(fftLength));
			currentSpectrumLength = fftLength;
		}
		return complexSpectrum[channel];
	}
	
	
	/**
	 *  Returns the complex spectrum for waveform using a set FFT length
	 * @param waveData
	 * @return the complex Spectrum
	 */
	public  static Complex[] getComplexSpectrum(double[] rawData, int fftLength){
		double[] paddedRawData;
		int i, mn;
	
	
			paddedRawData = new double[fftLength];
			mn = Math.min(fftLength, rawData.length);
			for (i = 0; i < mn; i++) {
				paddedRawData[i] = rawData[i];
			}
			for (i = mn; i < fftLength; i++) {
				paddedRawData[i] = 0;
			}
			FastFFT fft=new FastFFT();
			
			Complex[] c = fft.rfft(paddedRawData, null,
					PamUtils.log2(fftLength));

		
		return c;
	}
	
	/**
	 * Find out whether there are complex 
	 * spectrum data - and if there are, data may
	 * get cleaned up. 
	 * @return true if complex spec data exist. 
	 */
	public boolean hasComplexSpectrum() {
		return (complexSpectrum != null);
	}

	public int getCurrentSpectrumLength() {
		return currentSpectrumLength;
	}
	
	/**
	 * Get the power spectrum of the entire click. 
	 * @param channel channel number
	 * @return power spectrum
	 */
	public double[] getPowerSpectrum(int channel) {
		int fftLen = FastFFT.nextBinaryExp((int)getDuration());
		return getPowerSpectrum(channel, fftLen);
	}

	/**
	 * Returns the power spectrum for a given channel (square of magnitude of
	 * complex spectrum)
	 * 
	 * @param channel channel number
	 * @param fftLength
	 * @return Power spectrum
	 */
	public double[] getPowerSpectrum(int channel, int fftLength) {
		if (powerSpectra == null) {
			powerSpectra = new double[nChan][];
		}
		if (fftLength == 0) {
			fftLength = getCurrentSpectrumLength();
		}
		if (fftLength == 0) {
			fftLength = PamUtils.getMinFftLength(duration);
		}
		if (powerSpectra[channel] == null
				|| powerSpectra[channel].length != fftLength / 2) {
			Complex[] cData = getComplexSpectrum(channel, fftLength);
			powerSpectra[channel] = new double[fftLength / 2];
			for (int i = 0; i < fftLength / 2; i++) {
				powerSpectra[channel][i] = cData[i].magsq();
			}
		}
		return powerSpectra[channel];
	}

	/**
	 * Returns the sum of the power spectra for all channels
	 * 
	 * @param fftLength
	 * @return Sum of power spectra
	 */
	public double[] getTotalPowerSpectrum(int fftLength) {
		if (fftLength == 0) {
			fftLength = getCurrentSpectrumLength();
		}
		if (fftLength == 0) {
			fftLength = PamUtils.getMinFftLength(duration);
		}
		double[] ps;
		if (totalPowerSpectrum == null
				|| totalPowerSpectrum.length != fftLength / 2) {
			totalPowerSpectrum = new double[fftLength / 2];
			for (int c = 0; c < nChan; c++) {
				ps = getPowerSpectrum(c, fftLength);
				for (int i = 0; i < fftLength / 2; i++) {
					totalPowerSpectrum[i] += ps[i];
				}
			}
		}
		return totalPowerSpectrum;
	}
	
	/**
	 * Get the analytic waveform for  a given channel
	 * @param iChan channel index
	 * @return analytic waveform
	 */
	public double[] getAnalyticWaveform(int iChan) {
		if (analyticWaveform == null) {
			analyticWaveform = new double[nChan][];
		}
//		if (analyticWaveform[iChan] == null) {
			analyticWaveform[iChan] = getClickDetector().getHilbert().getHilbert(getWaveData(iChan));
//		}
		return analyticWaveform[iChan];
	}
	
	/**
	 * Get filtered or unfiltered analytic waveform. Easy access method for click 
	 * detector modules which can let this function work out what they want rather 
	 * than having to write their own. 
	 * @param iChan channel number
	 * @param filtered true if you want data to be filtered
	 * @param fftFilterParams fft filter parameters. 
	 * @return analystic waveform. 
	 */
	public double[] getAnalyticWaveform(int iChan, boolean filtered, FFTFilterParams fftFilterParams) {
		if (filtered == false || fftFilterParams == null) {
			return getAnalyticWaveform(iChan);
		}
		else {
			return getFilteredAnalyticWaveform(fftFilterParams, iChan);
		}
	}
	
	/**
	 * Get a filtered version of the analytic waveform. In principle, this could be made more efficient
	 * since the calc is done partly in freqeucny domain - so could save a couple of fft's back and forth. 
	 * @param fftFilterParams FFT filter parameters. 
	 * @param iChan channel number
	 * @return envelope of the filtered data. 
	 */
	public double[] getFilteredAnalyticWaveform(FFTFilterParams fftFilterParams, int iChan) {
		if (analyticWaveform == null) {
			analyticWaveform = new double[nChan][];
		}
//		if (analyticWaveform[iChan] == null) {
			analyticWaveform[iChan] = getClickDetector().getHilbert().
				getHilbert(getFilteredWaveData(fftFilterParams, iChan));
//		}
		return analyticWaveform[iChan];
	}

	/**
	 * Get the analytic waveform for all channels 
	 * if filter params = null, then return normal analytic waveform  
	 * @param fftFilterParams
	 * @return analystic waveforms 
	 */
	public double[][] getFilteredAnalyticWaveform(FFTFilterParams fftFilterParams) {
		if (analyticWaveform == null) {
			analyticWaveform = new double[nChan][];
		}
		for (int iChan = 0; iChan < nChan; iChan++) {
			if (fftFilterParams != null) {
				analyticWaveform[iChan] = getClickDetector().getHilbert().
				getHilbert(getFilteredWaveData(fftFilterParams, iChan));
			}
			else {
				analyticWaveform[iChan] = getAnalyticWaveform(iChan);
			}
		}
		return analyticWaveform;
	}
	
	

	/**
	 * Calculates the total energy within a particular frequency band
	 * 
	 * @see BasicClickIdentifier
	 * @param freqs
	 * @return In Band Energy
	 */
	public double inBandEnergy(double[] freqs) {
		double e = 0;
		int fftLen = getShortestFFTLength();
		double[][] specData = new double[nChan][];
		for (int i = 0; i < nChan; i++) {
			specData[i] = getPowerSpectrum(i, fftLen);
		}
		int f1 = Math.max(0, (int) Math.floor(freqs[0] * fftLen
				/ getClickDetector().getSampleRate()));
		int f2 = Math.min((fftLen / 2) - 1, (int) Math.ceil(freqs[1]
				* fftLen / getClickDetector().getSampleRate()));
		for (int iChan = 0; iChan < nChan; iChan++) {
			for (int f = f1; f <= f2; f++) {
				e += specData[iChan][f];
			}
		}
		if (e > 0.) {
			return 10 * Math.log10(e) + 172;
		} else
			return -100;
	}

	/**
	 * Calculates the length of a click in seconds averaged over all channels
	 * 
	 * @see BasicClickIdentifier
	 * @param percent
	 *            Fraction of total click energy to use in the calculation
	 * @return click length in seconds
	 */
	public double clickLength(double percent) {
		/*
		 * work out the length of the click - this first requries a bit of
		 * smoothing out of the rectified waveform, then an iterative search
		 * around either side of the peak, then average for all channels
		 */
		double sum = 0;
		for (int i = 0; i < nChan; i++) {
			sum += clickLength(i, percent);
		}
		return sum / nChan;
	}

	/**
	 * Calculates the length of a click in seconds for a particular channel
	 * 
	 * @see BasicClickIdentifier
	 * @param channel
	 * @param percent
	 *            Fraction of total click energy to use in the calculation
	 * @return Click Length (seconds)
	 */
	public double clickLength(int channel, double percent) {
		int length = 0;
		int nAverage = 3;
		double[] waveData = getWaveData(channel);
		double[] smoothData = new double[waveData.length];
		double squaredData;
		double totalData = 0;
		double dataMaximum = 0;
		int maxPosition = 0;
		for (int i = 0; i < smoothData.length; i++) {
			smoothData[i] = Math.pow(waveData[i], 2);
		}
		for (int i = 0; i < smoothData.length - nAverage; i++) {
			for (int j = 1; j < nAverage; j++) {
				smoothData[i] += smoothData[i + j];
			}
			totalData += smoothData[i];
			if (smoothData[i] > dataMaximum) {
				dataMaximum = smoothData[i];
				maxPosition = i;
			}
		}
		/*
		 * Now start at the maximum position and search out back and forwards
		 * until enough energy has been found use a generic peakwidth function
		 * for this, since it's the same basic process that does the width of
		 * the frequency peak
		 */
		length = getSpikeWidth(smoothData, maxPosition, percent);
		return length / getClickDetector().getSampleRate();
	}

	/**
	 * Calculates the width of a peak - either time or frequency data
	 * 
	 * @param data
	 * @param peakPos
	 * @param percent
	 * @return Width of spike in whatever bins are used for raw data given
	 */
	private int getSpikeWidth(double[] data, int peakPos, double percent) {
		/*
		 * This is used both by the length measuring and the frequency peak
		 * measuring functions
		 */
		int width = 1;
		int len = data.length;
		double next, prev;
		int inext, iprev;
		double targetEnergy = 0;
		if (percent > 100) {
			return len;
		}
		for (int i = 0; i < len; i++) {
			targetEnergy += data[i];
		}
		targetEnergy *= percent / 100;
		double foundEnergy = data[peakPos];
		inext = peakPos + 1;
		iprev = peakPos - 1;
		while (foundEnergy < targetEnergy) {
			next = prev = 0;
			if (inext < len)
				next = data[inext];
			if (iprev >= 0)
				prev = data[iprev];
			if (next > prev) {
				foundEnergy += next;
				inext++;
				width++;
			} else if (next < prev) {
				foundEnergy += prev;
				iprev--;
				width++;
			} else {
				foundEnergy += (next + prev);
				inext++;
				iprev--;
				width += 2;
			}
			if (iprev < 0 && inext >= len) {
				System.out.println("Can't find required energy in click");
			}
		}

		return width;
	}

	public double peakFrequency(double[] searchRange) {
		/*
		 * search range will be in Hz, so convert to bins NB - the
		 */
		int fftLength = getShortestFFTLength();
		double[] powerSpec = getTotalPowerSpectrum(fftLength);

		int bin1 = (int) Math.max(0, Math.floor(searchRange[0] * fftLength
				/ getClickDetector().getSampleRate()));
		int bin2 = (int) Math.min(fftLength / 2 - 1, Math.ceil(searchRange[1]
				* fftLength / getClickDetector().getSampleRate()));
		int peakPos = 0;
		double peakEnergy = 0;
		for (int i = bin1; i <= bin2; i++) {
			if (powerSpec[i] > peakEnergy) {
				peakEnergy = powerSpec[i];
				peakPos = i;
			}
		}
		return peakPos * getClickDetector().getSampleRate() / fftLength;
	}

	public double peakFrequencyWidth(double peakFrequency, double percent) {
		int fftLength = getShortestFFTLength();
		int peakPos = (int) (peakFrequency * fftLength / getClickDetector()
				.getSampleRate());
		int width = getSpikeWidth(getTotalPowerSpectrum(fftLength), peakPos,
				percent);
		return width * getClickDetector().getSampleRate() / fftLength;
	}
	
	public double getMeanFrequency(double[] searchRange) {

		int fftLength = getShortestFFTLength();
		double[] powerSpec = getTotalPowerSpectrum(fftLength);
		
		int bin1 = (int) Math.max(0, Math.floor(searchRange[0] * fftLength
				/ getClickDetector().getSampleRate()));
		int bin2 = (int) Math.min(fftLength / 2 - 1, Math.ceil(searchRange[1]
				* fftLength / getClickDetector().getSampleRate()));
		double top = 0, bottom = 0;
		for (int i = bin1; i <= bin2; i++) {
			top += (i * powerSpec[i]);
			bottom += powerSpec[i];
		}
		double meanFreq = top / bottom; // mean Freq in bins
		return meanFreq * getClickDetector().getSampleRate() / fftLength;
	}

	/**
	 * Get filtered waveform data for a single channel. <p>
	 * Data are filtered in the frequency domain using an FFT / Inverse FFT. 
	 * @param filterParams filter parameters
	 * @param channelIndex channel index
	 * @return filtered waveform data
	 */
	public double[] getFilteredWaveData(FFTFilterParams filterParams, int channelIndex) {
		filteredWaveData = getFilteredWaveData(filterParams);
		return filteredWaveData[channelIndex];
	}
	
	/**
	 * Get filtered waveform data for all channels. <p>
	 * Data are filtered in the frequency domain using an FFT / Inverse FFT. 
	 * @param filterParams filter parameters
	 * @return array of filtered data
	 */
	public double[][] getFilteredWaveData(FFTFilterParams filterParams) {
		if (filteredWaveData == null || filterParams != oldFFTFilterParams) {
			filteredWaveData = makeFilteredWaveData(filterParams);
		}
		return filteredWaveData;
	}
	
	private FFTFilterParams oldFFTFilterParams; 
	private double[][] makeFilteredWaveData(FFTFilterParams filterParams) {
		double[][] waveData = getWaveData();
		int nChan = waveData.length;
		int dataLen = waveData[0].length;
		filteredWaveData = new double[nChan][dataLen];
		FFTFilter filter = getClickDetector().getFFTFilter(filterParams);
		for (int i = 0; i < nChan; i++) {
			filter.runFilter(waveData[i], filteredWaveData[i]);
		}
		oldFFTFilterParams = filterParams;
		return filteredWaveData;
	}
	
	/**
	 * convenience method to get filtered or unfiltered data for a single channel.
	 * @param filtered flag saying you want it filtered
	 * @param fftFilterParams filter parameters. 
	 * @return data filtered or otherwise. 
	 */
	public double[][] getWaveData(boolean filtered, FFTFilterParams fftFilterParams) {
		if (filtered == false || fftFilterParams == null) {
			return getWaveData();
		}
		else {
			return getFilteredWaveData(fftFilterParams);
		}
	}
	/**
	 * convenience method to get filtered or unfiltered data for a single channel. 
	 * @param channelIndex channel index
	 * @param filtered flag saying you want it filtered
	 * @param fftFilterParams filter parameters. 
	 * @return data filtered or otherwise. 
	 */
	public double[] getWaveData(int channelIndex, boolean filtered, FFTFilterParams fftFilterParams) {
		if (filtered == false || fftFilterParams == null) {
			return getWaveData(channelIndex);
		}
		else {
			return getFilteredWaveData(fftFilterParams, channelIndex);
		}
	}

	/**
	 * Get raw waveform data for a given click channel index. 
	 * @param channelIndex channel index
	 * @return waveform data
	 */
	public double[] getWaveData(int channelIndex) {
		double[][] wD = getWaveData();
		if (wD == null) {
			return null;
		}
		return wD[channelIndex];
	}

	/**
	 * 
	 * @return waveform data for all channels. Convert from compressed (int16) data 
	 * if necessary. 
	 */
	public double[][] getWaveData() {
		if (waveData != null) {
			return waveData;
		}
		if (compressedWaveData == null) {
			return null;
		}

		waveAmplitude = getWaveAmplitude();

		int nChan = compressedWaveData.length;
		int nSamp = compressedWaveData[0].length;
		waveData = new double[nChan][nSamp];
		
		double scale = waveAmplitude/127.;
		for (int i = 0; i < nChan; i++) {
			for (int j = 0; j < nSamp; j++) {
				waveData[i][j] = scale * compressedWaveData[i][j];
			}
//			rotateWaveData(waveData[i]);
		}
		
		
		
		return waveData;
	}

	/**
	 * Rotate a waveform so that the ends are both at zero. 
	 * @param waveform from a single channel 
	 */
	private void rotateWaveData(double[] w) {
		int n = w.length;
		double b = w[0];
		double a = (w[n-1]-b)/(n-1);
		for (int i = 0; i < n; i++) {
			w[i] -= (a*i + b);
		}
//		System.out.println("Rotate wave data !");
	}


	public void setWaveData(double[][] waveData) {
		this.waveData = waveData;
	}
	
	/**
	 * Get compressed waveform data in int8 format, scaled
	 * so that the maximum range >-127 to +127 is utilised. 
	 * @return arrays of waveform data. 
	 */
	public byte[][] getCompressedWaveData() {
		if (compressedWaveData != null) {
			return compressedWaveData;
		}
		// otherwise, create the compressed data ...
		if (waveData == null) {
			return null;
		}
		
		waveAmplitude = getWaveMax(waveData);

		int nChan = waveData.length;
		int nSamp = waveData[0].length;
		compressedWaveData = new byte[nChan][nSamp];
		
		double scale = 127./waveAmplitude;
		for (int i = 0; i < nChan; i++) {
			for (int j = 0; j < nSamp; j++) {
				compressedWaveData[i][j] = (byte) (scale * waveData[i][j]);
			}
		}
		
		return compressedWaveData;
	}
	
	/**
	 * Set the compressed wave data (used when reading back from file).
	 * @param compressedWaveData
	 * @param waveAmplitude
	 */
	public void setCompressedData(byte[][] compressedWaveData, double waveAmplitude) {
		this.compressedWaveData = compressedWaveData;
		this.waveAmplitude = waveAmplitude;
	}
	
	/**
	 * @return the waveAmplitude - the double precision amplitude of the orignal
	 * wave data. 
	 */
	public double getWaveAmplitude() {
		return waveAmplitude;
	}

	/**
	 * get the maximum value of the wavedata. Will be used for scaling. 
	 * @param waveData wavedata 2D array
	 * @return maximum absolute value. 
	 */
	private double getWaveMax(double[][] waveData) {
		if (waveData == null) {
			return 0;
		}
		int l1 = waveData.length;
		if (l1 == 0) {
			return 0;
		}
		int l2 = waveData[0].length;
		if (l2 == 0) {
			return 0;
		}
		double max = 0;
		for (int i = 0; i < l1; i++) {
			for (int j = 0; j < l2; j++) {
				max = Math.max(max, Math.abs(waveData[i][j]));
			}
		}
		return max;
	}

	/**
	 * Free up as much click memory as possible. <p>
	 * Ensures that waveform data are retained in a compressed (int8) format
	 * so that all other data can be reconstructed if necessary. 
	 */
	public void freeClickMemory() {
//		waveData = null;
		powerSpectra = null;
		complexSpectrum = null;
		totalPowerSpectrum = null;
		analyticWaveform = null;
		filteredWaveData = null;
		/*
		 * Check the double waveform data can convert into a byte
		 * array, then get rid of it. In reality, this conversion was
		 * probably already done when the click was written to binary file.  
		 */
		if (getCompressedWaveData() != null) {
			waveData = null;
		}
	}
//
//	public double getMeanAmplitude()
//	{
//		double a = 0;
//		for (int i = 0; i < amplitude.length; i++) {
//			a += amplitude[i];
//		}
//		return a / amplitude.length;
//	}
//	/**
//	 * @return Returns the dBamplitude.
//	 */
//	public double getDBamplitude() {
//		return dBamplitude;
//	}
//
//	/**
//	 * @param bamplitude The dBamplitude to set.
//	 */
//	public void setDBamplitude(double bamplitude) {
//		dBamplitude = bamplitude;
//	}
//
//	/**
//	 * @return Returns the delay.
//	 */
//	public int getDelay() {
//		return delay;
//	}
	/**
	 * Set the time of arrival delay in samples.
	 * @param delay delay in samples
	 */
	public void setDelay(int iDelay, double delay) {
		if (getClickLocalisation() == null) {
			makeClickLocalisation(0, null);
		}
		if (delays == null) {
			delays = new double[iDelay+1];
		}
		if (iDelay >= delays.length) {
			delays = Arrays.copyOf(delays, iDelay+1);
		}
		delays[iDelay] = delay;
		if (iDelay == 0) {
			clickLocalisation.setFirstDelay((int) delay);
		}
		clickLocalisation.addLocContents(AbstractLocalisation.HAS_BEARING | AbstractLocalisation.HAS_AMBIGUITY);
		
	}
	
	/**
	 * return a list of delays. 
	 * @return
	 */
	public double[] getDelays() {
		return delays;
	}
	
//	/**
//	 * Set the correction to the delay in fractions of a sample
//	 * (derived from quadratic interpolation around the peak of the cross correlation function)
//	 * @param delayCorrection delay correction
//	 */
//	public void setDelayCorrection(double delayCorrection) {
//		if (getClickLocalisation() == null) {
//			makeClickLocalisation();
//		}
//		clickLocalisation.setDelayCorrection(delayCorrection);
//		clickLocalisation.addLocContents(AbstractLocalisation.HAS_BEARING);
//	}

	/**
	 * Set the amplitude for a given channel
	 * @param channel channel number
	 * @param amplitude amplitude
	 */
	public void setAmplitude(int channel, double amplitude) {
		if (this.amplitude == null) {
			this.amplitude = new double[1];
		}
		if (this.amplitude.length <= channel) {
			this.amplitude = Arrays.copyOf(this.amplitude, channel+1);
		}
		this.amplitude[channel] = amplitude;
	}

	public double getAmplitude(int channel) {
		return amplitude[channel];
	}
	
	/**
	 * Returns the angle in degrees for compatibilty with older version of click detector
	 * @return angle of the click detection in degrees
	 */
	public double getAngle() {
		double angle = 0;
		if (getClickLocalisation() != null) {
			angle = getClickLocalisation().getBearing(0) * 180 / Math.PI;
		}
		return angle;
	}
	
	public double getMeanAmplitude()
	{
		double a = 0;
		for (int i = 0; i < amplitude.length; i++) {
			a += amplitude[i];
		}
		return a / amplitude.length;
	}
	
	private void makeClickLocalisation(int arrayType, PamVector[] arrayAxes) {
		int hydrophoneList = this.channelBitmap;
		if (clickDetector != null && clickDetector.getParentDataBlock() != null) {
			ChannelListManager clm = ((PamRawDataBlock) clickDetector.getParentDataBlock()).getChannelListManager();
			if (clm != null) {
				hydrophoneList = clm.channelIndexesToPhones(channelBitmap);
			}
		}
//		AcquisitionControl dc = findDaqControl();
//		if (dc != null) {
//			hydrophoneList = dc.ChannelsToHydrophones(channelBitmap);
//		}
		BearingLocaliser bearingLocaliser = null;
		if (getChannelGroupDetector() != null) {
			getChannelGroupDetector().getBearingLocaliser();
		}
		if (bearingLocaliser != null) {
			setClickLocalisation(new ClickLocalisation(this, 0, hydrophoneList,
					getChannelGroupDetector().getBearingLocaliser().getArrayType(),
					getChannelGroupDetector().getBearingLocaliser().getArrayAxis()));
		}
		else {
			setClickLocalisation(new ClickLocalisation(this, 0, hydrophoneList, 
					arrayType, arrayAxes));
		}
	}
	
	private void setReferenceHydrophones(int channelMap) {
		int hydrophoneList = channelMap;
		AcquisitionControl dc = findDaqControl();
		if (dc != null) {
			hydrophoneList = dc.ChannelsToHydrophones(channelBitmap);
		}
		if (clickLocalisation != null) {
			clickLocalisation.setReferenceHydrophones(hydrophoneList);
		}
	}
	
	private AcquisitionControl findDaqControl() {
		if (getClickDetector() == null) return null;
		if (getClickDetector().getSourceProcess() == null) return null;
		try {
		  return ((AcquisitionProcess) getClickDetector().getSourceProcess()).
					getAcquisitionControl();
		}
		catch (ClassCastException e) {
			return null;
		}
	}

	public ClickLocalisation getClickLocalisation() {
		return clickLocalisation;
	}

	public void setClickLocalisation(ClickLocalisation clickLocalisation) {
		this.clickLocalisation = clickLocalisation;
		this.localisation = clickLocalisation;
	}
	
	@Override
	public AbstractDetectionMatch getDetectionMatch(int type){
		if (clickDetectionMatch!=null && clickDetectionMatch.getClickType()==type ){
			return clickDetectionMatch;
		}
		else{
			clickDetectionMatch=new ClickDetectionMatch(this,type);
			return clickDetectionMatch;
		}
	}
	
	@Override
	public AbstractDetectionMatch getDetectionMatch(){
		if (clickDetectionMatch!=null){
			return clickDetectionMatch;
		}
		else{
			clickDetectionMatch=new ClickDetectionMatch(this);
			return clickDetectionMatch;
		}
	}
	

	

	public int getNChan() {
		return PamUtils.getNumChannels(getChannelBitmap());
	}
	
	

	@Override
	public void setChannelBitmap(int channelBitmap) {
		this.nChan = PamUtils.getNumChannels(channelBitmap);
		setReferenceHydrophones(channelBitmap);
		super.setChannelBitmap(channelBitmap);
	}

	/**
	 * @param clickType the clickType (click species) to set
	 */
	public void setClickType(byte clickType) {
		this.clickType = clickType;
	}

	/**
	 * @return the clickType (click species)
	 */
	public byte getClickType() {
		return clickType;
	}

	/**
	 * 
	 * @return the type of data - click, noise, etc. 
	 */
	public byte getDataType() {
		return dataType;
	}

	/**
	 * 
	 * @param dataType the type of data - click, noise, etc. 
	 */
	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	/**
	 * @param clickDetector the clickDetector to set
	 */
	public void setClickDetector(ClickDetector clickDetector) {
		this.clickDetector = clickDetector;
	}

	/**
	 * @return the clickDetector
	 */
	public ClickDetector getClickDetector() {
		return clickDetector;
	}


	/**
	 * @return the iCI
	 */
	public double getICI() {
		if (clickDetector.getClickControl().isViewerMode()) {
			int nSuper = getSuperDetectionsCount();
			if (nSuper == 0) {
				return -1;
			}
			return ICI;
		}
		else {
			return ICI;
		}
	}


	/**
	 * @param iCI the iCI to set
	 */
	public void setICI(double iCI) {
		ICI = iCI;
	}


	@Override
	public void addSuperDetection(PamDetection superDetection) {
		super.addSuperDetection(superDetection);
	}

	@Override
	public void removeSuperDetection(PamDetection superDetection) {
		super.removeSuperDetection(superDetection);
		ICI = -1;
	}


	protected void setChannelGroupDetector(ChannelGroupDetector channelGroupDetector) {
		this.channelGroupDetector = channelGroupDetector;
	}


	public ChannelGroupDetector getChannelGroupDetector() {
		return channelGroupDetector;
	}


	/**
	 * @param clickFlags the clickFlags to set
	 */
	public void setClickFlags(int clickFlags) {
		this.clickFlags = clickFlags;
	}


	/**
	 * @return the clickFlags
	 */
	public int getClickFlags() {
		return clickFlags;
	}


	/**
	 * @param tempICI the tempICI to set
	 */
	public void setTempICI(double tempICI) {
		this.tempICI = tempICI;
	}


	/**
	 * @return the tempICI
	 */
	public double getTempICI() {
		return tempICI;
	}
	
}
