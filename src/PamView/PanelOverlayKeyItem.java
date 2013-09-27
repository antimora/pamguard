package PamView;

import java.awt.Component;

public class PanelOverlayKeyItem implements PamKeyItem {

	PamDetectionOverlayGraphics pamDetectionOverlayGraphics;
	
	public PanelOverlayKeyItem(PamDetectionOverlayGraphics pamDetectionOverlayGraphics) {
		super();
		this.pamDetectionOverlayGraphics = pamDetectionOverlayGraphics;
	}

	public Component getIcon(int keyType, int nComponent) {
		PamSymbol pamSymbol = pamDetectionOverlayGraphics.getPamSymbol();
		if (pamSymbol == null) {
			return null;
		}
		return new IconPanel(pamSymbol);
	}

	public int getNumItems(int keyType) {
		return 1;
	}

	public String getText(int keyType, int nComponent) {

		return pamDetectionOverlayGraphics.getParentDataBlock().getDataName();
		
	}

}
