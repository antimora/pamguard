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
package PamUtils;

import PamguardMVC.PamConstants;

/**
 * @author Doug Gillespie
 * 
 */
public class PamUtils {

	/**
	 * 
	 * check that the bits represent a single channel and return the number of
	 * that channel
	 * 
	 * @param channelMap
	 *            bitmap for multiple channels
	 * @return singel channel (or -1 if >1 or zero channels)
	 */
	public static int getSingleChannel(int channelMap) {
		int channels = 0;
		int singleChan = -1;
		for (int i = 0; i < 32; i++) {
			if ((1 << i & channelMap) != 0) {
				singleChan = i;
				channels++;
			}
		}
		if (channels > 1)
			return 1;
		return singleChan;
	}

	public static int getNumChannels(int channelBitmap) {
		int channels = 0;
		for (int i = 0; i < 32; i++) {
			if ((1 << i & channelBitmap) != 0) {
				channels++;
			}
		}
		return channels;
	}
	
	/**
	 * Works out the index of a particular channel in a channel list - often,
	 * if channelBitmap is a set of continuous channels starting with 0, then
	 * the channel pos is the same as the single channel number. However, if there
	 * are gaps in the channelBitmap, then the channel pos will be < than the 
	 * channel Number.
	 * @param singleChannel
	 * @param channelBitmap
	 * @return the channel position in the channel list
	 */
	public static int getChannelPos(int singleChannel, int channelBitmap) {
		
		int channelPos = 0;
		
		if ((1<<singleChannel & channelBitmap) == 0) return -1;
		
		for (int i = 0; i < singleChannel; i++) {
			if ((1<<i & channelBitmap) != 0) {
				channelPos++;
			}
		}
		
		return channelPos;
	}
	
