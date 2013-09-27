package PamDetection;

public class RawDataUnit extends AcousticDataUnit {

	double[] rawData = null;

	public RawDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
	}

	/**
	 * @return Returns the rawData.
	 */
	public double[] getRawData() {
		return rawData;
	}

	/**
	 * @param rawData The rawData to set.
	 */
	public void setRawData(double[] rawData) {
		setRawData(rawData, false);
	}

	/**
	 * @param rawData The rawData to set.
	 */
	public void setRawData(double[] rawData, boolean setAmplitude) {
		this.rawData = rawData;
		this.duration = rawData.length;
		if (setAmplitude) {
			double maxValue = 0.;
			for (int i = 0; i < rawData.length; i++) {
				maxValue = Math.max(maxValue, Math.abs(rawData[i]));
			}
			setMeasuredAmplitude(maxValue, AMPLITUDE_SCALE_LINREFSD);
		}
	}
	
}
