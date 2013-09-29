package staticLocaliser;

import java.sql.Types;

import PamUtils.LatLong;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.SQLTypes;

public class StaticLocaliserTableDefinition extends PamTableDefinition{

	
	//SAVABLE FIELDS
	
	PamTableItem algorithmName	=new PamTableItem("algorithmName",Types.CHAR,30);
	PamTableItem locTime		=new PamTableItem("locTime", Types.DOUBLE);
	PamTableItem locX			=new PamTableItem("locX", Types.DOUBLE);
	PamTableItem locY			=new PamTableItem("locY", Types.DOUBLE);
	PamTableItem locZ			=new PamTableItem("locZ", Types.DOUBLE);
	PamTableItem range			=new PamTableItem("range", Types.DOUBLE);
	PamTableItem xErr			=new PamTableItem("xErr", Types.DOUBLE);
	PamTableItem yErr			=new PamTableItem("yErr", Types.DOUBLE);
	PamTableItem zErr			=new PamTableItem("zErr", Types.DOUBLE);
	PamTableItem rangeErr		=new PamTableItem("rangeErr", Types.DOUBLE);
	PamTableItem chi			=new PamTableItem("chi", Types.DOUBLE);
	PamTableItem referenceHydrophone	=new PamTableItem("referenceHydrophone", Types.INTEGER);
	PamTableItem latitude		=new PamTableItem("latitude", Types.DOUBLE);
	PamTableItem longitude		=new PamTableItem("longitude", Types.DOUBLE);
	PamTableItem probability	=new PamTableItem("probability", Types.DOUBLE);
	PamTableItem nDegreesFreedom=new PamTableItem("nDegreesFreedom", Types.INTEGER);
	PamTableItem aic			=new PamTableItem("aic", Types.DOUBLE);
	PamTableItem ambiguity		=new PamTableItem("ambiguity", Types.INTEGER);
	PamTableItem timeDelay		=new PamTableItem("timeDelay", Types.INTEGER);
	PamTableItem tDPossibilities		=new PamTableItem("tDPossibilities", Types.INTEGER);
	
	
	public StaticLocaliserTableDefinition(String tableName) {
		super(tableName,SQLLogging.UPDATE_POLICY_WRITENEW);
		addTableItem(algorithmName);
		addTableItem(locTime);
		addTableItem(locX);
		addTableItem(locY);
		addTableItem(locZ);
		addTableItem(range);
		addTableItem(xErr);
		addTableItem(yErr);
		addTableItem(zErr);
		addTableItem(rangeErr);
		addTableItem(chi);
		addTableItem(referenceHydrophone);
		addTableItem(latitude);
		addTableItem(longitude);
		addTableItem(probability);
		addTableItem(nDegreesFreedom);
		addTableItem(aic);
		addTableItem(ambiguity);
		addTableItem(timeDelay);
		addTableItem(tDPossibilities);
	}

	

	public PamTableItem getAlgorithmName() {
		return algorithmName;
	}
	
	public PamTableItem getLocTime() {
		return locTime;
	}


	public PamTableItem getLocX() {
		return locX;
	}


	public PamTableItem getLocY() {
		return locY;
	}


	public PamTableItem getLocZ() {
		return locZ;
	}


	public PamTableItem getRange() {
		return range;
	}


	public PamTableItem getxErr() {
		return xErr;
	}


	public PamTableItem getyErr() {
		return yErr;
	}


	public PamTableItem getzErr() {
		return zErr;
	}


	public PamTableItem getRangeErr() {
		return rangeErr;
	}


	public PamTableItem getChi() {
		return chi;
	}


	public PamTableItem getReferenceHydrophone() {
		return referenceHydrophone;
	}


	public PamTableItem getLatitude() {
		return latitude;
	}


	public PamTableItem getLongitude() {
		return longitude;
	}


	public PamTableItem getProbability() {
		return probability;
	}


	public PamTableItem getnDegreesFreedom() {
		return nDegreesFreedom;
	}


	public PamTableItem getAic() {
		return aic;
	}


	public PamTableItem getAmbiguity() {
		return ambiguity;
	}
	
	public void setLocTime(PamTableItem locTime) {
		this.locTime = locTime;
	}



	public void setAlgorithmName(PamTableItem algorithmName) {
		this.algorithmName = algorithmName;
	}


	public void setLocX(PamTableItem locX) {
		this.locX = locX;
	}


	public void setLocY(PamTableItem locY) {
		this.locY = locY;
	}


	public void setLocZ(PamTableItem locZ) {
		this.locZ = locZ;
	}


	public void setRange(PamTableItem range) {
		this.range = range;
	}


	public void setxErr(PamTableItem xErr) {
		this.xErr = xErr;
	}


	public void setyErr(PamTableItem yErr) {
		this.yErr = yErr;
	}


	public void setzErr(PamTableItem zErr) {
		this.zErr = zErr;
	}


	public void setRangeErr(PamTableItem rangeErr) {
		this.rangeErr = rangeErr;
	}


	public void setChi(PamTableItem chi) {
		this.chi = chi;
	}


	public void setReferenceHydrophone(PamTableItem referenceHydrophone) {
		this.referenceHydrophone = referenceHydrophone;
	}


	public void setLatitude(PamTableItem latitude) {
		this.latitude = latitude;
	}


	public void setLongitude(PamTableItem longitude) {
		this.longitude = longitude;
	}


	public void setProbability(PamTableItem probability) {
		this.probability = probability;
	}


	public void setnDegreesFreedom(PamTableItem nDegreesFreedom) {
		this.nDegreesFreedom = nDegreesFreedom;
	}


	public void setAic(PamTableItem aic) {
		this.aic = aic;
	}


	public void setAmbiguity(PamTableItem ambiguity) {
		this.ambiguity = ambiguity;
	}
	

}
