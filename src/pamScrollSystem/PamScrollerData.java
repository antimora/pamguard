package pamScrollSystem;

import java.io.Serializable;

public class PamScrollerData implements Serializable, Cloneable {


	private static final long serialVersionUID = 1L;

	/*
	 * Name for the scroller - gets used in various dialogs. 
	 */
	protected String name;

	/**
	 * Minimum time of data loaded for this scroller
	 */
	protected long minimumMillis;

	/**
	 * Maximum time of data loaded for this scroller
	 */
	protected long maximumMillis;

	/**
	 * Scroller step size in millis (this will often be seconds or even minutes)
	 */
	protected int stepSizeMillis;


	/**
	 * Page step size - percentage of (maximumMills-mnimumMillis) to
	 * move back or forth through the data in response to a
	 * new load data command. 
	 */
	protected int pageStep = 75;

	/**
	 * Default data load time. 
	 */
	protected long defaultLoadtime = 120000;
	

	@Override
	protected PamScrollerData clone() {
		try {
			return (PamScrollerData) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * A quick way of getting the duration of loaded data
	 * @return maximumMillis - minimumMillis
	 */
	public long getLength() {	
		return maximumMillis - minimumMillis;
	}
}
