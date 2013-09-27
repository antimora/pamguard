package PamView;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import pamMaths.PamVector;
import Acquisition.AcquisitionProcess;
import Array.ArrayManager;
import Array.HydrophoneLocator;
import Array.PamArray;
import PamDetection.AbstractLocalisation;
import PamDetection.PamDetection;
import PamUtils.Coordinate3d;
import PamUtils.FrequencyFormat;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamView.GeneralProjector.ParameterType;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class PamDetectionOverlayGraphics implements PanelOverlayDraw, ManagedSymbol {

	private PamDataBlock<PamDataUnit> parentDataBlock;
	
	private PamSymbol pamSymbol;
	
	private static final int DEFSYMBOLSIZE = 8;
	
	protected Color lineColour = Color.BLACK;
	
	private boolean drawLineToLocations = true;
	
	/**
	 * Default range for detections with bearing and no range information in metres. 
	 */
	private double defaultRange = 1000;
	
	private boolean isDetectionData;
	
	/**
	 * Constructor for standard overlay graphics class. Requires parent data block 
	 * as a parameter. 
	 * 
	 * @param parentDataBlock
	 */
	public PamDetectionOverlayGraphics(PamDataBlock parentDataBlock) {
		super();
		this.parentDataBlock = parentDataBlock;
//		pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, DEFSYMBOLSIZE, DEFSYMBOLSIZE, true, lineColour, lineColour);
		/*
		 * 
		 * work out if the base class is based on PamDetection or not - if not, then it's 
		 * not going to have localisation information available, but it will still have a time and a 
		 * symbol, so could be used for drawing on the map and possibly some other displays. 
		 * 
		 */
		Class unitClass = parentDataBlock.getUnitClass();
		isDetectionData = PamDetection.class.isAssignableFrom(unitClass);
		
		PamSymbolManager.getInstance().addManagesSymbol(this);
		
		/*
		 * Then check there is a symbol loaded. If not, create one now
		 */
		if (pamSymbol == null) {
			pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, DEFSYMBOLSIZE, 
					DEFSYMBOLSIZE, false, Color.BLACK, Color.BLACK);
		}
		
	}

	/**
	 * 
	 * @return parent PamDataBlock for this PanelOverlayDraw. 
	 */
	public PamDataBlock<PamDataUnit> getParentDataBlock() {
		return parentDataBlock;
	}

	/**
	 * Used to tell the various displays whether or not the data in the
	 * parentDataBlock can be drawn on each of those displays. This is 
	 * based purely on the axis types of the displays and whether or not
	 * the parentDataBlock's data untits are likely to have data which 
	 * can transform into those axis types. 
	 * <p>
	 * For simplicity I've broken it up into the three main display types 
	 * currently existing in Pamguard. 
	 */
	public boolean canDraw(GeneralProjector generalProjector) {

		/*
		 * Go through the different combinations of parameters and return true
		 * if we think it's likely we'll be able to draw on them based on information 
		 * in the pamDataBlock.
		 * 
		 * If new displays are added that support graphic overlay, then this list of if, else if's
		 * will need to be expanded. 
		 * 
		 */
		if (generalProjector.getParmeterType(0) == ParameterType.LONGITUDE
				&& generalProjector.getParmeterType(1) == ParameterType.LATITUDE) {
			return canDrawOnMap();
		}
		else if (generalProjector.getParmeterType(0) == ParameterType.TIME
				&& generalProjector.getParmeterType(1) == ParameterType.FREQUENCY) {
			return canDrawOnSpectrogram();
		}
		else if (generalProjector.getParmeterType(0) == ParameterType.BEARING &&
				(generalProjector.getParmeterType(1) == ParameterType.AMPLITUDE ||
				generalProjector.getParmeterType(1) == ParameterType.RANGE ||
				generalProjector.getParmeterType(1) == ParameterType.SLANTANGLE)) {
			return canDrawOnRadar(generalProjector.getParmeterType(1));
		}
		
		return false;
	}
	
	/**
	 * 
	 * @return true if these data can be drawn on the map. 
	 * <p> Thsi shoudl always be true, since it's always possible
	 * to draw a symbol at the hydrophone location even if no range
	 * or bearing information are available. 
	 */
	protected boolean canDrawOnMap() {
		return true;
//		return (parentDataBlock.getLocalisationContents() != 0);
	}
	
	/**
	 * 
	 * @return true if these data can be drawn on the spectrogram. 
	 * <p> Generally, this is always true since it will just draw a 
	 * rectangle with the time and frequency limits of the sound. If 
	 * it is not the case, then override this function and return false. 
	 */
	protected boolean canDrawOnSpectrogram() {
		return true;
	}
	
	/**
	 * 
	 * @param radialParameter
	 * @return true if these data can be drawn on the radar. The detection will always need
	 * a bearing. The radial parameter is
	 * either amplitude (which all detections should have) or range which may or may not be there. 
	 */
	protected boolean canDrawOnRadar(GeneralProjector.ParameterType radialParameter) {
				
		if ((parentDataBlock.getLocalisationContents() & AbstractLocalisation.HAS_BEARING) == 0) {
			return false;
		}
		
		if (radialParameter == ParameterType.AMPLITUDE) {
			return isDetectionData;
		}
		else if (radialParameter == ParameterType.RANGE) {
			return ((parentDataBlock.getLocalisationContents() & AbstractLocalisation.HAS_RANGE) != 0);
		}
		else if (radialParameter == ParameterType.SLANTANGLE) {
			return isDetectionData;
		}
		return false;
	}

	/**
	 * Gets information for making up a key on various displays. 
	 * PamKeyItem is not yet implemented.
	 */
	public PamKeyItem createKeyItem(GeneralProjector generalProjector, int keyType) {
		return new PanelOverlayKeyItem(this);
	}

	/**
	 * Draw a PamDataUnit on a display. <p>
	 * This is split into separate routines for the three main display types for simplicity both 
	 * of reading this code and for overriding the various functions. <p>
	 * If display types are added to PAMGUARD, these functions will need to be added to. 
	 */
	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		
		if (canDraw(generalProjector) == false) return null;
		if (pamSymbol != null) {
			g.setColor(pamSymbol.getLineColor());
		}
		else {
			g.setColor(lineColour);
		}
		
		if (generalProjector.getParmeterType(0) == ParameterType.LONGITUDE
				&& generalProjector.getParmeterType(1) == ParameterType.LATITUDE) {
			return drawOnMap(g, pamDataUnit, generalProjector);
		}
		else if (generalProjector.getParmeterType(0) == ParameterType.TIME
				&& generalProjector.getParmeterType(1) == ParameterType.FREQUENCY) {
			return drawOnSpectrogram(g, pamDataUnit, generalProjector);
		}
		else if (generalProjector.getParmeterType(0) == ParameterType.BEARING &&
				generalProjector.getParmeterType(1) == ParameterType.AMPLITUDE) {
			return drawAmplitudeOnRadar(g, pamDataUnit, generalProjector);
		}
		else if (generalProjector.getParmeterType(0) == ParameterType.BEARING &&
				generalProjector.getParmeterType(1) == ParameterType.RANGE) {
			return drawRangeOnRadar(g, pamDataUnit, generalProjector);
		}
		else if (generalProjector.getParmeterType(0) == ParameterType.BEARING &&
				generalProjector.getParmeterType(1) == ParameterType.SLANTANGLE) {
			return drawSlantOnRadar(g, pamDataUnit, generalProjector);
		}
		return null;
	}
	
	protected Rectangle drawOnMap(Graphics g, PamDataUnit pamDetection, GeneralProjector generalProjector) {
		/*
		 * 
		 * four possibilities here.
		 * 1) No localisation information so just draw the symbol at the hydrophone location
		 * 2) Bearing only - draw bearing lines of default range.
		 * 3) Range only - draw symbol and a circle around it
		 * 4) Bearing and Range - draw lines to symbol at correct location. 
		 * 5) LatLong - supersedes range and bearing
		 */
		
		// all need to start off by finding the position of the hydrophones at moment of
		// detection. This may be in the localisation info, if not, get it from the detection channels. 
		AbstractLocalisation localisation = pamDetection.getLocalisation();
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		if (array == null) return null;
		HydrophoneLocator hydrophoneLocator = array.getHydrophoneLocator();
		if (hydrophoneLocator == null) return null;
		
//		LatLong centreLatLong = getDetectionCentre(pamDetection);
		LatLong centreLatLong = pamDetection.getOriginLatLong(false);
		if (centreLatLong == null) {
			return null;
		}
		
		Coordinate3d detectionCentre = generalProjector.getCoord3d(centreLatLong.getLatitude(), centreLatLong.getLongitude(), 0);
		
		if (localisation == null || 
				((localisation.getLocContents() & 
						(AbstractLocalisation.HAS_BEARING | AbstractLocalisation.HAS_RANGE)) == 0)) {
			// no localisation information, so draw the symbol and return.
			generalProjector.addHoverData(detectionCentre, pamDetection);
			return getPamSymbol(pamDetection).draw(g, detectionCentre.getXYPoint());
		}
		// here we know we have localisation information with range and or bearing. 
		double range, bearing, depth, referenceBearing;
		LatLong endLatLong, errLatLong1, errLatLong2;
		referenceBearing = 0;
		depth = 0;
		range = 0;
		bearing = 0;
		Coordinate3d endPoint;
		
		Rectangle bounds = null;
		Rectangle rr;
		double perpError, parallelError;
		Coordinate3d errEndPoint1, errEndPoint2;
		if ((localisation.getLocContents() & 
				AbstractLocalisation.HAS_LATLONG) != 0) {
			for (int i = 0; i < localisation.getNumLatLong(); i++) {
				endLatLong = localisation.getLatLong(i);
				if (endLatLong == null) continue;
				if (Double.isNaN(endLatLong.getLatitude())) {
					continue;
				}
				endPoint = generalProjector.getCoord3d(endLatLong.getLatitude(), endLatLong.getLongitude(), 0);
				if ((localisation.getLocContents() & 
						AbstractLocalisation.HAS_DEPTH) != 0) {
					depth = localisation.getDepth(i);
					String depth_str = String.format("%.0f m depth", localisation.getDepth(i));
					((Graphics2D) g).setFont(new Font("Arial", Font.PLAIN, 12));
					((Graphics2D) g).setColor(new Color(120,120,200));
					
					((Graphics2D) g).drawString(depth_str ,(int)endPoint.x+10, (int)endPoint.y);
					
				}
				rr = drawLineAndSymbol(g, pamDetection, detectionCentre.getXYPoint(), 
						endPoint.getXYPoint(), getPamSymbol(pamDetection));
				if (bounds == null) {
					bounds = rr;
				}
				else {
					bounds = bounds.union(rr);
				}
				generalProjector.addHoverData(endPoint, pamDetection);
				if ((localisation.getLocContents() & AbstractLocalisation.HAS_PERPENDICULARERRORS) != 0) {
					perpError = localisation.getPerpendiculaError(i);
					parallelError = localisation.getParallelError(i);
					plotErrors(g, getPamSymbol().getLineColor(), endLatLong, localisation.getErrorDirection(i), 
							parallelError, perpError, generalProjector);
					
				}
			}
			return bounds;
		}
		else if ((localisation.getLocContents() & 
				AbstractLocalisation.HAS_BEARING) != 0) {
			int n = 1;
			double[] surfaceAngles = localisation.getPlanarAngles(); // returns a single angle for planar arrays. 
			PamVector[] locVectors = localisation.getWorldVectors(); // need to get two angles back from this for a plane array
			n = surfaceAngles.length;
//			if ((localisation.getLocContents() & AbstractLocalisation.HAS_AMBIGUITY) != 0) {
//				n = 2;
//			}
			for (int i = 0; i < n; i++) {
				range = getDefaultRange();
				if ((localisation.getLocContents() & 
						AbstractLocalisation.HAS_RANGE) != 0) {
					range = localisation.getRange(i);
					if (range>1E6)
						range=getDefaultRange();
				}

//				System.out.println("Range: " + range + " m, bearing " + bearing*180/Math.PI + " degrees.");
				// draw lines from the centreLatLong to the true location and put a symbol at the end of 
				// the line.
				if (locVectors != null && locVectors.length > i) {
					bearing = PamVector.vectorToSurfaceBearing(locVectors[i]);
					referenceBearing = localisation.getBearingReference();
					endLatLong = centreLatLong.travelDistanceMeters(Math.toDegrees(referenceBearing + bearing), 
							range * locVectors[i].xyDistance());
//					endLatLong = centreLatLong.addDistanceMeters(locVectors[i].getElement(0)*range, 
//							locVectors[i].getElement(1)*range);
				}
				else {
					bearing = surfaceAngles[i];
					referenceBearing = localisation.getBearingReference();
					endLatLong = centreLatLong.travelDistanceMeters(Math.toDegrees(referenceBearing + bearing), range);
				}
				endPoint = generalProjector.getCoord3d(endLatLong.getLatitude(), endLatLong.getLongitude(), 0);

				if ((localisation.getLocContents() & 
						AbstractLocalisation.HAS_DEPTH) != 0) {
					depth = localisation.getDepth(0);
					String depth_str = String.format("%.0f m depth", localisation.getDepth(0));
					((Graphics2D) g).setFont(new Font("Arial", Font.PLAIN, 12));
					((Graphics2D) g).setColor(new Color(120,120,200));

					((Graphics2D) g).drawString(depth_str ,(int)endPoint.x+10, (int)endPoint.y);
				}
				bounds = drawLineOnly(g, pamDetection, detectionCentre.getXYPoint(), 
						endPoint.getXYPoint(), getPamSymbol(pamDetection));
				generalProjector.addHoverData(endPoint, pamDetection);

			}
			return bounds;
		}
		else if ((localisation.getLocContents() & 
				AbstractLocalisation.HAS_RANGE) != 0) {
			// draw the symbol at the centre and a circle around it connected by a single line
			generalProjector.addHoverData(detectionCentre, pamDetection);
			Point p = detectionCentre.getXYPoint();
			endLatLong = centreLatLong.travelDistanceMeters(0, localisation.getRange(0));
			endPoint = generalProjector.getCoord3d(endLatLong.getLatitude(), endLatLong.getLongitude(), 0);
			int radius = endPoint.getXYPoint().y - p.y;
			
			getPamSymbol(pamDetection).draw(g, p);
//			g.setColor(lineColour);
			g.drawLine(p.x, p.y, p.x, p.y - radius);
			g.drawOval(p.x-radius, p.y-radius, radius * 2, radius * 2);
			
		}
		return null;
	}
	
	private Rectangle plotErrors(Graphics g, Color col, LatLong refPoint, double refAngle, double err1, double err2, GeneralProjector generalProjector) {

//		g.setColor(col);
		LatLong ll;
		double refBearing = (refAngle * 180 / Math.PI);
		ll = refPoint.travelDistanceMeters(refBearing, err1);
		Coordinate3d eS = generalProjector.getCoord3d(ll.getLatitude(), ll.getLongitude(), 0);
		ll = refPoint.travelDistanceMeters(refBearing+ 180, err1);
		Coordinate3d eE = generalProjector.getCoord3d(ll.getLatitude(), ll.getLongitude(), 0);
		g.drawLine((int) eS.x, (int) eS.y, (int) eE.x, (int) eE.y); 
		Rectangle r = new Rectangle(eS.getXYPoint());
		r.add(eE.getXYPoint());
		ll = refPoint.travelDistanceMeters(refBearing + 90, err2);
		eS = generalProjector.getCoord3d(ll.getLatitude(), ll.getLongitude(), 0);
		ll = refPoint.travelDistanceMeters(refBearing - 90, err2);
		eE = generalProjector.getCoord3d(ll.getLatitude(), ll.getLongitude(), 0);
		g.drawLine((int) eS.x, (int) eS.y, (int) eE.x, (int) eE.y); 
		r.add(eS.getXYPoint());
		r.add(eE.getXYPoint());
		return r;
		
//		// refBEaring should be an angle in radians from the x axis (trig coordinates)
//		// convert this to a compass heading and get the positions of the ends. 
//		g.setColor(col);
//		double compassHeading = 90 - (refAngle * 180 / Math.PI);
//		Coordinate3d centre = generalProjector.getCoord3d(refPoint.getLatitude(), refPoint.getLongitude(), 0);
//		LatLong ll1 = refPoint.travelDistanceMeters(compassHeading, err1);
//		LatLong ll2 = refPoint.travelDistanceMeters(compassHeading+90, err2);
//		Coordinate3d p1 = generalProjector.getCoord3d(ll1.getLatitude(), ll1.getLongitude(), 0);
//		Coordinate3d p2 = generalProjector.getCoord3d(ll2.getLatitude(), ll2.getLongitude(), 0);
//		int cx = (int) centre.x;
//		int cy = (int) centre.y;
//		int dx = (int) (p1.x- centre.x);
//		int dy = (int) (p1.y- centre.y);
//		g.drawLine(cx + dx, cy - dy, cx - dx, cy + dy);
//		dx = (int) (p2.x- centre.x);
//		dy = (int) (p2.y- centre.y);
//		g.drawLine(cx + dx, cy - dy, cx - dx, cy + dy);
//		
//		return null;
	}
	
	protected Rectangle drawLineAndSymbol(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector, LatLong LL1, LatLong LL2, PamSymbol symbol) {
		return drawLineAndSymbol(g, pamDataUnit, generalProjector.getCoord3d(LL1.getLatitude(), LL1.getLongitude(), 0).getXYPoint(),
		generalProjector.getCoord3d(LL2.getLatitude(), LL2.getLongitude(), 0).getXYPoint(), symbol);
	}
	
	protected Rectangle drawLineAndSymbol(Graphics g, PamDataUnit pamDataUnit, Point p1, Point p2, PamSymbol symbol) {
		Rectangle r = symbol.draw(g, p2);
		if (drawLineToLocations) {
			g.setColor(symbol.getLineColor());
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
			r = r.union(new Rectangle(p1.x-1, p1.y-1, 2, 2));
		}
		return r;
	}
	
	protected Rectangle drawLineOnly(Graphics g, PamDataUnit pamDataUnit, Point p1, Point p2, PamSymbol symbol) {
		g.setColor(symbol.getLineColor());
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
		return new Rectangle(p1.x-1, p1.y-1, 2, 2);
	}
	
	/**
	 * 
	 * @param pamDetection PamDetection
	 * @return a map of hydrophones associated with this detection. 
	 */
	protected int getHydrophones(PamDataUnit pamDetection) { 

		int hydrophones = 0;
		AbstractLocalisation localisation = pamDetection.getLocalisation();
		if (localisation != null) {
			hydrophones = localisation.getReferenceHydrophones();
		}
		if (hydrophones == 0) {
			// annoying - will have to find the data souce and get the relationship between the software channels
			// listed in the detection and hydrophone mapping (not necessarily 1:1)
			AcquisitionProcess daqProcess = null;
			try {
				daqProcess = (AcquisitionProcess) parentDataBlock.getSourceProcess();
			}
			catch (Exception ex) {
				return 0;
			}
			if (daqProcess == null) {
				return 0;
			}
			hydrophones = daqProcess.getAcquisitionControl().ChannelsToHydrophones(pamDetection.getChannelBitmap());
		}
		return hydrophones;
	}

	/**
	 * Draw on spectrogram changed March 2010 so that the default time unit is 
	 * milliseconds (Jave time from 1970) rather than samples. This makes it posible 
	 * to work with data colected over multiple files when operating in viewer mode. 
	 * @param g
	 * @param pamDataUnit
	 * @param generalProjector
	 * @return
	 */
	protected Rectangle drawOnSpectrogram(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		// draw a rectangle with time and frequency bounds of detection.
		// spectrogram projector is now updated to use Hz instead of bins. 
		if (isDetectionData == false) return null;
		PamDetection pamDetection = (PamDetection) pamDataUnit;
		double[] frequency = pamDetection.getFrequency();
		Coordinate3d topLeft = generalProjector.getCoord3d(pamDetection.getTimeMilliseconds(), 
				frequency[1], 0);
		Coordinate3d botRight = generalProjector.getCoord3d(pamDetection.getTimeMilliseconds() + 
				pamDetection.getDuration() * 1000./parentDataBlock.getSampleRate(),
				frequency[0], 0);
		if (botRight.x < topLeft.x){
			botRight.x = g.getClipBounds().width;
		}
		if (generalProjector.isViewer()) {
			Coordinate3d middle = new Coordinate3d();
			middle.x = (topLeft.x + botRight.x)/2;
			middle.y = (topLeft.y + botRight.y)/2;
			middle.z = (topLeft.z + botRight.z)/2;
			generalProjector.addHoverData(middle, pamDataUnit);
		}
//		int channel = PamUtils.getSingleChannel(pamDetection.getChannelBitmap());
//		if (channel >= 0) {
//			g.setColor(PamColors.getInstance().getChannelColor(channel));
//		}
//		g.setColor(lineColour);

		g.drawRect((int) topLeft.x, (int) topLeft.y, 
				(int) botRight.x - (int) topLeft.x, (int) botRight.y - (int) topLeft.y);
		return new Rectangle((int) topLeft.x, (int) topLeft.y, 
				(int) botRight.x - (int) topLeft.x, (int) botRight.y - (int) topLeft.y);
	}
	protected Rectangle drawAmplitudeOnRadar(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {
		if (isDetectionData == false) return null;
		PamDetection pamDetection = (PamDetection) pamDataUnit;
		AbstractLocalisation localisation = pamDataUnit.getLocalisation();
		if (localisation == null || localisation.hasLocContent(AbstractLocalisation.HAS_BEARING) == false) return null;
		/*
		 * Try to get the new bearing information first, then if that fails, get 
		 * the old one. 
		 */
		double bearing = 0;
		double[] angleData = localisation.getPlanarAngles();
		bearing = Math.toDegrees(angleData[0]);

		/*
		 * For debug purposes, try to work out what is going on with the array orientation
		 */
//		PamVector[] v = localisation.getArrayOrientationVectors();
//		if (v != null) {
//
//			Graphics2D g2d = (Graphics2D) g;
//
//			// g2d.setPaint(lineColor);
//			// g2d.setColor(lineColor);
//			Stroke oldStroke = g2d.getStroke();
//			g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND,
//					BasicStroke.JOIN_MITER));
//			Coordinate3d plotCent = generalProjector.getCoord3d(0, 10000, 0);
//			Point plotCentPt = plotCent.getXYPoint();
//			PamVector av;
//			for (int i = 0; i < v.length; i++) {
//				av = v[i];
//				if (i == 0) {
//					g.setColor(Color.RED);
//				}
//				else if (i == 1) {
//					g.setColor(Color.GREEN);
//				}
//				else if (i == 2) {
//					g.setColor(Color.BLUE);
//				}
//				g.drawLine(plotCentPt.x, plotCentPt.y, 
//						(int) (plotCentPt.x + 1000*av.getElement(0)), 
//						(int) (plotCentPt.y-1000*av.getElement(1)));
//			}
//		}
		
		double amplitude = pamDetection.getAmplitudeDB();
		
		PamVector[] worldVectors = localisation.getWorldVectors();
		Rectangle r = null;
		Coordinate3d c3d;
		if (worldVectors == null) {
			c3d = generalProjector.getCoord3d(bearing, amplitude, 0);
			if (c3d == null) return null;
			generalProjector.addHoverData(c3d, pamDataUnit);	
			r = getPamSymbol(pamDataUnit).draw(g, c3d.getXYPoint());		
		}
		else {
			// slightly annoying, but will have to convert the vectors, which are in x,y,z coordinates into 
			// a bearing from North !
			double lastBearing = 9999999999.;
			for (int i = 0; i < worldVectors.length; i++) {
				bearing = Math.atan2(worldVectors[i].getElement(1), worldVectors[i].getElement(0));
				bearing = Math.toDegrees(bearing);
				bearing = 90.-bearing;
				if (bearing == lastBearing) {
					continue;
				}
				lastBearing = bearing;
				c3d = generalProjector.getCoord3d(bearing, amplitude, 0);
				if (c3d == null) continue;
				generalProjector.addHoverData(c3d, pamDataUnit);	
				r = getPamSymbol(pamDataUnit).draw(g, c3d.getXYPoint());		
			}
		}
		return r;
	}
	protected Rectangle drawRangeOnRadar(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {

		AbstractLocalisation localisation = pamDataUnit.getLocalisation();
		if (localisation == null || localisation.hasLocContent(AbstractLocalisation.HAS_BEARING | AbstractLocalisation.HAS_RANGE) == false) return null;
		double bearing, range;
		Rectangle r = null, newR;
		Coordinate3d c3d;
		for (int i = 0; i < localisation.getNumLatLong(); i++) {
			bearing = localisation.getBearing(i) * 180 / Math.PI;
			range = localisation.getRange(i);
			c3d = generalProjector.getCoord3d(bearing, range, 0);
			if (c3d == null) return null;
			generalProjector.addHoverData(c3d, pamDataUnit);
			newR = getPamSymbol(pamDataUnit).draw(g, c3d.getXYPoint());
			if (r == null) {
				r = newR;
			}
			else {
				r = r.union(newR);
			}
		}
//		PamSymbol symbol = ClickBTDisplay.getClickSymbol(clickDetector.getClickIdentifier(), click);
		return r;
	}
	protected Rectangle drawSlantOnRadar(Graphics g, PamDataUnit pamDataUnit, GeneralProjector generalProjector) {

		AbstractLocalisation localisation = pamDataUnit.getLocalisation();
		if (localisation == null || 
				localisation.hasLocContent(AbstractLocalisation.HAS_BEARING) == false) return null;
		double bearing, slantAngle;
		Rectangle r = null, newR;
		Coordinate3d c3d;
		PamVector v[] = localisation.getWorldVectors();
		if (v == null | v.length == 0) {
			return null;
		}
		double[] vec;
		PamSymbol pamSymbol = getPamSymbol(pamDataUnit);
		for (int i = 0; i < v.length; i++) {
			vec = v[i].getVector();
			bearing = Math.atan2(vec[1],vec[0]);
			bearing = 90.-Math.toDegrees(bearing);
			slantAngle = Math.atan2(vec[2], v[i].xyDistance());
			slantAngle = Math.toDegrees(slantAngle);
			c3d = generalProjector.getCoord3d(bearing, slantAngle, 0);
			if (c3d == null) return null;
			generalProjector.addHoverData(c3d, pamDataUnit);
			newR = pamSymbol.draw(g, c3d.getXYPoint());
			if (r == null) {
				r = newR;
			}
			else {
				r = r.union(newR);
			}
		}
//		for (int i = 0; i < localisation.getNumLatLong(); i++) {
//			bearing = localisation.getBearing(i) * 180 / Math.PI;
////			range = localisation.getRange(i);
//			slantAngle = 
//			c3d = generalProjector.getCoord3d(bearing, slantAngle, 0);
//			if (c3d == null) return null;
//			generalProjector.addHoverData(c3d, pamDataUnit);
//			newR = getPamSymbol(pamDataUnit).draw(g, c3d.getXYPoint());
//			if (r == null) {
//				r = newR;
//			}
//			else {
//				r = r.union(newR);
//			}
//		}
//		PamSymbol symbol = ClickBTDisplay.getClickSymbol(clickDetector.getClickIdentifier(), click);
		return r;
	}

	public String getHoverText(GeneralProjector generalProjector, PamDataUnit dataUnit, int iSide) {
		String str =  "<html>" + parentDataBlock.getDataName();
		str += String.format("<br> Start: %s %s", 
				PamCalendar.formatDate(dataUnit.getTimeMilliseconds()),
				PamCalendar.formatTime(dataUnit.getTimeMilliseconds(), true));
		if (isDetectionData && dataUnit.getChannelBitmap() != 0) {
			str += String.format("<br> Channels: %s", PamUtils.getChannelList(dataUnit.getChannelBitmap()));
		}
		if (isDetectionData) {
			PamDetection pd = (PamDetection) dataUnit;
			str += "<br>"+FrequencyFormat.formatFrequencyRange(pd.getFrequency(), true);
		}
		AbstractLocalisation loc = dataUnit.getLocalisation();
		if (loc != null) {
			int locCont = loc.getLocContents();
			if ((locCont & AbstractLocalisation.HAS_LATLONG) != 0) {
				LatLong latLong = loc.getLatLong(iSide);
				str += String.format("<br> %s, %s", latLong.formatLatitude(), latLong.formatLongitude());
			}
			if ((locCont & AbstractLocalisation.HAS_BEARING) != 0) {
				double[] surfaceAngles = loc.getPlanarAngles();
				if (surfaceAngles != null) {
					str += String.format("<br> Angle %4.1f\u00B0", Math.toDegrees(surfaceAngles[0]));
//					if (surfaceAngles.length > 1) {
//						str += String.format("  ± %4.1f\u00B0", loc.getBearingError(iSide)*180/Math.PI);
//					}
					if (surfaceAngles.length > 1) {
						str += "<br> (Left Right Ambiguity)";
					}
				}
			}
			if ((locCont & AbstractLocalisation.HAS_RANGE) != 0) {
				str += String.format("<br> Range %4.1f m", loc.getRange(iSide));
				if ((locCont & AbstractLocalisation.HAS_RANGEERROR) != 0) {
					str += String.format("  ± %4.1f\u00B0", loc.getRangeError(iSide)*180/Math.PI);
				}
			}
			
			if((locCont & AbstractLocalisation.HAS_DEPTH) != 0) {
				str += String.format("<br> Depth %4.1f m", loc.getDepth(iSide));
			}
		}
		str += "</html>";
		return str;
	}

	public Color getLineColour() {
		return lineColour;
	}

	public void setLineColour(Color lineColour) {
		if (pamSymbol != null) {
			pamSymbol.setLineColor(lineColour);
		}
		this.lineColour = lineColour;
	}

	/**
	 * 
	 * @param pamDataUnit
	 * @return PamSymbol to use in plotting. Generally this is just the 
	 * set symbol for the overlay, but can be overridden if a detector has
	 * some complicated way of using different symbols for different dataUnits. 
	 */
	public PamSymbol getPamSymbol(PamDataUnit pamDataUnit) {
//		if (pamSymbol == null) {
//			pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, DEFSYMBOLSIZE, 
//					DEFSYMBOLSIZE, false, Color.BLACK, Color.BLACK);
//		}
		return pamSymbol;
	}
	
	public PamSymbol getPamSymbol() {
//		if (pamSymbol == null) {
//			pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, DEFSYMBOLSIZE, 
//					DEFSYMBOLSIZE, false, Color.BLACK, Color.BLACK);
//		}
		return pamSymbol;
	}

	public void setPamSymbol(PamSymbol pamSymbol) {
		this.pamSymbol = pamSymbol;
	}

	public double getDefaultRange() {
		return defaultRange;
	}

	public void setDefaultRange(double defaultRange) {
		this.defaultRange = defaultRange;
	}

	/**
	 * 
	 * @return true if the datablock associated with this overlay contians data units
	 * subclassed from PamDetection - in which case they might have Localisation information. 
	 */
	public boolean isDetectionData() {
		return isDetectionData;
	}

	public void setDetectionData(boolean isDetectionData) {
		this.isDetectionData = isDetectionData;
	}

	public boolean isDrawLineToLocations() {
		return drawLineToLocations;
	}

	public void setDrawLineToLocations(boolean drawLineToLocations) {
		this.drawLineToLocations = drawLineToLocations;
	}

	public ManagedSymbolInfo getSymbolInfo() {
		return new ManagedSymbolInfo(this.getParentDataBlock().getDataName());
	}

	public PamKeyItem getMenuKeyItem() {
		return new PanelOverlayKeyItem(this);
	}
	
	public void setLineColor( Color c ) {
		this.lineColour = c;
	}
	
	public Color getLineColor() {
		return this.lineColour;
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
