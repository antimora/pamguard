package Filters;

import java.util.Arrays;

import fftManager.Complex;

public abstract class IIRFilterMethod extends FilterMethod {

	public IIRFilterMethod(double sampleRate, FilterParams filterParams) {
		super(sampleRate, filterParams);
	}
//
//	@Override
//	int calculatePoleZeros() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	String filterName() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public Complex[] getPoles(FilterParams filterParams) {
		if (!this.filterParams.equals(filterParams)) {
			this.filterParams = filterParams.clone();
			calculateOmegaValues();
			calculateFilter();
		}
		return poles;
	}

	public Complex[] getZeros(FilterParams filterParams) {
		if (!this.filterParams.equals(filterParams)) {
			this.filterParams = filterParams.clone();
			calculateOmegaValues();
			calculateFilter();
		}
		return zeros;
	}

	public int poleZeroCount() {
		if (filterParams.filterBand == FilterBand.BANDPASS)
			return filterParams.filterOrder * 2;
		else if (filterParams.filterBand == FilterBand.BANDSTOP)
			return filterParams.filterOrder * 2;
		else
			return filterParams.filterOrder;
	}

	protected double omega1, omega2, omega3, zeroValue;

	public void calculateOmegaValues() {

		switch (filterParams.filterBand) {
		case LOWPASS:
			omega1 = filterParams.lowPassFreq / getSampleRate() * 2. * Math.PI;
			// / sampleRate;
			zeroValue = -1.0;
			break;
		case HIGHPASS:
			omega1 = filterParams.highPassFreq / getSampleRate() * 2. * Math.PI;
			// / sampleRate;
			omega1 = Math.PI - omega1;
			zeroValue = +1.0;
			break;
		case BANDPASS:
			omega2 = filterParams.highPassFreq / getSampleRate() * 2. * Math.PI;
			// / sampleRate;
			omega3 = filterParams.lowPassFreq / getSampleRate()* 2. * Math.PI;
			// / sampleRate;
			omega1 = omega3 - omega2;
			zeroValue = 0.0;
			break;
		case BANDSTOP:
			omega2 = filterParams.highPassFreq / getSampleRate() * 2. * Math.PI;
			// / sampleRate;
			omega3 = filterParams.lowPassFreq / getSampleRate()* 2. * Math.PI;
			// / sampleRate;
			omega1 = (omega3 - omega2);
			zeroValue = 1.0;
			break;
		}
	}

	double mPiTerm(int m) {

		double dn = filterParams.filterOrder;
		double dm = m;
		if (filterParams.filterOrder % 2 == 0)
			return (2.0 * dm + 1.0) * Math.PI / (2.0 * dn);
		else
			return dm * Math.PI / dn;
	}

	int doBandpassTransformation(Complex[] poles, Complex[] zeros, int nPoints) {
		// move the data into a couple of temporary arrays...
		Complex[] oldPoles = new Complex[poles.length];
		Complex[] oldZeros = new Complex[zeros.length];
		for (int i = 0; i < nPoints; i++) {
			oldPoles[i] = new Complex(poles[i]);
			oldZeros[i] = new Complex(zeros[i]);
		}
		if (poles.length != nPoints*2) {
			poles = Arrays.copyOf(poles, nPoints*2);
		}
		if (zeros.length != nPoints*2) {
			zeros = Arrays.copyOf(zeros, nPoints*2);
		}

		double A = Math.cos((omega3 + omega2) / 2.0)
				/ Math.cos((omega3 - omega2) / 2.0);
		int m, m1, m2;
		Complex FirstBit = new Complex();
		Complex SecondBit = new Complex();
//		Complex temp;
		for (m = 0; m < nPoints; m++) {
			m1 = 2 * m;
			m2 = m1 + 1;
			FirstBit = new Complex(0.5 * A, 0).times(oldPoles[m]
					.plus(new Complex(1., 0)));
//			temp = oldPoles[m].plus(new Complex(1, 0)).times(A);
//			temp = temp.times(temp);
//			temp = temp.minus(oldPoles[m]);
//			temp = temp.times(0.25);
			// Copy of line at top of Lynn & Fuerst p 185 !
			SecondBit = (oldPoles[m].plus(1.).pow(2.).times(
					0.25 * Math.pow(A, 2.)).minus(oldPoles[m])).pow(0.5);

			poles[m1] = FirstBit.plus(SecondBit);
			zeros[m1] = new Complex(1, 0);
			poles[m2] = FirstBit.minus(SecondBit);
			zeros[m2] = new Complex(-1, 0);
		}

		return nPoints * 2;
	}
	int doBandStopTransformation(Complex[] poles, Complex[] zeros, int nPoints) {
		// move the data into a couple of temporary arrays...
		Complex[] oldPoles = new Complex[poles.length];
		Complex[] oldZeros = new Complex[zeros.length];
		for (int i = 0; i < nPoints; i++) {
			oldPoles[i] = new Complex(poles[i]);
			oldZeros[i] = new Complex(zeros[i]);
		}

		if (poles.length != nPoints*2) {
			poles = Arrays.copyOf(poles, nPoints*2);
		}
		if (zeros.length != nPoints*2) {
			zeros = Arrays.copyOf(zeros, nPoints*2);
		}
		/*
		 * Bandpass alpha term is
		 * A = Math.cos((omega3 + omega2) / 2.0)
		 *	/ Math.cos((omega3 - omega2) / 2.0);
		 */
		double A = Math.cos((omega3 + omega2) / 2.0)
				/ Math.cos((omega3 - omega2) / 2.0);
		int m, m1, m2;
		Complex FirstBit = new Complex();
		Complex SecondBit = new Complex();
		Complex firstBitZ, secondBitZ;
		Complex temp;
		for (m = 0; m < nPoints; m++) {
			m1 = 2 * m;
			m2 = m1 + 1;
			FirstBit = getFirstBit(oldPoles[m],A);
			temp = oldPoles[m].plus(new Complex(1, 0)).times(A);
			temp = temp.times(temp);
			temp = temp.minus(oldPoles[m]);
			temp = temp.times(0.25);
			// Copy of line at top of Lynn & Fuerst p 185 !
			SecondBit = getSecondBit(oldPoles[m], A);
			
			firstBitZ = getFirstBit(oldZeros[m], A);
			secondBitZ = getSecondBit(oldZeros[m], A);
			
			poles[m1] = FirstBit.plus(SecondBit);
			zeros[m1] = firstBitZ.plus(secondBitZ);
			poles[m2] = FirstBit.minus(SecondBit);
			zeros[m2] = firstBitZ.minus(secondBitZ);
		}

		return nPoints * 2;
	}
	private Complex getFirstBit(Complex z, double A) {
		return new Complex(0.5 * A, 0).times(z.plus(1));
	}
	private Complex getSecondBit(Complex z, double A) {
		return (z.plus(1.).pow(2.).times(
				0.25 * Math.pow(A, 2.)).minus(z)).pow(0.5);
	}

	/*
	 * Gain and phase
	 */

	@Override
	public double getFilterGain(double omega) {
		Complex ComplexFreq = new Complex(Math.cos(omega), Math.sin(omega));
		Complex ComplexLine;
		double FilterGain = 1.0;
		int j;
		for (j = 0; j <poleZeroCount(); j++) {
			ComplexLine = poles[j].minus(ComplexFreq);
			FilterGain /= Math.sqrt(ComplexLine.norm());
			ComplexLine = zeros[j].minus(ComplexFreq);
			FilterGain *= Math.sqrt(ComplexLine.norm());
		}
		return FilterGain;
	}

	@Override
	public double getFilterPhase(double omega) {
		Complex ComplexFreq = new Complex(Math.cos(omega), Math.sin(omega));
		Complex ComplexLine;
		double FilterPhase = 0.0;
		int j;
		for (j = 0; j < poleZeroCount(); j++) {
			ComplexLine = poles[j].minus(ComplexFreq);
			FilterPhase -= ComplexLine.ang();
			ComplexLine = zeros[j].minus(ComplexFreq);
			FilterPhase += ComplexLine.ang();
		}
		return FilterPhase;
	}

	@Override
	public double getFilterGainConstant() {
		double Omega2, Omega3;
		double FilterGain;
		switch (filterParams.filterBand) {
		case LOWPASS:
			FilterGain = getFilterGain(0.0);
			break;
		case HIGHPASS:
			FilterGain = getFilterGain(Math.PI);
			break;
		case BANDPASS:
			Omega2 = filterParams.highPassFreq / getSampleRate() * 2.0 * Math.PI;
			// / (double) sampleRate;
			Omega3 = filterParams.lowPassFreq / getSampleRate() * 2.0 * Math.PI;
			// / (double) sampleRate;
			FilterGain = getFilterGain(Math.sqrt(Omega2 * Omega3));
			break;
		case BANDSTOP:
			Omega2 = filterParams.highPassFreq / getSampleRate() * 2.0 * Math.PI;
			// / (double) sampleRate;
			Omega3 = filterParams.lowPassFreq / getSampleRate() * 2.0 * Math.PI;
			// / (double) sampleRate;
			FilterGain = getFilterGain(Math.sqrt(0));
			break;
		default:
			return 0.0;
		}
		// if its a Chebyshev filter, its much harder !
		// better to be a bit big, so that signal doesn't saturate....
		if (filterParams.filterType == FilterType.CHEBYCHEV) {
			if (filterParams.filterBand == FilterBand.BANDSTOP)
				FilterGain *= Math
				.pow(10.0, filterParams.passBandRipple / 20.0);
			if (filterParams.filterOrder % 2 == 0)
				FilterGain *= Math
				.pow(10.0, filterParams.passBandRipple / 20.0);
		}
		return FilterGain;
	}
	/*
	 * end of gain and phase
	 */

	/* (non-Javadoc)
	 * @see Filters.FilterMethod#createFilter()
	 */
	@Override
	public Filter createFilter(int channel) {
		// TODO Auto-generated method stub
		return new IirfFilter(channel, getSampleRate(), getFilterParams());
	}
}
