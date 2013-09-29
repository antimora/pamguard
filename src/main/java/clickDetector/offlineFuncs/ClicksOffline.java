package clickDetector.offlineFuncs;

import generalDatabase.PamTableItem;
import generalDatabase.dataExport.DataExportDialog;
import generalDatabase.dataExport.IntValueParams;
import generalDatabase.dataExport.LookupFilter;
import generalDatabase.dataExport.TimeValueParams;
import generalDatabase.dataExport.ValueFilter;
import generalDatabase.lookupTables.LookUpTables;
import generalDatabase.lookupTables.LookupList;

import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JMenuItem;
import offlineProcessing.OLProcessDialog;
import offlineProcessing.OfflineTaskGroup;

import binaryFileStorage.BinaryOfflineDataMap;
import binaryFileStorage.BinaryStore;

import clickDetector.ClickBTDisplay;
import clickDetector.ClickBinaryDataSource;
import clickDetector.ClickControl;
import clickDetector.ClickDataBlock;
import clickDetector.ClickDetection;
import PamController.PamController;
import PamView.CtrlKeyManager;
import PamView.PamColors;
import PamView.PamSymbol;
import PamView.zoomer.ZoomShape;

/**
 * Functions for handling offline data display
 * and processing. 
 * <p>
 * Basically, this is an add on the ClickController, but
 * split off into a separate class since the main
 * ClickController has got a bit over cumbersome. 
 *  
 * @author Doug Gillespie
 *
 */
public class ClicksOffline {

	private ClickControl clickControl;

	private OfflineParameters offlineParameters = new OfflineParameters();

	private OLProcessDialog clickOfflineDialog;

	private OfflineTaskGroup offlineTaskGroup;

	/**
	 * Constructor, called from ClickControl. 
	 * @param clickControl
	 */
	public ClicksOffline(ClickControl clickControl) {
		super();
		this.clickControl = clickControl;
		//		setOfflineStore(new SingleRCFileOffline(clickControl, this));
	}

	protected void runClickClassification() {

	}

	/**
	 * Called when the click store has closed
	 * Will need to delete all data from the module. 
	 * will be called from the offlineStore
	 */
	protected void storageClosed() {

	}

	/** 
	 * Called from when data have changed (eg from re doing click id). 
	 * Needs to notify the display and maybe some other classes. 
	 */
	protected void offlineDataChanged() {
		clickControl.offlineDataChanged();
	}

	/**
	 * Add offline functions to the top of the main Detector menu
	 * when operating in viewer mode. 
	 * @param menu menu to add items to 
	 * @return number of items added. 
	 */
	public int addDetectorMenuItems(Frame owner, Container menu) {
		JMenuItem menuItem;
		menuItem = new JMenuItem("Show Events ...");
		menuItem.addActionListener(new ShowEvents(owner));
		menu.add(menuItem);
		menuItem = new JMenuItem("Reanalyse click types ...");
		menuItem.addActionListener(new ReanalyseClicks());
		menu.add(menuItem);

		return 2;
	}

