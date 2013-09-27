package loggerForms;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import depthReadout.DepthSidePanel;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import loggerForms.controls.LatLongControl;

//import clickDetector.ClickParameters;
//import clickDetector.ClickControl.MenuClickClassification;
//import clickDetector.ClickControl.MenuClickTrainId;
//import clickDetector.ClickControl.MenuStorageOptions;
//import clickDetector.dialogs.ClickParamsDialog;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import generalDatabase.DBProcess;
import PamController.PamControlledUnit;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamView.PamSidePanel;
import PamView.PamTabPanel;
import PamguardMVC.PamDataBlock;
/**
 * 
 * @author Graham Weatherup
 * controls the logger forms module
 */
public class FormsControl extends PamControlledUnit {

	public static ArrayList<String> restrictedTitles = new ArrayList<String>();
	
	private ArrayList<FormDescription> formDescriptions = new ArrayList<FormDescription>();
	private ArrayList<UDFErrors> UDFErrors = new ArrayList<UDFErrors>();

	private FormsTabPanel formsTabPanel;

	private FormsAlertSidePanel formsAlertSidePanel;

	private FormsProcess formsProcess;

	public FormsControl(String unitName) {
		super("Logger Forms", unitName);
		addPamProcess(formsProcess = new FormsProcess(this, "Forms Output"));
		formsTabPanel = new FormsTabPanel(this);
		formsAlertSidePanel = new FormsAlertSidePanel(this);
	}

