package Acquisition;

import PamguardMVC.PamDataUnit;

/**
 * Data unit containing information on run starts and stops. 
 * <p>
 * The main purpose of producing these is so that they get picked up 
 * by the database and logged
 * 
 * @author Doug Gillespie
 *
 */
public class DaqStatusDataUnit extends PamDataUnit {

	private String status = "Stop";
	
	private String reason = "";
	
	private String daqSystemType = "";
	
	public int sampleRate;
	
	public int nChannels = 2;
	
	public double voltsPeak2Peak = 5;
	
	public double duration = 0;
	
	public double clockError;

	public DaqStatusDataUnit(long timeMilliseconds, String status, String reason, String daqSystemType, 
			double sampleRate, int channels, double voltsPeak2Peak, double duration, double clockError) {
		super(timeMilliseconds);
		this.status = status;
		this.reason = reason;
		this.daqSystemType = daqSystemType;
		this.sampleRate = (int) sampleRate;
		nChannels = channels;
		this.voltsPeak2Peak = voltsPeak2Peak;
		this.duration = duration;
		this.clockError = clockError;
	}

	public String getReason() {
		return reason;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status, String reason) {
		this.status = status;
		this.reason = reason;
	}

	public String getDaqSystemType() {
		return daqSystemType;
	}

	public void setDaqSystemType(String daqSystemType) {
		this.daqSystemType = daqSystemType;
	}

	public int getNChannels() {
		return nChannels;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public double getVoltsPeak2Peak() {
		return voltsPeak2Peak;
	}

	public double getDuration() {
		return duration;
	}


}