	/**
	 * Add menu items associated with right mouse actions on bearing time
	 * display 
	 * @param menu menu to add items to
	 * @param hasZoom whether or not the display has a zoomed area. 
	 * @param isOnClick whether or not the mouse is on a click.
	 * @return number of items added to the menu
	 */
	public int addBTMenuItems(Container menu, ClickBTDisplay btDisplay, boolean hasZoom, ClickDetection clickedClick) {
		JMenuItem menuItem;
		OfflineEventDataUnit autoEvent = null;
		OfflineEventDataUnit anEvent;
		Color col;
		OfflineEventDataUnit[] markedEvents = findMarkedEvents(btDisplay);
		int nMenuItems = 0;
		if (markedEvents == null) {
			autoEvent = clickControl.getLatestOfflineEvent();
		}
		else if (markedEvents.length == 1) {
			autoEvent = markedEvents[0];
		}
		PamSymbol eventSymbol = null;
		int eventNumber = 0;
		if (autoEvent != null) {
			eventNumber = autoEvent.getDatabaseIndex();
			col = PamColors.getInstance().getWhaleColor(autoEvent.getColourIndex());
			eventSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, true, col, col);
		}
		ArrayList<ClickDetection> markedClicks = btDisplay.getMarkedClicks();
//		ActionListener al;
//		CtrlKeyManager cm = btDisplay.getCtrlKeyManager();
		if (clickedClick != null) {
			if (autoEvent != null) {
				menuItem = new JMenuItem(String.format("Add click to event %d", 
						eventNumber), eventSymbol);
				menuItem.addActionListener(new QuickAddClicks(autoEvent, clickedClick,btDisplay));
//				cm.addCtrlKeyListener('a', al);
				menu.add(menuItem);
				nMenuItems++;
			}
			menuItem  = new JMenuItem("Label click ...");
			menuItem.addActionListener(new LabelClick(btDisplay, clickedClick));
			menu.add(menuItem);
			nMenuItems++;
			anEvent = (OfflineEventDataUnit) clickedClick.getSuperDetection(OfflineEventDataUnit.class);
			if (anEvent != null) {
				menuItem = new JMenuItem(String.format("Remove click from event %d", anEvent.getEventNumber()));
				menuItem.addActionListener(new UnlabelClick(btDisplay, clickedClick));
				menu.add(menuItem);
				nMenuItems++;
			}
		}
		else if (markedClicks != null && markedClicks.size() > 0){
			if (autoEvent != null) {
				menuItem = new JMenuItem(String.format("Add %d clicks to event %d (Ctrl+A)", 
						markedClicks.size(), eventNumber), eventSymbol);
				menuItem.addActionListener(new QuickAddClicks(autoEvent, markedClicks,btDisplay));
				menu.add(menuItem);
				nMenuItems++;
			}
			menuItem  = new JMenuItem("Label clicks (Ctrl+L) ...");
			menuItem.addActionListener(new LabelClicks(btDisplay));
			menu.add(menuItem);
			nMenuItems++;
			menuItem = new JMenuItem("Create new event (Ctrl+N) ...");
			menuItem.addActionListener(new NewEvent(btDisplay));
			menu.add(menuItem);
			nMenuItems++;
			if (markedEvents != null && markedEvents.length > 0) {
				if (markedEvents.length > 1) {
					menuItem = new JMenuItem("Remove all marked clicks from multiple events");
					menuItem.addActionListener(new UnlabelMarkedClicks(btDisplay, 
							-1));
					menu.add(menuItem);
					nMenuItems++;
				}
				for (int i = 0; i < markedEvents.length; i++) {
					anEvent = markedEvents[i];
					col = PamColors.getInstance().getWhaleColor(anEvent.getColourIndex());
					eventSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 12, 12, true, col, col);
					menuItem = new JMenuItem(String.format("Remove marked clicks from event %d", 
							anEvent.getEventNumber()), eventSymbol);
					menuItem.addActionListener(new UnlabelMarkedClicks(btDisplay, 
							anEvent.getEventNumber()));
					menu.add(menuItem);
					nMenuItems++;
				}
			}
			
		}
		return nMenuItems;
	}

	/**
	 * Automatically work out if there is an obvious event to add clicks to.
	 * <p>this will either be the last event anything was added to, or 
	 * a unique event already used with the clicks in the marked list. 
	 * @param btDisplay Bearing time display
	 * @return existing event, or null. 
	 */
	private OfflineEventDataUnit[] findMarkedEvents(ClickBTDisplay btDisplay) {
		OfflineEventDataUnit[] existingEvents = null;
		int nEvents = 0;
		OfflineEventDataUnit anEvent;
		ClickDetection aClick;
		boolean haveEvent;
		ArrayList<ClickDetection> markedClicks = btDisplay.getMarkedClicks();
		if (markedClicks != null) {
			for (int i = 0; i < markedClicks.size(); i++) {
				aClick = markedClicks.get(i);
				anEvent = (OfflineEventDataUnit) aClick.getSuperDetection(OfflineEventDataUnit.class);
				if (anEvent == null) {
					continue; // nothing doing
				}
				if (existingEvents == null) {
					existingEvents = new OfflineEventDataUnit[1];
					existingEvents[0] = anEvent;
					nEvents = 1;
				}
				else {
					haveEvent = false;
					for (int e = 0; e < nEvents; e++) {
						if (anEvent == existingEvents[e]) {
							haveEvent = true;
							break;
						}
					}
					if (!haveEvent) {
						existingEvents = Arrays.copyOf(existingEvents, nEvents+1);
						existingEvents[nEvents++] = anEvent;
					}
				}
			}
		}
		return existingEvents;
	}

	private class ShowEvents implements ActionListener{

		private Frame frame;

		public ShowEvents(Frame frame) {
			super();
			this.frame = frame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			clickControl.showOfflineEvents(frame);
		}

	}

	private class QuickAddClicks implements ActionListener {
		private ArrayList<ClickDetection> markedClicks;
		private ClickDetection singleClick;
		private OfflineEventDataUnit event;
		private ClickBTDisplay btDisplay;
		
		public QuickAddClicks(OfflineEventDataUnit event, ArrayList<ClickDetection> markedClicks, ClickBTDisplay btDisplay) {
			super();
			this.event = event;
			this.markedClicks = markedClicks;
			this.btDisplay=btDisplay;
		}
		public QuickAddClicks(OfflineEventDataUnit event, ClickDetection singleClick,ClickBTDisplay btDisplay) {
			super();
			this.event = event;
			this.singleClick = singleClick;
			this.btDisplay=btDisplay;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (event == null) {
				return;
			}
			if (singleClick != null) {
				event.addSubDetection(singleClick);
				btDisplay.repaintTotal();

			}
			else if (markedClicks != null) {
				event.addClicks(markedClicks);
				btDisplay.repaintTotal();

			}
			
			clickControl.getClickDetector().getOfflineEventDataBlock().
			updatePamData(event, System.currentTimeMillis());
			clickControl.setLatestOfflineEvent(event);
		}

	}
	private class LabelClick implements ActionListener {
		private ClickBTDisplay btDisplay;
		private ClickDetection singleClick;
		/**
		 * @param btDisplay
		 */
		public LabelClick(ClickBTDisplay btDisplay, ClickDetection singleClick) {
			super();
			
			this.btDisplay = btDisplay;
			this.singleClick = singleClick;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			labelClick(btDisplay, singleClick);
		}
	}
	private class UnlabelClick implements ActionListener {
		private ClickBTDisplay btDisplay;
		private ClickDetection singleClick;
		/**
		 * @param btDisplay
		 */
		public UnlabelClick(ClickBTDisplay btDisplay, ClickDetection singleClick) {
			super();
			
			this.btDisplay = btDisplay;
			this.singleClick = singleClick;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			unLabelClick(btDisplay, singleClick);
		}
	}
	
	private class LabelClicks implements ActionListener {
		private ClickBTDisplay btDisplay;
		/**
		 * @param btDisplay
		 */
		public LabelClicks(ClickBTDisplay btDisplay) {
			super();
			this.btDisplay = btDisplay;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			labelClicks(btDisplay);
		}
	}
	
	private class NewEvent implements ActionListener {
		private ClickBTDisplay btDisplay;

		/**
		 * @param btDisplay
		 */
		public NewEvent(ClickBTDisplay btDisplay) {
			this.btDisplay = btDisplay;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			newEvent(btDisplay);
		}
	}
	
	private class UnlabelMarkedClicks implements ActionListener {
		private int eventNumber;
		private ClickBTDisplay btDisplay;

		/**
		 * @param eventNumber
		 */
		public UnlabelMarkedClicks(ClickBTDisplay btDisplay, int eventNumber) {
			super();
			this.btDisplay = btDisplay;
			this.eventNumber = eventNumber;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			unLabelClicks(btDisplay, eventNumber);
		}
		
	}

	private class ReanalyseClicks implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			reAnalyseClicks();
		}
	}
	
	private class ExportEventData implements ActionListener {
		
		private Frame frame;
		
		/**
		 * @param parentFrame
		 */
		public ExportEventData(Frame parentFrame) {
			super();
			this.frame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			exportEventData(frame);
		}
	}
	private class CheckEventDatabase implements ActionListener {
		
		private Frame frame;
		
		/**
		 * @param parentFrame
		 */
		public CheckEventDatabase(Frame parentFrame) {
			super();
			this.frame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			checkEventDatabase(frame);
		}
	}

	/**
	 * Create a menu item for exporting click event data. 
	 * @param parentFrame parent frame (for any created dialog)
	 * @return menu item.
	 */
	public JMenuItem getExportMenuItem(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem("Export click event data ...");
		menuItem.addActionListener(new ExportEventData(parentFrame));
		return menuItem;
	}

	public void checkEventDatabase(Frame frame) {
		DatabaseCheckDialog.showDialog(frame, clickControl);
		
	}

	public JMenuItem getDatabaseCheckItem(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem("Check Offline Event Database ...");
		menuItem.addActionListener(new CheckEventDatabase(parentFrame));
		return menuItem;
	}
	
	/**
	 * Go through clicks and do things like re classify to species, look for trains, etc. 
	 * This may eventually need to be done within each offlineStore if it's not possible
	 * to find a common way of scrolling through all clicks. 
	 */
	public void reAnalyseClicks() {
		if (clickOfflineDialog == null) {
			clickOfflineDialog = new OLProcessDialog(clickControl.getPamView().getGuiFrame(), 
					getOfflineTaskGroup(), "Click Reprocessing");
		}
		clickOfflineDialog.setVisible(true);
	}
	
	public void exportEventData(Frame frame) {
		OfflineEventLogging offlineEventLogging = clickControl.getClickDetector().getOfflineEventLogging();
		DataExportDialog exportDialog = new DataExportDialog(frame, offlineEventLogging.getTableDefinition(), 
				"Export Click Events");
		exportDialog.excludeColumn("UTCMilliseconds");
		exportDialog.excludeColumn("colour");
		
		PamTableItem eventTableItem = offlineEventLogging.getTableDefinition().findTableItem("eventType");
		LookupList lutList = LookUpTables.getLookUpTables().getLookupList("OfflineRCEvents");
		exportDialog.addDataFilter(new LookupFilter(exportDialog, lutList, eventTableItem));
		
		PamTableItem tableItem;
		tableItem = offlineEventLogging.getTableDefinition().findTableItem("UTC");
		exportDialog.addDataFilter(new ValueFilter<TimeValueParams>(exportDialog, new TimeValueParams(), tableItem));

		tableItem = offlineEventLogging.getTableDefinition().findTableItem("nClicks");
		exportDialog.addDataFilter(new ValueFilter<IntValueParams>(exportDialog, new IntValueParams(), tableItem));
		
		
		
		exportDialog.showDialog();
	}
	
	/**
	 * Get / Create an offline task group for click re-processing. 
	 * @return offline task group. Create if necessary
	 */
	private OfflineTaskGroup getOfflineTaskGroup() {
		offlineTaskGroup = new OfflineTaskGroup(clickControl, "Click Reprocessing");
		offlineTaskGroup.addTask(new ClickReClassifyTask(clickControl));
		offlineTaskGroup.addTask(new EchoDetectionTask(clickControl));
		offlineTaskGroup.addTask(new ClickDelayTask(clickControl));
		offlineTaskGroup.addTask(new ClickBearingTask(clickControl));
		return offlineTaskGroup;
	}

	public void labelClicks(ClickBTDisplay btDisplay) {
		Window win = clickControl.getPamView().getGuiFrame();
		LabelClicksDialog.showDialog(win, clickControl, btDisplay, null);
	}
	
	public void newEvent(ClickBTDisplay btDisplay) {
		Window win = clickControl.getPamView().getGuiFrame();
		OfflineEventDataBlock offlineEventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();
		ArrayList<ClickDetection> markedClicks = btDisplay.getMarkedClicks();
		if (markedClicks == null) {
			return;
		}
		OfflineEventDataUnit newUnit = new OfflineEventDataUnit(null, 0, null);
		newUnit = OfflineEventDialog.showDialog(win, clickControl, newUnit);
		if (newUnit != null) {
			newUnit.addClicks(markedClicks);
			offlineEventDataBlock.addPamData(newUnit);
			clickControl.setLatestOfflineEvent(newUnit);
		}
	}
	
	public void unLabelClicks(ClickBTDisplay btDisplay, int eventNumber) {
		ArrayList<ClickDetection> markedClicks = btDisplay.getMarkedClicks();
		if (markedClicks == null) {
			return;
		}
		OfflineEventDataBlock eventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();
		ClickDataBlock clickDataBlock = clickControl.getClickDataBlock();
		int n = markedClicks.size();
		ClickDetection aClick;
		OfflineEventDataUnit clickEvent;
		long now = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			aClick = markedClicks.get(i);
			clickEvent = (OfflineEventDataUnit) aClick.getSuperDetection(OfflineEventDataUnit.class);
			if (clickEvent == null) {
				continue;
			}
			if (eventNumber < 0 || clickEvent.getEventNumber() == eventNumber) {
				clickEvent.removeSubDetection(aClick);
				eventDataBlock.updatePamData(clickEvent, now);
				clickDataBlock.updatePamData(aClick, now);
			}
		}
		btDisplay.repaintTotal();
	}

	public void labelClick(ClickBTDisplay btDisplay, ClickDetection singleClick) {
		Window win = clickControl.getPamView().getGuiFrame();
		LabelClicksDialog.showDialog(win, clickControl, btDisplay, singleClick);
	}
	
	
	public void unLabelClick(ClickBTDisplay btDisplay, ClickDetection singleClick) {
		OfflineEventDataUnit anEvent = (OfflineEventDataUnit) singleClick.getSuperDetection(OfflineEventDataUnit.class);
		if (anEvent == null) {
			return;
		}
		anEvent.removeSubDetection(singleClick);
		anEvent.updateDataUnit(System.currentTimeMillis());
		btDisplay.repaintTotal();
	}

