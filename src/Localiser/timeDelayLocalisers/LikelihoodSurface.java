package Localiser.timeDelayLocalisers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;

import clickDetector.ConcatenatedSpectParams;
import clickDetector.ConcatenatedSpectrogramdialog;
import clickDetector.ConcatenatedSpectrogram.MShowPopUpMenu;

import Layout.PamAxis;
import Layout.PamAxisPanel;
import Localiser.timeDelayLocalisers.MCMCPanel.ChiGraphAxis;
import Localiser.timeDelayLocalisers.MCMCPanel.MouseDragged;
import Localiser.timeDelayLocalisers.MCMCPanel.MouseWheelMoved;
import PamGraph3D.MouseGetVirtualLocation;
import PamGraph3D.MouseRightClickMenu;
import PamGraph3D.PamPanel3D;
import PamGraph3D.spectrogram3D.Surface3D;
import PamUtils.FrequencyFormat;
import PamView.PamBorderPanel;
import PamView.PamLabel;

/**
 * This is a simple panel which displays the chi/likilihood surface.  Note (Chi-squared  � � is lnL for Gaussian distributed variables)
 * @author Jamie Macaulay
 *
 */
public class LikelihoodSurface {
	////Swing components////
	JPanel mainPanel;
	LikilihoodSurfaceAxis axisPanel;
	PamAxis xAxis;
	PamAxis yAxis;
	JCheckBoxMenuItem option3D;
	PamLabel cursorPos;

	////3D components////
	PamPanel3D plotPanel;
	MouseGetVirtualLocation mouseGetVirtualLocation;
	double j3DGraphSize=100;
	//hold the branch groups so we don't have to recalculate a new surface every time 3D and 2D options are selected. 
	BranchGroup surface3D;
	BranchGroup surface2D;
	//real size of out square in meters
	double minX=0;
	double minY=0;
	double maxX=100;
	double maxY=100;
	private Frame frame;

	////Params////
	LikelihoodSurfaceParams simplexPanelParams;

	////Surface Info////
	//hold surface info incase we need to replot after new options have been selected. 
	private MultivariateRealFunction chiSquaredFunction;
	private double[] location;
	private int[] dimensions;
	
	public LikelihoodSurface(Frame frame){
		
		this.frame=frame;
		this.simplexPanelParams=new LikelihoodSurfaceParams();
		this.dimensions=calcDimensionArray();
		
		mainPanel=new JPanel(new BorderLayout());
		
		plotPanel=new PamPanel3D(frame);
		plotPanel.setAspectRatioEnabled(true);
		
		
		mouseGetVirtualLocation=new MouseShowInfo();
		mouseGetVirtualLocation.setAspectRatioPolicy(true);
		
		MouseDragged mouseDragged=new MouseDragged();
		mouseDragged.setSchedulingBounds(plotPanel.bounds);
		BranchGroup mouseDrag=new BranchGroup();
		mouseDrag.addChild(mouseDragged);
		
		MouseWheelMoved mouseWheelMoved=new MouseWheelMoved();
		mouseWheelMoved.setSchedulingBounds(plotPanel.bounds);
		BranchGroup mouseWheelMove=new BranchGroup();
		mouseWheelMove.addChild(mouseWheelMoved);
		
		MShowPopUpMenu mouseRightClick=new MShowPopUpMenu();
		mouseRightClick.setMainPanel(mainPanel);
		
		plotPanel.addMouseGetVirtualLocation(mouseGetVirtualLocation);
		plotPanel.addMouseRightClickMenu(mouseRightClick);
		plotPanel.addChildtoRotateGroup(mouseDrag);
		plotPanel.addChildtoRotateGroup(mouseWheelMove);
		plotPanel.addMouseZoom();
		plotPanel.setMouseZoomFactor(0.1);
		plotPanel.addMouseTranslate();
		if (simplexPanelParams.is3D==true) 	plotPanel.addMouseRotate();
		plotPanel.setMouseTranslateFactor(0.01);
		
		//set the swing components and axis. 
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(BorderLayout.CENTER, plotPanel);
		innerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
				
		axisPanel = new LikilihoodSurfaceAxis();
		axisPanel.setPlotPanel(plotPanel);
		axisPanel.setInnerPanel(innerPanel);
		
		mainPanel.setPreferredSize(new Dimension(200, 300));	
		mainPanel.add(BorderLayout.CENTER,axisPanel);
		mainPanel.add(BorderLayout.NORTH, new SurfaceInfo());
		mainPanel.setBorder(new TitledBorder("Likelihood Surface"));
		
		axisPanel.setPlotAxis();
		axisPanel.validate();
		
		resetPlot();
		
	}
	
