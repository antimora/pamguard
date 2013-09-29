/**
 * 
 */
package IshmaelDetector;

/**
 * @author Hisham Qayum and Dave Mellinger
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

public class SgramCorrControl extends IshDetControl implements PamSettings 
{
	public SgramCorrControl(String unitName) {
		super("Spectrogram Correlation Detector", unitName, new SgramCorrParams());
	}
	
	@Override
	public PamDataBlock getDefaultInputDataBlock() {
		return PamController.getInstance().getFFTDataBlock(0);
	}

	@Override
	public IshDetFnProcess getNewDetProcess(PamDataBlock defaultDataBlock) {
		return new SgramCorrProcess(this, defaultDataBlock);
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
		return super.createDetectionMenu(parentFrame, "Spectrogram Correlation Settings...");
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
		SgramCorrParams newParams =
			SgramCorrParamsDialog.showDialog2(parentFrame, (SgramCorrParams)ishDetParams);
		installNewParams(parentFrame, newParams);
	}
	
//	public long getSettingsVersion() {
//		return KernelSmoothingParameters.serialVersionUID;
//	}

	//This is called after a settings file is read.  Copy the newly read settings
	//to sgramCorrParams.
	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		SgramCorrParams newParams = 
			(SgramCorrParams)pamControlledUnitSettings.getSettings();
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
		return SgramCorrParams.serialVersionUID;
	}
}
