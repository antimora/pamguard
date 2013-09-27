package angleMeasurement;

import PamguardMVC.PamDataUnit;

/**
 * Data from an angle measurement. 
 * <br>
 * Angle data come in three stages. 
 * <br>1. Raw data as came out of the instrument
 * <br>2. Calibrated data - the raw data after calibration, 0 degrees
 * should be equal to 0 degrees in the calibrated data.
 * <br>3. Correct raw data - the calibrated data - the set constant offset. 
 * @author Douglas Gillespie
 *
 */
public class AngleDataUnit extends PamDataUnit {

	public double rawAngle;
	
	public double calibratedAngle;
	
	public double correctedAngle;
	
	public boolean held;

	public AngleDataUnit(long timeMilliseconds, double rawAngle, double calibratedAngle, double correctedAngle) {
		super(timeMilliseconds);
		this.rawAngle = rawAngle;
		this.calibratedAngle = calibratedAngle;
		this.correctedAngle = correctedAngle;
	}
	
}
