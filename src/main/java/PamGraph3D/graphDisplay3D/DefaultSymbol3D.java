package PamGraph3D.graphDisplay3D;

import javax.media.j3d.Shape3D;

import PamGraph3D.PamShapes3D;
import PamguardMVC.PamDataUnit;

public class DefaultSymbol3D implements DataSymbol3DProvider{
	
	public DefaultSymbol3D(){
	}

	@Override
	public DataUnitShape3D getSymbol(PamDataUnit unit) {
		return new DataUnitShape3D( unit,PamShapes3D.createSphere(PamShapes3D.sphereAppearanceMatt(PamShapes3D.blue), 0.03f));
	}

	@Override
	public DataUnitShape3D getSymbolSelected(PamDataUnit unit) {
		return new DataUnitShape3D(unit,PamShapes3D.createSphere(PamShapes3D.sphereAppearanceMatt(PamShapes3D.red), 0.05f));
	}

}
