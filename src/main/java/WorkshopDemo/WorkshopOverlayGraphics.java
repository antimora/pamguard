package WorkshopDemo;

import java.awt.Color;
import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamguardMVC.PamDataBlock;

/**
 * Graphics examples showing how to draw the detector output on the map and
 * on the spectrogram display.
 * The class GeneralProjector knows how to turn parameters such as lat, long, time, 
 * frequency into meaningful screen coordinates. It will tell us which types
 * of coordinates are required for each display, if we can provide them we can
 * go ahead and draw. The canDraw function is mainly used by Pamguard for making
 * up options menus of what can be plotted on top of the various displays. The 
 * PanelOverlayDraw is attached to a specific data block, so this one class needs
 * to handle drawing on all different types of display. 
 * @author Doug
 *
 */
public class WorkshopOverlayGraphics extends  PamDetectionOverlayGraphics {

	public WorkshopOverlayGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock);
		if (getPamSymbol() == null) {
			setLineColour(Color.RED);
			setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 6, 6, true, Color.RED, Color.GREEN));
		}
	}

}
