package clickDetector.offlineFuncs;

import java.util.List;

import clickDetector.ClickDetection;
import PamDetection.PamDetection;
import PamView.PamColors;
import PamguardMVC.PamDataBlock;

/**
 * OfflineEventDataUnit replicates the RainbowClick functionality in 
 * OfflineRCEvent. 
 * <p>
 * A lot of the functionality required to associate clicks with 
 * an event is already wrapped up in the base classes of PamDataBlock 
 * and PamDataUnit. 
 * 
 * @author Doug Gillespie
 *
 */
public class OfflineEventDataUnit extends PamDetection<ClickDetection, PamDetection> {

	private String eventType;
	
	private String comment;
	
	private int nClicks;
	

	
	/**
	 * Flag to say that event times may be a bit dodgy. 
	 * This will happen if an event is only partially loaded 
	 * into memory when some clicks are deleted and it may 
	 * be impossible to work out what the true start and end times
	 * are. 
	 */
	private boolean suspectEventTimes = false;
	
//	private int eventNumber; // will use the database index instead 
	
	/**
	 * specific colour index - can be null.
	 */
	private Integer colourIndex;
	
	/**
	 * Minimum number of animals in event
	 * <p>(can be null)
	 */
	private Short minNumber;
	/**
	 * Best estimate of number of animals in event
	 * <p>(can be null)
	 */
	private Short bestNumber;
	/**
	 * Maximum number of animals in event
	 * <p>(can be null)
	 */
	private Short maxNumber;
	
	public OfflineEventDataUnit(String eventType, Integer colourIndex, ClickDetection firstClick) {
//		super(firstClick.getTimeMilliseconds(), firstClick.getChannelBitmap(), 
//				firstClick.getStartSample(), firstClick.getDuration());
		super(0,0,0,0);
		this.eventType = eventType;
//		this.eventNumber = eventNumber;
		this.colourIndex = colourIndex;
		if (firstClick != null) {
			this.addSubDetection(firstClick);
		}
	}

	/**
	 * Add a list of clicks to an event. 
	 * 
	 * @param markedClicks List of marked clicks. 
	 */
	public void addClicks(List<ClickDetection> markedClicks) {
		if (markedClicks == null) {
			return;
		}
		for (int i = 0; i < markedClicks.size(); i++) {
			addSubDetection(markedClicks.get(i));
		}
	}
	
	/**
	 * Add a sub detection with the option of not increasing the click
	 * count. This is used when setting up data as it's read from the 
	 * database, not when adding new clicks under normal operation
	 * @param subDetection click to add to event
	 * @param countClick true if click count should be increased. 
	 */
	public void addSubDetection(ClickDetection subDetection, boolean countClick) {
		if (addSubDetection(subDetection) > 0 && countClick == false) {
			nClicks--;
		}
	}
	
	/**
	 * Add a new click to the event. As each click is added, 
	 * some checks are done to make sure that the click is not
	 * already part of some other event, and if all events are 
	 * removed from some other event, make sure that that other 
	 * event get's deleted. 
	 * @param subDetection a new click to add. 
	 *  
	 */
	@Override
	public int addSubDetection(ClickDetection subDetection) {
		/*
		 * First check that the click is not already part of this event. 
		 * If it is, then it's super detection will point at this, so 
		 * just return true and get on with it. 
		 */
//		if (subDetection.getSuperDetection(OfflineEventDataUnit.class) == this) {
//			return 0;
//		}
		ClickDetection existingSubDetection = findSubDetection(subDetection.getTimeMilliseconds(), 
				subDetection.getStartSample(), subDetection.getChannelBitmap());
		if (existingSubDetection != null) {
			replaceSubDetection(existingSubDetection, subDetection);
			checkEventICIData(subDetection.getParentDataBlock().getSampleRate());
			return 0;
		}
		
		if (getSubDetectionsCount() == 0) {
			setStartSample(subDetection.getStartSample());
		}
		setChannelBitmap(subDetection.getChannelBitmap() | getChannelBitmap());
		if (getTimeMilliseconds() == 0) {
			setTimeMilliseconds(subDetection.getTimeMilliseconds());
			setEventEndTime(subDetection.getTimeMilliseconds());
		}
		else {
			setEventEndTime(Math.max(getEventEndTime(), subDetection.getTimeMilliseconds()));
			setTimeMilliseconds(Math.min(getTimeMilliseconds(), subDetection.getTimeMilliseconds()));
		}
		if (setUniqueEvent(subDetection, this) == 0) {
			return 0; // sub det already there, so nothing added
		}
		if (getParentDataBlock() != null) {
			getParentDataBlock().updatePamData(this, System.currentTimeMillis());
		}
		subDetection.getParentDataBlock().updatePamData(subDetection, System.currentTimeMillis());
		nClicks++;
		
		checkEventICIData(subDetection.getParentDataBlock().getSampleRate());
		
		return super.addSubDetection(subDetection);
	}

	public ClickDetection findSubDetection(long timeMillis, long startSample, int channelBitmap) {
		// TODO Auto-generated method stub
		ClickDetection click = super.findSubDetection(timeMillis, channelBitmap);
		if (click != null && click.getStartSample() == startSample) {
			return click;
		}
		else {
			return null;
		}
	}

