package videoRangePanel;

import PamUtils.PamCalendar;
import PamguardMVC.PamProcess;

public class VRProcess extends PamProcess {

	private VRControl vrControl;
	
	private VRDataBlock vrDataBlock;
	
	public VRProcess(VRControl vrControl) {
		super(vrControl, null);
		this.vrControl = vrControl;
		addOutputDataBlock(vrDataBlock = new VRDataBlock(vrControl.getUnitName(), this));
		vrDataBlock.SetLogging(new VRLogging(vrControl, this));
	}

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub

	}
	
	public void newRange(VRMeasurement vrMeasurement) {
		VRDataUnit vrDataUnit = new VRDataUnit(PamCalendar.getTimeInMillis(), vrMeasurement);
		vrDataBlock.addPamData(vrDataUnit);
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}

	public VRDataBlock getVrDataBlock() {
		return vrDataBlock;
	}

}
