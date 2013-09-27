package offlineProcessing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import PamUtils.PamCalendar;
import PamView.CancelObserver;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamguardMVC.PamDataBlock;

/**
 * Dialog for offline processing of a particular data type.<br>
 * Will offer the user choices in how to select data (e.g. loaded, or all)
 * and then scroll through those data passing one data unit at a time to a series
 * of tasks which will have been added by the programmer, but can be individually 
 * turned off and on by the user. Each task will have a check box to enable it and 
 * an optional button to configure it. 
 * Bottom part of the dialog shows a progress indicator.  
 * @author Douglas Gillespie
 *
 */
public class OLProcessDialog extends PamDialog {

	private OfflineTaskGroup taskGroup;

	private JComboBox dataSelection;
	private JCheckBox[] taskCheckBox;
	private JButton[] settingsButton;
	private JLabel status, currFile;
	private JProgressBar globalProgress, fileProgress;
	private JCheckBox deleteOldData;
	private JLabel dataInfo;

	int currentStatus = TaskMonitor.TASK_IDLE;

	public OLProcessDialog(Window parentFrame, OfflineTaskGroup taskGroup, String title) {
		super(parentFrame, title, false);
		this.taskGroup = taskGroup;
		taskGroup.setTaskMonitor(new OLMonitor());

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		JPanel dataSelectPanel = new JPanel(new BorderLayout());
		dataSelectPanel.setBorder(new TitledBorder("Data Options"));
		dataSelectPanel.add(BorderLayout.WEST, new JLabel("Data "));
		dataInfo = new JLabel(" ", SwingConstants.CENTER); // create this first to avoid null pointer exception
		dataSelectPanel.add(BorderLayout.CENTER, dataSelection = new JComboBox());
		dataSelection.addActionListener(new DataSelectListener());
		dataSelection.addItem("Loaded Data");
		dataSelection.addItem("All Data");
		dataSelection.addItem("New Data");
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new BorderLayout());
		southPanel.add(BorderLayout.NORTH, dataInfo);
//		dataSelection.setSelectedIndex(offlineClassifierParams.dataChoice);
		southPanel.add(BorderLayout.SOUTH, deleteOldData = new JCheckBox("Delete old database entries"));
		deleteOldData.setToolTipText("<html>" +
				"Delete old data entries in the corresponding database table<p>" +
				"(Binary file data will always be overwritten)</html>)");
		dataSelectPanel.add(BorderLayout.SOUTH, southPanel);
				

		JPanel tasksPanel = new JPanel(new GridBagLayout());
		tasksPanel.setBorder(new TitledBorder("Tasks"));
		int nTasks = taskGroup.getNTasks();
		taskCheckBox = new JCheckBox[nTasks];
		settingsButton = new JButton[nTasks];
		OfflineTask aTask;		
		JButton aButton;
		GridBagConstraints c = new PamGridBagContraints();
		for (int i = 0; i < nTasks; i++) {
			c.gridx = 0;
			aTask = taskGroup.getTask(i);
			addComponent(tasksPanel, taskCheckBox[i] = new JCheckBox(aTask.getName()), c);
			taskCheckBox[i].addActionListener(new SelectionListener(aTask, taskCheckBox[i]));
			c.gridx++;
			if (aTask.hasSettings()) {
				addComponent(tasksPanel, settingsButton[i] = new JButton("Settings ..."), c);
				settingsButton[i].addActionListener(new SettingsListener(aTask));
			}
			c.gridy++;
		}

