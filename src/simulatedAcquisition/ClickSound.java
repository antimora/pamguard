package simulatedAcquisition;

public class ClickSound extends SimSignal {

	private double[] sound;
	private double f0;
	private double duration;
	private String name;
	
	public ClickSound(double sampleRate, double f0, double duration) {
		super(sampleRate);
		this.f0 = f0;
		this.duration = duration;
		int nSamp = (int) (duration * sampleRate);
		sound = new double[nSamp];
		for (int i = 0; i < nSamp; i++) {
			sound[i] = Math.sin(i/sampleRate*2*Math.PI*f0) * (1.-i/(double)nSamp);
		}
		name = String.format("Click %d kHz %3.1fms", (int)(f0/1000.), duration * 1000);
	}
	
	@Override
	String getName() {
		return name;
	}

	@Override
	public double[] getSignal() {
		return sound;
	}

}
