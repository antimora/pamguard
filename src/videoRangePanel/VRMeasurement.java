package videoRangePanel;

import java.awt.Point;
import java.io.Serializable;


public class VRMeasurement implements Serializable, Cloneable {

	public Point animalPoint;
	
	public Point horizonPoint;
	
	public double distancePixels;
	
	public double distanceMeters = 0;
	
	public double pixelAccuracy = 0;
	
	public String imageName;
	
	public int imageAnimal;
	
	public int groupSize;
	
	public Double cameraAngle = null; // angle of camera / binoculars relative to North  heading, etc. 
	
	public double angleCorrection = 0;
	
	public double totalAngle = 0; // shoul be sum of cameraAngle and angleCorrection. 
	
	public String comment;
	
	public VRCalibrationData calibrationData;
	
	public VRHeightData heightData;
	
	public VRRangeMethod rangeMethod;
	
	public int measurementType;
	
	public static final int MEASURE_FROM_HORIZON = VRControl.MEASURE_FROM_HORIZON;
	public static final int MEASURE_FROM_SHORE = VRControl.MEASURE_FROM_SHORE;

	/**
	 * Constructor for measurements from the horizon
	 * @param horizonPoint1 first horizon point
	 * @param horizonPoint2 second horizon point
	 * @param animalPoint clicked animal point. 
	 */
	public VRMeasurement(Point horizonPoint1, Point horizonPoint2, Point animalPoint) {
		super();
		this.animalPoint = animalPoint;
		
		measurementType = MEASURE_FROM_HORIZON;
		/* 
		 * work out the perpendicular intercept point on the horizon.
		 */
		horizonPoint = new Point();
		
		if (horizonPoint1.x == horizonPoint2.x) {
			// vertical horizon
			horizonPoint.y = animalPoint.y;
			horizonPoint.x = horizonPoint1.x;
		}
		else if (horizonPoint1.y == horizonPoint2.y) {
			// horizontal horizon
			horizonPoint.y = horizonPoint1.y;
			horizonPoint.x = animalPoint.x;
		} 
		else {
			double x1  = horizonPoint1.x - animalPoint.x;
			double y1  = horizonPoint1.y - animalPoint.y;
			double x2  = horizonPoint2.x - animalPoint.x;
			double y2  = horizonPoint2.y - animalPoint.y;
			// slanted horizon
			double T = (y2 - y1) / (x2 - x1);
			horizonPoint.y = (int) ((y1 - x1 * T) / 
			(1 + T * T));
			horizonPoint.x =(int) ((y1 - T * x1) / 
			(-1/T - T));
			horizonPoint.y += animalPoint.y;
			horizonPoint.x += animalPoint.x;
		}
		
		distancePixels = horizonPoint.distance(animalPoint);
	}
	
	/**
	 * Constructor for shore based measurements
	 * @param shorePoint clicked shore point on image
	 * @param animalPoint clicked animal point on image
	 */
	public VRMeasurement(Point shorePoint, Point animalPoint) {
		super();
		this.animalPoint = animalPoint;
		measurementType = MEASURE_FROM_SHORE;
		distancePixels = shorePoint.distance(animalPoint);
	}
	
	/**
	 * Returns the angle of the animal in degrees. 
	 * @param vrCalibrationData
	 * @return angle in degrees based on measured distance in pixels and 
	 * given calibration data. 
	 */
	public double getAnimalAngleDegrees(VRCalibrationData vrCalibrationData) {
		double angle = distancePixels * vrCalibrationData.degreesPerUnit;
		return angle;
	}
	
	public double getAnimalAngleRadians(VRCalibrationData vrCalibrationData) {
		return getAnimalAngleDegrees(vrCalibrationData) * Math.PI / 180.;
	}
	
	public String getHoverText() {
		String str = String.format("<html>Image %s (%d)<br>Range: %.1f m<br>Bearing: %.1f\u00B0",
				imageName, imageAnimal, distanceMeters, totalAngle);
		if (comment != null) {
			str += "<br>" + comment;
		}
		str += "</html>";
		return str;
	}

	@Override
	protected VRMeasurement clone() {
		try {
			return (VRMeasurement) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
