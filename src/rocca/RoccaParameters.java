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

import java.io.File;
import java.io.Serializable;

/**
 * Parameters for Rocca
 * 
 * serialVersionUID = 5
 * 		modified contour calculations to better match original Matlab routine
 * 
 * serialVersionUID = 6
 * 		added whistle & moan as a potential data source for classifier
 * 
 * 
 * @author Michael Oswald
 */
public class RoccaParameters implements Serializable, Cloneable {

    /** for serialization */
    public static final long serialVersionUID = 6;

    /** constant used to define areas of spectrogram that do not contain whistle */
    public static final int NO_CONTOUR_HERE = -1;

    /** use the first (0th) fft datablock in the model */
    int fftDataBlock = 0;

    /** Bitmap of channels to be used - use all available */
	int channelMap = 0xFFFF;

    /** noise sensitivity parameter (in percent) */
    double noiseSensitivity = 11.0;

    /** size of freq bin (around peak freq) to calc energy spectrum (Hz) */
    int energyBinSize = 500;

    /** directory to store contours and statistics */
    File roccaOutputDirectory = new File("C:\\");

    /** filename for contour statistics storage */
    File roccaContourStatsOutputFilename = new File("C:\\RoccaContourStats.csv");

    /** filename for contour statistics storage */
    File roccaSightingStatsOutputFilename = new File("C:\\SchoolStats.csv");

    /** filename for classification model */
    File roccaClassifierModelFilename = new File("C:\\RF8sp12att.model");

    /** the percent of classification tree votes required to explicitly classify
     * a contour.  Contours with less than the required votes are classified as
     * 'Ambig'
     */
    int classificationThreshold = 40;

    /** the percent of classification tree votes required for a species to
     * explicitly classify a sighting.  Sightings with less than the required
     * votes are classified as 'Ambig'
     */
    int sightingThreshold = 40;

    /** the filename template, modelled after the Ishmael template */
    String filenameTemplate = "Detection%X-%f-Channel%t-%Y%M%D_%H%m%s";

    /**
     * the name of the whistle & moan data source
     */
    String wmDataSource;
    
    /**
     * boolean indicating whether to use the FFT as the source (true) or a whistle & moan detector (false)
     */
    boolean useFFT = true;
    

	public RoccaParameters() {
    }


	@Override
    public RoccaParameters clone() {
		try {
			RoccaParameters n = (RoccaParameters) super.clone();
			return n;
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public int getChannelMap() {
        return channelMap;
    }

    public void setChannelMap(int channelMap) {
        this.channelMap = channelMap;
    }

    public int getEnergyBinSize() {
        return energyBinSize;
    }

    public int getFftDataBlock() {
        return fftDataBlock;
    }

    public double getNoiseSensitivity() {
        return noiseSensitivity;
    }

    public void setNoiseSensitivity(double noiseSensitivity) {
        this.noiseSensitivity = noiseSensitivity;
    }

    public File getRoccaClassifierModelFilename() {
        return roccaClassifierModelFilename;
    }

    public File getRoccaContourStatsOutputFilename() {
        return roccaContourStatsOutputFilename;
    }

    public File getRoccaOutputDirectory() {
        return roccaOutputDirectory;
    }

    public int getClassificationThreshold() {
        return classificationThreshold;
    }

    public void setClassificationThreshold(int classificationThreshold) {
        this.classificationThreshold = classificationThreshold;
    }

    public int getSightingThreshold() {
        return sightingThreshold;
    }

    public void setSightingThreshold(int sightingThreshold) {
        this.sightingThreshold = sightingThreshold;
    }

    public File getRoccaSightingStatsOutputFilename() {
        return roccaSightingStatsOutputFilename;
    }

    public String getFilenameTemplate() {
        return filenameTemplate;
    }

    public void setFilenameTemplate(String filenameTemplate) {
        this.filenameTemplate = filenameTemplate;
    }

    public String getWmDataSource() {
		return wmDataSource;
	}

	public void setWmDataSource(String wmDataSource) {
		this.wmDataSource = wmDataSource;
	}

	public boolean weAreUsingFFT() {
		return useFFT;
	}

	public void setUseFFT(boolean useFFT) {
		this.useFFT = useFFT;
	}
}
