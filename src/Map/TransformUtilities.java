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

import java.awt.Polygon;

import GPS.GpsData;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;

/**
 * @author David McLaren
 *         <p>
 *         Coordinate transform utilites
 * 
 */

public class TransformUtilities {

	private CoordsXY xy;

	private Coordinate3d xyz;

	private double degrees;

	private double radians;
	
	static final double EARTHRADIUS = 6378160.;

	public static void rotationXY(CoordsXY xyIn, double thetaRads) {
		CoordsXY xy = new CoordsXY();
		xy.x = xyIn.x * Math.cos(thetaRads) + xyIn.y * Math.sin(thetaRads);
		xy.y = xyIn.y * Math.cos(thetaRads) - xyIn.x * Math.sin(thetaRads);
		xyIn.x = xy.x;
		xyIn.y = xy.y;
	}

	/*
	 * public static void rotationXYZ(Coordinate3d c, double thetaRads){
	 * Coordinate3d xyz = new Coordinate3d(); xyz.x =
	 * c.x*Math.cos(thetaRads)+c.y*Math.sin(thetaRads); xyz.y =
	 * c.y*Math.cos(thetaRads)-c.x*Math.sin(thetaRads); c.x = xyz.x; c.y =
	 * xyz.y; c.z = 0.0; }
	 */

	public static Coordinate3d rotationDegreesXYZ(Coordinate3d c, double theta) {
		Coordinate3d xyz = new Coordinate3d();
		double thetaRads = theta * Math.PI / 180.0;
		xyz.x = c.x * Math.cos(thetaRads) + c.y * Math.sin(thetaRads);
		xyz.y = c.y * Math.cos(thetaRads) - c.x * Math.sin(thetaRads);
		c.x = xyz.x;
		c.y = xyz.y;
		c.z = c.z;
		return (c);
	}

	/*
	 * public static void rotationDegreesXYZ(double x, double y, double z,
	 * double theta){ Coordinate3d xyz = new Coordinate3d(); double thetaRads =
	 * theta*Math.PI/180.0; xyz.x = x*Math.cos(thetaRads)+y*Math.sin(thetaRads);
	 * xyz.y = y*Math.cos(thetaRads)-x*Math.sin(thetaRads); x = xyz.x; y =
	 * xyz.y; z = 0.0; }
	 */

	public static void Coordinate3d2XyArrays(int[] x, int[] y, Coordinate3d[] c) {

		for (int i = 0; i < c.length; i++) {
			x[i] = (int) c[i].x;
			y[i] = (int) c[i].y;
		}
	}

	public static CoordsXY rotateXY(CoordsXY xyIn, double thetaRads) {
		CoordsXY xy = new CoordsXY();
		xy.x = xyIn.x * Math.cos(thetaRads) + xyIn.y * Math.sin(thetaRads);
		xy.y = xyIn.y * Math.cos(thetaRads) - xyIn.x * Math.sin(thetaRads);
		return (xy);
	}

	public double[] rotateXY(double xIn, double yIn, double thetaRads) {
		double[] xy = new double[2];
		xy[0] = xIn * Math.cos(thetaRads) + yIn * Math.sin(thetaRads);
		xy[1] = yIn * Math.cos(thetaRads) - xIn * Math.sin(thetaRads);
		return (xy);
	}
	
	// rotate a polygon about 0,0
//	public static Polygon rotatePolygon(Polygon polygon, double angleRadians) {
//		double cosTheta = Math.cos(angleRadians);
//		double sinTheta = Math.sin(angleRadians);
//		int newX[] = new int[polygon.npoints];
//		int newY[] = new int[polygon.npoints];
//		for (int i = 0; i < polygon.npoints; i++) {
//			newX[i] = (int) (polygon.xpoints[i] * cosTheta + polygon.ypoints[i] * sinTheta);
//			newY[i] = (int) (polygon.ypoints[i] * cosTheta - polygon.xpoints[i] * sinTheta);
//		}
//		return new Polygon(newX, newY, polygon.npoints);
//	}
	// rotate a polygon about 0,0
	public static void rotatePolygon(Polygon polygon, double angleRadians) {
		double cosTheta = Math.cos(angleRadians);
		double sinTheta = Math.sin(angleRadians);
//		int newX[] = new int[polygon.npoints];
//		int newY[] = new int[polygon.npoints];
		int newX, newY;
		for (int i = 0; i < polygon.npoints; i++) {
			newX = (int) (polygon.xpoints[i] * cosTheta + polygon.ypoints[i] * sinTheta);
			newY = (int) (polygon.ypoints[i] * cosTheta - polygon.xpoints[i] * sinTheta);
			polygon.xpoints[i] = newX;
			polygon.ypoints[i] = newY;
		}
	}

