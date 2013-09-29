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

package clickDetector;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soundPlayback.PlaybackControl;
import staticLocaliser.StaticLocalise;
import targetMotion.TargetMotionLocaliser;

import binaryFileStorage.BinaryStore;

import Filters.FilterDialog;
import Filters.FilterParams;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamUtils;
import PamView.PamGui;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import alarm.AlarmCounter;
import angleVetoes.AngleVetoes;
import clickDetector.ClickClassifiers.ClickClassifierManager;
import clickDetector.ClickClassifiers.ClickClassifyDialog;
import clickDetector.ClickClassifiers.ClickIdentifier;
import clickDetector.alarm.ClickAlarmCounter;
import clickDetector.dialogs.ClickAlarmDialog;
import clickDetector.dialogs.ClickMapDialog;
import clickDetector.dialogs.ClickParamsDialog;
import clickDetector.dialogs.ClickStorageOptionsDialog;
import clickDetector.dialogs.ClickTrainIdDialog;
import clickDetector.echoDetection.EchoDetectionSystem;
import clickDetector.echoDetection.JamieEchoDetectionSystem;
import clickDetector.offlineFuncs.ClicksOffline;
import clickDetector.offlineFuncs.EventListDialog;
import clickDetector.offlineFuncs.OfflineEventDataUnit;
import clickDetector.offlineFuncs.OfflineToolbar;
import clickDetector.offlineFuncs.rcImport.BatchRainbowFileConversion;
import clickDetector.offlineFuncs.rcImport.RainbowDatabaseConversion;

/**
 * Main Controller for click detection.
 * <p>
 * ClickControl contains both the detector and the display panel. It also
 * contains information on Detection and Display menus which will get added to
 * the main PamGuard menu.
 * 
 * @author Doug Gillespie
 * 
 */

public class ClickControl extends PamControlledUnit implements PamSettings {

	protected ClickDetector clickDetector;
	
	protected ClickTrainDetector clickTrainDetector;
	
	protected EchoDetectionSystem echoDetectionSystem;
	
	protected TrackedClickLocaliser trackedClickLocaliser;

	protected ClickTabPanelControl tabPanelControl;

	protected ClickParameters clickParameters = new ClickParameters();

	protected JMenu rightMouseMenu;

	protected ClickTabPanel clickPanel;
	
	protected ClickControl clickControl;
	
	private OfflineToolbar offlineToolbar;
	
	protected ClickSidePanel clickSidePanel;
	
	protected AngleVetoes angleVetoes;
	
    protected ClickAlarmManager clickAlarmManager;

	private ClickIdentifier clickIdentifier;
	
	private ClicksOffline clicksOffline;
	
	private ClickClassifierManager classifierManager;
	
	private boolean viewerMode = false;
		
	private OfflineEventDataUnit latestOfflineEvent;
	
	private TargetMotionLocaliser<OfflineEventDataUnit> targetMotionLocaliser;
	
	private String dataBlockPrefix = "";

	private ClickAlarmCounter alarmCounter;
	
	public ClickControl(String name) {

		super("Click Detector", name);
		
		sortDataBlockPrefix();
		
		viewerMode = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;
		//
//		if (inputControl.getPamProcess(0).getOutputDataBlock(0).getDataType() == DataType.RAW) {
//			rawDataBlock = (PamRawDataBlock) inputControl.getPamProcess(0).getOutputDataBlock(0);
//		}
		clickControl = this;
		
		
		angleVetoes = new AngleVetoes(this);

		offlineToolbar = new OfflineToolbar(this);
		
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			clicksOffline = new ClicksOffline(this);
//			offlineToolbar.addButtons(getClicksOffline().getCommandButtons());
		}
		
		PamSettingManager.getInstance().registerSettings(this);
		
		addPamProcess(clickDetector = new ClickDetector(this));
		
		addPamProcess(clickTrainDetector = new ClickTrainDetector(this, clickDetector.getClickDataBlock()));
		
