package binaryFileStorage;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;


import PamUtils.SelectFolder;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class BinaryStorageDialogPanel {

	private JPanel p;
	
	private SelectFolder storageLocation;

	private JCheckBox autoNewFiles, dateSubFolders, limitfileSize;

	private JTextField fileLength, fileSize;
	
	private String errorTitle = "Binary Storage Options";
	
	private Window owner;
	
	public BinaryStorageDialogPanel(Window owner) {
		
		this.owner = owner;

		p = new JPanel(new BorderLayout());
		p.setBorder(new TitledBorder("Binary storage options"));
		storageLocation = new SelectFolder("", 50, false);
		((JPanel) storageLocation.getFolderPanel()).setBorder(null);
		p.add(BorderLayout.NORTH, storageLocation.getFolderPanel());

		JPanel q = new JPanel(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		PamDialog.addComponent(q, dateSubFolders = new JCheckBox("Store data in sub folders by date"), c);
		c.gridy++;
		c.gridx = 0;
		PamDialog.addComponent(q, autoNewFiles = new JCheckBox("Automatically start new files every "), c);
		c.gridx++;
		PamDialog.addComponent(q, fileLength = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(q, new JLabel(" minutes"), c);
		c.gridy++;
		c.gridx = 0;
		PamDialog.addComponent(q, limitfileSize = new JCheckBox("Limit data file size to a maximum of "), c);
		c.gridx++;
		PamDialog.addComponent(q, fileSize = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(q, new JLabel(" Mega Bytes"), c);
		p.add(BorderLayout.CENTER, q);

		autoNewFiles.addActionListener(new ChangeAction());
		limitfileSize.addActionListener(new ChangeAction());

	}
	
	public boolean getParams(BinaryStoreSettings binaryStoreSettings) {
		if (binaryStoreSettings == null) {
			return false;
		}
		binaryStoreSettings.setStoreLocation(storageLocation.getFolderName(true));
		binaryStoreSettings.autoNewFiles = autoNewFiles.isSelected();
		binaryStoreSettings.datedSubFolders = dateSubFolders.isSelected();
		binaryStoreSettings.limitFileSize = limitfileSize.isSelected();
		if (binaryStoreSettings.autoNewFiles) {
			try {
				binaryStoreSettings.fileSeconds = Integer.valueOf(fileLength.getText()) * 60;
				if (binaryStoreSettings.fileSeconds <= 0) {
					return PamDialog.showWarning(owner, errorTitle, "File length must be > 0");
				}
			}
			catch (NumberFormatException e) {
				return PamDialog.showWarning(owner, errorTitle, "Invalid file length data");
			}
		}
		if (binaryStoreSettings.limitFileSize) {
			try {
				binaryStoreSettings.maxFileSize = Integer.valueOf(fileSize.getText());
				if (binaryStoreSettings.maxFileSize <= 0) {
					return PamDialog.showWarning(owner, errorTitle, "File size must be > 0");
				}
			}
			catch (NumberFormatException e) {
				return PamDialog.showWarning(owner, errorTitle, "Invalid file size data");
			}
		}
		return true;
	}
	
	public void setParams(BinaryStoreSettings binaryStoreSettings) {
		storageLocation.setFolderName(binaryStoreSettings.getStoreLocation());
		autoNewFiles.setSelected(binaryStoreSettings.autoNewFiles);
		dateSubFolders.setSelected(binaryStoreSettings.datedSubFolders);
		fileLength.setText(String.format("%d", binaryStoreSettings.fileSeconds/60));
		limitfileSize.setSelected(binaryStoreSettings.limitFileSize);
		fileSize.setText(String.format("%d", binaryStoreSettings.maxFileSize));
		
		enableControls();
	}

	/**
	 * @return the panel
	 */
	public JPanel getPanel() {
		return p;
	}

	private class ChangeAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			enableControls();
		}

	}

	public void enableControls() {
		fileLength.setEnabled(autoNewFiles.isSelected());
		fileSize.setEnabled(limitfileSize.isSelected());
	}

}
