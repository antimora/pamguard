package clipgenerator;

import java.io.Serializable;

/**
 * Settings for a clip generator - which can be triggered by any AcousticDataUnit. 
 * <p>
 * 
 * @author Doug Gillespie
 *
 */
public class ClipGenSetting implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	/*
	 * Include only the detected channels in the clip
	 */
	public static final int DETECTION_CHANNELS_ONLY = 0;
	/*
	 * Include only the first of the detected channels in the clip
	 */
	public static final int FIRST_DETECTION_CHANNEL_ONLY = 1;
	
	/*
	 * include all channels in the clip
	 */
	public static final int ALL_CHANNELS = 2;

	/**
	 * Types of channel selection. 
	 */
	public static final String[] channelSelTypes = {"Detection channels only", "First detection channel only", "All channels"};

	/**
	 * Data name of the trigger data block. 
	 */
	public String dataName;
	
	/**
	 * Enabled
	 */
	boolean enable = true;

	/**
	 * Seconds before start of trigger
	 */
	public double preSeconds = 0;

	/**
	 * Seconds after end of trigger. 
	 */
	public double postSeconds = 0;

	/**
	 * Channel selection, all, first, one, etc. 
	 */
	public int channelSelection = DETECTION_CHANNELS_ONLY;
	
	/**
	 * prefix for the clip (ahead of the date string). 
	 * Can be null in which case the default is used. 
	 */
	public String clipPrefix;
	
	/**
	 * If false, then record absolutely everything. 
	 */
	public boolean useDataBudget = true;

	/**
	 * Data budget in kilobytes. 
	 */
	public int dataBudget = 10*1024;

	/**
	 * Budget period in hours. 
	 */
	public double budgetPeriodHours = 24.;


	/**
	 * @param dataName
	 */
	public ClipGenSetting(String dataName) {
		super();
		this.dataName = dataName;
	}


	@Override
	protected ClipGenSetting clone()  {
		try {
			return (ClipGenSetting) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
