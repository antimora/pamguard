package fftFilter;

import java.util.Arrays;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import fftManager.Complex;
import Filters.Filter;
import PamUtils.PamUtils;

public class FFTFilter implements Filter {
	
	private DoubleFFT_1D doubleFFT_1D;
	
	private FFTFilterParams fftFilterParams;
	
	private float sampleRate;

	private int currentFFTLength;
	
	public FFTFilter(FFTFilterParams fftFilterParams, float sampleRate) {
		setParams(fftFilterParams, sampleRate);
	}
	
	public void setParams(FFTFilterParams fftFilterParams, float sampleRate) {
		this.fftFilterParams = fftFilterParams.clone();
		this.sampleRate = sampleRate;
	}

	@Override
	public int getFilterDelay() {
		return 0;
	}

	@Override
	public void prepareFilter() {
		// TODO Auto-generated method stub
		
	}
	
//	public void runFilter(Complex[] complexData) {
//		int fftLen = complexData.length;
//		int bin1, bin2, j;
//		int i;
//		switch(fftFilterParams.filterBand) {
//		case HIGHPASS:
//			bin1 = getFFTBin(fftFilterParams.highPassFreq, fftLen, sampleRate);
//			j = fftLen*2-1;
//			for (i = 0; i < bin1*2; i++, j--) {
//				complexData[i].assign(0,0);
//				complexData[j].assign(0,0);
//			}
//			break;
//		case LOWPASS:
//			bin1 = getFFTBin(fftFilterParams.lowPassFreq, fftLen, sampleRate);
//			j = fftLen*2-1-bin1*2;
//			for (i = bin1*2; i < fftLen; i++, j--) {
//				complexData[i].assign(0,0);
//				complexData[j].assign(0,0);
//			}
//			break;
//		case BANDPASS:
//			bin1 = getFFTBin(Math.min(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
//			j = fftLen*2-1;
//			for (i = 0; i < bin1; i++,j--) {
//				complexData[i].assign(0,0);
//				complexData[j].assign(0,0);
//			}
//			bin1 = getFFTBin(Math.max(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
//			j = fftLen*2-1-bin1*2;
//			for (i = bin1; i < fftLen/2; i++,j--) {
//				complexData[i].assign(0,0);
//				complexData[j].assign(0,0);
//			}
//			break;
//		case BANDSTOP:
//			bin1 = getFFTBin(Math.min(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
//			bin2 = getFFTBin(Math.max(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
//			j = fftLen*2-1-bin1*2;
//			for (i = bin1; i < bin2; i++,j--) {
//				complexData[i].assign(0,0);
//				complexData[j].assign(0,0);
//			}
//		}
//	}

	@Override
	public void runFilter(double[] inputData, double[] outputData) {

		int len = inputData.length;
		int fftLen = 1<<PamUtils.log2(len);
		if (currentFFTLength != fftLen) {
			currentFFTLength = fftLen;
			doubleFFT_1D = new DoubleFFT_1D(fftLen);
		}
		double[] complexData = Arrays.copyOf(inputData, fftLen*2);
//		doubleFFT_1D.realForward(fftData);
		doubleFFT_1D.realForwardFull(complexData);
		// now make a second array of twice that length so as to make the 
		// full complex conjugate data prior to filtering and the inverse transform...
		/*
		 * Do the filtering on the first half of the FFT only.
		 * N.B. the array is real and imaginary pairs, so will have to 
		 * operate on 2* the number of elements and double a lot of bin numbers. 
		 */
		int bin1, bin2, j;
		switch(fftFilterParams.filterBand) {
		case HIGHPASS:
			bin1 = getFFTBin(fftFilterParams.highPassFreq, fftLen, sampleRate);
			j = fftLen*2-1;
			for (int i = 0; i < bin1*2; i++, j--) {
				complexData[i] = 0;
				complexData[j] = 0;
			}
			break;
		case LOWPASS:
			bin1 = getFFTBin(fftFilterParams.lowPassFreq, fftLen, sampleRate);
			j = fftLen*2-1-bin1*2;
			for (int i = bin1*2; i < fftLen; i++, j--) {
				complexData[i] = 0;
				complexData[j] = 0;
			}
			break;
		case BANDPASS:
			bin1 = getFFTBin(Math.min(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
			j = fftLen*2-1;
			for (int i = 0; i < bin1*2; i++,j--) {
				complexData[i] = 0;
				complexData[j] = 0;
			}
			bin1 = getFFTBin(Math.max(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
			j = fftLen*2-1-bin1*2;
			for (int i = bin1*2; i < fftLen; i++,j--) {
				complexData[i] = 0;
				complexData[j] = 0;
			}
			break;
		case BANDSTOP:
			bin1 = getFFTBin(Math.min(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
			bin2 = getFFTBin(Math.max(fftFilterParams.highPassFreq, fftFilterParams.lowPassFreq), fftLen, sampleRate);
			j = fftLen*2-1-bin1*2;
			for (int i = bin1*2; i < bin2*2; i++,j--) {
				complexData[i] = 0;
				complexData[j] = 0;
			}
		}
		/**
		 * Then copy the complex conjugates of the first half over into the second half. 
		 */
//		int nC = fftLen / 2;
//		j = fftLen*2-2;
//		for (int i = 0; i < fftLen; i+=2, j-=2) {
//			complexData[j] = complexData[i];
//			complexData[j+1] = -complexData[i+1];
//		}
		/*
		 * Then do the inverse transform which will return another 
		 * complex result !
		 */
		doubleFFT_1D.complexInverse(complexData, true);
		
		/**
		 * And copy that into the output data. 
		 */
		j = 0;
		for (int i = 0; i < len; i++, j+=2) {
			outputData[i] = complexData[j];
		}
	}

	@Override
	public void runFilter(double[] inputData) {
		runFilter(inputData, inputData);
	}
	
	private int getFFTBin(double freq, int fftLen, float sampleRate) {
		int bin = (int) Math.round(freq * fftLen / sampleRate);
		return Math.min(Math.max(0, bin), fftLen/2-1);
	}

	@Override
	public double runFilter(double aData) {
		// can't do this with fft filtering, since data need to be in blocks. 
		return Double.NaN;
	}

}
