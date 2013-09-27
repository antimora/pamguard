package noiseBandMonitor;

import java.io.Serializable;

import Filters.FilterType;

public class NoiseBandSettings implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	public String rawDataSource;
	public int channelMap = 1;
	public int startDecimation = 0;
	public int endDecimation = 8;
	public BandType bandType = BandType.THIRDOCTAVE;
	public FilterType filterType = FilterType.BUTTERWORTH;
	public int iirOrder = 3;
	public int firOrder = 7;
	public double firGamma = 2.5;
	public int lowBandNumber;
	public int highBandNumber = 30; 
	public int outputIntervalSeconds = 10;
	
	// a few params for the plot
	public boolean logFreqScale = true;
	public boolean showGrid = true;
	public boolean showDecimators = true;
	private boolean[] showStandard = new boolean[3];
	public int scaleToggleState = 0;
	

	@Override
	protected NoiseBandSettings clone() {
		try {
			return (NoiseBandSettings) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean getShowStandard(int iStandard) {
		if (showStandard == null) {
			showStandard = new boolean[3];
		}
		return showStandard[iStandard];
	}
	
	public void setShowStandard(int iStandard, boolean show) {
		if (showStandard == null) {
			showStandard = new boolean[3];
		}
		showStandard[iStandard] = show;
	}

}
