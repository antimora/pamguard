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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.text.DecimalFormat;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;

/**
 * @author David McLaren \n Generates straight Lat/Long lines and Strings for
 *         e.g. MapRectProjector
 */

public class StraightLineGrid {

	double pixelsPerMetre;

	int panelWidth;

	int panelHeight;

	double mapRotation;

	double mapRangeMetres;

	LatLong panelTopLeftLL;

	LatLong panelTopRightLL;

	LatLong panelLowerLeftLL;

	LatLong panelLowerRightLL;

	double latLinesPerDegree;

	double longLinesPerDegree;

	int numLatLines;

	int numLongLines;

	private double lineGradient;

	private double b;

	private Coordinate3d intersectPoint;

	private Coordinate3d intersectPoint2;

	public StraightLineGrid() {
		super();

		panelTopLeftLL = new LatLong();
		panelTopRightLL = new LatLong();
		panelLowerLeftLL = new LatLong();
		panelLowerRightLL = new LatLong();
	}

	public void drawGrid(Graphics2D g2d, MapRectProjector rectProj) {

		intersectPoint = new Coordinate3d();
		intersectPoint2 = new Coordinate3d();

		double minLat, maxLat, minLong, maxLong;
		double longDegs;
		double longMins;
		double latDegs;
		double latMins;
		double latitudeRangeFactor;
		double longitudeRangeFactor;

		String latString, longString;
		float stringWidth, stringHeight;
		Font latLongFont = new Font("Arial", Font.PLAIN, 12);
		FontRenderContext frc = (g2d).getFontRenderContext();

		panelTopLeftLL = rectProj.panel2LL(new Coordinate3d(0.0, 0.0, 0.0));
		panelTopRightLL = rectProj.panel2LL(new Coordinate3d(
				panelWidth, 0.0, 0.0));
		panelLowerLeftLL = rectProj.panel2LL(new Coordinate3d(0.0,
				panelHeight, 0.0));
		panelLowerRightLL = rectProj.panel2LL(new Coordinate3d(
				panelWidth, panelHeight, 0.0));
		
		minLat = panelTopLeftLL.getLatitude();
		if (panelTopRightLL.getLatitude() < minLat)
			minLat = panelTopRightLL.getLatitude();
		if (panelLowerLeftLL.getLatitude() < minLat)
			minLat = panelLowerLeftLL.getLatitude();
		if (panelLowerRightLL.getLatitude() < minLat)
			minLat = panelLowerRightLL.getLatitude();
		minLong = panelTopLeftLL.getLongitude();
		if (panelTopRightLL.getLongitude() < minLong)
			minLong = panelTopRightLL.getLongitude();
		if (panelLowerLeftLL.getLongitude() < minLong)
			minLong = panelLowerLeftLL.getLongitude();
		if (panelLowerRightLL.getLongitude() < minLong)
			minLong = panelLowerRightLL.getLongitude();
		maxLat = panelTopLeftLL.getLatitude();
		maxLong = panelTopLeftLL.getLongitude();
		// longDegreesDisplayed = maxLong-minLong;
		if (panelTopRightLL.getLatitude() > maxLat)
			maxLat = panelTopRightLL.getLatitude();
		if (panelLowerLeftLL.getLatitude() > maxLat)
			maxLat = panelLowerLeftLL.getLatitude();
		if (panelLowerRightLL.getLatitude() > maxLat)
			maxLat = panelLowerRightLL.getLatitude();

		if (panelTopRightLL.getLongitude() > maxLong)
			maxLong = panelTopRightLL.getLongitude();
		if (panelLowerLeftLL.getLongitude() > maxLong)
			maxLong = panelLowerLeftLL.getLongitude();
		if (panelLowerRightLL.getLongitude() > maxLong)
			maxLong = panelLowerRightLL.getLongitude();

		// latDegreesDisplayed = maxLat-minLat;

		mapRotation = Math.abs(Math.IEEEremainder(Math.abs(mapRotation
				* Math.PI / 180.0), Math.PI / 2.0));

		// latDegreesDisplayed = (maxLat-minLat)*Math.cos(mapRotation);

		latitudeRangeFactor = mapRangeMetres / 60000.0;
		longitudeRangeFactor = mapRangeMetres
				/ 60000.0
				/ Math.cos(rectProj.getMapCentreDegrees().getLatitude() * Math.PI
						/ 180.0); // (maxLong-minLong)*(2.0-Math.tan(mapRotation));

		if (longitudeRangeFactor <= 0.25) {
			longLinesPerDegree = 30.0;
		} else if (longitudeRangeFactor > 0.25 && longitudeRangeFactor <= 0.5) {
			longLinesPerDegree = 15.0;
		} else if (longitudeRangeFactor > 0.5 && longitudeRangeFactor <= 1.0) {
			longLinesPerDegree = 7.5;
		} else if (longitudeRangeFactor > 1.0 && longitudeRangeFactor <= 2.0) {
			longLinesPerDegree = 4;
		} else if (longitudeRangeFactor > 2.0 && longitudeRangeFactor <= 5.0) {
			longLinesPerDegree = 2;
		} else if (longitudeRangeFactor > 5.0 && longitudeRangeFactor <= 10.0) {
			longLinesPerDegree = 1;
		} else if (longitudeRangeFactor > 10.0 && longitudeRangeFactor <= 20.0) {
			longLinesPerDegree = .5;
		} else if (longitudeRangeFactor > 20.0 && longitudeRangeFactor <= 40.0) {
			longLinesPerDegree = .25;
		} else if (longitudeRangeFactor > 40.0 && longitudeRangeFactor <= 80.0) {
			longLinesPerDegree = .125;
		} else {
			longLinesPerDegree = 0.0625;
		}

		if (latitudeRangeFactor <= 0.25) {
			latLinesPerDegree = 30.0;
		} else if (latitudeRangeFactor > 0.25 && latitudeRangeFactor <= 0.5) {
			latLinesPerDegree = 15.0;
		} else if (latitudeRangeFactor > 0.5 && latitudeRangeFactor <= 1.0) {
			latLinesPerDegree = 7.5;
		} else if (latitudeRangeFactor > 1.0 && latitudeRangeFactor <= 2.0) {
			latLinesPerDegree = 4.0;
		} else if (latitudeRangeFactor > 2.0 && latitudeRangeFactor <= 5.0) {
			latLinesPerDegree = 2.0;
		} else if (latitudeRangeFactor > 5.0 && latitudeRangeFactor <= 10.0) {
			latLinesPerDegree = 1.0;
		} else if (latitudeRangeFactor > 10.0 && latitudeRangeFactor <= 20.0) {
			latLinesPerDegree = 0.5;
		} else if (latitudeRangeFactor > 20.0 && latitudeRangeFactor <= 40.0) {
			latLinesPerDegree = 0.25;
		} else if (latitudeRangeFactor > 40.0 && latitudeRangeFactor <= 80.0) {
			latLinesPerDegree = 0.125;
		} else {
			latLinesPerDegree = 0.0625;
		}

		numLatLines = (int) (Math.ceil(maxLat * latLinesPerDegree) - Math
				.floor(minLat * latLinesPerDegree));

		numLongLines = (int) (Math.ceil(maxLong * longLinesPerDegree) - Math
				.floor(minLong * longLinesPerDegree));

		double minPlottedLat = Math.floor(minLat * latLinesPerDegree)
				/ latLinesPerDegree;
		double minPlottedLong = Math.floor(minLong * longLinesPerDegree)
				/ longLinesPerDegree;

		Coordinate3d coord1 = new Coordinate3d();
		Coordinate3d coord2 = new Coordinate3d();
		LatLong LL1 = new LatLong();
		LatLong LL2 = new LatLong();

		Color currentColor, latLineColor, longLineColor;
		latLineColor = new Color(218, 142, 180);
		longLineColor = new Color(150, 150, 200);
		currentColor = g2d.getColor();
		(g2d).setColor(latLineColor);

		DecimalFormat f = new DecimalFormat();
		f.setMaximumFractionDigits(3);
		(g2d).setFont(latLongFont);

		for (int i = 0; i < numLatLines; i++) {
			LL1.setLatitude(minPlottedLat + i / latLinesPerDegree);
			LL1.setLongitude(maxLong);
			LL2.setLatitude(LL1.getLatitude());
			LL2.setLongitude(minLong);

			coord1 = rectProj.LL2panel(LL1);
			coord2 = rectProj.LL2panel(LL2);

			g2d.drawLine((int) coord1.x, (int) coord1.y, (int) coord2.x,
					(int) coord2.y);

			lineGradient = (coord2.y - coord1.y) / (coord2.x - coord1.x);
			b = coord1.y - lineGradient * coord1.x;

			intersectPoint.x = panelWidth - 50.0;
			intersectPoint.y = lineGradient * intersectPoint.x + b;
			intersectPoint2.y = panelHeight - 20.0;
			intersectPoint2.x = (intersectPoint2.y - b) / lineGradient;

			if (LL1.getLatitude() >= 0.0) {
				latDegs = Math.floor(LL1.getLatitude());
				latMins = (LL1.getLatitude() - latDegs) * 60.0;
			} else {
				latDegs = Math.ceil(LL1.getLatitude());
				latMins = -(LL1.getLatitude() - latDegs) * 60.0;
			}

			LL1.setLongitude(maxLong); // -(1)/longLinesPerDegree;
			// coord1=rectProj.LL2panel(LL1);
			// coord1.x = panelWidth;
			latString = (String.valueOf(f.format(latDegs)) + (char) 176 + " "
					+ String.valueOf(f.format(latMins)) + (char) 180 + " Lat");
			stringWidth = (float) latLongFont.getStringBounds(latString, frc)
					.getWidth();
			stringHeight = (float) latLongFont.getStringBounds(latString, frc)
					.getHeight();

			(g2d).setColor(currentColor);
			(g2d).fillRect(
					(int) (intersectPoint.x - stringWidth / 2.0),
					(int) (intersectPoint.y - stringHeight / 2 - 3),
					(int) stringWidth + 6, (int) stringHeight + 6);
			(g2d).setColor(latLineColor);
			(g2d).drawString(latString,
					(int) (intersectPoint.x - stringWidth / 2.0),
					(int) (intersectPoint.y + stringHeight / 2));
			(g2d).setColor(currentColor);
			(g2d).fillRect(
					(int) (intersectPoint2.x - stringWidth / 2.0),
					(int) (intersectPoint2.y - stringHeight / 2 - 3),
					(int) stringWidth + 6, (int) stringHeight + 6);
			(g2d).setColor(latLineColor);
			(g2d).drawString(latString,
					(int) (intersectPoint2.x - stringWidth / 2.0),
					(int) (intersectPoint2.y + stringHeight / 2));
			// System.out.println("StraightLineGrid, intersectPoint2: " +
			// intersectPoint2.x + ", " + intersectPoint2.y + " panelHeight: " +
			// panelHeight+ " panelWidth: " + panelWidth) ;

		}

		for (int i = 0; i < numLongLines + 1; i++) {
			LL1.setLongitude(minPlottedLong + i / longLinesPerDegree);
			LL1.setLatitude(maxLat);
			LL2.setLongitude(LL1.getLongitude());
			LL2.setLatitude(minLat);
			// System.out.println("StraightLineGrid, LongLines..." + LL1.latDegs
			// + " " + LL1.longDegs + " " + LL2.latDegs + " " + LL2.longDegs);
			coord1 = rectProj.LL2panel(LL1);
			coord2 = rectProj.LL2panel(LL2);
			g2d.drawLine((int) coord1.x, (int) coord1.y, (int) (coord2.x),
					(int) coord2.y);

			if (Math.abs(coord2.x - coord1.x) > 0) {
				lineGradient = (coord2.y - coord1.y) / (coord2.x - coord1.x);
				b = coord1.y - lineGradient * coord1.x;
				intersectPoint2.y = 40.0;
				intersectPoint2.x = (intersectPoint2.y - b) / lineGradient;
				intersectPoint.x = 50.0;
				intersectPoint.y = lineGradient * intersectPoint.x + b;
			} else {
				intersectPoint2.x = coord1.x;
				intersectPoint2.y = 40.0;
				intersectPoint.x = -100.0;
				intersectPoint.y = -100.0;

			}

			// double longDegs;
			// double longMins;
			if (LL1.getLongitude() >= 0.0) {
				longDegs = Math.floor(LL1.getLongitude());
				longMins = (LL1.getLongitude() - longDegs) * 60.0;
			} else {
				longDegs = Math.ceil(LL1.getLongitude());
				longMins = -(LL1.getLongitude() - longDegs) * 60.0;
			}

			// LL1.latDegs=maxLat;
			// ////minPlottedLat+(Math.floor(numLatLines/2+1)+0.5)/latLinesPerDegree;
			// coord1=rectProj.LL2panel(LL1);
			longString = (String.valueOf(f.format(longDegs)) + (char) 176 + " "
					+ String.valueOf(f.format(longMins)) + (char) 180 + " Lon");
			stringWidth = (float) latLongFont.getStringBounds(longString, frc)
					.getWidth();
			stringHeight = (float) latLongFont.getStringBounds(longString, frc)
					.getHeight();

			(g2d).setColor(currentColor);
			(g2d).fillRect(
					(int) (coord1.x - stringWidth / 2.0 - 3), (int) (coord1.y
							- stringHeight / 2.0 - 3), (int) stringWidth + 6,
					(int) stringHeight + 6);
			// ((Graphics2D) g2d).setColor(longLineColor);
			// ((Graphics2D)
			// g2d).drawString(longString,(int)(coord1.x-stringWidth/2.0),(int)(coord1.y+stringHeight/2.0));
			// ((Graphics2D) g2d).setColor(currentColor);
			// ((Graphics2D)
			// g2d).fillRect((int)(intersectPoint.x-stringWidth/2.0),(int)(intersectPoint.y-
			// stringHeight/2-3),(int)stringWidth+6, (int)stringHeight+6);
			(g2d).setColor(longLineColor);
			(g2d).drawString(longString,
					(int) (intersectPoint.x - stringWidth / 2.0),
					(int) (intersectPoint.y + stringHeight / 2));
			(g2d).setColor(currentColor);
			(g2d).fillRect(
					(int) (intersectPoint2.x - stringWidth / 2.0),
					(int) (intersectPoint2.y - stringHeight / 2 - 3),
					(int) stringWidth + 6, (int) stringHeight + 6);
			(g2d).setColor(longLineColor);
			(g2d).drawString(longString,
					(int) (intersectPoint2.x - stringWidth / 2.0),
					(int) (intersectPoint2.y + stringHeight / 2));

		}

		(g2d).setColor(currentColor);

	}

	public void setPanelHeight(int height) {
		this.panelHeight = height;
	}

	public void setPanelWidth(int width) {
		this.panelWidth = width;
	}

	public void setPixelsPerMetre(double pixelsPerMetre) {
		this.pixelsPerMetre = pixelsPerMetre;
	}

	public void setLatLinesPerDegree(double latLinesPerDegree) {
		this.latLinesPerDegree = latLinesPerDegree;
	}

	public void setLongLinesPerDegree(double longLinesPerDegree) {
		this.longLinesPerDegree = longLinesPerDegree;
	}

	public void setMapRangeMetres(double mapRangeMetres) {
		this.mapRangeMetres = mapRangeMetres;
	}

	public void setMapRotation(double mapRotation) {
		this.mapRotation = mapRotation;
	}

}
