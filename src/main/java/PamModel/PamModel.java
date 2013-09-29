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
package PamModel;

import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JFrame;

import offlineProcessing.OfflineProcessingControlledUnit;

import clickDetector.ClickDetection;

import whistlesAndMoans.AbstractWhistleDataUnit;


import echoDetector.EchoDataUnit;
import fftManager.FFTDataUnit;

import Array.ArrayManager;
import Array.PamArray;
import GPS.GpsDataUnit;
import NMEA.NMEADataUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamguardVersionInfo;
import PamDetection.RawDataUnit;
import PamguardMVC.PamDataBlock;

/**
 * @author Doug Gillespie
 * 
 * Simple creation of a PAM model, but using the correct interface for the
 * PamController.
 * 
 */
final public class PamModel implements PamModelInterface, PamSettings {

	private PamController pamController;

	private PamDataBlock gpsDataBlock;

	private DependencyManager dependencyManager;

	private PamModelSettings pamModelSettings = new PamModelSettings();

	/**
	 * @param pamController
	 *            Needs to be parsed a valid reference to a PamController
	 */
	public PamModel(PamController pamController) {
		this.pamController = pamController;
		pamModel = this;
		createPamModel();
	}

	static PamModel pamModel = null;

	public static PamModel getPamModel() {
		return pamModel;
	}

	public PamDataBlock<GpsDataUnit> getGpsDataBlock() {
		/*
		 * If it's a fixed array then don't get this data block, but
		 * get one that's sitting in the ArrayManager and return that 
		 * instead
		 */
		PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
		if (currentArray != null && currentArray.getArrayType() == PamArray.ARRAY_TYPE_STATIC) {
			return currentArray.getFixedPointReferenceBlock();
		}
		// otherwise, just return the normal gps data block that was set from the NMEA module
		return gpsDataBlock;
	}

	public void setGpsDataBlock(PamDataBlock gpsDataBlock) {
		this.gpsDataBlock = gpsDataBlock;
	}


