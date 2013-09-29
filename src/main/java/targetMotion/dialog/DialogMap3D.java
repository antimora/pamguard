package targetMotion.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewInfo;
import com.sun.j3d.utils.universe.ViewingPlatform;

import pamMaths.PamVector;

import targetMotion.EventRotator;
import targetMotion.TargetMotionLocaliser;
import targetMotion.TargetMotionResult;
import GPS.GPSDataBlock;
import GPS.GpsDataUnit;
import PamController.PamController;
import PamDetection.PamDetection;
import PamGraph3D.MouseRightClickMenu;
import PamGraph3D.Pam3DGraphicsUtils;
import PamGraph3D.PamPanel3D;
import PamGraph3D.PamShapes3D;
import PamUtils.LatLong;
import PamView.PamSymbol;
import PamView.PopupTextField;

public class DialogMap3D<T extends PamDetection> extends DialogMap<T>  {


	//the Swing JPanel
	private JPanel mainPanel;
	private PamPanel3D plotPanel;
	JLabel cursorLabel;

	
	//3D Graphics environment
	protected Canvas3D canvas;
	protected View view;
	protected ViewInfo viewInfo; 
	protected ViewingPlatform vip;
	protected GraphicsConfiguration config;
	protected GraphicsConfigTemplate3D template;
	SimpleUniverse u;

	
	//Rotation and interaction
	private TransformGroup rotateGroup;


	//3D Objects
	float range=1000;

	float lastrange=range;
	
	double resetZoom=-5000;

	BranchGroup Rootgroup;

	BranchGroup graphicsGroup;

	BranchGroup locGroup;

	Appearance appLocVectors0;
	Appearance appLocVectors1;

	Appearance appGPStrack;

	Appearance appHydroArray;


	//Events
	int lastEvent=-2;

	long lastupdateTime;

	PamSymbol originSymbol, resultSymbol;

	private EventRotator eventRotator;



