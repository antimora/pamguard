package PamDetection;

import Acquisition.AcquisitionProcess;
import PamUtils.FrequencyFormat;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;

/**
 * Basic class for all acoustic data, everything from 
 * raw audio data to final detections with localisations
 * @author Doug Gillespie
 *
 */
abstract public class AcousticDataUnit extends PamDataUnit {

	/**
	 * all acoustic data must somehow have a knowledge of sample rate.
	 * This gives the ultimate in precise timing for all data
	 */
	protected long startSample;
	
	/**
	 * also in samples for precision. 
	 * The PamDataBlock class has knowledge of the sample rate, which
	 * can be used to convert this into seconds. 
	 */
	protected long duration;
		
	/**
	 * Frequency limits of the data in the data unit. 
	 */
	protected double[] frequency = { 0., 0. };
	
	/*
	 * The fun really starts here - what on earth are the amplitude units to be in ?
	 * Ultimately, users will want to see things displayed in dB re 1 uPa, but 
	 * it's good to keep the number of conversions small (log's take time). See
	 * amplitude functions for how we may be able to handle this reasonably
	 * efficiently. Also need to think about RMS / peak to peak issues. 
	 */
	/**
	 * measured amplitude contains the most easily used value of 
	 * amplitude. The int's below tell us how it was measured (this list
	 * will need extending, as will the getAmplitudedB function. 
	 */
	private double measuredAmplitude;
	/**
	 * Amplitude value is in dB relative to 1 micropascal
	 */
	static public final int AMPLITUDE_SCALE_DBREMPA = 0;
	/**
	 * Amplitude scale is linear relative to full scale
	 */
	static public final int AMPLITUDE_SCALE_LINREFSD = 1;
//	/**
//	 * Amplitude is taken from the spectrum (not spectrum level)
//	 */
//	static public final int AMPLITUDE_SCALE_SPECTRUM = 2;
	private int measuredAmplitudeType;
	
	private double calculatedAmlitudeDB = Double.NaN;
	
	public AcousticDataUnit(long timeMilliseconds,
			int channelBitmap, long startSample, long duration) {
		super(timeMilliseconds);
		setChannelBitmap(channelBitmap);
		setStartSample(startSample);
		setDuration(duration);
//		frequency[0] = 0;
//		frequency[1] = parentDataBlock.getSampleRate()/2;
	}

	/**
	 * @return Returns the measuredAmplitude.
	 */
	public double getMeasuredAmplitude() {
		return measuredAmplitude;
	}
	
	/**
	 * @return Returns the measuredAmplitudeType.
	 */
	public int getMeasuredAmplitudeType() {
		return measuredAmplitudeType;
	}
	
	public void amplifyMeasuredAmplitudeByDB(double gaindB) {
		switch (measuredAmplitudeType) {
		case AMPLITUDE_SCALE_DBREMPA:
//		case AMPLITUDE_SCALE_SPECTRUM:
			measuredAmplitude += gaindB;
			break;
		case AMPLITUDE_SCALE_LINREFSD:
			measuredAmplitude *= Math.pow(10, gaindB/20);
		}
	}
	public void amplifyMeasuredAmplitudeByLinear(double gaindB) {
		gaindB = Math.abs(gaindB);
		switch (measuredAmplitudeType) {
		case AMPLITUDE_SCALE_DBREMPA:
//		case AMPLITUDE_SCALE_SPECTRUM:
			measuredAmplitude += 20 * Math.log10(gaindB);
			break;
		case AMPLITUDE_SCALE_LINREFSD:
			measuredAmplitude *= gaindB;
		}
	}

	/**
	 * @param measuredAmplitude The measuredAmplitude to set.
	 */
	public void setMeasuredAmplitude(double measuredAmplitude, int measuredAmplitudeType) {
		this.measuredAmplitude = measuredAmplitude;
		this.measuredAmplitudeType = measuredAmplitudeType;
	}

	public double getAmplitudeDB() {
		/*
		 * try to be a bit swish here and see if the amplitude calculation
		 * has already been done, redoing it if necessary.
		 */ 
		if (!Double.isNaN(calculatedAmlitudeDB) ) {
			return calculatedAmlitudeDB;
		}
		else return calculatedAmlitudeDB = calculateAmplitudeDB();
	}
	
	private double calculateAmplitudeDB() {
		switch (measuredAmplitudeType) {
		case AMPLITUDE_SCALE_DBREMPA:
			return measuredAmplitude;
		case AMPLITUDE_SCALE_LINREFSD:
			return linAmplitudeToDB(measuredAmplitude); // some calculation !
//		case AMPLITUDE_SCALE_SPECTRUM:
//			return 1; // some other calculation				
		}
		return calculatedAmlitudeDB;
	}
	
