package staticLocaliser;

import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import staticLocaliser.panels.StaticLocalisationMainPanel;
import staticLocaliser.panels.UnsyncdataBlocks;
import Array.ArrayManager;
import Array.PamArray;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamUtils;
import PamView.PamGui;
import PamguardMVC.PamDataBlock;

/**
 * <b>Static Localiser.</b>
 * <p>
 * The static localiser ('localiser' with an 's') is designed to allow the localisation of a single vocalisation incident on a synchronised or unsynchronised hydrophone array. As of 17/05/2012 only a click and offline click datablock can be used in 
 * localisation, however the module has been written to allow others to enable different datablocks easily. In addition different localisation algorithms can be integrated by writing a single class. 
 * <p>
 * <b>To enable a new datablock for localisation.</b> 
 * <p>
 * 1) Create a new sub class of 'AbstractDetectionMatch' and fill out the relevant functions. This class takes a PamDataUnit and it's parent datablock then works out all the possible time delays between the hydrophone elements within the specified array. A flag can be passed to this class in order to determine how to calculate time delays (usually by specifying which dataunits on other hydrophones to use). For example in 'ClickDetectionMatch' flags to include only clicks which are part of the same event and/or species 
 * are passed when working out time delays. However a flag could be passed which initiates a more complex type of time delay calculation, perhaps using click train matching techniques. 
 * <p>
 * 2)Create a new control panel, a subclass of 'AbstractLocaliserControl' and again make sure to fill out all the required functions properly. This panel should allow the user to navigate through the individual pamdataunits within the pamdatablock. It should also allow the user to select which flags to send to the time delay calculation. For example the click control panel allows the user to select different species classifications. If a species classification is selected in the control panel, the panel will return a flag which, in 'ClickDetectionMatch' tells the time delay algorithm only to select dataunits which are of the same classification
 * <p>
 * 3)Implement 'StaticLocaliserProvider' for the datablock in question. This will create a function which must return an AbstractLocaliserControl class, i.e. the controlPanel you just made. 
 * <p>
 * 4)In  your subclass of PamDataUnit, eg ClickDetection, change the functions  'getDetectionMatch()'  and 'getDetectionMatch(int type)' to return  the subclass of AbstractDetectionMatch you've created. Note if there are no flags for your AbstractDetectionMatch sub class simply return the same for both these functions. 
 * <p>
 * 5)Sit back and enjoy the program crashing. Time to debug. 
 * <p>
 * <p>
 * <p>
 * <b>To create a new localisation algorithm</b>
 * <p>
 * 1) Create a subclass of 'AbstractStaticLocaliserAlgorithm' and fill out the relevant functions. Note that the runModel() algorithm returns an ArrayList of StaticLocalisationResults. This is because an algorithm may be intelligent enough to calculate two possible locations. If not just have an ArrayList of one result.
 * <p>
 * 2)In the StaticLoclalise class add an instance of your new AbstractStaticLocaliserAlgorithm subclass to the 'algorithms' ArrayList in the constructor. ]
 * <p>
 * Hopefully users will add new compatible detections and localisation algorithms over time.
 * 
 
 * @author Jamie Macaulay
 *
 */
public class StaticLocaliserControl extends PamControlledUnit implements PamSettings {
	
	private StaticLocalise staticLocaliser;
	
	private StaticLocaliserOfflineFunctions offlineFunctions;
	
	private StaticLocalisationMainPanel staticMainPanel;
	
	private ArrayList<ArrayList<PamDataBlock>> dataBlocks;
	
	private ArrayList<ArrayList<PamDataBlock>> unSyncDataBlocks;
	
	private PamDataBlock currentDataBlock;
	
	private ArrayManager arrayManager=ArrayManager.getArrayManager();
	
	private PamArray currentArray=arrayManager.getCurrentArray();
	
	private StaticLocaliserParams staticLocaliserParams=new StaticLocaliserParams();
	
	StaticLocaliserDataBlock staticLocaliserDataBlock;
	
	StaticLocaliserProcess staticLocaliserProcess;
	
	//flags for the update function in static dialog components. 
	public static final int RUN_ALGORITHMS=0x1;
	
	public static final int TD_SEL_CHANGED=0x2;
	
	public static final int SEL_DETECTION_CHANGED=0x4;
	
	public static final int SEL_DETECTION_CHANGED_BATCH=0x6;
	
