package clickDetector;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamView.GeneralProjector;
import PamView.PamColors;
import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class ClickTrainLocalisationGraphics extends PamDetectionOverlayGraphics {

	ClickControl clickControl;
	
	public ClickTrainLocalisationGraphics(ClickControl clickControl, PamDataBlock parentDataBlock) {
		
		super(parentDataBlock);
		this.clickControl= clickControl;
		setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 15, 15, false, Color.BLACK, Color.BLACK));
		
	}

	@Override
	protected Rectangle drawOnMap(Graphics g, PamDataUnit pamDetection, GeneralProjector generalProjector) {
		ClickTrainDetection clickTrain = (ClickTrainDetection) pamDetection;
		if (clickTrain.isHasFit()) {
			return drawFixOnMap(g, clickTrain, generalProjector);
		}
		else {
			return drawBearingOnMap(g, clickTrain, generalProjector);
		}
	}
	// copied from TrackedClickLocalisationGraphics - very bad !
	protected Rectangle drawFixOnMap(Graphics g, ClickTrainDetection clickTrain, GeneralProjector generalProjector) {
		// TODO Auto-generated method stub
		// get the origin and the locations and draw lines between them
		LatLong originLatLong;
		Coordinate3d origin;
		LatLong latLong;
		Coordinate3d llCoord, errorCoord;
//		Rectangle r = originSymbol.draw(g, origin.getXYPoint());
		double parallelError, perpendicularError;
		LatLong errLatLong;
		double pixsPerMeter;
		double referenceHeading;
		double sinRH, cosRH;
		Rectangle r = null;
		for (int iSide = 0; iSide <= 1; iSide ++) {
			originLatLong = clickTrain.getOriginLatLong(iSide);
			if (originLatLong == null) continue;
			origin = generalProjector.getCoord3d(originLatLong.getLatitude(), originLatLong.getLongitude(), 0);
			latLong = clickTrain.getLatLong(iSide);
			if (latLong == null) continue;
			getPamSymbol().setLineColor(PamColors.getInstance().getWhaleColor(clickTrain.getEventId()));
			llCoord = generalProjector.getCoord3d(latLong.getLatitude(), latLong.getLongitude(), 0);
			if (r == null) {
				r = getPamSymbol().draw(g, llCoord.getXYPoint());
			}
			else  {
				r = r.union(getPamSymbol().draw(g, llCoord.getXYPoint()));
			}
			parallelError = clickTrain.getParallelError(iSide);
			perpendicularError = clickTrain.getPerpendiculaError(iSide);	
			errLatLong = latLong.travelDistanceMeters(0, 1000);
			errorCoord = generalProjector.getCoord3d(errLatLong.getLatitude(), errLatLong.getLongitude(), 0);
			pixsPerMeter = Math.abs((errorCoord.y - llCoord.y) / 1000.);
			parallelError *= pixsPerMeter;
			perpendicularError *= pixsPerMeter;
//			 draw error lines across the symbol. 
			referenceHeading = (Math.PI / 2 - clickTrain.getReferenceHeading(iSide));
			sinRH = Math.sin(referenceHeading);
			cosRH = Math.cos(referenceHeading);
//			perpendicularError = 500;
//			g.setColor(Color.RED);
			g.drawLine((int)(llCoord.x-cosRH*parallelError), (int)(llCoord.y+sinRH*parallelError), 
					(int)(llCoord.x+cosRH*parallelError), (int)(llCoord.y-sinRH*parallelError));
//			g.setColor(Color.RED);
			g.drawLine((int)(llCoord.x+sinRH*perpendicularError), (int)(llCoord.y+cosRH*perpendicularError), 
					(int)(llCoord.x-sinRH*perpendicularError), (int)(llCoord.y-cosRH*perpendicularError));
			
		}
		return r;
	}
	protected Rectangle drawBearingOnMap(Graphics g, ClickTrainDetection clickTrain, GeneralProjector generalProjector) {
		
		Rectangle r = null;
		// draw the bearing from the last click.
		ClickDetection lastClick = clickTrain.getSubDetection(clickTrain.getSubDetectionsCount()-1);
		
		return super.drawOnMap(g, lastClick, generalProjector);
	}
