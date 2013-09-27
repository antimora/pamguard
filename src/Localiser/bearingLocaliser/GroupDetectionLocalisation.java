package Localiser.bearingLocaliser;

import PamDetection.AbstractLocalisation;
import PamUtils.LatLong;

public class GroupDetectionLocalisation extends AbstractLocalisation {

	GroupDetection groupDetection;
	
	public GroupDetectionLocalisation(GroupDetection groupDetection, int locContents, int referenceHydrophones) {
		super(groupDetection, locContents, referenceHydrophones);
		this.groupDetection = groupDetection;
	}

	@Override
	public double getBearing(int iSide) {
		return groupDetection.getBearing(iSide);
	}
//
//	@Override
//	public double[] getAngles() {
//		return groupDetection.getAngles();
//	}

	@Override
	public double getBearingReference() {
		return groupDetection.getReferenceHeading(0);
	}

	@Override
	public double getRange() {
		return groupDetection.getRange(0);
	}

	@Override
	public boolean bearingAmbiguity() {
		return (groupDetection.getNumLatLong() > 1);
	}

	@Override
	public LatLong getOriginLatLong() {
		return groupDetection.getOriginLatLong(0);
	}

	@Override
	public LatLong getLatLong(int iSide) {
		return groupDetection.getLatLong(iSide);
	}

	@Override
	public int getNumLatLong() {
		return groupDetection.getNumLatLong();
	}

	@Override
	public double getErrorDirection(int iSide) {
		return groupDetection.getErrorReferenceAngle(iSide);
	}

	@Override
	public double getParallelError(int iSide) {
		return groupDetection.getParallelError(iSide);
	}

	@Override
	public double getPerpendiculaError(int iSide) {
		return groupDetection.getPerpendiculaError(iSide);
	}

	@Override
	public double getRange(int iSide) {
		return groupDetection.getRange(iSide);
	}

//	@Override
//	public double getRangeError(int iSide) {
//		return groupDetection.getRangeError(iSide);
//	}

}
