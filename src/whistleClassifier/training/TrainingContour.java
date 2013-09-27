package whistleClassifier.training;

import java.io.Serializable;

import whistleClassifier.WhistleContour;

public class TrainingContour implements Serializable, WhistleContour {
	
	static public final long serialVersionUID = 0;

	private double[] timeSeconds;
	
	private double[] frequencyHz;

	public TrainingContour(double[] timeSeconds, double[] frequencyHz) {
		super();
		this.timeSeconds = timeSeconds;
		this.frequencyHz = frequencyHz;
	}

	public double[] getTimesInSeconds() {
		return timeSeconds;
	}

	public void setTimesInSeconds(double[] timeSeconds) {
		this.timeSeconds = timeSeconds;
	}

	public double[] getFreqsHz() {
		return frequencyHz;
	}

	public void setFreqsHz(double[] frequencyHz) {
		this.frequencyHz = frequencyHz;
	}
	
	/**
	 * Get the length of the contour in time bins.
	 * @return
	 */
	public int getLength() {
		if (timeSeconds == null) {
			return 0;
		}
		return timeSeconds.length;
	}

	
}