	/**
	 * Creates a list of available Pamguard modules and sets dependencies between them
	 * <p>
	 * Also sets grouping which are used for menu construction and the minimum and 
	 * maximum numbers of each type of module that may get created. 
	 */
	protected void createPamModel() {


		/*
		 * Make a series of module menu groups and add most of the 
		 * modules to one group or another
		 */
		ModulesMenuGroup mapsGroup = new ModulesMenuGroup("Maps and Mapping");
		ModulesMenuGroup processingGroup = new ModulesMenuGroup("Sound Processing");
		ModulesMenuGroup detectorsGroup = new ModulesMenuGroup("Detectors");
		ModulesMenuGroup classifierGroup = new ModulesMenuGroup("Classifiers");
		ModulesMenuGroup localiserGroup = new ModulesMenuGroup("Localisers");
		ModulesMenuGroup displaysGroup = new ModulesMenuGroup("Displays");
		ModulesMenuGroup utilitiesGroup = new ModulesMenuGroup("Utilities");
		ModulesMenuGroup visualGroup = new ModulesMenuGroup("Visual Methods");
		//		ModulesMenuGroup smruGroup = new ModulesMenuGroup("SMRU Stuff");		
		dependencyManager = new DependencyManager(this);

		boolean isViewer = (pamController.getRunMode() == PamController.RUN_PAMVIEW);
		boolean isSMRU = PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU;

		PamModuleInfo mi;

		/*
		 * ************* Start Maps and Mapping Group *******************
		 */
		/*
		 * Changed to allow any number, DG 13 March 2008
		 * This cooincides with changes to AIS and GPS modules
		 * which allow them to select an NMEA source. 
		 * These changes mean you can have GPS data and AIS data comign in 
		 * over separate serial ports.
		 */
		mi = PamModuleInfo.registerControlledUnit("NMEA.NMEAControl", "NMEA Data Collection");
		mi.setModulesMenuGroup(mapsGroup);
		mi.setToolTipText("Collects NMEA data from a serial port");
		mi.setMinNumber(0);

		mi = PamModuleInfo.registerControlledUnit("GPS.GPSControl", "GPS Processing");
		mi.addDependency(new PamDependency(NMEADataUnit.class, "NMEA.NMEAControl"));
		mi.setModulesMenuGroup(mapsGroup);
		mi.setToolTipText("Interprets NMEA data to extrct GPS data");
		mi.setMinNumber(0);
		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("AIS.AISControl", "AIS Processing");
		mi.addDependency(new PamDependency(NMEADataUnit.class, "NMEA.NMEAControl"));
		mi.setToolTipText("Interprets NMEA data to extract AIS data");
		mi.setModulesMenuGroup(mapsGroup);
		mi.setMinNumber(0);
		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("AirgunDisplay.AirgunControl", "Airgun Display");
		mi.setModulesMenuGroup(mapsGroup);
		mi.setToolTipText("Shows the position of airguns (or any other source) on the map");
		mi.setMinNumber(0);
		//		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("Map.MapController", "Map");	
		mi.addDependency(new PamDependency(GpsDataUnit.class, "GPS.GPSControl"));
		mi.setToolTipText("Displays a map of vessel position and detections");
		mi.setModulesMenuGroup(mapsGroup);
		mi.setMinNumber(0);
		//		mi.setMaxNumber(1);
		
        mi = PamModuleInfo.registerControlledUnit("WILDInterface.WILDControl", "WILD ArcGIS Interface");
        mi.setModulesMenuGroup(mapsGroup);
		mi.setToolTipText("Outputs data in an NMEA string via a serial port");
        mi.setMaxNumber(1);
		
//		mi = PamModuleInfo.registerControlledUnit("Map3D.Map3DControl", "3D Map");	
//		//mi.addDependency(new PamDependency(GpsDataUnit.class, "GPS.GPSControl"));
//		mi.setModulesMenuGroup(mapsGroup);
//		mi.setMinNumber(0);
//		mi.setMaxNumber(1);
	

		/*
		 * ************* End Maps and Mapping Group *******************
		 */

		/*
		 * ************* Start Utilities Group *******************
		 */

		mi = PamModuleInfo.registerControlledUnit("generalDatabase.DBControlUnit", "Database");
		mi.setModulesMenuGroup(utilitiesGroup);
		mi.setToolTipText("Stores PAMGuard data in a database");
		if (isViewer) {
			mi.setMinNumber(1);
		}
		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("binaryFileStorage.BinaryStore", "Binary Storage");
		mi.setModulesMenuGroup(utilitiesGroup);
		mi.setToolTipText("Stores PAMGuard data in files on the hard drive");
//		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
//			mi.setMinNumber(1);
//		}
		mi.setMaxNumber(1);


		if (isSMRU) {
			mi = PamModuleInfo.registerControlledUnit("networkTransfer.send.NetworkSender", "Network Sender");
			mi.setModulesMenuGroup(utilitiesGroup);
			mi.setToolTipText("Sends PAMGuard data over a network to other computers");
//			mi.setMaxNumber(1);

			if (pamController.getRunMode() == PamController.RUN_NETWORKRECEIVER) {
				mi = PamModuleInfo.registerControlledUnit("networkTransfer.receive.NetworkReceiver", "Network Receiver");
				mi.setModulesMenuGroup(utilitiesGroup);
				mi.setToolTipText("Receives PAMGuard data sent over the network from the Network Sender module");
				mi.setMaxNumber(1);
				mi.setMinNumber(1);
			}
		}

		if (isViewer) {
			mi = PamModuleInfo.registerControlledUnit("dataMap.DataMapControl", "Data Map");
			mi.setModulesMenuGroup(utilitiesGroup);
			mi.setToolTipText("Shows a summary of data density over time for large datasets");
			mi.setMinNumber(1);
			mi.setMaxNumber(1);
		}

		mi = PamModuleInfo.registerControlledUnit("UserInput.UserInputController", "User input");	
		mi.setModulesMenuGroup(utilitiesGroup);
		mi.setToolTipText("Creates a form for the user to type comments into");
		mi.setMinNumber(0);
		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("depthReadout.DepthControl", "Hydrophone Depth Readout");
		mi.setModulesMenuGroup(utilitiesGroup);
		mi.setToolTipText("Reads and displays hydrophone depth information");
		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("listening.ListeningControl", "Aural Listening Form");
		mi.setToolTipText("Creates a form for the user to manually log things they hear");
		mi.setModulesMenuGroup(utilitiesGroup);		


		if (isViewer && isSMRU) {
			mi = PamModuleInfo.registerControlledUnit("xBatLogViewer.XBatLogControl", "XBat Log Viewer");
			mi.setToolTipText("Displays converted xBat log files");
			mi.setModulesMenuGroup(utilitiesGroup);
			
			mi = PamModuleInfo.registerControlledUnit("offlineProcessing.OfflineProcessingControlledUnit", "Offline Processing");
			mi.setModulesMenuGroup(utilitiesGroup);
			mi.setMinNumber(0);
			mi.setMaxNumber(1);
		}
		
		if (isSMRU) {
			mi = PamModuleInfo.registerControlledUnit("alarm.AlarmControl", "Alarm");
			mi.setToolTipText("Alerts the operator when certain detections are made");
			mi.setModulesMenuGroup(utilitiesGroup);
		}

		/*
		 * ************* End Utilities  Group *******************
		 */

		/*
		 * ************* Start Displays  Group *******************
		 */

		mi = PamModuleInfo.registerControlledUnit("userDisplay.UserDisplayControl", "User Display");
		mi.setToolTipText("Creates an empty display panel which the user can add spectrograms and other displays to");		
		mi.setModulesMenuGroup(displaysGroup);

		mi = PamModuleInfo.registerControlledUnit("localTime.LocalTime", "Local Time");		
		mi.setToolTipText("Shows local time on the display");
		mi.setModulesMenuGroup(displaysGroup);

		/*
		 * ************* End Displays Group *******************
		 */

		/*
		 * ************* Start Sound Processing  Group *******************
		 */

		mi = PamModuleInfo.registerControlledUnit("Acquisition.AcquisitionControl", "Sound Acquisition");	
		mi.setToolTipText("Controls input of sound data from sound cards, NI cards, etc. ");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("soundPlayback.PlaybackControl", "Sound Output");	
		mi.setToolTipText("Controls output of sound data for listening to on headphones");
		mi.setModulesMenuGroup(processingGroup);
		if (isViewer) {
			mi.setMinNumber(1);
			mi.setMaxNumber(1);
		}

		mi = PamModuleInfo.registerControlledUnit("fftManager.PamFFTControl", "FFT (Spectrogram) Engine");
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Computes spectrograms of audio data");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("Filters.FilterControl", 
				isSMRU ? "Filters (IIR and FIR)" : "IIR Filters");
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Filters audio data");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("decimator.DecimatorControl", "Decimator");	
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Decimates (reduces the frequency of) audio data");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("SoundRecorder.RecorderControl", "Sound recorder");	
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Records audio data to wav of AIF files");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("clipgenerator.ClipControl", "Clip generator");	
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Generates and stores short clips of sound data in response to detections");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("amplifier.AmpControl", "Signal Amplifier");	
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Amplifies (or attenuates) audio data");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("patchPanel.PatchPanelControl", "Patch Panel");	
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Reorganises and mixes audio data between channels");
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("KernelSmoothing.KernelSmoothingControl", "Spectrogram smoothing kernel");	
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		mi.setToolTipText("Smooths a spectrogram of audio data");
		mi.setModulesMenuGroup(processingGroup);

