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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.Vector;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import pamScrollSystem.PamScrollSlider;
import Array.ArrayManager;
import Array.Hydrophone;
import Array.PamArray;
import GPS.GPSControl;
import GPS.GPSParameters;
import GPS.GpsData;
import GPS.GpsDataUnit;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamModel.PamModel;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.BasicKeyItem;
import PamView.ColorManaged;
import PamView.ColourArray;
import PamView.JPanelWithPamKey;
import PamView.KeyPanel;
import PamView.PamColors;
import PamView.PamKeyItem;
import PamView.PamSymbol;
import PamView.PamColors.PamColor;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

/**
 * This is the actual map display, with the bluebackground, the gps track, etc.
 * IT sits inside the SimpleMap along with the right hand controls. 
 *
 */
public class MapPanel extends JPanelWithPamKey implements PamObserver, ColorManaged {

	private static final long serialVersionUID = 1L;
	JPopupMenu detectorMenu;
	JPopupMenu plotDetectorMenu;
	private double mapRotationDegrees;
//	private LatLong shipLLD = new LatLong();
	private MapRectProjector rectProj = new MapRectProjector();
	private LatLong mapCentreDegrees;
	private int mapRangeMetres;
	TransformUtilities trans = new TransformUtilities();
	private double pixelsPerMetre = 1.0;
	public Vessel ship;
	public Compass myCompass;
	StraightLineGrid grid;
//	ArrayList<PamDataBlock> plottedBlocks;
//	ArrayList<PamDataBlock> detectorDataBlocks;
	MouseAdapter popupListener;
	MouseAdapter detectorPopupListener;
	MapController mapController;
	MapContourGraphics mapContourGraphics;
	SimpleMap simpleMapRef;
	Coordinate3d[] arraygeom;
	Coordinate3d[] arrayPanelOffsets;
	Coordinate3d[] arrayPanelPositions;
	private Color coastColour = new Color(100, 50, 0);
	private Color shallowColour = new Color(0, 255, 255);
	private Color deepestColour = new Color(0, 0, 255);
	
	private AffineTransform baseXform;
	
	private boolean repaintBase = false;
	
	public MapPanel(MapController mapController) {
		this.mapController = mapController;
		this.setOpaque(true);
		ship = new Vessel(Color.RED);
		grid = new StraightLineGrid();
		mapCentreDegrees = new LatLong();
		myCompass = new Compass();
		mapContourGraphics = new MapContourGraphics();

		baseXform = new AffineTransform();
		baseXform.scale(1,1);
		
		//GetPlotDetectorMenu();
		detectorPopupListener = new DetectorPopupListener();
		addMouseListener(detectorPopupListener);
		// contour();
		baseXform = new AffineTransform();
		baseXform.scale(1,1);
		rectProj.setMapPanelRef(this);
		
		addMouseListener(rectProj.getMouseHoverAdapter(this));
		addMouseMotionListener(rectProj.getMouseHoverAdapter(this));

		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		
//		PamColors.getInstance().registerComponent(this, PamColor.MAP);
	
	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}
	
	@Override
	public PamColor getColorId() {
		return PamColor.MAP;
	}
	
	
	public void newViewTime(GpsDataUnit gpsDataUnit) {

		if (gpsDataUnit == null) return;
		
		GpsData gpsData = gpsDataUnit.getGpsData();
		
		ship.setShipGps(gpsData);

	}

	/**
	 * Called from simple map when new gps data arrive. 
	 * @param newGpsDataUnit
	 */
	protected void newGpsData(GpsDataUnit newGpsDataUnit) {
		ship.setShipGps(newGpsDataUnit.getGpsData());
		newShipLLD();
		paintNewGPSData(newGpsDataUnit);
	}

	private PamArray lastArray;
//	long lastCall = 0;
	private LatLong lastMapCentreDegrees = new LatLong(0,0);
	private int lastMapRangeMetres = 0;
	private double lastMapRotationDegrees = 0;
	private int lastHeight, lastWidth;
	private BufferedImage baseDrawing;
	private int imageWidth, imageHeight; 
	
