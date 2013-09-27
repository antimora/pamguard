/*
 *  PAMGUARD - Passive Acoustic Monitoring GUARDianship.
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


package rocca;

import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import Spectrogram.SpectrogramDisplay;
import Spectrogram.SpectrogramMarkObserver;
import Spectrogram.SpectrogramMarkObservers;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;


public class RoccaWhistleSelect extends PamProcess implements SpectrogramMarkObserver {

    RoccaControl roccaControl;
    boolean startSelected = false;
    boolean endSelected = false;
    double startFreq = 0.0;
    double endFreq = 0.0;
    long startTime = 0;
    long endTime = 0;

	public RoccaWhistleSelect(RoccaControl roccaControl) {
		super(roccaControl, null);
        this.roccaControl = roccaControl;
		SpectrogramMarkObservers.addSpectrogramMarkObserver(this);
	}

	public void spectrogramNotification(SpectrogramDisplay display, int downUp, int channel,
			long startMilliseconds, long duration, double f1, double f2) {
        
        if (downUp == MOUSE_DOWN) {
            startSelected = true;
            endSelected = false;
            startFreq = f1;
            startTime = startMilliseconds;
            roccaControl.roccaSidePanel.setStartOfWhistle(startTime, startFreq);
//            System.out.println(String.format(
//                    "Mouse Down - Starting frequency is %3.1f at %d ms.  Duration is %d ms.  f2 is %3.1f", startFreq, startTime, duration, f2));


        } else if (downUp == MOUSE_UP) {
            endSelected = true;
            startFreq = f1;
            endFreq = f2;
            
            /* if the user has selected the whistle from right-to-left, reset
             * the startTime to the earlier value
             */
            if (startMilliseconds<startTime) {
                startTime  = startMilliseconds;
            }
            endTime = startMilliseconds+duration;
            roccaControl.roccaSidePanel.setStartOfWhistle(startTime, startFreq);
            roccaControl.roccaSidePanel.setEndOfWhistle(endTime, endFreq);
