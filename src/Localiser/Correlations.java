/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package Localiser;

import fftManager.Complex;
import fftManager.FastFFT;

/**
 * @author Doug Gillespie
 *         <p>
 *         Various functions to do wtih cross correlating two or more signals
 *         <p>
 *         As with the FastFFT class, these functions are no longer static so 
 *         that Allocation of FFT internal storage is done separately for each detector. This 
 *         avoids it having to be continually reallocated if fft's of different lenghts are taken. 
 *         <p>
 *         Also calculates a delay correction factor based on a parabolic interpolation around the 
 *         maximum value. For compatibility with previous versions, the returned results is still 
 *         the integer solution. If you require greater accuracy, you should get the interpolated
 *         correction and add this to the main integer result for double precision accuracy. 
 *         
 */
public class Correlations {

	private FastFFT fastFFT = new FastFFT();
	
	/**
	 * Measure the time delay between pulses on two channels. 
	 * @param f1 waveform on channel 1
	 * @param f2 waveform on channel 2
	 * @param fftLength FFT length to use (data will be packed or truncated as necessary)
	 * @return time delay in samples. 
	 */
	public double getDelay(double[] s1, double[] s2, int fftLength) {

		double[][] fftdata = null;
		Complex[][] fftOutData = null;

		double[][] inputSound = new double[2][];
		inputSound[0] = s1;
		inputSound[1] = s2;

		int soundLen = Math.max(s1.length, s2.length);
		if (fftLength == 0) {
			fftLength = FastFFT.nextBinaryExp(soundLen);
		}
		
		int log2FFTLen = 1;
		int dum = 2;
		while (dum < fftLength) {
			dum *= 2;
			log2FFTLen++;
		}
		if (fftOutData == null)
			fftOutData = new Complex[2][];
		if (fftdata == null)
			fftdata = new double[2][];
		int nS;
		for (int i = 0; i < 2; i++) {
			if (fftdata[i] == null || (fftdata[i].length != fftLength)) {
				fftdata[i] = new double[fftLength];
			}
			nS = Math.min(fftLength, inputSound[i].length);
			for (int iS = 0; iS < nS; iS++) {
				fftdata[i][iS] = inputSound[i][iS];
			}
			for (int iS = nS; iS < fftLength; iS++) {
				fftdata[i][iS] = 0.;
			}
			fftOutData[i] = fastFFT.rfft(fftdata[i], fftOutData[i], log2FFTLen);
		}
		return getDelay(fftOutData[0], fftOutData[1], fftLength);
	}

	/**
	 * Measure the time delay between pulses on two channels. Inputs in this case are the 
	 * spectrum data (most of the cross correlation is done in the frequency domain)
	 * @param f1 complex spectrum on channel 1
	 * @param f2 complex spectrum on channel 2
	 * @param fftLength FFT length to use (data will be packed or truncated as necessary)
	 * @return time delay in samples. 
	 */
	public double getDelay(Complex[] f1, Complex[] f2, int fftLength) {

		int delay = 0;
		Complex[] corrData = null;

		// now make up a new complex array which consists of one chan * the
		// complex conjugate of the other
		// and at the same time, fill in the other half of it.
		if (corrData == null || corrData.length != fftLength) {
			corrData = new Complex[fftLength];
			for (int i = 0; i < fftLength; i++) {
				corrData[i] = new Complex();
			}
		}
		for (int i = 0; i < fftLength / 2; i++) {
			corrData[i].assign(f1[i].times(f2[i].conj()));
			corrData[fftLength - i - 1] = f1[i].conj().times(f2[i]);
		}
		// now take the inverse FFT ...
		fastFFT.ifft(corrData, FastFFT.log2(fftLength));

		// now find the maximum, if it's in the second half, then
		// delay is that - fftLength
		double corrMax = 0;
		double sumSq = 0;
		for (int i = 0; i < fftLength; i++) {
			if (corrData[i].real > corrMax) {
				corrMax = corrData[i].real;
				delay = i;
			}
			sumSq += corrData[i].magsq();
		}

		// calculate interpolated correction around the maximum
		int x1 = delay - 1;
		int x2 = delay;
		int x3 = delay + 1;
		double y1, y2, y3;
		if (x1 < 0) {
			x1 += fftLength;
		}
		if (x3 >= fftLength) {
			x3 -= fftLength;
		}
		y1 = corrData[x1].real;
		y2 = corrData[x2].real;
		y3 = corrData[x3].real;
		double interpolatedCorrection = (y1 - y3) / (y1 + y3 - 2*y2) / 2.;
		
		if (delay > fftLength / 2) {
			delay -= fftLength;
		}
		delay = -delay;
		interpolatedCorrection = -interpolatedCorrection; // need to change sign of this if returning -delay !
		
		return delay + interpolatedCorrection;
	}

	/**
	 * Generate a default timing error which is 1/sqrt(12) times
	 * the sample interval
	 * @param sampleRate sample rate
	 * @return typical timing error
	 */
	public static double defaultTimingError(float sampleRate) {
		return 1./sampleRate/Math.sqrt(12.);
	}

	/**
	 * Calculate a parabolic fit correction based on three bin heights
	 * @param y1 first bin
	 * @param y2 second bin
	 * @param y3 third bin
	 * @return correction = 0.5*(y3-y1)/(2*y2-y1-y3);
	 */
	public static double parabolicCorrection(double y1, double y2, double y3) {
		// need to be wary in case all three values are identical ...
		double bottom = 2*y2-y1-y3;
		if (bottom == 0.) {
			return 0.;
		}
		return 0.5*(y3-y1)/bottom;
	}
}