	public JPanel getPanel(){
		return mainPanel;
	}
	
	public void resetPlot(){
		plotPanel.transformTranslation(new Vector3d(0,0,defaultZoomFactor()));
		plotPanel.resetPlotRotation();
		axisPanel.setPlotAxis();
	}
	
	public double defaultZoomFactor(){
		return 	-120;
	}
	
	public void removeGraphData(){
		plotPanel.getGraphicsGroup().removeAllChildren();
	}
	
	/**
	 * Set the correct axis labels for the dimensions. The first dimensions is the x Axis, the second dimensions is the y Axis and the third is the dimension which stays constant. For example if dim={1,2,0} then xAxis is "y(m)"
	 * , y axis is "z(m)". The chi surface should therefore be constant in x;
	 * @param dim
	 */
	public void setAxisLabels(int[] dim){
		xAxis.setLabel(getAxisString(dim[0]));
		yAxis.setLabel(getAxisString(dim[1]));
		axisPanel.repaint();
	}
	
	public String getAxisString(int dim){
		String axisString="";
		switch (dim){
			case(0):
				axisString="x (m)";
			break;
			case(1):
				axisString="y (m)";
			break;
			case(2):
				axisString="z (m)";
			break;
		}
		return axisString;
	}
	
	/**
	 * Sets the plot axis every time the mouse is dragged. Note that this type of listener is required for Java3D windows. 
	 *  
	 * @author EcologicUK
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
	
	
	/**
	 * Sets the plot axis every time the mouse wheel is moved. Note that this type of listener is required for Java3D windows. 
	 *  
	 * @author EcologicUK
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
	
	/**Show pop up menu
	 * 
	 * @author EcologicUK
	 *
	 */
	public class MShowPopUpMenu extends MouseRightClickMenu {
		@Override
		public void showPopupMenu(JPanel mainPanel, Point point) {
			JPopupMenu menu=createPopupMenu();
			menu.show(getMainPanel(), point.x, point.y);
		}

		private JPopupMenu createPopupMenu() {
			
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			JPopupMenu menu = new JPopupMenu();
			
			JMenuItem menuItem = new JMenuItem("Plot Options...");
			menuItem.addActionListener(new PlotOptions());
			menu.add(menuItem);
			
			menu.addSeparator();
			
			menuItem = new JMenuItem("x-y");
			menuItem.addActionListener(new DimensionChange(2));
			menu.add(menuItem);
			menuItem = new JMenuItem("x-z");
			menuItem.addActionListener(new DimensionChange(1));
			menu.add(menuItem);
			menuItem = new JMenuItem("y-z");
			menuItem.addActionListener(new DimensionChange(0));
			menu.add(menuItem);
			
			
			menu.addSeparator();

			option3D = new JCheckBoxMenuItem("3D");
			option3D.setSelected(simplexPanelParams.is3D);
			option3D.addActionListener(new ThreeD());
			menu.add(option3D);
			
			menuItem = new JMenuItem("Reset Plot");
			menuItem.addActionListener(new Reset());
			menu.add(menuItem);
			
			return menu;
		}
	
	  } 
	
	class DimensionChange implements ActionListener {
		
		private int dim; 
		
		public DimensionChange(int dim){
			this.dim=dim;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (simplexPanelParams.dim==dim) return;
			simplexPanelParams.dim=dim;
			plotPanel.getGraphicsGroup().removeAllChildren();
			createSurface(getChiSquaredFuntion(), getLocation());
		}
		
	}
	

	/**
	 * Changes the plot from 3D to 2D and vice versa. I've tried my best here to keep things fast by storing 3D and 2D shapes unless there's a change in the surface data/colour. 
	 * @author Jamie Macaulay
	 *
	 */
	class ThreeD implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (simplexPanelParams.is3D==option3D.getState()) return;
			
			simplexPanelParams.is3D=option3D.getState();
			
			plotPanel.getGraphicsGroup().removeAllChildren();
			
