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

/*
 * subclass of PamDataBlock specifically for raw ADC data. 
 * Contains additional variables needed to describe the raw data. 
 * Each PamRawDataBlock will hold the input from a single file or device in 
 * an ArrayList of PamDataUnits
 */
package PamguardMVC;

import java.util.ListIterator;

import Acquisition.AcquisitionProcess;
import Acquisition.RawDataBinaryDataSource;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;

/**
 * Extension of RecyclingDataBlock that is used for Raw audio data. 
 * <p>
 * Has the extra function for getting raw data samples out of the blocks. Also
 * has some special constructors that set the parent and source data blocks to
 * null. However, Raw data my be poduced by intermediate processes as well, in
 * which case parent and source blocks will not be null
 * 
 * @author Doug Gillespie
 *
 */
public class PamRawDataBlock extends AcousticDataBlock<RawDataUnit> {
	
	private long desiredSample = -1;
	private long[] prevChannelSample = new long[PamConstants.MAX_CHANNELS];

	/**
	 * Keep a record of the last sample added. 
	 */
//	long latestSample = 0;
	
	@Override
	protected int removeOldUnitsT(long currentTimeMS) {
		// TODO Auto-generated method stub
		if (pamDataUnits.isEmpty())
			return 0;
		int n = super.removeOldUnitsT(currentTimeMS);
//		checkIntegrity();
		return n;
	}

	/**
	 * Check the data block integrity - that is that all units are
	 * in order and that the sample numbers increase correctly.
	 * <p>This is used when loading data offline. 
	 * @return
	 */
	synchronized private boolean checkIntegrity() {
		int nChannels = PamUtils.getNumChannels(getChannelMap());
		int errors = 0;
		int[] channelList = new int[nChannels];
		for (int i = 0; i < nChannels; i++) {
			channelList[i] = PamUtils.getNthChannel(i, getChannelMap());
		}
		int expectedChannel = channelList[0];
		ListIterator<RawDataUnit> iterator = getListIterator(0);
		RawDataUnit dataUnit;
		long[] expectedSample = new long[PamConstants.MAX_CHANNELS];
		int singleChannel;
		int channelIndex = 0;
		int unitIndex = 0;
		while (iterator.hasNext()) {
			expectedChannel = channelList[channelIndex];
			dataUnit = iterator.next();
			// check it's the expected channel. 
			singleChannel = PamUtils.getSingleChannel(dataUnit.channelBitmap);
			if (singleChannel != expectedChannel) {
				reportProblem(++errors, unitIndex, String.format("Got channel %d, expected %d", singleChannel, expectedChannel)
						, dataUnit);
			}
			
			// check the sample number
			if (expectedSample[channelIndex] > 0) {
				if (expectedSample[channelIndex] != dataUnit.getStartSample()) {
					reportProblem(++errors, unitIndex, String.format("Got sample %d, expected %d", 
							dataUnit.getStartSample(), expectedSample[channelIndex]), dataUnit);
				}
			}
			
			// check the length
			if (dataUnit.getDuration() != dataUnit.getRawData().length) {
				reportProblem(++errors, unitIndex, String.format("Have %d samples, expected %d", 
						dataUnit.getDuration(), dataUnit.getRawData().length), dataUnit);
			}
			
			// move expectations.
			expectedSample[channelIndex] = dataUnit.getStartSample() + dataUnit.getDuration();
			if (++channelIndex >= nChannels) {
				channelIndex = 0;
			}
			unitIndex++;
		}
		
		return errors == 0;
	}
	private void reportProblem(int nErrors, int index, String str, RawDataUnit unit) {
		System.out.println(String.format("Error %d in RawDataBlock item %d of %d: %s", 
				nErrors, index, getUnitsCount(), str));
		System.out.println(unit.toString());
	}
	public PamRawDataBlock(String name, PamProcess parentProcess, 
			int channelMap, float sampleRate) {
		super(RawDataUnit.class, name, parentProcess, channelMap);
		new RawDataDisplay(this);
		setBinaryDataSource(new RawDataBinaryDataSource(this));
	}
	
