package angleMeasurement;

import java.sql.Types;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class AngleLogging extends SQLLogging {

	private AngleControl angleControl;
	
	private PamTableDefinition tableDef;
	
	PamTableItem rawAngle, calibratedAngle, correctedAngle, held;
	
	public AngleLogging(AngleControl angleControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.angleControl = angleControl;
		
		tableDef = new PamTableDefinition(angleControl.getUnitType() + " " + angleControl.getUnitName(), UPDATE_POLICY_WRITENEW);
		tableDef.addTableItem(rawAngle = new PamTableItem("Raw Angle", Types.DOUBLE));
		tableDef.addTableItem(calibratedAngle = new PamTableItem("Calibrated Angle", Types.DOUBLE));
		tableDef.addTableItem(correctedAngle = new PamTableItem("Corrected Angle", Types.DOUBLE));
		tableDef.addTableItem(held = new PamTableItem("Held", Types.BIT));
		tableDef.setUseCheatIndexing(true);
		
		setTableDefinition(tableDef);
	}
//
//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		
		AngleDataUnit adu = (AngleDataUnit) pamDataUnit;
		rawAngle.setValue(adu.rawAngle);
		calibratedAngle.setValue(adu.calibratedAngle);
		correctedAngle.setValue(adu.correctedAngle);
		held.setValue(adu.held);

	}

}
