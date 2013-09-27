package clickDetector.ClickClassifiers.basicSweep;

import pamMaths.Regressions;

/**
 * Simple container for a few zero crossing parameters. 
 * Will extract these from zero crosing data sent to the 
 * constructor. 
 * @author Doug Gillespie
 *
 */
public class ZeroCrossingStats {

	double[] zeroCrossings;
	double sampleRate;
	
	/**
	 * Number of zero crossings
	 */
	public int nCrossings;
	
	/**
	 * Zero crossing sweep rate in Hz/second
	 */
	public double sweepRate;
	
	/**
	 * Zero crossing start frequency from fit. 
	 */
	public double startFreq;
	
	/**
	 * Zero crossing end frequency from fit
	 */
	public double endFreq;
	
	/**
	 * Default constructor, does nothing. 
	 */
	public ZeroCrossingStats() {
		
	}
	
	/**
	 * Constructor which automatically extracts parameters
	 * from some zero crossing data
	 * @param zeroCrossings array of zero crossing times in samples
	 * @param sampleRate sample rate
	 */
	public ZeroCrossingStats(double[] zeroCrossings, double sampleRate) {
		this.zeroCrossings = zeroCrossings;
		this.sampleRate = sampleRate;
		extractParams();
	}
	
	private void extractParams() {
		nCrossings = zeroCrossings.length;
		if (nCrossings < 2) {
			startFreq = endFreq = sweepRate = 0;
			return;
		}
		double[] freqData = new double[nCrossings-1];
		double[] t = new double[nCrossings-1];
		for (int i = 0; i < nCrossings-1; i++) {
			freqData[i] = sampleRate / 2 / (zeroCrossings[i+1]-zeroCrossings[i]);
			t[i] = ((zeroCrossings[i+1]+zeroCrossings[i])/2 - zeroCrossings[0]) / sampleRate;
		}
		if (freqData.length < 2) {
			startFreq = endFreq = freqData[0];
			sweepRate = 0;
			return;
		}
		double[] fitData = Regressions.linFit(t, freqData);
		sweepRate = fitData[1];
		startFreq = fitData[0];
		endFreq = fitData[0] + sweepRate * t[t.length-1];
	}
	
}
