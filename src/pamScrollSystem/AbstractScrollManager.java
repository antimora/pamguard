package pamScrollSystem;

import java.util.Vector;

import javax.swing.JPopupMenu;

import dataMap.OfflineDataMap;

import PamController.PamController;
import PamguardMVC.PamDataBlock;

public abstract class AbstractScrollManager {

	protected Vector<AbstractPamScroller> pamScrollers = new Vector<AbstractPamScroller>();
	
	private Vector<ScrollerCoupling> scrollerCouplings = new Vector<ScrollerCoupling>();
	
	private static AbstractScrollManager singleInstance;
	
	protected AbstractScrollManager() {
		
	}
	
	public static synchronized AbstractScrollManager getScrollManager() {
		if (singleInstance == null) {
			int runMode = PamController.getInstance().getRunMode();
			
			switch(runMode) {
			case PamController.RUN_PAMVIEW:
				singleInstance = new ViewerScrollerManager();
				break;
			default:
				singleInstance = new RealTimeScrollManager();	
			}
		}
		return singleInstance;
	}
	
	/**
	 * Add a new scroller to the managed list. 
	 * @param pamScroller scroller to add
	 */
	public void addPamScroller(AbstractPamScroller pamScroller) {
		if (pamScrollers.indexOf(pamScroller) < 0) {
			pamScrollers.add(pamScroller);
		}
	}
	
	/**
	 * Remove a pam scroller from the managed list. 
	 * @param pamScroller scroller to remove
	 */
	public void removePamScroller(AbstractPamScroller pamScroller) {
		pamScrollers.remove(pamScroller);
	}
	
	/**
	 * Move the scroll bar component of a scroller. This should not cause the
	 * reloading of any data, but other scroll bars should be notified of 
	 * any changes. 
	 * @param scroller scroller that moved
	 * @param newValue new value (time in milliseconds). 
	 */
	abstract public void moveInnerScroller(AbstractPamScroller scroller, long newValue);
	
	/**
	 * Move the data load component of a scroller. This should cause data to be 
	 * reloaded and will need to notify all other scrollers incase they also 
	 * need to shuffle along a bit. 
	 * @param scroller scroller that changed
	 * @param newMin new data min value in millis
	 * @param newMax new data max value in millis
	 */
	abstract public void moveOuterScroller(AbstractPamScroller scroller, long newMin, long newMax);

	/**
	 * Check the maximum time requested by a scroll bar doesn't go beyond the end of the data
	 * @param requestedTime requested time in millis.
	 * @return the minimum of the requested time and the actual end time of the data
	 */
	abstract public long checkMaximumTime(long requestedTime);

	/**
	 * Check the minimum time requested by a scroll bar doesn't go below the start of the data
	 * @param requestedTimerequested time in millis.
	 * @return the maximum of the requested time and the actual start time of the data
	 */
	abstract public long checkMinimumTime(long requestedTime);

	abstract public void notifyModelChanged(int changeType);

	/**
	 * Centre all data in all data blocks at the given time
	 * @param menuMouseTime time in milliseconds
	 */
	abstract public void centreDataAt(PamDataBlock dataBlock, long menuMouseTime);

	/**
	 * Start all data in all data blocks at the given time
	 * @param dataBlock
	 * @param menuMouseTime time in milliseconds
	 * @param immediateLoad load data immediately in current thread. Don't re-schedule for later. 
	 */
	abstract public void startDataAt(PamDataBlock dataBlock, long menuMouseTime, boolean immediateLoad);
	
	final public void startDataAt(PamDataBlock dataBlock, long menuMouseTime) {
		startDataAt(dataBlock, menuMouseTime, false);
	}
	
	

	/**
	 * Couple a scroller to another scroller so that both
	 * have exactly the same behaviour, load the same data period, 
	 * move their scrolls together, etc. 
	 * <p>
	 * Scollers are coupled by name so that they don't necessarily
	 * need to find references to each other in the code. These names 
	 * can be anything by measures should be taken to ensure that they
	 * are going to be unique, for example by using module names as
	 * part of the coupling name.  
	 * @param abstractPamScroller scroller to couple
	 * @param couplingName coupling name
	 * @return reference to the coupler
	 */
	public ScrollerCoupling coupleScroller(AbstractPamScroller abstractPamScroller, String couplingName) {
		ScrollerCoupling coupling = findCoupling(couplingName, true);
		coupling.addScroller(abstractPamScroller);
		return coupling;
	}

	/**
	 * Uncouple a scroller. 
	 * @param abstractPamScroller scroller to uncouple
	 */
	public void uncoupleScroller(AbstractPamScroller abstractPamScroller) {
		ScrollerCoupling aCoupling = abstractPamScroller.getScrollerCoupling();
		if (aCoupling == null) {
			return;
		}
		aCoupling.removeScroller(abstractPamScroller);
		if (aCoupling.getScrollerCount() == 0) {
			scrollerCouplings.remove(aCoupling);
		}
	}	
	
	/**
	 * Find a scroller coupling with a given name
	 * @param name name of coupling
	 * @param autoCreate flag to automatically create a coupling if one isn't found. 
	 * @return the scroller coupling or null if none was found and the autoCreate flag was false. 
	 */
	private ScrollerCoupling findCoupling(String name, boolean autoCreate) {
		ScrollerCoupling aCoupling = null;
		for (int i = 0; i < scrollerCouplings.size(); i++) {
			aCoupling = scrollerCouplings.get(i);
			if (aCoupling.name.equals(name)) {
				return aCoupling;
			}
		}
		// if it get's here, then no coupling was found
		if (autoCreate) {
			scrollerCouplings.add(aCoupling = new ScrollerCoupling(name));
		}
		return aCoupling;
	}
	
	/**
	 * Command telling manager to reload it's data. 
	 */
	public abstract void reLoad();

	public JPopupMenu getStandardOptionsMenu(AbstractPamScroller pamSCroller) {
		return null;
	}
	
	/**
	 * Work out whether or not a particular time falls in the 
	 * gap between points in a datamap .
	 * @param dataBlock Pamguard data block
	 * @param timeMillis time in milliseconds
	 * @return true if the data are in a gap.
	 */
	public int isInGap(PamDataBlock dataBlock, long timeMillis) {
		OfflineDataMap dataMap = dataBlock.getPrimaryDataMap();
		if (dataMap == null) {
			return -1;
		}
		return dataMap.isInGap(timeMillis);
	}
	

	
	/**
	 * Check to see whether or not we are scrolling into a data gap. 
	 * Rules exist for stopping  / starting / jumping over gaps 
	 * depending on the current state and the new position of 
	 * the scroller. 
	 * @param abstractPamScroller PamScroller that moved
	 * @param oldMin old minimum time
	 * @param oldMax old maximum time
	 * @param newMin new minimum time
	 * @param newMax new maximum time
	 * @param direction direction of scroll +1 = forward, -1 = backward, 0 = plonked down by mouse on datamap. 
	 * @return new minimum position. Calling function must then work out the new maximum position. 
	 */
	public abstract long checkGapPos(AbstractPamScroller abstractPamScroller,
			long oldMin, long oldMax, long newMin, long newMax, int direction);
	
}
