package Acquisition.offlineFuncs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionDialog;
import PamController.OfflineRawDataStore;
import PamUtils.SelectFolder;
import PamView.PamDialog;

/**
 * An extra panel that appears in the DAQ control when offline
 * so that user can point the DAQ at a set of wav of aif files
 * to use with the offline viewer. 
 * @author Doug Gillespie
 *
 */
public class OfflineDAQDialogPanel {

	private OfflineRawDataStore offlineRawDataStore;
	
	private PamDialog parentDialog;

	private JPanel mainPanel;
	
	private JCheckBox enableOffline;
	
	private SelectFolder storageLocation;
	/**
	 * @param acquisitionControl
	 * @param acquisitionDialog
	 */
	public OfflineDAQDialogPanel(OfflineRawDataStore acquisitionControl,
			PamDialog acquisitionDialog) {
		super();
		this.offlineRawDataStore = acquisitionControl;
		this.parentDialog = acquisitionDialog;
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Offline file store"));
		storageLocation = new SelectFolder("", 30, true);
		((JPanel) storageLocation.getFolderPanel()).setBorder(null);
		mainPanel.add(BorderLayout.NORTH, enableOffline = new JCheckBox("Use offline files"));
		enableOffline.addActionListener(new EnableButton());
		JPanel centPanel = new JPanel(new BorderLayout());
		centPanel.add(BorderLayout.NORTH, storageLocation.getFolderPanel());
		mainPanel.add(BorderLayout.CENTER, centPanel);
		
	}
	
	public Component getComponent() {
		return mainPanel;
	}
	
	private class EnableButton implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			enableControls();
		}
	}
	
	private void enableControls() {
		storageLocation.setEnabled(enableOffline.isSelected());
	}

	public void setParams() {
		OfflineFileParameters p = offlineRawDataStore.getOfflineFileServer().getOfflineFileParameters();
		enableOffline.setSelected(p.enable);
		storageLocation.setFolderName(p.folderName);
		storageLocation.setIncludeSubFolders(p.includeSubFolders);
		enableControls();
	}
	
	private boolean checkFolder(String file) {
		if (file == null) {
			return false;
		}
		File f = new File(file);
		if (f.exists() == false) {
			return false;
		}
		return true;
	}
	
	public OfflineFileParameters getParams() {
		OfflineFileParameters p = new OfflineFileParameters();
		p.enable = enableOffline.isSelected();
		p.includeSubFolders = storageLocation.isIncludeSubFolders();
		p.folderName = storageLocation.getFolderName(false);
		if (checkFolder(p.folderName) == false && p.enable) {
			if (p.folderName == null) {
				parentDialog.showWarning("Error in file store", "No storage folder selected");
				return null;
			}
			else {
				String err = String.format("The folder %s does not exist", p.folderName);
				parentDialog.showWarning("Error in file store", err);
				return null;
			}
		}
		return p;
	}
}
