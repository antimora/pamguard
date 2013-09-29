package clickDetector;

import PamguardMVC.AcousticDataBlock;
import PamguardMVC.PamProcess;

public class TrackedClickDataBlock extends AcousticDataBlock<ClickDetection> {

	private ClickControl clickControl;
		
	public TrackedClickDataBlock(ClickControl clickControl, PamProcess parentProcess, int channelMap) {
		super(ClickDetection.class, clickControl.getDataBlockPrefix() + "Tracked Clicks", parentProcess, channelMap);
		this.clickControl = clickControl;
	}
}
