package clipgenerator.clipDisplay;

import java.io.Serializable;

import PamView.ColourArray.ColourArrayType;

public class ClipDisplayParameters implements Cloneable, Serializable {

	public static final long serialVersionUID = 1L;

	/*
	 * Values that get used for controlling the display
	 */
	double amlitudeMinVal = 50;
	double amplitudeRangeVal = 70;
	double imageVScale = 1, imageHScale = 1;
	int logFFTLength = 9;
	private ColourArrayType colourMap = ColourArrayType.GREY;
	public boolean showTriggerOverlay = true;
	public double frequencyScale = 1.0; // value <= 1. 1 is all data up to niquist. 
	int maxClips = 100;
	int maxMinutes = 10;

	
	/**
	 * Values used to set max and min values for the controls. 
	 */
	double amplitudeMinMin = 0;
	double amplitudeMinMax = 200;
	double amplitudeMinStep = 10;
	double amplitudeRangeMin = 40;
	double amplitudeRangeMax = 200;
	double amplitudeRangeStep = 10;

	@Override
	protected ClipDisplayParameters clone() {
		try {
			return (ClipDisplayParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public ColourArrayType getColourMap() {
		if (colourMap == null) {
			colourMap = ColourArrayType.GREY;
		}
		return colourMap;
	}

	public void setColourMap(ColourArrayType colourMap) {
		this.colourMap = colourMap;
	}
}
