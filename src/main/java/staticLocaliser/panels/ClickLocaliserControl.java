package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import bearingTimeDisplay.BearingAmpAxis;
import bearingTimeDisplay.GraphDisplay;
import bearingTimeDisplay.TimeAxis;

import clickDetector.ClickControl;
import clickDetector.ClickDataBlock;
import clickDetector.ClickDetection;
import clickDetector.ClickDetectionMatch;

import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;

import PamDetection.PamDetection;
import PamUtils.PamUtils;
import PamView.PamColors.PamColor;
import PamView.PamPanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

@SuppressWarnings("rawtypes")
public class ClickLocaliserControl extends AbstractLocaliserControl implements LocaliserControlModel{
	
	
	private StaticLocalise staticLocalise;
	StaticLocaliserControl staticLocaliserControl;
	private ClickControl clickControl;
	
	//detections
	ClickDataBlock clickDataBlock;
	ClickDetection currentClick;
	ArrayList<PamDetection> currentClicks;
	int currentID=-1;
	String[] detections;
	String[] classifiers;
	int[] classifierID;
	int clickType=ClickDetectionMatch.USE_ALL;
	
	
	//Dialog Components
	private PamPanel mainPanel;
	private ClickPlotPanel clickPlot;
	private JComboBox<String> selectDetection;
	private JComboBox<String> selectClassifiaction;
	//TODO
	private GraphDisplay clickBTDisplay;
	private JCheckBox showEchos;
	
	//Dialog Components 
	
