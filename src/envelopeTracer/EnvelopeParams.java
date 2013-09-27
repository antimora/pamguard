package envelopeTracer;

import java.io.Serializable;

import Filters.FilterParams;

public class EnvelopeParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	public String dataSourceName;
	
	public int channelMap = 3;
	
	public float outputSampleRate = 48000;
	
	public boolean logScale;
	
	public FilterParams filterSelect = new FilterParams();
	
	public FilterParams postFilterParams = new FilterParams();

	@Override
	protected EnvelopeParams clone() {
		try {
			return (EnvelopeParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