	protected PamRawDataBlock(String name, PamProcess parentProcess, 
			int channelMap, float sampleRate, boolean autoDisplay) {
		super(RawDataUnit.class, name, parentProcess, channelMap);
		if (autoDisplay) {
			new RawDataDisplay(this);
		}
	}
	
	@Override
	public void addPamData(RawDataUnit pamDataUnit) {
		/*
		 *  need to do a few extra tests to check that data are arriving in the
		 *  correct channel order before data can be added to the list. 
		 *  The danger here occurs primarily when running in net receiver mode
		 *  where after a disconnect, some data may have been dumped to avoid buffer overflow. 
		 *  The problem then is that data may start at some random channel number, not
		 *  on channel 0. 
		 *  <p>
		 *  <p>
		 *  The fix in here could make things a lot worse though in some situations ???? 
		 */
		int firstChannel = PamUtils.getLowestChannel(getChannelMap());
		int thisChannel = PamUtils.getSingleChannel(pamDataUnit.channelBitmap);
		if (thisChannel == firstChannel) {
			desiredSample = pamDataUnit.getStartSample();
		}
		else if (desiredSample != pamDataUnit.getStartSample()) {
			// don't add this data unit since its probably out of synch
			System.out.println(String.format("Sample %d channel %d in %s out of synch - expected sample %d, previously got %d",
					pamDataUnit.getStartSample(), thisChannel, getDataName(), desiredSample, prevChannelSample[thisChannel]));
//			return; add the data anyway, may get back into synch !!!! 
		}
		prevChannelSample[thisChannel] = pamDataUnit.getStartSample();
//		System.out.println(String.format("Sample %d channel %d in %s is in  synch - expected sample %d",
//				pamDataUnit.getStartSample(), thisChannel, getDataName(), desiredSample));
		
		super.addPamData(pamDataUnit);
	}

	/**
	 * Creates an array and fills it with raw data samples. 
	 * @param startSample
	 * @param duration
	 * @param channelMap
	 * @return double array of raw data
	 */
	public double[][] getSamples(long startSample, int duration, int channelMap) throws RawDataUnavailableException {
		// run  a few tests ...
		int chanOverlap = channelMap & getChannelMap();
		if (chanOverlap != channelMap) {
			throw new RawDataUnavailableException(this, RawDataUnavailableException.INVALID_CHANNEL_LIST);
		}
		RawDataUnit dataUnit = getFirstUnit();
		if (dataUnit == null) {
			return null;
		}
		if (dataUnit.getStartSample() > startSample) {
			throw new RawDataUnavailableException(this, RawDataUnavailableException.DATA_ALREADY_DISCARDED);
		}
		dataUnit = getLastUnit();
		if (dataUnit.getStartSample() + dataUnit.getDuration() < startSample + duration) {
			throw new RawDataUnavailableException(this, RawDataUnavailableException.DATA_NOT_ARRIVED);
		}
		
		int nChan = PamUtils.getNumChannels(channelMap);
		double[][] wavData = new double[nChan][duration];
		if (getTheSamples(startSample, duration, channelMap, wavData)) {
			return wavData;
		}
		return null;
	}
	
	/**
	 * Gets samples of raw data into a pre existing array. If the array is the wrong
	 * size or does not exist, then a new one is created. 
	 * @param startSample
	 * @param duration
	 * @param channelMap
	 * @param wavData
	 * @return double array of raw data
	 */
	public double[][] getSamples(long startSample, int duration, int channelMap, double[][] wavData) {
		int nChan = PamUtils.getNumChannels(channelMap);
		if (duration < 1) return null;
		if (wavData == null || nChan != wavData.length || duration != wavData[0].length) {
			wavData = new double[nChan][duration];
		}
		if (getTheSamples(startSample, duration, channelMap, wavData)) {
			return wavData;
		}
		return null;
	}

