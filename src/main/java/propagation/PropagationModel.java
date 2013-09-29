package propagation;

import PamUtils.LatLong;

/**
 * Really simple propagation models.
 * @author Doug Gillespie
 *
 */
public interface PropagationModel {
	/**
	 * Set the locations of hydrophone and the source
	 * @param hydrophoneLatLong
	 * @param hydrophoneDepth
	 * @param sourceLatLong
	 * @param sourceHeight - i.e. normally this will be negative within the code even 
	 * though the dialog may show a positive number for depth !
	 * @return true if model ran OK
	 */
	public boolean setLocations(LatLong hydrophoneLatLong, double hydrophoneHeight, 
			LatLong sourceLatLong, double sourceHeight);
	
	/**
	 * Get the number of propagation paths that will be returned
	 * @return number of paths
	 */
	public int getNumPaths();
	
	/**
	 * Get the time delays for each path
	 * @return delays in seconds
	 */
	public double[] getDelays();
	
	/**
	 * Get the gains for each path
	 * <p>These are the inverse of attenuation
	 * and are a scale factors NOT in dB so that
	 * surface reflections can be given a negative number
	 * @return path gains. 
	 */
	public double[] getGains();
	
	/**
	 * 
	 * @return name
	 */
	public String getName();
	
}
