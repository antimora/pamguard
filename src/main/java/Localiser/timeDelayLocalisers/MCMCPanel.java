package Localiser.timeDelayLocalisers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Shape3D;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.TitledBorder;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.picking.PickCanvas;


import Layout.PamAxis;
import Layout.PamAxisPanel;
import PamGraph3D.MouseGetVirtualLocation;
import PamGraph3D.MouseRightClickMenu;
import PamGraph3D.PamPanel3D;
import PamGraph3D.PamShapes3D;
import PamView.PamPanel;

/**
 * This class creates a PamPanel with a graph which shows the chi2 values from a set of MCMC jumps.
 * @author Jamie Macaulay
 *
 */
public class MCMCPanel {
	
	private PamPanel mainPanel;
	protected BranchGroup pickGroup;
	
	//Axis
	protected ChiGraphAxis axisPanel;
	private PamAxis xAxis;
	private PamAxis yAxis;
	
	//the  chi value to initially display on the graph
	protected float maxChiValue=1000;
	//the number of jumps to initially show;
	protected float maxNoJumps=10000;
	//size of the graph in java3D units- graph goes from -50 to 50 for x and y;
	protected double j3DGraphSize=100;
	
	//there are tens of thousands of jumps in every MCMC chain. compressVal value specifies how many jumps to use. e.g. compressVal of 5 means take a fifth of the jumps. 
	protected int compressVal=10; 

	

	private PamPanel3D plotPanel;
	MouseGetVirtualLocation mouseGetVirtualLocation;

	
	public MCMCPanel(Frame frame){
		
		mainPanel=new PamPanel(new BorderLayout());

		//Craete 3D Panel
	
		plotPanel=new PamPanel3D(frame, true);
		plotPanel.setAspectRatioEnabled(true);

		////Mouse Functionality////
		MouseDragged mouseDragged=new MouseDragged();
		mouseDragged.setSchedulingBounds(plotPanel.bounds);
		BranchGroup mouseDrag=new BranchGroup();
		mouseDrag.addChild(mouseDragged);
		
		MShowPopUpMenu mouseRightClick=new MShowPopUpMenu();
		mouseRightClick.setMainPanel(plotPanel);
		
		MouseWheelMoved mouseWheelMoved=new MouseWheelMoved();
		mouseWheelMoved.setSchedulingBounds(plotPanel.bounds);
		BranchGroup mouseWheelMove=new BranchGroup();
		mouseWheelMove.addChild(mouseWheelMoved);
		
		mouseGetVirtualLocation=new MouseGetVirtualLocation();
		mouseGetVirtualLocation.setAspectRatioPolicy(true);

		plotPanel.addMouseRightClickMenu(mouseRightClick);
		plotPanel.addMouseGetVirtualLocation(mouseGetVirtualLocation);
		plotPanel.addChildtoRotateGroup(mouseDrag);
		plotPanel.addChildtoRotateGroup(mouseWheelMove);
		plotPanel.addMouseZoom();
		plotPanel.addMouseTranslate();
		plotPanel.getMouseProportionalZoom().setProportionalityFactor(0.01);
		plotPanel.getMouseProportionalZoom().setMinZoomFactor(0.05);

		pickGroup=new BranchGroup();
		pickGroup.setCapability(BranchGroup.ALLOW_DETACH);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		pickGroup.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		
		plotPanel.addChildtoGraphicsGroup(pickGroup);

		///Construct components	
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(BorderLayout.CENTER, plotPanel);
		innerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		
		axisPanel = new ChiGraphAxis();
		axisPanel.setPlotPanel(plotPanel);
		axisPanel.setInnerPanel(innerPanel);
		
		mainPanel.setPreferredSize(new Dimension(200, 300));	
		mainPanel.add(BorderLayout.CENTER,axisPanel);
		mainPanel.setBorder(new TitledBorder("Chi Graph"));
		
		resetPlot();
		axisPanel.setPlotAxis();
		axisPanel.validate();
	
	}
	

	public JPanel getPanel(){
		return mainPanel;
	}
	
	public double defaultZoomFactor(){
		return 	-120;
	}
	
	public void resetPlot(){
		plotPanel.transformTranslation(new Vector3d(0,0,defaultZoomFactor()));
	}
	
