package seismicVeto;

import java.io.Serializable;

public class VetoParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 0;
	
	protected String dataSourceName;
	
	protected int channelBitmap = 0xFFFFFFFF;
	
	protected double threshold = 10;
	
	protected double f1 = 50, f2= 200;
	
	protected double backgroundConstant = 2;
	
	protected double vetoPreTime = 0.1, vetoPostTime = 1.2;
	
	protected boolean randomFillSpectorgram = false;
	
	protected boolean randomFillWaveform = false;

	@Override
	public VetoParameters clone() {

		try {
			return (VetoParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	
}
