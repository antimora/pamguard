package beakedWhaleProtocol;

import java.io.Serializable;

import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;

public class BeakedExperimentData extends PamDataUnit implements Serializable, Cloneable {

	public BeakedExperimentData(long timeMilliseconds) {
		super(timeMilliseconds);
	}

	public static final long serialVersionUID = 0;
	
	public static final int APPROACH_START = 0;
	public static final int ON_TRACK = 1;
	public static final int COMPLETE = 2;
	public static final int ABORTED = 3;
	public static final int AUTOCOMPLETE = 4;
	
	LatLong vesselStart;
	
	LatLong trackStart;
	
	LatLong trackEnd;
	
	LatLong alternateEnd;
	
	double alternateCourse;
	
	double perpDistance;
	
	double totalTrackLength;
	
	int status;
	
	double course;
	
	String comment;
	
	long endTime;
	
	BeakedDataUnit experimentLocationData;
	
	public void swapSides() {
		double dumCourse = course;
		LatLong dumLL = trackEnd;
		course = alternateCourse;
		trackEnd = alternateEnd;
		alternateCourse = dumCourse;
		alternateEnd = dumLL;
	}
	
	public String getStatusString() {
		switch (status) {
		case APPROACH_START:
			return "Approacing start point";
		case ON_TRACK:
			return "On track";
		case COMPLETE:
			return "Experiment complete";
		case ABORTED:
			return "Experiment aborted";
		case AUTOCOMPLETE:
			return "Experiment completed automatically";
		default:
			return "Unknown status";
		}
	}
	
}
