package spectrogramNoiseReduction.kernelSmoothing;

import java.io.Serializable;

import org.w3c.dom.Element;

import PamUtils.PamUtils;
import PamguardMVC.PamConstants;

import spectrogramNoiseReduction.SpecNoiseDialogComponent;
import spectrogramNoiseReduction.SpecNoiseMethod;

import fftManager.Complex;
import fftManager.FFTDataUnit;

public class KernelSmoothing extends SpecNoiseMethod {

	private double kernel[][];
	private int nCol, nRow;
	private ChannelProcess[] channelProcesses;
	
	public KernelSmoothing() {
		super();
		
		/*
		 * Create and normalise a standard Gaussian kernel. 
		 */
		setKernel(new double[][] { { 1, 2, 1 }, { 2, 4, 2 }, { 1, 2, 1 } });
//		setKernel(new double[][] { { 0, 0, 0 }, { 0, 1, 0 }, { 0, 0, 0 } });
		
		
	}
	
	private void setKernel(double[][] kernel) {
		this.kernel = kernel;
		nCol = kernel.length;
		nRow = kernel[0].length;
		normaliseKernel();
	}
	
	private void normaliseKernel() {
		double kt = 0;
		
		for (int i = 0; i < kernel.length; i++) {
			for (int j = 0; j < kernel[i].length; j++) {
				kt += kernel[i][j];
			}
		}
		for (int i = 0; i < kernel.length; i++) {
			for (int j = 0; j < kernel[i].length; j++) {
				kernel[i][j] /= kt;
			}
		}
	}

	@Override
	public SpecNoiseDialogComponent getDialogComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return "Gaussian Kernel Smoothing";
	}

	@Override
	public String getDescription() {
		return "<html>The spectrogram is smoothed by convolving <p>"+
		             "the image with a Gaussian smoothing kernel<p>"+
		             " 1 2 1<p>" +
		             " 2 4 2<p>" +
		             " 1 2 1</html>";
	}

	@Override
	public int getDelay() {
		return 1;
	}

	@Override
	public Serializable getParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean initialise(int channelMap) {
		channelProcesses = new ChannelProcess[PamConstants.MAX_CHANNELS];
		for (int i = 0; i < channelProcesses.length; i++) {
			if ((channelMap & 1<<i) != 0) {
				channelProcesses[i] = new ChannelProcess();
			}
		}
		return true;
	}

	@Override
	public boolean runNoiseReduction(FFTDataUnit fftDataUnit) {
		int iChan = PamUtils.getSingleChannel(fftDataUnit.getChannelBitmap());
		return channelProcesses[iChan].run(fftDataUnit.getFftData());
	}
	
	class ChannelProcess {
		
		private double[][] localStore;
		private Complex[][] localComplex;
		
		public ChannelProcess() {
			
			localStore = new double[nCol][];
			localComplex = new Complex[nCol][];
			
		}
		
		/**
		 * The kernel is made up of power data, and is used as a
		 * scaling factor on the fft data so that fft data phase is maintained. 
		 * @param fftData FFT data to smooth
		 * @return true if Ok.
		 */
		boolean run(Complex[] fftData) {
			/*
			 * Shuffle along old stores, then add the latest to the list end
			 * keeping old arrays so that they don't need to be reallocated. 
			 */
			int lastCol = nCol-1;
			double[] dummyStore = localStore[0];
			Complex[] dummyComplex = localComplex[0];
			for (int iCol = 0; iCol < lastCol; iCol++) {
				localStore[iCol] = localStore[iCol+1];
				localComplex[iCol] = localComplex[iCol+1];
			}
			localStore[lastCol] = dummyStore;
			localComplex[lastCol] = dummyComplex;
			
			/*
			 * Data will have to be copied into the double arrays - so
			 * make the arrays a bit longer so that when the 
			 * convolution is run, there are enough spare zeros
			 * at the ends. 
			 */
			int space = 1;
			int L = fftData.length + 2*space;
			if (localStore[lastCol] == null || localStore[lastCol].length != L) {
				localStore[lastCol] = new double[L];
			}
			if (localComplex[lastCol] == null || localComplex[lastCol].length != fftData.length) {
				localComplex[lastCol] = Complex.allocateComplexArray(fftData.length);
			}
			for (int i = 0; i < fftData.length; i++) {
				localStore[lastCol][i+space] = fftData[i].magsq();
				localComplex[lastCol][i].assign(fftData[i]);
			}
			
			/*
			 * If the first row is empty, then it's not possible
			 * to do the convolution, so just return. things will kick
			 * in when the arrays all fill up on the third call.  
			 */
			if (localStore[0] == null) {
				return false;
			}
			/*
			 * Now do the convolution for each point
			 */
			double dumTot;
			double cenVal;
			for (int i = 0; i < fftData.length; i++) {
				dumTot = 0;
				cenVal = localStore[1][i+space];
				for (int iCol = 0; iCol < nCol; iCol++) {
					for (int iRow = 0; iRow < nRow; iRow ++) {
						dumTot += localStore[iCol][iRow+i] * kernel[iCol][iRow];
					}
				}
				fftData[i] = localComplex[1][i].times(Math.sqrt(dumTot / cenVal));
			}			
			
			return true;
		}
	}

	@Override
	public boolean setParams(Serializable noiseParams) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void fillXMLParameters(Element n) {
		// TODO Auto-generated method stub
		
	}

}
