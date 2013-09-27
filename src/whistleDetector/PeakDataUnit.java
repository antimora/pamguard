package whistleDetector;

import java.util.ArrayList;

import PamDetection.PamDetection;

/**
 * WhistlePeakUnits contain information
 * from a single FFT slice. Each FFT slice may have several peaks
 * so these are stored as an rray list.
 * @author Doug
 *
 */
public class PeakDataUnit extends PamDetection<PamDetection, ShapeDataUnit> {

	private ArrayList<WhistlePeak> whistlePeaks;
	
	private int slicenumber;
	
	public PeakDataUnit(long timeMilliseconds, int channelBitmap, long startSample, int duration,
			ArrayList<WhistlePeak> whistlePeaks, int sliceNumber) {
		super(timeMilliseconds, channelBitmap, startSample, duration);
		this.whistlePeaks = whistlePeaks;
		this.slicenumber = sliceNumber;
	}

	public ArrayList<WhistlePeak> getWhistlePeaks() {
		return whistlePeaks;
	}

	public void setWhistlePeaks(ArrayList<WhistlePeak> whistlePeaks) {
		this.whistlePeaks = whistlePeaks;
	}

	public int getSlicenumber() {
		return slicenumber;
	}

}