	/**
	 * Does the work for the above two functions.
	 * @param startSample
	 * @param duration
	 * @param channelMap
	 * @param waveData
	 * @return copies data into a double array, taking if from multiple raw datablocks
	 * if necessary
	 */
	synchronized private boolean getTheSamples(long startSample, int duration, int channelMap,
			double[][] waveData) {
		// find the first data block
//		int blockNo = -1;
		ListIterator<RawDataUnit> rawIterator = pamDataUnits.listIterator();
		RawDataUnit unit = null;
		if (pamDataUnits.size() == 0) {
			return false;
		}
		boolean foundStart = false;
		while (rawIterator.hasNext()) {
			unit = rawIterator.next();
			if (unit.getLastSample() >= startSample) {
				foundStart = true;
				break;
			}
		}
//		for (int i = 0; i < pamDataUnits.size(); i++) {
//			if (pamDataUnits.get(i).getLastSample() >= startSample) {
//				blockNo = i;
//				break;
//			}
////		}
		if (foundStart == false) {
			return false;
		}
//		if (blockNo < 0) {
////			System.out.println("start sample always after last data block");
//			return false;
//		}
		if (startSample < unit.getStartSample()) {
//			System.out.println("start sample always less than data block start");
			return false;
		}

		int nChan = PamUtils.getNumChannels(channelMap);
		int iChan;
		int outChan;
		double[] unitData;
		int offset;
		int completeChannels = 0;
		int[] channelSamples = new int[nChan]; // will need to keep an eye on
												// how many samples we have for
												// each channel
//		RawDataUnit unit = pamDataUnits.get(blockNo);
//		RawDataUnit prevUnit = null;
//		if (startSample == 7309272) {
//			System.out.println("About to crash");
//		}
		while (true) {
			if ((unit.getChannelBitmap() & channelMap) > 0) {
				iChan = PamUtils.getSingleChannel(unit.getChannelBitmap());
				outChan = PamUtils.getChannelPos(iChan, channelMap);
				unitData = unit.getRawData();
				/*
				 * This will have to be improved upon when the channel numbers
				 * are no longer just 0 and 1
				 */
				offset = (int) (startSample - unit.getStartSample())
						+ channelSamples[outChan];
				while (channelSamples[outChan] < duration
						&& offset < unitData.length) {
					// put some checks in here to see why it crashes ...
					if (offset < 0){
						System.out.println("Negative value for offset " + offset  + " samples " + channelSamples[outChan]);
						checkIntegrity();
						return false;
					}
					if (offset >= unitData.length) {
						System.out.println("Taking data from beyond end of array - will crash !");
					}
					if (waveData.length <= outChan){
						System.out.println("Not enough channels in waveData - will crash !");
					} 
					if (channelSamples.length <= outChan){
						System.out.println("Not enough entries in channel LUT - will crash !");
					} 
					if (waveData[0].length <= channelSamples[outChan]){
						System.out.println("Not enough samples in waveData - will crash !");
					} 
					waveData[outChan][channelSamples[outChan]] = unitData[offset];
					channelSamples[outChan]++;
					offset++;
				}
				if (channelSamples[outChan] == duration) {
					completeChannels |= unit.getChannelBitmap();
					if (completeChannels == channelMap) {
						return true;
					}
				}
			}

			/** 
			 * if we get here, then we still need more data, but there
			 * may not be any, so may have to bail out. 
			 */
			if (rawIterator.hasNext() == false) {
				return false;
			}
			unit = rawIterator.next();
		}
	}

	
	@Override
	public ChannelListManager getChannelListManager() {
		if (getParentSourceData() != null) {
//			System.out.println("Good ... " + getDataName() + " uses a higher level channel list manager !!!");
			return getParentSourceData().getChannelListManager();
		}
		// must be acquisition module, so return the master manager. 
		if (AcquisitionProcess.class.isAssignableFrom(parentProcess.getClass())) {
//			System.out.println("Good ... " + getDataName() + " has a channel list manager !!!");
			return ((AcquisitionProcess) parentProcess).getAcquisitionControl().getDaqChannelListManager();
		}
		System.out.println("Error ... " + getDataName() + " has no channel list manager !!!");
		return null;
	}

//	@Override
//	protected void findParentSource() {
//		super.findParentSource();
//		if (getParentSourceData() == null) {
//			System.out.println(getDataName() + " has no source data");
//		}
//	}

}
