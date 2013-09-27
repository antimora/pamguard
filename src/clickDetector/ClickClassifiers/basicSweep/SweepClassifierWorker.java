package clickDetector.ClickClassifiers.basicSweep;

import java.util.Arrays;

import Filters.SmoothingFilter;
import PamUtils.PamUtils;
import Spectrogram.WindowFunction;
import clickDetector.ClickControl;
import clickDetector.ClickDetection;
import clickDetector.ClickClassifiers.ClickIdInformation;
import fftManager.Complex;
import fftManager.FastFFT;

/**
 * Do the actual work of the click seep classifier
 * Separated into a separate class for clarity and to keep 
 * Separate from all the control functions. 
 * @author Doug Gillespie
 *
 */
public class SweepClassifierWorker {

	private SweepClassifier sweepClassifier;
	
	private ClickControl clickControl;
	
	int nChannels;
	double sampleRate;
	int[][] lengthData;
	double[][] specData;
	double[][] smoothSpecData;
	ZeroCrossingStats[] zeroCrossingStats;

	public SweepClassifierWorker(ClickControl clickControl,
			SweepClassifier sweepClassifier) {
		super();
		this.clickControl = clickControl;
		this.sweepClassifier = sweepClassifier;
	}

	public ClickIdInformation identify(ClickDetection click) {
		clearExtractedParams();
		nChannels = PamUtils.getNumChannels(click.getChannelBitmap());
		sampleRate = clickControl.getClickDetector().getSampleRate();
		
		int n = sweepClassifier.sweepClassifierParameters.getNumSets();
		SweepClassifierSet scs;
		for (int i = 0; i < n; i++) {
			scs = sweepClassifier.sweepClassifierParameters.getSet(i);
			if (scs.enable) {
				if (classify(click, scs)) {
					return new ClickIdInformation(scs.speciesCode, scs.discard);
				}
			}
		}
		return new ClickIdInformation(0);
	}

	private boolean classify(ClickDetection click, SweepClassifierSet scs) {
		if (scs.enableLength) {
			if (testLength(click, scs) == false) {
				return false;
			}
		}
		if (scs.enableEnergyBands) {
			if (testEnergyBands(click, scs) == false) {
				return false;
			}
		}
		if (scs.enablePeak) {
			if (testPeakFreq(click, scs) == false) {
				return false;
			}
		}
		if (scs.enableWidth) {
			if (testPeakWidth(click, scs) == false) {
				return false;
			}
		}
		if (scs.enableMean) {
			if (testMeanFreq(click, scs) == false) {
				return false;
			}
		}
		if (scs.enableZeroCrossings) {
			if (testZeroCrossings(click, scs) == false) {
				return false;
			}
		}
		
		return true;
	}

	private boolean testLength(ClickDetection click, SweepClassifierSet scs) {
		int[][] lData = getLengthData(click, scs);
		if (lData == null) {
			return false;
		}
		double[] realLengthData = new double[nChannels];
		for (int i = 0; i < nChannels; i++) {
			realLengthData[i] = (lData[i][1]-lData[i][0]) / sampleRate * 1000;
		}
		switch (scs.channelChoices) {
		case SweepClassifierSet.CHANNELS_REQUIRE_ALL:
			for (int i = 0; i < nChannels; i++) {
				if (realLengthData[i] < scs.minLength || realLengthData[i] > scs.maxLength) {
					return false;
				}
			}
			return true;
		case SweepClassifierSet.CHANNELS_REQUIRE_ONE:
			for (int i = 0; i < nChannels; i++) {
				if (realLengthData[i] >= scs.minLength && realLengthData[i] <= scs.maxLength) {
					return true;
				}
			}
			return false;
		case SweepClassifierSet.CHANNELS_USE_MEANS:
			double m = realLengthData[0];
			for (int i = 1; i < nChannels; i++) {
				m += realLengthData[i];
			}
			m /= nChannels;
			return (m >= scs.minLength && m <= scs.maxLength);
		}
		return false;
	}

