package Acquisition;

import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

import java.sql.Types;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Very simple concrete implementation of SQLLogging to log the starts 
 * and stops of PAMGUARD in the database.
 * @author Doug Gillespie
 *
 */
public class AcquisitionLogging extends SQLLogging {

	AcquisitionControl acquisitionControl;
	
	PamTableDefinition tableDef;
	
	PamTableItem status, reason, daqSystemType, sampleRate, nChannels, voltsPeak2Peak, 
	duration, clockError;
	
	public AcquisitionLogging(PamDataBlock pamDataBlock, AcquisitionControl acquisitionControl) {
		super(pamDataBlock);
		this.acquisitionControl = acquisitionControl;
		
		tableDef = new PamTableDefinition(pamDataBlock.getDataName(), getUpdatePolicy());
		tableDef.addTableItem(status = new PamTableItem("Status", Types.CHAR, 20));
		tableDef.addTableItem(reason = new PamTableItem("Reason", Types.CHAR, 50));
		tableDef.addTableItem(daqSystemType = new PamTableItem("SystemType", Types.CHAR, 50));
		tableDef.addTableItem(sampleRate = new PamTableItem("sampleRate", Types.INTEGER));
		tableDef.addTableItem(nChannels = new PamTableItem("nChannels", Types.INTEGER));
		tableDef.addTableItem(voltsPeak2Peak = new PamTableItem("voltsPeak2Peak", Types.DOUBLE));
		tableDef.addTableItem(duration = new PamTableItem("duration", Types.DOUBLE));
		tableDef.addTableItem(clockError = new PamTableItem("clockError", Types.DOUBLE));
		
		setTableDefinition(tableDef);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		DaqStatusDataUnit ds = (DaqStatusDataUnit) pamDataUnit;
		status.setValue(ds.getStatus());
		reason.setValue(ds.getReason());
		daqSystemType.setValue(ds.getDaqSystemType());
		sampleRate.setValue(ds.getSampleRate());
		nChannels.setValue(ds.getNChannels());
		voltsPeak2Peak.setValue(ds.getVoltsPeak2Peak());
		duration.setValue(ds.getDuration());
		clockError.setValue(ds.clockError);
	}

}
