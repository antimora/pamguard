package Map;

import java.sql.Types;

import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;

public class MapCommentSQLLogging extends SQLLogging {

	PamTableDefinition tableDef;
		
	PamTableItem latitude, longitude, comment;

	private MapCommentDataBlock mapCommentDataBlock;
	
	public MapCommentSQLLogging(MapCommentDataBlock pamDataBlock) {
		super(pamDataBlock);
		mapCommentDataBlock = pamDataBlock;
		setUpdatePolicy(SQLLogging.UPDATE_POLICY_WRITENEW);

		setCanView(true);
		
		tableDef = new PamTableDefinition(pamDataBlock.getParentProcess().getProcessName() + "_Comments", getUpdatePolicy());
		tableDef.addTableItem(latitude = new PamTableItem("Latitude", Types.DOUBLE));
		tableDef.addTableItem(longitude = new PamTableItem("Longitude", Types.DOUBLE));
		tableDef.addTableItem(comment = new PamTableItem("Comment", Types.CHAR, MapComment.MAXCOMMENTLENGTH));
		tableDef.setUseCheatIndexing(true);

		setTableDefinition(tableDef);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {
//		return tableDef;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		
		MapComment mapComment = (MapComment) pamDataUnit;
		
		latitude.setValue(mapComment.latLong.getLatitude());
		longitude.setValue(mapComment.latLong.getLongitude());
		comment.setValue(mapComment.comment);

	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {

//		Timestamp ts = (Timestamp) getTableDefinition().getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);
		double lat = (Double) latitude.getValue();
		double lon = (Double) longitude.getValue();
		String com = (String) comment.getValue();
		
		MapComment mapComment = new MapComment(timeMilliseconds, new LatLong(lat, lon), com);
		
		mapComment.setDatabaseIndex(databaseIndex);
		
		mapCommentDataBlock.addPamData(mapComment);
		
		return mapComment;
	}

}
