package whistlesAndMoans;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamDetectionLogging;

public class WhistleToneLogging extends PamDetectionLogging {

	public WhistleToneLogging(WhistleMoanControl whistleMoanControl, PamDataBlock pamDataBlock, int updatePolicy) {
		super(pamDataBlock, updatePolicy);
		getTableDefinition().setTableName(whistleMoanControl.getUnitName());
		getTableDefinition().setUseCheatIndexing(true);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see generalDatabase.PamDetectionLogging#setTableData(PamguardMVC.PamDataUnit)
	 */
	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub
		super.setTableData(pamDataUnit);
	}

}
