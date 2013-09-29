package PamGraph3D;

import java.awt.Font;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Sphere;

/**
 * Simple class to hold various useful 3D shapes for Pamguard.
 * @author Jamie Macaulay 
 *
 */
public class PamShapes3D  {
	
	//Common Colours
	public static Color3f red = new Color3f(1.0f, 0.0f, 0.0f);

	public static Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);

	public static Color3f green = new Color3f(0.0f, 1.0f, 0.0f);

	public static Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

	public static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

	public static Color3f grey = new Color3f(0.5f, 0.5f, 0.5f);
	
	public static Color3f purple = new Color3f(0.6f, 0f, 0.7f);
	
	
	public static BranchGroup createHydrophoneArray(ArrayList<Point3d> hydrophones){
		Transform3D transform;
		Sphere sphere ;
		TransformGroup tg;
		LineArray line;
		Shape3D line3D;
		
		BranchGroup hydrophoneArray=new BranchGroup();
		hydrophoneArray.setCapability(BranchGroup.ALLOW_DETACH);
		
		Appearance ap = sphereAppearance(new Color3f(0.5f,0.5f,0.5f));	
		
		Appearance lAp=lineAppearance(2,false, red);
		
		for (int i=0; i<hydrophones.size(); i++){
			tg=new TransformGroup();
			transform = new Transform3D();
			transform.setTranslation(new Vector3d(hydrophones.get(i).getX(),hydrophones.get(i).getY(),hydrophones.get(i).getZ()));
			line=new LineArray(2, GeometryArray.COORDINATES);
			line.setCoordinate(0,new Point3f((float) hydrophones.get(i).getX(),(float) hydrophones.get(i).getY(),0f));
			line.setCoordinate(1,new Point3f((float) hydrophones.get(i).getX(),(float) hydrophones.get(i).getY(),(float) hydrophones.get(i).getZ()));
			sphere = new Sphere(0.1f);
			sphere.setAppearance(ap);
			line3D = new Shape3D(line, lAp);
			tg.addChild(sphere);
			tg.setTransform(transform);
			
			hydrophoneArray.addChild(tg);
			hydrophoneArray.addChild(line3D);
		}
		

		return hydrophoneArray;
	}
	
	/**
	 * Create a sphere of appearance ap moved to a position specified by point3f. 
	 * @param point3f-the desired position of the sphere
	 * @param ap- the sphere appearance. 
	 * @param size- the size of the sphere.
	 * @return A transform group containing a sphere at a specified position. 
	 */
	public static TransformGroup createSphere(Point3f point3f, Appearance ap, float size){	
		Sphere sphere=new Sphere (size, Sphere.ENABLE_APPEARANCE_MODIFY|Sphere.GENERATE_NORMALS,
				null);
		TransformGroup tg=new TransformGroup();
		Transform3D transform = new Transform3D();
		transform.setTranslation(new Vector3d(point3f.getX(),point3f.getY(),point3f.getZ()));
		sphere.setAppearance(ap);
		tg.addChild(sphere);
		tg.setTransform(transform);
		return tg;
	}
	
	/**
	 * Create a sphere with appearance ap and specified size.
	 * @param ap- appearance of the sphere. 
	 * @param size - size of the sphere
	 * @return A Shape3D sphere. 
	 */
	public static Shape3D createSphere (Appearance ap, float size){	
		Sphere sphere=new Sphere (size, Sphere.ENABLE_APPEARANCE_MODIFY|Sphere.GENERATE_NORMALS,
				null);
		sphere.setAppearance(ap);
		Shape3D shape=new Shape3D();
		///must duplicate to avoid multiple parent exception. The sphere is the parent of the Shape3D. If the shape3D is simply extracted then the sphere is still the parent and an exception will occur if the shape3d is added to any group. Hence we must duplicate the shape 3D and get the function to return the duplicate
		shape.duplicateNode(sphere.getShape(), true);
		return shape;
	}
	