	private boolean testEnergyBands(ClickDetection click, SweepClassifierSet scs) {
		double[][] specData = getSpecData(click, scs);
		double[] testEnergy = new double[nChannels];
		double[][] controlEnergy = new double[nChannels][SweepClassifierSet.nControlBands];
		int nSpecs = specData.length;
		for (int i = 0; i < nSpecs; i++) {
			testEnergy[i] = 10*Math.log10(pickSpecEnergy(specData[i], scs.testEnergyBand));
			for (int b = 0; b < SweepClassifierSet.nControlBands; b++) {
				controlEnergy[i][b] = 10*Math.log10(pickSpecEnergy(specData[i], scs.controlEnergyBand[b]));
			}
		}
		switch (scs.channelChoices) {
		case SweepClassifierSet.CHANNELS_REQUIRE_ALL:
			for (int i = 0; i < nChannels; i++) {
				for (int b = 0; b < SweepClassifierSet.nControlBands; b++) {
					if (testEnergy[i] - controlEnergy[i][b] < scs.energyThresholds[b]) {
						return false;
					}
				}
			}
			return true;
		case SweepClassifierSet.CHANNELS_REQUIRE_ONE:
			int okBands;
			for (int i = 0; i < nChannels; i++) {
				okBands = 0;
				for (int b = 0; b < SweepClassifierSet.nControlBands; b++) {
					if (testEnergy[i] - controlEnergy[i][b] >= scs.energyThresholds[b]) {
						okBands ++;
					}
				}
				if (okBands == SweepClassifierSet.nControlBands) {
					return true;
				}
			}
			return false;
		case SweepClassifierSet.CHANNELS_USE_MEANS:
			// sum all data onto the first channel bin.
			for (int b = 0; b < SweepClassifierSet.nControlBands; b++) {
				if (testEnergy[0] - controlEnergy[0][b] < scs.energyThresholds[b]) {
					return false;
				}
			}
			return true;
			
		}
		
		return true;
	}

	/**
	 * Get some energy measurement from some spectral data. 
	 * @param specData single channel of power spectrum data
	 * @param frequency limits (Hz)
	 * @return summed energy (allowing for non integer bins)
	 */
	private double pickSpecEnergy(double[] specData, double[] freqLims) {
		double binsPerHz = specData.length * 2 / sampleRate;
		double rBin1 = freqLims[0] * binsPerHz;
		double rBin2 = freqLims[1] * binsPerHz;
		rBin1 = Math.max(rBin1, 0);
		rBin1 = Math.min(rBin1, specData.length);
		rBin2 = Math.max(rBin2, 0);
		rBin2 = Math.min(rBin2, specData.length);
		if (rBin2 <= rBin1) return 0;
		int bin1 = (int) Math.floor(rBin1);
		int bin2 = (int) Math.ceil(rBin2);
		bin2 = Math.min(bin2, specData.length-1);
		if (bin1 < 0 || bin2 >= specData.length) {
			return Double.NaN;
		}
		double e = 0;
		for (int i = bin1; i < bin2; i++) {
			e += specData[i];
		}
		e -= specData[bin1] * (rBin1-bin1);
		e -= specData[bin2-1] * (bin2-rBin2);
		return e;
	} 

	int[] peakBins;
	private int[] getPeakBins(ClickDetection click, SweepClassifierSet scs) {
		if (peakBins != null) {
			return peakBins;
		}
		double[][] specData = getSmoothSpecData(click, scs);
		double binsPerHz = specData.length * 2 / sampleRate;
		if (specData == null) {
			return null;
		}
		int nSpecs = specData.length;
		peakBins = new int[nSpecs];
		for (int i = 0; i < nSpecs; i++) {
			peakBins[i] = getPeakBin(specData[i], scs.peakSearchRange);
		}
		return peakBins;
	}
	private boolean testPeakFreq(ClickDetection click, SweepClassifierSet scs) {
		int[] peakBin = getPeakBins(click, scs);
		if (peakBin == null) {
			return false;
		}
		double binsPerHz = specData[0].length * 2 / sampleRate;
		double[] peakFreq = new double[peakBin.length];
		int nSpecs = specData.length;
		for (int i = 0; i < nSpecs; i++) {
			peakBin[i] = getPeakBin(specData[i], scs.peakSearchRange);
			peakFreq[i] = peakBin[i] / binsPerHz;
		}
		
		switch (scs.channelChoices) {
		case SweepClassifierSet.CHANNELS_REQUIRE_ALL:
			for (int i = 0; i < nSpecs; i++) {
				if (peakFreq[i] < scs.peakRange[0] || peakFreq[i] > scs.peakRange[1]) {
					return false;
				}
			}
			return true;
		case SweepClassifierSet.CHANNELS_REQUIRE_ONE:
			for (int i = 0; i < nSpecs; i++) {
				if (peakFreq[i] >= scs.peakRange[0] && peakFreq[i] <= scs.peakRange[1]) {
					return true;
				}
			}
			return false;
		case SweepClassifierSet.CHANNELS_USE_MEANS:
			if (peakFreq[0] >= scs.peakRange[0] && peakFreq[0] <= scs.peakRange[1]) {
				return true;
			}
			return false;
		}
		
		return true;
	}

