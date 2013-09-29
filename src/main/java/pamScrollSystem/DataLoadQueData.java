package pamScrollSystem;

import PamguardMVC.PamDataBlock;

/**
 * Used in a simple store of datablocks ques for loading.
 * @author Doug
 *
 */
public class DataLoadQueData {

	private long dataStart;
	
	private long dataEnd;
	
	private PamDataBlock pamDataBlock;

	public DataLoadQueData(PamDataBlock pamDataBlock, long dataStart,
			long dataEnd) {
		super();
		this.pamDataBlock = pamDataBlock;
		this.dataStart = dataStart;
		this.dataEnd = dataEnd;
	}

	protected long getDataStart() {
		return dataStart;
	}

	protected long getDataEnd() {
		return dataEnd;
	}

	protected PamDataBlock getPamDataBlock() {
		return pamDataBlock;
	}
	
	
}