	/**
	 * 
	 */
	private boolean buildRestrictedTitles() {
		
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		if (dbControl == null) {
			return false;
		}
		String keywordString = DBControlUnit.findDatabaseControl().getDatabaseSystem().getKeywords();
		
		String[] keywords;
		if (keywordString != null) {
			keywords = keywordString.split(",");

			for (String k:keywords){
				restrictedTitles.add(k);
			}
		}
		
		try {
			Connection con = DBControlUnit.findConnection();
			if (con == null) {
				return false;
			}
			keywordString = con.getMetaData().getSQLKeywords();
			
			keywords = keywordString.split(",");
			
			for (String k:keywords){
				restrictedTitles.add(k);
			}
//			System.out.println(keywordString);
//			System.out.println("");
//			System.out.println(keywords);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {
		switch(changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			generateForms();
			break;
		}
	}


	private void updateFormDataMaps() {
		ArrayList<PamDataBlock> allFormDataBlocks = formsProcess.getOutputDataBlocks();
		DBControlUnit.findDatabaseControl().mapNewDataBlock(null, allFormDataBlocks);

	}

	int getNumFormDescriptions() {
		return formDescriptions.size();
	}

	FormDescription getFormDescription(int iForm) {
		return formDescriptions.get(iForm);
	}

	public Character getOutputTableNameCounterSuffix(FormDescription thisFormDescription){

		String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String thisTableName = thisFormDescription.getDBTABLENAME();
		int count=0;
		int position = 0;

		for (FormDescription formDescription : formDescriptions){
			if ((formDescription.getDBTABLENAME()==thisFormDescription.getDBTABLENAME())){
				position=count;
				break;
			}

			count++;

		}
		//		if (count==0){
		//			return "";
		//		}else{
		//			return Character.;
		//			Integer.toString(arg0, arg1)
		//			System.out.println("*********");
		//			System.out.println(position);

		//		}
		//			System.out.println(letters.charAt(position));
		//			System.out.println("*********");
		return letters.charAt(position);
	}


	/**
	 * Generates a list of tables beginning with UDF_ and reads
	 * their contents into a FormDescription
	 */
	private void readUDFTables() {
		/*
		 * first find the database and get the database connection
		 */
		formDescriptions.clear();
		if (UDFErrors != null) {
			UDFErrors.clear();
		}


		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		if (dbControl == null) {
			System.out.println("No database module - create one to use forms");
			return;
		} else {

			ArrayList<String> udfTableNameList = new ArrayList<String>();

			Connection dbCon = dbControl.getConnection();
			
			if (dbCon == null) {
				System.out.println("Database not opened: Logger forms cannot be read");
				return;
			}

			try {
				DatabaseMetaData dbmd = dbCon.getMetaData();
				String[] types = {"TABLE"};
				ResultSet resultSet = dbmd.getTables(null, null, "%", types);//not getting all tables from db in ODB



				//Loop through database tables
				while (resultSet.next()){
					String tableName = resultSet.getString(3);
//					System.out.println("LogFor: "+tableName);
					//If starts with 'UDF_' create form description from it.
					if(  tableName.startsWith("UDF_")){
						udfTableNameList.add(tableName);

					}
				}


			} catch (SQLException e) {
				e.printStackTrace();
			}


			if (UDFErrors.size()>0){
			}


			for (String tableName:udfTableNameList){

				FormDescription formDescription = new FormDescription(this, tableName);
				formDescriptions.add(formDescription);
				formsProcess.addOutputDataBlock(formDescription.getFormsDataBlock());
			}


		}
	}
	
	public void addFormDescription(String newFormName){
		FormDescription formDescription = new FormDescription(this, newFormName);
		formsProcess.addOutputDataBlock(formDescription.getFormsDataBlock());
	}

	/**
	 * Get the correct type of reference to the forms tab panel. 
	 * @return reference to the forms tab panel. 
	 */
	public FormsTabPanel getFormsTabPanel() {
		return formsTabPanel;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#getTabPanel()
	 */
	@Override
	public PamTabPanel getTabPanel() {
		return formsTabPanel;
	}

	@Override
	public PamSidePanel getSidePanel() {
		if (formsAlertSidePanel == null) {
			formsAlertSidePanel = new FormsAlertSidePanel(this);
		}
		return formsAlertSidePanel;
	}

	private void createProcesses() {

	}



	/**
	 * @return the formsProcess
	 */
	public FormsProcess getFormsProcess() {
		return formsProcess;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem detMenu = new JMenu(getUnitName());
		JMenuItem menuItem = new JMenuItem("Create New Form ...");
		detMenu.add(menuItem);
		menuItem.addActionListener(new NewLoggerForm(parentFrame));
		detMenu.add(menuItem = new JMenuItem("Regenerate all forms"));
		menuItem.addActionListener(new ReGenerateForms(parentFrame));
		return detMenu;
	}

	class NewLoggerForm implements ActionListener {
		
		private Frame parentFrame;
		
		public NewLoggerForm(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			newLoggerform(parentFrame);
			
		}
		
	}

	class ReGenerateForms implements ActionListener {
		
		private Frame parentFrame;
		
		public ReGenerateForms(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			regenerateForms(parentFrame);
			
		}
		
	}
	
	public void newLoggerform(Frame parentFrame) {
		String newName = JOptionPane.showInputDialog(parentFrame, "Enter the name for the new user form", "New Logger Form", JOptionPane.OK_CANCEL_OPTION);
		if (newName == null) {
			return;
		}
		// will make a form table definition with a standard structure and name UDF_ ...
		// check the current name starts with UDF and add if necessary. 
		if (newName.toUpperCase().startsWith("UDF_") == false) {
			newName = "UDF_" + newName;
		}
		String message = String.format("The table definition %s will now be created in the database.", newName);
		message += "\nNote that youwill have to exit PAMGUARD and enter form control data by hand into this table.";
		message += "\nFuture releases will (hopefully) contain a more friendly programmable interface";
		int ans = JOptionPane.showConfirmDialog(parentFrame, message, "Create Form", JOptionPane.OK_CANCEL_OPTION);
		if(ans == JOptionPane.CANCEL_OPTION) {
			return;
		}
		UDFTableDefinition tableDef = new UDFTableDefinition(newName);
		message = String.format("The table %s could not be created in the databse %s", newName, 
				DBControlUnit.findDatabaseControl().getDatabaseName());
		if (DBControlUnit.findDatabaseControl().getDbProcess().checkTable(tableDef) == false) {
			JOptionPane.showMessageDialog(parentFrame, "Error Creating form", message, JOptionPane.ERROR_MESSAGE);
		}
	}

	
	/**
	 * Generate all forms and associated processes, notifying databases, maps, etc
	 * so that any required actions can be taken. 
	 */
	private void generateForms() {

		buildRestrictedTitles();
		
		readUDFTables();

		createProcesses();
		formsTabPanel.createForms();
		//			initialise

		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		if (dbControl == null) {
			return;
		}
		
		DBControlUnit.findDatabaseControl().getDbProcess().updateProcessList();
		if (isViewer) {
			updateFormDataMaps();
		}
		formsAlertSidePanel.getFormsAlertPanel().updateFormsShowing();		
	}
	
	/**
	 * Delete and recreate all forms / form data, etc. 
	 * @param parentFrame
	 */
	public void regenerateForms(Frame parentFrame) {
		formsTabPanel.removeAllForms();
		
		for (FormDescription formDescription:formDescriptions) {
			formDescription.destroyForms();
		}
		formDescriptions.clear();
		formsProcess.removeAllDataBlocks();
		generateForms();
		FormsPlotOptionsDialog.deleteDialog();
	}

	@Override
	public JMenuItem createDisplayMenu(Frame parentFrame) {
		JMenuItem plotMenu = new JMenuItem(getUnitName() + " plot options ...");
		plotMenu.addActionListener(new DisplayMenu(parentFrame));
		return plotMenu;
	}

	private class DisplayMenu implements ActionListener {

		private Frame parentFrame;

		public DisplayMenu(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			displayOptions(parentFrame);
		}
	}

	public void displayOptions(Frame parentFrame) {
		boolean ans = FormsPlotOptionsDialog.showDialog(parentFrame, this);
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#canClose()
	 */
	@Override
	public boolean canClose() {
		// return false if any forms have open sub forms.
		int subFormCount = 0;
		for (FormDescription fd:formDescriptions) {
			subFormCount += fd.getSubformCount();
		}
		if (subFormCount == 0) {
			return true;
		}
		String message = "One or more forms have open sub tab forms. Do you still want to close PAMguard";
		int ans = JOptionPane.showConfirmDialog(getGuiFrame(), message, getUnitName(), JOptionPane.YES_NO_OPTION);
		return (ans == JOptionPane.YES_OPTION);
	}
}
