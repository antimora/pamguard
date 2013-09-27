package IshmaelDetector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import Layout.DisplayPanel;
import Layout.DisplayPanelContainer;
import Layout.DisplayPanelProvider;
import Layout.DisplayProviderList;
import PamUtils.PamUtils;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

/**
 * IshDetGraphics displays a detection function from an Ishmael-type 
 * detector (EnergySum, MatchFilt, SgramCorr).  Detection function
 * units are of type double[], with minimum length 1 (this is about
 * the minimum you could require!).  
 * @author Dave Mellinger
 *
 */
public class IshDetGraphics implements DisplayPanelProvider
{
	IshDetControl ishDetControl;	//should produce vectors of length >= 1
	PamDataBlock ishDetDataBlock = null;
	boolean firstTime;				//true for the very first line draw

	public IshDetGraphics(IshDetControl ishDetControl, PamDataBlock ishDetDataBlock)
	{
		super();
		DisplayProviderList.addDisplayPanelProvider(this);
		this.ishDetControl   = ishDetControl;
		this.ishDetDataBlock = ishDetDataBlock;
	}

	public DisplayPanel createDisplayPanel(DisplayPanelContainer displayPanelContainer) {
		return new IshDisplayPanel(this, displayPanelContainer);
	}

	public String getDisplayPanelName() {
		return ishDetControl.getUnitName() + " graphics";
	}
	
	public void prepareForRun() {
		firstTime = true;
	}
    
	class IshDisplayPanel extends DisplayPanel implements PamObserver {
		int clearLastX;
		public class PerChannelInfo {
			int xAdvance;	
			int yLo, yHi;			//Y-bounds of my display region
			int lastX, lastY;		//where the last drawn line ended, in pixels
			float yScale;
			public PerChannelInfo(int yLo, int yHi, float yScale) {
				this.yLo = yLo;
				this.yHi = yHi;
				this.yScale = yScale;
				lastX = 0;
				xAdvance = -2;		//special case meaning "skip first 2 values"
				lastY = yHi;
			}
		}
		PerChannelInfo perChannelInfo[] = new PerChannelInfo[PamConstants.MAX_CHANNELS];

		@Override
		public PamObserver getObserverObject() {
			return this;
		}
		
		public IshDisplayPanel(DisplayPanelProvider displayPanelProvider, 
				DisplayPanelContainer displayPanelContainer)
		{
			super(displayPanelProvider, displayPanelContainer);
			if (ishDetDataBlock != null) {
				ishDetDataBlock.addObserver(this); //call my update() when unit added
			}
		}

		@Override
		public void containerNotification(DisplayPanelContainer displayContainer, 
				int noteType) 
		{
			// TODO Auto-generated method stub	
		}

		@Override
		public void destroyPanel() {
			if (ishDetDataBlock != null)
				ishDetDataBlock.deleteObserver(this);
		}

		public String getObserverName() {
			return displayPanelProvider.getDisplayPanelName();
		}

		public long getRequiredDataHistory(PamObservable o, Object arg) {
			return 0;
		}

		public void noteNewSettings() {	
		}

		public void removeObservable(PamObservable o) {
		}

		float sampleRate;
		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			// TODO Auto-generated method stub
			
		}

		public void setSampleRate(float sampleRate, boolean notify) {
			this.sampleRate = sampleRate;
		}
		
		public void update(PamObservable o, PamDataUnit dataUnit1) {
			IshDetFnDataUnit dataUnit = (IshDetFnDataUnit)dataUnit1; 
			//This is called whenever new data arrives from my parent.
			//PamDataBlock inputDataBlock = (PamDataBlock)o;
			IshDetParams p = ishDetControl.ishDetParams;
			double[] ishDetectorData = dataUnit.getDetData();
			
			//if (ishDetectorData.length != 1)
				//ishDetectorData[0] = 0;
			
			//int totalChannels = PamUtils.getNumChannels(dataBlock.getChannelMap());
			int chanIx = PamUtils.getSingleChannel(dataUnit.getChannelBitmap());

			BufferedImage image = getDisplayImage();
			if (image == null) return;

			int hi = getInnerHeight() - 1;
			int chansActive = ((PamDataBlock)o).getChannelMap();
			if (firstTime) {
				clearLastX = 0;
				int nChansActive = PamUtils.getNumChannels(chansActive);
				int chanN = 0;
				for (int i = 0; i < perChannelInfo.length; i++) {
					if ((chansActive & (1 << i)) != 0) {
						int yLo = Math.round((float)chanN       / (float)nChansActive * hi);
						int yHi = Math.round((float)(chanN + 1) / (float)nChansActive * hi);
						float yScale = (float)p.vscale;
						perChannelInfo[i] = new PerChannelInfo(yLo, yHi, yScale);
						chanN++;
					}
				}
				firstTime = false;
			}
			//Figure out which channel this data is for.
			PerChannelInfo chan = perChannelInfo[chanIx];
			if (chan == null) return;
			
			int xEnd = (int)displayPanelContainer.getCurrentXPixel();
			int xStart = chan.lastX;
			int len = ishDetectorData.length;
			if (clearLastX != xEnd) {
			  clearImage(clearLastX + 1, xEnd + 1, true);
			  clearLastX = xEnd;
			}
			for (int i = 0; i < len; i++) {
				int x = (len == 1) ? xEnd : 
					(int)Math.round(PamUtils.linterp(0,len-1,xStart,xEnd,i));
				int y       = chan.yHi - (int)(ishDetectorData[i] * chan.yScale * (chan.yHi-chan.yLo));
				int threshY = chan.yHi - (int)(p.thresh           * chan.yScale * (chan.yHi-chan.yLo));
				y = Math.min(chan.yHi, Math.max(y, chan.yLo));
				//if (x >= lastX) {
				Graphics2D g2d = (Graphics2D)image.getGraphics();
				g2d.setColor(Color.BLUE);				//detection function
				if (x >= chan.lastX) {
				  g2d.drawLine(chan.lastX, chan.lastY, x, y);
				}
				if (threshY >= chan.yLo && threshY < chan.yHi) {
					g2d.setColor(Color.RED);			//threshold line
					g2d.drawLine(chan.lastX, threshY, x, threshY);
				}
				chan.xAdvance = (chan.xAdvance <= 2) ? chan.xAdvance+1 
					: Math.max(chan.xAdvance, x - chan.lastX);
				chan.lastX = x;
				chan.lastY = y;
			}
			//if (xAdvance > 0)
			//	clearImage(lastX+1, lastX+1+xAdvance, true);
			repaint();
		}	
	}
}
