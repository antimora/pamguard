package staticLocaliser;

import java.awt.Component;
import java.awt.Dialog;

import javax.swing.JPanel;

import clickDetector.offlineFuncs.ClickBearingTask;
import clickDetector.offlineFuncs.ClickDelayTask;
import clickDetector.offlineFuncs.ClickReClassifyTask;
import clickDetector.offlineFuncs.EchoDetectionTask;
import offlineProcessing.OLProcessDialog;
import offlineProcessing.OfflineTaskGroup;

/**
 * This class deals with processes in the static localiser which ONLY occur in offline mode. The major difference between the static localiser on offline and real time mode is the 'Batch run' function. In real time the static localiser will attempt 
 * to loclaise every incoming detection specified by the user, however in offline mode the localiser must access all binary files. 
 *
 * @author Jamie Macaulay
 *
 */
public class StaticLocaliserOfflineFunctions {
	
	StaticLocaliserControl staticLocaliserControl;
	
	OfflineTaskGroup staticOfflineTaskGroup;
	
	OLProcessDialog batchLocaliseDialog;
	
	public StaticLocaliserOfflineFunctions(StaticLocaliserControl staticLocaliserControl){
		this.staticLocaliserControl=staticLocaliserControl;
		
	}
	
	
	/**
	 * Get / Create an offline task group for batch run processing. 
	 * @return offline task group. Create if necessary
	 */
	private OfflineTaskGroup getOfflineTaskGroup() {
		staticOfflineTaskGroup = new OfflineTaskGroup(staticLocaliserControl, "Batch Localise");
		staticOfflineTaskGroup.setPrimaryDataBlock(staticLocaliserControl.getCurrentDatablock());
		staticOfflineTaskGroup.addTask(new BatchLocaliseOfflineTask(staticLocaliserControl));

		return staticOfflineTaskGroup;
	}
	
	/**
	 * 
	 */
	public void openBatchRunDialog() {
		//need to disable the whole localisation control panel.
		staticLocaliserControl.getStaticMainPanel().getCurrentControlPanel().setlayerPanelEnabled(false);
		staticLocaliserControl.getStaticMainPanel().setLocaliserControlEnabled(false);
		//TODO
		if (batchLocaliseDialog == null) {
			batchLocaliseDialog = new OLProcessDialog(staticLocaliserControl.getPamView().getGuiFrame(), 
					getOfflineTaskGroup(), "Batch Loclalise");
			//batchLocaliseDialog.setModalityType(Dialog.ModalityType.MODELESS);
		}
		batchLocaliseDialog.setVisible(true);
		

		//once the batch localise panel has closed can enable the panel again. 
		staticLocaliserControl.getStaticMainPanel().getCurrentControlPanel().setlayerPanelEnabled(true);
		staticLocaliserControl.getStaticMainPanel().setLocaliserControlEnabled(true);
		//once the batch locaiser has closed cancel any running localiser threads
		staticLocaliserControl.getStaticLocaliser().cancelThread();
		//TODO
		//Once the control panel is enabled, find the currently selected click and update all other panels. 
		//TODO
	}
	
	public void setPanelEnabled(JPanel panel, boolean enable){
		Component[] com = panel.getComponents();  
		for (int a = 0; a < com.length; a++) {  
		     com[a].setEnabled(enable);  
		}  
	}
	

	
	 

}
