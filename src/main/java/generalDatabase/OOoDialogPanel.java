package generalDatabase;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamUtils.PamFileFilter;

public class OOoDialogPanel implements SystemDialogPanel {
	
	OOoDBSystem oodbSystem;
	
	JPanel p;
	
	JComboBox dbList;
	
	JButton browseButton;
	
	Component parent;
	
	public OOoDialogPanel(Component parent, OOoDBSystem oodbSystem) {
		super();
		this.parent = parent;
		this.oodbSystem = oodbSystem;
		p = new JPanel();
		p.setBorder(new TitledBorder("Open Office database file"));
		p.setLayout(new BorderLayout());
		p.add(BorderLayout.NORTH, dbList = new JComboBox());
		JPanel q = new JPanel();
		q.setLayout(new BorderLayout());
		q.add(BorderLayout.EAST, browseButton = new JButton("Browse / Create ..."));
		p.add(BorderLayout.CENTER, q);
		
		browseButton.addActionListener(new BrowseButtonAction());
	}
	
	public JPanel getPanel() {
		return p;
	}
	
	public boolean getParams() {
		// selected item may not be first in the list - so re-order the 
		// list to make sure that it is.
		int ind = dbList.getSelectedIndex();
		if (ind >= 0) {
			File selFile = (File) dbList.getSelectedItem();
			if (selFile.exists() == false) return false;
			oodbSystem.recentDatabases.remove(selFile);
			oodbSystem.recentDatabases.add(0, selFile);
			return true;
		}
		else {
			return false;
		}
	}
	
	public void setParams() {
		
		dbList.removeAllItems();
		for (int i = 0; i < oodbSystem.recentDatabases.size(); i++) {
			dbList.addItem(oodbSystem.recentDatabases.get(i));
		}
		
	}
	
//	class ListAction implements ActionListener {
//	
//	public void actionPerformed(ActionEvent e) {
//	
//	int ind = dbList.getSelectedIndex();
//	if (ind > 0) {
//	File selFile = (File) dbList.getSelectedItem();
//	msAccessSystem.recentDatabases.remove(selFile);
//	msAccessSystem.recentDatabases.add(0, selFile);
//	dbList.remove(ind);
//	setParams();
//	}
//	}
//	
//	}
	
//	class BrowseButtonAction implements ActionListener {
//		
//		public void actionPerformed(ActionEvent e) {
//			
//			String newDB = oodbSystem.browseDatabases(parent);
//			if (newDB != null) {
//				
//				// see if this file exists in the list and if it does, remove it
//				for (int i = 0; i < oodbSystem.recentDatabases.size(); i++) {
//					if (oodbSystem.recentDatabases.get(i).toString().equalsIgnoreCase(newDB)) {
//						oodbSystem.recentDatabases.remove(i);
//					}
//				}
//				// then insert the file at the top of the list.
//				File newFile = new File(newDB);
//				oodbSystem.recentDatabases.add(0, newFile);
//				setParams();
//				
//			}
//			
//		}
//		
//	}
	class BrowseButtonAction implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			
			String newDB = oodbSystem.browseDatabases(parent);
			if (newDB != null) {
				
				// see if this file exists in the list and if it does, remove it
				for (int i = 0; i < oodbSystem.recentDatabases.size(); i++) {
					if (oodbSystem.recentDatabases.get(i).toString().equalsIgnoreCase(newDB)) {
						oodbSystem.recentDatabases.remove(i);
					}
				}
				// then insert the file at the top of the list.
				File newFile = new File(newDB);
				// if the file doesn't exit, consider creating it.
				if (newFile.exists() == false) {
					newFile = createNewDatabase(newDB);
					if (newFile == null) {
						return;
					}
					
				}
				
				oodbSystem.recentDatabases.add(0, newFile);
				setParams();
				
			}
			
		}

	}

	public File createNewDatabase(String newDB) {
		String dummy = "BlankOOo._odb";
		
		File cpdb=new File(dummy);
		if (cpdb.exists() == false) {
			return null;
		}
		File newFile = new File(newDB);
//		String end = newFile.
		newFile = PamFileFilter.checkFileEnd(newFile, ".odb", true);

		int ans = JOptionPane.showConfirmDialog(parent, "Create blank database " + newFile.getAbsolutePath() + " ?", "OOo", JOptionPane.OK_CANCEL_OPTION);
		if (ans == JOptionPane.CANCEL_OPTION) {
			return null;
		}
		
		try {
			Files.copy(cpdb.toPath(), newFile.toPath());
		}
		catch (Exception e) {
			return null;
		}
		return newFile;
	}
	
}
