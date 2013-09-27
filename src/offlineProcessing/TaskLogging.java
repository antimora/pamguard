package offlineProcessing;

import java.sql.Connection;
import java.sql.Types;

import PamController.PamControlledUnit;
import PamUtils.PamCalendar;

import generalDatabase.DBControlUnit;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableItem;

/**
 * Handles logging of tasks to the database. 
 * 
 * @author Doug Gillespie
 *
 */
public class TaskLogging {

	private static TaskLogging taskLogging;
	private EmptyTableDefinition tableDef;
	PamTableItem localTime, moduleType, moduleName, taskName, taskStart, taskStartMillis, taskEnd, taskEndMillis,
	completionCode; 
	
	Connection con;

	private TaskLogging() {
		tableDef = new EmptyTableDefinition("OfflineTasks");
		tableDef.addTableItem(localTime = new PamTableItem("PCLocalTime", Types.TIMESTAMP));
		tableDef.addTableItem(moduleType = new PamTableItem("Module Type", Types.CHAR, 50));
		tableDef.addTableItem(moduleName = new PamTableItem("Module Name", Types.CHAR, 50));
		tableDef.addTableItem(taskName = new PamTableItem("Task Name", Types.CHAR, 50));
		tableDef.addTableItem(taskStart = new PamTableItem("TaskStart", Types.TIMESTAMP));
		tableDef.addTableItem(taskStartMillis = new PamTableItem("TaskStartMillis", Types.INTEGER));
		tableDef.addTableItem(taskEnd = new PamTableItem("TaskEnd", Types.TIMESTAMP));
		tableDef.addTableItem(taskEndMillis = new PamTableItem("TaskEndMillis", Types.INTEGER));
		tableDef.addTableItem(completionCode = new PamTableItem("CompletionCode", Types.CHAR, 20));
		
		/**
		 * Note that completionCode Strings can be got from 
		 * TaskMonitorData.getStatusString
		 */
	}
	
	public static TaskLogging getTaskLogging() {
		if (taskLogging == null) {
			taskLogging = new TaskLogging();
		}
		taskLogging.checkConnection();
		return taskLogging;
	}

	private void checkConnection() {
		// TODO Auto-generated method stub
		Connection currentCon = DBControlUnit.findConnection();
		if (currentCon == con) {
			return;
		}
		/**
		 * Need to check tables, etc. 
		 */
		currentCon = con;
		DBControlUnit.findDatabaseControl().getDbProcess().checkTable(tableDef);
	}
	
	public boolean logTask(PamControlledUnit pcu, OfflineTask task, long startTime, 
			long endTime, int completionStatus) {
		localTime.setValue(PamCalendar.getTimeStamp(System.currentTimeMillis()));
		moduleType.setValue(pcu.getUnitType());
		moduleName.setValue(pcu.getUnitType());
		taskName.setValue(task.getName());
		taskStart.setValue(PamCalendar.getTimeStamp(startTime));
		taskEnd.setValue(PamCalendar.getTimeStamp(endTime));
		taskStartMillis.setValue(startTime%1000);
		taskEndMillis.setValue(endTime%1000);
		completionCode.setValue(TaskMonitorData.getStatusString(completionStatus));
		
		return false;
		
	}
	
}
