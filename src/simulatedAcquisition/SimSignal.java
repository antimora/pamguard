package simulatedAcquisition;

import Spectrogram.WindowFunction;

/**
 * Class to hold information and generate a signal 
 * of a specific type
 * @author Doug Gillespie
 *
 */
public abstract class SimSignal {
	
	abstract String getName();
	
	protected double sampleRate;

	/**
	 * @param sampleRate
	 */
	public SimSignal(double sampleRate) {
		super();
		this.sampleRate = sampleRate;
	}

	@Override
	public String toString() {
		return getName();
	}
	
	abstract public double[] getSignal();
	
	/**
	 * Taper the ends of the signal using a hanning 
	 * window function. 
	 * <p> if the percentTaper parameter is 50%, then the 
	 * entire signal is windowed, otherwise, it will 
	 * just taper the bits at the end. 
	 * @param percentTaper
	 */
	protected void taperEnds(double[] signal, double percentTaper) {
		int len = signal.length;
		int winLen = (int) (len * percentTaper / 100);
		winLen /=2;
		winLen *=2;
		
		double[] winFunc = WindowFunction.hanning(winLen);
		for (int i = 0; i < winLen/2; i++) {
			signal[i] *= winFunc[i];
			signal[len-1-i] *= winFunc[i];
		}
	}

	public double getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}

}
