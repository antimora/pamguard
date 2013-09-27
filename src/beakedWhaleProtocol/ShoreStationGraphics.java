package beakedWhaleProtocol;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;

import angleMeasurement.AngleDataUnit;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.GeneralProjector;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PanelOverlayDraw;
import PamguardMVC.PamDataUnit;

public class ShoreStationGraphics implements PanelOverlayDraw {

	BeakedControl beakedControl;
	
	PamSymbol shoreSymbol = new PamSymbol(PamSymbol.SYMBOL_PENTAGRAM, 20, 20, true, Color.GREEN, Color.RED);

	public ShoreStationGraphics(BeakedControl beakedControl) {
		this.beakedControl = beakedControl;
	}

	public boolean canDraw(GeneralProjector generalProjector) {
//		if (true) return true;
		return (generalProjector.getParmeterType(0) == GeneralProjector.ParameterType.LONGITUDE && 
				generalProjector.getParmeterType(1) == GeneralProjector.ParameterType.LATITUDE);
	}

	public PamKeyItem createKeyItem(GeneralProjector generalProjector,
			int keyType) {
		return shoreSymbol.makeKeyItem("Shore Station");
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

		ShoreStationDataUnit sdu = (ShoreStationDataUnit) pamDataUnit;
		if (sdu == null || sdu.getLatLong() == null) {
			return null;
		}
		Coordinate3d c3d = generalProjector.getCoord3d(sdu.getLatLong().getLatitude(), sdu.getLatLong().getLongitude(), sdu.getHeight());
		generalProjector.addHoverData(c3d, pamDataUnit, 0);
		
		Rectangle r = shoreSymbol.draw(g, c3d.getXYPoint());
		
		if (beakedControl.beakedParameters.measureAngles &&
				beakedControl.beakedParameters.showLine && sdu.getMeasuredAngle() != null) {
			g.setColor(shoreSymbol.getLineColor());
			LatLong end = sdu.getLatLong().travelDistanceMeters(sdu.getMeasuredAngle(), 
					beakedControl.beakedParameters.lineLength);
			Coordinate3d c2 = generalProjector.getCoord3d(end.getLatitude(), end.getLongitude(), 0);
			g.drawLine((int)c3d.x, (int)c3d.y, (int)c2.x, (int)c2.y);
			generalProjector.addHoverData(c2, pamDataUnit, 1);
			
			AngleDataUnit heldAngle = beakedControl.beakedProcess.getHeldAngle();
			if (heldAngle != null) {
				g.setColor(shoreSymbol.getFillColor());
				end = sdu.getLatLong().travelDistanceMeters(heldAngle.correctedAngle, 
						beakedControl.beakedParameters.lineLength);
				c2 = generalProjector.getCoord3d(end.getLatitude(), end.getLongitude(), 0);
				g.drawLine((int)c3d.x, (int)c3d.y, (int)c2.x, (int)c2.y);
				generalProjector.addHoverData(c2, pamDataUnit, 2);
			}
		}
		
		return r;
	}

	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
		ShoreStationDataUnit sdu = (ShoreStationDataUnit) dataUnit;
		String str = null;
		if (iSide == 0) {
		str = "<html>Shore Station";
		str += "<br>Latitude " + LatLong.formatLatitude(sdu.getLatLong().getLatitude());
		str += "<br>Longitude " + LatLong.formatLongitude(sdu.getLatLong().getLongitude());
		str += String.format("<br>Height %.0f m", sdu.getHeight());
		if (beakedControl.beakedParameters.measureAngles) {
			if (sdu.getMeasuredAngle() == null) {
				str += "<br>Angle measurement error";
			}
			else {
				str += String.format("<br>Angle %.1f\u00B0 (%s)", sdu.getMeasuredAngle(), 
						PamCalendar.formatDateTime(sdu.getTimeMilliseconds()));
			}
		}
		str += "</html>";
		}
		else if (iSide == 1){
			str = String.format("<html> Bearing from Shore Station: %.1f\u00B0T", sdu.getMeasuredAngle());
		}
		else {
			AngleDataUnit ha = beakedControl.beakedProcess.getHeldAngle();
			if (ha == null) {
				return null;
			}
			str = String.format("<html> Held angle from Shore Station: %.1f\u00B0T", ha.correctedAngle);
		}
		return str;
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
