package simulatedAcquisition;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import javax.swing.JComponent;

import propagation.PropagationModel;
import propagation.SphericalPropagation;
import propagation.SurfaceEcho;


import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionDialog;
import Acquisition.DaqSystem;
import Array.ArrayManager;
import Array.PamArray;
import Map.MapController;
import Map.MapPanel;
import Map.MapRectProjector;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.RawDataUnit;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;

/**
 * Simulation of simulated sound. 
 * <p>Sound simulated on a time at approximately 10x real time
 * <p>Slow it down with a sound playback module if needs be. 
 * <p>
 * Controls creation and movement of simulated objects
 * @author Doug Gillespie
 *
 */
public class SimProcess extends DaqSystem implements PamSettings {

	//	private SimControl simControl;

	//	private PamRawDataBlock outputData;
	private AcquisitionControl daqControl;

	private SimObjectsDataBlock simObjectsDataBlock;

	private PamArray currentArray;

	private Random random = new Random();

	private long totalSamples;

	private long startTimeMillis;

	SimParameters simParameters = new SimParameters();

	private SimDialogPanel simdialogPanel;

	private SimGraphics simGraphics;

	private volatile boolean dontStop;

	private volatile boolean stillRunning;

	private GenerationThread genThread;

	private Thread theThread;

	protected List<RawDataUnit> newDataUnits;

	private SimMouseAdapter simMouseAdapter;

	protected SimSignals simSignals;
	
	protected ArrayList<PropagationModel> propagationModels = new ArrayList<PropagationModel>();
	
	private PropagationModel propagationModel;

	public SimProcess(AcquisitionControl daqControl) {
		this.daqControl = daqControl;
		setArray();
		propagationModels.add(new SphericalPropagation());
		propagationModels.add(new SurfaceEcho(new SphericalPropagation()));
		simSignals = new SimSignals(this);
		simdialogPanel = new SimDialogPanel(null, this);
		PamSettingManager.getInstance().registerSettings(this);
		simMouseAdapter = new SimMouseAdapter();
		sortMapMice();
		setupObjects();
		setupSim();
	}

	private void sortMapMice() {
		if (simMouseAdapter == null) {
			return;
		}
		PamControlledUnit pamControlledUnit;
		MapController mapController;
		int n = PamController.getInstance().getNumControlledUnits();
		for (int i = 0; i < n; i++) {
			pamControlledUnit = PamController.getInstance().getControlledUnit(i);
			if (pamControlledUnit.getClass() == MapController.class) {
				mapController = (MapController) pamControlledUnit;
				mapController.addMouseAdapterToMapPanel(simMouseAdapter);
			}
		}
	}

	private boolean wasSelected;

	private int dataUnitSamples;
	
	@Override
	public void setSelected(boolean select) {
		if (simObjectsDataBlock == null) {
			simObjectsDataBlock = new SimObjectsDataBlock(daqControl.getAcquisitionProcess());
			simObjectsDataBlock.setOverlayDraw(simGraphics = new SimGraphics());
		}
		if (wasSelected && select == false) {
			daqControl.getAcquisitionProcess().removeOutputDatablock(simObjectsDataBlock);
			//			PamController.getInstance().notifyModelChanged(PamControllerInterface.REMOVE_DATABLOCK);
		}
		else if (wasSelected == false && select) {
			daqControl.getAcquisitionProcess().addOutputDataBlock(simObjectsDataBlock);
			setupObjects();
			//			PamController.getInstance().notifyModelChanged(PamControllerInterface.ADD_DATABLOCK);
		}
		wasSelected = select;
	}

	protected void setupObjects() {
		if (simObjectsDataBlock == null) {
			return;
		}
		simObjectsDataBlock.clearOldData();
		int n = simParameters.getNumObjects();
		SimObject s;
		SimObjectDataUnit sdu;
		for (int i = 0; i < n; i++) {
			s = simParameters.getObject(i);
			sdu = new SimObjectDataUnit(this, s, 1000);
			if (sdu.currentPosition == null) {
				sdu.currentPosition = s.startPosition.clone();
			}
			sdu.lastUpdateTime = 0;
			simObjectsDataBlock.addPamData(sdu);
			sdu.getSimSignal().setSampleRate(daqControl.acquisitionParameters.sampleRate);
		}
	}

	protected void setupSim() {
		propagationModel = getPropagationModel(simParameters.propagationModel);
		//		outputData.setSampleRate(simControl.simParameters.sampleRate, true);
		//		super.setSampleRate(simControl.simParameters.sampleRate, false);
	}

	public PropagationModel getPropagationModel() {
		if (propagationModel == null) {
			return getPropagationModel(simParameters.propagationModel);
		}
		return propagationModel;
	}
	
	public PropagationModel getPropagationModel(String propName) {
		for (int i = 0; i < propagationModels.size(); i++) {
			if (propagationModels.get(i).getName().equals(propName)) {
				return propagationModels.get(i);
			}
		}
		if (propagationModel == null) {
			propagationModel = propagationModels.get(0);
		}
		return propagationModel;
	}