	private int getPeakBin(double[] specData, double[] peakSearchRange) {
		double binsPerHz = getBinsPerHz();
		double rBin1 = peakSearchRange[0] * binsPerHz;
		double rBin2 = peakSearchRange[1] * binsPerHz;
		rBin1 = Math.max(rBin1, 0);
		rBin2 = Math.min(rBin2, specData.length);
		int bin1 = (int) Math.floor(rBin1);
		int bin2 = (int) Math.ceil(rBin2);
		double maxVal = specData[bin1];
		int maxInd = bin1;
		for (int i = bin1; i < bin2; i++) {
			if (specData[i] > maxVal) {
				maxVal = specData[i];
				maxInd = i;
			}
		}
		return maxInd;
	}

	private boolean testPeakWidth(ClickDetection click, SweepClassifierSet scs) {
		int[] peakBin = getPeakBins(click, scs);
		double[][] specData = getSmoothSpecData(click, scs);
		int nSpec = peakBin.length;
		int[] peakWidth = new int[nSpec];
		for (int i = 0; i < nSpec; i++) {
			peakWidth[i] = getPeakWidth(specData[i], peakBin[i], scs.peakWidthThreshold);
		}
		int f1, f2;
		double binsPerHz = getBinsPerHz();
		f1 = (int) (scs.peakWidthRange[0] * binsPerHz);
		f2 = (int) (scs.peakWidthRange[1] * binsPerHz);
		switch(scs.channelChoices) {
		case SweepClassifierSet.CHANNELS_REQUIRE_ALL:
			for (int i = 0; i < nSpec; i++) {
				if (peakWidth[i] < f1 || peakWidth[i] > f2) {
					return false;
				}
			}
			return true;
		case SweepClassifierSet.CHANNELS_REQUIRE_ONE:
			for (int i = 0; i < nSpec; i++) {
				if (peakWidth[i] >= f1 && peakWidth[i] <= f2) {
					return true;
				}
			}
			return false;
		case SweepClassifierSet.CHANNELS_USE_MEANS:
			if (peakWidth[0] >= f1 && peakWidth[0] <= f2) {
				return true;
			}
			return false;
		}
		return true;
	}

	private int getPeakWidth(double[] specData, int peakBin, double peakWidthThreshold) {

		double thresh = specData[peakBin];
		thresh /= Math.pow(10, Math.abs(peakWidthThreshold)/10);
		int lData = specData.length;
		int bin1, bin2;
		bin1 = bin2 = peakBin;
		for (int i = peakBin-1; i >= 0; i--) {
			if (specData[i] >= thresh) {
				bin1 = i;
			}
			else {
				break;
			}
		}
		for (int i = peakBin+1; i < lData; i++) {
			if (specData[i] >= thresh) {
				bin2 = i;
			}
			else {
				break;
			}
		}
		
		return bin2-bin1+1;
	}

