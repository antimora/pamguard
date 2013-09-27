package bearingTimeDisplay;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.ListIterator;
import java.awt.Toolkit;


import javax.media.j3d.Bounds;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;


import Layout.PamAxis;
import Layout.PamAxisPanel;
import PamView.JBufferedPanel;
import PamView.PamSymbol;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 *This class is designed primarily to be used for a new (post 2012) generic bearing time display but could be extended to other types of graphs and displays if necessary. 
 *<p>
 *The class itself simply brings several other classes together, creating a display. Two axis must be defined, an x-axis (horizontal) and a y-Axis (vertical).
 *<p>
 *Each axis is a subclass of AbstractAxis and has several functions. 
 *<p>
 *1)Defines the min/max data range and visible datarange. 
 *<p>
 *2)Provides an axis panel- this can contain scroll bars, JSpinners etc. These can be used to change the datarange. 
 *<p>
 *3)Provides the unit of measurement from a PamDataUnit. For example the time axis will call PamDataUnit.getTimeMillis(); 
 *<p>
 *4)Can provide JPopUpBox options and dialog boxes. 
 *<p>
 *The two axis classes plug into the GraphDisplay using the setXAxis and setYAxis functions. This means that any display can be rotated 90 degrees simply by swapping these two functions with whichever two AbstractAxis classes are being used. Be careful with formatting of the axis JPanel though. 
 *<p>
 *The graphDisplay requires at one datablock although many can be added. Although the axis classes define which measurements to take from the dataunits within the datablocks the datablocks themselves define the symbol that should be displayed on the graph. 
 *This means multiple datablocks can be added as long as they have dataunits which share some common measurment e.g.time and amplitude/bearing. 
 *<p>
 *So in summary: 
 *Define two axis classes,
 *Create a graphDisplay, 
 *Add a datablock. Done
 * 
 * @author Jamie Macaulay
 *
 */


public class GraphDisplay extends JPanel {


	ArrayList<PamDataBlock> dataBlocks=new ArrayList<PamDataBlock>();
	
	AxisProvider xAxis;
	
	AxisProvider yAxis;
	
	private JBufferedPanel plotPanel;
	
	JPanel transparentLayer;

	private Frame frame;
	
	GraphDisplay graphDisplay;
	
	PamAxisPanel pamAxisPanel;

	private double windowWidth;

	private double windowHeight;
	
	boolean scrollBarsShowing=false;
	
	//maximum and minimum values of data which are visible on the window.
	private double[] yMinMax;

	private double[] xMinMax;

	//.. the currently selected click, null if no click is selected
	private PamDataUnit selectedDataUnit;

	private JPanel innerPanel;
	
	 final static BasicStroke solid =
		        new BasicStroke(1.5f);
	
	public GraphDisplay(Frame frame){
		
		this.frame=frame;
		this.setLayout(new BorderLayout());
		graphDisplay=this;
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

		plotPanel=new PlotPanel();
		plotPanel.setLayout(new BorderLayout());
		plotPanel.addMouseListener(new MousePick());

		innerPanel=new JPanel(new BorderLayout());
		innerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		innerPanel.add(BorderLayout.CENTER, plotPanel);
		
		pamAxisPanel=new PamAxisPanel();
		pamAxisPanel.SetBorderMins(10, 10, 10, 10);
		pamAxisPanel.setPlotPanel(plotPanel);
		pamAxisPanel.setInnerPanel(innerPanel);
		


//		//JPanel plotPanelHolder=new JPanel(new BorderLayout());
//		//plotPanelHolder.add(BorderLayout.CENTER, plotPanel);
//		
//		System.out.println("Bounds: "+this.getBounds());
//		
//		JLayeredPane layeredPane=new JLayeredPane();
//		//layeredPane.setLayout(new BorderLayout());
//		//layeredPane.setPreferredSize(new Dimension(300,300));
//		layeredPane.add(plotPanel, JLayeredPane.DEFAULT_LAYER);
//		plotPanel.setBounds(new Rectangle(500,180));
//		transparentLayer=new JPanel(new BorderLayout());
//		transparentLayer.setOpaque(false);
//		transparentLayer.setBounds(new Rectangle(500,180));
//		transparentLayer.addMouseListener(mouseEvent);
//		transparentLayer.add(BorderLayout.NORTH, new JLabel("Transparant Layer"));
//		layeredPane.add(transparentLayer, JLayeredPane.DRAG_LAYER);
//		layeredPane.validate();
		//this.addMouseListener(mouseEvent);
		
		this.add(BorderLayout.CENTER, pamAxisPanel);

		
		Toolkit.getDefaultToolkit().addAWTEventListener( new TargetedMouseHandler(innerPanel), AWTEvent.MOUSE_EVENT_MASK );

		
	
	}