	/**
	 * 		
	 * get's the number of the nth used channel in a bitmap. e.g. if the 
	 * channelBitmap were 0xc (1100), then the 0th channel would be 2 and the 
	 * 1st channel would be 3.
	 * @param singleChannel nth channel in a list 
	 * @param channelBitmap bitmap of all used channels. 
	 * @return true channel number
	 */
	public static int getNthChannel(int singleChannel, int channelBitmap) {
		/*
		 * get's the number of the nth used channel in a bitmap. e.g. if the 
		 * channelBitmap were 0xc (1100), then the 0th channel would be 2 and the 
		 * 1st channel would be 3.
		 */
		int countedChannels = 0;
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			if ((channelBitmap & (1<<i)) != 0) {
				if (++countedChannels > singleChannel) return i;
			}
		}
		return -1;
	}
	
	public static int[] getChannelPositionLUT(int channelBitmap) {
//		int[] lut = new int[getHighestChannel(channelBitmap) + 1];
//		int pos = -1;
//		for (int i = 0; i <= getHighestChannel(channelBitmap); i++) {
//			pos = getChannelPos(i, channelBitmap);
//			lut[i] = pos;
//		}
		int[] lut = new int[PamConstants.MAX_CHANNELS];
		int pos = -1;
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			pos = getChannelPos(i, channelBitmap);
			lut[i] = pos;
		}
		return lut;
	}
	/**
	 * Get the highest channel number in a channel map. 
	 * @param channelBitmap
	 * @return the last channel in the channel bitmap
	 */
	public static int getHighestChannel(int channelBitmap) {
		int highestChan = -1;
		for (int i = 0; i < 32; i++) {
			if ((1 << i & channelBitmap) != 0) {
				highestChan = i;
			}
		}
		return highestChan;
	}
	
	/**
	 * Get the lowest channel number in a channel map. 
	 * @param channelBitmap
	 * @return the first channel in the channel bitmap
	 */
	public static int getLowestChannel(int channelBitmap) {
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			if ((1<<i & channelBitmap) != 0) return i;
		}
		return -1;
	}

	/**
	 * Make a simple bitmap for nChannels of data, 
	 * i.e. <p>
	 * if nChannels = 1, map = 1 <p>
	 * if nChannels = 2, map = 3 <p>
	 * if nChannels = 3, map = 7 <p>
	 * if nChannels = 4, map = 15 <p>
	 * etc.
	 * @param nChannels
	 * @return a bitmap representing the set channels 
	 */
	public static int makeChannelMap(int nChannels) {
		int map = 0;
		for (int i = 0; i < nChannels; i++) {
			map += 1 << i;
		}
		return map;
	}
	
	/**
	 * Make a channel map from a list
	 * @param nChannels number of elements in the list
	 * @param channelList list 
	 * @return channel bitmap
	 * @deprecated
	 */
	@Deprecated
	public static int makeChannelMap(int nChannels, int[] channelList) { //Xiao Yan Deng
		/*
		 * Is getting called with channelList = null in which case
		 * just do the old way D.G.
		 */
		if (channelList == null) {
			return makeChannelMap(nChannels);
		}
		int map = 0;
		for (int i = 0; i < nChannels; i++) {
			map += 1 << channelList[i];
		}
//		System.out.println("PamUtils.java->makeChannelMap:: map" + map);
		return map;
	}
	
	/**
	 * Make a channel bitmap from a list. 
	 * @param channelList channel list
	 * @return bitmap of channels. 
	 */
	public static int makeChannelMap(int[] channelList) {
		int map = 0;
		for (int i = 0; i < channelList.length; i++) {
			map += 1 << channelList[i];
		}
		return map;
	}

	public static int SetBit(int bitMap, int bitNumber, int bitValue) {
		return SetBit(bitMap, bitNumber, bitValue == 1);
	}

	public static int SetBit(int bitMap, int bitNumber, boolean bitSet) {
		if (bitSet) {
			return (bitMap |= 1 << bitNumber);
		}
		return (bitMap &= ~(1 << bitNumber));
	}

	public static int getMinFftLength(long len) {
		int fftLength = 4;
		while (fftLength < len) {
			fftLength *= 2;
		}
		return fftLength;
	}

	/**
	 * Gets the next integer log of 2 which will
	 * give 2^ans >= n, i.e. the minimum length
	 * required to FFT these data.
	 * @param n length
	 * @return log2 of mnimum data required to fit n. 
	 */
	public static int log2(int n) {

		int log2FFTLen = 2;
		int dum = 4;
		while (dum < n) {
			dum *= 2;
			log2FFTLen++;
		}
		return log2FFTLen;
	}

	
	/** Linear interpolation. Define f() as the linear mapping from the
	 * domain [x0,x1] to the range [y0,y1]. Return y=f(x) according to
	 * this mapping.
	 * <p>author Dave Mellinger
	 */
	public static double linterp(double x0,double x1,double y0,double y1,double x) {
		return (x - x0) / (x1 - x0) * (y1 - y0) + y0;
	}
	

	/**
	 * Create a string of channel numbers in the channel 
	 * map separated by commas. 
	 * @param channelMap channel map
	 * @return string of channel numbers. 
	 */
	static public String getChannelList(int channelMap) {
		String str = null;
		if (channelMap == 0) return "";
		for (int iBit = 0; iBit < PamConstants.MAX_CHANNELS; iBit++) {
			if ((1<<iBit & channelMap) != 0) {
				if (str == null) {
					str = String.format("%d", iBit);
				}
				else {
					str += String.format(", %d", iBit);
				}
			}
		}
		return str;
	}
	
	/**
	 * Turn a bitmap into an array of channel numbers. 
	 * @param channelMap channel map
	 * @return channel list array
	 */
	public static int[] getChannelArray(int channelMap) {
		int nChan = getNumChannels(channelMap);
		if (nChan <= 0) {
			return null;
		}
		int[] channels = new int[nChan];
		for (int i = 0; i < nChan; i++) {
			channels[i] = getNthChannel(i, channelMap);
		}
		return channels;
	}
	

	 /**
	  * Force an angle to sit 0<= angle < 360.
	  * @param angle input angle (degrees) 
	  * @return output angle (degrees)
	  */
	static public double constrainedAngle(double angle) {
		while (angle >= 360) {
			angle -= 360;
		}
		while (angle < 0) {
			angle += 360;
		}
		return angle;
	}
	 /**
	  * Force an angle to sit within some range. 
	  * @param angle input angle (degrees) 
	  * @param maxAngle maximum angle in degrees
	  * @return output angle (degrees)
	  */
	static public double constrainedAngle(double angle, double maxAngle) {
		while (angle > maxAngle) {
			angle -= 360;
		}
		while (angle <= (maxAngle - 360)) {
			angle += 360;
		}
		return angle;
	}
	
	public static double roundNumber(double number, double step) {
		return Math.round(number / step) * step;
	}
	
	/**
	 * Format a frequency in Hz as Hz, kHz, MHz, etc.
	 * @param f frequency value in Hz
	 * @return Formatted string
	 */
	public static String formatFrequency(double f) {
		double absF = Math.abs(f);
		if (absF < 1) {
			return String.format("%f Hz", f);
		}
		else if (absF < 10) {
			return String.format("%4.2f Hz", f);
		}
		else if (absF < 100) {
			return String.format("%4.1f Hz", f);
		}
		else if (absF < 1000) {
			return String.format("%4.0f Hz", f);
		}
		else if (absF < 10000) {
			return String.format("%4.2f kHz", f/1000.);
		}
		else if (absF < 100000) {
			return String.format("%4.1f kHz", f/1000.);
		}
		else if (absF < 1000000) {
			return String.format("%4.0f kHz", f/1000.);
		}
		else if (absF < 1e7) {
			return String.format("%4.3f MHz", f/1.e6);
		}
		else {
			return String.format("%4.3f MHz", f/1.e6);
		}
			
	}
	/**
	 * Leave data alone, but create a list of indexes which will
	 * give the ascending order of data.
	 * <br>
	 * Uses a simple bubble sort, so only suitable for short arrays. 
	 * @param data array to sort
	 * @return sort indexes
	 */
	static public int[] getSortedInds(int[] data) {
		int l = data.length;
		int[] inds = new int[l];
		for (int i = 0; i < l; i++) {
			inds[i] = i;
		}
		int dum;
		for (int i = 0; i < l-1; i++) {
			for (int j = 0; j < l-1-i; j++) {
				if (data[inds[j]] > data[inds[j+1]]) {
					dum = inds[j];
					inds[j] = inds[j+1];
					inds[j+1] = dum;
				}
			}
		}
		return inds;
	}

	

}