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
package PamView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import offlineProcessing.OfflineTask;
import offlineProcessing.OfflineTaskGroup;

import performanceTests.PerformanceDialog;
import tipOfTheDay.TipOfTheDayManager;
import whistleClassifier.offline.ReclassifyTask;

import Array.ArrayManager;
//import Logging.LogDataObserver;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamguardVersionInfo;
import PamModel.PamModelInterface;
import PamModel.PamModuleInfo;
import PamUtils.Splash;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
/**
 * @author Doug Gillespie
 * 
 * <p>
 * Simple PamGui implementing a tab control.
 * 
 */
public class PamGui extends PamView implements WindowListener, PamSettings {

	boolean gpsSimActive = true;
	private JPanel mainPanel;
	private PamTabbedPane mainTab;
	private HidingSidePanel sidePanel;
//	private JScrollPane scrollingSidePanel;
	private boolean initializationComplete = false;
    private boolean outputToScreen = true;  // output is currently directed to console screen - Michael Oswald 9/18/10
    private JMenuItem redirectOutputItem;  // menu item - define here so we can change text in actionlistener - Michael Oswald 9/19/10
    private PrintStream origStream = System.out;  // keep track of default output - Michael Oswald 9/18/10
    private String outputFileString = "PamguardLog.txt";       // filename used when outputting to file - Michael Oswald 9/19/10
	protected GuiParameters guiParameters = new GuiParameters();
	private TopToolBar topToolbar;

	private MenuItemEnabler startMenuEnabler, stopMenuEnabler, addModuleEnabler, removeModuleEnabler,
	orderModulesEnabler;

	public PamGui(PamControllerInterface pamControllerInterface, 
			PamModelInterface pamModelInterface, int frameNumber)
	{
		super(pamControllerInterface, pamModelInterface, frameNumber);

		startMenuEnabler = new MenuItemEnabler();
		stopMenuEnabler = new MenuItemEnabler();
		stopMenuEnabler.enableItems(false);
		addModuleEnabler = new MenuItemEnabler();
		removeModuleEnabler = new MenuItemEnabler();
		orderModulesEnabler = new MenuItemEnabler();

		frame = new JFrame(getModeName());

		initializationComplete = PamController.getInstance().isInitializationComplete();

		//		frame.setJMenuBar(new JMenuItem("Configuring PAMGUARD please be patient ..."));
		if (getFrameNumber() == 0) {
			frame.setJMenuBar(getDummyMenuBar());
		}

		frame.setSize(800,500);

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		String iconLoc;
		switch (PamController.getInstance().getRunMode()){
			case(PamController.RUN_NETWORKRECEIVER):iconLoc="Resources/pamguardIconNR.png";break;
			case(PamController.RUN_PAMVIEW):iconLoc="Resources/pamguardIconV.png";break;
			case(PamController.RUN_MIXEDMODE):iconLoc="Resources/pamguardIconM.png";break;
			default:iconLoc="Resources/pamguardIcon.png";
		}
		
		frame.setIconImage(new ImageIcon(ClassLoader
				.getSystemResource(iconLoc)).getImage());

		frame.setExtendedState(Frame.MAXIMIZED_BOTH);

		frame.addWindowListener(this);

		mainPanel = new PamBorderPanel(new BorderLayout());
		mainPanel.setOpaque(true);
		mainPanel.addComponentListener(new GUIComponentListener());

		JPanel centralPanel = new PamBorderPanel(new BorderLayout());
		centralPanel.setOpaque(true);

		sidePanel = new HidingSidePanel(this);

		mainTab = new PamTabbedPane(pamControllerInterface, this);
		//		mainTab.setForeground(Color.BLUE);

		centralPanel.add(BorderLayout.CENTER, mainTab);
//		JPanel outerSidePanel = new PamBorderPanel();
//		outerSidePanel.setLayout(new BorderLayout());
//		outerSidePanel.add(BorderLayout.NORTH, sidePanel.getSidePanel());
//		scrollingSidePanel = new PamScrollPane(outerSidePanel);
//		scrollingSidePanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//		scrollingSidePanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		centralPanel.add(BorderLayout.WEST, sidePanel.getSidePanel());

		mainPanel.add(BorderLayout.CENTER, centralPanel);
		if (getFrameNumber() == 0) {
			mainPanel.add(BorderLayout.SOUTH, PamStatusBar.getStatusBar().getToolBar());
		}
		mainPanel.add(BorderLayout.NORTH, topToolbar = new TopToolBar(this));

		mainTab.addChangeListener(new tabListener());

		frame.setContentPane(mainPanel);


		PamSettingManager.getInstance().registerSettings(this);

		if (guiParameters.bounds != null) {
			/* 
			 * now need to check that the frame is visible on the
			 * current screen - a pain when psf files are sent between
			 * users, or when I work on two screens at work and then one
			 * at home !
			 */
			Rectangle screenRect = ScreenSize.getScreenBounds(20000);
			//			Rectangle intercept = screenRect.intersection(frame.getBounds());
			if (screenRect == null) {
				System.out.println("Unable to get screen dimensions from system");
			}
			else {
				while (guiParameters.bounds.x + guiParameters.bounds.width < screenRect.x + 200) {
					guiParameters.bounds.x += screenRect.width;
				}
				while (guiParameters.bounds.x > screenRect.x+screenRect.width) {
					guiParameters.bounds.x -= screenRect.width;
				}
				while (guiParameters.bounds.y + guiParameters.bounds.height < screenRect.y + 200) {
					guiParameters.bounds.y += screenRect.height;
				}
				while (guiParameters.bounds.y > screenRect.y+screenRect.height) {
					guiParameters.bounds.y -= screenRect.height;
				}
			}

			frame.setBounds(guiParameters.bounds);

		}

		sidePanel.showPanel(!guiParameters.hideSidePanel);

		frame.setExtendedState(guiParameters.extendedState);

		frame.setVisible(true);
		
		somethingShowing = true;
	}

