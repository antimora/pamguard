package PamGraph3D;

import java.awt.Frame;
import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Geometry;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import javax.media.j3d.Node;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;

import com.sun.j3d.utils.picking.PickTool;

/**
 * Contains utility functions used to help implement 3D in Pamguard.
 * @author Jamie Macaulay
 *
 */
public class Pam3DGraphicsUtils {
	
	/**
	 * Gets the best graphics conifguration to display on the current device.
	 * This prevents issues with dual monitor displays
	 *
	 * @param  frame that the Canvas3D will be added to. 
	 */
	public static  GraphicsConfiguration getBestConfigurationOnSameDevice(Frame frame){

		GraphicsConfiguration gc = frame.getGraphicsConfiguration();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		GraphicsConfiguration good = null;

		GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();
		
		gct.setSceneAntialiasing(GraphicsConfigTemplate.REQUIRED);

		for(GraphicsDevice gd: gs){
			
				gct.setStereo(GraphicsConfigTemplate.PREFERRED);
			
			
			if(gd==gc.getDevice()){
				good = gct.getBestConfiguration(gd.getConfigurations());
				if(good!=null)
					break;
			}
		}

		return good;
	}
	
	/**
	 * Convert a Point3d to a Point3f.
	 * @param point3d
	 * @return Point3f converstion of Point3d.
	 */
	public static Point3f convertto3f(Point3d point3d){
		Double x=point3d.getX();
		Double y=point3d.getY();
		Double z=point3d.getZ();
		Float xf=x.floatValue();
		Float yf=y.floatValue();
		Float zf=z.floatValue();
		
		return new Point3f(xf,yf,zf);
	}
	
	
	/**
	 * Convert a point3d array to a point3f array. 
	 * @param point3d
	 * @return
	 */
	public static ArrayList<Point3f> convertToPoint3f(ArrayList<Point3d> point3d){
		ArrayList<Point3f> point3fs=new ArrayList<Point3f>();
		Point3f point3f;
		for (int i=0; i<point3d.size(); i++){
			point3f=new Point3f();
			point3f.setX((float) (point3d.get(i).getX()));
			point3f.setY((float) (point3d.get(i).getY()));
			point3f.setZ((float) (point3d.get(i).getZ()));
			point3fs.add(point3f);
		}
		return point3fs;
	}
	
	
	/**
	 * Convert a point3d array to a point3f array. 
	 * @param point3d
	 * @return
	 */
	public static ArrayList<Point3d> convertToPoint3d(ArrayList<Point3f> point3fs){
		ArrayList<Point3d> point3ds=new ArrayList<Point3d>();
		Point3d point3d;
		for (int i=0; i<point3fs.size(); i++){
			point3d=new Point3d();
			point3d.setX((double) (point3fs.get(i).getX()));
			point3d.setY((double) (point3fs.get(i).getY()));
			point3d.setZ((double) (point3fs.get(i).getZ()));
			point3ds.add(point3d);
		}
		return point3ds;
	}
	
	
	
	/**Enables branchgroup picking of Shape3D's.
	 * 
	 * @param node
	 */
	public static void enablePicking(Node node) {

	    node.setPickable(true);

	    node.setCapability(Node.ENABLE_PICK_REPORTING);

	    try {

	       Group group = (Group) node;

	       for (Enumeration e = group.getAllChildren(); e.hasMoreElements();) {

	          enablePicking((Node)e.nextElement());

	       }

	    }

	    catch(ClassCastException e) {

	        // if not a group node, there are no children so ignore exception

	    }

	    try {

	          Shape3D shape = (Shape3D) node;

	          PickTool.setCapabilities(node, PickTool.INTERSECT_FULL);

	          for (Enumeration e = shape.getAllGeometries(); e.hasMoreElements();) {

	             Geometry g = (Geometry)e.nextElement();

	             g.setCapability(g.ALLOW_INTERSECT);

	          }

	       }

	    catch(ClassCastException e) {

	       // not a Shape3D node ignore exception

	    }

	}
	

}
