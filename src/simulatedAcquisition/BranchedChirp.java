package simulatedAcquisition;

import java.util.Arrays;

public class BranchedChirp extends LinearChirp {

	LinearChirp branch;
	public BranchedChirp(double sampleRate, double f0, double f1, double f2, double f3,
			double duration) {
		super(sampleRate, f0, f1, duration);
		addChirp(sampleRate, f0, f1, .25, f2, duration*3/2);
		addChirp(sampleRate, f0, f1, .6, f3, duration);
	}
	
	private void addChirp(double sampleRate, double f0, double f1, double tFrac, double f3, double duration) {
		double f = f0*(1-tFrac) + f1*tFrac;
		LinearChirp newChirp = new LinearChirp(sampleRate, f, f3, duration);
		int bin1 = (int) (tFrac * sound.length);
		int bin2 = bin1+newChirp.sound.length;
		sound = Arrays.copyOf(sound, Math.max(sound.length, bin2));
		int sB = 0;
		for (int i = bin1; i < bin2; i++) {
			sound[i] += newChirp.sound[sB++];
		}
	}
	
	
	@Override
	String getName() {
		// TODO Auto-generated method stub
		return "Branched " + super.getName();
	}

}
