package staticLocaliser;

import java.util.ArrayList;
import java.util.Arrays;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import pamMaths.PamVector;
import staticLocaliser.panels.LocShape3D;
import staticLocaliser.panels.StaticDialogComponent;

import com.sun.j3d.utils.geometry.Sphere;

import PamDetection.AbstractDetectionMatch;
import PamDetection.PamDetection;
import PamGraph3D.PamShapes3D;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;

/**
 * This is the abstract class for building an algorithm for the static localiser. Note that a lot of default options have been included here, including a symbol for the map, symbol colour, calculation of time delays for a detection, symbol highlighted appearances etc. 
 * These can all be overridden depending on the type of algorithm.
 * <p>'
 * The time delay calculations simply use the default abstractDetectionMatch.groupDetectionsTD() function, however new localisation algorithms may not want to use all possible time delays approach. In this case, to keep things neat, you should change the subclass of 'AbstractDetectionMatch' for the detection in question- time delays calculations belong in this class- the algorithm should simply accept time delays (or other info) and attempt to localise. 
 * <p> 
 * @author Jamie Macualay.
 *
 */
public abstract class AbstractStaticLocaliserAlgorithm implements StaticLocalisationAlgorithmModel {
	
	
	public AbstractStaticLocaliserAlgorithm(StaticLocalise staticLocalise){
		this.staticLocaliser=staticLocalise;
	}
	
	StaticLocalise staticLocaliser;
	
	AbstractDetectionMatch abstractDetectionMatch;
	
	private PamDetection currentDetection;
	private Integer detectiontype=null;
	
	 ArrayList<ArrayList<ArrayList<Double>>> timeDelays;
	 ArrayList<ArrayList<Point3f>> hydrophonePos;
	 ArrayList<ArrayList<ArrayList<Double>>> timeDelayErrors;
	 
	 private boolean selected=false;
	
	
	public float[] getSymbolColour(){
		float[] col= new float[3];
		col[0]=0.6f;
		col[2]=0.7f;
		return col;
	}
	
	public float[] getSymbolHightlightColour(){
		float[] col= new float[3];
		col[0]=1f;
		return col;
	}
	
	/**
	 * Calculate the detection match for a PamDetection. Includes an arraylist of datablocks to be used if an unsynchrnoised array. Otherwise the Arraylist contains only the pamDetections parent datablock. 
	 * @param pamDetection
	 * @param dataBlocks
	 * @param detectionType
	 */
	public  void calcDetectionMatchTDs(PamDetection pamDetection, ArrayList<PamDataBlock> dataBlocks, Integer detectionType){
		
		//is the detection and dteectionType flag the same;
		boolean detectionChanged=(pamDetection==currentDetection && detectionType==detectiontype);
		//is the time delay info saved in memory still;
		boolean nonNull=(timeDelays!=null && timeDelayErrors!=null && hydrophonePos!= null);
		//if both of the above are true don't bother recalculating everything again. 
		if (detectionChanged && nonNull ) return;
		currentDetection=pamDetection;
		detectiontype=detectionType;
		
		//list of all possible time delays on each cluster of synchronised hydrophones.
		ArrayList<ArrayList<ArrayList<Double>>> timeDelays=new ArrayList<ArrayList<ArrayList<Double>>>();
		//list of corresponding time delay errors
		ArrayList<ArrayList<ArrayList<Double>>> timeDelayErrors=new ArrayList<ArrayList<ArrayList<Double>>>();
		//hydrophone positions of each cluster of synchronised hydrophones.
		ArrayList<ArrayList<Point3f>> hydrophonePos=new ArrayList<ArrayList<Point3f>>();
		

		for (int i=0; i<dataBlocks.size(); i++){
			
			ArrayList<ArrayList<Double>> timeDelaySet;
			ArrayList<ArrayList<Double>> timeDelayErrorSet=new ArrayList<ArrayList<Double>>();
			
			if (i==0){
				
				if (detectionType!=null){
					abstractDetectionMatch=pamDetection.getDetectionMatch(detectionType);
				}
				else{
					abstractDetectionMatch=pamDetection.getDetectionMatch();
				}
				
				//calculate time delays.
				timeDelaySet=null;//abstractDetectionMatch.getGroupTimeDelays(staticLocaliser.getStaticLocaliserControl().getParams().channels);
				
				//calculate corresponding  time delay errors
				for (int n=0; n<timeDelaySet.size(); n++){
					timeDelayErrorSet.add(abstractDetectionMatch.getTDErrrorsAll());
				}
				
	
				timeDelays.add(timeDelaySet);
				timeDelayErrors.add(timeDelayErrorSet);
//				hydrophonePos.add(abstractDetectionMatch.getHydrophones3d(pamDetection.getTimeMilliseconds()));
			
				
			}
			
			else{
				//TODO
				//find pamdetections in other datablocks within time window of main pamDetection. Seems like the best way to do this is to select the furthest away hydrophone, find those detections and 
				//then use them to calc time delays on other detection block. 
				//how to get the right hydrophones for different datablocks?
			}
		}
		
		
		//TODO
		//**************botch****************//
		//now must convert into correct format for localisation
		ArrayList<ArrayList<ArrayList<Double>>> tds3D=new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<Double>>> tds3DErrors=new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<Double>> tds2D;
		ArrayList<ArrayList<Double>> tds2DErrors;

		for (int j=0; j<timeDelays.get(0).size(); j++){
			tds2D=new ArrayList<ArrayList<Double>>();
			tds2DErrors=new ArrayList<ArrayList<Double>>();
			tds2D.add(timeDelays.get(0).get(j));
			tds2DErrors.add(timeDelayErrors.get(0).get(j));
			tds3D.add(tds2D);
			tds3DErrors.add(tds2DErrors);
		}
		//************************************//
				
		this.timeDelays=tds3D;
		this.timeDelayErrors=tds3DErrors;
		this.hydrophonePos=hydrophonePos;

	}