		addPamProcess(trackedClickLocaliser = new TrackedClickLocaliser(this, clickDetector.getTrackedClicks()));
		
		
		////////////////////////////////////////////////////////////////////////////////
		echoDetectionSystem = new JamieEchoDetectionSystem(this);
		//////////////////////////////////////////////////////////////////////////////
		
		addPamProcess(clickAlarmManager = new ClickAlarmManager(this, clickDetector.getClickDataBlock()));

		setClassifierManager(new ClickClassifierManager(this));
		
		clickIdentifier = getClassifierManager().getClassifier(clickParameters.clickClassifierType);
		setClickIdentifier(clickIdentifier);
		
		setTabPanel(tabPanelControl = new ClickTabPanelControl(this));

		clickPanel = tabPanelControl.getClickPanel();
		
		if (offlineToolbar != null) {
//			tabPanelControl.getPanel().add(BorderLayout.NORTH, offlineToolbar.getToolBar());
			setToolbarComponent(offlineToolbar.getToolBar());
		}
		
		if (PamController.getInstance().getRunMode() != PamController.RUN_PAMVIEW) {
			setSidePanel(clickSidePanel = new ClickSidePanel(this));
		}
		
		if (viewerMode) {
			targetMotionLocaliser = new TargetMotionLocaliser<OfflineEventDataUnit>(this, 
					clickDetector.getOfflineEventDataBlock(), clickDetector.getClickDataBlock());
			clickDetector.getTargetMotionSQLLogging().setTargetMotionLocaliser(targetMotionLocaliser);
		}

		rightMouseMenu = createDisplayMenu(null);

		new ClickSpectrogramPlugin(this);
		
		alarmCounter = new ClickAlarmCounter(this);

	}


	/**
	 * Get the datablock containing click data
	 * @return Click data block
	 */
	public ClickDataBlock getClickDataBlock() {
		return clickDetector.getClickDataBlock();
	}
	
	/**
	 * Speedier way of knowing if it's viewer mode than going back to the controller every time 
	 * @return
	 */
	public boolean isViewerMode() {
		return viewerMode;
	}

	public void secondTimer(long sampleNumber) {

	}

	public int getTrueChannelNumber(int iCh) {
		// get the real input channel number from the bitmap.
		return PamUtils.getNthChannel(iCh, clickParameters.channelBitmap);
	}

	public void displayTriggerHistogram(TriggerHistogram[] triggerHistogram) {
//		clickPanel.getTriggerDisplay()
//				.displayTriggerHistogram(triggerHistogram);
	}

	public void notifyNewStorage(String storageName) {
//		clickPanel.getBtDisplay().notifyNewStorage(storageName);
	}

	@Override
	public JMenu createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem;
		JMenu menu = new JMenu(getUnitName());
		
		if (isViewerMode()) {
			if (clicksOffline.addDetectorMenuItems(parentFrame, menu) > 0) {
				if (targetMotionLocaliser != null) {
					targetMotionLocaliser.addDetectorMenuItems(parentFrame, menu);
				}
				menu.addSeparator();
			}
		}

		menuItem = new JMenuItem("Detection Parameters ...");
		menuItem.addActionListener(new MenuDetection(parentFrame));
		menu.add(menuItem);

		menuItem = new JMenuItem("Digital pre filter ...");
		menuItem.addActionListener(new MenuPreFilter(parentFrame));
		menu.add(menuItem);

		menuItem = new JMenuItem("Digital trigger filter ...");
		menuItem.addActionListener(new MenuTriggerFilter(parentFrame));
		menu.add(menuItem);

		menu.add(angleVetoes.getSettingsMenuItem(parentFrame));
		
		// menuItem = new JMenuItem("Click types ...");
		// menuItem.addActionListener(new menuClickTypes());
