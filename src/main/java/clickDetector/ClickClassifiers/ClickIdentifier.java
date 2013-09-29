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

package clickDetector.ClickClassifiers;

import java.awt.Frame;

import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import clickDetector.ClickDetection;

import PamView.PamSymbol;

/**
 * 
 * @author Doug Gillespie
 *         <p>
 *         Interface for click identification from the click detector
 *         <p>
 */
public interface ClickIdentifier {

	public ClickIdInformation identify(ClickDetection click);

	public JMenuItem getMenuItem(Frame parentFrame);

	public PamSymbol getSymbol(ClickDetection click);
	
	public String[] getSpeciesList();
	
	public PamSymbol[] getSymbols();
	
	public int codeToListIndex(int code);
	
	public ClassifyDialogPanel getDialogPanel(Frame windowFrame); 
	
	public String getSpeciesName(int code);

	public boolean fillXMLParamaeters(Document doc, Element classEl);
	
	public String getParamsInfo(ClickDetection click);

    /**
     * Returns a list of the currently-defined click types / species codes
     * @return int array with the codes
     */
    public int[] getCodeList();

    /**
     * Return the superclass of the click type parameters class - currently used for
     * accessing the alarm functions.  Subclasses include ClickTypeParams and
     * SweepClassifierSet.
     *
     * @param code the click type to check
     * @return the ClickTypeCommonParams object related to the species code
     */
    public ClickTypeCommonParams getCommonParams(int code);
}
