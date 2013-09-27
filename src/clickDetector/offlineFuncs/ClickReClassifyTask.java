package clickDetector.offlineFuncs;

import clickDetector.ClickControl;
import clickDetector.ClickDetection;
import clickDetector.ClickTabPanelControl;
import clickDetector.ClickClassifiers.ClickIdInformation;
import clickDetector.ClickClassifiers.ClickIdentifier;
import dataMap.OfflineDataMapPoint;
import offlineProcessing.OfflineTask;


public class ClickReClassifyTask extends OfflineTask<ClickDetection> {

	private ClickControl clickControl;
	
	private ClicksOffline clicksOffline;

	private ClickIdentifier clickClassifier;
	
	/**
	 * @param clickControl
	 */
	public ClickReClassifyTask(ClickControl clickControl) {
		super();
		this.clickControl = clickControl;
		clicksOffline = clickControl.getClicksOffline();
		setParentDataBlock(clickControl.getClickDataBlock());
		addAffectedDataBlock(clickControl.getClickDataBlock());
	}

	@Override
	public String getName() {
		return "Reclassify Clicks";
	}

	@Override
	public void newDataLoad(long startTime, long endTime,
			OfflineDataMapPoint mapPoint) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean processDataUnit(ClickDetection click) {
		ClickIdInformation idInfo = clickClassifier.identify(click);
		click.freeClickMemory();
		if (idInfo.clickType != click.getClickType()) {
			click.setClickType((byte) idInfo.clickType);
//			click.getDataUnitFileInformation().setNeedsUpdate(true);
			return true;
		}
		return false;
	}

	@Override
	public void loadedDataComplete() {
		ClickTabPanelControl ctpc = (ClickTabPanelControl) clickControl.getTabPanel();
		if (ctpc != null) {
			ctpc.offlineDataChanged();
		}
	}


	@Override
	public boolean callSettings() {
		return clickControl.classificationDialog(clickControl.getPamView().getGuiFrame());
	}


	@Override
	public boolean hasSettings() {
		return true;
	}

	@Override
	public void prepareTask() {
		clickClassifier = clickControl.getClickIdentifier();
	}

}
