package GPS;

import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JButton;
import PamView.PamDialog;
import PamView.PamLoadBar;

public class ImportGPSLoadBar extends PamDialog{

public static  ImportGPSLoadBar singleInstance;
	
	private PamLoadBar loadBar;
	
	public ImportGPSLoadBar(Window parentFrame, String name) {
		super(parentFrame, name, false);
		
		loadBar = new PamLoadBar("Load GPS Data");
		loadBar.setPanelSize(350, 50); 
		loadBar.setTextUpdate("...");
		
		setDialogComponent(loadBar.getPanel());
		
		getOkButton().setVisible(false);
		getCancelButton().setVisible(true);
		getCancelButton().setText("Stop");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setModalityType(Dialog.ModalityType.MODELESS);

	}
	
	
	public static ImportGPSLoadBar showDialog(Window parentFrame, String name) {
	
		if (singleInstance == null || singleInstance.getOwner() == null ||
				singleInstance.getOwner() != parentFrame) {
			singleInstance = new ImportGPSLoadBar(parentFrame, name);
		}
		
		singleInstance.setVisible(true);
		return singleInstance;
		
	}
	
	public PamLoadBar getLoadBar(){
		return loadBar; 
	}
	
	
	public JButton getCancelButton( ){
		return super.getCancelButton();
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible == false) {
			closeLater();
		}
		else {
			super.setVisible(visible);
		}
	}

	
	@Override
	public boolean getParams() {
		return false;
	}

	@Override
	public void cancelButtonPressed() {
		
	}

	@Override
	public void restoreDefaultSettings() {
		
	}

}
