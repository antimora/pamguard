package clickDetector;

import PamDetection.AbstractLocalisation;

public class ClickTrainLocalisation extends AbstractLocalisation {

	ClickTrainDetection clickTrainDataUnit;
	
	double referenceBearing = Double.NaN;
	
	public ClickTrainLocalisation(ClickTrainDetection parentDetection, int referenceHydrophones) {
		super(parentDetection, HAS_BEARING, referenceHydrophones);
		this.clickTrainDataUnit = parentDetection;
	}

	@Override
	public boolean bearingAmbiguity() {
		return true;
	}

	@Override
	public double getBearing(int iSide) {
		return clickTrainDataUnit.firstClickAngle;
	}

	@Override
	public int getLocContents() {
		return super.getLocContents();
	}

	@Override
	public double getRange(int iSide) {
		return clickTrainDataUnit.getPerpendiculaError(0);
//		return super.getRange();
	}

	@Override
	public double getBearingReference() {
		return referenceBearing;
	}

	public void setReferenceBearing(double referenceBearing) {
		this.referenceBearing = referenceBearing;
	}

}
