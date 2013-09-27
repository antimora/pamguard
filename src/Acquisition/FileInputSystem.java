package Acquisition;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import wavFiles.ByteConverter;
import wavFiles.WavHeadChunk;
import wavFiles.WavHeader;

import Acquisition.pamAudio.PamAudioSystem;
import Acquisition.pamAudio.WavFileInputStream;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamguardVersionInfo;
import PamDetection.RawDataUnit;
import PamUtils.PamCalendar;

/**
 * Implementation of DaqSystem for reading data from audio files. 
 * 
 * @author Doug Gillespie
 * @see Acquisition.DaqSystem
 * @see Acquisition.AcquisitionProcess
 * @see FolderInputSystem
 *
 */
public class FileInputSystem  extends DaqSystem implements ActionListener, PamSettings {

	private JPanel daqDialog;
	
	protected JComboBox fileName;
	
	protected JButton fileSelect;
	
	protected JTextField fileDateText;
	
	protected AcquisitionDialog acquisitionDialog;
	
	protected FileInputParameters fileInputParameters = new FileInputParameters();
	
	protected AcquisitionControl acquisitionControl;
	
	protected int blockSamples = 4800;
	
	private JProgressBar fileProgress = new JProgressBar();
	
	private JLabel etaLabel;
	
	private JLabel speedLabel;
	
	/**
	 * using a system.currentTimeMS not PamCalander time to predict eta. 
	 */
	protected long fileStartTime;

	private volatile boolean dontStop;

	private double fileData[];

	protected AudioFormat audioFormat;

	protected AudioInputStream audioStream;
	
	protected CollectorThread collectorThread;

	protected Thread theThread;

	protected List<RawDataUnit> newDataUnits;
	
	long startTimeMS;
	
	int nChannels;
	
	float sampleRate;
	
	protected FileDate fileDate = new StandardFileDate();
	
	long fileDateMillis;
	
//	long fileLength;
	
	long fileSamples;
	
	long readFileSamples;

	protected JCheckBox repeat;

	private ByteConverter byteConverter;
	
	public FileInputSystem() {
		PamSettingManager.getInstance().registerSettings(this);
	}
	
	@Override
	public JPanel getDaqSpecificDialogComponent(AcquisitionDialog acquisitionDialog) {

		this.acquisitionDialog = acquisitionDialog;
		
		if (daqDialog == null) {
			daqDialog = createDaqDialogPanel();
		}
		
		return daqDialog;
	}