	private static volatile boolean somethingShowing = false;

	/**
	 * Static flag to say that at least one GUI has opened. 
	 * @return true when one or more GUI frames are visible. 
	 */
	public static boolean isSomethingShowing() {
		return somethingShowing;
	}
	/**
	 * Get the virtual bounds of all graphics screens. In a single
	 * screen environment, this should be the dimensions of the screen.
	 * In a multi-screen environment, it will be a rectangle that 
	 * encompasses many screens.  
	 * @return rectangle encompassing all screens (note may have gaps in 
	 * corners if screens are different sizes).
//	 */
	//	private Rectangle getScreenBounds() {
	//		if (virtualBounds == null) {
	//			virtualBounds = new Rectangle();
	//			GraphicsEnvironment ge = GraphicsEnvironment.
	//			getLocalGraphicsEnvironment();
	//			GraphicsDevice[] gs =
	//				ge.getScreenDevices();
	//			for (int j = 0; j < gs.length; j++) { 
	//				GraphicsDevice gd = gs[j];
	//				GraphicsConfiguration[] gc =
	//					gd.getConfigurations();
	//				for (int i=0; i < gc.length; i++) {
	//					virtualBounds =
	//						virtualBounds.union(gc[i].getBounds());
	//				}
	//			} 
	//		}
	//		return virtualBounds;
	//	}
	/**
	 * Static for getScreenBounds. 
	 */
	private static Rectangle virtualBounds;

