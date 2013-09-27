package networkTransfer.receive;

import java.sql.Types;

import generalDatabase.PamTableItem;
import GPS.GpsDataUnit;
import GPS.GpsLogger;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class NetGPSLogging extends GpsLogger {

	PamTableItem buoyId1, buoyId2, channel;
	
	NetworkReceiver networkReceiver;
	
	
	public NetGPSLogging(NetworkReceiver networkReceiver, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.networkReceiver = networkReceiver;
		getTableDefinition().setTableName("BuoyGPSData");
		getTableDefinition().addTableItem(buoyId1 = new PamTableItem("BuoyId1", Types.INTEGER));
		getTableDefinition().addTableItem(buoyId2 = new PamTableItem("BuoyId2", Types.INTEGER));
		getTableDefinition().addTableItem(channel = new PamTableItem("Channel", Types.INTEGER));
	}
	
	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		super.setTableData(pamDataUnit);
		int chan = PamUtils.getSingleChannel(pamDataUnit.getChannelBitmap());
		BuoyStatusDataUnit stats = (BuoyStatusDataUnit) pamDataUnit;
		if (stats != null) {
			buoyId1.setValue(new Integer(stats.getBuoyId1()));
			buoyId2.setValue(new Integer(stats.getBuoyId2()));
		}
		else {
			buoyId1.setValue(null);
			buoyId2.setValue(null);
		}
		channel.setValue(new Integer(chan));
	}

	@Override
	protected PamDataUnit createDataUnit(long dataTime, int iD) {
		GpsDataUnit gpsDataUnit = (GpsDataUnit) super.createDataUnit(dataTime, iD);
		int chan = channel.getIntegerValue();
		int id1 = buoyId1.getIntegerValue();
		int id2 = buoyId2.getIntegerValue();
		BuoyStatusDataUnit stats = networkReceiver.findBuoyStatusDataUnit(id1, id2, true);
		gpsDataUnit.setChannelBitmap(1<<chan);
		stats.setGpsData(dataTime, gpsDataUnit.getGpsData());
		return gpsDataUnit;
	}
	

}
