package bearingTimeDisplay;

import java.awt.Point;

import javax.swing.JPopupMenu;

import PamGraph3D.graphDisplay3D.DataUnitShape3D;
import PamView.PamSymbol;
import PamguardMVC.PamDataUnit;

public interface DataSymbolProvider {
	
	/**
	 * @param unit
	 * @return
	 */
	public PamSymbol getSymbol(PamDataUnit unit);
	
	public PamSymbol getSymbolSelected(PamDataUnit unit);
	
	public Point getSymbolSize(PamDataUnit unit);
	
	public void addMenuItems(JPopupMenu popUpMenu);
		

	
}
