package fftManager;

import java.util.Arrays;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;

/**
 * FFT Wrapper which uses the edu.emory.mathcs.jtransforms.fft
 * transforms library for the actual FFT calculations. 
 * <br>These are simple wrappers to use the transofrms library with 
 * standard PAMGUARD transform classes. 
 * 
 * @author Doug Gillespie
 *
 */
public class FastFFT  {
	
	private DoubleFFT_1D doubleFFT_1D;
	
	private DoubleFFT_2D doubleFFT_2D;
	
	private int transformSize = 0;
	
	public FastFFT() {
		
	}

	/**
	 * Dummy data for input to the doubleFFT_1D function
	 */
	private double[] dummyX;
	
	/**
	 * Fast FFT function for real data. 
	 * @param x real data array
	 * @param y preallocated Complex array for output data (can be null)
	 * @param m log2 of the FFT length (sorry !)
	 * @return Complex FFT data. 
	 */
	public synchronized Complex[] rfft(double[] x, Complex[] y, int m) {
		int n = 1<<m;
		/*
		 * Copy the double array since it's going to be transformed 
		 * and we won't want to mess with x.
		 */
//		if (dummyX == null || dummyX.length != n) {
//			dummyX = new double[n];
//		}
//		int n2 = Math.min(n, x.length);
//		int i = 0;
//		for (; i < n2; i++) {
//			dummyX[i] = x[i];
//		}
//		for (; i < dummyX.length; i++) {
//			dummyX[i] = 0;
//		}
		dummyX = Arrays.copyOf(x, n);
	
		/*
		 * Check the transform has been created with the right fft length
		 */
		if (doubleFFT_1D == null || transformSize != n) {
			doubleFFT_1D = new DoubleFFT_1D(transformSize = n);
		}
		
		/*
		 * Run the FFT
		 */
		doubleFFT_1D.realForward(dummyX);
		
		/*
		 * Now copy the interleaved data out of the double array back into the 
		 * Complex y.
		 */
		return packDoubleToComplex(dummyX, y);
	}

	/**
	 * In place fft of complex data. 
	 * @param x complex array
	 */
	public synchronized void fft(Complex[] x) {
		double[] d = packComplexToDouble(x, dummyX);
		int n = x.length;
		if (doubleFFT_1D == null || transformSize != n) {
			doubleFFT_1D = new DoubleFFT_1D(transformSize = n);
		}
		doubleFFT_1D.complexForward(d);
		packDoubleToComplex(d, x);
	}
	
	/**
	 * In lace FFT of a 2D complex array. 
	 * Will use the multithreading abilities of the 
	 * JTransofrms library. 
	 * @param x
	 */
	public synchronized void fft(Complex[][] x) {
		int rows = x.length;
		int cols = x[0].length;
		doubleFFT_2D = new DoubleFFT_2D(rows, cols);
		double[][] d = packComplexToDouble(x);
		doubleFFT_2D.complexForward(d);
		x = packDoubleToComplex(d, x);
	}

	/**
	 * Inverse FFT for Complex data. 
	 * <br> I FFT is performed 'in place' so data are overwritten
	 * @param x Complex Data
	 * @param m log2 of the FFT length (sorry !)
	 */
	public synchronized void ifft(Complex[] x, int m) {
		int n = 1<<m;
		Complex[] inData = x; 
		/*
		 * Pack the complex data into a double array
		 */
		double[] d = packComplexToDouble(x, dummyX);
		/*
		 * Check the transform has been created with the right fft length
		 */
		if (doubleFFT_1D == null || transformSize != n) {
			doubleFFT_1D = new DoubleFFT_1D(transformSize = n);
		}
		doubleFFT_1D.complexInverse(d, false);
		/*
		 * Unpack the double data back into a complex array. 
		 */
		packDoubleToComplex(d, x);
		if (x != inData) {
			System.out.println("Repacked complex data into new array - ERROR !!!!");
		}

	}

	/**
	 * Finds the next highest binary exponential of the input integer. If the
	 * input is itself a binary exponential, then the result is itself. E.g.
	 * given 7 returns 8, 8 returns 8, 9 returns 16. Notes has limit of 2^100.
	 * Matlab calls this function nextpow2; it's also akin to frexp in C.
	 * 
	 * @param sourceNumber
	 * @return The next highest 2^ of the input, unless input is itself a binary
	 *         exponential.
	 */
	public static int nextBinaryExp(int sourceNumber) {
		int power = 0;

		for (int i = 0; i < 100; i++) {
			power = 1 << i;
			//power = (int) (java.lang.Math.pow(2, i));
			if (power >= sourceNumber)
				break;
		}
		// //System.out.println("Nearest power: " + power);

		return power;
	};
	public static int log2(int num) {
		// return -1 if it's not a natural power of 2
		for (int i = 0; i < 32; i++) {
			if (1<<i == num) return i;
		}
		return -1;
	}
	
	double[][] packComplexToDouble(Complex[][] c) {
		int nR = c.length;
		int nC = c[0].length;
		double[][] d = new double[nR][nC*2];
		for (int i = 0; i < nR; i++) {
			d[i] = packComplexToDouble(c[i], d[i]);
		}
		return d;
	}
	
	Complex[][] packDoubleToComplex(double[][] d, Complex[][] c) {
		int nR = d.length;
		int nC = d[0].length/2;
		if (c == null || c.length != d.length) {
			c = new Complex[nR][];
		}
		for (int i = 0; i < nR; i++) {
			c[i] = packDoubleToComplex(d[i], c[i]);
		}
		return c;
	}
	
	/**
	 * Packs a complex array into a double array of twice the length
	 * <br>
	 * Will allocate double array if necessary
	 * @param c Complex array
	 * @param d double array
	 * @return double array
	 */
	private double[] packComplexToDouble(Complex[] c, double[] d) {
		int n = c.length;
		if (d == null || d.length != 2*n) {
			d = new double[2*n];
		}
		int iD = 0;
		for (int i = 0; i < n; i++) {
			d[iD++] = c[i].real;
			d[iD++] = c[i].imag;
		}
		return d;
	}
	
	private Complex[] packDoubleToComplex(double[] d, Complex[] c) {
		int n = d.length;
		if (c == null || c.length != n/2) {
			c = Complex.allocateComplexArray(n/2);
		}
		int iC = 0;
		for (int i = 0; i < n; i+=2) {
//			System.out.println("n, i, ic " + n + " " + i + " " + iC);
			c[iC].real = d[i];
			c[iC++].imag = d[i+1];
		}
		return c;
	}


}
