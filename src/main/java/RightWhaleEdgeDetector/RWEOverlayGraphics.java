package RightWhaleEdgeDetector;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import PamView.GeneralProjector;
import PamView.PamColors;
import PamView.PamDetectionOverlayGraphics;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class RWEOverlayGraphics extends PamDetectionOverlayGraphics {

	public RWEOverlayGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock);
	}

	@Override
	protected Rectangle drawOnSpectrogram(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {
		int type = ((RWEDataUnit) pamDataUnit).rweSound.soundType;
		Color col = PamColors.getInstance().getWhaleColor(type);
		g.setColor(col);
		Rectangle r = super.drawOnSpectrogram(g, pamDataUnit, generalProjector);
		if (r != null) {
			g.drawString(String.format("%d", type), r.x+r.width, r.y);
		}
		
		return r;
	}

	/* (non-Javadoc)
	 * @see PamView.PamDetectionOverlayGraphics#getHoverText(PamView.GeneralProjector, PamguardMVC.PamDataUnit, int)
	 */
	@Override
	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
		RWEDataUnit rweDataUnit = (RWEDataUnit) dataUnit;
		RWESound aSound = rweDataUnit.rweSound;
		
		String txt = super.getHoverText(generalProjector, dataUnit, iSide);
		// remove the </html> from the end
		int ht = txt.indexOf("</html>");
		if (ht > 0) {
			txt = txt.substring(0, ht);
		}
		txt += String.format("<br>Sound Type %d - %s", aSound.soundType, aSound.getTypeString());
		txt += "</html>";
		
		return txt;
	}

}
