package ltsa;

import PamguardMVC.LoadObserver;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.RequestCancellationObject;
import fftManager.FFTDataBlock;

public class LtsaDataBlock extends FFTDataBlock {

	private boolean moreAveraged;
	
	public LtsaDataBlock(String dataName, PamProcess parentProcess, boolean moreAveraged) {
		super(dataName, parentProcess, 0, 1, 1);
		this.moreAveraged = moreAveraged;
		
		
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#getOfflineData(PamguardMVC.PamObserver, PamguardMVC.PamObserver, long, long, boolean, PamguardMVC.RequestCancellationObject)
	 */
	@Override
	public synchronized int getOfflineData(PamObserver observer,
			PamObserver endUser, long startMillis, long endMillis,
			boolean allowRepeats, RequestCancellationObject cancellationObject) {

		/**
		 * This would normally be used in the FFT data block to regenerate off-line data from original
		 * raw data, and if allowed to do it's own thing and raw data were available, would probably attempt to
		 * do just that now !
		 * Clearly, that is unnecessary since data are stored in the binary data store for the LTSA, so call 
		 * that instead !
		 */
		loadViewerData(startMillis, endMillis, null);
		return REQUEST_DATA_LOADED;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamDataBlock#orderOfflineData(PamguardMVC.PamObserver, PamguardMVC.LoadObserver, long, long, int, boolean)
	 */
	@Override
	public void orderOfflineData(PamObserver dataObserver,
			LoadObserver loadObserver, long startMillis, long endMillis,
			int interrupt, boolean allowRepeats) {
		/*
		 *As for getOfflineData, overrride this.
		 */
		loadViewerData(startMillis, endMillis, null);
		if (loadObserver != null) {
			loadObserver.setLoadStatus(REQUEST_DATA_LOADED);
		}
	}

	
}
