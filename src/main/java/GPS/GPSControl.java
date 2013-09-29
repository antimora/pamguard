package GPS;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import NMEA.NMEADataBlock;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;

public class GPSControl extends PamControlledUnit implements PamSettings{

	GPSParameters gpsParameters = new GPSParameters();
	
	protected NMEADataBlock nmeaDataBlock;

	boolean doAutoClockUpdate;
	
	private GPSControl gpsControl;
	
	private ProcessNmeaData gpsProcess;
	
	protected ProcessHeadingData headingProcess;
	
	public static final String gpsUnitType = "GPS Acquisition";
	
	//viewer functionality;
	private ImportGPSData importGPSData;
	GPSImportParams gpsImportParams= new GPSImportParams();

	GPSDataBlock gpsDataBlock; 

	
	public GPSControl(String unitName) {
		super(gpsUnitType, unitName);
		
		gpsControl = this;
		addPamProcess(gpsProcess = new ProcessNmeaData(this));
		addPamProcess(headingProcess = new ProcessHeadingData(this));
		PamSettingManager.getInstance().registerSettings(this);
		gpsProcess.noteNewSettings();
		headingProcess.noteNewSettings();
		
		gpsDataBlock=gpsProcess.gpsDataBlock;
		
		if (super.isViewer){
			
			importGPSData=new ImportGPSData(this);
			gpsImportParams=new GPSImportParams();
		}
	
	}
	NMEADataBlock getNMEADataBlock() {
		return nmeaDataBlock;
	}

	public JMenuItem createGPSMenu(Frame parentFrame) {
		JMenuItem menuItem;
		
		JMenu subMenu = new JMenu("GPS");
		
		menuItem = new JMenuItem("GPS Options ...");
		menuItem.addActionListener(new GpsOptions(parentFrame));
		subMenu.add(menuItem);
		
		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			menuItem = new JMenuItem("Import GPS Data ...");
			menuItem.addActionListener(new ImportGPSDataDialog());
			subMenu.add(menuItem);
		}
		
		menuItem = new JMenuItem("Update PC Clock ...");
		menuItem.addActionListener(new UpdateClock(parentFrame));
		subMenu.add(menuItem);
		

		return subMenu;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		return createGPSMenu(parentFrame);
	}

	
	public GPSDataBlock getGpsDataBlock() {
		return gpsProcess.gpsDataBlock;
	}

	
	class ImportGPSDataDialog implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String currentPath;
			GPSImportParams newParams=ImportGPSDialog.showDialog(PamController.getInstance().getMainFrame(), PamController.getInstance().getMainFrame().getMousePosition(),gpsImportParams, importGPSData);
			
			if (newParams!=null) gpsImportParams=newParams.clone();
			
			if (newParams.path.size()==0) {
				currentPath=null;
				return; 
			}
			else{
				currentPath=newParams.path.get(0);
				importGPSData.loadFile(currentPath);
			}

		}
		
	}


	class GpsOptions implements ActionListener {
		Frame parentFrame;
		
		public GpsOptions(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			GPSParameters newP = GPSParametersDialog.showDialog(parentFrame, gpsParameters);
			if (newP != null) {
				gpsParameters = newP.clone();
				gpsProcess.noteNewSettings();
				headingProcess.noteNewSettings();
			}
		}
	}
	class UpdateClock implements ActionListener {
		Frame parentFrame;
		
		public UpdateClock(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			GPSParameters newP = UpdateClockDialog.showDialog(parentFrame, gpsControl, gpsParameters, false);
			gpsParameters = newP.clone();
		}
	}
	public Serializable getSettingsReference() {
		return gpsParameters;
	}
	public long getSettingsVersion() {
		return GPSParameters.serialVersionUID;
	}
	
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {

		if (pamControlledUnitSettings.getUnitType().equals(this.getUnitType())
			&& pamControlledUnitSettings.getVersionNo() == GPSParameters.serialVersionUID) {
			this.gpsParameters = ((GPSParameters) pamControlledUnitSettings.getSettings()).clone();
		}
		
		doAutoClockUpdate = gpsParameters.setClockOnStartup;
		
		
		return true;
	}
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		gpsProcess.noteNewSettings();
		headingProcess.noteNewSettings();
	}
	public GPSParameters getGpsParameters() {
		return gpsParameters;
	}
	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#fillXMLParameters(org.w3c.dom.Document, org.w3c.dom.Element)
	 */
	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("Interval", "2");
		return true;
	}
}
