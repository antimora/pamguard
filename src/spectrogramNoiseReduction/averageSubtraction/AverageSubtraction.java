package spectrogramNoiseReduction.averageSubtraction;

import java.io.Serializable;

import org.w3c.dom.Element;

import PamUtils.PamUtils;
import PamguardMVC.PamConstants;
import spectrogramNoiseReduction.SpecNoiseDialogComponent;
import spectrogramNoiseReduction.SpecNoiseMethod;
import fftManager.Complex;
import fftManager.FFTDataUnit;

public class AverageSubtraction extends SpecNoiseMethod {

	private AverageSubtractionDialogBits averageSubtractionDialogBits;

	protected AverageSubtractionParameters averageSubtractionParameters = new AverageSubtractionParameters();

	/*
	 * update constants for stored and new data.
	 */
	private double newConstant, oldConstant;

	private static final int runInSlices = 10;

	private static final double runInScale = 2;

	int totalSlices;

	/**
	 * Storage of data for each channel
	 */
	private double[][] channelStorage;

	public AverageSubtraction() {
		super();

		averageSubtractionDialogBits = new AverageSubtractionDialogBits(this);
	}

	@Override
	public SpecNoiseDialogComponent getDialogComponent() {
		return averageSubtractionDialogBits;
	}

	@Override
	public String getName() {
		return "Average Subtraction";
	}

	@Override
	public String getDescription() {
		return "<html>A decaying average spectrogram <p>is computed" +
		"and subtracted from <p>the current spectrogram value</html>";
	}

	@Override
	public int getDelay() {
		return 0;
	}

	@Override
	public Serializable getParams() {
		return averageSubtractionParameters;
	}

	@Override
	public boolean initialise(int channelMap) {
		channelStorage = new double[PamConstants.MAX_CHANNELS][];

		totalSlices = 0;
		// can only allocate individual channel data when we know 
		// how long the fft data are !

		newConstant = averageSubtractionParameters.updateConstant;
		oldConstant = 1. - newConstant;

		return true;
	}

	@Override
	public boolean runNoiseReduction(FFTDataUnit fftDataUnit) {

		Complex[] fftData = fftDataUnit.getFftData();
		int len = fftData.length;
		int iChan = PamUtils.getSingleChannel(fftDataUnit.getChannelBitmap());
		double[] channelData = channelStorage[iChan];
		double dum;
		// the first time this get's called, the channelData will all 
		// be zero, so it won't be possible to update unless a copy
		// of the new data is put in there as a starting reference
		double scale;
		if (channelData == null || channelData.length != len) {
			channelData = channelStorage[iChan] = new double[len];
			totalSlices = 0;
		}
		/*
		 * the log10(fftData) are divided by an additional 
		 * factor of two since it's the log of the squared data and
		 * it's divided off the unsquared data. 
		 */
		if (totalSlices++ < runInSlices) {
			for (int i = 0; i < len; i++) {
				if (fftData[i].isNaN()) {
					continue;
				}
				if ((dum = fftData[i].magsq()) == 0) {
					continue;
				}
				channelData[i] += Math.log10(dum)/2. / runInSlices * runInScale;
				scale = Math.pow(10., channelData[i]);
				fftData[i].internalTimes(1./runInSlices);
			}	
		}
		else {
			for (int i = 0; i < len; i++) {
				if (fftData[i].isNaN()) {
					continue;
				}
				if ((dum = fftData[i].magsq()) == 0) {
					continue;
				}
				scale = Math.pow(10., channelData[i]);
				channelData[i] *= oldConstant;
				channelData[i] += newConstant * Math.log10(dum)/2;
				fftData[i].internalTimes(1./scale);
			}
		}


		return true;
	}

	@Override
	public boolean setParams(Serializable noiseParams) {
		try {
			averageSubtractionParameters = (AverageSubtractionParameters) noiseParams;
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void fillXMLParameters(Element n) {
		n.setAttribute("updateConstant", new Double(averageSubtractionParameters.updateConstant).toString());
	}


}
