package beakedWhaleProtocol;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;

import PamUtils.Coordinate3d;
import PamUtils.PamCalendar;
import PamView.GeneralProjector;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PanelOverlayDraw;
import PamguardMVC.PamDataUnit;

public class BeakedExperimentGraphics implements  PanelOverlayDraw  {

	BeakedControl beakedControl;
	
	PamSymbol experimentSymbol = new PamSymbol(PamSymbol.SYMBOL_HEXAGRAM, 10, 10, true, Color.ORANGE, Color.ORANGE);

	public BeakedExperimentGraphics(BeakedControl beakedControl) {
		this.beakedControl = beakedControl;
	}

	public boolean canDraw(GeneralProjector generalProjector) {
//		if (true) return true;
		return (generalProjector.getParmeterType(0) == GeneralProjector.ParameterType.LONGITUDE && 
				generalProjector.getParmeterType(1) == GeneralProjector.ParameterType.LATITUDE);
	}

	public PamKeyItem createKeyItem(GeneralProjector generalProjector,
			int keyType) {
		return experimentSymbol.makeKeyItem("Old beaked whale experiments");
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

		BeakedExperimentData expData = (BeakedExperimentData) pamDataUnit;
		if (expData == null) {
			return null;
		}
		Coordinate3d vesselStart, trackStart, trackEnd;
//		if (expData.vesselStart != null) {
//			vesselStart = generalProjector.getCoord3d(expData.vesselStart.getLatitude(),expData.vesselStart.getLongitude(), 0);
//		}
		trackStart = generalProjector.getCoord3d(expData.trackStart.getLatitude(),expData.trackStart.getLongitude(), 0);
		trackEnd = generalProjector.getCoord3d(expData.trackEnd.getLatitude(),expData.trackEnd.getLongitude(), 0);
		
		g.setColor(experimentSymbol.getFillColor());
//		g.drawLine((int) vesselStart.x, (int) vesselStart.y, (int) trackStart.x, (int) trackStart.y);
		g.drawLine((int) trackStart.x, (int) trackStart.y, (int) trackEnd.x, (int) trackEnd.y);
		experimentSymbol.draw(g, trackStart.getXYPoint());
		experimentSymbol.draw(g, trackEnd.getXYPoint());
		generalProjector.addHoverData(trackStart, expData, -1);
		generalProjector.addHoverData(trackEnd, expData, +1);
		
		return null;
	}
	
	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
		BeakedExperimentData expData = (BeakedExperimentData) dataUnit;

		String str = "<html>Old beaked whale experiment";
		if (iSide == -1) {
			str += " start point";
		}
		else if (iSide == +1) {
			str += " end point";
		}
		str += "<br> Start " + PamCalendar.formatTime(expData.getTimeMilliseconds()); 
		str += "; End   " + PamCalendar.formatTime(expData.endTime); 
		str += "<br>Perp distance " + String.format("%d m", (int) expData.perpDistance);
		if (expData.comment != null && expData.comment.length() > 0) {
			str += "<br>" + expData.comment;
		}

		str += "</html>";
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
