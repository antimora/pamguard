package staticLocaliser;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;

public class StaticLocaliserDataBlock extends PamDataBlock<StaticLocalisationResults> {

	public StaticLocaliserDataBlock(Class unitClass, String dataName,
			PamProcess parentProcess, int channelMap) {
		super(StaticLocalisationResults.class, dataName, parentProcess, channelMap);
		
	}


	@Override
	public boolean shouldNotify() {
		return true;
	}


	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#addPamData(PamguardMVC.PamDataUnit)
	 */
	@Override
	public void addPamData(StaticLocalisationResults pamDataUnit) {
		super.addPamData(pamDataUnit);
	}


	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#getShouldLog(PamguardMVC.PamDataUnit)
	 */
	@Override
	public boolean getShouldLog(PamDataUnit pamDataUnit) {
		return true;
	}

}
