package networkTransfer.receive;

import java.util.ListIterator;

import GPS.GpsDataUnit;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class BuoyStatusDataBlock extends PamDataBlock<BuoyStatusDataUnit> {

	public BuoyStatusDataBlock(PamProcess parentProcess) {
		super(BuoyStatusDataUnit.class, "Buoy Status Data", parentProcess, 0);
	}


	@Override
	protected synchronized int removeOldUnitsS(long mastrClockSample) {
		return 0; // units are never removed. 
	}


	@Override
	protected synchronized int removeOldUnitsT(long currentTimeMS) {
		return 0; // units are never removed. 
	}


	@Override
	public synchronized void clearAll() {
		// never delete !
	}

//	@Override
//	public synchronized void addPamData(BuoyStatusDataUnit pamDataUnit) {
//		GpsDataUnit du = findDataUnit(pamDataUnit.getChannelBitmap());
////		if (du == null) {
////			super.addPamData(pamDataUnit);
////		}
////		else if (pamDataUnit != null){
////			du.setGpsData(pamDataUnit.getGpsData());
////		}
//	}
	
	public synchronized BuoyStatusDataUnit findDataUnit(int channelMap) {
		ListIterator<BuoyStatusDataUnit> li = getListIterator(0);
		BuoyStatusDataUnit du;
		while (li.hasNext()) {
			du = li.next();
			if ((du.getChannelBitmap() & channelMap) != 0) {
				return du;
			}
		}
		return null;
	}
	
	public BuoyStatusDataUnit findBuoyStatusData(int buoyId1, int buoyId2) {
		ListIterator<BuoyStatusDataUnit> li = getListIterator(0);
		BuoyStatusDataUnit du;
		while (li.hasNext()) {
			du = li.next();
			if (du.getBuoyId1() == buoyId1 && du.getBuoyId2() == buoyId2) {
				return du;
			}
		}
		return null;
	}
}
