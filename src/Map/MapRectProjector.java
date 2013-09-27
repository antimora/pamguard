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

import java.awt.event.MouseMotionAdapter;

import PamUtils.Coordinate3d;
import PamUtils.LatLong;

public class MapRectProjector extends MapProjector {
	static final double EARTHRADIUS = 6378160.;

	double xScale;

	double yScale;

	double zScale;

	double originX;

	double originY;

	double originZ;

	double mapRangeMetres;

	LatLong mapCentreDegrees;// = new LatLong();

	double panelWidth;

	double panelHeight;

	double mapRotationDegrees = 0.0;

	double pixelsPerMetre;
	
	MapPanel mapPanelRef;
	
	LatLong lastClickedMouseLatLong;
	
	
	
	

	public MapRectProjector() {
		super();
		mapCentreDegrees = new LatLong();
		lastClickedMouseLatLong = new LatLong();
		
	}

	@Override
	public void setScales(double xScale, double yScale, double zScale,
			double originX, double originY, double originZ) {
		this.originX = originX;
		this.originY = originY;
		this.originZ = originZ;
		this.xScale = xScale;
		this.yScale = yScale;
		this.zScale = zScale;
	}

	@Override
	public Coordinate3d getCoord3d(double latDegrees, double longDegrees,
			double d3) {
		Coordinate3d panelPos = new Coordinate3d();
		panelPos.x = (longDegrees - mapCentreDegrees.getLongitude())
				* 2
				* Math.PI
				* EARTHRADIUS
				* Math
						.abs(Math.cos(mapCentreDegrees.getLatitude() * Math.PI
								/ 180.0)) * pixelsPerMetre / 360.0;
		panelPos.y = (latDegrees - mapCentreDegrees.getLatitude()) * 2 * Math.PI
				* EARTHRADIUS * pixelsPerMetre / 360.0;
		TransformUtilities.rotationDegreesXYZ(panelPos, mapRotationDegrees);
		panelPos.x = panelPos.x + panelWidth / 2.0;
		panelPos.y = panelHeight / 2.0 - panelPos.y;
		return (panelPos);
	}
	
	public Coordinate3d lld2Coord3dMeters(double latDegrees, double longDegrees,
			double d3, LatLong origin) {
		Coordinate3d offsetPosition  = new Coordinate3d();
		offsetPosition.x = (longDegrees - origin.getLongitude())
				* 2
				* Math.PI
				* EARTHRADIUS
				* Math
						.abs(Math.cos(origin.getLatitude() * Math.PI
								/ 180.0)) / 360.0;
		offsetPosition.y = (latDegrees - origin.getLatitude()) * 2 * Math.PI
				* EARTHRADIUS  / 360.0;
		return (offsetPosition);
	}

	public Coordinate3d LL2panel(LatLong LL) {
		return (getCoord3d(LL.getLatitude(), LL.getLongitude(), 0));

		/*
		 * Coordinate3d panelPos = new Coordinate3d(); panelPos.x =
		 * (mapCentreDegrees.longDegs-LL.longDegs)*2*Math.PI*EARTHRADIUS*Math.cos(mapCentreDegrees.getLatitude()*Math.PI/180.0)*pixelsPerMetre/360.0;
		 * panelPos.y =
		 * (LL.getLatitude()-mapCentreDegrees.getLatitude())*2*Math.PI*EARTHRADIUS*pixelsPerMetre/360.0;
		 * panelPos=TransformUtilities.rotationDegreesXYZ(panelPos,mapRotationDegrees);
		 * panelPos.x=panelPos.x+(double)panelWidth/2.0;
		 * panelPos.y=(double)panelHeight/2.0-panelPos.y; return(panelPos);
		 */
	}

	/*
	 * public LatLongDegrees panel2LL(Coordinate3d panelPos2){
	 * return(panel2LL(panelPos2.x, panelPos2.y, panelPos2.z)); }
	 */