//		menu.add(menuItem);
//		if (getClickIdentifier() != null
//				&& getClickIdentifier().getMenuItem(parentFrame) != null) {
//			menu.add(getClickIdentifier().getMenuItem(parentFrame));
//		}
		menuItem = new JMenuItem("Click Classification ...");
		menuItem.addActionListener(new MenuClickClassification(parentFrame));
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Click Train Identification ...");
		menuItem.addActionListener(new MenuClickTrainId(parentFrame));
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Storage Options ...");
		menuItem.addActionListener(new MenuStorageOptions(parentFrame));
		menu.add(menuItem);
		
        menuItem = new JMenuItem("Audible Alarm ...");
        menuItem.addActionListener(new MenuAlarm(parentFrame));
        menu.add(menuItem);

		/*
		 *  if in viewer mode and there is  a binary store
		 *  include options for batch converting clk files. 
		 */
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			BinaryStore binaryStore = (BinaryStore) PamController.getInstance().findControlledUnit(BinaryStore.unitType);
			if (binaryStore != null) {
				menu.addSeparator();
//				JMenuItem subMenu = new JMenu("Rainbow Click Import");
				menuItem = new JMenuItem("Import RainbowClick Data ...");
				menuItem.addActionListener(new BatchConvertClkFiles(parentFrame));
//				subMenu.add(menuItem);
//				menuItem = new JMenuItem("Import event database information ...");
//				menuItem.addActionListener(new BatchConvertClkDatabase(parentFrame));
//				subMenu.add(menuItem);
				
				menu.add(menuItem);
			}
			
			menu.add(clicksOffline.getDatabaseCheckItem(parentFrame));
			menu.add(clicksOffline.getExportMenuItem(parentFrame));
		}
		
		

		return menu;
	}