	/**
	 * Set the x axis for the graph. 
	 * @param xAxis
	 */
	public void setXAxis(AxisProvider xAxis){
		this.xAxis=xAxis;
		pamAxisPanel.setNorthAxis(xAxis.getPamAxis());
		pamAxisPanel.repaint();
		

	}
	
	/**
	 * Set the y axis for the graph.
	 * @param yAxis
	 */
	public void setYAxis(AxisProvider yAxis){
		this.yAxis=yAxis;
		pamAxisPanel.setWestAxis(yAxis.getPamAxis());
		pamAxisPanel.repaint();
	}
	
	
	/**
	 * Called if the visible range has changed
	 */
	public void rangeValueChanged(){
		scrollBarChanged();
	}
	
	/**
	 * Called if the scroll bar on either the x or y axis is moved
	 */
	public void scrollBarChanged(){
		this.repaint(50);
	}
	
	/**
	 * Calculates the current minimum and maximum of VISIBLE data for the axis. Note this will only return a value which is not the minimum/maximum of the graph axis if the axis class has scroll bars and/or range spinners. 
	 * @param axis
	 * @return a double[] of which [0] is the minimum visible value of data and [1] is the maximum visible value of data for this graph. The units are entirely dependent on the dataunit used. For example if this was a time axis then returned min/max will be the min/max in time.  seconds
	 */
	public static double[] getWindowMinMax(AxisProvider axis){

		double unitFrac=axis.getScrollBarValue()/(axis.getScrollBarMax()-axis.getScrollBarMin());
				
		double maximumTranslate=(axis.getAxisMax()-axis.getAxisMin()-axis.getAxisVisibleRange());
		
		double unitStart=unitFrac*maximumTranslate+axis.getAxisMin();
		
		double unitEnd=unitStart+axis.getAxisVisibleRange();
		
		double[] minMax=new double[2];
		minMax[0]=unitStart;
		minMax[1]=unitEnd;
			
//		System.out.println("AxisMin: "+axis.getAxisMin());
//		System.out.println("AxisMax: "+axis.getAxisMax());
//		System.out.println("AxisRange: "+axis.getAxisVisibleRange());
//		System.out.println("ScrollBarMax: "+axis.getScrollBarMax());
//		System.out.println("ScrollBarMin: "+axis.getScrollBarMin());
//		System.out.println("ScrollBarValue: "+axis.getScrollBarValue());
//		System.out.println("CompFactor: "+ maximumTranslate);
//		System.out.println("UnitFrac: "+unitFrac);
//		System.out.println("UnitStart: "+unitStart);
//		System.out.println("UnitStart: "+unitEnd);
			
		return minMax;
	}
	

	
	/**
	 * Repaints all the detections from all the datablocks onto the graph. Maintains any highlighted detection. 
	 * @param g
	 */
	public void paintDetections(Graphics g){
		
		for (int i=0; i<dataBlocks.size(); i++){
			paintDataBlockDetections(dataBlocks.get(i), g);
			highLightSelectedDetection(selectedDataUnit,  g);
		}		
	}
	
