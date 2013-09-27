package PamView;

import java.io.Serializable;

import PamUtils.PamUtils;

/**
 * Specific parameters which always to with a GroupedSourcePanel
 * @author Doug Gillespie
 *
 */
abstract public class GroupedSourceParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;

	/**
	 * Name of data source
	 */
	private String dataSource;
	
	/**
	 * Bitmap of all channels used
	 */
	private int channelBitmap;

	/**
	 * integer list of which group each channel belongs to
	 */
	private int[] channelGroups;

	/**
	 * Grouping selection
	 */
	private int groupingType = GroupedSourcePanel.GROUP_ALL;
	
	/**
	 * Get the total number of channel groups
	 * @return number of groups
	 */
	public int countChannelGroups() {
		return GroupedSourcePanel.countChannelGroups(channelBitmap, channelGroups);
	}
	
	/**
	 * Get the specific channels associated with a particular group. 
	 * @param iGroup group index (0, 1, 2, 3 whatever the actual group numbers are !)
	 * @return bitmap of group channels
	 */
	public int getGroupChannels(int iGroup) {
		return GroupedSourcePanel.getGroupChannels(iGroup, channelBitmap, channelGroups);
	}

	/**
	 * @return the dataSource
	 */
	public String getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @return the channelBitmap
	 */
	public int getChannelBitmap() {
		return channelBitmap;
	}

	/**
	 * @param channelBitmap the channelBitmap to set
	 */
	public void setChannelBitmap(int channelBitmap) {
		this.channelBitmap = channelBitmap;
	}

	/**
	 * @return the channelGroups
	 */
	public int[] getChannelGroups() {
		return channelGroups;
	}

	/**
	 * @param channelGroups the channelGroups to set
	 */
	public void setChannelGroups(int[] channelGroups) {
		this.channelGroups = channelGroups;
	}

	/**
	 * @return the groupingType
	 */
	public int getGroupingType() {
		return groupingType;
	}

	/**
	 * @param groupingType the groupingType to set
	 */
	public void setGroupingType(int groupingType) {
		this.groupingType = groupingType;
	}
	
	/**
	 * 
	 * @return true if at least one group has multiple elements, so 
	 * might be able to calculate bearings. 
	 */
	public boolean mayHaveBearings() {
		int n = countChannelGroups();
		int groupMap;
		for (int i = 0 ; i< n; i++) {
			groupMap = getGroupChannels(i);
			if (PamUtils.getNumChannels(groupMap) > 1) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @return true if mayHaveBearings is true and if there is more than one group
	 */
	public boolean mayHaveRange() {
		return (mayHaveBearings() && countChannelGroups() > 1); 
	}

	@Override
	protected GroupedSourceParameters clone() {
		try {
			return (GroupedSourceParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
