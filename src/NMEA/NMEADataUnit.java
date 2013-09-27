package NMEA;

import PamguardMVC.PamDataUnit;

public class NMEADataUnit extends PamDataUnit {

	private StringBuffer charData;
	
	private String stringId;
	

	public NMEADataUnit(long timeMilliseconds, StringBuffer charData) {
		super(timeMilliseconds);
		this.charData = charData;
		stringId = NMEADataBlock.getSubString(charData, 0);
	}

	/**
	 * Get teh full NMEA character string. 
	 * @return
	 */
	public StringBuffer getCharData() {
		return charData;
	}

	/**
	 * Set teh full NMEA character string.
	 * @param charData
	 */
	public void setCharData(StringBuffer charData) {
		this.charData = charData;
		stringId = NMEADataBlock.getSubString(charData, 0);
	}


	/**
	 * @return the stringId
	 */
	public String getStringId() {
		return stringId;
	}

}
