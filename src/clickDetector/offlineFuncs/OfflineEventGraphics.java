package clickDetector.offlineFuncs;

import java.awt.Graphics;
import java.awt.Rectangle;

import PamView.GeneralProjector;
import PamView.PamDetectionOverlayGraphics;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class OfflineEventGraphics extends PamDetectionOverlayGraphics {

	public OfflineEventGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock);
	}

	/* (non-Javadoc)
	 * @see PamView.PamDetectionOverlayGraphics#drawOnMap(java.awt.Graphics, PamguardMVC.PamDataUnit, PamView.GeneralProjector)
	 */
	@Override
	protected Rectangle drawOnMap(Graphics g, PamDataUnit pamDetection,
			GeneralProjector generalProjector) {
		// TODO Auto-generated method stub
		return super.drawOnMap(g, pamDetection, generalProjector);
	}

}
