package generalDatabase;

import java.sql.Timestamp;
import java.sql.Types;

import PamDetection.AbstractLocalisation;
import PamDetection.PamDetection;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Standard logging class for any PamDetection, but you'll want to extend it for any 
 * additional information your detector outputs. 
 * <p>
 * Handles data from all fields in the PamDetection class.
 * <p>
 * Handles data from expected fields in associated localisation data.
 * <p>
 * N.B. Not all databases support huge (64 bit) integers, so the sample number is
 * written as a 32 bit integer, but the detection time is also written as a double
 * number of seconds from the start of the run, so any overflow should show up clearly
 * (and seconds are much easier to deal with in any offline analysis of data from the database).
 * 
 * @author Doug Gillespie
 *
 */
public class PamDetectionLogging extends SQLLogging {

	PamTableDefinition tableDefinition;

	// basic items available in any PamDetection.
	PamTableItem channelMap, startSample, startSeconds, duration, lowFreq, highFreq, amplitude, detectionType;
	// some extra items if Localisation data are available.
	PamTableItem bearingAmbiguity;
	PamTableItem[] bearing, range, depth, bearingError, rangeError, depthError;
	PamTableItem[] latitude, longitude;
	PamTableItem[] parallelError, perpError, referenceAngle;

	int localisationFlags;
	
	int nSides;

	public PamDetectionLogging(PamDataBlock pamDataBlock, int updatePolicy) {

		super(pamDataBlock);

		setUpdatePolicy(updatePolicy);

		makeStandardTableDefinition();

	}

