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

import java.io.Serializable;

import clickDetector.ClickClassifiers.ClickClassifierManager;
import fftFilter.FFTFilterParams;

import Filters.FilterBand;
import Filters.FilterParams;
import Filters.FilterType;
import Localiser.DelayMeasurementParams;
import PamView.GroupedSourcePanel;
import PamguardMVC.PamConstants;
import java.util.ArrayList;

public class ClickParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 8;

	public String rawDataSource;

	public int channelBitmap = 3;

	public int triggerBitmap = 0xFFFFFFFF;

	public int[] channelGroups;

	public int groupingType = GroupedSourcePanel.GROUP_ALL;

	public double dbThreshold = 10.;

	public double longFilter = 0.00001;

	public double longFilter2 = longFilter / 10;

	public double shortFilter = 0.1;

	public int preSample = 40;

	public int postSample = 0;

	public int minSep = 100;

	public int maxLength = 1024;

	public FilterParams preFilter = new FilterParams();

	public FilterParams triggerFilter = new FilterParams();

	public boolean sampleNoise = true;

	public double noiseSampleInterval = 5;
	
	public int clickClassifierType = ClickClassifierManager.CLASSIFY_BASIC;
	
	public boolean runEchoOnline = false;
	
	public boolean discardEchoes = false;
	
	/**
	 * Run classification in real time / online ops. 
	 */
	public boolean classifyOnline;

	public boolean discardUnclassifiedClicks;
	/*
	 * Stuff to do with database storage
	 */
//	public boolean saveAllClicksToDB = false;

	/*
	 * Stuff to do with file storage
	 */
	// changed to false 17/8/08
	public boolean createRCFile = false;

	public boolean rcAutoNewFile = true;

	public float rcFileLength = 1;

	public String storageDirectory = new String();

	public String storageInitials = new String();

	/*
	 * Stuff to do with audible alarm
	 */
    public ArrayList<ClickAlarm> clickAlarmList = new ArrayList<ClickAlarm>();

	/**
	 * Make the trigger function output data available as raw data so it can be viewed. 
	 */
	public boolean publishTriggerFunction = false;

	/*
	 * Waveform display options 
	 */

	/**
	 * Show the envelope waveform
	 */
	public boolean waveShowEnvelope = false;

	/**
	 * Stop auto scaling the x axis - fix it at the max click length. 
	 */
	public boolean waveFixedXScale = false;
	
	/**
	 * view a filtered waveform in the display
	 */
	public boolean viewFilteredWaveform = false;
	
	/**
	 * Parameters for waveform filter. 
	 */
	public FFTFilterParams waveformFilterParams;
	
	public DelayMeasurementParams delayMeasurementParams = new DelayMeasurementParams();
	
	/**
	 * How to colour clicks on radar displays (this will apply to 
	 * all radars - not possible to do them individually at the moment). 
	 */
	public int radarColour = BTDisplayParameters.COLOUR_BY_TRAIN;

	/**
	 * How to colour clicks on spectrogram displays (this will apply to 
	 * all radars - not possible to do them individually at the moment). 
	 */
	public int spectrogramColour = BTDisplayParameters.COLOUR_BY_TRAIN;
	

	/*
	 * Parameters for map display.
	 */

	// stuff for map display
	public static final int LINES_SHOW_NONE = 0;
	public static final int LINES_SHOW_SOME = 1;
	public static final int LINES_SHOW_ALL = 2;
	public int showShortTrains = LINES_SHOW_SOME;
	public double minTimeSeparation = 60;
	public double minBearingSeparation = 5;
	public double defaultRange = 5000;

	/*
	 * parameters for click train identification ...
	 */
	public boolean runClickTrainId = false;
	public double[] iciRange = {0.1, 2.0};
	public double maxIciChange = 1.2;
	public double okAngleError = 2.0;
	public double initialPerpendicularDistance = 100;
	public int minTrainClicks = 6;
	public double iciUpdateRatio = 0.5; //1 == full update, 0 = no update

	public ClickParameters() {

		channelGroups = new int[PamConstants.MAX_CHANNELS];

		preFilter.filterBand = FilterBand.HIGHPASS;
		preFilter.filterOrder = 4;
		preFilter.filterType = FilterType.BUTTERWORTH;
		preFilter.highPassFreq = 500;

		triggerFilter.filterBand = FilterBand.HIGHPASS;
		triggerFilter.filterOrder = 2;
		triggerFilter.filterType = FilterType.BUTTERWORTH;
		triggerFilter.highPassFreq = 2000;

		storageDirectory = new String("C:\\clicks");
		storageInitials = new String("RBC");

        /* add a ClickAlarm to the ClickAlarmList.  Note that there must always
         * be at least 1 ClickAlarm in the list, and the first ClickAlarm is
         * the default alarm
         */
        clickAlarmList.add(new ClickAlarm());
	}

	@Override
	public ClickParameters clone() {
		try {
			ClickParameters n = (ClickParameters) super.clone();
			// set some defaults for new parameters (which will set to zero)
			if (n.maxIciChange == 0) {
				n.iciRange[0] = 0.1;
				n.iciRange[1] = 2.;
				n.maxIciChange = 1.2;
				n.okAngleError = 1;
			}
			if (n.noiseSampleInterval == 0) {
				n.noiseSampleInterval = 5;
				n.sampleNoise = true;
			}
			if (n.delayMeasurementParams == null) {
				n.delayMeasurementParams = new DelayMeasurementParams();
			}
			return n;
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

}