	@Override
	public void paintComponent(Graphics g) {
		
		if (isShowing() == false || g == null) return;
		
		super.paintComponent(g);
		
		rectProj.clearHoverList();

		PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
		if (currentArray != null && currentArray.getArrayType() == PamArray.ARRAY_TYPE_STATIC) {
			PamDataBlock<GpsDataUnit> dataBlock = currentArray.getFixedPointReferenceBlock();
			GpsDataUnit dataUnit = dataBlock.getLastUnit();
			if (dataUnit != null) {
				GpsData gpsData = dataUnit.getGpsData();
				ship.setShipGps(gpsData);
				if (currentArray != lastArray) {
					setMapCentreDegrees(new LatLong(gpsData.getLatitude(), gpsData.getLongitude()));
					lastArray = currentArray;
				}
			}
		}

		pixelsPerMetre = (double) this.getHeight() / mapRangeMetres;
		rectProj.setMapCentreDegrees(mapCentreDegrees);
		rectProj.setMapRangeMetres(mapRangeMetres);
		rectProj.setMapRotationDegrees(mapRotationDegrees);
		rectProj.setPanelHeight(this.getHeight());
		rectProj.setPanelWidth(this.getWidth());
		rectProj.setPixelsPerMetre(pixelsPerMetre);
		ship.setPixelsPerMetre(pixelsPerMetre);
		simpleMapRef.gpsTextPanel.setPixelsPerMetre(getPixelsPerMetre());
		/*
		 * to speed up map drawing, only perform certain actions
		 * if the map dimension have changed in any way. This is to 
		 * include most of the drawing of the base map, grid and ships 
		 * track (eventually). These will all be drawn onto a separate
		 * BufferedImage which will then be quickly drawn onto the main
		 * graphics device without having to go through all the 
		 * transformations.  
		 * In the future, it may also be extended to include a lot
		 * of the other data, but will leave that for now.  
		 */
		if (repaintBase ||
				lastHeight != getHeight() ||
				lastWidth != getWidth() ||
				lastMapRangeMetres != mapRangeMetres ||
				lastMapRotationDegrees != mapRotationDegrees ||
				lastMapCentreDegrees.equals(mapCentreDegrees) == false) {
			lastHeight = getHeight();
			lastWidth = getWidth();
			lastMapCentreDegrees = mapCentreDegrees.clone();
			lastMapRangeMetres = mapRangeMetres;
			lastMapRotationDegrees = mapRotationDegrees;
			pixelsPerMetre = (double) this.getHeight() / mapRangeMetres;
			// pixelsPerMetre = (double)this.getWidth()/mapRangeMetres;
			rectProj.setMapCentreDegrees(mapCentreDegrees);
			rectProj.setMapRangeMetres(mapRangeMetres);
			rectProj.setMapRotationDegrees(mapRotationDegrees);
			rectProj.setPanelHeight(this.getHeight());
			rectProj.setPanelWidth(this.getWidth());
			rectProj.setPixelsPerMetre(pixelsPerMetre);
			ship.setPixelsPerMetre(pixelsPerMetre);
			
			prepareBaseImage();
			
//			drawBase = true;
			
		}
		
//		long timeNow2 = PamCalendar.getTimeInMillis();
		
		Graphics2D g2d = (Graphics2D) g;
		
		myCompass.setMapRotationDegrees(mapRotationDegrees);
		myCompass.setPanelWidth(this.getWidth());

		Color currentColor;
		currentColor = PamColors.getInstance().getColor(PamColor.MAP);
		((Graphics2D) g).setColor(currentColor);
		((Graphics2D) g).fillRect(0, 0, this.getWidth(), this.getHeight());
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		grid.setPanelHeight(this.getHeight());
		grid.setPanelWidth(this.getWidth());
		grid.setMapRotation(mapRotationDegrees);
		grid.setMapRangeMetres(mapRangeMetres);

		g2d.drawImage(baseDrawing, baseXform, this);

		/*
		 * Draw the ship track
		 */
		if (currentArray.getArrayType() == PamArray.ARRAY_TYPE_TOWED) {
//			paintTrack(g); // draw it all
			// was drawn on the base map.
			paintPredictedGPS(g);
			paintShip(g);
		}
		paintDetectorData(g);

		if(mapController.mapParameters.showHydrophones){
			paintHydrophones(g);
		}
//		long timeTaken = PamCalendar.getTimeInMillis() - timeNow;
//		long timeTaken2 = PamCalendar.getTimeInMillis() - timeNow2;
//		if (drawBase) 
//			System.out.println("Draw map and base map in " + timeTaken + " ms, and " + timeTaken2 +" ms");
//		else
//			System.out.println("Draw map in " + timeTaken + " ms, and " + timeTaken2 +" ms");
		if (simpleMapRef.mouseDownPoint != null && simpleMapRef.mouseDragPoint != null && 
				mapController.getMouseMoveAction() == MapController.MOUSE_MEASURE) {
			g.setColor(PamColors.getInstance().getColor(PamColor.AXIS));
			g.drawLine(simpleMapRef.mouseDownPoint.x, simpleMapRef.mouseDownPoint.y, 
					simpleMapRef.mouseDragPoint.x, simpleMapRef.mouseDragPoint.y);
			measureSymbol.setFillColor(PamColors.getInstance().getColor(PamColor.AXIS));
			measureSymbol.setLineColor(measureSymbol.getFillColor());
			measureSymbol.draw(g, simpleMapRef.mouseDownPoint);
			measureSymbol.draw(g, simpleMapRef.mouseDragPoint);
		}
	}
	
