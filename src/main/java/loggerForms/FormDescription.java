package loggerForms;

import generalDatabase.DBControlUnit;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.pamCursor.PamCursor;
import generalDatabase.pamCursor.PamCursorManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import pamScrollSystem.AbstractPamScroller;
import pamScrollSystem.ScrollPaneAddon;

import com.sun.org.apache.xpath.internal.compiler.Keywords;

import sun.misc.Regexp;

import PamView.PamDetectionOverlayGraphics;
import PamView.PamPanel;
import PamView.PamTabPanel;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamCalendar;
import loggerForms.controlDescriptions.ControlDescription.ControlTypes;
import loggerForms.PropertyDescription.propertyTypes;
import loggerForms.controlDescriptions.ControlDescription;
import loggerForms.controls.LoggerControl;
import loggerForms.propertyInfos.BEARINGinfo;
import loggerForms.propertyInfos.HEADINGinfo;
import loggerForms.propertyInfos.RANGEinfo;
/**
 * 
 * @author Graham Weatherup - SMRU
 * Holds a description of the Form to be created
 */
public class FormDescription implements Cloneable {
	
	private UDFTableDefinition udfTableDefinition;
	private PamCursor udfCursor;
	private FormsControl formsControl;
	private DBControlUnit dbControl;
	private boolean udfTableOK;
	private ArrayList<PropertyDescription> propertyDescriptions = new ArrayList<PropertyDescription>();
	private ArrayList<ControlDescription> controlDescriptions = new ArrayList<ControlDescription>();
	private ArrayList<ControlDescription> inputControlDescriptions = new ArrayList<ControlDescription>();
	private PamTableDefinition outputTableDef;
	private FormPlotOptions formPlotOptions = new FormPlotOptions();
	private char counterSuffix;
	private String strippedName;
	private Long timeOfLastSave;
	private Long timeOfNextSave;
	private FormsDataBlock formsDataBlock;
	private UDFErrors formErrors;
	private UDFErrors formWarnings;
	private boolean outputTableOK;
	
//	private static enum formTypes{Normal,Hidden,Subtabs,Subform,Popup}
	
	
	
	/**
	 * Tab number for this forms items on the main logger tab
	 */
	private int tabNumber = -1;
	private JComponent tabComponent;
	private FormsDataDisplayTable formsDataDisplayTable;
	private LoggerSubTabbedPane subTabPane;
	private LoggerForm hiddenForm;
	private ArrayList<LoggerForm> subtabForms;
	private PamCursor outputCursor;
	
	// indexes of special controls used in plotting and stuff like that on the map
//	int bearingControlIndex = -1;
//	int rangeControlIndex = -1;
	/*
	 * Type of bearing - TURE, MAGNETIC,RELATIVE1, RELATIVE2
	 */
	private BearingTypes bearingType;
	private RangeUnitTypes rangeType;
	private Integer fixedRange;
	private Integer headingLength;
	private BearingTypes headingType;
	private RangeUnitTypes headingRangeUnit;
	private LoggerForm normalForm;
	
	private FormSettingsControl formSettingsControl;
	private JSplitPane splitPane;
	private BEARINGinfo bearingInfo;
	private RANGEinfo rangeInfo;
	private HEADINGinfo headingInfo;
	
	
	public FormDescription(FormsControl formsControl, String udfName){
		
		this.formsControl = formsControl;
		dbControl = DBControlUnit.findDatabaseControl();
		formSettingsControl = new FormSettingsControl(this, udfName);
		
		formErrors = new UDFErrors(this);
		formWarnings = new UDFErrors(this);
		
		udfTableDefinition = new UDFTableDefinition(udfName);
		
		udfTableOK = dbControl.getDbProcess().checkTable( udfTableDefinition);
		
		udfCursor = PamCursorManager.createCursor(udfTableDefinition);
		
		strippedName = udfName.substring(4);

		
		readUDFTable();
		
		createOutputTableDef();
		
//		tableComponant=new FormsDataDisplayTable(this);
		
		/*
		 * report UDF errors
		 */
		PamTabPanel tabPanel = formsControl.getTabPanel();
		Component c = null;
		if (tabPanel != null) {
			c = tabPanel.getPanel();
		}
		if (formErrors.popupAll(c)){
			
		}
		
		outputCursor=PamCursorManager.createCursor(outputTableDef);
		
		outputTableOK = dbControl.getDbProcess().checkTable(outputTableDef);
		
		
		formsDataBlock = new FormsDataBlock(this, getFormName(), formsControl.getFormsProcess(), 0);
		formsDataBlock.SetLogging(new FormsLogging(this,formsDataBlock));
		formsDataBlock.setOverlayDraw(new LoggerFormGraphics(formsControl, this));
		
		
		
		
		setTimeOfNextSave();
		
		findSpecialControls();
		
		PamSettingManager.getInstance().registerSettings(new FormPlotOptionsStore());
	}
	
	
	

