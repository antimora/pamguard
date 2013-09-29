package clickDetector.offlineFuncs;

import clickDetector.ClickControl;
import clickDetector.ClickDetection;
import clickDetector.ClickTabPanelControl;
import clickDetector.ClickDetector.ChannelGroupDetector;
import Localiser.Correlations;
import Localiser.bearingLocaliser.BearingLocaliser;
import PamUtils.PamUtils;
import dataMap.OfflineDataMapPoint;
import offlineProcessing.OfflineTask;

public class ClickBearingTask extends OfflineTask<ClickDetection> {

	private ClickControl clickControl;
	
	private ClicksOffline clicksOffline;
	
	private BearingLocaliser bearingLocaliser;

	private double sampleRate;
	
	/**
	 * @param clickControl
	 */
	public ClickBearingTask(ClickControl clickControl) {
		super();
		this.clickControl = clickControl;
		clicksOffline = clickControl.getClicksOffline();
		setParentDataBlock(clickControl.getClickDataBlock());
		addAffectedDataBlock(clickControl.getClickDataBlock());
		prepareLocalisers();
	}
	
	private void prepareLocalisers() {
		int n = clickControl.getClickDetector().getnChannelGroups();
		ChannelGroupDetector gd;
		BearingLocaliser bl;
		int[] groupPhones;
		for (int i = 0; i < n; i++) {
			gd = clickControl.getClickDetector().getChannelGroupDetector(i);

			int[] phones = PamUtils.getChannelArray(gd.getGroupHydrophones());
			bl = gd.getBearingLocaliser();
			if (bl != null) {
				bl.prepare(phones, Correlations.defaultTimingError(clickControl.getClickDetector().getSampleRate()));
			}
		}
	}

	@Override
	public String getName() {
		return "Click bearings";
	}

	@Override
	public void newDataLoad(long startTime, long endTime,
			OfflineDataMapPoint mapPoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean processDataUnit(ClickDetection click) {

		double[][] angles = null;
		double[] delays = click.getDelays();
		/*
		 * Need to copy array otherwise we write over the old delays in the click 
		 * (which is disastrous !). 
		 */
		double[] delaySecs = new double[delays.length];
		for (int i = 0; i < delaySecs.length; i++) {
			delaySecs[i] = delays[i] / sampleRate;
		}
		bearingLocaliser = click.getChannelGroupDetector().bearingLocaliser;
		if (bearingLocaliser != null) {
			angles = bearingLocaliser.localise(delaySecs);
		}
		if (click.getClickLocalisation() != null) {
			click.getClickLocalisation().setAnglesAndErrors(angles);
		}
		return true;
	}
	
	@Override
	public void prepareTask() {
		sampleRate = clickControl.getClickDataBlock().getSampleRate();
	}

	@Override
	public void loadedDataComplete() {
		ClickTabPanelControl ctpc = (ClickTabPanelControl) clickControl.getTabPanel();
		if (ctpc != null) {
			ctpc.offlineDataChanged();
		}
	}

}
