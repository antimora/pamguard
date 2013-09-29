package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;
import staticLocaliser.StaticLocaliserParams;
import staticLocaliser.StaticLocaliserProvider;
import PamView.ColorManaged;
import PamView.PamColors.PamColor;
import PamView.PamColors;
import PamView.PamGridBagContraints;
import PamView.PamPanel;
import PamView.PamTabPanel;
import PamguardMVC.PamDataBlock;
/**
 * Creates the entire dialog panel for the static localiser. The design aim here is to create something much along the line of microsofts new user interface
 * for word and more recently windows eight. The user should be able to access most of the controls (bar a few advanced options) straight from the panel. Although 
 * this provides a friendly user environment it is space intensive. 
 * @author Jamie Macaulay
 *
 */
public class StaticLocalisationMainPanel  implements PamTabPanel{
	
	
	StaticDialogComponent[] dialogComponents=new StaticDialogComponent[5];
	private StaticLocalise staticLocaliser;
	private StaticLocaliserControl staticlocalisercontrol;

	//private LocalisationVisualisation<S> localiserVisualisation;
	private ArrayList<AbstractLocaliserControl> detectionControlPanels;
	private AbstractLocaliserControl detectionControl;
	private DialogMap3DSL dialogMap;   
	private AlgorithmControl algorithmControl;
	private StaticLocalisationResultsPanel staticLocalisationResults;
	private LocalisationVisualisation localisationVisualisation;
	private LocalisationInformation localisationInfo;
	
	//Dialog Components
	private PamPanel userLocaliserControlsPanel;
	private JLayer<JPanel> detectionControlPanel;
	private PamPanel mainPanel;
	private JSplitPane eastPanel;
	private JSplitPane mapEastPanels;
	
	//Panel containing localiser control and datablock selection
	private JComboBox<String> dataBlockList;
	private PamPanel localiserControlPanel;
	JButton run;
	private JButton runAll;
	private JButton batchRun;
	private JButton stop;
	private JButton options;

	
	
	
	
	
	
	public static Dimension controlPanelDimensions=new Dimension(180,200);
	

	public StaticLocalisationMainPanel(StaticLocaliserControl staticlocalisercontrol, PamDataBlock pamDataBlock){
		super();

		this.staticlocalisercontrol=staticlocalisercontrol;
		this.staticLocaliser=staticlocalisercontrol.getStaticLocaliser();

		//Only add panels which do not have JAVA3D components
		this.algorithmControl=new AlgorithmControl(staticLocaliser,this);
		this.staticLocalisationResults=new StaticLocalisationResultsPanel(staticLocaliser, this);
		
		mapEastPanels=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		Border empty = BorderFactory.createEmptyBorder();
		mapEastPanels.setBorder(empty);
		
		mainPanel = new PamPanel(new BorderLayout());
		mainPanel.add(BorderLayout.WEST, algorithmControl.getPanel());
		mainPanel.add(BorderLayout.CENTER,mapEastPanels);
		mainPanel.add(BorderLayout.SOUTH, staticLocalisationResults.getPanel());

		//setHelpPoint("localisation.targetmotion.docs.targetmotion_overview");
	}
	
	public void createAlgorithmPanel(){
		this.algorithmControl=new AlgorithmControl(staticLocaliser,this);
		mainPanel.add(BorderLayout.WEST, algorithmControl.getPanel());
		mainPanel.validate();
	}
	
	
	public void create3DMap(){
		this.dialogMap=new DialogMap3DSL(staticlocalisercontrol,this);
		//mainPanel.add(BorderLayout.CENTER, dialogMap.getPanel());
		mapEastPanels.setLeftComponent(dialogMap.getPanel());
		mainPanel.validate();
	}
	
	
	public void createEastPanel(){
		
		this.localisationVisualisation=new LocalisationVisualisation(this);
		this.localisationInfo=new LocalisationInformation(this);
		
		eastPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, localisationVisualisation.getPanel(), localisationInfo.getPanel());
		eastPanel.setDividerSize(5);
		Border empty = BorderFactory.createEmptyBorder();
		eastPanel.setBorder(empty);
		eastPanel.setDividerLocation(mainPanel.getHeight()/4);
		
		PamPanel eastPanelHolder=new PamPanel(new BorderLayout());
		eastPanelHolder.add(BorderLayout.CENTER,eastPanel);
		eastPanelHolder.setPreferredSize(new Dimension(300,300));
		