	private double linAmplitudeToDB(double linamp) {
		/*
		 * Need to find the acquisition process for these data
		 */
		PamProcess daqProcess = getParentDataBlock().getSourceProcess();
		if (daqProcess == null) {
			return 0;
		}
		if (AcquisitionProcess.class.isAssignableFrom(daqProcess.getClass())) {
			AcquisitionProcess ap = (AcquisitionProcess) daqProcess;
			return ap.rawAmplitude2dB(linamp, PamUtils.getLowestChannel(channelBitmap), false);
		}
		return 0;
	}

	public long getLastSample() {
		return startSample + duration - 1;
	}

	public double getCalculatedAmlitudeDB() {
		return calculatedAmlitudeDB;
	}

	public void setCalculatedAmlitudeDB(double calculatedAmlitudeDB) {
		this.calculatedAmlitudeDB = calculatedAmlitudeDB;
	}

	/**
	 * @return the length of the acoustic data in samples
	 */
	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public double[] getFrequency() {
		return frequency;
	}

	public void setFrequency(double[] frequency) {
		this.frequency = frequency;
	}

	/**
	 * 
	 * @return The start sample of the data unit counting from the start of the run. 
	 */
	public long getStartSample() {
		return startSample;
	}

	public void setStartSample(long startSample) {
		this.startSample = startSample;
	}
	
	/**
	 * @return  the start time of the data unit in seconds relative to the start of the run.
	 */
	public double getSeconds() {
		return startSample / getParentDataBlock().getSampleRate();
	}

	public void setMeasuredAmplitude(double measuredAmplitude) {
		this.measuredAmplitude = measuredAmplitude;
	}

	public void setMeasuredAmplitudeType(int measuredAmplitudeType) {
		this.measuredAmplitudeType = measuredAmplitudeType;
	}
	
	public void setInfo(long timeMilliseconds,
			int channelBitmap, long startSample, long duration) {
		setTimeMilliseconds(timeMilliseconds);
		setChannelBitmap(channelBitmap);
		setStartSample(startSample);
		setDuration(duration);
	}
	
	/**
	 * returns the time overlap of another unit on this unit - so if the other unit is 
	 * longer in time and completely coveres this unit, the overlap is 1.0, if the 
	 * other unit is shorter or not aligned, then the overlap will be < 1.
	 * @param o Other AcousticDataUnit
	 * @return fractional overlap in time
	 */
	public double getTimeOverlap(AcousticDataUnit o) {
		if (o.startSample+o.duration< startSample) {
			return 0;
		}
		else if (o.startSample > startSample + duration) {
			return 0;
		}
		long oStart = Math.max(o.startSample, startSample);
		long oEnd = Math.min(o.startSample+o.duration, startSample+duration);
		return (double) (oEnd - oStart) / duration;
	}
	/**
	 * returns the frequency overlap of another unit on this unit - so if the other unit is 
	 * longer in time and completely coveres this unit, the overlap is 1.0, if the 
	 * other unit is shorter or not aligned, then the overlap will be < 1.
	 * @param o Other AcousticDataUnit
	 * @return fractional overlap in time
	 */
	public double getFrequencyOverlap(AcousticDataUnit o) {
		if (frequency == null || frequency.length < 2) {
			return 0;
		}
		else if (o.frequency == null || o.frequency.length < 2) {
			return 0;
		}
		if (o.frequency[1] < frequency[0]) {
			return 0;
		}
		else if (o.frequency[0] > frequency[1]) {
			return 0;
		}
		double oStart = Math.max(o.frequency[0], frequency[0]);
		double oEnd = Math.max(o.frequency[1], frequency[1]);
		return  (oEnd - oStart) / (frequency[1] - frequency[0]);
	}

	@Override
	public int compareTo(PamDataUnit o) {
		int ans1 = super.compareTo(o);
		if (ans1 == 0) {
			Long thisStart = new Long(startSample);
			ans1 = thisStart.compareTo(((AcousticDataUnit) o).getStartSample());
			if (ans1 == 0) {
				return this.getChannelBitmap() - o.getChannelBitmap();
			}
		}
		return ans1;
	}

	@Override
	public String getSummaryString() {
		String str = super.getSummaryString();
		str += "Frequency: " + FrequencyFormat.formatFrequencyRange(getFrequency(), true) + "<p>";
		str += String.format("Amplitude: %3.1fdB", getAmplitudeDB());
		
		return str;
	}
	
	
	
}
