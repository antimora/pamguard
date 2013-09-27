package amplifier;

import java.io.Serializable;

import PamguardMVC.PamConstants;


public class AmpParameters implements Cloneable, Serializable {

	static public final long serialVersionUID = 0;
	
	int rawDataSource;
	
	/**
	 * gain is stored as a simple facter (NOT dB) for speed of use. 
	 * The dialog where they are set may well convert to dB values. 
	 */
	public double[] gain = new double[PamConstants.MAX_CHANNELS];

	public AmpParameters () {
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			gain[i] = 1.0;
		}
	}
	
	@Override
	public AmpParameters clone() {

		try {
			return (AmpParameters) super.clone();
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
