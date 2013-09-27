package simulatedAcquisition;

public class LinearChirp extends SimSignal {

	private double f0, f1, duration;
	protected double sound[];
	private String name;
	
	public LinearChirp(double sampleRate, double f0, double f1, double duration) {
		super(sampleRate);
		this.f0 = f0;
		this.f1 = f1;
		this.duration = duration;
		int nSamp = (int) (duration * sampleRate);
		sound = new double[nSamp];
		double t, phase, sweep;
		sweep = (f1-f0)/duration;
		for (int i = 0; i < nSamp; i++) {
			t = i/sampleRate;
			phase = (f0*t + 0.5 * sweep * t * t)*2*Math.PI;
			sound[i] = Math.sin(phase);
		}
		taperEnds(sound, 10);
		name = String.format("Chirp %d-%d kHz %3.1fms", (int)(f0/1000.), (int)(f1/1000.), duration * 1000);
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