	/**
	 * Paints all the visible detections from the pamDataBlock onto the plot panel. 
	 * @param pamDataBlock
	 */
	public void paintDataBlockDetections(PamDataBlock pamDataBlock, Graphics g){
		
		PamDataUnit unit;
		double xAxisVal;
		double yAxisVal;

		Point plotPoint;
		PamSymbol symbol = null;
		 
		this. windowWidth= plotPanel.getSize().getWidth();
		this. windowHeight=plotPanel.getSize().getHeight();
		 
		this.xMinMax=getWindowMinMax(xAxis);
		this.yMinMax=getWindowMinMax(yAxis);
		
		
		synchronized (pamDataBlock) {
			ListIterator<PamDataUnit> detectionIterator = pamDataBlock.getListIterator(PamDataBlock.ITERATOR_END);
			while (detectionIterator.hasPrevious()) {
				unit = detectionIterator.previous();
				
				 xAxisVal=xAxis.getMeasurment(unit);
				 yAxisVal=yAxis.getMeasurment(unit);
				
				 if (xAxisVal<this.xMinMax[0] || xAxisVal>this.xMinMax[1] || yAxisVal<this.yMinMax[0] || yAxisVal>this.yMinMax[1]){
					// System.out.println("Continue");
					continue;
				 }
				 
				 symbol=pamDataBlock.get2DPlotProvider().getSymbol(unit);
				 Point size=pamDataBlock.get2DPlotProvider().getSymbolSize(unit);
				 
				 plotPoint=new Point(getPlotCoOrd(xMinMax[0] , xMinMax[1], xAxisVal, windowWidth, xAxis.invert()),getPlotCoOrd(yMinMax[0] , 
						 yMinMax[1], yAxisVal, windowHeight, yAxis.invert()));
				 

				 symbol.draw(g, plotPoint, size.x, size.y);

			}
			
		}
		
	}
	
	public static int getPlotCoOrd(double axisMin, double axisMax, double unitValue,double  windowSize, boolean invert){
		
		int point= (int) (windowSize*(unitValue-axisMin)/(axisMax-axisMin));
		
		if (invert==false) return (int) (windowSize-point);
		else return point;
		
	}
	
	public boolean shouldPlot(PamDataUnit unit){
		if (xAxis.getMeasurment(unit)<xMinMax[0] || xAxis.getMeasurment(unit)>xMinMax[1] || yAxis.getMeasurment(unit)<yMinMax[0] || yAxis.getMeasurment(unit)>yMinMax[1]){
				return false;
		}
			 return true;
	}
	
	
	
	/**
	 * Get the point (in pixels) at which the unit is located. 
	 * @param unit
	 * @return
	 */
	public Point getDetectionCoOrd(PamDataUnit unit){
	
		double xAxisVal=xAxis.getMeasurment(unit);
		double yAxisVal=yAxis.getMeasurment(unit);
		 
		double []  xMinMax=getWindowMinMax(xAxis);
		double []  yMinMax=getWindowMinMax(yAxis);
		
		return new Point(getPlotCoOrd(xMinMax[0] , xMinMax[1], xAxisVal, windowWidth,xAxis.invert()),getPlotCoOrd(yMinMax[0] , 
				 yMinMax[1], yAxisVal, windowHeight, yAxis.invert()));
	}
	

	
	public void addDetection(){
	
	}
	
	public void addDataBlock(PamDataBlock pamDataBlock){
		dataBlocks.add(pamDataBlock);
	}


	public Window getFrame() {
		return this.frame;
	}

	public void refreshAllData() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Removes all the datablocks from the graph;
	 */
	public void removeAllDataBlocks() {
		 dataBlocks=new ArrayList<PamDataBlock>();		
	}
	
	/**
	 * This is called every time a detection is selected. Override to integrate the display into other modules etc. 
	 * @param dataUnit
	 */
	public void detectionSelected(PamDataUnit dataUnit){
		
		
	}

