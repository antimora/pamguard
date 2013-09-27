package alarm;

import java.awt.Color;
import java.io.Serializable;

public class AlarmParameters implements Serializable, Cloneable {

	/**
	 * 
	 */
	public static final long serialVersionUID = 1L;
	
	public static final int COUNT_SIMPLE = 0;
	public static final int COUNT_SCORES = 1;
	
	public static final int COUNT_LEVELS = 2;
	public static final String[] levelNames = {"Amber", "Red"};
	public static final Color alarmColours[] = {new Color(255, 128, 0), new Color(255, 0, 0)};
	
	public String dataSourceName;
	public int countType = COUNT_SIMPLE;
	public long countIntervalMillis = 10000L;
	private double[] triggerCounts = new double[COUNT_LEVELS];

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected AlarmParameters clone() {
		try {
			AlarmParameters np = (AlarmParameters) super.clone();
			if (np.triggerCounts == null || np.triggerCounts.length != COUNT_LEVELS) {
				np.triggerCounts = new double[COUNT_LEVELS];
			}
			return np;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public double getTriggerCount(int countLevel) {
		return triggerCounts[countLevel];
	}
	
	public void setTriggerCount(int countLevel, double value) {
		triggerCounts[countLevel] = value;
	}
	
}
