package Map;

import java.io.Serializable;
import java.util.ArrayList;

import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;

/**
 * Holds information about all things plotted on the map,
 * primarily how long they should plot for and whether they
 * are currently enabled or not. 
 * <br>
 * Will ty to have an ever increasing list of data blocks, identified 
 * by name. 
 * 
 * @author Douglas Gillespie
 *
 */
public class MapDetectionsManager implements PamSettings {

	private MapController mapControl;
	
	private MapDetectionsParameters mapDetectionsParameters = new MapDetectionsParameters();
	
	ArrayList<PamDataBlock> plottableBlocks = new ArrayList<PamDataBlock>();
	
	private int defaultTime;

	public MapDetectionsManager(MapController mapControl) {
		super();
		this.mapControl = mapControl;
		PamSettingManager.getInstance().registerSettings(this);
		
		defaultTime = mapControl.mapParameters.dataShowTime;
	}
	
	public void notifyModelChanged(int changeType) {
		
//		if (changeType == PamController.ADD_DATABLOCK || changeType == PamController.ADD_CONTROLLEDUNIT) {
//			addDataBlock();
//		}
//		if (changeType == PamController.ADD_DATABLOCK || changeType == PamController.REMOVE_DATABLOCK) {
			createBlockList();
//		}
		
	}
	
	/*
	 * See if we're already holding data about this data block 
	 * and add to list if not, with default values. 	 *
	 */
	private void createBlockList() {
		plottableBlocks = PamController.getInstance().
		getPlottableDataBlocks(mapControl.mapTabPanelControl.simpleMap.mapPanel.getRectProj()); 
		MapDetectionData mdd;
//		want to know which detectiondata are associated with data blocks and which 
//		are just left overs from previous existances. 
		for (int i = 0; i < mapDetectionsParameters.mapDetectionDatas.size(); i++) {
			mapDetectionsParameters.mapDetectionDatas.get(i).dataBlock = null;
		}
		for (int i = 0; i < plottableBlocks.size(); i++) {
			if ((mdd=findDetectionData(plottableBlocks.get(i))) == null) {
				addDataBlock(plottableBlocks.get(i));
			}
			else {
				mdd.dataBlock = plottableBlocks.get(i); 
			}
		}
	}

	
	private void addDataBlock(PamDataBlock pamDataBlock) {
		MapDetectionData mdd = new MapDetectionData();
		mdd.dataName = new String(pamDataBlock.getLongDataName());
		mdd.displaySeconds = mapControl.mapParameters.dataShowTime;
//		mdd.forcedStart = 0;
		mdd.shouldPlot = false;
		mdd.dataBlock = pamDataBlock;
		addDetectionData(mdd);
	}
	
	public MapDetectionData findDetectionData(PamDataBlock pamDataBlock) {
		return findDetectionData(pamDataBlock.getLongDataName());
	}
	
	public MapDetectionData findDetectionData(String dataName) {
		int n = mapDetectionsParameters.mapDetectionDatas.size();
		for (int i = 0; i < n; i++) {
			if (dataName.equals(mapDetectionsParameters.mapDetectionDatas.get(i).dataName)){
				return mapDetectionsParameters.mapDetectionDatas.get(i);
			}
		}
		return null;
	}
	
	private void addDetectionData(MapDetectionData mapDetectionData) {
		mapDetectionsParameters.mapDetectionDatas.add(mapDetectionData);
	}
	
	public ArrayList<MapDetectionData> getMapDetectionDatas() {
		return mapDetectionsParameters.mapDetectionDatas;
	}
	
	public void setShouldPlot(String pamDataBlock, boolean shouldPlot) {
		MapDetectionData mapDetectionData = findDetectionData(pamDataBlock);
		if (mapDetectionData == null) {
			return;
		}
		mapDetectionData.shouldPlot = shouldPlot;
	}
	
	public void setShouldPlot(PamDataBlock pamDataBlock, boolean shouldPlot) {
		setShouldPlot(pamDataBlock.getLongDataName(), shouldPlot);
	}
	
	public boolean isShouldPlot(String pamDataBlock) {
		MapDetectionData mapDetectionData = findDetectionData(pamDataBlock);
		if (mapDetectionData == null) {
			return false;
		}
		return mapDetectionData.shouldPlot;
	}
	
	public boolean isShouldPlot(PamDataBlock pamDataBlock) {
		return isShouldPlot(pamDataBlock.getLongDataName());
	}

	/**
	 * functions for storing of settings ... 
	 */
	
	public Serializable getSettingsReference() {
		return mapDetectionsParameters;
	}

	public long getSettingsVersion() {
		return MapDetectionsParameters.serialVersionUID;
	}

	public String getUnitName() {
		return mapControl.getUnitName();
	}

	public String getUnitType() {
		return "Map Detections Manager";
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		mapDetectionsParameters = ((MapDetectionsParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	public MapDetectionsParameters getMapDetectionsParameters() {
		return mapDetectionsParameters;
	}

	public void setMapDetectionsParameters(
			MapDetectionsParameters mapDetectionsParameters) {
		this.mapDetectionsParameters = mapDetectionsParameters;
	}

	public int getDefaultTime() {
		return defaultTime;
	}

	public void setDefaultTime(int defaultTime) {
		this.defaultTime = defaultTime;
	}
}