	/**
	 * Make a standard table for detection and localisation data. 
	 *
	 */
	protected void makeStandardTableDefinition() {
		tableDefinition = new PamTableDefinition(this.getPamDataBlock().getLoggingName(), getUpdatePolicy());
		// add all the standard information fields...
		tableDefinition.addTableItem(channelMap = new PamTableItem("channelMap", Types.INTEGER));
		tableDefinition.addTableItem(startSample = new PamTableItem("startSample", Types.INTEGER));
		tableDefinition.addTableItem(startSeconds = new PamTableItem("startSeconds", Types.DOUBLE));
		tableDefinition.addTableItem(duration = new PamTableItem("duration", Types.INTEGER));
		tableDefinition.addTableItem(lowFreq = new PamTableItem("lowFreq", Types.DOUBLE));
		tableDefinition.addTableItem(highFreq = new PamTableItem("highFreq", Types.DOUBLE));
		tableDefinition.addTableItem(amplitude = new PamTableItem("amplitude", Types.DOUBLE));
		tableDefinition.addTableItem(detectionType = new PamTableItem("detectionType", Types.CHAR, 20));
		// add the localisation information only if it exists (or may exist).
		localisationFlags = getPamDataBlock().getLocalisationContents();
		tableDefinition.setUseCheatIndexing(true);

		nSides = 1;
		if ((localisationFlags & AbstractLocalisation.HAS_AMBIGUITY) != 0) {
			nSides = 2;
		}
		
		bearing = new PamTableItem[nSides];
		range = new PamTableItem[nSides];
		depth = new PamTableItem[nSides];
		bearingError = new PamTableItem[nSides];
		rangeError = new PamTableItem[nSides];
		depthError = new PamTableItem[nSides];
		latitude = new PamTableItem[nSides];
		longitude = new PamTableItem[nSides];
		parallelError = new PamTableItem[nSides];
		perpError = new PamTableItem[nSides];
		referenceAngle = new PamTableItem[nSides];

		String suffix = "";
		tableDefinition.addTableItem(bearingAmbiguity = new PamTableItem("bearingAmbiguity", Types.BIT));
		for (int i = 0; i < nSides; i++) {
			suffix = String.format("%d", i);
			if ((localisationFlags & AbstractLocalisation.HAS_BEARING) != 0) {
				tableDefinition.addTableItem(bearing[i] = new PamTableItem("bearing"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_RANGE) != 0) {
				tableDefinition.addTableItem(range[i] = new PamTableItem("range"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_DEPTH) != 0) {
				tableDefinition.addTableItem(depth[i] = new PamTableItem("depth"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_BEARINGERROR) != 0) {
				tableDefinition.addTableItem(bearingError[i] = new PamTableItem("bearingError"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_RANGEERROR) != 0) {
				tableDefinition.addTableItem(rangeError[i] = new PamTableItem("rangeError"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_DEPTH) != 0) {
				tableDefinition.addTableItem(depthError[i] = new PamTableItem("depthError"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_LATLONG) != 0) {
				tableDefinition.addTableItem(latitude[i] = new PamTableItem("Latitude"+suffix, Types.DOUBLE));
				tableDefinition.addTableItem(longitude[i] = new PamTableItem("Longitude"+suffix, Types.DOUBLE));
			}
			if ((localisationFlags & AbstractLocalisation.HAS_PERPENDICULARERRORS) != 0) {
				tableDefinition.addTableItem(parallelError[i] = new PamTableItem("ParallelError"+suffix, Types.DOUBLE));
				tableDefinition.addTableItem(perpError[i] = new PamTableItem("PerpendicularError"+suffix, Types.DOUBLE));
				tableDefinition.addTableItem(referenceAngle[i] = new PamTableItem("ErrorReferenceAngle"+suffix, Types.DOUBLE));
			}
		}

		setTableDefinition(tableDefinition);
	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		PamDetection pamDetection = (PamDetection) pamDataUnit;
		AbstractLocalisation abstractLocalisation = pamDetection.getLocalisation();

		channelMap.setValue(pamDetection.getChannelBitmap());
		startSample.setValue((int) pamDetection.getStartSample());
		startSeconds.setValue((double) pamDetection.getStartSample() / getPamDataBlock().getSampleRate());
		duration.setValue(pamDetection.getDuration());
		double[] frequency = pamDetection.getFrequency();
		if (frequency != null && frequency.length >= 2) {
			lowFreq.setValue(frequency[0]);
			highFreq.setValue(frequency[1]);
		}
		else {
			lowFreq.setValue(0);
			highFreq.setValue(0);
		}
		amplitude.setValue(pamDetection.getAmplitudeDB());
		detectionType.setValue(pamDetection.getDetectionType());

		int unitLocalisationFlags = 0;
		if (abstractLocalisation != null) {
			unitLocalisationFlags = abstractLocalisation.getLocContents();
		}
		else { // set everything to do with localisation to null and get out. 
			setNullData(bearingAmbiguity);
			setNullData(bearing);
			setNullData(range);
			setNullData(depth);
			setNullData(bearingError);
			setNullData(rangeError);
			setNullData(depthError);
			setNullData(longitude);
			setNullData(latitude);
			setNullData(parallelError);
			setNullData(perpError);
			setNullData(referenceAngle);
			return;
		}

		if (bearingAmbiguity != null && abstractLocalisation != null) {
			bearingAmbiguity.setValue(abstractLocalisation.bearingAmbiguity());
		}
		double[] angles = abstractLocalisation.getPlanarAngles();
		int nBearings = 0;
		if (angles != null) {
			nBearings = Math.min(angles.length, bearing.length);
		}
		if (bearing != null && (localisationFlags & AbstractLocalisation.HAS_BEARING) != 0) {
			for (int i = 0; i < nBearings; i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for bearing information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_BEARING) != 0) {
					bearing[i].setValue(angles[i]);
				}
				else {
					bearing[i].setValue(null);
					bearingAmbiguity.setValue(null);
				}
			}
		}

		if ((localisationFlags & AbstractLocalisation.HAS_RANGE) != 0) {
			for (int i = 0; i < range.length; i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for range information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_RANGE) != 0) {
					range[i].setValue(abstractLocalisation.getRange(i));
				}
				else {
					range[i].setValue(null);
				}
			}
		}

		if ((localisationFlags & AbstractLocalisation.HAS_DEPTH) != 0) {
			for (int i = 0; i < depth.length; i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for depth information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_DEPTH) != 0) {
					depth[i].setValue(abstractLocalisation.getDepth(i));
				}
				else {
					depth[i].setValue(null);
				}
			}
		}

		if ((localisationFlags & AbstractLocalisation.HAS_BEARINGERROR) != 0) {
			for (int i = 0; i < bearingError.length; i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for bearing error information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_BEARINGERROR) != 0) {
					bearingError[i].setValue(abstractLocalisation.getBearingError(i));
				}
				else {
					bearingError[i].setValue(null);
				}
			}
		}

		if ((localisationFlags & AbstractLocalisation.HAS_RANGEERROR) != 0) {
			for (int i = 0; i < rangeError.length; i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for range error information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_RANGEERROR) != 0) {
					rangeError[i].setValue(abstractLocalisation.getRangeError(i));
				}
				else {
					rangeError[i].setValue(null);
				}
			}
		}

		if ((localisationFlags & AbstractLocalisation.HAS_DEPTHERROR) != 0) {
			for (int i = 0; i < depthError.length; i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for depth error information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_DEPTHERROR) != 0) {
					depthError[i].setValue(abstractLocalisation.getDepthError(i));
				}
				else {
					depthError[i].setValue(null);
				}
			}
		}
		
		if ((localisationFlags & AbstractLocalisation.HAS_LATLONG) != 0) {
			LatLong ll;
			for (int i = 0; i < abstractLocalisation.getNumLatLong(); i++) {
				if (i >= nSides) {
					System.out.println("Incorrect database set up - no enought columns for LatLong information in table " 
							+ getTableDefinition().getTableName());
					continue;
				}
				ll = abstractLocalisation.getLatLong(i);
				if (ll == null) {
					continue;
				}
				if (latitude[i] == null) {
					System.out.println("Null latlong field " + i);
				}
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_LATLONG) != 0) {
					latitude[i].setValue(ll.getLatitude());
					longitude[i].setValue(ll.getLongitude());
				}
				else {
					latitude[i].setValue(null);
					longitude[i].setValue(null);
				}
			}
		}
		if ((localisationFlags & AbstractLocalisation.HAS_PERPENDICULARERRORS) != 0) {
			for (int i = 0; i < perpError.length; i++) {
				if ((unitLocalisationFlags & AbstractLocalisation.HAS_PERPENDICULARERRORS) != 0) {
					perpError[i].setValue(abstractLocalisation.getPerpendiculaError(i));
					parallelError[i].setValue(abstractLocalisation.getParallelError(i));
					referenceAngle[i].setValue(abstractLocalisation.getErrorDirection(i));
				}
				else {
					perpError[i].setValue(null);
					parallelError[i].setValue(null);
					referenceAngle[i].setValue(null);
				}
			}
		}

	}
	private void setNullData(PamTableItem tableItem) {
		if (tableItem == null) return;
		tableItem.setValue(null);
	}
	private void setNullData (PamTableItem[] tableItems) {
		if (tableItems == null) {
			return;
		}
		for (int i = 0; i < tableItems.length; i++) {
			setNullData(tableItems[i]);
		}
	}

	/**
	 * PamDetection is nearly always overridden (Not sure why it's not declared abstract)
	 * so it's quite difficult for createDataUnit to fill and do anything with these in the 
	 * general class. Therefore, assume that createDataUnit will be overridden in more concrete classes
	 * and just provide a function here to fill the data in to a newDataUnit from standard database columns
	 */
	protected boolean fillDataUnit(PamDetection pamDetection) {
		pamDetection.setChannelBitmap((Integer) channelMap.getValue()); 
		pamDetection.setDatabaseIndex(tableDefinition.getIndexItem().getIntegerValue());
		pamDetection.setDuration((Integer) duration.getValue());
		pamDetection.setDatabaseUpdateOf(tableDefinition.getUpdateReference().getIntegerValue());
		double freq[] = new double[2];
		freq[0] = (Double) lowFreq.getValue();
		freq[1] = (Double) highFreq.getValue();
		pamDetection.setFrequency(freq);
		pamDetection.setStartSample((Integer) startSample.getValue());
		pamDetection.setMeasuredAmplitude((Double) amplitude.getValue());		
		Timestamp ts = (Timestamp) tableDefinition.getTimeStampItem().getValue();
		long t = PamCalendar.millisFromTimeStamp(ts);
		pamDetection.setTimeMilliseconds(t);
		return true; 
	}

	public PamTableItem getAmplitude() {
		return amplitude;
	}

	public PamTableItem[] getBearing() {
		return bearing;
	}

	public PamTableItem getBearingAmbiguity() {
		return bearingAmbiguity;
	}

	public PamTableItem[] getBearingError() {
		return bearingError;
	}

	public PamTableItem getChannelMap() {
		return channelMap;
	}

	public PamTableItem[] getDepth() {
		return depth;
	}

	public PamTableItem[] getDepthError() {
		return depthError;
	}

	public PamTableItem getDetectionType() {
		return detectionType;
	}

	public PamTableItem getDuration() {
		return duration;
	}

	public PamTableItem getHighFreq() {
		return highFreq;
	}

	public PamTableItem[] getLatitude() {
		return latitude;
	}

	public int getLocalisationFlags() {
		return localisationFlags;
	}

	public PamTableItem[] getLongitude() {
		return longitude;
	}

	public PamTableItem getLowFreq() {
		return lowFreq;
	}

	public int getNSides() {
		return nSides;
	}

	public PamTableItem[] getParallelError() {
		return parallelError;
	}

	public PamTableItem[] getPerpError() {
		return perpError;
	}

	public PamTableItem[] getRange() {
		return range;
	}

	public PamTableItem[] getRangeError() {
		return rangeError;
	}

	public PamTableItem[] getReferenceAngle() {
		return referenceAngle;
	}

	public PamTableItem getStartSample() {
		return startSample;
	}

	public PamTableItem getStartSeconds() {
		return startSeconds;
	}
	
//	@Override
//	protected boolean createDataUnit() {
//		// rare fot this to get used. 
//		return super.createDataUnit();
//	}
}
