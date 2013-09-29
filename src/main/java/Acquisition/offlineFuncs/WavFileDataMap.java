package Acquisition.offlineFuncs;

import PamController.OfflineDataStore;
import PamguardMVC.PamDataBlock;
import dataMap.OfflineDataMap;

public class WavFileDataMap extends OfflineDataMap<WavFileDataMapPoint> {

	public WavFileDataMap(OfflineDataStore offlineDataStore,
			PamDataBlock parentDataBlock) {
		super(offlineDataStore, parentDataBlock);
		
	}

}
