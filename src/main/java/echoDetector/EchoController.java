package echoDetector;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;
/**
 * This class is a very basic broadband echo detector. It computes the echo
 * delay present in a waveform by picking the peak of the cepstrum of the 
 * waveform (see Bogert et al. 1963 - <i>Proc. of the Symposium on Time Series
 * Analysis</i>. ed: M. Rosenblatt pp 209-243).  
 * 
 * @author Brian Miller
 *
 */
public class EchoController extends PamControlledUnit implements PamSettings{
	EchoProcess echoProcess;
	EchoProcessParameters echoProcessParameters;
	//EchoProcessParametersDialog echoProcessParametersDialog;
	
	public EchoController(String unitName){
		super("Echo detector", unitName);
		
		echoProcessParameters = new EchoProcessParameters();
		
		addPamProcess(echoProcess = new EchoProcess(this));
		
		PamSettingManager.getInstance().registerSettings(this);
	}

	@Override
	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub
		super.notifyModelChanged(changeType);
		
		/*
		 * This gets called every time a new module is added - make sure
		 * that the echoProcess get's a chance to look around and see
		 * if there is data it wants to subscribe to. 
		 */
		switch (changeType) {
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.REMOVE_CONTROLLEDUNIT:
		case PamControllerInterface.ADD_DATABLOCK:
		case PamControllerInterface.REMOVE_DATABLOCK:
			echoProcess.prepareProcess();
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

			EchoProcessParameters newParams = EchoParametersDialog.showDialog(parentFrame, 
					echoProcessParameters);
			/*
			 * The dialog returns null if the cancel button was set. If it's 
			 * not null, then clone the parameters onto the main parameters reference
			 * and call preparePRocess to make sure they get used !
			 */
			if (newParams != null) {
				echoProcessParameters = newParams.clone();
				echoProcess.prepareProcess();
			}
			
		}
		
	}

	
	/**
	 * These next three functions are needed for the PamSettings interface
	 * which will enable Pamguard to save settings between runs
	 */
	public Serializable getSettingsReference() {
		return echoProcessParameters;
	}

	public long getSettingsVersion() {
		return EchoProcessParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		echoProcessParameters = (EchoProcessParameters) pamControlledUnitSettings.getSettings();
		return true;
	}

}