//	 public class PamSphere extends Sphere{
//		 public PamSphere(float size){
//		 super(size, Sphere.ENABLE_APPEARANCE_MODIFY | Sphere.GENERATE_NORMALS ,
//				 null);
//		 }
//	}
	
	/**
	 * Creates an appearance. This appearance allows interaction with lighting effects. This can be used with other 3D shapes if required. 
	 * @param col- colour of the sphere.
	 * @return. 
	 */
	public static Appearance sphereAppearance(Color3f col){
		Appearance ap = new Appearance();
		Material mat = new Material();
		mat.setAmbientColor(new Color3f(0.0f,1.0f,1.0f));
		mat.setDiffuseColor(col);
		mat.setSpecularColor(col);
		ap.setMaterial(mat);
		ColoringAttributes ca = new ColoringAttributes(col, ColoringAttributes.NICEST);	
		ap.setColoringAttributes(ca);	
		return ap;
	}
	
	/**
	 * Creates a sphere appearance. This appearance does not allow for interaction with lighting effects. This can be used with other 3D shapes if required.
	 * @param col
	 * @return
	 */
	public static Appearance sphereAppearanceMatt(Color3f col){
		Appearance ap = new Appearance();
		ColoringAttributes ca = new ColoringAttributes(col, ColoringAttributes.NICEST);	
		ap.setColoringAttributes(ca);	
		return ap;
	}
	
	
	
	
	/**
	 * Creates a semi-transparent square surface of specified width and colour. Useful to show sea surfaces. 
	 * @param width- width of surface.
	 * @param colour-colour of surface
	 * @return BranchGroup containing translucent square surface. 
	 */
	public static BranchGroup createSeaSurface(float width,Color3f colour){

		BranchGroup SeaSurface=new BranchGroup();
		SeaSurface.setCapability(BranchGroup.ALLOW_DETACH);
		QuadArray polygon1 = new QuadArray (4,GeometryArray.COORDINATES);
		polygon1.setCoordinate (0, new Point3f (-width, -width, 1f));
		polygon1.setCoordinate (1, new Point3f (width, -width, 1f));
		polygon1.setCoordinate (2, new Point3f (width, width, 1f));
		polygon1.setCoordinate (3, new Point3f (-width, width, 1f));

		Material mat = new Material();

		mat.setAmbientColor(new Color3f(0.0f,1.0f,1.0f));
		mat.setDiffuseColor(colour);
		mat.setSpecularColor(colour);
	
		Appearance polygon1Appearance = new Appearance();
		
		polygon1Appearance.setMaterial(mat);
		PolygonAttributes pl= new PolygonAttributes();
		pl.setBackFaceNormalFlip(true);
		pl.setCullFace(0);

		ColoringAttributes ca = new ColoringAttributes(colour, ColoringAttributes.NICEST);

		TransparencyAttributes trans=new TransparencyAttributes(TransparencyAttributes.NICEST,0.7f);

		polygon1Appearance.setPolygonAttributes(pl);
		polygon1Appearance.setColoringAttributes(ca);
		polygon1Appearance.setTransparencyAttributes(trans);
		Shape3D Polygon=new Shape3D(polygon1,polygon1Appearance );
		SeaSurface.addChild(Polygon);

		return SeaSurface;
	}
	
	
	/** Creates a 3D cross, with each axis of the cross the size specified by x, y and z. The appearance of the cross is determined by lineApp
	 * 
	 * @param x- size of the cross in x
	 * @param y- size of the cross in y
	 * @param z- size of the cross in z
	 * @param lineapp - appearance of the cross. 
	 * @return
	 */
	public static Shape3D create3DCross(float x, float y, float z, Appearance lineApp){
		
		ArrayList<Point3f> data=new ArrayList<Point3f>();
		
				data.add(new Point3f(-x,0,0));
				data.add(new Point3f(x,0,0));
				data.add(new Point3f(0,-y,0));
				data.add(new Point3f(0,y,0));
				data.add(new Point3f(0,0,z));
				data.add(new Point3f(0,0,-z));
				
				LineArray DataPoints=new LineArray(data.size(), LineArray.COORDINATES);
				for (int i=0; i<data.size(); i++){
					DataPoints.setCoordinate((i),data.get(i));
				}
				
				Shape3D axis3=new Shape3D(DataPoints, lineApp);
				
				return axis3;
		
	}


	/**Specific function for setting the appearance of a line. Note if using this in conjuction with transparant shapes 
	 * @param LineWidth
	 * @param AntiAliasing
 	* @param colour
 	* @return
 	*/
	public static Appearance lineAppearance(float LineWidth, boolean AntiAliasing, Color3f colour){
		Appearance app = new Appearance();

		//Line width and dots
		LineAttributes dotLa = new LineAttributes(LineWidth,0,AntiAliasing);

		//Transparency
//		TransparencyAttributes trans=new TransparencyAttributes();
//		trans.setTransparencyMode(TransparencyAttributes.NICEST);
//		trans.setTransparency (0.3f);
//		TransparencyAttributes trans=new TransparencyAttributes(TransparencyAttributes.NICEST,0.5f);
//		ap.setTransparencyAttributes(trans);

		//Colouring
		ColoringAttributes ca = new ColoringAttributes(colour, ColoringAttributes.FASTEST);

		//Apply to appearance
		app.setColoringAttributes(ca);
		app.setLineAttributes(dotLa);
//		app.setTransparencyAttributes(trans);

		return app; 
	}


	/**
	 * Creates a random colour of blue.
	 * 
	 * @return a random shade of blue. 
	 */
	public static Color3f randomBlue(){
		Double bl= Math.random();
		if (bl<0.5){
			bl=bl*2;
		}
		//System.out.println("blue"+bl);
		float blueC=bl.floatValue();

		return new Color3f(0.0f,0.0f,blueC);
	}
		

	/**
	 * Creates a joined series of lines specified by an array of 3D points. 
	 * @param Data= an array of 3D points
	 * @param Line appearance
	 * @return
	 */
	public static Shape3D linePolygon3D(ArrayList<Point3f> Data, Appearance app){

		LineArray DataPoints=new LineArray(Data.size()*2, GeometryArray.COORDINATES);

		for (int i=0; i<Data.size()-1; i++){
			DataPoints.setCoordinate((2*i),Data.get(i));
			DataPoints.setCoordinate((2*i+1),Data.get(i+1));
		}
	
		Shape3D DataShape = new Shape3D(DataPoints, app);
		return DataShape;
	}
	
	/**
	 * Creates a series of points specified by an array of 3d points of appearance ap
	 * @param Data. An array of the points to create the shape3D. 
	 * @param app. Appearance of the point array. 
	 * @return
	 */
	public static Shape3D pointArray3D(ArrayList<Point3f> Data, Appearance app){

		PointArray pointArray = new PointArray(Data.size(),
		        GeometryArray.COORDINATES);

		for (int i=0; i<Data.size(); i++){
			pointArray.setCoordinate(i,Data.get(i));
		}
	
		Shape3D DataShape = new Shape3D(pointArray, app);
		return DataShape;
	}
	
	/**
	 * Create an appearance for a pointArray. Use with pointArray3D.
	 * @param size- size of each point within point array.
	 * @param colour-colour of each point within point array.
	 * @return
	 */
	public static Appearance PointAppearance(float size, Color3f colour){
		Appearance pointApp = new Appearance();
		pointApp.setPointAttributes(new PointAttributes(size, false));
		ColoringAttributes ca = new ColoringAttributes(colour, ColoringAttributes.FASTEST);
		pointApp.setColoringAttributes(ca);
		
		return pointApp;
	}
	

	
	

	
	/**
	 * Creates a set of labelled axis showing each 3D dimension.
	 * @param v1-x axis size
	 * @param v2-y axis size
	 * @param v3-z axis size-often depth
	 * @param l1- x axis text
	 * @param l2- y axis text
	 * @param l3- z axis text
	 * @param col-colour
	 * @return branch group containing axis shapes
	 */
	public static BranchGroup createAxis(float v1, float v2, float v3, float textSize, String l1, String l2, String l3, Color3f col ){
		
		Font my2DFont = new Font(
			    "Arial",     // font name
			    Font.PLAIN,  // font style
			    1 );         // font size
			FontExtrusion myExtrude = new FontExtrusion( );
		Font3D my3DFont = new Font3D( my2DFont, myExtrude );

		//create BranchGroup
		BranchGroup axisGroup=new BranchGroup();
		Appearance lineApp=PamShapes3D.lineAppearance(2f, false, col); 
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
		Vector3f[] vector3fText={new Vector3f((v1+(0.1f*v1)),0,0),new Vector3f(0,(v2+(0.1f*v2)),0),new Vector3f(0,0,-(v3+(0.1f*v3)))};
		Text3D textObject;
		Transform3D textTranslation;
		for (int i=0; i<text.length;i++){
		 textObject = new Text3D();
		 textObject.setFont3D(my3DFont);
		 textObject.setString( text[i]);
		 
		 Shape3D myShape = new Shape3D( textObject, lineApp );
		    
				textTranslation = new Transform3D();
			    textTranslation.setTranslation(vector3fText[i]);
			    textTranslation.setScale(textSize);
			    TransformGroup textTranslationGroup = new TransformGroup();
			    textTranslationGroup.setTransform(textTranslation);
			    textTranslationGroup.addChild(myShape);
			    axisGroup.addChild(textTranslationGroup);
		}
		
		//add arrows
		Vector3f[] vector3fCone={new Vector3f(v1,0,0),new Vector3f(0,v2,0),new Vector3f(0,0,-v3)};
		
		for (int i=0; i<text.length;i++){
			
					Cone cone=new Cone(v1/20, v1/10);
					cone.setAppearance(sphereAppearanceMatt(grey));
			 		Transform3D coneTranslation = new Transform3D();
				    if (i==0) coneTranslation.rotZ(3*Math.PI/2);
				    if (i==2) coneTranslation.rotX(3*Math.PI/2);
				    coneTranslation.setTranslation(vector3fCone[i]);
				    TransformGroup coneTranslationGroup = new TransformGroup(coneTranslation);
				    coneTranslationGroup.addChild(cone);
				    axisGroup.addChild(coneTranslationGroup);
		}
		
		//tick Marks
			    
		axisGroup.addChild(axis1);
		axisGroup.addChild(axis2);
		axisGroup.addChild(axis3);
		axisGroup.setCapability(BranchGroup.ALLOW_DETACH);
	
		return axisGroup;
	}


	

}



