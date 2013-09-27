package ishmaelComms;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import Array.ArrayManager;
import Array.HydrophoneLocator;
import Array.PamArray;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamView.GeneralProjector;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PanelOverlayDraw;
import PamView.GeneralProjector.ParameterType;
import PamguardMVC.PamDataUnit;

public class IshmaelGraphics implements PanelOverlayDraw{
	
	IshmaelDataControl ishmaelDataControl;
	
	static private final float lineLength = 1000; // meters

	public IshmaelGraphics(IshmaelDataControl ishmaelDataControl) {
		super();
		this.ishmaelDataControl = ishmaelDataControl;
	}

	public boolean canDraw(GeneralProjector generalProjector) {
		if (generalProjector.getParmeterType(0) == ParameterType.LONGITUDE
				&& generalProjector.getParmeterType(1) == ParameterType.LATITUDE)
			return true;
		if (generalProjector.getParmeterType(0) == ParameterType.TIME
				&& generalProjector.getParmeterType(1) == ParameterType.FREQUENCY)
			return true;
		return false;
	}

	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		IshmaelDataUnit ishmaelDataUnit = (IshmaelDataUnit) pamDataUnit;
		if (generalProjector.getParmeterType(0) == ParameterType.LONGITUDE
				&& generalProjector.getParmeterType(1) == ParameterType.LATITUDE)
			return drawOnMap(g, ishmaelDataUnit, generalProjector);
		if (generalProjector.getParmeterType(0) == ParameterType.TIME
				&& generalProjector.getParmeterType(1) == ParameterType.FREQUENCY)
			return drawOnSpectrogram(g, ishmaelDataUnit, generalProjector);
		return null;
	}

	private Rectangle drawOnMap(Graphics g, IshmaelDataUnit pamDataUnit, GeneralProjector generalProjector) {
		IshmaelData ishmaelData = pamDataUnit.getIshmaelData();
		if(ishmaelData.ishmaelDataType == IshmaelData.IshmaelDataType.NEWBEARING) {
			drawAngleOnMap(g, pamDataUnit, generalProjector);
		}
		else if(ishmaelData.ishmaelDataType == IshmaelData.IshmaelDataType.NEWPOSITION) {
			drawPositionOnMap(g, pamDataUnit, generalProjector);
		}
		return null;
	}

	private Rectangle drawAngleOnMap(Graphics g, IshmaelDataUnit pamDataUnit, GeneralProjector generalProjector) {
		IshmaelData ishmaelData =  pamDataUnit.getIshmaelData();

		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		LatLong hLatLong = array.getHydrophoneLocator().getPhoneLatLong(pamDataUnit.getTimeMilliseconds(), 0);
		double heading = array.getHydrophoneLocator().getPairAngle(pamDataUnit.getTimeMilliseconds(), 
				1, 0, HydrophoneLocator.ANGLE_RE_NORTH);
		
		Graphics2D g2 = (Graphics2D) g;
		
		Coordinate3d coord = generalProjector.getCoord3d(hLatLong.getLatitude(),
				hLatLong.getLongitude(), 0);
		
		Rectangle r = null;

		
		double angle = ishmaelData.angle;

		g2.setColor(new Color(255, 0, 0));
		
		LatLong latlong = new LatLong(hLatLong.getLatitude(),
				hLatLong.getLongitude());
		
		LatLong lineEnd = latlong.travelDistanceMeters(
				heading + angle, lineLength);
		Point origin = new Point((int) coord.x, (int) coord.y);
		coord = generalProjector.getCoord3d(lineEnd.getLatitude(), lineEnd.getLongitude(), 0);
		g2.setStroke(new BasicStroke(1));
		g.drawLine(origin.x, origin.y, (int) coord.x, (int) coord.y);
//		r = new Rectangle()
		
		if (ishmaelData.ambig == 1) {
			lineEnd = latlong.travelDistanceMeters(
					heading - angle, lineLength);
			coord = generalProjector.getCoord3d(lineEnd.getLatitude(), lineEnd.getLongitude(), 0);
			g.drawLine(origin.x, origin.y, (int) coord.x, (int) coord.y);
		}
		return r;
	}
	
	

	private Rectangle drawPositionOnMap(Graphics g, IshmaelDataUnit pamDataUnit, GeneralProjector generalProjector) {

		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		LatLong hLatLong = array.getHydrophoneLocator().getPhoneLatLong(pamDataUnit.getTimeMilliseconds(), 0);
		double heading = array.getHydrophoneLocator().getPairAngle(pamDataUnit.getTimeMilliseconds(), 
				1, 0, HydrophoneLocator.ANGLE_RE_NORTH);
		IshmaelData ishmaelData = pamDataUnit.getIshmaelData();
		if (hLatLong == null) return null;
		
//		Graphics2D g2 = (Graphics2D) g;
		
		Coordinate3d origin = generalProjector.getCoord3d(hLatLong.getLatitude(),
				hLatLong.getLongitude(), 0);

		LatLong latlong = new LatLong(hLatLong.getLatitude(),
				hLatLong.getLongitude());
		
		// calculate angle to position relative to the ship
		double angle = Math.atan2(ishmaelData.ypos, ishmaelData.xpos);
		
		// calculate the distance
		double distance = Math.sqrt(ishmaelData.ypos*ishmaelData.ypos + ishmaelData.xpos * ishmaelData.xpos);
		
		distance = Math.min(distance, 200000);


		LatLong position = latlong.travelDistanceMeters(
				heading + angle, distance);
		
		Rectangle r = null;
		
		PamSymbol symbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 10, 10, true, Color.BLACK, Color.BLACK);

		Coordinate3d pos = generalProjector.getCoord3d(position.getLatitude(), position.getLongitude(), 0);
		
		symbol.draw(g, new Point((int) pos.x, (int) pos.y));
		
		Graphics2D g2 = (Graphics2D) g;

		float[] dashes = {2, 6};
		g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, dashes, 0));
		g2.setColor(new Color(255, 255, 255));
		g.drawLine((int) origin.x, (int) origin.y, (int) pos.x, (int) pos.y);
				
		return r;
	}

	private Rectangle drawOnSpectrogram(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		return null;
	}

	public PamKeyItem createKeyItem(GeneralProjector generalProjector, int keyType) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getHoverText(GeneralProjector generalProjector, PamDataUnit dataUnit, int iSide) {
		// TODO Auto-generated method stub
		return null;
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
