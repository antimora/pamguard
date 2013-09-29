package pamScrollSystem;

/**
 * Class for tansfering data about  adata load precess. 
 * @author Doug Gillespie
 *
 */
public interface ViewLoadObserver {
	
	/**
	 * Report progress back to the load observer
	 * @param state
	 * @param loadStart
	 * @param loadEnd
	 * @param lastTime
	 * @param nLoaded
	 */
	void sayProgress(int state, long loadStart, long loadEnd, long lastTime, int nLoaded);
	
	/**
	 * Ask the load observer if loading should be stopped. 
	 * @return
	 */
	boolean cancelLoad();
}
