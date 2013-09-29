package clipgenerator;

import PamguardMVC.PamDataBlock;

public class ClipDataBlock extends PamDataBlock<ClipDataUnit> {
	
	protected ClipProcess clipProcess;

	public ClipDataBlock(String dataName,
			ClipProcess clipProcess) {
		super(ClipDataUnit.class, dataName, clipProcess, 0);
		this.clipProcess = clipProcess;
	}

}
