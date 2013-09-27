package signal;

import java.util.Arrays;

import fftManager.Complex;
import fftManager.FastFFT;

/**
 * Functions to calculate the Hilbert transform of data.
 *  
 * @author Doug
 *
 */
public class Hilbert {

	private FastFFT fastFFT = new FastFFT();
	
//	private FFT fft = new FFT();
	
	public synchronized  Complex[] getHilbertC(double[] signal) {
		int fftLength = FastFFT.nextBinaryExp(signal.length);
		Complex[] fullFFTData = getHilbertC(signal, fftLength);
		if (fullFFTData.length == fftLength) {
			return fullFFTData;
		}
		else return Arrays.copyOf(fullFFTData, signal.length);
	}

	public synchronized Complex[] getHilbertC(double[] signal, int fftLength) {	

		int logFFTLen = FastFFT.log2(fftLength);
		
		if (fftLength == 0) {
			return null;
		}

		double[] data121 = get1221Data(fftLength);

		makeStorageArrays(fftLength);

		storedFFTArray = fastFFT.rfft(signal, storedFFTArray, logFFTLen);

		int i = 0;
		for (; i < fftLength/2; i++) {
			fullFFTArray[i] = storedFFTArray[i].times(data121[i]);
			fullFFTArray[fftLength-i-1].assign(0,0);
		}
//		fullFFTArray[i] = storedFFTArray[i-1].conj().times(data121[i]);
//		for (; i < fftLength; i++) {
//			fullFFTArray[i].assign(0, 0);
//		}


		fastFFT.ifft(fullFFTArray,logFFTLen);

		return fullFFTArray;

	}
	/**
	 * Calculate the Hilbert Transform of a sample of data. 
	 * and return as a real array of the magnitude
	 * 
	 * @param signal signal waveform
	 * @return Complex Hilbert Transform of the data. 
	 */
	public synchronized double[] getHilbert(double[] signal) {

		int dataLen = signal.length;
		
		int fftLength = FastFFT.nextBinaryExp(dataLen);
		
		if (dataLen < fftLength) {
			signal = Arrays.copyOf(signal, fftLength);
		}
		
		Complex[] hData = getHilbertC(signal, fftLength);

		double[] newData = new double[dataLen];

		for (int i = 0; i < dataLen; i++) {
			newData[i] = fullFFTArray[i].mag() / fftLength;
		}

		return newData;
	}

	private Complex[] storedFFTArray = null;
	private Complex[] fullFFTArray = null;
	private double[] storedData1221 = null;

	/**
	 * Get an array of 12222100000 to use as a multiplier 
	 * in the Hilbert transform
	 * @param length of array
	 * @return array of coefficients. 
	 */
	private double[] get1221Data(int length) {

		if (storedData1221 != null && storedData1221.length == length) {
			return storedData1221;
		}
		storedData1221 = new double[length];
		int halfLength = length / 2;
		for (int i = 1; i < halfLength; i++) {
			storedData1221[i] = 2;
		}
		storedData1221[0] = storedData1221[halfLength] = 1;

		return storedData1221;
	}

	/**
	 * Make sure storage arrays are correctly allocated.
	 * @param fftLength
	 */
	private void makeStorageArrays(int fftLength) {
		if (storedFFTArray == null || storedFFTArray.length != fftLength / 2) {
			storedFFTArray = Complex.allocateComplexArray(fftLength / 2);
		}
		if (fullFFTArray == null || fullFFTArray.length != fftLength) {
			fullFFTArray = Complex.allocateComplexArray(fftLength);
		}
	}
}
