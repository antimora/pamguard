package PamController;

import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JFrame;

import PamguardMVC.PamDataBlock;

public class StorageOptions implements PamSettings {
	
	private static StorageOptions singleInstance;
	
	private StorageParameters storageParameters = new StorageParameters();
	
	private StorageOptions() {
		PamSettingManager.getInstance().registerSettings(this);
	}
	
	public boolean showDialog(JFrame parentFrame) {
		StorageParameters newParams = StorageOptionsDialog.showDialog(parentFrame, storageParameters);
		if (newParams != null) {
			storageParameters = newParams.clone();
			setBlockOptions();
			return true;
		}
		else {
			return false;
		}
	}
	
	public static StorageOptions getInstance() {
		if (singleInstance == null) {
			singleInstance = new StorageOptions();
		}
		return singleInstance;
	}

	@Override
	public Serializable getSettingsReference() {
		return storageParameters;
	}

	@Override
	public long getSettingsVersion() {
		return StorageParameters.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return "PAMGUARD Storage Options";
	}

	@Override
	public String getUnitType() {
		return "PAMGUARD Storage Options";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		storageParameters = ((StorageParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}
	
	/**
	 * Set the options in available data blocks. 
	 */
	public void setBlockOptions() {
		ArrayList<PamDataBlock> blocks = PamController.getInstance().getDataBlocks();
		boolean doLog;
		for (PamDataBlock aBlock:blocks) {
			doLog = storageParameters.isStoreDatabase(aBlock.getDataName(), true);
			aBlock.setShouldLog(doLog);
			doLog = storageParameters.isStoreBinary(aBlock.getDataName(), true);
			aBlock.setShouldBinary(doLog);
		}
	}

}
