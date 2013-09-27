package targetMotion;

import java.awt.Color;
import java.util.Arrays;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;

import PamDetection.PamDetection;
import PamView.PamSymbol;

abstract public class AbstractTargetMotionModel<T extends PamDetection> implements TargetMotionModel<T> {

	abstract Color getSymbolColour();
	

	
	private PamSymbol pamSymbol;

	@Override
	public PamSymbol getPlotSymbol(int iResult) {
		if (pamSymbol == null) {
			pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 9, 9, true, getSymbolColour(), getSymbolColour());
		}
		return pamSymbol;
	}


	public TransformGroup getPlotSymbol3D(Vector3f vector, double[] sizeVector, double minSize) {
	

		sizeVector = Arrays.copyOf(sizeVector, 3);
		for (int i = 0; i < 2; i++) {
			// only loop to 2 so that points can have 0 dimension in z. 
			sizeVector[i] = Math.max(sizeVector[i], minSize);
		}
		sizeVector[2] = Math.max(sizeVector[2], 2);
		
		TransformGroup tg = new TransformGroup();
		tg.setCapability( BranchGroup.ALLOW_DETACH );

		Sphere sphere = new Sphere(5f);
		//		sphere.setAppearance();
		Appearance ap = new Appearance();
		
		Material mat = new Material();

		Color3f col = new Color3f(getSymbolColour());	
		mat.setAmbientColor(new Color3f(0.0f,1.0f,1.0f));
		mat.setDiffuseColor(col);
		mat.setSpecularColor(col);
		ap.setMaterial(mat);
		
		ColoringAttributes ca = new ColoringAttributes(col, ColoringAttributes.NICEST);	
		ap.setColoringAttributes(ca);	
		TransparencyAttributes trans=new TransparencyAttributes(TransparencyAttributes.NICEST,0.2f);
		ap.setTransparencyAttributes(trans);

		sphere.setAppearance(ap); 
	

		Transform3D transform = new Transform3D();

		transform.setScale(new Vector3d(sizeVector));
		transform.setTranslation(vector);	
		tg.addChild(sphere);
		tg.setTransform(transform);
	


		return tg;
	}
}
