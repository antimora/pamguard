package PamGraph3D;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.universe.ViewInfo;

/**
 * This class finds the virtual location of a mouse on the canvas. Takes into account how far the canvas has been zoomed. Use this to create dynamic axis or mouse location indicators in Java3D components. 
 * @author Jamie Macaulay
 *
 */
public class MouseGetVirtualLocation extends MouseBehavior {
	
	Canvas3D canvas;
	View view;
	Transform3D current=new Transform3D();
	boolean aspectRatio=true;
	
	Vector3d scalevec;
	double fieldofView;
	protected double x;
	protected double y;
	
	Point p1;
	
	public MouseGetVirtualLocation(TransformGroup transformGroup, Canvas3D canvas, boolean aspectRatio) {
		super (transformGroup);
		this.canvas=canvas;
		this.view=canvas.getView();
		this.aspectRatio=aspectRatio;
	}

	public MouseGetVirtualLocation(TransformGroup transformGroup) {
		super (transformGroup);
	}
	
	public MouseGetVirtualLocation() {
		super (0);
	}
	
	
	public void setCanvas3D(Canvas3D canvas){
			this.canvas=canvas;
			this.view=canvas.getView();
	}
	
	public void setAspectRatioPolicy(boolean aspectRatio){
		this.aspectRatio=aspectRatio;
}

	@Override
	public void initialize() {
		// set initial wakeup condition
		this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_MOVED));
	}

	@Override
	public void processStimulus(Enumeration criteria) {

		WakeupCriterion wakeup =
			(WakeupCriterion)criteria.nextElement();
		if (wakeup instanceof WakeupOnAWTEvent)
		{
			AWTEvent[] events =
				((WakeupOnAWTEvent)wakeup).getAWTEvent();
			if (events.length > 0 && events[events.length-1] instanceof
					MouseEvent){
				
				MouseEvent event = (MouseEvent)events[events.length-1];
				
				p1=event.getPoint();
				Point3d loc=getRealLoc(p1);
				
				x=loc.getX();
				y=loc.getY();
				
				mouseFunction();
				
				//System.out.println("x: "+x+" y: "+y);
				
			}   	  
			
		}
		this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_MOVED));
		
	}
	
	/**
	 * Converts a pixel point on the screen to a real world co-ordinate, taking into account zoom and translation effects. Use this as a communication link between 2D and 3D java components
	 * Be aware of aspect ratio. Make sure to set this mouse listener to aspect ration=true if any scale operations are performed on the corresponding transform group. 
	 * 
	 * @param p1
	 * @return
	 */
	public Point3d getRealLoc(Point p1){
		
		super.transformGroup.getTransform(current);
		scalevec=new Vector3d();
		current.get(scalevec);
		fieldofView=view.getFieldOfView();
		Point3d coOrd;
		Point xyzMeters;
		double x;
		double y;
		double ratio=1;
		Dimension s;
		
		s=canvas.getSize();     
		coOrd=translatePoint(canvas, p1);
		
		if (aspectRatio==true){
		ratio=s.getWidth()/s.getHeight();
		}
		
//		System.out.println("field of View: "+fieldofView);
//		System.out.println("Z: "+scalevec.getZ());
//		System.out.println("Co Ord: "+coOrd);
		
		xyzMeters=converttoxyMeters(coOrd, fieldofView, scalevec.getZ(), ratio);
		x=xyzMeters.getX()-scalevec.getX();
		y=xyzMeters.getY()-scalevec.getY()*ratio;
		
		return new Point3d(x,y,scalevec.getZ());
	}
	
	
	/**Converts virtual world points to real meters.
	 * 
	 * @param translatePoint
	 * @param fieldofView
	 * @param translateHeight
	 * @return A real world Point in meters.
	 */
	public  Point converttoxyMeters(Point3d translatePoint, double fieldofView, double translateHeight, double ratio){
		double x;
		double y;
		Point p=new Point();

		x=-(translatePoint.getX()*Math.tan( fieldofView/2.0)*translateHeight);//*(50/9.45);
		y=-(translatePoint.getY()*Math.tan( fieldofView/2.0)*translateHeight)*ratio;//*(50/9.35);
		p.setLocation(x, y);

		return p;
	}
	
	/**Translates pixels to virtual world Points
	 * 
	 * @param myCanvas
	 * @param viewInfo
	 * @param awtPoint
	 * @return
	 */
	public  Point3d translatePoint(Canvas3D myCanvas, Point awtPoint) {

		// construct an empty point
		Point3d p3 = new Point3d();

		// after this, p3 will contain the 'metres' distance of awtPoint
		// from the origin of the image plate
		canvas.getPixelLocationInImagePlate(awtPoint.x,awtPoint.y,p3);
		

		// construct an empty transform to hold our plate location
		Transform3D toVirtualUniverse = new Transform3D();

		// stores the transform from the image plate to the virtual world
		canvas.getImagePlateToVworld(toVirtualUniverse);
		
		// apply the image plate transform to p3 - p3 is now awtPoint in
		// virtual world coordinates!
		toVirtualUniverse.transform(p3);

		return p3;
	}
	
	public void mouseFunction(){
		
	}
	
	public Point3d getRealMouseLoc(){
		return new Point3d(x,y,scalevec.getZ());
	}
	
	public Point getPixelLocation(){
		return p1;
	}

	
}