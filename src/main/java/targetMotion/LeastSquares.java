package targetMotion;

import java.awt.Color;

import Localiser.bearingLocaliser.DetectionGroupLocaliser;
import Localiser.bearingLocaliser.GroupDetection;
import PamDetection.PamDetection;
import PamUtils.LatLong;

/**
 * Least squares localisation for Target motion analysis. 
 * Basically a wrapper around older least sq method developed for real time tracking. 
 * @author Doug Gillespie
 *
 * @param <T>
 */
public class LeastSquares<T extends PamDetection> extends AbstractTargetMotionModel<T> {

	private DetectionGroupLocaliser detectionGroupLocaliser;

	public LeastSquares(TargetMotionLocaliser<T> targetMotionLocaliser) {
		detectionGroupLocaliser = new DetectionGroupLocaliser(null);
	}

	@Override
	public String getName() {
		return "Least Squares";
	}

	@Override
	public boolean hasParameters() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean parametersDialog() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TargetMotionResult[] runModel(T pamDetection) {
		GroupDetection groupDetection;

		int nSub = pamDetection.getSubDetectionsCount();
		if (nSub < 2) {
			return null;
		}
		groupDetection = new GroupDetection<PamDetection>(pamDetection.getSubDetection(0));
		for (int i = 1; i < nSub; i++) {
			groupDetection.addSubDetection(pamDetection.getSubDetection(i));
		}
		TargetMotionResult[] results = new TargetMotionResult[2];
		boolean[] sideOk = new boolean[2];
		LatLong ll;
		sideOk[0] = detectionGroupLocaliser.localiseDetectionGroup(groupDetection, 1);
		if (sideOk[0]) {
			ll = detectionGroupLocaliser.getDetectionLatLong();
			results[0] = new TargetMotionResult(this, ll, 0, 0);
			results[0].setPerpendicularDistance(detectionGroupLocaliser.getRange());
			results[0].setPerpendicularDistanceError(detectionGroupLocaliser.getPerpendicularError());
			results[0].setReferenceHydrophones(pamDetection.getChannelBitmap());
//			results[0].setProbability(detectionGroupLocaliser.)
//			System.out.println(String.format("Fit lat long %d = %s, %s", 0, ll.formatLatitude(), ll.formatLongitude()));
		}
		sideOk[1] = detectionGroupLocaliser.localiseDetectionGroup(groupDetection, -1);
		if (sideOk[1]) {
			ll = detectionGroupLocaliser.getDetectionLatLong();
			results[1] = new TargetMotionResult(this, ll, 1, 0);
			results[1].setPerpendicularDistance(detectionGroupLocaliser.getRange());
			results[1].setPerpendicularDistanceError(detectionGroupLocaliser.getPerpendicularError());
			results[1].setReferenceHydrophones(pamDetection.getChannelBitmap());
//			System.out.println(String.format("Fit lat long %d = %s, %s", 1, ll.formatLatitude(), ll.formatLongitude()));
		}
		if (sideOk[0] == false && sideOk[1] == false) {
			return null;
		}
		//		for (int i = 0; i < 2; i++) {
		//			LatLong ll = detectionGroupLocaliser.getDetectionLatLong();
		//			System.out.println(String.format("Fit lat long %d = %s, %s", i, ll.formatLatitude(), ll.formatLongitude()));
		//		}
		return results;
	}

	@Override
	public String getToolTipText() {
		return "<html>Least squares approximation - assumes vessel track is a straight line</html>";
	}

	private Color symbolColour = new Color(0, 0, 0);

	@Override
	Color getSymbolColour() {
		return symbolColour;
	}

	
	
	
/**public TransformGroup getPlotSymbol3D(Vector3f vector, Double[] sizeVector, double minSize){
	
	TransformGroup trg=new TransformGroup();

	Transform3D posStretch=new Transform3D();
	Appearance app=new Appearance();
	
	for (int i=0; i<sizeVector.length;i++){
		if (sizeVector[i]<minSize){
			sizeVector[i]=minSize;
		}
	}
	
	ColoringAttributes colour=new ColoringAttributes();
	colour.setColor(new Color3f(0f,0f,0f));
	app.setColoringAttributes(colour);

	Sphere sphr=new Sphere(5f);
	sphr.setAppearance(app);
	trg.addChild(sphr);
	
	posStretch.setTranslation(vector);
	posStretch.setScale(new Vector3d(sizeVector[0],sizeVector[1],sizeVector[2] ));
	
	trg.setTransform(posStretch);
		
		return trg;

	};**/

}
