package autecPhones;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamView.BasicKeyItem;
import PamView.GeneralProjector;
import PamView.ManagedSymbol;
import PamView.ManagedSymbolInfo;
import PamView.PamColors;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PanelOverlayDraw;
import PamView.PamColors.PamColor;
import PamguardMVC.PamDataUnit;

public class AutecGraphics implements
		PanelOverlayDraw , ManagedSymbol {


	private static final int SYMBOLSIZE = 10;
	
	private PamSymbol defSymbol = new PamSymbol(PamSymbol.SYMBOL_DIAMOND, SYMBOLSIZE, SYMBOLSIZE, true, Color.RED, Color.BLUE);
	
	private PamSymbol symbol;
	
	@Override
	public boolean canDraw(GeneralProjector generalProjector) {
		return (generalProjector.getParmeterType(0) == GeneralProjector.ParameterType.LONGITUDE && 
				generalProjector.getParmeterType(1) == GeneralProjector.ParameterType.LATITUDE);
	}

	@Override
	public PamKeyItem createKeyItem(GeneralProjector generalProjector,
			int keyType) {
		return new BasicKeyItem(getPamSymbol(), "Autec Hydrophones");
	}

	@Override
	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {
		PamSymbol symbol = getPamSymbol();
		AutecDataUnit adu = (AutecDataUnit) pamDataUnit;
		Coordinate3d c3d = generalProjector.getCoord3d(adu.getLatLong().getLatitude(), 
				adu.getLatLong().getLongitude(), adu.getDepth());
		generalProjector.addHoverData(c3d, pamDataUnit);
		Rectangle r =  symbol.draw(g, c3d.getXYPoint());
		g.setColor(PamColors.getInstance().getColor(PamColor.AXIS));
		g.drawString(String.format("%d", adu.getID()), (int) c3d.x + symbol.getWidth()/2, 
				(int) c3d.y + symbol.getHeight()/2);
		return r;
	}

	@Override
	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int side) {
		AutecDataUnit adu = (AutecDataUnit) dataUnit;
		LatLong ll = adu.getLatLong();
		return String.format("<html>Hydropone %d (%s)<br>%s, %s<br>Depth %5.1f m</html>", 
				adu.getID(), adu.isActive() ? "Active" : "Inactive",
				LatLong.formatLatitude(ll.getLatitude()),
				LatLong.formatLongitude(ll.getLongitude()), adu.getDepth());
	}

	@Override
	public PamSymbol getPamSymbol() {
		if (symbol == null) {
			symbol = defSymbol;
		}
		return symbol;
	}

	@Override
	public ManagedSymbolInfo getSymbolInfo() {
		return new ManagedSymbolInfo("Autec Hydrophones");
	}

	@Override
	public void setPamSymbol(PamSymbol pamSymbol) {
		symbol = pamSymbol.clone();
	}

	@Override
	public boolean hasOptionsDialog(GeneralProjector generalProjector) {
		return false;
	}

	@Override
	public boolean showOptions(Window parentWindow,
			GeneralProjector generalProjector) {
		return false;
	}
}
