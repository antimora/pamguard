package PamGraph3D;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;

import PamView.PamPanel;

public class AxisVisualisation {
	
	PamPanel3D axisVis3D;
	BranchGroup axisVis;
	TransformGroup axisVisTr;
	JPanel mainPanel;
	
	
	public AxisVisualisation(Frame frame){
			axisVis3D=new PamPanel3D(frame);
			createAxisVisualisation( "x (m)", "y (m)", "z (m)");
			mainPanel=new JPanel(new BorderLayout());
			mainPanel.setBorder(new TitledBorder("3D Axis"));
			mainPanel.add(BorderLayout.CENTER,axisVis3D);
			mainPanel.setPreferredSize(new Dimension(200,110));
	}
	
	public JPanel getPanel(){
		return mainPanel;
	}
	
	
	/**Add a small icon to show the orientatio of the graph
	 * 
	 */
	public void createAxisVisualisation(String axisX, String axisY, String axisZ){
		
		BoundingSphere bounds=new BoundingSphere(new Point3d(0,0,0), Double.MAX_VALUE);
		//create a new transform group
		axisVis=new BranchGroup();
		axisVis.setCapability( BranchGroup.ALLOW_DETACH );

		axisVisTr=new TransformGroup();
		axisVisTr.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		axisVisTr.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		axisVisTr.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		axisVisTr.setCapability(TransformGroup.ALLOW_COLLIDABLE_READ);
		
		//create a new mouse rotate
		MouseRotate myMouseRotate = new MouseRotate();
		myMouseRotate.setTransformGroup(axisVisTr);
		myMouseRotate.setSchedulingBounds(bounds);
		
		//add axis and rotate functionality to transform
		axisVisTr.addChild(myMouseRotate);
		axisVisTr.addChild(PamShapes3D.createAxis(0.5f, 0.5f, -0.5f, 0.02f, axisX,axisY,axisZ,new Color3f(0.25f,0.25f,0.25f)));
		
		axisVis.addChild(axisVisTr);
		axisVis3D.getRootGroup().addChild(axisVis);
		//add to rootGroup 
		
	}
	
	/**
	 * Takes a 3D, takes only the rotational component and transforms the axis by thta rotation
	 * @param trs3D
	 */
	public void setTransform(Transform3D trs3D){
		Matrix3f rotM=new Matrix3f();
		trs3D.get(rotM);
		trs3D=new Transform3D();
		trs3D.setRotation(rotM);
		axisVisTr.setTransform(trs3D);
	}
	
}
