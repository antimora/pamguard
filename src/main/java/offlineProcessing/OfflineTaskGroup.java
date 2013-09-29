package offlineProcessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.SwingWorker;

import binaryFileStorage.DataUnitFileInformation;

import dataMap.OfflineDataMap;
import dataMap.OfflineDataMapPoint;

import PamController.OfflineDataStore;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * 
 * @author Doug Gillespie
 * 
 * Handles a series of offline tasks which all use a 
 * common data block so that data can be loaded, a whole
 * series of tasks completed and the data then saved in 
 * a single operation. 
 * <p>
 * This will be the primary interface to OfflineTasks - even
 * if there is only one task it will be in a group of one !
 *  
 *
 */
public class OfflineTaskGroup implements PamSettings {

	/**
	 * Summary list of all data blocks required by all tasks in 
	 * the list. 
	 */
	private ArrayList<PamDataBlock> requiredDataBlocks = new ArrayList<PamDataBlock>();

	/**
	 * Summary list of all data blocks affected by the list. 
	 */
	private ArrayList<PamDataBlock> affectedDataBlocks = new ArrayList<PamDataBlock>();

	private PamControlledUnit pamControlledUnit;

	private String settingsName;
	
	private TaskGroupParams taskGroupParams = new TaskGroupParams();

	/**
	 * PamControlledunit required in constructor since some bookkeeping will
	 * be goign on in the background which will need the unit type and name. 
	 * @param pamControlledUnit host contrlled unit. 
	 * @param settingsName  Name to be used in PamSettings for storing some basic information 
	 * (which tasks are selected)
	 */
	public OfflineTaskGroup(PamControlledUnit pamControlledUnit, String settingsName) {
		super();
		this.pamControlledUnit = pamControlledUnit;
		pamControlledUnit.addOfflineTaskGroup(this);
		this.settingsName = settingsName;
		PamSettingManager.getInstance().registerSettings(this);
	}

	/**
	 * Setup summary lists of required and affected datablocks
	 * based on which tasks are actually going to run .
	 */
	public void setSummaryLists() {
		requiredDataBlocks.clear();
		affectedDataBlocks.clear();
		OfflineTask aTask;
		PamDataBlock aBlock;
		for (int iTask = 0; iTask < getNTasks(); iTask++) {
			aTask = getTask(iTask);
			for (int i = 0; i < aTask.getNumRequiredDataBlocks(); i++) {
				aBlock = aTask.getRequiredDataBlock(i);
				if (requiredDataBlocks.indexOf(aBlock) < 0) {
					requiredDataBlocks.add(aBlock);
				}
			}
			for (int i = 0; i < aTask.getNumAffectedDataBlocks(); i++) {
				aBlock = aTask.getAffectedDataBlock(i);
				if (affectedDataBlocks.indexOf(aBlock) < 0) {
					affectedDataBlocks.add(aBlock);
				}
			}
		}
	}

	/**
	 * A task monitor which will receive progress updates 
	 * as the tasks complete. 
	 */
	private TaskMonitor taskMonitor;

	/**
	 * Data block used by ALL tasks in the group.
	 */
	private PamDataBlock primaryDataBlock;

	private ArrayList<OfflineTask> offlineTasks = new ArrayList<OfflineTask>();

	private TaskGroupWorker worker;

	/**
	 * Run all the tasks. 
	 * @param offlineClassifierParams 
	 * @return
	 */
	public boolean runTasks() {
		setSummaryLists();
		worker = new TaskGroupWorker();
		worker.execute();
		return true;
	}

	public void killTasks() {
		if (worker == null) {
			return;
		}
		worker.killWorker();
	}

	/**
	 * 
	 * @param task task to add to the group
	 */
	public void addTask(OfflineTask task) {
		offlineTasks.add(task);
		task.setOfflineTaskGroup(this);
		task.setDoRun(taskGroupParams.getTaskSelection(offlineTasks.size()-1));
		if (primaryDataBlock == null) {
			primaryDataBlock = task.getDataBlock();
		}
		else if (primaryDataBlock != task.getDataBlock()) {
			System.out.println(String.format("Error - cannot combine tasks with data from %s and %s",
					primaryDataBlock.getDataName(), task.getDataBlock().getDataName()));
		}
	}
	/**
	 * 
	 * @return the number of tasks in the group
	 */
	public int getNTasks() {
		return offlineTasks.size();
	}

	/**
	 * 
	 * @param iTask the task number
	 * @return the task. 
	 */
	public OfflineTask getTask(int iTask) {
		return offlineTasks.get(iTask);
	}
	/**
	 * @return the processTime
	 */
	public int getProcessTime() {
		return taskGroupParams.dataChoice;
	}

	/**
	 * @return the primaryDataBlock
	 * 
	 */
	public PamDataBlock getPrimaryDataBlock() {
		return primaryDataBlock;
	}

