package EdgeDetector;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;

/**
 * Edge detector control
 * @author Doug
 *
 */
public class EdgeControl extends PamControlledUnit implements PamSettings {
	
	EdgeParameters edgeParameters = new EdgeParameters();
	
	EdgeProcess edgeProcess;
	
	public EdgeControl(String unitName) {
		
		super("EDge Detector", unitName);
		
		addPamProcess(edgeProcess = new EdgeProcess(this));
		
		PamSettingManager.getInstance().registerSettings(this);
		
	}

	@Override
	public void pamToStart() {
		// TODO Auto-generated method stub
		super.pamToStart();
//		Point p = null;
//		p.x = 5;
//		int h = p.x/2;
	}

	public Serializable getSettingsReference() {
		return edgeParameters;
	}

	public long getSettingsVersion() {
		return EdgeParameters.serialVersionUID;
	}
	
	private void newSettings() {
		PamDataBlock pamDatablock = PamController.getInstance().getFFTDataBlock(edgeParameters.fftBlockIndex);
		edgeProcess.setParentDataBlock(pamDatablock);
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		edgeParameters = (EdgeParameters) pamControlledUnitSettings.getSettings();
		newSettings();
		return true;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem;

		menuItem = new JMenuItem("Edge Detection Settings ...");
		menuItem.addActionListener(new menuEdgeDetection(parentFrame));

		return menuItem;
	}
	class menuEdgeDetection implements ActionListener {
		
		Frame parentFrame;
		
		public menuEdgeDetection(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent ev) {
			EdgeParameters newParams = EdgeParamsDialog.showDialog(parentFrame, edgeParameters);
			if (newParams != null) {
				edgeParameters = newParams.clone();
//				edgeSettings.prepareProcess();
				newSettings();
				PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);
			}
		}
	}

}
