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

package clickDetector;

import networkTransfer.receive.BuoyStatusDataUnit;
import clickDetector.ClickClassifiers.ClickIdInformation;
import clickDetector.ClickClassifiers.ClickIdentifier;
import clickDetector.echoDetection.EchoDetectionSystem;
import clickDetector.echoDetection.EchoDetector;
import clickDetector.offlineFuncs.OfflineEventDataBlock;
import clickDetector.offlineFuncs.OfflineEventLogging;

import signal.Hilbert;
import targetMotion.TargetMotionSQLLogging;
import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionProcess;
import Filters.Filter;
import Filters.FilterMethod;
import Filters.FilterType;
import Localiser.Correlations;
import Localiser.DelayMeasurementParams;
import Localiser.bearingLocaliser.BearingLocaliser;
import Localiser.bearingLocaliser.BearingLocaliserSelector;
import PamController.PamController;
import PamController.PamguardVersionInfo;
import PamDetection.AbstractLocalisation;
import PamDetection.RawDataUnit;
import PamModel.PamModel;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamView.GroupedSourcePanel;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import clickDetector.ClickClassifiers.ClickTypeCommonParams;
import fftFilter.FFTFilter;
import fftFilter.FFTFilterParams;
import fftManager.FastFFT;
import java.util.ArrayList;

/**
 * Main click detector process.
 * <p>
 * Observes a raw data block, filters and thresholds the data to create Click
 * objects
 * 
 * @see ClickDetection
 * @author Doug Gillespie
 * 
 */
public class ClickDetector extends PamProcess {

	enum ClickStatus {
		CLICK_ON, CLICK_ENDING, CLICK_OFF
	};

	private ClickDetector THIS;

	private int nChannelGroups = 0;

	// use the general flag from the pamModel instead.
	private boolean multiThread = false;

	private ChannelGroupDetector[] channelGroupDetectors;

	private int[] globalChannelList;

	private int[] globalChannelIndex;

	private long startTimeMillis;

	private ClickControl clickControl;

	private int channelsReceived; // bitmap of channels who's data has arrived (it's
	// all blocked for different channels !

	private double[][] channelData;

	private int nChan;

	private Hilbert hilbert = new Hilbert();

	//	private PamDataBlock<ClickDetection> outputClickData;
	private ClickDataBlock outputClickData;

	private OfflineEventDataBlock  offlineEventDataBlock;

	private NoiseDataBlock noiseDataBlock;

	private long blockStartSample;

	private long blockDuration;

	private long allSamplesProcessed;

	private PamRawDataBlock rawDataSource;

	private PamDataBlock<TriggerLevelDataUnit> triggerDataBlock;

	private PamRawDataBlock doubleFilteredData;

	protected PamDataBlock<ClickDetection> trackedClicks;

	private ClickTriggerFunctionDataBlock triggerFunctionDataBlock;

	private Filter[] firstFilter = new Filter[PamConstants.MAX_CHANNELS];
	
	int firstFilterDelay;

	private Filter[] secondFilter = new Filter[PamConstants.MAX_CHANNELS];
	
	int secondFilterDelay;

	// boolean[] initialisedTriggers;
	private long secondCounter;

	private TriggerHistogram[] triggerHistogram = new TriggerHistogram[PamConstants.MAX_CHANNELS];
	//	firstFilter = new IirfFilter[PamConstants.MAX_CHANNELS];
	//	secondFilter = new IirfFilter[PamConstants.MAX_CHANNELS];
	//	triggerHistogram = new TriggerHistogram[PamConstants.MAX_CHANNELS];

	private ClickFileStorage clickStorage;

	private long samplesPerStore, fileStartSample;

	private long clickCount;

	private double[] freqLims = new double[2];

	//private int loopCount;

	private long requiredKeptSamples;

	protected FastFFT fastFFT = new FastFFT();

	private Correlations correlations = new Correlations();

	private NewClickMonitor newClickMonitor;

	AcquisitionProcess acquisitionProcess;
	//	 get the smallest value which will encompass the click Length
	int correlationLength;

	private ClickBinaryDataSource clickBinaryDataSource;

	private TargetMotionSQLLogging targetMotionSQLLogging;

	private OfflineEventLogging offlineEventLogging;
	
	public ClickDetector(ClickControl clickControl){ 

		super(clickControl, null);

		THIS = this;

		this.clickControl = clickControl;

		multiThread = wantMultiThread();

		newClickMonitor = new NewClickMonitor();
		//		clickControl.rawDataBlock.addObserver(this);

		//		addOutputDataBlock(outputClickData = new PamDataBlock<ClickDetection>(ClickDetection.class, "Clicks", this, 0));
		addOutputDataBlock(outputClickData = new ClickDataBlock(clickControl, this, 0));

		noiseDataBlock = new NoiseDataBlock(clickControl, this, 0);
		addOutputDataBlock(noiseDataBlock);

		triggerDataBlock = new PamDataBlock<TriggerLevelDataUnit>(TriggerLevelDataUnit.class, 
				clickControl.getDataBlockPrefix() + "Trigger Levels", this, 0);

		NewClickOverlayGraphics newClickOverlayGraphics;
		if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
			outputClickData.setOverlayDraw(newClickOverlayGraphics = new NewClickOverlayGraphics(clickControl, 
					outputClickData, NewClickOverlayGraphics.SHOW_SPECTROGRAM | NewClickOverlayGraphics.SHOW_MAP, 
			"All clicks"));
		}

		clickBinaryDataSource = new ClickBinaryDataSource(this, outputClickData, "Clicks");
		outputClickData.setBinaryDataSource(clickBinaryDataSource);
		
		outputClickData.setDatagramProvider(new ClickDatagramProvider(clickControl));
		
		outputClickData.setRecordingTrigger(new ClickRecorderTrigger(clickControl));
		

		//		addBinaryDataSource(outputClickData.getSisterBinaryDataSource());

		//		newClickOverlayGraphics.setLineColour(Color.BLUE);
		//		newClickOverlayGraphics.setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_SQUARE, 8, 8, true, Color.BLUE, Color.BLUE));
		//		PamDetectionOverlayGraphics pamDetectionOverlayGraphics = new PamDetectionOverlayGraphics(outputClickData);
		//		pamDetectionOverlayGraphics.setLineColour(Color.BLUE);
		//		pamDetectionOverlayGraphics.setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_SQUARE, 8, 8, true, Color.BLUE, Color.BLUE));
		//		outputClickData.setOverlayDraw(pamDetectionOverlayGraphics);


		outputClickData.setLocalisationContents(AbstractLocalisation.HAS_BEARING);
		//		outputClickData.SetLogging(new ClickLogger(clickControl, outputClickData));
		outputClickData.SetLogging(new ClickLogger(clickControl, outputClickData));
		outputClickData.setCanClipGenerate(false); // can't do clips for clicks - would get far too many
		// outputData.SetLogging(new ClickLogger());

		triggerFunctionDataBlock = new ClickTriggerFunctionDataBlock(clickControl.getDataBlockPrefix() + "Click Trigger Function", this, 
				clickControl.clickParameters.channelBitmap, getSampleRate());
		addOutputDataBlock(triggerFunctionDataBlock);
		clickControl.clickParameters.publishTriggerFunction = true;

		triggerFunctionDataBlock.setChannelMap(clickControl.clickParameters.channelBitmap);
		if (clickControl.clickParameters.publishTriggerFunction) {
			addOutputDataBlock(triggerFunctionDataBlock);
		}
		else {
			removeOutputDatablock(triggerFunctionDataBlock);
		}

		addOutputDataBlock(trackedClicks = new TrackedClickDataBlock(clickControl, this, 0));