	public void setPropagationModel(PropagationModel propagationModel) {
		this.propagationModel = propagationModel;
		if (propagationModel != null) {
			simParameters.propagationModel = propagationModel.getName();
		}
	}
	

	private void updateObjectPositions(long timeMilliseconds) {
		int n = simObjectsDataBlock.getUnitsCount();
		SimObjectDataUnit sdu;
		ListIterator<SimObjectDataUnit> li = simObjectsDataBlock.getListIterator(0);
		while (li.hasNext()) {
			sdu = li.next();
			updateObjectPosition(timeMilliseconds, sdu);
		}
	}
	private void updateObjectPosition(long timeMilliseconds, SimObjectDataUnit simObjectDataUnit) {
		SimObject s = simObjectDataUnit.getSimObject();
		if (simObjectDataUnit.lastUpdateTime == 0) {
			simObjectDataUnit.lastUpdateTime = timeMilliseconds;
			return;
		}
		long updateInterval = timeMilliseconds - simObjectDataUnit.lastUpdateTime;
		double dist = (double) updateInterval / 1000 * s.speed;
		simObjectDataUnit.currentPosition = simObjectDataUnit.currentPosition.travelDistanceMeters(s.course, dist);
		simObjectsDataBlock.updatePamData(simObjectDataUnit, timeMilliseconds);
	}

	//	@Override
	//	public void masterClockUpdate(long timeMilliseconds, long sampleNumber) {
	//		super.masterClockUpdate(timeMilliseconds, sampleNumber);
	//		updateObjectPositions(timeMilliseconds);
	//	}

	public void notifyArrayChanged() {
		setArray();
	}

	public float getSampleRate() {
		return daqControl.getAcquisitionParameters().sampleRate;
	}

	private void setArray() {
		currentArray = ArrayManager.getArrayManager().getCurrentArray();
		int nChan = currentArray.getHydrophoneCount();
		//		outputData.setChannelMap(PamUtils.makeChannelMap(nChan));
	}

	/**
	 * Generates data as fast as it can. If
	 * we want to slow it down, then it's necessary 
	 * to add a sound playback module or something.
	 * @author Doug
	 *
	 */
	class GenerationThread implements Runnable {

