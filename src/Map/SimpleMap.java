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
package Map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;

import pamScrollSystem.AbstractPamScroller;
import pamScrollSystem.PamScrollObserver;
import pamScrollSystem.PamScrollSlider;
import Array.ArrayManager;
import Array.PamArray;
import GPS.GpsData;
import GPS.GpsDataUnit;
import GPS.ProcessNmeaData;
import PamController.PamController;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.ClipboardCopier;
import PamView.CornerLayoutContraint;
import PamView.PamBorderPanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

/**
 * Mainly a container for map objects, holding the main MapPanel and the right
 * hand control items. <br>
 * Originally written by Dave McLaren. Modified by Doug Gillespie to incorporate
 * controls onto main panel to increase overall visible size.
 * 
 */
public class SimpleMap extends JPanel implements PamObserver, PamScrollObserver {

	MapMouseMotionAdapter mouseMotion = new MapMouseMotionAdapter();

	MapMouseInputAdapter mouseInput = new MapMouseInputAdapter();

	MouseWheelHandler mouseWheel = new MouseWheelHandler();

	boolean centerOnFirstShipGPS = true;

	ArrayList<PamDataBlock> dataBlocks;

	double shipRotTest = 0.0;

	// GpsData gpsData;

	PamDataBlock<GpsDataUnit> gpsDataBlock;

	MapPanel mapPanel;

	// JPanel mapControlPanel;

	GpsTextDisplay gpsTextPanel;

	DisplayPanZoom panZoom;

	MouseMeasureDisplay mouseMeasureDisplay;

	PamZoomOnMapPanel panZoomOnMap;

	private JPanel controlContiner;

	protected ClipboardCopier clipboardCopier;

	MapController mapController;

	boolean mouseDragging;

	private PamDataUnit mousedDataUnit = null;

	LatLong lastClickedMouseLatLong;

	boolean mouseReleased = false;

	boolean mapCanScroll = true;

	Coordinate3d newCursorPos = new Coordinate3d();

	Coordinate3d oldCursorPos = new Coordinate3d();

	Coordinate3d diffCursorPos = new Coordinate3d();

	Point mouseDownPoint, mouseDragPoint;

	private PamScrollSlider viewerScroller;

	// JToolTip mouseToolTip;

	public SimpleMap(MapController mapController) {
		// Make this an observer of the (GPS) output data blocks of
		// ProcessNmeaData

		this.mapController = mapController;

		createMapObjects();

		initMapPanel();

		mapPanel.setSimpleMapRef(this);

		lastClickedMouseLatLong = new LatLong(0.0, 0.0);

		clipboardCopier = new ClipboardCopier(this, "PAMGUARD Map");

	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}

	/**
	 * Fires once a second so map can re-draw, including update of ship position
	 * based on prediction now that GPS is not read out every second.
	 */
	protected void timerActions() {
		// this repaint and other actions are
		// needed to give the ship a smooth look when GPS data are
		// slow to arrive
		mapPanel.repaint(1000);
		gpsTextPanel.updateGpsTextArea();
		gpsTextPanel.displayZoomedorRotated(); // causes it to redisplay
		// information
	}

	/**
	 * Create the map objects, but don't necessarily show them.
	 * 
	 */
	private void createMapObjects() {
		this.setOpaque(true);
		setBackground(Color.white);
		// simpleMap = new JPanel();
		gpsTextPanel = new GpsTextDisplay(mapController, this);

		mouseMeasureDisplay = new MouseMeasureDisplay(mapController, this);

		// mapControlPanel = new JPanel();
		//
		// mapControlPanel.setLayout(new GridLayout(0, 1));

		panZoom = new DisplayPanZoom();

		this.setLayout(new BorderLayout());

		int panelHeight = 600;
		int panelWidth = 600;
		mapPanel = new MapPanel(mapController);
		// mapPanel.setSize(panelWidth, panelHeight);
		// mapPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
		// myMP.setPanelHeight(panelHeight);
		// myMP.setPanelWidth(panelWidth);
		mapPanel.setMapRotationDegrees(45.0);
		mapPanel.setBorder(BorderFactory.createRaisedBevelBorder());

		mapPanel.addMouseListener(mouseMotion);
		mapPanel.addMouseMotionListener(mouseMotion);
		mapPanel.addMouseListener(mouseInput);
		mapPanel.addMouseWheelListener(mouseWheel);

		panZoom.setHandler(new PamZoomOnMapPanel(mapPanel));

		this.add(BorderLayout.CENTER, mapPanel);

		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			viewerScroller = new PamScrollSlider(mapController.getUnitName(), 
					AbstractPamScroller.HORIZONTAL, 1000, 3600L*2L*1000L, true);
			this.add(BorderLayout.SOUTH, viewerScroller.getComponent());
			viewerScroller.addObserver(this);
		}

