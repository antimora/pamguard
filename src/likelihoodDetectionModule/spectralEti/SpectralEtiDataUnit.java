package likelihoodDetectionModule.spectralEti;

import PamDetection.AcousticDataUnit;

/**
 * The data type for the output of the spectralEti module.
 * 
 * @author Dave Flogeras
 *
 */
public class SpectralEtiDataUnit extends AcousticDataUnit {

	double[] spectraData = null;
	
	public SpectralEtiDataUnit( long timeMilliseconds, int channelBitmap, long startSample, long duration ) {
		super( timeMilliseconds, channelBitmap, startSample, duration );
	}
	
	public void setData( double[] data ) {
		this.spectraData = data;
	}
	
	public double[] getData() {
		return spectraData;
	}
}
