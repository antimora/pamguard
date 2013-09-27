package Array;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import Acquisition.AcquisitionControl;
import Layout.PamAxis;
import Layout.PamAxisPanel;
import PamView.PamSymbol;
import PamView.PamColors.PamColor;

/**
 * Panel for the ArrayDialog to show a diagram of the array
 * @author Doug Gillespie
 * @see Array.ArrayDialog
 *
 */
public class HydrophoneDiagram {

	private JPanel hydrophonePanel;
	
	private PlotPanel plotPanel;
	
	private AxisPanel axisPanel;
	
	private PamAxis xAxis, yAxis;
	
	/**
	 * Double vector of x,y,z max and min values.  
	 */
	private double [][] plotLims;
//	private double plotScale = 1;
	
	private ArrayDialog arrayDialog;
	
	public HydrophoneDiagram(ArrayDialog arrayDialog) {
		
		this.arrayDialog = arrayDialog;
		
		hydrophonePanel = makeHydrophonePanel();
	}
	
	public JPanel getPlotPanel() {
		return hydrophonePanel;
	}

	JPanel makeHydrophonePanel() {
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder("Hydrophone positions"));
		panel.setLayout(new BorderLayout());
//		JPanel b = new JPanel();
//		b.setBorder(BorderFactory.createLoweredBevelBorder());
//		b.setLayout(new BorderLayout());
		panel.add(BorderLayout.CENTER, axisPanel = new AxisPanel());
		panel.setPreferredSize(new Dimension(390, 110));
//		panel.add(BorderLayout.CENTER, new PamAxisP);
		
