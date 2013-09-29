
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

package clickDetector;

import java.awt.Adjustable;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import binaryFileStorage.DataUnitFileInformation;

import pamMaths.PamVector;
import pamScrollSystem.AbstractPamScroller;
import pamScrollSystem.PamScrollObserver;
import pamScrollSystem.PamScroller;
import pamScrollSystem.RangeSpinner;
import pamScrollSystem.RangeSpinnerListener;
import soundPlayback.PlaybackControl;
import soundPlayback.PlaybackDataServer;
import soundPlayback.PlaybackProgressMonitor;

import Array.ArrayManager;
import Layout.PamAxis;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.PamDetection;
import PamDetection.RawDataUnit;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamView.ColorManaged;
import PamView.CornerLayoutContraint;
import PamView.CtrlKeyManager;
import PamView.GroupedSourcePanel;
import PamView.JBufferedPanel;
import PamView.KeyPanel;
import PamView.PamCheckBox;
import PamView.PamColors;
import PamView.PamKeyItem;
import PamView.PamLabel;
import PamView.PamPanel;
import PamView.PamSymbol;
import PamView.TextKeyItem;
import PamView.PamColors.PamColor;
import PamView.zoomer.ZoomShape;
import PamView.zoomer.Zoomable;
import PamView.zoomer.Zoomer;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import clickDetector.ClickClassifiers.ClickIdInformation;
import clickDetector.ClickClassifiers.ClickIdentifier;
import clickDetector.dialogs.ClickDisplayDialog;
import clickDetector.offlineFuncs.OfflineEventDataBlock;
import clickDetector.offlineFuncs.OfflineEventDataUnit;

public class ClickBTDisplay extends ClickDisplay implements PamObserver, PamSettings {

	private ClickControl clickControl;

	private BTDisplayParameters btDisplayParameters = new BTDisplayParameters();

	private PamScroller hScrollBar;

	private PamDataBlock<ClickDetection> trackedClicks;

	protected  BTPlot btPlot;

	private BTAxis btAxis;

	private TopControls topControls;

	private JLabel timeLabel;

	private JCheckBox followCheckBox;

	private PamAxis yAxis, xAxis;

	private int lastAxisType;

	private int displayNumber = 0;

	private boolean hasData = false;

	private Zoomer zoomer;

	private JScrollBar vScrollBar;

	private VScaleManager[] vScaleManagers;

	private OfflineEventDataBlock offlineEventDatablock;

	private CtrlKeyManager ctrlKeyManager;


	/**
	 * Available time ranges for the BT display in seconds. 
	 */
	private int timeRanges[] = { 3600, 2700, 1800, 1200, 900, 600, 300, 120, 60, 30,
			20, 10, 5, 2, 1};

	/**
	 * Current Display Window start time in milliseconds
	 */
	private long displayStartMillis; // milliseconds

	/**
	 * Current Display length in samples. 
	 */
	private long displayLengthMillis; // milliseconds

	private float sampleRate = 48000;

	private double xScale, yScale, yStart;

	private boolean mouseDown;

	private boolean didFollow;

	private static PamSymbol defaultSymbol;

	private int scrollChecks;

	private ClickDetection selectedClick;

	private HScrollManager hScrollManager;

	static private final int minPaintTime = 1000;