		//		mi = PamModuleInfo.registerControlledUnit("spectrogramNoiseReduction.SpectrogramNoiseControl", "Spectrogram noise reduction");	
		//		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		//		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("seismicVeto.VetoController", "Seismic Veto");
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));	
		mi.setToolTipText("Cuts out loud sounds from audio data");	
		mi.setModulesMenuGroup(processingGroup);

		mi = PamModuleInfo.registerControlledUnit("noiseMonitor.NoiseControl", "Noise Monitor");
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));
		mi.setToolTipText("Measures noise in predefined frequency bands (e.g. thrid octave)");
		mi.setModulesMenuGroup(processingGroup);

		if (isSMRU) {
			mi = PamModuleInfo.registerControlledUnit("noiseBandMonitor.NoiseBandControl", "Noise Band Monitor");
			mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));
			mi.setToolTipText("");
			mi.setModulesMenuGroup(processingGroup);

			mi = PamModuleInfo.registerControlledUnit("dbht.DbHtControl", "dBHt Measurement");
			mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));
			mi.setToolTipText("");		
			mi.setModulesMenuGroup(processingGroup);

			mi = PamModuleInfo.registerControlledUnit("envelopeTracer.EnvelopeControl", "Envelope Tracing");
			mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
			mi.setToolTipText("");
			mi.setModulesMenuGroup(processingGroup);

			mi = PamModuleInfo.registerControlledUnit("ltsa.LtsaControl", "Long term spectral average");
			mi.addDependency(new PamDependency(RawDataUnit.class, "fftManager.PamFFTControl"));	
			mi.setToolTipText("");
			mi.setModulesMenuGroup(processingGroup);
		}
		/*
		 * ************* End Sound Processing Group *******************
		 */

		/*
		 * ************* Start Detectors Group *******************
		 */

		mi = PamModuleInfo.registerControlledUnit("clickDetector.ClickControl", "Click Detector");
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
		mi.setToolTipText("Searches for transient sounds, attempts to assign species, measure bearings to source, group into click trains, etc.");
		mi.setModulesMenuGroup(detectorsGroup);

		mi = PamModuleInfo.registerControlledUnit("whistlesAndMoans.WhistleMoanControl", 
		"Whistle and Moan Detector");	
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		mi.setToolTipText("Searches for tonal noises. Measures bearings and locations of source. Replaces older Whistle Detector");
		mi.setModulesMenuGroup(detectorsGroup);

		mi = PamModuleInfo.registerControlledUnit("whistleDetector.WhistleControl", "Whistle Detector");	
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		mi.setToolTipText("Searches for tonal noises. Measures bearings and locations of source");
		mi.setModulesMenuGroup(detectorsGroup);

		mi = PamModuleInfo.registerControlledUnit("IshmaelDetector.EnergySumControl", "Ishmael energy sum");
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		mi.setModulesMenuGroup(detectorsGroup);
		mi.setToolTipText("Detects sounds with energy in a specific frequency band");

		mi = PamModuleInfo.registerControlledUnit("IshmaelDetector.SgramCorrControl", "Ishmael spectrogram correlation");
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		mi.setToolTipText("Detects sounds matching a user defined 'shape' on a spectrogram");
		mi.setModulesMenuGroup(detectorsGroup);

		mi = PamModuleInfo.registerControlledUnit("IshmaelDetector.MatchFiltControl", "Ishmael matched filtering");	
		mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));
		mi.setToolTipText("Detects sounds using a user defined matched filter");	
		mi.setModulesMenuGroup(detectorsGroup);


		mi = PamModuleInfo.registerControlledUnit("likelihoodDetectionModule.LikelihoodDetectionUnit", "Likelihood Detector" );
		mi.addDependency( new PamDependency( RawDataUnit.class, "Acquisition.AcquisitionControl" ) );
		mi.setToolTipText("An implementation of a likelihood ratio test");
		mi.setModulesMenuGroup(detectorsGroup);



		if (isSMRU) {
			mi = PamModuleInfo.registerControlledUnit("RightWhaleEdgeDetector.RWEControl", "Right Whale Edge Detector");
			mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));
			mi.setToolTipText("Detects right whale upsweep calls");
			mi.setModulesMenuGroup(detectorsGroup);	
		}

		mi = PamModuleInfo.registerControlledUnit("WorkshopDemo.WorkshopController", "Workshop Demo Detector");
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));
		mi.setToolTipText("Simple demo detector for programmers");
		mi.setModulesMenuGroup(detectorsGroup);	


		//		mi = PamModuleInfo.registerControlledUnit("EdgeDetector.EdgeControl", "Edge Detector");		
		//		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));		
		//		mi.setModulesMenuGroup(detectorsGroup);

		if (isSMRU) {
			// two modules from Brian Miller ...
			mi = PamModuleInfo.registerControlledUnit("echoDetector.EchoController", "Echo Detector" );
			mi.addDependency( new PamDependency( ClickDetection.class, "clickDetector.ClickControl" ) );
			mi.setToolTipText("Detects echos from the click detector");
			mi.setModulesMenuGroup(detectorsGroup);

			mi = PamModuleInfo.registerControlledUnit("ipiDemo.IpiController", "Sperm whale IPI computation" );
			mi.addDependency( new PamDependency( EchoDataUnit.class, "echoDetector.EchoController" ) );
			mi.setToolTipText("Measures inter pulse interval from the click detector");
			mi.setModulesMenuGroup(detectorsGroup);
		}

		/*
		 * ************* End Detectors Group *******************
		 */
		
		/*
		 * ************* Start Classifiers Group **************
		 * 
		 */
		mi = PamModuleInfo.registerControlledUnit("whistleClassifier.WhistleClassifierControl", "Whistle Classifier");	
		mi.addDependency(new PamDependency(AbstractWhistleDataUnit.class, "whistlesAndMoans.WhistleMoanControl"));	
		mi.setToolTipText("Analyses multiple whistle contours to assign to species");
		mi.setModulesMenuGroup(classifierGroup);
		
		mi = PamModuleInfo.registerControlledUnit("rocca.RoccaControl", "Rocca");
		mi.addDependency(new PamDependency(FFTDataUnit.class, "fftManager.PamFFTControl"));	
		mi.setToolTipText("Classifies dolphin whistles selected from the spectrogram display");
		mi.setToolTipText("");
		mi.setModulesMenuGroup(classifierGroup);

		/*
		 * ************* End Classifiers Group *******************
		 */
		
		/*
		 * ************* Start Localisation Group **************
		 * 
		 */
		mi = PamModuleInfo.registerControlledUnit("IshmaelLocator.IshLocControl", "Ishmael Locator");	
		mi.setModulesMenuGroup(localiserGroup);
		mi.setToolTipText("Locates sounds extracted either from areas marked out on a spectrogram display or using output from a detector");
		
		mi = PamModuleInfo.registerControlledUnit("loc3d_Thode.TowedArray3DController", "3D localizer");
		mi.addDependency(new PamDependency(ClickDetection.class, "clickDetector.ClickControl"));
		mi.setModulesMenuGroup(localiserGroup);
		mi.setToolTipText("Locates sounds detected by the click detector using surface echo's to obtain slant angles and generate a 3-D location");
		