	public ClickLocaliserControl(StaticLocaliserControl staticLocaliserControl){
			
		this.staticLocaliserControl=staticLocaliserControl;

		
		clickDataBlock=(ClickDataBlock) staticLocaliserControl.getCurrentDatablock();
		clickDataBlock.getParentProcess().getPamControlledUnit();
		clickControl=(ClickControl) clickDataBlock.getParentProcess().getPamControlledUnit();
	
		Object[] classificationInfo=ClickEventLocaliserControl.setClickClassifiactionList(clickControl);
		classifiers=(String[]) classificationInfo[0];
		classifierID=(int[]) classificationInfo[1];
		
		PamPanel arrowButtons=new PamPanel(new GridLayout(1,2));
		BasicArrowButton arrowBack=new BasicArrowButton(BasicArrowButton.WEST);
		arrowBack.addActionListener(new ArrowBack());
		BasicArrowButton arrowForward=new BasicArrowButton(BasicArrowButton.EAST);
		arrowForward.addActionListener(new ArrowForward());
		arrowForward.setEnabled(true);
		arrowButtons.add(arrowBack);
		arrowButtons.add(arrowForward);
		
		PamPanel detectionSelection=new PamPanel(new GridLayout(0,1));
		
		detectionSelection.setBorder(new TitledBorder("Click Selection"));
		selectDetection=new JComboBox<String>();
		selectDetection.addActionListener(new SelectDetection());
		selectDetection.setLightWeightPopupEnabled(false);
		detectionSelection.add(new JLabel("Select Detection"));
		detectionSelection.add(selectDetection);
		detectionSelection.add(arrowButtons);
		detectionSelection.add(showEchos=new JCheckBox("Filter Echoes"));
		showEchos.addActionListener(new EchoFilter());
		
		createClickList();
		
		selectClassifiaction=new JComboBox<String>(classifiers);
		selectClassifiaction.addActionListener(new SelectSpecies());
		selectClassifiaction.setLightWeightPopupEnabled(false);
		detectionSelection.add(new JLabel("Filter Species"));
		detectionSelection.add(selectClassifiaction);
		detectionSelection.setPreferredSize(new Dimension(160,StaticLocalisationMainPanel.controlPanelDimensions.height));
		
		clickPlot=new ClickPlotPanel();
		PamPanel clickPlotPanel=new PamPanel(new BorderLayout());
		clickPlotPanel.setBorder(new TitledBorder("Click Plot"));
		clickPlotPanel.add(BorderLayout.CENTER,clickPlot);
		clickPlotPanel.setPreferredSize(new Dimension(300,StaticLocalisationMainPanel.controlPanelDimensions.height));
		
		//TODO
		clickBTDisplay=new BTDisplay(staticLocaliserControl.getFrame());
		TimeAxis timeAxis=new TimeAxis(clickBTDisplay, TimeAxis.HORIZONTAL );
		BearingAmpAxis btAxis=new BearingAmpAxis(clickBTDisplay, BearingAmpAxis.VERTICAL);
		clickBTDisplay.setXAxis(timeAxis);
		clickBTDisplay.setYAxis(btAxis);
	

		PamPanel eastPanel=new PamPanel(new BorderLayout());
		eastPanel.add(BorderLayout.EAST, clickPlotPanel);
		eastPanel.add(BorderLayout.CENTER,detectionSelection);
		
		mainPanel=new PamPanel(new BorderLayout());
		mainPanel.add(BorderLayout.EAST,eastPanel);
		//TODO
		mainPanel.add(BorderLayout.CENTER,clickBTDisplay);
		mainPanel.setBorder(new TitledBorder("Localiser Control"));		
		mainPanel.setPreferredSize(StaticLocalisationMainPanel.controlPanelDimensions);
		
		
	}
	
	
	private class SelectSpecies implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			 currentID=classifierID[ selectClassifiaction.getSelectedIndex()];
			 setDetectionListComboBox();
			 if (currentID!=-1){
				clickType=ClickDetectionMatch.USE_CLASSIFICATION;
			 }
			 else{
				 clickType=ClickDetectionMatch.USE_ALL;
			 }
		    }
		}
	
	private class EchoFilter implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			setDetectionListComboBox();
		}
	}
	
	
	private class SelectDetection implements ActionListener{
		@Override
		 public void actionPerformed(ActionEvent e) {
			changeSelectedClick(selectDetection.getSelectedIndex());
			notifyDetectionUpdate();
		    }
		}
	
	
	private class ArrowBack implements ActionListener {
		@Override
		 public void actionPerformed(ActionEvent e) {
			int click=selectDetection.getSelectedIndex()-1;
			changeSelectedClick(click);
		}
	}
	

	private class ArrowForward implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e)  {
			int click=selectDetection.getSelectedIndex()+1;
			changeSelectedClick(click);
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
				 clickBTDisplay.setSelectedDetection(currentClick);
			 	}
			 selectDetection.setSelectedIndex(click);
		 	}
		}
	}
	

	
	/**
	 * Creates a list of all loaded clicks
	 */
	public void createClickList(){
		ClickDetection click;
		currentClicks=new ArrayList<PamDetection>();
		System.out.println(staticLocaliserControl.getCurrentDatablock());
		clickDataBlock=(ClickDataBlock) staticLocaliserControl.getCurrentDatablock();
		ListIterator<ClickDetection> clicks=clickDataBlock.getListIterator(0);
		
		if (clickDataBlock.getFirstUnit()==null) {
			setallNull();
			return;
		}

		click=clickDataBlock.getFirstUnit();
		
		if (canLocalise(click)==true) 	currentClicks.add(click);
		
		
		while (clicks.hasNext()){
			click =clicks.next();
			if (canLocalise(click)==true) currentClicks.add(click);
		}

		if (currentClicks.size()==0){
			setallNull();
			return;
		} 
		
		detections=new String[	currentClicks.size()];
		for (int j=0;j<	currentClicks.size();j++){	
			this.detections[j]=("Click "+j+ " id ");
		}
		

	}
	
	/**
	 * Set current detection to null
	 */
	public void setallNull(){
		currentClicks=null;
		currentClick=null;
		detections=null;
	}
	
	
	
	/**
	 * Resets the combo box to the latest list 
	 */
	public void setDetectionListComboBox(){
		createClickList();

//		if (detections!=null){
//		System.out.println("Detections size: "+detections.length);
//		}
		
		selectDetection.removeAllItems();
		selectDetection.setEnabled(true);
		if (detections==null){
			selectDetection.setEnabled(false);
			return;
		}
		
		for (int i=0; i<detections.length;i++){
			selectDetection.addItem(detections[i]);
		}
		if (detections.length>0 ) selectDetection.setSelectedIndex(0);
	}	
	
	class BTDisplay extends GraphDisplay {

		public BTDisplay(Frame frame) {
			super(frame);
		}
		
		@Override
		public void detectionSelected(PamDataUnit dataUnit){
			selectClick(dataUnit);
			//changeSelectedClick(dataUnit.getAbsBlockIndex());
		}

	}
	
	public void selectClick(PamDataUnit click){
		//System.out.println("ClickLocaliserControl.selectClick");
		
		for (int i=0; i<currentClicks.size(); i++){
			if (currentClicks.get(i)==click){
				currentClick=(ClickDetection) click;
				selectDetection.setSelectedIndex(i);
				break;
			}
		}
		
		

//		PamDataUnit listclick;
//		ListIterator lI =currentClicks.listIterator();
//		listclick=currentClicks.get(0);
//		int n=0;
//		while(lI.hasNext()){
//			if (listclick==click){
//					 currentClick=(ClickDetection) lI.next();
//					 clickPlot.repaint();
//					 selectDetection.setSelectedIndex(n);
//			}
//			listclick= (PamDataUnit) lI.next();
//			n++;
//				
//		}

	}

	/**
	 * Plots a click waveform;
	 * @author Jamie Macaulay
	 *
	 */
	 class ClickPlotPanel extends PamPanel {
		 
	
		ClickDetection click;
			
			private static final long serialVersionUID = 1L;

			public ClickPlotPanel() {
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
				        ClickEventLocaliserControl.paintPLineSpectrum(g2,getWaveform(), getBounds());
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
	
	@Override
	public String getLocaliserPanelName(){
		return "Click Localiser Panel";
	}

	@Override
	public JPanel getPanel() {
		return mainPanel;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		for (int i=0; i<currentClicks.size(); i++){
			if (currentClicks.get(i).equals(pamDetection)){
				changeSelectedClick(i);
			}
			//need this for batch run I think. If the timeMillis and channel are the same then assume it's the same detection...a bit dodge but can't think of a better way. 
			if (currentClicks.get(i).getTimeMilliseconds()==pamDetection.getTimeMilliseconds() && pamDetection.getChannelBitmap()==currentClicks.get(i).getChannelBitmap()){
				changeSelectedClick(i);
			}
		}
		
	}


	@Override
	public void update(int flag) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean canLocalise(PamDetection pamDetection) {
		
		ClickDetection click=(ClickDetection) pamDetection;
		
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
		
		//check that the click corresponds to the current ID
		if (click.getClickType()!=currentID && currentID!=-1) return false;
		//check if exclude echoes is selected and if so that the click is not an echo. 
		if (showEchos.isSelected() && click.isEcho()==true) return false;
			
		return true;
	}
	
	@Override
	public void refreshData() {
//		setDetectionListComboBox();
//		ArrayList<PamDataBlock> clickdataBlocks=new ArrayList<PamDataBlock>();
//		clickBTDisplay.removeAllDataBlocks();
//		if (staticLocaliserControl.getParams().channels!=null){
//			clickBTDisplay.setChannels(PamUtils.makeChannelMap(staticLocaliserControl.getParams().channels));
//		}
//		else{
//		clickBTDisplay.setChannels(PamUtils.makeChannelMap(staticLocaliserControl.getCurrentDatablock().getChannelMap()));
//		}
//		clickBTDisplay.addDataBlock(clickDataBlock);

		
	}

	@Override
	public PamDetection getCurrentDetection() {
		return currentClick;
	}

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
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticLocaliserControl.getStaticMainPanel();
	}



	@Override
	public int[] getChannelMap() {
		return PamUtils.getChannelArray(clickDataBlock.getChannelMap());
	}
	

}
