package Array;

import pamMaths.PamVector;

/**
 * Some abstract class function for hydrophone location that are used by
 * more concreate locators, in particular the StaticHydrophoneLocator
 * and StraightHydrophoneLocators
 * @author Doug Gillespie
 * @see Array.StraightHydrophoneLocator
 * @see Array.StaticHydrophoneLocator
 *
 */

abstract public class SimpleHydrophoneLocator implements HydrophoneLocator{

	PamArray pamArray;
	
	public SimpleHydrophoneLocator(PamArray pamArray) {
		super();
		// TODO Auto-generated constructor stub
		this.pamArray = pamArray;
	}

	/**
	 * 
	 */
	public double getPairAngle(long timeMilliseconds, int phone1, int phone2, int angleType) {
		PamVector v1, v2;
		v1 = pamArray.getAbsHydrophoneVector(phone1);
		v2 = pamArray.getAbsHydrophoneVector(phone2);
		double ang = Math.atan2(v2.getElement(1) - v1.getElement(1), v2.getElement(0) - v1.getElement(0));
		ang = 90 - (ang * 180/Math.PI);
		if (ang < 0) ang += 360;
		return ang;
		
//		Hydrophone h1 = pamArray.getHydrophone(phone1);
//		Hydrophone h2 = pamArray.getHydrophone(phone2);
//		
//		return getPairAngle(h1, h2);
	}
	
	/**
	 * Gets the angle between a pair of hydrophones in degrees 
	 * in navigation units, i.e. clockwise from North
	 * @param h1 first hydrophone
	 * @param h2 second hydrophone
	 * @return angle from north in degrees
	 */
//	private double getPairAngle(Hydrophone h1, Hydrophone h2) {
//		// angle up from x axis ...
//		double x1, x2, y1, y2;
//		x1 = pamArray.get
//		double ang = Math.atan2(h2.getY() - h1.getY(), h2.getX() - h1.getX());
//		ang = 90 - (ang * 180/Math.PI);
//		if (ang < 0) ang += 360;
//		return ang;
//	}

	public double getPairSeparation(long timeMilliseconds, int phone1, int phone2) {
		PamVector v1, v2;
		v1 = pamArray.getAbsHydrophoneVector(phone1);
		v2 = pamArray.getAbsHydrophoneVector(phone2);
		return v1.dist(v2);
//		Hydrophone h1 = pamArray.getHydrophone(phone1);
//		Hydrophone h2 = pamArray.getHydrophone(phone2);
//		return getPairSeparation(h1, h2);
	}
	
//	private double getPairSeparation(Hydrophone h1, Hydrophone h2) {
//		if (h1 == null || h2 == null) return 1;
//		return Math.sqrt(Math.pow(h2.getY() - h1.getY(), 2) + 
//				Math.pow(h2.getX() - h1.getX(), 2) + 
//				Math.pow(h2.getZ() - h1.getZ(), 2));
//	}
	

	public double getPhoneHeight(long timeMilliseconds, int phoneNo) {
		if (pamArray == null || pamArray.getHydrophone(phoneNo) == null) return 0;
		return pamArray.getHydrophone(phoneNo).getZ();
	}

	public double getPhoneTilt(long timeMilliseconds, int phoneNo) {
		if (pamArray == null || pamArray.getHydrophone(phoneNo) == null) return 0;
		return pamArray.getHydrophone(phoneNo).getTilt();
	}
	
	public double getPhoneHeading(long timeMilliseconds, int phoneNo) {
		if (pamArray == null || pamArray.getHydrophone(phoneNo) == null) return 0;
		return pamArray.getHydrophone(phoneNo).getHeading();
	}
//
//	public LatLong getPhoneLatLong(long timeMilliseconds, int phoneNo) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub
		
	}

	
}
