package beakedWhaleProtocol;

import PamguardMVC.PamDataBlock;

public class BeakedDataBlock extends PamDataBlock<BeakedDataUnit> {
	
	private BeakedProcess beakedProcess;
	
	private BeakedControl beakedControl;

	public BeakedDataBlock(String dataName, BeakedProcess beakedProcess) {
		super(BeakedDataUnit.class, dataName, beakedProcess, 0);
		this.beakedControl = beakedControl;
		this.beakedProcess = beakedProcess;
	}

}
