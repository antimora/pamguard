package WorkshopDemo;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamSymbol;
import PamView.PamSymbolDialog;

/**
 * Simple detector designed to demonstrate main Pamguard developer 
 * environment, using as many Pamguard features as possible, but in 
 * a really simple way.
 * 
 * The detector is a very simple in band energy detector. It will 
 * subscribe to a block of FFT (spectrogram) data and measure 
 * the background noise in a given frequency band over some time period 
 * and compare the signal to that background measure. If the SNR is >
 * threhsold a detection starts, if it's below threshold it stops again. 
 * 
 * @author Doug
 *
 */

public class WorkshopController extends PamControlledUnit implements PamSettings{

	WorkshopProcess workshopProcess; 
	
	WorkshopProcessParameters workshopProcessParameters;
	
	private WorkshopPluginPanelProvider workshopPluginPanelProvider;
	
	/**
	 * Must have a default constructor that takes a single String as an argument. 
	 * @param unitName Instance specific name to give this module. 
	 */
	public WorkshopController(String unitName) {
		super("Workshop Demo Detector", unitName);

		/*
		 * create the parameters that will control the process. 
		 * (do this before crating the process in case the process
		 * tries to access them from it's constructor). 
		 */ 
		workshopProcessParameters = new WorkshopProcessParameters();
		
		/*
		 * make a WorkshopProcess - which will actually do the detecting
		 * for us. Although the super class PamControlledUnit keeps a list
		 * of processes in this module, it's also useful to keep a local 
		 * reference.
		 */
		addPamProcess(workshopProcess = new WorkshopProcess(this));
		
		/*
		 * provide plug in panels for the bottom of the spectrogram displays
		 * (and any future displays that support plug in panels)
		 */
		workshopPluginPanelProvider = new WorkshopPluginPanelProvider(this);
		
		/*
		 * Tell the PAmguard settings manager that we have settings we wish to
		 * be saved between runs. IF settings already exist, the restoreSettings()
		 * function will get called back from here with the most recent settings. 
		 */
		PamSettingManager.getInstance().registerSettings(this);
	}
	
	@Override
	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub
		super.notifyModelChanged(changeType);
		
		/*
		 * This gets called every time a new module is added - make sure
		 * that the workshopProcess get's a chance to look around and see
		 * if there is data it wants to subscribe to. 
		 */
		switch (changeType) {
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
		case PamControllerInterface.ADD_DATABLOCK:
		case PamControllerInterface.REMOVE_DATABLOCK:
			workshopProcess.prepareProcess();
		}
	}

	/**
	 * This next function sets up a menu which wil be added to the main Display menu
	 */
	@Override
	public JMenuItem createDisplayMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " Map symbol");
		menuItem.addActionListener(new MapSymbolSelect());
		return menuItem;
	}
	
	/*
	 * Menu actionlistener, using a standard Pamgaurd symbol dialog to 
	 * select the map symbol shape and colour. 
	 */
	class MapSymbolSelect implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			PamSymbol newSymbol = PamSymbolDialog.show(getPamView().getGuiFrame(), 
					workshopProcessParameters.mapSymbol);
			if (newSymbol != null) {
				workshopProcessParameters.mapSymbol = newSymbol;
			}
			
		}
		
	}
	

	/*
	 * Menu item and action for detection parameters...
	 *  (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " Parameters");
		menuItem.addActionListener(new SetParameters(parentFrame));
		return menuItem;
	}
	
	class SetParameters implements ActionListener {

		Frame parentFrame;
		
		public SetParameters(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {

			WorkshopProcessParameters newParams = WorkshopParametersDialog.showDialog(parentFrame, 
					workshopProcessParameters);
			/*
			 * The dialog returns null if the cancel button was set. If it's 
			 * not null, then clone the parameters onto the main parameters reference
			 * and call preparePRocess to make sure they get used !
			 */
			if (newParams != null) {
				workshopProcessParameters = newParams.clone();
				workshopProcess.prepareProcess();
			}
			
		}
		
	}

	/**
	 * These next three functions are needed for the PamSettings interface
	 * which will enable Pamguard to save settings between runs
	 */
	public Serializable getSettingsReference() {
		return workshopProcessParameters;
	}

	public long getSettingsVersion() {
		return WorkshopProcessParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		workshopProcessParameters = (WorkshopProcessParameters) pamControlledUnitSettings.getSettings();
		return true;
	}

}