//	@Override
//	protected Rectangle drawOnMap(Graphics g, PamDataUnit pamDetection, GeneralProjector generalProjector) {
//
//		// need to fudge from ClickTrainGRaphics to deal with the very odd data formats in 
//		// click train detection. 
//		// Just plot a single point
//		ClickTrainDetection clickTrain = (ClickTrainDetection) pamDetection;
//
//		int nClicks = clickTrain.getSubDetectionsCount();
//		//int middleClick = Math.max(1, Math.min(nClicks-1, nClicks / 2));
//		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
//
//		ClickDetection firstClickUnit = clickTrain.getSubDetection(0);
//		ClickDetection lastClickUnit = clickTrain.getSubDetection(nClicks-1);
//		int channels = clickTrain.getChannelBitmap();
//		int firstChannel = PamUtils.getLowestChannel(channels);
//		int lastChannel = PamUtils.getHighestChannel(channels);
//		int phone1 = array.getHydrophone(firstChannel).getID();
//		int phone2 = array.getHydrophone(lastChannel).getID();
//		LatLong firstLatLong = array.getHydrophoneLocator().getPhoneLatLong(firstClickUnit.getTimeMilliseconds(), phone1);
//		LatLong lastLatLong = array.getHydrophoneLocator().getPhoneLatLong(lastClickUnit.getTimeMilliseconds(), phone1);
//		double firstHeading = array.getHydrophoneLocator().getPairAngle(firstClickUnit.getTimeMilliseconds(), 
//				phone2, phone1, HydrophoneLocator.ANGLE_RE_NORTH);		
//		double lastHeading = array.getHydrophoneLocator().getPairAngle(lastClickUnit.getTimeMilliseconds(), 
//						phone2, phone1, HydrophoneLocator.ANGLE_RE_NORTH);
//		double distanceTravelled = firstLatLong.distanceToMetres(lastLatLong);
//		double speed = distanceTravelled / (lastClickUnit.getTimeMilliseconds() - firstClickUnit.getTimeMilliseconds()) * 1000;
//		if (distanceTravelled <= 0) return null;
//		double perpDist = clickTrain.getPerpendiculaError(0);
//		double msFromFirstClick = 0;//clickTrain.eT0;
//		double msFromLastClick = msFromFirstClick - (lastClickUnit.getTimeMilliseconds() - firstClickUnit.getTimeMilliseconds());
//		double firstAngle = Math.atan2(perpDist, msFromFirstClick / 1000. * speed) * 180/Math.PI;
//		double lastAngle = Math.atan2(perpDist, msFromLastClick / 1000. * speed) * 180/Math.PI;
////		double firstHeading = array.getHydrophoneLocator().getPairAngle(firstClickUnit.getTimeMilliseconds(), 
////				1, 0, HydrophoneLocator.ANGLE_RE_NORTH);
////		double lastHeading = array.getHydrophoneLocator().getPairAngle(lastClickUnit.getTimeMilliseconds(), 
////				1, 0, HydrophoneLocator.ANGLE_RE_NORTH);
//		double firstRange = clickControl.clickParameters.defaultRange;
//		double lastRange = firstRange;
//		if (clickTrain.isHasFit()) {
//			firstRange = Math.sqrt(Math.pow(perpDist,2) + Math.pow(msFromFirstClick / 1000. * speed,2));
//			lastRange = Math.sqrt(Math.pow(perpDist,2) + Math.pow(msFromLastClick / 1000. * speed,2));
//		}
//		// now a load of stuff copied from the Click Projector.
//		// shoudl really combine some of this functionality !
//
//		Rectangle r;
//
////		Graphics2D g2 = (Graphics2D) g;
////		g2.setStroke(new BasicStroke(1));
////		g2.setColor(PamColors.getInstance().getWhaleColor(clickTrain.getTrainId()));
////		r = drawBearingLines (g, generalProjector, clickTrain, firstLatLong, firstHeading, firstAngle, firstRange);
////		r.add(drawBearingLines (g, generalProjector, clickTrain, lastLatLong, lastHeading, lastAngle, lastRange));
//		
//		getPamSymbol().setLineColor(PamColors.getInstance().getWhaleColor(firstClickUnit.eventId));
//		LatLong pos = lastLatLong.travelDistanceMeters(lastHeading + lastAngle, lastRange);
//		Coordinate3d c3d = generalProjector.getCoord3d(pos.getLatitude(), pos.getLongitude(), 0);
//		r = getPamSymbol().draw(g, c3d.getXYPoint());
//		pos = lastLatLong.travelDistanceMeters(lastHeading - lastAngle, lastRange);
//		c3d = generalProjector.getCoord3d(pos.getLatitude(), pos.getLongitude(), 0);
//		r.add(getPamSymbol().draw(g, c3d.getXYPoint()));
//		
//		return r;
//	}

}