//            System.out.println(String.format(
//                    "Mouse Up - Starting frequency is %3.1f at %d ms.  Duration is %d ms.  f2 is %3.1f", startFreq, startTime, duration, f2));

            // set the parentDataBlock for this proces to the raw acoustic data
            setParentDataBlock(display.getSourceRawDataBlock());

            /* create a new FFTDataBlock with just the data units between
             * the start and end times.  Set the natural lifetime to
             * as large a number as possible so that the FFTDataUnits will
             * not be recycled until the user closes the RoccaSpecPopUp.
             * Note that the .setNaturalLifetime method assumes the passed
             * integer is in seconds, so multiplies by 1000 to convert to
             * milliseconds.  Thus, to keep the largest number possible, it
             * is first divided by 1000 and then passed.
             */
            FFTDataBlock selectedWhistle = getDataBlockSubset(display, channel);
            selectedWhistle.setNaturalLifetimeMillis(Integer.MAX_VALUE);

            /* create a new PamRawDataBlock containing only the data from
             * the selectedWhistle.  Set the natural lifetime to
             * as large a number as possible so that the FFTDataUnits will
             * not be recycled until the user closes the RoccaSpecPopUp.
             * Note that the .setNaturalLifetime method assumes the passed
             * integer is in seconds, so multiplies by 1000 to convert to
             * milliseconds.  Thus, to keep the largest number possible, it
             * is first divided by 1000 and then passed.
             */
            PamRawDataBlock selectedWhistleRaw = getRawData(selectedWhistle);
            selectedWhistleRaw.setNaturalLifetimeMillis(Integer.MAX_VALUE);

            /* if the user hasn't added a sighting yet, do that now */
            if (roccaControl.roccaSidePanel.getSightingNum().equals(
                    RoccaSightingDataUnit.NONE)) {
                String dummy =
                        roccaControl.roccaSidePanel.sidePanel.addASighting(false);
            }

            /* open the selected whistle in a new pop-up spectrogram */
            RoccaSpecPopUp roccaSpecPopUp = new RoccaSpecPopUp
                    (roccaControl.roccaProcess,
                    selectedWhistle,
                    startFreq,
                    endFreq,
                    selectedWhistleRaw,
                    display,
                    channel);
        }
	}

    /**
     * Create a subset of the FFTDataBlock being displayed, based on the
     * starting and ending times selected by the user
     *
     * @param display The spectrogram display object
     * @param channel The channel the whistle was selected from
     * @return a new FFTDataBlock containing only the FFTDataUnits between the
     * start and end times, for the selected channel
     */
    public FFTDataBlock getDataBlockSubset(SpectrogramDisplay display, int channel) {

        FFTDataBlock fullFFTDataBlock = display.getSourceFFTDataBlock();
        int[] channelList = new int[1];
        channelList[0] = channel;
        int channelBitmap = PamUtils.makeChannelMap(channelList);
        FFTDataBlock subFFTDataBlock = new FFTDataBlock(
                roccaControl.roccaSidePanel.getSightingNum(),
                this,
                channelBitmap,
                fullFFTDataBlock.getFftHop(),
                fullFFTDataBlock.getFftLength());

        /* find the index number of the FFTDataUnit closest to the start time
         * selected by the user.  If the closest FFTDataUnit is for the wrong
         * channel, step backwards through the list until we find one with the
         * right channel
         */
        int firstIndx = fullFFTDataBlock.getUnitIndex(
                fullFFTDataBlock.getPreceedingUnit(startTime, channelBitmap));
//        int firstIndx = fullFFTDataBlock.getUnitIndex(
//                fullFFTDataBlock.getClosestUnitMillis(startTime));
//        while (PamUtils.getSingleChannel(
//                fullFFTDataBlock.getDataUnit(firstIndx, FFTDataBlock.REFERENCE_CURRENT)
//                .getChannelBitmap()) != channel) {
//            firstIndx--;
//        }

        /* find the index number of the FFTDataUnit closest to the end time
         * selected by the user, in the channel desired.  Once we have the
         * preceeding unit, step forward one unit to make sure we have enough
         * data (we actually want the next unit, not the preceeding unit)
         */
        int lastIndx = fullFFTDataBlock.getUnitIndex(
                fullFFTDataBlock.getNextUnit(endTime, channelBitmap));
//        int lastIndx = fullFFTDataBlock.getUnitIndex(
//                fullFFTDataBlock.getClosestUnitMillis(endTime));
//        while (PamUtils.getSingleChannel(
//                fullFFTDataBlock.getDataUnit(lastIndx, FFTDataBlock.REFERENCE_CURRENT)
//                .getChannelBitmap()) != channel) {
//            lastIndx++;
//        }


        /* Now step through the FFTDataUnits, from firstIndx to lastIndx, adding
         * each data unit from the correct channel to the new FFTDataBlock
         */
        FFTDataUnit unit;
        for (int i = firstIndx; i <= lastIndx; i++) {
            unit = fullFFTDataBlock.getDataUnit(i, PamDataBlock.REFERENCE_CURRENT );
            if (unit.getChannelBitmap() == channelBitmap) {
                subFFTDataBlock.addPamData(unit);
            }
        }
        return subFFTDataBlock;
    }

    public PamRawDataBlock getRawData(FFTDataBlock fftDataBlock) {

        PamRawDataBlock prdb = fftDataBlock.getRawSourceDataBlock2();
        PamRawDataBlock newBlock = new PamRawDataBlock(
                prdb.getDataName(),
                this,
                roccaControl.roccaParameters.getChannelMap(),
                prdb.getSampleRate());

        /* find the index number of the PamRawDataUnit closest to the start time
         * of the first unit in the FFTDataBlock, and in the lowest channel
         * position to be saved.  Use the .getPreceedingUnit method to ensure
         * that the start time of the raw data is earlier than the start time
         * of the FFT data (otherwise we'll crash later in RoccaContour)
         */
        int[] lowestChanList = new int[1];
        lowestChanList[0] =
                PamUtils.getLowestChannel(roccaControl.roccaParameters.channelMap);
        int firstIndx = prdb.getUnitIndex(prdb.getPreceedingUnit(
                fftDataBlock.getFirstUnit().getTimeMilliseconds(),
                PamUtils.makeChannelMap(lowestChanList)));

        /* find the index number of the PamRawDataUnit closest to the start time
         * of the last unit in the FFTDataBlock, and in the highest channel
         * position to be saved.  Use the .getNextUnit method to ensure
         * that the start time of the raw data is later than the start time
         * of the FFT data (otherwise we'll crash later in RoccaContour)
         */
        int[] highestChanList = new int[1];
        highestChanList[0] =
                PamUtils.getHighestChannel(roccaControl.roccaParameters.channelMap);
        int lastIndx = prdb.getUnitIndex(prdb.getNextUnit(
                fftDataBlock.getLastUnit().getTimeMilliseconds(),
                PamUtils.makeChannelMap(highestChanList)));
        
        /* check to make sure lastIndx is a real number - sometimes the start time of the last FFTDataBlock is > the start
         * time of the last PamRawDataBlock, which causes lastIndx to be set to -1.  In such a case, set the lastIndx to
         * point to the last PamRawDataBlock
         */
        if (lastIndx == -1) {
        	lastIndx = prdb.getUnitsCount()-1;
        }

        /* add the units, from firstIndx to lastIndx, to the new PamRawDataBlock */
        for (int i = firstIndx; i <= lastIndx; i++) {
            newBlock.addPamData
                    (prdb.getDataUnit
                    (i, PamDataBlock.REFERENCE_CURRENT ));
        }
        return newBlock;
    }

    @Override
	public String getMarkObserverName() {
		return getProcessName();
	}

    @Override
	public void pamStart() {}

	@Override
	public void pamStop() {}

}