	private PamSymbol highlightSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 20, 20, false, Color.WHITE, Color.GRAY);

	public PamLabel cursorLabel;

	private boolean isViewer, isNetReceiver;

	private ClickPlaybackMonitor clickPlaybackMonitor = new ClickPlaybackMonitor();

	private ClickBTDisplay clickBTDisplay;

	public ClickBTDisplay(ClickControl clickControl, ClickDisplayManager clickDisplayManager, ClickDisplayManager.ClickDisplayInfo clickDisplayInfo) {

		super(clickControl, clickDisplayManager, clickDisplayInfo);

		clickBTDisplay = this;

		this.clickControl = clickControl;
		
		/* 
		 * count the number of bearing time displays and
		 * set the displayNumber 
		 */
		displayNumber = clickDisplayManager.countDisplays(ClickBTDisplay.class);
		
		PamSettingManager.getInstance().registerSettings(this);

		ctrlKeyManager = new CtrlKeyManager();

		isViewer = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;
		isNetReceiver = PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER;



		btAxis = new BTAxis();

		setAxisPanel(btAxis);

		setPlotPanel(new BTPlotFrame());

		PlotMouse plotMouse = new PlotMouse();
		btPlot.addMouseListener(plotMouse);
		btPlot.addMouseMotionListener(plotMouse);
		btPlot.setFocusable(true);
		btPlot.addKeyListener(ctrlKeyManager);

		btPlot.requestFocus();

		//		getPlotPanel().addKeyListener(new PlotKeyListener());
		btPlot.addKeyListener(new PlotKeyListener());

		//				getAxisPanel().addKeyListener(new PlotKeyListener());
		topControls = new TopControls();
		setNorthPanel(topControls);

		trackedClicks = clickControl.getClickDetector().getTrackedClicks();

		highlightSymbol.setLineThickness(3);

		vScaleManagers = new VScaleManager[4];
		vScaleManagers[0] = new BearingScaleManager();
		vScaleManagers[1] = new ICIScaleManager();
		vScaleManagers[2] = new AmplitudeScaleManager();
		vScaleManagers[3] = new SlantScaleManager();
		getVScaleManager().setSelected();

		// set the time range after everything is constructed.
		rangeSpinner.setSpinnerValue(btDisplayParameters.getTimeRange());

		if (clickControl.isViewerMode()) {
			offlineEventDatablock = clickControl.getClickDetector().getOfflineEventDataBlock();
			offlineEventDatablock.addObserver(this);
		}

		// was in start, put here so it works with the net receiver. 
		clickControl.clickDetector.getClickDataBlock().addObserver(this);
	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}

	@Override
	public void created() {
		super.created();
		getFrame().addInternalFrameListener(new BTListener());
		//		getFrame().addKeyListener(new PlotKeyListener());
		followCheckBox.addKeyListener(new PlotKeyListener());
	}

	class BTListener extends InternalFrameAdapter {

		@Override
		public void internalFrameActivated(InternalFrameEvent e) {

			clickDisplayManager.setBAutoScroll(followCheckBox.isSelected());
		}

	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		if (o == clickControl.getClickDataBlock()) {
			return hScrollBar.getMaximumMillis()-hScrollBar.getMinimumMillis();
		}
		return 0;
	}

	@Override
	public void update(PamObservable obs, PamDataUnit newData) {
		// should get here everytime there is a new raw data unit - check the
		// scroll occasionally - every 20th should be more than enough

		//		if (this.btPlot.isShowing() == false) {
		//			System.out.println("Plot not showing");
		//			return;
		//		}

		if (clickControl.getClickDataBlock().getUnitsCount() > 0) {
			hasData = true;
		}

		if (obs == clickControl.getClickDataBlock()) {
			newClick((ClickDetection) newData);
			if (isNetReceiver) {
				newScrollTimingData(newData.getTimeMilliseconds());
			}
		}
		else if (obs == offlineEventDatablock) {
			changedEvent((OfflineEventDataUnit) newData);
		}
		else { // it must be raw data if it's not a click
			newRawData ((PamDataBlock) obs, (RawDataUnit) newData);
		}
	}

	private void changedEvent(OfflineEventDataUnit event) {
		btPlot.repaint(10);
	}

	public void newClick(ClickDetection clickDataUnit) {
		if (shouldPlot(clickDataUnit)){
			//			btPlot.drawClick(btPlot.getImageGraphics(), clickDataUnit, null);
			//			btPlot.repaint(minPaintTime);
		}
		if (followCheckBox.isSelected() == true) {
			selectedClick = null;
		}

	}



	/**
	 * 
	 * @return display length in milliseconds
	 */
	private long getDisplayLength() {
		return displayLengthMillis;
	}

	private long getDisplayStartMillis() {
		return displayStartMillis;
	}

	long lastRawScrollCheck = 0;
	/**
	 * during real time operation the scrolling is all controlled by the arrival of 
	 * new raw data unis observed by the BT display.  This is now delegated to 
	 * newScrollTimingData so that it cab be called for clicks arriving from the
	 * network receiver. 
	 * @param rawDataBlock
	 * @param newRawData
	 */
	public void newRawData(PamDataBlock rawDataBlock, RawDataUnit newRawData) {
		/*
		 * Calculate how many millis to wait before scrolling
		 * which should either be 1/60 the display, min 100, max 3000
		 */
		newScrollTimingData(newRawData.getTimeMilliseconds());
		//		double displaySecs = getDisplayLength() / 1000.;
		//		int minMillis = (int) (displaySecs*1000./60.);
		//		minMillis = Math.min(Math.max(100, minMillis),3000);
		//		if (newRawData.getTimeMilliseconds() >= lastRawScrollCheck + minMillis ||
		//				newRawData.getTimeMilliseconds() < lastRawScrollCheck) {
		//			//			hScrollManager.setupScrollBar(newRawData.getTimeMilliseconds());
		//			long runMillis = (newRawData.getTimeMilliseconds());
		//			if (hScrollManager.setupScrollBar(runMillis)) {
		//				repaintBoth();
		//				topControls.ShowTime();
		//			}
		//			//			checkScroll(newRawData.getStartSample() + newRawData.getDuration());
		//			lastRawScrollCheck = newRawData.getTimeMilliseconds();
		//		}
	}

	public void newScrollTimingData(long millis) {
		/*
		 * Calculate how many millis to wait before scrolling
		 * which should either be 1/60 the display, min 100, max 3000
		 * but do have to scroll at least that far past the latest data
		 * so that new clicks will be instantly seen. 
		 */
		double displaySecs = getDisplayLength() / 1000.;
		int minMillis = (int) (displaySecs*1000./60.);
		minMillis = Math.min(Math.max(100, minMillis),3000);
		if (millis >= lastRawScrollCheck + minMillis ||
				millis < lastRawScrollCheck) {
			//			hScrollManager.setupScrollBar(newRawData.getTimeMilliseconds());
			long runMillis = (millis);
			if (hScrollManager.setupScrollBar(runMillis+minMillis)) {
				repaintBoth();
				topControls.ShowTime();
			}
			//			checkScroll(newRawData.getStartSample() + newRawData.getDuration());
			lastRawScrollCheck = millis;
		}
	}

	public String getObserverName() {
		return "click detector bearing time display";
	}

	/**
	 * Called just before data collection starts
	 */
	public void reset() {
		displayStartMillis = 0;
		if (hScrollManager != null) {
			hScrollManager.reset();
		}

		int channels = clickControl.clickParameters.channelBitmap;
		int[] channelGroups = clickControl.clickParameters.channelGroups;
		int nChannelGroups = GroupedSourcePanel.countChannelGroups(channels, channelGroups);
		//		if (GroupedSourcePanel.)
		if (GroupedSourcePanel.getGroupIndex(btDisplayParameters.displayChannels,
				channels, channelGroups) < 0) {
			btDisplayParameters.displayChannels = 0;
		}
		//		hScrollManager.setupScrollBar(0);
		//		setupTimeBar();
		repaintBoth();
		btAxis.makeAxis();
	}

	public void setSampleRate(float sampleRate, boolean notify) {
		this.sampleRate = sampleRate;
		setScales();
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub

	}

	//	class OfflineHorizontalListener implements AdjustmentListener {
	//		public void adjustmentValueChanged(AdjustmentEvent e) {
	//			offlineHorizontalAdjustment(e);
	//		}
	//	}
	//
	//	class HorizontalListener implements AdjustmentListener {
	//		public void adjustmentValueChanged(AdjustmentEvent e) {
	//			SetScales();
	//			//repaintBoth();
	//		}
	//	}

	//	class VerticalListener implements AdjustmentListener {
	//		public void adjustmentValueChanged(AdjustmentEvent e) {
	//			if (e.getValueIsAdjusting()) {
	//				return;
	//			}
	//			displayLengthMillis = timeRanges[e.getValue()] * (long) 1000;
	//			btDisplayParameters.vScrollValue = e.getValue();
	//			hScrollManager.vertScrollChanged();
	//
	//			repaintBoth();
	//			//			setupTimeBar();
	//		}
	//	}

	/**
	 * Sets the x and y scales. Should be used in the same way 
	 * both in real time and offline. 
	 */
	private void setScales() {
		// if (displayLength == 0)
		//		displayLengthMillis = timeRanges[rangeScrollBar.getValue()] * (int) 1000;
		if (rangeSpinner == null) {
			return;
		}
		displayLengthMillis = (int) (rangeSpinner.getSpinnerValue() * 1000.);

		xAxis.setRange(0, getDisplayLength() / 1000.);

		displayStartMillis = hScrollBar.getValueMillis();
		Rectangle r = btPlot.getBounds();
		xScale = (double) r.width / (double) getDisplayLength();
		yScale = r.height / 180.;
		VScaleManager sm = getVScaleManager();
		if (sm != null) {
			sm.calculateScales();
		}
		//		switch(btDisplayParameters.VScale){
		//		case BTDisplayParameters.DISPLAY_BEARING:
		//			yScale = r.height / 180.;
		//			yStart = 0;
		//			if (view360()) {
		//				yScale/=2.;
		//				yStart = -180;
		//			}
		//			break;
		//		case BTDisplayParameters.DISPLAY_AMPLITUDE:
		//			yScale = r.height / (btDisplayParameters.amplitudeRange[1] - 
		//					btDisplayParameters.amplitudeRange[0]);
		//			yStart = btDisplayParameters.amplitudeRange[0];
		//			break;
		//		case BTDisplayParameters.DISPLAY_ICI:
		//			yScale = r.height / btDisplayParameters.maxICI;
		//			yStart = 0;
		//			break;
		//		}
	}

	/*
	 * Horizontal scroll bar policy and updates
	 * Units
	 * Units are milliseconds for min max, value, step, etc. 
	 * Minimum is always zero
	 * Maximum is current clock time - the display width, (poss rounded up to nearest second) 
	 * 
	 * Real Time
	 * Maximum value increases steadily using values from ???
	 * Scroll bar stays over to right hand side to that 
	 * current value is steadily increasing. 
	 * When the time width changes, will need to update the 
	 * maximum value. 
	 * if follow check box is not checked, then value doesn't update. 
	 * 
	 * Offline
	 * When a new store is opened, set the new max value. 
	 * When the time width changes, will need to update the 
	 * maximum value. 
	 * 
	 */
	/**
	 * setup the horizontal scroll bar
	 */
	//	void setupTimeBar() {
	//		if (clickControl.clicksOffline != null) {
	////			setupOfflineTimebar();
	//		}
	//		else {
	//			setupRTTimeBar();
	//		}
	//	}

	class ZoomableInterface implements Zoomable {

		@Override
		public boolean canStartZoomArea(MouseEvent mouseEvent) {
			return true;
		}
		
		@Override
		public boolean canClearZoomShape(MouseEvent mouseEvent){
			return !trackClick(mouseEvent);
		}

		@Override
		public int getCoordinateType() {
			return btDisplayParameters.VScale;
		}

		@Override
		public double getXScale() {
			return xScale;
		}

		@Override
		public double getXStart() {
			return getDisplayStartMillis();
		}

		@Override
		public double getXRange() {
			return getDisplayLength();
		}

		@Override
		public double getYScale() {
			return yScale;
		}

		@Override
		public double getYStart() {
			return yStart;
		}

		@Override
		public double getYRange() {
			return getVScaleManager().getCurrentRange();
		}

		@Override
		public void zoomPolygonComplete(ZoomShape zoomShape) {
			//System.out.println("Zoom Polygon Complete");
			btPlot.setTotalRepaint();
			btPlot.repaint();

			makeMarkedClickList();
			if (clickControl.getClicksOffline() != null) {
				clickControl.getClicksOffline().newMarkedClickList(zoomShape, clickBTDisplay);
			}
		}

		@Override
		public void zoomShapeChanging(ZoomShape zoomShape) {
			btPlot.repaint();
		}

		@Override
		public void zoomToShape(ZoomShape zoomShape) {
			rangeSpinner.setSpinnerValue(zoomShape.getXLength()/1000.);
			hScrollBar.setValueMillis((long)zoomShape.getXStart());
			if (getVScaleManager() != null) {
				getVScaleManager().setZoom(zoomShape);
			}
		}

	}

	class VScrollListener implements AdjustmentListener {
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			if (getVScaleManager() != null) {
				getVScaleManager().adjustmentValueChanged(e);
			}
		}
	}

	/**
	 * 
	 * @return the appropriate manager for the vertical scale. 
	 */
	private VScaleManager getVScaleManager() {
		return getVScaleManager(btDisplayParameters.VScale);
	}

	/**
	 * 
	 * @param vScaleType type of vertical scaleS
	 * @return the appropriate manager for the vertical scale. 
	 */
	private VScaleManager getVScaleManager(int vScaleType) {
		if (vScaleManagers == null) {
			return null;
		}
		return vScaleManagers[vScaleType];
	}

	/**
	 * Class for managing vertical scales and scrolls which will either
	 * be bearing, ICI or Amplitude.  
	 * <p> Jobs this beast has to do are:
	 * <p> set up the scroll bar at start and after a zoom
	 * <p> set the yStart and yScale values whatever the zoom state
	 * <p> write the correct values and titles on the y axis. 
	 * 
	 * @author Doug Gillespie
	 *
	 */
	abstract class VScaleManager implements AdjustmentListener {

		double currentStart, currentRange;

		protected ZoomShape lastZoomShape;

		public void restoreDefaults() {
			lastZoomShape = null;
			setupScrollBar(getMin(), getMax()-getMin());
		}

		public void setSelected() {
			if (checkNoZoom() == true) {
				setupScrollBar(currentStart, currentRange);
			}
		}

		public abstract int getCoordinateType();

		/**
		 * Check for there being no zooms in this coordinate frame
		 * <p> If there was no zoom, restore defaults and return true. 
		 * @return true if there were no zooms, and false otherwise
		 */
		public boolean checkNoZoom() {
			ZoomShape lastShape = getLastZoom();
			if (lastShape == null) {
				restoreDefaults();
				return true;
			}
			return false;
		}

		public ZoomShape getLastZoom() {
			if (zoomer == null) {
				return null;
			}
			return zoomer.findLastZoom(getCoordinateType());
		}

		public void setZoom(ZoomShape zoomShape) {
			lastZoomShape = zoomShape;
		}

		public void calculateScales() {
			yStart = currentStart;
			yScale = btPlot.getHeight() / currentRange;
		}

		public void setupScrollBar(double start, double page) {
			currentStart = start;
			currentRange = page;
			if (vScrollBar == null) {
				return;
			}
			double min = Math.min(getMin(), getMax());
			double max = Math.max(getMin(), getMax());
			vScrollBar.setMinimum((int) Math.floor(min/getStep()));
			vScrollBar.setMaximum((int) Math.ceil(max/getStep()));
			page = Math.abs(page);
			int visAmount = (int) (page/getStep());
			int scValue = valueToScroll(start, visAmount);
			vScrollBar.setValue(scValue);
			vScrollBar.setVisibleAmount(visAmount);
			if (vScrollBar.getValue() != scValue) {
				vScrollBar.setValue(scValue);
			}
			//			int v2 = vScrollBar.getVisibleAmount();
			//			if (v2 != visAmount) {
			//				System.out.println(String.format("Vis is %d, wanted %d", v2, visAmount));
			//			}
			vScrollBar.setUnitIncrement(Math.max(1, visAmount/10));
			vScrollBar.setBlockIncrement(Math.max(1, visAmount*8/10));
		}

		/**
		 * convert a value to a scroll bar position
		 * @param value value to convert 
		 * @param visAmount visible amount of scroll br
		 * @return new scroll bar position
		 */
		public abstract int valueToScroll(double value, int visAmount);

		/**
		 * Convert a scroll bar position back into  avalue. 
		 * @param scrollPos scroll position
		 * @return new value
		 */
		public abstract double scrollToValue(int scrollPos);

		/**
		 * Get unit step for scroll bar (e.g. 0.1 if you want a bearing scroll to 
		 * be in 1/10 degree steps). 
		 * @return unit step
		 */
		public abstract double getStep();

		/**
		 * @return the maximum value for the scroll bar 
		 */
		public abstract double getMin();

		/**
		 * 
		 * @return the minimum value for the scroll bar 
		 */
		public abstract double getMax();

		/**
		 * 
		 * @return the current value at the bottom of the display
		 */
		public double getCurrentStart() {
			return currentStart;
		}

		/**
		 * the range from the bottom to the top of the display 
		 * (may be negative if top of display has smaller values than bottom)
		 * @return range
		 */
		public double getCurrentRange() {
			return currentRange;
		}

		/**
		 * Value at the top of the display
		 * @return
		 */
		public double getCurrentEnd() {
			return currentStart + currentRange;
		} 

		/**
		 *
		 * @return  Title to use in the display axis. 
		 */
		public abstract String getTitle();

		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			currentStart = scrollToValue(e.getValue());
			//			currentRange = vScrollBar.getVisibleAmount()*getStep();
			repaintBoth();
		}

		public abstract double getYAxisInterval();
	}

	private class BearingScaleManager extends VScaleManager {

		public BearingScaleManager() {
			super();
			restoreDefaults();
		}

		@Override
		public int getCoordinateType() {
			return BTDisplayParameters.DISPLAY_BEARING;
		}

		@Override
		public double scrollToValue(int scrollPos) {
			int maxS = vScrollBar.getMaximum()-vScrollBar.getVisibleAmount();
			double val = getMin()-(maxS-scrollPos)*getStep();
			//			System.out.println(String.format("scrollToValue %d of %d converts to %3.2f", 
			//					scrollPos, maxS, val));
			return val;
		}

		@Override
		public int valueToScroll(double value, int visibleAmount) {
			int maxS = vScrollBar.getMaximum()-visibleAmount;
			int s = maxS-(int)((getMin()-value)/getStep());
			//			System.out.println(String.format("valueToScroll %3.1f converts to %d of %d (%d %d)", 
			//					value, s, maxS, vScrollBar.getMaximum(), vScrollBar.getVisibleAmount()));
			return Math.min(s, maxS);
		}
		@Override
		public String getTitle() {
			return "Bearing";
		}
		@Override
		public double getStep() {
			return 1;
		}
		@Override
		public double getMax() {
			if (btDisplayParameters.view360) {
				return -180;
			}
			else {
				return 0;
			}
		}
		@Override
		public double getMin() {
			return 180;
		}
		@Override
		public void setSelected() {
			if (vScrollBar == null) {
				return;
			}
			vScrollBar.setToolTipText("Bearing scale scrolling");
			super.setSelected();
		}
		@Override
		public void setZoom(ZoomShape zoomShape) {
			if (zoomShape == null) {
				restoreDefaults();
				return;
			}
			if (zoomShape.getCoordinateType() != BTDisplayParameters.DISPLAY_BEARING) {
				return;
			}
			setupScrollBar(zoomShape.getYStart() + zoomShape.getYLength(), -zoomShape.getYLength());
		}

		/* (non-Javadoc)
		 * @see clickDetector.ClickBTDisplay.VScaleManager#getYAxisInterval()
		 */
		@Override
		public double getYAxisInterval() {
			return -45;
		}

	}
	private class ICIScaleManager extends VScaleManager {
		public ICIScaleManager() {
			restoreDefaults();
		}
		@Override
		public int getCoordinateType() {
			return BTDisplayParameters.DISPLAY_BEARING;
		}
		@Override
		public void setSelected() {
			vScrollBar.setToolTipText("ICI scale scrolling");
			super.setSelected();
		}
		@Override
		public void setZoom(ZoomShape zoomShape) {
			if (zoomShape == null) {
				restoreDefaults();
				return;
			}
			if (zoomShape.getCoordinateType() != BTDisplayParameters.DISPLAY_ICI) {
				return;
			}
			//			double y1 = Mathmin(zoomShape.getYStart(), zoomShape.gety)
			// don't zoom on log scale. 
			if (btDisplayParameters.logICIScale) {
				setupScrollBar(btDisplayParameters.minICI, btDisplayParameters.maxICI);
			}
			else {
				setupScrollBar(zoomShape.getYStart(), zoomShape.getYLength());
			}
			//			double yStart = Math.max(zoomShape.getYStart(), btDisplayParameters.minICI);
		}
		@Override
		public double getMax() {
			return btDisplayParameters.maxICI;
		}
		@Override
		public double getMin() {
			return btDisplayParameters.logICIScale ? btDisplayParameters.minICI : 0;
		}
		@Override
		public double getStep() {
			return btDisplayParameters.maxICI/100;
		}
		@Override
		public String getTitle() {
			return "Inter click interval";
		}
		@Override
		public double scrollToValue(int scrollPos) {
			int maxS = vScrollBar.getMaximum()-vScrollBar.getVisibleAmount();
			int minS = vScrollBar.getMinimum();
			double val = (minS + maxS-scrollPos)*getStep();
			//			System.out.println(String.format("scrollToValue %d  converts to %3.2f", 
			//					scrollPos, val));
			return val;
		}
		@Override
		public int valueToScroll(double value, int visAmount) {
			int maxS = vScrollBar.getMaximum()-visAmount;
			int minS = vScrollBar.getMinimum();
			int s = (int) (minS+maxS - value/getStep());
			return s;
		}

		@Override
		public double getYAxisInterval() {
			return PamAxis.INTERVAL_AUTO;
		}
	}
	private class AmplitudeScaleManager extends VScaleManager {
		public AmplitudeScaleManager() {
			restoreDefaults();
		}
		@Override
		public int getCoordinateType() {
			return BTDisplayParameters.DISPLAY_BEARING;
		}
		@Override
		public void setSelected() {
			vScrollBar.setToolTipText("Amplitude scale scrolling");
			super.setSelected();
		}
		@Override
		public void setZoom(ZoomShape zoomShape) {
			if (zoomShape == null) {
				restoreDefaults();
				return;
			}
			if (zoomShape.getCoordinateType() != BTDisplayParameters.DISPLAY_AMPLITUDE) {
				return;
			}
			setupScrollBar(zoomShape.getYStart(), zoomShape.getYLength());
		}
		@Override
		public double getMax() {
			return btDisplayParameters.amplitudeRange[1];
		}
		@Override
		public double getMin() {
			return btDisplayParameters.amplitudeRange[0];
		}
		@Override
		public double getStep() {
			return 1;
		}
		@Override
		public String getTitle() {
			return "Click Amplitude (dB)";
		}
		@Override
		public double scrollToValue(int scrollPos) {
			int maxS = vScrollBar.getMaximum()-vScrollBar.getVisibleAmount();
			int minS = vScrollBar.getMinimum();
			double val = (minS + maxS-scrollPos)*getStep();
			//			System.out.println(String.format("scrollToValue %d  converts to %3.2f", 
			//					scrollPos, val));
			return val;
		}
		@Override
		public int valueToScroll(double value, int visAmount) {
			int maxS = vScrollBar.getMaximum()-visAmount;
			int minS = vScrollBar.getMinimum();
			int s = (int) (minS+maxS - value/getStep());
			return s;
		}
		@Override
		public double getYAxisInterval() {
			return PamAxis.INTERVAL_AUTO;
		}
	}
	private class SlantScaleManager extends VScaleManager {
		public SlantScaleManager() {
			restoreDefaults();
		}
		@Override
		public int getCoordinateType() {
			return BTDisplayParameters.DISPLAY_SLANT;
		}
		@Override
		public void setSelected() {
			vScrollBar.setToolTipText("Slant scale scrolling");
			super.setSelected();
		}
		@Override
		public void setZoom(ZoomShape zoomShape) {
			if (zoomShape == null) {
				restoreDefaults();
				return;
			}
			if (zoomShape.getCoordinateType() != BTDisplayParameters.DISPLAY_SLANT) {
				return;
			}
			setupScrollBar(zoomShape.getYStart(), zoomShape.getYLength());
		}
		@Override
		public double getMax() {
			return 0.;
		}
		@Override
		public double getMin() {
			return 90.;
		}
		@Override
		public double getStep() {
			return 1;
		}
		@Override
		public String getTitle() {
			return "Slant Angle (deg.)";
		}
		@Override
		public double scrollToValue(int scrollPos) {
			int maxS = vScrollBar.getMaximum()-vScrollBar.getVisibleAmount();
			int minS = vScrollBar.getMinimum();
			double val = (minS + maxS-scrollPos)*getStep();
			//			System.out.println(String.format("scrollToValue %d  converts to %3.2f", 
			//					scrollPos, val));
			return val;
		}
		@Override
		public int valueToScroll(double value, int visAmount) {
			int maxS = vScrollBar.getMaximum()-visAmount;
			int minS = vScrollBar.getMinimum();
			int s = (int) (minS+maxS - value/getStep());
			return s;
		}
		@Override
		public double getYAxisInterval() {
			return -45;
		}
	}

	class HScrollObserver implements PamScrollObserver {

		@Override
		public void scrollRangeChanged(AbstractPamScroller pamScroller) {
			displayStartMillis = hScrollBar.getValueMillis();
			hScrollManager.timeRangeChanged();
			checkBTAmplitudeSelectHisto();

			btPlot.setTotalRepaint();
			repaintBoth();
		}


		@Override
		public void scrollValueChanged(AbstractPamScroller pamScroller) {
			displayStartMillis = hScrollBar.getValueMillis();
			btPlot.setTotalRepaint();
			repaintBoth();
		}

	}

	abstract class HScrollManager implements PamScrollObserver {

		/**
		 * Set up the scroll bar
		 * @param maxMillis the maximum time of the display in milliseconds. 
		 * @return true if the scroll posiiton has changed (will require a repaint)
		 */
		abstract boolean setupScrollBar(long maxMillis);

		abstract public void reset();

		/**
		 * Called whenever the vertical scroll position
		 * i.e. the length of the display, has changed
		 */
		abstract void timeRangeChanged();

		/**
		 * Function for rounding up times in milliseconds to the 
		 * nearest second. 
		 * @param val value to round
		 * @return rounded value. 
		 */
		protected long roundUp(long val) {
			long rem = val%1000;
			return val + 1000-rem;
		}

		/**
		 * Get the start time of the display in milliseconds.
		 * @return
		 */
		long getDisplayStartMillis() {
			return hScrollBar.getValueMillis();
		}	

	}

	class OfflineScrollManager extends HScrollManager {

		@Override
		boolean setupScrollBar(long maxMillis) {

			hScrollBar.setVisibleAmount(getTimeRangeMillis());
			hScrollBar.setUnitIncrement(getUnitIncrement(getTimeRangeMillis()));
			hScrollBar.setBlockIncrement(getTimeRangeMillis() * 7 / 8);

			return true;
		}

		/**
		 * Take the time range of the display in seconds and 
		 * make up a suitable scroll bar increment step for the 
		 * @param hRange range in seconds
		 * @return increment in milliseconds
		 */
		private int getUnitIncrement(int hRange) {
			if (hRange < 5) {
				return 100;
			}
			if (hRange < 10) {
				return 500;
			}
			return (int) Math.pow(10, Math.floor(Math.log10(hRange))-1);
		}

		@Override
		void timeRangeChanged() {
			setupScrollBar(0);
		}

		@Override
		public void scrollRangeChanged(AbstractPamScroller pamScroller) {

		}

		@Override
		public void scrollValueChanged(AbstractPamScroller pamScroller) {
			// TODO Auto-generated method stub
			topControls.ShowTime();
		}

		@Override
		public void reset() {

		}

	}

	/**
	 * Class to manage scrolling when in network recieve mode. 
	 * A bit of a combination of real time and offline functions. 
	 * @author Doug Gillespie
	 *
	 */
	class NetRXScrollManager extends HScrollManager {

		private long lastScrollCheck = 0;

		private long displayMaxMillis;

		NetRXScrollManager() {
			hScrollBar.setRangeMillis(0, 3600000L, false);
		}

		@Override
		boolean setupScrollBar(long maxMillis) {
			/**
			 * Called every time new data arrive.
			 * Need to move the limits of the outer scroller, but
			 * not actually move the data unless the follow box is checked. 
			 */
			//			System.out.println("Display MAx = " + PamCalendar.formatTime(maxMillis));
			maxMillis = roundUp(maxMillis);
			//			System.out.println("Round up to = " + PamCalendar.formatTime(maxMillis));
			displayMaxMillis = Math.max(displayMaxMillis, maxMillis);
			//			System.out.println("New max time = " + PamCalendar.formatTime(displayMaxMillis));
			//			System.out.println(String.format("Set up scroll bar at %s", PamCalendar.formatDateTime(displayMaxMillis)));
			long displayLength = getTimeRangeMillis();
//			long currentStart = hScrollBar.getMinimumMillis();
			long scrollRange = hScrollBar.getRangeMillis(); // total scrollable range
			long currentValue = hScrollBar.getValueMillis();
			long displayMinMillis = displayMaxMillis-scrollRange; // start of scrollable region. 

			hScrollBar.setVisibleAmount(displayLength);

			//			String stT = PamCalendar.formatTime(displayMinMillis);
			//			System.out.println(String.format("Set range %s to %s visAm %d " ,
			//					stT, PamCalendar.formatTime(displayMaxMillis), displayLength));
			hScrollBar.setRangeMillis(displayMinMillis, displayMaxMillis, true);
			// it's possible that the current value had to nudge forward to stay in range. 
			if (followCheckBox.isSelected()) {
				hScrollBar.setValueMillis(displayMaxMillis-displayLength);
			}
			else if (currentValue < displayMinMillis) {
				hScrollBar.setValueMillis(displayMinMillis);
			}
			else {
				hScrollBar.setValueMillis(currentValue);
			}
			displayStartMillis = hScrollBar.getValueMillis();
			repaintBoth();
			topControls.ShowTime();

			return false;
		}

		@Override
		void timeRangeChanged() {
			//			called when the  spinner changes the range 
			setupScrollBar(displayMaxMillis);
		}

		@Override
		public void scrollRangeChanged(AbstractPamScroller pamScroller) {
			// TODO Auto-generated method stub
			repaintBoth();
		}

		@Override
		public void scrollValueChanged(AbstractPamScroller pamScroller) {
			topControls.ShowTime();			
		}

		@Override
		public void reset() {
		}

	}

	class RealTimeScrollManager extends HScrollManager {

		private long lastScrollCheck = 0;

		private long displayMaxMillis;

		@Override
		boolean setupScrollBar(long maxMillis) {
			this.displayMaxMillis = maxMillis;
			if (displayMaxMillis < lastScrollCheck - 1000) {
				System.out.println("Going backwards in time in CheckScroll");
			}
			lastScrollCheck = displayMaxMillis;

			long displayEnd = roundUp(displayMaxMillis);
			long displayLength = getTimeRangeMillis();

			long currentStart = hScrollBar.getMinimumMillis();
			currentStart = Math.max(currentStart, PamCalendar.getSessionStartTime());
			long normalStart = displayEnd - displayLength;
			long currentValue = hScrollBar.getValueMillis();
			/**
			 * Allow to go back the equivalent of one display length
			 * or one minute, whichever is greater. 
			 */
			long minimumStart = normalStart - Math.max(displayLength, 60000L);
			currentStart = Math.max(currentStart, minimumStart);
			displayStartMillis = Math.max(currentValue, currentStart);
			//				if (currentStart < normalStart - 10) {
			//					followCheckBox.setSelected(true);
			//					return setupScrollBar(maxMillis);
			//				}
			hScrollBar.setRangeMillis(currentStart, displayEnd, true);
			hScrollBar.setVisibleAmount(displayLength);
			if (followCheckBox.isSelected()) {
				hScrollBar.setValueMillis(displayEnd-displayLength);
			}
			else {
				hScrollBar.setValueMillis(displayStartMillis);
			}
			//				System.out.println(hScrollBar);
			//			}
			hScrollBar.setBlockIncrement(displayLength/2);
			hScrollBar.setUnitIncrement(displayLength/20);


			return true;

		}

		@Override
		void timeRangeChanged() {
			setupScrollBar(displayMaxMillis);
		}

		@Override
		public void scrollRangeChanged(AbstractPamScroller pamScroller) {
			// TODO Auto-generated method stub

		}

		@Override
		public void scrollValueChanged(AbstractPamScroller pamScroller) {
			// TODO Auto-generated method stub

		}

		@Override
		public void reset() {
			lastScrollCheck = 0;
			displayMaxMillis = 0;
			// need to move the display back to 0, in case 
			// we're starting over on some data. 
			long scRange = hScrollBar.getRangeMillis();
			hScrollBar.setRangeMillis(0, scRange, false);
			hScrollBar.setValueMillis(0);
		}

	}

	//	private void setupRTTimeBar(){
	//		long displayEnd = displayStart + displayLength;
	//		long newMin = displayEnd - timeRanges[vScrollBar.getValue()];
	//		boolean atEnd = true;//(hScrollBar.getValueMillis() >= hScrollBar.getMaximum() - 1);
	//		if (newMin < 0) {
	//			displayStart = 0;
	//			displayLength = timeRanges[vScrollBar.getValue()]
	//			                           * (long) sampleRate;
	//		} else {
	//			displayStart = displayEnd - timeRanges[vScrollBar.getValue()]
	//			                                       * (long) sampleRate;
	//			displayLength = timeRanges[vScrollBar.getValue()]
	//			                           * (long) sampleRate;
	//		}
	////		hScrollBar.setMinimum((int) (displayStart / (long) sampleRate));
	////		hScrollBar.setMaximum((int) (displayEnd / (long) sampleRate));
	////		if (atEnd) {
	////			hScrollBar.setValue(hScrollBar.getMaximum());
	////		}
	//
	//		setScales();
	//		repaintBoth();
	//	}
	//	void setupOfflineTimebar() {
	//		sampleRate = clickControl.clicksOffline.getSampleRate();
	//		hScrollManager.setupScrollBar((int)clickControl.clicksOffline.getTotalMillisNoGaps());
	//		//		hScrollBar.setValue(0);
	//	}

	//	private void offlineHorizontalAdjustment(AdjustmentEvent e) {
	//		displayStart = (long) (e.getValue() * sampleRate);
	//		getAxisPanel().repaint(100);
	//	}

	//	@Override
	//	public void newOfflineStore() {
	//		super.newOfflineStore();
	//		setSampleRate(clickControl.getClicksOffline().getSampleRate(), true);
	//		hScrollManager.setupScrollBar(clickControl.getClicksOffline().getTotalMillisNoGaps());
	//		hasData = true;
	////		hScrollBar.setValue(0);
	//		hScrollManager.vertScrollChanged(); // force scroll reset.
	//		repaintBoth();
	//	}

	@Override
	public void offlineDataChanged() {
		repaintBoth();
	}

	/**
	 * Get the display time range
	 * @return time range in seconds. 
	 */
	private double getTimeRangeSeconds() {
		if (rangeSpinner == null) {
			return 1;
		}
		return rangeSpinner.getSpinnerValue();
	}

	private int getTimeRangeMillis() {
		return (int)(getTimeRangeSeconds() * 1000.);
	}

	/**
	 * check horizontal scroll bar during real time operation. 
	 * gets called once a second, based on adc counts from 
	 * the raw data clock. 
	 * @param lastSample
	 * @return
	 */
	//	public boolean checkScroll(long lastSample) {
	//		if (lastSample < lastScrollCheck - 100000) {
	//			System.out.println("Going backwards in time in ChcekScroll");
	//		}
	//		lastScrollCheck = lastSample;
	//		int jump = 1;
	//		int offset = 0;
	//		if (followCheckBox.isSelected() == false)
	//			return false;
	//		if (lastSample < displayStart) {
	//			/*
	//			 * need to scroll backwards for some reason - 
	//			 * possibly after reset of click detector.
	//			 * in this case, set the display start to the 
	//			 * click time and see how it goes from there
	//			 */  
	//
	//			offset = (int) ((lastSample - (displayStart + jump)) / (long) sampleRate);
	//		}
	//		if (lastSample + sampleRate < displayStart + displayLength) {
	//			btPlot.repaint();
	//			return false; // no scrolling required (>1 second gap)
	//		}
	//		else {
	//			/*
	//			 * Click is close to or beyond end of display, so need to scroll.
	//			 */
	//			offset = (int) ((lastSample - (displayStart + displayLength)) / (long) sampleRate);
	//			offset += jump;
	////			int pixsLeft = (int) (offset * sampleRate * xScale);
	//		}
	//		if (offset != 0) {
	//			hScrollBar.setMinimum(hScrollBar.getMinimum() + offset);
	//			hScrollBar.setMaximum(hScrollBar.getMaximum() + offset);
	//			hScrollBar.setValue(hScrollBar.getValue() + offset);
	//			btPlot.repaint();
	//		}
	//		getAxisPanel().repaint();
	//
	//		return true;
	//	}


	private Point clickXYPos(ClickDetection click) {
		return new Point(clickXPos(click), clickYPos(click));
	}

	private int clickXPos(ClickDetection click) {
		return (int) ((click.getTimeMilliseconds() - displayStartMillis) * xScale);
	}

	private int clickXPos(long milliseconds) {
		return (int) ((milliseconds - displayStartMillis) * xScale);
	}

	private long millisFromXPos(int xPos) {
		return (long) (xPos/xScale) + displayStartMillis;
	}

	private int clickYPos(ClickDetection click) {
		int y = 0;
		int yMax = btPlot.getHeight();
		switch(btDisplayParameters.VScale){
		case BTDisplayParameters.DISPLAY_BEARING:
			y = yMax - (int) clickAngleToY(click); 
			break;
		case BTDisplayParameters.DISPLAY_AMPLITUDE:
			y = yMax - (int) clickAmplitudeToY(click);
			break;
		case BTDisplayParameters.DISPLAY_ICI:
			y = yMax - (int) clickICIToY(click);
			break;
		case BTDisplayParameters.DISPLAY_SLANT:
			y = yMax - (int) clickSlantToY(click);
			break;
		}
		y = Math.max(0, Math.min(y, yMax));
		return y;
	}

	/**
	 * Work out a slant angle - only possible with planar or volumetric arrays. 
	 * @param click click detection
	 * @return y coordinate from slant angle. 
	 */
	private double clickSlantToY(ClickDetection click) {
		ClickLocalisation loc = click.getClickLocalisation();
		PamVector v = loc.getPlanarVector();
		if (v == null) {
			return 0;
		}
		/*
		 * work out the anlge from the z coordinate to the x,y plane. 
		 */
		double x = v.getElement(0);
		double y = v.getElement(1);
		double z = v.getElement(2);
		x = Math.sqrt(x*x+y*y);
		double ang = Math.abs(Math.toDegrees(Math.atan2(z,x)));
		return (ang-yStart)*yScale;

	}
	private double clickAngleToY(ClickDetection click) {
		ClickLocalisation loc = click.getClickLocalisation();
		if (loc == null) return 0;
		double angle = 0;
		switch(loc.getSubArrayType()) {
		case ArrayManager.ARRAY_TYPE_NONE:
		case ArrayManager.ARRAY_TYPE_POINT:
			return 0;
		case ArrayManager.ARRAY_TYPE_LINE:
		case ArrayManager.ARRAY_TYPE_PLANE:
			double[] surfaceAngle = loc.getPlanarAngles();
			angle = Math.toDegrees(surfaceAngle[0]);
			break;
		case ArrayManager.ARRAY_TYPE_VOLUME:
			PamVector[] vecs = loc.getWorldVectors();
			if (vecs == null || vecs.length < 1) {
				return 0;
			}
			angle = Math.toDegrees(PamVector.vectorToSurfaceBearing(vecs[0]));
			break;
		default:
			return 0;
		}
		angle = PamUtils.constrainedAngle(angle, 180.);
		if (angle < 0 && yStart == 0.) {
			angle = -angle;
		}
		return (angle-yStart) * yScale;
	}

	private double angleFromYPos(int yPos) {
		return yStart + yPos/yScale;
	}

	private double clickAmplitudeToY(ClickDetection click) {
		return (click.getAmplitudeDB()-yStart) * yScale;
	}

	private double amplitudeFromYPos(int yPos) {
		int yMax = btPlot.getHeight();
		return yStart + (yMax-yPos)/yScale;
	}

	private ClickDetection lastICIClick;
	private double clickICIToY(ClickDetection click) {
		if (click.getICI() > 0) {
			return btPlot.getHeight() - yAxis.getPosition(click.getICI());
			//			return click.getICI() * yScale;
		}
		else {
			return btPlot.getHeight() - yAxis.getPosition(click.getTempICI());
			//			return click.getTempICI()  * yScale;
		}

	}

	private double iciFromYPos(int yPos) {
		int yMax = btPlot.getHeight();
		return (yMax-yPos)/yScale;
	}

	private double getClickHeight(ClickDetection click) {
		// scale according to the amplitude range and min / max pixels of 3 and 12
		return Math.max((click.getAmplitudeDB() - btDisplayParameters.amplitudeRange[0]) / 
				(btDisplayParameters.amplitudeRange[1] - btDisplayParameters.amplitudeRange[0]) * 
				(btDisplayParameters.maxClickHeight - btDisplayParameters.minClickHeight) + 
				btDisplayParameters.minClickHeight, 2);
	}

	private double getClickWidth(ClickDetection click) {
		return (double) click.getDuration() / (double) clickControl.clickParameters.maxLength * 
		(btDisplayParameters.maxClickLength - btDisplayParameters.minClickLength) + 
		btDisplayParameters.minClickLength;		
	}

	/**
	 * 
	 * @return true if want to and can view 360 degrees. 
	 */
	private boolean view360() {
		//@TODO need code to check whether any multi element sub arrays are in use. 
		return btDisplayParameters.view360;
	}

	/**
	 * Repaints both the plot and the axis panel
	 */
	public void repaintBoth() {
		btAxis.repaint();
		btPlot.setTotalRepaint();
		btPlot.repaint(minPaintTime);

	}

	/**
	 * Repaints forcing a redrawing of all click. Use this to redraw plot from other packages.
	 */
	public void repaintTotal() {
		btPlot.setTotalRepaint();
		btPlot.repaint(minPaintTime);

	}

	//	public long firstRequiredSample() {
	//		return displayStart + displayLength - (long) sampleRate * 600;
	//	}


	private ClickDetection popupClick;
	/**
	 * See if the mouse is on a click to be tracked with the mouse. 
	 * If not, return false, 
	 * @param e mouse event. 
	 * @return true if a click was found and marked as tracked. 
	 */
	private boolean trackClick(MouseEvent e) {
		/*
		 * See if a click is within 10 pixels of here.
		 */
		ClickDetection click = findClick(e.getX(), e.getY(), 10);
		if (click == null)
			return false;
		/*
		 * If it was mouse left button, then only track it if
		 * the display was in follow mode, it the display is paused
		 * then don't track it since it's possible the user
		 * was just selecting the click to view with Pamguard.
		 */
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (didFollow) {
				trackClick(click);
			}
		}
		else {
			trackClickMenu(e, click);
		}
		return true;
	}
	private void trackClickMenu(MouseEvent e, ClickDetection click) {
		// make a pop up menu
		popupClick = click;
		JPopupMenu pm = getClickPUMenu(click);
		pm.show(e.getComponent(), e.getX(), e.getY());

	}

	private JPopupMenu clickPopupMenu = null;

	public RangeSpinner rangeSpinner;

	private BTAmplitudeSelector btAmplitudeSelector;

	private JPopupMenu getClickPUMenu(ClickDetection click) {
		if (clickPopupMenu == null){
			clickPopupMenu = new JPopupMenu();
			JMenuItem menuItem;
			PamSymbol pamSymbol;
			Color symbolColour;
			for (int i = 0; i < PamColors.getInstance().NWHALECOLORS; i++) {
				menuItem = new JMenuItem(" Whale Train " + i);
				symbolColour = PamColors.getInstance().getWhaleColor(i);
				pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, true, symbolColour, symbolColour);
				menuItem.setIcon(pamSymbol);
				menuItem.addActionListener(new clickPUListener(i));
				clickPopupMenu.add(menuItem);
			}
		}
		return clickPopupMenu;
	}

	private class clickPUListener implements ActionListener {

		private int whaleId;

		public clickPUListener(int whaleId) {
			super();
			this.whaleId = whaleId;
		}

		public void actionPerformed(ActionEvent arg0) {

			if (popupClick == null) return;

			popupClick.setEventId(whaleId);

			trackClick(popupClick);

			popupClick = null;

		}

	}

	private void trackClick(ClickDetection click) {

		click.setTracked(true);
		clickControl.clickDetector.reWriteClick(click, false);
		btPlot.invalidateClick(click);

		//		ClickDetection newDataUnit = new ClickDetection(click.getChannelBitmap(), click.getStartSample(), 
		//				click.getDuration(), click.clickDetector, click.triggerList);

		trackedClicks.addPamData(click);
	}

	class BTAxis extends PamPanel {

		int yAxisExtent = 0;

		int xAxisExtent = 0;

		public BTAxis() {
			super();
			makeAxis();
		}

		public void makeAxis() {

			if (xAxis == null) {
				xAxis = new PamAxis(0, 0, 0, 100, 0, 30, PamAxis.ABOVE_LEFT,
						"", PamAxis.LABEL_NEAR_MAX, "%3.1f");
			}

			if (yAxis == null) {
				yAxis = new PamAxis(0, 1, 0, 1, 0, 1, PamAxis.ABOVE_LEFT,
						"Bearing", PamAxis.LABEL_NEAR_CENTRE, "%3.0f");
			}
			VScaleManager scaleManager = getVScaleManager();
			if (scaleManager == null) {
				return;
			}
//			scaleManager
//			System.out.println(String.format("set y range from %s %3.4f to %3.4fs", scaleManager.getTitle(), 
//					scaleManager.getCurrentStart(), scaleManager.getCurrentEnd()));
			yAxis.setRange(scaleManager.getCurrentStart(), scaleManager.getCurrentEnd());	
			yAxis.setLogScale(btDisplayParameters.logICIScale && btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_ICI);
			yAxis.setInterval(scaleManager.getYAxisInterval());
			yAxis.setLabel(scaleManager.getTitle());
			yAxis.setAutoFormat(false);
//			String format = getYAxisFormat()
//			double range = Math.abs(scaleManager.currentRange);
//			if (range > 30) {
//				yAxis.setFormat("%d");
//			}
//			else if (range > 8) {
//				yAxis.setFormat("%3.1f");
//			}
//			else if (range > 1) {
//				yAxis.setFormat("%3.1f");
//			}
//			else if (range > .1) {
//				yAxis.setFormat("%3.2f");
//			}
//			else if (range <= 0) {
//				yAxis.setFormat("%3.2f");
//			}
//			else {
//				int nDP = (int)Math.ceil(Math.abs(Math.log10(range)));
//				yAxis.setFormat(String.format("%%%d.%df", nDP+2, nDP));
//			}

			//			switch (btDisplayParameters.VScale) {
			//			
			//			case BTDisplayParameters.DISPLAY_BEARING:
			//				double lim2 = 0;
			//				if (view360()) {
			//					lim2 = -180;
			//				}
			//				yAxis = new PamAxis(0, 0, 0, 100, 180, lim2, PamAxis.ABOVE_LEFT,
			//						"Bearing", PamAxis.LABEL_NEAR_CENTRE, "%3.0f");				
			//				yAxis.setInterval(-90);
			//				break;
			//			case BTDisplayParameters.DISPLAY_AMPLITUDE:
			//				yAxis = new PamAxis(0, 0, 0, 100, btDisplayParameters.amplitudeRange[0], 
			//						btDisplayParameters.amplitudeRange[1], PamAxis.ABOVE_LEFT,
			//						"Amplitude (dB)", PamAxis.LABEL_NEAR_CENTRE, "%3.1f");
			//				break;
			//			case BTDisplayParameters.DISPLAY_ICI:
			//				if (btDisplayParameters.maxICI >= .5)
			//					yAxis = new PamAxis(0, 0, 0, 100, 0, 
			//							btDisplayParameters.maxICI, PamAxis.ABOVE_LEFT,
			//							"Inter Click Interval (s)", PamAxis.LABEL_NEAR_CENTRE, "%3.1f");
			//				else
			//					yAxis = new PamAxis(0, 0, 0, 100, 0, 
			//							btDisplayParameters.maxICI * 1000., PamAxis.ABOVE_LEFT,
			//							"Inter Click Interval (ms)", PamAxis.LABEL_NEAR_CENTRE, "%3.1f");
			//				break;
			//			}
			lastAxisType = btDisplayParameters.VScale;

			//			setWestAxis(yAxis);
			//			setNorthAxis(xAxis);
			// this.getGraphics();
			// Image im = new Image();
			// im.
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			//			if (lastAxisType != btDisplayParameters.VScale) {
			btAxis.makeAxis();
			//			}
			setScales();
			// just draw the axis along the bottom of the plot
			Rectangle r = getBounds();
			Rectangle rp = btPlot.getBounds();
			Insets insets = getInsets();
			Insets plotInsets = btPlot.getInsets();
			//plotInsets = 
			//btPlot.getBorder().getBorderInsets(btPlot.getc));
			int newYExtent = yAxis.getExtent(g, "180.0");
			int newXExtent = xAxis.getExtent(g, "");
			/*
			 * Took this block of code out on 4/4/2010
			 * since all it seems to do is to make the BT panel continually resize
			 * itself. It's possible that it is needed though for some other 
			 * eventuality and will need to be reinstated in some form or another. 
			 */
			//			if (newYExtent != yAxisExtent || newXExtent != xAxisExtent) {
			if (newXExtent != xAxisExtent) {
				Insets currentInsets = getInsets();
				currentInsets.left = newYExtent;
				setBorder(new EmptyBorder(currentInsets));
				//				setBorder(new EmptyBorder(newXExtent, newYExtent,
				//						insets.bottom, insets.right));
				topControls.setBorder(new EmptyBorder(0, newYExtent, 0,
						insets.right));
				//				yAxisExtent = newYExtent;
				xAxisExtent = newXExtent;
			}

			yAxis.drawAxis(g, insets.left, rp.height + insets.top + plotInsets.top + 1, insets.left,
					insets.top+1);

			xAxis.drawAxis(g, insets.left, insets.top, insets.left + rp.width,
					insets.top);

			if (btDisplayParameters.trackedClickMarkers) {
				drawTrackedClickMarkers(g);
			}

			// draw the time in the top left.
			//			FontMetrics fontMetrics = g.getFontMetrics();
			//			long startTime = hScrollManager.getDisplayStartMillis();
			//			if (startTime > 0) {
			//				g.drawString(PamCalendar.formatDateTime(startTime), insets.left, fontMetrics.getAscent() + 2);
			//			}

			// Rectangle r = getBounds();
			// g.drawLine(0,0, getBorder().getBorderInsets(this).left, r.height
			// - getBorder().getBorderInsets(this).bottom);
			paintAmplitudeSelectInfo(g);
		}

		protected void paintAmplitudeSelectInfo(Graphics g) {

			/**
			 * paint information about click excluded due to amplitude selection
			 */
			if (btDisplayParameters.amplitudeSelect == false) {
				return;
			}
			int n = countAmplitudeDeselected();
			PamDataBlock<ClickDetection> clickData = clickControl.getClickDataBlock();
			int nAll = clickData.getUnitsCount();
			String txt = String.format("%d of %d loaded clicks will not be displayed because their amplitude is < %3.1fdB",
					n, nAll, btDisplayParameters.minAmplitude);
			Insets insets = getInsets();
			int x = insets.left;
			int y = getHeight()-5;
			//			g.setColor(n > 0 ? Color.red : Color.BLACK);
			g.drawString(txt, x, y);
		}

		PamSymbol trackedClickMark = new PamSymbol(PamSymbol.SYMBOL_TRIANGLEL, 15, 15, true, Color.BLACK, Color.BLACK);
		/**
		 * Draw little markers down the side of the axis to 
		 * show where the next tracked click thingy is likely to be. 
		 * @param g graphics handle. 
		 */
		private void drawTrackedClickMarkers(Graphics g) {
			ClickGroupDataBlock<TrackedClickGroup> trackedClickGroups = clickControl.trackedClickLocaliser.trackedClickGroups;
			TrackedClickGroup tcg;
			//			ClickDetection clickDetection;
			int n = trackedClickGroups.getUnitsCount();
			Double lastBearing;
			Rectangle rp = btPlot.getBounds();
			Point pt = new Point(0,0);
			Insets insets = getInsets();
			int rightX = pt.x = getWidth() - insets.right + trackedClickMark.getWidth() / 2 + 1;
			int leftX = trackedClickMark.getWidth() / 2 + 1;
			Color col;
			try {
				ListIterator<TrackedClickGroup> tcgIterator = trackedClickGroups.getListIterator(0);
				while (tcgIterator.hasNext()) {
					tcg = tcgIterator.next();
					lastBearing = tcg.getPredictedBearing(PamCalendar.getTimeInMillis());
					if (lastBearing == null) {
						continue;
					}
					col = PamColors.getInstance().getWhaleColor(tcg.getEventId());
					trackedClickMark.setFillColor(col);
					trackedClickMark.setLineColor(col);
					pt.y = (int) yAxis.getPosition(lastBearing * 180 / Math.PI) + getInsets().top;
					pt.x = leftX;
					trackedClickMark.setSymbol(PamSymbol.SYMBOL_TRIANGLER);
					trackedClickMark.draw(g, pt);
					pt.x = rightX;
					trackedClickMark.setSymbol(PamSymbol.SYMBOL_TRIANGLEL);
					trackedClickMark.draw(g, pt);
				}
			}
			catch (ConcurrentModificationException e) {

			}
		}
	}

	public class FollowBoxListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			clickDisplayManager.setBAutoScroll(followCheckBox.isSelected());
			hScrollManager.setupScrollBar(hScrollBar.getMaximumMillis());
		}

	}
	public class PlotKeyListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			//			System.out.println("Key pressed " + e);
			super.keyPressed(e);
			switch (e.getKeyCode()) {
			case 37:
				selectClick(-1);
				break;
			case 39:
				selectClick(+1);
				break;
			}
			if (selectedClick != null) {
				setSelectedClick(selectedClick);
			}
		}

	}


	private class ClassifyClick implements ActionListener {

		private ClickDetection click;

		public ClassifyClick(ClickDetection click) {
			super();
			this.click = click;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ClickIdentifier clickIdentifier = clickControl.getClickIdentifier();
			if (clickIdentifier == null) {
				return;
			}
			ClickIdInformation idInfo = clickIdentifier.identify(click);
			click.setClickType((byte)idInfo.clickType);
		}
	}

	private JPopupMenu getPopupMenu(ClickDetection clickedClick) {
		boolean isView = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;
		JPopupMenu menu = new JPopupMenu();
		JMenuItem menuItem;
		if (isView) {
			if (clickControl.getClicksOffline().addBTMenuItems(menu, this, false, clickedClick)>0) {
				menu.addSeparator();
			}
			if (clickedClick != null) {
				menuItem = new JMenuItem("Classify Click");
				menuItem.addActionListener(new ClassifyClick(clickedClick));
				menu.add(menuItem);
				if (clickedClick.getSuperDetection(0) != null) {
					PamDetection sd = clickedClick.getSuperDetection(0);
					if (sd.getSubDetectionsCount() > 1) {
						menuItem = clickControl.getTargetMotionLocaliser().getEventMenuItem((OfflineEventDataUnit) sd, "Click Train");
						menu.add(menuItem);
					}
				}
			}

		}
		menuItem = new JMenuItem("Settings ...");
		menuItem.addActionListener(new SettingsMenuAction());
		menu.add(menuItem);
		menuItem = new JMenuItem("Show amplitude selector ...");
		menuItem.addActionListener(new AmplitudeSelector());
		menu.add(menuItem);
		menuItem = new JCheckBoxMenuItem("Colour by species id", 
				btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_SPECIES);
		menuItem.addActionListener(new ColourByAction(BTDisplayParameters.COLOUR_BY_SPECIES));
		menu.add(menuItem);
		menuItem = new JCheckBoxMenuItem("Colour by click train", 
				btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_TRAIN);
		menuItem.addActionListener(new ColourByAction(BTDisplayParameters.COLOUR_BY_TRAIN));
		menu.add(menuItem);
		menuItem = new JCheckBoxMenuItem("Colour by train, then species", 
				btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_TRAINANDSPECIES);
		menuItem.addActionListener(new ColourByAction(BTDisplayParameters.COLOUR_BY_TRAINANDSPECIES));
		menu.add(menuItem);
		//		if (isNetReceiver) {
		menuItem = new JCheckBoxMenuItem("Colour by hydrophone", 
				btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_HYDROPHONE);
		menuItem.addActionListener(new ColourByAction(BTDisplayParameters.COLOUR_BY_HYDROPHONE));
		menu.add(menuItem);			
		//		}
		menu.addSeparator();
		menuItem = new JCheckBoxMenuItem("Bearing / Time");
		menuItem.addActionListener(new AxesMenuAction(BTDisplayParameters.DISPLAY_BEARING));
		menuItem.setSelected(btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_BEARING);
		menu.add(menuItem);
		menuItem = new JCheckBoxMenuItem("Amplitude / Time");
		menuItem.addActionListener(new AxesMenuAction(BTDisplayParameters.DISPLAY_AMPLITUDE));
		menuItem.setSelected(btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_AMPLITUDE);
		menu.add(menuItem);
		menuItem = new JCheckBoxMenuItem("ICI / Time");
		menuItem.addActionListener(new AxesMenuAction(BTDisplayParameters.DISPLAY_ICI));
		menuItem.setSelected(btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_ICI);
		menu.add(menuItem);
		menuItem = new JCheckBoxMenuItem("Slant Angle / Time");
		menuItem.addActionListener(new AxesMenuAction(BTDisplayParameters.DISPLAY_SLANT));
		menuItem.setSelected(btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_SLANT);
		if (ArrayManager.getArrayManager().getCurrentArray().getArrayShape() < ArrayManager.ARRAY_TYPE_PLANE) {
			menuItem.setEnabled(false);
		}
		menu.add(menuItem);

		if (zoomer != null) {
			menu.addSeparator();
			zoomer.appendZoomMenuItems(menu);
		}

		// just do the stuff for selecting different channel groups for now
		int channels = clickControl.clickParameters.channelBitmap;
		int[] channelGroups = clickControl.clickParameters.channelGroups;
		int nChannelGroups = GroupedSourcePanel.countChannelGroups(channels, channelGroups);
		if (nChannelGroups > 1) {
			menu.addSeparator();
			menuItem = new JCheckBoxMenuItem("Show all channel groups");
			menuItem.addActionListener(new ChannelGroupAction(0));
			menu.add(menuItem);
			if (getDisplayChannels() == 0) menuItem.setSelected(true);
			String str;
			int groupChannels;
			for (int i = 0; i < nChannelGroups; i++) {
				str = "Show channels " + GroupedSourcePanel.getGroupList(i, channels, channelGroups);
				menuItem = new JCheckBoxMenuItem(str);
				groupChannels = GroupedSourcePanel.getGroupChannels(i, channels, channelGroups);
				menuItem.addActionListener(new ChannelGroupAction(groupChannels));
				if (getDisplayChannels() == groupChannels) menuItem.setSelected(true);
				menu.add(menuItem);
			}
		}

		// now the key options
		menu.addSeparator();
		menuItem = new JCheckBoxMenuItem("Show key", btDisplayParameters.showKey);
		menuItem.addActionListener(new ShowKeyAction());
		menu.add(menuItem);

		return menu;
	}

	class PlotMouse extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
				mouseDown = true;
				didFollow = followCheckBox.isSelected();
				followCheckBox.setSelected(false);
			}
			ClickDetection click = findClick(e.getX(), e.getY(), 10);
			if (click != null) {
				setSelectedClick(click);
			}
			else {
				maybeShowPopup(e);
			}
			btPlot.requestFocus();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// saySomething("Mouse released; # of clicks: "
			// + e.getClickCount(), e);

			mouseDown = false;
			if (didFollow) {
				followCheckBox.setSelected(true);
				didFollow = false;
			}

			if (!clickControl.isViewerMode() && (e.getButton() == MouseEvent.BUTTON1 || 
					e.getButton() == MouseEvent.BUTTON3)) {
				if (trackClick(e)) {
					return;
				}
			}
			maybeShowPopup(e); // normal popup menu
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			sayCursorInfo(e.getPoint());
			if (isViewer) {
				//				setHoverText(e);
			}
		}

		//		void setHoverText(MouseEvent e) {
		//			ClickDetection click = findClick(e.getX(), e.getY(), 10);
		//			if (click == null) {
		//				btPlot.setToolTipText(null);
		//			}
		//			else {
		//				btPlot.setToolTipText("On click");
		//				
		//			}
		//		}

		@Override
		public void mouseExited(MouseEvent e) {
			cursorLabel.setText("");
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// saySomething("Mouse clicked (# of clicks: "
			// + e.getClickCount() + ")", e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			//			btPlot.requestFocus();
		}
	}

	private void sayCursorInfo(Point pt) {
		long t = millisFromXPos(pt.x);
		String cuString = "Cursor Time " + PamCalendar.formatTime(t, true);
		double yVal = yAxis.getDataValue(pt.y);
		switch(btDisplayParameters.VScale){
		case BTDisplayParameters.DISPLAY_BEARING:
			//			yVal = angleFromYPos(pt.y);
			cuString += String.format(", Angle %3.2f deg", yVal);
			break;
		case BTDisplayParameters.DISPLAY_AMPLITUDE:
			//			yVal = amplitudeFromYPos(pt.y);
			cuString += String.format(", Amp %3.2f dB", yVal);
			break;
		case BTDisplayParameters.DISPLAY_ICI:
			//			yVal = yAxis.getDataValue(pt.y);//iciFromYPos(pt.y);
			cuString += String.format(", ICI %4.3f s", yVal);
			break;
		case BTDisplayParameters.DISPLAY_SLANT:
			cuString += String.format(", Slant Angle %3.2f deg", yVal);
			break;
		}
		cursorLabel.setText(cuString);
	}

	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger() == false) {
			return;
		}
		ClickDetection click = findClick(e.getX(), e.getY(), 10);
		JPopupMenu menu = getPopupMenu(click);
		if (menu != null) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}

	}

	class ShowKeyAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			btDisplayParameters.showKey = !btDisplayParameters.showKey;
			btPlot.createKey();
		}
	}

	class ColourByAction implements ActionListener {

		private int colourChoice;

		public ColourByAction(int colourChoice) {
			super();
			this.colourChoice = colourChoice;
		}
		public void actionPerformed(ActionEvent e) {
			btDisplayParameters.colourScheme = colourChoice;
			btPlot.setTotalRepaint();
			btPlot.repaint(minPaintTime);
			btPlot.createKey();
		}
	}

	class SettingsMenuAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			BTDisplayParameters newParameters = 
				ClickDisplayDialog.showDialog(clickControl, 
						clickControl.getPamView().getGuiFrame(), btDisplayParameters);
			if (newParameters != null){
				btDisplayParameters = newParameters.clone();
				if (getVScaleManager() != null) {
					getVScaleManager().setSelected();
				}
				btAxis.makeAxis();
//				System.out.println("360: "+btDisplayParameters.view360);
				repaintBoth();
				btPlot.createKey();
				if (clickControl.getOfflineToolbar() != null) {
					clickControl.getOfflineToolbar().displayActivated(clickBTDisplay);
				}
			}

		}

	}

	class AmplitudeSelector implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			showAmplitudeSelector();
		}
	}

	private void showAmplitudeSelector() {
		
		//TODO FIXME! qiuck fix so scales reset without scrolling/restarting
		Rectangle b = null ;
		if (btAmplitudeSelector != null) {
			b= btAmplitudeSelector.getFrame().getBounds();
//			btAmplitudeSelector.getFrame().setVisible(true);
//			//			btAmplitudeSelector.getFrame().
		}
//		else {
			btAmplitudeSelector = BTAmplitudeSelector.showAmplitudeFrame(clickControl, this);
			if(b!=null){
				
				btAmplitudeSelector.getFrame().setBounds(b);
			}
//		}
	}

	private void checkBTAmplitudeSelectHisto() {
		if (btAmplitudeSelector == null) {
			return;
		}
		btAmplitudeSelector.createHistograms();	
	}

	class AxesMenuAction implements ActionListener {

		private int axesType;

		public AxesMenuAction(int axesType) {
			this.axesType = axesType;
		}

		public void actionPerformed(ActionEvent e) {
			setVScale(axesType);
		}
	}

	void setVScale(int scaleType) {
		btDisplayParameters.VScale = scaleType;
		if (getVScaleManager() != null) {
			getVScaleManager().setSelected();
		}
		repaintBoth();
	}

	class ChannelGroupAction implements ActionListener{
		int groupSelection;

		public ChannelGroupAction(int groupSelection) {
			super();
			this.groupSelection = groupSelection;
		}

		public void actionPerformed(ActionEvent e) {

			setDisplayChannels(groupSelection);

		}

	}


	@Override
	protected void finalize() throws Throwable {
		hScrollBar.uncoupleScroller();
	}

	private class TimeRangeListener implements RangeSpinnerListener {

		@Override
		public void valueChanged(double oldValue, double newValue) {
			btDisplayParameters.setTimeRange(newValue);
			hScrollManager.timeRangeChanged();
			repaintBoth();
		}

	}

	/**
	 * Panel which sits around the BTPlot panel and 
	 * primarily acts as a container for the scroll bars
	 * @author Doug Gillespie
	 *
	 */
	class BTPlotFrame extends JPanel {

		BTPlotFrame() {
			super();
			btPlot = new BTPlot();

			setLayout(new BorderLayout());

			this.add(BorderLayout.CENTER, btPlot);

			//			rangeScrollBar = new JScrollBar(Adjustable.VERTICAL,
			//					timeRanges.length - 2, 1, 0, timeRanges.length);
			//			rangeScrollBar.addAdjustmentListener(new VerticalListener());
			//			// vScrollBar.setUnitIncrement(2);
			//			this.add(BorderLayout.EAST, rangeScrollBar);

			//			int hRange = timeRanges[rangeScrollBar.getValue()];
			hScrollBar = new PamScroller("Click Time Display " + displayNumber, 
					AbstractPamScroller.HORIZONTAL, 100, 5*60*1000, isViewer | isNetReceiver);
			rangeSpinner = new RangeSpinner();
			rangeSpinner.addRangeSpinnerListener(new TimeRangeListener());
			hScrollBar.addControl(rangeSpinner.getComponent());
			hScrollBar.addObserver(rangeSpinner);
			// create the bar since it will be used even if it's not shown. 
			vScrollBar = new JScrollBar(Adjustable.VERTICAL);
			if (isViewer) {
				hScrollManager = new OfflineScrollManager();
				hScrollBar.addDataBlock(clickControl.clickDetector.getClickDataBlock());
				hScrollBar.addDataBlock(clickControl.clickDetector.getOfflineEventDataBlock());
				hScrollBar.addObserver(new HScrollObserver());
				hScrollBar.coupleScroller(clickControl.getUnitName() + "BT Scroll Coupler");
				hScrollBar.addMouseWheelSource(this);
				hScrollBar.addMouseWheelSource(btPlot);
				hScrollBar.addMouseWheelSource(btAxis);
				zoomer = new Zoomer(new ZoomableInterface(), btPlot);
				this.add(BorderLayout.EAST, vScrollBar);
				vScrollBar.addAdjustmentListener(new VScrollListener());
			}
			else if (isNetReceiver) {
				hScrollManager = new NetRXScrollManager();
				hScrollBar.addObserver(new HScrollObserver());
			}
			else {
				hScrollManager = new RealTimeScrollManager();
			}
			//			hScrollBar = new JScrollBar(Adjustable.HORIZONTAL, hRange, 1, 0,
			//					hRange + 1);
			//			hScrollBar.addAdjustmentListener(hScrollManager);
			hScrollBar.addObserver(hScrollManager);
			this.add(BorderLayout.SOUTH, hScrollBar.getComponent());

			setScales();
		}


	}

	static PamSymbol getDefaultSymbol() {
		return getDefaultSymbol(false);
	}
	static PamSymbol getDefaultSymbol(boolean makeClone) {
		if (defaultSymbol == null) {
			defaultSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 8, 8,
					true, PamColors.getInstance().getColor(PamColor.PLAIN), 
					PamColors.getInstance().getColor(PamColor.PLAIN));
		}
		if (defaultSymbol.getFillColor() != PamColors.getInstance().getColor(PamColor.PLAIN)) {
			defaultSymbol.setFillColor(PamColors.getInstance().getColor(PamColor.PLAIN));
			defaultSymbol.setLineColor(PamColors.getInstance().getColor(PamColor.PLAIN));
		}
		// always reset the shape since it may have been messed about with
		defaultSymbol.setSymbol(PamSymbol.SYMBOL_CIRCLE);
		if (makeClone) {
			return defaultSymbol.clone();
		}
		else {
			return defaultSymbol;
		}
	}
	//
	//	static PamSymbol getClickSymbol(ClickIdentifier clickIdentifier, ClickDetection click, int colourType) {
	//		return getClickSymbol(clickIdentifier, click, colourType);
	//	}
	//	static PamSymbol getClickSymbol(ClickIdentifier clickIdentifier, ClickDetection click) {
	//		return getClickSymbol(clickIdentifier, click, BTDisplayParameters.COLOUR_BY_TRAIN);
	//	}

	static PamSymbol getClickSymbol(ClickIdentifier clickIdentifier, ClickDetection click, int colourType) {
		PamSymbol speciesSymbol = clickIdentifier.getSymbol(click);
		PamSymbol symbol = getDefaultSymbol();
		Color aCol;

		if (colourType == BTDisplayParameters.COLOUR_BY_SPECIES) {
			// use the species shape and colour from the speciesSymbol
			if (speciesSymbol != null) {
				return speciesSymbol;
			}
		}
		else if (colourType == BTDisplayParameters.COLOUR_BY_HYDROPHONE) {
			symbol = getDefaultSymbol();
			if (speciesSymbol != null) {
				symbol.setSymbol(speciesSymbol.getSymbol());
			}
			int chan = PamUtils.getSingleChannel(click.getChannelBitmap());
			Color col = PamColors.getInstance().getWhaleColor(chan+1);
			symbol.setFillColor(col);
			symbol.setLineColor(col);
		}
		else {// (colourType == BTDisplayParameters.COLOUR_BY_TRAIN) {
			// use the colours of the train, but the shape of the default symbol. 
			symbol = getDefaultSymbol();

			if (speciesSymbol != null) {
				symbol.setSymbol(speciesSymbol.getSymbol());
			}			
			OfflineEventDataUnit offlineEvent = 
				(OfflineEventDataUnit) click.getSuperDetection(OfflineEventDataUnit.class);
			if (offlineEvent != null) {
				symbol.setFillColor(aCol = PamColors.getInstance().getWhaleColor(offlineEvent.getColourIndex()));
				symbol.setLineColor(aCol);
			}
			else if (colourType == BTDisplayParameters.COLOUR_BY_TRAINANDSPECIES && 
					speciesSymbol != null) {
				return speciesSymbol;
			}
			else {
				symbol.setFillColor(aCol = PamColors.getInstance().getWhaleColor(click.getEventId()));
				symbol.setLineColor(aCol);
			}
		}
		return symbol;
	}
	//	static PamSymbol getClickSymbol(ClickIdentifier clickIdentifier, int eventId) {
	//		PamSymbol symbol = clickIdentifier.getSymbol(click);
	//		if (symbol == null) {
	//			symbol = getDefaultSymbol();
	//			symbol.setFillColor(PamColors.getInstance().getWhaleColor(eventId));
	//			symbol.setLineColor(PamColors.getInstance().getWhaleColor(eventId));
	//		}
	//		return symbol;
	//	}
	static PamSymbol getClickSymbol(ClickDetection click) {
		return getClickSymbol(click.getEventId());
	}

	static PamSymbol getClickSymbol(int eventId) {
		PamSymbol symbol = getDefaultSymbol();
		symbol.setFillColor(PamColors.getInstance().getWhaleColor(eventId));
		symbol.setLineColor(PamColors.getInstance().getWhaleColor(eventId));

		return symbol;
	}



	class BTPlot extends JBufferedPanel implements ColorManaged {
		boolean redrawClicks=false;
		long lasthScrollPos=-1;
		int lastVScrollPos=-1;
		int lastWidth=-1;
		int lastHeight=-1;
		BufferedImage b;


		private KeyPanel keyPanel;
		private BufferedImage dougsBufferedImage;
		private boolean totalRepaint;

		public BTPlot() {
			super();
			//			setSize(200, 200);
			setDoubleBuffered(true);
			setVisible(true);
			setExtraEast(200);
			setOpaque(true);
			//			PamColors.getInstance().registerComponent(this,
			//					PamColors.PamColor.PlOTWINDOW);
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			createKey();

			if (isViewer || isNetReceiver) {
				setToolTipText("Click detector bearing time display");
			}
			//			RepaintManager.currentManager(this).
		}
		public void setTotalRepaint() {
			totalRepaint = true;
		}
		@Override
		public PamColor getColorId() {
			return PamColor.PlOTWINDOW;
		}

		public Graphics getGraphics(){
			Graphics g=super.getGraphics();
			return g;
		}

		public BufferedImage getOverlayImage(Graphics g){

			int Height=super.getHeight();
			int Width=super.getWidth();

			BufferedImage bb = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
			//Color currentColor;
			Color currentColor;

			Graphics2D g2 = (Graphics2D) bb.getGraphics();
			currentColor = PamColors.getInstance().getColor(PamColor.MAP);
			g2.setColor(currentColor);
			g2.fillRect(0, 0, super.getWidth(), super.getHeight());		
			super.paint(g2);

			return bb;
		}


		public boolean redrawAllClicks(){
			//System.out.println(	hScrollBar.getValueMillis()+ " "+vScrollBar.getValue()+" "+	hScrollBar.getMaximumMillis()+"  "+hScrollBar.getStepSizeMillis());
			if (totalRepaint) {
				return true;
			}
			if (hScrollBar.getValueMillis()!=lasthScrollPos || vScrollBar.getValue()!= lastVScrollPos || lastHeight!=this.getHeight() || lastWidth!=this.getWidth() ){
		
				lasthScrollPos=hScrollBar.getValueMillis();
				lastVScrollPos=vScrollBar.getValue();
				lastHeight=this.getHeight();
				lastWidth=this.getWidth();

				return true	;
			}

			else	{
				return false;
			}
		}

//		double lastPaintTime;
		public void paintClicks(Graphics g, Rectangle clipRectangle) {

			long t0 = System.nanoTime();
			//			System.out.println("Paint all clicks");

			ClickDetection click = null;
			ClickDetection prevPlottedClick = null;
			PamDataBlock<ClickDetection> clickData = clickControl.getClickDataBlock();

			if (zoomer != null) {
				zoomer.paintShape(g, this, true);
			}

//			long t1 = System.nanoTime();
			synchronized (clickData) {
//				long t2 = System.nanoTime();
//				double ms = ((double) (t2-t1)) / 1000000.;
				
				ListIterator<ClickDetection> clickIterator = clickData.getListIterator(PamDataBlock.ITERATOR_END);
				while (clickIterator.hasPrevious()) {
					click = clickIterator.previous();
					if (shouldPlot(prevPlottedClick)){
						if (btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_ICI) {
							prevPlottedClick.setTempICI((double) (prevPlottedClick.getStartSample()-click.getStartSample()) / sampleRate);
						}
						if (drawClick(g, prevPlottedClick, clipRectangle) < -0){
							break;
						}
					}
					prevPlottedClick = click;
				}
				if (shouldPlot(prevPlottedClick)){ // and draw the last one !
					drawClick(g, prevPlottedClick, clipRectangle);
				}
//				g.drawString(String.format("Wait synch %3.3fms", ms), 0, 20);
			}
//			long t3 = System.nanoTime();
//			g.drawString(String.format("Last draw %3.3fms", lastPaintTime), 0, 20);
//			lastPaintTime = ((double) (t3-t0)) / 1000000.;
		}

		@Override
		public void paintPanel(Graphics g, Rectangle clipRectangle) {

			redrawClicks=redrawAllClicks();
			
			if (isViewer){
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			}
			
			//			g.setColor(Color.BLUE);
			//			g.fillRect(0, 0, 30, 30);
			if (hasData == false) {
				g.setColor(PamColors.getInstance().getColor(PamColor.AXIS));
				g.setFont(PamColors.getInstance().getBoldFont());
				Graphics2D g2d = (Graphics2D) g;
				String str = "Right click for display options menu";
				Rectangle2D r = g2d.getFontMetrics().getStringBounds(str, g2d);
				g.drawString(str, (int) ((getWidth() - r.getWidth())/2), 5 + (int)r.getHeight());
				//				return;
			}
			setScales();


			Graphics2D g2d = (Graphics2D) g;
			g2d.setStroke(new BasicStroke());

			if (isViewer == false) {
				paintClicks(g, clipRectangle);
				if (selectedClick != null) {
					drawClick(g, selectedClick, clipRectangle, true);
				}
				return;
			}

			// need to be always plotting the next / prev' click so that 
			// ici can be calculated on the fly if necessary. 

			if (redrawClicks==true || dougsBufferedImage == null) {
				createBufferedImage(clipRectangle, getBackground());
			}

			g2d.drawImage(dougsBufferedImage, 0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), null);

			if (selectedClick != null) {
				drawClick(g, selectedClick, clipRectangle, true);
			}


			if (zoomer != null) {
				//				zoomer.paintShape(g, this, true);
				zoomer.paintShape(g, this, false);
			}

			if (playbackStatus == PlaybackProgressMonitor.PLAY_START) {
				g.setColor(Color.RED);
				int x = clickXPos(playbackMilliseconds);
				g.drawLine(x, 0, x, getHeight());
			}
		}

		private void createBufferedImage(Rectangle clipRectangle, Color defBackground) {
			Graphics g;
			if (dougsBufferedImage == null || dougsBufferedImage.getHeight() != getHeight() || dougsBufferedImage.getWidth() != getWidth()) {
				dougsBufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			}
			g = dougsBufferedImage.getGraphics();
			g.setColor(defBackground);
			g.fillRect(0, 0, getWidth(), getHeight());
			paintClicks(g, clipRectangle);
			totalRepaint = false;
		}
		private int setTempICI(float f) {
			// TODO Auto-generated method stub
			return 0;
		}


		private void drawGrid(Graphics g, Rectangle clipRectangle)
		{

			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(PamColors.getInstance().getColor(PamColor.AXIS));
			int nL = 0;
			switch(btDisplayParameters.VScale){
			case BTDisplayParameters.DISPLAY_BEARING:
				nL = btDisplayParameters.nBearingGridLines;
				break;
			case BTDisplayParameters.DISPLAY_AMPLITUDE:
				nL = btDisplayParameters.nAmplitudeGridLines;
				break;
			case BTDisplayParameters.DISPLAY_ICI:
				nL = btDisplayParameters.nICIGridLines;
				break;
			}			
			int tH = getHeight();
			int y;
			double dH = (double) tH / (double) (nL + 1);
			//			float[] dashes = {2, 6};
			//			g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, dashes, 0));
			int x1 = clipRectangle.x;
			int x2 = x1 + clipRectangle.width;
			for (int i = 0; i < nL; i++) {
				y = (int) (dH * (i+1));
				g2.drawLine(0, y, getWidth(), y);
			}
		}

		public int drawClick(Graphics g, ClickDetection click, Rectangle clipRegion) {
			return drawClick(g, click, clipRegion, false);
		}


		public int drawClick(Graphics g, ClickDetection click, Rectangle clipRegion, boolean highlightClick) {
			//System.out.println("Draw Click");
			Point pt = clickXYPos(click);
			int width = (int) getClickWidth(click);
			int height = (int) getClickHeight(click);
			if (clipRegion != null) {
				if (pt.x + width < clipRegion.x) return -1;
				if (pt.x - width > clipRegion.x + clipRegion.width) return +1;
			}
			PamSymbol symbol = getClickSymbol(clickControl.getClickIdentifier(), click, btDisplayParameters.colourScheme);
			symbol.setFill(!click.isEcho());
			symbol.draw(g, pt, width, height);
			if (click.isTracked()) {
				g.drawLine(pt.x - width - 2, pt.y, pt.x + width + 2, pt.y);
				g.drawLine(pt.x, pt.y - height - 2, pt.x, pt.y + height + 2);
			}
			if (highlightClick) {
				highlightSymbol.setHeight(height+8);
				highlightSymbol.setWidth(width+8);
				highlightSymbol.draw(g, pt);
			}
			return 0;
		}

		public Rectangle getClickRectangle(ClickDetection click) {
			Point pt = clickXYPos(click);
			int width, height;
			if (click.isTracked()) {
				return new Rectangle(pt.x - (width = (int) getClickWidth(click)), pt.y
						- (height = (int) getClickHeight(click)), width * 2, height * 2);
			} else {
				return new Rectangle(pt.x - (width = (int) getClickWidth(click)) / 2, pt.y
						- (height = (int) getClickHeight(click)) / 2, width, height);
			}
		}

		/**
		 * Redraws the region around a click
		 * 
		 * @param click
		 *            Click to redraw
		 */
		public void invalidateClick(ClickDetection click) {
			repaint(getClickRectangle(click));
		}

		@Override
		public void setBackground(Color bg) {
			// TODO Auto-generated method stub
			super.setBackground(bg);
			if (keyPanel != null) {
				createKey();
				//				keyPanel.getTitledBorder().setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
			}
		}

		int tipCalls = 0;
		@Override
		public String getToolTipText(MouseEvent event) {
			if (!isViewer && !isNetReceiver) {
				return super.getToolTipText(event);
			}
			ClickDetection click = findClick(event.getX(), event.getY(), 10);
			if (click == null) {
				return null;
			}
			String tip = String.format("<html>Click No. %d; Channels %s", 
					click.clickNumber, PamUtils.getChannelList(click.getChannelBitmap()));
			byte type = click.getClickType();
			if (clickControl.getClickIdentifier() != null) {
				String typeStr = clickControl.getClickIdentifier().getSpeciesName(type);
				if (typeStr == null) {
					typeStr = "Unassigned";
				}
				tip += "<p>Species " + typeStr; 
			}
			DataUnitFileInformation fileInfo = click.getDataUnitFileInformation();
			if (fileInfo != null) {
				tip += String.format("<p>File %s", fileInfo.getFile().getName());
			}
			tip += String.format("<p>Update count %d", click.getUpdateCount());

			if (clickControl.getClickIdentifier() != null) {
				String idString = clickControl.getClickIdentifier().getParamsInfo(click);
				if (idString != null) {
					tip += "<p>" + idString;
				}
			}


			OfflineEventDataUnit odu = (OfflineEventDataUnit) click.getSuperDetection(OfflineEventDataUnit.class);
			if (odu != null) {
				tip += String.format("<p>Event %d, type %s<p>Event information ...", odu.getEventNumber(), odu.getEventType());
			}
			//			if (click.getDatabaseIndex() > 0) {
			tip += String.format("<p>&#x0009;Database Index %d", click.getDatabaseIndex());
			//			}
			if (odu != null) {
				tip +=  String.format("<p>&#x0009;Start %s", PamCalendar.formatDateTime(odu.getTimeMilliseconds()));
				tip +=  String.format("<p>&#x0009;End %s", PamCalendar.formatDateTime(odu.getEventEndTime()));
				tip +=  String.format("<p>&#x0009;Duration %3.1ss", (odu.getEventEndTime() - odu.getTimeMilliseconds())/1000.);
				tip +=  String.format("<p>&#x0009;No. of clicks %d", odu.getNClicks());
				Short n;
				n = odu.getMinNumber();
				if (n != null) {
					tip += String.format("<p>&#x0009;Min Animals %d", n);
				}
				n = odu.getBestNumber();
				if (n != null) {
					tip += String.format("<p>&#x0009;Best Animals %d", n);
				}
				n = odu.getMaxNumber();
				if (n != null) {
					tip += String.format("<p>&#x0009;Max Animals %d", n);
				}
				if (odu.getComment() != null && odu.getComment().length() > 0) {
					tip += "<p>&#x0009;Comment: " + odu.getComment();
				}
			}
			//			tip += "<\\html>";
			return tip;
		}

		public void createKey() {
			if (btDisplayParameters.showKey == false) {
				this.setKeyPanel(null);
				return;
			}
			keyPanel = new KeyPanel("Species", PamKeyItem.KEY_SHORT);
			if (btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_SPECIES) {
				keyPanel.add(new TextKeyItem("Colour by species"));
			}
			else if (btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_TRAIN) {
				keyPanel.add(new TextKeyItem("Colour by train"));
			}
			else if (btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_TRAINANDSPECIES) {
				keyPanel.add(new TextKeyItem("Colour by train and species"));
			}
			else if (btDisplayParameters.colourScheme == BTDisplayParameters.COLOUR_BY_HYDROPHONE) {
				keyPanel.add(new TextKeyItem("Colour by hydrophone"));
			}
			if (btDisplayParameters.getShowSpecies(0)) {
				keyPanel.add(getDefaultSymbol(true).makeKeyItem("Unidentified species"));
			}

			//			System.out.println("Create key now " + PamCalendar.formatDateTime(System.currentTimeMillis()));

			ClickIdentifier clickIdentifier = clickControl.getClickIdentifier();
			PamSymbol bwSymbol;
			String[] speciesList = clickControl.getClickIdentifier().getSpeciesList();
			PamSymbol[] symbols = clickControl.getClickIdentifier().getSymbols();
			if (speciesList != null) {
				for (int i = 0; i < speciesList.length; i++) {
					if (btDisplayParameters.getShowSpecies(i+1)) {
						if (btDisplayParameters.colourScheme != BTDisplayParameters.COLOUR_BY_TRAIN) {
							keyPanel.add(symbols[i].makeKeyItem(speciesList[i]));
						}
						else {
							bwSymbol = symbols[i].clone();
							bwSymbol.setFillColor(PamColors.getInstance().getColor(PamColor.PLAIN));
							bwSymbol.setLineColor(bwSymbol.getFillColor());
							keyPanel.add(bwSymbol.makeKeyItem(speciesList[i]));
						}
					}
				}
			}
			setKeyPanel(keyPanel);
			try {
			keyPanel.getTitledBorder().setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
			setKeyPosition(CornerLayoutContraint.LAST_LINE_START);
			}
			catch (NullPointerException e) {
//				e.printStackTrace();
			}
		}

	}

	/**
	 * @param x
	 *            Search X coordinate
	 * @param y
	 *            Search y coordinate
	 * @param maxdist
	 *            Maximum distance from (x,y) to the centre of the click.
	 * @return The closest click within maxDist of the point x, y or null if no
	 *         click is close enough
	 */
	ClickDetection findClick(int x, int y, int maxdist) {
		ClickDetection closestClick = null;
		PamDataBlock<ClickDetection> clickData = clickControl.getClickDataBlock();
		ClickDetection unit;
		Point pt;
		int dist;
		int closest = maxdist * maxdist;
		synchronized (clickData) {
			ListIterator<ClickDetection> clickIterator = clickData.getListIterator(PamDataBlock.ITERATOR_END);
			while (clickIterator.hasPrevious()) {
				unit = clickIterator.previous();
				//				if (unit.getTimeMilliseconds() < displayStartMillis - 1000)
				//					break;
				if (shouldPlot(unit) == false) continue;
				pt = clickXYPos(unit);
				if ((dist = ((pt.x - x) * (pt.x - x) + (pt.y - y) * (pt.y - y))) <= closest) {
					closest = dist;
					closestClick = unit;
				}
			}
		}

		return closestClick;
	}

	/**
	 * Count the number of loaded clicks not displayed due to 
	 * amplitude selection
	 * @return clicks not displayed due to amplitude selection
	 */
	private int countAmplitudeDeselected() {
		PamDataBlock<ClickDetection> clickData = clickControl.getClickDataBlock();
		ClickDetection click;
		int n = 0;
		synchronized (clickData) {
			ListIterator<ClickDetection> clickIterator = clickData.getListIterator(0);
			while (clickIterator.hasNext()) {
				click = clickIterator.next();
				if (click.getAmplitudeDB() < btDisplayParameters.minAmplitude) {
					n++;
				}
			}
		}
		return n;
	}

	/**
	 * Return true if the click should be plotted on the current display
	 * using the following tests:
	 * <p>Click is within the display time window
	 * <p>If the display is ICI, does the click have ICI ? 
	 * <p>If channels are selected, is if from the right ones ? 
	 * <p>Checks of selected species types
	 * 
	 * @param click click to test
	 * @return true if the click should be plotted
	 */
	synchronized boolean shouldPlot(ClickDetection click) {
		if (click == null) return false;
		if (!clickInTimeWindow(click)) return false;
		if (btDisplayParameters.showEchoes == false && click.isEcho()) {
			return false;
		}
		if (btDisplayParameters.VScale == BTDisplayParameters.DISPLAY_ICI) {
			if (btDisplayParameters.showUnassignedICI == false && click.getICI() < 0) return false;
			// otherwise may be ok, since will estimate all ici's on teh fly. 
		}
		if (btDisplayParameters.amplitudeSelect && click.getAmplitudeDB() < btDisplayParameters.minAmplitude) {
			return false;
		}
		if (btDisplayParameters.displayChannels > 0 && (btDisplayParameters.displayChannels & click.getChannelBitmap()) == 0) return false;

		int speciesIndex = clickControl.getClickIdentifier().codeToListIndex(click.getClickType());	
		boolean showSpecies = btDisplayParameters.getShowSpecies(speciesIndex+1);
		boolean showEvents = (btDisplayParameters.showEventsOnly == false || click.getSuperDetectionsCount() > 0);
		if (btDisplayParameters.showANDEvents) {
			return showSpecies & showEvents;
		}
		else {
			return showSpecies | showEvents;
		}
	}

	/**
	 * 
	 * @param click click detection
	 * @return true if the click is within the current time window. 
	 */
	boolean clickInTimeWindow(ClickDetection click) {
		if (click.getTimeMilliseconds() < displayStartMillis) {
			return false;
		}
		if (click.getTimeMilliseconds() > displayStartMillis + displayLengthMillis) {
			return false;
		}
		return true;
	}

	class TopControls extends JPanel implements ColorManaged {

		TopControls() {
			super();
			setBorder(new EmptyBorder(0, 30, 0, 10));
			setLayout(new BorderLayout());

			JPanel westBit = new PamPanel();
			JPanel eastBit = new PamPanel(new BorderLayout());
			westBit.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
			eastBit.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));

			timeLabel = new PamLabel("Time");
			cursorLabel = new PamLabel("");
			westBit.add(timeLabel);
			westBit.add(cursorLabel);
			add(BorderLayout.WEST, westBit);
			add(BorderLayout.EAST, eastBit);
			followCheckBox = new PamCheckBox("Follow Display");
			followCheckBox.setSelected(true);
			followCheckBox.addActionListener(new FollowBoxListener());
			if (PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW) {
				eastBit.add(followCheckBox);
			}
			eastBit.add(new PamLabel("Seconds"));

		}

		@Override
		public PamColor getColorId() {
			return PamColor.BORDER;
		}

		/**
		 * Only used in real time ops. 
		 */
		void ShowTime() {
			timeLabel.setText("Start: " + PamCalendar.formatDateTime(getDisplayStartMillis()));
		}
	}

	@Override
	public String getName() {
		return "Bearing Time Display";
	}

	@Override
	public void noteNewSettings() {
		if (clickControl.getClickDataBlock().getUnitsCount() > 0) {
			hasData = true;
		}
		reset();
		btPlot.createKey();
		repaintBoth();
	}

	public void notifyNewStorage(String storageName) {
		if (storageName != null) {
			getFrame().setTitle(getName() + " - " + storageName);
		}
	}

	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pamStart() {
		reset();
		if (clickControl.clickDetector.getParentDataBlock() == null) {
			return;
		}
		clickControl.clickDetector.getParentDataBlock().addObserver(this);
		// move this to constructor since detector will have been created first. 
		//		clickControl.clickDetector.getClickDataBlock().addObserver(this);
		sampleRate = clickControl.clickDetector.getSampleRate();
	}

	public int getDisplayChannels() {
		return btDisplayParameters.displayChannels;
	}

	public void setDisplayChannels(int displayChannels) {
		btDisplayParameters.displayChannels = displayChannels;
		repaintBoth();
	}

	public Serializable getSettingsReference() {
		return btDisplayParameters;
	}

	public long getSettingsVersion() {
		return BTDisplayParameters.serialVersionUID;
	}

	public String getUnitName() {
		return clickControl.getUnitName() + "_BTDisplay_" + displayNumber; 
	}

	public String getUnitType() {
		return "Click Detector Bearing Time Display";
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		this.btDisplayParameters = ((BTDisplayParameters) pamControlledUnitSettings
				.getSettings()).clone();
		//			rangeScrollBar.setValue(btDisplayParameters.vScrollValue);
//		rangeSpinner.setSpinnerValue(btDisplayParameters.getTimeRange());
//		btPlot.createKey();
		return true;
	}

	public ClickDetection getSelectedClick() {
		return selectedClick;
	}

	public void setSelectedClick(ClickDetection selectedClick) {
		this.selectedClick = selectedClick;
		clickControl.clickedOnClick(selectedClick);
		getPlotPanel().repaint(minPaintTime);
		//btPlot.paintPanelFast(btPlot.getGraphics());
	}

	/**
	 * Select the next or the preceding click
	 * @param relClick +1 to move forwards, -1 to move backwards
	 * @return new selected click. 
	 */
	private boolean selectClick(int relClick) {
		//		if ()
		PamDataBlock<ClickDetection> cdb = clickControl.getClickDataBlock();
		if (selectedClick == null) {
			if (relClick < 0) {
				//				selectedClick = cdb.getLastUnit();
				setSelectedClick(getLastSelectableClick());
				return (selectedClick != null);
			}
			else if (relClick > 0) {
				setSelectedClick(getFirstSelectableClick());
				return (selectedClick != null);
				//				selectedClick = cdb.getDataUnit(0, PamDataBlock.REFERENCE_CURRENT);
			}
		}
		// first find the current click
		ClickDetection click;
		ListIterator<ClickDetection> clickIterator = cdb.getListIterator(PamDataBlock.ITERATOR_END);
		while (clickIterator.hasPrevious()) {
			click = clickIterator.previous();
			if (click == selectedClick) {
				break;
			}
		}
		if (relClick > 0 && clickIterator.hasNext()) {
			clickIterator.next();
			while (clickIterator.hasNext()) { // do it again to get the right one since were going backwards before
				click = clickIterator.next();
				if (shouldPlot(click) && clickInMarkedArea(click)) {
					setSelectedClick(click);
					break;
				}
			}
		}
		else if (relClick < 0 && clickIterator.hasPrevious()) {
			while (clickIterator.hasPrevious()) {
				click = clickIterator.previous();
				if (shouldPlot(click) && clickInMarkedArea(click)) {
					setSelectedClick(click);
					break;
				}
			}
		}
		return (selectedClick != null);
	}

	@Override
	public void clickedOnClick(ClickDetection click) {
		// TODO Auto-generated method stub
		super.clickedOnClick(click);
	}

	private ClickDetection getFirstSelectableClick() {
		PamDataBlock<ClickDetection> cdb = clickControl.getClickDataBlock();
		ListIterator<ClickDetection> clickIterator = cdb.getListIterator(0);
		ClickDetection click;
		while (clickIterator.hasNext()) {
			click = clickIterator.next();
			if (shouldPlot(click) == false || clickInMarkedArea(click) == false) {
				continue;
			}
			return click;
		}
		return null;
	}

	private ClickDetection getLastSelectableClick() {
		PamDataBlock<ClickDetection> cdb = clickControl.getClickDataBlock();
		ListIterator<ClickDetection> clickIterator = cdb.getListIterator(PamDataBlock.ITERATOR_END);
		ClickDetection click;
		while (clickIterator.hasPrevious()) {
			click = clickIterator.previous();
			if (shouldPlot(click) == false || clickInMarkedArea(click) == false) {
				continue;
			}
			return click;
		}
		return null;
	}

	/**
	 * Test whether or not a click is within a marked area on the screen   
	 * @param click
	 * @return true if no mark or click is within the mark. 
	 */
	private boolean clickInMarkedArea(ClickDetection click) {
		if (zoomer == null) {
			return true;
		}
		Point clickPoint = clickXYPos(click);
		if (shouldPlot(click) == false) {
			return false;
		}
		return zoomer.isInMark(btPlot, clickPoint);
	}

	private ArrayList<ClickDetection> markedClicks;

	/**
	 * Called when a zoom polygon has been completed and 
	 * immediately makes a list of all marked clicks. 
	 * @param zoomShape 
	 */
	public void makeMarkedClickList() {
		if (markedClicks == null) {
			markedClicks = new ArrayList<ClickDetection>();
		}
		else {
			markedClicks.clear();
		}
		if (zoomer == null) {
			return;
		}
		PamDataBlock<ClickDetection> cdb = clickControl.getClickDataBlock();
		ListIterator<ClickDetection> clickIterator = cdb.getListIterator(0);
		ClickDetection click;
		while (clickIterator.hasNext()) {
			click = clickIterator.next();
			if (shouldPlot(click) == false || clickInMarkedArea(click) == false) {
				continue;
			}
			markedClicks.add(click);
		}
	}

	/**
	 * 
	 * @return a list of clicks which are within a marked area. 
	 */
	public ArrayList<ClickDetection> getMarkedClicks() {

		return markedClicks;
	}

	/**
	 * Scroll the display to a specific event. 
	 * @param event event to scroll to
	 */
	public void gotoEvent(OfflineEventDataUnit event) {
		long evStart = event.getTimeMilliseconds();
		if (evStart < hScrollBar.getMinimumMillis() || evStart > hScrollBar.getMaximumMillis()) {
			long range = hScrollBar.getMaximumMillis() - hScrollBar.getMinimumMillis();
			hScrollBar.setRangeMillis(evStart, evStart + range, true);
		}
		hScrollBar.setValueMillis(evStart);

	}

	int playbackStatus = PlaybackProgressMonitor.PLAY_END;
	int playbackChannels;
	long playbackMilliseconds;
	class ClickPlaybackMonitor implements PlaybackProgressMonitor {

		@Override
		public void setProgress(int channels, long timeMillis, double percent) {
			playbackChannels = channels;
			playbackMilliseconds = timeMillis;
			btPlot.repaint();
		}

		@Override
		public void setStatus(int status) {
			playbackStatus = status;
			btPlot.repaint();
		}

	}
	/**
	 * Play the first two channels in the channel list for this display. 
	 */
	public void playViewerData() {
		int chanList = clickControl.clickParameters.channelBitmap;
		int nChan = PamUtils.getNumChannels(chanList);
		if (nChan == 0) {
			return;
		}
		long startMillis = hScrollManager.getDisplayStartMillis();
		long endMillis = startMillis + displayLengthMillis;
		if (zoomer != null) {
			ZoomShape topShape = zoomer.getTopMostShape();
			if (topShape != null) {
				startMillis = (long) topShape.getXStart();
				endMillis = (long) (startMillis + topShape.getXLength());
			}
		}
		if (nChan > 2) {
			PlaybackControl.getViewerPlayback().playViewerData(startMillis, 
					endMillis, clickPlaybackMonitor);
		}
		else {
			PlaybackControl.getViewerPlayback().playViewerData(chanList, startMillis, 
					endMillis, clickPlaybackMonitor);
		}

	}

	void playReconstructedClicks() {
		int chanList = clickControl.clickParameters.channelBitmap;
		int nChan = PamUtils.getNumChannels(chanList);
		if (nChan == 0) {
			return;
		}
		long startMillis = hScrollManager.getDisplayStartMillis();
		long endMillis = startMillis + displayLengthMillis;
		if (zoomer != null) {
			ZoomShape topShape = zoomer.getTopMostShape();
			if (topShape != null) {
				startMillis = (long) topShape.getXStart();
				endMillis = (long) (startMillis + topShape.getXLength());
			}
		}
		int channels = 3 & chanList; 
		ClickPlayLoader cpl = new ClickPlayLoader(channels);
		PlaybackControl.getViewerPlayback().playViewerData(channels, startMillis, 
				endMillis, clickPlaybackMonitor, cpl);
	}

	class ClickPlayLoader implements PlaybackDataServer {

		private int channels;
		private OfflineEventDataUnit offlineEvent;
		private double[][] rawData;
		private int nChannels;
		private int[] channelNumbers;
		private ListIterator<ClickDetection> clickIterator;

		private volatile boolean cancelNow = false;
		/**
		 * @param channels
		 * @param offlineEvent
		 */
		public ClickPlayLoader(int channels, OfflineEventDataUnit offlineEvent) {
			super();
			this.channels = channels;
			this.offlineEvent = offlineEvent;
		}

		/**
		 * @param channels
		 */
		public ClickPlayLoader(int channels) {
			super();
			this.channels = channels;
		}

		@Override
		public void cancelPlaybackData() {
			cancelNow = true;
		}

		@Override
		public void orderPlaybackData(PamObserver dataObserver,
				PlaybackProgressMonitor progressMonitor, float playbackRate, long startMillis,
				long endMillis) {
			/*
			 * Need to go through the clicks packing them into dummy
			 * rawDataUnits which can get sent on to dataObserver. 
			 */
			ClickDetection currentSelectedClick = getSelectedClick();
			nChannels = PamUtils.getNumChannels(channels);
			channelNumbers = PamUtils.getChannelArray(channels);
			float sampleRate = clickControl.getClickDetector().getSampleRate();
			int blockLength = (int) (playbackRate / 20); // will output data in 1/10 s blocks
			if (blockLength < 1000) {
				blockLength = 1000;
			}
			long blockMillis = (long) ((blockLength * 1000) / sampleRate);
			rawData = new double[nChannels][blockLength];
			RawDataUnit rawDataUnit;
			ClickDetection click;
			ClickDetection prevClick = null;
			PamDataBlock<ClickDetection> clickData = clickControl.getClickDataBlock();
			long dataUnitMillis = startMillis;
			long dataUnitSample = 0;
			int blockPosition = 0;
			long currentClickSample = 0;
			long clickChannels;
			int clickWavePos = 0;
			double[][] clickWave = null;
			int clickLen = 0;
			try {
				clickIterator = clickData.getListIterator(0);

				click = getFirstClick(startMillis);
				if (click != null) {
					clickChannels = click.getChannelBitmap();
					clickWave = click.getWaveData();
					clickLen = clickWave[0].length;
					clickWavePos = 0;

					blockPosition = (int) ((click.getTimeMilliseconds() - dataUnitMillis) * sampleRate / 1000.);
				}
				/*
				 * Main loop is over output data blocks since they will always exist. 
				 * For each set out output units, move through clicks (if any) and copy
				 * data from those clicks into the output data units. 
				 */
				while (dataUnitMillis < endMillis) {
					if (click == null) {
						break;
					}
					while (blockPosition < blockLength) {
						for (int iChan = 0; iChan < nChannels; iChan++) {
							rawData[iChan][blockPosition] = clickWave[iChan][clickWavePos];
						}
						blockPosition++;
						clickWavePos++;
						if (clickWavePos == clickLen) { // move onto the next click. 
							prevClick = click;
							click = getNextClick();
							if (click != null) {
								clickChannels = click.getChannelBitmap();
								clickWave = click.getWaveData();
								clickLen = clickWave[0].length;
								clickWavePos = 0;
								if (click.getStartSample() < prevClick.getStartSample() - sampleRate) {
									/**
									 * probably started a new file, so work out time of next click based
									 * on millisecond time. 
									 */
									blockPosition = (int) ((click.getTimeMilliseconds() - dataUnitMillis) * sampleRate / 1000.);
								}
								else {
									blockPosition += (click.getStartSample() - prevClick.getStartSample());
								}
							}
							else {
								break;
							}
						}
					}
					if (cancelNow) {
						break;
					}
					for (int i = 0; i < nChannels; i++) {
						rawDataUnit = new RawDataUnit(dataUnitMillis, 1<<channelNumbers[i],
								dataUnitSample, blockLength);
						rawDataUnit.setRawData(rawData[i]);
						dataObserver.update(null, rawDataUnit);
						//						setSelectedClick(prevClick);
						clickControl.clickedOnClick(prevClick);
						selectedClick = prevClick;
						rawData[i] = new double[blockLength];
					}
					dataUnitSample += blockLength;
					dataUnitMillis += blockMillis;
					blockPosition -= blockLength;
				}
			}
			catch (ConcurrentModificationException e) {
				// will hit here if click data block is modified in any way. 

			}
			catch (Exception e) {
				e.printStackTrace();
			}

			progressMonitor.setStatus(PlaybackProgressMonitor.PLAY_END);
			setSelectedClick(currentSelectedClick);
		}

		boolean wantClick(ClickDetection click) {
			if (shouldPlot(click) == false) {
				return false;
			}
			if (clickInMarkedArea(click) == false) {
				return false;
			}
			if (offlineEvent != null) {
				if (click.getSuperDetection(OfflineEventDataUnit.class) != offlineEvent) {
					return false;
				}
			}
			return true;
		}

		ClickDetection getFirstClick(long startMillis) {
			ClickDetection click;
			while (clickIterator.hasNext()) {
				click = clickIterator.next();
				if (wantClick(click) == false){
					continue;
				}
				if (click.getTimeMilliseconds() < startMillis) {
					continue;
				}
				return click;
			}
			return null;
		}

		ClickDetection getNextClick() {
			ClickDetection click;
			while (clickIterator.hasNext()) {
				click = clickIterator.next();
				if (wantClick(click) == false){
					continue;
				}
				return click;
			}
			return null;
		}
	}

	public void playClicks() {
		playReconstructedClicks();
	}

	/**
	 * @return the btDisplayParameters
	 */
	public BTDisplayParameters getBtDisplayParameters() {
		return btDisplayParameters;
	}

	/**
	 * @return the ctrlKeyManager
	 */
	public CtrlKeyManager getCtrlKeyManager() {
		return ctrlKeyManager;
	}
}