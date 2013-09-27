package whistlesAndMoans;

import pamMaths.PamVector;
import Array.ArrayManager;
import Array.HydrophoneLocator;
import Array.PamArray;
import Localiser.bearingLocaliser.BearingLocaliser;
import PamDetection.AbstractLocalisation;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;

public class WhistleBearingInfo extends AbstractLocalisation {

//	double[] delays;
	/* (non-Javadoc)
	 * @see PamDetection.AbstractLocalisation#getBearingReference()
	 */

	double[][] anglesAndErrors;
	PamVector[] arrayAxis;
	double bearingRef;
	
	public WhistleBearingInfo(PamDataUnit pamDataUnit, BearingLocaliser bearingLocaliser, 
			int hydrophones, double[][] anglesAndErrors) {
		super(pamDataUnit, AbstractLocalisation.HAS_BEARING, hydrophones);
		this.anglesAndErrors = anglesAndErrors;
//		System.out.println("Whistle bearing = " + bearing * 180/Math.PI);
		/**
		 * this next bit of code should really change now since there is no
		 * real guarantee that the first two phones create the 
		 * hydrophone axis - it's the same in the click detector. 
		 */
		int p1, p2;
		p1 = PamUtils.getLowestChannel(hydrophones);
		p2 = PamUtils.getNthChannel(1, hydrophones);
		PamArray array = ArrayManager.getArrayManager().getCurrentArray();
//		bearingRef = array.getHydrophoneLocator().getPairAngle(pamDataUnit.getTimeMilliseconds(), 
//				p2, p1, HydrophoneLocator.ANGLE_RE_NORTH);
//		bearingRef = Math.toRadians(bearingRef);
		// bearing ref is now the heading of the array as a whole, not elements within the array. D.G. 6/2/2012
		// just get for the first hydrophone in the group. 
		int firstPhone = PamUtils.getLowestChannel(hydrophones);
		bearingRef = array.getHydrophoneLocator().getArrayHeading(pamDataUnit.getTimeMilliseconds(), firstPhone);
		bearingRef = Math.toRadians(bearingRef);
//		arrayAxis = bearingLocaliser.getArrayAxis();
//		bearingRef = Math.atan2(arrayAxis[0].getElement(1), arrayAxis[0].getElement(0));
//		bearingRef = Math.PI/2-bearingRef;
	}

	@Override
	public double[] getAngles() {
		if (anglesAndErrors == null) {
			return null;
		}
		return anglesAndErrors[0];
	}

	@Override
	public double getBearingReference() {
		return bearingRef;
	}
	
	@Override
	public boolean bearingAmbiguity() {
		return (anglesAndErrors != null && anglesAndErrors[0].length == 1);
	}

	/* (non-Javadoc)
	 * @see PamDetection.AbstractLocalisation#getRange(int)
	 */
	@Override
	public double getRange(int side) {
		return 2000;
	}

	/* (non-Javadoc)
	 * @see PamDetection.AbstractLocalisation#getTimeDelays()
	 */
	@Override
	public double[] getTimeDelays() {
		return ((ConnectedRegionDataUnit) getParentDetection()).getTimeDelaysSeconds();
	}
	
	

}