		mapEastPanels.setRightComponent(eastPanelHolder);
		mapEastPanels.setDividerLocation(2*mainPanel.getWidth()/3);

		//mainPanel.add(BorderLayout.EAST,eastPanelHolder);
		mainPanel.validate();
	}
	
	/**
	 * The North panel contains the detection specific control panel, the datablock selection combo box and the buttons used to run/stop and configure localisations.
	 */
	public void createNorthPanel(){
		updateControlPanelList();
		localiserControlPanel=new PamPanel(new BorderLayout());
		localiserControlPanel.add(BorderLayout.NORTH,	dataBlockSelectionPanel());
		localiserControlPanel.add(BorderLayout.CENTER,	localiserControlButtons());
		localiserControlPanel.setPreferredSize(controlPanelDimensions);
		detectionControl=detectionControlPanels.get(0);
		detectionControlPanel=detectionControl.getLayerPanel();
		userLocaliserControlsPanel=new PamPanel(new BorderLayout());
		userLocaliserControlsPanel.add(BorderLayout.WEST,localiserControlPanel);
		userLocaliserControlsPanel.add(BorderLayout.CENTER, detectionControlPanel);
		mainPanel.add(BorderLayout.NORTH,userLocaliserControlsPanel);
		mainPanel.validate();
	}
	
	/**
	 * Updates the list of control panels. Use this function if additional datablocks are added.
	 */
	public void updateControlPanelList(){
		StaticLocaliserProvider staticDataBlock;
		detectionControlPanels=new ArrayList<AbstractLocaliserControl>();
		for (int i=0; i<staticlocalisercontrol.getDataBlocksAll().size(); i++){
			 staticDataBlock=(StaticLocaliserProvider) staticlocalisercontrol.getDataBlocksAll().get(i).get(0);
			 staticlocalisercontrol.setCurrentDatablock(staticlocalisercontrol.getDataBlocksAll().get(i).get(0));
			 detectionControlPanels.add(staticDataBlock.getSLControlDialog(staticlocalisercontrol));
		}
	}
	
	/**
	 * Refreshes the data of current control panels. Must set the current dataBlock in localiser control to refresh data. Then must reset to the correct data block. 
	 */
	public void refreshControlPanelData(){
		int N=0;
		for (int i=0; i<detectionControlPanels.size(); i++){
			staticlocalisercontrol.setCurrentDatablock(staticlocalisercontrol.getDataBlocksAll().get(i).get(0));
			detectionControlPanels.get(i).refreshData();
			if (detectionControlPanels.get(i)==detectionControl) N=i;
		}
		staticlocalisercontrol.setCurrentDatablock(staticlocalisercontrol.getDataBlocksAll().get(N).get(0));
	}
	
	public DialogMap3DSL getDialogMap3D(){
		return this.dialogMap;
	}
	
	public LocalisationVisualisation getLocalisationVisualisation(){
		return this.localisationVisualisation;
	}
	
	public JComboBox getDataBlockList(){
		return dataBlockList;
	}
	
	public void updateAll(){
		
	}
	
	/**
	 * Change to the control panel corresponding to the selected datablock. Sets the control panel in the mainPanel and refreshes control panel data;
	 */
	public void changeControlPanel() {
		
		if (detectionControlPanels.get(dataBlockList.getSelectedIndex())==detectionControl) return;
		
		this.detectionControl=detectionControlPanels.get(dataBlockList.getSelectedIndex());
		detectionControl.refreshData();
		userLocaliserControlsPanel.remove(detectionControlPanel);
		detectionControlPanel=detectionControl.getLayerPanel();
		userLocaliserControlsPanel.add(BorderLayout.CENTER, detectionControlPanel);
		userLocaliserControlsPanel.validate();
		localisationVisualisation.update(StaticLocaliserControl.SEL_DETECTION_CHANGED);
	}
	
	/**
	 * Creates the combo box which contains the datablocks which can be localised. Choose true if only single datablocks are to be present in the comboBox. Choose false to include unsynchrnised datablocks
	 */
	public JComboBox createComboDataList(boolean single){
		ArrayList<ArrayList<PamDataBlock>> dataBlocks;
		if (single==true){
		dataBlocks=staticlocalisercontrol.getDataBlocks();
		}
		else{
		dataBlocks=staticlocalisercontrol.getDataBlocksAll();
		}
		
		String[] dataBlockNames=new String[dataBlocks.size()];
		
		for (int i=0; i<dataBlocks.size(); i++){
			dataBlockNames[i]=dataBlocks.get(i).toString();
		};
		
		JComboBox dataBlockList = new JComboBox(dataBlockNames);
		dataBlockList.setMaximumSize(controlPanelDimensions);
		
		return dataBlockList;
	}
	
	
	
	/**
	 * Updates the combo box list. Use this if a new data block has been added. 
	 */
	public void updateComboList(){
		String[] dataBlockNames=new String[staticlocalisercontrol.getDataBlocksAll().size()];
		for (int i=0; i<staticlocalisercontrol.getDataBlocksAll().size(); i++){
			dataBlockNames[i]=staticlocalisercontrol.getDataBlocksAll().get(i).toString();
		};
		//System.out.println("Item count: "+dataBlockList.getItemCount());
		//System.out.println("Item sel: "+dataBlockList.getSelectedItem());
		
		dataBlockList.setSelectedIndex(0);
		
		if (dataBlockList.getItemCount()<=0)return;
		dataBlockList.removeAllItems();
		for (int i=0; i<dataBlockNames.length; i++){
			dataBlockList.addItem(dataBlockNames[i]);
		}
		if (dataBlockNames.length>0){
			dataBlockList.setSelectedIndex(0);
		}
		
	}
	
	
	
	/**
	 * Creates the JPanel which holds the datablock combo box.
	 * @return JPanel with a combo box allowing selection of datablocks which satisfy the static localiser interface.
	 */
	public PamPanel dataBlockSelectionPanel(){
		//create datablock list and add action listener
		dataBlockList=createComboDataList(false);
		//need this to fix the size of the combo box
		dataBlockList.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXX");
		dataBlockList.addActionListener(new SelectDataBlock());	

		PamPanel dataBlockSelectionPanel=new PamPanel(new GridBagLayout());
		PamGridBagContraints c = new PamGridBagContraints();
	
		c.gridx=0;
		c.gridy=0;
		dataBlockSelectionPanel.setBorder(new TitledBorder("Select Detection Data"));
		dataBlockSelectionPanel.add(dataBlockList,c);
		dataBlockList.setPreferredSize(new Dimension(200,20));

		return dataBlockSelectionPanel;
	}
	
	/**
	 * Returns a JPanel with localiser control buttons. These buttons are generic to every datablock
	 * Run- runs a localisation for a single detection
	 * RunAll -runs the localiser for a group of detections, one adetection at a time - allows batch processing
	 * Save- saves the results to a database.
	 * Stop- stops the localisation in it's tracks, results are null;
	 * Options- brings up an options panel which contains options that are generic to every localisation, no matter what the PamDetection is (be careful here);
	 * @return JPanel with localiser controls;
	 */
	
	public PamPanel localiserControlButtons(){
		
		PamPanel localiserControlButtons=new PamPanel(new GridLayout(0,1)); 
		localiserControlButtons.setBorder(new TitledBorder("Localiser Controls"));
		
		//controls for real time
		if (staticlocalisercontrol.isViewer==false){
		JButton runRealTime=new JButton("Localise"); 
		localiserControlButtons.add(runRealTime);
		JButton stopRealTime=new JButton("Stop"); 
		localiserControlButtons.add(stopRealTime);
		localiserControlButtons.add(new JLabel(""));
		}

		//controls for viewer mode
		if (staticlocalisercontrol.isViewer==true){
		run=new JButton("Localise"); 
		run.addActionListener(new Run());
		localiserControlButtons.add(run);
		runAll=new JButton("Localise All"); 
		runAll.addActionListener(new RunAll());
		localiserControlButtons.add(runAll);
		batchRun=new JButton("Batch Localise"); 
		batchRun.addActionListener(new BatchRun());
		localiserControlButtons.add(batchRun);
		stop=new JButton("Stop"); 
		stop.addActionListener(new Stop());
		localiserControlButtons.add(stop);
		}
		
		
		options=new JButton("Options");
		options.addActionListener(new Options() );
		localiserControlButtons.add(options);

		return localiserControlButtons;
	}
	
	public void setLocaliserControlEnabled(boolean enable){
		
		if (staticlocalisercontrol.isViewer==true){ 
			run.setEnabled(enable);
			runAll.setEnabled(enable);
			batchRun.setEnabled(enable);
			dataBlockList.setEnabled(enable);
			options.setEnabled(enable);
////			if (staticlocalisercontrol.getStaticLocaliser().getCurrentRunConfig()!=StaticLocalise.RUN_BATCH){
				getCurrentControlPanel().setlayerPanelEnabled(enable);
////		}
			
		}
	}

	
	///Action Listeners//////////////////////////////////////////////////////
	
	
	public class SelectDataBlock implements ActionListener{
		
		 public void actionPerformed(ActionEvent e) {
			 if (dataBlockList.getSelectedIndex()>=0){
		      staticlocalisercontrol.setCurrentDatablock(staticlocalisercontrol.getDataBlocksAll().get(dataBlockList.getSelectedIndex()).get(0));
		      staticlocalisercontrol.dataBlockChanged();
			 }
		  }
	}
		
	
	public class LocaliserOptions implements ActionListener{
			@Override
			 public void actionPerformed(ActionEvent e) {
			   
			 }
		}
		
		
	public class Run implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			staticlocalisercontrol.run();
		    }
		}
	
	public class RunAll implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			staticlocalisercontrol.runAll();
			
		    }
		}
	
	
	public class BatchRun implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			
			staticlocalisercontrol.getOfflineFunctions().openBatchRunDialog();
			
			//staticlocalisercontrol.runBatch();
			
