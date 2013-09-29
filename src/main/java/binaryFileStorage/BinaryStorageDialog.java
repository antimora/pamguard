package binaryFileStorage;

import java.awt.Frame;
import java.awt.Window;
import PamView.PamDialog;

public class BinaryStorageDialog extends PamDialog {

	private static BinaryStorageDialog singleInstance;

	private BinaryStoreSettings binaryStoreSettings;
	
	private BinaryStorageDialogPanel binaryStorageDialogPanel;


	private BinaryStorageDialog(Window parentFrame) {
		super(parentFrame, "Binary Storage Options", false);

		binaryStorageDialogPanel = new BinaryStorageDialogPanel(parentFrame);
		setDialogComponent(binaryStorageDialogPanel.getPanel());
	}

	public static BinaryStoreSettings showDialog(Frame parentFrame, BinaryStoreSettings binaryStoreSettings) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
			singleInstance = new BinaryStorageDialog(parentFrame);
		}
		singleInstance.binaryStoreSettings = binaryStoreSettings.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.binaryStoreSettings;
	}

	private void setParams() {
		binaryStorageDialogPanel.setParams(binaryStoreSettings);
	}

	@Override
	public void cancelButtonPressed() {
		binaryStoreSettings = null;
	}

	@Override
	public boolean getParams() {
		if (binaryStoreSettings == null) {
			binaryStoreSettings = new BinaryStoreSettings();
		}
		return binaryStorageDialogPanel.getParams(binaryStoreSettings);
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