	PamSymbol measureSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 5, 5, true, Color.RED, Color.RED);
	
	private void prepareBaseImage()
	{
//		System.out.println("Draw base map, image width = " + imageWidth);
		if (baseDrawing == null || baseDrawing.getWidth() != lastWidth ||
				baseDrawing.getHeight() != lastHeight) {
			imageWidth = getWidth();
			imageHeight = getHeight();
			baseDrawing = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		}
		Graphics2D g = (Graphics2D) baseDrawing.getGraphics();
		Color currentColor;
		currentColor = PamColors.getInstance().getColor(PamColor.MAP);
		g.setColor(currentColor);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (grid != null) {
			grid.setPanelHeight(this.getHeight());
			grid.setPanelWidth(this.getWidth());
			grid.setMapRotation(mapRotationDegrees);
			grid.setMapRangeMetres(mapRangeMetres);
		
					
			grid.drawGrid(g, rectProj);
			myCompass.drawCompass(g, rectProj);
			
		}
		paintContours(baseDrawing.getGraphics());

		myCompass.setPanelWidth(getWidth());
		myCompass.drawCompass((g), rectProj);
		
		/*
		 * Draw the track in the main paintComponent since it continually needs
		 * redrawing as it's uncovered by the ships body.
		 */
		PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
		if (currentArray.getArrayType() == PamArray.ARRAY_TYPE_TOWED) {
			paintTrack(g); // paint the entire track held in memory
//			paintShip(g);
		}
		repaintBase = false;
	}

	private boolean[] wantedContours = null;
	private Color[] contourColours = null;
	private int[] contourDepths = null;
	/**
	 * Need to make two lists, one is a list of depth numbers
	 * and colours, the other is a list of contours that 
	 * should actually be plotted. 
	 * @return true if there is anything at all to plot. 
	 */
	private boolean prepareContourData() {
	

		/*
		 * First work out what we WANT to plot
		 */
		MapFileManager mapManager = mapController.mapFileManager;
		Vector<java.lang.Integer> availableContours = mapManager.getAvailableContours();
		if (availableContours == null || availableContours.size() == 0) {
			return false;
		}
		int nPossibles = Math.min(availableContours.size(), 
				mapController.mapParameters.mapContours.length);
		if (nPossibles == 0) {
			return false;
		}
		wantedContours = new boolean[nPossibles];
		contourColours = new Color[nPossibles];
		contourDepths = new int[nPossibles];
		int nUsed = 0;
		for (int i = 0; i < nPossibles; i++) {
			if (mapController.mapParameters.mapContours[i]) {
				nUsed++;
			}
		}
		int iUsed = 0;
		for (int i = 0; i < nPossibles; i++) {
			wantedContours[i] = mapController.mapParameters.mapContours[i];
			contourDepths[i] = availableContours.get(i);
			if (wantedContours[i]) {
				contourColours[i] = getContourColour(iUsed++, nUsed);
			}
		}
		if (nUsed == 0) {
			return false;
		}
		return true;
		/* 
		 * We now know how many there are, so can work out the colours
		 * putting null in for contours that aren't used. 
		 */
//		for (int i = 0; i < nUsed; i++) {
//			contourColours[i] = getContourColour(i, nUsed);
//		}
//		
//		int nColours = availableContours.size();
//		int nContours = mapManager.getContourCount(); // count of contour objects
//		contourValues = null;
////		contourValues = null;
//		int nUsedContours = 0;
//		if (nColours == 0) {
//			return false;
//		}
////		if (nColours > 0) {
////			contourColours = new Color[nColours];
////			for (int i = 0; i < nColours; i++) {
////				contourColours[i] = getContourColour(i, nColours);
////			}
////		}
//		boolean[] shouldPlot = mapController.mapParameters.mapContours;
//		if (shouldPlot == null || shouldPlot.length < nContours) {
//			mapController.mapParameters.mapContours = shouldPlot = 
//				new boolean[nContours];
//			shouldPlot[0] = true;
//		}
//		nUsedContours = 0;
//		for (int i = 0; i < nContours; i++) {
//			if (shouldPlot[i]) {
//				nUsedContours = 0;
//			}
//		}
////		contourColours = new Color[nUsedContours];
////		usedContours = new MapContour[nUsedContours];
////		int iCont = 0;
////		for (int i = 0; i < nContours; i++) {
////			if (shouldPlot[i]) {
////				contourColours[iCont] = getContourColour(iCont, nUsedContours);
////				usedContours[iCont] = mapManager.getMapContour(iCont);
////				iCont++;
////			}
////		}
//		return true;
	}
	private void paintContours(Graphics g) {
		if (prepareContourData() == false) {
			return;
		}
		MapFileManager mapManager = mapController.mapFileManager;
		MapContour mapContour;
		int ci;
		int nSegments = mapManager.getContourCount();
		for (int i = 0; i < nSegments; i++) {
			mapContour = mapManager.getMapContour(i);
			ci = mapManager.getContourIndex(mapContour.getDepth());
			if (ci < 0) continue;
			if (ci >= contourColours.length) continue;
			if (contourColours[ci] == null) continue;
			g.setColor(contourColours[ci]);
			paintContour(g, mapContour);
		}
//		g.setColor(Color.WHITE);
//		MapContour mapContour;
//		int ci;
//		for (int i = 0; i < nContours; i++) {
//			mapContour = mapManager.getMapContour(i);
//			ci = mapManager.getContourIndex(mapContour.getDepth());
//			if (ci < 0) continue;
//			if (shouldPlot[ci]) {
//				if (contourColours != null) {
//					g.setColor(contourColours[ci]);
//				}
//				paintContour(g, mapContour);
//			}
////		}
//		if (prepareContourData()) {
//			for (int i = 0; i < usedContours.length; i++) {
//
//				if (contourColours != null) {
//					g.setColor(contourColours[i]);
//				}
//				paintContour(g, usedContours[i]);
//			}
//		}
	}

	private Color getContourColour(int listPos, int nContours) {
//		if (listPos == 0) return coastColour;
//		double f = (double) listPos / (double) nContours;
//		double f1 = 1-f;
//		return new Color((int)(shallowColour.getRed()*f1 + deepestColour.getRed()*f),
//				(int)(shallowColour.getGreen()*f1 + deepestColour.getGreen()*f),
//				(int)(shallowColour.getBlue()*f1+ deepestColour.getBlue()*f));
		if (listPos == 0) {
			return Color.BLACK;
		}
		if (listPos >= nContours) {
			return Color.WHITE;
		}
		ColourArray colourArray = ColourArray.createMergedArray(nContours-1, Color.GREEN, Color.BLUE);
		return colourArray.getColours()[listPos-1];
	}

	private void paintContour(Graphics g, MapContour mapContour) {
		Graphics2D g2d = (Graphics2D) g;
		Point p1, p2;
		p1 = new Point();
		p2 = new Point();
		Rectangle rect = new Rectangle(getWidth(), getHeight());
		Coordinate3d c1, c2;
		Vector<LatLong>  latLongs = mapContour.getLatLongs();
		if (latLongs == null || latLongs.size() < 2) return;
		LatLong latLong = latLongs.get(0);
		c1 = rectProj.getCoord3d(latLong.getLatitude(), latLong.getLongitude(), 0);
		for (int i = 1; i < latLongs.size(); i++) {
			latLong = latLongs.get(i);
			c2 = rectProj.getCoord3d(latLong.getLatitude(), latLong.getLongitude(), 0);
			p1.x = (int) c1.x;
			p1.y = (int) c1.y;
			p2.x = (int) c2.x;
			p2.y = (int) c2.y;
			if ((rect.contains(p1) || rect.contains(p2)) && (p1.x != p2.x || p1.y != p2.y)) {
				g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			c1 = c2;
		}
	}
	private void paintTrack(Graphics g) {
		paintTrack(g, null);
	}
	
	private GpsDataUnit lastDrawGpsDataUnit;
	private Coordinate3d lastGpsCoodinate;
	
	long getMapStartTime() {
		switch (PamController.getInstance().getRunMode()) {
		case PamController.RUN_NORMAL:
		case PamController.RUN_MIXEDMODE:
			return PamCalendar.getTimeInMillis() - mapController.mapParameters.trackShowTime * 1000;
		case PamController.RUN_PAMVIEW:
			return PamCalendar.getSessionStartTime();				
		}
		return PamCalendar.getTimeInMillis();
	}
	private void paintTrack(Graphics g, PamDataUnit lastDrawnUnit) {

		long mapStartTime = getMapStartTime();
		g.setColor(Color.WHITE);
		PamDataBlock<GpsDataUnit> gpsDataBlock = PamModel.getPamModel().getGpsDataBlock();
		long maxInterpTime = mapController.getMaxInterpolationTime() * 1000;
		long lastFixTime = 0, thisFixTime;
		if (gpsDataBlock != null && gpsDataBlock.getUnitsCount() > 0) {
			GpsData gpsData;
			Coordinate3d c1, c2 = null;
			GpsDataUnit dataUnit;
			synchronized (gpsDataBlock) {
				ListIterator<GpsDataUnit> gpsIterator = gpsDataBlock.getListIterator(0);
				if (gpsIterator.hasNext()) {
					dataUnit = gpsIterator.next();
					gpsData = dataUnit.getGpsData();
					lastFixTime = dataUnit.getTimeMilliseconds();
					c1 = rectProj.getCoord3d(gpsData.getLatitude(), gpsData
							.getLongitude(), 0.);
					while (gpsIterator.hasNext()) {
						dataUnit = gpsIterator.next();
						gpsData =  dataUnit.getGpsData();
						if (gpsData.isDataOk() == false) {
							continue;
						}
						c2 = rectProj.getCoord3d(gpsData.getLatitude(), gpsData
								.getLongitude(), 0.);
						thisFixTime = dataUnit.getTimeMilliseconds();
						if (lastDrawnUnit == null || dataUnit.getTimeMilliseconds() <= lastDrawnUnit.getTimeMilliseconds()) {
							if (dataUnit.getTimeMilliseconds() >= mapStartTime && 
									thisFixTime - lastFixTime < maxInterpTime) {
								g.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
							}
						}
						lastFixTime = thisFixTime;
						c1.x = c2.x;
						c1.y = c2.y;
						lastDrawGpsDataUnit = dataUnit;
					}
					lastGpsCoodinate = c2;
					// and finally, draw to the predicted ship position ...
//					if (PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW) {
//						if (gpsData != null) {
//							gpsData = gpsData.getPredictedGPSData(PamCalendar.getTimeInMillis());
//							c2 = rectProj.getCoord3d(gpsData.getLatitude(), gpsData
//									.getLongitude(), 0.);
//							g.drawLine((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
//						}
//					}
				}
			}
		}
	}
	private void paintPredictedGPS(Graphics g) {

		if (lastDrawGpsDataUnit == null || lastGpsCoodinate == null) {
			return;
		}
		GpsData gpsData = lastDrawGpsDataUnit.getGpsData();
		Coordinate3d c2;
		g.setColor(Color.WHITE);
		// and finally, draw to the predicted ship position ...
		if (PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW) {
			if (gpsData != null) {
				gpsData = gpsData.getPredictedGPSData(PamCalendar.getTimeInMillis());
				c2 = rectProj.getCoord3d(gpsData.getLatitude(), gpsData
						.getLongitude(), 0.);
				g.drawLine((int) lastGpsCoodinate.x, (int) lastGpsCoodinate.y, (int) c2.x, (int) c2.y);
			}
		}
	}
	/**
	 * Paint the new GPS data onto the base image, starting 
	 * from the previous coordinate. 
	 * @param newGpsDataUnit new Gps data
	 */
	private void paintNewGPSData(GpsDataUnit newGpsDataUnit) {
		if (baseDrawing == null) {
			return;
		}
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			return;
		}
		paintNewGPSData(baseDrawing.getGraphics(), newGpsDataUnit);
	}
	/**
	 * Pint new gps data on the given graphics handle
	 * @param g graphics handle
	 * @param newGpsDataUnit gps data unit. 
	 */
	private void paintNewGPSData(Graphics g, GpsDataUnit newGpsDataUnit) {

		Coordinate3d c2;
		g.setColor(Color.WHITE);
		GpsData gpsData = newGpsDataUnit.getGpsData();
		c2 = rectProj.getCoord3d(gpsData.getLatitude(), gpsData
				.getLongitude(), 0.);
		if (lastGpsCoodinate != null) {
			g.drawLine((int) lastGpsCoodinate.x, (int) lastGpsCoodinate.y, (int) c2.x, (int) c2.y);
		}
		lastDrawGpsDataUnit = newGpsDataUnit;
		lastGpsCoodinate = c2;
		
	}
	private void paintShip(Graphics g) {
		// find the GPS controller - need to get the ships dimensions out
		// of it.
		PamControllerInterface pc = PamController.getInstance();
		GPSControl gpsControl = (GPSControl) pc.findControlledUnit("GPS Acquisition");
		GPSParameters gpsParameters = null;
		if (gpsControl != null) {
			gpsParameters = gpsControl.getGpsParameters();
			ship.setVesselDimension(gpsParameters.dimA, gpsParameters.dimB, 
					gpsParameters.dimC, gpsParameters.dimD);
			ship.setPredictionArrow(gpsParameters.plotPredictedPosition ? 
					gpsParameters.predictionTime : 0);
		}
		
		ship.drawShip(((Graphics2D) g), rectProj);
	}
	
	private void paintHydrophones(Graphics g) {
		 
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		int phoneCount = array.getHydrophoneCount();
		Hydrophone h;
		LatLong latLong;
		long t = PamCalendar.getTimeInMillis();
		Coordinate3d c3d;
		PamSymbol symbol;
		for (int i = 0; i < phoneCount; i++) {
			h = array.getHydrophone(i);
			latLong = array.getHydrophoneLocator().getPhoneLatLong(t, i);
			if (latLong == null) continue;
			c3d = rectProj.getCoord3d(latLong.getLatitude(), latLong.getLongitude(), 0);
			symbol = h.getSymbol();
			symbol.draw(g, c3d.getXYPoint());
		}
	}

	
	private void paintDetectorData(Graphics g) {
		g.setColor(Color.BLACK);
		long now = PamCalendar.getTimeInMillis();
		long mapStartTime = now - mapController.mapParameters.dataShowTime * 1000;
		long entTime = Long.MAX_VALUE;
		MapDetectionData mapDetectionData;
		PamDataBlock dataBlock;
		PamDataUnit dataUnit;
		String tempDataUnitId;
		ArrayList<PamDataBlock> detectorDataBlocks = mapController.mapDetectionsManager.plottableBlocks;
		ListIterator<PamDataUnit> duIterator;
		if (detectorDataBlocks != null) {
			for (int m=0;m<detectorDataBlocks.size();m++){
				dataBlock = detectorDataBlocks.get(m);
				try {
					synchronized (dataBlock) {
						mapDetectionData = mapController.mapDetectionsManager.findDetectionData(dataBlock);
						tempDataUnitId = detectorDataBlocks.get(m).getDataName() + ", " 
						+ detectorDataBlocks.get(m).getParentProcess().getProcessName();
						if (mapDetectionData.shouldPlot) {
							duIterator = dataBlock.getListIterator(0);
							while (duIterator.hasNext()) {
								dataUnit = duIterator.next();
								if (shouldPlot(dataUnit, mapDetectionData, now) == false) {
									continue;
								}
								//						if (dataUnit.getTimeMilliseconds() < mapStartTime)
								//							continue;
								//						dataBlock.addObserver(this);
								dataBlock.drawDataUnit(g, dataUnit, this.rectProj);
							}
						}
					}
				}
				catch (ConcurrentModificationException ex) {
					System.out.println("Concurrency problem in " + dataBlock.getDataName());
					ex.printStackTrace();
				}
			}
		}
	}


