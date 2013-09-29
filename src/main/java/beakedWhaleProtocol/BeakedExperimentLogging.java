package beakedWhaleProtocol;

import java.sql.Timestamp;
import java.sql.Types;

import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class BeakedExperimentLogging extends SQLLogging {

	PamTableDefinition tableDef;
	PamTableItem startLat, startLong, endLat, endLong, length, course, perpDistance, comment;
	PamTableItem endTime, locationReference;
	
	public static final int COMMENT_LENGTH = 128;
	
	BeakedExperimentDataBlock beakedExperimentDataBlock;
	
	public BeakedExperimentLogging(BeakedExperimentDataBlock beakedExperimentDataBlock) {
		super(beakedExperimentDataBlock);
		this.beakedExperimentDataBlock = beakedExperimentDataBlock;

		setCanView(true);

		tableDef = new PamTableDefinition(beakedExperimentDataBlock.getDataName(), SQLLogging.UPDATE_POLICY_WRITENEW);
		tableDef.addTableItem(endTime = new PamTableItem("End Time", Types.TIMESTAMP));
		tableDef.addTableItem(locationReference = new PamTableItem("Location Reference", Types.INTEGER));
		tableDef.addTableItem(startLat = new PamTableItem("Start Latitude", Types.DOUBLE));
		tableDef.addTableItem(startLong = new PamTableItem("Start Longitude", Types.DOUBLE));
		tableDef.addTableItem(endLat = new PamTableItem("End Latitude", Types.DOUBLE));
		tableDef.addTableItem(endLong = new PamTableItem("End Longitude", Types.DOUBLE));
		tableDef.addTableItem(length = new PamTableItem("Length", Types.DOUBLE));
		tableDef.addTableItem(course = new PamTableItem("Perpendicular Distance", Types.DOUBLE));
		tableDef.addTableItem(perpDistance = new PamTableItem("Course", Types.DOUBLE));
		tableDef.addTableItem(comment = new PamTableItem("Comment", Types.CHAR, COMMENT_LENGTH));
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
		
		BeakedExperimentData bed = (BeakedExperimentData) pamDataUnit;
		
		endTime.setValue(PamCalendar.getTimeStamp(bed.endTime));
		locationReference.setValue(bed.experimentLocationData.getDatabaseIndex());
		startLat.setValue(bed.trackStart.getLatitude());
		startLong.setValue(bed.trackStart.getLongitude());
		endLat.setValue(bed.trackEnd.getLatitude());
		endLong.setValue(bed.trackEnd.getLongitude());
		length.setValue(bed.totalTrackLength);
		course.setValue(bed.course);
		perpDistance.setValue(bed.perpDistance);
		comment.setValue(bed.comment);
		
	}

	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds, int databaseIndex) {

		Timestamp ts;
		long t;
//		Timestamp ts = (Timestamp) tableDef.getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);
		
		BeakedExperimentData bed = new BeakedExperimentData(timeMilliseconds);
		bed.setDatabaseIndex(databaseIndex);
		ts = (Timestamp) endTime.getValue();
		t = PamCalendar.millisFromTimeStamp(ts);
		bed.endTime = t;
		
		double lat, longi;
		lat = (Double) startLat.getValue();
		longi = (Double) startLong.getValue();	
		bed.trackStart = new LatLong(lat, longi);
		lat = (Double) endLat.getValue();
		longi = (Double) endLong.getValue();	
		bed.trackEnd = new LatLong(lat, longi);
		
		bed.totalTrackLength = (Double) length.getValue();
		bed.course = (Double) course.getValue();
		bed.perpDistance = (Double) perpDistance.getValue();
		bed.comment = (String) comment.getValue();
		
		beakedExperimentDataBlock.addPamData(bed);
		
		return bed;
		
	}

}
