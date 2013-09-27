package beakedWhaleProtocol;

import java.sql.Types;

import PamUtils.LatLong;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class ShoreStationLogging extends SQLLogging {

	PamTableDefinition tableDef;
	PamTableItem latitude, longigude, height;
	
	public ShoreStationLogging(PamDataBlock pamDataBlock) {
		super(pamDataBlock);

		setCanView(true);
		
		tableDef = new PamTableDefinition(pamDataBlock.getDataName(), SQLLogging.UPDATE_POLICY_WRITENEW);
		tableDef.addTableItem(latitude = new PamTableItem("Latitude", Types.DOUBLE));
		tableDef.addTableItem(longigude = new PamTableItem("Longitude", Types.DOUBLE));
		tableDef.addTableItem(height = new PamTableItem("Height", Types.DOUBLE));

		setTableDefinition(tableDef);
	}
//
//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {


		ShoreStationDataUnit sdu = (ShoreStationDataUnit) pamDataUnit;
		latitude.setValue(sdu.getLatLong().getLatitude());
		longigude.setValue(sdu.getLatLong().getLongitude());
		height.setValue(sdu.getHeight());

	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {

//		Timestamp ts = (Timestamp) tableDef.getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);
		
		
		double lat, longi, h;
		try {
			lat = (Double) latitude.getValue();
			longi = (Double) longigude.getValue();	
			h = (Double) height.getValue();	
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
		ShoreStationDataUnit sdu = new ShoreStationDataUnit(timeMilliseconds, new LatLong(lat, longi), h);
		sdu.setDatabaseIndex(databaseIndex);
		
		getPamDataBlock().addPamData(sdu);
		
		return sdu;
	}
}