//	Timer t = new Timer(100, new ActionListener() {
//		public void actionPerformed(ActionEvent evt) {
//			repaint();
//		}
//	});

	/**
	 * Instruct map to redraw it's base image next time 
	 * anything is redrawn. 
	 */
	public void repaintBaseDrawing() {
		repaintBase = true;
	}
	
//	@Override
	public void repaint(boolean baseToo) {
		// sets a flag to make sure the base map gets repainted too. 
		repaintBase |= baseToo;
		super.repaint();

	}

	public double getMapRotationDegrees() {
		return mapRotationDegrees;
	}

	public void setMapRotationDegrees(double mapRotationDegrees) {
		this.mapRotationDegrees = mapRotationDegrees;
	}

	public LatLong getMapCentreDegrees() {
		return mapCentreDegrees;
	}

	public void setMapCentreDegrees(LatLong mapCentreDegrees) {

		this.mapCentreDegrees = mapCentreDegrees;
		/*
		 * if (mapCentreDegrees.longDegs<0.0){ //System.out.println("MapPanel
		 * Setter: long is negative"); System.exit(0); }
		 */
	}

	public void setMapCentreCoords(Coordinate3d c) {
		this.mapCentreDegrees = rectProj.panel2LL(c);
	}

	public int getMapRangeMetres() {
		return mapRangeMetres;
	}

	public void setMapRangeMetres(int mapRangeMetres) {
		this.mapRangeMetres = mapRangeMetres;
		if (simpleMapRef != null) {
			simpleMapRef.gpsTextPanel.displayZoomedorRotated();
		}
	}

	public LatLong getShipLLD() {
		return ship.getShipLLD();
	}
	