	/**
	 * Resets the plot
	 * @author spn1
	 *
	 */
	class Reset implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			resetPlot();
			axisPanel.setPlotAxis();
		}
	}
	
	
	/**
	 * Sets the plot axis every time the mouse is dragged. Note that this type of listener is required for Java3D windows. 
	 *  
	 * @author Jamie Macaulay
	 *
	 */
	public class MouseDragged extends Behavior {
		
	    // create SimpleBehavior
	    MouseDragged() {
	    }
	    
	    public void initialize() {
	      // set initial wakeup condition
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED));
	  	//System.out.println("mouse dragged");
	    }
	    
	    // called by Java 3D when appropriate stimulus occures
	    public void processStimulus(Enumeration criteria) {
	    	axisPanel.setPlotAxis();
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED));
	    }
	  } 
	
	/**Show pop up menu.
	 * 
	 * @author Jamie Macaulay
	 *
	 */
	private class MShowPopUpMenu extends MouseRightClickMenu {
		@Override
		public void showPopupMenu(JPanel mainPanel, Point point) {
			JPopupMenu menu=createPopupMenu();
			menu.show(mainPanel, point.x, point.y);
		}
	  } 
	
	/**
	 * Sets the plot axis every time the mouse wheel is moved. Note that this type of listener is required for Java3D windows. 
	 *  
	 * @author Jamie Maculay
	 *
	 */
	public class MouseWheelMoved extends Behavior {
		
	    // constructor
		MouseWheelMoved() {
	    }
	    
	    public void initialize() {
	      // set initial wake up condition
	      this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL));
	    }
	    
	    // called by Java 3D when appropriate stimulus occures
	    public void processStimulus(Enumeration criteria) {	    	
	    	axisPanel.setPlotAxis();
	    	this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL));
	    }
	    
	  } 
	
	public JPopupMenu createPopupMenu() {
		
		JCheckBoxMenuItem jBoxMenuItem;
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		
		JPopupMenu menu = new JPopupMenu();
		
		JMenuItem menuItem = new JMenuItem("Reset Plot");
		menuItem.addActionListener(new Reset());
		menu.add(menuItem);
		return menu;
	}
	
	
	/**
	 * Add an arraylist of chi values to the graph. 
	 * @param chiList
	 */
	public void addChiValuestoGraph(ArrayList<Double> chiList){
		
		Point3f point;
		ArrayList<Point3f> displayPoints=new ArrayList<Point3f>();
		
		for (int i=0; i<chiList.size(); i=i+compressVal){
			//maxNoJumps
			point=new Point3f((float) ((j3DGraphSize*i/(double) maxNoJumps)-j3DGraphSize/2),(float)((j3DGraphSize*chiList.get(i).floatValue()/maxChiValue)-j3DGraphSize/2),0f);
			displayPoints.add(point);
		}
		
		BranchGroup bg=new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		
		Shape3D chiLine= PamShapes3D.linePolygon3D(displayPoints, PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.randomBlue()));

		bg.addChild(chiLine);

		pickGroup.addChild(bg);
	}
	
	public void removeAllGraphData(){
		pickGroup.removeAllChildren();
	}
	
	
	/**
	 * Create the chi squared/iteration axis. Automatically resizes with zoom. 
	 * @author EcologicUK
	 *
	 */
	public class ChiGraphAxis extends PamAxisPanel {

		private static final long serialVersionUID = 1L;


		public ChiGraphAxis() {
			super();
			xAxis = new PamAxis(0, 0, 1, 1, 0, 1, false, "Jumps", "%.1f");
			yAxis = new PamAxis(0, 0, 1, 1, 0, 1, true, "Chi Squared", "%d");
			setSouthAxis(xAxis);
			setWestAxis(yAxis);
			this.SetBorderMins(10, 20, 10, 20);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
		}
		
		public void setPlotAxis(){
			
			double chiBins=maxChiValue/j3DGraphSize;
			double jumpBins=10000/j3DGraphSize;
			
			int xPixs = plotPanel.getWidth();
			int yPixs = plotPanel.getHeight();
			
			Point3d WindowMin=mouseGetVirtualLocation.getRealLoc(new Point(xPixs,yPixs));
			Point3d WindowMax=mouseGetVirtualLocation.getRealLoc(new Point(0,0));
			
			xAxis.setRange((WindowMax.getX()+j3DGraphSize/2)*jumpBins, (WindowMin.getX()+j3DGraphSize/2)*jumpBins);
			yAxis.setRange((WindowMin.getY()+j3DGraphSize/2)*chiBins, (WindowMax.getY()+j3DGraphSize/2)*chiBins);
			yAxis.setInterval((-WindowMin.getY()+WindowMax.getY())*chiBins/ 4);
			axisPanel.repaint();
		}
		
		
		public void setSpectrumXAxis() {
		}
		
		
		private void setSpectrumYAxis() {
		}
	}
	
	public PickCanvas getPickCanvas(){
		return plotPanel.getPickCanvas();
	}
	
	public int getCompressVal(){
		return compressVal;
	}
	
	/**
	 * Sets the compression value. This value reduces the number of values displayed on the graph by a factor of compressVal. This is important to reduce memory footprint as MCMC can have tens of thousands of values.   
	 * @param compressVal
	 */
	public void setCompressVal(int compressVal){
		if (compressVal<1){
			System.out.println("Compression value for this graph must be >0");
			return;
		}
		this.compressVal=compressVal;
	}
	
	
}
	


