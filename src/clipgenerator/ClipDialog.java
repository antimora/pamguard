package clipgenerator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import binaryFileStorage.BinaryStore;

import PamController.PamController;
import PamDetection.PamDetection;
import PamDetection.RawDataUnit;
import PamUtils.SelectFolder;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;

public class ClipDialog extends PamDialog {

	private static ClipDialog singleInstance;
	
	private ClipSettings clipSettings;

	private SourcePanel sourcePanel;
	private StoragePanel storagePanel;
	private ClipPanel clipPanel;
	
	public ClipDialog(Window parentFrame) {
		super(parentFrame, "Clip generation settings", false);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		sourcePanel = new SourcePanel(parentFrame, "Audio Data Source", RawDataUnit.class, false, true);
		storagePanel = new StoragePanel();
		clipPanel = new ClipPanel();
		mainPanel.add(sourcePanel.getPanel());
		mainPanel.add(storagePanel);
		mainPanel.add(clipPanel);
		setDialogComponent(mainPanel);
	}
	
	public static ClipSettings showDialog(Window window, ClipSettings clipSettings) {
		if (singleInstance == null || singleInstance.getOwner() != window) {
			singleInstance = new ClipDialog(window);
		}
		if (clipSettings == null) {
			clipSettings = new ClipSettings();
		}
		singleInstance.clipSettings = clipSettings.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.clipSettings;
	}

	@Override
	public void cancelButtonPressed() {
		clipSettings = null;
	}

	private void setParams() {
		sourcePanel.setSource(clipSettings.dataSourceName);
		storagePanel.setParams();
		clipPanel.setParams();
		enableControls();
	}

	@Override
	public boolean getParams() {
		clipSettings.dataSourceName = sourcePanel.getSource().getDataName();
		if (clipSettings.dataSourceName == null) {
			return showWarning("No data source");
		}
		if (storagePanel.getParams() == false) return showWarning("Error in storage location");
		if (clipPanel.getParams() == false) return showWarning("Error in clip generator settings");
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
	
	}
	
	
	class StoragePanel extends JPanel {

		private SelectFolder selectFolder;
		
		private JCheckBox dateSubFolders;
		
		private JRadioButton storeWavFiles, storeBinary;
		
		public StoragePanel() {
			this.setLayout(new BorderLayout());
			setBorder(new TitledBorder("Storage options"));
			JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
			topRow.add(storeWavFiles = new JRadioButton("Store in wav files"));
			topRow.add(storeBinary = new JRadioButton("Store in binary files"));
			ButtonGroup bg = new ButtonGroup();
			bg.add(storeBinary);
			bg.add(storeWavFiles);
			StoreChanged sc = new StoreChanged();
			storeBinary.addActionListener(sc);
			storeWavFiles.addActionListener(sc);
			this.add(BorderLayout.NORTH, topRow);
			selectFolder = new SelectFolder("", 50, false);
			((JPanel) selectFolder.getFolderPanel()).setBorder(null);
			dateSubFolders = new JCheckBox("Store data in sub folders by date");
			this.add(BorderLayout.CENTER, selectFolder.getFolderPanel());
			this.add(BorderLayout.SOUTH, dateSubFolders);
		}
		
		public void setParams() {
			selectFolder.setFolderName(clipSettings.outputFolder);
			dateSubFolders.setSelected(clipSettings.datedSubFolders);
			storeWavFiles.setSelected(clipSettings.storageOption == ClipSettings.STORE_WAVFILES);
			storeBinary.setSelected(clipSettings.storageOption == ClipSettings.STORE_BINARY);
		}
		
		public boolean getParams() {
			clipSettings.datedSubFolders = dateSubFolders.isSelected();
			clipSettings.outputFolder = selectFolder.getFolderName(storeWavFiles.isSelected());
			if (storeWavFiles.isSelected()) {
				clipSettings.storageOption = ClipSettings.STORE_WAVFILES;
			}
			else {
				clipSettings.storageOption = ClipSettings.STORE_BINARY;
			}
			return true;
		}

		public void enableControls() {
			selectFolder.setEnabled(storeWavFiles.isSelected());
			dateSubFolders.setEnabled(storeWavFiles.isSelected());
		}
	}
	
