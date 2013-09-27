package whistlesAndMoans;

import java.awt.Color;

import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamguardMVC.PamDataBlock;

public class WhistleToneLocalisationGraphics extends PamDetectionOverlayGraphics {

	PamDataBlock<WhistleToneGroupedDetection> whistleLocations;
	public WhistleToneLocalisationGraphics(PamDataBlock<WhistleToneGroupedDetection> whistleLocations) {
		super(whistleLocations);
		this.whistleLocations = whistleLocations;
		if (getPamSymbol() == null) {
			setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, false, Color.BLACK, Color.BLUE));
		}
	}

}