	public void setSelectedDetection(PamDataUnit detection) {
		this.selectedDataUnit=detection;
		repaint(50);
	}
	
	
	/**
	 * Pop up menu if blank space on the graph is clicked
	 * @return
	 */
	public JPopupMenu jMenuBlankSpace(){
		JPopupMenu popMenu=new JPopupMenu();
		xAxis.addMenuItems(popMenu);
		yAxis.addMenuItems(popMenu);
		return popMenu;
	}
	
	
	/**
	 * Pop up menu if a data unit on the graph is selected
	 * @return
	 */
	public JPopupMenu JMenuSelDetection(){
		JPopupMenu popMenu=new JPopupMenu();
		xAxis.addMenuItems(popMenu);
		yAxis.addMenuItems(popMenu);
		//popMenu.add(new JSeparator());
		return popMenu;
	}
	
	/**
	 * Finds the detection closest to the pixel x,y on the plotpanel. If no detection is present within maxdist then a null result is returned,  
	 * @param x - x pixel on the graph to look from
	 * @param y - y pixel on the graph to look from
	 * @param maxdist-  the maximum distance in pixels within which a detection must occur in order to be highlighted.
	 * @param dataBlock 
	 * @return the detection closest to the point x, y on the plotpanel. 
	 */
	PamDataUnit findUnit(int x, int y, int maxdist, PamDataBlock dataBlock) {
		
		this.xMinMax=getWindowMinMax(xAxis);
		this.yMinMax=getWindowMinMax(yAxis);
		
		PamDataUnit closestDataUnit = null;
		PamDataUnit unit;
		Point pt;
		int dist;
		int closest = maxdist * maxdist;
		synchronized (dataBlock) {
			ListIterator<PamDataUnit> listIterator = dataBlock.getListIterator(PamDataBlock.ITERATOR_END);
			while (listIterator.hasPrevious()) {
				unit = listIterator.previous();
				if (shouldPlot(unit)==false) continue;
				pt = getDetectionCoOrd(unit);
				if ((dist = ((pt.x - x) * (pt.x - x) + (pt.y - y) * (pt.y - y))) <= closest) {
					closest = dist;
					closestDataUnit = unit;
				}
			}
		}

		return closestDataUnit;
	}
	
	/**
	 * Highlights a specified detection on the plotPanel. 
	 * @param unit
	 * @param g
	 */
	public void highLightSelectedDetection(PamDataUnit unit, Graphics g){
		if (unit==null || xMinMax==null || yMinMax==null) return;
		PamSymbol symbol=unit.getParentDataBlock().get2DPlotProvider().getSymbolSelected(unit);
		 Point size=unit.getParentDataBlock().get2DPlotProvider().getSymbolSize(unit);
		Point plotPoint=new Point(getPlotCoOrd(xMinMax[0] , xMinMax[1], xAxis.getMeasurment(unit), windowWidth,xAxis.invert()),getPlotCoOrd(yMinMax[0] , 
				 yMinMax[1], 	yAxis.getMeasurment(unit), windowHeight, yAxis.invert()));
		symbol.draw(g, plotPoint, size.x+5, size.y+5);
	}
	


	
	
	public class MousePick implements MouseListener {
		


		@Override
		public void mouseClicked(MouseEvent e) {
			
			if (e.getButton()==MouseEvent.BUTTON1){
				setSelectedDetection(findUnit(e.getX(), e.getY(), 10, dataBlocks.get(0)));
				detectionSelected(selectedDataUnit);
			}
			
			if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
				jMenuBlankSpace().show(plotPanel,e.getX(),e.getY());
			}
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
	
		}

