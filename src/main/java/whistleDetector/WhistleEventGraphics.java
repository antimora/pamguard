package whistleDetector;

import java.awt.Color;
import PamView.GeneralProjector;
import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamView.GeneralProjector.ParameterType;

public class WhistleEventGraphics extends PamDetectionOverlayGraphics {

	PamSymbol mapSymbol = new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 10, 10, true,
			Color.BLUE, Color.BLUE);
	
	WhistleEventDetector whistleEventDetector;
	
	public WhistleEventGraphics(WhistleEventDetector whistleEventDetector) {
		super(whistleEventDetector.getOutputDataBlock(0));
		this.whistleEventDetector = whistleEventDetector;
		if (getPamSymbol() == null) {
			PamSymbol mapSymbol = new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 10, 10, true,
					Color.BLUE, Color.BLUE);
			setPamSymbol(mapSymbol);
		}
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canDraw(GeneralProjector generalProjector) {
		if (generalProjector.getParmeterType(0) == ParameterType.LONGITUDE
				&& generalProjector.getParmeterType(1) == ParameterType.LATITUDE){
			return true;
		}
		return false;
	}

//	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
//		if (!canDraw(generalProjector)) return null;
//
//		Rectangle r = new Rectangle();
//		Graphics2D g2 = (Graphics2D) g;
//		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
//		LatLong latLong = array.getHydrophoneLocator().getPhoneLatLong(pamDataUnit.getTimeMilliseconds(), 0);
//		Coordinate3d coord = generalProjector.getCoord3d(latLong.getLatitude(),
//				latLong.getLongitude(), 0);
//
//		mapSymbol.draw(g2, coord.getXYPoint());
//
//		return r;
//	}
//
//	public PamKeyItem createKeyItem(GeneralProjector generalProjector, int keyType) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	public String getHoverText(GeneralProjector generalProjector, PamDataUnit dataUnit) {
//		String str =  "<html>Whistle Event";
//		str += String.format("<br> Channels: %s", PamUtils.getChannelList(dataUnit.getChannelBitmap()));
//		str += "</html>";
//		return str;
//	}
}