//			mi = PamModuleInfo.registerControlledUnit("staticLocaliser.StaticLocaliserControl", "Static Localiser");
//			//mi.addDependency(new PamDependency(RawDataUnit.class, "Acquisition.AcquisitionControl"));	
//			mi.setModulesMenuGroup(localiserGroup);
//			mi.setMaxNumber(1);
		/*
		 *************** End Localisation Group **************** 
		 */

		/*
		 * ************* Start Visual Group ********************
		 */

		mi = PamModuleInfo.registerControlledUnit("angleMeasurement.AngleControl", "Angle Measurement");
		mi.setModulesMenuGroup(visualGroup);
		mi.setToolTipText("Reads angles from a Fluxgate World shaft angle encode. (Can be used to read angle of binocular stands)");

		if (isSMRU) {
				mi = PamModuleInfo.registerControlledUnit("beakedWhaleProtocol.BeakedControl", "Beaked Whale Protocol");
		//		mi.addDependency(new PamDependency(GpsDataUnit.class, "GPS.GPSControl"));
				mi.setModulesMenuGroup(visualGroup);
				mi.setToolTipText("");
		//		mi.setMaxNumber(1);
		}

		mi = PamModuleInfo.registerControlledUnit("videoRangePanel.VRControl", "Video Range");
		mi.setModulesMenuGroup(visualGroup);
		mi.setToolTipText("Calculates ranges based on angles measured from video, observer height and earth radius");
		//		mi.setMaxNumber(1);

		mi = PamModuleInfo.registerControlledUnit("landMarks.LandmarkControl", "Fixed Landmarks");
		mi.setModulesMenuGroup(visualGroup);
		mi.setToolTipText("Place object symbols on the PAMGUARD map");

		mi = PamModuleInfo.registerControlledUnit("loggerForms.FormsControl", "Logger Forms");
		mi.setModulesMenuGroup(visualGroup);
		mi.setToolTipText("Replicates the functionality of User Defined Forms in the IFAW Logger software");
		mi.setMaxNumber(1);

		//		mi = PamModuleInfo.registerControlledUnit("autecPhones.AutecPhonesControl", "AUTEC Phones");
		//		mi.setModulesMenuGroup(visualGroup);
		//		mi.setMaxNumber(1);

		/*
		 * ************* End Visual Group ********************
		 */

	}

	/* (non-Javadoc)
	 * @see PamModel.PamModelInterface#startModel()
	 */
	public synchronized boolean startModel() {
		/*
		 * this get's called after the PamController has loaded it's main settings. 
		 * So at this point, go through all the PamModuleInfo's and check that 
		 * all have at least the minimum number required 
		 */

		PamSettingManager.getInstance().registerSettings(this);

		ArrayList<PamModuleInfo> moduleInfoList = PamModuleInfo.getModuleList();
		PamModuleInfo moduleInfo;
		for (int i = 0; i < moduleInfoList.size(); i++) {
			moduleInfo = moduleInfoList.get(i);
			while (moduleInfo.getNInstances() < moduleInfo.getMinNumber()) {
				pamController.addControlledUnit(moduleInfo.create(moduleInfo.getDefaultName()));
			}
		}
		pamController.notifyModelChanged(PamControllerInterface.CHANGED_MULTI_THREADING);
		return false;
	}

	/* (non-Javadoc)
	 * @see PamModel.PamModelInterface#stopModel()
	 */
	public void stopModel() {

	}

	@Override
	public boolean modelSettings(JFrame frame) {
		PamModelSettings newSettings = ThreadingDialog.showDialog(frame, pamModelSettings);
		if (newSettings != null) {
			boolean changed = (!newSettings.equals(pamModelSettings));
			pamModelSettings = newSettings.clone();
			if (changed) {
				pamController.notifyModelChanged(PamControllerInterface.CHANGED_MULTI_THREADING);
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Set multithreading for all of PAMGUARD. 
	 * <p>
	 * Be warned that this function should very rarely ever be called and 
	 * has been included only so that the Likelihood detector can turn off
	 * multithreading. Once multithreading has been debugged in the Likelihood 
	 * detector, this function will be removed or deprecated. 
	 * @param multithreading
	 */
	public void setMultithreading(boolean multithreading) {
		pamModelSettings.multiThreading = multithreading;
		pamController.notifyModelChanged(PamControllerInterface.CHANGED_MULTI_THREADING);
	}
	/**
	 * @return Returns the dependencyManager.
	 */
	public DependencyManager getDependencyManager() {
		return dependencyManager;
	}

	@Override
	public Serializable getSettingsReference() {
		return pamModelSettings;
	}

	@Override
	public long getSettingsVersion() {
		return PamModelSettings.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return "PAMGUARD Data Model";
	}

	@Override
	public String getUnitType() {
		return "PAMGUARD Data Model";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		pamModelSettings = ((PamModelSettings)pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	public boolean isMultiThread() {
		return pamModelSettings.multiThreading;
	}

	public PamModelSettings getPamModelSettings() {
		return pamModelSettings;
	}

}