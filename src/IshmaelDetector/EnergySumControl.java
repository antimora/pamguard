/**
 * 
 */
package IshmaelDetector;

/**
 * @author Dave Mellinger and Hisham Qayum
 */

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;

public class EnergySumControl extends IshDetControl implements PamSettings 
{
	public EnergySumControl(String unitName) {
		super("Energy Sum Detector", unitName, new EnergySumParams());
	}
	
	@Override
	public PamDataBlock getDefaultInputDataBlock() {
		return PamController.getInstance().getFFTDataBlock(0);
	}

	@Override
	public IshDetFnProcess getNewDetProcess(PamDataBlock defaultDataBlock) {
		return new EnergySumProcess(this, defaultDataBlock);
	}
	
	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#SetupControlledUnit()
	 */
	//@Override
	//public void setupControlledUnit() {
	//	super.setupControlledUnit();
	//	//have it find its own data block - for now, just take the 
	//	//first fft block that can be found.
	//}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		return super.createDetectionMenu(parentFrame, "Energy Sum Settings...");
	}
	
	class menuSmoothingDetection implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
//			KernelSmoothingParameters newParams = KernelSmoothingDialog.show(smoothingParameters, smoothingProcess.getOutputDataBlock(0));
//			if (newParams != null) {
//				smoothingParameters = newParams.clone();
////				edgeSettings.prepareProcess();
//				newSettings();
//				PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);
//			}
		}
	}

	@Override
	public void showParamsDialog1(Frame parentFrame) {
		EnergySumParams newParams =
			EnergySumParamsDialog.showDialog2(parentFrame, (EnergySumParams)ishDetParams);
		installNewParams(parentFrame, newParams);
	}
	
//	public long getSettingsVersion() {
//		return KernelSmoothingParameters.serialVersionUID;
//	}

	//This is called after a settings file is read.  Copy the newly read settings
	//to energySumParams.
	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		EnergySumParams newParams = 
			(EnergySumParams)pamControlledUnitSettings.getSettings();
		//I can't figure out why inputDataSource is not in the newParams
		//returned by getSettings, but sometimes it's not. 
		if (newParams.inputDataSource == null)
			newParams.inputDataSource = ishDetParams.inputDataSource;
		ishDetParams = newParams.clone();
		return super.restoreSettings(pamControlledUnitSettings);
	}

	public Serializable getSettingsReference() {
		return ishDetParams;
	}
	 
	/**
	 * @return An integer version number for the settings
	 */
	public long getSettingsVersion() {
		return EnergySumParams.serialVersionUID;
	}
}
