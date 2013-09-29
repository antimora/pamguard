package whistlesAndMoans;

import java.util.List;
import java.util.ListIterator;

import PamUtils.PamUtils;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import fftManager.Complex;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;
import fftManager.FastFFT;

/**
 * Class for estimating whistle delays from a whistle shape. 
 * <br>to save processing, one of these should be created for each channel group
 * so that multiple delay measures can be pre prepared and populated 
 * simultaneously - i.e. each bit of FFT data will need to go into one 
 * measure as it, but into the next as it's complex conjugate. 
 * <br>Doing everything at once should save data access times. 
 * @author Doug Gillespie
 *
 */
public class WhistleDelays {

	private WhistleMoanControl whistleMoanControl;
	
	private WhistleToneConnectProcess whProcess;
	
	private FFTDataBlock sourceData;
	
	private int channelMap; // channel map for one detector group
	private int nChannels; // total num of channels
	private int[] channelList; // list of channels. 
	int nDelays;
	
	private DelayMeasure[] delayMeasures;
	
	private FastFFT fft = new FastFFT();

	public WhistleDelays(WhistleMoanControl whistleMoanControl, int channelMap) {
		super();
		this.whistleMoanControl = whistleMoanControl;
		this.channelMap = channelMap;
		whProcess = whistleMoanControl.getWhistleToneProcess();
		nChannels = PamUtils.getNumChannels(channelMap);
		channelList = PamUtils.getChannelArray(channelMap);
		nDelays = nChannels * (nChannels - 1) / 2;
	}
	
	public void prepareBearings() {
		sourceData = (FFTDataBlock) whProcess.getParentDataBlock();
		if (nDelays <= 0) {
			return;
		};
		delayMeasures = new DelayMeasure[nDelays];
		for (int i = 0; i < nDelays; i++) {
			delayMeasures[i] = new DelayMeasure(sourceData.getFftLength());
		}
	}
	
	/**
	 * Gets the delays for a connected region.
	 * nChan(nChan-1)/2 delays will be returned. 
	 * Delays are channels 0-1, 0-2, 1-2, etc.  
	 * @param channelMap channel bitmap
	 * @param region connected region
	 * @return array of delays from cross correlation in samples. 
	 */
	public double[] getDelays(ConnectedRegion region) {
		if (nDelays <= 0) {
			return null;
		}
		double[] delays = new double[nDelays];
		
		/**
		 * The whistle will have ended in one of the last slices of the data block, 
		 * so the plan is to iterate backwards through the FFT data until we either 
		 * reach the start of the region OR run out of FFT data (It may have been discarded if
		 * the region was very long).
		 * 
		 *   We want a channel list of groupChannels, so set up a wantedChannels flag 
		 */
		int wantedChannels = 0;
		int firstSlice = region.getFirstSlice();
		List<SliceData> sliceList = region.getSliceData();
		ListIterator<SliceData> sliceIterator = sliceList.listIterator(sliceList.size()-1);
		SliceData sliceData;
		Complex[][] channelFFTData = new Complex[PamConstants.MAX_CHANNELS][];
		int iChan;
		for (int iC = 0; iC < nDelays; iC++) {
			delayMeasures[iC].clear(sourceData.getFftLength());
		}
		
		FFTDataUnit fftDataUnit;
//		System.out.println("Bearings for region length " + region.getDuration() + " start " + region.getFirstSlice());
		synchronized (sourceData) {
			ListIterator<FFTDataUnit> fftIterator = sourceData.getListIterator(PamDataBlock.ITERATOR_END);
			while (sliceIterator.hasPrevious()) {
				sliceData = sliceIterator.previous();
				wantedChannels = 0;
				while (fftIterator.hasPrevious()) {
					fftDataUnit = fftIterator.previous();
					if (fftDataUnit.getFftSlice() > sliceData.sliceNumber) {
						continue;
					}
					else if (fftDataUnit.getFftSlice() < firstSlice) {
						break;
					}
					wantedChannels |= fftDataUnit.getChannelBitmap();
					iChan = PamUtils.getSingleChannel(fftDataUnit.getChannelBitmap());
					channelFFTData[iChan] = fftDataUnit.getFftData();
					if ((wantedChannels & channelMap) == channelMap) {
//						System.out.println("Complete slice data channels " + channelMap + " fft slice " + fftDataUnit.getFftSlice());
						/**
						 *  can now get the data out of the channelDataUnits which correspond to
						 *  the frequencies with hits in this slice.  
						 */
						for (int iPeak = 0; iPeak < sliceData.nPeaks; iPeak++) {
							for (int iF = sliceData.peakInfo[iPeak][0]; iF <= sliceData.peakInfo[iPeak][2]; iF++) {
								int iD = 0;
								for (int iC = 0; iC < nChannels-1; iC++) {
									for (int iC2 = iC+1; iC2 < nChannels; iC2++) {
									delayMeasures[iD++].addFFTData(channelFFTData[channelList[iC]][iF], 
											channelFFTData[channelList[iC2]][iF], iF);
									}
								}
							}
						}
						break; // what the F*** is this break for ? 
					}
				}
			}			
		} // end of source data synchronisation
		for (int iD = 0; iD < nDelays; iD++) {
			delays[iD] = delayMeasures[iD].getDelay();
		}
		
		return delays;
	}
	
