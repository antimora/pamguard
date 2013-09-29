package clickDetector;


import java.util.ArrayList;

import pamMaths.PamVector;
import Array.ArrayManager;
import Array.PamArray;
import PamDetection.AbstractDetectionMatch;
import PamDetection.AbstractLocalisation;

public class ClickLocalisation extends AbstractLocalisation {

	ClickDetection clickDataUnit;
		
	private int firstDelay;
	
	private double[][] anglesAndErrors;
	
	private double delayCorrection;
	
	private boolean bearingAmbiguity = true;

	private ArrayList<Integer> indexM1;

	private ArrayList<Integer> indexM2;
	
	public ClickLocalisation(ClickDetection parentDetection, int locContents, int referenceHydrophones, 
			int subArrayType, PamVector[] arrayOrientation) {
		super(parentDetection, locContents, referenceHydrophones, subArrayType, arrayOrientation);
		clickDataUnit = parentDetection;
		
	}

	@Override
	public boolean bearingAmbiguity() {
		return bearingAmbiguity;
	}

	@Override
	public double getBearing(int iSide) {
		//THODE flip sign of delay
		double ang = (firstDelay + delayCorrection) / clickDataUnit.getClickDetector().getSampleRate()
				/ ArrayManager.getArrayManager().getCurrentArray().getSeparationInSeconds(getReferenceHydrophones());
		ang = Math.min(1., Math.max(ang, -1.));
		double angle = Math.acos(ang);
		//System.out.println("");
		//System.out.println("cos angle: "+ ang + " Angle: "+ angle*180/Math.PI);
		return angle;
	}
	
//	@Override
//	public double getBearingReference() {
//
//		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
//		int p1 = PamUtils.getNthChannel(0, this.getReferenceHydrophones());
//		int p2 = PamUtils.getNthChannel(1, this.getReferenceHydrophones());
//		
//		return array.getHydrophoneLocator().
//		getPairAngle(this.getParentDetection().getTimeMilliseconds(), p2, p1, HydrophoneLocator.ANGLE_RE_NORTH) * Math.PI / 180.;
//		return clickDataUnit.getPairAngle(0, false) * Math.PI / 180.;
//	}

	public int getFirstDelay() {
		return firstDelay;
	}

	public void setFirstDelay(int delay) {
		this.firstDelay = delay;
	}

	public double getDelayCorrection() {
		return delayCorrection;
	}

	public void setDelayCorrection(double delayCorrection) {
		this.delayCorrection = delayCorrection;
	}

	/**
	 * @param anglesAndErrors the anglesAndErrors to set
	 */
	public void setAnglesAndErrors(double[][] anglesAndErrors) {
		this.anglesAndErrors = anglesAndErrors;
		if (anglesAndErrors != null) {
			if (anglesAndErrors.length > 0 && anglesAndErrors[0].length > 1) {
				bearingAmbiguity = false;
			}
		}
	}

	@Override
	public double[] getAngles() {
		if (anglesAndErrors == null) {
			return null;
		}
		return anglesAndErrors[0]; // return first row - angles, not errors. 
	}
	
	@Override
	public double[] getAngleErrors() {
		if (anglesAndErrors == null || anglesAndErrors.length < 2) {
			return null;
		}
		return anglesAndErrors[1]; // return first row - angles, not errors. 
	}
	
	@Override
	public double[] getTimeDelays() {
		double delay;
		double[] timeDelays=new double[clickDataUnit.getDelays().length];
		
		clickDataUnit.setComplexSpectrum();

		for(int i=0; i<timeDelays.length; i++){
			delay=-(clickDataUnit.getDelays()[i])/clickDataUnit.getClickDetector().getSampleRate();
			timeDelays[i]=delay;
		}

		return timeDelays;	
	}
	
	@Override
	public double[] getTimeDelayErrors() {
		
		double[] timeDelayErrors=new double[clickDataUnit.getDelays().length];
		
		if (indexM1==null) indexM1=super.indexM1(clickDataUnit.getNChan());
		if (indexM2==null) indexM2=super.indexM2(clickDataUnit.getNChan());
		
		for (int n=0; n<timeDelayErrors.length;n++){
			timeDelayErrors[n]=(ArrayManager.getArrayManager().getCurrentArray().getTimeDelayError(indexM1.get(n),indexM2.get(n)));
		}
		return timeDelayErrors;
	}
	
	public void setAngles(double[] angles) {
		int nAngles = angles.length;
		if (anglesAndErrors == null) {
			anglesAndErrors = new double[2][nAngles];
		}
		anglesAndErrors[0] = angles;
	}

	public void setAngleErrors(double[] angleErrors) {
		int nAngles = angleErrors.length;
		if (anglesAndErrors == null) {
			anglesAndErrors = new double[2][nAngles];
		}
		anglesAndErrors[1] = angleErrors;
	}

	/**
	 * @return the anglesAndErrors
	 */
	public double[][] getAnglesAndErrors() {
		return anglesAndErrors;
	}

}
