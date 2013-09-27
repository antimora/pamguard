package PamController;

import java.awt.Window;

import pamScrollSystem.ViewLoadObserver;

import PamguardMVC.PamDataBlock;
import PamguardMVC.RequestCancellationObject;

/**
 * Interface implemented by PamControlledUnits which 
 * are capable of reloading and restoring data when operating in 
 * Viewer mode.
 * @author Doug Gillespie
 *
 */
public interface OfflineDataStore {

	/**
	 * Create a basic map of the data including first and 
	 * last times and some kind of data/unit time count 
	 * plus ideally some kind of start and stop time list
	 * of where there are gaps in the data. 
	 */
	public void createOfflineDataMap(Window parentFrame);
	
	/**
	 * Get the data source name
	 * @return data source name
	 */
	public String getDataSourceName();
	
	/**
	 * Load data for a given datablock between two time limits. 
	 * @param dataBlock datablock owner of the data
	 * @param dataStart start time in milliseconds
	 * @param dataEnd end time in milliseconds
	 * @param loadObserver 
	 * @return true if load successful. 
	 */
	public boolean loadData(PamDataBlock dataBlock, long dataStart, long dataEnd, 
			RequestCancellationObject cancellationObject, ViewLoadObserver loadObserver);
	
	/**
	 * Save data previously loaded from the store during 
	 * offline viewing. 
	 * @param dataBlock datablock owner of the data
	 * @return true if saved or save not needed. False if an error prevents saving. 
	 */
	public boolean saveData(PamDataBlock dataBlock);
	
}