//	public GpsData getShipGpsData() {
//		return ship.getShipGps();
//	}
	public GpsData getShipGpsData(boolean predict) {
		return ship.getShipGps(predict);
	}

	public void newShipLLD() {
		LatLong shipLLD = ship.getShipLLD();
		if (mapController.mapParameters.keepShipOnMap) {
			Coordinate3d sc = rectProj.getCoord3d(shipLLD.getLatitude(), shipLLD.getLongitude(), 0);
			if (sc.x <= 0 || sc.x >= getWidth()) {
				mapCentreDegrees.setLongitude((shipLLD.getLongitude() + mapCentreDegrees.getLongitude())/2.);
				repaint();
			}
			if (sc.y <= 0 || sc.y >= getHeight()) {
				mapCentreDegrees.setLatitude((shipLLD.getLatitude() + mapCentreDegrees.getLatitude())/2.);
				repaint();
			}
		}
		if (simpleMapRef != null) {
			simpleMapRef.gpsTextPanel.displayZoomedorRotated();
		}
	}

	public MapRectProjector getRectProj() {
		return rectProj;
	}

	public void setRectProj(MapRectProjector rectProj) {
		this.rectProj = rectProj;
	}

	public void update(PamObservable o, PamDataUnit arg) {

//		PamDataBlock block = (PamDataBlock) o;
		repaint(250);
	}
	
	public String getObserverName() {
		return "Map Panel";
	}

	@Override
