package radardisplay;

import java.awt.Point;

import clickDetector.ClickBTDisplay.PlotKeyListener;

import PamUtils.Coordinate3d;
import PamView.GeneralProjector;

public class RadarProjector extends GeneralProjector {

	private RadarDisplay radarDisplay;
	
	private RadarParameters radarParameters;

	private double headingReference;
	
	public RadarProjector(RadarDisplay radarDisplay) {
		this.radarDisplay = radarDisplay;
		setParmeterType(0, ParameterType.BEARING);
		setParmeterType(1, ParameterType.AMPLITUDE);
	}
	
	@Override
	public Coordinate3d getCoord3d(double d1, double d2, double d3) {
		/*
		 * d1 is bearing
		 * d2 is amplitude or range
		 */
		switch(getParmeterType(1)) {
		case AMPLITUDE:
			return getAmplitudeCood3d(d1, d2);
		case RANGE:
			return getRangeCood3d(d1, d2);
		case SLANTANGLE:
			return getSlantCoord3d(d1,d2);
		}
		return null;
//		if (getParmeterType(1) == ParameterType.AMPLITUDE) {
//			return getAmplitudeCood3d(d1, d2);
//		}
//		else {
//			return getRangeCood3d(d1, d2);
//		}
	}
	
	private Coordinate3d getAmplitudeCood3d(double bearing, double amplitude) {
		
		Coordinate3d coord = new Coordinate3d();
		Point p = radarDisplay.getCentre();

		double maxRadius = radarDisplay.getRadius();
		double r = maxRadius * (amplitude - radarParameters.rangeStartdB) / 
		(radarParameters.rangeEnddB - radarParameters.rangeStartdB);
		r = Math.max(r, 0.);
		// if points are beyond the nominal radius, then draw them just outside the circle ...
		r = Math.min(r, maxRadius * 1.1);
		double ang = Math.toRadians(90. - bearing) - headingReference;
		coord.x = p.x + r * Math.cos(ang);
		coord.y = p.y - r * Math.sin(ang);
		return coord;
		
	}
	
	/**
	 * be warned that this function will return null if 
	 * the given bearing cannot be displayed !
	 * @param bearing
	 * @param range
	 * @return the 3-D coordinate of the given bearing and range
	 */
	private Coordinate3d getRangeCood3d(double bearing, double range) {

		if (canPlot(bearing) == false) return null;
		
		Coordinate3d coord = new Coordinate3d();
		Point p = radarDisplay.getCentre();
		
		double maxRadius = radarDisplay.getRadius();
		double r = maxRadius * (range - radarParameters.rangeStartm) / 
		(radarParameters.rangeEndm - radarParameters.rangeStartm);
		r = Math.max(r, 0.);
		// if points are beyond the nominal radius, then draw them just outside the circle ...
		r = Math.min(r, maxRadius * 1.1);
		
		double ang = Math.toRadians(90. - bearing) - headingReference;
		coord.x = p.x + r * Math.cos(ang);
		coord.y = p.y - r * Math.sin(ang);
		
		return coord;
	}
	
	public double getPixelsPerMetre() {

		if (getParmeterType(1) == ParameterType.AMPLITUDE) {
			return Double.NaN;
		}
		return radarDisplay.getRadius() / (radarParameters.rangeEndm - radarParameters.rangeStartm);
	}

	private Coordinate3d getSlantCoord3d(double bearing, double slantAngle) {

		if (canPlot(bearing) == false) return null;
		
		Coordinate3d coord = new Coordinate3d();
		Point p = radarDisplay.getCentre();
		
		double maxRadius = radarDisplay.getRadius();
		double r = maxRadius * (90.-Math.abs(slantAngle)) / 90.;
		r = Math.max(r, 0.);
		// if points are beyond the nominal radius, then draw them just outside the circle ...
		r = Math.min(r, maxRadius * 1.1);
		
		double ang = Math.toDegrees(90. - bearing) - headingReference;
		coord.x = p.x + r * Math.cos(ang);
		coord.y = p.y - r * Math.sin(ang);
		
		return coord;
	}

	public void setRadarParameters(RadarParameters radarParameters) {
		this.radarParameters = radarParameters;
		switch(radarParameters.radialAxis) {
		case RadarParameters.RADIAL_AMPLITIDE:
			setParmeterType(1, ParameterType.AMPLITUDE);
			break;
		case RadarParameters.RADIAL_DISTANCE:
			setParmeterType(1, ParameterType.RANGE);
			break;
		case RadarParameters.RADIAL_SLANT_ANGLE:
			setParmeterType(1, ParameterType.SLANTANGLE);
			break;
		default:
			setParmeterType(1, ParameterType.SLANTANGLE);
			break;
		}
	}

	/**
	 * Return true if the point can be plotted on this particular 
	 * display. This is based purely on the style of the radar plot
	 * and whether or not the angle fits in within the drawn segment.
	 * Bearings are measured clockwise from north (geometric calculations
	 * are generally anti clockwise from east)
	 * @return true if the bearing can be plotted on the current display
	 */
	private boolean canPlot(double bearing) {
		switch (radarParameters.sides) {
		case RadarParameters.SIDES_ALL:
			return true;
		case RadarParameters.SIDES_BACKHALF:
			return ((bearing >= 90 && bearing <= 270) || bearing <= -90);
		case RadarParameters.SIDES_FRONTHALF:
			return ((bearing >= -90 && bearing <= 90) || bearing >= 270);
		case RadarParameters.SIDES_LEFTHALF:
			return ((bearing >= -180 && bearing <= 0) || bearing >= 180);
		case RadarParameters.SIDES_RIGHTHALF:
			return (bearing >= 0 && bearing <= 180);
		}
		return false;
	}

	public RadarDisplay getRadarDisplay() {
		return radarDisplay;
	}

	/**
	 * Set the plot heading reference in radians. this is used
	 * when plotting relative to true North. It's zero for plotting 
	 * headings relative to the array. In existing code, all data are plotted
	 * using only relative heading, so this will make a conversion to true north
	 * for all data. 
	 * @param plotHeading in RADIANS !!!!!
	 */
	public void setHeadingReference(double plotHeading) {
		this.headingReference = plotHeading;
	}
	
}
