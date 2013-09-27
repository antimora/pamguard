package clickDetector.ClickClassifiers.basicSweep;

import java.awt.Color;
import java.io.Serializable;

import javax.swing.JOptionPane;

import fftFilter.FFTFilterParams;

import PamView.PamSymbol;
import clickDetector.ClickClassifiers.ClickTypeCommonParams;

public class SweepClassifierSet extends ClickTypeCommonParams
        implements Serializable, Cloneable {
	
	transient static public final String[] defaultSpecies = {"Porpoise", "Beaked Whale"};

	public static final long serialVersionUID = 1L;
	
	public String name;

	public int speciesCode;
	
	public boolean discard = false;

	public PamSymbol symbol;
	
	public boolean enable;
	
	// general options
	
	static public final int CHANNELS_REQUIRE_ALL = 0;
	static public final int CHANNELS_REQUIRE_ONE = 1;
	static public final int CHANNELS_USE_MEANS = 2;
	static public String getChannelOptionsName(int iOpt) {
		switch (iOpt) {
		case CHANNELS_REQUIRE_ALL:
			return "Require positive idenitification on all channels individually";
		case CHANNELS_REQUIRE_ONE:
			return "Require positive identification on only one channel";
		case CHANNELS_USE_MEANS:
			return "Use mean parameter values over all channels";
		}
		return null;
	}
	
	public int channelChoices = CHANNELS_REQUIRE_ALL;
	
	public boolean restrictLength = true;
	
	public int restrictedBins = 128;
	
	
	// length stuff
	public boolean enableLength = true;
	
	public int lengthSmoothing = 5;
	
	public double lengthdB = 6;
	
	public double minLength = 0, maxLength = 1;
	
	// energy bands stuff
	public static final transient int nControlBands = 2;
	
	public boolean enableEnergyBands = false;
	
	public double[] testEnergyBand = new double[2];
	
	public double[][] controlEnergyBand = new double[nControlBands][2];
	
	public double[] energyThresholds = new double[nControlBands];
	
	public boolean enableFFTFilter = false;
	
	public FFTFilterParams fftFilterParams;
	
	// peak frequency stuff
	boolean enablePeak, enableWidth, enableMean;
	double peakSearchRange[];
	double peakRange[];
	double peakWidthRange[];
	double meanRange[];
	int peakSmoothing = 5;
	double peakWidthThreshold = 6;
	
	// zero crossings stuff. 
	public boolean enableZeroCrossings;
	public int[] nCrossings;
	public double[] zcSweep;
	
	public SweepClassifierSet() {
		checkEnergyParamsAllocation();
		checkPeakFreqAllocation();
		checkZCAllocation();
	}

	@Override
	protected SweepClassifierSet clone() {
		try {
			return (SweepClassifierSet) super.clone();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public void checkEnergyParamsAllocation() {
		if (testEnergyBand == null || testEnergyBand.length != 2) {
			testEnergyBand = new double[2];
		}
		if (controlEnergyBand == null || controlEnergyBand.length != nControlBands) {
			controlEnergyBand = new double[nControlBands][];
		}
		for (int i = 0; i < nControlBands; i++) {
			if (controlEnergyBand[i] == null || controlEnergyBand[i].length != 2) {
				controlEnergyBand[i] = new double[2];
			}
		}
		if (energyThresholds == null || energyThresholds.length != nControlBands) {
			energyThresholds = new double[nControlBands];
		}
	}

	public void checkPeakFreqAllocation() {
		if (peakSearchRange == null || peakSearchRange.length != 2) {
			peakSearchRange = new double[2];
		}
		if (peakRange == null || peakRange.length != 2) {
			peakRange = new double[2];
		}
		if (peakWidthRange == null || peakWidthRange.length != 2) {
			peakWidthRange = new double[2];
		}
		if (meanRange == null || meanRange.length != 2) {
			meanRange = new double[2];
		}
	}
	
	public void checkZCAllocation() {
		if (nCrossings == null || nCrossings.length != 2) {
			nCrossings = new int[2];
		}
		if (zcSweep == null || zcSweep.length != 2) {
			zcSweep = new double[2];
		}
	}
	
	/**
	 * check that the settings can be processed at the current sample rate. 
	 * @param sampleRate sample rate in Hz
	 * @param verbose true if you want visible warning messages. 
	 * @return true if OK, false otherwise. 
	 */
	public boolean canProcess(double sampleRate, boolean verbose) {
		// check that the given settings can process at 
		// the given frequency
		double nFreq = sampleRate/2;
		if (enableEnergyBands) {
			if (testEnergyBand[1] > nFreq) {
				return sayWarning("Test energy band is at too high a frequency", verbose);
			}
			for (int i = 0; i < nControlBands; i++) {
				if (controlEnergyBand[i][1] > nFreq) {
					return sayWarning("A control energy band is at too high a frequency", verbose);
				}
			}
		}
		if (enablePeak || enableWidth || enableMean) {
			if (testEnergyBand[1] > nFreq) {
				return sayWarning("Peak frequency search and integration range is at too high a frequency", verbose);
			}			
		}
		if (enablePeak && peakSearchRange[1] > nFreq){
			return sayWarning("Peak frequency range is at too high a frequency", verbose);
		}			
		if (enableWidth && peakSearchRange[1] > nFreq){
			return sayWarning("Peak Width range is at too high a frequency", verbose);
		}			
		if (enableMean && peakSearchRange[1] > nFreq){
			return sayWarning("Mean frequency range is at too high a frequency", verbose);
		}			
		
		return true;
	}
	
	/**
	 * 
	 * @return true if one or more tests require length data. 
	 */
	protected boolean needLength() {
		if (enableLength || restrictLength || enableZeroCrossings) {
			return true;
		}
		return false;
	}
	
	private boolean sayWarning(String warningText, boolean verbose) {
		if (verbose == false) {
			return false;
		}
		String warnTitle = "Click Classifier - ";
		if (name != null) {
			warnTitle += name;
		}
		JOptionPane.showMessageDialog(null, warningText, warnTitle, JOptionPane.ERROR_MESSAGE);
		return false;
	}
	
	public boolean setSpeciesDefaults(String species) {
		if (species.equalsIgnoreCase(defaultSpecies[0])) {
			porpoiseDefaults();
			return true;
		}
		else if (species.equalsIgnoreCase(defaultSpecies[1])) {
			beakedWhaleDefaults();
			return true;
		}
		return false;
	}
	
	public void beakedWhaleDefaults() {
		checkEnergyParamsAllocation();
		checkPeakFreqAllocation();
		checkZCAllocation();
		name = "Beaked Whale";
		symbol = new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 10, 10, true, Color.MAGENTA, Color.MAGENTA);
		enableLength = false;
		minLength = 0.1;
		maxLength = 0.5;
		
		enableEnergyBands = true;
		testEnergyBand[0] = 24000;
		testEnergyBand[1] = 48000;
		controlEnergyBand[0][0] = 12000;
		controlEnergyBand[0][1] = 24000;
		controlEnergyBand[1][0] = 12000;
		controlEnergyBand[1][1] = 24000;
		energyThresholds[0] = 3;
		energyThresholds[1] = 3;
		
		enablePeak = true;
		peakSearchRange[0] = 10000;
		peakSearchRange[1] = 96000;
		peakSmoothing = 5;
		peakRange[0] = 25000;
		peakRange[1] = 48000;
		peakWidthThreshold = 6;
		enableWidth = enableMean = false;
		
		enableMean = true;
		meanRange[0] = 25000;
		meanRange[1] = 48000;
		
		enableZeroCrossings = true;
		nCrossings[0] =7;
		nCrossings[1] = 50;
		zcSweep[0] = 1;
		zcSweep[1] = 500;
	}
	public void porpoiseDefaults() {
		checkEnergyParamsAllocation();
		checkPeakFreqAllocation();
		checkZCAllocation();
		name = "Porpoise";
		symbol = new PamSymbol(PamSymbol.SYMBOL_TRIANGLEU, 10, 10, true, Color.RED, Color.RED);
		enableLength = true;
		minLength = 0.07;
		maxLength = 0.1;
		lengthdB = 6;
		
		enableEnergyBands = true;
		testEnergyBand[0] = 100000;
		testEnergyBand[1] = 150000;
		controlEnergyBand[0][0] = 40000;
		controlEnergyBand[0][1] = 90000;
		controlEnergyBand[1][0] = 160000;
		controlEnergyBand[1][1] = 190000;
		energyThresholds[0] = 6;
		energyThresholds[1] = 6;
		
		enablePeak = true;
		peakSearchRange[0] = 40000;
		peakSearchRange[1] = 240000;
		peakSmoothing = 5;
		peakRange[0] = 100000;
		peakRange[1] = 150000;
		enableWidth = enableMean = false;
		
		enableZeroCrossings = true;
		nCrossings[0] = 10;
		nCrossings[1] = 50;
		zcSweep[0] = -200;
		zcSweep[1] = 200;
		
	}

}