		JPanel progressPanel = new JPanel(new GridBagLayout());
		progressPanel.setBorder(new TitledBorder("Progress"));
		c = new PamGridBagContraints();
		addComponent(progressPanel, status = new JLabel(" "), c);
		c.gridx++;
		addComponent(progressPanel, currFile = new JLabel(" "), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(progressPanel, new JLabel("File ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(progressPanel, fileProgress = new JProgressBar(), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(progressPanel, new JLabel("All Data ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(progressPanel, globalProgress = new JProgressBar(), c);

		mainPanel.add(dataSelectPanel);
		mainPanel.add(tasksPanel);
		mainPanel.add(progressPanel);

		getOkButton().setText("Start");
		
		setCancelObserver(new CancelObserverOLDialog());

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WinListener());

		setDialogComponent(mainPanel);

		setParams();

		enableControls();

	}

	private void setParams() {
		TaskGroupParams taskGroupParams = taskGroup.getTaskGroupParams();
		int nTasks = taskGroup.getNTasks();
		dataSelection.setSelectedIndex(taskGroupParams.dataChoice);
		deleteOldData.setSelected(taskGroupParams.deleteOld);
		OfflineTask aTask;
		for (int i = 0; i < nTasks; i++) {
			aTask = taskGroup.getTask(i);
			taskCheckBox[i].setSelected(taskGroupParams.getTaskSelection(i));
		}
//		deleteOldData.setSelected(offlineClassifierParams.deleteOld);
	}
	

	
	class CancelObserverOLDialog implements CancelObserver {

		@Override
		public boolean cancelPressed() {
			if (currentStatus==TaskMonitor.TASK_RUNNING) {	
			cancelButtonPressed(); 
			return false;
			}
			return true;
		}
		
	}

	@Override
	protected void okButtonPressed() {
		if (getParams() == false) {
			return;
		}
		if (taskGroup.runTasks()) {
			currentStatus = TaskMonitor.TASK_RUNNING;
			getCancelButton().setText("Stop!");
		}
	}

	private void enableControls() {
		boolean nr = currentStatus != TaskMonitor.TASK_RUNNING;		
		int nTasks = taskGroup.getNTasks();
		OfflineTask aTask;
		int selectedTasks = 0;
		for (int i = 0; i < nTasks; i++) {
			aTask = taskGroup.getTask(i);
			taskCheckBox[i].setEnabled(aTask.canRun() && nr);
			if (aTask.canRun() == false) {
				taskCheckBox[i].setSelected(false);
			}
			if (settingsButton[i] != null) {
				settingsButton[i].setEnabled(nr);
			}
			if (taskCheckBox[i].isSelected()) {
				selectedTasks++;
			}
		}
		getOkButton().setEnabled(selectedTasks > 0 && nr);
	}

	@Override
	public void cancelButtonPressed() {
		if (currentStatus == TaskMonitor.TASK_RUNNING) {
			taskGroup.killTasks();
			currentStatus=TaskMonitor.TASK_INTERRRUPTED;
			enableControls();
			getCancelButton().setText("Close");
			
		}
		else  {}
	}

	@Override
	public boolean getParams() {
		TaskGroupParams taskGroupParams = taskGroup.getTaskGroupParams();
		taskGroupParams.dataChoice = dataSelection.getSelectedIndex();
		int nTasks = taskGroup.getNTasks();
		OfflineTask aTask;
		for (int i = 0; i < nTasks; i++) {
			aTask = taskGroup.getTask(i);
			aTask.setDoRun(taskCheckBox[i].isSelected());
			taskGroupParams.setTaskSelection(i, taskCheckBox[i].isSelected());
		}
		taskGroupParams.deleteOld = deleteOldData.isSelected();
		return true;
	}

	public void newDataSelection() {
		int sel = dataSelection.getSelectedIndex();
		PamDataBlock primaryDataBlock = taskGroup.getPrimaryDataBlock();
		String selStr = null;
		switch (sel) {
		case TaskGroupParams.PROCESS_ALL:
			selStr = "Process all data";
			break;
		case TaskGroupParams.PROCESS_LOADED:
			long msPerDay = 3600L*24000L;
			long startDay = primaryDataBlock.getCurrentViewDataEnd()/msPerDay;
			long endDay = primaryDataBlock.getCurrentViewDataEnd()/msPerDay;
			if (endDay == startDay) {
				selStr = String.format("%s to %s", PamCalendar.formatDateTime(primaryDataBlock.getCurrentViewDataStart()),
						PamCalendar.formatTime(primaryDataBlock.getCurrentViewDataEnd()));
			}
			else {
				selStr = String.format("%s to %s", PamCalendar.formatDateTime(primaryDataBlock.getCurrentViewDataStart()),
						PamCalendar.formatDateTime(primaryDataBlock.getCurrentViewDataEnd()));
			}
			break;
		case TaskGroupParams.PROCESS_NEW:
			selStr = String.format("All data from %s", 
					PamCalendar.formatDateTime(taskGroup.getTaskGroupParams().lastDataTime));
			break;
		}
		dataInfo.setText(selStr);
		dataSelection.setToolTipText(selStr);
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

	class WinListener extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent arg0) {
			if (currentStatus == TaskMonitor.TASK_RUNNING) {
				return;
			}
			setVisible(false);
		}

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setParams();
			enableControls();
		}
	}

	class DataSelectListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			newDataSelection();
		}
	}
	/**
	 * Listener for selecting / deselecting individual tasks. 
	 * @author Doug Gillespie
	 *
	 */
	class SelectionListener implements ActionListener {

		private OfflineTask offlineTask;
		
		private JCheckBox checkBox;

		public SelectionListener(OfflineTask offlineTask, JCheckBox checkBox) {
			this.offlineTask = offlineTask;
			this.checkBox = checkBox;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			offlineTask.setDoRun(checkBox.isSelected());
			enableControls();
		}

	}

	/**
	 * Listner for settings buttons
	 * @author Doug Gillespie
	 *
	 */
	class SettingsListener implements ActionListener {

		private OfflineTask offlineTask;

		public SettingsListener(OfflineTask offlineTask) {
			this.offlineTask = offlineTask;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			offlineTask.callSettings();
		}

	}

	/**
	 * Monitor for AWT calls back from the thing running the tasks
	 * which will be running in a separate thread. 
	 * @author Doug Gillespie
	 *
	 */
	class OLMonitor implements TaskMonitor {

		int doneFiles = 0;

		int numFiles = 0;

		@Override
		public void setFileName(String fileName) {
			//			currFile.setText(fileName);
			if (taskGroup.getTaskGroupParams().dataChoice == TaskGroupParams.PROCESS_LOADED) {
				currFile.setText("Loaded data");
			}
			currFile.setText(String.format("File %d of %d", doneFiles, numFiles));
		}

		@Override
		public void setNumFiles(int nFiles) {
			globalProgress.setMaximum(numFiles = nFiles);
		}

		@Override
		public void setProgress(int global, double loaded) {
			doneFiles = global;
			globalProgress.setValue(global);
			fileProgress.setValue((int) (loaded*100));
		}

		@Override
		public void setStatus(int taskStatus) {
			status.setText(TaskMonitorData.getStatusString(taskStatus));
			currentStatus=taskStatus;
			enableControls();
			switch(taskStatus) {
			case TaskMonitor.TASK_IDLE:
			case TaskMonitor.TASK_COMPLETE:
				getCancelButton().setText("Close");
				break;
			case TaskMonitor.TASK_RUNNING:
				getCancelButton().setText("Stop!");
				break;
			default:
				getCancelButton().setText("Close");
				break;
			}
		}
	}
}
