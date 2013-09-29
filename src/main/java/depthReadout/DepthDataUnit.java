package depthReadout;

import PamguardMVC.PamDataUnit;

public class DepthDataUnit extends PamDataUnit {
	
	private double[] depthData;
	
	private double[] rawDepthData;

	public DepthDataUnit(long timeMilliseconds) {
		super(timeMilliseconds);
	}

	public double[] getDepthData() {
		return depthData;
	}

	public void setDepthData(double[] depthData) {
		this.depthData = depthData;
	}

	public double[] getRawDepthData() {
		return rawDepthData;
	}

	public void setRawDepthData(double[] rawDepthData) {
		this.rawDepthData = rawDepthData;
	}

}