//	@Override
//	public JMenuItem createHelpMenu(Frame parentFrame) {
//		JMenuItem menuItem;
//		JMenu menu = new JMenu(getUnitName());
//		menuItem = new JMenuItem("Click Detector");
//		menuItem.addActionListener(new StartHelp(parentFrame));
//		menu.add(menuItem);
//		return menuItem;
//	}


	@Override
	public JMenu createDisplayMenu(Frame parentFrame) {
		return tabPanelControl.createMenu(parentFrame);
	}

	private class MenuDetection implements ActionListener {
		
		Frame pf;
		
		public MenuDetection(Frame parentFrame) {
			pf = parentFrame;
		}

		public void actionPerformed(ActionEvent ev) {
			ClickParameters newParameters = ClickParamsDialog.showDialog(pf, clickControl, clickParameters);
			if (newParameters != null) {
				clickParameters = newParameters.clone();
				clickDetector.newParameters();
				newClassifySettings();
				PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);
			}
		}
		
	}

	private boolean modelComplete = false;
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		tabPanelControl.getClickPanel().noteNewSettings();
		switch (changeType) {
		case PamControllerInterface.ADD_CONTROLLEDUNIT: 
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
			if (modelComplete){
				clickDetector.setupProcess();
			}
			break;
		
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			modelComplete = true;
			clickDetector.setupProcess();
			break;
		case PamControllerInterface.DATA_LOAD_COMPLETE:
			setLatestOfflineEvent(null);
		}
	}

	@Override
	public void notifyArrayChanged() {
		super.notifyArrayChanged();
		clickDetector.notifyArrayChanged();
	}

	class MenuPreFilter implements ActionListener {
		
		Frame pf;
		
		public MenuPreFilter(Frame parentFrame) {
			pf = parentFrame;
		}
		public void actionPerformed(ActionEvent ev) {
			FilterParams newParams = FilterDialog.showDialog(pf,
					clickParameters.preFilter, getClickDetector()
							.getSampleRate());
			if (newParams != null) {
				clickParameters.preFilter = newParams;
				clickDetector.newParameters();
			}
		}
	}

	class MenuTriggerFilter implements ActionListener {
		
		Frame pf;
		
		public MenuTriggerFilter(Frame parentFrame) {
			pf = parentFrame;
		}
		public void actionPerformed(ActionEvent ev) {
			FilterParams newParams = FilterDialog.showDialog(pf,
					clickParameters.triggerFilter, getClickDetector()
							.getSampleRate());
			if (newParams != null) {
				clickParameters.triggerFilter = newParams;
				clickDetector.newParameters();
			}
		}
	}

	class MenuClickTrainId implements ActionListener {
		
		Frame pf;
		
		public MenuClickTrainId(Frame parentFrame) {
			pf = parentFrame;
		}
		public void actionPerformed(ActionEvent ev) {
			ClickParameters newParams;
			if ((newParams = ClickTrainIdDialog.showDialog(pf, clickParameters)) != null) {
				clickParameters = newParams.clone();
				//clickDetector.NewParameters();
			}
		}
	}

	class MenuClickClassification implements ActionListener {
		
		Frame pf;
		
		public MenuClickClassification(Frame parentFrame) {
			pf = parentFrame;
		}
		public void actionPerformed(ActionEvent ev) {
			classificationDialog(pf);
		}
	}
	
	/**
	 * Opens the offline click dialog. 
	 * @param pf frame
	 * @return Returns true if settings have changed
	 */
	public boolean classificationDialog(Frame pf) {
		ClickParameters newParams;
		if ((newParams = ClickClassifyDialog.showDialog(this, pf, clickParameters)) != null) {
			clickParameters = newParams.clone();
			tabPanelControl.clickPanel.noteNewSettings();
			newClassifySettings();
			return true;
		}
		return false; // return so that offline dialog can know. 
	}

	private void newClassifySettings() {
		clickIdentifier = getClassifierManager().getClassifier(clickParameters.clickClassifierType);
		if (offlineToolbar != null) {
			offlineToolbar.setupToolBar();
		}
	}

	class MenuStorageOptions implements ActionListener {
		
		Frame pf;
		
		public MenuStorageOptions(Frame parentFrame) {
			pf = parentFrame;
		}
		public void actionPerformed(ActionEvent ev) {
			ClickParameters newParams;
			if ((newParams = ClickStorageOptionsDialog.showDialog(pf, clickParameters)) != null) {
				clickParameters = newParams.clone();
				clickDetector.newParameters();
			}
		}
	}
	
    class MenuAlarm implements ActionListener {

		Frame pf;

		public MenuAlarm(Frame parentFrame) {
			pf = parentFrame;
		}
		public void actionPerformed(ActionEvent ev) {
			ClickParameters newParams;
			if ((newParams = ClickAlarmDialog.showDialog(pf, clickParameters))
                    != null) {
				clickParameters = newParams.clone();
				clickDetector.newParameters();
			}
		}
	}


	/**
	 * Batch convert rainbow click files into the latest 
	 * PAMGUARD binary format. 
	 * @author Doug Gillespie
	 *
	 */
	class BatchConvertClkFiles implements ActionListener {
		
		Frame parentFrame;
		
		public BatchConvertClkFiles(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent ev) {
			batchConvertClicks(parentFrame);
		}
	}
	class BatchConvertClkDatabase implements ActionListener {

		Frame parentFrame;

		public BatchConvertClkDatabase(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent ev) {
			batchConvertClickDatabase(parentFrame);
		}
	}


	/**
	 * Batch convert RainbowClick files to the latest
	 * PAMGUARD binary format. 
	 * @param parentFrame parent frame for dialog. 
	 */
	private void batchConvertClicks(Frame parentFrame) {
		BinaryStore binaryStore = (BinaryStore) PamController.getInstance().findControlledUnit(BinaryStore.unitType);
		if (binaryStore == null) {
			return;
		}
		BatchRainbowFileConversion.showDialog(parentFrame, this, binaryStore);
	}

	public void batchConvertClickDatabase(Frame parentFrame) {
		BinaryStore binaryStore = (BinaryStore) PamController.getInstance().findControlledUnit(BinaryStore.unitType);
		if (binaryStore == null) {
			return;
		}
		RainbowDatabaseConversion.showDialog(parentFrame, this, binaryStore);
	}


	public ClickDetector getClickDetector() {
		return clickDetector;
	}
	
	protected void newRawData(PamObservable source, PamDataUnit data) {
//		tabPanelControl.clickPanel.triggerDisplay.update(source, data);
//		tabPanelControl.clickPanel.btDisplay.update(source, data);
	}

	/*
	 * Since this is not an observer of anything, these actually get called from
	 * the equivalent functions in the Detector
	 */
	public void pamStart() {
//		this.clickPanel.btDisplay.reset();
		if (clickTrainDetector != null) clickTrainDetector.clearAllTrains();
		ArrayList<ClickDisplay> displays = tabPanelControl.clickDisplayManager.getWindowList();
		for (int i = 0; i < displays.size(); i++) {
			displays.get(i).pamStart();
		}
		
//		clickDetector.getSourceProcess().getParentDataBlock().addObserver(tabPanelControl.clickPanel.triggerDisplay);
//		clickDetector.getSourceProcess().getParentDataBlock().addObserver(tabPanelControl.clickPanel.btDisplay);
//		tabPanelControl.clickPanel.btDisplay.sampleRate = clickDetector.getSampleRate();
	}

	/*
	 * Since this is not an observer of anything, these actually get called from
	 * the equivalent functions in the Detector
	 */
	public void pamStop() {
		ArrayList<ClickDisplay> displays = tabPanelControl.clickDisplayManager.getWindowList();
		for (int i = 0; i < displays.size(); i++) {
			displays.get(i).pamStop();
        }
	}

	/*
	 * Stuff for settings interface
	 */

	@Override
	public boolean canClose() {
//		if (isViewerMode()) {
//			getClicksOffline().saveClicks();
//		}
		return super.canClose();
	}

	public long getSettingsVersion() {
		return ClickParameters.serialVersionUID;
	}

	public Serializable getSettingsReference() {
		return clickParameters;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		this.clickParameters = ((ClickParameters) pamControlledUnitSettings
				.getSettings()).clone();
		return true;
	}

	JMenuBar clickTabMenu = null;

	
	@Override
	public JMenuBar getTabSpecificMenuBar(Frame parentFrame, JMenuBar standardMenu, PamGui pamGui) {
		JMenu aMenu;
		// start bymaking a completely new copy.
//		if (clickTabMenu == null) {
			clickTabMenu = standardMenu;
			for (int i = 0; i < clickTabMenu.getMenuCount(); i++) {
				if (clickTabMenu.getMenu(i).getText().equals("Display")) {
					//clickTabMenu.remove(clickTabMenu.getMenu(i));
					
					aMenu = createDetectionMenu(parentFrame);
					aMenu.setText("Click Detection");
					clickTabMenu.add(aMenu, i+1);
					
					aMenu = tabPanelControl.createMenu(parentFrame);
					aMenu.setText("Click Display");
					clickTabMenu.add(aMenu, i+2);
					
					break;
				}
			}
//		}
		return clickTabMenu;
	}

	@Override
	public void addOtherRelatedMenuItems(Frame parentFrame, JMenu menu, String name) {
		if (name.equals("Map")) {
			
			JMenuItem menuItem = new JMenuItem("Click Bearings ...");
			menuItem.addActionListener(new MapOptions(parentFrame));
			menu.add(menuItem);
		}
	}
	protected void clickedOnClick(ClickDetection click) {
		tabPanelControl.clickDisplayManager.clickedOnClick(click);
	}
	/**
	 * 
	 * @author dgillespie
	 * This is a direct copy of the class in ClickTabPanelControl - am 
	 * thinking of getting rid of TabPanelControl though and doing everything
	 * from within each PamControlledUnit. 
	 */
	 class MapOptions implements ActionListener {
		 Frame parentFrame;
		 MapOptions(Frame parentFrame) {
			 this.parentFrame = parentFrame;
		 }
		 public void actionPerformed(ActionEvent ev) {
			 ClickParameters newParameters = 
				 ClickMapDialog.showDialog(parentFrame, clickParameters);
			 if (newParameters != null){
				 clickParameters = newParameters.clone();
				 clickTrainDetector.clickTrains.resetShouldPlots();
			 }
		 }
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#SetupControlledUnit()
	 */
	@Override
	public void setupControlledUnit() {
		// if there is no input data block, get th first one in the system.
//		PamDataBlock rawBlock = PamController.getInstance().getRawDataBlock(clickParameters.rawDataSource);
//		if (rawBlock == null) {
//			rawBlock = PamController.getInstance().getRawDataBlock(0);
//			if (rawBlock != null) clickParameters.rawDataSource = rawBlock.toString();
//		}
		super.setupControlledUnit();
		//tabPanelControl.clickToolBar.setControls(clickParameters);
	}

	public ClickIdentifier getClickIdentifier() {
		return clickIdentifier;
	}

	public void setClickIdentifier(ClickIdentifier clickIdentifier) {
		this.clickIdentifier = clickIdentifier;
		newClassifySettings();
	}

	/**
	 * @param classifierManager the classifierManager to set
	 */
	public void setClassifierManager(ClickClassifierManager classifierManager) {
		this.classifierManager = classifierManager;
	}

	/**
	 * @return the classifierManager
	 */
	public ClickClassifierManager getClassifierManager() {
		return classifierManager;
	}

	public ClickParameters getClickParameters() {
		return clickParameters;
	}

	public void setClickParameters(ClickParameters clickParameters) {
		this.clickParameters = clickParameters;
	}

//	public void newOfflineStore() {
//		clickDetector.setSampleRate(getClicksOffline().getSampleRate(), false);
//		tabPanelControl.newOfflineStore();
//		offlineToolbar.newStore();
//	}
	
	/** 
	 * Called from clicksOffline when data have changed (eg from re doing click id). 
	 * Needs to notify the display and maybe some other classes. 
	 */
	public void offlineDataChanged() {
		tabPanelControl.offlineDataChanged();
	}

	public OfflineToolbar getOfflineToolbar() {
		return offlineToolbar;
	}

	
	/**
	 * @return the clicksOffline
	 */
	public ClicksOffline getClicksOffline() {
		return clicksOffline;
	}

	
	/**
	 * @return the latestOfflineEvent
	 */
	public OfflineEventDataUnit getLatestOfflineEvent() {
		return latestOfflineEvent;
	}

	/**
	 * @param latestOfflineEvent the latestOfflineEvent to set
	 */
	public void setLatestOfflineEvent(OfflineEventDataUnit latestOfflineEvent) {
		this.latestOfflineEvent = latestOfflineEvent;
	}

	/**
	 * Show a list of offline events. 
	 * Later on will add more functionality to it. 
	 * @param frame
	 */
	public void showOfflineEvents(Frame frame) {
		EventListDialog.showDialog(frame, clickControl);
	}

	/**
	 * Scrolls the display to a specific event. 
	 * @param event event to scroll to
	 */
	public void gotoEvent(OfflineEventDataUnit event) {
		tabPanelControl.clickDisplayManager.gotoEvent(event);
	}

	/**
	 * Delete an offline event. Un-assign all clicks and 
	 * delete from datablock, ready for deletion from database. 
	 * @param event event to delete. 
	 */
	public void deleteEvent(OfflineEventDataUnit event) {
		if (event == null) {
			return;
		}
		/**
		 * Need to tell all the sub detections so that they get deleted
		 * when the clicks are saved. 
		 */
		int n = event.getSubDetectionsCount();
		for (int i = 0; i < n; i++) {
			event.getSubDetection(i).removeSuperDetection(event);
		}
		clickDetector.getOfflineEventDataBlock().remove(event);
	}

	@Override
	public boolean canPlayViewerSound() {
		return PlaybackControl.getViewerPlayback().hasPlayDataSource();
	}

	@Override
	public void playViewerSound() {
		clickPanel.clickTabPanelControl.clickDisplayManager.playViewerData();
	}

	public void playClicks() {
		clickPanel.clickTabPanelControl.clickDisplayManager.playClicks();		
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#fillXMLParameters(org.w3c.dom.Document, org.w3c.dom.Element)
	 */
	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("dbThreshold", (new Double(clickParameters.dbThreshold)).toString());
		paramsEl.setAttribute("longFilter", (new Double(clickParameters.longFilter)).toString());
		paramsEl.setAttribute("longFilter2", (new Double(clickParameters.longFilter2)).toString());
		paramsEl.setAttribute("shortFilter", (new Double(clickParameters.shortFilter)).toString());
		paramsEl.setAttribute("maxLength", (new Integer(clickParameters.maxLength)).toString());
		paramsEl.setAttribute("minSep", (new Integer(clickParameters.minSep)).toString());
		paramsEl.setAttribute("preSample", (new Integer(clickParameters.preSample)).toString());
		paramsEl.setAttribute("postSample", (new Integer(clickParameters.postSample)).toString());
		Element filterEl;
		if (clickParameters.preFilter != null) {
			filterEl = doc.createElement("PreFilter");
			clickParameters.preFilter.fillXMLParameters(doc, filterEl);
			paramsEl.appendChild(filterEl);
		}
		if (clickParameters.triggerFilter != null) {
			filterEl = doc.createElement("TriggerFilter");
			clickParameters.triggerFilter.fillXMLParameters(doc, filterEl);
			paramsEl.appendChild(filterEl);
		}
		
		Element classEl;
		if (clickIdentifier != null) {
			classEl = doc.createElement("Classification");
			classEl.setAttribute("classifyOnline", (new Boolean(clickParameters.classifyOnline)).toString());
			classEl.setAttribute("discardUnclassifiedClicks", (new Boolean(clickParameters.discardUnclassifiedClicks)).toString());
			if (clickIdentifier.fillXMLParamaeters(doc, classEl)) {
				paramsEl.appendChild(classEl);
			}
		}
		/*
		 * Will also need to add types classifier at some point ....
		 */
		return true;
	}

	/**
	 * @return the targetMotionLocaliser
	 */
	public TargetMotionLocaliser<OfflineEventDataUnit> getTargetMotionLocaliser() {
		return targetMotionLocaliser;
	}
	

	/**
	 * @return the echoDetectionSystem
	 */
	public EchoDetectionSystem getEchoDetectionSystem() {
		return echoDetectionSystem;
	}

	/**
	 * @param echoDetectionSystem the echoDetectionSystem to set
	 */
	public void setEchoDetectionSystem(EchoDetectionSystem echoDetectionSystem) {
		this.echoDetectionSystem = echoDetectionSystem;
	}

	public void displayActivated(ClickDisplay clickDisplay) {
		if (offlineToolbar != null) {
			offlineToolbar.displayActivated(clickDisplay);
		}
	}
	
	public ClickDisplayManager getDisplayManager(){
		return tabPanelControl.clickDisplayManager;
	}
	



	
	/**
	 * Solve problems with all click data blocks being called the same thing
	 * if multiple click detectors are in the system. 
	 */
	private void sortDataBlockPrefix() {
		ArrayList<PamControlledUnit> unitList = PamController.getInstance().findControlledUnits(getUnitType());
		boolean haveOthers = (unitList != null && unitList.size() > 0);
		if (haveOthers) {
			dataBlockPrefix = getUnitName() + "_";
		}
	}
	/**
	 * Solve problems with all click data blocks being called the same thing
	 * if multiple click detectors are in the system. 
	 * @return a name, defaulting to "" if it's the first created click detector. 
	 */
	public String getDataBlockPrefix() {
		return dataBlockPrefix;
	}

	@Override
	public String getModuleSummary() {
		// TODO Auto-generated method stub
		return super.getModuleSummary();
	}

	@Override
	public Object getShortUnitType() {
		return "CD";
	}


	/**
	 * Get the counting system for the click alarm. 
	 * @return
	 */
	public AlarmCounter getAlarmCounter() {
		return alarmCounter;
	}


}
