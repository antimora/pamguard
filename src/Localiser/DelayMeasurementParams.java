package Localiser;

import java.io.Serializable;

import fftFilter.FFTFilterParams;

/**
 * Generic parameters associated with delay measurement. 
 * @author Doug Gillespie
 *
 */
public class DelayMeasurementParams implements Serializable, Cloneable {


	public static final long serialVersionUID = 1L;

	/**
	 * Filter data prior to bearing measurement
	 */
	public boolean filterBearings;
	
	/**
	 * Parameters for bearing filter 
	 */
	public FFTFilterParams delayFilterParams;
	/**
	 * Measure bearings from the waveform envelope, not the full wavefrom. 
	 */
	public boolean envelopeBearings;


	@Override
	public DelayMeasurementParams clone() {
		try {
			return (DelayMeasurementParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
