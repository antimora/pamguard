package PamController;

import java.io.Serializable;


public class PamViewParameters extends Object implements Serializable, Cloneable {

	static final long serialVersionUID = 1;

	/**
	 * start and end times for Pamguard viewer
	 * These are the times of the data (UTC in most
	 * database tables)
	 */
	public long viewStartTime, viewEndTime;
	
	/**
	 * Analysis offline may have gone through the data multiple times.
	 * IF this is the case, you may want to select by analysis time 
	 * as well as the data time. (LocalTime in most database tables)
	 */
	public boolean useAnalysisTime;

	/**
	 * Analysis offline may have gone through the data multiple times.
	 * IF this is the case, you may want to select by analysis time 
	 * as well as the data time. (LocalTime in most database tables)
	 */
	public long analStartTime, analEndTime;
	
	/**
	 * List of modules to use. This is sent directly into all modules
	 * from the view times dialog. 
	 */
	public boolean[] useModules;

	@Override
	public PamViewParameters clone() {
		try {
			return (PamViewParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean equals(PamViewParameters otherParameters) {
		if (otherParameters == null) {
			return false;
		}
		if (otherParameters.viewStartTime != viewStartTime) {
			return false;
		}
		if (otherParameters.viewEndTime != viewEndTime) {
			return false;
		}
		if (otherParameters.useAnalysisTime != useAnalysisTime) {
			return false;
		}
		if (useAnalysisTime == false) {
			return true; // no need to shcek the other parameters in this instance
		}
		if (otherParameters.analStartTime != analStartTime) {
			return false;
		}
		if (otherParameters.analEndTime != analEndTime) {
			return false;
		}
		return true;
	}
	
	
	
	/**
	 * @return the viewStartTime
	 */
	public long getRoundedViewStartTime() {
		return roundDown(viewStartTime);
	}

	/**
	 * @param viewStartTime the viewStartTime to set
	 */
	public void setViewStartTime(long viewStartTime) {
		this.viewStartTime = viewStartTime;
	}

	/**
	 * @return the viewEndTime
	 */
	public long getRoundedViewEndTime() {
		return roundUp(viewEndTime);
	}

	/**
	 * @param viewEndTime the viewEndTime to set
	 */
	public void setViewEndTime(long viewEndTime) {
		this.viewEndTime = viewEndTime;
	}

	/**
	 * @return the useAnalysisTime
	 */
	public boolean isUseAnalysisTime() {
		return useAnalysisTime;
	}

	/**
	 * @param useAnalysisTime the useAnalysisTime to set
	 */
	public void setUseAnalysisTime(boolean useAnalysisTime) {
		this.useAnalysisTime = useAnalysisTime;
	}

	/**
	 * @return the analStartTime
	 */
	public long getRoundedAnalStartTime() {
		return roundDown(analStartTime);
	}

	/**
	 * @param analStartTime the analStartTime to set
	 */
	public void setAnalStartTime(long analStartTime) {
		this.analStartTime = analStartTime;
	}

	/**
	 * @return the analEndTime
	 */
	public long getRoundedAnalEndTime() {
		return roundUp(analEndTime);
	}

	/**
	 * @param analEndTime the analEndTime to set
	 */
	public void setAnalEndTime(long analEndTime) {
		this.analEndTime = analEndTime;
	}

	/**
	 * Round a time down to nearest second
	 * @param time
	 * @return time rounded down to nearest second
	 */
	public long roundDown(long time) {
		long r = time%1000;
		return time-r;
	}
	/**
	 * Round a time down up nearest second
	 * @param time
	 * @return time rounded down to nearest second
	 */
	public long roundUp(long time) {
		long r = time%1000;
		if (r == 0) {
			return time;
		}
		return time-r+1000;
	}
}
