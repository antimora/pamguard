package networkTransfer.emulator;

import java.io.Serializable;
import java.util.Arrays;

import PamController.PamControlledUnitSettings;
import PamUtils.LatLong;

public class EmulatorParams implements Cloneable, Serializable {


	public static final long serialVersionUID = 1L;
	
	LatLong gpsCentre = new LatLong(52.5, 3.087);
	
	double circleRadius = 1000;
	
	int nBuoys = 10;
	
	int firstBuoyId = 201;
	
	int statusIntervalSeconds = 30;
	
	boolean[] usedBlocks;
	
	public boolean[] getUsedBlocks(int nBlocks) {
		if (usedBlocks == null) {
			usedBlocks = new boolean[nBlocks];
		}
		else if (usedBlocks.length < nBlocks) {
			usedBlocks = Arrays.copyOf(usedBlocks, nBlocks);
		}
		return usedBlocks;
	}


	@Override
	protected EmulatorParams clone() {
		try {
			return (EmulatorParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
