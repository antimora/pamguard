package fftManager;

import java.io.Serializable;

public class FFTDataDisplayOptions implements Serializable, Cloneable {
	
	static final long serialVersionUID = 1;
	
	double maxVal = 110;
	double minVal = 70;
	int smoothingFactor = 10;
	boolean useSpecValues = true;
//	//ArrayList<Integer> channelsToPlot = new ArrayList<Integer>();
//	ArrayList<Integer> plottablechannels = new ArrayList<Integer>();
//	ArrayList<Boolean> plottedchannels = new ArrayList<Boolean>();
//	int plottableChannels;
	int plottedChannels = 0xFFFF;
	
//	int getNumChannels() {
//		return PamUtils.getNumChannels(channelBitmap);
//	}
	//int [] channelId = new int[numChannels];
	//boolean [] plotChannel = new boolean [numChannels];
	
	//PamConstants.MAX_CHANNELS
	
	@Override
	protected FFTDataDisplayOptions clone() {
		try {
			return (FFTDataDisplayOptions) super.clone();
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

}