	/**
	 * @return the formErrors
	 */
	public UDFErrors getFormErrors() {
		return formErrors;
	}




	/**
	 * @param formErrors the formErrors to set
	 */
	public void setFormErrors(UDFErrors formErrors) {
		this.formErrors = formErrors;
	}




	/**
	 * @return the formWarnings
	 */
	public UDFErrors getFormWarnings() {
		return formWarnings;
	}




	/**
	 * @param formWarnings the formWarnings to set
	 */
	public void setFormWarnings(UDFErrors formWarnings) {
		this.formWarnings = formWarnings;
	}




	/**
	 * find special controls for range, bearing and heading information on the map and
	 * also collate a few other things about each of them. 
	 */
	private void findSpecialControls() {
		bearingInfo = findBEARINGInfo();
		rangeInfo = findRANGEInfo();
		headingInfo = findHEADINGInfo();		
	}


	private void readUDFTable() {
		try {
			udfCursor.openScrollableCursor(dbControl.getConnection(), 
					true, true, "ORDER By "+dbControl.getDatabaseSystem().getSqlTypes().formatColumnName("Order"));
			
			//			udfCursor.openReadOnlyCursor(dbControl.getConnection(), "WHERE Id > 0");
			udfCursor.beforeFirst();

			while (udfCursor.next()) {

				udfCursor.moveDataToTableDef(true);

				String	type = udfTableDefinition.getType().getStringValue();
				
				/* decide what type, control or property.
				 * new item description(udfTableDefinition)/.
				 */
				if (PropertyDescription.isProperty(type)){
					propertyDescriptions.add( new PropertyDescription(this));
				}else if (ControlDescription.isControl(type)!=null){
					ControlDescription ctrlDesc;
					controlDescriptions.add(ctrlDesc = ControlDescription.makeCd(this,ControlDescription.isControl(type)));
//					controlDescriptions.add(ctrlDesc = Cd.makeCd(this, type));
					if (ctrlDesc.isInput()){
						String dbTitle = ctrlDesc.getDbTitle();
						
						if (dbTitle == null) {
							formErrors.add(String.format("Table item ID %d, ORDER %d does not have a valid \"Title\" or \"Dbtitle\"",
									ctrlDesc.getId(), ctrlDesc.getOrder()));
							ctrlDesc.addItemError();
							continue;
						}
						
						for (String rT:FormsControl.restrictedTitles){
							if (dbTitle.equalsIgnoreCase(rT)){
								formErrors.add(String.format("%s has a resticted DbTitle of \"%s\"",ctrlDesc.getTitle(),ctrlDesc.getDbTitle()));
							}
						}
						
						for (ControlDescription cd:inputControlDescriptions){
							if (dbTitle==cd.getDbTitle()){
								formErrors.add(String.format("%s at %s has the same DbTitle as %s at %s",ctrlDesc.getTitle(),ctrlDesc.getId(),cd.getTitle(),cd.getId()));
							}
							
						}
						
						
						inputControlDescriptions.add(ctrlDesc);
					}
				}else {
					formErrors.add(type+" in "+getUdfName()+" is not a recognised control or property type");
				}
				
//				udfCursor.
//				if (controlDescriptions.size() > 20) break;
			}


		} catch (SQLException e) {
			System.out.println("UDF_"+strippedName+" table not read properly");
			e.printStackTrace();
		}
		//formErrors.printAll();
	}
	
	@Override
	public String toString(){
		return getFormNiceName();
	}
	
	private void createOutputTableDef(){
		outputTableDef = new PamTableDefinition(getDBTABLENAME(), SQLLogging.UPDATE_POLICY_OVERWRITE);
		
		for (ControlDescription ctrlDesc:inputControlDescriptions){

			for (int i=0;i<ctrlDesc.getFormsTableItems().length;i++){
				//this index can be used in creating index lookups
				int index = outputTableDef.addTableItem(ctrlDesc.getFormsTableItems()[i]);
			}
			
		}
	}

	
	
	/**
	 * @return the controlDescriptions
	 */
	public ArrayList<ControlDescription> getControlDescriptions() {
		return controlDescriptions;
	}


