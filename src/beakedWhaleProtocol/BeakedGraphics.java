package beakedWhaleProtocol;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.GeneralProjector;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PanelOverlayDraw;
import PamguardMVC.PamDataUnit;

/**
 * Draws the beaked whale protocol graphics on the map
 * @author Douglas Gillespie
 *
 */
public class BeakedGraphics implements PanelOverlayDraw {

	BeakedControl beakedControl;
	
	PamSymbol symbol = new PamSymbol(PamSymbol.SYMBOL_HEXAGRAM, 15, 15, true, Color.RED, Color.BLUE);
	
	public BeakedGraphics(BeakedControl beakedControl) {
		super();
		this.beakedControl = beakedControl;
		
	}

	public boolean canDraw(GeneralProjector generalProjector) {
//		if (true) return true;
		return (generalProjector.getParmeterType(0) == GeneralProjector.ParameterType.LONGITUDE && 
				generalProjector.getParmeterType(1) == GeneralProjector.ParameterType.LATITUDE);
	}

	public PamKeyItem createKeyItem(GeneralProjector generalProjector,
			int keyType) {
		// TODO Auto-generated method stub
		return symbol.makeKeyItem("Beaked Whale Location");
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
		Graphics2D g2d = (Graphics2D) g;
		Rectangle r;
		BeakedDataUnit beakedDataUnit = (BeakedDataUnit) pamDataUnit;
		LatLong latLong = beakedDataUnit.getBeakedLatLong();
		Coordinate3d c3d = generalProjector.getCoord3d(latLong.getLatitude(), latLong.getLongitude(), 0);
		generalProjector.addHoverData(c3d, pamDataUnit);
		boolean isLast = isLastLocation(pamDataUnit);
		boolean isCurrent = isCurrentExperiment(pamDataUnit);
		if (isLast || isCurrent) {
			symbol.setHeight(15);
			symbol.setWidth(15);
		}
		else {
			symbol.setHeight(8);
			symbol.setWidth(8);
		}
		r = symbol.draw(g, c3d.getXYPoint());
		if (isLast || isCurrent) {
			// draw a circlae around it at the right range ...
			LatLong latLong2 = latLong.travelDistanceMeters(0, beakedControl.beakedParameters.maxAcousticRange);
			Coordinate3d ep = generalProjector.getCoord3d(latLong2.getLatitude(), latLong2.getLongitude(), 0);
			double pixs = Math.sqrt((ep.x-c3d.x) * (ep.x-c3d.x) + (ep.y-c3d.y) * (ep.y-c3d.y));
			g.setColor(symbol.getLineColor());
//			g2d.setColor(PamColors.getInstance().getColor(PamColor.AXIS));
			g2d.drawOval((int) (c3d.x - pixs), (int) (c3d.y - pixs), (int) pixs * 2, (int) pixs * 2);
		}
		
		// draw the experiment plan.
		drawExperimentData(g, generalProjector);
		
		return r;
	}
	
	PamSymbol experimentSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 4, 4, true, Color.RED, Color.RED);
	private Rectangle drawExperimentData(Graphics g, GeneralProjector generalProjector) {

		BeakedExperimentData expData = beakedControl.currentExperiment;
		if (expData == null) {
			return null;
		}
		Coordinate3d vesselStart, trackStart, trackEnd;
		vesselStart = generalProjector.getCoord3d(expData.vesselStart.getLatitude(),expData.vesselStart.getLongitude(), 0);
		trackStart = generalProjector.getCoord3d(expData.trackStart.getLatitude(),expData.trackStart.getLongitude(), 0);
		trackEnd = generalProjector.getCoord3d(expData.trackEnd.getLatitude(),expData.trackEnd.getLongitude(), 0);
		
		g.setColor(experimentSymbol.getFillColor());
		g.drawLine((int) vesselStart.x, (int) vesselStart.y, (int) trackStart.x, (int) trackStart.y);
		g.drawLine((int) trackStart.x, (int) trackStart.y, (int) trackEnd.x, (int) trackEnd.y);
		experimentSymbol.draw(g, trackStart.getXYPoint());
		experimentSymbol.draw(g, trackEnd.getXYPoint());
		generalProjector.addHoverData(trackStart, expData);
		generalProjector.addHoverData(trackEnd, expData);
		
		return null;
	}
	
	private boolean isLastLocation(PamDataUnit pamDataUnit) {
		BeakedDataBlock bdb = beakedControl.beakedProcess.beakedDataBlock;
		PamDataUnit last = bdb.getLastUnit();
		return (last == pamDataUnit);
	}
	
	private boolean isCurrentExperiment(PamDataUnit pamDataUnit) {
		BeakedExperimentData bed = beakedControl.currentExperiment;
		if (bed == null) {
			return false;
		}
		return (bed.experimentLocationData == pamDataUnit);
	}

	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
		if (dataUnit.getClass() == BeakedDataUnit.class) {
			return getLocationHoverText(generalProjector, dataUnit, iSide);
		}
		if (dataUnit.getClass() == BeakedExperimentData.class) {
			return getExperimentHoverText(generalProjector, dataUnit, iSide);
		}
		return null;
	}
	private String getLocationHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
		BeakedDataUnit beakedDataUnit = (BeakedDataUnit) dataUnit;
		String str = "<html>";
		if (isLastLocation(dataUnit)) {
			str +=  "Latest Beaked Whale Location";
		}
		else {
			str +=  "Earlier Beaked Whale Location";
		}
		if (isCurrentExperiment(dataUnit)) {
			str += "<br>Current experiment whale location";
		}
		str += String.format("<br> Time: %s", PamCalendar.formatTime(dataUnit.getTimeMilliseconds()));
		str += String.format("<br>%s,  %s", beakedDataUnit.getBeakedLatLong().formatLatitude(), 
				beakedDataUnit.getBeakedLatLong().formatLongitude());
		str += String.format("<br>Range and bearing from shore station<br>%.1f m,  %.1f\u00B0T", 
			beakedDataUnit.getBeakedLocationData().range, beakedDataUnit.getBeakedLocationData().bearing);
		str += "<br>" + beakedDataUnit.getLocationName();
		String comment = beakedDataUnit.getBeakedComment();
		if (comment != null && comment.length() > 0) {
			str += "<br>" + comment;
		}
		if (beakedDataUnit.getBeakedLocationData().videoData != null) {
			str += "<br><br>" + beakedDataUnit.getBeakedLocationData().videoData.getHoverText();
		}
		str += "</html>";
		return str;
	}

	private String getExperimentHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
		return "Beaked whale experiment waypoint";
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
