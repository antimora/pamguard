package offlineProcessing;

import generalDatabase.SQLLogging;

import java.util.ArrayList;

import dataMap.OfflineDataMapPoint;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * An offline task, such as click species id. 
 * Generally controlled and operated from within 
 * an OLProcessDialog
 * @author Doug Gillespie
 * @see OLProcessDialog
 *
 */
public abstract class OfflineTask<T extends PamDataUnit> {

	/**
	 * We want this particular task to run ? 
	 * N.B. this is different to canRun !
	 */
	private boolean doRun = true;
	
	private OfflineTaskGroup offlineTaskGroup;
	
	/**
	 * primary data block for the task;
	 */
	private PamDataBlock<T> parentDataBlock;
	
	/**
	 * list of other data blocks also required by this task. 
	 */
	private ArrayList<PamDataBlock> requiredDatablocks;
	
	/**
	 * Data blocks who's data may be affected by this task
	 * (so will need saving or will need their data deleted)
	 */
	private ArrayList<PamDataBlock> affectedDataBlocks;
	
	/**
	 * (Not sure what happens if we need multiple data units to 
	 * complete a task !)
	 * @return the datablock used by the task. 
	 */
	public PamDataBlock<T> getDataBlock() {
		return parentDataBlock;
	}
	
	/**
	 * @param dataBlock the dataBlock to set
	 */
	public void setParentDataBlock(PamDataBlock dataBlock) {
		this.parentDataBlock = dataBlock;
		if (offlineTaskGroup != null && offlineTaskGroup.getPrimaryDataBlock() == null) {
			offlineTaskGroup.setPrimaryDataBlock(dataBlock);
		}
	}

	/**
	 * 
	 * @return a name for the task, to be displayed in the dialog. 
	 */
	abstract public String getName();
	
	/**
	 * task has settings which can be called
	 * @return true or false
	 */
	public boolean hasSettings() {
		return false;
	}
	
	/**
	 * Call any task specific settings
	 * @return true if settings may have changed. 
	 */
	public boolean callSettings() {
		return false;
	}
	
	/**
	 * can the task be run ? This will generally 
	 * be true, but may be false if the task is dependent on 
	 * some other module which may not be present.  
	 * @return true if it's possible to run the task. 
	 */
	public boolean canRun() {
		boolean can = getDataBlock() != null; 
		return can;
	}
	
	/**
	 * Process a single data unit. 
	 * @return true if the data unit has changed in some
	 * way so that it will need re-writing to it's binary file 
	 * or database. 
	 */
	abstract public boolean processDataUnit(T dataUnit);

	/**
	 * Called when new data are loaded for offline processing 
	 * (or once at the start of processing loaded data). 
	 * @param startTime start time of loaded data
	 * @param endTime end time of loaded data
	 */
	abstract public void newDataLoad(long startTime, long endTime, OfflineDataMapPoint mapPoint);
	
	/**
	 * Called when processing of loaded data, or each map point worth of data,
	 * is complete. 
	 */
	abstract public void loadedDataComplete();
	
	/**
	 * Add a required data block. 
	 * These are data blocks apart from the main one which 
	 * are required before this task can complete. 
	 * Data for these block will be loaded automatically. 
	 * @param dataBlock required data block. 
	 */
	public void addRequiredDataBlock(PamDataBlock dataBlock) {
		if (requiredDatablocks == null) {
			requiredDatablocks = new ArrayList<PamDataBlock>();
		}
		requiredDatablocks.add(dataBlock);
	}
	/**
	 * @return the number of data blocks required to run 
	 * this task. 
	 */
	public int getNumRequiredDataBlocks() {
		if (requiredDatablocks == null) {
			return 0;
		}
		return requiredDatablocks.size();
	}

	
	/**
	 * A data block required to run this task. 
	 * @param iBlock block index
	 * @return data block .
	 */
	public PamDataBlock getRequiredDataBlock(int iBlock) {
		return requiredDatablocks.get(iBlock);
	}

	/**
	 * Add an affected data block. 
	 * These are data blocks apart from the main one which 
	 * will have their contents changed by tehe task and will 
	 * require saving / updating as the task progresses. 
	 * @param dataBlock affected data block. 
	 */
	public void addAffectedDataBlock(PamDataBlock dataBlock) {
		if (affectedDataBlocks == null) {
			affectedDataBlocks = new ArrayList<PamDataBlock>();
		}
		affectedDataBlocks.add(dataBlock);
	}
	/**
	 * @return the number of data blocks required to run 
	 * this task. 
	 */
	public int getNumAffectedDataBlocks() {
		if (affectedDataBlocks == null) {
			return 0;
		}
		return affectedDataBlocks.size();
	}
	
	/**
	 * A data block required to run this task. 
	 * @param iBlock block index
	 * @return data block .
	 */
	public PamDataBlock getAffectedDataBlock(int iBlock) {
		return affectedDataBlocks.get(iBlock);
	}

	/**
	 * return whether or not the task SHOULD be run - i.e. is it selected in 
	 * the dialog, etc. ?  
	 * @return the doRun
	 */
	public boolean isDoRun() {
		if (canRun() == false) {
			return false;
		}
		return doRun;
	}

	/**
	 * Set whether or not this task within a taskGroup should be run. 
	 * @param doRun the doRun to set
	 */
	public void setDoRun(boolean doRun) {
		this.doRun = doRun;
	}

	/**
	 * @return the offlineTaskGroup
	 */
	public OfflineTaskGroup getOfflineTaskGroup() {
		return offlineTaskGroup;
	}

	/**
	 * @param offlineTaskGroup the offlineTaskGroup to set
	 */
	public void setOfflineTaskGroup(OfflineTaskGroup offlineTaskGroup) {
		this.offlineTaskGroup = offlineTaskGroup;
	}

	/**
	 * Delete database outptut data in the list of output datablocks. 
	 * All data in the time range of data read into the primary 
	 * source data block will be deleted. 
	 * @param currentViewDataStart
	 * @param currentViewDataEnd
	 * @param mapPoint
	 */
	public void deleteOldOutput(long currentViewDataStart,
			long currentViewDataEnd, OfflineDataMapPoint mapPoint) {
		if (affectedDataBlocks == null || parentDataBlock == null) {
			return;
		}
		SQLLogging sqlLogging;
		for (int i = 0; i < affectedDataBlocks.size(); i++) {
			sqlLogging = affectedDataBlocks.get(i).getLogging();
			if (sqlLogging == null) {
				continue;
			}
			sqlLogging.deleteData(parentDataBlock.getCurrentViewDataStart(), 
					parentDataBlock.getCurrentViewDataEnd());
		}
	}

	/**
	 * Called at the start of the thread which executes this task. 
	 */
	public void prepareTask() {	}

	/**
	 * Called at the end of the thread which executes this task. 
	 */
	public void completeTask() { }



}