	public PropertyDescription findProperty(propertyTypes propertyType) {
		for(PropertyDescription p:propertyDescriptions){
			if (p.getPropertyType() == propertyType) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Find an input control by name and return it's index
	 * @param name Input control name
	 * @return index in table, or -1 if nothing found. 
	 */
	private int findInputControlByName(String name) {
		if (name == null) {
			return -1;
		}
		for (int i = 0; i < inputControlDescriptions.size(); i++) {
			if (name.equals(inputControlDescriptions.get(i).getTitle())) {
				return i;
			}
		}
		return -1;
	}
	
	
	String getFormName() {
		return strippedName;
	}
	
	String getFormNiceName(){
		return EmptyTableDefinition.reblankString(strippedName);
	}
	
	String getFormTabName() {
		if (isSubTabs()) {
			int nSubTabs = 0;
			if (subTabPane != null) {
				nSubTabs = subTabPane.getTabCount();
			}
			return String.format("%s (%d)", getFormNiceName(), nSubTabs);
		}
		else {
			return getFormNiceName();
		}
	}
	
	/**
	 * Function to say whether or not the forms should be displayed as sub-tabs. 
	 * <p> This should replace all direct references to the SUBTABS property 
	 * since in viewer mode, sub tabs are NOT used. 
	 * @return
	 */
	boolean isSubTabs() {
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			return false;
		}
		return (findProperty(propertyTypes.SUBTABS) != null);
	}
	/**
	 * 
	 */
//	private void createOutputTableDef() {
//		outputTableDef = new EmptyTableDefinition(getDBTABLENAME());
//		ArrayList<String> outputTableFieldNames = new ArrayList<String>();
//		for(ControlDescription c:inputControlDescriptions){
//			String newField = c.getDbTitle();
//			if (outputTableFieldNames.contains(newField)){
//				System.out.println("Table Already has this field: "+newField);
//			}else{
//				outputTableFieldNames.add(newField);
//				outputTableDef.addTableItem(new FormsTableItem(c, newField, c.getDataType()));
//			}
//		}
//	}
	
	public Integer getAUTOALERT(){
		PropertyDescription p = findProperty(propertyTypes.AUTOALERT);
		if (p == null) {
//			formWarnings.add("The AUTOALERT Property was not found in "+getUdfName());
			return null;
		}
		Integer alert = p.getAutoUpdate();
		if (alert == null || alert == 0) {
			formErrors.add("The AUTOALERT AutoUpdate field is not filled in correctly, in "+getUdfName());
			return null;
		}
		
		return alert;
		
	}
	
	private int getRelatedDescription(PropertyDescription p){
		int cSelect = -1;
		Boolean titleExists=false;
		int count = 0;
		//Check the specified control exists and select the first one if it does.
		int iCtrol = 0;
		if (p == null || p.getTitle() == null) {
			return -1;
		}
		for (ControlDescription c:inputControlDescriptions){
			if (p.getTitle().equals(c.getTitle())){
				if (!titleExists){
					cSelect=iCtrol;
				}
				titleExists=true;
				count++;
			}
			iCtrol++;
		}
		if (titleExists){
			if (count==0){
				//This should never happen
				formErrors.add("Doesn't really exist");
				return -1;
			}else if (count==1){
				/*
				 * Normal situation no warning necessary
				 */
			}else if (count>1){
				//Two titles exist, Warn User and notify first will be used.
				formErrors.add(count+" \""+p.getTitle()+"\"rows with \""+p.getType()+"\" information exist in "+getUdfName()+". The first one will be used.");
				//FUTURE IMPROVEMENT:Ask User which to use and modify Title of other so is ignored in future
			}
			return cSelect;
		}else{
			formErrors.add("The row \""+p.getTitle()+"\" with \""+p.getType()+"\" information does not exist in "+getUdfName());
			return -1;
		}
	}
	
	
	public Font getFONT(){
		Font font = new Font("Arial", Font.PLAIN, 12);
		PropertyDescription p = findProperty(propertyTypes.FONT);
		if (!(p==null)){
			if (!(new Font(p.getTitle(), Font.PLAIN, p.getLength()).getName()=="Default")){
				font = new Font(p.getTitle(), Font.PLAIN, p.getLength());
			}
		}
		return font;
	}
	
	public String getDBTABLENAME() {
		PropertyDescription p = findProperty(propertyTypes.DBTABLENAME);
		if (p == null) {
			return strippedName;
		}
		String name = p.getTitle();
		if (name == null || name.length() == 0) {
			formErrors.add("The DBTABLENAME Title field is not filled in correctly in "+getUdfName());
			return strippedName;
		}
		return name;
	}
	
	/**
		 * 
		 * @return BEARINGinfo 
		 * 
		 * @containing (
		 * @ControlDescription based on Title field,
		 * @PropertyDescription.bearingTypes based on Topic field,
		 * @boolean (primitive) based on Plot field)
		 * 
		 */
		public BEARINGinfo findBEARINGInfo(){
			ControlDescription relatedControl;
			BearingTypes type;
			boolean plot;
			
	//		Object[] bearingObject = new Object[3];
			PropertyDescription p = findProperty(propertyTypes.BEARING);
			if (p == null) {
				//kill if not existant
				return null;
			}
			int ctrolIndex = getRelatedDescription(p);
			if (ctrolIndex < 0){
				return null;
			}else{
				relatedControl=inputControlDescriptions.get(ctrolIndex);
			}
			
			//Check topic for type of bearing to save set to TRUE if not correct.
			String topic = p.getTopic();
			if (BearingTypes.getValue(topic) == null){
				formErrors.add("The information \""+topic+"\" in topic field of \""+p.getType()+"\" is not correct in"+getUdfName()+". As default it will be saved as TRUE");
				type=BearingTypes.getValue("TRUE");
			}else {
				type=BearingTypes.getValue(topic);
			}
			
			Boolean plotB = p.getPlot();
			if (plotB==null){
				plot=false;
				formErrors.add("Plot Field is null, BEARING will not be plotted for "+getUdfName());
			}else {
				plot=(boolean) plotB;
			}
			
			return new BEARINGinfo( relatedControl, ctrolIndex, type, plot);
		}




	/**
	 * 
	 * @return RANGEinfo
	 * 
	 * @containing (
	 * @ControlDescription relatedControl,
	 * @rangeUnitTypes unitType,
	 * @rangeTypes type,
	 * @int fixedLength)
	 */
	public RANGEinfo findRANGEInfo(){
		/*
		 * check if RANGE exists
		 */	
		PropertyDescription p = findProperty(propertyTypes.RANGE);
		//No heading Property
		if (p == null) {
			return null;
		}
	
		
		/**
		 * get dataInput field(ControlDescription)
		 */	
		int ctrolIndex = getRelatedDescription(p);
		ControlDescription c;
		if (ctrolIndex < 0){
			return null;
		}else{
			c=inputControlDescriptions.get(ctrolIndex);
		}
		/**
		 * get/set-to-default rangeUnit field(enum)
		 */	
		String rangeUnitType = p.getPostTitle();
		RangeUnitTypes unitType;
		if (RangeUnitTypes.getValue(rangeUnitType) == null){
			formErrors.add("The information \""+rangeUnitType+"\" in PostTitle field of \""+p.getType()+"\" is not correct in"+getUdfName()+". As default it will be saved as M");
			unitType=RangeUnitTypes.getValue("M");
		}else {
			unitType=RangeUnitTypes.getValue(rangeUnitType);
		}
		/**
		 * get/set-to-default rangeType field(enum)
		 */	
		String rangeType = p.getTopic();
		RangeTypes type;
		if (RangeTypes.getValue(rangeType) == null){
			formErrors.add("The information \""+rangeType+"\" in PostTitle field of \""+p.getType()+"\" is not correct in"+getUdfName()+". As default it will not be FIXED");
			type=RangeTypes.VARIABLE;
		}else {
			type=RangeTypes.getValue(rangeType);
		}
		
		/**
		 * get/set-to-default rangeUnit field(enum)
		 */	
		int fixedLength = 1;
		if (type==(RangeTypes.getValue("FIXED"))){
			Integer range = p.getLength();
			if (range==null||range==0){
				formErrors.add("The FIXED range in the Length Field of RANGE in "+getUdfName()+" is null or 0. As default it will be 50m, 1km or 1nmi depending on units specified");
				if ((type)==(RangeTypes.getValue("m"))){
					fixedLength=50;
				}
				fixedLength=1;
			}else{
				fixedLength=range;
			}
		}
		RANGEinfo info=new RANGEinfo(c, ctrolIndex, unitType, type, fixedLength);
		return info;
	}




	/**
		 * 
		 * @return HEADINGinfo
		 * 
		 * 
		 * @containing (
		 * @ControlDescription relatedControl;
		 * @PropertyDescription.headingUnitTypes unitType;
		 * @int arrowLength;
		 * @int arrowHeadSize;
		 * @PropertyDescription.headingTypes type;
		 * @boolean fillHead;
		 * @Color colour;)
		 */
		public HEADINGinfo findHEADINGInfo(){
			RangeUnitTypes unitType;
			int arrowLength;
			int arrowHeadSize;
			HeadingTypes type;
			boolean fillHead;
			Color colour;
			
			
			/**
			 * check if HEADING exists
			 */	
			PropertyDescription p = findProperty(propertyTypes.HEADING);
			//No heading Property
			if (p == null) {
				return null;
			}
	
			Object[] headingObject = new Object[7];
			/**
			 * get dataInput field(ControlDescription)
			 */	
			ControlDescription relatedControl = null;
			int ctrolIndex = getRelatedDescription(p);
			if (ctrolIndex < 0){
				return null;
			}else{
				relatedControl=inputControlDescriptions.get(ctrolIndex);
			}
			
			/**
			 * get/set-to-default headingUnit field(enum)
			 */	
			String headingUnitType = p.getPostTitle();
			if (headingUnitType == null){
				formErrors.add("The information \""+headingUnitType+"\" in PostTitle field of \""+p.getType()+"\" is not correct in"+getUdfName()+". As default it will be saved as M");
				unitType=RangeUnitTypes.m;
			}else {
				unitType=RangeUnitTypes.getValue(headingUnitType);
			}
			/**
			 * get/set-to-default arrowLength field(int)
			 */	
			Integer arrowLengthI = p.getLength();
			if (arrowLengthI==null||arrowLengthI==0){
				formErrors.add("The Length of the Heading arrow in \""+p.getType()+"\" has not been entered or is 0. A default value of 10 will be used.");
				arrowLengthI=10;
			}
			arrowLength=(int)arrowLengthI;
			
			/**
			 * get/set-to-default arrowHeadSize field(int)
			 */	
			Integer arrowHeadSizeI = p.getLength();
			if (arrowHeadSizeI==null||arrowHeadSizeI==0){
				formErrors.add("The Length of the Heading arrow in \""+p.getType()+"\" has not been entered or is 0. A default value of 1 will be used.");
				arrowHeadSizeI=1;
			}
			arrowHeadSize=(int)arrowHeadSizeI;
			
			
			/**
			 * get/set-to-default headingType field(enum)
			 */	
			String headingType = p.getTopic();
			if (HeadingTypes.getValue(headingType) == null){
				formErrors.add("The information \""+headingType+"\" in Topic field of \""+p.getType()+"\" is not correct in"+getUdfName()+". As default it will be saved as TRUE");
				type=HeadingTypes.getValue("TRUE");
			}else {
				type=HeadingTypes.getValue(headingType);
			}
			
			/**
			 * get/set-to-default fillHead field(boolean)
			 */
			Boolean fillHeadB = p.getPlot();
			if (fillHeadB==null){
				fillHeadB=false;
			}
			fillHead=(boolean)fillHeadB;
			
			/**
			 * get/set-to-default Colour field(Color)
			 */
			colour=Color.getColor(p.getColour(), Color.black);
			
	//		return headingObject;
			
	//		ControlDescription relatedControl,PropertyDescription.headingUnitTypes unitType,int arrowLength,
	//		int arrowHeadSize,PropertyDescription.headingTypes type,boolean fillHead, Color colour
			
			return new HEADINGinfo(relatedControl,ctrolIndex, unitType,arrowLength,
					arrowHeadSize,type, fillHead,  colour);
		}




	public void getHIDDEN(){
//		PropertyDescription p = findProperty(propertyTypes.HIDDEN);
//		
//		
//		//No hidden Property
//		if (!(p == null)) {
//			return false;
//		}
//		
//		boolean hidden = false;
//		for (ControlDescription c:controlDescriptions){
//			if (c.
//		}
//		
//		
//		return hidden;
//		
	}
	
	public String getHOTKEY(){
		/**
		 * check if HOTKEY-property exists
		 */	
		PropertyDescription p = findProperty(propertyTypes.HOTKEY);
		//No heading Property
		if (p == null) {
			return null;
		}
		
		/**
		 * check if HOTKEY-value exists and is valid (F5-F24 for now)
		 */
		String keyName = p.getTitle();
		if (keyName == null || keyName.length() == 0) {
			formErrors.add("The HOTKEY "+keyName+" in Title field is not filled in correctly");
			return null;
		}
		
		return keyName;
	}
	
	public String getUdfName() {
		return udfTableDefinition.getTableName();
	}

	/**
	 * Get a component to go into the main tab panel for the Logger forms
	 * If it's a POPUP form, return null since these don't go into the tab panel. 
	 * If it's SUBTABS, then return an empty tab panel, but be prepared to add stuff to it
	 * IF it's a normal form, then return a normal form.
	 * @return
	 */
	public JComponent getTabComponent() {
		boolean isSubTabs = isSubTabs();
		boolean isSubForm = (findProperty(propertyTypes.SUBFORM) != null);
		boolean isPopup = (findProperty(propertyTypes.POPUP) != null);
		boolean isHidden = (findProperty(propertyTypes.HIDDEN) != null);
		if (isSubForm || isPopup) {
			return null;
		}
		if (isHidden) {
			if (hiddenForm == null) {
				hiddenForm = createForm();
			}
			return null;
		}
		if (tabComponent == null) {
			JComponent formComponent;
			if (isSubTabs) {
				/*
				 * Create a sub tab panel which will be able to hold multiple forms
				 */
				subTabPane = new LoggerSubTabbedPane(this);
				formComponent = subTabPane.getComponent();
			}
			else { 
				/*
				 * Create a normal form.
				 */
				normalForm = createForm();
				formComponent = normalForm.getComponent();
			}
			formsDataDisplayTable = new FormsDataDisplayTable(this);
			
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formComponent, formsDataDisplayTable.getMainPanel());
			if (formSettingsControl.getFormSettings().splitPanelPosition != null) {
				splitPane.setDividerLocation((int) formSettingsControl.getFormSettings().splitPanelPosition);
			}
			else {
				splitPane.setDividerLocation(400);
			}
						
			if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
				ScrollPaneAddon sco = new ScrollPaneAddon(formsDataDisplayTable.getScrollPane(), getFormNiceName(),
						AbstractPamScroller.HORIZONTAL, 1000, 2*24*3600*1000, true);
				sco.addDataBlock(getFormsDataBlock());
				if (normalForm != null) {
					// try to incorporate the scrollers into the bottom of the main form by the save button
					normalForm.getLastRow().add(sco.getButtonPanel());
					tabComponent = splitPane;
				}
				else {
					// otherwise put them near the top. 
					JPanel bPanel = new PamPanel();
					bPanel.setLayout(new BorderLayout());
					JPanel blPanel = new PamPanel();
					blPanel.setLayout(new BorderLayout());
					blPanel.add(BorderLayout.EAST, sco.getButtonPanel());
					bPanel.add(BorderLayout.NORTH, blPanel);
					bPanel.add(BorderLayout.CENTER, splitPane);
					tabComponent = bPanel;
				}
			}
			else {	
				tabComponent = splitPane;
			}
			
//			splitPane.setAutoscrolls(true);
			splitPane.setBorder(BorderFactory.createEmptyBorder());
		}
		return tabComponent;
	}


	private int getFormNLines(){
		
		int newLines=0;
		for (ControlDescription cD:controlDescriptions){
			if(cD.getEType()==ControlTypes.NEWLINE){
				newLines+=1;
			}
		}
		return newLines;
	}

	public LoggerForm createForm() {
		return new LoggerForm(this,LoggerForm.NewDataForm);
	}


	/**
	 * @return the udfTableDefinition
	 */
	public UDFTableDefinition getUdfTableDefinition() {
		return udfTableDefinition;
	}


	/**
	 * @return the formsDataBlock
	 */
	public FormsDataBlock getFormsDataBlock() {
		return formsDataBlock;
	}


	/**
	 * @return the inputControlDescriptions
	 */
	public ArrayList<ControlDescription> getInputControlDescriptions() {
		return inputControlDescriptions;
	}


	/**
	 * @return the outputTableDef
	 */
	public PamTableDefinition getOutputTableDef() {
		if (outputTableDef==null)createOutputTableDef();
		return outputTableDef;
	}


	/**
	 * Process an event created by a mouse action on the tab associated with this form. 
	 * <p>This will include all button presses and mouse enter / exit. Does not capture mousemove. 
	 * @param evt
	 */
	public void processTabMouseEvent(LoggerTabbedPane loggerTabbedPane, MouseEvent evt) {
		switch (evt.getID()) {
//		case MouseEvent.MOUSE_ENTERED:
////			setTabToolTip(loggerTabbedPane);
//			break;
		case MouseEvent.MOUSE_CLICKED:
			if (evt.getClickCount() == 2 && isSubTabs()) {
				createSubtabForm();
			}
		}
		
	}

	/**
	 * Create a new subtab form on the appropriate sub tab panel. 
	 */
	private void createSubtabForm() {
		LoggerForm newForm = createForm();
		String subtabName = getFormNiceName();
		
		if (newForm.hasCounter()){
			subtabName+=" ("+newForm.getCounter().getData().toString()+")";
		}
		
		subTabPane.addTab(subtabName, newForm.getComponent());
		
		subTabPane.setSelectedComponent(newForm.getComponent());
		formsControl.getFormsTabPanel().setTabName(this);
		if (subtabForms == null) {
			subtabForms = new ArrayList<LoggerForm>();
		}
		subtabForms.add(newForm);
	}
	
	/**
	 *
	 * removes the Subtabform that the loggerForm calling it is on
	 */
	void removeSubtabform(LoggerForm loggerForm){
		
		//removes the Subtabform that is currently selected(it will be the one this command is called from)
		
//		subTabs.removeTabAt(subTabs.getSelectedIndex());
		subTabPane.removeTabAt(subTabPane.indexOfComponent(loggerForm.getComponent()));
		subtabForms.remove(loggerForm);
		formsControl.getFormsTabPanel().setTabName(this);
	}
	
	/**
	 * 
	 * @param loggerForm
	 * @return 
	 * @return the subtab which the specified logger form is on
	 */
	public Component getSubtab(LoggerForm loggerForm){
		
//		Component tab = subTabPane.getTabComponentAt(subTabPane.indexOfComponent(loggerForm.getComponent()));
		
		return subTabPane.getTabComponentAt(subTabPane.indexOfComponent(loggerForm.getComponent()));
	}
	


	/**
	 * Set an appropriate tool tip for the tab panel. 
	 * @param loggerTabbedPane
	 */
	public String getTabToolTip() {
		String tipText = null;
		if (isSubTabs()) {
			tipText = String.format("Double click to create a new %s form", getFormNiceName());
		}
		else {
			tipText = String.format("Click to view %s form", getFormNiceName());			
		}
		return tipText;
	}
	
	/**
	 * 
	 * @return true if the form data can be plotted on the PAMGUARD map. 
	 * May adapt this later on so it can draw on other projections as well. 
	 */
	public boolean canDrawOnMap() {
		if (findProperty(propertyTypes.PLOT) != null) {
			return true;
		}
		for (ControlDescription aControl:controlDescriptions) {
			if (aControl.getPlot() != null && aControl.getPlot()) {
				return true;
			}
		}
		return false;
	}


	/**
	 * @param formPlotOptions the formPlotOptions to set
	 */
	public void setFormPlotOptions(FormPlotOptions formPlotOptions) {
		this.formPlotOptions = formPlotOptions;
	}


	/**
	 * @return the formPlotOptions
	 */
	public FormPlotOptions getFormPlotOptions() {
		return formPlotOptions;
	}


	/**
	 * @return the timeOfNextSave
	 */
	public long getTimeOfNextSave() {
		return timeOfNextSave;
	}


	/**
	 * 
	 */
	public void setTimeOfNextSave() {
		if (getAUTOALERT()==null){
			timeOfNextSave= null;
			return;
		}
		
		
		/*
		 * find the last save time of the form
		 */
		Long timeOfLastSave;
		if (formsDataBlock.getUnitsCount()>0){
			timeOfLastSave=formsDataBlock.getLastUnit().getTimeMilliseconds();
		}else{
			timeOfLastSave=getLastSaveTimeFromDB();
			
		}
		
		
		/*
		 * set time of next save based on time of last save if one exists if not sets to now
		 */
		if (timeOfLastSave==null){
			timeOfNextSave=PamCalendar.getTimeInMillis();
		}else{
			timeOfNextSave=(long)timeOfLastSave+getAUTOALERT()*60*1000;
		}
		
		
//		this.secondsUntilNextSave = secondsUntilNextSave;
	}
	
	
