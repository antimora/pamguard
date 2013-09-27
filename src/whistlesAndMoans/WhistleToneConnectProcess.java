package whistlesAndMoans;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import networkTransfer.receive.BuoyStatusDataUnit;

import spectrogramNoiseReduction.SpectrogramNoiseProcess;
import spectrogramNoiseReduction.SpectrogramNoiseSettings;
import eventCounter.DataCounter;
import fftManager.Complex;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;
import generalDatabase.PamDetectionLogging;
import generalDatabase.SQLLogging;
import Array.ArrayManager;
import Localiser.Correlations;
import Localiser.bearingLocaliser.BearingLocaliser;
import Localiser.bearingLocaliser.BearingLocaliserSelector;
import Localiser.bearingLocaliser.DetectionGroupLocaliser;
import PamController.PamController;
import PamDetection.AbstractLocalisation;
import PamDetection.AcousticDataUnit;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import SoundRecorder.RecorderControl;

public class WhistleToneConnectProcess extends PamProcess {

	private WhistleMoanControl whistleMoanControl;

	/**
	 * This will be the source data for the noise reduction and threshold process. 
	 */
	private FFTDataBlock sourceData;

	private ConnectedRegionDataBlock outputData;

	private PamDataBlock<WhistleToneGroupedDetection> whistleLocations;

	private ShapeConnector[] shapeConnectors;
	
	protected DataCounter dataCounter;

	/**
	 * total number of shape connectors. 
	 */
	private int nConnectors;

	private WhistleDetectionGrouper detectionGrouper;

	private DetectionGroupLocaliser detectionGroupLocaliser;

	private WhistleToneConnectProcess THAT;

	private WhistleToneLogging whistleToneLogging;
	
	private static final int NSUMMARYPOINTS = 4;
	
	private int summaryBlockSize = 1;
	
	private int[] whistleSummaryCount = new int[NSUMMARYPOINTS];
	
	private WMRecorderTrigger wmRecorderTrigger;

	private int channelMap;

