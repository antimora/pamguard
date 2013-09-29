/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package fftManager;

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
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.RawDataUnit;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamRawDataBlock;

public class PamFFTControl extends PamControlledUnit implements PamSettings {

	protected PamFFTProcess fftProcess;

	protected FFTParameters fftParameters = new FFTParameters();

	private FFTPluginPanelProvider fFTPluginPanelProvider;

	private SpectrogramNoiseProcess spectrogramNoiseProcess;

	public PamFFTControl(String unitName) {
		super("FFT Engine", unitName);

		PamRawDataBlock rawDataBlock = PamController.getInstance().
		getRawDataBlock(fftParameters.dataSource);

		addPamProcess(fftProcess = new PamFFTProcess(this, rawDataBlock));

		spectrogramNoiseProcess = new SpectrogramNoiseProcess(this);
		addPamProcess(spectrogramNoiseProcess);

		/*
		 * provide plug in panels for the bottom of the spectrogram displays
		 * (and any future displays that support plug in panels)
		 */
		fFTPluginPanelProvider = new FFTPluginPanelProvider(this);

		PamSettingManager.getInstance().registerSettings(this);
		//		setOutputDataName();

	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {

		JMenuItem menuItem;
		menuItem = new JMenuItem(getUnitName() + " settings ...");
		menuItem.addActionListener(new FFTSettings(parentFrame));
		return menuItem;
	}

	class FFTSettings implements ActionListener {
		Frame parentFrame;

		public FFTSettings(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {
			showParamsDialog(parentFrame);
		}
	}

	private void showParamsDialog(Frame parentFrame) {
		FFTParameters newParameters = FFTParametersDialog
		.showDialog(parentFrame, fftParameters, spectrogramNoiseProcess);
		//		String oldSource = fftParameters.rawDataSource;
		if (newParameters != null) {
			fftParameters = newParameters.clone();
			if (fftProcess != null) {
				setupControlledUnit();
//				fftProcess.setupFFT();
//				spectrogramNoiseProcess.setNoiseSettings(fftParameters.spectrogramNoiseSettings);
//				PamController.getInstance().notifyModelChanged(PamControllerInterface.CHANGED_PROCESS_SETTINGS);
				//				setOutputDataName();
			}
		}
	}

	public FFTParameters getFftParameters() {
		return fftParameters;
	}

	public Serializable getSettingsReference() {
		fftParameters.spectrogramNoiseSettings = spectrogramNoiseProcess.getNoiseSettings();
		return fftParameters;
	}

	/**
	 * @return An integer version number for the settings
	 */
	public long getSettingsVersion() {
		return FFTParameters.serialVersionUID;
	}


	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		FFTParameters newParameters = (FFTParameters) pamControlledUnitSettings
		.getSettings();
		fftParameters = newParameters.clone();
		setupControlledUnit();
//		if (fftProcess != null) fftProcess.setupFFT();
		return true;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#notifyModelChanged(int)
	 */
	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamController.INITIALIZATION_COMPLETE:
			setupControlledUnit();
			break;
		}
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#SetupControlledUnit()
	 */
	@Override
	public void setupControlledUnit() {
		// if no source data block, set up on the first one. 
//		PamDataBlock rawBlock;
//		
//		if (fftParameters.dataSourceName != null) {
//			rawBlock = PamController.getInstance().getDataBlock(RawDataUnit.class, fftParameters.dataSourceName);
//		}
//		else {
//			rawBlock = PamController.getInstance().getRawDataBlock(fftParameters.dataSource);
//			if (rawBlock != null) {
//				fftParameters.dataSourceName = rawBlock.getDataName();
//			}
//		}
//		if (rawBlock == null) {
//			rawBlock = PamController.getInstance().getRawDataBlock(0);
//			//			if (rawBlock != null) fftParameters.rawDataSource = rawBlock.toString();
//		}
		fftProcess.setupFFT();
		// always force the noise denoiser to use the output data of fftProcess. 
		fftParameters.spectrogramNoiseSettings.dataSource = fftProcess.getOutputData().getDataName();
		spectrogramNoiseProcess.setNoiseSettings(fftParameters.spectrogramNoiseSettings);
		super.setupControlledUnit();
	}

	//	private void setOutputDataName() {
	//		fftProcess.getOutputData().setDataName(String.format("%s - %d pt FFT", getUnitName(), fftParameters.fftLength));
	//	}

	@Override
	public void rename(String newName) {

		super.rename(newName);

		//		setOutputDataName();
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		FFTParameters fP = fftParameters;
		paramsEl.setAttribute("FFTLength", String.format("%d",fP.fftLength));
		paramsEl.setAttribute("FFTHop", String.format("%d",fP.fftHop));
		paramsEl.setAttribute("ChannelMap", String.format("%d",fP.channelMap));
		paramsEl.setAttribute("WindowFunction", String.format("%d",fP.windowFunction));
		paramsEl.setAttribute("clickRemoval", new Boolean(fP.clickRemoval).toString());
		paramsEl.setAttribute("clickThreshold", new Double(fP.clickThreshold).toString());
		paramsEl.setAttribute("clickPower", new Integer(fP.clickPower).toString());
//		SpectrogramNoiseSettings ns = fP.spectrogramNoiseSettings;
//		if (ns != null) {
//			Element n = doc.createElement("NoiseRemoval");
//			spectrogramNoiseProcess.fillXMLParameters(doc, n);
//			paramsEl.appendChild(n);
//		}
		return true;
	}

}
