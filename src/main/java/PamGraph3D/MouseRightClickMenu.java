
package PamGraph3D;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;


import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;

/**
	 * Attach this to a TransformGroup or BranchGroup in order to return a menu or other action if the right or middle mouse is clicked. 
	 * @author Jamie Macaulay
	 *
	 */
	public class MouseRightClickMenu extends MouseBehavior {
		
		JPanel mainPanel;
		
		public MouseRightClickMenu(TransformGroup transformGroup) {
			super (transformGroup);
		}
		
		public MouseRightClickMenu() {
			super (0);
		}
		
		public MouseRightClickMenu(TransformGroup transformGroup, JPanel mainPanel) {
			super (transformGroup);
			this.mainPanel=mainPanel;
		}
		
	
		@Override
		public void initialize() {
			// set initial wakeup condition
			this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_CLICKED));
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

					if  (event.getButton()==MouseEvent.BUTTON2 || event.getButton()==MouseEvent.BUTTON3 ){
						showPopupMenu(this.mainPanel, event.getPoint());
					}
				}   	  
			}
			this.wakeupOn(new WakeupOnAWTEvent(MouseEvent.MOUSE_CLICKED));
		}
		

		public void showPopupMenu(JPanel mainPanel, Point point) {
	
		}
		
		
		public void setMainPanel(JPanel mainPanel ){
			this.mainPanel=mainPanel;
		}
		
		public JPanel getMainPanel(  ){
			return mainPanel;
		}

		
	}