//	public void paint(Graphics g) {
//		if (getKeyPanel() != null) {
//			synchronized (getKeyPanel()) {
//				super.paint(g);
//			}
//		}
//		else {
//			super.paint(g);
//		}
//	}


	public void setSampleRate(float sampleRate, boolean notify) {
	}
	
	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub
		
	}


	public double getPixelsPerMetre() {
		return pixelsPerMetre;
	}

	public void setPixelsPerMetre(double pixelsPerMetre) {
		this.pixelsPerMetre = pixelsPerMetre;
	}



	class DetectorPopupListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
				simpleMapRef.createMapComment(e.getX(), e.getY());
			}
		}

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
				plotDetectorMenu().show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}


	protected void updateObservers(){

		ArrayList<PamDataBlock> detectorDataBlocks = mapController.mapDetectionsManager.plottableBlocks;
		for (int i = 0; i < detectorDataBlocks.size(); i++) {
			detectorDataBlocks.get(i).addObserver(this);
		}
	}
	
	protected void createKey() {
		if (PamController.getInstance().isInitializationComplete() == false) {
			return;
		}
		if (mapController.mapParameters.showKey == false) {
//			keyPanel.getPanel().setVisible(false);
			this.setKeyPanel(null);
			return;
		}
		
		KeyPanel keyPanel = new KeyPanel("Key", PamKeyItem.KEY_SHORT);
		

//		Component keyComponent;
//		String keyText;
		ArrayList<PamDataBlock> detectorDataBlocks = mapController.mapDetectionsManager.plottableBlocks;
		int nUsed = 0;
		if (detectorDataBlocks != null) {
			PamDataBlock dataBlock;
			for(int m=0;m<detectorDataBlocks.size();m++) {
				dataBlock = detectorDataBlocks.get(m);
//				tempDataUnitId = detectorDataBlocks.get(m).getDataName() + ", " 
//				+ detectorDataBlocks.get(m).getParentProcess().getProcessName();
				if (dataBlock.canDraw(rectProj) == false) continue;
				if (shouldPlot(dataBlock)){	
					keyPanel.add(detectorDataBlocks.get(m).createKeyItem(rectProj, PamKeyItem.KEY_SHORT));
					nUsed ++;
				}
			}
		}
		// then add the coast and the colours.
		if (prepareContourData()) {

//			private boolean[] wantedContours = null;
//			private Color[] contourColours = null;
//			private int[] contourDepths = null;
			for (int i = 0; i < wantedContours.length; i++) {
				if (wantedContours[i] == false) {
					continue;
				}
				keyPanel.add(createContourKeyItem(contourDepths[i], contourColours[i]));
				nUsed++;
			}
		}
		
		this.setKeyPanel(keyPanel);
		keyPanel.getPanel().setVisible(nUsed > 0);
	}
	
	private PamKeyItem createContourKeyItem(int depth, Color colour) {
		String str;
		if (depth == 0) {
			str = " Coast";
		}
		else {
			str = String.format(" %d m Contour", depth);
		}
//		color
		PamSymbol symbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 2, 20, true, colour, colour);
		symbol.setLineThickness(3);
		symbol.setIconStyle(PamSymbol.ICON_STYLE_LINE);
		return new BasicKeyItem(symbol, str);
	}
	

