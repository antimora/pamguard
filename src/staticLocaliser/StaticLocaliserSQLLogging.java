package staticLocaliser;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import generalDatabase.DBProcess;
import generalDatabase.PamTableDefinition;
import generalDatabase.SQLLogging;

public class StaticLocaliserSQLLogging extends SQLLogging {

	
	
	private StaticLocaliserTableDefinition table;

	protected StaticLocaliserSQLLogging(PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		
		table=new StaticLocaliserTableDefinition("StaticLocaliserData");
		dbControl.getDbProcess().checkTable(table);
		setTableDefinition(table);
		
		System.out.println("init Logging");
	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		StaticLocalisationResults staticLocaliserDataUnit=(StaticLocalisationResults) pamDataUnit;
		
		System.out.println("Set data for table: "+staticLocaliserDataUnit.getTimeMillis());
		System.out.println(table.findTableItem("locTime"));
		System.out.println(table.findTableItem("algorithmName"));
		
		table.findTableItem("algorithmName")	.setValue(staticLocaliserDataUnit.getAlgorithmName());
		table.findTableItem("locTime")	.setValue(Double.valueOf(staticLocaliserDataUnit.getTimeMillis().doubleValue()));
		table.findTableItem("locX")				.setValue(staticLocaliserDataUnit.getX());
		table.findTableItem("locY")				.setValue(staticLocaliserDataUnit.getY());
		table.findTableItem("locZ")				.setValue(staticLocaliserDataUnit.getZ());
		table.findTableItem("range")			.setValue(staticLocaliserDataUnit.getRange());
		table.findTableItem("xErr")				.setValue(staticLocaliserDataUnit.getXError());
		table.findTableItem("yErr")				.setValue(staticLocaliserDataUnit.getYError());
		table.findTableItem("zErr")				.setValue(staticLocaliserDataUnit.getZError());
		table.findTableItem("rangeErr")			.setValue(staticLocaliserDataUnit.getRangeError());
		table.findTableItem("chi")				.setValue(staticLocaliserDataUnit.getChi2());
		table.findTableItem("referenceHydrophone").setValue(staticLocaliserDataUnit.getReferenceHydrophone());
		table.findTableItem("latitude")			.setValue(/*(Double)staticLocaliserDataUnit.getLatLong().getLatitude()*/0.0);
		table.findTableItem("longitude")		.setValue(/*(Double)staticLocaliserDataUnit.getLatLong().getLongitude()*/0.0);
		table.findTableItem("probability")		.setValue(staticLocaliserDataUnit.getProbability());
		table.findTableItem("nDegreesFreedom")	.setValue(staticLocaliserDataUnit.getnDegreesFreedom());
		table.findTableItem("aic")				.setValue(staticLocaliserDataUnit.getAic());
		table.findTableItem("ambiguity")		.setValue((Integer)staticLocaliserDataUnit.getAmbiguity());
		table.findTableItem("timeDelay")		.setValue((Integer)staticLocaliserDataUnit.getTimeDelay());
		table.findTableItem("tDPossibilities")	.setValue((Integer)staticLocaliserDataUnit.getNTimeDelayPossibilities());
//
//		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
//		
//		DBProcess dbp = new DBProcess(dbControl);
//		dbp.
//		System.out.println("Done data for table");
	}

	


}
