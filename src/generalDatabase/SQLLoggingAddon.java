package generalDatabase;

import PamguardMVC.PamDataUnit;

/**
 *  common functionality to add on to an existing SQL logging object. 
 *  <p>Initially developed for the target motion analysis which may 
 *  get added to a variety of different things. 
 * @author Doug Gillespie
 *
 */
public interface SQLLoggingAddon {

	/**
	 * Add a load of comumns to an existing table definition
	 * @param pamTableDefinition
	 */
	public void addTableItems(PamTableDefinition pamTableDefinition);
	
	/**
	 * Save data - that is transfer data from the pamDataUnit to the data objects
	 * within the table definition
	 * @param pamTableDefinition table definition
	 * @param pamDataUnit data unit
	 * @return true if successful
	 */
	public boolean saveData(PamTableDefinition pamTableDefinition, PamDataUnit pamDataUnit);
	
	/**
	 * Load data - that is read data from the table definition and turn it into something sensible
	 * within or attached to the data unit. 
	 * @param pamTableDefinition table definition
	 * @param pamDataUnit data unit
	 * @return true if successful
	 */
	public boolean loadData(PamTableDefinition pamTableDefinition, PamDataUnit pamDataUnit);
}
