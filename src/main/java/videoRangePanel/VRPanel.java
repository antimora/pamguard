package videoRangePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import Map.MapContour;
import Map.MapFileManager;
import Map.MasterReferencePoint;
import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamView.ManagedSymbol;
import PamView.ManagedSymbolInfo;
import PamView.PamSymbol;
import PamView.PamSymbolManager;

public class VRPanel extends JPanel {

	VRControl vrControl;
	
	BufferedImage vrImage;
	
	BufferedImage scaledImage;
		
	JScrollPane scrollPane;
	
	JPanel innerPanel;
	
	int imageWidth, imageHeight; // image actual size in pixels. 
	int frameWidth, frameHeight; // viewable images size on screen.
	int panelWidth, panelHeight; // size of scaled image on screen. 
	int cropWidth, cropHeight; // viewable area of image. 
	double xScale = 1, yScale = 1;
	
	Point currentMouse;
	

	public VRPanel(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
		this.setLayout(new BorderLayout());
		innerPanel = new InnerPanel();
		add(BorderLayout.CENTER, scrollPane = new JScrollPane(innerPanel));
//		scrollPane.setLayout(new BorderLayout());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	}
	
	boolean loadImageFromFile(File file) {
		
		try {
			vrImage = ImageIO.read(file);
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}

        newImage();
        
		return (vrImage != null);
	}
	boolean pasteImage() {
		
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
   
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
                if (image == null) {
                	return false;
                }
                int w = image.getWidth(null);
                int h = image.getHeight(null);
                vrImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
                Graphics g = vrImage.getGraphics();
                g.drawImage(image, 0, 0, w, h, 0, 0, w, h, null);
                newImage();
        		return (vrImage != null);
            }
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        return false;

	}
	private void newImage() {
		if (vrImage == null) {
			return;
		}
		cloneNewImage();
		sortScales();
		setImageBrightness();
		scrollPane.repaint();
	}
	
	@Override
	public void repaint() {
		super.repaint();
		if (innerPanel != null) {
			innerPanel.repaint();
		}
	}
	
	private void cloneNewImage() {
		if (vrImage == null) {
			return;
		}
		int w = vrImage.getWidth();
		int h =vrImage.getHeight();
		scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics g = scaledImage.getGraphics();
        g.drawImage(vrImage, 0, 0, w, h, 0, 0, w, h, null);
	}
	
	private float brightness = 1;
	private float contrast = 10;
	protected void setImageBrightness(float brightness, float contrast) {
		/*
		 * scaleFctor can be between 0 and 2
		 * offset can be between 0 and 255
		 */
//		float scaleFactor = 1 + brightness;
//		float offset = 10;
		this.brightness = brightness;
		this.contrast = contrast;
		setImageBrightness();
	}
	protected void setImageBrightness() {
		if (vrImage == null || scaledImage == null) {
			return;
		}
		RescaleOp rescaleOp = new RescaleOp(brightness, contrast, null);
		
		rescaleOp.filter(vrImage, scaledImage);
	}

	void sortScales() {
		sortScales(vrControl.vrParameters.imageScaling);
	}
	void sortScales(int scaleType) {
		if (vrImage == null) {
			return;
		}
		imageHeight = vrImage.getHeight(this);
		imageWidth = vrImage.getWidth(this);
		cropWidth = imageWidth;
		cropHeight = imageHeight;
		Insets frameInsets;
		switch (scaleType) {
		case VRParameters.IMAGE_CROP:
			showScrollBars(false);
			frameInsets = scrollPane.getInsets(); 
			frameWidth = scrollPane.getWidth() - frameInsets.right - frameInsets.left;
			frameHeight = scrollPane.getHeight() - frameInsets.top - frameInsets.bottom;
			cropWidth = panelWidth = Math.min(frameWidth, imageWidth);
			cropHeight = panelHeight = Math.min(frameHeight, imageHeight);
			xScale = yScale = 1;
			break;
		case VRParameters.IMAGE_SCROLL:
			showScrollBars(true);
			frameInsets = scrollPane.getInsets(); 
			frameWidth = scrollPane.getWidth() - frameInsets.right - frameInsets.left;
			frameHeight = scrollPane.getHeight() - frameInsets.top - frameInsets.bottom;
			panelWidth = imageWidth;
			panelHeight = imageHeight;
			xScale = yScale = 1;
			break;
		case VRParameters.IMAGE_SHRINK:
			showScrollBars(false);
			frameInsets = scrollPane.getInsets(); 
			frameWidth = scrollPane.getWidth() - frameInsets.right - frameInsets.left;
			frameHeight = scrollPane.getHeight() - frameInsets.top - frameInsets.bottom;
			double shrinkScaleX = 1, shrinkScaleY = 1;
			if (imageWidth > frameWidth) {
				shrinkScaleX = (double) frameWidth / imageWidth;
			}
			if (imageHeight > frameHeight) {
				shrinkScaleY = (double) frameHeight / imageHeight;
			}
			xScale = yScale = Math.min(shrinkScaleX, shrinkScaleY);
			panelWidth = (int) Math.floor(imageWidth * xScale);
			panelHeight = (int) Math.floor(imageHeight * yScale);
			break;
		case VRParameters.IMAGE_SHRINKORSTRETCH:
		case VRParameters.IMAGE_STRETCH:
		}
//		System.out.println("Sort scales width, height = " + imageWidth + ", " + imageHeight);
		innerPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
		scrollPane.invalidate();
	}
	
