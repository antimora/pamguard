package SoundRecorder;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import Acquisition.RawSourceDialogPanel;
import PamUtils.SelectFolder;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class RecorderSettingsDialog extends PamDialog {
	
	private static RecorderSettingsDialog singleInstance;
	
	private RawSourceDialogPanel sourcePanel;
	
	private BufferPanel bufferPanel;
	
	private SelectFolder selectFolder;
	
	private OutputFormat outputFormat;
	
	private FileLengthPanel fileLengthPanel;
	
	private AutoPanel autoPanel;
	
	private RecorderSettings recorderSettings;
	
//	JButton okButton, cancelButton;
	
	private RecorderSettingsDialog (Frame parentFrame) {
		
		super(parentFrame, "Sound Recording Settings", false);
		
		
		sourcePanel = new RawSourceDialogPanel("Raw data source");
		bufferPanel = new BufferPanel();
		selectFolder = new SelectFolder("Select output folder", 30);
		outputFormat = new OutputFormat();
		fileLengthPanel = new FileLengthPanel();
		autoPanel = new AutoPanel();
		
		JTabbedPane mainPanel = new JTabbedPane();
		
		
		JPanel p = new JPanel();
		//p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		//p.setLayout(new GridLayout(2,1));
		p.setLayout(gb);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		addComponent(p, sourcePanel, c);
		c.gridy ++;
		addComponent(p, bufferPanel, c);
		c.gridy ++;
		addComponent(p, selectFolder.getFolderPanel(), c);
		c.gridy ++;
		addComponent(p, outputFormat, c);
		c.gridy ++;
		addComponent(p, fileLengthPanel, c);
		c.gridy ++;
		addComponent(p, autoPanel, c);

		mainPanel.add("General", p);

		setHelpPoint("sound_processing.soundRecorderHelp.docs.RecorderOverview");
		
		setDialogComponent(mainPanel);
		
	}
	
	public static RecorderSettings showDialog(Frame parentFrame, RecorderSettings recorderSettings) {
//		if (singleInstance == null || singleInstance.getOwner() != parentFrame)  {
			singleInstance = new RecorderSettingsDialog(parentFrame);
//		}
		singleInstance.recorderSettings = recorderSettings.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.recorderSettings;
	}
	
	private void setParams() {
		sourcePanel.setSource(recorderSettings.rawDataSource);
		bufferPanel.setParams();
		selectFolder.setFolderName(recorderSettings.outputFolder);
		outputFormat.setParams();
		fileLengthPanel.setParams();
		autoPanel.setParams();
		pack();
	}
	
	@Override
	public boolean getParams() {
		recorderSettings.rawDataSource = sourcePanel.getSource();
		if (recorderSettings.rawDataSource == null) return showWarning("No raw data source");;
		if (bufferPanel.getParams() == false) return showWarning("Error in buffer settings");;
		recorderSettings.outputFolder = selectFolder.getFolderName(true);
		if (outputFormat.getParams() == false) return showWarning("Error in output format settings");;
		if (fileLengthPanel.getParams() == false) return showWarning("Error in file length settings");;
		if (autoPanel.getParams() == false) return showWarning("Error in automatic recording settings");;
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		recorderSettings = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}

//	public void actionPerformed(ActionEvent e) {
//
//		if (e.getSource() == okButton) {
//			if (getParams()) setVisible(false);
//		}
//		else if (e.getSource() == cancelButton) {
//			recorderSettings = null;
//			setVisible(false);
//		}
//		
//	}
	class BufferPanel extends JPanel {
		JCheckBox autoStart;
		JCheckBox enableBuffer;
		JTextField bufferLength;
		BufferPanel() {
			super();
			setBorder(new TitledBorder("Recording Options"));
			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new PamGridBagContraints();
			setLayout(gb);
			c.gridwidth = 1;
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 3;
			autoStart = new JCheckBox("Automatically re-start recording after PAMGUARD Stop / Start");
			addComponent(this, autoStart, c);
			c.gridy++;
			c.gridwidth = 1;
			addComponent(this, enableBuffer = new JCheckBox("Enable Buffer - length  "), c);
//			c.gridx ++;
//			c.gridwidth = 1;
//			addComponent(this, new JLabel("Buffer length "), c);
			c.gridx++;
			addComponent(this, bufferLength = new JTextField(4), c);
			c.gridx++;
			addComponent(this, new JLabel(" s "), c);
		}
		
		void setParams() {
			autoStart.setSelected(recorderSettings.autoStart);
			enableBuffer.setSelected(recorderSettings.enableBuffer);
			bufferLength.setText(String.format("%d", recorderSettings.bufferLength));
		}
		
		boolean getParams() {
			recorderSettings.autoStart = autoStart.isSelected();
			recorderSettings.enableBuffer = enableBuffer.isSelected();
			try {
				recorderSettings.bufferLength = Integer.valueOf(bufferLength.getText());
			}
			catch (NumberFormatException Ex) {
				return false;
			}
			return true;
		}
	}
	
	class FileLengthPanel extends JPanel {
		
		JCheckBox limitLengthSeconds, limitLengthMegaBytes;
		JTextField maxLengthSeconds, maxLengthMegaBytes;
		
		FileLengthPanel() {
			super();
			limitLengthSeconds = new JCheckBox("Limit file length in seconds  ");
			limitLengthMegaBytes = new JCheckBox("Limit file length in megabytes  ");
			maxLengthSeconds = new JTextField(6);
			maxLengthMegaBytes = new JTextField(6);
			setBorder(new TitledBorder("Maximum file lengths"));
			GridBagConstraints c = new GridBagConstraints();
			GridBagLayout gb = new GridBagLayout();
			setLayout(gb);
			c.gridx = c.gridy = 0;
			c.anchor = GridBagConstraints.WEST;
			addComponent(this, limitLengthSeconds, c);
			c.gridx++;
			addComponent(this, maxLengthSeconds, c);
			c.gridx++;
			addComponent(this, new JLabel(" s"), c);
			c.gridx = 0;
			c.gridy = 1;
			addComponent(this, limitLengthMegaBytes, c);
			c.gridx++;
			addComponent(this, maxLengthMegaBytes, c);
			c.gridx++;
			addComponent(this, new JLabel(" Mbytes"), c);
		}
		void setParams() {
			limitLengthSeconds.setSelected(recorderSettings.limitLengthSeconds);
			limitLengthMegaBytes.setSelected(recorderSettings.limitLengthMegaBytes);
			maxLengthSeconds.setText(String.format("%d", recorderSettings.maxLengthSeconds));
			maxLengthMegaBytes.setText(String.format("%d", recorderSettings.maxLengthMegaBytes));
		}
		boolean getParams() {
			recorderSettings.limitLengthSeconds = limitLengthSeconds.isSelected();
			recorderSettings.limitLengthMegaBytes = limitLengthMegaBytes.isSelected();
			try {
				recorderSettings.maxLengthSeconds = Integer.valueOf(maxLengthSeconds.getText());
				recorderSettings.maxLengthMegaBytes = Integer.valueOf(maxLengthMegaBytes.getText());
			}
			catch (NumberFormatException Ex) {
//				Ex.printStackTrace();
				return false;
			}
			return true;
		}
	}
	
	class OutputFormat extends JPanel {
		
		JTextField fileInitials;
		
		JComboBox fileType;
		
		OutputFormat() {
			super();
			fileType = new JComboBox();
			setBorder(new TitledBorder("File name and type"));
			GridBagConstraints c = new GridBagConstraints();
			GridBagLayout gb = new GridBagLayout();
			setLayout(gb);
			c.gridx = c.gridy = 0;
			c.anchor = GridBagConstraints.WEST;
			addComponent(this, new JLabel("File name prefix "), c);
			c.gridx++;
			addComponent(this, fileInitials = new JTextField(5), c);
			c.gridx++;
			addComponent(this, new JLabel("                              File type "), c);
			c.gridx++;
			addComponent(this, fileType, c);
			c.gridwidth = 4;
			c.gridx = 0;
			c.gridy ++;
			addComponent(this, new JLabel("(file names automatically contain the date in the format YYYYMMDD_HHMMSS)"), c);
		}
		void setParams() {
			fileType.removeAllItems();
			AudioFileFormat.Type types[] = AudioSystem.getAudioFileTypes();
			for (int i = 0; i < types.length; i++) {
				fileType.addItem(types[i]);
			}
			fileType.setSelectedItem(recorderSettings.getFileType());
			fileInitials.setText(recorderSettings.fileInitials);
		}
		boolean getParams() {
			recorderSettings.setFileType((AudioFileFormat.Type) fileType.getSelectedItem());
			if (recorderSettings.getFileType() == null) return false;
			try {
				recorderSettings.fileInitials = fileInitials.getText();
			}
			catch (NullPointerException Ex) {
				return false;
			}
			return true;
		}
	}
	
	class AutoPanel extends JPanel {
		JTextField autoInterval, autoDuration;
		AutoPanel() {
			super();
			setBorder(new TitledBorder("Automatic recordings"));
			autoInterval = new JTextField(4);
			autoDuration = new JTextField(4);
			GridBagConstraints c = new GridBagConstraints();
			GridBagLayout gb = new GridBagLayout();
			setLayout(gb);
			c.gridx = c.gridy = 0;
			c.anchor = GridBagConstraints.WEST;
			addComponent(this, new JLabel("Interval between recordings "), c);
			c.gridx++;
			addComponent(this, autoInterval, c);
			c.gridx ++;
			addComponent(this, new JLabel(" s"), c);
			c.gridy ++;
			c.gridx = 0;
			addComponent(this, new JLabel("Recording length "), c);
			c.gridx++;
			addComponent(this, autoDuration, c);
			c.gridx ++;
			addComponent(this, new JLabel(" s"), c);
		}
		void setParams() {
			autoInterval.setText(String.format("%d", recorderSettings.autoInterval));
			autoDuration.setText(String.format("%d", recorderSettings.autoDuration));
		}
		boolean getParams() {
			try {
				recorderSettings.autoInterval = Integer.valueOf(autoInterval.getText());
				recorderSettings.autoDuration = Integer.valueOf(autoDuration.getText());
			}
			catch (NumberFormatException Ex) {
				return false;
			}
			return true;
		}
	}
}
