/**
 * 
 */
package ipiDemo;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import PamController.PamController;
import echoDetector.EchoDataUnit;

/**
 * This is a module for Sperm whale Inter-pulse interval (IPI) computation. It
 * uses cepstral analysis to compute the Inter-pulse interval of sperm whale
 * clicks.Two related signal processing methods are used to obtain an IPI:
 * <p>
 * Method 1 computes the IPI for each click and then averages these IPIs
 * together as described in Goold 1996 - J. Acoust. Soc. Am. Vol 100(5), pp
 * 3431-3441.
 * <p>
 * Method 2 computes the ensemble averaged cepstrum from all clicks and then
 * picks one IPI from this ensemble averaged signal similar to Teloni 2007 - J.
 * Cetacean Res. Manage. Vol 9(2), pp 127-136.
 * 
 * @author Brian Miller
 */
public class IpiProcess extends PamProcess {

	/**
	 * Address of the controlling process. Needed for accessing IPI parameters
	 * etc.
	 */
	IpiController ipiController;

	/**
	 * The number of clicks used to create the ensemble averaged functions
	 */
	int numClicks;

	/**
	 * Number of samples required to achieve the ipiDuration
	 */
	private int ipiSamples;

	/**
	 * Store the IPI for each click as an element in this vector
	 */
	private Vector<Double> ipiDelay;

	/**
	 * The value of the echoData at the ipiDelay time.
	 */
	private Vector<Double> ipiValue;

	/**
	 * A histogram of the IPIs of each click.
	 * Indicies represent cepstrum time bins. Values hold the bin count.
	 */
	int[] ipiHistogram;
	
	/**
	 * The peak value of the histogam (count of the tallest bin
	 */
	int maxHistogramCount;
	
	/**
	 * The average IPI value from each of the echo detections
	 */
	private double ipiDelayMean;

	/**
	 * Store the ensemble average of each of the echo detection functions Then
	 * we can pick the peak of the ensemble averaged function This is how Teloni
	 * et al 2008 do it.
	 */
	private double[] summedIpiFunction;

	/**
	 * The computed IPI of the whale in milliseconds computed from the ensemble
	 * averaged cepstrum.
	 */
	private double ipiDelayEnsembleAvg;

	/**
	 * This value reflects the uncertainty in the measurement of the ensemble
	 * averaged IPI. The true IPI should be within ipiDelayEnsembleAvg -
	 * ipiDelayLowerLimit.
	 */
	private double ipiDelayLowerLimit;

	/**
	 * This value reflects the uncertainty in the measurement of the ensemble
	 * averaged IPI. The true IPI should be within ipiDelayEnsembleAvg +
	 * ipiDelayUpperLimit.
	 */
	private double ipiDelayUpperLimit;

	/**
	 * The height of the cepstrum at the time of the IPI peak. Used for scaling
	 * the ensemble average (when displayed as a plugin panel).
	 */
	private double ipiValueEnsembleAvg;

	public IpiProcess(IpiController ipiController) {
		// you must call the constructor from the super class.
		super(ipiController, null);
		// Store a reference to the controller of this process
		this.ipiController = ipiController;

	}

	@Override
	public void prepareProcess() {
		// TODO Auto-generated method stub
		super.prepareProcess();
		/*
		 * Need to hunt around now in the Pamguard model and try to find the
		 * Echo Detection data block we want and then subscribe to it. First get
		 * a list of all PamDataBlocks holding EchoDataUnits from the
		 * PamController
		 */
		ArrayList<PamDataBlock> echoDataBlocks = PamController.getInstance()
				.getDataBlocks(EchoDataUnit.class, false);
		if (echoDataBlocks == null) {
			System.out.println("No click detectors found");
			setParentDataBlock(null);
			return;
		}
		/*
		 * If a click data blocks exist, subscribe to the one in the parameters.
		 */
		PamDataBlock<EchoDataUnit> echoDataBlock = echoDataBlocks
				.get(ipiController.ipiProcessParameters.echoDataBlock);

		setParentDataBlock(echoDataBlock);

		// Figure out how many samples are required to get the right IPI
		// duration
		long targetSamples = (long) (echoDataBlock.getSampleRate()
				* ipiController.ipiProcessParameters.ipiDuration / 1e3);

		// For faster FFT processing make this a power of 2
		// This is now done in the Echo detector. Perhaps remove it?
		ipiSamples = PamUtils.getMinFftLength(targetSamples);

		summedIpiFunction = new double[ipiSamples];
		ipiHistogram = new int[ipiSamples];
		maxHistogramCount = 0;

	}
	
