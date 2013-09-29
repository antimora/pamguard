package ltsa;

import PamUtils.FrequencyFormat;
import PamguardMVC.PamDataUnit;
import dataGram.DatagramProvider;
import dataGram.DatagramScaleInformation;
import fftManager.Complex;
import fftManager.FFTDataUnit;

public class LtsaDatagramProvider implements DatagramProvider {
	
	private LtsaProcess ltsaProcess;

	public LtsaDatagramProvider(LtsaProcess ltsaProcess) {
		super();
		this.ltsaProcess = ltsaProcess;
	}

	@Override
	public int addDatagramData(PamDataUnit dataUnit, double[] dataGramLine) {
		FFTDataUnit du = (FFTDataUnit) dataUnit;
		Complex[] fftData = du.getFftData();
		int n = Math.min(fftData.length, dataGramLine.length);
		for (int i = 0; i < n; i++) {
			dataGramLine[i] += fftData[i].magsq(); 
		}
		return n;
	}

	@Override
	public int getNumDataGramPoints() {
		if (ltsaProcess.sourceDataBlock == null) {
			return 0;
		}
		return ltsaProcess.sourceDataBlock.getFftLength()/2;
	}

	@Override
	public DatagramScaleInformation getScaleInformation() {
		float sampleRate = ltsaProcess.getSampleRate();
		double maxFreq = sampleRate/2;
		FrequencyFormat ff = FrequencyFormat.getFrequencyFormat(maxFreq);
		
		return new DatagramScaleInformation(0, maxFreq/ff.getScale(), ff.getUnitText());
	}

}