	private void checkEventICIData(double sampleRate) {
		int nSub = getSubDetectionsCount();
		if (nSub <= 0) {
			return;
		}
		ClickDetection aClick = getSubDetection(0);
		aClick.setICI(0);
		if (nSub == 1) {
			return;
		}
		/*
		 * Try to do this with sample, but if it's in a different 
		 * file that may not work ! IF ici from sample number
		 * is > a second different from that from milliseconds, use the
		 * milliseconds. 
		 */
		long lastTime = aClick.getTimeMilliseconds();
		long lastSample = aClick.getStartSample();
		long thisTime;
//		double sampleRate = getParentDataBlock().getSampleRate();
		double ici, ici2, iciDiff;
		for (int i = 1; i < nSub; i++) {
			aClick = getSubDetection(i);
			thisTime = aClick.getTimeMilliseconds();
			ici = (thisTime-lastTime) / 1000.;
			ici2 = (aClick.getStartSample()-lastSample) / sampleRate;
			iciDiff = Math.abs(ici2-ici);
			if (iciDiff < 1.) {
				aClick.setICI(ici2);
//				aClick.setICI((thisTime-lastTime) / 1000.);
			}
			else {
				aClick.setICI(ici);
			}
			lastTime = thisTime;
			lastSample = aClick.getStartSample();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void removeSubDetection(ClickDetection subDetection) {
		super.removeSubDetection(subDetection);
		PamDataBlock clickDataBlock = subDetection.getParentDataBlock();
		nClicks--;
		if (nClicks <= 0) {
//			getParentDataBlock().remove(this);
		}
		else {
			/*
			 * Need to get new start and end times - problem is that 
			 * it's just possible not all data are loaded in memory
			 * so it may be impossible to tell if it was the first or
			 * last click that got loaded. 
			 */
			boolean haveStart = false, haveEnd = false;
			if (getSubDetectionsCount() == nClicks) {
				// easy - everything in memory. 
				haveStart = haveEnd = true;
				suspectEventTimes = false;
			}
			else if (clickDataBlock.getCurrentViewDataStart() > getTimeMilliseconds()) {
				haveStart = false;
				suspectEventTimes = true;
			}
			else if (clickDataBlock.getCurrentViewDataEnd() < getEventEndTime()) {
				haveEnd = false;
				suspectEventTimes = true;
			}
			else {
				// this should never happen !
				suspectEventTimes = true;
				System.out.println("Some confusion as to which data are loaded for event click removal");
			}
			if (haveStart) {
				setTimeMilliseconds(getSubDetection(0).getTimeMilliseconds());
			}
			if (haveEnd) {
				setEventEndTime(getSubDetection(getSubDetectionsCount()-1).getTimeMilliseconds());
			}
			if (getParentDataBlock() != null) {
				getParentDataBlock().updatePamData(this, System.currentTimeMillis());
			}	
		}
		subDetection.getParentDataBlock().updatePamData(subDetection, System.currentTimeMillis());
		
	}

	/**
	 * Ensure that there is only one event superdetection
	 * per click. 
	 * @param clickDetection
	 * @param event
	 */
	private int setUniqueEvent(ClickDetection clickDetection, OfflineEventDataUnit event) {
		return clickDetection.setUniqueSuperDetection(event);
	}

	/**
	 * @return the eventType
	 */
	public String getEventType() {
		return eventType;
	}
	
	public short getColourIndex() {
		int col = getDatabaseIndex();
		if (colourIndex != null && colourIndex != 0) {
			col = colourIndex;
		}
		int nCol = PamColors.getInstance().NWHALECOLORS;
		return (short) ((col-1)%nCol+1);
	}

	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	
	/**
	 * @return the eventNumber which is the
	 * same as the database index.
	 */
	public int getEventNumber() {
		return getDatabaseIndex();
	}
//
//	/**
//	 * @param eventNumber the eventNumber to set
//	 */
//	public void setEventNumber(int eventNumber) {
//		this.eventNumber = eventNumber;
//	}

	public Short getMinNumber() {
		return minNumber;
	}

	public void setMinNumber(Short minNumber) {
		this.minNumber = minNumber;
	}

	public Short getBestNumber() {
		return bestNumber;
	}

	public void setBestNumber(Short bestNumber) {
		this.bestNumber = bestNumber;
	}

	public Short getMaxNumber() {
		return maxNumber;
	}

	public void setMaxNumber(Short maxNumber) {
		this.maxNumber = maxNumber;
	}

	public int getNClicks() {
		return nClicks;
	}

	public void setNClicks(int nClicks) {
		this.nClicks = nClicks;
	}

	/**
	 * @return the suspectEventTimes
	 */
	public boolean isSuspectEventTimes() {
		return suspectEventTimes;
	}

	/**
	 * @param suspectEventTimes the suspectEventTimes to set
	 */
	public void setSuspectEventTimes(boolean suspectEventTimes) {
		this.suspectEventTimes = suspectEventTimes;
	}

	/**
	 * Quick way for events to tell observers of the data block that they 
	 * have updated. 
	 */
	public void notifyUpdate() {
		if (getParentDataBlock() != null) {
			getParentDataBlock().updatePamData(this, System.currentTimeMillis());
		}
	}

}
