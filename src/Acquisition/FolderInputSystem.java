package Acquisition;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import Acquisition.pamAudio.PamAudioSystem;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamguardVersionInfo;
import PamUtils.PamAudioFileFilter;
import PamUtils.PamCalendar;
import PamUtils.PamFileFilter;

/**
 * Read multiple files in sequence. Options exist to either pause and
 * restart analysis after each file, or to merge files into one long
 * continuous sound stream.
 * 
 * @author Doug Gillespie
 *
 */
public class FolderInputSystem extends FileInputSystem implements PamSettings{

	//	Timer timer;

	private boolean running = false;

	private ArrayList<File> allFiles = new ArrayList<File>();

	private int currentFile;

	private PamFileFilter audioFileFilter = new PamAudioFileFilter();

	private Timer newFileTimer;

	private JCheckBox subFolders, mergeFiles;

	private JButton checkFiles;

	private long eta = -1;

	private FolderInputParameters folderInputParameters;

	@Override
	public boolean runFileAnalysis() {
		currentFileStart = System.currentTimeMillis();
		return super.runFileAnalysis();
	}

	long currentFileStart;

	public FolderInputSystem() {
		//		super();
		setFolderInputParameters(new FolderInputParameters());
		PamSettingManager.getInstance().registerSettings(this);
		makeSelFileList();
		newFileTimer = new Timer(1000, new RestartTimer());
		newFileTimer.setRepeats(false);
		//		timer = new Timer(1000, new TimerAction());
	}

