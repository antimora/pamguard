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

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;


public abstract class IshDetControl extends PamControlledUnit implements PamSettings 
{
	IshDetFnProcess ishDetFnProcess;
	IshDetParams ishDetParams;
	IshPeakProcess ishPeakProcess;
	IshDetGraphics ishDetGraphics;
	IshDetSave ishDetSave;

	/** Initializer. 
	 * <p>IMPORTANT: The subclass initializer should construct the ishDetParams
	 * to pass here.  See EnergySumControl for an example.
	 */ 
	public IshDetControl(String controlName, String unitName, IshDetParams ishDetParams)
	{
		super(controlName, unitName);
		this.ishDetParams = ishDetParams;

		//Detection function.
		PamDataBlock defaultInputDataBlock = getDefaultInputDataBlock();
		ishDetParams.inputDataSource = defaultInputDataBlock.getDataName();

		PamSettingManager.getInstance().registerSettings(this);
		
		//Call subclass to provide appropriate instance of detection process.
		ishDetFnProcess = getNewDetProcess(defaultInputDataBlock);
		
		addPamProcess(ishDetFnProcess); //make it show up in the Data Model window
		ishDetFnProcess.setParentDataBlock(defaultInputDataBlock);

		//Peak picker.
		//PamDataBlock detfnDataBlock = ishDetProcess.outputDataBlock;
		ishPeakProcess = new IshPeakProcess(this, getOutputDataBlock());
		addPamProcess(ishPeakProcess);
		ishPeakProcess.prepareMyParams();
		
		//Display.
		ishDetGraphics = new IshDetGraphics(this, getOutputDataBlock());
		
		//Saver.
		ishDetSave = new IshDetSave(this);
	}
	
	/** Return any old data block of the right type so that the detection 
	 * process's input can get hooked up to something from the get-go.  The
	 * input is typically re-hooked when the settings file is read.
	 * @return PamDataBlock
	 */
	public abstract PamDataBlock getDefaultInputDataBlock();
	
	/** Create a new IshDetProcess of the appropriate type and return it.
	 * For example, EnergySumControl returns an EnergySumProcess.
	 */
	public abstract IshDetFnProcess getNewDetProcess(PamDataBlock defaultDataBlock);
	
	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#SetupControlledUnit()
	 */
	//@Override
	//public void setupControlledUnit() {
	//	super.setupControlledUnit();
	//}

	/* This is a hack to get the non-detection processes initialized.  Since
	 * the detection process is the only one registered via addPamProcess, it
	 * is the only one that gets a call to prepareProcess.  Here we make sure
	 * that my other processes (peak-picking, graphics, save) get initialized. 
	 * These other processes don't have addPamProcess called on them because
	 * then they would show up in the Data Model.
	 */
	public void prepareNonDetProcesses() {
		ishDetGraphics.prepareForRun();
		ishPeakProcess.prepareForRun();
		ishDetSave.prepareForRun();
	}
	
	public JMenuItem createDetectionMenu(Frame parentFrame, String menuString) {
		JMenuItem menuItem = new JMenuItem(menuString);
		menuItem.addActionListener(new IshDetSettings(parentFrame));
		return menuItem;
	}
	
	class IshDetSettings implements ActionListener {
		Frame parentFrame;
		public IshDetSettings(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}
		public void actionPerformed(ActionEvent e) { 
			//old way: showParamsDialog(parentFrame, ishDetParams);
			showParamsDialog1(parentFrame);
		}
	}

	abstract public void showParamsDialog1(Frame parentFrame);
	
//	class menuSmoothingDetection implements ActionListener {
//		public void actionPerformed(ActionEvent ev) {
//			KernelSmoothingParameters newParams = KernelSmoothingDialog.show(smoothingParameters, smoothingProcess.getOutputDataBlock(0));
//			if (newParams != null) {
//				smoothingParameters = newParams.clone();
////				edgeSettings.prepareProcess();
//				newSettings();
//				PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);
//			}
//		}
//	}

//	public long getSettingsVersion() {
//		return KernelSmoothingParameters.serialVersionUID;
//	}

	/** This is called after a settings file is read.  The subclass should 
	 * get newParams and clone it as ishDetParams before calling here.
	 */
	public boolean restoreSettings(PamControlledUnitSettings dummy) {
		//Subclass should clone newParams before calling here!!
		if (ishDetFnProcess  != null) ishDetFnProcess .setupConnections();
		if (ishPeakProcess   != null) ishPeakProcess  .setupConnections();
		return true;
	}
	
	protected void installNewParams(Frame parentFrame, IshDetParams newParams) {
		if (newParams != null) {
			ishDetParams = newParams.clone(); //makes a new EnergySumParams etc.
			if (ishDetFnProcess != null) {
				ishDetFnProcess.setupConnections();
				PamController.getInstance().notifyModelChanged(
						PamControllerInterface.CHANGED_PROCESS_SETTINGS);
			}
			if (ishPeakProcess != null)
				ishPeakProcess.setupConnections();
		}
	}
	
	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#pamToStart()
	 */
	@Override
	public void pamToStart() {
		super.pamToStart();
		if (ishDetFnProcess != null) {
			ishDetFnProcess.setupConnections();
		}
		if (ishPeakProcess != null)
			ishPeakProcess.setupConnections();
	}

	public PamDataBlock getOutputDataBlock() {
		return ishDetFnProcess.outputDataBlock;
	}
}