	protected JPanel createDaqDialogPanel() {
		
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Select sound file"));
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
		constraints.gridwidth = 1;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
			constraints.gridx = 0;
			addComponent(p, repeat = new JCheckBox("Repeat"), constraints);
		}
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.anchor = GridBagConstraints.EAST;
		addComponent(p, fileSelect = new JButton("Select File"), constraints);
		fileSelect.addActionListener(this);
		constraints.gridy++;
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		addComponent(p, new JLabel("File date :"), constraints);
		constraints.gridx++;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = 2;
		addComponent(p, fileDateText = new JTextField(), constraints);
		fileDateText.setEnabled(false);
		return p;
		
	}
	
	void addComponent(JPanel panel, Component p, GridBagConstraints constraints){
		((GridBagLayout) panel.getLayout()).setConstraints(p, constraints);
		panel.add(p);
	}

	@Override
	public void dialogSetParams() {
		
		fillFileList();
		
		if (repeat != null) {
			repeat.setSelected(fileInputParameters.repeatLoop);
		}
		
	}
	
	private void fillFileList() {
		// the array list will always be set up so that the items are in most
		// recently used order ...
		fileName.removeAllItems();
		String file;
		if (fileInputParameters.recentFiles.size() == 0) return;
		for (int i = 0; i < fileInputParameters.recentFiles.size(); i++){
			file = fileInputParameters.recentFiles.get(i);
			if (file == null || file.length() == 0) continue;
			fileName.addItem(file);
		}
		fileName.setSelectedIndex(0);
	}
	
	@Override
	public boolean dialogGetParams() {
		String file = (String) fileName.getSelectedItem();
		if (file != null && file.length() > 0) {
			fileInputParameters.recentFiles.remove(file);
			fileInputParameters.recentFiles.add(0, file);
			// check we're not building up too long a list. 
			while (fileInputParameters.recentFiles.size() > FileInputParameters.MAX_RECENT_FILES) {
				fileInputParameters.recentFiles.remove(fileInputParameters.recentFiles.size()-1);
				fileInputParameters.recentFiles.trimToSize();
			}
		}
		if (repeat == null) {
			fileInputParameters.repeatLoop = false;
		}
		else {
			fileInputParameters.repeatLoop = repeat.isSelected();
		}
		return true;
	}

	@Override
	public String getSystemType() {
		return "Audio File";
	}
	
	@Override
	public String getSystemName() {
		if (fileInputParameters.recentFiles == null) return null;
		if (fileInputParameters.recentFiles.size() < 1) return null;
		File f = getCurrentFile();
		if (f == null) return null;
		return f.getName();
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == fileSelect) {
			selectFile();
		}
		else if (e.getSource() == fileName) {
			setNewFile((String) fileName.getSelectedItem());
		}
		
	}
	protected void selectFile() {
		//IshmaelDetector.MatchFiltParamsDialog copies a bunch of this.  If you
		//modifiy this, please check that too.
		String currFile = (String) fileName.getSelectedItem();
		// seems to just support aif and wav files at the moment
//		Type[] audioTypes = AudioSystem.getAudioFileTypes();
//		for (int i = 0; i < audioTypes.length; i++) {
//			System.out.println(audioTypes[i]);
//		}
//		AudioStream audioStream = AudioSystem.getaudioin
		JFileChooser fileChooser = null;
		if (fileChooser == null) {
			fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new SoundFileFilter());
			fileChooser.setDialogTitle("Select audio input file...");
			//fileChooser.addChoosableFileFilter(filter);
			fileChooser.setFileHidingEnabled(true);
			fileChooser.setApproveButtonText("Select");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			javax.swing.filechooser.FileFilter[] filters = fileChooser
			.getChoosableFileFilters();
			for (int i = 0; i < filters.length; i++) {
				fileChooser.removeChoosableFileFilter(filters[i]);
			}
			fileChooser.addChoosableFileFilter(new SoundFileFilter());
			
			if (currFile != null) fileChooser.setSelectedFile(new File(currFile));
		}
		int state = fileChooser.showOpenDialog(daqDialog);
		if (state == JFileChooser.APPROVE_OPTION) {
			currFile = fileChooser.getSelectedFile().getAbsolutePath();
			//System.out.println(currFile);
			setNewFile(currFile);
		}
	}
	
	public void setNewFile (String newFile) {
		fileInputParameters.recentFiles.remove(newFile);
		fileInputParameters.recentFiles.add(0, newFile);
		fillFileList();
		interpretNewFile(newFile);
	}
	
	public void interpretNewFile(String newFile){
		if (newFile == null) return;
		if (newFile.length() == 0) return;
		File file = new File(newFile);
		if (file == null) return;
		// try to work out the date of the file
		fileDateMillis = getFileStartTime(file);
		if (fileDateMillis <= 0) {
			fileDateText.setText("Unknown file time");
		}
		else if (fileDateText != null) {
			fileDateText.setText(PamCalendar.formatDateTime(fileDateMillis));
		}
		// work out the number of channels and sample rate and set them in the main dialog
//		acquisitionDialog.NotifyChange();
		if (file.isFile() && acquisitionDialog != null) {
			try {
				AudioInputStream audioStream = PamAudioSystem.getAudioInputStream(file);

//      // Get additional information from the header if it's a wav file. 
//				if (WavFileInputStream.class.isAssignableFrom(audioStream.getClass())) {
//					WavHeader wavHeader = ((WavFileInputStream) audioStream).getWavHeader(); 
//					int nChunks = wavHeader.getNumHeadChunks();
//					for (int i = 0; i < nChunks; i++) {
//						WavHeadChunk aChunk = wavHeader.getHeadChunk(i);
//						System.out.println(String.format("Chunk %d %s: %s", i, aChunk.getChunkName(), aChunk.toString()));
//					}
//				}
				
				AudioFormat audioFormat = audioStream.getFormat();
//				fileLength = file.length();
				fileSamples = audioStream.getFrameLength();
				acquisitionDialog.setSampleRate(audioFormat.getSampleRate());
				acquisitionDialog.setChannels(audioFormat.getChannels());
				audioStream.close();
			}
			catch (Exception Ex) {
				Ex.printStackTrace();
			}
			loadByteConverter(audioFormat);
		}
	}

	private boolean loadByteConverter(AudioFormat audioFormat) {
		byteConverter = ByteConverter.createByteConverter(audioFormat);
		return (byteConverter != null);
	}

	@Override
	public void setStreamStatus(int streamStatus) {
		super.setStreamStatus(streamStatus);
		// file has ended, so notify the daq control.
		if (streamStatus == STREAM_ENDED) {
			// tell the rest of PAMGUARD to stop. 
			PamController.getInstance().stopLater();
		}
	}
		
	@Override
	public int getMaxChannels() {
		return PARAMETER_FIXED;
	}

	@Override
	public int getMaxSampleRate() {
		return PARAMETER_FIXED;
	}
	
	/* (non-Javadoc)
	 * @see Acquisition.DaqSystem#getPeak2PeakVoltage()
	 */
	@Override
	public double getPeak2PeakVoltage(int swChannel) {
		return PARAMETER_UNKNOWN;
	}

	@Override
	public boolean isRealTime() {
		return fileInputParameters.realTime;
	}

	@Override
	public boolean canPlayBack(float sampleRate) {
		// TODO Auto-generated method stub
		return false;
	}

	public Serializable getSettingsReference() {
		return fileInputParameters;
	}

	public long getSettingsVersion() {
		return FileInputParameters.serialVersionUID;
	}

	public String getUnitName() {
		return "File Input System";
	}

	public String getUnitType() {
		return "Acquisition System";
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		if (PamSettingManager.getInstance().isSettingsUnit(this, pamControlledUnitSettings)) {
			fileInputParameters = ((FileInputParameters) pamControlledUnitSettings.getSettings()).clone();
			return true;
		}
		return false;
	}

	public int getChannels() {
		return PARAMETER_UNKNOWN;
	}

	public float getSampleRate() {
		if (audioFormat == null) {
		return PARAMETER_UNKNOWN;
		}
		else {
			return audioFormat.getSampleRate();
		}
	}

	@Override
	public boolean prepareSystem(AcquisitionControl daqControl) {
		
		this.acquisitionControl = daqControl;

		fileSamples = 0;
		PamCalendar.setSoundFileTimeInMillis(0);
		// check a sound file is selected and open it.
//		if (fileInputParameters.recentFiles == null) return false;
//		if (fileInputParameters.recentFiles.size() < 1) return false;
//		String fileName = fileInputParameters.recentFiles.get(0);
		return runFileAnalysis();
	}
	
	public File getCurrentFile() {
		if (fileInputParameters.recentFiles == null) return null;
		if (fileInputParameters.recentFiles.size() < 1) return null;
		String fileName = fileInputParameters.recentFiles.get(0);
		return new File(fileName);
	}
	
	
	/* (non-Javadoc)
	 * @see Acquisition.DaqSystem#getDataUnitSamples()
	 */
	@Override
	public int getDataUnitSamples() {
		return blockSamples;
	}

	public boolean prepareInputFile() {

		File currentFile = getCurrentFile();
		if (currentFile == null) return false;

		try {
			
			if (audioStream != null) {
				audioStream.close();
			}
			
			audioStream = PamAudioSystem.getAudioInputStream(currentFile);

			audioFormat = audioStream.getFormat();
			
//			fileLength = currentFile.length();
			fileSamples = audioStream.getFrameLength();
			readFileSamples = 0;
			
			acquisitionControl.getAcquisitionProcess().setSampleRate(audioFormat.getSampleRate(), true);
			
			loadByteConverter(audioFormat);

		} catch (UnsupportedAudioFileException ex) {
			ex.printStackTrace();
			return false;
		} catch (FileNotFoundException ex) {
			System.out.println("Input filename: '" + fileName + "' not found.");
			return false;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}		
		return true;
	}
		
	
	public boolean runFileAnalysis() {
		// keep a reference to where data will be put.
		this.newDataUnits = acquisitionControl.getDaqProcess().getNewDataUnits();
		if (this.newDataUnits == null) return false;
		
		
		if (!prepareInputFile()) {
			return false;
		}

		PamCalendar.setSoundFile(true);
		PamCalendar.setSoundFileTimeInMillis(0);
		long fileTime = getFileStartTime(getCurrentFile());
		if (fileTime > 0) {
			PamCalendar.setSessionStartTime(fileTime);
		}
		else {
			PamCalendar.setSessionStartTime(System.currentTimeMillis());
		}
		
		setStreamStatus(STREAM_OPEN);
		
		// ideally we would get this from the file information.
		this.startTimeMS = PamCalendar.getTimeInMillis();

		nChannels = audioFormat.getChannels();
		
		acquisitionControl.getDaqProcess().setSampleRate(sampleRate = audioFormat.getSampleRate(), true);
//		System.out.println("Audio sample rate set to " + sampleRate);
		
		blockSamples = Math.max((int) sampleRate / 10, 1000); // make sure the
															// block has at
															// least 1000 samples
		acquisitionControl.getDaqProcess().setNumChannels(nChannels);

//		daqControl.getDaqProcess().getRawDataBlock().SetInfo(nChannels, sampleRate, blockSamples);

		fileStartTime = System.currentTimeMillis();
		
		collectorThread = new CollectorThread();

		theThread = new Thread(collectorThread);
		
		return true;
	}
	
	/**
	 * Interpret the file name to get the file time. 
	 * <p>Moved to a separate function so it can be overridden in a special version 
	 * for the DCL5 data set. 
	 * @param file audio file. 
	 * @return time in milliseconds. 
	 */
	public long getFileStartTime(File file) {
		return fileDate.getTimeFromFile(file);
	}
	
	@Override
	public boolean startSystem(AcquisitionControl daqControl) {
		
		if (audioStream == null) return false;
		
		dontStop = true;
		
		theThread.start();

		setStreamStatus(STREAM_RUNNING);
		
		return true;
	}

	@Override
	public void stopSystem(AcquisitionControl daqControl) {
		/*
		 * This only gets called when daq is stopped manually from the GUI menu.
		 * It does not get called when a file ends. 
		 */
		boolean stillRunning = (audioStream != null);

		dontStop = false; // flag to tell the file reading thread to exit
		
		int count = 0;
		while (++count < 20 && audioStream != null) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex){
				ex.printStackTrace();
			}
		}
		