	public ArrayList<ArrayList<ArrayList<Double>>> getTimeDelays(){
		return timeDelays;
	}
	
	public ArrayList<ArrayList<Point3f>> getHydrophonePos(){
		return hydrophonePos;
	}
	
	public ArrayList<ArrayList<ArrayList<Double>>> getTimeDelayErrors(){
		return timeDelayErrors;
	}


	/**
	 * Default symbol for a localisation is a 3D ellipse with dimensions equal to the calculated errors in 3D.
	 */
	public TransformGroup getPlotSymbol3D(StaticLocalisationResults staticLocalisationResults) {
		
		if (staticLocalisationResults==null){
			System.out.println("Loclaisation results are null.");
			return null;
		}
		
		//minimum size of the symbol
		double minSize=2;
		double maxSize=1000;
		//maximum translation of the symbol
		double maxLoc=50000;
				
		Vector3f vector=new Vector3f( staticLocalisationResults.getX().floatValue(), staticLocalisationResults.getY().floatValue(),staticLocalisationResults.getZ().floatValue());
		Double[] sizeVector=staticLocalisationResults.getlocalisationError();
		
		sizeVector = Arrays.copyOf(sizeVector, 3);
		for (int i = 0; i < 2; i++) {
			vector.setX((float) Math.min(vector.getX(), maxLoc));
			vector.setY((float) Math.min(vector.getY(), maxLoc));
			vector.setZ((float) Math.min(vector.getZ(), maxLoc));
			sizeVector[i] = Math.max(sizeVector[i], minSize);
			sizeVector[i] = Math.min(sizeVector[i], maxSize);
		}
		sizeVector[2] = Math.max(sizeVector[2], 2);
		sizeVector[2] = Math.min(sizeVector[2], maxSize);
		
		Shape3D sphere= PamShapes3D.createSphere(PamShapes3D.sphereAppearance(new Color3f(getSymbolColour())), 1f);
		sphere.getAppearance().setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.4f));
		
		LocShape3D locshape3d=new LocShape3D(sphere,staticLocalisationResults);
			
		TransformGroup tg = new TransformGroup();
		tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg.setCapability( BranchGroup.ALLOW_DETACH );
		
		Transform3D transform = new Transform3D();
		
		double[] sizeVector2=new double[sizeVector.length];
		for (int i=0; i<sizeVector.length; i++){
			sizeVector2[i]=sizeVector[i];
		}
		
		try{
		transform.setScale(new Vector3d(sizeVector2));
//		System.out.println("vector: "+vector+"  sizeVector: "+sizeVector);
		transform.setTranslation(vector);	
		tg.addChild(locshape3d);
		tg.setTransform(transform);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	
		return tg;
	}
	
	public Appearance getNormalAppearance(){
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
		
		return ap;	
	}
	
	public Appearance getHighlightAppearance(){
		Appearance ap = new Appearance();
		
		Material mat = new Material();

		Color3f col = new Color3f(getSymbolHightlightColour());	
		mat.setAmbientColor(new Color3f(0.0f,1.0f,1.0f));
		mat.setDiffuseColor(col);
		mat.setSpecularColor(col);
		ap.setMaterial(mat);
		
		ColoringAttributes ca = new ColoringAttributes(col, ColoringAttributes.NICEST);	
		ap.setColoringAttributes(ca);	
		TransparencyAttributes trans=new TransparencyAttributes(TransparencyAttributes.NICEST,0.2f);
		ap.setTransparencyAttributes(trans);
		
		return ap;	
	}
	
	public void setNormalAppearance(LocShape3D locShape){
		locShape.setAppearance(getNormalAppearance());
	}
	
	public void setHighlightAppearance(LocShape3D locShape){
		locShape.setAppearance(getHighlightAppearance());
	}

	@Override
	public StaticDialogComponent getDisplayPanel() {
		return null;
	}
	
	
	@Override
	public boolean isSelected() {
		return selected;
	}
	
	@Override
	public void setSelected(boolean selected) {
		this.selected=selected;
	}

	public void setCurrentDetection(PamDetection pamDetection) {
		this.currentDetection=pamDetection;
	}
	
	


}
