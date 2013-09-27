package nidaqdev;

import java.io.Serializable;

import Acquisition.SoundCardParameters;
import PamguardMVC.PamConstants;

public class NIDaqParams extends SoundCardParameters implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;

	private int[] deviceList;
	
	private int[] hwChannelList;
	
	public int terminalConfiguration = NIConstants.DAQmx_Val_Diff; 
	
	private double[][] aiRange;
	
	public boolean enableMultiBoard;

	public int[] getDeviceList() {
		return deviceList;
	}

	public void setDeviceList(int[] deviceList) {
		this.deviceList = deviceList;
	}

	public int[] getHwChannelList() {
		return hwChannelList;
	}

	public void setHwChannelList(int[] hwChannelList) {
		this.hwChannelList = hwChannelList;
	}

	@Override
	public NIDaqParams clone() {
		return (NIDaqParams) super.clone();
	}
	
	public double[] getAIRange(int iChannel) {
		if (aiRange == null) {
			createDefaultAIRanges();
		}
		return aiRange[iChannel];
	}
	
	public void setAIRange(int iChannel, double[] range) {
		if (aiRange == null) {
			createDefaultAIRanges();
		}
		aiRange[iChannel] = range;
	}
	
	private void createDefaultAIRanges() {
		aiRange = new double[PamConstants.MAX_CHANNELS][2];
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			aiRange[i][0] = -1;
			aiRange[i][1] = 1;
		}
	}
}
