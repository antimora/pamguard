package listening;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.GeneralProjector;
import PamView.ManagedSymbol;
import PamView.ManagedSymbolInfo;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PamSymbolManager;
import PamView.PanelOverlayDraw;
import PamguardMVC.PamDataUnit;

public class ListeningEffortGraphics implements PanelOverlayDraw , ManagedSymbol {

	private ListeningControl listeningControl;
	
	private PamSymbol thingSymbol = null;

	public ListeningEffortGraphics(ListeningControl listeningControl) {
		this.listeningControl = listeningControl;
		PamSymbolManager.getInstance().addManagesSymbol(this);
	}
	public boolean canDraw(GeneralProjector generalProjector) {
//		if (true) return true;
		return (generalProjector.getParmeterType(0) == GeneralProjector.ParameterType.LONGITUDE && 
				generalProjector.getParmeterType(1) == GeneralProjector.ParameterType.LATITUDE);
	}

	public PamKeyItem createKeyItem(GeneralProjector generalProjector,
			int keyType) {
		return getPamSymbol().makeKeyItem("Listening Effort");
	}

	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {
		if (generalProjector.getParmeterType(0) == GeneralProjector.ParameterType.LONGITUDE && 
				generalProjector.getParmeterType(1) == GeneralProjector.ParameterType.LATITUDE) {
			return drawOnMap(g, pamDataUnit, generalProjector);
		}
		return null;
	}
	
	public Rectangle drawOnMap(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {

		ListeningEffortData thing = (ListeningEffortData) pamDataUnit;
		if (thing == null) {
			return null;
		}
		Coordinate3d pos;
		LatLong oll = thing.getOriginLatLong(false);
		if (oll == null) {
			return null;
		}
		pos = generalProjector.getCoord3d(oll.getLatitude(),oll.getLongitude(), 0);
		Rectangle r = getPamSymbol().draw(g, pos.getXYPoint());
		generalProjector.addHoverData(pos, pamDataUnit);

		return r;
	}
	
	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {

		ListeningEffortData thing = (ListeningEffortData) dataUnit;

		String str = "<html>Listening Effort at " + PamCalendar.formatTime(thing.getTimeMilliseconds());
		str += String.format("<br>%s", thing.getStatus());
		LatLong oll = thing.getOriginLatLong(false);
		if (oll != null) {
			str += String.format("<br>%s, %s", LatLong.formatLatitude(oll.getLatitude()),
					LatLong.formatLongitude(oll.getLongitude()));
		}
		str += "</html>";
		return str;
	}
	@Override
	public PamSymbol getPamSymbol() {
		if (thingSymbol == null) {
			setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 6, 6, true, Color.BLUE, Color.RED));
		}
		return thingSymbol;
	}
	@Override
	public ManagedSymbolInfo getSymbolInfo() {
		return new ManagedSymbolInfo("Listening Effort");
	}
	@Override
	public void setPamSymbol(PamSymbol pamSymbol) {
		thingSymbol = pamSymbol;
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
