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

package Array;

import java.awt.Color;
import java.io.Serializable;
import java.util.Arrays;

import pamMaths.PamVector;

import PamView.PamSymbol;

/**
 * 
 * @author Doug Gillespie Contains information on a single hydrophone
 * @see Array.PamArray
 */
public class Hydrophone implements Serializable, Cloneable {
	

	public static final long serialVersionUID = 1;

	private int iD;
	
	private int streamerId;
	
	private String type;

	private double[] coordinate = new double[3];
	
	private double[] coordinateError = new double[3];
	
	/*
	 * Angle from horizontal - positive = angled up
	 */
	private double tilt;
	
	/*
	 * angle relative to ?
	 */
	private double heading;
	
	private double sensitivity;
	
	private double preampGain;

	private double[] bandwidth = {1, 20000}; //new double[2];

	//private Preamplifier preamp;
	transient private PamSymbol symbol;
	
	private boolean dontInvertDepth = true;
	

	/**
	 * A funny one this - on 22 December, I realised I had
	 * to invert the depth coordinate to satisfy standard RH rotation geometry. 
	 * <p>so that old depths get automatically turned upside down in the main 
	 * coordinate variable, I include this. It a new phone is created, then 
	 * this will default to true and we'll know that all is OK. If an old
	 * hydrophone is deserialised from a settings file, then this will 
	 * default to false - so we'll know to turn the depth coordinate over. 
	 */
	private void checkDepthInversion() {
		if (dontInvertDepth) return;
		coordinate[2] = -coordinate[2];
		dontInvertDepth = true;
	}


	public Hydrophone(int id) {
		super();
		symbol = getDefaultSymbol();
		iD = id;
	}
	
