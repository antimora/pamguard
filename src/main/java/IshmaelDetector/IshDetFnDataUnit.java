package IshmaelDetector;

import PamDetection.AcousticDataUnit;

public class IshDetFnDataUnit extends AcousticDataUnit {
	double[] detData;               //a sequence of detection function points
	
	public IshDetFnDataUnit(long timeMilliseconds, int channelBitmap, long startSample,
			long duration, double[] detData) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
		this.detData = detData;
	}

	public double[] getDetData() {
		return detData;
	}

	public void setDetData(double[] detData) {
		this.detData = detData;
	}

}
