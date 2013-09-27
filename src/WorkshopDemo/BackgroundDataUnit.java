package WorkshopDemo;

import PamDetection.AcousticDataUnit;

public class BackgroundDataUnit extends AcousticDataUnit {

	private double background;
	
	public BackgroundDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long duration, double background) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
		this.background = background;
		
	}

	public double getBackground() {
		return background;
	}

	public void setBackground(double background) {
		this.background = background;
	}

}