//	private void processClick(ClickDetection click) {
//		ClickIdInformation idInfo = clickControl.getClickIdentifier().identify(click);
//		if (idInfo.clickType != click.getClickType()) {
//			click.setClickType((byte) idInfo.clickType);
//			click.getDataUnitFileInformation().setNeedsUpdate(true);
//			//			click.setUpdated(true);
//		}
//	}

	public ClickControl getClickControl() {
		return clickControl;
	}

	/**
	 * @param offlineParameters the offlineParameters to set
	 */
	public void setOfflineParameters(OfflineParameters offlineParameters) {
		this.offlineParameters = offlineParameters;
	}

	/**
	 * @return the offlineParameters
	 */
	public OfflineParameters getOfflineParameters() {
		return offlineParameters;
	}

	public ClickBinaryDataSource findBinaryDataSource() {
		return (ClickBinaryDataSource) clickControl.getClickDetector().
		getClickDataBlock().getBinaryDataSource();
	}

	private BinaryStore findBinaryStore() {
		return (BinaryStore) PamController.getInstance().findControlledUnit(BinaryStore.unitType);
	}

	public BinaryOfflineDataMap findOfflineDataMap() {
		BinaryStore bs = findBinaryStore();
		return (BinaryOfflineDataMap) clickControl.getClickDetector().getClickDataBlock().getOfflineDataMap(bs);
	}

	public boolean saveClicks() {
		BinaryStore bs = findBinaryStore();
		if (bs == null) {
			return true;
		}
		return bs.saveData(clickControl.getClickDataBlock());
	}

	public void newMarkedClickList(ZoomShape zoomShape, ClickBTDisplay btDisplay) {
		/*
		 *  a new zoom mark has been created, so set up the shortcuts
		 *  for event creation.  
		 */
		CtrlKeyManager cm = btDisplay.getCtrlKeyManager();
		if (zoomShape == null) {
			cm.clearAll();
			return;
		}
		
		OfflineEventDataUnit autoEvent = null;
		OfflineEventDataUnit anEvent;
		Color col;
		OfflineEventDataUnit[] markedEvents = findMarkedEvents(btDisplay);
		int nMenuItems = 0;
		if (markedEvents == null) {
			autoEvent = clickControl.getLatestOfflineEvent();
		}
		else if (markedEvents.length == 1) {
			autoEvent = markedEvents[0];
		}
		
		ActionListener al;
		al = new LabelClicks(btDisplay);
		cm.addCtrlKeyListener('L', al);
		
		cm.addCtrlKeyListener('N', new NewEvent(btDisplay));

		ArrayList<ClickDetection> markedClicks = btDisplay.getMarkedClicks();
		if (markedClicks != null) {
			al = new QuickAddClicks(autoEvent, markedClicks,btDisplay);
			cm.addCtrlKeyListener('A', al);
		}
	}

}