package clickDetector;

import java.io.Serializable;

public class ClickSpectrumParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 1L;
	
	static public final transient int CHANNELS_SINGLE = 0;
	static public final transient int CHANNELS_MEANS = 1;
	
	public boolean logScale = false;

	public double logRange = 30;
	
	public int plotSmoothing = 5;
	
	public boolean smoothPlot = false;
	
	public int channelChoice = CHANNELS_SINGLE;
	
	public boolean showEventInfo=true;
	
	@Override
	protected ClickSpectrumParams clone()  {
		try {
			return (ClickSpectrumParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}


}
