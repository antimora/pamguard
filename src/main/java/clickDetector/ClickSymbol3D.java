package clickDetector;

import javax.media.j3d.Shape3D;

import PamGraph3D.graphDisplay3D.DataSymbol3DProvider;
import PamGraph3D.graphDisplay3D.DataUnitShape3D;
import PamguardMVC.PamDataUnit;

/**
 * Provides symbols for the graph3Ddisplay of a click data block is used. . 
 * @author spn1
 *
 */
public class ClickSymbol3D implements DataSymbol3DProvider{
	
	private ClickDataBlock clickDataBlock;
	private ClickControl clickControl;

	public ClickSymbol3D(ClickDataBlock clickDataBlock){
		this.clickDataBlock=clickDataBlock;
		this.clickControl=clickDataBlock.clickControl;
	}

	@Override
	public DataUnitShape3D getSymbol(PamDataUnit unit) {
		return null;
	}

	@Override
	public DataUnitShape3D getSymbolSelected(PamDataUnit unit) {
		return null;
	}
	

}
