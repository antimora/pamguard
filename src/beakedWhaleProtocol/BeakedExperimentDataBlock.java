
package beakedWhaleProtocol;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class BeakedExperimentDataBlock extends PamDataBlock<BeakedExperimentData> {

	public BeakedExperimentDataBlock(String dataName, PamProcess parentProcess) {
		super(BeakedExperimentData.class, dataName, parentProcess, 0);
		setNaturalLifetime(10000000);
	}

}
