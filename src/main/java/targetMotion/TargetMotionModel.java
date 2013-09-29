package targetMotion;

import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import PamDetection.PamDetection;
import PamView.PamSymbol;

public interface TargetMotionModel<T extends PamDetection> {

	String getName();
	
	String getToolTipText();
	
	boolean hasParameters();
	
	boolean parametersDialog();
	
	TargetMotionResult[] runModel(T pamDetection);
	
	
	public PamSymbol getPlotSymbol(int iResult);
	
	public TransformGroup getPlotSymbol3D(Vector3f vector, double[] sizeVector, double minSize);
	
}
