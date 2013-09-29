package clickDetector.offlineFuncs;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;

import clickDetector.ClickControl;
import PamController.PamViewParameters;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.SaveRequirements;
import generalDatabase.DBControlUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.lookupTables.LookUpTables;

public class OfflineEventLogging extends SQLLogging {
	
	private PamTableDefinition tableDef;
	
	static public final int COMMENT_LENGTH = 80;
	
	private ClickControl clickControl;
	
	private PamTableItem endTime, nClicks, minNumber, bestNumber, maxNumber, eventType, 
	comment, colourIndex, channels;
	

	public OfflineEventLogging(ClickControl clickControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.clickControl = clickControl;
		tableDef = new PamTableDefinition(clickControl.getUnitName()+"_OfflineEvents", SQLLogging.UPDATE_POLICY_OVERWRITE);
		tableDef.addTableItem(endTime = new PamTableItem("EventEnd", Types.TIMESTAMP));
		tableDef.addTableItem(eventType = new PamTableItem("eventType", Types.CHAR, LookUpTables.CODE_LENGTH));
		tableDef.addTableItem(nClicks = new PamTableItem("nClicks", Types.INTEGER));
		tableDef.addTableItem(minNumber = new PamTableItem("minNumber", Types.SMALLINT));
		tableDef.addTableItem(bestNumber = new PamTableItem("bestNumber", Types.SMALLINT));
		tableDef.addTableItem(maxNumber = new PamTableItem("maxNumber", Types.SMALLINT));
		tableDef.addTableItem(colourIndex = new PamTableItem("colour", Types.SMALLINT));
		tableDef.addTableItem(comment = new PamTableItem("comment", Types.CHAR, COMMENT_LENGTH));
		tableDef.addTableItem(channels = new PamTableItem("channels", Types.INTEGER));
		tableDef.setUseCheatIndexing(false);
		
		setTableDefinition(tableDef);
		
		setUpdatePolicy(UPDATE_POLICY_OVERWRITE);
	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		OfflineEventDataUnit oedu = (OfflineEventDataUnit) pamDataUnit;
		endTime.setValue(PamCalendar.getTimeStamp(oedu.getEventEndTime()));
		eventType.setValue(oedu.getEventType());
		nClicks.setValue(oedu.getNClicks());
		minNumber.setValue(oedu.getMinNumber());
		bestNumber.setValue(oedu.getBestNumber());
		maxNumber.setValue(oedu.getMaxNumber());
		colourIndex.setValue(oedu.getColourIndex());
		comment.setValue(oedu.getComment());
		channels.setValue(oedu.getChannelBitmap());		
	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {
		OfflineEventDataUnit dataUnit = new OfflineEventDataUnit(eventType.getStringValue(), 
				colourIndex.getIntegerValue(), null);
		dataUnit.setDatabaseIndex(tableDef.getIndexItem().getIntegerValue());
		dataUnit.setTimeMilliseconds(timeMilliseconds);	
		dataUnit.setDatabaseIndex(databaseIndex);
		Timestamp ts = (Timestamp) endTime.getValue();
		if (ts != null) {
			long endTime = PamCalendar.millisFromTimeStamp(ts);
			dataUnit.setEventEndTime(endTime);
		}
		dataUnit.setNClicks(nClicks.getIntegerValue());
		dataUnit.setMinNumber(minNumber.getShortValue());
		dataUnit.setBestNumber(bestNumber.getShortValue());
		dataUnit.setMaxNumber(maxNumber.getShortValue());
		dataUnit.setComment(comment.getStringValue());
		int chans = channels.getIntegerValue(); 
		if (chans == 0) {
			chans = 3;
		}
		dataUnit.setChannelBitmap(chans);
		getPamDataBlock().addPamData(dataUnit);
		return dataUnit;
	}

	@Override
	public String getViewerLoadClause(PamViewParameters pvp) {
		return "ORDER BY UTC, UTCMilliseconds";
	}

	@Override
	public boolean saveOfflineData(DBControlUnit dbControlUnit,
			Connection connection) {
		/**
		 * Needs to delete a load of clicks as well as 
		 * doing the normal delete of events. 
		 */
		SaveRequirements sr = getPamDataBlock().getSaveRequirements(dbControlUnit);
		if (sr.getNumDeletions() > 0) {
			deleteEventClicks(connection, sr.getDeleteIndexes());
		}
		return super.saveOfflineData(dbControlUnit, connection);
	}

	private void deleteEventClicks(Connection connection, int[] deleteIndexes) {
		/*
		 * First need to find the click logging class. 
		 */
		OfflineClickLogging clickLogging = clickControl.getClickDataBlock().getOfflineClickLogging();
		clickLogging.deleteEventClicks(connection, deleteIndexes);
	}

	
}
