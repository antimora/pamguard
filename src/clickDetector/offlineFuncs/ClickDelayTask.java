package clickDetector.offlineFuncs;

import Localiser.Correlations;
import Localiser.DelayMeasurementParams;
import Localiser.bearingLocaliser.DelayOptionsDialog;
import PamUtils.PamUtils;
import offlineProcessing.OfflineTask;
import clickDetector.ClickControl;
import clickDetector.ClickDetection;
import clickDetector.ClickDetector;
import clickDetector.ClickTabPanelControl;
import dataMap.OfflineDataMapPoint;

public class ClickDelayTask extends OfflineTask<ClickDetection> {

	private ClickControl clickControl;
	
	private ClickDetector clickDetector;

	private ClicksOffline clicksOffline;

	private DelayMeasurementParams delayMeasurementParams;

	private Correlations correlations;

	private int correlationLength;

	public ClickDelayTask(ClickControl clickControl) {
		this.clickControl = clickControl;
		clicksOffline = clickControl.getClicksOffline();
		clickDetector = clickControl.getClickDetector();
		setParentDataBlock(clickControl.getClickDataBlock());
		addAffectedDataBlock(clickControl.getClickDataBlock());
	}

	@Override
	public String getName() {
		return "Recalculate click delays";
	}

	@Override
	public void newDataLoad(long startTime, long endTime,
			OfflineDataMapPoint mapPoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean processDataUnit(ClickDetection click) {
		// need to look at options to see whether or not to filter the data
		// and whether or not to use the envelope.
		double[][] correlationSignals;
		if (delayMeasurementParams.envelopeBearings) {
			if (delayMeasurementParams.filterBearings) {
				correlationSignals = click.getFilteredAnalyticWaveform(delayMeasurementParams.delayFilterParams);
			}
			else {
				correlationSignals = click.getFilteredAnalyticWaveform(null);
			}
		}
		else {
			correlationSignals = click.getWaveData(delayMeasurementParams.filterBearings, 
					delayMeasurementParams.delayFilterParams);
		}
		int nChannels = correlationSignals.length;
		double delaySamples;
		int iD = 0;
		for (int i = 0; i < nChannels; i++) {
			for (int j = i+1; j < nChannels; j++) {
				delaySamples = correlations.getDelay(correlationSignals[i], correlationSignals[j], correlationLength);
				click.setDelay(iD++, delaySamples);
			}
		}
		click.freeClickMemory();
		return false;
	}

	@Override
	public void prepareTask() {
		delayMeasurementParams = clickControl.getClickParameters().delayMeasurementParams;
		if (delayMeasurementParams == null) {
			// use default set - will be no filtering and no envelope
			delayMeasurementParams = new DelayMeasurementParams();
		}
		correlations = clickDetector.getCorrelations();
		correlationLength = PamUtils.getMinFftLength(clickControl.getClickParameters().maxLength);
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
		DelayMeasurementParams newParams = DelayOptionsDialog.showDialog(clickControl.getPamView().getGuiFrame(), 
				clickControl.getClickParameters().delayMeasurementParams);
		if (newParams != null) {
			clickControl.getClickParameters().delayMeasurementParams = newParams.clone();
			return true;
		}
		return false;
	}


	@Override
	public boolean hasSettings() {
		return true;
	}

}
