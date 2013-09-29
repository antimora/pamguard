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

package Acquisition;

import hfDaqCard.SmruDaqSystem;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pamScrollSystem.ViewLoadObserver;

import simulatedAcquisition.SimProcess;

import asiojni.ASIOSoundSystem;
import asiojni.NewAsioSoundSystem;

import nidaqdev.NIDAQProcess;

import Acquisition.offlineFuncs.OfflineFileServer;
import Array.ArrayManager;
import Array.PamArray;
import PamController.OfflineDataStore;
import PamController.OfflineRawDataStore;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamguardVersionInfo;
import PamView.MenuItemEnabler;
import PamView.PamStatusBar;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RequestCancellationObject;

/**
 * Main data acquisition control to get audio data from sound cards,
 * NI cards (via UDP), files, directories of files, etc.
 * <p>
 * Uses a plug in architecture to allow new types to be added. This
 * is done through RegisterDaqType()
 * 
 * @author Doug Gillespie
 * @see Acquisition.DaqSystem
 *
 */
public class AcquisitionControl extends PamControlledUnit implements PamSettings, OfflineRawDataStore {

	protected ArrayList<DaqSystem> systemList;

	public AcquisitionParameters acquisitionParameters = new AcquisitionParameters();

	private AcquisitionProcess acquisitionProcess;

	protected MenuItemEnabler daqMenuEnabler;

	private OfflineFileServer offlineFileServer;

	private JLabel statusBarText;

	private JProgressBar levelBar;

	public static final String unitType = "Data Acquisition";

	private AcquisitionControl acquisitionControl;

	private Component statusBarComponent;

	protected PamController pamController;

	private DAQChannelListManager daqChannelListManager;

	private FolderInputSystem folderSystem;

	private DCL5System dclSystem;

	/**
	 * Main control unit for audio data acquisition.
	 * <p>
	 * It is possible to instantiate several instances of this, preferebly
	 * with different names to simultaneously aquire sound from a number of
	 * sources such as multiple sound cards, fast ADC boards, etc. 
	 * <p>
	 * Each different acquisition device must implement the DaqSystem interface 
	 * and register with each AcquisitionControl.
	 * @param name name of the Acquisition control that will apear in menus. These should be
	 * different for each instance of AcquistionControl since the names are used by PamProcesses
	 * to find the correct data blocks.
	 * @see DaqSystem
	 */
	public AcquisitionControl(String name) {

		super(unitType, name);

		acquisitionControl = this;

		pamController = PamController.getInstance();
		
		registerDaqSystem(new SoundCardSystem());
//		if (PlatformInfo.calculateOS() == OSType.WINDOWS) {
			registerDaqSystem(new ASIOSoundSystem(this));
			registerDaqSystem(new NewAsioSoundSystem(this));
//		}
		registerDaqSystem(new FileInputSystem());
		registerDaqSystem(folderSystem = new FolderInputSystem());
//		registerDaqSystem(dclSystem = new DCL5System());
		registerDaqSystem(new NIDAQProcess(this));
		registerDaqSystem(new SimProcess(this));
//		if (PlatformInfo.calculateOS() == OSType.LINUX) {
			if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
				registerDaqSystem(new SmruDaqSystem(this));
			}
//		}

		daqChannelListManager = new DAQChannelListManager(this);

		PamSettingManager.getInstance().registerSettings(this);

		addPamProcess(acquisitionProcess = new AcquisitionProcess(this));

		daqMenuEnabler = new MenuItemEnabler();

		statusBarComponent = getStatusBarComponent();

