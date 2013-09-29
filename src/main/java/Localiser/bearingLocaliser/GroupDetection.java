package Localiser.bearingLocaliser;

import java.util.Arrays;

import Array.ArrayManager;
import PamDetection.AbstractLocalisation;
import PamDetection.PamDetection;
import PamUtils.LatLong;

public class GroupDetection<t extends PamDetection> extends PamDetection<t, PamDetection> {
		
		private int status = STATUS_OPEN;
		
		private int eventId;
		
//		same as in ClickTRainDetection
//		static public final int STATUS_STARTING = 0;
		static public final int STATUS_OPEN = 1;
		static public final int STATUS_CLOSED = 2;
		
		static private final int NUMPOSITIONS = 2;
		
//		static private int usedPositions = 0;
		
		private LatLong latLong[] = new LatLong[NUMPOSITIONS];
		private LatLong originLatLong[] = new LatLong[NUMPOSITIONS];
		private double bearing[] = new double[NUMPOSITIONS];
		private double range[] = new double[NUMPOSITIONS];
		private double perpendicularError[] = new double[NUMPOSITIONS];
		private double parallelError[] = new double[NUMPOSITIONS];
		private double errorReferenceAngle[] = new double[NUMPOSITIONS];
		private double referenceHeading[] = new double[NUMPOSITIONS];
		private int referenceChannels[] = new int[NUMPOSITIONS];
		private int numLatLong = 0;
		
		//static public final int STATUS_BINME = 3;
		
		private GroupDetectionLocalisation groupDetectionLocalisation;


		public GroupDetection(t firstDetection) {
			
			super(firstDetection.getTimeMilliseconds(), firstDetection.getChannelBitmap(), 
					firstDetection.getStartSample(), firstDetection.getDuration());
			
			
			addSubDetection(firstDetection);

			if (firstDetection.getLocalisation() != null) {
				makeLocalisation(firstDetection.getLocalisation());
				groupDetectionLocalisation.setLocContents(firstDetection.getLocalisation().getLocContents());
				groupDetectionLocalisation.setSubArrayType(firstDetection.getLocalisation().getSubArrayType());
				double[] angles = firstDetection.getLocalisation().getPlanarAngles();
				for (int i = 0; i < angles.length; i++) {
					setBearing(i, angles[i]);
					setReferenceHeading(i, firstDetection.getLocalisation().getBearingReference());
				}
				if (angles.length > 1) {
					groupDetectionLocalisation.addLocContents(AbstractLocalisation.HAS_AMBIGUITY);
				}
			}
			
		}
		

		public GroupDetection(long timeMilliseconds, int channelBitmap, long startSample, long duration) {
			super(timeMilliseconds, channelBitmap, startSample, duration);
		}

		private void makeLocalisation(AbstractLocalisation firstLocalisation) {
			if (groupDetectionLocalisation == null) {
				groupDetectionLocalisation = new GroupDetectionLocalisation(this, 0, 0);
//				if (firstLocalisation != null) {
//					if (firstLocalisation.getSubArrayType() >= ArrayManager.ARRAY_TYPE_PLANE) {
//						setNumLocations(1);
//						groupDetectionLocalisation.removeLocContents(AbstractLocalisation.HAS_AMBIGUITY);
//					}
//				}
				/*
				 * Initially set the axis of that localisation to be the same as the 
				 * first sub detection which has localisation data ...
				 */
				synchronized (getSubDetectionSyncronisation()) {
					int n = getSubDetectionsCount();
					t subDetection;
					for (int i = 0; i < n; i++) {
						subDetection = getSubDetection(i);
						if (subDetection.getLocalisation() != null) {
							groupDetectionLocalisation.setArrayAxis(subDetection.getLocalisation().getArrayOrientationVectors());
						}
					}
				}
				
				setLocalisation(groupDetectionLocalisation);
			}
		}


		private void setNumLocations(int numLocations) {
			latLong = Arrays.copyOf(latLong, numLocations);
			originLatLong = Arrays.copyOf(originLatLong, numLocations);
			bearing = Arrays.copyOf(bearing, numLocations);
			range = Arrays.copyOf(range, numLocations);
			perpendicularError = Arrays.copyOf(perpendicularError, numLocations);
			parallelError = Arrays.copyOf(parallelError, numLocations);
			errorReferenceAngle = Arrays.copyOf(errorReferenceAngle, numLocations);
			referenceHeading = Arrays.copyOf(referenceHeading, numLocations);
			referenceChannels = Arrays.copyOf(referenceChannels, numLocations);			
		}


		@Override
		public int addSubDetection(t subDetection) {
			return super.addSubDetection(subDetection);
		}


