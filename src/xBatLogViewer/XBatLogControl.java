package xBatLogViewer;

import java.io.Serializable;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettings;

public class XBatLogControl extends PamControlledUnit implements PamSettings {

	XBatLogProcess xBatProcess;
	XBatLogSettings xBatSettings = new XBatLogSettings();
	
	public XBatLogControl(String unitName) {
		super("XBat Viewer", unitName);
		xBatProcess = new XBatLogProcess(this, null);
		addPamProcess(xBatProcess);
		
	}

	@Override
	public Serializable getSettingsReference() {
		return null;
	}

	@Override
	public long getSettingsVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		// TODO Auto-generated method stub
		return false;
	}

}
