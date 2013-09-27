package PamguardMVC;

/**
 * Exception thrown by PamRawDataBlock when raw data are requested. 
 * @author Doug Gillespie
 *
 */
public class RawDataUnavailableException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public static final int DATA_ALREADY_DISCARDED = 1;
	public static final int DATA_NOT_ARRIVED = 2;
	public static final int INVALID_CHANNEL_LIST = 3;
	
	
	private PamRawDataBlock rawDataBlock;
	/**
	 * @return the dataCause
	 */
	public int getDataCause() {
		return dataCause;
	}

	/**
	 * @param rawDataBlock
	 * @param cause
	 */
	public RawDataUnavailableException(PamRawDataBlock rawDataBlock, int dataCause) {
		super();
		this.rawDataBlock = rawDataBlock;
		this.dataCause = dataCause;
	}


	/**
	 * Cause of the exception - no data or invalid channel list. 
	 */
	private int dataCause;

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		switch (dataCause) {
		case DATA_ALREADY_DISCARDED:
			return String.format("Data requested from %s have already been discarded", rawDataBlock.getDataName());
		case DATA_NOT_ARRIVED:
			return String.format("Data requested from %s have not yet arrived", rawDataBlock.getDataName());
		case INVALID_CHANNEL_LIST:
			return String.format("Data requested from %s do not contain the reqeusted channels %s", 
					rawDataBlock.getDataName());
		}
		return super.getMessage();
	}
	

}
