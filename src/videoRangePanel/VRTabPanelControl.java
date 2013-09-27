package videoRangePanel;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import PamView.PamBorderPanel;
import PamView.PamTabPanel;

public class VRTabPanelControl implements PamTabPanel {
	
	private VRControl vrControl;
	
	protected VRPanel vrPanel;
	
	protected ImageAnglePanel imageAnglePanel;
	
	private PamBorderPanel videoPanel; 
	

	public VRTabPanelControl(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
		
		videoPanel = new PamBorderPanel();
		videoPanel.setLayout(new BorderLayout());
		
		vrPanel = new VRPanel(vrControl);
		imageAnglePanel = new ImageAnglePanel(vrControl);
		
		videoPanel.add(BorderLayout.CENTER, vrPanel);
		videoPanel.add(BorderLayout.NORTH, imageAnglePanel);
	}
	
	protected boolean loadFile(File file) {
		boolean imageOk = vrPanel.loadImageFromFile(file);
//		if (imageOk) {
			newImage();
//		}
		return imageOk;
	}
	
	protected boolean pasteImage() {
		boolean imageOk =  vrPanel.pasteImage();
//		if (imageOk) {
			newImage();
//		}
		return imageOk;
	}
	
	/**
	 * Called when a new image is loaded. 
	 */
	public void newImage() {
		imageAnglePanel.newImage();
	}
	
	/**
	 * Called for new calibration or new height data
	 */
	public void newCalibration() {
		imageAnglePanel.newCalibration();
	}
	/**
	 * Called when a new VR Method is selected
	 */
	public void newMethod() {
		imageAnglePanel.newMethod();
	}

	public JMenu createMenu(Frame parentFrame) {
		return null;
	}

	public JComponent getPanel() {
		return videoPanel;
	}

	public JToolBar getToolBar() {
		return null;
	}
	
	public void newSettings() {
		vrPanel.repaint();
		imageAnglePanel.newCalibration();
	}
	
	public void setStatus() {
		vrPanel.innerPanel.setToolTipText(vrControl.getInstruction());
	}
	
	public void showComponents() {
		boolean showShoreAngle = false;
		if (vrControl.getVrStatus() == VRControl.MEASURE_FROM_SHORE) {
			showShoreAngle = true;
		}
		imageAnglePanel.setVisible(true);
	}
	
	public void enableControls() {
		if (imageAnglePanel != null) 
			imageAnglePanel.enableControls();
	}

}