		@Override
		public void mouseExited(MouseEvent e) {
	
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
		}
		
		
		
	}
	

	
	private void removeMainAxisPanels(){
		scrollBarsShowing=false;

		innerPanel.remove(yAxis.getAxisControlPanel());
		innerPanel.remove(xAxis.getAxisControlPanel());
		innerPanel.revalidate();
		innerPanel.repaint();
		pamAxisPanel.repaint();
		plotPanel.revalidate();
		plotPanel.repaint();
	}
	
	private void addMainAxisPanels(){
		scrollBarsShowing=true;

		innerPanel.add(BorderLayout.EAST, yAxis.getAxisControlPanel());
		innerPanel.add(BorderLayout.SOUTH, xAxis.getAxisControlPanel());
		innerPanel.validate();
		innerPanel.repaint();
		plotPanel.revalidate();
		pamAxisPanel.repaint();
		plotPanel.repaint();
		
	}

	
	class WindowResize implements ComponentListener{
		
	public void componentResized(ComponentEvent e) {
		scrollBarChanged();
	}


	@Override
	public void componentHidden(ComponentEvent arg0) {		
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {		
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}
	
	}
	
	class MouseMotion implements MouseMotionListener{

		@Override
		public void mouseDragged(MouseEvent arg0) {
			
			
		}

		@Override
		public void mouseMoved(MouseEvent arg0) {
			System.out.println(arg0.getLocationOnScreen());
			
		}
		
	}
	
	class PlotPanel extends JBufferedPanel {
		
			@Override
			public void paintPanel(Graphics g, Rectangle clipRectangle) {
				paintDetections(g);
			}
			
	}
	
	class MyGlassPane extends JComponent {

		public MyGlassPane(){
			super.addMouseListener(new MousePick());
			this.setOpaque(false);
		}
		
	}
	
	/**
	 * Need to write my own boundry conditions functions as the java one doesn't seem to work very well- have added in a pixel error here so that 
	 * @param bounds
	 * @param p
	 * @param pixelsEr
	 * @return
	 */
	private static boolean checkForExitBoundryConditions(Rectangle bounds ,Point p, int pixelsEr){
		
		 p=new Point((int)p.getX()+bounds.x, (int)p.getY()+bounds.y);
		 
		 if (!bounds.contains(p)) return true;
		 else if (p.getY()>= bounds.getHeight()+bounds.y-pixelsEr ) return true;
		 else if (p.getX()>= bounds.getWidth()+bounds.x-pixelsEr ) return true;
		 else if (p.getY()>= bounds.getHeight()+bounds.y-pixelsEr ) return true;
		 else if (p.getY()-pixelsEr<= bounds.y) return true;
		 else if (p.getX()-pixelsEr<=bounds.x) return true;
		 
		
		 return false;
	}
	

public class TargetedMouseHandler implements AWTEventListener {

    private Component parent;
    private boolean hasExited = true;

    public TargetedMouseHandler(Component p)
    {
        parent = p;

    }

    @Override
    public void eventDispatched(AWTEvent e)
    {
        if (e instanceof MouseEvent)
        {
        	MouseEvent m = (MouseEvent) e;
        
	            if (SwingUtilities.isDescendingFrom(
	                (Component) e.getSource(), parent))
	            {
	               
	                if (m.getID() == MouseEvent.MOUSE_ENTERED){
	                    if (hasExited){
	                        hasExited = false;
	                        addMainAxisPanels();
	                    }
	                    
	                } 
	                else if (m.getID() == MouseEvent.MOUSE_EXITED){
	                    Point p = SwingUtilities.convertPoint(
	                        (Component) e.getSource(),
	                        m.getPoint(),
	                        parent);                    
	                  // p=new Point((int)p.getX()+parent.getBounds().x, (int)p.getY()+parent.getBounds().y);
	                    if (checkForExitBoundryConditions(parent.getBounds() , p, 3)){
	                        hasExited = true;
	                        removeMainAxisPanels();
	                    }
	                }
	            }
        	}
    	}
	}

class GraphAxisH extends PamAxisPanel{
	
	public GraphAxisH() {
		super();
//		PamAxis xNAxis = new PamAxis(0, 0, 1, 1, 0, 1, false, "x (m)", "%.1f");
//		PamAxis yWAxis = new PamAxis(0, 0, 1, 1, 0, 1, true, "y (m)", "%d");
//		setWestAxis(yWAxis);
//		setNorthAxis(xNAxis);
		this.SetBorderMins(10, 10, 10, 10);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}

	
}
