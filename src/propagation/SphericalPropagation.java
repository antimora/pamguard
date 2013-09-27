package propagation;

import Array.ArrayManager;
import PamUtils.LatLong;

public class SphericalPropagation implements PropagationModel {

	private double[] delays;
	
	private double[] gains;
	
	@Override
	public double[] getDelays() {
		return delays;
	}

	@Override
	public double[] getGains() {
		return gains;
	}

	@Override
	public int getNumPaths() {
		return 1;
	}

	@Override
	public boolean setLocations(LatLong hydrophoneLatLong,
			double hydrophoneHeight, LatLong sourceLatLong, double sourceHeight) {
		double dist = hydrophoneLatLong.distanceToMetres(sourceLatLong);
		/*
		 * Hydrophone depth is stored as an alitude (i.e. is negative, so add it to the 
		 * sourceDepth which is stored as a depth. 
		 */
		double depth = hydrophoneHeight - sourceHeight;
		dist = Math.sqrt(dist*dist + depth*depth);
		double soundSpeed = ArrayManager.getArrayManager().getCurrentArray().getSpeedOfSound();
		delays = new double[1];
		gains = new double[1];
		delays[0] = dist/soundSpeed;
		gains[0] = 1./dist;
		return true;
	}

	@Override
	public String getName() {
		return "Spherical Propagation";
	}

	@Override
	public String toString() {
		return getName();
	}
	
	

}
