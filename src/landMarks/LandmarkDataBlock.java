package landMarks;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class LandmarkDataBlock extends PamDataBlock<LandmarkDataUnit> {

	LandmarkControl landmarkControl;
	
	public LandmarkDataBlock(String dataName, LandmarkControl landmarkControl, PamProcess parentProcess) {
		super(LandmarkDataUnit.class, dataName, parentProcess, 0);
		setOverlayDraw(new LandmarkGraphics(landmarkControl));
	}

	@Override
	protected int removeOldUnitsT(long currentTimeMS) {
//		return super.removeOldUnitsT(currentTimeMS);
		return 0; // don't do anything !
	}
	
	protected void createDataUnits(LandmarkDatas landmarkDatas) {
		clearAll();
		long now = PamCalendar.getTimeInMillis();
		for (int i = 0; i < landmarkDatas.size(); i++) {
			addPamData(new LandmarkDataUnit(now, landmarkDatas.get(i)));
		}
	}

}