	private boolean testMeanFreq(ClickDetection click, SweepClassifierSet scs) {
		double[][] specData = getSpecData(click, scs);
		double binsPerHz = getBinsPerHz();
		double rBin1 = scs.peakSearchRange[0] * binsPerHz;
		double rBin2 = scs.peakSearchRange[1] * binsPerHz;
		rBin1 = Math.max(rBin1, 0);
		rBin2 = Math.min(rBin2, specData[0].length);
		int bin1 = (int) Math.floor(rBin1);
		int bin2 = (int) Math.ceil(rBin2);
		int nSpec = specData.length;
		double[] meanFreq = new double[nSpec];
		double a, b;
		for (int c = 0; c < nSpec; c++) {
			a = b = 0;
			for (int i = bin1; i < bin2; i++) {
				a += (i*specData[c][i]);
				b += specData[c][i];
			}
			meanFreq[c] = a / b / binsPerHz;
		}
		

		switch (scs.channelChoices) {
		case SweepClassifierSet.CHANNELS_REQUIRE_ALL:
			for (int i = 0; i < nChannels; i++) {
				if (meanFreq[i] < scs.meanRange[0] || meanFreq[i] > scs.meanRange[1]) {
					return false;
				}
			}
			return true;
		case SweepClassifierSet.CHANNELS_REQUIRE_ONE:
			for (int i = 0; i < nChannels; i++) {
				if (meanFreq[i] >= scs.meanRange[0] && meanFreq[i] <= scs.meanRange[1]) {
					return true;
				}
			}
			return false;
		case SweepClassifierSet.CHANNELS_USE_MEANS:
			if (meanFreq[0] >= scs.meanRange[0] && meanFreq[0] <= scs.meanRange[1]) {
				return true;
			}
			return false;
		}
		
		return true;
	}

	private boolean testZeroCrossings(ClickDetection click,
			SweepClassifierSet scs) {
		double[][] zeroCrossings = getZeroCrossings(click, scs);
		if (zeroCrossingStats == null) {
			return false;
		}
		switch (scs.channelChoices) {
		case SweepClassifierSet.CHANNELS_REQUIRE_ALL:
			for (int i = 0; i < nChannels; i++) {
				if (testZeroCrossingStat(zeroCrossingStats[i], scs) == false) {
					return false;
				}
			}
			return true;
		case SweepClassifierSet.CHANNELS_REQUIRE_ONE:
			for (int i = 0; i < nChannels; i++) {
				if (testZeroCrossingStat(zeroCrossingStats[i], scs) == true) {
					return true;
				}
			}
			return false;
		case SweepClassifierSet.CHANNELS_USE_MEANS:
			ZeroCrossingStats meanStats = new ZeroCrossingStats();
			for (int i = 0; i < nChannels; i++) {
				meanStats.nCrossings += zeroCrossingStats[i].nCrossings;
				meanStats.startFreq += zeroCrossingStats[i].startFreq;
				meanStats.endFreq += zeroCrossingStats[i].endFreq;
				meanStats.sweepRate += zeroCrossingStats[i].sweepRate;
			}
			meanStats.nCrossings /= nChannels;
			meanStats.startFreq /= nChannels;
			meanStats.endFreq /= nChannels;
			meanStats.sweepRate /= nChannels;
			return testZeroCrossingStat(meanStats, scs);
		}
		
		return false;
	}
	
	public boolean testZeroCrossingStat(ZeroCrossingStats zcStat, SweepClassifierSet scs) {
		if (zcStat.nCrossings < scs.nCrossings[0] || zcStat.nCrossings > scs.nCrossings[1]) {
			return false;
		}
		double sweep = zcStat.sweepRate / 1e6; // convert to kHz per millisecond.
		if (sweep < scs.zcSweep[0] || sweep > scs.zcSweep[1]) {
			return false;
		}
		return true;
	}
	
	double[][] zeroCrossings;
	private double[][] getZeroCrossings(ClickDetection click,
			SweepClassifierSet scs) {
		if (zeroCrossings == null) {
			zeroCrossings = createZeroCrossings(click, scs);
		}
		
		return zeroCrossings;
	}