	public static double degrees2radians(double degrees) {
		return (degrees * Math.PI / 180);
	}

	public void degrees2radians(double degrees, double radians) {
		radians = (degrees * Math.PI / 180);
	}

	public static double radians2degrees(double radians) {
		return (radians * 180 / Math.PI);
	}

	public static LatLongMinutes degrees2minutes(LatLong a) {
		return (new LatLongMinutes(a.getLatitude() * 60.0, a.getLongitude() * 60.0));
	}

	public static void degrees2minutes(LatLong degs, LatLongMinutes mins) {

		mins.latMins = degs.getLatitude() * 60.0;
		mins.longMins = degs.getLongitude() * 60.0;
		// return( new LatLongMinutes(a.latDegs * 60.0, a.longDegs * 60.0));
	}

	// public static void simpleLLD2PanelPoint(CoordsXY xyPanel, LatLongMinutes
	// latLongCentre, LatLongMinutes latLongIn, double dx, double dy , double
	// theta, double metre2pixel, int width, int height){
	public CoordsXY simpleLLD2PanelPoint(LatLongMinutes latLongCentre,
			LatLongMinutes latLongIn, double dx, double dy, double theta,
			double metre2pixel, int width, int height) {
		CoordsXY xyPanel = new CoordsXY();
		xyPanel.y = (latLongIn.latMins - latLongCentre.latMins) * dy;
		xyPanel.x = (latLongIn.longMins - latLongCentre.longMins) * dx;
		xyPanel = rotateXY(xyPanel, theta);
		xyPanel.x = xyPanel.x * metre2pixel + width / 2.0;
		xyPanel.y = height / 2.0 - xyPanel.y * metre2pixel;
		return (xyPanel);
	}

	public static void simpleMapGridResolution(double dy, double dx, double r,
			LatLong coordinates) {
		dx = 2.0 * Math.PI * r
				* Math.abs(Math.cos(coordinates.getLatitude() * Math.PI / 180.0))
				/ 360.0;
		dy = 2.0 * Math.PI * r / 360.0;
	}

	public static void simpleMapGridResolution(CoordsXY xy, double r,
			LatLong coordinates) {
		xy.x = 2.0 * Math.PI * r
				* Math.abs(Math.cos(coordinates.getLongitude() * Math.PI / 180.0))
				/ 360.0;
		xy.y = 2.0 * Math.PI * r / 360.0;
	}

	public static void simpleMapGridResolution(double dy, double dx, double r,
			LatLongMinutes coordinates) {
		dx = 2.0 * Math.PI * r
				* Math.abs(Math.cos(coordinates.latMins * Math.PI / 10800))
				/ 21600.0;
		dy = 2.0 * Math.PI * r / 21600.0;
	}

	public static void simpleMapGridResolution(CoordsXY xy, double r,
			LatLongMinutes coordinates) {
		xy.x = 2.0 * Math.PI * r
				* Math.abs(Math.cos(coordinates.latMins * Math.PI / 10800))
				/ 21600.0;
		xy.y = 2.0 * Math.PI * r / 21600.0;
	}
	
	public static Coordinate3d lld2Coord3dMeters(double latDegrees, double longDegrees,
			 double originLatDegrees, double originLonDegrees) {
		
		//EARTHRADIUS =  6378000.;
		
		Coordinate3d offsetPosition  = new Coordinate3d();
		offsetPosition.x = (longDegrees - originLonDegrees)
				* 2
				* Math.PI
				* EARTHRADIUS
				* Math
						.abs(Math.cos(originLatDegrees * Math.PI
								/ 180.0)) / 360.0;
		offsetPosition.y = (latDegrees - originLatDegrees) * 2 * Math.PI
				* EARTHRADIUS  / 360.0;
		return (offsetPosition);
	}
	
	
	public static GpsData calcMovingObjectPosition(GpsData oldGps, double dT){
		double distance = dT*oldGps.getSpeed();
		double theta = TransformUtilities.degrees2radians(oldGps.getCourseOverGround());
		double latitudeFactor = Math.abs(Math.cos(oldGps.getLatitude()*Math.PI/180.0));
		GpsData newGps = new GpsData();
		newGps.setSpeed(oldGps.getSpeed());
		newGps.setCourseOverGround(oldGps.getCourseOverGround());
		Coordinate3d offsetPosition  = new Coordinate3d();
		offsetPosition.x=distance*Math.sin(theta);
		offsetPosition.y=distance*Math.cos(theta);
		newGps.setLatitude(360.0*offsetPosition.y/2/Math.PI/EARTHRADIUS+oldGps.getLatitude());
		newGps.setLongitude(360.0*offsetPosition.x/2/Math.PI/EARTHRADIUS/latitudeFactor+oldGps.getLongitude());		
		return newGps;
	}
	
	
	

}