//		if (audioStream != null) {
//			try{
//				audioStream.close();
//				audioStream = null;
//			}
//			catch (Exception Ex) {
//				Ex.printStackTrace();
//			}
//		}
		
		systemHasStopped(stillRunning);
	}
	
	public void systemHasStopped(boolean wasRunning) {
		long stopTime = System.currentTimeMillis();
		if (getCurrentFile() == null) {
			return;
		}
		double fileSecs = readFileSamples / getSampleRate();
		double analSecs = (stopTime - fileStartTime) / 1000.;
		System.out.println(String.format("File %s, SR=%dHz, length=%3.1fs took %3.1fs = %3.1fx real time",
				getCurrentFile().getName(), (int)getSampleRate(), fileSecs, analSecs, fileSecs / analSecs));
	}

	public class CollectorThread implements Runnable {
		
		public void run() {
			CollectData();
		}

		private void CollectData() {
			/*
			 * keep reading blocks of data from the file and creating
			 * PamDataUnits from them. Once a unit is created, tell this thread
			 * to wait until it has been used by the main thread.
			 */
			/*
			 * File should have been opened in the constructor so just read it
			 * in in chunks and pass to datablock
			 */
			int blockSize = blockSamples * audioFormat.getFrameSize();
			int bytesRead = 0;
			byte[] byteArray = new byte[blockSize];
			long totalSamples = 0;
			long lastProgressUpdate = 0;
			long lastProgressTime = 0;
			int newSamples;
			double[][] doubleData;
			short sample;
			int startbyte;
			RawDataUnit newDataUnit = null;
			long ms;
			
			while (dontStop) {
				try {
					bytesRead = audioStream.read(byteArray, 0, blockSize);
				} catch (Exception ex) {
					ex.printStackTrace();
					break; // file read error
				}
				while (bytesRead < blockSize) {
					// for single file operation, don't do anything, but need to have a hook 
					// in here to read multiple files, in which case we may just get the extra
					// samples from the next file. 
					if (bytesRead == -1) {
						bytesRead = 0;
					}
					if (openNextFile()) {
						try {
							int newBytes = audioStream.read(byteArray, bytesRead, blockSize - bytesRead);
							if (newBytes == -1) {
								break;
							}
							bytesRead += newBytes;
						} catch (Exception ex) {
							ex.printStackTrace();
							break; // file read error
						}
					}
					else {
						break;
					}
				}
				if (bytesRead > 0) {
					// convert byte array to set of double arrays, one per
					// channel					
					newSamples = bytesRead / audioFormat.getFrameSize();
					doubleData = new double[nChannels][newSamples];
					int convertedSamples = byteConverter.bytesToDouble(byteArray, doubleData, bytesRead);
					ms = acquisitionControl.getAcquisitionProcess().absSamplesToMilliseconds(totalSamples);
					
					for (int ichan = 0; ichan < nChannels; ichan++) {

						newDataUnit = new RawDataUnit(ms, 1 << ichan, totalSamples, newSamples);
						newDataUnit.setRawData(doubleData[ichan]);

						newDataUnits.add(newDataUnit);
						
						// GetOutputDataBlock().addPamData(pamDataUnit);
					}
					long blockMillis = (int) ((newDataUnit.getStartSample() * 1000) / sampleRate);
//					newDataUnit.timeMilliseconds = blockMillis;
					PamCalendar.setSoundFileTimeInMillis(blockMillis);
					if (fileSamples > 0 && totalSamples - lastProgressUpdate >= getSampleRate()*10) {
						int progress = (int) (1000 * readFileSamples / fileSamples);
						fileProgress.setValue(progress);
						sayEta();
						long now = System.currentTimeMillis();
						if (lastProgressTime > 0 && totalSamples > lastProgressUpdate) {
							double speed = (double) (totalSamples - lastProgressUpdate) / 
							getSampleRate() / ((now-lastProgressTime)/1000.);
							speedLabel.setText(String.format(" (%3.1f X RT)", speed));
						}
						lastProgressTime = now;
						lastProgressUpdate = totalSamples;
					}
					/*
					 * this is the point we wait at for the other thread to
					 * get it's act together on a timer and use this data
					 * unit, then set it's reference to zero.
					 */
					while (newDataUnits.size() > 10) {
						if (dontStop == false) break;
						try {
							Thread.sleep(1);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

					totalSamples += newSamples;
					readFileSamples += newSamples;

				} 
				else {
					break; // end of file
				}
			}
			try {
				audioStream.close();
				audioStream = null;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			// note the reason why the file has ended.
			if (dontStop == false) {
				setStreamStatus(STREAM_PAUSED);
			}
			else {
				setStreamStatus(STREAM_ENDED);
			}
			//acquisitionControl.getDaqProcess().acquisitionStopped();
//			System.out.println("quit DAQ process thread");
//			System.out.println(totalSamples + " samples read from audio file "
//					+ fileName.getSelectedItem().toString());

		}
	}
	
	protected boolean openNextFile() {
		if (fileInputParameters.repeatLoop == false) {
			return false;
		}
		// otherwise, open the same file again.
		boolean ok = prepareInputFile();
		System.out.println("Reopening same file in infinite loop " + ok);
		return ok;
	}
	
	/** Format one channel of the data in a byte array into a sample array.
	 */
	public static double[] bytesToSamples(byte[] byteArray, long nBytes, int channel,
			AudioFormat audioFormat) 
	{
		int nSamples = (int)(nBytes / audioFormat.getFrameSize());
		double[] samples = new double[nSamples];
		
		int bytesPerSample = ((audioFormat.getSampleSizeInBits() + 7) / 8);
		int byteI = channel * bytesPerSample;
		for (int isamp = 0; isamp < nSamples; isamp++) {
			samples[isamp] = 
				getSample(byteArray, byteI, bytesPerSample, audioFormat.isBigEndian());
			byteI += audioFormat.getFrameSize();
		}
		return samples;
	}
	
	/**
	 * Convenience method for getting samples from a byte array.
	 * Samples should be signed, integer, of either endian-ness, and
	 * 8, 16, 24, or 32 bits long.  Result is scaled to the range of [-1,1).
	 * Note that .wav files are little-endian and .aif files are big-endian.
	 */
	public static double getSample(byte[] buffer, int position, int bytesPerSample,
			boolean isBigEndian)
	{
		switch (bytesPerSample) {
		case 1: return buffer[position] / 256.0; //endian-ness doesn't matter here
		case 2: return (isBigEndian
				? (double)(short)(((buffer[position  ] & 0xff) << 8) | (buffer[position+1] & 0xff))
				: (double)(short)(((buffer[position+1] & 0xff) << 8) | (buffer[position  ] & 0xff)))
				/ 32768.0;
		case 3: return (isBigEndian
				? (double)(((buffer[position  ]) << 16) | ((buffer[position+1] & 0xff) << 8) | (buffer[position+2] & 0xff))
				: (double)(((buffer[position+2]) << 16) | ((buffer[position+1] & 0xff) << 8) | (buffer[position  ] & 0xff)))
				/ 8388608.0;
		case 4: return (isBigEndian
				? (double)(((buffer[position  ]) << 24) | ((buffer[position+1] & 0xff) << 16) | ((buffer[position+2] & 0xff) << 8) | (buffer[position+3] & 0xff))
				: (double)(((buffer[position+3]) << 24) | ((buffer[position+2] & 0xff) << 16) | ((buffer[position+1] & 0xff) << 8) | (buffer[position  ] & 0xff)))
				/ 2147483648.0;
		default: return 0.0;
		}
	}

	@Override
	public void daqHasEnded() {
		// TODO Auto-generated method stub
		
	}

	JPanel statusPanel;
	@Override
	public Component getStatusBarComponent() {
		if (statusPanel == null) {
			statusPanel = new JPanel();
			statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
			statusPanel.add(new JLabel("File "));
			statusPanel.add(fileProgress);
			statusPanel.add(new JLabel("  "));
			statusPanel.add(etaLabel = new JLabel(" "));
			statusPanel.add(speedLabel = new JLabel(" "));
			fileProgress.setMinimum(0);
			fileProgress.setMaximum(1000);
			fileProgress.setValue(0);
		}
		return statusPanel;
	}

	public void sayEta() {
		sayEta(getEta());
	}
	
	public long getEta() {
		double fileFraction = fileProgress.getValue() / 1000.;
		long now = System.currentTimeMillis();
		return (long) (fileStartTime + (now - fileStartTime) / fileFraction);
	}
	
	public void sayEta(long timeMs) {

		if (timeMs < 0) {
			etaLabel.setText(" ");
			return;
		}
		
		long now = System.currentTimeMillis();
		DateFormat df;
		if (timeMs - now < (6 * 3600 * 1000)) {
			df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		}
		else {
			df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
		}
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMs);
		etaLabel.setText("End " + df.format(c.getTime()));
	}

	@Override
	public String getDeviceName() {
		if (getCurrentFile() == null) {
			return null;
		}
		else {
			return getCurrentFile().getAbsolutePath();
		}
	}
}