//			//******temp*******//
//			//checking channel stuff
//			AbstractDetectionMatch detectionMatch=getCurrentControlPanel().getCurrentDetection().getDetectionMatch();
//			System.out.println("HydrophonePos: "+detectionMatch.getHydrophones3d());
//			for (int i=0;i<staticlocalisercontrol.getDataBlocksAll().size();i++){
//				PamDetection pamDetection=(PamDetection) staticlocalisercontrol.getDataBlocksAll().get(i).get(0).getFirstUnit();
//			if (pamDetection!=null){
//				int[] chnls=PamUtils.getChannelArray(pamDetection.getChannelBitmap());
//				for (int j=0; j<chnls.length; j++){
//					System.out.print("Ch: "+chnls[j]);
//				}
//			}
//				System.out.println();
//			}
//			
//			///****temp****/////
//			staticlocalisercontrol.save(staticlocalisercontrol.getStaticLocaliser().getResults().get(0));
		    }
		}
	
	public class Stop implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			staticlocalisercontrol.getStaticLocaliser().cancelThread();
		    }
	}
	
	public class Options implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			openOptionsDialog();
		}
	}
	
	public void openOptionsDialog(){
		Point pt=new Point((int) (staticlocalisercontrol.getFrame().getSize().getWidth()/2),(int) (staticlocalisercontrol.getFrame().getSize().getHeight()/2));
		
		//need a copy of the old channel array to compare to the new one; 
		int[] oldChannels;
		if (staticlocalisercontrol.getParams().channels!=null) oldChannels=staticlocalisercontrol.getParams().channels.clone();
		else oldChannels=null;
		
		StaticLocaliserParams newParams=OptionsDialog.showDialog(staticlocalisercontrol.getFrame(), pt, staticlocalisercontrol.getStaticMainPanel(), staticlocalisercontrol.getParams());
		if (newParams!=null) staticlocalisercontrol.setParams(newParams.clone());
		
		//if the user has changed the channels then we need to refresh the control panel. 
		if (Arrays.equals(staticlocalisercontrol.getParams().channels, oldChannels)==false){
			getCurrentControlPanel().refreshData();
		}

	}

	public ArrayList<AbstractLocaliserControl> getControlPanels(){
		return detectionControlPanels;
	}
	

	public AbstractLocaliserControl getCurrentControlPanel(){
		return detectionControl;
	}
	
	public StaticLocaliserControl getStaticLocaliserControl(){
		return this.staticlocalisercontrol;
	}
	

	public StaticLocalisationResultsPanel getResultsPanel() {
		return staticLocalisationResults;
	}
	
	public LocalisationInformation getLocalisationInformation() {
		return localisationInfo;
	}
	
	


	@Override
	public JComponent getPanel() {
		return mainPanel;
	}
	
	/**
	 * Call this if the datablocks are changed;
	 */
	public void dataBlockListChanged(){
		//create a new arrayList of control Panels
		updateControlPanelList();
		//update comboBox
		updateComboList();
		//selects the correct control panels to display if not already selected
		changeControlPanel();
	}


	@Override
	public JToolBar getToolBar() {
		return null;
	}

	public JPanel getMainPanel() {
		return mainPanel;
	}


	@Override
	public JMenu createMenu(Frame parentFrame) {
		return null;
	}


}
