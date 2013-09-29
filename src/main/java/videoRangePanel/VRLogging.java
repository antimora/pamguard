package videoRangePanel;

import java.sql.Types;

import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class VRLogging extends SQLLogging {

	VRControl vrControl;
	
	VRProcess vrProcess;
	
	PamTableDefinition vrTable;
	
	PamTableItem range, rangeError, pixels, degrees, image, heightValue, heightName, 
		method, calibrationValue, calibrationName;
	PamTableItem imageAnimal, cameraAngle, angleCorrection, totalAngle, comment;
	

	public VRLogging(VRControl vrControl, VRProcess vrProcess) {
		super(vrProcess.getVrDataBlock());
		this.vrControl = vrControl;
		this.vrProcess = vrProcess;
		setUpdatePolicy(SQLLogging.UPDATE_POLICY_WRITENEW);
		vrTable = new PamTableDefinition(vrControl.getUnitName(), SQLLogging.UPDATE_POLICY_WRITENEW);
		vrTable.addTableItem(range = new PamTableItem("Range", Types.DOUBLE));
		vrTable.addTableItem(rangeError = new PamTableItem("Range Error", Types.DOUBLE));
		vrTable.addTableItem(pixels = new PamTableItem("Pixels", Types.DOUBLE));
		vrTable.addTableItem(degrees = new PamTableItem("Degrees", Types.DOUBLE));
		vrTable.addTableItem(image = new PamTableItem("Image Name", Types.CHAR, 20));
		vrTable.addTableItem(imageAnimal = new PamTableItem("Image Animal", Types.INTEGER));
		vrTable.addTableItem(heightValue = new PamTableItem("Height", Types.DOUBLE));
		vrTable.addTableItem(heightName = new PamTableItem("Height Name", Types.CHAR, 20));
		vrTable.addTableItem(method = new PamTableItem("Method", Types.CHAR, 20));
		vrTable.addTableItem(calibrationValue = new PamTableItem("Calibration", Types.DOUBLE));
		vrTable.addTableItem(calibrationName = new PamTableItem("Calibration Name", Types.CHAR, 20));
		vrTable.addTableItem(cameraAngle = new PamTableItem("Camera Angle", Types.DOUBLE));
		vrTable.addTableItem(angleCorrection = new PamTableItem("Angle Correction", Types.DOUBLE));
		vrTable.addTableItem(totalAngle = new PamTableItem("Total Angle", Types.DOUBLE));
		vrTable.addTableItem(comment = new PamTableItem("Comment", Types.CHAR, VRControl.DBCOMMENTLENGTH));

		setTableDefinition(vrTable);
	}

//	@Override
//	public PamTableDefinition getTableDefinition() {	
//		return vrTable;
//	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {

		VRDataUnit vrDataUnit = (VRDataUnit) pamDataUnit;
		VRMeasurement vrm = vrDataUnit.getVrMeasurement();

		range.setValue(vrm.distanceMeters);
		rangeError.setValue(vrm.pixelAccuracy);
		pixels.setValue(vrm.distancePixels);
		degrees.setValue(vrm.getAnimalAngleDegrees(vrm.calibrationData));
		image.setValue(vrm.imageName);
		imageAnimal.setValue(vrm.imageAnimal);
		heightValue.setValue(vrm.heightData.height);
		heightName.setValue(vrm.heightData.name);
		method.setValue(vrm.rangeMethod.getName());
		calibrationValue.setValue(vrm.calibrationData.degreesPerUnit);
		calibrationName.setValue(vrm.calibrationData.name);
		cameraAngle.setValue(vrm.cameraAngle);
		angleCorrection.setValue(vrm.angleCorrection);
		totalAngle.setValue(vrm.totalAngle);
		comment.setValue(vrm.comment);
	}

}