	private String getModeName() {
		int runMode = PamController.getInstance().getRunMode();
		switch (runMode) {
		case PamController.RUN_NORMAL:
			return "PAMGUARD";
		case PamController.RUN_PAMVIEW:
			return "PAMGUARD - Viewer";
		case PamController.RUN_MIXEDMODE:
			return "PAMGUARD - Mixed mode Offline Analysis";
		}
		return "PAMGUARD";
	}
	/**
	 * Makes a dummy menu bar with some text in it
	 * which is displayed as PAMGUARD is first starting up
	 * @return dummy Menu.
	 */
	private JMenuBar getDummyMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new JMenu("Configuring PAMGUARD please be patient while modules are loaded ..."));
		return menuBar;
	}

	class GUIComponentListener implements ComponentListener {

		public void componentHidden(ComponentEvent e) {
			// TODO Auto-generated method stub

		}

		public void componentMoved(ComponentEvent e) {
			// TODO Auto-generated method stub

		}

		public void componentResized(ComponentEvent e) {

			if (getFrameNumber() == 0) {
				PamStatusBar.getStatusBar().resize();
			}
			mainPanel.invalidate();

		}

		public void componentShown(ComponentEvent e) {
			// TODO Auto-generated method stub

		}

	}

	public void addControlledUnit(PamControlledUnit unit) {
		if (unit.getFrameNumber() != this.getFrameNumber()) {
			return;
		}
		unit.setPamView(this);
		if (unit.getTabPanel() != null){
			mainTab.addTab(unit.getUnitName(), null, unit.getTabPanel().getPanel(), getTabTipText());
			if (mainTab.getTabCount() == 1) mainTab.setSelectedIndex(0);
		}
		if (unit.getSidePanel() != null) sidePanel.add(unit.getSidePanel().getPanel());
		int tabHeight = 0;
		if (mainTab.getComponentCount() > 0) {
			if (mainTab.isShowing() && mainTab.getComponent(0).isShowing()) {
				tabHeight = mainTab.getComponent(0).getLocationOnScreen().y - mainTab.getLocationOnScreen().y;
			}
		}
		if (sidePanel != null) {
//			scrollingSidePanel.setBorder(new EmptyBorder(tabHeight, 0, 0, 0));
		}
//		showSidePanel();
		if (initializationComplete) {
			ShowTabSpecificSettings();
		}
	}

	@Override
	public void setTitle(String title) {
		frame.setTitle(title);
	}
	private String getTabTipText() {
		return "Right click for more options";
	}

	public void removeControlledUnit(PamControlledUnit unit) {
		unit.setPamView(null);
		if (unit.getTabPanel() != null) mainTab.remove(unit.getTabPanel().getPanel());
		if (unit.getSidePanel() != null) sidePanel.remove(unit.getSidePanel().getPanel());
//		showSidePanel();
		ShowTabSpecificSettings();
	}

	@Override
	public void renameControlledUnit(PamControlledUnit unit) {
		if (unit.getTabPanel() != null) {
			int tabIndex = mainTab.indexOfComponent(unit.getTabPanel().getPanel());
			if (tabIndex >= 0) {
				mainTab.setTitleAt(tabIndex, unit.getUnitName());
			}
		}
	}

	class tabListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent e) {
			if (initializationComplete) {
				guiParameters.selectedTab = mainTab.getSelectedIndex();
			}
			ShowTabSpecificSettings();

		}	
	}

	public PamControlledUnit findControlledUnit(int tabNo)
	{
		if (mainTab == null) return null;
		if (tabNo < 0 || tabNo >= mainTab.getComponentCount()) return null;
		Component tabComponent = mainTab.getComponentAt(tabNo);
		//System.out.println("Comp: "+tabNo);
		PamControlledUnit unit;
		for (int i = 0; i < pamControllerInterface.getNumControlledUnits(); i++) {
			unit = pamControllerInterface.getControlledUnit(i);
			if (unit.getTabPanel() != null && tabComponent == unit.getTabPanel().getPanel()){ 
				return unit;}
		}

		return null;
	}

	/**
	 * Set up specific settings for the tab - get's called quite a lot, 
	 * including whenever modules are added or removed in order to 
	 * make sure that menus contain the correct options for 
	 * existing modules. 
	 *
	 */
	public void ShowTabSpecificSettings()
	{   
		if (initializationComplete == false) return;
		//System.out.println("Index: "+mainTab.getSelectedIndex());
		//System.out.println("Placement: "+ mainTab.getTabPlacement());

		PamControlledUnit pamUnit = findControlledUnit(mainTab.getSelectedIndex());
		JMenuBar guiMenuBar;
		topToolbar.setActiveControlledUnit(pamUnit);
		if (pamUnit == null) {
			guiMenuBar = makeGuiMenu();
		}
		else {
			guiMenuBar = pamUnit.getTabSpecificMenuBar(frame, makeGuiMenu(), this);
		}
		//		if (pamUnit.getTabPanel() == null) return;
		//System.out.println(pamUnit.GetUnitName() + " tab activated");
		//if (getFrameNumber() == 0) {
		/*
		 * Before setting a new menu bar, clean up the old
		 * one from the menu item enablers. 
		 */
		MenuItemEnabler.removeMenuBar(frame.getJMenuBar());
	
		frame.setJMenuBar(guiMenuBar);
		enableMenus();
		//		}

		//JToolBar toolBar = pamUnit.GetTabPanel().GetToolBar();
		//if (toolBar != null) mainPanel.add(toolBar);
	}


	//	MakeGuiMenu()
	/**
	 * Makes a standard GUI menu for the display which will include add ins
	 * taken from the varous PamControlledUnits. 
	 */

	JMenu fileMenu;
	JMenuBar menuBar;	

	public JMenuBar makeGuiMenu() {

		//		if (getFrameNumber() > 0) {
		//			return null;
		//		}

		int runMode = PamController.getInstance().getRunMode();

		boolean isViewer = (runMode == PamController.RUN_PAMVIEW);

		menuBar = new JMenuBar();

		JMenuItem menuItem;
		JMenu menu;
		
		// =======================
		/* File menu */
		fileMenu = new JMenu("File");
		fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);

		if (isViewer) {
			menuItem = new JMenuItem("Save data");
			menuItem.addActionListener(new MenuDataSave());
			fileMenu.add(menuItem);
			fileMenu.addSeparator();
		}
		/*
		 * Settings load, import and export functions
		 */
		menuItem = new JMenuItem("Save configuration");
		menuItem.addActionListener(new menuSave());
		fileMenu.add(menuItem);
		if (isViewer) {
			menuItem.setToolTipText("Save configuration to current database");
		}
		else {
			menuItem.setToolTipText("Save configuration to psf file");
		}
		if (isViewer) {
			menuItem = new JMenuItem("Export configuration  ...");
			menuItem.setToolTipText("Export configuration to a new psf file");
		}
		else {
			menuItem = new JMenuItem("Save configuration As ...");
			menuItem.setToolTipText("Save configuration to a new psf file");
		}
		menuItem.addActionListener(new menuSaveAs());
		fileMenu.add(menuItem);
		if (!isViewer) {
			if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
				menuItem = new JMenuItem("Export XML Configuration");
				menuItem.addActionListener(new MenuXMLExport());
				fileMenu.add(menuItem);
			}
			menuItem = new JMenuItem("Load configuration ...");
			menuItem.addActionListener(new menuLoadSettings());
			fileMenu.add(menuItem);
		}
		else {
//			if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
//				menuItem = new JMenuItem("Export XML OfflineSettings Configuration");
//				menuItem.setToolTipText("Save configuration to an XML file");
//				menuItem.addActionListener(new MenuXMLOfflineSettingsExport());
//				fileMenu.add(menuItem);
//			}
			
			menuItem = new JMenuItem("Import configuration ...");
			menuItem.addActionListener(new menuImportSettings());
			menuItem.setToolTipText("Import a configuration from a psf file");
			fileMenu.add(menuItem);
		}
		fileMenu.addSeparator();

		//		if (runMode == PamController.RUN_NORMAL || runMode == PamController.RUN_MIXEDMODE) {

		fileMenu.add(menuItem = PamModuleInfo.getModulesMenu(frame));
		addModuleEnabler.addMenuItem(menuItem);
		fileMenu.add(menuItem = PamModuleInfo.getRemoveMenu());
		removeModuleEnabler.addMenuItem(menuItem);

		menuItem = new JMenuItem("Module Ordering ...");
		menuItem.addActionListener(new menuModuleOrder());
		orderModulesEnabler.addMenuItem(menuItem);
		fileMenu.add(menuItem);

		//		}

		menuItem = new JMenuItem("Hydrophone array ...");
		menuItem.addActionListener(new menuArray());
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Show object list ...");
		menuItem.addActionListener(new menuShowObjectList());
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Show data model ...");
		menuItem.addActionListener(new menuShowObjectDiagram());
		fileMenu.add(menuItem);

		fileMenu.addSeparator();

		boolean needSeperator = false;
		
		menuItem = new JMenuItem("Storage Options ...");
		menuItem.addActionListener(new StorageOptions(getGuiFrame()));
		startMenuEnabler.addMenuItem(menuItem);
		fileMenu.add(menuItem);

		for (int i = 0; i < pamControllerInterface.getNumControlledUnits(); i++) {

			menuItem = pamControllerInterface.getControlledUnit(i).createFileMenu(frame);

			if (menuItem != null) {
				needSeperator = true;
				fileMenu.add(menuItem);
			}
		}
		if (needSeperator) {
			fileMenu.addSeparator();
		}


		if (isViewer) {
			menuItem = new JMenuItem("Save and Exit");
		}
		else {
			menuItem = new JMenuItem("Exit");
		}
		menuItem.addActionListener(new menuExit());
		fileMenu.add(menuItem);



		menuBar.add(fileMenu);
		//menuBar.addContainerListener(new tempListener());

		//System.out.println("MENU REBUILD");


		// =======================
		/* Detection menu */
		menu = new JMenu("Detection");
		menu.getPopupMenu().setLightWeightPopupEnabled(false);

		boolean needSeparator = false;

		if (runMode == PamController.RUN_NORMAL || runMode == PamController.RUN_MIXEDMODE) {

			menuItem = new JMenuItem("Start");
			menuItem.addActionListener(new menuPamStart());
			startMenuEnabler.addMenuItem(menuItem);
			menu.add(menuItem);

			menuItem = new JMenuItem("Stop");
			menuItem.addActionListener(new menuPamStop());
			stopMenuEnabler.addMenuItem(menuItem);
			menu.add(menuItem);

			menu.addSeparator();

			menuItem = new JMenuItem("Multi-threading ...");
			menuItem.addActionListener(new MenuMultiThreading());
			startMenuEnabler.addMenuItem(menuItem);
			menu.add(menuItem);
			
			needSeparator = true;

		}		
		//		else if  (runMode == PamController.RUN_PAMVIEW) {
		//
		//			menuItem = new JMenuItem("View Times ...");
		//			menuItem.addActionListener(new menuViewTimes(frame));
		////			startMenuEnabler.addMenuItem(menuItem);
		//			menu.add(menuItem);
		//		}

		// go through the Controllers and see if any have a detection menu to add
		JMenuItem detectorMenu;
		boolean separator = false;
		for (int i = 0; i < pamControllerInterface.getNumControlledUnits(); i++) {
			detectorMenu = pamControllerInterface.getControlledUnit(i).createDetectionMenu(frame);
			if (detectorMenu != null) {
				if (!separator && needSeparator) {
					menu.addSeparator();
					separator = true;
				}
				menu.add(detectorMenu);
			}
		}		
		menuBar.add(menu);		

		// =======================
		// DISPLAY MENU
		JMenu displayMenu = null;
		JMenuItem subMenu = null;
		if (displayMenu == null) {
			displayMenu = new JMenu("Display");
		}

		displayMenu.add(PamColors.getInstance().getMenu());

		JMenuItem symbolItem = PamSymbolManager.getInstance().getMenu(getGuiFrame());
		if (symbolItem != null) {
			displayMenu.add(symbolItem);
		}

		displayMenu.addSeparator();

		for (int i = 0; i < pamControllerInterface.getNumControlledUnits(); i++) {
			subMenu = pamControllerInterface.getControlledUnit(i).createDisplayMenu(frame);
			if (subMenu != null) {
				displayMenu.add(subMenu);
			}
		}		

		if (displayMenu != null) menuBar.add(displayMenu);


		// =======================
		// HELP MENU
		menu = new JMenu("Help");
		menu.getPopupMenu().setLightWeightPopupEnabled(false);

		//		new PamHelpContentViewerUI()
		//menu.add(PamHelp.getInstance().getMenu());
		//		PamHelp.getInstance();
		menuItem = new JMenuItem("About PAMGUARD");
		menuItem.addActionListener(new menuAbout());
		menu.add(menuItem);

		PamHelp.getInstance();
		JMenuItem testHelpMenu = new JMenuItem("Help");
		testHelpMenu.addActionListener(new TestMenuHelpPG() );
		separator = false;
		menu.add(testHelpMenu);


		PamHelp.getInstance();
		JMenuItem tipMenu = new JMenuItem("Tip of the day ...");
		tipMenu.addActionListener(new TipMenu() );
		menu.add(tipMenu);

        /*
         * Add menu item to redirect output to file or console screen
         *
         * Michael Oswald
         * 9/18/10
         */
		menu.addSeparator();
		redirectOutputItem = new JMenuItem("Redirect output to file");
		redirectOutputItem.addActionListener(new RedirectOutput());
		menu.add(redirectOutputItem);

		menu.addSeparator();
		menuItem = new JMenuItem("System performance tests ...");
		menuItem.addActionListener(new PerformanceTests());
		startMenuEnabler.addMenuItem(menuItem);
		menu.add(menuItem);

		menuItem = new JMenuItem("PAMGUARD Web site");
		menuItem.addActionListener(new MenuPamguardURL(PamguardVersionInfo.webAddress));
		menu.add(menuItem);

		menuItem = new JMenuItem("Contact and Support");
		menuItem.addActionListener(new MenuPamguardURL("www.pamguard.org/contact.shtml"));
		menu.add(menuItem);

		menuBar.add(menu);	

		enableMenus();

		return menuBar;
	}


	class StorageOptions implements ActionListener {
		private JFrame parentFrame;
		
		public StorageOptions(JFrame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			PamController.getInstance().storageOptions(parentFrame);
		}
	}
	class MenuXMLExport implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamController.getInstance().exportXMLSettings();
		}
	}
