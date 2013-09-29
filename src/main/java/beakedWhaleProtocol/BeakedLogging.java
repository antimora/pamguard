package beakedWhaleProtocol;

import java.sql.Types;

import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class BeakedLogging extends SQLLogging {

	private BeakedControl beakedControl;
	private BeakedProcess beakedProcess;
	
	private PamTableDefinition tableDefinition;
	
	PamTableItem latitude, longitude, source, comment;
	
	public static final int SOURCE_LENGTH = 50;
	public static final int COMMENT_LENGTH = 128;
	
	public BeakedLogging(BeakedControl beakedControl, BeakedProcess beakedProcess) {
		super(beakedProcess.beakedDataBlock);

		setCanView(true);
		
		this.beakedControl = beakedControl;
		this.beakedProcess = beakedProcess;
		tableDefinition = new PamTableDefinition(beakedProcess.beakedDataBlock.getDataName(),SQLLogging.UPDATE_POLICY_WRITENEW);
		tableDefinition.addTableItem(latitude = new PamTableItem("Latitude", Types.DOUBLE));
		tableDefinition.addTableItem(longitude = new PamTableItem("Longitude", Types.DOUBLE));
		tableDefinition.addTableItem(source = new PamTableItem("Location source", Types.CHAR, SOURCE_LENGTH));
		tableDefinition.addTableItem(comment = new PamTableItem("Comment", Types.CHAR, COMMENT_LENGTH));
		tableDefinition.setUseCheatIndexing(true);

		setTableDefinition(tableDefinition);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDefinition;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		BeakedDataUnit bdu = (BeakedDataUnit) pamDataUnit;
		
		LatLong ll = bdu.getBeakedLatLong();
		latitude.setValue(ll.getLatitude());
		longitude.setValue(ll.getLongitude());
		source.setValue(bdu.getLocationName());
		comment.setValue(bdu.getBeakedComment());

	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {

//		Timestamp ts = (Timestamp) tableDefinition.getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);
		
		BeakedDataUnit bdu = new BeakedDataUnit(timeMilliseconds,null);
		bdu.setDatabaseIndex(databaseIndex);
		BeakedLocationData bld = new BeakedLocationData();
		bdu.setBeakedLocationData(bld);
		double lat, longi;
		lat = (Double) latitude.getValue();
		longi = (Double) longitude.getValue();
		bld.latLong = new LatLong(lat, longi);
		bld.locationSource = BeakedLocationData.interpretSource((String) source.getValue());
		bld.comment = (String) comment.getValue();
		
		beakedProcess.beakedDataBlock.addPamData(bdu);
		return bdu;
		
	}

}