//	private Long getLastSaveTime() {
//		
//	}
	
	
	private Long getLastSaveTimeFromDB() {
		try {
			
			Timestamp timestampNewest = PamCalendar.getTimeStamp(0);
			outputCursor.openScrollableCursor(dbControl.getConnection(), 
					true, true, "ORDER By \"UTC\" DESC");
			outputCursor.beforeFirst();
			/*
			 * if a record exists
			 */
			if (outputCursor.next()){
			
//			while (outputCursor.next()) {

				outputCursor.moveDataToTableDef(true);
				
				
				Timestamp timestamp=(Timestamp) outputTableDef.getTimeStampItem().getValue();
				if (timestamp.after(timestampNewest)){
					timestampNewest=timestamp;
				}
				
//				timestamp.toString();
				
//			}
				return PamCalendar.millisFromTimeStamp(timestampNewest);
			}else{
//				System.out.println("No last saved data for "+getFormName()+" was found in "+outputTableDef.getTableName());
				return null;
			}
			
			

		} catch (SQLException e) {
			System.out.println("No table "+outputTableDef.getTableName()+" was found in the database");
			e.printStackTrace();
			return null;
		}
		
	}


	/**
	 * @return the formsControl
	 */
	public FormsControl getFormsControl() {
		return formsControl;
	}
	
	
	
