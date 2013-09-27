package whistleDetector;

import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

import java.sql.Connection;
import java.sql.Types;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class WhistleEventLogger extends SQLLogging {

	WhistleEventDetector whistleEventDetector;
	
	Connection connection;
	
	PamTableDefinition tableDef;
	
	PamTableItem startTime, endTime, duration, nWhistles;
	
	public WhistleEventLogger(WhistleEventDetector whistleEventDetector, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.whistleEventDetector = whistleEventDetector;
		setUpdatePolicy(SQLLogging.UPDATE_POLICY_WRITENEW);
		tableDef = new PamTableDefinition(whistleEventDetector.whistleControl.getUnitName() + "_Events", getUpdatePolicy());
		tableDef.addTableItem(startTime = new PamTableItem("EventStart", Types.TIMESTAMP));
		tableDef.addTableItem(endTime = new PamTableItem("EventEnd", Types.TIMESTAMP));
		tableDef.addTableItem(duration = new PamTableItem("Duration", Types.DOUBLE));
		tableDef.addTableItem(nWhistles = new PamTableItem("nWhistles", Types.INTEGER));
		tableDef.setUseCheatIndexing(true);

		setTableDefinition(tableDef);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		EventDataUnit whistleEvent = (EventDataUnit) pamDataUnit;
		startTime.setValue(PamCalendar.getTimeStamp(whistleEvent.startTimeMillis));
		endTime.setValue(PamCalendar.getTimeStamp(whistleEvent.endTimeMillis));
		duration.setValue((whistleEvent.endTimeMillis - whistleEvent.startTimeMillis)/1000.);
		nWhistles.setValue(whistleEvent.getWhistleCount());

	}

	@Override
	public synchronized boolean logData(Connection con, PamDataUnit dataUnit) {
		connection = con;
		return true;
	}
	public synchronized boolean logData(PamDataUnit dataUnit) {
		return super.logData(connection, dataUnit);
	}

}