			if (simplexPanelParams.is3D==true){
				if (surface3D==null){
					createSurface(chiSquaredFunction ,location);
				}
				else{
					plotPanel.addChildtoGraphicsGroup(surface3D);
				}
				plotPanel.addMouseRotate();
			}
			if (simplexPanelParams.is3D==false){
				if (surface2D==null){
					createSurface(chiSquaredFunction ,location);
				}
				else{
					plotPanel.addChildtoGraphicsGroup(surface2D);
				}
				plotPanel.resetPlotRotation();
				plotPanel.removeMouseRotate();
					
			}
			
		}
	}
	
	
	
	/**
	 * Resets the plot.
	 * @author Jamie Macaulay
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
	 * Open plot options.
	 * @author Jamie Macaulay
	 *
	 */
	class PlotOptions implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Point pt = new Point();
			pt = plotPanel.getLocationOnScreen();
			LikelihoodSurfaceParams newOptions = LikelihoodSurfaceDialog.showDialog(
					frame, pt, simplexPanelParams);
			
			if (newOptions!=null){
				simplexPanelParams=newOptions.clone();
			}
				try {
					rePaint3D();
				} catch (FunctionEvaluationException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
			}
			
		}

	}
	
	public void rePaint3D() throws FunctionEvaluationException, IllegalArgumentException{
		plotPanel.getGraphicsGroup().removeAllChildren();
		if (chiSquaredFunction==null ||location==null) return;
		createSurface(this.chiSquaredFunction,this.location);
	}
	
	public int[] calcDimensionArray(){
		int [] dimensions=new int[3];
		int n=0;
		for (int i=0; i<dimensions.length; i++){
			if (i!=simplexPanelParams.dim) {
				dimensions[n]=i;
				n++;
			}
		}
		
		dimensions[2]=simplexPanelParams.dim;
		
		return dimensions;
	}


	
	
	/**
	 * Calculates and displays a chi2 surface for an area around the location point.
	 * @param chiSquaredFunction
	 * @param location
	 * @param dimesnions which define the plane of the chi2 surface i.e. the spatial dimension which does NOT change; 
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public  ArrayList<ArrayList<Float>> createSurface(MultivariateRealFunction chiSquaredFunction, double[] location){

		///array containing the correct dimensions for surface()
		
		this.dimensions=new int[3];
		int n=0;
		for (int i=0; i<dimensions.length; i++){
			if (i!=simplexPanelParams.dim) {
				dimensions[n]=i;
				n++;
			}
		}
		
		this.dimensions[2]=simplexPanelParams.dim;
		
		//change the graph axis to the correct dimensions
		 setAxisLabels(dimensions);
		 // set the graph label
		 mouseGetVirtualLocation.mouseFunction();
		
		this.chiSquaredFunction=chiSquaredFunction;
		this.location=location;
		
		ArrayList<ArrayList<Float>> surfaceData=new ArrayList<ArrayList<Float>>();
		
		int size=(int) (simplexPanelParams.gridRange/simplexPanelParams.gridMeshSize);
		
		ArrayList<Float> oneLine;
		double[] gridPos=new double[3];
		float chiValue=0;
		float maxChi=0;
		float minChi=Float.MAX_VALUE;
		System.out.println("Location x: "+location[0]+"Location y: "+location[1]+"Location z: "+location[2]);
		//calc chi surface
		try{
		for (int i =-size; i<size; i++){
			oneLine=new ArrayList<Float>();
			for (int j=-size; j<size; j++){
				gridPos[dimensions[0]]=i*simplexPanelParams.gridMeshSize+location[dimensions[0]];
				gridPos[dimensions[1]]=j*simplexPanelParams.gridMeshSize+location[dimensions[1]];
				gridPos[dimensions[2]]=location[dimensions[2]];
				chiValue=(float) chiSquaredFunction.value(gridPos);
				if (chiValue>maxChi) maxChi=chiValue;
				if (chiValue<minChi) minChi=chiValue;
				oneLine.add((float) chiSquaredFunction.value(gridPos));
			}
			surfaceData.add(oneLine);
		}
		double[] p={60,60,-60};
		System.out.println("Chi Value at 60,60,60: "+chiSquaredFunction.value(p));
		}
		catch(Exception e){
			System.out.println("Error in caclulating chi squared surface for likelihood plot. This is likely due to an error in the MultivariateRealFunction class");
			e.printStackTrace();
		}

		
		if (maxChi>simplexPanelParams.maxChiValue) maxChi=simplexPanelParams.maxChiValue;
		System.out.println("MaxChi: "+maxChi);
		System.out.println("MinChi: "+minChi);

		//normalise
		ArrayList<ArrayList<Float>> surfaceNormalised=new ArrayList<ArrayList<Float>>();
		ArrayList<Float> oneLineNormalised;
		float chiNorm;
		for (int i =0; i<surfaceData.size(); i++){
			oneLineNormalised=new ArrayList<Float>();
			for (int j =0; j<surfaceData.get(i).size(); j++){
			chiNorm=surfaceData.get(i).get(j)/maxChi;
			if (chiNorm>1){oneLineNormalised.add(1f);}
			else{oneLineNormalised.add(chiNorm);}
			}
			surfaceNormalised.add(oneLineNormalised);
		}
	
		//find the correct location of grid in real space
		this.minX=location[dimensions[0]]-simplexPanelParams.gridRange;
		this.minY=location[dimensions[1]]-simplexPanelParams.gridRange;
		this.maxX=location[dimensions[0]]+simplexPanelParams.gridRange;
		this.maxY=location[dimensions[1]]+simplexPanelParams.gridRange;
		
		surface3D=null;
		surface2D=null;
		//create and display 3D components
		if (simplexPanelParams.is3D){
			System.out.println("Create 3D Simplex");
			surface3D=new BranchGroup();
			Surface3D surfaceShape3D=new Surface3D(surfaceNormalised, simplexPanelParams.is3D, j3DGraphSize,j3DGraphSize, simplexPanelParams.colourMap);
			surface3D.addChild(surfaceShape3D);
			surface3D.setCapability(BranchGroup.ALLOW_DETACH);
			plotPanel.addChildtoGraphicsGroup(surface3D);
		}
		else{
			surface2D=new BranchGroup();
			Surface3D surfaceShape2D=new Surface3D(surfaceNormalised, simplexPanelParams.is3D, j3DGraphSize,j3DGraphSize, simplexPanelParams.colourMap);
			surface2D.addChild(surfaceShape2D);
			surface2D.setCapability(BranchGroup.ALLOW_DETACH);
			plotPanel.addChildtoGraphicsGroup(surface2D);
		}

		return surfaceData;
	}
	
	class LikilihoodSurfaceAxis extends PamAxisPanel{
		
		public LikilihoodSurfaceAxis() {
			super();
			xAxis = new PamAxis(0, 0, 1, 1, 0, 1, false, "x (m)", "%.1f");
			yAxis = new PamAxis(0, 0, 1, 1, 0, 1, true, "y (m)", "%d");
			setSouthAxis(xAxis);
			setWestAxis(yAxis);
			this.SetBorderMins(10, 20, 10, 20);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
		}
		
		public void setPlotAxis(){
			
//			System.out.println("minX: "+ minX);
//			System.out.println("minY: "+ minY);
//			System.out.println("maxX: "+ maxX);
//			System.out.println("maxY: "+ maxY);
			
			double YBins=(maxY-minY)/j3DGraphSize;
			double Xbins=(maxX-minX)/j3DGraphSize;
			
			int xPixs = plotPanel.getWidth();
			int yPixs = plotPanel.getHeight();
			
			Point3d WindowMin=mouseGetVirtualLocation.getRealLoc(new Point(xPixs,yPixs));
			Point3d WindowMax=mouseGetVirtualLocation.getRealLoc(new Point(0,0));
			
			xAxis.setRange((WindowMax.getX()+j3DGraphSize/2)*Xbins+minX, (WindowMin.getX()+j3DGraphSize/2)*Xbins+minX);
			yAxis.setRange((WindowMin.getY()+j3DGraphSize/2)*YBins+minY, (WindowMax.getY()+j3DGraphSize/2)*YBins+minY);
			yAxis.setInterval((((WindowMax.getY()+j3DGraphSize/2)*YBins+minY)-((WindowMin.getY()+j3DGraphSize/2)*YBins+minY))/ 4);
			
			axisPanel.repaint();
		}


		public void setSpectrumXAxis() {
		}
		
		
		private void setSpectrumYAxis() {
		}
		
	}
	
	class SurfaceInfo extends PamBorderPanel {

		String emptyText = "Move cursor over plot for frequency information";
		public SurfaceInfo() {
			super();
			setLayout(new BorderLayout());
			add(BorderLayout.CENTER, cursorPos = new PamLabel(emptyText));
			setBorder(new EmptyBorder(new Insets(mainPanel.getInsets().left, 2, 2, 2)));
		}

	}
	
	private MultivariateRealFunction getChiSquaredFuntion(){
		return chiSquaredFunction;
	}
	private double[] getLocation(){
		return location;
	}
	
	private double getMinY(){
		return this.minY;
	}
	
	private double getMinX(){
		return this.minX;
	}
	

	public class MouseShowInfo extends MouseGetVirtualLocation {
		double xPos;
		double yPos;
		String xString;
		String yString;
		@Override
		public void mouseFunction(){
			 xPos=(this.x+j3DGraphSize/2)*((maxX-minX)/j3DGraphSize)+minX;
			 yPos=(this.y+j3DGraphSize/2)*((maxY-minY)/j3DGraphSize)+minY;
			 xString=String.format("%3.1f",xPos);
			 yString=String.format("%3.1f",yPos);
			
			String txt =  getAxisString(dimensions[0]) +": " + xString + "   "+ getAxisString(dimensions[1])+ ": "+  yString;
			cursorPos.setText(txt);
		}
	}
	
 } 
	
	
	
	

