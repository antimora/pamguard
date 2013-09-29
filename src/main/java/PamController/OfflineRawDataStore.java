package PamController;

import Acquisition.offlineFuncs.OfflineFileServer;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;

public interface OfflineRawDataStore extends OfflineDataStore {


	/**
	 * 
	 * @return The offline file server which will do the actual work
	 */
	public OfflineFileServer getOfflineFileServer() ;

	public PamRawDataBlock getRawDataBlock();
	
	public PamProcess getParentProcess();
	
	public String getUnitName();
	
}
