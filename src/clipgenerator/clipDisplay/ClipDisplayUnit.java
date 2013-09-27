package clipgenerator.clipDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import PamUtils.FrequencyFormat;
import PamUtils.PamCalendar;
import PamView.PamColors;
import PamView.PamPanel;
import PamView.PamColors.PamColor;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import Spectrogram.DirectDrawProjector;

import clipgenerator.ClipDataUnit;

/**
 * Graphic component for a single clip display. 
 *
 *
 * @author Doug Gillespie
 *
 */
public class ClipDisplayUnit extends PamPanel {
	/*
	 * will need at least three nested panels with different borders to allow for the colouring 
	 * and things we'll want to do. 
	 */

	private ClipDisplayPanel clipDisplayPanel;
	private ClipDataUnit clipDataUnit;
	private PamDataUnit triggerDataUnit; // data unit that triggered the clip. 
//	private JPanel clipPanel;
	private JPanel axisPanel;
	private JPanel imagePanel;
	private BufferedImage image;
	private int fontHeight;
	private int borderSize;
	private int fontAscent;
	private FontMetrics fm; 
	
	public ClipDisplayUnit(ClipDisplayPanel clipDisplayPanel,
			ClipDataUnit clipDataUnit, PamDataUnit triggerDataUnit) {
		super();
		this.clipDisplayPanel = clipDisplayPanel;
		this.clipDataUnit = clipDataUnit;
		this.triggerDataUnit = triggerDataUnit;
		setLayout(new BorderLayout());
		axisPanel = new ClipAxisPanel(new BorderLayout());
		imagePanel = new ImagePanel(new BorderLayout());
		this.add(BorderLayout.CENTER, axisPanel);
		axisPanel.add(BorderLayout.CENTER, imagePanel);
		imagePanel.setBackground(Color.RED);
		if (triggerDataUnit != null) {
			imagePanel.setToolTipText(triggerDataUnit.getSummaryString());
		}
		
		this.setBorder(new EmptyBorder(1,1,1,1));
		this.setBackground(Color.black);
		
//		axisPanel.setFont(this.clipDisplayPanel.clipFont);
		fm = axisPanel.getFontMetrics(axisPanel.getFont());
		fontHeight = fm.getAscent(); // the numbers used dont' have any decent !
		borderSize = fontHeight + 1;
		fontAscent = fm.getAscent();
		axisPanel.setBorder(new EmptyBorder(borderSize, borderSize, borderSize, 2));
		
		createImage();
	}
	
	public void redrawUnit(boolean needNewImage) {
		// get a new image and call repaint. 
		if (needNewImage) {
			createImage();
		}
//		.invalidate();
		repaint(100);
	}
	
	private void createImage() {
		ClipDisplayParameters clipParams = clipDisplayPanel.clipDisplayParameters;
		int fftLen = 1<<clipParams.logFFTLength;
		image = clipDataUnit.getClipImage(0, fftLen, fftLen/2, clipParams.amlitudeMinVal, 
				clipParams.amlitudeMinVal + clipParams.amplitudeRangeVal, clipDisplayPanel.getColourArray().getColours());
		if (image != null) {
			imagePanel.setPreferredSize(new Dimension((int) (image.getWidth()*clipParams.imageHScale), 
					(int) (image.getHeight()*clipParams.imageVScale)));
		}
		else {
			imagePanel.setPreferredSize(new Dimension(60,60));
		}
		imagePanel.invalidate();
	}

	
	private class ClipAxisPanel extends PamPanel {

		public ClipAxisPanel(LayoutManager layout) {
			super(layout);
//			setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
		}
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			String tStr = PamCalendar.formatTime(clipDataUnit.getTimeMilliseconds(), true);
			g.drawString(tStr, borderSize, fontAscent);
			
			
			String lenString = String.format("%3.2fs", (float)clipDataUnit.getDuration() / clipDisplayPanel.getSampleRate());
			Rectangle2D strSize = fm.getStringBounds(lenString, g);
			g.drawString(lenString, (int) (getWidth()-strSize.getWidth()), getHeight()-fm.getDescent());

			double f = clipDisplayPanel.getSampleRate()/2.*clipDisplayPanel.clipDisplayParameters.frequencyScale;
			String fStr = FrequencyFormat.formatFrequency(f, true);
			strSize = fm.getStringBounds(fStr, g);
			int x = fontAscent;
			int y = (int) (strSize.getWidth() + borderSize);
			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(x, y);
			g2d.rotate(-Math.PI/2.);
			g2d.drawString(fStr, 0,0);
			g2d.rotate(+Math.PI/2.);
			g2d.translate(-x, -y);
		}
		
	}
	
	private class ImagePanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ImagePanel(BorderLayout borderLayout) {
			super(borderLayout);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (image != null) {
				int imageYStart = 0;
				double miss = (1.-clipDisplayPanel.clipDisplayParameters.frequencyScale);
				imageYStart = (int) (image.getHeight() * miss);
				imageYStart = Math.max(imageYStart, 1);
				g.drawImage(image, 0, 0, getWidth(), getHeight(), 0, imageYStart, image.getWidth(), image.getHeight(), null);
				if (clipDisplayPanel.clipDisplayParameters.showTriggerOverlay) {
					drawTriggerDataUnit(g);
				}
//				return;
			}
			else {
				String str = "no image";
				int x = (getWidth()-getFontMetrics(getFont()).stringWidth(str)) / 2;
				g.drawString(str, x, getHeight()/2);
			}
		}

		private void drawTriggerDataUnit(Graphics g) {
			if (triggerDataUnit == null) {
				return;
			}
			ClipDataProjector proj = clipDisplayPanel.getClipDataProjector();
			PamDataBlock dataBlock = triggerDataUnit.getParentDataBlock();
			if (dataBlock == null || dataBlock.canDraw(proj) == false) {
				return;
			}
			proj.setClipStart(clipDataUnit.getTimeMilliseconds());
			dataBlock.drawDataUnit(g, triggerDataUnit, proj);
			
		}
		
		
	}

	/**
	 * @return the clipDataUnit
	 */
	public ClipDataUnit getClipDataUnit() {
		return clipDataUnit;
	}
	
}
