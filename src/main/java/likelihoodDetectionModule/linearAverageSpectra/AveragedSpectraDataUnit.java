package likelihoodDetectionModule.linearAverageSpectra;

import PamDetection.AcousticDataUnit;

/**
 * This is the data type produced by the linearAverageSpectra
 * 
 * @author Dave Flogeras
 *
 * 
 */
public class AveragedSpectraDataUnit extends AcousticDataUnit {

	double[] spectraData = null;
	
	public AveragedSpectraDataUnit( long timeMilliseconds, int channelBitmap, long startSample, long duration ) {
		super( timeMilliseconds, channelBitmap, startSample, duration );
	}
	
	public void setData( double[] data ) {
		this.spectraData = data;
	}
	
	public double[] getData() {
		return spectraData;
	}
}