	/**
	 * Most of the IPI computation is done here. It makes sense to only compute
	 * things when new data arrive. The new data should contain the cepstrum of
	 * a sperm whale click. We pick the peak and add the cepstrum to the 
	 * ensemble average. 
	 */
	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		// Get the echo data units which contain the cepstrum of the click
		EchoDataUnit echoData = (EchoDataUnit) arg;

		double[] newIpiFunction = echoData.getEchoData();
		double dataSampleRate = echoData.getEchoSampleRate();
		double peakWidthPercent = ipiController.ipiProcessParameters.ipiPeakWidthPercent;
		double ipiSum = 0;
		double ipiValue = 0;
		int minIpiIndex, maxIpiIndex, ipiEnsembleAvgIx = 0, ipiDelayIx = 0;

		int availableSamples = newIpiFunction.length;

		if (availableSamples < ipiSamples) {
			System.out.println("ipiProcess.newData(): " + ipiSamples
					+ " required but only " + availableSamples + " found");
			return;
		}

		// Add the new cepstrum to the ensemble average
		numClicks++;
		for (int i = 0; i < ipiSamples; i++) {
			summedIpiFunction[i] += newIpiFunction[i];
		}

		/*
		 * Work out the lower and upper indicies for searching for the cepstral
		 * peak. This should probably be done only once during pamStart...
		 */
		minIpiIndex = (int) (ipiController.ipiProcessParameters.minIpiTime
				/ 1e3 * dataSampleRate / 2);
		maxIpiIndex = (int) (ipiController.ipiProcessParameters.maxIpiTime
				/ 1e3 * dataSampleRate / 2);
		maxIpiIndex = Math.min(maxIpiIndex, availableSamples);

		/*
		 * Now pick the peak from both the new data and the ensemble averaged
		 * IPI function.
		 */
		double ensemblePeak = 0;
		for (int i = minIpiIndex; i < maxIpiIndex; i++) {
			if (summedIpiFunction[i] > ensemblePeak) {
				ensemblePeak = summedIpiFunction[i];
				ipiEnsembleAvgIx = i;
			}
			if (newIpiFunction[i] > ipiValue) {
				ipiValue = newIpiFunction[i];
				ipiDelayIx = i;
			}
		}
		ipiValueEnsembleAvg = ensemblePeak;

		/*
		 * Again remember the factors of 2 here are because we're dealing with
		 * the cepstral period = sample period/2
		 */
		ipiDelayEnsembleAvg = ipiEnsembleAvgIx / dataSampleRate * 2;

		int[] peakWidthIx = getPeakWidth(summedIpiFunction, ipiEnsembleAvgIx,
				peakWidthPercent);

		ipiDelayLowerLimit = ipiDelayEnsembleAvg - peakWidthIx[0]
				/ dataSampleRate * 2.0;

		ipiDelayUpperLimit = peakWidthIx[1] / dataSampleRate * 2.0
				- ipiDelayEnsembleAvg;

		ipiDelay.addElement(ipiDelayIx / dataSampleRate * 2);
		
		ipiHistogram[ipiDelayIx]++;
		
		if(ipiHistogram[ipiDelayIx] >= maxHistogramCount){
			maxHistogramCount = ipiHistogram[ipiDelayIx];
		}

		/*
		 * Compute the mean of all of the individual IPI delays This is similar
		 * to Goold 1996 and Rhinelander and Dawson 2004
		 */
		for (int i = 0; i < numClicks; i++)
			ipiSum += ipiDelay.get(i);

		ipiDelayMean = ipiSum / numClicks;
		
