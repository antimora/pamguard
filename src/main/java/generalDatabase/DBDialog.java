package generalDatabase;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;

public class DBDialog extends PamDialog {

	DBParameters dbParameters;
	
	DBControl dbControl;
	
	static DBDialog singleInstance;
	
	JComboBox systemList;
	
	SystemDialogPanel systemDialogPanel;
	
//	StandardDBPanel standardDBPanel;
	
	JPanel dialogBottomPanel;
	
	private DBDialog(DBControl dbControl, Frame parentFrame) {
		
		super(parentFrame, "Database Selection", false);
		
		this.dbControl = dbControl;
		
//		standardDBPanel = new StandardDBPanel();
//		
//		systemDialogPanel = standardDBPanel;
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		
		JPanel top = new JPanel();
		top.setBorder(new TitledBorder("Database system"));
		top.setLayout(new BorderLayout());
		top.add(BorderLayout.CENTER, systemList = new JComboBox());
		systemList.setPreferredSize(new Dimension(300, 20));
		systemList.addActionListener(new SelectDBSystem());
		
		p.add(BorderLayout.NORTH, top);
		
		dialogBottomPanel = new JPanel();
		dialogBottomPanel.setLayout(new BorderLayout());
//		dialogBottomPanel.add(BorderLayout.CENTER, standardDBPanel.getPanel());
		
		p.add(BorderLayout.CENTER, dialogBottomPanel);
		setDialogComponent(p);
		
		setHelpPoint("utilities.generalDatabaseHelp.docs.database_database");
	}
	
	
	
	public static DBParameters showDialog(DBControl dbControl, Frame parentFrame, DBParameters dbParameters) {
		if (singleInstance == null || singleInstance.getParent() != parentFrame) {
			singleInstance = new DBDialog(dbControl, parentFrame);
		}
		singleInstance.dbParameters = dbParameters;
		singleInstance.setParams();
		singleInstance.setVisible(true);		
		return singleInstance.dbParameters;
	}

	@Override
	public void cancelButtonPressed() {
		dbParameters = null;
	}
	
	private void setParams() {
		systemList.removeAllItems();
		ArrayList<DBSystem> dbSystems = dbControl.databaseSystems;
		for (int i = 0; i < dbSystems.size(); i++) {
			systemList.addItem(dbSystems.get(i).getSystemName());
		}
		if (dbParameters.databaseSystem < dbSystems.size()) {
			systemList.setSelectedIndex(dbParameters.databaseSystem);
		}
//		systemDialogPanel.setParams();
	}

	@Override
	public boolean getParams() {

		if (systemDialogPanel != null && systemDialogPanel.getParams() == false) return false;
		
		dbParameters.databaseSystem = systemList.getSelectedIndex();
		
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}
	
	private void selectSystem() {

		int currenIndex = systemList.getSelectedIndex();
		dialogBottomPanel.removeAll();
		DBSystem currentSystem = dbControl.getSystem(currenIndex);
		if (currentSystem != null) {
			systemDialogPanel = currentSystem.getDialogPanel(this);
			if (systemDialogPanel != null) {
				dialogBottomPanel.add(BorderLayout.CENTER, systemDialogPanel.getPanel());
				systemDialogPanel.setParams();
			}
		}
		else {
			systemDialogPanel = new NullDialogPanel();
			dialogBottomPanel.add(BorderLayout.CENTER, systemDialogPanel.getPanel());
		}
		pack();
		
	}
	
	class SelectDBSystem implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			selectSystem();
			
		}
		
	}
/*
	class StandardDBPanel implements SystemDialogPanel {

		
		JTextField databaseName;
		
		JTextField userName;
		
		JPasswordField password;
		
		JButton browseButton;
		
		JPanel bot;
		
		
		public JPanel getPanel() {
			bot = new JPanel();
			bot.setBorder(new TitledBorder("Database selection"));
			GridBagConstraints c = new GridBagConstraints();
			bot.setLayout(new GridBagLayout());
			
			c.gridx = c.gridy = 0;
			addComponent(bot, new JLabel("Database"), c);
			c.gridy++;
			c.gridwidth = 3;
			addComponent(bot, databaseName = new JTextField(30), c);
			databaseName.setEditable(false);
			
			c.gridy++;
			c.gridwidth = 1;
			c.gridx = 2;
			c.anchor = GridBagConstraints.EAST;
			addComponent(bot, browseButton = new JButton("Browse"), c);
			browseButton.addActionListener(new BrowseDatabases());

			return bot;
		}

		public boolean getParams() {
//			dbParameters.databaseName = databaseName.getText();
			return true;
		}

		public void setParams() {
			
//			databaseName.setText(dbParameters.databaseName);
			
		}
		
		class BrowseDatabases implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				
				String newDatabase = dbControl.browseDatabases(bot);
				if (newDatabase != null) {
					databaseName.setText(newDatabase);
				}
				
			}
			
		}
		
	}
	*/
}
