package targetMotion;

import java.sql.Timestamp;
import java.sql.Types;

import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLoggingAddon;

public class TargetMotionSQLLogging implements SQLLoggingAddon {

	private String colSuffix = "";
	
	private TargetMotionLocaliser targetMotionLocaliser;
	
	/**
	 * @param colSuffix
	 */
	public TargetMotionSQLLogging(String colSuffix) {
		super();
		this.colSuffix = colSuffix;
		if (this.colSuffix == null) {
			this.colSuffix = "";
		}
	}

	private PamTableItem modelName, latitude, longitude, side, chi2, aic, prob, degsFreedom, 
	perpDist, perpdistErr, phones, comment, beamLatitude, beamLongitude, beamTime, depth, depthErr;
	
	@Override
	public void addTableItems(PamTableDefinition pamTableDefinition) {

		pamTableDefinition.addTableItem(modelName = new PamTableItem("TMModelName"+colSuffix, Types.CHAR, 30));
		pamTableDefinition.addTableItem(latitude = new PamTableItem("TMLatitude"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(longitude = new PamTableItem("TMLongitude"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(beamLatitude = new PamTableItem("BeamLatitude"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(beamLongitude = new PamTableItem("BeamLongitude"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(beamTime = new PamTableItem("BeamTime"+colSuffix, Types.TIMESTAMP));
		pamTableDefinition.addTableItem(side = new PamTableItem("TMSide"+colSuffix, Types.INTEGER));
		pamTableDefinition.addTableItem(chi2 = new PamTableItem("TMChi2"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(aic = new PamTableItem("TMAIC"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(prob = new PamTableItem("TMProbability"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(degsFreedom = new PamTableItem("TMDegsFreedom"+colSuffix, Types.INTEGER));
		pamTableDefinition.addTableItem(perpDist = new PamTableItem("TMPerpendicularDistance"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(perpdistErr = new PamTableItem("TMPerpendicularDistanceError"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(depth = new PamTableItem("TMDepth"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(depthErr = new PamTableItem("TMDepthError"+colSuffix, Types.DOUBLE));
		pamTableDefinition.addTableItem(phones = new PamTableItem("TMHydrophones"+colSuffix, Types.INTEGER));
		pamTableDefinition.addTableItem(comment = new PamTableItem("TMComment"+colSuffix, Types.CHAR, 80));
	}

	@Override
	public boolean saveData(PamTableDefinition pamTableDefinition,
			PamDataUnit pamDataUnit) {
		try {
			TargetMotionLocalisation tml = (TargetMotionLocalisation) pamDataUnit.getLocalisation();
			if (tml == null) {
				clearEverything();
				return false;
			}
			TargetMotionResult tmr = tml.getTargetMotionResult();
			if (tmr == null) {
				clearEverything();
				return false;
			}
			TargetMotionModel model = tmr.getModel();
			if (model == null) {
				modelName.setValue(null);
			}
			else {
				modelName.setValue(tmr.getModel().getName());
			}
			LatLong ll = tmr.getLatLong();
			if (ll != null) {
				latitude.setValue(ll.getLatitude());
				longitude.setValue(ll.getLongitude());
				depth.setValue(-ll.getHeight());
				Double depthError = tmr.getZError();
				if (Double.isNaN(depthError)) {
					depthErr.setValue(null);
				}
				else {
					depthErr.setValue(depthError);
				}
			}
			else {
				longitude.setValue(null);
				latitude.setValue(null);
			}
			ll = tmr.getBeamLatLong();
			if (ll != null) {
				beamLatitude.setValue(ll.getLatitude());
				beamLongitude.setValue(ll.getLongitude());
			}
			else {
				beamLatitude.setValue(null);
				beamLongitude.setValue(null);
			}
			if (tmr.getBeamTime() != null) {
				beamTime.setValue(PamCalendar.getTimeStamp(tmr.getBeamTime()));
			}
			else {
				beamTime.setValue(null);
			}
			side.setValue(tmr.getSide());
			chi2.setValue(tmr.getChi2());
			aic.setValue(tmr.getAic());
			prob.setValue(tmr.getProbability());
			degsFreedom.setValue(tmr.getnDegreesFreedom());
			perpDist.setValue(tmr.getPerpendicularDistance());
			perpdistErr.setValue(tmr.getPerpendicularDistanceError());
			phones.setValue(tmr.getReferenceHydrophones());
			comment.setValue(tmr.getComment());
		}
		catch (ClassCastException e) {
			System.out.println("Localisation is not from target motion analysis in event " + pamDataUnit.getDatabaseIndex());
		}
		return true;
	}

	private void clearEverything() {
		modelName.setValue(null); 
		latitude.setValue(null);
		longitude.setValue(null);
		beamLatitude.setValue(null);
		beamLongitude.setValue(null);
		side.setValue(null);
		depth.setValue(null);
		depthErr.setValue(null);
		chi2.setValue(null);
		aic.setValue(null);
		prob.setValue(null);
		degsFreedom.setValue(null);
		perpDist.setValue(null);
		perpdistErr.setValue(null);
		phones.setValue(null);
		comment.setValue(null);
		beamLatitude.setValue(null);
		beamLongitude.setValue(null);
		beamTime.setValue(null);
	}

	@Override
	public boolean loadData(PamTableDefinition pamTableDefinition,
			PamDataUnit pamDataUnit) {
		/*
		 * 
		modelName.setValue(null); 
		latitude.setValue(null);
		longitude.setValue(null);
		side.setValue(null);
		chi2.setValue(null);
		aic.setValue(null);
		prob.setValue(null);
		degsFreedom.setValue(null);
		perpDist.setValue(null);
		perpdistErr.setValue(null);
		phones.setValue(null);
		 */
		double latVal, longVal;
		Double dVal;
		if (latitude.getValue() == null) {
			return false;
		}
		latVal = latitude.getDoubleValue();
		longVal = longitude.getDoubleValue();
		LatLong ll = new LatLong(latVal, longVal);
		Double dep = depth.getDoubleValue();
		if (dep != null) {
			ll.setHeight(-dep);
		}
		
		
		latVal = beamLatitude.getDoubleValue();
		longVal = beamLongitude.getDoubleValue();
		LatLong bLL = new LatLong(latVal, longVal);
		
		
		Timestamp ts = (Timestamp) beamTime.getValue();
		
		int sideVal = side.getIntegerValue();
		double chiVal = chi2.getDoubleValue();
	
		TargetMotionResult tmr = new TargetMotionResult(null, ll, sideVal, chiVal);
		tmr.setBeamLatLong(bLL);
		if (ts != null) {
			tmr.setBeamTime(PamCalendar.millisFromTimeStamp(ts));
		}
		tmr.setChi2((Double) chi2.getValue());
		tmr.setAic((Double) aic.getValue());
		tmr.setProbability((Double) prob.getValue());
		tmr.setnDegreesFreedom((Integer) degsFreedom.getValue());
		tmr.setPerpendicularDistance((Double) perpDist.getValue());
		tmr.setPerpendicularDistanceError((Double) perpdistErr.getValue());
		if (depthErr.getValue() != null) {
			tmr.setError(2, (Double) depthErr.getValue());
		}
		tmr.setReferenceHydrophones((Integer) phones.getValue());
		tmr.setComment(comment.getDeblankedStringValue());
		String mName = modelName.getStringValue();
		TargetMotionModel model = targetMotionLocaliser.findModelByName(mName, true);
		tmr.setModel(model);
		
		TargetMotionLocalisation tml = new TargetMotionLocalisation(pamDataUnit, tmr);
		pamDataUnit.setLocalisation(tml);
		
		return true;
	}

	/**
	 * @return the targetMotionLocaliser
	 */
	public TargetMotionLocaliser getTargetMotionLocaliser() {
		return targetMotionLocaliser;
	}

	/**
	 * @param targetMotionLocaliser the targetMotionLocaliser to set
	 */
	public void setTargetMotionLocaliser(TargetMotionLocaliser targetMotionLocaliser) {
		this.targetMotionLocaliser = targetMotionLocaliser;
	}

}
