package IshmaelDetector;

import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import java.sql.Types;


import PamDetection.IshDetection;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

//import pamDatabase.SQLLogging;
//import PamguardMVC.RecyclingDataBlock;
import generalDatabase.PamDetectionLogging;

public class IshLogger extends PamDetectionLogging {
	IshDetControl ishDetControl;
	PamTableDefinition tableDefinition;
	PamTableItem systemDate, durationSecs, secSinceStart, peakHeight;
//	PamTableItem peakSample;
	
	public IshLogger(IshDetControl ishDetControl, PamDataBlock pamDataBlock) 
	{
		super(pamDataBlock, UPDATE_POLICY_WRITENEW);
		this.ishDetControl = ishDetControl;
		
		tableDefinition = getTableDefinition();
		
//		PamTableItem tableItem;
//		setUpdatePolicy(UPDATE_POLICY_WRITENEW);
//		tableDefinition = new PamTableDefinition(ishDetControl.getUnitName(), getUpdatePolicy());
////		tableDefinition.addTableItem(tableItem = new PamTableItem("GpsIndex", Types.INTEGER));
////		tableItem.setCrossReferenceItem("GpsData", "Id");
//		tableDefinition.addTableItem(systemDate    = new PamTableItem("SystemDate",    Types.TIMESTAMP));
//		tableDefinition.addTableItem(peakSample = new PamTableItem("Peak Sample", Types.INTEGER));
		tableDefinition.addTableItem(peakHeight = new PamTableItem("PeakHeight", Types.DOUBLE));
		tableDefinition.addTableItem(secSinceStart = new PamTableItem("SecSinceStart", Types.DOUBLE));
		tableDefinition.addTableItem(durationSecs      = new PamTableItem("DurationSeconds",      Types.DOUBLE));
//		setTableDefinition(tableDefinition);
	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		super.setTableData(pamDataUnit);

		IshDetection detUnit = (IshDetection)pamDataUnit;

		long dur = detUnit.getDuration();					//in det samples (e.g., slices)
		float dRate = ishDetControl.ishDetFnProcess.getDetSampleRate();
		peakHeight.setValue(detUnit.getPeakHeight());
		durationSecs.setValue((double)dur / dRate);
		secSinceStart.setValue((double)detUnit.getStartSample() / ishDetControl.ishDetFnProcess.getSampleRate());

	}

	/* (non-Javadoc)
	 * @see generalDatabase.SQLLogging#createDataUnit(long, int)
	 */
	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds,
			int databaseIndex) {
		long duration = getDuration().getIntegerValue();
		double durationS = durationSecs.getDoubleValue();
		long endMillis = timeMilliseconds + (long) (durationS*1000);
		int chanMap = getChannelMap().getIntegerValue(); 
		long startSam = getStartSample().getIntegerValue();
		long durationSam = getDuration().getIntegerValue();
		double pHeight = peakHeight.getDoubleValue();
		IshDetection id = new IshDetection(timeMilliseconds, endMillis, (float)getLowFreq().getDoubleValue(), 
				(float)getHighFreq().getDoubleValue(), 0, pHeight, getPamDataBlock(), chanMap, startSam, durationSam);
		id.setDatabaseIndex(databaseIndex);
		getPamDataBlock().addPamData(id);
		return id;
	}

}