	private void showScrollBars(boolean show) {
		if (show) {
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		}
		else {
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		}
		scrollPane.invalidate();
	}
	
//	@Override
//	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
//		// from ImageObserver interface.
//		boolean ans = super.imageUpdate(img, infoflags, x, y, width, height);
//		repaint();
//		System.out.println("Image update called");
//		return ans;
//	}
	
	
	class InnerPanel extends JPanel {

		private VRSymbolManager horizonSymbol, animalSymbol, candidateSymbol, calibrationSymbol;
		
		private VRRangeMethod vrRangeMethod;
		private VRCalibrationData calData;
		private VRHeightData heightData;
		
		private Color landColour = Color.BLACK;
		private PamSymbol landPointSymbol = new PamSymbol(PamSymbol.SYMBOL_POINT, 1, 1, true, Color.RED, Color.RED);
		
		public InnerPanel() {
			super();
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(31, 31);
			VRCursor cursor = new VRCursor(d);
			setCursor(cursor.getCursor());

			addMouseMotionListener(new VRMouseAdapter());
			addMouseListener(new VRMouseAdapter());
			PamSymbol horizonMarker = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, false, Color.BLUE, Color.BLUE);
			PamSymbol animalMarker = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, false, Color.GREEN, Color.GREEN);
			PamSymbol candidateMarker = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, false, Color.RED, Color.RED);
			PamSymbol calibrationMarker = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, false, Color.RED, Color.RED);
			horizonSymbol = new VRSymbolManager(horizonMarker, "Video Range Horizon");
			animalSymbol = new VRSymbolManager(animalMarker, "Video Range Animal");
			candidateSymbol = new VRSymbolManager(candidateMarker, "Video Range Candidate Animal");
			calibrationSymbol = new VRSymbolManager(calibrationMarker, "Video Range Calibration Mark. ");

		}

		@Override
		protected void paintComponent(Graphics g) {
			vrRangeMethod = vrControl.rangeMethods.getCurrentMethod();
			sortScales();
			super.paintComponent(g);
			if (vrImage == null) {
//				String msg = "No Image";
				return;
			}
			if (scaledImage == null) {
				scaledImage = vrImage;
			}
			g.drawImage(scaledImage, 0, 0, panelWidth, panelHeight, 0, 0, cropWidth, cropHeight, this);
			// add markers. 
			switch (vrControl.getVrStatus()) {
			case VRControl.MEASURE_FROM_HORIZON:
			case VRControl.MEASURE_FROM_SHORE:
				if (vrControl.vrParameters.showShore) {
					drawLand(g);
				}
				addMeasurementMarks(g);
				addAnimals(g);
				break;
			case VRControl.CALIBRATE:
				addCalibrationMarks(g);
				break;
			}
		}
		
		private void addMeasurementMarks(Graphics g) {
			drawMarksandLine(g, vrControl.getHorizonPoint1(), vrControl.getHorizonPoint2(), 
					horizonSymbol.getPamSymbol(), vrControl.vrParameters.drawTempHorizon);
		}
		private void addCalibrationMarks(Graphics g) {
			drawMarksandLine(g, vrControl.getCalibratePoint1(), vrControl.getCalibratePoint2(), 
					calibrationSymbol.getPamSymbol(), true);
		}

		private void drawMarksandLine(Graphics g, Point p1, Point p2, PamSymbol symbol, boolean drawLine) {

			Point sp1 = null, sp2 = null;
			if (p1 != null) {
				symbol.draw(g, sp1 = imageToScreen(p1));
			}
			if (p2 != null) {
				symbol.draw(g, sp2 = imageToScreen(p2));
				if (sp1 != null) {
					g.setColor(symbol.getLineColor());
					g.drawLine(sp1.x, sp1.y, sp2.x, sp2.y);
				}
			}
			if (sp1 != null && sp2 == null && currentMouse != null && drawLine) {
				g.setColor(symbol.getLineColor());
				g.drawLine(sp1.x, sp1.y, currentMouse.x, currentMouse.y);
			}
			
			sp1 = vrControl.getShorePoint();
			if (sp1 != null) {
				symbol.draw(g, imageToScreen(sp1));
			}
		}
		private void addAnimals(Graphics g) {
			ArrayList<VRMeasurement> vrms = vrControl.getMeasuredAnimals();
			if (vrms != null) {
				for (int i = 0; i < vrms.size(); i++) {
					drawAnimal(g, vrms.get(i), animalSymbol.getPamSymbol());
				}
			}
			if (vrControl.getCandidateMeasurement() != null) {
				drawAnimal(g, vrControl.getCandidateMeasurement(), candidateSymbol.getPamSymbol());
			}
		}
		
		private void drawAnimal(Graphics g, VRMeasurement vr, PamSymbol symbol) {
			Point p1, p2;
			symbol.draw(g, p1 = imageToScreen(vr.animalPoint));
			p2 = imageToScreen(vr.horizonPoint);
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
		
		/**
		 * Draw the land outline
		 * <p>
		 * Requires knowledge of the horizon or some point on the land and 
		 * also the angle for this to work. 
		 * @param g graphics handle
		 */
		private void drawLand(Graphics g) {
			/**
			 * Will have to get the positions of the land at each x pixel. For this
			 * we will need to convert a distance to a y coordinate. 
			 * The most general way of doing this will be to have a horizon pixel for
			 * any point, and then work out the angle below the horizon for a given angle. 
			 * (can't plot anything above the horizon of course). 
			 * If we're working to the shore, then work out where the horizon should be before 
			 * plotting. 
			 */
			Double landAngle = vrControl.vrTabPanelControl.imageAnglePanel.getAngle();
			if (landAngle == null) {
				return;
			}
			// check that there are some reference points before drawing
			if (getHorizonPixel(getImageWidth()/2) == null) {
				return; 
			}
//			if (hasReferenceMarks() == false) {
//				return;
//			}
			LatLong origin = MasterReferencePoint.getRefLatLong();
			ShoreManager shoreManager = vrControl.shoreManager;
			MapFileManager mapManager = shoreManager.getMapFileManager();
			if (mapManager == null) {
				return;
			}
			heightData = vrControl.vrParameters.getCurrentheightData();
			if (heightData == null) {
				return;
			}
			
			calData = vrControl.vrParameters.getCurrentCalibrationData();
			if (calData == null) {
				return;
			}
			g.setColor(Color.BLACK);
			Vector<LatLong> contour;
			MapContour mapContour;
			for (int i = 0; i < mapManager.getContourCount(); i++) {
				mapContour = mapManager.getMapContour(i);
				contour = mapContour.getLatLongs();
				for (int l = 0; l < contour.size()-1; l++) {
					drawMapSegment(g, origin, heightData.height, calData.degreesPerUnit, landAngle, contour.get(l), contour.get(l+1));
				}
			}
		}
		
		private void drawMapSegment(Graphics g, LatLong origin, double height, double degreesPerUnit,
				double imageAngle, LatLong ll1, LatLong ll2) {
			Point p1, p2;
			p1 = getObjectPoint(origin, height, degreesPerUnit, imageAngle, ll1);
			p2 = getObjectPoint(origin, height, degreesPerUnit, imageAngle, ll2);
			if (p1 == null || p2 == null) {
				return;
			}
			if (p1.x < 0 && p2.x < 0) {
				return;
			}
			if (p1.x > getImageWidth() && p2.x > getImageWidth()) {
				return;
			}
			p1 = imageToScreen(p1);
			p2 = imageToScreen(p2);
			g.setColor(landColour);
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
			if (vrControl.vrParameters.showShorePoints) {
				landPointSymbol.draw(g, p1);
				landPointSymbol.draw(g, p2);
			}
		}
		
		private Point getObjectPoint(LatLong origin, double height, double degreesPerUnit, 
				double imageAngle, LatLong objectLL) {
			if (vrRangeMethod == null) {
				return null;
			}
			double range = origin.distanceToMetres(objectLL);
			double angle = vrRangeMethod.getAngle(height, range);
			if (angle < 0) {
				return null;
			}
			double bearing = origin.bearingTo(objectLL);
			double angDiff = PamUtils.constrainedAngle(imageAngle - bearing, 180);
			if (Math.abs(angDiff) > 90) {
				return null;
			}
			int x = bearingTox(imageAngle, bearing);
			int y = (int) (getHorizonPixel(x) + vrRangeMethod.getAngle(height, range) * 180 / Math.PI / degreesPerUnit);
			return new Point(x,y);
		}
		
		/**
		 * Get's the Y coordinate of the horizon at a given X.
		 * @param x
		 * @return
		 */
		private Double getHorizonPixel(int x) {
			Double y = null;
			switch (vrControl.getVrStatus()) {
			case VRControl.MEASURE_FROM_HORIZON:
				y = getHorizonPixel_H(x);
				break;
			case VRControl.MEASURE_FROM_SHORE:
				y = getHorizonPixel_H(x);
				break;
			}
			if (y == null) {
				/*
				 * Work something out based on the tilt so that the 
				 * shore line gets shown at the top of the image in any case. 
				 */
				double dx = x - (getImageWidth()/2);
				y = -dx * Math.tan(vrControl.getHorizonTilt() * Math.PI / 180);
			}
			return y;
		}
		/**
		 * Gets the horizon pixel number when horizon points have beenmarked. 
		 * <p>
		 * Only this one needed, since VRControl immediately works out the horizon
		 * position whenever the shore point is set or the angle changes. 
		 * @param x c coordinate on screeen
		 * @return horizon pixel number
		 */
		private Double getHorizonPixel_H(int x) {
			Point p1 = vrControl.getHorizonPoint1();
			Point p2 = vrControl.getHorizonPoint2();
			if (p1 == null) {
				return null;
			}
			if (p2 == null) {
				return (double) p1.y;
			}
			// extrapolate / interpolate to allow for slanty horizon. 
			return p1.y + (double) (p2.y-p1.y) / (double) (p2.x-p1.x) * (x - p1.x);
		}
//		private Double getHorizonPixel_S(int x) {
//			if (vrRangeMethod == null || calData == null || heightData == null) {
//				return null;
//			}
//			Point p = vrControl.getShorePoint();
//			if (p == null) {
//				return null;
//			}
//			Double shoreRange = vrControl.getShoreRange();
//			if (shoreRange == null) {
//				return null;
//			}
//			// dip from horizon to this point. 
//			Double angleTo = vrRangeMethod.getAngle(heightData.height, shoreRange);
//			if (angleTo < 0) {
//				// over horizon
//				return null; 
//			}
//			return p.y - angleTo * 180 / Math.PI / calData.degreesPerUnit;
//		}
		private int bearingTox(double centreAngle, double objectBearing) {
			/*
			 * centreAngle should have been taken from the little top panel
			 * and is the bearing at the centre of the image. Work out the
			 * x pixel from the bearing difference. 
			 */
			VRCalibrationData calData = vrControl.vrParameters.getCurrentCalibrationData();
			if (calData == null) return 0;
			double ad = PamUtils.constrainedAngle(objectBearing - centreAngle, 180);
			return getImageWidth() / 2 + (int) (ad / calData.degreesPerUnit); 
		}
	}
	
	class VRMouseAdapter extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {

			super.mouseClicked(e);
			if (e.getButton() == MouseEvent.BUTTON1) {
				vrControl.mouseClick(screenToImage(e.getPoint()));
				innerPanel.repaint();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);
			vrControl.newMousePoint(screenToImage(e.getPoint()));
			currentMouse = e.getPoint();
			if (vrControl.getVrSubStatus() == VRControl.MEASURE_HORIZON_2 || 
					vrControl.getVrSubStatus() == VRControl.CALIBRATE_2) {
				innerPanel.repaint();
			}
			checkHoverText(e.getPoint());
		}
		
		private void checkHoverText(Point mousePoint) {
			Point imagePoint = screenToImage(mousePoint);
			double closest = 50;
			double newDist;
			String txt = null;
			Point tp;
			tp = vrControl.getHorizonPoint1();
			if (tp != null)
				if ((newDist = imagePoint.distance(tp)) < closest) {
				txt = "Horizon point 1";
				closest = newDist;
			}
			tp = vrControl.getHorizonPoint2();
			if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
				txt = "Horizon point 2";
				closest = newDist;
			}
			tp = vrControl.getShorePoint();
			if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
				txt = "Shore point";
				closest = newDist;
			}
			tp = vrControl.getCalibratePoint1();
			if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
				txt = "Calibration point 1";
				closest = newDist;
			}
			tp = vrControl.getCalibratePoint2();
			if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
				txt = "Calibration point 2";
				closest = newDist;
			}
			ArrayList<VRMeasurement> vrms = vrControl.getMeasuredAnimals();
			VRMeasurement vrm;
			if (vrms != null) for (int i = 0; i < vrms.size(); i++) {
				vrm = vrms.get(i);
				tp = vrm.animalPoint;
				if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
					closest = newDist;
//					txt = String.format("<html>Animal measurement %d, range %.1f m", vrm.imageAnimal, vrm.distanceMeters);
//					if (vrm.comment != null && vrm.comment.length() > 0) {
//						txt += "<br>" + vrm.comment;
//					}
//					txt += "</html>";
					txt = "<html>Animal measurement<br>" + vrm.getHoverText();
				}
				tp = vrm.horizonPoint;
				if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
					closest = newDist;
					txt = String.format("<html>Horizon intercept animal %d, range %.1f m", vrm.imageAnimal, vrm.distanceMeters);
					if (vrm.comment != null && vrm.comment.length() > 0) {
						txt += "<br>" + vrm.comment;
					}
					txt += "</html>";
				}
			}
			if ((vrm = vrControl.getCandidateMeasurement()) != null) {
				tp = vrm.animalPoint;
				if (tp != null && (newDist = imagePoint.distance(tp)) < closest) {
					closest = newDist;
					txt = "<html>Canididate measurement<br>" + vrm.getHoverText();
				}
				
			}
			if (txt != null) {
				innerPanel.setToolTipText(txt);
			}
			else {
				innerPanel.setToolTipText(vrControl.getInstruction());
			}
				
		}