		public boolean addFitData(LatLong originLatLong, LatLong latLong, double bearing, double range, 
				double errorReferenceAngle, 
				double perpendicularError, double parallelError, double referenceHeading) {
			if (numLatLong >= this.latLong.length) {
				setNumLocations(numLatLong+1);
			}
			return setFitData(numLatLong++, originLatLong, latLong, bearing, range, 
					errorReferenceAngle, perpendicularError, parallelError, referenceHeading);
		}
		
		public boolean setFitData(int i, LatLong originLatLong, LatLong latLong, 
				double bearing, double range, double errorReferenceAngle, 
				double perpendicularError, double parallelError, double referenceHeading) {
			setOriginLatLong(i, originLatLong);
			setLatLong(i, latLong);
			setBearing(i, bearing);
			setRange(i, range);
			setErrorReferenceAngle(i, errorReferenceAngle);
			setPerpendicularError(i, perpendicularError);
			setParallelError(i, parallelError);
			setReferenceHeading(i, referenceHeading);
			this.groupDetectionLocalisation.addLocContents(AbstractLocalisation.HAS_BEARING |  AbstractLocalisation.HAS_RANGE |
					AbstractLocalisation.HAS_RANGEERROR | AbstractLocalisation.HAS_LATLONG | AbstractLocalisation.HAS_PERPENDICULARERRORS);
			return true;
		}
		
		public void clearFitData() {
			numLatLong = 0;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public int getEventId() {
			return eventId;
		}
		
		public void setEventId(int eventId) {
			this.eventId = eventId;
		}

		public LatLong getLatLong(int i) {
			return latLong[i];
		}
		
		public void setLatLong(int i, LatLong latLong) {
			makeLocalisation(null);
			this.latLong[i] = latLong;
		}
		

		public double getBearing(int i) {
			return bearing[i];
		}
		
		public Double getPredictedBearing(long predictionTime) {
			if (getSubDetectionsCount() <= 0) {
				return null;
			}
			t lastSubDet = getSubDetection(getSubDetectionsCount()-1);
			AbstractLocalisation loc = lastSubDet.getLocalisation();
			if (localisation == null) {
				return null;
			}
			double[] planarAngles = loc.getPlanarAngles();
			if (planarAngles == null) {
				return loc.getBearing(0);
			}
			return planarAngles[0];
			
//			return null;
		}

		public void setBearing(int i, double bearing) {
			makeLocalisation(null);
			groupDetectionLocalisation.addLocContents(AbstractLocalisation.HAS_BEARING);
			this.bearing[i] = bearing;
		}

		public LatLong getOriginLatLong(int i) {
			return originLatLong[i];
		}

		public void setOriginLatLong(int i, LatLong originLatLong) {
			makeLocalisation(null);
			groupDetectionLocalisation.addLocContents(AbstractLocalisation.HAS_LATLONG);
			this.originLatLong[i] = originLatLong;
		}

		public double getRange(int i) {
			return range[i];
		}

		public void setRange(int i, double range) {
			makeLocalisation(null);
			groupDetectionLocalisation.addLocContents(AbstractLocalisation.HAS_RANGE);
			this.range[i] = range;
		}

		public double getParallelError(int i) {
			return parallelError[i];
		}

		public void setParallelError(int i, double parallelError) {
			this.parallelError[i] = parallelError;
		}

		public double getPerpendiculaError(int i) {
			return perpendicularError[i];
		}

		public void setPerpendicularError(int i, double perpendicularError) {
			this.perpendicularError[i] = perpendicularError;
		}

		public double getReferenceHeading(int i) {
			return referenceHeading[i];
		}

		public void setReferenceHeading(int i, double referenceHeading) {
			makeLocalisation(null);
			this.referenceHeading[i] = referenceHeading;
		}

		public boolean isHasFit() {
			return (latLong[0]!= null && numLatLong> 0);
		}

		public int getReferenceChannels(int i) {
			return referenceChannels[i];
		}

		public void setReferenceChannels(int i, int referenceChannels) {
			this.referenceChannels[i] = referenceChannels;
			makeLocalisation(null);
			groupDetectionLocalisation.setReferenceHydrophones(referenceChannels);
		}

		public GroupDetectionLocalisation getGroupDetectionLocalisation() {
			return groupDetectionLocalisation;
		}

		public int getNumLatLong() {
			return numLatLong;
		}

		public void setNumLatLong(int numLatLong) {
			this.numLatLong = numLatLong;
		}

		public double getErrorReferenceAngle(int iSide) {
			return errorReferenceAngle[iSide];
		}

		public void setErrorReferenceAngle(int iSide, double errorReferenceAngle) {
			this.errorReferenceAngle[iSide] = errorReferenceAngle;
		}
}
