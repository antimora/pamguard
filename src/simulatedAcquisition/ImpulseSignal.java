package simulatedAcquisition;

public class ImpulseSignal extends SimSignal {

	private double[] sig;
	
	public ImpulseSignal(float sampleRate) {
		super(sampleRate);
		sig = new double[5];
		for (int i = 0; i < sig.length; i++){
			sig[i] = 1;	
		}
	}

	@Override
	public double[] getSignal() {
		return sig;
	}
	
	@Override
	String getName() {
		return "Impulse";
	}
	

}
