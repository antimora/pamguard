package PamGraph3D.spectrogram3D;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.media.j3d.Background;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewInfo;
import com.sun.j3d.utils.universe.ViewingPlatform;

import PamController.PamController;
import PamGraph3D.MouseGetVirtualLocation;
import PamGraph3D.MouseProportionalZoom;
import PamGraph3D.MouseRightClickMenu;
import PamGraph3D.Pam3DGraphicsUtils;
import PamView.PamPanel;
import PamView.ColourArray.ColourArrayType;
import PamView.PamColors.PamColor;

public class Spectrogram3DPamGraph extends PamPanel {
	
	
	private static final long serialVersionUID = 1L;
	
	// BranchGroups
	public BranchGroup rootGroup;
	
	public TransformGroup rotateGroup;
	
	public BranchGroup spectrogramGroup;
	
	public BranchGroup graphGroup;
	
	public BranchGroup spectro2D=null;
	
	public BranchGroup spectro3D=null;
	
	public ColourArrayType colour=ColourArrayType.GREY;
	
	
	//Rotation and interaction
	private MouseRotate myMouseRotate;
	
	private BranchGroup myMouseRotateG;

	private MouseWheelZoom myMouseZoom;

	private MouseTranslate myMouseTranslate;

	private MouseProportionalZoom myMouseProportionalZoom;
	