		// update the side panel
		ipiController.ipiSidePanel.fillData();
	}

	@Override
	public void pamStart() {
		// this method is abstract in the superclass and must therefore be
		// overridden
		clearIpiData();
	}

	@Override
	public void pamStop() {
		// this method is abstract in the superclass and must therefore be
		// overridden 
		
		// Dump the ensemble averaged IPI measurement to the IPI file
		writeIpiFile();
	}

	/**
	 * Used to measure the peak width of the IPI delay. Start at the
	 * peak position and iterate to the left until the sample amplitude is
	 * less than percent% of the IPI value. Then do the same thing but iterate
	 * to the right.
	 */
	private int[] getPeakWidth(double[] data, int peakPos, double percent) {
		
		double ipiValue, nextVal, testRatio;
		int i, lowIx, highIx, limits[];

		// Find the lower peak limit
		ipiValue = data[peakPos];
		i = peakPos;
		nextVal = data[i];
		testRatio = (nextVal / ipiValue * 100);
		while ((i > 0) && (testRatio > percent)) {
			i--;
			nextVal = data[i];
			testRatio = (nextVal / ipiValue * 100);
		}
		lowIx = i;

		// Find the upper peak limit
		i = peakPos;
		nextVal = data[i];
		testRatio = (nextVal / ipiValue * 100);
		while (i < data.length - 1 && testRatio > percent) {
			i++;
			nextVal = data[i];
			testRatio = (nextVal / ipiValue * 100);
		}
		highIx = i;

		limits = new int[2];
		limits[0] = lowIx;
		limits[1] = highIx;
		return limits;
	}

	/*
	 * public int getChannel(){ return
	 * ipiController.ipiProcessParameters.channel; }
	 */

	public int getNumIpiSamples() {
		return ipiSamples;
	}

	public Vector<Double> getIpiDelays() {
		return ipiDelay;
	}

	public Vector<Double> getIpiValues() {
		return ipiValue;
	}
	
	public int getMaxHistogramCount() {
		return maxHistogramCount;
	}
	
	public int[] getIpiHistogram(){
		return ipiHistogram;
	}
	
	public double getIpiDelayMean() {
		return ipiDelayMean;
	}

	public double[] getSummedIpiFunction() {
		return summedIpiFunction;
	}

	public double getIpiDelayEnsembleAvg() {
		return ipiDelayEnsembleAvg;
	}

	public double getIpiValueEnsembleAvg() {
		return ipiValueEnsembleAvg;
	}

	public double getIpiDelayLowerLimit() {
		return ipiDelayLowerLimit;
	}

	public double getIpiDelayUpperLimit() {
		return ipiDelayUpperLimit;
	}
	
	private void clearIpiData() {
		/*
		 * Reset all of the IPI data before starting processing
		 */
		numClicks = 0;
		ipiDelay = new Vector<Double>(10);
		ipiValue = new Vector<Double>(10);
		for (int i = 0; i < ipiSamples; i++)
			summedIpiFunction[i] = 0;

		ipiDelayMean = 0;

		ipiDelayEnsembleAvg = 0;

		ipiValueEnsembleAvg = 0;

		ipiDelayLowerLimit = 0;

		ipiDelayUpperLimit = 0;
	}

	private void writeIpiFile() {
		String filename = ipiController.ipiProcessParameters.outputFileName;
		double[] summedIpiFunction = getSummedIpiFunction();

		/*
		 * System.out.println("" + numClicks + " IPIs averaged.");
		 * System.out.println("Mean IPI delay was: " + ipiDelayMean);
		 * System.out.println("Ensemble averaged IPI delay was: " +
		 * ipiDelayEnsembleAvg);
		 */
		PrintWriter out;
		try {
			out = new PrintWriter(filename);
			for (int i = 0; i < ipiSamples; i++) {
				out.println(summedIpiFunction[i]/ipiValueEnsembleAvg);
			}
			out.println("");
			out.println("" + numClicks + " IPIs averaged.");
			out.println("Mean IPI delay was: " + ipiDelayMean);
			out.println("Ensemble averaged IPI delay was: "
					+ ipiDelayEnsembleAvg);
			out.println("Audio sample rate was: " + sampleRate);
			out.close();
			System.out.println("Wrote ensemble averaged IPI function to "
					+ filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}