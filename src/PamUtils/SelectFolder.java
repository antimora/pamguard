/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package PamUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamGridBagContraints;

/**
 * Makes a dialog panel for selecting a folder
 * typically for file output. 
 * @author Doug Gillespie
 *
 */
public class SelectFolder {

	private FolderPanel folderPanel;
	private int textLength = 50;
	private String borderLabel;
	private JButton browseButton;
	private JTextField folderName;
	private JCheckBox includeSubFoldersCheckBox;
	private boolean showSubFolderOption = false;
	private ArrayList<FolderChangeListener> folderChangeListeners = new ArrayList<FolderChangeListener>();

	public SelectFolder(String borderLabel, int textLength, boolean showSubFolderOption) {
		this.showSubFolderOption = showSubFolderOption;
		createPanel(borderLabel, textLength);
	}

	public SelectFolder(String borderLabel, int textLength) {
		createPanel(borderLabel, textLength);
	}

	public SelectFolder(int textLength) {
		createPanel("Select Folder", textLength);
	}

	public void createPanel(String borderLabel, int textLength) {
		this.textLength = textLength;
		this.borderLabel = borderLabel;
		folderPanel = new FolderPanel();
	}
	
	public void setEnabled(boolean enable) {
		browseButton.setEnabled(enable);
		includeSubFoldersCheckBox.setEnabled(enable);
	}

	private class FolderPanel extends JPanel {

		JFileChooser fc;

		FolderPanel() {
			super();
			browseButton = new JButton("Browse");
			browseButton.addActionListener(new BrowseButton());
			folderName = new JTextField(textLength);
			folderName.setEditable(false);
			includeSubFoldersCheckBox = new JCheckBox("Include sub folders");
			includeSubFoldersCheckBox.addActionListener(new SubFolders());
			GridBagLayout gb;
			GridBagConstraints c = new PamGridBagContraints();
			setLayout(gb = new GridBagLayout());
			setBorder(new TitledBorder(borderLabel));
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			gb.setConstraints(folderName, c);
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.WEST;
			gb.setConstraints(includeSubFoldersCheckBox, c);
			c.gridx++;
			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.NONE;
			gb.setConstraints(browseButton, c);

			add(folderName);
			add(includeSubFoldersCheckBox);
			add(browseButton);

			setVisibleControls();
		}
		class BrowseButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				browseDirectories();				
			}
		}
		class SubFolders implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				notifyChangeListeners();
			}
		}
		private void browseDirectories() {
			if (fc == null) {
				fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}

			fc.setCurrentDirectory(new File(folderName.getText()));

			int ans = fc.showDialog(this, "Select storage folder");

			if (ans == JFileChooser.APPROVE_OPTION) {
				folderName.setText(fc.getSelectedFile().toString());

				notifyChangeListeners();
			}

		}
	}

	private void notifyChangeListeners() {
		String newName = getFolderName(false);
		for (int i = 0; i < folderChangeListeners.size(); i++) {
			folderChangeListeners.get(i).folderChanged(newName, isIncludeSubFolders());
		}
	}

	/**
	 * Set the state of the sub folder check box.
	 * @param includeSubfolders true to check the box.
	 */
	public void setIncludeSubFolders(boolean includeSubfolders) {
		includeSubFoldersCheckBox.setSelected(includeSubfolders);
	}

	/**
	 * Get the state of the sub folder check box.
	 * @return true if selected
	 */
	public boolean isIncludeSubFolders() {
		return includeSubFoldersCheckBox.isSelected();
	}

	/**
	 * Add a folder change listener to receive notification if the 
	 * browse button was used to select a new folder. 
	 * @param folderChangeListener change listener
	 */
	public void addFolderChangeListener(FolderChangeListener folderChangeListener) {
		folderChangeListeners.add(folderChangeListener);
	}

	/**
	 * Remove a folder change listener
	 * @param folderChangeListener
	 */
	public void removeFolderChangeListener(FolderChangeListener folderChangeListener) {
		folderChangeListeners.remove(folderChangeListener);
	}

	/**
	 * Get the folder name and optionally check and create the
	 * path for data storage. 
	 * @param checkPath set true to check / create the storage path.
	 * @return Path string, or null if path check fails. 
	 */
	public String getFolderName(boolean checkPath) {
		String folder = folderName.getText();
		File f = new File(folder);
		if (checkPath) {
			if (f.exists() == false) {
				if (JOptionPane.showConfirmDialog(folderPanel, "The directory " + folder + " does not exist " +
						"do you want to create it ?", "Select Directory", JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
					if (f.mkdirs() == false) {
						// print a warning message
						JOptionPane.showMessageDialog(folderPanel, "The folder " + folder + " could not be created", 
								"Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			if (f.exists() == false) return null;
		}
		return folder;
	}

	public void setFolderName(String folderName) {
		if (folderName != null) {
			this.folderName.setText(folderName);
		}
		else {
			this.folderName.setText("");
		}
	}

	public FolderPanel getFolderPanel() {
		return folderPanel;
	}

	public void setTextLength(int textLength) {
		this.textLength = textLength;
		folderName.setColumns(textLength);
	}

	public boolean isShowSubFolderOption() {
		return showSubFolderOption;
	}

	public void setShowSubFolderOption(boolean showSubFolderOption) {
		this.showSubFolderOption = showSubFolderOption;
		setVisibleControls();
	}

	private void setVisibleControls() {
		includeSubFoldersCheckBox.setVisible(showSubFolderOption);
	}

}
