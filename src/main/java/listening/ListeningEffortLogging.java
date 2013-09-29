package listening;

import java.sql.Types;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class ListeningEffortLogging extends SQLLogging {

	PamTableDefinition tableDef;
	ListeningControl listeningControl;
	PamTableItem effortStatus, channels;
	
	public ListeningEffortLogging(ListeningControl listeningControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.listeningControl = listeningControl;
		setCanView(true);
		tableDef = new PamTableDefinition(pamDataBlock.getDataName(), SQLLogging.UPDATE_POLICY_WRITENEW);
		tableDef.addTableItem(effortStatus = new PamTableItem("Status",Types.CHAR,50));
		tableDef.addTableItem(channels = new PamTableItem("Channels",Types.INTEGER));
		tableDef.setUseCheatIndexing(true);

		setTableDefinition(tableDef);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {	
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		ListeningEffortData effortData = (ListeningEffortData) pamDataUnit;
		effortStatus.setValue(effortData.getStatus());
		channels.setValue(effortData.getChannelBitmap());

	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {

//		Timestamp ts = (Timestamp) getTableDefinition().getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);

		String stat = effortStatus.getDeblankedStringValue();
		int chan = (Integer) channels.getValue();
		
		ListeningEffortData effortData = new ListeningEffortData(timeMilliseconds,stat,chan);
		effortData.setDatabaseIndex(databaseIndex);
				
		getPamDataBlock().addPamData(effortData);
		
		return effortData;
	}


}
