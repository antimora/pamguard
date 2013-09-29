package pamMaths;

import java.util.Arrays;

import signal.Hilbert;

import fftManager.Complex;
import fftManager.FastFFT;

public class WignerTransform {

	
	/**
	 * Calculate Wigner transform of real data. 
	 * Will need to take the hilbert transform and 
	 * then call the complex function. 
	 * @param doubleData double array of Wigner data
	 */
	public static double[][] wignerTransform(double[] doubleData) {
		int originalDataLength, packedDataLength;
		originalDataLength = doubleData.length;
		packedDataLength = FastFFT.nextBinaryExp(originalDataLength);
		doubleData = Arrays.copyOf(doubleData, packedDataLength);
		Hilbert h = new Hilbert();
		Complex[] hData = h.getHilbertC(doubleData, packedDataLength);
		double[][] tData = transformData2(hData, packedDataLength);
		if (originalDataLength == packedDataLength) {
			return tData;
		}
		return Arrays.copyOf(tData, originalDataLength);
	}
	
	/**
	 * Calculate the Wigner transform from data that has already been 
	 * Hilbert transformed. 
	 * @param complexData
	 * @return double array of Wigner data
	 */
	public static double[][] wignerTransform(Complex[] complexData) {
		int originalDataLength, packedDataLength;
		originalDataLength = complexData.length;
		packedDataLength = FastFFT.nextBinaryExp(originalDataLength);
		double[][] tData = transformData2(complexData, packedDataLength);
		if (originalDataLength == packedDataLength) {
			return tData;
		}
		return Arrays.copyOf(tData, originalDataLength);
	}
	
	/** 
	 * Wigner transform of a complex array, padded if necessary to be 
	 * a power of 2 long. 
	 * This has been largely copied from the Matlab tfrwv library by F. Auger
	 * 
	 * @param hData
	 * @param fftLength
	 * @return Wigner transform of the data. 
	 */
	private static double[][] transformData2(Complex[] x, int N) {
		Complex[][] tfr = Complex.allocateComplexArray(N, N);
		double[][] d = new double[N][N];
		for (int i = 0; i < N; i++) {
			d[i] = new double[N];
		}
		
		int xrow, xcol, taumax;
		int tau, indices;
		FastFFT fft = new FastFFT();
		
		xrow = N;
		xcol = 1;
		for (int iCol = 0; iCol < N; iCol++) {
			taumax = min(iCol, xrow-iCol-1, Math.round(N)/2 - 1);
			for (tau = -taumax; tau <= taumax; tau++) {
				indices = (N+tau)%N ;
				tfr[iCol][indices] = x[iCol+tau].times(0.5).times(x[iCol-tau].conj()); 
			}
			tau = Math.round(N/2);
			if (iCol < xrow-tau && iCol >= tau) {
				tfr[iCol][tau] = x[iCol+tau].times(x[iCol-tau].conj()).plus(
				x[iCol-tau].times(x[iCol+tau].conj())).times(.5);
			}
		}
		for (int i = 0; i < N; i++) {
			fft.fft(tfr[i]);
		}
		for (int i = 0; i < N; i ++) {
			for (int j = 0; j < N; j++) {
				d[i][j] = tfr[i][j].real;
			}
		}
		
		return d;
	}
	 public static double getMaxValue(double[][] array) {
		 double max = array[0][0];
		 for (int i = 0; i < array.length; i++) {
			 for (int j = 0; j < array[i].length; j++) {
				 max = Math.max(max, array[i][j]);
			 }
		 }
		 return max;
	 }
	 public static double getMinValue(double[][] array) {
		 double min = array[0][0];
		 for (int i = 0; i < array.length; i++) {
			 for (int j = 0; j < array[i].length; j++) {
				 min = Math.min(min, array[i][j]);
			 }
		 }
		 return min;
	 }
	
	private static int min(int a, int b, int c) {
		return Math.min(a, Math.min(b, c));
	}
}
