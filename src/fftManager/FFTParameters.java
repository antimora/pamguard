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

import java.io.Serializable;

import spectrogramNoiseReduction.SpectrogramNoiseSettings;

import Spectrogram.WindowFunction;

public class FFTParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 2;

	String name = "";

	public int fftLength = 1024;

	public int fftHop = 512;

	public int channelMap = 3;
	
//	public String rawDataSource;
	public int dataSource = 0;
	
	public String dataSourceName;
	
	public int windowFunction = WindowFunction.HANNING;
	
	// parameters for click removal
	public boolean clickRemoval = false;
	
	public double clickThreshold = 5;
	
	public int clickPower = 6;
	
	public SpectrogramNoiseSettings spectrogramNoiseSettings = new SpectrogramNoiseSettings();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static boolean isValidLength(int len) {
		int nBits = 0;
		for (int i = 0; i < 32; i++) {
			if ((len & 1 << i) > 0)
				nBits++;
		}
		return (nBits == 1);
	}

	@Override
	protected FFTParameters clone() {
		FFTParameters newParams = null;
		try {
			newParams = (FFTParameters) super.clone();
			if (newParams.spectrogramNoiseSettings == null) {
				newParams.spectrogramNoiseSettings = new SpectrogramNoiseSettings();
			}
			else {
				newParams.spectrogramNoiseSettings = this.spectrogramNoiseSettings.clone();
			}
		}
		catch(CloneNotSupportedException Ex) {
			Ex.printStackTrace(); 
			return null;
		}
		return newParams;
	}
}
