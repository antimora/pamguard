package clickDetector;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import Localiser.bearingLocaliser.GroupDetection;
import PamDetection.PamDetection;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamView.GeneralProjector;
import PamView.PamColors;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PanelOverlayKeyItem;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

@Deprecated
public class ClickGroupGraphics extends	NewClickOverlayGraphics {
	
	PamSymbol originSymbol;

	public ClickGroupGraphics(ClickControl clickControl, PamDataBlock parentDataBlock, int drawTypes, String name) {
		super(clickControl, parentDataBlock, drawTypes, name);
		setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 20, 20, false, Color.BLUE, Color.BLUE));
		originSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 5, 5, true, Color.RED, Color.BLUE);
	}

	@Override
	protected PamKeyItem createMapKey(int keyType) {
		return new PanelOverlayKeyItem(this);
//		return super.createMapKey(keyType);
	}
	
	protected boolean shouldPlot(PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		return true;
	}


	@Override
	protected boolean canDrawOnMap() {
		return true;
	}
	
	@Override
	protected Rectangle drawOnMap(Graphics g, PamDataUnit pamDetection, GeneralProjector generalProjector) {
		if (!shouldPlot(pamDetection, generalProjector)) {
			return null;
		}
		GroupDetection clickTrain = (GroupDetection) pamDetection;
		if (clickTrain.isHasFit()) {
			return drawFixOnMap(g, clickTrain, generalProjector);
		}
		else {
			return drawBearingOnMap(g, clickTrain, generalProjector);
		}
	}

	protected Rectangle drawFixOnMap(Graphics g, PamDataUnit pamDetection, GeneralProjector generalProjector) {
		// TODO Auto-generated method stub
		GroupDetection tcg = (GroupDetection) pamDetection;
		// get the origin and the locations and draw lines between them
		LatLong originLatLong = tcg.getOriginLatLong(0);
		if (originLatLong == null) return null;
		Coordinate3d origin = generalProjector.getCoord3d(originLatLong.getLatitude(), originLatLong.getLongitude(), 0);
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
			latLong = tcg.getLatLong(iSide);
			if (latLong == null) continue;
			getPamSymbol().setLineColor(PamColors.getInstance().getWhaleColor(tcg.getEventId()));
			llCoord = generalProjector.getCoord3d(latLong.getLatitude(), latLong.getLongitude(), 0);
			if (r == null) {
				r = getPamSymbol().draw(g, llCoord.getXYPoint());
			}
			else  {
				r = r.union(getPamSymbol().draw(g, llCoord.getXYPoint()));
			}
			parallelError = tcg.getParallelError(iSide);
			perpendicularError = tcg.getPerpendiculaError(iSide);	
			errLatLong = latLong.travelDistanceMeters(0, 1000);
			errorCoord = generalProjector.getCoord3d(errLatLong.getLatitude(), errLatLong.getLongitude(), 0);
			pixsPerMeter = Math.abs((errorCoord.y - llCoord.y) / 1000.);
			parallelError *= pixsPerMeter;
			perpendicularError *= pixsPerMeter;
//			 draw error lines across the symbol. 
			referenceHeading = (Math.PI / 2 - tcg.getReferenceHeading(iSide));
//			MapProjector mapProjector = (MapProjector) generalProjector;
//			referenceHeading += mapProjector.get
			sinRH = Math.sin(referenceHeading);
			cosRH = Math.cos(referenceHeading);
			g.drawLine((int)(llCoord.x-cosRH*parallelError), (int)(llCoord.y+sinRH*parallelError), 
					(int)(llCoord.x+cosRH*parallelError), (int)(llCoord.y-sinRH*parallelError));
			g.drawLine((int)(llCoord.x+sinRH*perpendicularError), (int)(llCoord.y+cosRH*perpendicularError), 
					(int)(llCoord.x-sinRH*perpendicularError), (int)(llCoord.y-cosRH*perpendicularError));
			
		}
		return r;
	}
	protected Rectangle drawBearingOnMap(Graphics g, GroupDetection clickGroup, GeneralProjector generalProjector) {
		
		Rectangle r = null;
		// draw the bearing from the last click.
		PamDetection lastClick = clickGroup.getSubDetection(clickGroup.getSubDetectionsCount()-1);
		
		return super.drawOnMap(g, lastClick, generalProjector);
	}
	
}