		if (isViewer) {
			offlineFileServer = new OfflineFileServer(this);
		}
		else {
			PamStatusBar statusBar = PamStatusBar.getStatusBar();

			if (statusBar != null) {
				//				statusBar.getToolBar().add(statusBarText = new JLabel());
				//				fillStatusBarText();
				//				statusBar.getToolBar().add(levelBar = new JProgressBar(-60, 0));
				//				levelBar.setValue(-60);
				//				levelBar.setOrientation(JProgressBar.HORIZONTAL);
				statusBar.getToolBar().add(statusBarComponent);
				statusBar.getToolBar().addSeparator();
				setupStatusBar();
			}
		}
		setSelectedSystem();

	}
	public AcquisitionControl(String name, boolean isSimulator) {

		super(unitType, name);

		acquisitionControl = this;

	}

	private JPanel systemPanel;
	private Component getStatusBarComponent() {
		JPanel p = new JPanel();
		p.add(statusBarText = new JLabel());
		p.add(levelBar = new JProgressBar(-60, 0));
		p.add(systemPanel = new JPanel());
		levelBar.setOrientation(SwingConstants.HORIZONTAL);
		fillStatusBarText();
		levelBar.setValue(-60);
		return p;
	}

	/**
	 * Registered new DAQ systems and makes them available via the AcquisitionCialog
	 * @param daqSystem
	 */
	public void registerDaqSystem(DaqSystem daqSystem){
		if (systemList == null) {
			systemList = new ArrayList<DaqSystem>();
		}
		systemList.add(daqSystem);
		//daqSystem.getItemsList();
		fillStatusBarText();
	}

	public static ArrayList<AcquisitionControl> getControllers() {
		ArrayList<AcquisitionControl> daqControllers = new ArrayList<AcquisitionControl>();
		PamControlledUnit pcu;
		for (int i = 0; i < PamController.getInstance().getNumControlledUnits(); i++) {
			pcu = PamController.getInstance().getControlledUnit(i);
			if (pcu.getUnitType().equals(unitType)) {
				daqControllers.add((AcquisitionControl) pcu);
			}
		}
		return daqControllers;
	}

	public AcquisitionProcess getDaqProcess() {
		return acquisitionProcess;
	}

	public void setDaqProcess(AcquisitionProcess acquisitionProcess) {
		this.acquisitionProcess = acquisitionProcess;
	}

	/**
	 * Finds a reference to a given DAQ system based on it's type (e.g.  sound card, file, etc.
	 * @param systemType
	 * @return reference to a DaqSystem object 
	 */
	public DaqSystem findDaqSystem(String systemType) {

		if (systemList == null) return null;

		if (systemType == null) systemType = acquisitionParameters.daqSystemType;

		for (int i = 0; i < systemList.size(); i++) {
			if (systemList.get(i).getSystemType().equals(systemType)) return systemList.get(i);
		}

		return null;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem daqMenu;
		daqMenu = new JMenuItem(getUnitName() + " ...");
		daqMenu.addActionListener(new acquisitionSettings(parentFrame));
		daqMenuEnabler.addMenuItem(daqMenu);
		return daqMenu;
	}


	class acquisitionSettings implements ActionListener {
		Frame parentFrame;

		public acquisitionSettings(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			AcquisitionParameters newParameters = AcquisitionDialog.showDialog(parentFrame, acquisitionControl, acquisitionParameters);
			if (newParameters != null) {
				acquisitionParameters = newParameters.clone();
				setSelectedSystem();
				checkArrayChannels(parentFrame);
				acquisitionProcess.setupDataBlock();
				setupStatusBar();
				fillStatusBarText();
				PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);
				if (isViewer) {
					offlineFileServer.createOfflineDataMap(parentFrame);
				}
			}
		}

	}

	private DaqSystem lastSelSystem = null;
	private void setSelectedSystem() {
		DaqSystem selSystem = findDaqSystem(null);
		if (selSystem == lastSelSystem) {
			return;
		}
		if (lastSelSystem != null) {
			lastSelSystem.setSelected(false);
		}
		if (selSystem != null) {
			selSystem.setSelected(true);
		}
		lastSelSystem = selSystem;
	}

	/**
	 * Run a check to see that all read out channels are connected to 
	 * a hydrophone and if not, do something about it. 
	 * @return true if ok, or problem resolved. 
	 */
	public boolean checkArrayChannels(Frame parentFrame) {		

		int error = arrayChannelsOK();
		if (error == ARRAY_ERROR_OK) {
			return true;
		}
		String message = new String("Do you want to go to the array manager now to rectify this problem ?");
		message += "\n\nFailure to do so may result in PAMGUARD crashing or features not working correctly";
		int ans = JOptionPane.showConfirmDialog(parentFrame, message, getArrayErrorMessage(error), JOptionPane.YES_NO_OPTION);
		if (ans == JOptionPane.YES_OPTION) {
			ArrayManager.showArrayDialog(getPamView().getGuiFrame());
			return checkArrayChannels(parentFrame);
		}

		return false;
	}

	private final int ARRAY_ERROR_OK = 0;
	private final int ARRAY_ERROR_NO_ARRAYMANAGER = 1;
	private final int ARRAY_ERROR_NO_ARRAY = 2;
	private final int ARRAY_ERROR_NOT_ENOUGH_HYDROPHONES = 3;
	private int arrayChannelsOK() {
		ArrayManager am = ArrayManager.getArrayManager();
		if (am == null) {
			return ARRAY_ERROR_NO_ARRAY;
		}
		PamArray pamArray  = am.getCurrentArray();
		if (pamArray == null) {
			return ARRAY_ERROR_NO_ARRAYMANAGER;
		}
		int nChannels = acquisitionParameters.nChannels;
		int[] hydrophoneList = acquisitionParameters.getHydrophoneList();
		if (hydrophoneList == null || hydrophoneList.length < nChannels) {
			return ARRAY_ERROR_NOT_ENOUGH_HYDROPHONES;
		}
		for (int iChan = 0; iChan < nChannels; iChan++) {
			if (hydrophoneList[iChan] >= pamArray.getHydrophoneCount()) {
				return ARRAY_ERROR_NOT_ENOUGH_HYDROPHONES;
			}
		}

		return ARRAY_ERROR_OK;
	}

	String getArrayErrorMessage(int error) {
		switch(error) {
		case ARRAY_ERROR_OK:
			return "Array Ok";
		case ARRAY_ERROR_NO_ARRAYMANAGER:
			return "There is no array manager within the system";
		case ARRAY_ERROR_NO_ARRAY:
			return "No PAMGUARD array has been found in the system";
		case ARRAY_ERROR_NOT_ENOUGH_HYDROPHONES:
			return "There are not enough hydrophones in the PAMGUARD array to support this number of channels";
		}
		return null;
	}

	void fillStatusBarText() {
		if (statusBarText != null){
			statusBarText.setText(getStatusBarText());
		}
	}

	@Override
	public String toString() {
		// write current status into the status bar
		String string;

		DaqSystem daqSystem = findDaqSystem(null);
		if (daqSystem == null) {
			string = new String("No Data Acquisition Configured");
			return string;
		}

		String daqName = daqSystem.getSystemName();
		if (daqName == null) daqName = new String("");

		string = String.format("%s %s %.1fkHz, %d channels", daqSystem.getSystemType(), daqName,
				acquisitionParameters.sampleRate / 1000., acquisitionParameters.nChannels);
		//		System.out.println("Set status text to " + string);

		return string;
	}

	/**
	 * Sets a level meter on the status bar
	 * @param peakValue Maximum amplitude fom AcquisitionProcess
	 */
	public void setStatusBarLevel(double peakValue) {
		// convert to dB, status bar is set on a scale of -60 to 0
		double dB = 20. * Math.log10(peakValue);
		levelBar.setValue((int) dB);
	}

	void setupStatusBar() {
		if (systemPanel == null) return;
		systemPanel.removeAll();
		DaqSystem daqSys = findDaqSystem(null);
		if (daqSys == null) return;
		Component specialComponent = daqSys.getStatusBarComponent();
		if (specialComponent != null) {
			systemPanel.add(specialComponent);
		}
	}
	/**
	 * Prepares text for the status bar
	 * @return text in the status bar
	 */
	private String getStatusBarText() {

		String statusString = toString();

		if (acquisitionProcess.getRunningSystem() == null) {
			statusString += " : Idle";
		}
		else {
			statusString += String.format(" : Running, buffer %3.1fs", acquisitionProcess.getBufferSeconds());
		}
		return statusString;
	}

	/*
	 *  (non-Javadoc)
	 * @see PamController.PamSettings#GetSettingsReference()
	 */
	public Serializable getSettingsReference() {
		return acquisitionParameters;
	}

	public long getSettingsVersion() {
		return AcquisitionParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {

		if (PamSettingManager.getInstance().isSettingsUnit(this, pamControlledUnitSettings)) {

			acquisitionParameters = ((AcquisitionParameters) pamControlledUnitSettings.getSettings()).clone();

			if (acquisitionProcess != null) {
				acquisitionProcess.setSampleRate(acquisitionParameters.sampleRate, true);
			}
			setupStatusBar();

			fillStatusBarText();

			return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#SetupControlledUnit()
	 */
	@Override
	public void setupControlledUnit() {
		super.setupControlledUnit();
		fillStatusBarText();
	}

	// converts a list of ADC channels to a list of hydrophones
	public int ChannelsToHydrophones(int channels) {
		int[] hydrophoneList = getHydrophoneList();
		if (hydrophoneList == null) return channels; // they are the same by default
		int hydrophones = 0;
		int channelListIndex;
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			channelListIndex = acquisitionParameters.getChannelListIndexes(i);
			if ((1<<i & channels) != 0 && channelListIndex >= 0) {
				hydrophones |= 1<<hydrophoneList[channelListIndex];
			}
		}
		return hydrophones;
	}

	/**
	 * Return a list of which channels are connected to which hydrophones in 
	 * the currentarray. 
	 * @return List of hydrophone numbers.
	 */
	public int[] getHydrophoneList() {
		return acquisitionParameters.getHydrophoneList();
	}

	/**
	 * Sets the list of hydrophone numbers.
	 * @param hydrophoneList List of hydrophone numbers in channel order
	 */
	public void setHydrophoneList(int[] hydrophoneList) {
		acquisitionParameters.setHydrophoneList(hydrophoneList);
	}
	/**
	 * 
	 * finds the ADC channel for a given hydrophone. 
	 * Will return -1 if no ADC channel uses this hydrophone
	 * 
	 * @param hydrophoneId Number of a hydrophone in a PamArray
	 * @return the ADC channel for the given hydrophone
	 */
	public int findHydrophoneChannel(int hydrophoneId) {
		// finds the ADC channel for a given hydrophone. 
		// will return -1 if no ADC channel uses this hydrophone
		// if no list, assume 1-1 mapping
		int channelList[] = acquisitionControl.acquisitionParameters.getHardwareChannelList();
		if (acquisitionParameters.getHydrophoneList() == null){
			if (hydrophoneId < acquisitionParameters.nChannels) {
				return hydrophoneId;
			}
			else return -1;
		}
		int channelNumber;
		for (int i = 0; i < acquisitionParameters.getHydrophoneList().length; i++){
			channelNumber = acquisitionParameters.getChannelList(i);
			if (acquisitionParameters.getHydrophone(channelNumber) == hydrophoneId){
				return channelNumber;
			}
		}
		return -1;
	}

	// Dajo, PR,
	// Method: find the phone connected to a given channel
	public int getChannelHydrophone(int channel) {

		//		int channelIndex = this.acquisitionParameters.getChannelListIndexes(channel);
		int phone = acquisitionParameters.getHydrophone(channel);

		return phone;
	}

	public double getPeak2PeakVoltage(int swChannel) {
		return acquisitionProcess.getPeak2PeakVoltage(swChannel);
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#removeUnit()
	 */
	@Override
	public boolean removeUnit() {

		boolean ans = super.removeUnit();

		PamStatusBar statusBar = PamStatusBar.getStatusBar();

		if (statusBar != null) {
			statusBar.getToolBar().remove(statusBarComponent);
			//			statusBar.getToolBar().invalidate();
			statusBar.getToolBar().repaint();
		}

		return ans;
	}

	/**
	 * Getter for acquisition parameters.
	 * @return data acquisition parameters. 
	 */
	public AcquisitionParameters getAcquisitionParameters() {
		return acquisitionParameters;
	}

	public AcquisitionProcess getAcquisitionProcess() {
		return acquisitionProcess;
	}
	public DAQChannelListManager getDaqChannelListManager() {
		return daqChannelListManager;
	}
	/**
	 * @return the offlineFileServer
	 */
	public OfflineFileServer getOfflineFileServer() {
		return offlineFileServer;
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		AcquisitionParameters daqParams = acquisitionControl.acquisitionParameters;
		paramsEl.setAttribute("SampleRate", ((Float) daqParams.sampleRate).toString());
		paramsEl.setAttribute("Channels", ((Integer) daqParams.nChannels).toString());
		paramsEl.setAttribute("voltsPeak2Peak", ((Double) daqParams.voltsPeak2Peak).toString());
		paramsEl.setAttribute("preampGain", ((Double) daqParams.preamplifier.getGain()).toString());
		DaqSystem rS = acquisitionControl.findDaqSystem(null);
		if (rS != null) {
			paramsEl.setAttribute("System.name", rS.getSystemName());
			paramsEl.setAttribute("System.type", rS.getSystemType());
			String devName = rS.getDeviceName();
			if (devName == null) {
				devName = "";
			}
			paramsEl.setAttribute("System.Device", devName);
		}
		paramsEl.setAttribute("System.DeviceNumber", "0");
		/*
		 * And a couple of dummy values which are needed by the Linux sound system
		 * so that these can be edited in the XML as necessary ...
		 */
		paramsEl.setAttribute("System.BufferSize", "3000000"); // default buffer size in microseconds. ;
		paramsEl.setAttribute("System.PeriodTime", "100000"); // default buffer size in microseconds. ;

		Element systemEl = doc.createElement("SYSTEMPARAMETERS");
		if (rS.fillXMLParameters(doc, systemEl)) {
			paramsEl.appendChild(systemEl);
		}

		//		paramsEl.setAttribute("DeviceName", daqParams.)
		return true;
	}
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch(changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			if (isViewer) {
				offlineFileServer.createOfflineDataMap(PamController.getMainFrame());
			}
		}
	}
	/**
	 * @return the folderSystem
	 */
	public FolderInputSystem getFolderSystem() {
		if (findDaqSystem(null) == dclSystem) {
			return dclSystem;
		}
		else {
			return folderSystem;
		}
	}


	@Override
	public void createOfflineDataMap(Window parentFrame) {
		offlineFileServer.createOfflineDataMap(parentFrame);
	}
	@Override
	public String getDataSourceName() {
		return offlineFileServer.getDataSourceName();
	}
	@Override
	public boolean loadData(PamDataBlock dataBlock, long dataStart, long dataEnd, 
			RequestCancellationObject cancellationObject, ViewLoadObserver loadObserver) {
		return offlineFileServer.loadData(dataBlock, dataStart, dataEnd, cancellationObject, loadObserver);
	}
	@Override
	public boolean saveData(PamDataBlock dataBlock) {
		return offlineFileServer.saveData(dataBlock);
	}
	@Override
	public PamRawDataBlock getRawDataBlock() {
		return acquisitionProcess.getRawDataBlock();
	}

	@Override
	public PamProcess getParentProcess() {
		return acquisitionProcess;
	}

}