//		private getDist()

		@Override
		public void mouseExited(MouseEvent e) {			
			super.mouseExited(e);
			vrControl.newMousePoint(null);
			currentMouse = null;
		}
		
	}
	
	Point screenToImage(Point screenPoint) {
		Point p = new Point(screenPoint);
		p.x = (int) (p.x / xScale);
		p.y = (int) (p.y / yScale);
		return p;
	}
	Point imageToScreen(Point screenPoint) {
		Point p = new Point(screenPoint);
		p.x = (int) (p.x * xScale);
		p.y = (int) (p.y * yScale);
		return p;
	}
	
	class VRSymbolManager implements ManagedSymbol {
		
		PamSymbol symbol;
		
		ManagedSymbolInfo symbolInfo;
		
		public VRSymbolManager(PamSymbol defSymbol, String description) {
			symbolInfo = new ManagedSymbolInfo(description);
			PamSymbolManager.getInstance().addManagesSymbol(this);
			if (getPamSymbol() == null) {
				setPamSymbol(defSymbol);
			}
		}

		public PamSymbol getPamSymbol() {
			return symbol;
		}

		public ManagedSymbolInfo getSymbolInfo() {
			return symbolInfo;
		}

		public void setPamSymbol(PamSymbol pamSymbol) {
			this.symbol = pamSymbol;
		}
		
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public int getImageWidth() {
		return imageWidth;
	}
	

}
