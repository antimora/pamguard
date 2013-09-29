package simulatedAcquisition;

public class RightWhales extends RandomQuadratics {

	public RightWhales(double sampleRate) {
		super(sampleRate);
		double slope[] = {50, 150};
		double length[] = {.4, 1};
		double meanF[] = {90, 130};
		double meanCurve[] = {30, 300};
		setCurveR(meanCurve);
		setLengthR(length);
		setMeanR(meanF);
		setSlopeR(slope);
		
	}

	@Override
	String getName() {
		return "Right Whale like calls";
	}

}
