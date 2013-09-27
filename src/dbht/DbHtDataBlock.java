package dbht;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class DbHtDataBlock extends PamDataBlock<DbHtDataUnit> {

	public DbHtDataBlock(String dataName,
			PamProcess parentProcess, int channelMap) {
		super(DbHtDataUnit.class, dataName, parentProcess, channelMap);
		
	}

}
