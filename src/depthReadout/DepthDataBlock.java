package depthReadout;

import PamguardMVC.PamDataBlock;

public class DepthDataBlock extends PamDataBlock<DepthDataUnit> {
	
	DepthProcess depthProcess;
	
	public DepthDataBlock(String dataName, DepthProcess depthProcess) {
		super(DepthDataUnit.class, dataName, depthProcess, 0);
		this.depthProcess = depthProcess;
	}

}
