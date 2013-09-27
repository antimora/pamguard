package staticLocaliser.panels;

import java.awt.BorderLayout;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import pamScrollSystem.AbstractScrollManager;
import pamScrollSystem.ViewerScrollerManager;

import clickDetector.ClickControl;
import clickDetector.ClickDetection;
import clickDetector.ClickDetectionMatch;

import clickDetector.ClickClassifiers.ClickClassifierManager;
import clickDetector.ClickClassifiers.ClickIdentifier;
import clickDetector.offlineFuncs.OfflineEventDataBlock;
import clickDetector.offlineFuncs.OfflineEventDataUnit;
import clickDetector.offlineFuncs.OfflineEventListPanel;

import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;

import GPS.GPSDataBlock;
import GPS.GpsDataUnit;
import Layout.PamAxis;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamDetection.PamDetection;
import PamUtils.PamUtils;
import PamView.PamColors;
import PamView.PamPanel;
import PamView.PamColors.PamColor;

/**
 * Panel for selecting a detection from the the offline click data block. An event or multiple events can be selected from a table and individual clicks selected form a drop down combo box. 
 * @author Jamie Macaulay 
 *
 */

@SuppressWarnings("rawtypes")
public class ClickEventLocaliserControl extends AbstractLocaliserControl implements LocaliserControlModel{
	
	
	private StaticLocalise staticLocalise;
	StaticLocaliserControl staticLocaliserControl;
	ClickControl clickControl;
	OfflineEventDataBlock dataBlock;
	
	//Data
	ArrayList<OfflineEventDataUnit> selectedEvents;
	ArrayList<PamDetection> currentClicks;
	PamDetection currentClick;
	int currentID=-1;
	String[] detections;
	String[] classifiers;
	int[] classifierID;
	int clickType=ClickDetectionMatch.USE_EVENT;

	
	//Dialog Components
	private PamPanel mainPanel;
	private OfflineEventListPanel offlineEventListPanel;
	private JComboBox selectDetection;
	private JComboBox selectClassifiaction;
	private JCheckBox showEchos; 
	private ClickPlotPanelEvent clickPlot;
	private PamAxis xAxis, yAxis;
	
	private boolean disableEventTableListener=false;
	

    final static BasicStroke solid =
        new BasicStroke(1f);
	
	
	public ClickEventLocaliserControl(StaticLocaliserControl staticLocaliserControl){
		
		dataBlock=(OfflineEventDataBlock) staticLocaliserControl.getCurrentDatablock();
		dataBlock.getParentProcess().getPamControlledUnit();
		clickControl=(ClickControl) dataBlock.getParentProcess().getPamControlledUnit();
		
		this.staticLocaliserControl=staticLocaliserControl;
		
		JPanel arrowButtons=new JPanel(new GridLayout(1,2));
		BasicArrowButton arrowBack=new BasicArrowButton(BasicArrowButton.WEST);
		arrowBack.addActionListener(new ArrowBack());
		BasicArrowButton arrowForward=new BasicArrowButton(BasicArrowButton.EAST);
		arrowForward.addActionListener(new ArrowForward());
		arrowForward.setEnabled(true);
		arrowButtons.add(arrowBack);
		arrowButtons.add(arrowForward);
		
		JPanel detectionSelection=new JPanel(new GridLayout(0,1));
		
		detectionSelection.setBorder(new TitledBorder("Click Selection"));
		selectDetection=new JComboBox();
		selectDetection.addActionListener(new SelectDetection());
		selectDetection.setLightWeightPopupEnabled(false);
		detectionSelection.add(new JLabel("Select Detection"));
		detectionSelection.add(selectDetection);
		detectionSelection.add(arrowButtons);
		detectionSelection.add(showEchos=new JCheckBox("Filter Echoes"));
		showEchos.addActionListener(new EchoFilter());
		
		Object[] classificationInfo=setClickClassifiactionList(clickControl);
		classifiers=(String[]) classificationInfo[0];
		classifierID=(int[]) classificationInfo[1];
		selectClassifiaction=new JComboBox<String>(classifiers);
		selectClassifiaction.addActionListener(new SelectSpecies());
		selectClassifiaction.setLightWeightPopupEnabled(false);
		detectionSelection.add(new JLabel("Filter Species"));
		detectionSelection.add(selectClassifiaction);
		detectionSelection.setPreferredSize(new Dimension(160,StaticLocalisationMainPanel.controlPanelDimensions.height));
		
		mainPanel=new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Click Event Control"));

		JPanel eventListPanel = new JPanel(new BorderLayout());
		offlineEventListPanel = new OfflineEventListPanel(clickControl);
		offlineEventListPanel.getTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		offlineEventListPanel.getPanel().setPreferredSize(StaticLocalisationMainPanel.controlPanelDimensions);
		eventListPanel.add(BorderLayout.CENTER, offlineEventListPanel.getPanel());
		eventListPanel.add(BorderLayout.WEST,offlineEventListPanel.getSelectionPanel());
		offlineEventListPanel.addListSelectionListener(new ListSelection());
		
		JPanel clickPlotPanel=new JPanel(new BorderLayout());
		clickPlot=new ClickPlotPanelEvent();
		clickPlot.setPreferredSize(new Dimension(300,StaticLocalisationMainPanel.controlPanelDimensions.height));
		clickPlotPanel.setBorder(new TitledBorder("Click Plot"));
		clickPlotPanel.add(BorderLayout.CENTER,clickPlot);
		
		JPanel eastPanel=new  JPanel(new BorderLayout());
		eastPanel.add(BorderLayout.WEST,detectionSelection);
		eastPanel.add(BorderLayout.CENTER,clickPlotPanel);
		
		mainPanel.add(BorderLayout.CENTER, eventListPanel);
		mainPanel.add(BorderLayout.EAST,eastPanel);
		mainPanel.setVisible(true);
		mainPanel.setPreferredSize(StaticLocalisationMainPanel.controlPanelDimensions);
		
		offlineEventListPanel.setShowSelection(OfflineEventListPanel.SHOW_SELECTION);
		offlineEventListPanel.tableDataChanged();
	
		
	}
	
