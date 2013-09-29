package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.Group;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import staticLocaliser.StaticLocalise;

import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;


import PamDetection.PamDetection;
import PamGraph3D.PamPanel3D;
import PamGraph3D.PamShapes3D;


/**
 * This displays previous locaisation results from the different algorithms. Each algorithm has it's own results symbol, this could  be a 3D symbol a bearing line, a hyprbole - any shape. The user can switch between results from different algorithms and pick
 * reuslts to see summary statistics. 
 * ****Currently not in use*******
 * @author Jamie Macaulay
 *
 */
public class LocalisationResultsGraph implements StaticDialogComponent{
	
	private StaticLocalisationMainPanel staticLocalisationDialog;
	
	//3D Components
	private PamPanel3D graphLocResults;
	private BranchGroup pickGroup;
	private BranchGroup graphicsGroup;
	
	//DialogComponents;
	JPanel locGraph;
	JPanel resultInfo;
	
	Appearance lineApp;
	
	
	
	public LocalisationResultsGraph(StaticLocalisationMainPanel staticLocalisationDialog){
	
		this.staticLocalisationDialog=staticLocalisationDialog;
		resultInfo=new JPanel();
		resultInfo.setBorder(new TitledBorder("Selected Result Info"));
		resultInfo.setPreferredSize(new Dimension(300,100));
		

		locGraph=new JPanel(new BorderLayout());
		locGraph.setBorder(new TitledBorder("Previous results"));
		locGraph.add(BorderLayout.CENTER, locResultsGraph());	
		//locGraph.add(BorderLayout.SOUTH, resultInfo);	
		
		graphicsGroup=new BranchGroup();
		graphicsGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		graphicsGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		graphicsGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		
		PamShapes3D.createSeaSurface(1000, PamShapes3D.blue);

		graphicsGroup.addChild(createAxis(100f, 100f, -100f, "x(m)","y(m)","z(m)",new Color3f(0.25f,0.25f,0.25f)));
		BranchGroup sea=PamShapes3D.createSeaSurface(100f,PamShapes3D.blue);	
		graphicsGroup.addChild(sea);
		
		graphLocResults.addChildtoRotateGroup(graphicsGroup);
		reset();	
	}
	
	
	/**Creates the Java3D component to display the time delay visualisation graph. 
	 * 
	 * @return
	 */
	public JPanel locResultsGraph(){
		
		Frame frame =staticLocalisationDialog.getStaticLocaliserControl().getPamView().getGuiFrame();
		
		graphLocResults=new PamPanel3D(frame,true);
	

		pickGroup=new BranchGroup();
		pickGroup.setCapability(BranchGroup.ALLOW_DETACH);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		pickGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		pickGroup.setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		graphLocResults.addChildtoGraphicsGroup(pickGroup);
		

		 //add a mouse listener to pick shapes
		graphLocResults.getCanvas3D().addMouseListener(new MousePick(graphLocResults.getPickCanvas()));
		graphLocResults.addMouseRotate();
		graphLocResults.addMouseZoom();
		graphLocResults.addMouseTranslate();

		 //add to JPanel
		 JPanel locResult3D=new JPanel(new BorderLayout());
		 locResult3D.add(BorderLayout.CENTER,graphLocResults);
		
		return locResult3D;
		
	}
	
	
	/**
	 * Create axis, there are 4 variables, x,y,z, and time
	 * @param Size
	 * @return
	 */
	public BranchGroup createAxis(float v1, float v2, float v3, String l1, String l2, String l3, Color3f col ){
		
		Font my2DFont = new Font(
			    "Arial",     // font name
			    Font.PLAIN,  // font style
			    1 );         // font size
			FontExtrusion myExtrude = new FontExtrusion( );
		Font3D my3DFont = new Font3D( my2DFont, myExtrude );

		//create BranchGroup
		BranchGroup axisGroup=new BranchGroup();
		lineApp=PamShapes3D.lineAppearance(2f, true, col); 
		//create Axis
		ArrayList<Point3f> axis=new ArrayList<Point3f>();
		axis.add(new Point3f(-v1,0,0));
		axis.add(new Point3f(v1,0,0));
		Shape3D axis1=PamShapes3D.linePolygon3D(axis, lineApp);
		axis=new ArrayList<Point3f>();
		axis.add(new Point3f(0,-v2,0));
		axis.add(new Point3f(0,v2,0));
		Shape3D axis2=PamShapes3D.linePolygon3D(axis, lineApp);
		axis=new ArrayList<Point3f>();
		axis.add(new Point3f(0,0,-v3));
		axis.add(new Point3f(0,0,v3));
		Shape3D axis3=PamShapes3D.linePolygon3D(axis, lineApp);
		
		//add Text
		String[] text={l1,l2,l3};
		Vector3f[] vector3f={new Vector3f((v1+(0.1f*v1)),0,0),new Vector3f(0,(v2+(0.1f*v2)),0),new Vector3f(0,0,(v3+(0.1f*v3)))};
		Text3D textObject;
		Transform3D textTranslation;
		for (int i=0; i<text.length;i++){
		 textObject = new Text3D();
		 textObject.setFont3D(my3DFont);
		 textObject.setString( text[i]);
		 
		 Shape3D myShape = new Shape3D( textObject, lineApp );
		    
				textTranslation = new Transform3D();
			    textTranslation.setTranslation(vector3f[i]);
			    textTranslation.setScale(8);
			    TransformGroup textTranslationGroup = new TransformGroup();
			    textTranslationGroup.setTransform(textTranslation);
			    textTranslationGroup.addChild(myShape);
			    axisGroup.addChild(textTranslationGroup);
		}
			    
		axisGroup.addChild(axis1);
		axisGroup.addChild(axis2);
		axisGroup.addChild(axis3);
	
		return axisGroup;
	}
	
	
	/**
	 * Allows picking of localisation results; 
	 * @author spn1
	 *
	 */
	private class MousePick extends MouseAdapter{
		
		PickCanvas pickCanvas;
		
		public MousePick(PickCanvas pickCanvas){
			this.pickCanvas=pickCanvas;
		}
		
		public void mouseClicked(MouseEvent e){

			pickCanvas.setShapeLocation(e.getX(),e.getY());
			PickResult result = pickCanvas.pickClosest();
			
			//return all shapes to blue
			if (result==null){
				
			}
		}
	}
	
	
	private void reset(){
		graphLocResults.resetPlot(-2*115.77);
	}
	
	

	@Override
	public JPanel getPanel() {
		// TODO Auto-generated method stub
		return locGraph;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {

	}


	@Override
	public void update(int flag) {
		//Add in the plot symbols.
	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticLocalisationDialog;
	}

}