	private MouseRightClickMenu myMouseMenuRightClick;
	
	
	//3D Graphics environment
	protected Canvas3D canvas;
	protected View view;
	protected ViewInfo viewInfo; 
	protected ViewingPlatform vip;
	protected GraphicsConfiguration config;
	protected GraphicsConfigTemplate3D template;
	SimpleUniverse u;
	public BoundingSphere bounds;
	Frame frame;
	
	
	
	
	//Common Colours
	Color3f red = new Color3f(1.0f, 0.0f, 0.0f);

	Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);

	Color3f green = new Color3f(0.0f, 1.0f, 0.0f);

	Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

	Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

	Color3f grey = new Color3f(0.5f, 0.5f, 0.5f);
	

	/**
	 * Create a spectrogram
	 */
	public Spectrogram3DPamGraph(Frame frame){
		super(PamColor.PlOTWINDOW);
		//must include the frame or dual monitors are not supported.
		this.frame=frame;
		setLayout( new BorderLayout() );
		init();
	}

	/**
	 * Generate a spectrogram of random data/noise. This is purely for testing purposes
	 * @param bins
	 * @param fftSize
	 * @return. 2D ArrayList of random float values.
	 */
	public ArrayList<ArrayList<Float>> generateTestData(int bins, int fftSize){
		
		ArrayList<ArrayList<Float>> specData=new ArrayList<ArrayList<Float>>();

		for (int i=0; i<bins; i++){
			ArrayList<Float> fftData=new ArrayList<Float>();
			for (int j=0; j<fftSize; j++){
				Double rand=Math.random();
				fftData.add(rand.floatValue());
			}
			specData.add(fftData);
		}
		
		return specData;
	}
	
	/**Create the 3D components and 3D interaction components
	 * 
	 * @return Java3D BranchGroup.
	 */
	public BranchGroup createSpectroSceneGraph(){
		
		bounds=new BoundingSphere(new Point3d(1,1,1), Double.MAX_VALUE);

		rootGroup=new BranchGroup();

		rotateGroup = new TransformGroup();
		rotateGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		rotateGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		rotateGroup.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		rotateGroup.setCapability( Group.ALLOW_CHILDREN_READ );
		rotateGroup.setCapability( Group.ALLOW_CHILDREN_WRITE );

		myMouseZoom = new MouseWheelZoom();
		myMouseZoom.setFactor(5);
		myMouseZoom.setTransformGroup(rotateGroup);
		myMouseZoom.setSchedulingBounds(bounds);
		rotateGroup.addChild(myMouseZoom);

		myMouseTranslate = new MouseTranslate();
		myMouseTranslate.setFactor(5);
		myMouseTranslate.setTransformGroup(rotateGroup);
		myMouseTranslate.setSchedulingBounds(bounds);
		rotateGroup.addChild(myMouseTranslate);
		
		myMouseRotate = new MouseRotate();
		myMouseRotate.setTransformGroup(rotateGroup);
		myMouseRotate.setSchedulingBounds(bounds);
		myMouseRotateG=new BranchGroup();
		myMouseRotateG.setCapability(BranchGroup.ALLOW_DETACH);
		myMouseRotateG.addChild(myMouseRotate);
		//rotateGroup.addChild(myMouseRotateG);
		
		/**MouseGetVirtualLocation myMousePoint=new MouseGetVirtualLocation(rotateGroup);
		myMousePoint.setCanvas3D(canvas);
		myMousePoint.setAspectRatioPolicy(true);
		myMousePoint.setSchedulingBounds(bounds);
		rotateGroup.addChild(myMousePoint);**/
		
		/**MouseMove myMousePoint=new MouseMove(rotateGroup);
		myMousePoint.setSchedulingBounds(bounds);
		rotateGroup.addChild(myMousePoint);**/
	
		/**myMouseMenuRightClick=new MouseRightClickMenu();
		myMouseMenuRightClick.setTransformGroup(rotateGroup);
		myMouseMenuRightClick.setMenu(new JPopupMenu());
		myMouseMenuRightClick.setMainPanel(this);
		myMouseMenuRightClick.setSchedulingBounds(bounds);
		rotateGroup.addChild(myMouseMenuRightClick);**/

		myMouseProportionalZoom=new MouseProportionalZoom();
		myMouseProportionalZoom.setTransformGroup(rotateGroup);
		myMouseProportionalZoom.setMouseZoom(myMouseZoom);
		myMouseProportionalZoom.setMouseTranslate(myMouseTranslate);
		myMouseProportionalZoom.setSchedulingBounds(bounds);
		rotateGroup.addChild(myMouseProportionalZoom);

		spectrogramGroup=new BranchGroup();
		spectrogramGroup.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		spectrogramGroup.setCapability( Group.ALLOW_CHILDREN_READ );
		spectrogramGroup.setCapability( Group.ALLOW_CHILDREN_WRITE );
		spectrogramGroup.setCapability( BranchGroup.ALLOW_DETACH );

		rotateGroup.addChild(spectrogramGroup);
		//addSpectrogram(false);
		//transformSpectrogram();
		
		Background background = new Background();
		background.setColor(1.0f, 1.0f, 1.0f);
		background.setApplicationBounds(bounds);
		
		DirectionalLight light1  = new DirectionalLight (white, new Vector3f(0.0f, 0.0f, -1000f));
    	light1.setInfluencingBounds (bounds);
		
		rootGroup.addChild(light1);
		rootGroup.addChild(background);
		rootGroup.addChild(rotateGroup);
		
		rootGroup.compile();
		return rootGroup;
	}
	
	
	/**
	 * Add a spectrogram to the graph.
	 * @param threeD
	 */
	public void addSpectrogram(boolean threeD){
		System.out.println("Create spectrogram");
		Surface3D spectro=new Surface3D(generateTestData(512,500),threeD,100,200,ColourArrayType.GREY);
		BranchGroup spectroBranch=new BranchGroup();
		spectroBranch.addChild(spectro);
		spectrogramGroup.addChild(spectroBranch);
	}
	
	public void createSpectrograms(ArrayList<ArrayList<Float>> fftData, ColourArrayType colours){
		System.out.println("Create spectrogram");
		 this.spectro2D=new BranchGroup();
		 this.spectro3D=new BranchGroup();	
		 spectro2D.setCapability(BranchGroup.ALLOW_DETACH);
		 spectro3D.setCapability(BranchGroup.ALLOW_DETACH);
		 this.spectro2D.addChild(new Surface3D(fftData,false,100,100,colours));
		 this.spectro3D.addChild(new Surface3D(fftData,true,100,100,colours));
	}
	
	
	
	/**
	 * Get the rotate group containing the spectrogram and mouse interaction behaviours. 
	 */
	public TransformGroup getSpectroRotateGroup(){
	return this.rotateGroup;
	}
	
	
	public void addChildSpectroRotateGroup(BranchGroup branchGroup){
		this.rotateGroup.addChild(branchGroup);
		}
	

	public void addMouseRightClickMenu(MouseBehavior mouserightClick){
		mouserightClick.setTransformGroup(this.rotateGroup);
		BranchGroup mouserightBranch=new BranchGroup();
		mouserightClick.setSchedulingBounds(bounds);
		mouserightBranch.addChild(mouserightClick);
		this.rotateGroup.addChild(mouserightBranch);
		}
	
	public void addMouseGetVirtualLocation(MouseGetVirtualLocation mouseGetVirtualLocation){
		BranchGroup mouseLocBranch=new BranchGroup();
		mouseGetVirtualLocation.setTransformGroup(this.rotateGroup);
		mouseGetVirtualLocation.setCanvas3D(canvas);
		mouseGetVirtualLocation.setAspectRatioPolicy(true);
		mouseGetVirtualLocation.setSchedulingBounds(bounds);
		mouseLocBranch.addChild(mouseGetVirtualLocation);
		this.rotateGroup.addChild(mouseLocBranch);
	}
	
	
	
	
	
	
	/**
	 * Adds a spectrogram to the graph
	 */
	public void addSpectrogram2D( ){
		if (spectro2D!=null){
		spectrogramGroup.addChild(spectro2D);
		}
	}
	
	public void addSpectrogram3D( ){
		if (spectro3D!=null){
		spectrogramGroup.addChild(spectro3D);
		}
	}
	

	/**
	 * Remove all spectrograms from graph.
	 */
	public void removeSpectrograms(){
		spectrogramGroup.removeAllChildren();
	}
	
	/**
	 * Remove the 3D spectrogram from the Graph;
	 */
	public void removeSpectrogram3D(){
		spectrogramGroup.removeChild(spectro3D);
	}
	
	/**
	 * Remove the 2D spectrogram from the graph
	 */
	public void removeSpectrogram2D(){
		spectrogramGroup.removeChild(spectro2D);
	}
	
	/**
	 * Add mouse rotate functionality 
	 */
	public void addMouseRotate(){
		if (myMouseRotateG.getParent()!=rotateGroup){
		rotateGroup.addChild(myMouseRotateG);
		}
		
	}
	
	/**
	 * Remove the rotate functionality 
	 */
	public void removeMouseRotate(){
		rotateGroup.removeChild(myMouseRotateG);
	}

	
	
	/**
	 * Change the aspect ratio of the spectrogram.
	 */
	public void transformAspectRatio(Vector3d v){
		
		Transform3D trs2D = new Transform3D();
		rotateGroup.getTransform(trs2D);
		
		Vector3f translation = new Vector3f();
		//Quat4f rot=new Quat4f();
		//Note- have to use Matrix 3f due to a NaN non affine transform exception
		Matrix3f rotM=new Matrix3f();
		trs2D.get(rotM);
		trs2D.get(translation);
		
		//translation.setX(-50);
		//translation.setY(-50);
		
		Transform3D t3d = new Transform3D();
//		System.out.println("x: "+v.getX()+"y: "+v.getY()+"z: "+v.getZ());
//		System.out.println("Translation: " +translation);
//		System.out.println("rot: " +rotM);
		t3d.setScale( v );
		t3d.setTranslation(translation);
		t3d.setRotation(rotM);
		
		
		
		rotateGroup.setTransform(t3d);
	}
	
	/**
	 * Move the graph by vector3d v;
	 */
	public void transformTranslation(Vector3d v){
		Transform3D t3d = new Transform3D();
		rotateGroup.getTransform(t3d);
		t3d.setTranslation(v);
		rotateGroup.setTransform(t3d);
	}
	
	/**Resets plot with Z axis translation of Z preserving the aspect ratio.
	 * 
	 * @param Z
	 */
	public void resetPlot(double Z){
		Transform3D t3d = new Transform3D();
		Vector3d scale= new Vector3d();
		Vector3d trans=new Vector3d(0,0,Z);
		rotateGroup.getTransform(t3d);
		t3d.getScale(scale);
		
		t3d.rotY(0);
		t3d.rotX(0);
		t3d.rotZ(0);
		t3d.setScale(scale);
		t3d.setTranslation(trans);
		
		rotateGroup.setTransform(t3d);
	}
	
	/**
	 * Resets plot to default rotation, maintaining aspect ratio and translation components.
	 * 
	 */
	public void resetPlot(){
		
		Transform3D t3d = new Transform3D();
		Vector3d scale= new Vector3d();
		Vector3d trans=new Vector3d();
		
		rotateGroup.getTransform(t3d);
		t3d.getScale(scale);
		t3d.get(trans);
		
		t3d.rotY(0);
		t3d.rotX(0);
		t3d.rotZ(0);
		t3d.setScale(scale);
		t3d.setTranslation(trans);
		
		rotateGroup.setTransform(t3d);
	}

	
	/**Create the 3D graph axis.
	 * 
	 * @return
	 */
	public BranchGroup createGraphComponents(){
		return graphGroup;
	}
	

	/**
	 * Initialise the 3D Graph.
	 */
	public void init(){  
		
	    setLayout(new BorderLayout());
		//Create scene
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);

		// get an Canvas3d
		GraphicsConfigTemplate3D gcTemplate = new GraphicsConfigTemplate3D();
		
		
		
		GraphicsEnvironment local = GraphicsEnvironment.getLocalGraphicsEnvironment();
		
		// there could be more than one screen
		GraphicsDevice screen = local.getDefaultScreenDevice();
		//config = screen.getBestConfiguration(gcTemplate);
		
		config=	Pam3DGraphicsUtils.getBestConfigurationOnSameDevice(frame);
		if (config==null){
		System.out.println("Best graphics config fail...attempting defaults");
		config = screen.getBestConfiguration(gcTemplate);
		}
		
		// Get a Canvas3D and call its constructor with the configuration	
		canvas = new Canvas3D(config);
		
		System.out.println("Screen Height: "+canvas.getScreen3D().getPhysicalScreenHeight());
		System.out.println("Screen Width: "+canvas.getScreen3D().getPhysicalScreenWidth());
	
		//canvas.getScreen3D().setPhysicalScreenWidth(canvas.getPhysicalWidth() );
		
		u = new SimpleUniverse(canvas);
		
		BranchGroup scene = createSpectroSceneGraph();
		if (scene == null) {
	         System.out.println("Error Pamgraph3D: No scenegraph to add to window");  
	            return;
	    }
		
		vip=u.getViewingPlatform();
		vip.setNominalViewingTransform();
		u.addBranchGraph(scene);

		//View
		view = u.getViewer().getView();
		viewInfo = new ViewInfo(view);
		view.setBackClipDistance(10000);
		view.setDepthBufferFreezeTransparent(false);
		//view.setScreenScalePolicy(View.SCALE_EXPLICIT  );
		//view.setWindowResizePolicy(View. PHYSICAL_WORLD );
		//view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
		//view.setSceneAntialiasingEnable(true);

		add("Center", canvas);
		
		//System.out.println("CanvasSize: "+canvas.getSize().getHeight() );
	}
	

}


	
	
	