		//		trackedClicks.setLinkGpsData(true);
		//		trackedClicks.setOverlayDraw(new ClickOverlayGraphics(clickControl
		//				.getClickDetector(), ClickOverlayGraphics.SHOW_SPECTROGRAM | ClickOverlayGraphics.SHOW_MAP,
		//				"Tracked clicks"));
		NewClickOverlayGraphics trackedClickOverlayGraphics;
		trackedClicks.setOverlayDraw(trackedClickOverlayGraphics = new NewClickOverlayGraphics(clickControl, 
				trackedClicks, NewClickOverlayGraphics.SHOW_MAP, "Tracked clicks"));
		//		trackedClickOverlayGraphics.setLineColour(Color.BLUE);
		//		trackedClickOverlayGraphics.setPamSymbol(new PamSymbol(PamSymbol.SYMBOL_DIAMOND, 8, 8, true, Color.BLUE, Color.BLUE));

		//		trackedClicks.SetLogging(new ClickLogger(clickControl, trackedClicks)); // ??

		if (PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW) {
			offlineEventDataBlock = new OfflineEventDataBlock(clickControl.getUnitName()+"_OfflineEvents",
					this, 0);
			offlineEventLogging = new OfflineEventLogging(clickControl, offlineEventDataBlock);
			targetMotionSQLLogging = new TargetMotionSQLLogging("1");
			offlineEventLogging.addAddOn(targetMotionSQLLogging);
			offlineEventDataBlock.SetLogging(offlineEventLogging);
			addOutputDataBlock(offlineEventDataBlock);
		}
	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		//		if (clickControl.isViewerMode()) {
		//			sampleRate = clickControl.getClicksOffline().getSampleRate();
		//		}
		super.setSampleRate(sampleRate, notify);
		createFilters();
	}

	private ClickParameters previousParameters;
	/**
	 * flag to stop the detector trying to do anything if the 
	 * parametrs are in the process of being recreated which
	 * is probably  going on in a different thread. 
	 */
	private volatile boolean pauseDetection = false;

	/**
	 * Quick way of making  a local decision on multithreading
	 * @return true if want multithreading on click detector
	 */
	private boolean wantMultiThread() {
		return PamModel.getPamModel().isMultiThread();
		//		return false;
	}
	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#SetupProcess()
	 */
	@Override
	public void setupProcess() {

		pauseDetection = true;

		super.setupProcess();

		ClickParameters cp = clickControl.clickParameters;

		boolean threadingChanged = (multiThread != wantMultiThread());
		multiThread = wantMultiThread();

		// Always complete the setup bit
		//		if (previousParameters == null || !previousParameters.rawDataSource.equals(cp.rawDataSource)) {
		// first we need to make sure that the old data source is still 
		// not sending data to the newClickMonitor.
		// otherwise, it remains subscribed and get's upset.
		if (rawDataSource != null) {
			rawDataSource.deleteObserver(newClickMonitor);
		}
		// get the right raw data block 		
		rawDataSource = PamController.getInstance().getRawDataBlock(clickControl.clickParameters.rawDataSource);
		if (rawDataSource == null) {
			// try to connect automatically to the acquisition module ...
//			ArrayList<PamDataBlock> rawBlocks = PamController.getInstance().getDataBlocks(RawDataUnit.class, false);
			AcquisitionControl daq = (AcquisitionControl) PamController.getInstance().findControlledUnit(AcquisitionControl.unitType);
			if (daq != null) {
				rawDataSource = daq.getRawDataBlock();
//				clickControl.clickParameters.rawDataSource = rawDataSource.getDataName();
			}
			
			if (rawDataSource == null) {
				return;
			}
		}
		setParentDataBlock(rawDataSource);
		if (multiThread) {
			// the new click monitor needs to monitor 
			// raw data flow so that it can know if it
			// should start a new file or not. 
			rawDataSource.addObserver(newClickMonitor);
		}
		//		}

		// sort out the channel groups, but only if they changed
		// since last time this was run. 
		boolean groupsChanged = false;

		if (nChannelGroups != GroupedSourcePanel.countChannelGroups(cp.channelBitmap, cp.channelGroups)) {
			groupsChanged = true;
		}
		else {
			for (int i = 0; i < nChannelGroups; i++) {
				if (channelGroupDetectors[i].groupChannels != GroupedSourcePanel.getGroupChannels(i, cp.channelBitmap, 
						cp.channelGroups)) {
					groupsChanged = true;
				}
			}
		}

		if (groupsChanged || threadingChanged) {
			if (channelGroupDetectors != null) {
				for (int i = 0; i < channelGroupDetectors.length; i++) {
					channelGroupDetectors[i].removeUnusedDataBlocks();
				}
			}
			outputClickData.setLocalisationContents(0);
			nChannelGroups = GroupedSourcePanel.countChannelGroups(cp.channelBitmap, cp.channelGroups);
			int groupChannels;
			channelGroupDetectors = new ChannelGroupDetector[nChannelGroups];
			for (int i = 0; i < nChannelGroups; i++) {
				groupChannels = GroupedSourcePanel.getGroupChannels(i, cp.channelBitmap, 
						cp.channelGroups);
				channelGroupDetectors[i] = new ChannelGroupDetector(i, groupChannels);
				if (PamUtils.getNumChannels(groupChannels) > 1) {
					outputClickData.setLocalisationContents(AbstractLocalisation.HAS_BEARING);
				}
				if (multiThread) {
					channelGroupDetectors[i].halfBuiltClicks.addObserver(newClickMonitor, true);
				}
				//				System.out.println("Group " + i + " contains channels list " + groupChannels);
			}




			globalChannelList = new int[nChan = PamUtils.getNumChannels(clickControl.clickParameters.channelBitmap)];
			globalChannelIndex = new int[PamUtils.getHighestChannel(clickControl.clickParameters.channelBitmap)+1];
			int iCh = 0;
			for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
				if ((1 << i & clickControl.clickParameters.channelBitmap) != 0) {
					globalChannelList[iCh] = i;
					globalChannelIndex[i] = iCh;
					iCh++;
				}
			}

			createFilters();
			sortOutDataBlocks();

			channelData = new double[PamUtils.getHighestChannel(clickControl.clickParameters.channelBitmap) + 1][];

			for (int i = 0; i < nChannelGroups; i++) {
				channelGroupDetectors[i].setupProcess();
			}
		}
		//		clickControl.clickParameters.channelBitmap = (rawDataBlock.getChannelMap() & 3);
		outputClickData.setChannelMap(clickControl.clickParameters.channelBitmap);
		noiseDataBlock.setChannelMap(clickControl.clickParameters.channelBitmap);
		triggerDataBlock.setChannelMap(clickControl.clickParameters.channelBitmap);

		//		sortOutDataBlocks();


		/*
		 * Channel data is an array of double arrays which will hold raw data
		 * for each channel. the ClickDetector has been generalised so that it
		 * can work with any combination of channels. Each ClickDetector will
		 * trigger on a logical OR of the triggers of all the channels it's set
		 * to process (set in clickControl.clickParameters.channelBitmap). For two elements close together,
		 * it's likely that you'll want to trigger them together just like in
		 * old RainbowClick. However, if elements are relatively far apart it
		 * mey be better to trigger the channels separately, or in small groups.
		 * <p> To manage this, the following lines of code <p> 1) Works out how
		 * many channels are to be processed and allocated an array of double
		 * arrays which can hold the data coming in from the raw block
		 * referenced by true channel number <p> 2) Makes a list of channels, so
		 * that loops within the click detector can be simply over 0 to nChan-1
		 * and the channelList used to find the correct raw data block <p> all
		 * filters and other processees can then be referenced using consecutive
		 * numbers in the 0 to nChan-1 range.
		 */

		// now allocate everything else needed ...
		// first the arrays to hold the objects
		//		
		//		firstFilter = new IirfFilter[PamConstants.MAX_CHANNELS];
		//		secondFilter = new IirfFilter[PamConstants.MAX_CHANNELS];
		//		triggerHistogram = new TriggerHistogram[PamConstants.MAX_CHANNELS];
		// initialisedTriggers = new boolean[nChan];

		createFilters();


		//		if (PamController.getInstance().getPamStatus() == PamController.PAM_RUNNING) {
		//			pamStart();
		//		}
		previousParameters = cp.clone();

		pauseDetection = false;

        /* check to make sure there's at least 1 ClickAlarm in the list.  If
         * there isn't, add one.  This can happen the first time a user runs
         * this version of the ClickDetector with an old parameters file
         */
        if (cp.clickAlarmList == null) {
            cp.clickAlarmList = new ArrayList<ClickAlarm>();
        }
        if (cp.clickAlarmList.size()==0) {
                        ClickAlarm ca = new ClickAlarm();
            cp.clickAlarmList.add(ca);
        }

        /* check if any of the click types specify an alarm that doesn't
         * exist.  If any are found, change them to the default alarm (which is
         * always the first alarm in the list)
         */
        int[] codeList = clickControl.getClickIdentifier().getCodeList();
        ClickTypeCommonParams commonParams = null;
        if (codeList != null) {
        	for (int i=0; i<codeList.length; i++) {
        		commonParams = clickControl.getClickIdentifier().getCommonParams(codeList[i]);
        		if (cp.clickAlarmList.indexOf(commonParams.getAlarm()) == -1) {
        			commonParams.setAlarm(cp.clickAlarmList.get(0));
        		}
        	}
        }
	}

	public void createFilters() {

		// then create the actual objects
		FilterMethod preFilterMethod = FilterMethod.createFilterMethod(getSampleRate(), clickControl.clickParameters.preFilter);
		FilterMethod trigFilterMethod = FilterMethod.createFilterMethod(getSampleRate(), clickControl.clickParameters.triggerFilter);
		for (int i = 0; i < nChan; i++) {
			if (clickControl.clickParameters.preFilter.filterType != FilterType.NONE) {
//				if (firstFilter[globalChannelList[i]] == null) {
					firstFilter[globalChannelList[i]] = preFilterMethod.createFilter(i);
					firstFilterDelay = firstFilter[globalChannelList[i]].getFilterDelay();
//				}
//				else {
//					firstFilter[globalChannelList[i]].setParams(globalChannelList[i],
//							clickControl.clickParameters.preFilter, getSampleRate());
//				}
			}
			if (clickControl.clickParameters.triggerFilter.filterType != FilterType.NONE) {
//				if (secondFilter[globalChannelList[i]] == null) {
					//					System.out.println("Setting up second (no list) filter in " + 
					//							clickControl.getUnitName() + " " + clickControl.clickParameters.triggerFilter);
					secondFilter[globalChannelList[i]] = trigFilterMethod.createFilter(i);
					secondFilterDelay = secondFilter[globalChannelList[i]].getFilterDelay();
//				}
//				else {
//					//					System.out.println("Setting up second filter in " + 
//					//							clickControl.getUnitName() + " " + clickControl.clickParameters.triggerFilter);
//					secondFilter[globalChannelList[i]].setParams(globalChannelList[i],
//							clickControl.clickParameters.triggerFilter, getSampleRate());
//				}
			}
			else {
				//				System.out.println("No second filter in " + 
				//						clickControl.getUnitName() + " " + clickControl.clickParameters.triggerFilter);

			}
			if (triggerHistogram[globalChannelList[i]] == null) {
				triggerHistogram[globalChannelList[i]] = new TriggerHistogram(-20, 50, 70, 3);
			}
		}
	}

	public void newParameters() {
		// creates a new set of filters - change over should be pretty seamless
		// and not crash anything
		setupProcess();

	}

	/**
	 * 
	 * @return The main output click data block. 
	 */
	public ClickDataBlock getClickDataBlock() {
		return outputClickData;
	}

	/**
	 * @return the offlineEventDataBlock
	 */
	public OfflineEventDataBlock getOfflineEventDataBlock() {
		return offlineEventDataBlock;
	}

	@Override
	public long getRequiredDataHistory(PamObservable o, Object arg) {
		return relSamplesToMilliseconds(requiredKeptSamples);
	}


	public void secondTimer(long sampleNumber) {
		//clickControl.displayTriggerHistogram(triggerHistogram);
		long ms = absSamplesToMilliseconds(sampleNumber);
		TriggerLevelDataUnit tdu = new TriggerLevelDataUnit(ms, triggerDataBlock.getChannelMap(), 
				sampleNumber, triggerHistogram);
		//		PamDataUnit tu = triggerDataBlock.getNewUnit(0, 0, triggerDataBlock.getChannelMap());
		tdu.setTriggerHistogram(triggerHistogram);
		triggerDataBlock.addPamData(tdu);

		for (int i = 0; i < nChan; i++) {
			triggerHistogram[globalChannelList[i]].decay(sampleNumber);
		}
	}

	@Override
	public void newData(PamObservable obs, PamDataUnit newData) {
		/*
		 * Data arrive channel at a time. Need to wait until all channels have
		 * arrived before starting processing. This will screw up big time if
		 * the blocks don't arrive in order, so do a few checks - all blocks
		 * should have the same startSample <p> 
		 * 
		 * This Process also subscribes to
		 * the filtered and double filtered data - now if we treat them like the
		 * other data we'll end up in an infinite loop, so why did I subscribe ?
		 * In order to get the FirstRequiredSample call from them to hold back a
		 * bit of old data. If they come here, then just get out immediately
		 * 
		 * One final problem - with the implimentation of the postSample, if a
		 * click ends close to the end of a block, then the data it needs may
		 * not be there, so it's necessary to NOT analyse the final block, but
		 * to go up to some stage before. In principle, postsample could be
		 * larger than a single block.
		 */
		RawDataUnit newRawData = (RawDataUnit) newData;

//		if (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER) {
//			System.out.println("raw data arrived at click det on channels " + 
//					newData.getChannelBitmap() + " sample " + newRawData.getStartSample());
//		}

		if ((newRawData.getChannelBitmap() & clickControl.clickParameters.channelBitmap) == 0) return;
		//
		//		if (obs == filteredDataBlock || obs == doubleFilteredData)
		//			return;

		clickControl.newRawData(obs, newData);

		// see if it's time to start a new file
		// only do this here if it's not multithread
		// otherwise, it's in the newClickMonitor. 
		if (multiThread == false && clickStorage != null
				&& clickControl.clickParameters.rcAutoNewFile
				&& (allSamplesProcessed - fileStartSample) >= samplesPerStore) {
			openOutputFile(true);
		}

		// see how much extra data needs to be in storage to alow postsample
		// readout
		int extraBlocks = (int) Math
		.ceil((double) clickControl.clickParameters.postSample
				/ (double) newRawData.getDuration());

		/*
		 * detector will probably not analyse the very latest block to allow room
		 * for a postsample, so it must ensure that the DataBlock keeps older
		 * blocks for analysis and a bit before that too for the presample if it
		 * finds a click. requiredKeptSamples should do it !
		 */
		requiredKeptSamples = (extraBlocks + 1) * newRawData.getDuration()
		+ clickControl.clickParameters.preSample + 1;

		int channel = PamUtils.getSingleChannel(newRawData.getChannelBitmap());
		/*
		 * Click Detector is keeping a 1 second timer going with which it
		 * notifies the Click controller so the controller can do things like
		 * plot the trigger histogram, etc.
		 */
		if (newRawData.getStartSample() - secondCounter > (long) getSampleRate()) {
			secondCounter = newRawData.getStartSample();
			clickControl.secondTimer(secondCounter);
			this.secondTimer(secondCounter);
		}

		if (channelsReceived == 0) {
			blockStartSample = newRawData.getStartSample();
			blockDuration = newRawData.getDuration();
		} else {
			if (blockStartSample != newRawData.getStartSample()
					|| blockDuration != newRawData.getDuration()) {
				// System.out.println("Data arriving in the click detector is no
				// longer synchronised");
			}
		}
		channelsReceived |= newRawData.getChannelBitmap();
		channelData[channel] = newRawData.getRawData();
		// channelBlockList[channel] = newData.absBlockIndex;
		// PamDataBlock dataBlock = (PamDataBlock) obs;
		// PamDataUnit oldUnit;

		if (channelsReceived == clickControl.clickParameters.channelBitmap) {
			// // we have data from every channel,
			// // find older blocks and process them.
			preProcessData();
			for (int i = 0; i < nChannelGroups; i++) {
				channelGroupDetectors[i].lookForClicks();
			}
			//			lookForClicks();
			channelsReceived = 0;
			allSamplesProcessed += blockDuration;
		}
	}

	@Override
	public void masterClockUpdate(long timeMilliseconds, long sampleNumber) {
		// TODO Auto-generated method stub
		super.masterClockUpdate(timeMilliseconds, sampleNumber);
	}

	@Override
	public void prepareProcess() {

		super.prepareProcess();

		startTimeMillis = PamCalendar.getTimeInMillis();
		//		System.out.println(PamCalendar.formatDateTime(PamCalendar.getTimeInMillis()));
		channelsReceived = 0;
		blockStartSample = -1;
		secondCounter = 0;
		clickControl.pamStart();
		allSamplesProcessed = 0;
		clickCount = 0;

		openOutputFile(true);

		acquisitionProcess = (AcquisitionProcess) getSourceProcess();
		//		 get the smallest value which will encompass the click Length
		correlationLength = PamUtils.getMinFftLength(clickControl.clickParameters.maxLength);

		sortOutDataBlocks();
	}

	@Override
	public void pamStart() {
		//loopCount = 0;
		// for (int i = 0; i < nChan; i++) {
		// initialisedTriggers[i] = false;
		// }


		for (int i = 0; i < nChannelGroups; i++) {
			channelGroupDetectors[i].pamStart();
		}

	}
	
	@Override
	public void processNewBuoyData(BuoyStatusDataUnit buoyStatus, PamDataUnit dataUnit) {
		ClickDetection click = (ClickDetection) dataUnit;
		ClickIdentifier clickIdentifier = clickControl.getClickIdentifier();
		if (clickIdentifier != null) {
			ClickIdInformation idInfo = clickIdentifier.identify(click);
			click.setClickType((byte) idInfo.clickType);
		}
	}

	private void preProcessData() {
		/*
		 * If the filter is doing anything, create a new output data block. If
		 * it isn't just set the triggerData[][] array to the raw data. The
		 * triggering which runs soon after will operate on triggerData[][]
		 * whether it's the old raw data or the filtered data. Once a click is
		 * found, it will be picked from the finalDataSource stream whether it's
		 * raw or filtered. (We hold the data over in the DataBlcok incase a
		 * click ends in this block that had started in the previous one
		 * Declared for the class so it doesn't continually regenerate.
		 //		 */
		//		RawDataUnit filteredUnit;
		//		double[] data;
		//		/*
		//		* Waveform data ends up poining either to the raw data, or the output
		//		* of the fist filter if there is one. new wavefformData is created
		//		* every time (or recycled from the data block) since we may beed to go
		//		* back a while to find data from a previous block
		//		*/
		//		double[][] waveformData = new double[nChan][];
		//		for (int i = 0; i < nChan; i++) {
		//		if (clickControl.clickParameters.preFilter.filterType != FilterType.NONE) {
		//		filteredUnit = filteredData.getRecycledUnit();
		//		if (filteredUnit != null) {
		//		filteredUnit.setInfo(absSamplesToMilliseconds(blockStartSample),
		//		1 << channelList[i], blockStartSample, blockDuration);
		//		}
		//		else {
		//		filteredUnit = new RawDataUnit(absSamplesToMilliseconds(blockStartSample),
		//		1 << channelList[i], blockStartSample, blockDuration);
		//		}
		//		data = (double[]) filteredUnit.getRawData();
		//		if (data == null) {
		//		data = new double[(int) blockDuration];
		//		}
		//		firstFilter[i].runFilter(channelData[channelList[i]], data);
		//		waveformData[i] = data;
		//		filteredUnit.setRawData(data);
		//		filteredData.addPamData(filteredUnit);
		//		} else {
		//		waveformData[i] = channelData[channelList[i]];
		//		}
		//		if (clickControl.clickParameters.triggerFilter.filterType != FilterType.NONE) {
		//		if (triggerData[i] == null
		//		|| triggerData[i].length != blockDuration) {
		//		triggerData[i] = new double[(int) blockDuration];
		//		}
		//		secondFilter[i].runFilter(waveformData[i], triggerData[i]);
		//		} else {
		//		triggerData[i] = waveformData[i];
		//		}
		// if (initialisedTriggers[i] == false) {
		// // take the average of all data and set it as the trigger value.
		// double meanval = 0.;
		// for (iSamp = 0; iSamp < blockDuration; iSamp++) {
		// meanval += triggerData[i][iSamp];
		// }
		// meanval /= blockDuration;
		// shortTriggerFilter[i].SetMemory(meanval);
		// longTriggerFilter[i].SetMemory(meanval);
		// initialisedTriggers[i] = true;
		// }
		//		}
	}
	private void sortOutDataBlocks() {
		/*
		 * need to check that the filtered data block exists - I don't know why I 
		 * make the filtered data public - it only causes trouble if it's not there !
		 */
		PamRawDataBlock rawDataBlock = (PamRawDataBlock) getParentDataBlock();
		if (rawDataBlock == null) return;

		// always make the filtered block even if it's not used.
		//		if (clickControl.clickParameters.preFilter.filterType != FilterType.NONE) {
		//			if (filteredDataBlock == null) {
		//				filteredDataBlock = new PamRawDataBlock("Filtered Data from " + clickControl.getUnitName(), 
		//						this, clickControl.clickParameters.channelBitmap, rawDataBlock.getSampleRate());
		//				addOutputDataBlock(filteredDataBlock);
		//			}
		//			filteredDataBlock.addObserver(this);
		//			finalDataSource = filteredDataBlock;
		//		} else {
		//			if (filteredDataBlock != null) removeOutputDatablock(filteredDataBlock);
		//			finalDataSource = (PamRawDataBlock) getParentDataBlock();
		//		}
	}

	/**
	 * Flag to stop the click detector running in the other thread
	 * from attempting to write to the file while it's
	 * either closed or it's in the process of writing it's header
	 * control structures when the pam stops. 
	 */
	private volatile boolean lockDownfile = false;
	public synchronized boolean openOutputFile(boolean doHeader) {

		closeOutputFile();

		if (clickControl.clickParameters.createRCFile == false) {
			return false;
		}

		clickStorage = new RainbowFile(this);

		if (clickStorage.checkStorage() == false) {
			//			clickStorage = null;
			return false;
		}

		boolean ans = clickStorage.openClickStorage(allSamplesProcessed);

		if (doHeader) {
			clickStorage.writeClickStructures(clickControl.clickParameters);
		}

		samplesPerStore = (long) (clickControl.clickParameters.rcFileLength * 3600 * getSampleRate());
		fileStartSample = allSamplesProcessed;
		System.out.println("File start sample in openOutputfile = " + fileStartSample);

		clickControl.notifyNewStorage(clickStorage.getStorageName());

		lockDownfile = false;
		return ans;
	}

	public synchronized void closeOutputFile() {
		lockDownfile = true;
		if (clickStorage != null) {
			clickStorage.writeClickStructures(clickControl.clickParameters);
			clickStorage.closeClickStorage();
			clickStorage = null;
		}
	}

	@Override
	public void pamStop() {
		clickControl.pamStop();		/*
		 * Go through the sub detectors and tell each of them to clear
		 */
		if (getSourceDataBlock() != null) {
			getSourceDataBlock().waitForThreadedObservers(500);
		}
		if (multiThread) {
			if (channelGroupDetectors == null) {
				return;
			}
			for (int i = 0; i < channelGroupDetectors.length; i++) {
				channelGroupDetectors[i].halfBuiltClicks.waitForThreadedObservers(500);
			}
		}
		/*
		 * Can't close the output file here because a click will 
		 * arrive from the other thread and start writing into 
		 * the file just as it's closing. 
		 */
		closeOutputFile();
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public boolean reWriteClick(ClickDetection click, boolean waveformToo) {
		if (clickStorage == null || lockDownfile)
			return false;
		if (waveformToo) {
			return clickStorage.writeClick(click);
		} else {
			return clickStorage.writeClickHeader(click);
		}
	}

	public ClickControl getClickControl() {
		return clickControl;
	}

	public long getSamplesProcessed() {
		return allSamplesProcessed;
	}

	public long getClickCount() {
		return clickCount;
	}

	public PamDataBlock<TriggerLevelDataUnit> getTriggerDataBlock() {
		return triggerDataBlock;
	}

	public PamDataBlock<ClickDetection> getOutputClickData() {
		return outputClickData;
	}

	/**
	 * Once a click is detected, does everything else to 
	 * it such as calculate bearing, check species, send to 
	 * click train detector, etc. 
	 * @return true if the tasks complete successfully and the click
	 * is to be kept. false otherwise. 
	 */
	private boolean completeClick(ClickDetection newClick) {

		if (newClick.dataType == ClickDetection.CLICK_CLICK) {
			int nChannels = PamUtils.getNumChannels(newClick.getChannelBitmap());
			int firstChannel = PamUtils.getLowestChannel(newClick.getChannelBitmap());
			double amplitude;
			int iD = 0;
			/*
			 * Echo Detection
			 */
			if (clickControl.clickParameters.runEchoOnline && 
					newClick.getChannelGroupDetector().getEchoDetector() != null) {
				boolean isEcho = newClick.getChannelGroupDetector().getEchoDetector().isEcho(newClick);
				if (isEcho && clickControl.clickParameters.discardEchoes) {
					return false;
				}
				else {
					newClick.setEcho(isEcho);
				}
			}
			/*
			 * Delay measurement. This can get a bit messy ! It's done very efficiently if 
			 * we use the waveform since the complex spectral data are needed in any case to
			 * do the classification later on.  There are now however four choices
			 * as to how we do this, whether or not we filter and whether or not we
			 * use the envelope. In the long term, this should be re-written so we
			 * can stay in the frequency domain, but for now, use existing functions
			 * to get the right type of waveform data.  
			 */
			int nPairs = nChannels * (nChannels-1) / 2;
			double[] delaySecs = new double[nPairs];
			double delaySamples;
			DelayMeasurementParams dmp = clickControl.clickParameters.delayMeasurementParams;
			if (!dmp.envelopeBearings && !dmp.filterBearings) {
				// normal case !
				for (int i = 0; i < nChannels; i++) {
					for (int j = i+1; j < nChannels; j++) {
						delaySamples = correlations.getDelay(newClick
								.getComplexSpectrum(i, correlationLength), newClick
								.getComplexSpectrum(j, correlationLength), correlationLength);
						delaySecs[iD] = delaySamples / getSampleRate();
						newClick.setDelay(iD++, delaySamples);
					}
				}
			}
			else {
				/*
				 *  other cases which may filter or envelope the data. Would be more 
				 *  efficient if the data weren't converted back into the time domain !  
				 */
				double[][] correlationSignals;
				if (dmp.envelopeBearings) {
					if (dmp.filterBearings) {
						correlationSignals = newClick.getFilteredAnalyticWaveform(dmp.delayFilterParams);
					}
					else {
						correlationSignals = newClick.getFilteredAnalyticWaveform(null);
					}
				}
				else {
					correlationSignals = newClick.getWaveData(dmp.filterBearings, 
							dmp.delayFilterParams);
				}
				for (int i = 0; i < nChannels; i++) {
					for (int j = i+1; j < nChannels; j++) {
						delaySamples = correlations.getDelay(correlationSignals[i], correlationSignals[j], correlationLength);
						delaySecs[iD] = delaySamples / getSampleRate();
						newClick.setDelay(iD++, delaySamples);
					}
				}
				
			}
			/**
			 * Angle measurement
			 */
			double[][] angles = null;
			BearingLocaliser bearingLocaliser = newClick.getChannelGroupDetector().bearingLocaliser;
			if (bearingLocaliser != null) {
				angles = bearingLocaliser.localise(delaySecs);
				ClickLocalisation clickLocalisation = newClick.getClickLocalisation();
				if (clickLocalisation != null) {
					clickLocalisation.setAnglesAndErrors(angles);
					clickLocalisation.setArrayAxis(bearingLocaliser.getArrayAxis());
					clickLocalisation.setSubArrayType(bearingLocaliser.getArrayType());
				}
			}
			/*
			 * Angle vetoes
			 */
			double angle = newClick.getAngle();
			//			clickControl.angleVetoes.addAngleData(angle);
			if (clickControl.angleVetoes.passAllVetoes(angle, true) == false) {	
				return false;
			}

			newClick.clickNumber = ++clickCount;
			for (int c = 0; c < nChannels; c++) {
				amplitude = 0;
				double[] waveData = newClick.getWaveData(c);
				for (int s = 0; s < newClick.getDuration(); s++) {
					amplitude = Math.max(amplitude, Math
							.abs(waveData[s]));
				}
				newClick.setAmplitude(c, amplitude);
			}
			// get the amplitude in dB
			newClick.setCalculatedAmlitudeDB(acquisitionProcess.
					rawAmplitude2dB(newClick.getMeanAmplitude(), firstChannel, false));

			if (clickControl.clickParameters.classifyOnline && 
					clickControl.getClickIdentifier() != null) {
				ClickIdInformation idInfo = clickControl.getClickIdentifier().identify(newClick);
				newClick.setClickType((byte) idInfo.clickType);
				if (wantClick(newClick, idInfo) == false) {
					return false;
				}
			}
		}


		if (clickStorage != null) {
			if (lockDownfile) {
				System.out.println("Click file is closed and the click cannot be written");
			}
			else {
				clickStorage.writeClick(newClick);
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#flushDataBlockBuffers(long)
	 */
	@Override
	public boolean flushDataBlockBuffers(long maxWait) {
		/**
		 * Override to make sure that half built clicks get emptied too !
		 */
		boolean ok =  super.flushDataBlockBuffers(maxWait);
		int errors = 0;
		for (int i = 0; i < nChannelGroups; i++) {
			if (channelGroupDetectors[i].halfBuiltClicks == null) {
				continue;
			}
			if (channelGroupDetectors[i].halfBuiltClicks.waitForThreadedObservers(maxWait) == false) {
				errors++;
			}
		}
		return (ok && errors == 0);
	}

	/**
	 * Return true if the click should be processed and stored. <p>
	 * 
	 * If there is no online classification, we want everything
	 * If there is classification, but we've not told it to discard, etc 
	 * 
	 * @param click
	 * @param idInfo 
	 * @return
	 */
	public boolean wantClick(ClickDetection click, ClickIdInformation idInfo) {
		if (clickControl.clickParameters.classifyOnline == false) {
			return true;
		}
		if (click.getClickType() == 0 && clickControl.clickParameters.discardUnclassifiedClicks) {
			return false;
		}
		else {
			return (idInfo.discard == false);
		}
	}


	/**
	 * To improve performance, raw data is sent to each
	 * ChannelGroupDetector into a different thread. Once
	 * a channel group detector has found something, it 
	 * sends it to NewClickMonitor, which will again bring 
	 * candidate clicks into a separate thread. Spreading the 
	 * click detection tasks between threads in this way should
	 * help to spread them across processor cores and generally
	 * make full use fo available CPU. 
	 * @author Doug Gillespie
	 *
	 */
	class NewClickMonitor implements PamObserver {

		@Override
		public String getObserverName() {
			return clickControl.getUnitName() + "New click monitor";
		}

		@Override
		public long getRequiredDataHistory(PamObservable o, Object arg) {
			return 0;
		}

		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			if (clickStorage != null
					&& clickControl.clickParameters.rcAutoNewFile
					&& (sampleNumber - fileStartSample) >= samplesPerStore) {
				openOutputFile(true);
				//				System.out.println("New file flag in NewClickMonitor.masterClockUpdate");
			}
		}

		@Override
		public PamObserver getObserverObject() {
			return this;
		}

		@Override
		public void noteNewSettings() {
		}

		@Override
		public void removeObservable(PamObservable o) {
		}

		@Override
		public void setSampleRate(float sampleRate, boolean notify) {
		}

		@Override
		public void update(PamObservable o, PamDataUnit arg) {
			if (o == rawDataSource) {
				return; // don't do anything with raw data.
			}
			// otherwise it will be data from a channelgroupdetector. 
			ClickDetection newClick = (ClickDetection) arg;
			if (completeClick(newClick) && (newClick.dataType == ClickDetection.CLICK_CLICK
					|| newClick.dataType == ClickDetection.CLICK_NOISEWAVE)) {
				if (newClick.dataType == ClickDetection.CLICK_CLICK) {
					outputClickData.addPamData(newClick);
				}
				else if (newClick.dataType == ClickDetection.CLICK_NOISEWAVE) {
					noiseDataBlock.addPamData(newClick);
				}
			}
		}

	}

	public class ChannelGroupDetector {
		/*
		 * does the actual work of detecting within a group of channels.
		 * Each Click detector may contain several of these things depending on 
		 * how the channel groups are arranged. 
		 */
		int groupChannels;

		int groupId;

		private ClickStatus clickStatus;

		private long samplesProcessed;

		private long clickCompleteSample;

		private boolean initialiseFilters;

		private long clickStartSample;

		private int clickTriggers;

		private int[] channelList;

		private int nChannels;

		private TriggerFilter[] shortTriggerFilter;

		private TriggerFilter[] longTriggerFilter;

		private double[][] triggerData;

		private ClickDetection newClick;

		boolean waveDataError;

		private int overThreshold;

		private int downCount, upCount;

		private PamDataBlock<ClickDetection> halfBuiltClicks;

		private long nextNoiseSample = 0;

		public BearingLocaliser bearingLocaliser;

		private int groupHydrophones;

		private PamRawDataBlock finalDataSource;

		private PamRawDataBlock filteredDataBlock;
		
		private EchoDetector echoDetector;

		public ChannelGroupDetector(int groupId, int groupChannels) {
			this.groupId = groupId;
			this.groupChannels = groupChannels;
			if (multiThread) {
				halfBuiltClicks = new PamDataBlock<ClickDetection>(ClickDetection.class,
						"Temp clicks channel " + groupChannels, null, groupChannels);
			}
			if (rawDataSource != null && rawDataSource.getChannelListManager() != null) {
				groupHydrophones = rawDataSource.getChannelListManager().channelIndexesToPhones(groupChannels);
				double timingError = Correlations.defaultTimingError(getSampleRate());
				bearingLocaliser = BearingLocaliserSelector.createBearingLocaliser(groupHydrophones, timingError);
			}
			EchoDetectionSystem eds = clickControl.getEchoDetectionSystem();
			if (eds != null) {
				setEchoDetector(eds.createEchoDetector(this, groupChannels));
			}
			sortOutDataBlocks();
		}

		/**
		 * call before creating new channel group detectors to make sure 
		 * that old data blocks get removed from the system. 
		 */
		private void removeUnusedDataBlocks() {
			if (filteredDataBlock != null) {
				removeOutputDatablock(filteredDataBlock);
				filteredDataBlock = null;
			}
		}

		/**
		 * sort out filtered data blocks. 
		 * do everything possible not to delete and recreate these unnecessarily. 
		 */
		private void sortOutDataBlocks() {

			// always make the filtered block even if it's not used.
			//			if (clickControl.clickParameters.preFilter.filterType != FilterType.NONE) {
			if (filteredDataBlock == null) {
				filteredDataBlock = new PamRawDataBlock("Filtered Data from " + clickControl.getUnitName() + " Ch " + PamUtils.getChannelList(groupChannels), 
						THIS, groupChannels, getSampleRate());
				addOutputDataBlock(filteredDataBlock);
			}
			//				filteredDataBlock.addObserver(this);
			int requiredKeepSamples = clickControl.clickParameters.maxLength *2;
			int keepMillis = Math.max(1, (int) relSamplesToMilliseconds(requiredKeepSamples));
			filteredDataBlock.setNaturalLifetime(keepMillis);
			finalDataSource = filteredDataBlock;
			//			} else {
			//				if (filteredDataBlock != null) removeOutputDatablock(filteredDataBlock);
			//				finalDataSource = (PamRawDataBlock) getParentDataBlock();
			//			}
		}

		public BearingLocaliser getBearingLocaliser() {
			return bearingLocaliser;
		}

		public void pamStart() {
			
			if (halfBuiltClicks != null) {
				halfBuiltClicks.clearAll();
			}

			initialiseFilters = true;

			samplesProcessed = 0;

			clickCompleteSample = -1;

			downCount = upCount = 0;

			overThreshold = 0;

			clickTriggers = 0;

			clickStatus = ClickStatus.CLICK_OFF;

			nextNoiseSample = clickControl.clickParameters.maxLength;
			
			if (echoDetector != null) {
				echoDetector.initialise();
			}

		}

		void setupProcess() {

			channelList = new int[nChannels = PamUtils.getNumChannels(groupChannels)];
			int iCh = 0;
			for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
				if ((1 << i & groupChannels) > 0) {
					channelList[iCh++] = i;
				}
			}

			shortTriggerFilter = new TriggerFilter[nChannels];
			longTriggerFilter = new TriggerFilter[nChannels];
			triggerData = new double[nChannels][];

			for (int i = 0; i < nChannels; i++) {
				shortTriggerFilter[i] = new TriggerFilter(
						clickControl.clickParameters.shortFilter, 0);
				longTriggerFilter[i] = new TriggerFilter(
						clickControl.clickParameters.longFilter, 1);
			}
		}
		/**
		 * Main function call to search for click in the data.
		 * 
		 */
		public void lookForClicks() {
			/*
			 * Run the first filter on all the data in place.
			 */
			if (pauseDetection) {
				samplesProcessed += blockDuration;
				return;
			}
			int iSamp, iChan;
			double shortVal, longVal;
			double threshold = Math.pow(10.,
					clickControl.clickParameters.dbThreshold / 20.);
			// Click newClick;
			//			ClickDataUnit newDataUnit;
			double dB;
			double amplitude;
			int clickDuration;
			/*
			 * triggerData ends up pointing at the output of the second filter if
			 * there was one, otherwise the output of the first filter, otherwise
			 * the original raw data. Trigger data is only required temporarily in
			 * this function, so no need to store between calls.
			 */
			// double[][] triggerData = new double[nChan][];
			RawDataUnit filteredUnit;
			double[] data;

			/*
			 * If trigger function data is being output as a wave (for display), then we need to create 
			 * that data unit, or get one from a recycled data unit. 
			 * 
			 */
			RawDataUnit[] triggerWaveDataUnit = new RawDataUnit[nChannels];
			double[][] triggerWaveData = new double[nChannels][];
			if (clickControl.clickParameters.publishTriggerFunction) {
				for (int i = 0; i < nChannels; i++) {
					triggerWaveDataUnit[i] = triggerFunctionDataBlock.getRecycledUnit();
					if (triggerWaveDataUnit[i] == null) {
						triggerWaveDataUnit[i] = new RawDataUnit(absSamplesToMilliseconds(blockStartSample),
								1 << channelList[i], blockStartSample, blockDuration);
					}
					else {
						try {
							triggerWaveDataUnit[i].setInfo(absSamplesToMilliseconds(blockStartSample),
									1 << channelList[i], blockStartSample, blockDuration);
							triggerWaveData[i] = triggerWaveDataUnit[i].getRawData();
						} catch(NullPointerException ex) {
							ex.printStackTrace();
						}
					}
					if (triggerWaveData[i] == null || triggerWaveData[i].length != blockDuration) {
						triggerWaveData[i] = new double[(int) blockDuration];
						triggerWaveDataUnit[i].setRawData(triggerWaveData[i], false);
					}
				}
			}

			/*
			 * Waveform data ends up pointing either to the raw data, or the output
			 * of the fist filter if there is one. new wavefformData is created
			 * every time (or recycled from the data block) since we may beed to go
			 * back a while to find data from a previous block
			 */
			double[][] waveformData = new double[nChannels][];
			for (int i = 0; i < nChannels; i++) {
				filteredUnit = filteredDataBlock.getRecycledUnit();
				if (filteredUnit != null) {
					filteredUnit.setInfo(absSamplesToMilliseconds(blockStartSample),
							1 << channelList[i], blockStartSample, blockDuration);
				}
				else {
					filteredUnit = new RawDataUnit(absSamplesToMilliseconds(blockStartSample),
							1 << channelList[i], blockStartSample, blockDuration);
				}
				if (clickControl.clickParameters.preFilter.filterType != FilterType.NONE) {
					data = filteredUnit.getRawData();
					if (data == null) {
						data = new double[(int) blockDuration];
					}
					firstFilter[channelList[i]].runFilter(channelData[channelList[i]], data);
				}
				else {
					data = channelData[channelList[i]];
				}
				waveformData[i] = data;
				filteredUnit.setRawData(data);
				filteredDataBlock.addPamData(filteredUnit);
				//				else {
				//					filteredDataBlock.addPamData(chan)
				//					waveformData[i] = channelData[channelList[i]];
				//				}
				if ((1<<channelList[i] & clickControl.clickParameters.triggerBitmap) == 0) {
					triggerData[i] = null;
				}
				else if (clickControl.clickParameters.triggerFilter.filterType != FilterType.NONE &&
						(1<<channelList[i] & clickControl.clickParameters.triggerBitmap) != 0) {
					if (triggerData[i] == null
							|| triggerData[i].length != blockDuration) {
						triggerData[i] = new double[(int) blockDuration];
					}
					secondFilter[channelList[i]].runFilter(waveformData[i], triggerData[i]);
				} 
				else {
					triggerData[i] = waveformData[i];
				}
			}

			// these are the long  / short filters - not the IIRF filters !
			if (initialiseFilters) {
				initialiseFilters = false;
				for (iChan = 0; iChan < nChannels; iChan++) {
					if ((1<<channelList[iChan] & clickControl.clickParameters.triggerBitmap) == 0) {
						continue;
					}
					// some checks since it's been crashing here. 
					if (triggerData.length != nChannels) {
						System.out.println("About to crash - wrong no of channels in click trigger");
					}
					if (triggerData[iChan].length != blockDuration) {
						System.out.println("About to crash - wrong no of samples in click trigger");
					}
					// end of checks
					shortVal = longVal = 0;
					for (iSamp = 0; iSamp < blockDuration; iSamp++) {
						shortVal += Math.abs(triggerData[iChan][iSamp]);
						longVal += Math.abs(triggerData[iChan][iSamp]);
					}
					shortTriggerFilter[iChan].setMemory(shortVal / blockDuration
							* threshold);
					longTriggerFilter[iChan].setMemory(longVal / blockDuration);
				}
			}

			// then go through the click detection loop...
			for (iSamp = 0; iSamp < blockDuration; iSamp++) {
				for (iChan = 0; iChan < nChannels; iChan++) {
					if ((1<<channelList[iChan] & clickControl.clickParameters.triggerBitmap) == 0) {
						continue;
					}
					shortVal = shortTriggerFilter[iChan].runFilter(
							triggerData[iChan][iSamp], false);
					longVal = longTriggerFilter[iChan].runFilter(
							triggerData[iChan][iSamp], overThreshold > 0);
					overThreshold = PamUtils.SetBit(overThreshold,
							channelList[iChan], shortVal > longVal * threshold);
					if (longVal > 0) {
						dB = 20. * Math.log10(shortVal / longVal);
						triggerHistogram[channelList[iChan]].addData(dB);
					}
					else {
						dB = -100;
					}
					if (clickControl.clickParameters.publishTriggerFunction) {
						//						triggerWaveData[iChan][iSamp] = (shortVal / longVal);
						triggerWaveData[iChan][iSamp] = (dB);
					}
				}

				// end of loop over channels to see which have triggered
				if (clickStatus == ClickStatus.CLICK_OFF && overThreshold != 0) { // start
					// a
					// new
					// click
					clickStatus = ClickStatus.CLICK_ON;
					clickStartSample = blockStartSample + iSamp
					- clickControl.clickParameters.preSample; // subtract
					// off
					// presample
					clickTriggers = overThreshold;
					downCount = 0;
					upCount = 1;
				} else if (clickStatus == ClickStatus.CLICK_ENDING) { // either
					// resume
					// the click
					// or end it
					// properly
					if (overThreshold > 0) {
						clickStatus = ClickStatus.CLICK_ON;
						downCount = 0;
						upCount++;
					} else if (++downCount > clickControl.clickParameters.minSep) {
						clickStatus = ClickStatus.CLICK_OFF;
						clickDuration = (int) Math.min(blockStartSample + iSamp
								- clickStartSample
								+ clickControl.clickParameters.postSample,
								clickControl.clickParameters.maxLength);
						if (clickDuration <= 0) {
							System.out.println("Empty click - duration = " +
									clickDuration);
						}
						/*
						 * See if a click is already waiting completion
						 */
						if (newClick != null) {
							// System.out.println("Clashing clicks in Click
							// Detector");
						}
						newClick = new ClickDetection(groupChannels, clickStartSample, clickDuration, THIS, this, clickTriggers);

						if (clickControl.clickParameters.triggerFilter != null) {
							freqLims[0] = clickControl.clickParameters.triggerFilter.highPassFreq;
							freqLims[1] = clickControl.clickParameters.triggerFilter.lowPassFreq;
						}
						else {
							freqLims[0] = 0;
							freqLims[1] = getSampleRate()/2;
						}
						newClick.setFrequency(freqLims);
						//						newClick = new Click(THIS, clickStartSample, nChannels,
						//								clickDuration, groupChannels, clickTriggers);
						clickCompleteSample = Math.max(samplesProcessed,
								clickStartSample + clickDuration + 1);
						/*
						 * Don't actually do anything to the click now - wait until
						 * the sample number reaches the actual end of the click -
						 * this may happen while processing the next data block !
						 */

					}
				} else if (clickStatus == ClickStatus.CLICK_ON) { // either start
					// to end the
					// click, or
					// continue the
					// click
					if (overThreshold == 0) {
						clickStatus = ClickStatus.CLICK_ENDING;
					} else {
						upCount++;
						clickTriggers |= overThreshold;
					}
				}
				if (newClick != null && clickCompleteSample == samplesProcessed) {
					// now find the raw data and complete processing of the click
					if (newClick.getDuration() <= 0) {
						System.out.println("Negative duration");
						finalDataSource.dumpBlockContents();
					}
					try {
						newClick.setWaveData(finalDataSource.getSamples(
								newClick.getStartSample()-secondFilterDelay, (int) newClick.getDuration(),
								groupChannels));
						newClick.setStartSample(newClick.getStartSample()-firstFilterDelay-secondFilterDelay);
						waveDataError = false;
					}
					catch (RawDataUnavailableException e) {
						System.out.println(e.getMessage());
					}
					for (int k = 0; k < nChannels; k++) {
						if (newClick.getWaveData(k) == null){
							waveDataError = true;
						}
					}
					if (waveDataError) {
						System.out.println("Null click with no waveform data at sample " + newClick.getStartSample());
//						newClick.setWaveData(finalDataSource.getSamples(
//								newClick.getStartSample(), (int)newClick.getDuration(),
//								groupChannels));
					} 
					else {
						// newClick.delay =
						// Correlations.getDelay(newClick.GetWaveData(0),
						// newClick.GetWaveData(1), 1024);
						if (multiThread) {
							halfBuiltClicks.addPamData(newClick);
						}
						else {
							if (completeClick(newClick)) {
								outputClickData.addPamData(newClick);
								//							newClick.freeClickMemory();
							}
						}
						newClick = null;
						clickCompleteSample = -1;
					}
				}  

				if (clickControl.clickParameters.sampleNoise && samplesProcessed > nextNoiseSample) {
					ClickDetection noiseClick = new ClickDetection(groupChannels, 
							nextNoiseSample-clickControl.clickParameters.maxLength, 
							clickControl.clickParameters.maxLength, THIS, this, 0);
					noiseClick.dataType = ClickDetection.CLICK_NOISEWAVE;
					try {
						noiseClick.setWaveData(finalDataSource.getSamples(
								noiseClick.getStartSample(), (int) noiseClick.getDuration(),
								groupChannels));
					}
					catch (RawDataUnavailableException e) {
						System.out.println("Click noise measurement: " + e.getMessage());
					}
					if (multiThread) {
						halfBuiltClicks.addPamData(noiseClick);
					}
					else {
						completeClick(noiseClick);
						noiseDataBlock.addPamData(noiseClick);
						//							newClick.freeClickMemory();

					}
					// JAM testpoint
					nextNoiseSample += (long) (clickControl.clickParameters.noiseSampleInterval * getSampleRate());
				}

				samplesProcessed++;

			} // end loop over all samples
			if (clickControl.clickParameters.publishTriggerFunction) {
				for (iChan = 0; iChan < nChannels; iChan++) {
					// force an amplitude calculation
					triggerWaveDataUnit[iChan].setRawData(triggerWaveData[iChan], true);
					triggerFunctionDataBlock.addPamData(triggerWaveDataUnit[iChan]);
				}
			}

		}

		public void notifyArrayChanged() {
			groupHydrophones = rawDataSource.getChannelListManager().channelIndexesToPhones(groupChannels);
			double timingError = Correlations.defaultTimingError(getSampleRate());
			bearingLocaliser = BearingLocaliserSelector.createBearingLocaliser(groupHydrophones, timingError);
			if (bearingLocaliser == null) {
				return;
			}
			int[] phones = PamUtils.getChannelArray(groupHydrophones);
			bearingLocaliser.prepare(phones, 
					Correlations.defaultTimingError(getSampleRate()));

		}

		/**
		 * @param echoDetector the echoDetector to set
		 */
		public void setEchoDetector(EchoDetector echoDetector) {
			this.echoDetector = echoDetector;
		}

		/**
		 * @return the echoDetector
		 */
		public EchoDetector getEchoDetector() {
			return echoDetector;
		}

		/**
		 * @return the groupHydrophones
		 */
		public int getGroupHydrophones() {
			return groupHydrophones;
		}

	}

	public PamRawDataBlock[] getFilteredDataBlocks() {
		if (nChannelGroups == 0 || channelGroupDetectors == null) {
			return null;
		}
		PamRawDataBlock filterDataBlocks[] = new PamRawDataBlock[nChannelGroups];
		for (int i = 0; i < nChannelGroups; i++) {
			filterDataBlocks[i] = channelGroupDetectors[i].filteredDataBlock;
		}
		return filterDataBlocks;
	}

	public ClickTriggerFunctionDataBlock getTriggerFunctionDataBlock() {
		return triggerFunctionDataBlock;
	}

	public PamDataBlock<ClickDetection> getTrackedClicks() {
		return trackedClicks;
	}

	public Hilbert getHilbert() {
		return hilbert;
	}

	public void notifyArrayChanged() {
		for (int i = 0; i < nChannelGroups; i++) {
			channelGroupDetectors[i].notifyArrayChanged();
		}
	}

	public ChannelGroupDetector findChannelGroupDetector(int channelBitmap) {
		if (channelGroupDetectors == null) {
			return null;
		}
		for (int i = 0; i < channelGroupDetectors.length; i++) {
			if (channelGroupDetectors[i].groupChannels == channelBitmap) {
				return channelGroupDetectors[i];
			}
		}
		return channelGroupDetectors[0];
	}

	/**
	 * @return the clickBinaryDataSource
	 */
	public ClickBinaryDataSource getClickBinaryDataSource() {
		return clickBinaryDataSource;
	}

	private FFTFilter fftFilter;
	/**
	 * Get an FFT filter, mainly used to generate filtered waveforms within click detections. 
	 * @param fftFilterParams
	 * @return FFT filter object. 
	 */
	public FFTFilter getFFTFilter(FFTFilterParams fftFilterParams) {
		if (fftFilter == null) {
			fftFilter = new FFTFilter(fftFilterParams, getSampleRate());
		}
		else {
			fftFilter.setParams(fftFilterParams, getSampleRate());
		}
		return fftFilter;
	}

	public Correlations getCorrelations() {
		return correlations;
	}

	/**
	 * @return the nChannelGroups
	 */
	public int getnChannelGroups() {
		return nChannelGroups;
	}
	
	public ChannelGroupDetector getChannelGroupDetector(int i) {
		return channelGroupDetectors[i];
	}

	/**
	 * @return the tragetMotionSQLLogging
	 */
	public TargetMotionSQLLogging getTargetMotionSQLLogging() {
		return targetMotionSQLLogging;
	}

	/**
	 * @return the offlineEventLogging
	 */
	public OfflineEventLogging getOfflineEventLogging() {
		return offlineEventLogging;
	}
}
