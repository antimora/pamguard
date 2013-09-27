package whistlesAndMoans;

public class ConnectedRegionDataUnit extends AbstractWhistleDataUnit {
	
	private ConnectedRegion connectedRegion;
	
	private double[] timeDelaysSeconds; 

	public ConnectedRegionDataUnit(ConnectedRegion connectedRegion, WhistleToneConnectProcess whistleToneConnectProcess) {
		super(connectedRegion.getStartMillis(), 1<<connectedRegion.getChannel(), 
				connectedRegion.getStartSample(), connectedRegion.getDuration());
		this.connectedRegion = connectedRegion;
		this.channelBitmap = 1<<connectedRegion.getChannel();
		// extract frequency information
		frequency = getFrequency(whistleToneConnectProcess.getSampleRate(), 
				whistleToneConnectProcess.getFFTLen());
	}

	public ConnectedRegion getConnectedRegion() {
		return connectedRegion;
	}

	@Override
	public double[] getFrequency() {
		if (getParentDataBlock() != null) {
			return getFrequency(getParentDataBlock().getSampleRate(), 
					((ConnectedRegionDataBlock) getParentDataBlock()).getFftLength());
		}
		return frequency;
	}
	
	private double[] getFrequency(double sampleRate, int fftLength) {

		double[] f = new double[2];
		int[] range = connectedRegion.getFreqRange();
		for (int i = 0; i < 2; i++) {
			f[i] = range[i] * sampleRate / fftLength;
		}
		return f;
	}

	@Override
	public int getSliceCount() {
		return connectedRegion.getNumSlices();
	}

	private double[] freqData;
	@Override
	public double[] getFreqsHz() {
		int[] fb = connectedRegion.getPeakFreqsBins();
		ConnectedRegionDataBlock db = (ConnectedRegionDataBlock) getParentDataBlock();
		int L = getSliceCount();
		if (freqData == null || freqData.length != L) {
			freqData = new double[L];
			for (int i = 0; i < L; i++) {
				freqData[i] = db.binsToHz(fb[i]);
			}
		}
		
		return freqData;
	}

	private double[] timeData;
	@Override
	public double[] getTimesInSeconds() {
		int[] tb = connectedRegion.getTimesBins();
		ConnectedRegionDataBlock db = (ConnectedRegionDataBlock) getParentDataBlock();
		int L = getSliceCount();
		if (timeData == null || timeData.length != L) {
			timeData = new double[L];
			for (int i = 0; i < L; i++) {
				timeData[i] = db.binsToSeconds(tb[i]);
			}
		}
		return timeData;
	}

	/**
	 * @param timeDelaysSamples the timeDelaysSamples to set
	 */
	public void setTimeDelaysSeconds(double[] timeDelaysSeconds) {
		this.timeDelaysSeconds = timeDelaysSeconds;
	}

	/**
	 * @return the timeDelaysSamples
	 */
	public double[] getTimeDelaysSeconds() {
		return timeDelaysSeconds;
	}

}