	private double[][] createZeroCrossings(ClickDetection click,
			SweepClassifierSet scs) {
		double[][] waveData = click.getWaveData(scs.enableFFTFilter, scs.fftFilterParams);
		if (waveData == null) {
			return null;
		}
		int[][] lengthData = getLengthData(click, scs);
		if (lengthData == null) {
			return null;
		}
		double[][] zeroCrossings = new double[nChannels][];
		zeroCrossingStats = new ZeroCrossingStats[nChannels];
		for (int i = 0; i < nChannels; i++) {
			zeroCrossings[i] = createZeroCrossings(waveData[i], lengthData[i]);
			zeroCrossingStats[i] = new ZeroCrossingStats(zeroCrossings[i], sampleRate);
		}
		return zeroCrossings;
	}

	/**
	 * Work out zero crossings for one channel between given limits. 
	 * @param waveData
	 * @param lengthData
	 * @return array of zero crossing times. 
	 */
	private double[] createZeroCrossings(double[] waveData, int[] lengthData) {
		/*
		 * Make an array that must be longer than needed, then 
		 * cut it down to size at the end - will be quicker than continually
		 * growing an array inside a loop. 
		 */
		double[] zc = new double[lengthData[1]-lengthData[0]]; // longer than needed. 
		double lastPos = -1;
		double exactPos;
		int nZC = 0;
		for (int i = lengthData[0]; i < lengthData[1]-1; i++) {
			if (waveData[i] * waveData[i+1] > 0) {
				continue; // no zero crossing between these samples
			}
			exactPos = i + waveData[i] / (waveData[i] - waveData[i+1]);
			/*
			 * Something funny happens if a value is right on zero since the 
			 * same point will get selected twice, so ensure ignore ! 
			 */
			if (exactPos > lastPos) {
				lastPos = zc[nZC++] = exactPos;
			}
		}
		return Arrays.copyOf(zc, nZC);
	}

	// remember the last length threshold and smoothing term 
	// re-make the wave if they have changed. 
	double lastLengthdB = 0;
	int lastLengthSmooth = 0;
	/**
	 * 
	 * Creates a 2D array of length data[channels][start/end]
	 * <p>
	 * Will only call getLengthData if it really has to. 
	 * really has to. 
	 * @param click click
	 * @param scs classifier settings
	 */
	public int[][] getLengthData(ClickDetection click, SweepClassifierSet scs) {
		if (lengthData == null || Math.abs(scs.lengthdB) != lastLengthdB || 
				scs.lengthSmoothing != lastLengthSmooth) {
			createLengthData(click, scs);
			lastLengthdB = Math.abs(scs.lengthdB);
			lastLengthSmooth = scs.lengthSmoothing;
		}
		return lengthData;
	}
	
	/**
	 * Creates a 2D array of length data[channels][start/end]
	 * <p>
	 * Better to call getLengthData which will only call this if it
	 * really has to. 
	 * @param click click
	 * @param scs classifier settings
	 */
	private void createLengthData(ClickDetection click, SweepClassifierSet scs) {
		lengthData = new int[nChannels][2];
		double[] aWave;
		double maxVal;
		int maxIndex;
		double threshold;
		double threshRatio = Math.pow(10., Math.abs(scs.lengthdB)/20);
		int waveLen;
		int p;
		for (int i = 0; i < nChannels; i++) {
			aWave = click.getAnalyticWaveform(i, scs.enableFFTFilter, scs.fftFilterParams);
			if (aWave == null) {
				return;
			}
			aWave = SmoothingFilter.smoothData(aWave, scs.lengthSmoothing);
			waveLen = aWave.length;
			maxVal = aWave[0];
			maxIndex = 0;
			for (int s = 1; s < waveLen; s++) {
				if (aWave[s] > maxVal) {
					maxVal = aWave[s];
					maxIndex = s;
				}
			}
			threshold = maxVal / threshRatio;
			p = maxIndex-1;
			lengthData[i][0] = 0;
			for (; p >= 0; p--) {
				if (aWave[p] < threshold) {
					lengthData[i][0] = p+1;
					break;
				}
			}
			p = maxIndex+1;
			lengthData[i][1] = waveLen;
			for (; p < waveLen; p++) {
				if (aWave[p] < threshold) {
					lengthData[i][1] = p-1;
					break;
				}
			}
		}
	}
	
