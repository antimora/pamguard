package RightWhaleEdgeDetector;

import PamguardMVC.PamProcess;
import whistlesAndMoans.AbstractWhistleDataBlock;

public class RWEDataBlock extends AbstractWhistleDataBlock {

	public RWEDataBlock(String dataName,
			PamProcess parentProcess, int channelMap) {
		super(RWEDataUnit.class, dataName, parentProcess, channelMap);
		// TODO Auto-generated constructor stub
	}

}
