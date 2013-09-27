package clickDetector;

import java.awt.Color;
import java.awt.Point;

import javax.swing.JPopupMenu;

import PamView.PamColors;
import PamView.PamSymbol;
import PamView.PamColors.PamColor;
import PamguardMVC.PamDataUnit;
import bearingTimeDisplay.DataSymbolProvider;
import bearingTimeDisplay.DefaultDataSymbol;

/**
 * A very simple symbol class. This is the default for pam data blocks which do not point towards to their own 2DSymbolProvider.
 * @author Jamie Macaulay
 *
 */
public class ClickDataSymbol implements DataSymbolProvider {
	
	static PamSymbol defaultSymbol;
	static PamSymbol defaultSymbolEcho;
	static PamSymbol highlightSymbol;
	
	private int maxSize=12;
	private int minSize=3;
	
	private ClickDataBlock clickDataBlock;
	
	public ClickDataSymbol(ClickDataBlock clickDataBlock){
		defaultSymbol=DefaultDataSymbol.getDefaultSymbol(false);
		defaultSymbolEcho=getEchoDefaultSymbol();
		highlightSymbol=getHighLightSymbol();
		this.clickDataBlock=clickDataBlock;
	}
	
	@Override
	public PamSymbol getSymbol(PamDataUnit unit) {
	
		if (((ClickDetection) unit).getClickType()!=0){
			return  (clickDataBlock.clickControl.getClickIdentifier().getSymbol(((ClickDetection) unit)));
		}
//		if (((ClickDetection) unit).getSuperDetectionsCount()!=0){
//			return new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 20, 20, false, Color.BLUE, Color.BLUE);
//		}
	return defaultSymbol;
	}

	@Override
	public PamSymbol getSymbolSelected(PamDataUnit unit) {
		return highlightSymbol;
	}


	public static PamSymbol getHighLightSymbol(){
		 return  new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 15, 15, false, Color.WHITE, Color.GRAY);
	}
	
	public static PamSymbol getEchoDefaultSymbol(){
		PamSymbol echoSymbol=DefaultDataSymbol.getDefaultSymbol(true);
		echoSymbol.setFillColor(Color.WHITE);
		return echoSymbol;
	}


	@Override
	public Point getSymbolSize(PamDataUnit unit) {
		
		int size=maxSize*((ClickDetection) unit).getWaveData()[0].length/(clickDataBlock.clickControl.getClickParameters().maxLength+clickDataBlock.clickControl.getClickParameters().postSample+clickDataBlock.clickControl.getClickParameters().preSample);
		
		if (size<minSize) return new Point(minSize,minSize);
		if (size>maxSize) return new Point(maxSize,maxSize);
		
		return new Point (size,size);
	}

	@Override
	public void addMenuItems(JPopupMenu popUpMenu) {
		//need to add colour by click train/ click id
		
	}

}
