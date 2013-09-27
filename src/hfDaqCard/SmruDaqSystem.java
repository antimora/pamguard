package hfDaqCard;

import java.io.Serializable;
import java.util.List;

import javax.swing.JComponent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soundPlayback.PlaybackControl;
import soundPlayback.PlaybackSystem;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionDialog;
import Acquisition.DaqSystem;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;
import PamguardMVC.PamConstants;

public class SmruDaqSystem extends DaqSystem implements PamSettings {

	protected SmruDaqParameters smruDaqParameters = new SmruDaqParameters();

	private SmruDaqDialogPanel smruDaqDialogPanel;

	private AcquisitionControl daqControl;

	private SmruDaqJNI smruDaqJNI;

	List<RawDataUnit> newDataUnits;

	private int wantedSamples;

	/**
	 * @param daqControl
	 */
	public SmruDaqSystem(AcquisitionControl daqControl) {
		super();
		this.daqControl = daqControl;
		smruDaqDialogPanel = new SmruDaqDialogPanel(this);
		smruDaqJNI = new SmruDaqJNI(this);
		PamSettingManager.getInstance().registerSettings(this);
	}

	@Override
	public boolean canPlayBack(float sampleRate) {
		return true;
	}

	@Override
	public boolean dialogGetParams() {
		SmruDaqParameters newParams = smruDaqDialogPanel.getParams();
		if (newParams != null) {
			smruDaqParameters = newParams;
			return true;
		}
		return false;
	}

	@Override
	public void dialogSetParams() {
		smruDaqDialogPanel.setParams(smruDaqParameters);
	}

	@Override
	public JComponent getDaqSpecificDialogComponent(
			AcquisitionDialog acquisitionDialog) {
		smruDaqDialogPanel.setDaqDialog(acquisitionDialog);
		return smruDaqDialogPanel.getDialogPanel();
	}

	@Override
	public int getDataUnitSamples() {
		return wantedSamples;
	}

	@Override
	public String getDeviceName() {
		return "SMRU Ltd DAQ Card";
	}

	@Override
	public int getMaxChannels() {
		return DaqSystem.PARAMETER_FIXED;
	}

	@Override
	public int getMaxSampleRate() {
		return DaqSystem.PARAMETER_FIXED; // will disable the built in sampel rate data. 
	}

	@Override
	public double getPeak2PeakVoltage(int swChannel) {
		return SmruDaqParameters.VPEAKTOPEAK;
	}

	@Override
	public String getSystemName() {
		return "SMRU Ltd DAQ Card";
	}

	@Override
	public String getSystemType() {
		return "SMRU Ltd DAQ Card";
	}

	@Override
	public boolean isRealTime() {
		return true;
	}

	@Override
	public boolean prepareSystem(AcquisitionControl daqControl) {
		int errors = 0;

		wantedSamples = 1<<12;
		while (wantedSamples < daqControl.getAcquisitionParameters().sampleRate/10) {
			wantedSamples *= 2;
		}

		if (!smruDaqJNI.resetCard()) {
			errors ++;
		}

		if (!smruDaqJNI.setSampleRateIndex(smruDaqParameters.sampleRateIndex)) {
			errors ++;
		}

		if (!smruDaqJNI.setChannelMask(smruDaqParameters.channelMask)) {
			errors ++;
		}

		for (int i = 0; i < SmruDaqParameters.NCHANNELS; i++) {
			if (!smruDaqJNI.prepareChannel(i, smruDaqParameters.channelGainIndex[i], smruDaqParameters.channelFilterIndex[i])) {
				errors++;
			}
		}
		return (errors == 0);
	}

	public int toggleLED(int led) {
		return smruDaqJNI.toggleLED(led);
	}

	public int getLED(int led) {
		if (smruDaqJNI == null) {
			return -2;
		}
		return smruDaqJNI.getLED(led);
	}

	private volatile boolean keepRunning;
	private volatile boolean daqThreadRunning;

	@Override
	public boolean startSystem(AcquisitionControl daqControl) {
		keepRunning = true;
		daqThreadRunning = true;

		boolean ans = smruDaqJNI.startSystem();

		Thread t = new Thread(new DaqThread());
		t.start();

		return ans;
	}

