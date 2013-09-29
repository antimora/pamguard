package clickDetector;

import java.io.Serializable;
import java.util.Arrays;

public class BTDisplayParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 2;
	
	static public final int DISPLAY_BEARING   = 0;
	static public final int DISPLAY_ICI       = 1;
	static public final int DISPLAY_AMPLITUDE = 2;
	static public final int DISPLAY_SLANT = 3;
	
	static public final int COLOUR_BY_TRAIN = 0;
	static public final int COLOUR_BY_SPECIES = 1;
	static public final int COLOUR_BY_TRAINANDSPECIES = 2;
	static public final int COLOUR_BY_HYDROPHONE = 3;

	public static final String[] colourNames = {"Train", "Species", "Train then Species"};
	
	
	// main BT display
	public int VScale = DISPLAY_BEARING;
	public double maxICI = 1;
	public double minICI = 0.001; // used on log scale only. 
	public double[] amplitudeRange = {60, 160};
	public int nBearingGridLines = 1;
	public int nAmplitudeGridLines = 0;
	public int nICIGridLines = 0;
	public boolean showEchoes = true;
	public int minClickLength = 2, maxClickLength = 12;
	public int minClickHeight = 2, maxClickHeight = 12;
	private double timeRange = 10;
	public int displayChannels = 0;
	public boolean view360;
	public boolean amplitudeSelect = false;
	public double minAmplitude = 0;
	public boolean showUnassignedICI = false;
	public boolean showEventsOnly = false;
	public boolean showANDEvents = true;
	public boolean logICIScale;
	
//	/*
//	 * Show all unidentified species.
//	 */
//	public boolean showNonSpecies = true;
	/*
	 * Show identified species
	 */
	private boolean[] showSpeciesList;
	
	public int colourScheme = COLOUR_BY_TRAIN;
	
	public boolean showKey;
	
	/**
	 * Show little markers for tracked click angles down the sides of the display
	 */
	public boolean trackedClickMarkers = true;

	@Override
	public BTDisplayParameters clone() {
		try {
			return (BTDisplayParameters) super.clone();
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		showSpeciesList = null;
		return null;
	}

	/**
	 * @return the timeRange
	 */
	public double getTimeRange() {
		if (timeRange <= 0) {
			timeRange = 20;
		}
		return timeRange;
	}

	/**
	 * @param timeRange the timeRange to set
	 */
	public void setTimeRange(double timeRange) {
		this.timeRange = timeRange;
	}

	/**
	 * @return the showSpeciesList
	 */
	public boolean getShowSpecies(int speciesIndex) {
		if (showSpeciesList != null && showSpeciesList.length > speciesIndex) {
			return showSpeciesList[speciesIndex];
		}
		makeShowSpeciesList(speciesIndex);
		return true;
	}
	private void makeShowSpeciesList(int maxIndex) {
		if (showSpeciesList == null) {
			showSpeciesList = new boolean[0];
		}
		else if (showSpeciesList.length > maxIndex) {
			return;
		}
		int oldLength = showSpeciesList.length;
		showSpeciesList = Arrays.copyOf(showSpeciesList, maxIndex + 1);
		for (int i = oldLength; i <= maxIndex; i++) {
			showSpeciesList[i] = true;
		}
	}

	/**
	 * @param showSpeciesList the showSpeciesList to set
	 */
	public void setShowSpecies(int speciesIndex, boolean showSpecies) {
		makeShowSpeciesList(speciesIndex);
		showSpeciesList[speciesIndex] = showSpecies;
	}
}