	PamSymbol getDefaultSymbol() {
		return new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 5, 5, false, Color.BLUE, Color.BLUE);
	}
	
	public Hydrophone(int id, double x, double y, double z, String type, double sensitivity,
			double[] bandwidth, double preampGain) {
		super();
		this.iD = id;
		this.coordinate[0] = x;
		this.coordinate[1] = y;
		this.coordinate[2] = z;
		this.type = new String(type);
		this.sensitivity = sensitivity;
		this.bandwidth = bandwidth;
		this.preampGain = preampGain;
	}

	public double[] getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(double[] bandwidth) {
		this.bandwidth = bandwidth;
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(double sensitivity) {
		this.sensitivity = sensitivity;
	}
	
	private void checkCoordinateError() {
		if (coordinateError == null || coordinateError.length != 3) {
			coordinateError = new double[3];
		}
	}
	
	/**
	 * Get the error on one of the three coordinates. 
	 * @param iCoordinate coordinate (x = 0; y = 1; z = 2)
	 * @return error in metres. 
	 */
	public double getCoordinateError(int iCoordinate) {
		checkCoordinateError();
		return coordinateError[iCoordinate];
	}
	
	/**
	 * Set the coordinate error array. 
	 * @param errors array of errors on x,y,z
	 */
	public void setCoordinateErrors(double[] errors) {
		this.coordinateError = errors;
	}
	/**
	 * Set the coordinate error on one of the three dimensions. 
	 * @param iCoordinate coordinate (x = 0; y = 1; z = 2)
	 * @param error error in metres. 
	 */
	public void setCoordinateError(int iCoordinate, double error) {
		coordinateError[iCoordinate] = error;
	}
	
	public double[] getCoordinateErrors() {
		checkCoordinateError();
		return coordinateError;
	}
	
	/**
	 * @return error on the hydrophone x coordinate. 
	 */
	public double getdX() {
		return getCoordinateError(0);
	}

	/**
	 * Set the error on the hydrophone x coordinate
	 * @param error error in metres. 
	 */
	public void setdX(double error) {
		setCoordinateError(0, error);
	}

	
	/**
	 * @return error on the hydrophone y coordinate. 
	 */
	public double getdY() {
		return getCoordinateError(1);
	}

	/**
	 * Set the error on the hydrophone y coordinate
	 * @param error error in metres. 
	 */
	public void setdY(double error) {
		setCoordinateError(1, error);
	}

	/**
	 * @return error on the hydrophone depth coordinate. 
	 */
	public double getdZ(){
		return getCoordinateError(2);
	}

	/**
	 * Set the error on the hydrophone z coordinate
	 * @param error error in metres. 
	 */
	public void setdZ(double error) {
		setCoordinateError(2, error);
	}

	public double getX() {
		return coordinate[0];
	}

	public void setX(double x) {
		this.coordinate[0] = x;
	}

	public double getY() {
		return coordinate[1];
	}
	
	/**
	 * 
	 * @return the coordinates in vector form. s
	 */
	public PamVector getVector() {
		return new PamVector(coordinate);
	}
	
	/**
	 * 
	 * @return the coordinate errors in vector form.
	 */
	public PamVector getErrorVector() {
		return new PamVector(coordinateError);
	}
	
	/**
	 * Get the hydrophone distance astern, corrected for hydrophone depth. 
	 * @param depth
	 * @return corrected y coordinate. 
	 */
	public double getY(double depth) {
		double y = getY();
		depth = Math.max(0, Math.min(depth, Math.abs(y)));
		double y2 = Math.sqrt(y * y - depth * depth);
		if (y < 0) {
			y2 = -y2;
		}
		return y2;
	}

	public void setY(double y) {
		this.coordinate[1] = y;
	}

	public double getZ() {
		return coordinate[2];
	}

	public void setZ(double z) {
		this.coordinate[2] = z;
	}

	public double getDepth() {
		return -coordinate[2];
	}

	public void setDepth(double d) {
		this.coordinate[2] = -d;
	}

	/**
	 * Get's a coordinate of the array 
	 * @param c index of the coordinate (0 = x; 1 = y; 2 = z)
	 * @return coordinate value
	 */
	public double getCoordinate(int c) {
		return coordinate[c];
	}

	@Override
	protected Hydrophone clone() {
		try {
			// need to explicity create a new copy of the double[3] coordinate 
			// and the double[2] bandwidth
			Hydrophone h = (Hydrophone) super.clone();
			h.setBandwidth(Arrays.copyOf(getBandwidth(), h.getBandwidth().length));
			h.setCoordinate(Arrays.copyOf(getCoordinates(), 3));
			h.setCoordinateErrors(Arrays.copyOf(getCoordinateErrors(), 3));
			h.checkDepthInversion();
			return h;
		} catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

	public String getType() {
		return type;
	}

	/**
	 * @return Returns the coordinate.
	 */
	public double[] getCoordinates() {
		return Arrays.copyOf(coordinate,3);
	}

	/**
	 * @param coordinate The coordinate to set.
	 */
	public void setCoordinate(double[] coordinate) {
		this.coordinate = coordinate;
	}

	/**
	 * @return Returns the preampGain.
	 */
	public double getPreampGain() {
		return preampGain;
	}

	/**
	 * @param preampGain The preampGain to set.
	 */
	public void setPreampGain(double preampGain) {
		this.preampGain = preampGain;
	}

	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return Returns the iD.
	 */
	public int getID() {
		return iD;
	}

	/**
	 * @param id The iD to set.
	 */
	public void setID(int id) {
		iD = id;
	}

	/**
	 * @return Returns the heading.
	 */
	public double getHeading() {
		return heading;
	}

	/**
	 * @param heading The heading to set.
	 */
	public void setHeading(double heading) {
		this.heading = heading;
	}

	/**
	 * @return Returns the tilt.
	 */
	public double getTilt() {
		return tilt;
	}

	/**
	 * @param tilt The tilt to set.
	 */
	public void setTilt(double tilt) {
		this.tilt = tilt;
	}

	public PamSymbol getSymbol() {
		if (symbol == null) symbol = getDefaultSymbol();
		return symbol;
	}

	public void setSymbol(PamSymbol symbol) {
		this.symbol = symbol;
	}

	public int getStreamerId() {
		return streamerId;
	}

	public void setStreamerId(int streamerId) {
		this.streamerId = streamerId;
	}

}
