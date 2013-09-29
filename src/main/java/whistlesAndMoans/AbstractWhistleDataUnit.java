package whistlesAndMoans;

import whistleClassifier.WhistleContour;
import PamDetection.PamDetection;;

public abstract class AbstractWhistleDataUnit 
	extends PamDetection<PamDetection, PamDetection> 
	implements WhistleContour {

	public AbstractWhistleDataUnit(long timeMilliseconds, int channelBitmap,
			long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
	}

	/**
	 * Get the total number of slices
	 * @return total number of slices
	 */
	abstract public int getSliceCount();
	
	/**
	 * Get an array of the times of each slice in seconds
	 * @return times in seconds
	 */
	abstract public double[] getTimesInSeconds();
	
	/**
	 * Get an array of the peak frequencies in Hz. 
	 * @return peak frequencies in Hz.
	 */
	abstract public double[] getFreqsHz();
}