//
//	public void refreshPlotableDetectorLists(){
//		updateObservers();
//		detectorDataBlocks = PamController.getInstance().getDataBlocks(PamDataUnit.class, true);
//		if(mapController.mapParameters.plotableBlockIdList==null){
//			mapController.mapParameters.plotableBlockIdList = new ArrayList<String>();
//		}
//		if(mapController.mapParameters.plottedBlockIds==null){
//			mapController.mapParameters.plottedBlockIds = new ArrayList<String>();
//		}
//		ArrayList<String> dataBlockIdList = new ArrayList<String>();
//		for(int i = 0;i<detectorDataBlocks.size();i++){
//			if (detectorDataBlocks.get(i).canDraw(rectProj) == false) continue;
//			String dataBlockId = detectorDataBlocks.get(i).getDataName() + ", " 
//			+ detectorDataBlocks.get(i).getParentProcess().getProcessName();
//			dataBlockIdList.add(dataBlockId);
//			if(mapController.mapParameters.plotableBlockIdList.indexOf(dataBlockId)==-1){
//				//Add the ID to the list
//				mapController.mapParameters.plotableBlockIdList.add(dataBlockId);
//			}
//		}
//		for (int i = 0; i<mapController.mapParameters.plotableBlockIdList.size();i++){
//			if(dataBlockIdList.indexOf(mapController.mapParameters.plotableBlockIdList.get(i))==-1){
//				//Remove the ID from the list
//				mapController.mapParameters.plotableBlockIdList.set(i, "");
//			}
//		}
//		while(mapController.mapParameters.plotableBlockIdList.contains("")){
//			mapController.mapParameters.plotableBlockIdList.
//			remove(mapController.mapParameters.plotableBlockIdList.indexOf(""));
//		}
//	}

	JPopupMenu plotDetectorMenu(){

//		refreshPlotableDetectorLists();
		plotDetectorMenu = new JPopupMenu();
//		DisplaySelection displaySelection = new DisplaySelection();
		ManagedDisplaySelection managedDisplaySelection = new ManagedDisplaySelection();
		JCheckBoxMenuItem checkMenuItem;

//		for(int i = 0;i<mapController.mapParameters.plotableBlockIdList.size();i++){
//			checkMenuItem = new JCheckBoxMenuItem(mapController.mapParameters.plotableBlockIdList.get(i));
//			checkMenuItem.addActionListener(displaySelection);
//			int plotedBlockIdIndex = mapController.mapParameters.plottedBlockIds.indexOf(mapController.mapParameters.plotableBlockIdList.get(i));
//			if(plotedBlockIdIndex>-1){
//				checkMenuItem.setSelected(true);
//			}else{
//				checkMenuItem.setSelected(false);
//			}
//			plotDetectorMenu.add(checkMenuItem);
//		}
//		
//		plotDetectorMenu.addSeparator();
		JMenuItem menuItem;
		menuItem = new JMenuItem("Plot overlay options...");
		menuItem.addActionListener(new OverlayOptions());
		plotDetectorMenu.add(menuItem);
		plotDetectorMenu.addSeparator();
		
		ArrayList<MapDetectionData> mddList = mapController.mapDetectionsManager.getMapDetectionDatas();
		for (int i = 0; i < mddList.size(); i++) {
			if (mddList.get(i).dataBlock == null) continue;
			checkMenuItem = new JCheckBoxMenuItem(mddList.get(i).dataName);
			checkMenuItem.setSelected(mddList.get(i).shouldPlot);
			checkMenuItem.addActionListener(managedDisplaySelection);
			plotDetectorMenu.add(checkMenuItem);
		}
		
		plotDetectorMenu.addSeparator();
		plotDetectorMenu.add(showKeyMenu = new JCheckBoxMenuItem("Show Key", mapController.mapParameters.showKey));
		showKeyMenu.addActionListener(new ShowKey());
		plotDetectorMenu.add(panZoomMenu = new JCheckBoxMenuItem("Show pan zoom controls", mapController.mapParameters.showPanZoom));
		panZoomMenu.addActionListener(new ShowPanZoom());
		plotDetectorMenu.add(showGpsMenu = new JCheckBoxMenuItem("Show Gps and Cursor data", mapController.mapParameters.showGpsData));
		showGpsMenu.addActionListener(new ShowGpsMenu());
//		plotDetectorMenu.addMouseListener(detectorPopupListener); // why was this here ??????
		plotDetectorMenu.addSeparator();
		plotDetectorMenu.add(simpleMapRef.clipboardCopier.getCopyMenuItem("Copy map to clipboard"));
		plotDetectorMenu.add(simpleMapRef.clipboardCopier.getPrintMenuItem("Print map ..."));
		
		return plotDetectorMenu;
	}
	JCheckBoxMenuItem panZoomMenu, showGpsMenu, showKeyMenu;
	class ShowKey  implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			mapController.mapParameters.showKey = showKeyMenu.isSelected();
			createKey();
		}
	}
	class ShowPanZoom  implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			mapController.mapParameters.showPanZoom = panZoomMenu.isSelected();
			simpleMapRef.showMapObjects();
		}
	}
	class ShowGpsMenu  implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			mapController.mapParameters.showGpsData = showGpsMenu.isSelected();
			simpleMapRef.showMapObjects();
		}
	}

	class ManagedDisplaySelection implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
			mapController.mapDetectionsManager.setShouldPlot(menuItem.getText(), menuItem.isSelected());
			mapController.checkViewerData();
			createKey();
		}
	}
	
	class OverlayOptions implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			MapDetectionsParameters newParams = MapDetectionsDialog.showDialog(mapController.getPamView().getGuiFrame(), 
					mapController.mapDetectionsManager);
			if (newParams != null) {
				mapController.mapDetectionsManager.setMapDetectionsParameters(newParams);
				createKey();
			}
			
		}
		
	}

