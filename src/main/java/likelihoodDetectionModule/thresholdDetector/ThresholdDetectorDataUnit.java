package likelihoodDetectionModule.thresholdDetector;

import PamDetection.PamDetection;

/**
 * 
 * This is the Threshold Detectors output data unit.  PamDetection units are sent off describing a detection,
 * and will be picked up by subscribers (SQL Logging, OverlayGraphics, localizers, etc).
 * <p>
 * Along with the constructor's parameters (see its documentation for those), we also set the following properties
 * of the PamDetection before sending it on its way:
 * <p>
 * MeasuredAmplitude - The peak value (in Decibels, 10*log10() ) of the Likelihood Ratio. The likelihood ratio
 *                     is the signal estimate/background noise estimate 
 * <p>                
 * PeakTime - The time of the peak value described above.  This value is in double precision seconds since the
 *            beginning of the data capture.
 * <p>       
 * Frequency - This is a double[2] describing a frequency range. frequency[0] is the low frequency, frequency[1] the
 *             high frequency.  These values are taken verbatim from the target configuration as entered by the
 *             user, not calculated from the actual detection region.
 * <p>
 * DetectionType - A string uniquely identifying the detection.  We concatenate the configuration name with the signal
 *                 band name which the detection occurred within.  For example "Bowhead - End Note"; Bowhead is the
 *                 configuration name, and End Note is the signal band.
 * <p>
 * EstimatedPeakTimeAccuracy - The estimated accuracy of our PeakTime property to help with localization calculations.
 *                             This value is in seconds (double precision) and is currently set to the spectral
 *                             processing time resolution.
 *                             
 * @author Dave Flogeras
 */
public class ThresholdDetectorDataUnit extends PamDetection<PamDetection,PamDetection> {

	private double estimatedPeakTimeAccuracy;
	
	/**
	 * 
	 * @param timeMilliseconds The time in milliseconds of the start of the detection.
	 * @param channelBitmap The channel that this detection was triggered on.
	 * @param startSample The sample number of the start of this detection. This is in terms of the raw
	 *                    sampled data from the data source.
	 * @param duration The duration of the detection, also in terms of raw input data samples.
	 */
	public ThresholdDetectorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);

	}
	
	public void setEstimatedPeakTimeAccuracy( double p ) {
		this.estimatedPeakTimeAccuracy = p;
	}
	
	public double getEstimatedPeakTimeAccuracy() {
		return this.estimatedPeakTimeAccuracy;
	}


}
