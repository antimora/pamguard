package clickDetector;

import Localiser.bearingLocaliser.GroupDetection;
import PamView.GeneralProjector;
import PamguardMVC.AcousticDataBlock;
import PamguardMVC.PamProcess;

/**
 * Click Train data block deletes old data in a slightly
 * different way to PamDataBlock. Where PamDataBlock
 * starts at the beginning of the list and deletes items
 * until one of them starts after the set time at which point
 * it stops, ClickTrainDataBlock examins all units and deletes
 * all of those which end before the set time. This is necessary since
 * some click train data units last for hours, whereas others 
 * can be deleted after a couple of seconds. 
 * @author Doug Gillespie
 *
 */
public class ClickGroupDataBlock<t extends GroupDetection> extends AcousticDataBlock<t> {



	public ClickGroupDataBlock(Class pduClass, String dataName, PamProcess parentProcess, int channelMap) {
		super(pduClass, dataName, parentProcess, channelMap);
	}


	@Override
	synchronized protected int removeOldUnitsT(long currentTimeMS) {
		int unitsRemoved = 0;
		if (pamDataUnits.isEmpty())
			return 0;
		GroupDetection clickTrain;
		long firstWantedTime = currentTimeMS - this.naturalLifetime * 1000;
		firstWantedTime = Math.min(firstWantedTime, currentTimeMS - getRequiredHistory());
		
		int i = 0;

		while (i < pamDataUnits.size()) {
			clickTrain = pamDataUnits.get(i);
			if (clickTrain.getTimeMilliseconds() + clickTrain.getDuration() < firstWantedTime) {
				pamDataUnits.remove(clickTrain);
			}
			else if ((clickTrain).getStatus() == ClickTrainDetection.STATUS_BINME) {
				pamDataUnits.remove(clickTrain);
			}
			else {
				i++;
			}
		}
		return unitsRemoved;
	}


//	@Override
//	public t getNextUnit(PamDataUnit pamDataUnit) {
//		/*
//		 * normal version of this goes wrong since units have nee removed - so 
//		 * need to dive right in and find the unit in the list
//		 */
//		int unitIndex = pamDataUnits.indexOf(pamDataUnit);
//		if (++unitIndex < pamDataUnits.size()) {
//			return pamDataUnits.get(unitIndex);
//		}
//		return null;
//	}
	
//	@Override
//	public int getUnitsCountFromTime(long countStart) {
//		// modify to discount starting or detetable units.
//		int firstIndex = getIndexOfFirstUnitAfter(countStart);
//		if (firstIndex < 0) return 0;
//		int count = 0;
//		GroupDetection clickTrain;
//		for (int i = firstIndex; i < getUnitsCount(); i++) {
//			clickTrain = getDataUnit(i, PamDataBlock.REFERENCE_CURRENT);
//			if (clickTrain.getStatus() == ClickTrainDetection.STATUS_OPEN ||
//					clickTrain.getStatus() == ClickTrainDetection.STATUS_CLOSED) {
//				count++;
//			}
//		}
//		return count;
//		
//	}


	@Override
	public boolean canDraw(GeneralProjector projectorInfo) {
		return super.canDraw(projectorInfo);
	}

}
