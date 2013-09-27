package whistlesAndMoans;

import Localiser.bearingLocaliser.DetectionGrouper;
import PamController.PamControlledUnit;
import PamDetection.PamDetection;
import PamguardMVC.PamDataBlock;

public class WhistleDetectionGrouper extends DetectionGrouper<ConnectedRegionDataUnit> {

	public WhistleDetectionGrouper(PamControlledUnit pamControlledUnit,
			PamDataBlock sourceDataBlock) {
		super(pamControlledUnit, sourceDataBlock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean match(PamDetection currentData, PamDetection olderData) {
		// see if there is a 50% overlap in time and frequency.
		if (currentData.getChannelBitmap() == olderData.getChannelBitmap()) {
			return false;
		}
		double oT = getTOverlap(currentData, olderData);
		double oF = getFOverlap(currentData, olderData);
		if (oF > 0.5 && oT > 0.5) {
			return true;
		}
		
		return false;
	}

	double getTOverlap(PamDetection w1, PamDetection w2) {
		return Math.max(w1.getTimeOverlap(w2), w2.getTimeOverlap(w1));
	}
	
	double getFOverlap(PamDetection w1, PamDetection w2) {
		return Math.max(w1.getFrequencyOverlap(w2), w2.getFrequencyOverlap(w1));
	}
	


}