		@Override
		public void run() {
			stillRunning = true;
			while (dontStop) {
				generateData();
				/*
				 * this is the point we wait at for the other thread to
				 * get it's act together on a timer and use this data
				 * unit, then set it's reference to zero.
				 */
				while (newDataUnits.size() > daqControl.acquisitionParameters.nChannels*2) {
					if (dontStop == false) break;
					try {
						Thread.sleep(2);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			stillRunning = false;
		}

	}

	/**
	 * Main generation function, called from within a continuous
	 * loop. Generates random data for each channel for each data unit
	 * and then overlays the individual objects data on top of each block of 
	 * raw data. 
	 */
	private void generateData() {
		RawDataUnit rdu;
		int nChan = daqControl.acquisitionParameters.nChannels;
		int phone;
		int nSource = simParameters.getNumObjects();
		int nSamples = (int) daqControl.acquisitionParameters.sampleRate / 10;
		double nse = Math.pow(10., simParameters.backgroundNoise / 20);
		double[] channelData;
		long currentTimeMillis = startTimeMillis + totalSamples / 1000;
		SimObject simObject;
		for (int i = 0; i < nChan; i++) {
			channelData = new double[nSamples];
			// simulate the noise
			double dbNse = simParameters.backgroundNoise + 10*Math.log10(getSampleRate());
			nse = daqControl.getDaqProcess().dbMicropascalToSignal(i, dbNse);
			generateNoise(channelData, nse);
			phone = daqControl.acquisitionParameters.getHydrophone(i);
			// then add the simulated data for each object.
			for (int o = 0; o < nSource; o++) {
				simObject = simParameters.getObject(o);
				generateSignals(simObject.simObjectDataUnit, channelData,
						phone, totalSamples);
			}
			// then create a dataunit
			rdu = new RawDataUnit(currentTimeMillis,1<<i,totalSamples,nSamples);
			rdu.setRawData(channelData, true);
			newDataUnits.add(rdu);
		}
		//				PamCalendar.set(currentTimeMillis);
		PamCalendar.setSoundFileTimeInMillis(totalSamples * 1000 / (int)getSampleRate());
		updateObjectPositions(currentTimeMillis);
		//		outputData.masterClockUpdate(currentTimeMillis, totalSamples);
		//		masterClockUpdate(currentTimeMillis, totalSamples);
		totalSamples += nSamples;
		for (int o = 0; o < nSource; o++) {
			simObject = simParameters.getObject(o);
			simObject.simObjectDataUnit.clearOldSounds(PamUtils.
					makeChannelMap(daqControl.acquisitionParameters.nChannels));
		}
	}
	
	/**
	 * Adds data from a single simulated object to a block of raw data. 
	 * @param sdu simulated object
	 * @param data raw data to add to 
	 * @param phone hydrophone number
	 * @param startSample start sample of data[]
	 */
	private void generateSignals(SimObjectDataUnit sdu, double[] data, int phone, long startSample) {
		sdu.takeSignals(data, phone, startSample);
	}

	/**
	 * Generate random noise on a data channel
	 * @param data data array to fill
	 * @param noise noise level 
	 */
	private void generateNoise(double[] data, double noise) {
		for (int i = 0; i < data.length; i++) {
			data[i] = random.nextGaussian() * noise;
		}
	}

	@Override
	public boolean canPlayBack(float sampleRate) {
		return true;
	}

	@Override
	public void daqHasEnded() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean dialogGetParams() {
		boolean ok = simdialogPanel.getParams();
		if (ok) {
			setupObjects();
		}
		return ok;
	}

	@Override
	public void dialogSetParams() {
		simdialogPanel.setParams(simParameters);
	}

	@Override
	public JComponent getDaqSpecificDialogComponent(
			AcquisitionDialog acquisitionDialog) {
		return simdialogPanel.dialogPanel;
	}

	@Override
	public int getMaxChannels() {
		return 0;
	}

	@Override
	public int getMaxSampleRate() {
		return 0;
	}

	@Override
	public double getPeak2PeakVoltage(int swChannel) {
		return DaqSystem.PARAMETER_UNKNOWN;
	}

	@Override
	public String getSystemName() {
		return "Simulated Sources";
	}

	@Override
	public String getSystemType() {
		// TODO Auto-generated method stub
		return "Simulated Sources";
	}

	@Override
	public boolean isRealTime() {
		return false;
	}

	@Override
	public boolean prepareSystem(AcquisitionControl daqControl) {
		newDataUnits = daqControl.getDaqProcess().getNewDataUnits();
		genThread = new GenerationThread();
		theThread = new Thread(genThread);
		startTimeMillis = System.currentTimeMillis();
		totalSamples = 0;
		dataUnitSamples = (int) (daqControl.acquisitionParameters.sampleRate/10);
		setupSim();
		PamCalendar.setSoundFile(true);
		PamCalendar.setSoundFileTimeInMillis(0);
		PamCalendar.setSessionStartTime(startTimeMillis);
		for (int i = 0; i < simParameters.getNumObjects(); i++) {
			simParameters.getObject(i).simObjectDataUnit.prepareSimulation();
		}
		return true;
	}

	@Override
	public int getDataUnitSamples() {
		return dataUnitSamples;
	}


	@Override
	public boolean startSystem(AcquisitionControl daqControl) {

		dontStop = true;

		theThread.start();

		setStreamStatus(STREAM_RUNNING);

		return true;
	}

	@Override
	public void stopSystem(AcquisitionControl daqControl) {

		dontStop = false;

	}

	@Override
	public Serializable getSettingsReference() {
		return simParameters;
	}

	@Override
	public long getSettingsVersion() {
		return SimParameters.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return "Simulated Data Sources";
	}

	@Override
	public String getUnitType() {
		return "Simulated Data DAQ";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		simParameters = ((SimParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;		
	}

	private class SimMouseAdapter extends MouseAdapter {
		private Point mouseClickPoint;
		private MapPanel mapPanel;
		private SimObjectDataUnit sodu;
		MapRectProjector mapProjector;

		@Override
		public void mousePressed(MouseEvent mouseEvent) {
			super.mouseClicked(mouseEvent);
			mapPanel = (MapPanel) mouseEvent.getSource();
			mapProjector = mapPanel.getRectProj();
			PamDataUnit dataUnit = mapProjector.getHoveredDataUnit();
			if (dataUnit == null) {
				return;
			}
			else if (dataUnit.getClass() == SimObjectDataUnit.class) {
				sodu = (SimObjectDataUnit) dataUnit;
			}
			mouseClickPoint = mouseEvent.getPoint();
		}

		@Override
		public void mouseDragged(MouseEvent mouseEvent) {
			if (sodu == null) {
				return;
			}
			mouseClickPoint = mouseEvent.getPoint();
			LatLong latLong = mapProjector.panel2LL(new Coordinate3d(mouseClickPoint.getX(), 
					mouseClickPoint.getY(), 0.0));
			SimObject so = sodu.getSimObject();
			sodu.currentPosition = latLong;
			simObjectsDataBlock.updatePamData(sodu, PamCalendar.getTimeInMillis());
		}

		@Override
		public void mouseReleased(MouseEvent mouseEvent) {
			if (sodu == null) {
				return;
			}
			SimObject so = sodu.getSimObject();
			so.startPosition = sodu.currentPosition.clone();
			sodu.lastUpdateTime = 0;
			sodu = null;
		}

	}



	public AcquisitionControl getDaqControl() {
		return daqControl;
	}

	@Override
	public String getDeviceName() {
		return null;
	}
}