	private double[][] getSmoothSpecData(ClickDetection click, SweepClassifierSet scs) {
		double[][] specData = getSpecData(click, scs);
		int nSpec = specData.length;
		smoothSpecData = new double[nSpec][];
		for (int i = 0; i < nSpec; i++) {
			smoothSpecData[i] = SmoothingFilter.smoothData(specData[i], scs.peakSmoothing);
		}
		
		return smoothSpecData;
	}
	
	private int lastPeakSmooth = 0;
	private double[][] getSpecData(ClickDetection click, SweepClassifierSet scs) {
		if (specData == null || lastPeakSmooth  != scs.peakSmoothing) {
			createSpecData(click, scs);
		}
		return specData;
	}
	
	private void createSpecData(ClickDetection click, SweepClassifierSet scs) {
		/** 
		 * have to decide whether to use the whole click, in which case can 
		 * get the power spectrum from the click, or if we've been told to 
		 * just get the data around the peak maximum
		 */
		double[][] tempData;
		if (scs.restrictLength) {
			tempData = createRestrictedLengthSpec(click, scs);
		}
		else {
			tempData = createSpec(click, scs);
		}
		if (scs.channelChoices == SweepClassifierSet.CHANNELS_USE_MEANS) {
			specData = new double[1][];
			specData[0] = tempData[1];
			for (int i = 0; i < nChannels; i++) {
				for (int s = 1; s < tempData[0].length; s++) {
					specData[0][s] += tempData[i][s];
				}
			}
		}
		else {
			specData = tempData;
		}
	}
	private double getBinsPerHz() {
		if (specData == null) {
			return 0;
		}
		return specData[0].length * 2 / sampleRate;
	}
	private double[][] createRestrictedLengthSpec(ClickDetection click,
			SweepClassifierSet scs) {
		lengthData = getLengthData(click, scs);
		double[][] newSpecData = new double[nChannels][];
		for (int iC = 0; iC < nChannels; iC++) {
			newSpecData[iC] = createRestrictedLenghtSpec(click, iC, lengthData[iC], scs);
		}
		return newSpecData;
	}

	private FastFFT fastFFT = new FastFFT();
	private double[] createRestrictedLenghtSpec(ClickDetection click, int chan, int[] lengthPoints,
			SweepClassifierSet scs) {
		int startBin = (lengthPoints[0] + lengthPoints[1] - scs.restrictedBins)/2;
		startBin = Math.max(0, startBin);
		int endBin = startBin + scs.restrictedBins;
		double[] waveData = click.getWaveData(chan, scs.enableFFTFilter, scs.fftFilterParams);
		endBin = Math.min(endBin, waveData.length);
		waveData = Arrays.copyOfRange(waveData, startBin, endBin);
		if (waveData.length < scs.restrictedBins) {
			waveData = Arrays.copyOf(waveData, scs.restrictedBins);
		}
		double[] win = getWindow(scs.restrictedBins);
		for (int i = 0; i < scs.restrictedBins; i++) {
			waveData[i]*=win[i];
		}
		Complex[] fftData = fastFFT.rfft(waveData, null, FastFFT.log2(scs.restrictedBins));
		double[] specData = new double[scs.restrictedBins/2];
		for (int i = 0; i < scs.restrictedBins/2; i++) {
			specData[i] = fftData[i].magsq();
		}
		return specData;
	}
	
	private double[] window;
	private double[] getWindow(int len) {
		if (window == null ||window.length != len) {
			window = WindowFunction.hanning(len);
		}
		return window;
	}

	/**
	 * Get a copy of the ordinary power spectrum data for the click. 
	 * @param click click
	 * @param scs sweep param settings
	 */
	private double[][] createSpec(ClickDetection click, SweepClassifierSet scs) {
		double[][] newSpecData = new double[nChannels][];
		for (int i = 0; i < nChannels; i++) {
			newSpecData[i] = click.getPowerSpectrum(i);
		}
		return newSpecData;
	}

	private void clearExtractedParams() {
		lengthData = null;
		specData = null;
		peakBins = null;
		smoothSpecData = null;
		zeroCrossings = null;
		zeroCrossingStats = null;
	}
	
	
	
}