	/**
	 * @param primaryDataBlock the primaryDataBlock to set
	 */
	public void setPrimaryDataBlock(PamDataBlock primaryDataBlock) {
		this.primaryDataBlock = primaryDataBlock;
	}

	/**
	 * @return the taskMonitor
	 */
	public TaskMonitor getTaskMonitor() {
		return taskMonitor;
	}

	/**
	 * @param taskMonitor the taskMonitor to set
	 */
	public void setTaskMonitor(TaskMonitor taskMonitor) {
		this.taskMonitor = taskMonitor;
	}

	/**
	 * Swing worker to do the actual work. 
	 * @author Doug Gillespie
	 *
	 */
	class TaskGroupWorker extends SwingWorker<Integer, TaskMonitorData> {

		volatile boolean instantKill = false;

		private int completionStatus = TaskMonitor.TASK_IDLE;

		public void killWorker() {
			instantKill = true;
		}

		@Override
		protected Integer doInBackground() {
			completionStatus = TaskMonitor.TASK_RUNNING;
			try {
				prepareTasks();
				switch (taskGroupParams.dataChoice) {
				case TaskGroupParams.PROCESS_LOADED:
					processLoadedData();
					break;
				case TaskGroupParams.PROCESS_ALL:
					processAllData(0);
					break;
				case TaskGroupParams.PROCESS_NEW:
					processAllData(taskGroupParams.lastDataTime);
					break;
				}
				if (instantKill) {
					completionStatus = TaskMonitor.TASK_INTERRRUPTED;
				}
				else {
					completionStatus = TaskMonitor.TASK_COMPLETE;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				completionStatus = TaskMonitor.TASK_CRASHED;
			}
			completeTasks();
			return null;
		}

		private void processAllData(long lastDataTime) {
			long currentStart = primaryDataBlock.getCurrentViewDataStart();
			long currentEnd = primaryDataBlock.getCurrentViewDataEnd();

			//			synchronized(primaryDataBlock) {
			OfflineDataMap dataMap = primaryDataBlock.getPrimaryDataMap();
			int nMapPoints = dataMap.getNumMapPoints(lastDataTime);
			int iMapPoint = 0;
			publish(new TaskMonitorData(TaskMonitor.TASK_RUNNING, nMapPoints));
			publish(new TaskMonitorData(0, 0.0));
			OfflineDataStore dataSource = dataMap.getOfflineDataSource();
			Iterator<OfflineDataMapPoint> mapIterator = dataMap.getListIterator();
			OfflineDataMapPoint mapPoint;
			while (mapIterator.hasNext()) {
				mapPoint = mapIterator.next();
				if (mapPoint.getStartTime() < lastDataTime) {
					continue; // will whip through early part of list without increasing the counters
				}
				publish(new TaskMonitorData(mapPoint.getName()));
				primaryDataBlock.clearAll();
				Runtime.getRuntime().gc();
				primaryDataBlock.loadViewerData(mapPoint.getStartTime(), mapPoint.getEndTime(), null);
				primaryDataBlock.sortData();
				processData(iMapPoint, mapPoint);
				iMapPoint++;
				publish(new TaskMonitorData(iMapPoint+1, 0.0));
				if (instantKill) {
					break;
				}
			}
			//			}
			publish(new TaskMonitorData(TaskMonitor.TASK_IDLE));
			publish(new TaskMonitorData(TaskMonitor.TASK_COMPLETE));
			primaryDataBlock.loadViewerData(currentStart, currentEnd, null);
		}

		private void processLoadedData() {
			publish(new TaskMonitorData(TaskMonitor.TASK_RUNNING, 1));
			processData(0, null);
			publish(new TaskMonitorData(TaskMonitor.TASK_IDLE));
			publish(new TaskMonitorData(TaskMonitor.TASK_COMPLETE));
		}

		private void prepareTasks() {
			int nTasks = getNTasks();
			OfflineTask aTask;
			for (int iTask = 0; iTask < nTasks; iTask++) {
				aTask = getTask(iTask);
				if (aTask.canRun() == false) {
					continue;
				}
				aTask.prepareTask();
			}			
		}

		private void processData(int globalProgress, OfflineDataMapPoint mapPoint) {
			int nDatas = primaryDataBlock.getUnitsCount();
			int nSay = Math.max(1, nDatas / 100);
			int nDone = 0;
			int nTasks = getNTasks();
			PamDataUnit dataUnit;
			OfflineTask aTask;
			boolean unitChanged;
			DataUnitFileInformation fileInfo;
			/**
			 * Make sure that any data from required data blocks is loaded.
			 */
			PamDataBlock aDataBlock;
			for (int i = 0; i < requiredDataBlocks.size(); i++) {
				aDataBlock = requiredDataBlocks.get(i);
				if (aDataBlock.getCurrentViewDataStart() > primaryDataBlock.getCurrentViewDataStart() ||
						aDataBlock.getCurrentViewDataEnd() < primaryDataBlock.getCurrentViewDataStart()) {
					aDataBlock.loadViewerData(primaryDataBlock.getCurrentViewDataStart(), 
							primaryDataBlock.getCurrentViewDataEnd(), null);
				}
			}
			// remember the end time of the data so we can use the "new data" selection flag. 
			taskGroupParams.lastDataTime = primaryDataBlock.getCurrentViewDataEnd();
			//			synchronized(primaryDataBlock) {
			/*
			 * Call newDataLoaded for each task before getting on with processing individual data units. 
			 */
			for (int iTask = 0; iTask < nTasks; iTask++) {
				aTask = getTask(iTask);
				if (aTask.isDoRun() == false) {
					continue;
				}
				aTask.newDataLoad(primaryDataBlock.getCurrentViewDataStart(), 
						primaryDataBlock.getCurrentViewDataEnd(), mapPoint);

				if (taskGroupParams.deleteOld) {
					aTask.deleteOldOutput(primaryDataBlock.getCurrentViewDataStart(), 
							primaryDataBlock.getCurrentViewDataEnd(), mapPoint);
				}
			}


			/**
			 * Now process the data
			 */
			ListIterator<PamDataUnit> it = primaryDataBlock.getListIterator(0);
			unitChanged = false;
			while (it.hasNext()) {
				dataUnit = it.next();
				for (int iTask = 0; iTask < nTasks; iTask++) {
					aTask = getTask(iTask);
					if (aTask.isDoRun() == false) {
						continue;
					}
					unitChanged |= aTask.processDataUnit(dataUnit);
				}
				if (unitChanged) {
					fileInfo = dataUnit.getDataUnitFileInformation();
					if (fileInfo != null) {
						fileInfo.setNeedsUpdate(true);
					}
				}
				if (instantKill) {
					break;
				}
				nDone++;
				if (nDone%nSay == 0) {
					publish(new TaskMonitorData(globalProgress+1, (double) nDone / (double) nDatas));
				}
			}
			for (int iTask = 0; iTask < nTasks; iTask++) {
				aTask = getTask(iTask);
				if (aTask.isDoRun() == false) {
					continue;
				}
				aTask.loadedDataComplete();
			}
			//			}
			for (int i = 0; i < affectedDataBlocks.size(); i++) {
				aDataBlock = affectedDataBlocks.get(i);
				aDataBlock.saveViewerData();
			}
			publish(new TaskMonitorData(globalProgress+1, (double) nDone / (double) nDatas));
		}


		private void completeTasks() {
			int nTasks = getNTasks();
			OfflineTask aTask;
			for (int iTask = 0; iTask < nTasks; iTask++) {
				aTask = getTask(iTask);
				if (aTask.canRun() == false) {
					continue;
				}
				aTask.completeTask();
			}			
		}

		@Override
		protected void done() {
			tasksDone();
		}


		@Override
		protected void process(List<TaskMonitorData> chunks) {
			for (int i = 0; i < chunks.size(); i++) {
				newMonitorData(chunks.get(i));
			}
		}

	}

