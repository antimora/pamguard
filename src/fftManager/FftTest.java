package fftManager;

import java.util.Random;

import Filters.Filter;
import Filters.FilterBand;
import Filters.FilterParams;
import Filters.FilterType;
import Filters.IirfFilter;
import PamUtils.PamUtils;

public class FftTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int nTrial = 10000;
		int fftLen = 1024;
		long startTime, endTime;
		FastFFT fastFFT = new FastFFT();
		FFT fft = new FFT();
		
		FilterParams filterParams = new FilterParams();
		filterParams.filterType = FilterType.BUTTERWORTH;
		filterParams.filterBand = FilterBand.LOWPASS;
		filterParams.filterOrder = 80;
		filterParams.lowPassFreq = 2000;
		filterParams.passBandRipple = 2;
		
		float sampleRate = 48000;
		
		Filter filter = new IirfFilter(0, sampleRate, filterParams);
		
		try {
			if (args.length > 0) {
				nTrial = Integer.valueOf(args[0]); 
			}
			if (args.length > 1) {
				fftLen = Integer.valueOf(args[1]); 
			}
		}
		catch(NumberFormatException e) {
			e.printStackTrace();
			return;
		}

		int m = PamUtils.log2(fftLen);
		
		double[] realData = new double[fftLen];
		Complex[] complexData = Complex.allocateComplexArray(fftLen);
		Random r = new Random();
		for (int i = 0; i < fftLen; i++) {
			realData[i] = r.nextGaussian();
		}
		startTime = System.currentTimeMillis();
		for (int i = 0; i < nTrial; i++) {
			fastFFT.rfft(realData, complexData, m);
//			filter.runFilter(realData);
		}
		endTime = System.currentTimeMillis();
		long timeTaken = endTime-startTime;
		System.out.println(String.format("Time taken for %d %dpt FFT's = %d ms", nTrial, fftLen, timeTaken));

//		Complex[] c = Complex.allocateComplexArray(8);
//		double[] d = new double[8];
//		d[1] = d[0] = 1;
//		c[1].real = 1;
//		Complex[] c2 = fastFFT.rfft(d, null, 3);
//		Complex[] c2a = fft.recursiveFFT(c);
//		fastFFT.fft(c, 3);
//		for (int i = 0; i < 4; i++) {
//			c[i].assign(c2[i]);
//			c[7-i].assign(c2[i].conj());
//		}
//		Complex[] c3 = fft.recursiveIFFT(c2);
//        fastFFT.ifft(c, 3);		
	}
}