		return panel;
	}
	
	void rePaint() {
		setScales();
		plotPanel.repaint();
		axisPanel.repaint();		
	}
	
	private boolean setScales() {
		// look through the hydrphones and set the min and max x and y scales. 
		
//		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
		PamArray array = arrayDialog.getHydrophoneDialogPanel().getDialogSelectedArray();
		
		double[][] arrayLims= new double[3][2];
		if (array != null&&array.getDimensionLimits()!=null){
			arrayLims = array.getDimensionLimits();
		}
		
		
		double maxDim = Math.max(arrayLims[0][1]-arrayLims[0][0], arrayLims[1][1]-arrayLims[1][0]);
		// need a border of about 1/4 the max dimension to make the plot look OK
		double border = maxDim/4;
		if (border <= 0) {
			border = 1;
		}
		// and force it to be a power of 10;
		border = (int) Math.ceil(Math.log10(border));
		border = Math.pow(10, border);
		for (int i = 0; i < 3; i++) {
			arrayLims[i][0] -= border;
			arrayLims[i][1] += border;
		}
		
		plotLims = new double[3][2];
		double[] dimSizes = new double[3];
		double minSize = 1;
		double extraNeeded;
		for (int i = 0; i < 3; i++) {
			dimSizes[i] = arrayLims[i][1]-arrayLims[i][0];
			plotLims[i] = Arrays.copyOf(arrayLims[i],2);
			if (dimSizes[i] < minSize) {
				extraNeeded = minSize-dimSizes[i];
				plotLims[i][0] -= extraNeeded/2;
				plotLims[i][1] += extraNeeded/2;
				dimSizes[i] = minSize;
			}
		}

		Dimension winSize = plotPanel.getSize();
		double xScale = dimSizes[0] / winSize.getWidth();
		double yScale = dimSizes[1] / winSize.getHeight();
		// need to take the largest
		double scale = Math.max(xScale, yScale);
		// now set the upper plot limits according to the scale.
		plotLims[0][1] = plotLims[0][0] + scale * winSize.getWidth();
		plotLims[1][1] = plotLims[1][0] + scale * winSize.getHeight();
//		
//		
//		double largestScale = Math.max(dimSizes[0], dimSizes[1]);
//		double scale = 1;
//		while (scale < largestScale * 3/2) scale *= Math.sqrt(10);
//		scale = 2 * Math.ceil(scale/2);
		
		
		
		xAxis.setRange(plotLims[0][0], plotLims[0][1]);
		yAxis.setRange(plotLims[1][0], plotLims[1][1]);
		
		return true;		
	}
	
	class AxisPanel extends PamAxisPanel {

		public AxisPanel() {
			super();
			
			setInnerPanel(plotPanel = new PlotPanel());
			
			setAutoInsets(true);
			
			xAxis = new PamAxis(0,1,2,3,-5, 5, false, "x (m)", "%3.1f");
			yAxis = new PamAxis(0,1,2,3,-5, 5, true, "y (m)", "%6.1f");
			
			setSouthAxis(xAxis);
			setWestAxis(yAxis);
		}

		@Override
		public PamColor getColorId() {
			return null;
		}
		
	}
	class PlotPanel extends JPanel {

		Dimension lastPlotSize = new Dimension(1,1);

		PamSymbol pamSymbol;
		
		int arrowLength = 6;
		
		public PlotPanel() {
			super();
			setBackground(Color.WHITE);
			setBorder(BorderFactory.createLoweredBevelBorder());
			pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 8, 8,
					true, Color.BLUE, Color.RED);
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			
			super.paintComponent(g);
			
			Graphics2D g2D = (Graphics2D) g;
			
			Dimension newSize = getSize();
			if (newSize.equals(lastPlotSize) == false) {
				setScales();
				if (axisPanel != null) axisPanel.repaint();
				lastPlotSize = newSize;
			}
			
			AcquisitionControl acquisitionControl = arrayDialog.getChannelPanel().getAcquisitionControl();

			PamArray array = arrayDialog.getHydrophoneDialogPanel().getDialogSelectedArray();
			
			/**
			 * Write the type of array up into the top left corner.
			 * 
			 */
			int arrayType = ArrayManager.getArrayManager().getArrayShape(array);
			ArrayManager.getArrayManager();
			String arrayStr = ArrayManager.getArrayTypeString(arrayType);
			FontMetrics fm = g.getFontMetrics();
			g.drawString(arrayStr, fm.charWidth(' '), fm.getHeight());
			
//			PamVector[] locVectors = ArrayManager.getArrayManager().getArrayDirections(array);
//			for (int i = 0; i < locVectors.length; i++) {
//				System.out.println(String.format("Loc vector %d = %s", i, locVectors[i].toString()));
//			}
			
			double[][] arrayLims= new double[3][2];
			if (array != null&&array.getDimensionLimits()!=null){
				arrayLims = array.getDimensionLimits();
			}
			
			double[] yLim = arrayLims[1];
			// probably it's behind
			yLim[1] = Math.max(yLim[1], 0);
			yLim[0] = Math.min(yLim[0], 0);
			Streamer streamer;
			double streamerX;
			int x1, y1, x2, y2;
//			Rectangle2D r = g.getFontMetrics().getStringBounds(course, g);
//			// now draw the rotated text
			
			Rectangle2D tR;
			String strString;
			
//			double maxY = arrayLims[1][1];
//			double minY = arrayLims[1][0];
			
			y1 = (int)yAxis.getPosition(yLim[0]);
			y2 = (int)yAxis.getPosition(yLim[1]);
//			y1 = newSize.height - (int) ((yLim[0] - minY) * plotScale);
//			y2 = newSize.height - (int) ((yLim[1] - minY) * plotScale);
			for (int i = 0; i < array.getNumStreamers(); i++) {
				streamer = array.getStreamer(i);
				x1 = x2 = (int) xAxis.getPosition(streamer.getX());
				g.drawLine(x1, y1, x2, y2);
				strString = String.format("Streamer %d", i);
				tR = g.getFontMetrics().getStringBounds(strString, g);
				g2D.rotate(-Math.PI/2.);
				g.drawString(strString, (int) -tR.getMaxX() - 30, (int) (x1+tR.getCenterY()));
				g2D.rotate(Math.PI/2.);
			}
			
//			PamArray array = ArrayManager.getArrayManager().getCurrentArray();
			int nPhones = array.getHydrophoneArray().size();
			Hydrophone phone;
			int x, y;
			String str;
			int adcChannel;
			Rectangle2D strRect;
			double phoneX, phoneY;
			for (int i = 0; i < nPhones; i++) {
				phone = array.getHydrophone(i);
				streamer = array.getStreamer(phone.getStreamerId());
				phoneX = phone.getX();
				phoneY = phone.getY();
				if (streamer != null) {
					phoneX += streamer.getX();
					phoneY += streamer.getY();
				}
				x = (int) xAxis.getPosition(phoneX);
				y = (int) yAxis.getPosition(phoneY);
//				y = newSize.height - (int) ((phone.getY() - minY) * plotScale);
				pamSymbol.draw(g, new Point(x,y));
				g.setColor(Color.BLACK);
				if (streamer != null) {
					x2 = (int) xAxis.getPosition(streamer.getX());
					g.drawLine(x, y, x2, y);
				}
				str = String.format("%d ", i);
				g.drawString(str, x+5, y-5);
				if (acquisitionControl != null) {
					adcChannel = acquisitionControl.findHydrophoneChannel(i);
					if (adcChannel >= 0) {
						strRect = g.getFontMetrics().getStringBounds(str, g);
						g.setColor(Color.RED);
						g.drawString(String.format("(%d)", adcChannel), (int)(x+5 + strRect.getWidth()), y-5);
					}
				}
				
			}
			// draw an arrow in the corner
			g.setColor(Color.BLUE);
			y = newSize.height /2;
			x = newSize.width * 9/10;
			y2 = y - Math.min((newSize.height/2), 100);
			g.drawLine(x, y, x, y2);
			g.drawLine(x, y2, x-arrowLength, y2+arrowLength);
			g.drawLine(x, y2, x+arrowLength, y2+arrowLength);
			String course;
			if (array.getArrayType() == PamArray.ARRAY_TYPE_TOWED) {
				course = new String("Ships Heading");
			}
			else {
				course = new String("True North");
			}
			Rectangle2D r = g.getFontMetrics().getStringBounds(course, g);
			// now draw the rotated text
			g2D.rotate(-Math.PI/2.);
			g.drawString(course, (int) -((y + y2)/2 + r.getCenterX()), (int) (x+r.getCenterY()));
			g2D.rotate(Math.PI/2.);
			
			// and finally write in the corner what the numbers mean.
			str = new String("Hydrophone numbers ");
			strRect = g.getFontMetrics().getStringBounds(str, g);
			g.setColor(Color.BLACK);
			x = 10;
			y = (newSize.height - g.getFontMetrics().getAscent() );
			g.drawString(str, x, y);
			x += (int) strRect.getWidth();
			g.setColor(Color.RED);
			g.drawString("(ADC channel numbers)", x, y);
		}
	
	}
	
	
}