	public LatLong panel2LL(Coordinate3d c) {
		LatLong LL = new LatLong();
		c.y = panelHeight / 2.0 - c.y;
		c.x = c.x - panelWidth / 2;
		c = TransformUtilities.rotationDegreesXYZ(c, -mapRotationDegrees);
		// LL.longDegs=c.x*360.0/Math.abs(Math.cos(mapCentreDegrees.getLatitude()*Math.PI/180.0)*2*Math.PI*EARTHRADIUS*pixelsPerMetre)-mapCentreDegrees.longDegs;
		LL.setLongitude( mapCentreDegrees.getLongitude()
				+ (c.x * 360.0)
				/ (Math.abs(Math
						.cos(mapCentreDegrees.getLatitude() * Math.PI / 180.0))
						* 2 * Math.PI * EARTHRADIUS * pixelsPerMetre));
		LL.setLatitude( c.y * 360.0 / (2 * Math.PI * EARTHRADIUS * pixelsPerMetre)
				+ mapCentreDegrees.getLatitude());
		return (LL);
	}

	public LatLong image2LL(Coordinate3d c) {
		LatLong LL = new LatLong();
		// LL.longDegs=c.x*360.0/(Math.abs(Math.cos(mapCentreDegrees.getLatitude()*Math.PI/180.0))*2*Math.PI*EARTHRADIUS*pixelsPerMetre);
		LL.setLongitude( c.x
				* 360.0
				/ (Math.abs(Math
						.cos(mapCentreDegrees.getLatitude() * Math.PI / 180.0))
						* 2 * Math.PI * EARTHRADIUS) / pixelsPerMetre);

		// LL.getLatitude()=c.y*360.0/(2*Math.PI*EARTHRADIUS*pixelsPerMetre);
		LL.setLatitude( c.y * 360.0 / (2 * Math.PI * EARTHRADIUS) / pixelsPerMetre );
		return (LL);
	}

	public LatLong getMapCentreDegrees() {
		return mapCentreDegrees;
	}

	public void setMapCentreDegrees(LatLong mapCentreDegrees) {
		this.mapCentreDegrees = mapCentreDegrees;
	}

	public double getMapRangeMetres() {
		return mapRangeMetres;
	}

	public void setMapRangeMetres(double mapRangeMetres) {
		this.mapRangeMetres = mapRangeMetres;
	}

	public double getMapRotationDegrees() {
		return mapRotationDegrees;
	}

	public void setMapRotationDegrees(double mapRotationDegrees) {
		this.mapRotationDegrees = mapRotationDegrees;
	}

	public double getPanelHeight() {
		return panelHeight;
	}

	public void setPanelHeight(double panelHeight) {
		this.panelHeight = panelHeight;
	}

	public double getPanelWidth() {
		return panelWidth;
	}

	public void setPanelWidth(double panelWidth) {
		this.panelWidth = panelWidth;
	}

	public double getPixelsPerMetre() {
		return pixelsPerMetre;
	}

	public void setPixelsPerMetre(double pixelsPerMetre) {
		this.pixelsPerMetre = pixelsPerMetre;
	}

	public void setMapPanelRef(MapPanel mapPanelRef) {
		this.mapPanelRef = mapPanelRef;
	}

	public MapPanel getMapPanelRef() {
		return mapPanelRef;
	}

	public LatLong getLastClickedMouseLatLong() {
		if (mapPanelRef != null) {
			if (mapPanelRef.simpleMapRef != null) {
				lastClickedMouseLatLong = mapPanelRef.simpleMapRef.getLastClickedMouseLatLong();
			}
		}
		return lastClickedMouseLatLong;
	}

	public void setLastClickedMouseLatLong(LatLong lastClickedMouseLatLong) {
		this.lastClickedMouseLatLong.setLatitude(lastClickedMouseLatLong.getLatitude());
		this.lastClickedMouseLatLong.setLongitude(lastClickedMouseLatLong.getLongitude());
	}
	
	public MouseMotionAdapter getMouseMotionAdapter(){
		return mapPanelRef.getSimpleMapRef().getMouseMotion();
	}


	
}