	private void newMonitorData(TaskMonitorData monData) {
		if (taskMonitor == null) {
			return;
		}
		int dataType = monData.dataType;
		if ((dataType & TaskMonitorData.SET_STATUS) != 0) {
			taskMonitor.setStatus(monData.status);
		}
		if ((dataType & TaskMonitorData.SET_NFILES) != 0) {
			taskMonitor.setNumFiles(monData.nFiles);
		}
		if ((dataType & TaskMonitorData.SET_PROGRESS) != 0) {
			taskMonitor.setProgress(monData.globalProgress, monData.loadedProgress);
			//			taskMonitor.setProgress(monData.globalProgress, .5);
		}
		if ((dataType & TaskMonitorData.SET_FILENAME) != 0) {
			taskMonitor.setFileName(monData.fileName);
		}
	}

	/**
	 * some bookkeeping - write information about task completion to the database. 
	 */
	public void tasksDone() {
		long currentStart = primaryDataBlock.getCurrentViewDataStart();
		long currentEnd = primaryDataBlock.getCurrentViewDataEnd();

	}

	@Override
	public Serializable getSettingsReference() {
		for (int i = 0; i < offlineTasks.size(); i++) {
			taskGroupParams.setTaskSelection(i, offlineTasks.get(i).isDoRun());
		}
		return taskGroupParams;
	}

	@Override
	public long getSettingsVersion() {
		return TaskGroupParams.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return settingsName;
	}

	@Override
	public String getUnitType() {
		return pamControlledUnit.getUnitType()+pamControlledUnit.getUnitName();
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		this.taskGroupParams = ((TaskGroupParams) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	/**
	 * @return the taskGroupParams
	 */
	public TaskGroupParams getTaskGroupParams() {
		return taskGroupParams;
	}
}
