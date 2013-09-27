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

public class ThingHeardGraphics implements PanelOverlayDraw , ManagedSymbol {

	private ListeningControl listeningControl;
	
	private PamSymbol thingSymbol = null;

	public ThingHeardGraphics(ListeningControl listeningControl) {
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
		return getPamSymbol().makeKeyItem("Thing Heard");
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

		ThingHeard thing = (ThingHeard) pamDataUnit;
		if (thing == null) {
			return null;
		}
		Coordinate3d pos;
		LatLong oll = thing.getOriginLatLong(false);
		if (oll == null) {
			return null;
		}
		pos = generalProjector.getCoord3d(oll.getLatitude(),oll.getLongitude(), 0);
		Rectangle r = getItemSymbol(thing).draw(g, pos.getXYPoint());
		generalProjector.addHoverData(pos, pamDataUnit);

		return r;
	}
	
	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {

		ThingHeard thing = (ThingHeard) dataUnit;

		String str = "<html>Thing Heard at " + PamCalendar.formatTime(thing.getTimeMilliseconds());
		if (thing.getVolume() >= 0) {
			str += String.format("<br>%s volume %d", thing.getSpeciesItem().getName(), thing.getVolume());
		}
		String comment = thing.getComment();
		if (comment != null && comment.length() > 0) {
			str += String.format("<br>%s", comment);
		}
		LatLong oll = thing.getOriginLatLong(false);
		if (oll != null) {
			str += String.format("<br>%s, %s", LatLong.formatLatitude(oll.getLatitude()),
					LatLong.formatLongitude(oll.getLongitude()));
		}
		str += "</html>";
		return str;
	}
	
	public PamSymbol getItemSymbol(ThingHeard thingHeard) {
		SpeciesItem speciesItem = thingHeard.getSpeciesItem();
		PamSymbol ps;
		if (speciesItem == null) {
			ps = getPamSymbol();
		}
		else {
			ps = speciesItem.getSymbol();
		}
		if (ps != null) {
			return ps;
		}
		return getPamSymbol();
	}
	
	@Override
	public PamSymbol getPamSymbol() {
		if (thingSymbol == null) {
			setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_HEXAGRAM, 10, 10, true, Color.ORANGE, Color.ORANGE));
		}
		return thingSymbol;
	}
	@Override
	public ManagedSymbolInfo getSymbolInfo() {
		return new ManagedSymbolInfo("Things Heard");
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