//	class DisplaySelection implements ActionListener {
//		public void actionPerformed(ActionEvent e) {
//			JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
//			if(menuItem.getState()){
//				mapController.mapParameters.plottedBlockIds.add(menuItem.getText());
//			}else{
//				mapController.mapParameters.plottedBlockIds.remove(menuItem.getText());	
//			}
//			createKey();
//		}
//	}
	
	private boolean shouldPlot(PamDataBlock pamDataBlock) {
		return mapController.mapDetectionsManager.isShouldPlot(pamDataBlock);
	}
	
	private boolean shouldPlot(PamDataUnit pamDataUnit, MapDetectionData mapDetectionData, long now) {
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			PamScrollSlider viewerSlider = simpleMapRef.getViewerScroller();
			if (pamDataUnit.getTimeMilliseconds() < viewerSlider.getMinimumMillis() ||
					pamDataUnit.getTimeMilliseconds() > viewerSlider.getMaximumMillis()) {
				return false;
			}
			return true;
		}
		if (mapDetectionData.shouldPlot == false) {
			return false;
		}
		if (mapDetectionData.allAvailable) {
			return true;
		}
		if (now - pamDataUnit.getTimeMilliseconds() > mapDetectionData.displaySeconds * 1000) {
			return false;
		}
		return true;
	}

	class SettingsAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// spectrogramDisplay.SetSettings();
		}
	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		MapDetectionData mdd = mapController.mapDetectionsManager.findDetectionData((PamDataBlock) o);
		if (mdd != null) {
			return mdd.displaySeconds * 1000;
		}
		else {
			return Math.max(mapController.mapParameters.dataKeepTime, mapController.mapParameters.dataShowTime) * 1000;
		}
	}

	public void noteNewSettings() {
	}

	public void setMapZoom(float zoomFactor) {
		this.setMapRangeMetres((int) (this.mapRangeMetres*zoomFactor));
	}
	

	public void removeObservable(PamObservable o) {
		
	}

	public SimpleMap getSimpleMapRef() {
		return simpleMapRef;
	}

	public void setSimpleMapRef(SimpleMap simpleMapRef) {
		this.simpleMapRef = simpleMapRef;
	}


	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		repaint(true);
	}

	/**
	 * @param mouseAdapter
//	 */
//	public void addMouseAdapter(MouseAdapter mouseAdapter) {
//		this.addMouseAdapter(mouseAdapter);
//	}

}
