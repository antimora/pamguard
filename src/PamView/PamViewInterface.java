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

/**
 * @author Doug Gillespie
 *         <p>
 *         Interface to the Pam viewer to receive commands and data from the Pam
 *         Model
 * 
 */
public interface PamViewInterface {
	
	// commands from the Controller
	public void pamStarted();

	public void pamEnded();
	
	public void modelChanged(int changeType);

	public void addControlledUnit(PamControlledUnit unit);
	
	public void removeControlledUnit(PamControlledUnit unit);
	
	public void showControlledUnit(PamControlledUnit unit);
	
	public void setTitle(String title);
	
	/**
	 * 
	 * @return Frame number used by the multiple GUI frames. All other objects, 
	 * such as the model view should return < 0. 
	 */
	public int getFrameNumber();
	
	public JFrame getGuiFrame();
	
	public void enableGUIControl(boolean enable);

}