	//Common Colours
	Color3f red = new Color3f(1.0f, 0.0f, 0.0f);

	Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);

	Color3f green = new Color3f(0.0f, 1.0f, 0.0f);

	Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

	Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

	Color3f grey = new Color3f(0.5f, 0.5f, 0.5f);

	TransparencyAttributes trans=new TransparencyAttributes(TransparencyAttributes.NICEST,0.5f);
	private BranchGroup seaSurface;




	public DialogMap3D(TargetMotionLocaliser<T> targetMotionLocaliser,
			TargetMotionDialog<T> targetMotionDialog) {
		super(targetMotionLocaliser, targetMotionDialog);

		this.targetMotionLocaliser=targetMotionLocaliser;

		appLocVectors0=setLineAppearance(2, false, red);
		appLocVectors1=setLineAppearance(2, false, green);
		appGPStrack=setLineAppearance(2, false, grey);
		appHydroArray=setLineAppearance(2, false, blue);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		plotPanel = new PamPanel3D((Frame) targetMotionDialog.getOwner());
		
		plotPanel.addMouseRotate();
		plotPanel.addMouseTranslate();
		plotPanel.addMouseZoom();
		
		MShowPopUpMenu mouseRightClick=new MShowPopUpMenu();
		mouseRightClick.setMainPanel(plotPanel);
		plotPanel.addMouseRightClickMenu(mouseRightClick);

		plotPanel.resetPlot(resetZoom);
		plotPanel.setPreferredSize(new Dimension(700, 400));
		

		//add some of the 3D components
		rotateGroup=plotPanel.getRotateGroup();
		
		//create the graphics group Group
		graphicsGroup=new BranchGroup();
		graphicsGroup.setCapability( BranchGroup.ALLOW_DETACH );
		graphicsGroup.setCapability( BranchGroup.ALLOW_CHILDREN_READ );
		graphicsGroup.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		graphicsGroup.setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );
		seaSurface=PamShapes3D.createSeaSurface(range, blue);
		graphicsGroup.addChild(seaSurface);

		
		//create group to hold localisation shapes
		locGroup=new BranchGroup();
		locGroup.setCapability( BranchGroup.ALLOW_DETACH );
		locGroup.setCapability( BranchGroup.ALLOW_CHILDREN_READ );
		locGroup.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		locGroup.setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );

		rotateGroup.addChild(locGroup);
		rotateGroup.addChild(graphicsGroup);
		
		mainPanel.add(BorderLayout.CENTER, plotPanel);


	}
	
	
	/**
	 * Show pop up menu.
	 * @author Jamie Macaulay.
	 *
	 */
	public class MShowPopUpMenu extends MouseRightClickMenu {
		@Override
		public void showPopupMenu(JPanel mainPanel, Point point) {
			JPopupMenu menu=createPopupMenu();
			menu.setLightWeightPopupEnabled(false);
			//System.out.println("Mouse right click");
			menu.show(mainPanel, point.x, point.y);
		}
	} 


	public JPopupMenu createPopupMenu() {
		//System.out.println("menu");

		JPopupMenu menu = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("Reset plot");
		menuItem.addActionListener(new ResetPlot());
		menu.add(menuItem);

		menuItem = new JMenuItem("Rotate plot");
		menuItem.addActionListener(new RotatePlot());
		menu.add(menuItem);

		menuItem = new JMenuItem("Set line length");
		menuItem.addActionListener(new LineLength());
		menu.add(menuItem);
		
		menu.setLightWeightPopupEnabled(false);
		
		return menu;

	}



	/**translates pixels to virtual world Points
	 * 
	 * @param myCanvas
	 * @param viewInfo
	 * @param awtPoint
	 * @return
	 */
	public  Point3d translatePoint(Canvas3D myCanvas,  ViewInfo viewInfo, Point awtPoint) {

		// construct an empty point
		Point3d p3 = new Point3d();

		// after this, p3 will contain the 'metres' distance of awtPoint
		// from the origin of the image plate
		viewInfo.getPixelLocationInImagePlate(myCanvas,awtPoint.x,awtPoint.y,p3);

		// construct an empty transform to hold our plate location
		Transform3D toVirtualUniverse = new Transform3D();

		// stores the transform from the image plate to the virtual world
		viewInfo.getImagePlateToVworld(myCanvas,toVirtualUniverse,null);

		// apply the image plate transform to p3 - p3 is now awtPoint in
		// virtual world coordinates!
		toVirtualUniverse.transform(p3);

		return p3;
	}


	/**Converts virtual world Co-Ordinates to real meters.
	 * 
	 * @param translatePoint
	 * @param fieldofView
	 * @param translateHeight
	 * @return
	 */
	public  Point converttoxyMeters(Point3d translatePoint, double fieldofView, double translateHeight){
		double x;
		double y;
		Point p=new Point();

		x=-(translatePoint.getX()*Math.tan( fieldofView/2)*translateHeight);
		y=-(translatePoint.getY()*Math.tan( fieldofView/2)*translateHeight);
		p.setLocation(x, y);

		return p;

	}
	

	class MapSettingsAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			settings();
		}
	}

	/////Right click menu Action Listeners/////
	class ResetPlot implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			rotateGroup.setTransform( returnto2DView());
		}

	}


	class RotatePlot implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Transform3D trs2D = new Transform3D();
			rotateGroup.getTransform(trs2D);
			rotateGroup.setTransform( rotate(-Math.PI/2,trs2D));
		}

	}
	

	class LineLength implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			setLineLength();
			updateGraph();
		}
	}


	public Transform3D returnto2DView(){

		Transform3D trs2D = new Transform3D();
		trs2D.setTranslation( new Vector3d( 0.0, 0.0, resetZoom) );
		rotate(Math.PI/2,trs2D);

		return trs2D;
	} 
	

	public Transform3D rotate(double RotateAngle, Transform3D trs2D){

		Transform3D Rot=new Transform3D() ;
		Rot.rotZ(RotateAngle);
		trs2D.mul(Rot);

		return trs2D;
	} 


	/**
	 * Creates a text box at the mouse position on the canvas3D. Input changes the range variable altering the size of localisation vectors. 
	 */
	private void setLineLength() {

		Point pt=plotPanel.getCanvas3D().getMousePosition();
		SwingUtilities.convertPointToScreen(pt, plotPanel);
		Double newVal = PopupTextField.getValue(plotPanel, "Length(m)", pt, getRange());
		if (newVal != null) {
			setRange(newVal.floatValue());
		}

	}
	

	/**Sets the range altering the size of the localisation vectors.
	 * 
	 * @param range
	 */
	private void setRange(float range) {
		this.range = range;
	}
	

	/**returns the value of the range which is used to change the size of localisation vectors
	 * 
	 * @return
	 */
	private double getRange() {
		return this.range ;
	}


	/** Updates the object and loc group depending on whether the event or localisation results have updated. 
	 * 
	 */
	protected void updateGraph() {
		
		System.out.println("UpdateGraph");
		
		BranchGroup GPSTrack=null;
		BranchGroup LocVectors=null;
		
		try {
			
			T currentEvent = targetMotionLocaliser.getCurrentEvent();
			int currentEventIndex = targetMotionLocaliser.getCurrentEventIndex();
			
			Float rangeSea=range;
			float rangeSeaf=rangeSea.floatValue()*2;

			System.out.println("Event:"+ currentEventIndex);
		
			
			if (targetMotionLocaliser.getResults()==null){
				System.out.println("Graph3D: Results are null;");
				locGroup.removeAllChildren();
				return;
			}
			
			
			if (currentEvent == null) {
				System.out.println("Graph3D: Current event is null;");
				return;
			}
			
			
			eventRotator = new EventRotator(currentEvent);
			
			if (eventRotator == null) {
				System.out.println("Graph3D: Event rotator is null;");
				return;
			}
			
		
			if (range!=lastrange){
				
				LocVectors=createLocVectors(currentEvent,appLocVectors0,appLocVectors1);
				GPSTrack=createGPSTrack(currentEvent,appGPStrack);
				
				graphicsGroup.removeAllChildren();
				graphicsGroup.addChild(createSeaSurface(rangeSeaf));
				if (LocVectors!=null){
					graphicsGroup.addChild(LocVectors);
					}
				if (GPSTrack!=null){
					graphicsGroup.addChild(GPSTrack);
					}
				lastrange=range;
				
				return;
			}
			
			//the event has been changed. 
			if (lastEvent!=currentEventIndex){
				lastEvent = currentEventIndex;
				try {
					//last event does not equal this event
					System.out.println("Add Object Group;");
					
					//remove any localisation results
					locGroup.removeAllChildren();
					
					LocVectors=createLocVectors(currentEvent,appLocVectors0,appLocVectors1);
					GPSTrack=createGPSTrack(currentEvent,appGPStrack);
					
					graphicsGroup.removeAllChildren();
					graphicsGroup.addChild(createSeaSurface(rangeSeaf));
					if (LocVectors!=null){
					graphicsGroup.addChild(LocVectors);
					}
					if (GPSTrack!=null){
					graphicsGroup.addChild(GPSTrack);
					}
				}

				catch (Exception e) {
					System.out.println("Graph3D: Could not add GPS or localisation vectors.");
					e.printStackTrace();
				}
			}

			
			if (currentEventIndex == lastEvent && targetMotionLocaliser.getResults().size()!=0 && targetMotionLocaliser.getResults()!=null) {
				// try to add results
				System.out.println("Add Loc Group;");
					locGroup.removeAllChildren();
					locGroup.addChild(plotResults(currentEvent,appHydroArray));


				return;
			}
			
		}
		catch (Exception e) {
			System.out.println("Graph3D: Update failure.");
			e.printStackTrace();
		}
	}


	/**
	 * Creates the localisation vectors in 3D space.
	 * @param currentEvent
	 * @param app
	 * @return
	 */
	public BranchGroup createLocVectors(T currentEvent, Appearance appLocVectors0, Appearance appLocVectors1){

		BranchGroup LocVectors=new BranchGroup();
		LocVectors.setCapability( BranchGroup.ALLOW_DETACH );

		PamVector[][] worldVectors=eventRotator.getRotatedWorldVectors();
		PamVector[] subDetectionOrigins=eventRotator.getRotatedOrigins();

		if (worldVectors==null || subDetectionOrigins==null){
			System.out.println("No data to construct bearing vectors.");
			return null;
		}

		Point3d Origin;
		Point3f Origin3f;
		Point3d End;
		Point3f End3f;

		for(int j=0; j<subDetectionOrigins.length;j++){

			Shape3D Line3D=new Shape3D();
			
			//System.out.println(subDetectionOrigins[j]);

			Origin=new Point3d(subDetectionOrigins[j].getVector()[0], subDetectionOrigins[j].getVector()[1], subDetectionOrigins[j].getVector()[2]);
			Origin3f=Pam3DGraphicsUtils.convertto3f(Origin)	;

			for (int n=0; n<worldVectors[0].length; n++){
				ArrayList<Point3f> Line=new ArrayList<Point3f>();

				End=new Point3d(range*(worldVectors[j][n].getVector()[0])+Origin.getX(), range*(worldVectors[j][n].getVector()[1])+Origin.getY(),range*(worldVectors[j][n].getVector()[2])+Origin.getZ());

				End3f=Pam3DGraphicsUtils.convertto3f(End);
				Line.add(Origin3f);
				Line.add(End3f);

				if (n%2==1){
					Line3D=addData(Line,appLocVectors0);
				}
				else{
					Line3D=addData(Line,appLocVectors1);
				}
				LocVectors.addChild(Line3D);
			}
		}
		return LocVectors;
	}




	public BranchGroup createArray(T currentEvent, Appearance app){
		return null;
	}


	/**Creates a random colour of blue for each series of Markov chain jumps.
	 * 
	 * @return
	 */
	public Color3f randomBlue(){
		Double bl= Math.random();
		if (bl<0.5){
			bl=bl*2;
		}
		//System.out.println("blue"+bl);
		float blueC=bl.floatValue();

		return new Color3f(0.0f,0.0f,blueC);
		
	}

	
	/**Iterates through results and creates a branchgroup of 3D shapes. For MCMC the jump series represents the localisation. For all other localisations a sphere transformed 
	 * to correspond to the 3D or 2D error is drawn. Add to this function to create more shapes. 
	 * @param currentEvent
	 * @param app
	 * @return
	 */
	private BranchGroup plotResults(T currentEvent, Appearance app) {

		BranchGroup LocGroup=new BranchGroup();
		LocGroup.setCapability( BranchGroup.ALLOW_DETACH );

		//target motion results is an arraylist of target motion result objects. 
		ArrayList<TargetMotionResult> results = targetMotionLocaliser.getResults();
	

		if (results==null || results.size()==0){
			System.out.println("Null results");
			return null;
		}
		if (results.get(0) == null) {
			System.out.println("Null results");
			return null;
		}

		for (int i=0; i<results.size(); i++){


			if (results.get(i).getMCMCJumps()!=null){
				System.out.println("MCMC results: "+i);

				ArrayList<ArrayList<Point3f>> jumps=results.get(i).getMCMCJumps();

				for (int k=0; k<jumps.size();k++){

					ArrayList<Point3f> Jumps3f= new ArrayList<Point3f>();
					
					for (int l=0; l<jumps.get(k).size(); l++){
						Point3f Data3f=jumps.get(k).get(l);
						Jumps3f.add(Data3f);	

					}
					
					System.out.println("Add MCMC results");

					Color3f blueC=randomBlue();
					app=setLineAppearance(1, false, blueC);
					//app.setTransparencyAttributes(trans);

					Shape3D shape3d=addData(Jumps3f,app);
					LocGroup.addChild(shape3d);
				}
			}


			else {
				
				TransformGroup Symbol3D;
				System.out.println("Symbol3D add: "+i);

				LatLong locl1=results.get(i).getLatLong();
				PamVector locPt=eventRotator.latLongToMetres(locl1,true);
				Double x=locPt.getVector()[0];
				Double y=locPt.getVector()[1];
				Double z=locPt.getVector()[2];
				double[] errors = new double[3];
				
				for (int e = 0; e < 3; e++) {
					errors[e] = results.get(i).getError(e);	
					if (Double.isNaN(errors[e])) {
						errors[e] = 0;
					}
				}
				
				Symbol3D = results.get(i).getModel().getPlotSymbol3D(new Vector3f(x.floatValue(),y.floatValue(),z.floatValue()), 
						errors, 2.5);
				if (Symbol3D!=null){
					LocGroup.addChild(Symbol3D);
					}
			}
		}
		System.out.println("Return results");
		return LocGroup;
	}


	/** Creates BranchGroup which holds a translucent square with vertices of 2*width. Used to create a reference point for the sea surface in 3D maps.
	 * 
	 * @param width
	 * @return
	 */
	public BranchGroup createSeaSurface(float width){
		
		BranchGroup SeaSurface=new BranchGroup();
		SeaSurface.setCapability(BranchGroup.ALLOW_DETACH);
		QuadArray polygon1 = new QuadArray (4,GeometryArray.COORDINATES);
		polygon1.setCoordinate (0, new Point3f (-width, -width, 1f));
		polygon1.setCoordinate (1, new Point3f (width, -width, 1f));
		polygon1.setCoordinate (2, new Point3f (width, width, 1f));
		polygon1.setCoordinate (3, new Point3f (-width, width, 1f));

		Material mat = new Material();

		mat.setAmbientColor(new Color3f(0.0f,1.0f,1.0f));
		mat.setDiffuseColor(blue);
		mat.setSpecularColor(blue);
	
		Appearance polygon1Appearance = new Appearance();
		
		polygon1Appearance.setMaterial(mat);
		PolygonAttributes pl= new PolygonAttributes();
		pl.setBackFaceNormalFlip(true);
		pl.setCullFace(0);

		ColoringAttributes ca = new ColoringAttributes(blue, ColoringAttributes.NICEST);

		TransparencyAttributes trans=new TransparencyAttributes(TransparencyAttributes.NICEST,0.7f);

		polygon1Appearance.setPolygonAttributes(pl);
		polygon1Appearance.setColoringAttributes(ca);
		polygon1Appearance.setTransparencyAttributes(trans);
		Shape3D Polygon=new Shape3D(polygon1,polygon1Appearance );
		SeaSurface.addChild(Polygon);

		return SeaSurface;
	}
	

	/**Converts a LatLong to a position a Point3f rotated using the event rotator.
	 * 
	 * @param GPSpoint
	 * @return
	 */
	public Point3f calcGPSPos3D(LatLong GPSpoint){
		
		Double x, y, z;
		float xf,yf,zf;
		//need to make sure this is directional 

		PamVector GPSxyz = eventRotator.latLongToMetres(GPSpoint,true);
		
		x=GPSxyz.getVector()[0];
		y=GPSxyz.getVector()[1];
		z=  GPSpoint.getHeight();
		
		xf=x.floatValue();
		yf=y.floatValue();
		zf=z.floatValue();

		Point3f pt = new Point3f(xf,yf,zf);
		return pt;

	}

	
	/**Creates a 3D Shape of a GPS track with Appearance app.
	 * 
	 * @param currentEvent
	 * @param app
	 * @return
	 */
	private BranchGroup createGPSTrack(T currentEvent, Appearance app) {

		BranchGroup GPSData=new BranchGroup();
		GPSData.setCapability( BranchGroup.ALLOW_DETACH );


		GPSDataBlock gpsDataBlock = (GPSDataBlock) PamController.getInstance().getDataBlock(GpsDataUnit.class, 0);
		if (gpsDataBlock == null) {
			return null;
		}

		if (eventRotator==null){
			System.out.println("GPS track. Event rotator is null.");
			return null;
		}


		Point3f pt1;
		ArrayList<Point3f> GPSTrack3D= new ArrayList<Point3f>();
		GpsDataUnit gpsDataUnit;

		ListIterator<GpsDataUnit> li = gpsDataBlock.getListIterator(0);

		//System.out.println(li);
		if (li.hasNext()){
			while (li.hasNext()) {

				gpsDataUnit = li.next();

				pt1=calcGPSPos3D(gpsDataUnit.getGpsData());
			

				if (pt1 != null) {
					GPSTrack3D.add(pt1);
				}
			}
		}
		else{
		return null;
		}

		Shape3D shape3d=addData(GPSTrack3D, app);

		GPSData.addChild(shape3d);

		return GPSData;

	}
	

	@Override
	public void setCurrentEventIndex(int eventIndex, Object sender) {
		updateGraph();		

	}

	@Override
	public boolean canRun() {
		return true;
	}

	@Override
	public void enableControls() {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyNewResults() {
		long start = System.nanoTime();    
		updateGraph();		
		long elapsedTime = System.nanoTime() - start;
		System.out.println("Graph load time (s): "+elapsedTime/1000/1000/1000);
	}

	
	/**Creates a 3D shape of points joined together by a line of Appearance app
	 * 
	 * @param Data
	 * @param app
	 * @return
	 */
	Shape3D addData(ArrayList<Point3f> Data, Appearance app){

		LineArray DataPoints=new LineArray(Data.size()*2, GeometryArray.COORDINATES);

		for (int i=0; i<Data.size()-1; i++){
			DataPoints.setCoordinate((2*i),Data.get(i));
			DataPoints.setCoordinate((2*i+1),Data.get(i+1));
		}
		
		Shape3D DataShape = new Shape3D(DataPoints, app);
		return DataShape;
	}


	/**Specific function for setting the appearance of a line
	 * 
	 * @param LineWidth
	 * @param AntiAliasing
	 * @param Colour
	 * @return
	 */
	Appearance setLineAppearance(float LineWidth, boolean AntiAliasing, Color3f Colour){
		Appearance app = new Appearance();

		//Line width and dots
		LineAttributes dotLa = new LineAttributes(LineWidth,0,AntiAliasing);

		//Transparency
		//TransparencyAttributes trans=new TransparencyAttributes();
		//trans.setTransparencyMode(TransparencyAttributes.NICEST);
		//trans.setTransparency (Transparancy);

		//Colouring
		ColoringAttributes ca = new ColoringAttributes(Colour, ColoringAttributes.NICEST);

		//Apply to appearance
		app.setColoringAttributes(ca);
		app.setLineAttributes(dotLa);
		//app.setTransparencyAttributes(trans);

		return app; 
	}
	

	@Override
	public void showMap(boolean b) {
		//System.out.println("AntiAliasing: "+u.getCanvas().getView().getSceneAntialiasingEnable());
		//config =getBestConfigurationOnSameDevice((Frame) targetMotionDialog.getOwner() );
		//canvas = new Canvas3D(config);
		updateGraph();
	}


	@Override
	public void settings() {
		// TODO Auto-generated method stub
	}


	@Override
	public JPanel getPanel() {
		// TODO Auto-generated method stub
		return mainPanel;
	}
}
