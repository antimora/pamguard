package clipgenerator.clipDisplay;

import PamUtils.Coordinate3d;
import PamView.GeneralProjector;

public class ClipDataProjector extends GeneralProjector {

	private ClipDisplayPanel clipDisplayPanel;
	
	private long clipStart;
	
//	private double minFrequency = 0;
//	private double maxFrequency = 1;
	
	public ClipDataProjector(ClipDisplayPanel clipDisplayPanel) {
		this.clipDisplayPanel = clipDisplayPanel;
		setParmeterType(0, GeneralProjector.ParameterType.TIME);
		setParmeterType(1, GeneralProjector.ParameterType.FREQUENCY);
	}
	
	public void setClipStart(long timeMillis) {
		this.clipStart = timeMillis;
	}

	@Override
	public Coordinate3d getCoord3d(double timeMillis, double freqHz, double d3) {
		ClipDisplayParameters clipParams = clipDisplayPanel.clipDisplayParameters;
		int halfFFTLen = 1<<(clipParams.logFFTLength-1);
		int clipHeight = (int) (halfFFTLen * clipParams.imageVScale);
		double fMax = clipDisplayPanel.getSampleRate() / 2. * clipParams.frequencyScale;
		double fPix = clipHeight - freqHz/fMax*clipHeight;
		double tBins = (timeMillis-clipStart)*clipDisplayPanel.getSampleRate()/1000./halfFFTLen *
		clipParams.imageHScale;
		
		
		return new Coordinate3d(tBins, fPix);
	}

}