//	class MenuXMLOfflineSettingsExport implements ActionListener {
//		public void actionPerformed(ActionEvent ev){
//			PamSettingManager.getInstance().saveOfflineSettingsToXMLFile();
//		}
//		
//	}
	class MenuDataSave implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamController.getInstance().saveViewerData();
		}
	}
	class menuSave implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamSettingManager.getInstance().saveSettings(frame);
		}
	}
	class menuSaveAs implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamSettingManager.getInstance().saveSettingsAs(frame);			
		}
	}
	class menuSaveAsXML implements ActionListener {
		public void actionPerformed(ActionEvent ev){
//			PamSettingManager.getInstance().saveSettingsAsXML(frame);			
		}
	}
	class menuLoadSettings implements ActionListener {
		public void actionPerformed(ActionEvent ev){
//			PamSettingManager.getInstance().loadSettingsFrom(frame);
			/**
			 * 4/3/13 Changed this to stop it attempting to replace the loaded
			 * configuration since this feature never really worked. 
			 */
			String msg = "To load or create a new configuration you should exit and re-start PAMGuard";
			JOptionPane.showMessageDialog(frame, msg, "New configuration", JOptionPane.INFORMATION_MESSAGE, null);
		}
	}
	/**
	 * Import a configuration from a psf file during viewer op's
	 * @author Doug Gillespie
	 *
	 */
	class menuImportSettings implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamSettingManager.getInstance().importSettings(frame);
		}
	}
	class menuExportSettings implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamSettingManager.getInstance().exportSettings(frame);
		}
	}
	class TestMenuHelpPG implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamHelp.getInstance().displayHelp();
			//DatabaseController.getInstance().
		}
	}

	class TipMenu implements ActionListener {
		public void actionPerformed(ActionEvent ev){	
			TipOfTheDayManager.getInstance().showTip(getGuiFrame(), null);
		}
	}

    /*
     * Redirect output to either file or screen
     *
     * Michael Oswald
     * 9/18/2010
     */
	class RedirectOutput implements ActionListener {
		public void actionPerformed(ActionEvent ev){
            PrintStream newStream = null;
            /*
             * if we're currently outputing to the console screen, redirect to
             * a file
             */
            if (outputToScreen) {
                try {
                    String currentDir = System.getProperty("user.dir");
                    File outputFile = new File(currentDir, outputFileString);
                    newStream = new PrintStream(new FileOutputStream(outputFile,true));
                    System.setOut(newStream);
                    System.setErr(newStream);
                    outputToScreen = false;
                    redirectOutputItem.setText("Redirect output to screen");
                    JOptionPane.showMessageDialog(mainPanel,
                            "All information and error messages will now be output to " + outputFile.getAbsolutePath(),
                            "Redirect Output",
                            JOptionPane.INFORMATION_MESSAGE);
                /*
                 * if there's a problem redirecting the output, reset the
                 * output back to the console screen and print an error message
                 */
                } catch (Exception ex) {
                    System.setOut(origStream);
                    System.setErr(origStream);
                    System.out.println("Error in output redirection");
                }

            /*
             * if we're currently outputting to the file, redirect to the
             * console
             */
            } else {
                System.setOut(origStream);
                System.setErr(origStream);
                outputToScreen = true;
                redirectOutputItem.setText("Redirect output to file");
                JOptionPane.showMessageDialog(mainPanel,
                        "All information and error messages will now be output to console screen",
                        "Redirect Output",
                        JOptionPane.INFORMATION_MESSAGE);
            }
		}
	}

	class PerformanceTests implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PerformanceDialog.showDialog(getGuiFrame());
		}
	}

	//	class menuHelpPG implements ActionListener {
	//		public void actionPerformed(ActionEvent ev){
	//			PamHelp.getInstance().displayHelp();
	//			//DatabaseController.getInstance().
	//		}
	//	}

	class menuModuleOrder implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			pamControllerInterface.orderModules(frame);
		}
	}
	class menuArray implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			ArrayManager.showArrayDialog(getGuiFrame());
		}
	}

	class menuShowObjectList implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamObjectList.ShowObjectList();
		}
	}

	class menuShowObjectDiagram implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamObjectViewer.Show();
		}
	}

	class menuOpenLogFile implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			PamObjectList.ShowObjectList();
		}
	}

	class menuExit implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			/*
			 * Really should stop Pam first !
			 */
			prepareToClose();

			//			frame.dispose();
			//			System.exit(0);
		}
	}

	class menuPamStart implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			pamControllerInterface.pamStart();

		}
	}

	//	class menuViewTimes implements ActionListener {
	//		Frame frame;
	//		public menuViewTimes(Frame frame) {
	//			this.frame = frame;
	//		}
	//		public void actionPerformed(ActionEvent ev){
	//			viewTimes(frame);
	//		}
	//	}

	class menuPamStop implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			pamControllerInterface.pamStop();
			//			enableLoggingMenu();		
		}
	}
	class MenuMultiThreading implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			pamControllerInterface.modelSettings(frame);
			//			enableLoggingMenu();		
		}
	}

	//	private void viewTimes(Frame frame) {
	//		pamControllerInterface.getNewViewTimes(frame);	
	//	}

	/*
	class menuLogStart implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			logDataObserver.setLoggingActive(true);	
			enableLoggingMenu();				
		}
	}

	class menuLogStop implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			logDataObserver.stopLogData();
			enableLoggingMenu();		
		}
	}

	class menuLogOpen implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			logDataObserver.openLogFile();
			enableLoggingMenu();		
		}
	}

	class menuLogClose implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			logDataObserver.closeLogFile();
			enableLoggingMenu();		
		}
	}*/


	/**
	 * Rewrote the menu enablers for the Logging menu (and made it more general
	 * to extend to other menus shoule they ever need to be anabled. 
	 * 
	 * This arises now that we have multiple main menus on the frame depending on
	 * which tab is being viewed. Since each menu bar has references to different
	 * menus and menu items, we can no lnger use the ones set in the constuctors for
	 * the menu. Each item is therefore found by name before it's enabled. For now
	 * I'm taking the names out of the reference to the last menu item. 
	 *
	 */


	private void enableMenus()
	{
		int pamStatus = PamController.getInstance().getPamStatus();
		//		enableLoggingMenu();
		startMenuEnabler.enableItems(pamStatus == PamController.PAM_IDLE);
		stopMenuEnabler.enableItems(pamStatus == PamController.PAM_RUNNING);
		addModuleEnabler.enableItems(pamStatus == PamController.PAM_IDLE);
		removeModuleEnabler.enableItems(pamStatus == PamController.PAM_IDLE);
		orderModulesEnabler.enableItems(pamStatus == PamController.PAM_IDLE);
	}
	private void enableLoggingMenu() {
		//		boolean fileOpen = logDataObserver.isLogFileOpened();
		//		boolean loggingActive = logDataObserver.isLoggingActive();
		// no idea what the references are for the loaded menu now so
		// in a right mess and need to search for each item.
		//		JMenuItem item;

		//		logCloseEnabler.enableItems(fileOpen == true && loggingActive == false);
		//
		//		logOpenEnabler.enableItems(fileOpen == false && loggingActive == false);
		//		
		//		logStartEnabler.enableItems(fileOpen == true && loggingActive == false);
		//		
		//		logStopEnabler.enableItems(loggingActive == true);		
	}

	class menuAbout implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			new Splash(30000, PamController.getInstance().getRunMode());
		}
	}

	class menuCoreHelp implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			//System.out.println("PamGui: launch core pamguard help here");
		}
	}

	class MenuPamguardURL implements ActionListener {
		String webAddr;
		public MenuPamguardURL(String webAddr) {
			super();
			this.webAddr = webAddr;
		}
		public void actionPerformed(ActionEvent ev){
			URL url = null;
			String fullAddr = "http://" + webAddr; 
			//			System.out.println(fullAddr);
			openURL(fullAddr);
		}
	}
	public static void openURL(String urlString) { 
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (url == null) {
			return;
		}
		try {
			Desktop.getDesktop().browse(url.toURI());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see PamView.PamViewInterface#PamEnded()
	 */
	public void pamEnded() {

		enableMenus();

	}

	/* (non-Javadoc)
	 * @see PamView.PamViewInterface#PamStarted()
	 */
	public void pamStarted() {

		enableMenus();

	}


	/* (non-Javadoc)
	 * @see PamView.PamViewInterface#ModelChanged()
	 */
	public void modelChanged(int changeType) {
		switch (changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			initializationComplete = true;
			/*
			 * during the startup phase, this may be the tab we were looking
			 * at last time PamGuard ran - so set the tab control
			 * to be looking at this one
			 */
			if (mainTab.getTabCount() > guiParameters.selectedTab) {
				mainTab.setSelectedIndex(guiParameters.selectedTab);
			}
			ShowTabSpecificSettings();
			break;
		case PamControllerInterface.REORDER_CONTROLLEDUNITS:
			changeUnitOrder();
			ShowTabSpecificSettings();
			break;
		case PamControllerInterface.DESTROY_EVERYTHING:
			this.mainPanel.removeAll();
			frame.removeAll();
			frame.setVisible(false);
			break;	
		case PamControllerInterface.CHANGED_DISPLAY_SETTINGS:
			ShowTabSpecificSettings();
			break;
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
			if (initializationComplete) {
				ShowTabSpecificSettings();
			}
			break;
		}
	}

	private void changeUnitOrder() {
		changeTabOrder();
		changeSidePanelOrder();
	}

	private void changeTabOrder() {
		PamControlledUnit unit;
		int tabNumber = 0;
		int currentTab;
		Component currentComponent = mainTab.getSelectedComponent();
		for (int i = 0; i < pamControllerInterface.getNumControlledUnits(); i++) {
			unit = pamControllerInterface.getControlledUnit(i);
			if (unit.getTabPanel() != null) {
				currentTab = mainTab.indexOfComponent(unit.getTabPanel().getPanel());
				if (currentTab >= 0) {
					mainTab.remove(currentTab);
					mainTab.insertTab(unit.getUnitName(), null, unit.getTabPanel().getPanel(), 
							getTabTipText(), tabNumber);
					tabNumber++;
				}
			}
		}
		if (currentComponent != null) {
			mainTab.setSelectedComponent(currentComponent);
		}
	}

	private void changeSidePanelOrder() {
		PamControlledUnit unit;
		sidePanel.removeAll();
		for (int i = 0; i < pamControllerInterface.getNumControlledUnits(); i++) {
			unit = pamControllerInterface.getControlledUnit(i);
			if (unit.getSidePanel() != null) {
				sidePanel.add(unit.getSidePanel().getPanel());
			}
		}
	}

//	private void showSidePanel() {
//		sidePanel.setVisible(sidePanel.getComponentCount() > 0);
//	}

	/**
	 * Implementation of WindowListener
	 */
	@Override
	public void windowActivated(WindowEvent e) {

	}
	@Override
	public void windowClosing(WindowEvent e) {
		/*
		 * Window closing action will now depend on which frame 
		 * this is. If it's the main frame (getFrameNumber() == 0)
		 * then exit if not running, or check to see if user wants to stop
		 * 
		 * If it's any other frame, then move all the tabs back to the 
		 * main frame and close normally. 
		 */
		if (getFrameNumber() == 0) {
			prepareToClose();
		}
		else {
			closeExtraFrame();
		}
	}
	@Override
	public void windowOpened(WindowEvent e) {
		PamColors.getInstance().setColors();
	}
	@Override
	public void windowIconified(WindowEvent e) {

	}
	@Override
	public void windowDeiconified(WindowEvent e) {

	}
	@Override
	public void windowDeactivated(WindowEvent e) {
		//		PamSettingManager.getInstance().SaveSettings();

	}
	public void windowClosed(WindowEvent e) {
		//		PamSettingManager.getInstance().SaveSettings();

	}

	/**
	 * Get the GUi parameters before saving so that these can 
	 * be written to the psf file, even if the Window was previouslu
	 * closed. 
	 */
	protected void getGuiParameters() {
		guiParameters.extendedState = frame.getExtendedState();
		guiParameters.state = frame.getState();
		if (guiParameters.state != Frame.MAXIMIZED_BOTH) {
			guiParameters.size = frame.getSize();
			guiParameters.bounds = frame.getBounds();
		}
	}
	/**
	 * 
	 * this get's called whenever the main window closes - 
	 * ideally, I'd like to stop this happening when the system
	 * is running, but since that's not possible, just make sure
	 * everything has stopped.
	 * 
	 * @return true if ok to close, false otherwise. 
	 */
	private boolean prepareToClose() {

		PamController pamController = PamController.getInstance();

		if (pamController.canClose() == false) {
			return false;
		}

		int pamStatus = pamController.getPamStatus();
		if (pamStatus != PamController.PAM_IDLE) {
			int ans = JOptionPane.showConfirmDialog(frame,  
					"Are you sure you want to stop and exit",
					"PAMGUARD is busy",
					JOptionPane.YES_NO_OPTION);
			if (ans == JOptionPane.NO_OPTION) {
				return false;
			}
		}

		pamControllerInterface.pamStop();
		
//		pamControllerInterface.pamClose();

		pamControllerInterface.getGuiFrameManager().getAllFrameParameters();

		if (pamController.getRunMode() == PamController.RUN_PAMVIEW) {
			pamController.saveViewerData();
		}

		// finally save all settings just before PAMGUARD closes. 
		PamSettingManager.getInstance().saveFinalSettings();
pamControllerInterface.pamClose();
		System.exit(0);

		return true;
	}

	/**
	 * Get's called if an extra frame is closed. 
	 * Moves all tabs back to the main frame then 
	 * closes itself. 
	 */
	private void closeExtraFrame() {

		getGuiParameters();

		pamControllerInterface.getGuiFrameManager().closeExtraFrame(this);
	}

	public void showDialog(String s1, String s2, int dialogType) 
	{ 	// custom title, error icon
		JOptionPane.showMessageDialog(frame,
				"Eggs aren't supposed to be green.",
				"Inane error",
				JOptionPane.ERROR_MESSAGE);


		// JOptionPane.showMessageDialog(frame,
		// s1, s2, dialogType
	}


	@Override
	public void showControlledUnit(PamControlledUnit pamControlledUnit) {
		PamTabPanel pamTabPanel = pamControlledUnit.getTabPanel();
		if (pamTabPanel == null) return;
		mainTab.setSelectedComponent(pamTabPanel.getPanel());		
	}

	@Override
	public String getViewName(){
		return "pamGui";
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#getSettingsReference()
	 */
	public Serializable getSettingsReference() {
		return guiParameters;
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#getSettingsVersion()
	 */
	public long getSettingsVersion() {
		return GuiParameters.serialVersionUID;
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#getUnitName()
	 */
	public String getUnitName() {
		if (getFrameNumber() == 0) {
			return "PamGUI";
		}
		else {
			return String.format("PamGUI Frame %d", getFrameNumber());
		}
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#getUnitType()
	 */
	public String getUnitType() {
		return "PamGUI";
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#restoreSettings(PamController.PamControlledUnitSettings)
	 */
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		guiParameters = ((GuiParameters) pamControlledUnitSettings.getSettings()).clone();

		return true;
	}
	public PamTabbedPane getMainTab() {
		return mainTab;
	}

	BusyLayeredPane busyPane;
	@Override
	public void enableGUIControl(boolean enable) {
		//		if (enable) {
		//			frame.getLayeredPane().remove(busyPane);
		////			busyPane.di
		//		}
		//		else {
		//			busyPane = new BusyLayeredPane(frame.getLayeredPane());
		//			frame.add(busyPane);
		//		}
	}

}