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
package Map;

import java.io.File;
import java.io.Serializable;

public class MapParameters implements Serializable, Cloneable {

	static final long serialVersionUID = 1;

	/**
	 * How long to display the ships track for
	 */
	int trackShowTime = 3600; 

	/**
	 * How long to keep GPS and other data for, even if it's not being used.
	 */
	int dataKeepTime = 3600;  

	/**
	 * How long a period to show detector data for.
	 */
	int dataShowTime = 600;  
	
	/**
	 * A list of datablocks which are suitable for drawing on the map
	 */
//	ArrayList<String> plotableBlockIdList;
	/**
	 * A list of datablocks which are selected for drawing on the map
	 */
//	ArrayList<String> plottedBlockIds;
	/**
	 * A flag to enable/disable drawing of the hydrophone array on the map
	 */
	boolean showHydrophones = true;
	
	boolean keepShipOnMap = true;
	
	File mapFile;
	
	boolean[] mapContours;
	
	boolean showKey = true;
	
	boolean showPanZoom = true;
	
	boolean showGpsData = true;

	@Override
	protected MapParameters clone() {
		// TODO Auto-generated method stub
		try {
			return (MapParameters) super.clone();
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}
}
