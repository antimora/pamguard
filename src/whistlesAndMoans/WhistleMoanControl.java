package whistlesAndMoans;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import spectrogramNoiseReduction.SpectrogramNoiseProcess;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamSidePanel;

public class WhistleMoanControl extends PamControlledUnit implements PamSettings {

	private WhistleToneConnectProcess whistleToneProcess;

	protected WhistleToneParameters whistleToneParameters = new WhistleToneParameters();
	
	private SpectrogramNoiseProcess spectrogramNoiseProcess;
	
	public WhistleMoanControl(String unitName) {
		super("WhistlesMoans", unitName);
		
		spectrogramNoiseProcess = new SpectrogramNoiseProcess(this);
		addPamProcess(spectrogramNoiseProcess);
		
		whistleToneProcess = new WhistleToneConnectProcess(this);
		addPamProcess(whistleToneProcess);
		
		PamSettingManager.getInstance().registerSettings(this);
		
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamControllerInterface.INITIALIZATION_COMPLETE) {
			whistleToneProcess.setupProcess();
		}
	}
	
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName());
		menuItem.addActionListener(new DetectionSettings(parentFrame));
		return menuItem;
	}
	
	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDisplayMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDisplayMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName());
		menuItem.addActionListener(new DisplaySettings(parentFrame));
		return menuItem;
	}

	class DetectionSettings implements ActionListener {

		private Frame parentFrame;
		
		public DetectionSettings(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			settingsDialog(parentFrame);	
		}
		
	}
	class DisplaySettings implements ActionListener {

		private Frame parentFrame;
		
		public DisplaySettings(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			displayDialog(parentFrame);	
		}
		
	}
	
	private void settingsDialog(Frame parentFrame) {
		WhistleToneParameters newSettings = WhistleToneDialog.showDialog(parentFrame, 
				this);
		if (newSettings != null) {
			whistleToneParameters = newSettings.clone();
			whistleToneProcess.setupProcess();
		}
	}
	
	private void displayDialog(Frame parentFrame) {
		WhistleToneParameters newSettings = WMDisplayDialog.showDialog(this, parentFrame);
		if (newSettings != null) {
			whistleToneParameters = newSettings.clone();
		}
	}

	@Override
	public PamSidePanel getSidePanel() {
		return whistleToneProcess.dataCounter.getSidePanel();
	}

	/**
	 * @return the spectrogramNoiseProcess
	 */
	public SpectrogramNoiseProcess getSpectrogramNoiseProcess() {
		return spectrogramNoiseProcess;
	}

	/**
	 * @return the whistleToneProcess
	 */
	public WhistleToneConnectProcess getWhistleToneProcess() {
		return whistleToneProcess;
	}


	@Override
	public Serializable getSettingsReference() {
		return whistleToneParameters;
	}

	@Override
	public long getSettingsVersion() {
		return WhistleToneParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		whistleToneParameters = ((WhistleToneParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		/*
		 * 
	private int connectType = 8;
	private double minFrequency;
	private double maxFrequency;
	public int minPixels = 20;
	public int minLength = 10;
	public int maxCrossLength = 5;
	public int fragmentationMethod = FRAGMENT_RELINK;
	
		 */
		paramsEl.setAttribute("connectType", String.format("%d", whistleToneParameters.getConnectType()));
		paramsEl.setAttribute("minFrequency", String.format("%f", whistleToneParameters.getMinFrequency()));
		paramsEl.setAttribute("maxFrequency", String.format("%f", whistleToneParameters.getMaxFrequency(whistleToneProcess.getSampleRate())));
		paramsEl.setAttribute("minPixels", String.format("%d", whistleToneParameters.minPixels));
		paramsEl.setAttribute("minLength", String.format("%d", whistleToneParameters.minLength));
		paramsEl.setAttribute("maxCrossLength", String.format("%d", whistleToneParameters.maxCrossLength));
		paramsEl.setAttribute("fragmentationMethod", String.format("%d", whistleToneParameters.fragmentationMethod));
		return true;
	}
	@Override
	public String getModuleSummary() {
		return whistleToneProcess.getModuleSummary();
	}

	@Override
	public Object getShortUnitType() {
		return "WMD";
	}
}
