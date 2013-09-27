package bearingTimeDisplay;

import java.awt.Color;
import java.awt.Point;

import javax.swing.JPopupMenu;

import PamView.PamColors;
import PamView.PamSymbol;
import PamView.PamColors.PamColor;
import PamguardMVC.PamDataUnit;

/**
 * A very simple symbol class. This is the default for pam data blocks which do not point towards to their own 2DSymbolProvider.
 * @author Jamie Macaulay
 *
 */
public class DefaultDataSymbol implements DataSymbolProvider {
	
	static PamSymbol defaultSymbol;
	static PamSymbol highlightSymbol;
	
	public DefaultDataSymbol(){
		defaultSymbol=getDefaultSymbol(false);
		highlightSymbol=getHighLightSymbol();
	}


	@Override
	public PamSymbol getSymbol(PamDataUnit unit) {
		return defaultSymbol;
	}

	@Override
	public PamSymbol getSymbolSelected(PamDataUnit unit) {
		return highlightSymbol;
	}




public static PamSymbol getDefaultSymbol(boolean makeClone) {
	if (defaultSymbol == null) {
		defaultSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 8, 8,
				true, PamColors.getInstance().getColor(PamColor.PLAIN), 
				PamColors.getInstance().getColor(PamColor.PLAIN));
	}
	if (defaultSymbol.getFillColor() != PamColors.getInstance().getColor(PamColor.PLAIN)) {
		defaultSymbol.setFillColor(PamColors.getInstance().getColor(PamColor.PLAIN));
		defaultSymbol.setLineColor(PamColors.getInstance().getColor(PamColor.PLAIN));
	}
	// always reset the shape since it may have been messed about with
	defaultSymbol.setSymbol(PamSymbol.SYMBOL_CIRCLE);
	if (makeClone) {
		return defaultSymbol.clone();
	}
	else {
		return defaultSymbol;
	}
}

public static PamSymbol getHighLightSymbol(){
	 return  new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 20, 20, false, Color.WHITE, Color.GRAY);

}


@Override
public Point getSymbolSize(PamDataUnit unit) {
	return new Point (5,5);
}


@Override
public void addMenuItems(JPopupMenu popUpMenu) {
	
}

}
