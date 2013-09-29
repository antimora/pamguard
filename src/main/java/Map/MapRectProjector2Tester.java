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

import PamUtils.Coordinate3d;
import PamUtils.LatLong;

public class MapRectProjector2Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Coordinate3d pp = new Coordinate3d();
		LatLong LLD = new LatLong(1,1);

		MapRectProjector myMap = new MapRectProjector();

		myMap.setMapCentreDegrees(new LatLong(0.0, 0.0));

		myMap.mapRotationDegrees = 90.0;
		// myMap.mapRangeMetres=250000.0;
		myMap.panelWidth = 1000.0;
		myMap.panelHeight = 1000.0;
		myMap.pixelsPerMetre = .001;

		// pp=myMap.LL2panel(LLD);
		pp = myMap.getCoord3d(LLD.getLatitude(), LLD.getLongitude(), 0.0);

		//System.out.println("pp: " + pp.x + " " + pp.y);
		// System.exit(0);
		// LLD=myMap.panel2LL(pp);
		LLD = myMap.panel2LL(pp);
		//System.out.println("LLD: " + LLD.latDegs + " " + LLD.longDegs);

		/*
		 * Coordinate3d xy = new Coordinate3d(); xy.x=1.0; xy.y=1.0;
		 * 
		 * TransformUtilities.rotationDegreesXYZ(xy,360.0);
		 * System.out.println("xy: " + xy.x + " " + xy.y);
		 */

		// public static void rotationDegreesXYZ(Coordinate3d c, double theta){
	}

}
