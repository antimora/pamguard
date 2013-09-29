package xBatLogViewer;

import PamguardMVC.AcousticDataBlock;
import PamguardMVC.PamProcess;

public class XBatLogDataBlock extends AcousticDataBlock<XBatLogDataUnit> {

	public XBatLogDataBlock(String dataName,
			PamProcess parentProcess, int channelMap) {
		super(XBatLogDataUnit.class, dataName, parentProcess, channelMap);
	}

}
