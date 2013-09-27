/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package Spectrogram;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import pamScrollSystem.AbstractPamScroller;
import pamScrollSystem.PamScrollObserver;
import pamScrollSystem.PamScroller;
import pamScrollSystem.RangeSpinner;
import pamScrollSystem.RangeSpinnerListener;

import soundPlayback.PlaybackControl;
import soundPlayback.PlaybackProgressMonitor;
import userDisplay.UserDisplayControl;
import userDisplay.UserFramePlots;

import Acquisition.AcquisitionProcess;
import Layout.DisplayPanel;
import Layout.DisplayPanelContainer;
import Layout.DisplayPanelProvider;
import Layout.DisplayProviderList;
import Layout.PamAxis;
import Layout.PamAxisPanel;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamDetection.AcousticDataUnit;
import PamDetection.RawDataUnit;
import PamUtils.FrequencyFormat;
import PamUtils.PamCalendar;
import PamView.ClipboardCopier;
import PamView.ColourArray;
import PamView.PamColors;
import PamView.PamLabel;
import PamView.ScrollableBufferedImage;
import PamguardMVC.LoadObserver;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import fftManager.Complex;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;

public class SpectrogramDisplay extends UserFramePlots implements PamObserver, LoadObserver,
InternalFrameListener, DisplayPanelContainer {

	private SpectrogramParameters spectrogramParameters;

	private SpectrogramParameters oldLayoutParameters;

	private SpectrogramOuterPanel spectrogramOuterPanel;

	private SpectrogramPanel[] spectrogramPanels;

	private SpectrogramAxis spectrogramAxis;

	private AmplitudePanel amplitudePanel;

	private SpectrogramPlotPanel spectrogramPlotPanel;

	private FFTDataBlock sourceFFTDataBlock;

	private PamRawDataBlock sourceRawDataBlock;

	private SpectrogramDisplay spectrogramDisplay;

	private BufferedImage amplitudeImage;

	private int imageWidth, imageHeight;

	private double[][] colorValues;

	private ColourArray colourArray;

	private double[] overlayColour = { 255, 0, 0 };

	private static int instanceCount;

	private float sampleRate = 1;

	private PamAxis timeAxis, frequencyAxis, amplitudeAxis;

	private int[] freqBinRange = new int[2];

	private double scaleX;

	private double scaleY;

	private Dimension panelSize;

	private Rectangle r;

	private int xAxisExtent, yAxisExtent;

	private SpectrogramProjector spectrogramProjector;

	private boolean firstUpdate = true;

	private ArrayList<PamDataBlock> detectorDataBlocks;

	private MouseAdapter popupListener;

	private int nMarkObservers;

	private PamScroller viewerScroller;

	//	private RangeSpinner viewerRangeSpinner;

	private Color freezeColour = Color.RED;

	private Color freezeColour2 = new Color(0, 198, 0);

	private boolean frozen = false;

	private PamLabel frequencyLabel = new PamLabel();

	private Object innerPanelSynchObject = new Object();

	/*
	 * Remember where the mouse was pressed and released. 
	 */
	private Point mouseDownPoint, currentMousePoint;
	private long mouseDownTime;

	protected ClipboardCopier panelClipBoardCopier;

	private boolean viewerMode, netRXMode;

	private UserDisplayControl userDisplayControl;

	private RangeSpinner rangeSpinner;

	private long masterClockMilliseconds;

	private long masterClockSamples;

	public SpectrogramDisplay(UserDisplayControl userDisplayControl, SpectrogramParameters spectrogramParameters) {

		super(userDisplayControl);

		this.userDisplayControl = userDisplayControl;

		this.spectrogramParameters = spectrogramParameters.clone();

		spectrogramDisplay = this;

		viewerMode = (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW);
		netRXMode = (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER);
		if (viewerMode || netRXMode) {
			viewerScroller = new PamScroller("Spectrogram", AbstractPamScroller.HORIZONTAL,
					1, 10000, true);
			viewerScroller.addObserver(new ViewScrollObserver());
			if (viewerMode) {
				viewerScroller.coupleScroller(userDisplayControl.getUnitName());
			}
			rangeSpinner = new RangeSpinner();
			rangeSpinner.addRangeSpinnerListener(new SpecTimeRangeListener());
			viewerScroller.addControl(rangeSpinner.getComponent());
			viewerScroller.addObserver(rangeSpinner);
			rangeSpinner.setSpinnerValue(this.spectrogramParameters.displayLength);
			//			viewerRangeSpinner = new RangeSpinner();
			//			viewerScroller.addControl(viewerRangeSpinner.getComponent());
			//			viewerRangeSpinner.addRangeSpinnerListener(new ViewRangeSpinnerListener());
		}

		createColours();

		spectrogramProjector = new SpectrogramProjector(this);

		setAxisPanel(spectrogramAxis = new SpectrogramAxis());
		setPlotPanel(spectrogramPlotPanel = new SpectrogramPlotPanel());
		setEastPanel(amplitudePanel = new AmplitudePanel());

		if (viewerMode) {
			viewerScroller.addMouseWheelSource(spectrogramPlotPanel);
			viewerScroller.addMouseWheelSource(spectrogramAxis);
		}

		for (int i = 0; i < spectrogramPanels.length; i++) {
			spectrogramPanels[i].addMouseListener(popupListener);
		}

		oldLayoutParameters = spectrogramParameters.clone();

		setParams(spectrogramParameters);

	}

	class SpecTimeRangeListener implements RangeSpinnerListener {

		@Override
		public void valueChanged(double oldValue, double newValue) {
			if (oldValue == newValue || spectrogramParameters.displayLength == newValue) {
				return;
			}
			spectrogramParameters.displayLength = newValue;
			spectrogramParameters.timeScaleFixed = true;
			noteNewSettings();
			newScrollPosition();
			//			repaintAll();
		}

	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}

	@Override
	public String getName() {
		return spectrogramParameters.windowName;
	}


	public void setParams(SpectrogramParameters newParameters) {
		if (PamController.getInstance().isInitializationComplete() == false) {
			return;
		}

		this.spectrogramParameters = newParameters.clone();


		detectorDataBlocks = PamController.getInstance().getDataBlocks(AcousticDataUnit.class, true);

		// count the number of SpectrogramMarkObservers
		nMarkObservers = 0;
		if (spectrogramParameters.useSpectrogramMarkObserver != null) {
			for (int i = 0; i < Math.min(spectrogramParameters.useSpectrogramMarkObserver.length,
					SpectrogramMarkObservers.getSpectrogramMarkObservers().size()); i++) {
				if (spectrogramParameters.useSpectrogramMarkObserver[i]) {
					nMarkObservers ++;
				}
			}
		}

		//		if (oldLayoutParameters.nPanels != spectrogramParameters.nPanels ||
		//		oldLayoutParameters.showWaveform != spectrogramParameters.showWaveform) {
		spectrogramPlotPanel.LayoutPlots();
		oldLayoutParameters = spectrogramParameters.clone();
		//		}

		if (getFrame() != null) {
			this.getFrame().setTitle(spectrogramParameters.sourceName);
		}

		ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
		.getFFTDataBlocks();

		if (spectrogramParameters.fftBlockIndex >= 0
				&& spectrogramParameters.fftBlockIndex < fftBlocks.size()) {
			if (sourceFFTDataBlock != (FFTDataBlock) fftBlocks.get(spectrogramParameters.fftBlockIndex)) {
				if (sourceFFTDataBlock != null) {
					sourceFFTDataBlock.deleteObserver(this);
				}
				sourceFFTDataBlock = (FFTDataBlock) fftBlocks.get(spectrogramParameters.fftBlockIndex);
				sourceFFTDataBlock.addObserver(this);
			}
			sampleRate = sourceFFTDataBlock.getSampleRate();

			//			PamProcess pamProcess = (PamProcess) fftDataSource;
			if (sourceFFTDataBlock != null) {
				//				spectrogramParameters.fftHop = sourceFFTDataBlock.getFftHop();
				//				spectrogramParameters.fftLength = sourceFFTDataBlock.getFftLength();
				spectrogramParameters.sourceName = sourceFFTDataBlock.getLongDataName();
			}
			else {
				System.out.println();
			}
		}
		if (sourceFFTDataBlock == null) {
			//System.out.println("No source data block for spectrogram " + getName());
		}

		/* 
		 * If there are SpectrogramMarkObservers, then it's
		 * necessary to subscribe to the waveform data since
		 * it may be needed by one of the mark observers.
		 * Go back through the block / process chain until we
		 * find the first raw data. This may not be the first block
		 * we came to - the fft block used for the spectrogram may
		 * have been through multiple porcesses (e.g. kernel smoothing)
		 * since it was first created from raw audio data. 
		 */
		PamRawDataBlock oldRawDataBlock = sourceRawDataBlock;

		sourceRawDataBlock = null;
		PamDataBlock pBlock;
		PamProcess pProcess;
		if (sourceFFTDataBlock != null) {
			pBlock = sourceFFTDataBlock;
			while (pBlock != null) {
				pProcess = pBlock.getParentProcess();
				if (pProcess == null) break;
				pBlock = pProcess.getParentDataBlock();
				if (pBlock != null && pBlock.getUnitClass() == RawDataUnit.class) {
					sourceRawDataBlock = (PamRawDataBlock) pBlock;
					break;
				}
			}
		}
		if (sourceRawDataBlock != oldRawDataBlock) {
			if (oldRawDataBlock != null) {
				oldRawDataBlock.deleteObserver(this);
			}
		}
		if (sourceRawDataBlock != null) {
			sourceRawDataBlock.addObserver(this);
			sampleRate = sourceRawDataBlock.getSampleRate();
		}

		if (sourceFFTDataBlock == null) {
			return;
		}

		// check the frequency bins ...
		freqBinRange[0] = (int) Math
		.floor(spectrogramParameters.frequencyLimits[0]
		                                             * sourceFFTDataBlock.getFftLength() / sampleRate);
		freqBinRange[1] = (int) Math
		.ceil(spectrogramParameters.frequencyLimits[1]
		                                            * sourceFFTDataBlock.getFftLength() / sampleRate);
		for (int i = 0; i < 2; i++) {
			freqBinRange[i] = Math.min(Math.max(freqBinRange[i], 0),
					sourceFFTDataBlock.getFftLength() / 2 - 1);
		}

		// now make the image - work out the required width and height
		createAllImages();

		createColours();
		// now make a standard amplitude image
		if (colorValues != null && colorValues.length > 0) {
			//			synchronized (spectrogramDisplay) {
			amplitudeImage = new BufferedImage(1, colorValues.length,
					BufferedImage.TYPE_INT_RGB);
			WritableRaster raster = amplitudeImage.getRaster();
			for (int i = 0; i < colorValues.length; i++) {
				raster.setPixel(0, colorValues.length - i - 1, colorValues[i]);
			}
			//			}
		}

		repaintAll();
	}

	public void repaintAll() {
		setAxisLimits();

		//		sayWithDate("repaint all");
		if (spectrogramOuterPanel != null) {
			spectrogramOuterPanel.repaint();
		}
		if (spectrogramAxis != null) {
			spectrogramAxis.repaint();
		}
		if (spectrogramPanels != null){
			for (int i = 0; i < spectrogramPanels.length; i++){
				if (spectrogramPanels[i] == null) continue;
				spectrogramPanels[i].repaint();
			}
		}
		if (amplitudePanel != null) {
			amplitudePanel.SetAmplitudePanelBorder();
			amplitudePanel.repaint();
		}
		//		if (waveformPanel != null) waveformPanel.repaint();
	}

	/**
	 * Width of the bitmap thats being drawn on - not the number of pixels, 
	 * though these may often be the same.
	 * @return the width of the image in pixels
	 */
	public int getImageWidth() {
		int width;
		if (sourceFFTDataBlock == null) {
			return 1;
		}
		if (spectrogramParameters.timeScaleFixed) {
			width = (int) (spectrogramParameters.displayLength
					* sampleRate / sourceFFTDataBlock.getFftHop());
			spectrogramParameters.pixelsPerSlics = 1;
		} else {
			width = getVariableWidth();
			spectrogramParameters.displayLength = (double) imageWidth
			* sourceFFTDataBlock.getFftHop() / sampleRate;
		}
		return Math.max(1, width);
	}

	public int getImageHeight() {
		int height = freqBinRange[1] - freqBinRange[0] + 1;
		return Math.max(1, height);
	}

	/**
	 * Called whenever the display size changes so that
	 * buffered images can be recreated. 
	 */
	public void createAllImages() {
		// imageHeight = spectrogramParameters.fftLength / 2;
		imageHeight = getImageHeight();
		imageWidth = getImageWidth();
		if (imageWidth <= 1)
			return;

		for (int i = 0; i < spectrogramPanels.length; i++) {
			spectrogramPanels[i].createImage();
		}

		createColours();

		timeAxis = new PamAxis(10, 0, imageWidth, 0, 0,
				spectrogramParameters.displayLength, true, "seconds", "%3.1f");
		spectrogramAxis.setNorthAxis(timeAxis);

		if (rangeSpinner != null) {
			rangeSpinner.setSpinnerValue(spectrogramParameters.displayLength);
		}

		double fScale = 1;
		if (spectrogramParameters.frequencyLimits[1] > 2000) {
			fScale = 1000;
		}

		frequencyAxis = new PamAxis(0, 200, 0, 10,
				spectrogramParameters.frequencyLimits[0] / fScale,
				spectrogramParameters.frequencyLimits[1] / fScale, true, "",
				null);
		frequencyAxis.setFractionalScale(true);
		spectrogramAxis.setWestAxis(frequencyAxis);

		if (amplitudeAxis != null) {
			amplitudeAxis.setRange(spectrogramParameters.amplitudeLimits[0], spectrogramParameters.amplitudeLimits[1]);
		}

		setupViewScroller();
	}

	private void setAxisLimits() {
		if (frequencyAxis != null && spectrogramParameters != null) {
			double fScale = 1;
			if (spectrogramParameters.frequencyLimits[1] > 2000) {
				fScale = 1000;
			}
			frequencyAxis.setRange(spectrogramParameters.frequencyLimits[0]/fScale,
					spectrogramParameters.frequencyLimits[1]/fScale);
		}
	}

	private void createColours() {

		//		colourArray = ColourArray.createHotArray(256);
		colourArray = ColourArray.createStandardColourArray(256, spectrogramParameters.getColourMap());
		colorValues = new double[256][3];
		for (int i = 0; i < 256; i++) {
			//			for (int j = 0; j < 3; j++) {
			//				colorValues[i][j] = 255 - i;
			////				colorValues[i][j] = colourArray.getColours()[i].ge
			//			}
			colorValues[i][0] = colourArray.getColours()[i].getRed();
			colorValues[i][1] = colourArray.getColours()[i].getGreen();
			colorValues[i][2] = colourArray.getColours()[i].getBlue();
		}
	}

	int getVariableWidth() {
		if (spectrogramPanels == null || spectrogramPanels[0] == null) return 1;
		int w = spectrogramPanels[0].getWidth();
		w /= spectrogramParameters.pixelsPerSlics;
		return Math.max(w, 1);
	}

	public void setSampleRate(float sampleRate, boolean notify) {
		//		System.out.println("samplerate " + sampleRate);
		this.sampleRate = sampleRate;
		// since this always gets called just before pam starts, use it to
		// reset the displays
		//		createAllImages(); don't do this since this get's called a lot when reloading data offline.
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		this.masterClockMilliseconds = milliSeconds;
		this.masterClockSamples = sampleNumber;
		if (netRXMode) {
			setupNetRXScroller();
		}
		//		spectrogramAxis.repaint(10);
	}

	private void setupNetRXScroller() {
		//		viewerScroller.getValueMillis();
		//		scro
		int spin = (int) (rangeSpinner.getSpinnerValue() * 1000.);
		long range = viewerScroller.getRangeMillis();
		viewerScroller.setVisibleAmount(spin);
		long visAm = viewerScroller.getVisibleAmount();
		if (range == 0) {
			range = 60000;
		}
		boolean onMax = viewerScroller.getValueMillis() ==
			(viewerScroller.getMaximumMillis()-viewerScroller.getVisibleAmount());
		long currentValue = viewerScroller.getValueMillis();
		if (currentValue <= 0) {
			onMax = true;
		}
		long ahead = 1000;
		ahead = Math.min(ahead, spin/5);
		viewerScroller.setRangeMillis(masterClockMilliseconds-range+ahead, masterClockMilliseconds+ahead, true);
		if (onMax) {
			viewerScroller.setValueMillis(masterClockMilliseconds+ahead-viewerScroller.getVisibleAmount());
		}
		else {
			viewerScroller.setValueMillis(currentValue);
		}
		//		viewerScroller.
		//		repaintAll();
		newScrollPosition();
	}

	public void PamToStart() {
		this.createAllImages();
		noteNewSettings();
	}


	/**
	 * Required data history depends on what's happening with the mouse.
	 * If the mouse is doing nothing and there are no SpectrogramMarkObservers
	 * then no data needs to be stored for drawing the spectrogram. 
	 * If there are mark observers and the mouse is up, then make sure that
	 * at least one screen full of data is always in memory (both FFT data
	 * and Raw wave data).
	 * If the mouse is down, then keep the maximum of either one screen full
	 * of from whenever the mouse was pressed - the use may hold it down for 
	 * a long time !
	 * 
	 * @see SpectrogramMarkObserver
	 */
	public long getRequiredDataHistory(PamObservable o, Object arg) {

		if (nMarkObservers == 0) {
			return 0;
		}

		long history = (long) getXDuration();
		if (frozen) { // then the mouse is down !
			history += (PamCalendar.getTimeInMillis()-mouseDownTime); 
		}
		return history + 1000; // save an extra second of data 
	}

	public String getObserverName() {
		return "Spectrogam Display";
	}
	@Override
	public void update(PamObservable obs, PamDataUnit newData) {
		//if (((1<<spectrogramParameters.channelList[0]) & newData.channelBitmap) == 0) return;
		// work out which panel(s) gets the data ...
		// it is possible for more than one panel to be using the same data
		if (spectrogramPanels == null) return;
		int dataChannel = -1;
		AcousticDataUnit acousticData = (AcousticDataUnit) newData;
		/*
		 * To work out which display data associates with, need to do a bitwais compare.
		 * The basic waveform and fft data will always be snge channel so we could in principle
		 * use ==. click data and other detector output may be multi channel though, so == will
		 * always be false since the channelList is always single channels. The bitwise comparison
		 * gets around this. 
		 * TODO - make it so each spectrogram can chose independently of the other.
		 */
		for (int i = 0; i < spectrogramParameters.nPanels; i++) {
			if (((1<<spectrogramParameters.channelList[i]) & acousticData.getChannelBitmap()) > 0) {
				updateChannel(obs, acousticData, i);
			}
		}

	}


	@Override
	public void setLoadStatus(int loadState) {
		//		System.out.println("Data load status = " + loadState);
		if (loadState == PamDataBlock.REQUEST_DATA_LOADED) {
			drawBackgroundImages();
			repaintAll();
		}
	}

	private void updateChannel(PamObservable obs, AcousticDataUnit newData, int panelNumber){
		if (viewerMode) {
			return;
		}
		synchronized (innerPanelSynchObject) {
			if (newData == null || spectrogramPanels == null || spectrogramProjector == null) return;
			if (spectrogramPanels.length <= panelNumber || spectrogramPanels[panelNumber] == null) return;
			if (obs == this.sourceFFTDataBlock) {
				FFTDataUnit fftDataUnit = (FFTDataUnit) newData;
				//			System.out.println("New FFT Data for Channel " + dataChannel + " Data length " + fftDataUnit.getFftData().length + " bins");
				spectrogramPanels[panelNumber].drawSpectrogram(obs, fftDataUnit, panelNumber);
				spectrogramProjector.setOffset(newData.getTimeMilliseconds(), spectrogramPanels[panelNumber].imagePos);
				if (spectrogramPanels[panelNumber].getSize().equals(panelSize) == false) {
					panelSize = spectrogramPanels[panelNumber].getSize();
					//				createOverlay(panelSize.width, panelSize.height);
				}
			} 
		}

	}


	private int getColourIndex(double dBLevel) {
		// fftMag is << 1
		double  p;
		p = 256
		* (dBLevel - spectrogramParameters.amplitudeLimits[0])
		/ (spectrogramParameters.amplitudeLimits[1] - spectrogramParameters.amplitudeLimits[0]);
		return (int) Math.max(Math.min(p, 255), 0);
	}

	//	private void sayWithDate(String msg) {
	//		System.out.println(String.format("%s: %s", PamCalendar.formatTime(System.currentTimeMillis()), msg));
	//	}


	class SettingsAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			spectrogramDisplay.setSettings();
		}
	}
	class MenuCancelPlayback implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			PlaybackControl.getViewerPlayback().stopViewerPlayback();
		}
	}
	class MenuPlayAll implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			long startMillis = viewerScroller.getValueMillis();
			long endMillis = startMillis + viewerScroller.getVisibleAmount();
			PlaybackControl.getViewerPlayback().playViewerData(startMillis, endMillis, new PlayProgress());
		}
	}
	class MenuPlayChannel implements ActionListener {
		int channel;

		public MenuPlayChannel(int channel) {
			super();
			this.channel = channel;
		}

		public void actionPerformed(ActionEvent e) {
			long startMillis = viewerScroller.getValueMillis();
			long endMillis = startMillis + viewerScroller.getVisibleAmount();
			PlaybackControl.getViewerPlayback().playViewerData(1<<channel, startMillis, endMillis, new PlayProgress());
		}
	}

	class MenuPlayFrom implements ActionListener {
		int channel;
		long startMillis;

		public MenuPlayFrom(int channel, long startMillis) {
			super();
			this.channel = channel;
			this.startMillis = startMillis;
		}

		public void actionPerformed(ActionEvent e) {
			long dispStartMillis = viewerScroller.getValueMillis();
			long endMillis = dispStartMillis + viewerScroller.getVisibleAmount();
			PlaybackControl.getViewerPlayback().playViewerData(1<<channel, startMillis, endMillis, new PlayProgress());
		}
	}

	private int playbackChannels;
	private long playbackTimeMillis;
	private int playbackStatus = PlaybackProgressMonitor.PLAY_END;
	class PlayProgress implements PlaybackProgressMonitor {

		@Override
		public void setProgress(int channels, long timeMillis, double percent) {
			playbackChannels = channels;
			playbackTimeMillis = timeMillis;
			repaintAll();
		}

		@Override
		public void setStatus(int status) {
			playbackStatus = status;
			repaintAll();
		}

	}

	public void setSettings() {
		// check that fft data is available before even trying

		ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
		.getFFTDataBlocks();
		if (fftBlocks == null || fftBlocks.size() == 0) {
			JOptionPane.showMessageDialog(spectrogramPlotPanel, "No spectrogram data is available in the current model." + 
			"\nCreate at least one FFT (spectrogram) machine");
			return;
		}
		SpectrogramParameters newParams = SpectrogramParamsDialog
		.showDialog(userDisplayControl.getPamView().getGuiFrame(), spectrogramParameters);
		if (newParams == null) return;
		spectrogramParameters = newParams.clone();

		noteNewSettings();
		//		setParams(newParams);
	}

	/**
	 * set up the page size on the view scroller
	 * and also sets up the range spinner
	 */
	private void setupViewScroller() {
		if (viewerScroller == null) {
			return;
		}
		long visibleMillis = (long)(spectrogramParameters.displayLength * 1000);
		viewerScroller.setVisibleAmount(visibleMillis);
		viewerScroller.setBlockIncrement(visibleMillis * 9 / 10);
		viewerScroller.setUnitIncrement(visibleMillis * 1 / 10);

		requestFFTData();

		subscribeDataBlocks();
	}

	/**
	 * Create lists of data blocks that each panel is viewing
	 * so they can plot the data efficiently. 
	 */
	private void subscribeDataBlocks() {
		if (spectrogramPanels == null) {
			return;
		}
		for (int i = 0; i < spectrogramPanels.length; i++) {
			spectrogramPanels[i].subscribeDataBlocks();
		}
		subscribeViewScrollData();
	}
	/**
	 * Subscribe overlaying data to the view scroller. 
	 */
	private void subscribeViewScrollData() {
		if (viewerScroller == null) {
			return;
		}
		PamDataBlock dataBlock;
		detectorDataBlocks = PamController.getInstance().getDataBlocks(AcousticDataUnit.class, true);
		//			if (spectrogramParameters.showDetector == null || spectrogramParameters.showDetector[panelId] == null) return;
		boolean want;
		for (int i = 0; i < detectorDataBlocks.size(); i++) {
			//				checkMenuItem = (JCheckBoxMenuItem) detectorMenu.getComponent(i);
			dataBlock = detectorDataBlocks.get(i);
			if (dataBlock.canDraw(spectrogramProjector)) {
				want = false;
				for (int iPanel = 0; iPanel < spectrogramParameters.nPanels; iPanel++) {
					if (spectrogramParameters.getShowDetector(iPanel,i)) {
						want = true;
					}
				}
				if (want) {
					viewerScroller.addDataBlock(dataBlock);
				}
				else {
					viewerScroller.removeDataBlock(dataBlock);
				}
			}
		}
	}

	class ViewScrollObserver implements PamScrollObserver {

		@Override
		public void scrollRangeChanged(AbstractPamScroller pamScroller) {
			//			System.out.println("Scroll range changed " + PamCalendar.formatTime(pamScroller.getMinimumMillis()));
			setupViewScroller();
		}

		@Override
		public void scrollValueChanged(AbstractPamScroller pamScroller) {
			//			System.out.println("Scroll value changed " + PamCalendar.formatTime(pamScroller.getValueMillis()));
			//			sayWithDate(String.format("scrollValueChanged to %d", pamScroller.getValueMillis()));
			newScrollPosition();
		}
	}

	void newScrollPosition() {
		if (spectrogramPanels == null) {
			return;
		}
		long s = viewerScroller.getValueMillis();
		//		System.out.println(String.format("Scroll spectrogram to %s", PamCalendar.formatDateTime(s)));
		long e = s + (long) (spectrogramParameters.displayLength * 1000.);
		if (viewerMode) {
			requestFFTData(s, e);
		}

		for (int i = 0; i <  spectrogramPanels.length; i++) {
			spectrogramPanels[i].currentTimeMilliseconds = viewerScroller.getValueMillis() +
			(long) (spectrogramParameters.displayLength * 1000.);
		}

		//		System.out.println(String.format("Repaint spectrogram to %s", PamCalendar.formatDateTime(s)));
		repaintAll();
	}

	private void requestFFTData() {
		if (viewerScroller == null) {
			return;
		}
		long s = viewerScroller.getValueMillis();
		long e = s + (long) (spectrogramParameters.displayLength * 1000.);
		requestFFTData(s, e);
	}

	private long lastReqStart, lastReqEnd;
	private void requestFFTData(long startMillis, long endMillis) {

		if (sourceFFTDataBlock == null) {
			return;
		}
		if (lastReqStart == startMillis && lastReqEnd == endMillis) {
			return;
		}
		lastReqStart = startMillis;
		lastReqEnd = endMillis;
		//					System.out.println(String.format("%s Requesting %5.2fs data from %s", 
		//							sourceFFTDataBlock.getDataName(), (double)(endMillis-startMillis)/1000., 
		//							PamCalendar.formatTime(startMillis)));
		//		long t1 = System.nanoTime();
		//		int r = sourceFFTDataBlock.getOfflineData(this, this, startMillis, endMillis);
		//		long t2 = System.nanoTime();
		//		if ((r & PamDataBlock.REQUEST_SAME_REQUEST) == 0) {
		//			drawBackgroundImages();
		//			long t3 = System.nanoTime();
		//			System.out.println(String.format("%s %3.2fs to load data, %3.2fs to draw it",
		//					sourceFFTDataBlock.getDataName(),
		//					(double)(t2-t1)/1.e9, (double) (t3-t2)/1.e9));
		//		}
		drawBackgroundImages(); // should effectively clear the image. 
		//		long t1 = System.nanoTime();
		sourceFFTDataBlock.orderOfflineData(this, this, startMillis, endMillis, 
				PamDataBlock.OFFLINE_DATA_INTERRUPT);
		//		long t2 = System.nanoTime();
		//		System.out.println(String.format("%s %3.2fs to request data",
		//				sourceFFTDataBlock.getDataName(),
		//				(double)(t2-t1)/1.e9));

	}

	/**
	 * redraw the background images in viewer mode with 
	 * new FFT data recreated from sound file data. 
	 */
	private void drawBackgroundImages() {
		if (viewerMode) {
			for (int i = 0; i < spectrogramPanels.length; i++) {
				spectrogramPanels[i].drawBackgroundImage();
			}
		}
	}


	//	class ViewRangeSpinnerListener implements RangeSpinnerListener {
	//		@Override
	//		public void valueChanged(double oldValue, double newValue) {
	//			double range = viewerRangeSpinner.getSpinnerValue();
	//			timeAxis.setRange(0, range);
	//			spectrogramParameters.displayLength = range;
	//			noteNewSettings();
	//			if (spectrogramPanels == null) {
	//				return;
	//			}
	//			for (int i = 0; i <  spectrogramPanels.length; i++) {
	//				spectrogramPanels[i].currentTimeMilliseconds = viewerScroller.getValueMillis() +
	//				(long) (spectrogramParameters.displayLength * 1000.);
	//			}
	//			repaintAll();
	//		}
	//	}

	/**
	 * Need to extend PamAxisPanel in order to 
	 * override the axis drawing to allow for the
	 * plug in panels at the bottom of the display.
	 * @author Doug
	 *
	 */
	class SpectrogramAxis extends PamAxisPanel {
		Rectangle oldBounds = new Rectangle();

		SpectrogramAxis() {
			super();
			//			PamColors.getInstance().registerComponent(this,
			//					PamColors.PamColor.BORDER);
			this.SetBorderMins(10, 10, 10, 10);
		}

		// need to redraw when heights of inner components are zero
		// but make sure it doesn't get stuck in an inf' loop !
		int heightErrors = 0; 
		/*
		 * Need to override paint to allow for funny scaling if waveform and
		 * spectrogram both shown
		 */
		@Override
		public void paintComponent(Graphics g) {
			//			super.paintComponent(g);
			if (isShowing() == false) return;
			Rectangle b = g.getClipBounds();
			g.setColor(getBackground());
			g.fillRect(b.x, b.y, b.width, b.height);
			/*
			 * Check that there is enough space for each of up to four axis and
			 * draw it.
			 */
			// Rectangle r = getBounds();
			Insets currentInsets = getInsets();
			Insets newInsets = new Insets(getMinNorth(), getMinWest(), getMinSouth(), getMinEast());
			// Insets plotInsets;
			if (getInnerPanel() != null) {
				// plotInsets = innerPanel.getInsets();
			} else {
				// plotInsets = new Insets(0,0,0,0);
			}
			if (getNorthAxis() != null) {
				newInsets.top = Math.max(newInsets.top, getNorthAxis().getExtent(g));
			}
			if (getWestAxis() != null) {
				newInsets.left = Math
				.max(newInsets.left, getWestAxis().getExtent(g));
			}
			if (getSouthAxis() != null) {
				newInsets.bottom = Math.max(newInsets.bottom, getSouthAxis()
						.getExtent(g));
			}
			if (getEastAxis() != null) {
				newInsets.right = Math.max(newInsets.right, getEastAxis()
						.getExtent(g));
			}
			if (!currentInsets.equals(newInsets)) {
				setBorder(new EmptyBorder(newInsets));
			}

			int panelRight = getWidth() - newInsets.right;
			int panelBottom = getHeight() - newInsets.bottom;
			if (getNorthAxis() != null) {
				getNorthAxis().drawAxis(g, newInsets.left, newInsets.top,
						panelRight, newInsets.top);
				// and draw the time at the left end of the axis.
				//				long axisStart = spectrogramProjector.getTimeOffsetMillis();
				//				String timeString = PamCalendar.formatDateTime(masterClockMilliseconds);
				//				g.drawString(timeString, newInsets.left, g.getFontMetrics().getHeight());
			}
			if (getWestAxis() != null) {
				for (int i = 0; i < spectrogramParameters.nPanels; i++){
					if (spectrogramPanels[0] == null) return;
					int singleHeight = spectrogramPanels[0].getHeight();
					if (singleHeight == 0 && heightErrors++ < 2) {
						repaint();
						return;
					}
					heightErrors = 0;
					getWestAxis().drawAxis(g, newInsets.left, newInsets.top
							+ singleHeight * (i+1), newInsets.left, newInsets.top + singleHeight *  i);
					//					System.out.println(String.format("Draw spec W axis with insets %d %d %d %d, single height = %d",
					//							newInsets.left, newInsets.top
					//							+ singleHeight * (i+1), newInsets.left, newInsets.top + singleHeight *  i, singleHeight));
				}
			}
			if (getSouthAxis() != null) {
				getSouthAxis().drawAxis(g, newInsets.left, panelBottom, panelRight,
						panelBottom);
			}
			if (getEastAxis() != null) {
				getEastAxis().drawAxis(g, panelRight, panelBottom, panelRight,
						newInsets.top);
			}
			/* 
			 * Also need to draw east and west axis of any plugged in displays, should they have them !
			 */
			//			synchronized (displayPanels) {
			//			if (((Object)displayPanels).getClass().) {
			//				
			//			}
			DisplayPanel dp;
			Point fLoc, dpLoc; 
			fLoc = getLocationOnScreen();
			int axTop, axBottom;
			PamAxis axis;

			synchronized(displayPanels) {
				int nD = displayPanels.size();
				for (int i = 0; i < nD; i++) {
					dp = displayPanels.get(i);
					if ((axis = dp.getWestAxis()) != null) {
						// need to work out where the hell the 
						// window is relative to this one...
						if (dp.getInnerPanel().isShowing() == false) {
							return;
						}
						dpLoc = dp.getInnerPanel().getLocationOnScreen();
						axTop = dpLoc.y - fLoc.y;
						axBottom = axTop + dp.getInnerPanel().getHeight();
						axis.drawAxis(g, newInsets.left, axBottom, newInsets.left, axTop);
					}
					if ((axis = dp.getSouthAxis()) != null) {
						dpLoc = dp.getInnerPanel().getLocationOnScreen();
						axBottom = dpLoc.y - fLoc.y + dp.getInnerPanel().getHeight();
						axis.drawAxis(g, newInsets.left, axBottom, newInsets.left + dp.getInnerPanel().getWidth(), axBottom);
					}
					//				if ((axis = dp.getEastAxis()) != null) {
					//				// need to work out where the hell the 
					//				// window is relative to this one...
					//				dpLoc = dp.getInnerPanel().getLocationOnScreen();
					//				axTop = dpLoc.y - fLoc.y;
					//				axBottom = axTop + dp.getInnerPanel().getHeight();
					//				axis.drawAxis(g, panelRight, axBottom, panelRight, axTop);
					//				}
				}
			}
			if (viewerScroller != null) {
				long t = viewerScroller.getValueMillis();
				long t2 = t + viewerScroller.getVisibleAmount();
				FontMetrics fm = g.getFontMetrics();
				g.drawString(PamCalendar.formatDateTime(t), newInsets.left, fm.getHeight());
				String ts = PamCalendar.formatDateTime(t2);
				g.drawString(ts, getWidth() - newInsets.right - fm.stringWidth(ts), fm.getHeight());
			}
		}

	}

	class AmplitudePanel extends PamAxisPanel {
		AmplitudePanel() {
			super();
			//			PamColors.getInstance().registerComponent(this,
			//					PamColors.PamColor.BORDER);
			// setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
			setLayout(new BorderLayout());
			add(new AmplitudeBar(), BorderLayout.CENTER);
			amplitudeAxis = new PamAxis(0, 200, 0, 10,
					spectrogramParameters.amplitudeLimits[0],
					spectrogramParameters.amplitudeLimits[1], false, "", "%3.0f");
			setEastAxis(amplitudeAxis);
		}

		@Override
		public void paintComponent(Graphics g) {
			// TODO Auto-generated method stub
			super.paintComponent(g);
		}

		/**
		 * needs to be called whenever the other sizes change to kepp
		 * borders the same.
		 * Should also resize, to allow space under if plug in plots are 
		 * created. Easiest to do this by checking on size of 
		 * spectrogramOutperPanel and making sure it's the same.
		 *
		 */
		void SetAmplitudePanelBorder() {
			// needs to be called whenever the other sizes change to keep
			// borders the same.

			Insets axisInsets = spectrogramAxis.getInsets();

			int botInset = axisInsets.bottom;
			if (spectrogramOuterPanel != null) {
				botInset += (spectrogramPlotPanel.getHeight() - spectrogramOuterPanel.getHeight());
			}

			SetBorderMins(axisInsets.top, 0, botInset, 10);
			//			SetBorderMins(12, 0, 19, 100);
			// setMinimumSize(new Dimension(300, 20));
		}

		class AmplitudeBar extends JPanel {
			AmplitudeBar() {
				setMinimumSize(new Dimension(100, 20));
			}

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				if (amplitudeImage == null)
					return;

				Graphics2D g2d = (Graphics2D) g;

				// double scaleWidthfactor = 1;
				double ascaleX = this.getWidth()
				/ (double) amplitudeImage.getWidth(null);
				double ascaleY = this.getHeight()
				/ (double) amplitudeImage.getHeight(null);
				AffineTransform xform = new AffineTransform();
				// xform.translate(1, amplitudeImage.getWidth(null));
				xform.scale(ascaleX, ascaleY);
				g2d.drawImage(amplitudeImage, xform, this);
			}
		}
	}

	class SpectrogramOuterPanel extends JPanel {

		private SpectrogramDisplay spectrogramDisplay;
		public SpectrogramOuterPanel(SpectrogramDisplay spectrogramDisplay) {
			super();
			this.spectrogramDisplay = spectrogramDisplay;

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			synchronized (innerPanelSynchObject) {
				int topBorder, bottomBorder;
				Color borderColour = Color.BLACK;
				if (spectrogramPanels != null) {
					for (int i = 0; i < spectrogramPanels.length; i++) {
						if (spectrogramPanels[i] != null) {
							spectrogramPanels[i].destroyPanel();
						}
					}
				}
				spectrogramPanels = new SpectrogramPanel[spectrogramParameters.nPanels];
				//popupListener = new PopupListener();
				for (int i = 0; i < spectrogramParameters.nPanels; i++) {
					topBorder = 0;
					bottomBorder = 1;
					spectrogramPanels[i] = new SpectrogramPanel(spectrogramDisplay, i);
					spectrogramPanels[i].setBorder((BorderFactory.createMatteBorder(topBorder,0,bottomBorder,0,borderColour)));
					add(spectrogramPanels[i]);
					spectrogramPanels[i].addMouseListener(spectrogramDisplay.popupListener);
				}
			}
		}

	}
	/**
	 * Outer panel which contains the spectrogram outer panel
	 * and also all the plug in plots a the bottom 
	 * 
	 * @author Doug
	 *
	 */
	class SpectrogramPlotPanel extends JPanel {

		JSplitPane splitPane;
		JPanel simplePane;

		public SpectrogramPlotPanel() {
			super();

			setLayout(new BorderLayout());

			LayoutPlots();
		}

		@Override
		public void paintComponent(Graphics g) {
			// draw a cross which will show if no panels are present. 
			g.drawLine(0,0,getWidth(), getHeight());
			g.drawLine(0,getHeight(),getWidth(), 0);
			//			sayWithDate("Draw spectrogram plot panel");
		}

		public void LayoutPlots() {

			// need to remove the old plots first
			removeAll();

			if (viewerScroller != null) {
				add(BorderLayout.SOUTH, viewerScroller.getComponent());
			}

			oldDisplayPanels = displayPanels;
			displayPanels = new Vector<DisplayPanel>();

			splitPane = null;
			simplePane = null;
			spectrogramOuterPanel = new SpectrogramOuterPanel(spectrogramDisplay);

			int nAvailablePlugins = DisplayProviderList.getDisplayPanelProviders().size();
			int nUsedPlugins = 0;
			if (nAvailablePlugins > 0 && spectrogramParameters.showPluginDisplay != null) {
				for (int i = 0; i < Math.min(nAvailablePlugins, spectrogramParameters.showPluginDisplay.length); i++){
					if (spectrogramParameters.showPluginDisplay[i]) {
						nUsedPlugins ++;
					}
				}
			}
			JPanel bottomPanel = null;

			//			Synchronised (displayPanels) {
			if (nUsedPlugins > 0) {
				DisplayPanelProvider dp;
				DisplayPanel displayPanel;
				bottomPanel = new JPanel();
				splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				splitPane.setTopComponent(spectrogramOuterPanel);
				bottomPanel.setLayout(new GridLayout(nUsedPlugins, 1));
				for (int i = 0; i < Math.min(nAvailablePlugins, spectrogramParameters.showPluginDisplay.length); i++){
					if (spectrogramParameters.showPluginDisplay[i]) {
						dp = DisplayProviderList.getDisplayPanelProviders().get(i);
						displayPanel = recyleOldPanel(dp);
						if (displayPanel == null) {
							displayPanel = dp.createDisplayPanel(spectrogramDisplay);
						}
						displayPanels.add(displayPanel);
						bottomPanel.add(displayPanel.getPanel());
					}
				}
				splitPane.setBottomComponent(bottomPanel);
				splitPane.setResizeWeight(0.7);
				add(BorderLayout.CENTER, splitPane);
			}
			else {
				simplePane = new JPanel();
				simplePane.setLayout(new BoxLayout(simplePane, BoxLayout.Y_AXIS));
				simplePane.add(spectrogramOuterPanel);
				add(BorderLayout.CENTER, simplePane);
				//				System.out.println("Create normal display");
			}

			clearOldPlugins();
			//			}

			if (bottomPanel != null) {
				bottomPanel.addAncestorListener(new SplitPaneListener());
				//				splitPane.add
			}
			// this bodge seems to be needed to make the new window appear
			//			setVisible(false);
			//			setVisible(true);
			invalidate();

			for (int i = 0; i < spectrogramPanels.length; i++)
				spectrogramPanels[i].addMouseListener(popupListener);

		}

		DisplayPanel recyleOldPanel(DisplayPanelProvider displayPanelProvider) {
			if (oldDisplayPanels == null) {
				return null;
			}
			for (int i = 0; i < oldDisplayPanels.size(); i++) {
				if (oldDisplayPanels.get(i).getDisplayPanelProvider() == displayPanelProvider) {
					return oldDisplayPanels.remove(i);
				}
			}
			return null;
		}
		void clearOldPlugins() {
			// properly remove all existing plugin panels.
			// each needs to be told it's on it's way out so that
			// it can unsubscribe from any data

			synchronized (oldDisplayPanels) {
				for (int i = 0; i < oldDisplayPanels.size(); i++) {
					oldDisplayPanels.get(i).destroyPanel();
				}
				oldDisplayPanels.clear();
			}
		}

	} // end class SpectrogramPlotPanel

	/**
	 * 
	 * Inner panels showing a single spectrogram display. 
	 * @author Doug Gillespie
	 *
	 */
	class SpectrogramPanel extends JPanel implements PamObserver {

		Dimension panelSize = new Dimension();

		int panelId; // identifier for this panel - 0, 1, 2, 3, etc...

		JPopupMenu detectorMenu;

		JCheckBoxMenuItem[] checkMenuItems;

		PopupListener popupListener;

		ArrayList<PamDataBlock> viewedDataBlocks = new ArrayList<PamDataBlock>();

//		private ScrollableBufferedImage specImage;
		private BufferedImage specImage;

		private DirectDrawProjector directDrawProjector;

		private WritableRaster writableRaster;

		SpecPanelMouse specPanelMouse;

		int imagePos;
		
		int frozenImagePos;

		long currentTimeMilliseconds;
		
		long frozenTimeMilliseconds;

		JLabel channelLabel = new PamLabel();

		//		private JToolTip frequencyTooltip;

		/**
		 * Frozen image used when the mouse is pressed over the display
		 */
		private BufferedImage frozenImage;

		/*
		 * Indicates that this is the marked spectrogram panel
		 */
		boolean markThis = false;

		private boolean firstWrapDone;

		SpectrogramPanel(SpectrogramDisplay spectrogramDisplay, int iD) {

			super();

			panelId = iD;

			directDrawProjector = new DirectDrawProjector(spectrogramDisplay, iD);
			//			if (viewerMode) {
			//				this.addMouseMotionListener(directDrawProjector.getMouseHoverAdapter(this));
			//				this.addMouseListener(directDrawProjector.getMouseHoverAdapter(this));
			//			}

			setToolTipText("Spectrogram Display Panel");

			setBackground(Color.BLACK);

			popupListener = new PopupListener();

			getPlotDetectorMenu(null); // initialises the menu immediately

			addMouseListener(popupListener);	

			createImage();

			specPanelMouse = new SpecPanelMouse(this);
			addMouseListener(specPanelMouse);
			addMouseMotionListener(specPanelMouse);

			channelLabel.setFont(PamColors.getInstance().getBoldFont());
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(channelLabel);

			if (colourArray != null) {
				setBackground(colourArray.getColours()[0]);
			}

			subscribeDataBlocks();

			//			this.setToolTipText("Spectrogram Display");
			//			frequencyTooltip = createToolTip();

		}

		public void subscribeDataBlocks() {

			viewedDataBlocks.clear();
			PamDataBlock dataBlock;
			detectorDataBlocks = PamController.getInstance().getDataBlocks(AcousticDataUnit.class, true);
			//			if (spectrogramParameters.showDetector == null || spectrogramParameters.showDetector[panelId] == null) return;

			for (int i = 0; i < detectorDataBlocks.size(); i++) {
				//				checkMenuItem = (JCheckBoxMenuItem) detectorMenu.getComponent(i);
				dataBlock = detectorDataBlocks.get(i);
				if (dataBlock.canDraw(spectrogramProjector)) {
					if (spectrogramParameters.getShowDetector(panelId,i)) {
						viewedDataBlocks.add(dataBlock);
					}
				}
			}
		}

		void destroyPanel() {
			if (detectorDataBlocks == null) return;
			for (int i = 0; i < detectorDataBlocks.size(); i++) {
				detectorDataBlocks.get(i).deleteObserver(this);
			}
		}

		@Override
		public PamObserver getObserverObject() {
			return this;
		}

		private void createImage() {
			synchronized (spectrogramDisplay) {	
				imageWidth = getImageWidth();
				imageHeight = getImageHeight();
				imageWidth = Math.min(imageWidth, 10240);
				//				System.out.println(String.format("Spec Image width and height = %d,%d", imageWidth, imageHeight));
				specImage = new BufferedImage(imageWidth, imageHeight,
						BufferedImage.TYPE_INT_RGB);
//				specImage.setParentComponent(this);
				writableRaster = specImage.getRaster();
			}
			imagePos = -1;
			firstWrapDone = false;

			//			currentTimeMilliseconds = 0;

			if (colourArray != null) {
				Graphics g = specImage.getGraphics();
				//				g.setColor(colourArray.getColours()[colourArray.getNumbColours()-1]);
//				g.setColor(colourArray.getColours()[0]);
				g.setColor(Color.MAGENTA);
				specImage.getGraphics().fillRect(0, 0, imageWidth, imageHeight);
			}

			channelLabel.setText(String.format("channel %d", spectrogramParameters.channelList[panelId]));
		}

		/**
		 * 
		 * @return XScale in pixels per second 
		 */
		double getXScale() {
			return getWidth() / spectrogramParameters.displayLength;
		}


		/**
		 * Called when new spectrogram data arrive. 
		 * @param obs PAmDataBlock that sent the data
		 * @param dataUnit FFT Data unit
		 * @param panelNumber number of panel to update. 
		 */
		private void drawSpectrogram(PamObservable obs, FFTDataUnit dataUnit, int panelNumber) {
			/*
			 * check the image is the correct width and make a new one if necessary
			 * this tends to occur when the display is using a fixed number of
			 * pixels per fftSlice.
			 */
//			spectrogramParameters.wrapDisplay = true;

			if (spectrogramParameters.timeScaleFixed == false) {
				int newWidth = getVariableWidth();
				if (newWidth != specImage.getWidth()) {
					createAllImages();
				}
			}

			AcquisitionProcess daqProcess = null;
			// somewhat awkward double cast here !
			try {
				daqProcess = (AcquisitionProcess) ((PamDataBlock) obs).getSourceProcess();
			}
			catch (ClassCastException e) {
				return;
			}

			Complex[] fftData = dataUnit.getFftData();

			double[] colval;

			double dBLevel;

			// int xStart = imagePos;
			int xDraw = imagePos;

			int channelNumber = spectrogramParameters.channelList[panelNumber];

			int iBin = 0;
			daqProcess.prepareFastAmplitudeCalculation(channelNumber);


			synchronized (spectrogramDisplay) {
				imageHeight = specImage.getHeight();
				imageWidth = specImage.getWidth();
				/*
				 * minBin and maxBin are bins in the FFT data. 
				 */
				int maxBin = Math.min(freqBinRange[1], fftData.length - 1);
				int minBin = Math.max(0, freqBinRange[0]);

				if (maxBin < 0) return;
				/**
				 * With new scroll system, drawing on the base image will 
				 * always wrap - it will be given the appearance of 
				 * scrolling at the point it's rendered onto the actual display in two parts. 
				 */
				//				if (spectrogramParameters.wrapDisplay) {
				/*
				 * ImagePos is set to -1 when the image is created, we add one
				 * to it before drawing ever starts. 
				 */
				xDraw = imagePos;
				
				// for (int p = 0; p < spectrogramParameters.pixelsPerSlics; p++) {
				if (++xDraw >= imageWidth) {
					xDraw = 0;
					firstWrapDone = true;
					if (viewerMode) {
						imagePos = -1;
						return;
					}
				}
//				System.out.println(String.format("Draw slice %d of %d", xDraw, specImage.getWidth()));
				//				}
				//				else {
				//					imagePos = imageWidth - 2;
				//					xDraw = imagePos;
				//					specImage.xScrollImage(-1);
				//				}

				for (int i = minBin; i <= maxBin; i++) {
					dBLevel = daqProcess.fftAmplitude2dB(fftData[i].magsq(), channelNumber, 
							sourceFFTDataBlock.getFftLength(), true, true);
					//				dBLevel = 90;
					colval = colorValues[getColourIndex(dBLevel)];
					//				colVal = colourArray.getColours()[getColourIndex(dBLevel)].

					writableRaster.setPixel(xDraw, imageHeight - iBin - 1, colval);
					// }
					if (++iBin >= imageHeight) {
						break;
					}
				}
				// need to update currentTimeMilliseconds
				// at the same time as imagePos !
				imagePos = xDraw;// += spectrogramParameters.pixelsPerSlics;
				currentTimeMilliseconds = dataUnit.getTimeMilliseconds();
			}

			int newPos = imagePos;
			int len = imageHeight;
			// int lineWidth = Math.max(2, spectrogramParameters.pixelsPerSlics);
			//			if (spectrogramParameters.wrapDisplay) {
			newPos = imagePos;
			if (++newPos >= imageWidth) {
				newPos = 0;
				if (viewerMode) {
					imagePos = -1;
					return;
				}
			}
			if (viewerMode ==  false && spectrogramParameters.wrapDisplay) {
				for (int i = 0; i < len; i++) {
					writableRaster.setPixel(newPos, i, overlayColour);
				}
			}
			//			}
			//			else {
			//
			//			}

			repaint(50);

			if (panelId == 0) notifyDisplayPanels(0);
		}

		/**
		 * Used offline to fill in the background image using loaded
		 * FFT data. this will happen whenever new data are loaded
		 * (when the scroller moves) or when the image resizes. 
		 */
		private void drawBackgroundImage() {
			/*
			 * There is absolutely no guarantee that the loaded FFT data 
			 * period matches the time period of the window.
			 * It's also possible that there are gaps in the data.  
			 */
			clearImage();
			imagePos = -1;
			long s = viewerScroller.getValueMillis(); // start of image. 
			long e = s + (long) (spectrogramParameters.displayLength * 1000.); // end of image.
			ListIterator<FFTDataUnit> li;

			boolean first = true;

			double xPos, lastXPos=0;

			/**
			 * Work out time scale in pixels per millisecond.
			 * remembering that the rendering of the image at this 
			 * point is always 1:1 and it will stretched or shrunk by
			 * the gui when it's displayed.  
			 */
			//			double timeScale = (double) sourceFFTDataBlock.getFftHop() / sourceFFTDataBlock.getSampleRate() / 1000.;
			double timeScale = (double) sourceFFTDataBlock.getSampleRate() / (double) sourceFFTDataBlock.getFftHop() / 1000.;
			try {
				//				System.out.println("Enter fft datablock lock");
				synchronized (sourceFFTDataBlock.orderLock) {
					if (sourceFFTDataBlock.getOrderStatus()) {
						//				System.out.println("Source data block probably locked");
						return;
					}
					long t1 = System.currentTimeMillis();
					//					System.out.println("Enter fft datablock lock");
					synchronized (sourceFFTDataBlock) {
						//						System.out.println("Pass fft datablock lock");
						long t2 = System.currentTimeMillis() - t1;
						if (t2 > 1) {
							System.out.println("drawBackgroundImages blocked for " + t2 + " ms");
						}
						li = sourceFFTDataBlock.getListIterator(0);
						FFTDataUnit fftUnit;
						while (li.hasNext()) {
							fftUnit = li.next();
							if (((1<<spectrogramParameters.channelList[panelId]) & 
									fftUnit.getChannelBitmap()) == 0) {
								continue;
							}
							if (fftUnit.getTimeMilliseconds() < s) {
								continue;
							}
							/**
							 * the first fft data unit may arrive long after the start
							 * of the display, or there may be gaps, so work out the 
							 * x coordinate more explicitly based on millisecond time.  
							 * <br> 1 is added before drawing in draw spectrogram, so initialise
							 * at 1 less than expected value. 
							 */
							//						System.out.println(String.format("Draw spectrogram panel %d slice time %s", 
							//								this.panelId, PamCalendar.formatDateTime(fftUnit.getTimeMilliseconds())));
							xPos = (fftUnit.getTimeMilliseconds()-s) * timeScale;
							if (first) {
								first = false;
								imagePos = (int) xPos - 1;
							}
							else {
								// if there is a gap of > 1 pixel, reset, otherwise, ignore small descrepencies.
								if (xPos-lastXPos > 1.5) {
									imagePos = (int) xPos - 1;
								}
							}
							lastXPos = xPos;

							drawSpectrogram(sourceFFTDataBlock, fftUnit, panelId);
							if (imagePos < 0 || fftUnit.getTimeMilliseconds() > e) {
								break;
							}
						}
					}
				}
			}
			catch (ConcurrentModificationException cme) {
				return;
			}

		}

		/**
		 * Clear the background image. 
		 */
		private void clearImage() {
			if (specImage == null) {
				return;
			}
			Graphics g = specImage.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, specImage.getWidth(), specImage.getHeight());
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			Dimension newSize = getSize();

			int channel = spectrogramParameters.channelList[panelId];

			//			if (viewerMode) {
			directDrawProjector.clearHoverList();
			//			}

			//			double secs = newSize.width * sourceFFTDataBlock.getFftHop() / sampleRate;
			//			System.out.println(String.format("Width = %d pixs = %5.2fs", 
			//					newSize.width, secs));
			//			sayWithDate("Draw SpectrogramPanel");

			if (sourceFFTDataBlock == null || (
					(1<<channel & 
							sourceFFTDataBlock.getChannelMap()) == 0)) {
				g.setColor(Color.RED);
				FontMetrics fm = g.getFontMetrics();
				String errTxt = String.format("FFT Data unavailable for channel %d", 
						spectrogramParameters.channelList[panelId]);
				int labelWidth = fm.charsWidth(errTxt.toCharArray(), 0, errTxt.length());
				g.drawString(errTxt, newSize.width / 2 - labelWidth / 2, newSize.height /2 - fm.getHeight());
				errTxt = "Check FFT parameters or select a different channel to display";
				labelWidth = fm.charsWidth(errTxt.toCharArray(), 0, errTxt.length());
				g.drawString(errTxt, newSize.width / 2 - labelWidth / 2, newSize.height /2 + fm.getHeight());
				return;
			}

			if (!newSize.equals(panelSize)) {
				amplitudePanel.SetAmplitudePanelBorder();
				panelSize = newSize;
			}

			if (spectrogramParameters.timeScaleFixed == false) {
				int newWidth = getVariableWidth();
				if (newWidth != imageWidth) {
					createAllImages();
				}
			}

			if (specImage == null)
				return;

			Graphics2D g2d = (Graphics2D) g;

			double scaleWidthfactor = 1;
			scaleX = this.getWidth() / (double) specImage.getWidth(null);
			scaleY = this.getHeight() / (double) specImage.getHeight(null);
			AffineTransform xform = new AffineTransform();
			xform.translate(scaleX * ((1 - scaleWidthfactor) / 2)
					* specImage.getWidth(null), 0);
			xform.scale(scaleX * scaleWidthfactor, scaleY);
			BufferedImage imageToDraw = specImage;
			if (frozenImage != null) {
				imageToDraw = frozenImage;
			}
			if (frozenImage != null) {
				g2d.drawImage(frozenImage, xform, this);

				//				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

				if (mouseDownPoint != null && currentMousePoint != null) {
					int x = Math.min(mouseDownPoint.x, currentMousePoint.x);
					int y = Math.min(mouseDownPoint.y, currentMousePoint.y);
					int w = Math.abs(mouseDownPoint.x - currentMousePoint.x);
					int h = Math.abs(mouseDownPoint.y - currentMousePoint.y);
					g2d.setColor(markThis ? freezeColour : freezeColour2);
					//					g2d.setColor(Color.RED);
					g2d.setStroke(new BasicStroke(3));
					g2d.drawRect(x, y, w, h);
					if (x + w > getWidth()) {
						g2d.drawRect(x - getWidth(), y, w, h);
					}
					if (x < 0) {
						g2d.drawRect(x + getWidth(), y, w, h);
					}
				}
			}
			else {
				if (spectrogramParameters.wrapDisplay) {
					g2d.drawImage(specImage, xform, this);
				}
				else {
					// draw in two parts. 
					/*
					 * imagePos is the current x position within the image. 
					 * So drawing of the first part of the image will be from 
					 * imagePos+1 to the end of the image. Drawing of the second
					 * part will be from the start of the image to imagePos. 
					 * Just need to work out the equivalent xpos
					 */
					int imageDrawPos = imagePos+1;
					int screenXPos = (int) (this.getWidth() - imageDrawPos*scaleX);
					int winH = getHeight();
					int imH = specImage.getHeight();
//					Graphics gDraw = g;
					
//					g.setColor(Color.ORANGE);
//					g.drawLine(0, 0, getWidth(), getHeight()/2);
					if (firstWrapDone) {
						g.drawImage(specImage, 0, 0, screenXPos, winH, imageDrawPos, 0, 
								specImage.getWidth(), imH, null);
//						g.setColor(Color.RED);
//						g.drawLine(0, 10, screenXPos, winH/2);
					}
					g.drawImage(specImage, screenXPos, 0, getWidth(), winH, 0, 0, 
							imageDrawPos-1, imH, null);
//					g.setColor(Color.red);
//					g.drawLine(screenXPos, 0, screenXPos, getHeight());
//					g.drawImage(specImage, 0, winH/2, getWidth(), winH, 0, 0, 
//							specImage.getWidth(), imH/2, null);
//					g.setColor(Color.PINK);
//					g.drawLine(screenXPos+1, 10, getWidth(), winH/2);
//					g.drawLine(10, 50, 13, 53);
				}

				double millisPerBin = sampleRate / 1000. / sourceFFTDataBlock.getFftHop();
				spectrogramProjector.setScales(millisPerBin,
						2. / sourceFFTDataBlock.getFftLength(), specImage.getWidth(),
						specImage.getHeight());
				/*
				 * direct draw projector currently only used in viewer mode, but may change this
				 * to improve rendering of lines. 
				 */
				long displayStartTime = currentTimeMilliseconds;
				if (viewerMode == false) {
					displayStartTime -= spectrogramParameters.displayLength * 1000.;
				}
				double directImagePos;
				if (spectrogramParameters.wrapDisplay) {
					directImagePos = (double) imagePos * (double) getWidth() / specImage.getWidth();
				}
				else {
					directImagePos = getWidth();
				}
				directDrawProjector.setScales(getWidth(), getHeight(), 
						displayStartTime,
						spectrogramParameters.displayLength * 1000, 
						spectrogramParameters.frequencyLimits, sampleRate, directImagePos);
				//				directDrawProjector.setOffset(displayStartTime, imagePos);

				//				System.out.println("Draw overlay data");
				drawOverlayData(g);

				/**
				 * And a drawing progress bar for playback!
				 */
				if (playbackStatus == PlaybackProgressMonitor.PLAY_START &&
						viewerScroller != null &&
						(playbackChannels & (1<<channel)) != 0) {
					double pos = (playbackTimeMillis - viewerScroller.getValueMillis())/1000.;
					int playX = (int) timeAxis.getPosition(pos);
					g.setColor(Color.RED);
					((Graphics2D) g).setStroke(new BasicStroke(1));
					g.drawLine(playX, 0, playX, getHeight());
				}
			}
		}

		/**
		 * Draw graphic overlays (only used in offline viewer mode)
		 * @param g
		 */
		private void drawOverlayData(Graphics g) {
			//			sayWithDate("Draw overlay data");
			int n = viewedDataBlocks.size();
			for (int i = 0; i < n; i++) {
				drawOverlayData(g, viewedDataBlocks.get(i));
			}
		}

		/**
		 * Draw the overlay data for a specific data block
		 * <p>Only used in offline viewer mode
		 * @param g graphics handle
		 * @param usedDataBlock datablock reference.
		 */
		private void drawOverlayData(Graphics g, PamDataBlock usedDataBlock) {

			ListIterator<PamDataUnit> iterator;
			PamDataUnit dataUnit;
			//			System.out.println("Draw " + usedDataBlock.getDataName());
			//			sayWithDate(String.format("Draw %s from %s to %s", usedDataBlock.getDataName(),
			//					PamCalendar.formatDateTime(currentTimeMilliseconds -
			//							(long)(spectrogramParameters.displayLength * 1000.)), 
			//							PamCalendar.formatTime(currentTimeMilliseconds)));
			synchronized (usedDataBlock) {
				//				System.out.println("Unlocked" + usedDataBlock.getDataName());
				iterator = usedDataBlock.getListIterator(PamDataBlock.ITERATOR_END);
				while(iterator.hasPrevious()) {
					dataUnit = iterator.previous();
					if (dataUnit.getTimeMilliseconds() < currentTimeMilliseconds -
							(long)(spectrogramParameters.displayLength * 1000.)) {
						break;
					}
					if (dataUnit.getTimeMilliseconds() > currentTimeMilliseconds) {
						continue;
					}
					if ((1<<spectrogramParameters.channelList[panelId] & dataUnit.getChannelBitmap()) == 0) {
						continue;
					}
					usedDataBlock.drawDataUnit(g, dataUnit, directDrawProjector);
				}
			}

		}

		/*
		 * Now held within each spectrogram panel so that each panel
		 * can hold it's own settings. 
		 */
		JPopupMenu getPlotDetectorMenu(MouseEvent e) {
			if (spectrogramParameters == null) {
				return null;
			}
			int channel = spectrogramParameters.channelList[panelId];
			//			if ((detectorMenu == null && detectorDataBlocks != null) 
			//				|| (detectorDataBlocks != null && detectorDataBlocks.size() != checkMenuItems.length)) {
			detectorMenu = new JPopupMenu();
			detectorDataBlocks = PamController.getInstance().getDataBlocks(AcousticDataUnit.class, true);
			checkMenuItems = new JCheckBoxMenuItem[detectorDataBlocks.size()];
			DisplaySelection displaySelection = new DisplaySelection(this);
			SettingsAction settingsAction = new SettingsAction();
			JMenuItem menuItem;
			if (viewerMode && e != null) {
				if (playbackStatus == PlaybackProgressMonitor.PLAY_START) {
					menuItem = new JMenuItem("Stop playback");
					menuItem.addActionListener(new MenuCancelPlayback());
					detectorMenu.add(menuItem);
				}
				else {
					menuItem = new JMenuItem("Play channel " + channel + " from start");
					menuItem.addActionListener(new MenuPlayChannel(channel));
					detectorMenu.add(menuItem);		
					long currentTime = timeFromMouseX(e.getX());
					menuItem = new JMenuItem("Play channel " + channel + " from mouse position");
					menuItem.addActionListener(new MenuPlayFrom(channel, currentTime));
					detectorMenu.add(menuItem);		

					//					int playChannels = PlaybackControl.getViewerPlayback().getPlaybackParameters().channelBitmap;
					//					if (playChannels != 1<<channel) {
					//						menuItem = new JMenuItem("Play channel(s) " + PamUtils.getChannelList(playChannels));
					//						menuItem.addActionListener(new MenuPlayAll());
					//						detectorMenu.add(menuItem);
					//					}
				}
			}
			menuItem = new JMenuItem("Settings ...");
			menuItem.addActionListener(settingsAction);
			detectorMenu.add(menuItem);
			detectorMenu.addSeparator();


			PamDataBlock dataBlock;
			for (int i = 0; i < detectorDataBlocks.size(); i++) {
				dataBlock = detectorDataBlocks.get(i);
				if (dataBlock.canDraw(spectrogramProjector)) {
					checkMenuItems[i] = new JCheckBoxMenuItem(dataBlock
							.getDataName());
					checkMenuItems[i].addActionListener(displaySelection);
					checkMenuItems[i].setActionCommand(Integer.toString(i));
					detectorMenu.add(checkMenuItems[i]);
				}
			}			
			InitialisePlotDetectorMenu();
			if (panelClipBoardCopier == null && getFrame() != null) {
				panelClipBoardCopier = new ClipboardCopier(getFrame().getContentPane());
			}
			if (panelClipBoardCopier != null) {
				detectorMenu.addSeparator();
				detectorMenu.add(panelClipBoardCopier.getCopyMenuItem());
			}
			//			}
			return detectorMenu;
		}

		public long getRequiredDataHistory(PamObservable o, Object arg) {
			long history = (long) getXDuration();
			if (frozen) { // then the mouse is down !
				history = Math.max(history, PamCalendar.getTimeInMillis()-mouseDownTime); 
			}
			return history + 1000;
		}

		public String getObserverName() {
			return "Spectrogram display panel";
		}

		public void noteNewSettings() {
			//System.out.println("New spectrogram panel settings");
			//			setParams(spectrogramParameters);
		}

		public void setSampleRate(float sampleRate, boolean notify) {
			//			System.out.println("New spectrogram panel sampleRate");
		}

		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			spectrogramDisplay.masterClockUpdate(milliSeconds, sampleNumber);
		}

		public void update(PamObservable obs, PamDataUnit newData) {

			//for (int i = 0; i < spectrogramPanel.length; i++) {
			//			AcousticDataUnit acousticData = (AcousticDataUnit) newData;
			//			if ((1<<spectrogramParameters.channelList[panelId] & acousticData.getChannelBitmap()) == 0) {
			//				return;
			//			}
			//			Rectangle r = obs.drawDataUnit(specImage.getGraphics(), newData,
			//					spectrogramProjector);
			//			if (r != null) {
			//				//				System.out.println("Repaint spectorgram panel id " + panelId);
			//				spectrogramPanels[panelId].repaint();
			//			}
			//}

		}
		@Override
		public String getToolTipText(MouseEvent mouseEvent)  {
			/*
			 * Make a standard time / frequency string
			 * THen if it's viewer mode, also see if we're hovered
			 * over an object of some sort. 
			 */
			String str = getMousePosText(mouseEvent.getPoint());
			if (viewerMode) {
				String moreStr = 
					directDrawProjector.getHoverText(mouseEvent.getPoint());
				if (moreStr != null) {
					str += "<p>" + moreStr;
				}
			}
			return "<html>" + str + "</html>";
		}

		public String getMousePosText(Point pt) {
			double frequency = getPixelFrequency(pt.y);

			String str = FrequencyFormat.formatFrequency(frequency, true);

			if (viewerScroller != null) {
				long t = timeFromMouseX(pt.x);
				str = String.format("%s<p>%s %s s", 
						str, PamCalendar.formatDate(t), PamCalendar.formatTime(t, true));	
			}
			return str;
		}



		class DisplaySelection implements ActionListener {

			SpectrogramPanel spectrogramPanel;
			DisplaySelection(SpectrogramPanel spectrogramPanel) {
				this.spectrogramPanel = spectrogramPanel;
			}

			public void actionPerformed(ActionEvent e) {
				String str = e.getActionCommand();
				int menuId;
				try {
					menuId = Integer.valueOf(str);
				} catch (Exception ex) {
					return;
				}
				JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();

				PamDataBlock dataBlock = detectorDataBlocks.get(menuId);
				boolean show = menuItem.isSelected();
				spectrogramParameters.setShowDetector(panelId, menuId, show);
				if (show) {
					dataBlock.addObserver(spectrogramPanel);
				} else {
					dataBlock.deleteObserver(spectrogramPanel);
				}
				subscribeDataBlocks();
				repaint();
			}

		}

		class PopupListener extends MouseAdapter {

			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					getPlotDetectorMenu(e)
					.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (viewerMode && e.getClickCount() == 2) {
					if (playbackStatus == PlaybackProgressMonitor.PLAY_END) {
						int channel = spectrogramParameters.channelList[panelId];
						long currentTime = timeFromMouseX(e.getX());
						long dispStartMillis = viewerScroller.getValueMillis();
						long endMillis = dispStartMillis + viewerScroller.getVisibleAmount();
						PlaybackControl.getViewerPlayback().playViewerData(1<<channel, currentTime, endMillis, new PlayProgress());
					}
					else {
						PlaybackControl.getViewerPlayback().stopViewerPlayback();
					}
				}
			}
		}


		void InitialisePlotDetectorMenu() {
			if (detectorMenu == null) return;
			if (checkMenuItems == null) return;
			PamDataBlock dataBlock;
			detectorDataBlocks = PamController.getInstance().getDataBlocks(AcousticDataUnit.class, true);
			//			if (spectrogramParameters.showDetector == null || spectrogramParameters.showDetector[panelId] == null) return;
			spectrogramParameters.getShowDetector(panelId, detectorDataBlocks.size()-1); // force check on size of showDetector variables
			for (int i = 0; i < detectorDataBlocks.size(); i++) {
				//				checkMenuItem = (JCheckBoxMenuItem) detectorMenu.getComponent(i);
				dataBlock = detectorDataBlocks.get(i);
				if (dataBlock.canDraw(spectrogramProjector) && checkMenuItems[i] != null) {
					checkMenuItems[i].setSelected(spectrogramParameters.getShowDetector(panelId,i));
					if (spectrogramParameters.getShowDetector(panelId,i)) {
						detectorDataBlocks.get(i).addObserver(this);
					}
					else {
						detectorDataBlocks.get(i).deleteObserver(this);
					}
				}
			}
		}

		public void removeObservable(PamObservable o) {
			// TODO Auto-generated method stub

		}

		protected void freezeImage(boolean markThis) {
			BufferedImage oldImage = specImage;
			this.markThis = markThis;
			frozenTimeMilliseconds = currentTimeMilliseconds;
//			System.out.println("Frozen time = " + PamCalendar.formatTime(frozenTimeMilliseconds, true));
			frozenImagePos = imagePos;
			frozenImage = new BufferedImage(oldImage.getWidth(), oldImage.getHeight(), 
					BufferedImage.TYPE_INT_RGB);
			Graphics g = frozenImage.getGraphics();
			if (spectrogramParameters.wrapDisplay) {
				g.drawImage(oldImage, 0, 0, null);	
			}
			else {
				int imageDrawPos = imagePos+1;
				int screenXPos = (int) (this.getWidth() - imageDrawPos);
				if (firstWrapDone) {
					g.drawImage(specImage, 0, 0, screenXPos, oldImage.getHeight(), imageDrawPos, 0, 
							specImage.getWidth(), oldImage.getHeight(), null);
					//				g.setColor(Color.RED);
					//				g.drawLine(0, 10, screenXPos, winH/2);
				}
				g.drawImage(specImage, screenXPos, 0, getWidth(), oldImage.getHeight(), 0, 0, 
						imageDrawPos-1, oldImage.getHeight(), null);
			}
		}

		protected void unFreezeImage() {
			if (frozenImage == null) return;
			//			if (e.getButton() != MouseEvent.BUTTON1) return;
			frozenImage = null;
		}

		protected void sayMousePos(Point pt) {
			//			String str = getMousePosText(pt);
			//			setToolTipText("<html>" + str + "</html>");
		}

		long timeFromMouseX(int mouseX) {
			long t = (long) (mouseX * 1000. * spectrogramParameters.displayLength / getWidth());
			t += viewerScroller.getValueMillis();
			return t;
		}

	}


	private void freezeImages(SpectrogramPanel selectedPanel) {
		for (int i = 0; i < spectrogramPanels.length; i++) {
			spectrogramPanels[i].freezeImage(spectrogramPanels[i] == selectedPanel);
		}
		frozen = true;
	}

	private void unFreezeImages() {
		for (int i = 0; i < spectrogramPanels.length; i++) {
			spectrogramPanels[i].unFreezeImage();
		}
		frozen = false;
	}

	class SpecPanelMouse extends MouseAdapter implements MouseMotionListener {

		SpectrogramPanel spectrogramPanel;

		long mouseDownTime, currentMouseTime;
		double mouseDownFrequency, currentMouseFrequency;

		public SpecPanelMouse(SpectrogramPanel spectrogramPanel) {
			super();
			this.spectrogramPanel = spectrogramPanel;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (nMarkObservers == 0) return;
			if (e.getButton() != MouseEvent.BUTTON1) return;
			super.mousePressed(e);
			currentMousePoint = mouseDownPoint = e.getPoint();
			freezeImages(spectrogramPanel);
			//			BufferedImage oldImage = spectrogramPanel.specImage;
			//			spectrogramPanel.frozenImage = new BufferedImage(oldImage.getWidth(), oldImage.getHeight(), 
			//					BufferedImage.TYPE_INT_RGB);
			//			spectrogramPanel.frozenImage.getGraphics().drawImage(oldImage, 0, 0, null);	
			fireMouseDownEvents(spectrogramPanel, mouseDownPoint);		
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
			//			if (spectrogramPanel.frozenImage == null) return;
			////			if (e.getButton() != MouseEvent.BUTTON1) return;
			fireMouseUpEvents(spectrogramPanel, mouseDownPoint, e.getPoint());
			unFreezeImages();
			//			spectrogramPanel.frozenImage = null;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			spectrogramPanel.sayMousePos(e.getPoint());
			if (spectrogramPanel.frozenImage == null) return;
			currentMousePoint = e.getPoint();
			for (int i = 0; i < spectrogramPanels.length; i++) {
				spectrogramPanels[i].repaint();
			}

		}

		@Override
		public void mouseMoved(MouseEvent e) {
			spectrogramPanel.sayMousePos(e.getPoint());
		}
	}

	class SplitPaneListener implements AncestorListener {

		public void ancestorAdded(AncestorEvent event) {
			//			spectrogramAxis.repaint();		
			repaintAll();
		}

		public void ancestorMoved(AncestorEvent event) {
			//			spectrogramAxis.repaint();		
			repaintAll();		
		}

		public void ancestorRemoved(AncestorEvent event) {
			//			spectrogramAxis.repaint();			
			repaintAll();	
		}

	}

	public SpectrogramParameters getSpectrogramParameters() {
		if (getFrame() != null) {
			spectrogramParameters.boundingRectangle = getFrame().getBounds();
		}
		if (spectrogramPlotPanel != null && spectrogramPlotPanel.splitPane != null) {
			spectrogramParameters.splitDividerLocation = spectrogramPlotPanel.splitPane.getDividerLocation();
		}
		return spectrogramParameters;
	}

	DisplayPanelProvider displayPanelProvider;
	Vector<DisplayPanel> displayPanels = new Vector<DisplayPanel>();
	Vector<DisplayPanel> oldDisplayPanels;
	@Override
	public void notifyModelChanged(int changeType) {
		// get's called whenever a new unit is added or removed to some part of the Pam model
		if (PamController.getInstance().isInitializationComplete()) {
			if (changeType == PamControllerInterface.ADD_CONTROLLEDUNIT) {
				if (sourceFFTDataBlock == null) noteNewSettings(); // give it a try !
				if (spectrogramPlotPanel != null) {
					spectrogramPlotPanel.LayoutPlots();
				}

			}
			else if (changeType == PamControllerInterface.REMOVE_CONTROLLEDUNIT) {
				noteNewSettings();
				if (spectrogramPlotPanel != null) {
					spectrogramPlotPanel.LayoutPlots();
				}
			}
			else if (changeType == PamControllerInterface.NEW_SCROLL_TIME) {
				newScrollTime();
			}
		}
		if (changeType ==PamControllerInterface.INITIALIZATION_COMPLETE) {
			noteNewSettings();
			createAllImages();
			repaintAll();
			//			if (spectrogramPlotPanel != null) {
			//				spectrogramPlotPanel.LayoutPlots();
			//			}
		}
	}

	private void newScrollTime() {
		repaintAll();
	}



	@Override
	public void internalFrameClosing(InternalFrameEvent e) {
		if (sourceFFTDataBlock != null) {
			sourceFFTDataBlock.deleteObserver(this);
		}
	}

	public void noteNewSettings() {
		setParams(spectrogramParameters);
	}

	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub

	}

	/**
	 * Used to get the time in milliseconds from a pixel number when 
	 * the user clicks on the display. Since the display is wrapping around, 
	 * this time is always going to be less than the currentXTime below.
	 * @param pixel
	 * @return time in milliseconds
	 */
	private long getPixelXTime(int pixel) {
		int pixsFromNow = pixel - (int) getCurrentXPixel();
		if (pixsFromNow > 0) {
			pixsFromNow -= spectrogramPanels[0].getWidth();
		}
		return (long) (getCurrentXTime() + pixsFromNow * getXDuration() / spectrogramPanels[0].getWidth());
	}

	/**
	 * 
	 * @param pixels
	 * @return time in milliseconds
	 */
	private long getRelativePixelTime(int pixels) {
		return (long) (pixels * getXDuration() / spectrogramPanels[0].getWidth());
	}

	/**
	 * convert a pixel number into a frequency.
	 * @param pixel
	 * @return frequency in Hz
	 */
	private double getPixelFrequency(int pixel) {
		return  (1.-(double) pixel / spectrogramPanels[0].getHeight()) * 
		(spectrogramParameters.frequencyLimits[1]-spectrogramParameters.frequencyLimits[0]) +
		spectrogramParameters.frequencyLimits[0];
	}


	@Override
	synchronized public double getCurrentXPixel() {
		if (spectrogramParameters.wrapDisplay) {
			if (spectrogramPanels[0].frozenImage == null) {
			return (double) spectrogramPanels[0].imagePos * spectrogramPanels[0].getWidth()/ getImageWidth();
			}
			else {
				return (double) spectrogramPanels[0].frozenImagePos * spectrogramPanels[0].getWidth()/ getImageWidth();
			}
		}
		else {
			return spectrogramPanels[0].getWidth();
		}
	}

	@Override
	synchronized public long getCurrentXTime() {
		if (spectrogramPanels[0].frozenImage == null) {
			return spectrogramPanels[0].currentTimeMilliseconds;
		}
		else {
			return spectrogramPanels[0].frozenTimeMilliseconds;
		}
	}

	/* (non-Javadoc)
	 * @see Layout.DisplayPanelContainer#wrapDisplay()
	 */
	@Override
	public boolean wrapDisplay() {
		return spectrogramParameters.wrapDisplay;
	}

	/**
	 * @return the display length in milliseconds. 
	 */
	@Override
	public double getXDuration() {
		return spectrogramParameters.displayLength * 1000.;
	}

	public void panelNotify(int noteType) {
		switch (noteType) {
		case DRAW_BORDER:
			//			spectrogramAxis.repaint();
			repaintAll();
			break;
		}

	}

	private void notifyDisplayPanels(int noteType) {
		if (displayPanels == null) return;
		for (int i = 0; i < displayPanels.size(); i++) {
			displayPanels.get(i).containerNotification(this, noteType);
		}
	}

	private void notifyMarkObservers(int downUp, int channel, long startTime, long duration, double f1, double f2) {
		if (spectrogramParameters.useSpectrogramMarkObserver == null) return;
		int n = Math.min(spectrogramParameters.useSpectrogramMarkObserver.length,
				SpectrogramMarkObservers.getSpectrogramMarkObservers().size());
		for (int i = 0; i < n; i++) {
			if (spectrogramParameters.useSpectrogramMarkObserver[i] == false) {
				continue;
			}
			SpectrogramMarkObservers.getSpectrogramMarkObservers().get(i).spectrogramNotification(this,
					downUp, channel, startTime, duration, f1, f2);
		}
	}

	@Override
	public int getFrameType() {
		return UserFramePlots.FRAME_TYPE_SPECTROGRAM;
	}

	private void fireMouseDownEvents(SpectrogramPanel spectrogramPanel, Point mouseDown) {
		mouseDownTime = getPixelXTime(mouseDown.x);
		double f1 = getPixelFrequency(mouseDown.y);
		int channel = spectrogramParameters.channelList[spectrogramPanel.panelId];
		notifyMarkObservers(SpectrogramMarkObserver.MOUSE_DOWN, channel, mouseDownTime, 
				0, f1, f1);
	}

	private void fireMouseUpEvents(SpectrogramPanel spectrogramPanel, Point mouseDown, Point mouseUp) {
		if (mouseDown == null || mouseUp == null) {
			return;
		}
//		if (mouseDown.equals(mouseUp)) {
			// should get out - just clicked - leave up to observers to decide on that !
//		}
		long t1 = getPixelXTime(mouseDown.x);
		long dt = getRelativePixelTime(mouseUp.x-mouseDown.x);
		long startTime = Math.min(t1, t1+dt);
		long duration = Math.abs(dt);
		double f1 = getPixelFrequency(Math.max(mouseDown.y, mouseUp.y));
		double f2 = getPixelFrequency(Math.min(mouseDown.y, mouseUp.y));
		int channel = spectrogramParameters.channelList[spectrogramPanel.panelId];
		notifyMarkObservers(SpectrogramMarkObserver.MOUSE_UP, channel, startTime, 
				duration, f1, f2);
		//		System.out.println(String.format("Event channel %d - start %s; duration %4.3fs; Frequency %4.1f to %4.1fHz",
		//				spectrogramParameters.channelList[spectrogramPanel.panelId],
		//				PamCalendar.formatTime(startTime), (double) duration / 1000., f1, f2));
	}

	public FFTDataBlock getSourceFFTDataBlock() {
		return sourceFFTDataBlock;
	}

	public PamRawDataBlock getSourceRawDataBlock() {
		return sourceRawDataBlock;
	}

	/**
	 * Should receive play commands from the top toolbar. 
	 */
	public void playViewerSound() {
		long startMillis = viewerScroller.getValueMillis();
		long endMillis = startMillis + viewerScroller.getVisibleAmount();
		PlaybackControl.getViewerPlayback().playViewerData(startMillis, endMillis, new PlayProgress());
	}

	/**
	 * @return the colourArray
	 */
	public ColourArray getColourArray() {
		return colourArray;
	}
}