		mapPanel.setMapRotationDegrees(shipRotTest);

		mapPanel.setMapRotationDegrees(0.0);
		mapPanel.setMapRangeMetres(10000);

		// gpsTextPanel.setPreferredSize(new Dimension(5 * 35 + 10, 5 * 35 +
		// 10));
		// gpsTextPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		gpsTextPanel.setBorder(new CompoundBorder(BorderFactory
				.createRaisedBevelBorder(), new EmptyBorder(5, 5, 5, 5)));

		panZoom.setBorder(new CompoundBorder(BorderFactory
				.createRaisedBevelBorder(), new EmptyBorder(5, 5, 5, 5)));

		// mapControlPanel.add(panZoom);
		// mapControlPanel.add(gpsTextPanel);
		// this.add(BorderLayout.EAST, mapControlPanel);
		controlContiner = new PamBorderPanel();
		controlContiner.setLayout(new BoxLayout(controlContiner,
				BoxLayout.Y_AXIS));
		controlContiner.add(mouseMeasureDisplay);
		controlContiner.add(panZoom);
		controlContiner.add(gpsTextPanel);
		CornerLayoutContraint c = new CornerLayoutContraint();
		c.anchor = CornerLayoutContraint.LAST_LINE_END;
		mapPanel.add(controlContiner, c);
		this.setVisible(true);

		showMapObjects();

	}

	public void showMapObjects() {
		boolean showMouseMeasure = getShowMouseMeasure();
		controlContiner.setVisible(mapController.mapParameters.showGpsData
				|| mapController.mapParameters.showPanZoom || showMouseMeasure);
		mouseMeasureDisplay.setVisible(showMouseMeasure);
		gpsTextPanel.setVisible(mapController.mapParameters.showGpsData);
		panZoom.setVisible(mapController.mapParameters.showPanZoom);
	}

	private boolean getShowMouseMeasure() {
		if (mapController.getMouseMoveAction() == MapController.MOUSE_MEASURE
				&& mouseDragPoint != null) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#initMapPanel()
	 */
	public void initMapPanel() {

		dataBlocks = PamController.getInstance().getDataBlocks(
				GpsDataUnit.class, false);
		// add this as an observer of the data block
		for (int i = 0; i < dataBlocks.size(); i++) {
			gpsDataBlock = dataBlocks.get(i);
			if (gpsDataBlock.getParentProcess().getClass() != ProcessNmeaData.class) {
				continue; // skip if it's not reall GPS data (i.e. buoy GPS data).
			}
			gpsDataBlock.addObserver(this);
			if (i == 0) {
				if (mapController.mapProcess != null) {
					mapController.mapProcess.setParentDataBlock(gpsDataBlock);
				}
			}
		}

		// mapPanel.refreshPlotableDetectorLists();
		mapPanel.updateObservers();
		mapPanel.repaint();
		mapPanel.createKey();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#getMapPanel()
	 */
	public MapPanel getMapPanel() {
		return (mapPanel);
	}

	public void update(PamObservable o, PamDataUnit arg) {
		PamDataBlock block = (PamDataBlock) o;
		if (gpsTextPanel == null)
			return;
		if (mapPanel == null)
			return;
		if (block.getUnitClass() == GpsDataUnit.class) {
			newGpsData((GpsDataUnit) arg);
		}
	}

	private void newGpsData(GpsDataUnit newGpsDataUnit) {

		PamArray currentArray = ArrayManager.getArrayManager()
		.getCurrentArray();
		if (currentArray != null
				&& currentArray.getArrayType() == PamArray.ARRAY_TYPE_STATIC) {
			return;
		}
		GpsData gpsData = newGpsDataUnit.getGpsData();

		LatLong latLong = new LatLong(gpsData.getLatitude(), gpsData
				.getLongitude());

		// 54.0+40/3600.0;
		// latLong.latDegs = 60.0 + 20.0/3600.0;
		// myMP.setMapCentreDegrees(latLong);
		// myMP.setShipLLD(latLong);
		// myMP.setMapRotationDegrees(shipRotTest++);
		if (centerOnFirstShipGPS) {
			mapPanel.setMapCentreDegrees(latLong);
			centerOnFirstShipGPS = false;
		}
		mapPanel.newGpsData(newGpsDataUnit);
		gpsTextPanel.setLastFix(gpsData);
		gpsTextPanel.updateGpsTextArea();
		this.repaint();
		gpsTextPanel.setPixelsPerMetre(mapPanel.getPixelsPerMetre());
		gpsTextPanel.newShipGps();
		// gpsTextPanel.setShipPosition(mapPanel.ship.getShipPosition());

	}

	public String getObserverName() {
		return "simple map display component";
	}

	public void noteNewSettings() {

	}

	public void setSampleRate(float sampleRate, boolean notify) {
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub

	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		PamDataBlock block = (PamDataBlock) o;
		if (block.getUnitClass() == GpsDataUnit.class) {
			return Math.max(mapController.mapParameters.dataKeepTime,
					mapController.mapParameters.trackShowTime) * 1000;
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#getDataBlocks()
	 */
	public ArrayList<PamDataBlock> getDataBlocks() {
		return dataBlocks;
	}

	Coordinate3d getShipCoordinate() {
		GpsData shipGps = mapPanel.getShipGpsData(true);
		if (shipGps == null) {
			return null;
		}
		return mapPanel.getRectProj().getCoord3d(shipGps.getLatitude(),
				shipGps.getLongitude(), 0);
	}

	class MapMouseMotionAdapter extends MouseMotionAdapter implements
	MouseListener, MouseMotionListener {

		@Override
		public void mouseMoved(MouseEvent e) {

			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			LatLong mouseLL = mapPanel.getRectProj().panel2LL(
					new Coordinate3d(e.getX(), e.getY(), 0.0));
			gpsTextPanel.updateMouseCoords(mouseLL);
			gpsTextPanel.setMouseX(e.getX());
			gpsTextPanel.setMouseY(e.getY());
			/*
			 * System.out .println("Mouse moved " + " (" + e.getX() + "," +
			 * e.getY() + ")" + " detected on " +
			 * e.getComponent().getClass().getName());
			 */
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (mousedDataUnit != null) {
				return;
			}
			switch (mapController.getMouseMoveAction()) {
			case MapController.MOUSE_MEASURE:
				mouseMeasure(e);
				mouseMoved(e);
				break;
			case MapController.MOUSE_PAN:
				mousePan(e);
				break;
			}
		}

		private void mouseMeasure(MouseEvent e) {

			// LatLong mouseLL = mapPanel.getRectProj().panel2LL(new
			// Coordinate3d(e.getX(), e.getY(), 0.0));
			mouseDragPoint = new Point(e.getX(), e.getY());
			mouseMeasureDisplay.showMouseData(mouseDownPoint, mouseDragPoint);
			mapPanel.repaint(false);

		}

		private void mousePan(MouseEvent e) {

			if (mapCanScroll) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

				newCursorPos.x = e.getX();
				newCursorPos.y = e.getY();
				diffCursorPos.x = oldCursorPos.x - newCursorPos.x;
				diffCursorPos.y = oldCursorPos.y - newCursorPos.y;

				if (mouseDragging) {
					mapPanel.setMapCentreCoords(new Coordinate3d(mapPanel
							.getWidth()
							/ 2.0 + diffCursorPos.x, mapPanel.getHeight() / 2.0
							+ diffCursorPos.y));
				}
				mouseDragging = true;
				oldCursorPos.x = newCursorPos.x;
				oldCursorPos.y = newCursorPos.y;
				mapPanel.repaint();
			} else {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stubLatLongDegrees mouseLL =
			// myMP.getRectProj().panel2LL(
			// lastClickedMouseLatLong.latDegs=mouseLL.latDegs;
			// lastClickedMouseLatLong.longDegs=mouseLL.longDegs;
			// System.out.println("mouseClicked");
		}

		@Override
		public void mousePressed(MouseEvent e) {

			LatLong mouseLL = mapPanel.getRectProj().panel2LL(
					new Coordinate3d(e.getX(), e.getY(), 0.0));

			mouseDownPoint = new Point(e.getX(), e.getY());

			// set this in a global variable, so that other modules can access
			// it
			MapController.setMouseClickLatLong(mouseLL);

			// System.out.println("before mousePressed " + mouseLL.latDegs);
			mapPanel.getRectProj().setLastClickedMouseLatLong(mouseLL);
			// System.out.println("after mousePressed " +
			// myMP.getRectProj().getLastClickedMouseLatLong().latDegs);
			mapPanel.getRectProj().getLastClickedMouseLatLong();

			mouseReleased = false;

			gpsTextPanel.copyMouseMapPositionToClipboard(mouseLL.getLatitude(),
					mouseLL.getLongitude());

			mousedDataUnit = mapPanel.getRectProj().getHoveredDataUnit();
			if (mousedDataUnit != null) {
				return;
			}

			if (mapController.getMouseMoveAction() == MapController.MOUSE_MEASURE) {
				mapPanel.setCursor(Cursor
						.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				mouseDragPoint = (Point) mouseDownPoint.clone();
				showMapObjects();
				// mouseToolTip = createToolTip();
				// mouseToolTip.setTipText("Mouse tool tip text");
				// mouseToolTip.setLocation(200,200);
				// mouseToolTip.setVisible(true);

				// mapPanel.getCursor().
			} else {
				mapPanel.setCursor(Cursor
						.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			mouseDragging = false;
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			mouseDownPoint = mouseDragPoint = null;
			// System.out.println("mouseReleased");
			mapPanel.setCursor(Cursor
					.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			if (mapController.getMouseMoveAction() == MapController.MOUSE_MEASURE) {
				showMapObjects();
			}
			mousedDataUnit = null;
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub

		}

	}

	private class MouseWheelHandler implements MouseWheelListener {

		public void mouseWheelMoved(MouseWheelEvent arg0) {

			if (arg0.getWheelRotation() < 0
					& mapPanel.getMapRangeMetres() >= 200) {
				mapPanel.setMapZoom(0.97f);
			}
			if (arg0.getWheelRotation() > 0
					& mapPanel.getMapRangeMetres() <= 500000000) {
				mapPanel.setMapZoom(1.03f);
			}
			mapPanel.repaint();

		}

	}

	class MapMouseInputAdapter extends MouseInputAdapter {
		@Override
		public void mouseExited(MouseEvent e) {
			gpsTextPanel.mouseExited();
		}

	}

	/**
	 * Gets a data unit currently hovered by the mouse. This only gets set if
	 * the mouse actually hovers and is then clicked.
	 * 
	 * @return hovered data unit.
	 */
	public PamDataUnit getMousedDataUnit() {
		return mousedDataUnit;
	}

	@Override
	protected void paintComponent(Graphics arg0) {
		super.paintComponent(arg0);

		// mapPanel.setPreferredSize(new Dimension(
		// (int) ((double) this.getWidth() * 0.6), (int) ((double) this
		// .getWidth() * 0.6)));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#removeObservable(PamguardMVC.PamObservable)
	 */
	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#getLastClickedMouseLatLong()
	 */
	public LatLong getLastClickedMouseLatLong() {
		return lastClickedMouseLatLong;
	}

	protected void createMapComment(int x, int y) {
		LatLong latLong = getLastClickedMouseLatLong().clone();
		long time = PamCalendar.getTimeInMillis();
		String comment = "Test comment";

		MapComment mapComment = new MapComment(time, latLong, comment);

		mapComment = MapCommentDialog.showDialog(mapController.getPamView()
				.getGuiFrame(), mapPanel, new Point(x, y), mapComment);

		if (mapComment != null) {
			mapController.mapProcess.getMapCommentDataBlock().addPamData(
					mapComment);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#getMouseMotion()
	 */
	public MapMouseMotionAdapter getMouseMotion() {
		return mouseMotion;
	}

	public JComponent getPanel() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Map.PamMap#addMouseAdapterToMapPanel(java.awt.event.MouseAdapter)
	 */
	public void addMouseAdapterToMapPanel(MouseAdapter mouseAdapter) {
		// TODO Auto-generated method stub
		mapPanel.addMouseListener(mouseAdapter);
		mapPanel.addMouseMotionListener(mouseAdapter);
	}

	public void mapCanScroll(boolean b) {
		// TODO Auto-generated method stub
		mapCanScroll = b;
	}

	/**
	 * 
	 */
	public void refreshDetectorList() {
		// TODO Auto-generated method stub
		// mapPanel.refreshPlotableDetectorLists();
		// System.out.println(
		// "mapPanel.refreshPlotableDetectorLists(); called from refreshDetectorList"
		// );
	}

	/**
	 * Notification of new viewer times
	 */
	protected void newViewTimes() {
		// need to clear the base drawing
		mapPanel.repaintBaseDrawing();

		newViewTime();
	}

	/**
	 * Notification that the viewer slider has moved.
	 */
	protected void newViewTime() {
		if (gpsDataBlock == null)
			return;
		long now = PamCalendar.getTimeInMillis();
		now = viewerScroller.getValueMillis();
		GpsDataUnit gpsDataUnit = gpsDataBlock.getClosestUnitMillis(now);
		mapPanel.newViewTime(gpsDataUnit);

		if (gpsDataUnit == null)
			return;

		newGpsData(gpsDataUnit);

		GpsData gpsData = gpsDataUnit.getGpsData();

		if (centerOnFirstShipGPS) {
			mapPanel.setMapCentreDegrees(gpsData);
			centerOnFirstShipGPS = false;
		}

		repaint();
	}


	@Override
	public void scrollRangeChanged(AbstractPamScroller absPamScroller) {
		newViewTimes();
	}

	@Override
	public void scrollValueChanged(AbstractPamScroller abstractPamScroller) {
		newViewTime();
	}

	/**
	 * Subscribes a variety of data blocks to the scroll bar. 
	 * @return returns true if the list has changes, indicating
	 * that it's probably necessary to call loadData in the
	 * scroll manager to get new data. 
	 */
	public boolean subscribeViewerBlocks() {
		if (viewerScroller == null) {
			return false;
		}
		boolean changes = false;
		viewerScroller.addDataBlock(gpsDataBlock);
		MapDetectionsManager detManager = mapController.mapDetectionsManager;
		ArrayList<PamDataBlock> detectorDataBlocks = detManager.plottableBlocks;
		PamDataBlock dataBlock;
		for (int i = 0; i < detectorDataBlocks.size(); i++) {
			dataBlock = detectorDataBlocks.get(i);
			if (detManager.isShouldPlot(dataBlock)) {
				if (dataBlock.getNumOfflineDataMaps() > 0) {
					if (viewerScroller.isDataBlockUsed(dataBlock) == false) {
						viewerScroller.addDataBlock(dataBlock);
						changes = true;
					}
				}
			}
			else {
				if (viewerScroller.isDataBlockUsed(dataBlock)) {
					viewerScroller.removeDataBlock(dataBlock);
					changes = true;
				}
			}
		}
		return changes;

	}

	public PamScrollSlider getViewerScroller() {
		return viewerScroller;
	}

}