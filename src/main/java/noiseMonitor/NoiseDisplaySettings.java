package noiseMonitor;

import java.io.Serializable;
import java.util.Arrays;

public class NoiseDisplaySettings implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	/*
	 * display parameters
	 */
	long displayLengthSeconds = 900;
	
	double levelMin = 70;
	
	double levelMax = 170;
	
	boolean[] selectedChannels;
	
	int selectedStats = 0x1;
	
	private boolean[] selectedData;

	public boolean autoScale = false;
	
	public boolean showGrid = false;
	
	public boolean showKey = false;
	
	public boolean isSelectData(int iBand) {
		if (selectedData == null || selectedData.length <= iBand) {
			return false;
		}
		return selectedData[iBand];
	}
	
	public void setSelectData(int iBand, boolean state) {
		if (selectedData == null) {
			selectedData = new boolean[iBand+1];
		}
		else if (selectedData.length <= iBand) {
			selectedData = Arrays.copyOf(selectedData, iBand+1);
		}
		selectedData[iBand] = state;
	}


	@Override
	protected NoiseDisplaySettings clone() {
		try {
			return (NoiseDisplaySettings) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
