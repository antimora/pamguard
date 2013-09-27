package PamDetection;

import java.util.ListIterator;
import java.util.Vector;


/**
 * Base class for all Pamguard detections.
 * Hopefully, we'll be able to subclass this off to become
 * more specific for various other detection types. 
 * <p>
 * These things will nearly always be stored within a PamDataUnit
 * which already contains some of the information - such as the 
 * channelMap - should this be repeated here ?  
 * <p> 
 * One thing this will have to do is to deal with the myriad of different
 * amplitude types. I suggest that the basic storage unit should either be
 * fft amplitude units - which will be there for any detection that comes
 * out of a spectrogram, or peak ADC values, such as would come from a click
 * detector. The class will have functions for converting between the different
 * amplitude types. 
 * 
 * @author Doug Gillespie
 *
 */
public class PamDetection<T extends PamDetection,U extends PamDetection> extends AcousticDataUnit {

	/**
	 * Time of peak within a detection, in msec.
	 * relative to the start of the PAMGUARD run
	 */
	private double peakTime;

	String detectionType;

	//	T subDetection; 
	private Vector<T> subDetections;

	//	U superDetection;
	private Vector<U> superDetections;

	private Object subDetectionSyncronisation = new Object();

	private Object superDetectionSyncronisation = new Object();

	/**
	 * Event end time in millis. Used when there are multiple sub detections. 
	 */
	private long eventEndTime;

	public PamDetection(long timeMilliseconds, 
			int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds, channelBitmap, 
				startSample, duration);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return Returns the peakTime, in msec.
	 * relative to the start of the PAMGUARD run
	 */
	public double getPeakTime() {
		return peakTime;
	}

	/**
	 * @param peakTimeMsec The peakTime to set, in msec.
	 * relative to the start of the PAMGUARD run.
	 */
	public void setPeakTime(double peakTimeMsec) {
		this.peakTime = peakTimeMsec;
	}

	/**
	 * @return Returns the detectionType.
	 */
	public String getDetectionType() {
		return detectionType;
	}

	/**
	 * @param detectionType The detectionType to set.
	 */
	public void setDetectionType(String detectionType) {
		this.detectionType = detectionType;
	}

	/**
	 * Add a sub detection to the sub detection list. 
	 * @param subDetection
	 */
	public int addSubDetection(T subDetection) {
		int nAdded = 0;
		synchronized (subDetectionSyncronisation) {
			if (subDetections == null) {
				subDetections = new Vector<T>();
			}
			if (getLocalisation() != null && 
					subDetection.getLocalisation() != null && 
					getLocalisation().getArrayOrientationVectors() == null) {
				getLocalisation().setArrayAxis(subDetection.getLocalisation().getArrayOrientationVectors());
			}
			/**
			 * These things may need to go in in the right order - so ...
			 */
			int sz = subDetections.size();
			boolean added = false;
			if (sz == 0) {
				subDetections.add(subDetection);
				added = true;
				nAdded ++;
			}
			else {// start at the end, since it's probably only going to get smaller.
				T aSubDetection;
				for (int i = sz-1; i >= 0; i--) {
					aSubDetection = subDetections.get(i);
					if (aSubDetection == subDetection) {
						return 0; // already added. 
					}
					if (aSubDetection.compareTo(subDetection) < 0) {
						subDetections.insertElementAt(subDetection, i+1);
						added = true;
						nAdded ++;
						break;
					}
				}
				if (!added) {
					subDetections.insertElementAt(subDetection, 0);
					nAdded++;
				}
			}
			setDuration(subDetection.getStartSample() + subDetection.getDuration() - subDetections.get(0).getStartSample());
			return nAdded;
		}
	}

	/**
	 * Sets a unique super detection. i.e. if the
	 * data unit already had a super detection of the 
	 * same class, then this data unit is removed from that
	 * pre-existing superdetection. 
	 * @param superDetection
	 */
	public int setUniqueSuperDetection(U superDetection) {
		synchronized (superDetectionSyncronisation) {
			if (superDetections != null) {
				ListIterator<U> superList = superDetections.listIterator();
				U aSuper;
				while (superList.hasNext()) {
					aSuper = superList.next();
					if (aSuper == superDetection) {
						return 0; // they are the same !
					}
					if (aSuper.getClass() == superDetection.getClass()) {
						superList.remove();
						aSuper.removeSubDetection(this);
					}
				}
			}
			addSuperDetection(superDetection);
			return 1;
		}
	}

	public void addSuperDetection(U superDetection) {
		synchronized (superDetectionSyncronisation) {
			if (superDetections == null) {
				superDetections = new Vector<U>();
			}
			superDetections.add(superDetection);
			//			superDetection.addSubDetection(this);
		}
	}
	
	/**
	 * find a sub detection based on it's time and it's channel map
	 * @param timeMillis time in milliseconds
	 * @param channelBitmap channel map
	 * @return found sub detection, or null if none exists. 
	 */
	public T findSubDetection(long timeMillis, int channelBitmap) {
		if (subDetections == null) {
			return null;
		}
		for (T aSub:subDetections) {
			if (aSub.getTimeMilliseconds() == timeMillis && aSub.getChannelBitmap() == channelBitmap) {
				return aSub;
			}
		}
		return null;
	}
	
	/**
	 * Replace a sub detection in an event. <p> if the original sub detection 
	 * does not exist, then the new one is added anyway. 
	 * @param oldOne old sub detection
	 * @param newOne new sub detection. 
	 */
	public void replaceSubDetection(T oldOne, T newOne) {
		int ind = subDetections.indexOf(oldOne);
		if (ind < 0) {
			addSubDetection(newOne);
		}
		else {
			subDetections.remove(oldOne);
			oldOne.removeSuperDetection(this);
			subDetections.add(ind, newOne);
			newOne.addSuperDetection(this);
		}
	}

	public int getSubDetectionsCount() {
		if (subDetections == null) return 0;
		return subDetections.size();
	}

	public int getSuperDetectionsCount() {
		if (superDetections == null) return 0;
		return superDetections.size();
	}

	public T getSubDetection(int ind) {
		synchronized (subDetectionSyncronisation) {
			if (subDetections == null) return null;
			return subDetections.get(ind);
		}
	}

	public U getSuperDetection(Class superClass) {
		synchronized (superDetectionSyncronisation) {
			if (superDetections == null) return null;
			U superDet;
			for (int i = 0; i < superDetections.size(); i++) {
				superDet = superDetections.get(i);
				if (superDet.getClass() == superClass) {
					return superDet;
				}
			}
		}		
		return null;
	}

	public U getSuperDetection(int ind) {
		synchronized (superDetectionSyncronisation) {
			if (superDetections == null || superDetections.size()<1) return null;
			return superDetections.get(ind);
		}
	}

	public void removeSubDetection(T subDetection) {
		synchronized (subDetectionSyncronisation) {
			if (subDetections != null) {
				subDetections.remove(subDetection);
			}
			if (subDetection != null) {
				subDetection.removeSuperDetection(this);
			}
		}
	}

	public void removeSuperDetection(U superDetection) {
		synchronized (superDetectionSyncronisation) {
			superDetections.remove(superDetection);
		}
	}

	public Object getSubDetectionSyncronisation() {
		return subDetectionSyncronisation;
	}

	public Object getSuperDetectionSyncronisation() {
		return superDetectionSyncronisation;
	}

	public void setEventEndTime(long eventEndTime) {
		this.eventEndTime = eventEndTime;
	}

	public long getEventEndTime() {
		return eventEndTime;
	}

}
