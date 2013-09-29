package EdgeDetector;

import PamController.PamControlledUnit;
import PamDetection.PamDetection;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class EdgeProcess extends PamProcess {

	PamDataBlock edgeDataBlock;
	
	public EdgeProcess(PamControlledUnit pamControlledUnit) {
		super(pamControlledUnit, null);
		edgeDataBlock = new PamDataBlock<PamDetection>(PamDetection.class, "Edge Data", this, 0);
		addOutputDataBlock(edgeDataBlock);
	}

	@Override
	public void pamStart() {
//		int x = 1 / (1-1);
//		Point p = null;
//		p.x = 5;
	}

	@Override
	public void pamStop() {
		
	}

}
