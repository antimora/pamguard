package PamGraph3D;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Group;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Node;
import javax.media.j3d.Screen3D;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
	
import com.sun.j3d.utils.picking.*;

	
import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;


import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewInfo;
import com.sun.j3d.utils.universe.ViewingPlatform;

import PamView.PamPanel;
import PamView.PamColors.PamColor;

	/**
	 * A class to create the basic foundations needed to make a Java3D panel in Pamguard. Without any alterations this will appear as a blank white canvas. Mouse interaction behaviour and 3D shapes can be added easily using various functions. 
	 * @author Jamie Macaulay
	 * 
	 **/
	public class PamPanel3D extends PamPanel {
		
		
		private static final long serialVersionUID = 1L;
		
		// BranchGroups
		public BranchGroup rootGroup;
		
		public TransformGroup rotateGroup;
		
		public BranchGroup graphicsGroup;
		
		public BranchGroup graphGroup;
		
		public 	AxisVisualisation axisVis;
		

		//Rotation and interaction
		private MouseRotate myMouseRotate;
		
		private BranchGroup myMouseRotateG;

		private MouseWheelZoom myMouseZoom;
		
		private BranchGroup myMouseZoomG;

		private MouseTranslate myMouseTranslate;
		
		private BranchGroup myMouseTranslateG;

		private MouseProportionalZoom myMouseProportionalZoom;
		
		
		//3D Graphics environment
		protected Canvas3D canvas;
		protected PickCanvas pickCanvas;
		protected View view;
		protected ViewInfo viewInfo; 
		protected ViewingPlatform vip;
		protected GraphicsConfiguration config;
		protected GraphicsConfigTemplate3D template;
		SimpleUniverse u;
		public BoundingSphere bounds;
		Frame frame;
		Background background;
		BranchGroup light;
		
		
		//boolean
		boolean pickable=false;
		
		private boolean aspectRatio=false;

		
		//Common Colours
		Color3f red = new Color3f(1.0f, 0.0f, 0.0f);

		Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);

		Color3f green = new Color3f(0.0f, 1.0f, 0.0f);

		Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

		Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

		Color3f grey = new Color3f(0.5f, 0.5f, 0.5f);

		private OffScreenCanvas offScreenCanvas3D;


		private int destWidth=400;

		private int destHeight=400;

		

		/**
		 * Create a spectrogram
		 */
		public PamPanel3D(Frame frame){
			super(PamColor.PlOTWINDOW);
			//must include the frame or dual monitors are not supported.
			this.frame=frame;
			setLayout( new BorderLayout() );
			init();
			this.addComponentListener(new windowResize());

		}
		
		
		public PamPanel3D(Frame frame, boolean pickable ){
			super(PamColor.PlOTWINDOW);
			//must include the frame or dual monitors are not supported.
			this.frame=frame;
			setLayout( new BorderLayout() );
			this.pickable=pickable;
			init();
			this.addComponentListener(new windowResize());
		}

		@Override
		public void setBackground(Color arg0) {
			super.setBackground(arg0);
			if (canvas != null) {
				float[] colors=new float[4];
				Color3f c=new Color3f(arg0.getComponents(colors));
				background.setColor(c);
			}
		}


		/**Create the 3D components and 3D interaction components
		 * 
		 * @return Java3D BranchGroup.
		 */
		public BranchGroup createSpectroSceneGraph(){
			
			bounds=new BoundingSphere(new Point3d(1,1,1), Double.MAX_VALUE);

			rootGroup=new BranchGroup();
			rootGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			rootGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			rootGroup.setCapability( Group.ALLOW_CHILDREN_EXTEND );
			rootGroup.setCapability( Group.ALLOW_CHILDREN_READ );
			rootGroup.setCapability( Group.ALLOW_CHILDREN_WRITE );
			

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
			myMouseZoomG=new BranchGroup();
			myMouseZoomG.setCapability(BranchGroup.ALLOW_DETACH);
			myMouseZoomG.addChild(myMouseZoom);
			//rotateGroup.addChild(myMouseZoomG);

			myMouseTranslate = new MouseTranslate();
			myMouseTranslate.setFactor(1);
			myMouseTranslate.setTransformGroup(rotateGroup);
			myMouseTranslate.setSchedulingBounds(bounds);
			myMouseTranslateG=new BranchGroup();
			myMouseTranslateG.setCapability(BranchGroup.ALLOW_DETACH);
			myMouseTranslateG.addChild(myMouseTranslate);
			//rotateGroup.addChild(myMouseTranslateG);
			
			myMouseRotate = new MouseRotate();
			myMouseRotate.setTransformGroup(rotateGroup);
			myMouseRotate.setSchedulingBounds(bounds);
			myMouseRotateG=new BranchGroup();
			myMouseRotateG.setCapability(BranchGroup.ALLOW_DETACH);
			myMouseRotateG.addChild(myMouseRotate);
			

			myMouseProportionalZoom=new MouseProportionalZoom();
			myMouseProportionalZoom.setTransformGroup(rotateGroup);
			myMouseProportionalZoom.setMouseZoom(myMouseZoom);
			myMouseProportionalZoom.setMouseTranslate(myMouseTranslate);
			myMouseProportionalZoom.setSchedulingBounds(bounds);
			myMouseProportionalZoom.setZoomFactor();
			rotateGroup.addChild(myMouseProportionalZoom);

			graphicsGroup=new BranchGroup();
			graphicsGroup.setCapability( Group.ALLOW_CHILDREN_EXTEND );
			graphicsGroup.setCapability( Group.ALLOW_CHILDREN_READ );
			graphicsGroup.setCapability( Group.ALLOW_CHILDREN_WRITE );
			graphicsGroup.setCapability( BranchGroup.ALLOW_DETACH );
			graphicsGroup.setCapability(BranchGroup.ENABLE_PICK_REPORTING);

			rotateGroup.addChild(graphicsGroup);
			//addSpectrogram(false);
			//transformSpectrogram();
			
			background = new Background();
			background.setCapability(Background.ALLOW_COLOR_WRITE);
			background.setColor(1.0f, 1.0f, 1.0f);
			background.setApplicationBounds(bounds);
			
			DirectionalLight light1  = new DirectionalLight (white, new Vector3f(0.0f, 0.0f, -1000));
	    	light1.setInfluencingBounds (bounds);
	    	light=new BranchGroup();
	    	light.setCapability( Group.ALLOW_CHILDREN_EXTEND );
	    	light.setCapability( Group.ALLOW_CHILDREN_READ );
	    	light.setCapability( Group.ALLOW_CHILDREN_WRITE );
	    	light.setCapability( BranchGroup.ALLOW_DETACH );
	    	light.addChild(light1);
			
			rootGroup.addChild(light);
			rootGroup.addChild(background);
			rootGroup.addChild(rotateGroup);
						
			rootGroup.compile();
			return rootGroup;
		}
		
		/**Add a small icon to show the oreintation of the graph
		 * 
		 */
		public void addAxisVisulaisation(String axisX, String axisY, String axisZ){
		}
		
		public void removeAxisVisulaisation(){
		}
		
		public void setAxisVisTransform(){
		}
		
		
		class windowResize implements ComponentListener{
			
			public void componentResized(ComponentEvent e) {
			   //System.out.println("window resize");  
				if (aspectRatio==true){
					setAspectRatio();
				}
			 
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			// TODO Auto-generated method stub
			
			}

			@Override
			public void componentMoved(ComponentEvent arg0) {
			// TODO Auto-generated method stub
			
			}

			@Override
			public void componentShown(ComponentEvent arg0) {
			// TODO Auto-generated method stub
			
			}
				
		}
		
		/**
		 * Set the aspect ratio of the graph to match the containing window aspect ratio. Note that this only sets once. To follow window resizing use 'setAspectRatioEnable'
		 */
		public void setAspectRatio(){
			Dimension panelSize=this.getSize();
			double y=panelSize.getHeight();
			double x=panelSize.getWidth();
			//note. None of these values can be zero otherwise an exception will be thrown for some transforms
			this.transformAspectRatio(new Vector3d(1,y/x,1));
		}
		
		/**
		 * Enable ore disable 3D scaling with window aspect ratio
		 * @param aspectRatio
		 */
		public void setAspectRatioEnabled(boolean aspectRatio){
			this.aspectRatio=aspectRatio;
		}
		
	
	
		/**
		 * Remove lighting from root group. 
		 */
		public void removeLight(){
			rootGroup.removeChild(light);
			light=new BranchGroup();
			light.setCapability( Group.ALLOW_CHILDREN_EXTEND );
	    	light.setCapability( Group.ALLOW_CHILDREN_READ );
	    	light.setCapability( Group.ALLOW_CHILDREN_WRITE );
	    	light.setCapability( BranchGroup.ALLOW_DETACH );
			rootGroup.addChild(light);
		}
		
		/**
		 * Add lighting to the root group.
		 * @param light1
		 */
		public void addLight(DirectionalLight light1){
			BranchGroup lighttemp=new BranchGroup();
			lighttemp.setCapability( BranchGroup.ALLOW_DETACH );
			lighttemp.addChild(light1);
			light.addChild(lighttemp);
		}
		
		/**
		 * Get the rootGroup. This contains all other groups. Note this is no part of rottaeGroup and therefore any objects added directly will no have mouse functionality. 
		 * @return
		 */
		public BranchGroup getRootGroup(){
			return rootGroup;
		}
		

		/**
		 * Get the rotate group containing graphics and mouse interaction behaviours. 
		 */
		public TransformGroup getRotateGroup(){
		return this.rotateGroup;
		}
		
		/**
		 * Add a branchgroup to the rotateGroup
		 * @param branchGroup
		 */
		public void addChildtoRotateGroup(BranchGroup branchGroup){
			this.rotateGroup.addChild(branchGroup);
		}
		
		/**
		 * 
		 * Add a transform group to the rotateGroup

		 * @param tg
		 */
		public void addChildtoRotateGroup(TransformGroup tg){
			BranchGroup bg=new BranchGroup();
			bg.setCapability(BranchGroup.ALLOW_DETACH);
			bg.addChild(tg);
			this.rotateGroup.addChild(bg);
			}
		
	
		public void addChildtoGraphicsGroup(BranchGroup bg){
			this.graphicsGroup.addChild(bg);
			}
		
		public void addChildtoGraphicsGroup(TransformGroup tg){
			BranchGroup bg=new BranchGroup();
			bg.setCapability(BranchGroup.ALLOW_DETACH);
			bg.addChild(tg);
			this.graphicsGroup.addChild(bg);
			}
		
		
		public void clearGraphicsGroup(){
			graphicsGroup.removeAllChildren();
			}
		
		public void clearRotateGroup(){
			rotateGroup.removeAllChildren();
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
		 * Add mouse rotate functionality.
		 */
		public void addMouseRotate(){
			if (myMouseRotateG.getParent()!=rotateGroup){
			rotateGroup.addChild(myMouseRotateG);
			}
			
		}
		
		/**
		 * Add mouse translate functionality.
		 */
		public void addMouseTranslate(){
			if (myMouseTranslateG.getParent()!=rotateGroup){
			rotateGroup.addChild(myMouseTranslateG);
			}
			
		}
		
		/**
		 * Add mouse zoom functionality.
		 */
		public void addMouseZoom(){
			if (myMouseZoomG.getParent()!=rotateGroup){
			rotateGroup.addChild(myMouseZoomG);
			}
			
		}
		
		/**
		 * Remove the rotate functionality.
		 */
		public void removeMouseRotate(){
			rotateGroup.removeChild(myMouseRotateG);
		}
		
		/**
		 * Remove the translate functionality.
		 */
		public void removeMouseTranslate(){
			rotateGroup.removeChild(myMouseTranslateG);
		}
		
		/**
		 * Remove the mouse zoom functionality.
		 */
		public void removeMouseZoom(){
			rotateGroup.removeChild(myMouseZoomG);
		}
		
		public void setMouseZoomFactor(double factor){
			myMouseZoom.setFactor(factor);
			myMouseProportionalZoom.setZoomFactor();
		}
	
		
		public void setMouseTranslateFactor(double factor){
			myMouseTranslate.setFactor(factor);
		}

		
		/**
		 * Change the aspect ratio of the rotateGroup, preserving translation etc. 
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
			

			
			Transform3D t3d = new Transform3D();
			t3d.setScale( v );
			t3d.setRotation(rotM);
			t3d.setTranslation(translation);
	
			rotateGroup.setTransform(t3d);
			setAxisVisTransform();
		}
		
		/**
		 * Move the rotateGroup by vector3d v, maintaining rotation components.;
		 */
		public void transformTranslation(Vector3d v){
			Transform3D t3d = new Transform3D();
			rotateGroup.getTransform(t3d);
			t3d.setTranslation(v);
			rotateGroup.setTransform(t3d);
		}
		
		/**
		 * Move the graphics group by vector3d v;
		 */
		public void transformTranslationG(Vector3d v){
			Transform3D t3d = new Transform3D();
			rotateGroup.getTransform(t3d);
			t3d.setTranslation(v);
			rotateGroup.setTransform(t3d);
		}

		
		/**Resets rotateGroup with Z axis translation of Z preserving the aspect ratio.
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
			setAxisVisTransform();
		}
		
		/**
		 * Resets plot to default rotation, maintaining aspect ratio and translation components.
		 * 
		 */
		public void resetPlotRotation(){
			
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
			setAxisVisTransform();
		}
		
		/**Rotate the plot by specified angle;
		 * 
		 * @param anagle
		 */
		public void rotatePlot(double angle){
			Transform3D trs2D = new Transform3D();
			rotateGroup.getTransform(trs2D);
			rotateGroup.setTransform( rotate(angle,trs2D));
			setAxisVisTransform();
		}
		
		public Transform3D rotate(double RotateAngle, Transform3D trs2D){

			Transform3D Rot=new Transform3D() ;
			Rot.rotZ(RotateAngle);
			trs2D.mul(Rot);

			return trs2D;
		} 
		
		
		/**Create the 3D graph axis.
		 * 
		 * @return
		 */
		public BranchGroup getGraphicsGroup(){
			return graphicsGroup;
		}
		
		
		public static Node createPickableObject(Node node){
			node.setCapability(Shape3D.ENABLE_PICK_REPORTING);
			PickTool.setCapabilities(node, PickTool.INTERSECT_FULL);
			return node;
		} 
		
		public Canvas3D getCanvas3D(){
			return canvas;
		}
		
		public PickCanvas getPickCanvas(){
			return pickCanvas;
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
			
			try{
			config=	Pam3DGraphicsUtils.getBestConfigurationOnSameDevice(frame);
			}
			catch (Exception e){
			e.printStackTrace();
			}

			if (config==null){
			System.out.println("Warning: PamGraph3D: Best graphics config fail...attempting defaults");
			config = screen.getBestConfiguration(gcTemplate);
			}

			// Get a Canvas3D and call its constructor with the configuration	
			canvas = createCanvas3D(config);
			canvas.setMinimumSize(new Dimension(1,1));
			offScreenCanvas3D=createOffScreenCanvas(config);
						
			u = new SimpleUniverse(canvas);
	
			BranchGroup scene = createSpectroSceneGraph();
			
			if (scene == null) {
		         System.out.println("Error Pamgraph3D: No scenegraph to add to window");  
		           return;
		    }
			
			
			if (pickable==true){	
				 pickCanvas = new PickCanvas(canvas, graphicsGroup);
				 pickCanvas.setMode(PickCanvas.GEOMETRY);
			}
			
			vip=u.getViewingPlatform();
			vip.setNominalViewingTransform();
			u.addBranchGraph(scene);

			//View
			view = u.getViewer().getView();
			viewInfo = new ViewInfo(view);
			view.setBackClipDistance(100000);
			view.setDepthBufferFreezeTransparent(false);
			//view.setProjectionPolicy(View.HMD_VIEW  );
			//view.setWindowResizePolicy(View. PHYSICAL_WORLD );
			//view.setWindowEyepointPolicy(View.RELATIVE_TO_COEXISTENCE );

			//view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);
			view.setSceneAntialiasingEnable(true);
			
			//view.addCanvas3D(canvas);
			view.addCanvas3D(offScreenCanvas3D);
			
			add("Center", canvas);
			
			//System.out.println("CanvasSize: "+canvas.getSize().getHeight() );
		}
		
		private Canvas3D createCanvas3D(GraphicsConfiguration config){
			Canvas3D canvas = new Canvas3D(config);
			return canvas;
		}
		
		public View getView(){
			return view;
		}
		
		public MouseProportionalZoom getMouseProportionalZoom(){
			return myMouseProportionalZoom;
		}
		
		
		private OffScreenCanvas createOffScreenCanvas(GraphicsConfiguration configuration) {

			// Create the canvas to capture images.
			OffScreenCanvas canvas = new OffScreenCanvas(configuration);
			
			// Set the size, width and height so that off-screen renderer knows
			// at what dimensions it needs to render.
			
			Screen3D screen3D = canvas.getScreen3D();
			screen3D.setSize(destWidth, destHeight);
			screen3D.setPhysicalScreenWidth(destWidth);
			screen3D.setPhysicalScreenHeight(destHeight);

			
			return canvas;
		}

		
		/**
		 * Class for creating an offscreen canvas. This allows the 3D image to be rendered to a 2D buffered image for export.
		 * @author spn1
		 *
		 */
		class OffScreenCanvas extends Canvas3D{

			private static final long serialVersionUID = 1L;

			public OffScreenCanvas(GraphicsConfiguration graphicsConfiguration) {
				super(graphicsConfiguration, true);
			}

			public BufferedImage doRender(int width, int height) {

				BufferedImage bImage = new BufferedImage(width, height,
						BufferedImage.TYPE_INT_RGB);

				ImageComponent2D buffer = new ImageComponent2D(
						ImageComponent.FORMAT_RGB, bImage);
				buffer.setCapability(ImageComponent.ALLOW_FORMAT_READ);

				setOffScreenBuffer(buffer);
				renderOffScreenBuffer();
				waitForOffScreenRendering();
				bImage = getOffScreenBuffer().getImage();
				
				// Release the buffer
				setOffScreenBuffer(null);
				
				return bImage;
			}
			
		}
		
		public BufferedImage getBufferedImage(){
			if (super.getWidth()>0 && super.getHeight()>0){
				destWidth=super.getWidth();
				destHeight=super.getHeight();
			}
			BufferedImage output =  offScreenCanvas3D.doRender(destWidth, destHeight);
			return output;
		}
		

		
		
		
		
		

	}


		
		
		





