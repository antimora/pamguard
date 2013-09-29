package PamGraph3D.graphDisplay3D;

import javax.media.j3d.Shape3D;

import PamguardMVC.PamDataUnit;

public interface DataSymbol3DProvider {
	
	/**
	 * @param unit
	 * @return
	 */
	public DataUnitShape3D getSymbol(PamDataUnit unit);
	
	public DataUnitShape3D getSymbolSelected(PamDataUnit unit);
	
}