	/**
	 * Restarts after a file has ended when processing multiple files. 
	 * 27 Jan 2011 - this now reschedules in the AWT thread
	 * @author Doug Gillespie
	 *
	 */
	class RestartTimer implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			newFileTimer.stop();
			PamController.getInstance().startLater(false); //don't save settings on restarts

		}

	}
	@Override
	protected JPanel createDaqDialogPanel() {
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Select sound file folder or multiple files"));
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[]{100, 100, 10};
		p.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(2,2,2,2);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addComponent(p, fileName = new JComboBox(), constraints);
		fileName.addActionListener(this);
		fileName.setMinimumSize(new Dimension(30,2));
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		addComponent(p, subFolders = new JCheckBox("Include sub folders"), constraints);
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.anchor = GridBagConstraints.EAST;
		addComponent(p, fileSelect = new JButton("Select Folder or Files"), constraints);
		fileSelect.addActionListener(new FindAudioFolder());
		repeat = new JCheckBox("Repeat: At end of file list, start again");
		if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
			constraints.gridy++;
			constraints.gridx = 0;
			constraints.gridwidth = 3;
			constraints.anchor = GridBagConstraints.WEST;
			addComponent(p, repeat, constraints);
		}
		constraints.gridy++;
		constraints.gridx = 0;
		constraints.gridwidth = 1;
		constraints.anchor = GridBagConstraints.WEST;
		//		constraints.gridy++;
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		addComponent(p, new JLabel("File date :"), constraints);
		constraints.gridx++;
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addComponent(p, fileDateText = new JTextField(), constraints);
		fileDateText.setEnabled(false);
		constraints.gridy++;
		constraints.gridx = 0;
		constraints.gridwidth = 2;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		addComponent(p, mergeFiles = new JCheckBox("Merge contiguous files"), constraints);
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			constraints.gridx+=2;
			constraints.gridwidth = 1;
			addComponent(p, checkFiles = new JButton("Check File Headers..."), constraints);
			checkFiles.addActionListener(new CheckFiles());
		}
		return p;
	}

	class CheckFiles implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			checkFileHeaders();
		}
	}

	/**
	 * Checks file length matched actual file data length and repairs if necessary. 
	 */
	private void checkFileHeaders() {
		CheckWavFileHeaders.showDialog(acquisitionDialog, this);
	}

	class FindAudioFolder implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			selectFolder();

		}

	}	

	int makeSelFileList() {

		if (fileInputParameters.recentFiles == null || fileInputParameters.recentFiles.size() < 1) {
			return 0;
		}

		if (folderInputParameters.selectedFiles != null && folderInputParameters.selectedFiles.length > 0) {
			return makeSelFileList(folderInputParameters.selectedFiles);
		}

		String folderName = fileInputParameters.recentFiles.get(0);

		if (folderName == null) return 0;

		File[] currentFolder = new File[1];

		currentFolder[0] = new File(folderName);

		return makeSelFileList(currentFolder);
	}

	int makeSelFileList(String fileOrFolder) {
		File[] file = new File[1];
		file[0] = new File(fileOrFolder);
		return makeSelFileList(file);

	}

	int makeSelFileList(File[] fileList) {

		allFiles.clear();

		currentFile = 0;

		if (fileInputParameters.recentFiles == null || fileInputParameters.recentFiles.size() < 1) return 0;

		String folderName = fileInputParameters.recentFiles.get(0);

		if (folderName == null) return 0;

		File currentFolder = new File(folderName);

		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isDirectory()) {
				addFolderFiles(currentFolder);
			}
			else if (fileList[i].isFile()) {
				allFiles.add(fileList[i]);
			}
		}

		if (allFiles.size() > 0) {

		}
		folderProgress.setMinimum(0);
		folderProgress.setMaximum(allFiles.size());
		folderProgress.setValue(0);

		Collections.sort(allFiles);
		
		return allFiles.size();

	}

	void addFolderFiles(File folder) {
		File[] files = folder.listFiles(audioFileFilter);
		if (files == null) return;
		boolean includeSubFolders = folderInputParameters.subFolders;
		File file;
		for (int i = 0; i < files.length; i++) {
			file = files[i];
			if (file.isDirectory() && includeSubFolders) {
				System.out.println(file.getAbsoluteFile());
				addFolderFiles(file.getAbsoluteFile());
			}
			else if (file.isFile()) {
				allFiles.add(file);
			}
		}
	}

	protected void selectFolder() {
		JFileChooser fc = null;

		if (fc == null) {
			fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setMultiSelectionEnabled(true);
			fc.setFileFilter(new SoundFileFilter());
		}

		if (fileName.getSelectedIndex() >= 0) {
			fc.setCurrentDirectory(new File(fileName.getSelectedItem().toString()));
		}

		if (folderInputParameters.selectedFiles != null) {
			fc.setSelectedFiles(folderInputParameters.selectedFiles);
		}

		int ans = fc.showDialog(null, "Select files and folders");

		if (ans == JFileChooser.APPROVE_OPTION) {
			/*
			 * if it's a single directory that's been selected, then 
			 * set that with setNewFile. If multiple files and directories
			 * are accepted, select the parent directory of all of them. 
			 */
			File[] files = fc.getSelectedFiles();
			if (files.length <= 0) return;
			else if (files.length == 1) {
				setNewFile(fc.getSelectedFile().toString());
			}
			else {
				// take the folder name from the first file
				File aFile = files[0];
				setNewFile(aFile.getAbsolutePath());
			}
			folderInputParameters.selectedFiles = fc.getSelectedFiles();

			makeSelFileList(fc.getSelectedFiles());
		}
	}

	protected String getCurrentFolder() {
		if (folderInputParameters.recentFiles.size() == 0) {
			return null;
		}
		return folderInputParameters.recentFiles.get(0);
	}

	@Override
	public void interpretNewFile(String newFile) {
		if (newFile == null) {
			return;
		}
		makeSelFileList(newFile);
		setFileDateText();
		// also open up the first file and get the sample rate and number of channels from it
		// and set these
		File file = getCurrentFile();
		if (file == null) return;
		AudioInputStream audioStream;
		if (file.isFile() && acquisitionDialog != null) {
			try {
				audioStream = PamAudioSystem.getAudioInputStream(file);
				AudioFormat audioFormat = audioStream.getFormat();
//				fileLength = file.length();
//				fileSamples = fileLength / audioFormat.getFrameSize();
				fileSamples = audioStream.getFrameLength();
				acquisitionDialog.setSampleRate(audioFormat.getSampleRate());
				acquisitionDialog.setChannels(audioFormat.getChannels());
				audioStream.close();
			}
			catch (Exception Ex) {
				Ex.printStackTrace();
			}
		}
	}

	public void setFileDateText() {
		if (allFiles.size() > 0) {
			long fileTime = getFileStartTime(getCurrentFile());
			fileDateText.setText(PamCalendar.formatDateTime(fileTime));
		}
	}

	@Override
	public String getSystemType() {
		return "Audio file folder or multiple files";
	}

	@Override
	public String getUnitName() {
		return "File Folder Analysis";
	}

	@Override
	public String getUnitType() {
		return "File Folder Acquisition System";
	}

	@Override
	public File getCurrentFile() {
		if (allFiles != null && allFiles.size() > currentFile) {
			return allFiles.get(currentFile);
		}
		return null;
	}

	//	private float currentSampleRate;

	@Override
	protected boolean openNextFile() {
		boolean ans = false;
		if (folderInputParameters.mergeFiles == false) return false;
		if (++currentFile < allFiles.size()) {
			// also check to see if the start time of the next file is the same as the 
			// end time of the current file.
			long fileEndTime = PamCalendar.getTimeInMillis();
			long lastBit = (long) ((blockSamples * 1000L) / getSampleRate());
			fileEndTime += lastBit;
			long newStartTime = getFileStartTime(getCurrentFile());
			long diff = newStartTime - fileEndTime;
			if (diff > 2000 || diff < -5000 || newStartTime == 0) {
				currentFile--;
				return false;
				/*
				 * Return since it's not possible to merge this file into the 
				 * next one. In this instance, DAQ will restart, and the currentfile
				 * counter will increment elsewhere. 
				 */
			}
			setFolderProgress();
//			sayEta();
			ans = prepareInputFile();
			currentFileStart = System.currentTimeMillis();
			//			if (ans && audioFormat.getSampleRate() != currentSampleRate && currentFile > 0) {
			//				acquisitionControl.getDaqProcess().setSampleRate(currentSampleRate = audioFormat.getSampleRate(), true);
			//			}
		}
		return ans;
	}

	@Override
	public void daqHasEnded() {
		currentFile++;
		if (folderInputParameters.repeatLoop && currentFile >= allFiles.size()) {
			currentFile = 0;
		}
		if (currentFile < allFiles.size()) {
			// only restart if the file ended - not if it stopped
			if (getStreamStatus() == STREAM_ENDED) {
				newFileTimer.start();
			}
		}
		calculateETA();
		setFolderProgress();
	}

	private void setFolderProgress() {
		folderProgress.setValue(currentFile);
	}
	
	private void calculateETA() {
		long now = System.currentTimeMillis();
		eta = now-currentFileStart;
		eta *= (allFiles.size()-currentFile);
		eta += now;
	}


	JPanel barBit;
	JProgressBar folderProgress = new JProgressBar();
	@Override
	public Component getStatusBarComponent() {

		if (barBit == null) {
			barBit = new JPanel();
			barBit.setLayout(new BoxLayout(barBit, BoxLayout.X_AXIS));
			barBit.add(new JLabel("Folder "));
			barBit.add(folderProgress);
			barBit.add(new JLabel("   "));
			barBit.add(super.getStatusBarComponent());
		}
		return barBit;
	}

	@Override
	public long getEta() {
		if (currentFile == allFiles.size()-1) {
			return super.getEta();
		}
		return eta;
	}

	@Override
	public Serializable getSettingsReference() {
		return folderInputParameters;
	}

	@Override
	public long getSettingsVersion() {
		return FolderInputParameters.serialVersionUID;
	}

	@Override
	public boolean dialogGetParams() {
		folderInputParameters.subFolders = subFolders.isSelected();
		folderInputParameters.mergeFiles = mergeFiles.isSelected();
		folderInputParameters.repeatLoop = repeat.isSelected();
		currentFile = 0;
		return super.dialogGetParams();
	}

	@Override
	public void dialogSetParams() {
		super.dialogSetParams();
		subFolders.setSelected(folderInputParameters.subFolders);
		mergeFiles.setSelected(folderInputParameters.mergeFiles);
		repeat.setSelected(folderInputParameters.repeatLoop);
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {

		FolderInputParameters newParams;

		try {
			newParams = (FolderInputParameters) pamControlledUnitSettings.getSettings();
		}
		catch (ClassCastException ex) {
			return false;
		}
		setFolderInputParameters(newParams);
		return true;
	}

	public FolderInputParameters getFolderInputParameters() {
		return folderInputParameters;
	}

	public void setFolderInputParameters(FolderInputParameters folderInputParameters) {
		this.folderInputParameters = folderInputParameters;
		fileInputParameters = this.folderInputParameters;
	}

	@Override
	public boolean startSystem(AcquisitionControl daqControl) {
		System.out.println("Start system");
		setFolderProgress();
		return super.startSystem(daqControl);
	}

//	/**
//	 * @param audioFileFilter the audioFileFilter to set
//	 */
//	public void setAudioFileFilter(PamFileFilter audioFileFilter) {
//		this.audioFileFilter = audioFileFilter;
//	}
//
//	/**
//	 * @return the audioFileFilter
//	 */
//	public PamFileFilter getAudioFileFilter() {
//		return audioFileFilter;
//	}
	@Override
	public String getDeviceName() {
		if (fileInputParameters.recentFiles == null || fileInputParameters.recentFiles.size() < 1) {
			return null;
		}
		return fileInputParameters.recentFiles.get(0);
	}
}