	//load status
	boolean initComplete=false;

	private StaticLocaliserSQLLogging staticLocaliserSQLLogging;
	
	public boolean isViewer = false;
	
	
	public StaticLocaliserControl(String unitName) {
		super("Static Localiser", unitName);
		
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			this.isViewer=true;
			offlineFunctions=new StaticLocaliserOfflineFunctions(this);
		}
	
		
		addPamProcess(staticLocaliserProcess=new StaticLocaliserProcess(this));
		staticLocaliserDataBlock= new StaticLocaliserDataBlock(StaticLocalisationResults.class, "StaticLocaliserData", staticLocaliserProcess, 0);
		staticLocaliserDataBlock.SetLogging(staticLocaliserSQLLogging = new StaticLocaliserSQLLogging(staticLocaliserDataBlock));
		staticLocaliserProcess.addOutputDataBlock(staticLocaliserDataBlock);
		
		unSyncDataBlocks=new ArrayList<ArrayList<PamDataBlock>>(); 

		findDataBlocks();
		if (dataBlocks.size()==0 || dataBlocks==null) {
			//warning message
			return;
		}
		

		//set current datablock
		currentDataBlock=dataBlocks.get(0).get(0);
		staticLocaliserParams.channels=PamUtils.getChannelArray(currentDataBlock.getChannelMap());

		staticLocaliser=new StaticLocalise(this);
		staticMainPanel=new StaticLocalisationMainPanel(this,currentDataBlock);
		
		setTabPanel(staticMainPanel);
		
