package clipgenerator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;

import fftManager.FFTDataBlock;

import PamController.PamController;
import PamView.GeneralProjector;
import PamView.PamKeyItem;
import PamView.PanelOverlayDraw;
import PamView.GeneralProjector.ParameterType;
import PamguardMVC.PamDataUnit;
import Spectrogram.DirectDrawProjector;
import Spectrogram.SpectrogramDisplay;
import Spectrogram.SpectrogramParameters;

public class ClipOverlayGraphics implements PanelOverlayDraw {

	private ClipControl clipControl;

	private boolean isViewer;

	private boolean isNetReceiver;

	/**
	 * @param clipControl
	 */
	public ClipOverlayGraphics(ClipControl clipControl) {
		super();
		this.clipControl = clipControl;
		isViewer = (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW);
		isNetReceiver = (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER);
	}

	@Override
	public boolean canDraw(GeneralProjector generalProjector) {
		if (generalProjector.getParmeterType(0) == ParameterType.TIME
				&& generalProjector.getParmeterType(1) == ParameterType.FREQUENCY) {
			return true;
		}
		return false;
	}

	@Override
	public PamKeyItem createKeyItem(GeneralProjector generalProjector,
			int keyType) {
		return null;
	}

	@Override
	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {		
		if (generalProjector.getParmeterType(0) == ParameterType.TIME
				&& generalProjector.getParmeterType(1) == ParameterType.FREQUENCY) {
			if (isViewer || isNetReceiver) {
				return drawViewerMode(g, pamDataUnit, generalProjector);
			}
			else {
				return drawNormalMode(g, pamDataUnit, generalProjector);
			}
		}
		return null;
	}

	private Rectangle drawNormalMode(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {

		return null;
	}
	
	private Rectangle drawViewerMode(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector generalProjector) {
		/*
		 * Will really need to get some spectrogram and FFT parameters out
		 * of the projector if this drawing is going to work !
		 */
		DirectDrawProjector spectrogramProjector;
		SpectrogramDisplay spectrogramDisplay;
		FFTDataBlock fftDataBlock;
		try {
			spectrogramProjector = (DirectDrawProjector) generalProjector;
		}
		catch (ClassCastException e) {
			return null;
		}
		spectrogramDisplay = spectrogramProjector.getSpectrogramDisplay();
		spectrogramDisplay.getSpectrogramParameters();
		Color[] colorLUT = spectrogramDisplay.getColourArray().getColours();
		fftDataBlock = spectrogramDisplay.getSourceFFTDataBlock();
		SpectrogramParameters specParams = spectrogramDisplay.getSpectrogramParameters();
		int panelId = spectrogramProjector.getPanelId();
		int channel = specParams.channelList[panelId];
		if (fftDataBlock == null) {
			return null;
		}
		float sampleRate = clipControl.clipProcess.getSampleRate();
		ClipDataUnit clipDataUnit = (ClipDataUnit) pamDataUnit;
		BufferedImage image = clipDataUnit.getClipImage(channel, fftDataBlock.getFftLength(), fftDataBlock.getFftHop(), 
				specParams.amplitudeLimits[0], specParams.amplitudeLimits[1], colorLUT);
		if (image == null) {
			return null;
		}
		/*
		 * find the top left and bottom right corners of the image. 
		 */
		double lenMillis = (double) image.getWidth() * (double) fftDataBlock.getFftHop() * 1000. / sampleRate;
		long tStart= clipDataUnit.getTimeMilliseconds();
		Point topLeft = generalProjector.getCoord3d(tStart, sampleRate/2, 0).getXYPoint();
		Point botRight = generalProjector.getCoord3d(tStart+lenMillis, 0, 0).getXYPoint();
		if (botRight.x < topLeft.x){
			// display has wrapped around. Do something !!!!
			botRight.x += spectrogramProjector.getDisplayWidth();
		}
		
		
		g.drawImage(image, topLeft.x, topLeft.y, botRight.x-topLeft.x, botRight.y-topLeft.y, null);
		
		return new Rectangle(topLeft.x, topLeft.y, botRight.x-topLeft.x, botRight.y-topLeft.y);
	}

	@Override
	public String getHoverText(GeneralProjector generalProjector,
			PamDataUnit dataUnit, int iSide) {
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
