package IshmaelLocator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import PamView.GeneralProjector;
import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
/**
 * Process for choosing a symbol to represent IshLocalizations on the
 * map (and elsewhere).
 * @author Dave Mellinger
 */
public class IshOverlayGraphics extends PamDetectionOverlayGraphics {

	/** Choose a symbol, color, size, etc.
	 */
	@Override
	public PamSymbol getPamSymbol() {
		if (super.getPamSymbol() == null) {
			//This code runs only once; thereafter, symbol type/color/size is read from prefs file.
			setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 5, 5, true, Color.red, Color.red));
		}
		return super.getPamSymbol();
	}

	public IshOverlayGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		// TODO Auto-generated method stub
		return super.drawDataUnit(g, pamDataUnit, generalProjector);
	}
}
