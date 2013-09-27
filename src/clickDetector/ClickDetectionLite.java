package clickDetector;

/**
 * The aim of this class is a low memory, convenient tool for storing a click Detection. Rather than channel bitmaps etc a single click is simply a stored  with time millisecond
 * data, a startime in sample units and a waveform along with a sample rate and channel detected. (no channel grouping) This should ONLY be used in very specific circumstances where a conveninet, quick storage tool for clicks is required.
 * For a more comprehensive clickstorage class  use clickDetector.ClickDetection. 
 * @author EcologicUK
 *
 */
public class ClickDetectionLite {
	
	private double[] clickWaveform;
	private long clickStarTimeMilli;
	private long startSample;
	private double sampleRate;
	private int channel;
	
	public ClickDetectionLite(){
		
	}
	
	/**
	 * 
	 * @param Click time in milliseconds
	 * @param Start of click in sample bins
	 * @param double[] of click waveform 
	 * @param Channel which detected the click
	 * @param Sample rate of detector,.
	 */
	public ClickDetectionLite(long clickStarTimeMilli,long startSample, double[] clickWaveform, int channel, double sampleRate){
	this.clickWaveform=clickWaveform;
	this.clickStarTimeMilli=clickStarTimeMilli;
	this.startSample=startSample;
	this.sampleRate=sampleRate;
	this.channel=channel;
	}
	
	public void setClickWaveform(double[] clickWaveform){
		this.clickWaveform=clickWaveform;
	}
	

	public void setClickStartSample(long startSample){
		this.startSample=startSample;
	}
	
	
	public void setClickTimeMillis(long clickStarTimeMilli){
		this.clickStarTimeMilli=clickStarTimeMilli;
	}
	
	public void setSampleRate(double sampleRate){
		this.sampleRate=sampleRate;
	}
	
	public void setChannel(){
		this.channel=channel;
	}
	
	public double[] getClickWaveform(){
		return clickWaveform;
	}
	

	public long getClickStartSample(){
		return startSample;
	}
	
	
	public long getClickTimeMillis(){
		return clickStarTimeMilli;
	}
	
	public double getSampleRate(){
		return sampleRate;
	}
	
	public int getChannel(){
		return channel;
	}
	
	
	


	
	
	
	

}
