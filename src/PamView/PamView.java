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
package PamView;

import javax.swing.JFrame;

import PamController.PamControlledUnit;
import PamController.PamControllerInterface;
import PamModel.PamModelInterface;

/**
 * @author Doug Gillespie
 *         <p>
 *         Makes a simple display with a main GUI and a list of data objects.
 */
abstract public class PamView implements PamViewInterface {

	protected PamControllerInterface pamControllerInterface;

	protected PamModelInterface pamModelInterface;

	/** 
	 * Frame for main window associated with this view (i.e a PamGUI).
	 */
	protected JFrame frame;
	
	private int frameNumber;
		

	public PamView(PamControllerInterface pamControllerInterface,
			PamModelInterface pamModelInterface, int frameNumber) {
		this.pamControllerInterface = pamControllerInterface;
		this.pamModelInterface = pamModelInterface;
		this.frameNumber = frameNumber;
	}

	/**
	 * tells the view to show the main display panel of a pamControlledUnit
	 * @param pamControlledUnit
	 */
	abstract public void showControlledUnit(PamControlledUnit pamControlledUnit);
	
	abstract public void renameControlledUnit(PamControlledUnit pamControlledUnit);
	
	abstract public String getViewName();


	public int getFrameNumber() {
		return frameNumber;
	}

	@Override
	public JFrame getGuiFrame() {
		return frame;
	}

	public void setFrameNumber(int frameNumber) {
		this.frameNumber = frameNumber;
	}

}
