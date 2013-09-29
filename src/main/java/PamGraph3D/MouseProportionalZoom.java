package PamGraph3D;

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Enumeration;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;

/**
 * Class which allows for proportional mouse zoom and translate functions. 
 * @author Jamie Macaulay;
 *
 */
public class MouseProportionalZoom extends MouseBehavior {
	
	MouseWheelZoom myMouseZoom=null;
	MouseTranslate myMouseTranslate=null;
	double proportionalityFactor=0.01;
	double minZoomFactor=0.5;
	
	public MouseProportionalZoom(TransformGroup transformGroup,  MouseWheelZoom myMouseZoom, MouseTranslate myMouseTranslate) {
		
		super (transformGroup);
		this.myMouseZoom=myMouseZoom;
		this.myMouseTranslate=myMouseTranslate;
		
	}
	
	public MouseProportionalZoom() {
		super (0);
	}

	@Override
	public void initialize() {
		this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL));
	}

	public void mouseWheelMoved(MouseWheelEvent e, double proportionalityFactor) {
		
		int notches = e.getWheelRotation();
		
		setZoomFactor();

	}
	
	/**
	 * Finds how far the graph is zoomed out and proportionally adjusts the zoom factor. 
	 */
	public void setZoomFactor(){
		
		double factor;

		Vector3d scalevec=new Vector3d();
		Transform3D current = new Transform3D();
		super.transformGroup.getTransform(current);
		current.get(scalevec);
		
		factor=-scalevec.getZ()*proportionalityFactor;

			if (factor>minZoomFactor){
				myMouseZoom.setFactor(factor);
				if (myMouseTranslate!=null){
				myMouseTranslate.setFactor(factor*0.5,factor*0.5);
				}
			}
			
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

				MouseWheelEvent event = (MouseWheelEvent)events[events.length-1];
				mouseWheelMoved(event,proportionalityFactor); 
			}   	  
		}
		this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL));
	}
	
	
	public void setProportionalityFactor(double factor){
		this.proportionalityFactor=factor;
	}
	
	public void setMinZoomFactor(double factor){
		this.minZoomFactor=factor;
	}
	
	public void setMouseZoom(MouseWheelZoom mousewheelZoom){
		this.myMouseZoom=mousewheelZoom;
		}
	
	public void setMouseTranslate( MouseTranslate mouseTranslate){
		this.myMouseTranslate=mouseTranslate;	
		}
	
}
