package pamScrollSystem;

import java.util.Vector;

/**
 * A class for coupling two or more scrollers. 
 * <p>
 * Coupled scrollers will move together - when one moves, the
 * others move. 
 * @author Doug Gillespie
 *
 */
public class ScrollerCoupling {

	protected String name;
	
	private Vector<AbstractPamScroller> scrollers = new Vector<AbstractPamScroller>();

	public ScrollerCoupling(String name) {
		super();
		this.name = name;
	}
	
	/**
	 * Add a new scroller to the coupling
	 * @param aScroller
	 */
	public void addScroller(AbstractPamScroller aScroller) {
		if (scrollers.indexOf(aScroller) < 0) {
			scrollers.add(aScroller);
		}
		/*
		 *  now need to immediately pick up settings from
		 *  one of the existing scrollers - may not be the first if 
		 *  this one was already there.  
		 */
		for (int i = 0; i < scrollers.size(); i++) {
			if (scrollers.get(i) == aScroller) {
				continue;
			}
			else {
				aScroller.coupledScrollerChanged(scrollers.get(i));
			}
			break;
		}
	}
	
	/**
	 * lock to stop looping of calls to the notifyOthers function.
	 */
	private boolean notificationLock = false;
	/**
	 * Notify other scrollers in the set that a scroller has
	 * changed then pass them a reference to the changed
	 * scroller so that they can copy information from it. 
	 * <p>
	 * This function holds a lock  since as soon as another 
	 * scroller is changed, it's likely to call back to this same
	 * function and set up an infinite loop. The lock will exit 
	 * the function if set to avoid this situation. 
	 * @param scroller scroller which changes. 
	 */
	public void notifyOthers(AbstractPamScroller scroller) {
		if (notificationLock) {
			return;
		}
		notificationLock = true;
		AbstractPamScroller aScroller;
		for (int i = 0; i < scrollers.size(); i++) {
			aScroller = scrollers.get(i);
			if (aScroller == scroller) {
				continue; // no need to notify itself !
			}
			aScroller.coupledScrollerChanged(scroller);
		}
		notificationLock = false;
	}
	
	/**
	 * Remove a scroller form a coupling
	 * @param aScroller
	 * @return true if the scroller wwas present. 
	 */
	public boolean removeScroller(AbstractPamScroller aScroller) {
		return scrollers.remove(aScroller);
	}
	
	public int getScrollerCount() {
		return scrollers.size();
	}
	
	
}
