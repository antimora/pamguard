package spectrogramNoiseReduction.threshold;

import java.io.Serializable;

import org.w3c.dom.Element;

import spectrogramNoiseReduction.SpecNoiseDialogComponent;
import spectrogramNoiseReduction.SpecNoiseMethod;
import fftManager.Complex;
import fftManager.FFTDataUnit;

public class SpectrogramThreshold extends SpecNoiseMethod{


	public static final int OUTPUT_BINARY = 0;
	public static final int OUTPUT_INPUT = 1;
	public static final int OUTPUT_RAW = 2;
	
	protected ThresholdParams thresholdParams = new ThresholdParams();
	
	private double powerThreshold;
	
	private ThresholdDialogComponent thresholdDialogComponent;
	
	public SpectrogramThreshold() {
		thresholdDialogComponent = new ThresholdDialogComponent(this);
	}
	
	@Override
	public SpecNoiseDialogComponent getDialogComponent() {
		return thresholdDialogComponent;
	}

	@Override
	public String getName() {
		return "Thresholding";
	}

	@Override
	public String getDescription() {
		return "<html>A threshold is applied and all data<p>" +
				"falling below that threshold set to 0</html>";
	}

	@Override
	public int getDelay() {
		return 0;
	}
	
	@Override
	public Serializable getParams() {
		return thresholdParams;
	}

	@Override
	public boolean initialise(int channelMap) {
		powerThreshold = Math.pow(10.,thresholdParams.thresholdDB/10.);
		return true;
	}

	@Override
	public boolean runNoiseReduction(FFTDataUnit fftDataUnit) {
		Complex[] fftData = fftDataUnit.getFftData();
		for (int i = 0; i < fftData.length; i++) {
			if (fftData[i].magsq() < powerThreshold) {
				fftData[i].assign(0,0);
			}
			else if (thresholdParams.finalOutput != OUTPUT_INPUT) {
				fftData[i].assign(1,0);
			}
		}
		return true;
	}
	
	/**
	 * go through an array of other data, and 
	 * copy data that's in earlyData into thresholdData
	 * if the threhsoldData is > 0;
	 * @param earlyData data to pick from (generally raw input fft data to noise process)
	 * @param binaryChoice output from runNoiseReduction()
	 */
	public void pickEarlierData(Complex[] earlyData, Complex[] thresholdData) {
		for (int i = 0; i < earlyData.length; i++) {
			if (thresholdData[i].real > 0) {
				thresholdData[i].assign(earlyData[i]);
			}
		}
	}

	@Override
	public boolean setParams(Serializable noiseParams) {
		try {
			thresholdParams = (ThresholdParams) noiseParams;
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void fillXMLParameters(Element n) {
		n.setAttribute("thresholdDB", new Double(thresholdParams.thresholdDB).toString());		
	}
}