//	/**
//	 * @return the counter and increment it
//	 */
//	public int getCounter() {
//		
//		
//		
//		
//		return counter++;
//	}


	/**
	 * @return the counterSuffix
	 */
	public char getCounterSuffix() {
		return counterSuffix;
	}

	/**
	 * @return the bearingControlIndex or -1 if none found
	 */
//	public int getBearingControlIndex() {
//		return bearingControlIndex;
//	}
//
//	/**
//	 * @return the rangeControlIndex or -1 if none found
//	 */
//	public int getRangeControlIndex() {
//		return rangeControlIndex;
//	}
//
//	/**
//	 * @return the headingControlIndex or -1 if none found
//	 */
//	public int getHeadingControlIndex() {
//		return headingControlIndex;
//	}

	/**
	 * @return the bearingType
	 * <p>This can be one of RELATIVE1, RELATIVE2, TRUE, MAGNETIC;
	 */
	public BearingTypes getBearingType() {
		return bearingType;
	}

	/**
	 * @return the rangeType
	 * <p>This can be one of nmi, km, m, FIXED;
	 */
	public RangeUnitTypes getRangeType() {
		return rangeType;
	}

	/**
	 * @return the range value to be used when getRangeType returns FIXED
	 */
	public Integer getFixedRange() {
		return fixedRange;
	}

	/**
	 * @return the length of the heading arrow in units given in getHEadingRangeUnit()
	 * 
	 */
	public Integer getHeadingLength() {
		return headingLength;
	}

	/**
	 * @return the headingType
	 * This can be one of RELATIVE1, RELATIVE2, TRUE, MAGNETIC;
	 */
	public BearingTypes getHeadingType() {
		return headingType;
	}

	/**
	 * @return the headingRangeUnit
	 * <p>This can be one of nmi, km, m, pix;
	 */
	public RangeUnitTypes getHeadingRangeUnit() {
		return headingRangeUnit;
	}

	class FormPlotOptionsStore implements PamSettings {

		@Override
		public Serializable getSettingsReference() {
			return formPlotOptions;
		}

		@Override
		public long getSettingsVersion() {
			return FormPlotOptions.serialVersionUID;
		}

		@Override
		public String getUnitName() {
			return getFormName();
		}

		@Override
		public String getUnitType() {
			return "UDF Plot Options";
		}

		@Override
		public boolean restoreSettings(
				PamControlledUnitSettings pamControlledUnitSettings) {
			formPlotOptions = ((FormPlotOptions) pamControlledUnitSettings.getSettings()).clone();
			return formPlotOptions != null;
		}
		
	}

	/**
	 * Destroy any open forms. This includes disconnecting any observer
	 * of NMEA data. 
	 */
	public void destroyForms() {
		if (normalForm != null) {
			normalForm.destroyForm();
		}
		if (hiddenForm != null) {
			normalForm.destroyForm();
		}
		if (subtabForms != null) {
			for (LoggerForm aForm:subtabForms) {
				aForm.destroyForm();
			}
		}
	}


	/**
	 * Called when data are added to or removed from the 
	 * datablock. 
	 * <p> goes on to notify the history table that things have changed. 
	 */
	public void dataBlockChanged() {
		if (formsDataDisplayTable != null) {
			formsDataDisplayTable.dataChanged();
		}
	}


	/**
	 * Called from FormSettingsControl just before PAMGuard exits (or settings
	 * are saved for some other reason).
	 * <p> Populate appropriate data into the formSettings as provided. 
	 * @param formSettings
	 */
	public void getFormSettingsData(FormSettings formSettings) {
		if (splitPane != null) {
			formSettings.splitPanelPosition = splitPane.getDividerLocation();
		}
	}


	/**
	 * Called only in viewer mode when the selection of a row in the summary table 
	 * changes. The contents of the data unit will be displayed in the form (which 
	 * cannot be edited !), or the form will be cleared if the data unit is null
	 * @param formsDataUnit Data unit to display. 
	 */
	public void viewDataUnit(FormsDataUnit formsDataUnit) {
		if (normalForm != null) {
			normalForm.restoreData(formsDataUnit);
		}
	}
	
	/**
	 * Get a count of open sub tab forms. 
	 * @return the number of open sub tab forms. 
	 */
	public int getSubformCount() {
		if (subtabForms == null) {
			return 0;
		}
		else {
			return subtabForms.size();
		}
	}




	public BEARINGinfo getBearingInfo() {
		return bearingInfo;
	}




	public RANGEinfo getRangeInfo() {
		return rangeInfo;
	}




	public HEADINGinfo getHeadingInfo() {
		return headingInfo;
	}
}
