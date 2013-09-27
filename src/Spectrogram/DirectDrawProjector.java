package Spectrogram;

import PamController.PamController;
import PamUtils.Coordinate3d;
import PamView.GeneralProjector;

/**
 * Spectrgoram projector for drawing directly onto the spectrogram display. 
 * <p>Unlike the SpectroramProjector, which maps pixels 1:1, this projector needs
 * the scales of the display since it draws directly onto the AWT component. 
 * @author Doug Gillespie
 * @see SpectrogramProjector
 *
 */
public class DirectDrawProjector extends GeneralProjector {

	/**
	 * The start of the display. In viewer mode, this will be quite
	 * straight forward, and be the current time from the scroller. 
	 * In real time, it will be the current time - the display length. 
	 * Setting this is handled in the spectrogram display. 
	 */
	private double xStartMillis;

	private double xPixsPerMillisecond;

	//	private long fftStartBin;
	//	
	//	private double xPixsPerFFTBin;

	private double[] frequencyRange;

	private double yPixsPerHz;

	private SpectrogramDisplay spectrogramDisplay; 

	private int panelId;

	private int displayHeight, displayWidth;

	/**
	 * This is the current cursor position, which will be 0 in 
	 * viewer mode, but will scroll forwards in real time ops. 
	 */
	private double xStartPix = 0;

	private double timeRangeMillis;

	private boolean isViewer;

	public DirectDrawProjector(SpectrogramDisplay spectrogramDisplay, int panelId) {

		this.spectrogramDisplay = spectrogramDisplay;

		this.panelId = panelId;

		isViewer = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;

		setParmeterType(0, GeneralProjector.ParameterType.TIME);

		setParmeterType(1, GeneralProjector.ParameterType.FREQUENCY);
	}
	protected void setScales(int displayWidth, int displayHeight, double startMillis, 
			double timeRangeMillis, double[] frequencyRange, float sampleRate, double imagePos) {
		this.displayHeight = displayHeight;
		this.displayWidth = displayWidth;
		this.frequencyRange = frequencyRange;
		this.timeRangeMillis = timeRangeMillis;
		xStartPix = imagePos;
		xStartMillis = startMillis;
		xPixsPerMillisecond = displayWidth / timeRangeMillis;
		double fRange = frequencyRange[1]-frequencyRange[0];
		if (fRange > 0) {
			yPixsPerHz = displayHeight / fRange;
		}
	}

	@Override
	public Coordinate3d getCoord3d(double timeMillis, double freqHz, double d3) {
		//		System.out.println("xStart = " + PamCalendar.formatDateTime((long) xStartMillis) + 
		//				" eventTime " + PamCalendar.formatDateTime((long) timeMillis) +
		//						" File start " + 
		//						PamCalendar.formatDateTime(PamCalendar.getSessionStartTime()));
		double x = xStartPix  + (timeMillis-xStartMillis) * xPixsPerMillisecond;
			// deal with wrap around - don't wrap if it's beyond the display max time. 
			if (x >= displayWidth && (timeMillis-xStartMillis) < timeRangeMillis) {
				x -= displayWidth;
			}
			if (x < 0) {
				x += displayWidth;
			}
		double y = displayHeight - (freqHz- frequencyRange[0]) * yPixsPerHz;
		return new Coordinate3d(x, y);
	}
	/**
	 * @return the spectrogramDisplay
	 */
	public SpectrogramDisplay getSpectrogramDisplay() {
		return spectrogramDisplay;
	}
	/**
	 * @return the displayHeight
	 */
	public int getDisplayHeight() {
		return displayHeight;
	}
	/**
	 * @return the displayWidth
	 */
	public int getDisplayWidth() {
		return displayWidth;
	}
	/**
	 * @return the panelId
	 */
	public int getPanelId() {
		return panelId;
	}

}
