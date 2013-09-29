package xBatLogViewer;

import PamController.PamControlledUnit;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class XBatLogProcess extends PamProcess {

	private XBatLogDataBlock xBatDataBlock;
	private XBatOverlayGraphics xBatOverlayGraphics;
	private XBatLogBinaryStorage xBatBinaryDataSource;
	
	public XBatLogProcess(PamControlledUnit pamControlledUnit,
			PamDataBlock parentDataBlock) {
		super(pamControlledUnit, parentDataBlock);
		xBatDataBlock = new XBatLogDataBlock(pamControlledUnit.getUnitName(), 
				this, 0);
		xBatDataBlock.setOverlayDraw(xBatOverlayGraphics = new XBatOverlayGraphics(xBatDataBlock));
		xBatDataBlock.setBinaryDataSource(xBatBinaryDataSource = new XBatLogBinaryStorage(xBatDataBlock));
		addOutputDataBlock(xBatDataBlock);
	}

	@Override
	public void pamStart() {}

	@Override
	public void pamStop() {}

}
