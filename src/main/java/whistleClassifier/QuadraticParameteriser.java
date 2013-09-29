package whistleClassifier;

import pamMaths.Regressions;

/**
 * Basic three parameters from a whistle fragment from 
 * a quadratic fit. 
 * @author Doug Gillespie
 *
 */
public class QuadraticParameteriser implements FragmentParameteriser {


	private Regressions r = new Regressions();
	
	@Override
	public int getNumParameters() {
		return 3;
	}

	@Override
	public double[] getParameters(WhistleContour whistleContour) {
		double[] params = new double[getNumParameters()];

		double[] quadFit = Regressions.squareFit(whistleContour.getTimesInSeconds(), whistleContour.getFreqsHz());
		double[] linFit = Regressions.polyFit(whistleContour.getTimesInSeconds(), whistleContour.getFreqsHz(), 1);
		double mean = Regressions.getMean(whistleContour.getFreqsHz());
		
		params[2] = quadFit[2];
		params[0] = mean;
		params[1] = linFit[1];
		
		return params;
	}

	
}