		PamSettingManager.getInstance().registerSettings(this);

	
	}
	
	/**
	 * Creates a static localiser specific JMenu. Only advanced options should be included in this menu and not on the main dialog panel
	 * @return
	 */
	public JMenu createDetectionMenu() {
		JMenuItem menuItem;
		JMenu menu = new JMenu(getUnitName());
		menu.getPopupMenu().setLightWeightPopupEnabled(false);
		menuItem=new JMenuItem("Create Unsynchronised DataBlock");
		menuItem.addActionListener(new CreateUnsyncDataBlock());
		menu.add(menuItem);
		return menu;
	}
	
	/**
	 * Saves a static localisation result directly to the database. A unique table exists for static localisation results. 
	 * @param staticLocalisationResults
	 */
	public void save(StaticLocalisationResults staticLocalisationResults){
		staticLocaliserDataBlock.addPamData(staticLocalisationResults);				
	}
	
	/**
	 * Saves only the the best result. The best result is by default the result with the lowest chi value. However, the bestResult param in the StaticLocalise class can be changed by selecting another result in the results table. 
	 * 
	 */
	public void saveBest(){
		
		if (staticLocaliser.getResults()==null) return;
		
		int a = staticLocaliser.getBestResultIndex();
		
		StaticLocalisationResults aResult = staticLocaliser.getResults().get(a);
		
		staticLocaliser.getStaticLocaliserControl().save(aResult);
		
	}
	
	/**
	 * Saves either all the possible results from all the algorithms, or if showOnlyLowestChiValueDelay is selected in the main options panel saves only the result with the lowest chi value but for all algorithms. e.g. if MCMC had the lowest chi value but both MCMC and Simplex algorithms were used then the MCMC and Simplex result would be saved.  
	 **/
	public void saveAll(){ 
		System.out.println("Save all data");
		if (staticLocaliser.getResults()==null) return;
			
		int a = staticLocaliser.getBestResultIndex();
		
		ArrayList<StaticLocalisationResults> resultsAll = staticLocaliser.getResults();
		
		if (getParams().showOnlyLowestChiValueDelay==true){
			for (int i=0; i<resultsAll.size(); i++){
				if (resultsAll.get(i).getTimeDelay()==staticLocaliser.getResults().get(a).getTimeDelay()){
					save(resultsAll.get(i));
				}
			}
		}
		else{
			for (int i=0; i<resultsAll.size(); i++){
				save(resultsAll.get(i));
			}
		}
	}
	

	class CreateUnsyncDataBlock implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Point pt=new Point((int) (getFrame().getSize().getWidth()/2),(int) (getFrame().getSize().getHeight()/2));
			UnsyncdataBlocks.showDialog(getFrame(), pt, staticMainPanel);
		}
		
	}
	
	/**
	 * Creates the static localiser specific menu in the menu tab.
	 * 
	 */
	JMenuBar staticLocaliserTabMenu = null;
	@Override
	public JMenuBar getTabSpecificMenuBar(Frame parentFrame, JMenuBar standardMenu, PamGui pamGui) {
		JMenu aMenu;
		staticLocaliserTabMenu = standardMenu;
			for (int i = 0; i < staticLocaliserTabMenu.getMenuCount(); i++) {
				if (staticLocaliserTabMenu.getMenu(i).getText().equals("Display")) {
					
					aMenu = createDetectionMenu();
					aMenu.setText("Static Localiser");
					staticLocaliserTabMenu.add(aMenu, i+1);
	
					break;
				}
			}
		return staticLocaliserTabMenu;
	}
	
	public Frame getFrame(){
		return this.getPamView().getGuiFrame();
	}
	
	
	public void setCurrentDatablock(PamDataBlock dataBlock){
		this.currentDataBlock=dataBlock;
	}
	
	
	public PamDataBlock getCurrentDatablock(){
		return currentDataBlock;
	}
	
	
	/**returns both the single and unsync dataBlocks
	 * 
	 * @return
	 */
	public ArrayList<ArrayList<PamDataBlock>> getDataBlocksAll(){
		ArrayList<ArrayList<PamDataBlock>> dataBlocksAll=new ArrayList<ArrayList<PamDataBlock>>();
		dataBlocksAll.addAll(dataBlocks);
		dataBlocksAll.addAll(unSyncDataBlocks);
		return dataBlocksAll;
	}
	
	/**
	 * Returns just the single datablocks
	 * @return
	 */
	public ArrayList<ArrayList<PamDataBlock>> getDataBlocks(){
		return dataBlocks;
	}
	
	/**
	 * Returns just the unsynchronised dataBlocks;
	 * @return
	 */
	public ArrayList<ArrayList<PamDataBlock>> getDataBlocksUnsync(){
		return unSyncDataBlocks;
	}
	
	/**
	 * Carries out tasks required if the dataBlock is changed. 
	 */
	public void dataBlockChanged(){
		staticMainPanel.changeControlPanel();
		staticMainPanel.getMainPanel().repaint();
		System.out.println("Current Data Block: "+currentDataBlock.toString());
	}
	
	public StaticLocalisationMainPanel getStaticMainPanel(){
		return staticMainPanel;
	}

	
	/**
	 * 	Get only datablocks which satisfy the StaticLocaliserProvider
	 */
	public void findDataBlocks(){
		dataBlocks=new ArrayList<ArrayList<PamDataBlock>>();
		ArrayList<PamDataBlock> rawDataBlock = PamController.getInstance().getDataBlocks();
		for (int i=0; i<rawDataBlock.size();i++){
			if (rawDataBlock.get(i) instanceof StaticLocaliserProvider){
				ArrayList<PamDataBlock> tempDataBlock=new ArrayList<PamDataBlock>();
				tempDataBlock.add(rawDataBlock.get(i));
				dataBlocks.add(tempDataBlock);
				System.out.println("Static Localiser: Satisfied Data Blocks: "+rawDataBlock.get(i).toString());
			}
		}
	}
	

	public void run(){	
		staticMainPanel.getDialogMap3D().update(StaticLocaliserControl.RUN_ALGORITHMS);
		staticMainPanel.getLocalisationInformation().update(StaticLocaliserControl.RUN_ALGORITHMS);
		staticLocaliser.localise(StaticLocalise.RUN);
	}
	
	public void runAll(){
		staticMainPanel.getDialogMap3D().update(StaticLocaliserControl.RUN_ALGORITHMS);
		staticMainPanel.getLocalisationInformation().update(StaticLocaliserControl.RUN_ALGORITHMS);
		staticLocaliser.localise(StaticLocalise.RUN_ALL);
	}
	
	public void runBatch(){
		staticMainPanel.getDialogMap3D().update(StaticLocaliserControl.RUN_ALGORITHMS);
		staticMainPanel.getLocalisationInformation().update(StaticLocaliserControl.RUN_ALGORITHMS);
		staticLocaliser.localise(StaticLocalise.RUN_BATCH);
	}
	


	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch(changeType) {
		case PamController.INITIALIZATION_COMPLETE:

			currentDataBlock=dataBlocks.get(0).get(0);
			staticMainPanel.create3DMap();
			staticMainPanel.createEastPanel();
			staticMainPanel.createNorthPanel();
			
			findDataBlocks();
			staticMainPanel.dataBlockListChanged();
			
			initComplete=true;
			
			break;
			
		case PamController.OFFLINE_DATA_LOADED:
			
			//check to ensure all 3D components have been initialised.
			if (staticMainPanel.getDialogMap3D()==null || staticMainPanel.getLocalisationVisualisation()==null || staticMainPanel.getControlPanels()==null){
				staticMainPanel.createNorthPanel();
				staticMainPanel.create3DMap();
				staticMainPanel.createEastPanel();
				initComplete=true;
			}

			
			//check if a new module has been added.
			int N=dataBlocks.size();
			findDataBlocks();
			if (dataBlocks.size()!=N){
				findDataBlocks();
				staticMainPanel.dataBlockListChanged();
			}
			
			//refresh control panel data.
			if (initComplete==true){
			staticMainPanel.refreshControlPanelData();
			}
			
			break;
			
			
		case PamController.ADD_DATABLOCK:
//			System.out.println("ADD_DATABLOCK");
//			if (initComplete==true){
//			findDataBlocks();
//			staticDialog.dataBlockListChanged();
//			}
		break;
		
		case PamController.REMOVE_DATABLOCK:
//			System.out.println("REMOVE_DATABLOCK");
			if (initComplete==true){
				findDataBlocks();
				staticMainPanel.dataBlockListChanged();
				staticMainPanel.refreshControlPanelData();
			}
			break;
			
//		case PamController.HYDROPHONE_ARRAY_CHANGED:
//			//redraw the hydrophone array on the 3D map. 
//			staticMainPanel.getDialogMap3D().createArrayandSea(); 
//			//get all the localisation algorithms and set the currentDetection to null. Whenever they are next used this will force a recalculation of the time delay errors and the hydrophone array. 
//			for (int i=0; i<staticLocaliser.getAlgorithms().size(); i++){
//				staticLocaliser.getAlgorithms().get(i).setCurrentDetection(null);
//			}
//		
//		break;
		
		}
	}
	
	/**
	 * Simple function to test time delays are not all null. Try to use as little as possible.
	 * @param timeDelays
	 * @return
	 */
	public static boolean testTimeDelays(ArrayList<ArrayList<Double>> timeDelays){
		if (timeDelays==null){
			return false;
		}
		///test to ensure that time delays are valid for localisation
		///must have at least one delay
		if (timeDelays.get(0).size()==0){
			return false;
		}
		///all delays cannot equal null
		boolean isAllnuLL=true;
		for (int i=0; i<timeDelays.size();i++){
			for (int j=0; j<timeDelays.get(i).size();j++){
				if (timeDelays.get(i).get(j)!=null) isAllnuLL=false;
			}
		}
		if (isAllnuLL==true) {
			return false;
		}
		return true;
	}
	

	public StaticLocalise getStaticLocaliser() {
		return staticLocaliser;
	}

	public StaticLocaliserParams getParams() {
		return staticLocaliserParams;
	}

	public void setParams(StaticLocaliserParams newParams) {
		this.staticLocaliserParams=newParams;
	}
	
	public StaticLocaliserOfflineFunctions getOfflineFunctions(){
		return offlineFunctions;
	}
	
	/**
	 * Gets a list of hydrophones in the current selected datablock. If multiple datablocks are present then uses the first datablock (.get(0)).
	 * @return
	 */
	public int[] getHydrophoneList(){
		int [] channelArray=PamUtils.getChannelArray(currentDataBlock.getChannelMap());
		if (channelArray==null) channelArray=new int[1];	
		return channelArray;
	}
	
	//options settings
	
	@Override
	public Serializable getSettingsReference() {
		return staticLocaliserParams;
	}

	@Override
	public long getSettingsVersion() {
		return StaticLocaliserParams.serialVersionUID;
	}
	
	@Override
	public String getUnitType() {
		return "Static Localiser Options";
	}
	

	@Override
	public boolean restoreSettings(
		PamControlledUnitSettings pamControlledUnitSettings) {
		staticLocaliserParams = ((StaticLocaliserParams) pamControlledUnitSettings.getSettings()).clone();		
		return true;
	}
	

}
