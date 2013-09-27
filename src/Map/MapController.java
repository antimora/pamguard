package Map;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.Timer;

import Array.ArrayManager;
import Array.PamArray;
import GPS.GPSControl;
import GPS.GpsData;
import GPS.GpsDataUnit;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.LatLong;
import PamUtils.MapContourValues;
import PamView.PamGui;
import PamguardMVC.PamDataUnit;


public class MapController extends PamControlledUnit implements PamSettings {

	MapParameters mapParameters = new MapParameters();
	GetMapFile getMapFile = new GetMapFile();
	ArrayList<MapContourValues> contourPoints = new ArrayList<MapContourValues>();
	boolean mapContoursAvailable = false;
	boolean mapContoursDeliveredToMap = false;
	public static final String unitType = "Map";
	MapTabPanelControl mapTabPanelControl;
	MapProcess mapProcess;

	Timer timer;

	MapFileManager mapFileManager = new GebcoMapFile();

	MapDetectionsManager mapDetectionsManager;

	private static LatLong mouseClickLatLong;

	static public final int MOUSE_PAN = 0;
	static public final int MOUSE_MEASURE = 1;

	public MapController(String name) {

		super(unitType, name);
		PamSettingManager.getInstance().registerSettings(this);
		mapDetectionsManager = new MapDetectionsManager(this);
		addPamProcess(mapProcess = new MapProcess(this));
		setTabPanel(mapTabPanelControl = new MapTabPanelControl(this));
		timer = new Timer(1000, new TimerListener());
		timer.start();
	}
	class TimerListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
			if (currentArray == null){
				return;
			}
			if (currentArray.getArrayType() == PamArray.ARRAY_TYPE_STATIC) {
				// static array
				PamDataUnit pamDataUnit = currentArray.getFixedPointReferenceBlock().getLastUnit();
				if (pamDataUnit!= null) {
					GpsData gpsData = ((GpsDataUnit) pamDataUnit).getGpsData();
					GpsTextDisplay gpsTextDisplay = mapTabPanelControl.getSimpleMap().gpsTextPanel;
					SimpleMap simpleMap = mapTabPanelControl.getSimpleMap();
					MapPanel mapPanel = simpleMap.mapPanel;
					gpsTextDisplay.updateGpsTextAreaWithStaticData(gpsData);
					gpsTextDisplay.newShipGps();
					gpsTextDisplay.setPixelsPerMetre(mapPanel.getPixelsPerMetre());
					//					gpsTextDisplay.setShipPosition(mapPanel.getRectProj().
					//							getCoord3d(gpsData.getLatitude(), gpsData.getLongitude(), 0));
					simpleMap.repaint();
				}
			}
			else {
				// towed array. 
				mapTabPanelControl.getSimpleMap().timerActions();
			}

		}

	}
	@Override
	public JMenu createDisplayMenu(Frame parentFrame) {
		JMenuItem menuItem;
		JMenu menu = new JMenu(getUnitName());

		menuItem = new JMenuItem("Map options ...");
		menuItem.addActionListener(new menuMapOptions(parentFrame));
		menu.add(menuItem);

		//		menuItem = new JMenuItem("Map file ...");
		//		menuItem.addActionListener(new menuMapFile(parentFrame));
		//		menu.add(menuItem);

		return menu;
	}

	/*@Override
	public JMenuItem createHelpMenu(Frame parentFrame) {
		//System.out.println("createHelpMenu called by:" + caller);
		JMenuItem menuItem;
		JMenu menu = new JMenu(getUnitName());
		menuItem = new JMenuItem("Map");
		menuItem.addActionListener(new startHelp());
		menu.add(menuItem);
		return menuItem;
	}*/


	/*class startHelp implements ActionListener {
	 *//**
	 * 
	 *//*
		public startHelp() {
			super();
			// TODO Auto-generated constructor stub
			//System.out.println("startHelp constructor");
		}

		public void actionPerformed(ActionEvent ev) {
			//System.out.println("pre try");
			//System.out.flush();
			try {

				//URL url =  new URL("file:/helpFiles/helpset.hs");
				URL url = URLClassLoader.getSystemResource("mapHelp/mapHelp.hs");

				HelpSet hs = new HelpSet(null, url);

				JHelp helpViewer = null;
				helpViewer = new JHelp(new HelpSet(null, ClassLoader.getSystemResource("mapHelp/mapHelp.hs")));


				if (helpViewer==null){
					System.out.println("helpViewer null");
				}
					if (hs==null){
					System.out.println("helpset null");
				}

				// Set the initial entry point in the table of contents.
				// Create a new frame.
				JFrame frame = new JFrame();
				// Set it's size.
				frame.setSize(800,700);
				// Add the created helpViewer to it.
				frame.getContentPane().add(helpViewer);
				// Set a default close operation.
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				// Make the frame visible.
				frame.setVisible(true);		
			}
			catch (Exception e) {
				e.printStackTrace();
			}


		}





	}*/

	class menuMapOptions implements ActionListener {
		Frame parentFrame;

		public menuMapOptions(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent ev) {
			MapParameters newParameters = MapParametersDialog
			.showDialog(parentFrame, mapParameters, mapFileManager);
			if (newParameters != null) {
				mapParameters = newParameters.clone();
				mapTabPanelControl.simpleMap.mapPanel.repaint(true);
				mapTabPanelControl.simpleMap.mapPanel.createKey();
				checkViewerData();
			}
		}
	}

	/**
	 * In viewer mode, check the right data are loaded. 
	 */
	protected void checkViewerData() {
		if (getPamController().getRunMode() == PamController.RUN_PAMVIEW) {
			if (mapTabPanelControl.simpleMap.subscribeViewerBlocks()) {
				mapTabPanelControl.simpleMap.getViewerScroller().reLoad();
			}
		}
	}

	class menuMapFile implements ActionListener {

		Frame parentFrame;

		public menuMapFile(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent ev) {
			//getMapFile.openMapDialog();
			if (getMapFile.openMapDialog() != null){
				contourPoints=getMapFile.getMapContours();
				mapContoursAvailable=true;
			}

			else{

				mapContoursAvailable=false;	
			}

			System.out.println("MapController: mapContoursAvailable: " + mapContoursAvailable);

		}
	}	

	public Serializable getSettingsReference() {
		return mapParameters;
	}

	public long getSettingsVersion() {
		return MapParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		mapParameters = ((MapParameters) pamControlledUnitSettings.getSettings()).clone(); 
		mapFileManager.readFileData(mapParameters.mapFile);
		return true;
	}

	JMenuBar mapTabMenu = null;
	private boolean initialisationComplete;
	@Override
	public JMenuBar getTabSpecificMenuBar(Frame parentFrame, JMenuBar standardMenu, PamGui pamGui) {

		// start bymaking a completely new copy.
		//if (mapTabMenu == null) {
		mapTabMenu = pamGui.makeGuiMenu();
		for (int i = 0; i < mapTabMenu.getMenuCount(); i++) {
			if (mapTabMenu.getMenu(i).getText().equals("Display")) {

				//mapTabMenu.remove(mapTabMenu.getMenu(i));

				JMenu aMenu = createDisplayMenu(parentFrame);
				aMenu.setText("Map");

				addRelatedMenuItems(parentFrame, aMenu, "Map");

				mapTabMenu.add(aMenu, i+1);
				break;
			}
		}
		//}
		return mapTabMenu;
	}

	public ArrayList<MapContourValues> getContourPoints() {
		if(mapContoursAvailable){
			mapContoursDeliveredToMap=true;
			return contourPoints;

		}
		else{
			return null;
		}


	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {

		super.notifyModelChanged(changeType);

		mapDetectionsManager.notifyModelChanged(changeType);

//		if (initialisationComplete) {
			mapTabPanelControl.simpleMap.initMapPanel();
//		}

		if (getPamController().getRunMode() == PamController.RUN_PAMVIEW) {
			switch(changeType) {
			case PamControllerInterface.INITIALIZATION_COMPLETE:
				initialisationComplete = true;
				mapTabPanelControl.simpleMap.subscribeViewerBlocks();
				mapTabPanelControl.simpleMap.initMapPanel();
				break;
			case PamControllerInterface.CHANGED_OFFLINE_DATASTORE:
			case PamControllerInterface.INITIALIZE_LOADDATA:
				if (PamController.getInstance().isInitializationComplete()) {
					checkViewerData();
				}
				break;
			}
		}
	}

	public int getMaxInterpolationTime() {
		int interpTime = 0;
		GPSControl gpsControl = (GPSControl) PamController.getInstance().findControlledUnit(GPSControl.class, null);
		if (gpsControl != null) {
			interpTime = gpsControl.getGpsParameters().readInterval * 2;
		}
		return Math.max(interpTime, 120);
	}


	public void addMouseAdapter(MouseAdapter mouseAdapter){
		this.getTabPanel();


	}

	public MapTabPanelControl getMapTabPanelControl() {
		return mapTabPanelControl;
	}

	public double getMapStuff(){
		return mapTabPanelControl.getSimpleMap().getMapPanel().getPixelsPerMetre();
	}

	public void addMouseAdapterToMapPanel(MouseAdapter mouseAdapter){
		//System.out.println("SimController::addMouseAdapterToMap");
		mapTabPanelControl.addMouseAdapterToMapPanel(mouseAdapter);


	}

	public void mapCanScroll(boolean b) {
		mapTabPanelControl.mapCanScroll(b);
		//System.out.println("Mapontroller::mapCanScroll = " + b);
	}

	/**
	 * 
	 */
	public LatLong getMapCentreLatLong() {
		// TODO Auto-generated method stub
		return mapTabPanelControl.simpleMap.getMapPanel().getMapCentreDegrees();
	}

	public void refreshDetectorList(){
		mapTabPanelControl.refreshDetectorList();
	}

	public static LatLong getMouseClickLatLong() {
		return mouseClickLatLong;
	}

	protected static void setMouseClickLatLong(LatLong mouseClickLatLong) {
		MapController.mouseClickLatLong = mouseClickLatLong.clone();
	}

	private int mouseMoveAction = MOUSE_PAN;
	protected void setMouseMoveAction(int mouseMoveAction) {
		this.mouseMoveAction = mouseMoveAction;
	}

	public int getMouseMoveAction() {
		return mouseMoveAction;
	}
}
