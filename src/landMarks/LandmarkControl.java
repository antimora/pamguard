package landMarks;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.PamSymbol;
import PamguardMVC.PamProcess;

public class LandmarkControl extends PamControlledUnit implements PamSettings {

	private LandmarkDataBlock landmarkDataBlock;
	
	private LandmarkControl THIS;
	
	private LandmarkProcess landmarkProcess;
	
	protected LandmarkDatas landmarkDatas = new LandmarkDatas();
	
	private PamSymbol defaultSymbol = new PamSymbol(PamSymbol.SYMBOL_HEXAGRAM, 12, 12, true, Color.BLACK, Color.BLUE);
	
	public LandmarkControl(String unitName) {
		super("Landmarks", unitName);
		THIS = this;
		landmarkProcess = new LandmarkProcess(this);
		addPamProcess(landmarkProcess);
		PamSettingManager.getInstance().registerSettings(this);
		landmarkDataBlock.createDataUnits(landmarkDatas);
	}

	@Override
	public JMenuItem createDisplayMenu(Frame parentFrame) {

		JMenuItem menuItem = new JMenuItem(getUnitName() + " objects");
		menuItem.addActionListener(new DisplayMenuAction(parentFrame));
		return menuItem;
	}

	public Serializable getSettingsReference() {
		return landmarkDatas;
	}

	public long getSettingsVersion() {
		return LandmarkDatas.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		landmarkDatas = ((LandmarkDatas) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	class DisplayMenuAction implements ActionListener {
		
		Frame parentFrame;

		public DisplayMenuAction(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			LandmarkDatas newLandmarks = LandmarksDialog.showDialog(parentFrame, THIS);
			if (newLandmarks != null) {
				landmarkDatas = newLandmarks;
				landmarkDataBlock.createDataUnits(landmarkDatas);
			}
		}
		
	}
	
	class LandmarkProcess extends PamProcess {

		LandmarkControl landmarkControl;
		
		public LandmarkProcess(LandmarkControl landmarkControl) {
			super(landmarkControl, null);
			this.landmarkControl = landmarkControl;
			addOutputDataBlock(landmarkDataBlock = new LandmarkDataBlock(getUnitName(), landmarkControl, this));
		}

		@Override
		public void pamStart() {
			
		}

		@Override
		public void pamStop() {
			
		}
		
	}

	public PamSymbol getDefaultSymbol() {
		return defaultSymbol;
	}

	public void setDefaultSymbol(PamSymbol defaultSymbol) {
		this.defaultSymbol = defaultSymbol;
	}
}