	/**
	 * Runs in a separate thread during acquisition. 
	 */
	private void acquireData() {
		newDataUnits = daqControl.getDaqProcess().getNewDataUnits();
		double[] dcOffset = new double[PamConstants.MAX_CHANNELS];
		double dcOffsetScale = 100.;
		double dcTotal;
		short[] newData;
		int readSamples = 0;
		long dataMillis;
		int nChan = PamUtils.getNumChannels(this.smruDaqParameters.channelMask);
		int[] chans = PamUtils.getChannelArray(smruDaqParameters.channelMask);
		int bytesPerSample = 2;
		long totalSamples = 0;
		RawDataUnit rawDataUnit;
		double[] rawData;
		boolean first = true;
		while (keepRunning) {
			dataMillis = daqControl.getAcquisitionProcess().absSamplesToMilliseconds(totalSamples);
			for (int i = 0; i < nChan; i++) {
				newData = smruDaqJNI.readSamples(i, wantedSamples);
				readSamples = newData.length;
				if (newData == null) {
					break;
				}
				if (readSamples < wantedSamples) {
					System.out.println(String.format("Error reading channel %d got %d samples of %d requested",
							i, readSamples, wantedSamples));
				}
				dcTotal = 0;
				rawData = new double[readSamples];
				for (int s = 0; s < readSamples; s++) {
					rawData[s] = (newData[s]-dcOffset[i]) / 32768.;
					dcTotal += newData[s];
				}
				dcOffset[i] = (dcTotal/readSamples - dcOffset[i]) / dcOffsetScale;

				rawDataUnit = new RawDataUnit(dataMillis, 1 << i, totalSamples, readSamples);
				rawDataUnit.setRawData(rawData);
				newDataUnits.add(rawDataUnit);
				first = false;
			}


			totalSamples += readSamples;

		}
		daqThreadRunning = false;
	}

	public static short getSample(byte[] buffer, int position) {
		//		return (short) (((buffer[position] & 0xff) << 8) | (buffer[position + 1] & 0xff));
		return (short) (((buffer[position+1] & 0xff) << 8) | (buffer[position] & 0xff));
	}

	@Override
	public double getChannelGain(int channel) {
		return smruDaqParameters.getChannelGain(channel);
	}

	@Override
	public void stopSystem(AcquisitionControl daqControl) {
		keepRunning = false;
		try {
			while(daqThreadRunning) {
				Thread.sleep(2);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		smruDaqJNI.stopSystem();
	}

	@Override
	public void daqHasEnded() {
		smruDaqJNI.systemStopped();
	}

	@Override
	public Serializable getSettingsReference() {
		return smruDaqParameters;
	}

	@Override
	public long getSettingsVersion() {
		return SmruDaqParameters.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return daqControl.getUnitName() + " SMRU Daq Card";
	}

	@Override
	public String getUnitType() {
		return "SMRU Daq Card";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		smruDaqParameters = ((SmruDaqParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	class DaqThread implements Runnable {

		@Override
		public void run() {
			acquireData();
		}

	}

	@Override
	public boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("sampleRateIndex", String.format("%d",smruDaqParameters.sampleRateIndex));
		paramsEl.setAttribute("channelMask", String.format("%d",smruDaqParameters.channelMask));
		double[] daqGains = SmruDaqParameters.getGains();
		double[] filters = SmruDaqParameters.filters;
		for (int i = 0; i < SmruDaqParameters.NCHANNELS; i++) {
			Element el = doc.createElement("Chan"+i);
			el.setAttribute("channelGainIndex", String.format("%d",smruDaqParameters.channelGainIndex[i]));
			el.setAttribute("channelFilterIndex", String.format("%d",smruDaqParameters.channelFilterIndex[i]));
			el.setAttribute("channelGain", new Double(daqGains[smruDaqParameters.channelGainIndex[i]]).toString());
			el.setAttribute("channelFilter", new Double(filters[smruDaqParameters.channelFilterIndex[i]]).toString());
			paramsEl.appendChild(el);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see Acquisition.DaqSystem#getPlaybackSystem(soundPlayback.PlaybackControl, Acquisition.DaqSystem)
	 */
	@Override
	public PlaybackSystem getPlaybackSystem(PlaybackControl playbackControl,
			DaqSystem daqSystem) {
		return playbackControl.getFilePlayback();
	}

}