	//action listeners...
	/**
	 * Listens for which events are selected. Single or multiple events can be selected. These create a list of possible detections
	 * 
	 * @author EcologicUK
	 *
	 */
	private class ListSelection implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			if (disableEventTableListener==false){
				getSelectedEvents();
				setDetectionListComboBox();
			}
		}
	}
	

	private class SelectSpecies implements ActionListener{
	@Override
	 public void actionPerformed(ActionEvent e) {
		 currentID=classifierID[ selectClassifiaction.getSelectedIndex()];
		 setDetectionListComboBox();
		 if (currentID!=-1){
			 clickType=ClickDetectionMatch.USE_EVENT+ClickDetectionMatch.USE_CLASSIFICATION;
		 }
		 else{
			 clickType=ClickDetectionMatch.USE_EVENT;
		 }
	    }
	}
	
	private class EchoFilter implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			setDetectionListComboBox();
		}
	}
	

	

		private class ArrowBack implements ActionListener {
			@Override
			 public void actionPerformed(ActionEvent e) {
				changeSelectedClick(selectDetection.getSelectedIndex()-1);
			}
		}
		

		private class ArrowForward implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e)  {
				
				changeSelectedClick(selectDetection.getSelectedIndex()+1);
				
			}
		}
		
		
		/**Changes the comboBox, clickPlot and current data to the ith of the current datablock
		 * 
		 * @param click
		 */
		public void changeSelectedClick(int click){
			if (currentClicks!=null){
				if (click<currentClicks.size() && click>=0){
				 currentClick=(ClickDetection) currentClicks.get(click);
				 	if (currentClick!=null){
					 clickPlot.repaint();
					 System.out.println("ClickControl: Change Selected Click");
				 	}
				 selectDetection.setSelectedIndex(click);
			 	}
			}
		}
	
	private class SelectDetection implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			changeSelectedClick(selectDetection.getSelectedIndex());
			 notifyDetectionUpdate();
		    }
		}
		
	
	
	/**
	 * Resets the combo box to the latest list 
	 */
	public void setDetectionListComboBox(){
		
		setClickDetectionList(selectedEvents);
		selectDetection.removeAllItems();
		selectDetection.setEnabled(true);
		if (detections==null){
			selectDetection.setEnabled(false);
			return;
		}
		for (int i=0; i<detections.length;i++){
			selectDetection.addItem(detections[i]);
		}
		if (detections.length>0 ) 	selectDetection.setSelectedIndex(0);
	}
	
	
	
	
	/**Make a list of all the clicks selected in the event table and which have been classified as the selected species. 
	 * 
	 * @param selectedEvents
	 */
	public void setClickDetectionList(ArrayList<OfflineEventDataUnit> selectedEvents){
		
		boolean nullEvent=false;
		
		if (selectedEvents==null ){
			detections =null;	
			currentClicks=null;
			currentClick=null;
			return;
		}
		if (selectedEvents.size()==0 ){
			detections =null;	
			currentClicks=null;
			currentClick=null;
			return;
		}
		
//		for (int i=0; i<selectedEvents.size(); i++){
//			if (selectedEvents.get(i)!=null && selectedEvents.get(i).getSubDetectionsCount()!=0){
//				nullEvent=false;
//			}
//		}
		
		if (nullEvent==false){
	
			int n=0;
			int nSize=0;
			
			disableEventTableListener=true;
			
			for (int i=0;i<selectedEvents.size();i++){
				checkDataLoadTime(selectedEvents.get(i));
				nSize+=selectedEvents.get(i).getSubDetectionsCount();
			}
			
		
			detections =new String[nSize];
			currentClicks=new ArrayList<PamDetection>(nSize);
			ClickDetection clickDetection;
			
			for (int i=0;i<selectedEvents.size();i++){
			
				System.out.println("CheckLoad: " +(selectedEvents.get(i)));
				
				checkDataLoadTime(selectedEvents.get(i));
				
				System.out.println("CheckLoadComplete: " +(currentClicks.size()));
				
				for (int j=0;j<	selectedEvents.get(i).getSubDetectionsCount();j++){
					
					clickDetection=selectedEvents.get(i).getSubDetection(j);
					// filter by species
					if (canLocaliseDetection(clickDetection)==true){
							detections[n]=("Click: "+n+"  Event: "+ selectedEvents.get(i).getEventNumber()+"  id: "+clickDetection.getClickType());
							currentClicks.add(clickDetection);
							n++;
					}
					
					}
				}
			
			
			if (currentClicks.size()==0){
				currentClicks=null;
				currentClick=null;
				detections=null;
			}
			
//			offlineEventListPanel.setSelectedEvents(selectedEvents);
			disableEventTableListener=false;
		}
		
		else{
			currentClicks=null;
			currentClick=null;
			detections=null;
		}


	}
	
	
	public static Object[] setClickClassifiactionList(ClickControl clickControl){
		ClickIdentifier clickClassifier;
		String[] speciesList;
		int[] codeList;
		ClickClassifierManager manager=clickControl.getClassifierManager();
		ArrayList<String> speciesClassifier=new ArrayList<String>();
		ArrayList<Integer> classifierID=new ArrayList<Integer>();
		
		for (int i=0; i<	manager.getNumClassifiers(); i++){
			 clickClassifier=manager.getClassifier(i);
			
			 if (clickClassifier!=null){
				 speciesList=clickClassifier.getSpeciesList();
				 codeList=clickClassifier.getCodeList();
				 				 if (speciesList!=null){
					 for (int j=0; j<speciesList.length;j++){
						 speciesClassifier.add(speciesList[j]);
						 classifierID.add(codeList[j]);
					 }
				 }
			 }
		}
		
		String[] classifiers=new String[speciesClassifier.size()+1];
		int [] classifierIDi=new int[speciesClassifier.size()+1];
		
		classifiers[0]="None";
		classifierIDi[0]=-1;
		for (int j=1; j<speciesClassifier.size()+1;j++){
			classifiers[j]=speciesClassifier.get(j-1);
			classifierIDi[j]=classifierID.get(j-1);
		}
		
		Object[] clickClassifierInfo=new Object[2];
		clickClassifierInfo[0]=classifiers;
		clickClassifierInfo[1]=classifierIDi;
		
		return clickClassifierInfo;
	}
	
	

	
	@Override
	public String getLocaliserPanelName(){
		return "Event Localiser Panel";
	}
	
	
	private void getSelectedEvents(){
		int[] selectedRows=offlineEventListPanel.getTable().getSelectedRows();
		selectedEvents= new ArrayList<OfflineEventDataUnit>();
		for (int i=0; i<selectedRows.length;i++){
		selectedEvents.add(	offlineEventListPanel.getSelectedEvent(selectedRows[i]));
		}			
	}
	


		/**
		 * Creates a plot of the current click waveform.
		 * @author EcologicUK
		 *
		 */
		public class ClickPlotPanelEvent extends PamPanel {
			
			ClickDetection click;
			
			private static final long serialVersionUID = 1L;

			public ClickPlotPanelEvent() {
				super(PamColor.PlOTWINDOW);
			}
			
			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				          RenderingHints.VALUE_ANTIALIAS_ON);
				        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
				          RenderingHints.VALUE_RENDER_QUALITY);
				        
				       

				super.paintComponent(g2);
			
			        if (getWaveform()!=null){
				        paintPLineSpectrum(g2,getWaveform(), getBounds());
			        }
			}
			
			public double[][] getWaveform(){
				this.click=(ClickDetection) currentClick;
				if (currentClick!=null){
				return click.getWaveData();
				}
				else{
				return null;
				}
			}
			

		}
		
		
		public static void paintPLineSpectrum(Graphics2D g2 ,double[][] waveform2d, Rectangle r){
			
		 	g2.setStroke(solid);
			GeneralPath waveformPolygon;
			double[] wave;
			double xScale;
			double yScale;
			double yGap;
			double x1;
			double y1;
			int height=(int) (r.height*0.9);
			
			double maxVal=Double.MIN_VALUE;
			double minVal=Double.MAX_VALUE;
			
			for (int i=0; i<waveform2d.length; i++){
				for (int j=0; j<waveform2d[i].length; j++){
					if(waveform2d[i][j]>maxVal){
						maxVal=waveform2d[i][j];
					}
					if(waveform2d[i][j]<minVal){
						minVal=waveform2d[i][j];
					}
				}
			}
			
			yGap=height/waveform2d.length;
			
			for (int i=0; i<waveform2d.length; i++){
				waveformPolygon =new GeneralPath();
				wave=waveform2d[i];
				
				xScale = (double) r.width / (double)( wave.length - 1);
				yScale = height/((maxVal-minVal)*waveform2d.length);

				x1 = 0;
				y1 =(int) ((yScale * (wave[0]))+(Math.abs(minVal*(yScale)))+(yGap*i));
				waveformPolygon.moveTo(0.0,y1);

				for (int j=0; j<waveform2d[i].length; j++){
					x1 = (int) (j * xScale);
					y1 = (int) ((yScale * (wave[j]))+(Math.abs(minVal*(yScale)))+(yGap*i));
					waveformPolygon.lineTo((int) x1, (int) y1);
				}
				
				g2.setPaint(PamColors.getInstance().getChannelColor(i));
				g2.draw(waveformPolygon);
			}
		}
		

		


	@Override
	public JPanel getPanel() {
		return mainPanel;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		for (int i=0; i<currentClicks.size(); i++){
			if (currentClicks.get(i)==pamDetection){
				currentClick=(ClickDetection) pamDetection;
				changeSelectedClick(i);
			}
			//need this for batch run I think. If the timeMillis and channel are the same then assume it's the same detection...a bit dodge but can't think of a better way. 
			if (currentClicks.get(i).getTimeMilliseconds()==pamDetection.getTimeMilliseconds() && pamDetection.getChannelBitmap()==currentClicks.get(i).getChannelBitmap()){
				changeSelectedClick(i);
			}
		}
		
	}



	@Override
	public void refreshData() {
		setDetectionListComboBox();
		offlineEventListPanel.tableDataChanged();
		mainPanel.repaint();
	}



	@SuppressWarnings("rawtypes")
	@Override
	public PamDetection getCurrentDetection() {
		return currentClick;
	}




	@SuppressWarnings("rawtypes")
	@Override
	public ArrayList<PamDetection> getCurrentDetections() {
		return currentClicks;
	}




	@Override
	public Integer getDetectionType() {
		if (showEchos.isSelected()) return clickType+ClickDetectionMatch.NO_ECHO;
		return clickType;
	}


	@Override
	public void update(int flag) {
		
	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticLocaliserControl.getStaticMainPanel();
	}

	private boolean canLocaliseDetection(PamDetection pamDetection){
		ClickDetection clickDetection= (ClickDetection) pamDetection;
		
		//check the click is included in the channel list. Note all clicks must be included in the channel list to be excluded;
		int[] clickChannels=PamUtils.getChannelArray(pamDetection.getChannelBitmap());
		
		int[] channels=staticLocaliserControl.getParams().channels;
		if (channels!=null){
			boolean included=false; 
			for (int i=0; i<clickChannels.length; i++){
				for (int j=0; j<channels.length; j++){
					if (clickChannels[i]==channels[j]) included=true;
				}
			}
			if (included==false) return false;
		}
		
		if (currentID!=(int) clickDetection.getClickType() && currentID!=-1) return false;
		
		if (showEchos.isSelected() && clickDetection.isEcho()==true) return false;
		
		return true;
	}


	@Override
	public boolean canLocalise(PamDetection pamDetection) {
		
		ClickDetection clickDetection= (ClickDetection) pamDetection;

		boolean eventSelected=false;
		if (selectedEvents==null) return false;
		//is this click a part of the selected events
		for (int i=0; i<selectedEvents.size(); i++){
			for (int j=0; j<selectedEvents.get(i).getSubDetectionsCount(); j++){
				if (selectedEvents.get(i).getSubDetection(j)==pamDetection) eventSelected=true; 
			}
		}
		System.out.println("Event Selected: "+eventSelected);

		if (eventSelected==true) return canLocaliseDetection(clickDetection);
		
		return false;
	}




	@Override
	public int[] getChannelMap() {
		return PamUtils.getChannelArray(clickControl.getClickDataBlock().getChannelMap());
	}
	
	
	public boolean checkDataLoadTime(OfflineEventDataUnit event) {
		if (event == null) {
			return false;
		}

		long evStart = event.getTimeMilliseconds() - 1000;
		long evEnd = event.getEventEndTime() + 1000;
		long gpsStart = evStart - 10*60*1000; // 10 minutes.

		/*
		 * First try to get there with a standard scroll of the whole thing ...
		 * Problem is that the standard scroll manager will load data
		 * asynchronously, so it may not be there !!!!!
		 */
		
		//System.out.println(offlineClickDataBlock.getCurrentViewDataStart());
		//System.out.println(offlineClickDataBlock.getCurrentViewDataEnd());
		
		if (clickControl.getClickDataBlock().getCurrentViewDataStart() > evStart || 
				clickControl.getClickDataBlock().getCurrentViewDataEnd() < evEnd) {
			ViewerScrollerManager scrollManager = (ViewerScrollerManager) AbstractScrollManager.getScrollManager();
			if (scrollManager != null) {
				scrollManager.startDataAt(clickControl.getClickDataBlock(), evStart, true);
				PamController.getInstance().notifyModelChanged(PamControllerInterface.DATA_LOAD_COMPLETE);
			}
		}

		/**
		 * There is a chance that not enough data will have been loaded if the standard
		 * display load time is lower than the event length, in which case, take a
		 * more direct approach to data loading...
		 */
		if (clickControl.getClickDataBlock().getCurrentViewDataStart() > evStart || 
				clickControl.getClickDataBlock().getCurrentViewDataEnd() < evEnd) {
			
			
						System.out.println("Loading more data for event " + event.getDatabaseIndex());
						
						clickControl.getClickDataBlock().getUnitsCount();
						
						System.out.println("data #= "+dataBlock.getUnitsCount());
						
			//			dataBlock.loadViewerData(evStart, evEnd); // don't load the events - they are all in memory anyway. 
						clickControl.getClickDataBlock().loadViewerData(evStart, evEnd, null);

			System.out.println("data #= "+dataBlock.getUnitsCount());
			
			PamController.getInstance().notifyModelChanged(PamControllerInterface.DATA_LOAD_COMPLETE);
		}

		/**
		 * The GPS data may be needed from some minutes earlier o work out the hydrophone
		 * position
		 */
		GPSDataBlock gpsDataBlock = (GPSDataBlock) PamController.getInstance().getDataBlock(GpsDataUnit.class, 0);
		if (gpsDataBlock == null) {
			return false;
		}
		if (gpsDataBlock.getCurrentViewDataStart() > gpsStart || gpsDataBlock.getCurrentViewDataEnd() < evEnd) {
			// need to load GPS data too
			gpsDataBlock.loadViewerData(gpsStart, evEnd, null);
		}

		return true;
	}

}