	public WhistleToneConnectProcess(WhistleMoanControl whitesWhistleControl) {
		super(whitesWhistleControl, null);
		this.whistleMoanControl = whitesWhistleControl;
		THAT = this;

		outputData = new ConnectedRegionDataBlock(whistleMoanControl.getUnitName() + " Connected Regions", this, 0);
		addOutputDataBlock(outputData);
		outputData.setOverlayDraw(new CROverlayGraphics(outputData, whitesWhistleControl));
		//		outputData.setNaturalLifetime(5);
		outputData.SetLogging(whistleToneLogging = new WhistleToneLogging(whitesWhistleControl, outputData, SQLLogging.UPDATE_POLICY_WRITENEW));
		outputData.setBinaryDataSource(new WhistleBinaryDataSource(this, outputData, "Contours"));
		outputData.setDatagramProvider(new WMDatagramProvider(outputData));
		outputData.setCanClipGenerate(true);
//		addBinaryDataSource(outputData.getSisterBinaryDataSource());

		detectionGrouper = new WhistleDetectionGrouper(whitesWhistleControl, outputData);

		whistleLocations = new PamDataBlock<WhistleToneGroupedDetection>(WhistleToneGroupedDetection.class,
				"Localised Tones and Whistles", this, 
				whitesWhistleControl.whistleToneParameters.getChannelBitmap());
		whistleLocations.setOverlayDraw(new WhistleToneLocalisationGraphics(whistleLocations));
		addOutputDataBlock(whistleLocations);
		whistleLocations.setLocalisationContents(AbstractLocalisation.HAS_BEARING | AbstractLocalisation.HAS_RANGE |
				AbstractLocalisation.HAS_LATLONG | AbstractLocalisation.HAS_PERPENDICULARERRORS| AbstractLocalisation.HAS_AMBIGUITY);
		whistleLocations.SetLogging(new PamDetectionLogging(whistleLocations, SQLLogging.UPDATE_POLICY_WRITENEW));

		detectionGroupLocaliser = new DetectionGroupLocaliser(this);
		
		dataCounter = new DataCounter(whitesWhistleControl.getUnitName(), outputData, 60);
		dataCounter.setEventTrigger(60, 10);
		dataCounter.setShortName("");
		
		wmRecorderTrigger = new WMRecorderTrigger(whitesWhistleControl);
		outputData.setRecordingTrigger(wmRecorderTrigger);
	}


	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		FFTDataUnit fftDataUnit = (FFTDataUnit) arg;
		int chan = PamUtils.getSingleChannel(fftDataUnit.getChannelBitmap());
		for (int i = 0; i < nConnectors; i++) {
			if (shapeConnectors[i].firstChannel == chan) {
				shapeConnectors[i].newData(fftDataUnit.getFftSlice(), fftDataUnit);
			}
		}
	}

	@Override
	public void pamStart() {
		
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setupProcess() {
		super.setupProcess();
		SpectrogramNoiseProcess snp = whistleMoanControl.getSpectrogramNoiseProcess();
		setParentDataBlock(snp.getOutputDataBlock());
		if (whistleMoanControl.whistleToneParameters.getDataSource() == null) {
			return;
		}
		sourceData = (FFTDataBlock) PamController.getInstance().getDataBlock(FFTDataUnit.class, 
				whistleMoanControl.whistleToneParameters.getDataSource());
//		snp.setParentDataBlock(sourceData);
		SpectrogramNoiseSettings specnoiseSettings = whistleMoanControl.whistleToneParameters.getSpecNoiseSettings();
		specnoiseSettings.dataSource = whistleMoanControl.whistleToneParameters.getDataSource();
		snp.setNoiseSettings(specnoiseSettings);

		channelMap = whistleMoanControl.whistleToneParameters.getChannelBitmap();
		if (sourceData != null) {
			channelMap = getParentDataBlock().getChannelMap() & 
			whistleMoanControl.whistleToneParameters.getChannelBitmap();
			outputData.setChannelMap(channelMap);
			outputData.setFftHop(sourceData.getFftHop());
			outputData.setFftLength(sourceData.getFftLength());
			//			smoothingChannelProcessList = new SmoothingChannelProcess[PamUtils.getHighestChannel(channelMap)+1];
			//			for (int i = 0; i < PamUtils.getHighestChannel(channelMap)+1; i++) {
			//				smoothingChannelProcessList[i] = new SmoothingChannelProcess();
			//			}
		}
		// set the localisation information in the two output datablocks. 
		boolean mayBearings = whistleMoanControl.whistleToneParameters.mayHaveBearings();
		boolean mayRange = whistleMoanControl.whistleToneParameters.mayHaveRange();
		if (mayBearings) {
			outputData.setLocalisationContents(AbstractLocalisation.HAS_BEARING);
			whistleLocations.setLocalisationContents(AbstractLocalisation.HAS_BEARING);
		}
		else {
			outputData.setLocalisationContents(0);
			whistleLocations.setLocalisationContents(0);
		}
		if (mayRange) {
			whistleLocations.setLocalisationContents(AbstractLocalisation.HAS_BEARING |
					AbstractLocalisation.HAS_RANGE);
		}
		if (mayBearings || mayRange) {
			outputData.SetLogging(whistleToneLogging = new WhistleToneLogging(whistleMoanControl, 
					outputData, SQLLogging.UPDATE_POLICY_WRITENEW));
			whistleToneLogging.reCheckTable();
		}
		
		prepareProcess();
	}

	@Override
	public void prepareProcess() {
		super.prepareProcess();
		nConnectors = whistleMoanControl.whistleToneParameters.countChannelGroups();
		shapeConnectors = new ShapeConnector[nConnectors];
		int groupChannels;
		for (int i = 0; i < nConnectors; i++) {
			groupChannels = whistleMoanControl.whistleToneParameters.getGroupChannels(i);
			shapeConnectors[i] = new ShapeConnector(i, groupChannels, 
					whistleMoanControl.whistleToneParameters.getConnectType());
			shapeConnectors[i].initialise();
		}
		detectionGrouper.setupGroups(whistleMoanControl.whistleToneParameters);
		
		clearSummaryData();
	}

	protected int getFFTLen() {
		if (sourceData != null) {
			return sourceData.getFftLength();
		}
		return 1;
	}
	
	/**
	 * Find the appropriate shape connector for the given channels. 
	 * The first time this gets called in viewer mode, there
	 * will be nothing there,so call setupProcess to create them.  
	 * @param channelMap channel bitmap we're looking for. 
	 * @return a ShapeConnector or null if the channelMap doesn't 
	 * match any existing connectors for this configuration. 
	 */
	public ShapeConnector findShapeConnector(int channelMap) {
		if (shapeConnectors == null || shapeConnectors[0] == null) {
			setupProcess();
		}
		for (int i = 0; i < nConnectors; i++) {
			if ((shapeConnectors[i].groupChannels & channelMap) != 0) {
				return shapeConnectors[i];
			}
		}
		return null;
	}

	public class ShapeConnector {

		private ConnectedRegion[][] regionArray = new ConnectedRegion[2][];
		private boolean[] spacedArray;

		final private int[] search8x = {1, 0, 0, 0};
		final private int[] search8y = {-1, -1, 0, 1};
		final private int[] search4x = {1, 0};
		final private int[] search4y = {-1, 0};
		private int[] searchx; 
		private int[] searchy;

		private int groupChannels;
		private int firstChannel;
		private int iD;
		private boolean[] newCol;
		
		int searchBin1, searchBin2;

		int regionNumber = 0;

		private WhistleDelays whistleDelays;

		LinkedList<ConnectedRegion> growingRegions;
		LinkedList<ConnectedRegion> recycleRegions;

		private RegionFragmenter regionFragmenter = new NullFragmenter(); 
		
		private BearingLocaliser bearingLocaliser;
		private int hydrophoneMap;
		private double maxDelaySeconds;

		ShapeConnector(int iD, int groupChannels, int connectType) {
			this.iD = iD;
			this.groupChannels = groupChannels;
			hydrophoneMap = groupChannels;
			
			this.firstChannel = PamUtils.getLowestChannel(groupChannels);
			setConnectionType(connectType);
			growingRegions = new LinkedList<ConnectedRegion>();
			recycleRegions = new LinkedList<ConnectedRegion>();
			whistleDelays = new WhistleDelays(whistleMoanControl, groupChannels);
			switch(whistleMoanControl.whistleToneParameters.fragmentationMethod) {
			case WhistleToneParameters.FRAGMENT_NONE:
				regionFragmenter = new NullFragmenter();
				break;
			case WhistleToneParameters.FRAGMENT_DISCARD:
				regionFragmenter = new DiscardingFragmenter();
				break;
			case WhistleToneParameters.FRAGMENT_FRAGMENT:
				regionFragmenter = new FragmentingFragmenter(whistleMoanControl);
				break;
			case WhistleToneParameters.FRAGMENT_RELINK:
				regionFragmenter = new RejoiningFragmenter(whistleMoanControl);
				break;
			}
		}

		private void initialise() {
			regionNumber = 0;
			if (sourceData == null) {
				return;
			}
			if (sourceData.getChannelListManager() != null) {
				hydrophoneMap = sourceData.getChannelListManager().channelIndexesToPhones(groupChannels);
				double timingError = Correlations.defaultTimingError(getSampleRate());
				bearingLocaliser = BearingLocaliserSelector.createBearingLocaliser(hydrophoneMap, timingError); 
				maxDelaySeconds = ArrayManager.getArrayManager().getCurrentArray().getMaxPhoneSeparation(hydrophoneMap) / 
									ArrayManager.getArrayManager().getCurrentArray().getSpeedOfSound();
			}
			whistleDelays.prepareBearings();
			searchBin1 = (int) (whistleMoanControl.whistleToneParameters.getMinFrequency() * 
					sourceData.getFftLength() / getSampleRate());			
			searchBin2 = (int) (whistleMoanControl.whistleToneParameters.getMaxFrequency(getSampleRate()) * 
					sourceData.getFftLength() / getSampleRate());
			searchBin1 = Math.max(0, Math.min(sourceData.getFftLength()/2, searchBin1));
			searchBin2 = Math.max(0, Math.min(sourceData.getFftLength()/2, searchBin2));
		}

		/**
		 * Gets passed a row of Complex data. 
		 * Converts to boolean array and passes to 
		 * boolean function with same name. 
		 * @param complexData array of Complex data. 
		 */
		public void newData(int iSlice, FFTDataUnit fftDataUnit) {
			Complex[] complexData = fftDataUnit.getFftData();
			if (newCol == null || newCol.length != complexData.length) {
				newCol = new boolean[complexData.length];
			}
			for (int i = searchBin1; i < searchBin2; i++) {
				newCol[i] = complexData[i].magsq() > 0;
			}
			newData(iSlice, newCol, fftDataUnit);
		}


		/**
		 * Gets passed a row of boolean values representing
		 * thresholded fft data.  
		 * @param newData boolean array. 
		 */
		private void newData(int iSlice, boolean[] newData, FFTDataUnit fftDataUnit) {
			// need space either side for region search. 
			int dataLen = newData.length;
			int regLen = dataLen + 2;
			int space = 1;
			ConnectedRegion connectedRegion, thisRegion;
			
			if (regionArray == null || regionArray[0] == null || regionArray[1] == null ||
					regionArray[1].length != regLen || regionArray[0].length != regLen) {
				regionArray = new ConnectedRegion[2][regLen];
			}
			if (spacedArray == null || spacedArray.length != regLen) {
				spacedArray = new boolean[regLen];
			}
			// now copy the data into the spaced array. 
			for (int i = 0; i < dataLen; i++) {
				spacedArray[i+space] = newData[i];
			}

			// flag all regions as NOT growing so that finished
			// ones can be sent on their way.
			labelGrowing(false);

			// loop over new data
			for (int i = space; i < dataLen+space; i++) {
				if (spacedArray[i] == false) {
					continue; // continue of this pixel is not set. 
				}
				/*
				 * Loop over search pixels. There are three options
				 * here.
				 * 1) it connects to nothing, so it's a new region
				 * 2) it connects to something immediately, but isn't assigned 
				 * to anything itself yet. 
				 * 3) it's already assigned itself due to either 1 or 2 above
				 * but then needs to re-assign
				 */
				thisRegion = null;
				for (int si = 0; si < searchx.length; si++) {
					connectedRegion = regionArray[searchx[si]][i+searchy[si]];
					if (connectedRegion == null) {
						// new region. Don't actually do anything yet !
					}
					else if (thisRegion == null) {
						/* 
						 * This cell is not yet assigned to any region, 
						 * so add it to the one we've just found. 
						 */
						thisRegion = connectedRegion;
						connectedRegion.addPixel(iSlice, i-space, fftDataUnit);
					}
					else if (thisRegion != connectedRegion){
						/*
						 * The cell is already assigned to a region, but 
						 * that region is not the same as this region, so 
						 * two regions need to be merged. 
						 * Take the first and merge the second onto it.  
						 */
						thisRegion = mergeRegions(thisRegion, connectedRegion);
						//						thisRegion.mergeRegion(connectedRegion);
					}
					regionArray[1][i] = thisRegion;
					/*
					 * Now pop back down the column, to include anything that is touching
					 * this region, but didn't get assigned. 
					 */
					for (int ii = i-1; ii >0; ii--) {
						if (spacedArray[ii] == false || regionArray[1][ii] != null) {
							break;
						}
						regionArray[1][ii] = regionArray[1][i];
					}
				} // end of search loop around this pixel
			} // end of loop up slice
			/**
			 * Since isolate pixels which did not connect to a region in the preceeding slice
			 * were ignored in the previous iteration (to avoid too much object creation) now
			 * need to run up the column again creating new regions. 
			 */
			for (int i = space; i < dataLen+space; i++) {
				if (spacedArray[i] == false || regionArray[1][i] != null) {
					continue; // continue of this pixel is not set. 
				}
				/*
				 * See if it links in with anything underneath it.
				 */
				if (regionArray[1][i-1] != null) {
					regionArray[1][i] = regionArray[1][i-1];
				}
				else {
					/*
					 *  it's only here that we actually create new regions.
					 *  i.e. at the bottom of a group of regions which have already 
					 *  been shown not to connect to any others.  
					 */
					regionArray[1][i] = createNewRegion(iSlice, i-space, dataLen, fftDataUnit);
				}
			}

			findCompleteRegions();

			// now shuffle the region arrays along and create a blank one ready for next 
			// call
			regionArray[0] = regionArray[1];
			regionArray[1] = new ConnectedRegion[regLen];
		}

		/**
		 * Merge two regions together. Merge the one that started second onto the one
		 * that started first to ensure that all slices are present in the master region
		 * @param r1 region 1
		 * @param r2 region 2
		 * @return reference to the remaining region. The other get's binned or recycled. 
		 */
		private ConnectedRegion mergeRegions(ConnectedRegion r1, ConnectedRegion r2) {
			ConnectedRegion m, s;
			if (r1.getFirstSlice() < r2.getFirstSlice()) {
				m = r1;
				s = r2;
			}
			else {
				m = r2;
				s = r1;
			}

			m.mergeRegion(s);

			removeRegion(s);

			int nCol = regionArray.length;
			int nRow = regionArray[0].length;
			for (int iCol = 0; iCol < nCol; iCol++) {
				for (int iRow = 0; iRow < nRow; iRow++) {
					if (regionArray[iCol][iRow] == s) {
						regionArray[iCol][iRow] = m;
					}
				}
			}
			return m;
		}
		/**
		 * Will eventually set a recycling scheme, but not now. 
		 * @param iSlice
		 * @param iCell
		 * @return new or recycled region
		 */
		private ConnectedRegion createNewRegion(int iSlice, int iCell, int dataLen, FFTDataUnit fftDataUnit) {
			ConnectedRegion newRegion;
			if (recycleRegions.size() > 0) {
				newRegion = recycleRegions.removeLast();
				newRegion.resetRegion(firstChannel, iSlice, regionNumber++, dataLen);
			}
			else {
				newRegion = new ConnectedRegion(firstChannel, iSlice, regionNumber++, dataLen);
			}
			newRegion.addPixel(iSlice, iCell, fftDataUnit);
			growingRegions.add(newRegion);
			return newRegion;
		}

		private void removeRegion(ConnectedRegion r) {
			growingRegions.remove(r);
			recycleRegion(r);
		}

		/**
		 * Remove a region from the growing list and recyce it. 
		 * @param r region to remove. 
		 */
		private void recycleRegion(ConnectedRegion r) {
			if (recycleRegions.size() < 20) {
				r.recycle();
				recycleRegions.add(r);
			}
		}


		private void labelGrowing(boolean growing) {
			ListIterator<ConnectedRegion> rl = growingRegions.listIterator();
			while(rl.hasNext()) {
				rl.next().setGrowing(growing);
			}
		}

		private void findCompleteRegions() {
			ListIterator<ConnectedRegion> rl = growingRegions.listIterator();
			ConnectedRegion r;
			while(rl.hasNext()) {
				r=rl.next();
				if (r.isGrowing() == false) {
					rl.remove();
					if (completeRegion(r) == false) {
						recycleRegion(r);
					}
				}
			}
		}

		private boolean completeRegion(ConnectedRegion region) {
			if (!wantRegion(region)) {
				return false;
			}
			//				region.sayRegion();
			region.condenseInfo();

			regionFragmenter.fragmentRegion(region);
			int nFrag = regionFragmenter.getNumFragments();
			//				if (nFrag == 5) {
			//					region.sayRegion();
			//					region.sayRegion();
			//					nFrag = regionFragmenter.fragmentRegion(region);
			//				}
			for (int i = 0; i < nFrag; i++) {
				region = regionFragmenter.getFragment(i);
				completeRegionFragment(region);
			}
			return nFrag > 0;
		}
		
		private boolean wantRegion(ConnectedRegion region) {
			// first two checks on size of whistle. 
			if (region.getTotalPixels() < whistleMoanControl.whistleToneParameters.minPixels) {
				return false;
			}
			if (region.getNumSlices() < whistleMoanControl.whistleToneParameters.minLength) {
				return false;
			}
			// then bin any whistles in first 1/2 second since they are often noisy. 
			if (region.getStartSample() < getSampleRate()/4) {
				return false;
			}
			
			return true;
		}
		
		private void completeRegionFragment(ConnectedRegion region) {
			ConnectedRegionDataUnit newData = new ConnectedRegionDataUnit(region, THAT);
			double[] delays = whistleDelays.getDelays(region);
			double[][] anglesAndErrors = null;
			if (delays != null) {
				for (int i = 0; i < delays.length; i++) {
					delays[i] /= -getSampleRate();
				}
				if (bearingLocaliser != null) {
					anglesAndErrors = bearingLocaliser.localise(delays);
				}
			}

//			int hydrophoneList = ((AcquisitionProcess) getSourceProcess()).
//			getAcquisitionControl().ChannelsToHydrophones(groupChannels);
			if (delays != null) {
				WhistleBearingInfo newLoc = new WhistleBearingInfo(newData, bearingLocaliser, 
						hydrophoneMap, anglesAndErrors);
				if (bearingLocaliser != null) {
					newLoc.setArrayAxis(bearingLocaliser.getArrayAxis());
				}
//				System.out.println("Timing delay = " + delays[0] + " channels " + groupChannels);
				newData.setTimeDelaysSeconds(delays);
				newData.setLocalisation(newLoc);
			}
			// work out the amplitude
			double a = region.calculateRMSAmplitude();
//			System.out.println("RMS amplitude = " + a);
			//				region.sayRegion();
			newData.setMeasuredAmplitude(a, AcousticDataUnit.AMPLITUDE_SCALE_LINREFSD);
			ArrayList<ConnectedRegionDataUnit> matchedUnits = detectionGrouper.findGroups(newData);
			if (matchedUnits != null && matchedUnits.size() == 1) {
				// have one matched whistle, so try to get a location from it
				//					System.out.println("Matched unit found");
				WhistleToneGroupedDetection wgd = new WhistleToneGroupedDetection(matchedUnits.get(0));
				wgd.addSubDetection(newData);
				boolean ok1 = detectionGroupLocaliser.localiseDetectionGroup(wgd, 1);
				boolean ok2 = detectionGroupLocaliser.localiseDetectionGroup(wgd, -1);
				if (ok1 || ok2) {
					whistleLocations.addPamData(wgd);
					//						newData.setLocalisation(wgd.getLocalisation());
				}
			}
			summariseWhistle(newData);
			outputData.addPamData(newData);
		}
		public void setConnectionType(int searchType) {
			if (searchType == 4) {
				searchx = search4x;
				searchy = search4y;
			}
			else {
				searchx = search8x;
				searchy = search8y;
			}
		}

		/**
		 * @return the bearingLocaliser
		 */
		public BearingLocaliser getBearingLocaliser() {
			return bearingLocaliser;
		}

		/**
		 * @return the groupChannels
		 */
		public int getGroupChannels() {
			return groupChannels;
		}
	}

	public ConnectedRegionDataBlock getOutputData() {
		return outputData;
	}


	private synchronized void summariseWhistle(ConnectedRegionDataUnit newData) {
		ConnectedRegion cr = newData.getConnectedRegion();
		int nS = cr.getNumSlices();
		int nP;
		List<SliceData> sliceData = cr.getSliceData();
//		SliceData aSlice;
		int sumBin;
		for (SliceData aSlice:sliceData) {
			nP = aSlice.nPeaks;
			for (int i = 0; i < nP; i++) {
				sumBin = aSlice.peakInfo[i][1] / summaryBlockSize;
				whistleSummaryCount[sumBin]++;
			}
		}	
	}


	private void clearSummaryData() {
		if (sourceData == null) {
			return;
		}
		int fftLen = sourceData.getFftLength();
		summaryBlockSize = fftLen / 2 / NSUMMARYPOINTS;
		for (int i = 0; i < NSUMMARYPOINTS; i++) {
			whistleSummaryCount[i] = 0;
		}
	}


	public String getModuleSummary() {
		String sumText = String.format("%d", NSUMMARYPOINTS);
		for (int i = 0; i < NSUMMARYPOINTS; i++) {
			sumText += String.format(",%d",whistleSummaryCount[i]);
		}
		
		clearSummaryData();
		return sumText;
	}


	/**
	 * When delay data are written to binary files, int16's are used, but these
	 * must be scaled up to allow for sub-sample timing. How much they can be 
	 * scaled depends a lot on the array spacing, sample rate and FFT length. 
	 * @return scale factor which will give the highest timing resolution without overflows. 
	 */
	public int getDelayScale() {
		// work out the max delay and then work out a scaling factor. 
		double maxSep = 0;
		double sep;
		for (int i = 0; i < nConnectors; i++) {
			sep = shapeConnectors[i].maxDelaySeconds;
			maxSep = Math.max(maxSep, sep);
		}
		maxSep *= getSampleRate();
		if (maxSep == 0) return 1;
		maxSep *= 1.1;
		int delayScale = 1;
		while (maxSep * delayScale < 16384) {
			delayScale *= 2;
		}
		return delayScale;
	}


	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#processNewBuoyData(networkTransfer.receive.BuoyStatusDataUnit, PamguardMVC.PamDataUnit)
	 */
	@Override
	public void processNewBuoyData(BuoyStatusDataUnit statusDataUnit,
			PamDataUnit dataUnit) {
		ConnectedRegionDataUnit crdu = (ConnectedRegionDataUnit) dataUnit;
		ShapeConnector shapeConnector = findShapeConnector(channelMap);
		if (shapeConnector != null) {
			BearingLocaliser bl = shapeConnector.getBearingLocaliser();
			if (bl != null) {
				double[][] angles = bl.localise(crdu.getTimeDelaysSeconds());
				if (angles != null) {
					WhistleBearingInfo newLoc = new WhistleBearingInfo(crdu, bl, 
							shapeConnector.getGroupChannels(), angles);
					newLoc.setArrayAxis(bl.getArrayAxis());
//					newLoc.set
					newLoc.setSubArrayType(bl.getArrayType());
					crdu.setLocalisation(newLoc);
				}
			}
		}
	}

}
