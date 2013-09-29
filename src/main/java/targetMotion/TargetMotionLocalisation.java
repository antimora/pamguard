package targetMotion;

import PamDetection.AbstractLocalisation;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;

public class TargetMotionLocalisation extends AbstractLocalisation {
	
	private TargetMotionResult targetMotionResult;

	public TargetMotionLocalisation(PamDataUnit pamDataUnit, TargetMotionResult targetMotionResult) {
		super(pamDataUnit, AbstractLocalisation.HAS_LATLONG | AbstractLocalisation.HAS_RANGE, 
				targetMotionResult.getReferenceHydrophones());
		this.targetMotionResult = targetMotionResult;
	}

	@Override
	public LatLong getLatLong(int iSide) {
		return targetMotionResult.getLatLong();
	}

	@Override
	public double getPerpendiculaError(int iSide) {
		return targetMotionResult.getPerpendicularDistanceError();
	}

	/**
	 * @return the targetMotionResult
	 */
	public TargetMotionResult getTargetMotionResult() {
		return targetMotionResult;
	}

	@Override
	public LatLong getOriginLatLong() {
		if (targetMotionResult.getBeamLatLong() != null) {
			return targetMotionResult.getBeamLatLong();
		}
		return super.getOriginLatLong();
	}

	@Override
	public String toString() {
		if (targetMotionResult == null) {
			return "null Target Motion Localisation";
		}
		return targetMotionResult.toString();
	}
	
	

}