	class DelayMeasure {
		
		private Complex[] complexData;
		
		private int fftLength;
		
		DelayMeasure(int fftLength) {
			this.fftLength = fftLength;
			complexData = Complex.allocateComplexArray(fftLength);
		}
		
		private void clear(int fftLength) {
			if (complexData == null || complexData.length != fftLength) {
				complexData = Complex.allocateComplexArray(fftLength);
				this.fftLength = fftLength;
			}
			else {
				Complex.zeroComplexArray(complexData);
			}
		}
		
		private void addFFTData(Complex ch1, Complex ch2, int iFreq) {
			complexData[iFreq].real += (ch1.real*ch2.real + ch1.imag*ch2.imag);
			complexData[iFreq].imag += (ch1.imag*ch2.real - ch2.imag*ch1.real);
		}
		
		private double getDelay() {
			int i2;
			for (int i = 0; i < fftLength / 2; i++) {
				i2 = fftLength - 1 - i;
				complexData[i2].real = complexData[i].real;
				complexData[i2].imag = -complexData[i].imag;
			}
			fft.ifft(complexData, FastFFT.log2(fftLength));
			Complex[] bearingData = complexData;
			double maxInd = getPeakPos(bearingData);
			
//			double delay = 0;
//			double p1, p2, p3;
//			p2 = bearingData[maxInd].real;
//			if (maxInd == 0) {
//				p1 = bearingData[fftLength-1].real;
//			}
//			else {
//				p1 = bearingData[maxInd-1].real;
//			}
//			if (maxInd == fftLength-1) {
//				p3 = bearingData[0].real;
//			}
//			else {
//				p3 = bearingData[maxInd+1].real;
//			}
////			interpolatedCorrection = (y1 - y3) / (y1 + y3 - 2*y2) / 2.;
//			delay = (p1-p3)/(p1+p3 - 2*p2)/2.;
			
			
//			delay += maxInd;
//			if (maxInd > fftLength/2) {
//				delay -= fftLength;
//			}
//			
//			/*
//			 * Now clear around the peak pos, and find the next biggest peak. 
//			 */
//			int iP = maxInd;
//			while (true) {
//				if (bearingData[iP].real < 0) {
//					break;
//				}
//				bearingData[iP++].real = 0;
//				if (iP >= fftLength) {
//					iP = 0;
//				}
//				if (iP == maxInd) {
//					break;
//				}
//			}
//			iP = maxInd;
//			while (true) {
//				if (bearingData[iP].real < 0) {
//					break;
//				}
//				bearingData[iP--].real = 0;
//				if (iP < 0) {
//					iP = fftLength-1;
//				}
//				if (iP == maxInd) {
//					break;
//				}
//			}
//			int nextPeak = getPeakPos(bearingData);
//			double peakRatio = p2/bearingData[nextPeak].real;
//			
////			System.out.println("Max bearing index = " + maxInd + " Height ratio " + peakRatio);
			return maxInd;
		}
		
		private double getPeakPos(Complex[] bearingData) {
			double[] realData = new double[bearingData.length];
			double max = 0;
			double maxInd = 0;
			int l2 = bearingData.length/2;
			for (int i = 0; i < l2; i++) {
				realData[i+l2] = bearingData[i].real;
				realData[i] = bearingData[i+l2].real;
			}
			int l3 = l2*2-1;
			double y1, y2, y3;
			double a, b, c;
			double x, y;
			// work out peak height for every position. 
			for (int i = 1; i < l3; i++) {
				y1 = realData[i-1];
				y2 = realData[i];
				y3 = realData[i+1];
				if (y2 > y1 && y2 > y3) {
					a = (y3+y1-2*y2)/2.;
					b = (y3-y1)/2;
					c = y2;
					x = -b/2./a;
					y = a*x*x+b*x+c;
					if (y > max) {
						max = y;
						maxInd = i+x;
					}
				}
//				if (bearingData[i].real > max) {
//					max = bearingData[i].real;
//					maxInd = i;
//				}
			}			
			return maxInd - l2;
		}
		
	}
}
