package depthReadout;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class DepthProcess extends PamProcess {
	
	DepthControl depthControl;

	DepthDataBlock depthDataBlock;
	
	public DepthProcess(DepthControl depthControl) {
		super(depthControl, null, "Hydrophone Depth Readout");
		this.depthControl = depthControl;

		addOutputDataBlock(depthDataBlock = new DepthDataBlock("Hydrophone Depth Data", this));
		depthDataBlock.SetLogging(new DepthSQLLogging(depthControl, depthDataBlock));
		depthDataBlock.setMixedDirection(PamDataBlock.MIX_OUTOFDATABASE);
		depthDataBlock.setNaturalLifetime(10000);
	}

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}

	protected boolean readDepthData() {
		DepthSystem depthSystem = depthControl.getDepthSystem();
		if (depthSystem == null) {
			return false;
		}
		
		double[] rawData = new double[depthControl.depthParameters.nSensors];
		double[] depth = new double[depthControl.depthParameters.nSensors];
		for (int i = 0; i < depthControl.depthParameters.nSensors; i++) {
			if (depthSystem.readSensor(i) == false) {
				return false;
			}
			rawData[i] = depthSystem.getDepthRawData(i);
			depth[i] = depthSystem.getDepth(i);
		}
		DepthDataUnit depthDataUnit = new DepthDataUnit(PamCalendar.getTimeInMillis());
		depthDataUnit.setRawDepthData(rawData);
		depthDataUnit.setDepthData(depth);
		
		depthDataBlock.addPamData(depthDataUnit);
		
		return true;
	}
}
