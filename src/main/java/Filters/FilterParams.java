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
package Filters;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import PamController.PamControlledUnit;

/**
 * @author Doug Gillespie
 *         <p>
 *         Parameters for digital filter design - just the filter on it's own,
 *         not the complete set with data sources and everything eles.
 */
public class FilterParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 1;

	public FilterType filterType = FilterType.CHEBYCHEV;

	public FilterBand filterBand = FilterBand.BANDPASS;

	public int filterOrder;

	public float lowPassFreq, highPassFreq;
	
	/**
	 * Centre frequency for band pass filters. This is really only
	 * used with the filter designs forANSI standard filters used in the noise module. 
	 */
	private double centreFreq; 

	public double passBandRipple;

	public double stopBandRipple;
	
	public double chebyGamma = 3;

	/**
	 * Scale type just used for drawing dialog
	 */
	int scaleType = SCALE_LOG;

	public static final int SCALE_LOG = 0;
	public static final int SCALE_LIN = 1;

	public FilterParams() {
		filterOrder = 4;
		//		lowPassFreq = 0.15f;
		//		highPassFreq = 0.05f;
		lowPassFreq = 20000;
		highPassFreq = 2000;
		passBandRipple = 2;
		stopBandRipple = 2;
	}

	//	public FilterParams(FilterParams p) {
	//
	//		this.filterType = p.filterType;
	//		this.filterBand = p.filterBand;
	//		this.filterOrder = p.filterOrder;
	//		this.lowPassFreq = p.lowPassFreq;
	//		this.highPassFreq = p.highPassFreq;
	//		this.passBandRipple = p.passBandRipple;
	//		this.stopBandRipple = p.stopBandRipple;
	//
	//	}

	public boolean equals(FilterParams p) {

		return (this.filterType == p.filterType
				&& this.filterBand == p.filterBand
				&& this.filterOrder == p.filterOrder
				&& this.lowPassFreq == p.lowPassFreq
				&& this.highPassFreq == p.highPassFreq
				&& this.passBandRipple == p.passBandRipple && this.stopBandRipple == p.stopBandRipple);

	}

	public void assign(FilterParams p) {

		this.filterType = p.filterType;
		this.filterBand = p.filterBand;
		this.filterOrder = p.filterOrder;
		this.lowPassFreq = p.lowPassFreq;
		this.highPassFreq = p.highPassFreq;
		this.passBandRipple = p.passBandRipple;
		this.stopBandRipple = p.stopBandRipple;
	}

	@Override
	public FilterParams clone() {
		try {
			FilterParams newP = (FilterParams) super.clone();
			if (newP.chebyGamma <= 0) {
				newP.chebyGamma = 3;
			}
			return newP;
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the filter type as a string
	 */
	public String sayType() {
		switch (filterType) {
		case NONE:
			return "None";
		case CHEBYCHEV:
			return "Chebychev";
		case BUTTERWORTH:
			return "Butterworth";
		case FIRWINDOW:
			return "FIR Filter";
		default:
			return "Unknown";
		}
	}

	/**
	 * 
	 * @return the filter band as a string
	 */
	public String sayBand() {
		switch (filterBand) {
		case BANDPASS:
			return "Bandpass";
		case BANDSTOP:
			return "Bandstop";
		case HIGHPASS:
			return "Highpass";
		case LOWPASS:
			return "Lowpass";
		default:
			return "Unknown";
		}
	}
	@Override
	public String toString() {
		String str;
		if (filterType == Filters.FilterType.NONE){
			return "No Filter";
		}
		else if (filterType == Filters.FilterType.CHEBYCHEV){
			str = "Chebychev";
		}
		else if (filterType == Filters.FilterType.BUTTERWORTH){
			str = "Butterworth";
		}
		else if (filterType == Filters.FilterType.FIRWINDOW){
			str = "FIR filter";
		}
		else {
			return "Unknown Filter parameters";
		}
		if (filterBand == FilterBand.LOWPASS) {
			str += String.format(" Low pass %3.1fHz", lowPassFreq);
		}
		else if (filterBand == FilterBand.HIGHPASS) {
			str += String.format(" High pass %3.1fHz", highPassFreq);
		}
		else if (filterBand == FilterBand.BANDPASS) {
			str += String.format(" Band pass %3.1fHz to %3.1fHz", highPassFreq, lowPassFreq);
		}
		else if (filterBand == FilterBand.BANDSTOP) {
			str += String.format(" Band stop %3.1fHz to %3.1fHz", highPassFreq, lowPassFreq);
		}
		else {
			str += " Unknown band type";
		}
		return str;
	}

	/**
	 * Fill in filter parameters attributes to XML element - for use in filters stand alone
	 * and also in click detector, etc. 
	 * @param doc XML document
	 * @param paramsEl XML element
	 * @return true
	 */
	public boolean fillXMLParameters(Document doc, Element paramsEl) {
//		paramsEl.setAttribute("filterType", sayType());
//		paramsEl.setAttribute("filterBand", sayBand());
//		paramsEl.setAttribute("filterOrder", (new Integer(filterOrder)).toString());
//		paramsEl.setAttribute("lowPassFreq", (new Double(lowPassFreq)).toString());
//		paramsEl.setAttribute("highPassFreq", (new Double(highPassFreq)).toString());
//		paramsEl.setAttribute("passBandRipple", (new Double(passBandRipple)).toString());
//		paramsEl.setAttribute("stopBandRipple", (new Double(stopBandRipple)).toString());
//		paramsEl.setAttribute("chebyGamma", (new Double(chebyGamma)).toString());
		
		PamControlledUnit.addXMLParameter(doc, paramsEl, sayType(), "filterType", "Filter Type", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, sayBand(), "filterBand", "Filter Band", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, filterOrder, "filterOrder", "Filter Order", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, lowPassFreq, "lowPassFreq", "Low Pass Frequency", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, highPassFreq, "highPassFreq", "High Pass Freqeuncy", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, passBandRipple, "passBandRipple", "Pass Band ripple", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, stopBandRipple, "stopBandRipple", "Stop Band ripple", 0, 0);
		PamControlledUnit.addXMLParameter(doc, paramsEl, chebyGamma, "chebyGamma", "Gamma for FIR Filters", 0, 0);
		
		return true;
	}

	public double getCenterFreq() {
		if (centreFreq != 0) {
			return centreFreq;
		}
		return Math.sqrt(lowPassFreq * highPassFreq);
	}

	public void setCentreFreq(double d) {
		centreFreq = d;
	}



}
