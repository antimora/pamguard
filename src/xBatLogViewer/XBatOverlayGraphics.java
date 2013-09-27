package xBatLogViewer;

import java.awt.Graphics;
import java.awt.Rectangle;

import PamUtils.Coordinate3d;
import PamView.GeneralProjector;
import PamView.PamDetectionOverlayGraphics;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class XBatOverlayGraphics extends PamDetectionOverlayGraphics {

	public XBatOverlayGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock);
		
	}

	@Override
	protected Rectangle drawOnSpectrogram(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {
		// draw a rectangle with time and frequency bounds of detection.
		// spectrogram projector is now updated to use Hz instead of bins. 
		XBatLogDataUnit pamDetection = (XBatLogDataUnit) pamDataUnit;
		double[] frequency = pamDetection.getFrequency();
		Coordinate3d topLeft = generalProjector.getCoord3d(pamDetection.getTimeMilliseconds(), 
				frequency[1], 0);
		Coordinate3d botRight = generalProjector.getCoord3d(pamDetection.getTimeMilliseconds() + 
				pamDetection.getDurationMillis(),
				frequency[0], 0);
//		int channel = PamUtils.getSingleChannel(pamDetection.getChannelBitmap());
//		if (channel >= 0) {
//			g.setColor(PamColors.getInstance().getChannelColor(channel));
//		}
//		g.setColor(lineColour);
		if (generalProjector.isViewer()) {
			Coordinate3d middle = new Coordinate3d();
			middle.x = (topLeft.x + botRight.x)/2;
			middle.y = (topLeft.y + botRight.y)/2;
			middle.z = (topLeft.z + botRight.z)/2;
			generalProjector.addHoverData(middle, pamDataUnit);
		}

		g.drawRect((int) topLeft.x, (int) topLeft.y, 
				(int) botRight.x - (int) topLeft.x, (int) botRight.y - (int) topLeft.y);
		return null;
	}

}