	class StoreChanged implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			enableControls();
		}
	}

	class ClipPanel extends JPanel {
	
		ArrayList<PamDataBlock> acousticDataBlocks;
		JLabel[] dataLabels;
		JCheckBox[] enableBoxes;
		JButton[] settingsButtons;
		ClipGenSetting[] clipgenSettings;
		public ClipPanel() {
			super();
			JPanel p = new JPanel();
			this.setLayout(new BorderLayout());
			
			p.setBorder(new TitledBorder("Data Triggers"));
			acousticDataBlocks = PamController.getInstance().getDataBlocks(PamDetection.class, true);
			dataLabels = new JLabel[acousticDataBlocks.size()];
			enableBoxes = new JCheckBox[acousticDataBlocks.size()];
			settingsButtons = new JButton[acousticDataBlocks.size()];
			clipgenSettings = new ClipGenSetting[acousticDataBlocks.size()];
			PamDataBlock aDataBlock;
			p.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();
			addComponent(p, new JLabel(" Data Name ", SwingConstants.CENTER), c);
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			addComponent(p, new JLabel(" Enabled "), c);
			for (int i = 0; i < acousticDataBlocks.size(); i++) {
				aDataBlock = acousticDataBlocks.get(i);
				if (aDataBlock.isCanClipGenerate() == false) {
					continue;
				}
				c.gridy++;
				c.gridx = 0;
				addComponent(p, dataLabels[i] = new JLabel(aDataBlock.getDataName() + " ", SwingConstants.RIGHT), c);
				c.gridx++;
				c.anchor = GridBagConstraints.CENTER;
				addComponent(p, enableBoxes[i] = new JCheckBox(), c);
				enableBoxes[i].addActionListener(new ClipEnableButton(i));
				c.gridx ++;
				addComponent(p, settingsButtons[i] = new JButton("Settings"), c);
				settingsButtons[i].addActionListener(new ClipSettingsButton(i));
			}
			this.add(BorderLayout.NORTH, p);
		}
		
		public void setParams() {
			boolean b;
			ClipGenSetting cgs;
			for (int i = 0; i < acousticDataBlocks.size(); i++) {
				PamDataBlock aDataBlock = acousticDataBlocks.get(i);
				if (aDataBlock.isCanClipGenerate() == false) {
					continue;
				}
				cgs = clipSettings.findClipGenSetting(acousticDataBlocks.get(i).getDataName());
				enableBoxes[i].setSelected(cgs != null && cgs.enable);
				clipgenSettings[i] = cgs;
				createToolTip(i);
			}
		}
		
		public boolean getParams() {
			clipSettings.clearClipGenSettings();
			for (int i = 0; i < acousticDataBlocks.size(); i++) {
				PamDataBlock aDataBlock = acousticDataBlocks.get(i);
				if (aDataBlock.isCanClipGenerate() == false) {
					continue;
				}
				if (clipgenSettings[i] == null) {
					continue;
				}
				clipgenSettings[i].enable = enableBoxes[i].isSelected();
				clipSettings.addClipGenSettings(clipgenSettings[i]);
			}
			return true;
		}
	
		void enableControls() {
			for (int i = 0; i < acousticDataBlocks.size(); i++) {
				PamDataBlock aDataBlock = acousticDataBlocks.get(i);
				if (aDataBlock.isCanClipGenerate() == false) {
					continue;
				}
				settingsButtons[i].setEnabled(enableBoxes[i].isSelected());
			}
		}
		
		void fireSettings(int iDataStream) {
			ClipGenSetting newSettings = ClipGenSettingDialog.showDialog(getOwner(), 
					clipgenSettings[iDataStream], acousticDataBlocks.get(iDataStream));
			if (newSettings == null && clipgenSettings[iDataStream] == null) {
				enableBoxes[iDataStream].setSelected(false);
			}
			else if (newSettings != null) {
				clipgenSettings[iDataStream] = newSettings.clone();
			}
			createToolTip(iDataStream);
			enableControls();
		}
		
		private void createToolTip(int iDataStream) {
			String tipText;
			if (enableBoxes[iDataStream].isSelected() && clipgenSettings[iDataStream] != null) {
				tipText = "Clip generation enabled";
			}
			else {
				tipText = null;
			}
			dataLabels[iDataStream].setToolTipText(tipText);
			enableBoxes[iDataStream].setToolTipText(tipText);
			settingsButtons[iDataStream].setToolTipText(tipText);
		}
	
		class ClipEnableButton implements ActionListener {
			int iDataStream;
			/**
			 * @param iDataStream
			 */
			public ClipEnableButton(int iDataStream) {
				super();
				this.iDataStream = iDataStream;
			}
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (enableBoxes[iDataStream].isSelected()) {
					if (clipgenSettings[iDataStream] == null) {
						fireSettings(iDataStream);
					}
				}
				else {
					if (clipgenSettings[iDataStream] != null) {
						clipgenSettings[iDataStream].enable = false;
					}
				}
				enableControls();
			}
			
		}
		class ClipSettingsButton implements ActionListener {
			int iDataStream;
			/**
			 * @param iDataStream
			 */
			public ClipSettingsButton(int iDataStream) {
				super();
				this.iDataStream = iDataStream;
			}
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fireSettings(iDataStream);
			}
			
		}
		
	}

	public void enableControls() {
		storagePanel.enableControls();
		clipPanel.enableControls();
	}

}
