package WorkshopDemo;

import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

import java.sql.Types;

import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class WorkshopSQLLogging extends SQLLogging {

	WorkshopController workshopController;
	
	PamTableDefinition tableDefinition;
	
	PamTableItem dateItem, durationItem, lowFreqItem, highFreqItem, energyItem, channelItem;
	
	public WorkshopSQLLogging(WorkshopController workshopController, PamDataBlock pamDataBlock) {
		// call the super constructor. 
		super(pamDataBlock);
		
		// hold a reference to the Controller. 
		this.workshopController = workshopController;
		
		// create the table definition. 
		tableDefinition = createTableDefinition();
	}
	
	public PamTableDefinition createTableDefinition() {
		setUpdatePolicy(SQLLogging.UPDATE_POLICY_WRITENEW);
		PamTableDefinition tableDef = new PamTableDefinition(workshopController.getUnitName(), getUpdatePolicy());
		
		PamTableItem tableItem;
		// add table items. 
//		PamTableItem dateItem, durationItem, lowFreqItem, highFreqItem, energyItem;
		tableDef.addTableItem(tableItem = new PamTableItem("GpsIndex", Types.INTEGER));
		// this first item will automatically pick up a cross reference from the GPS data block . 
		tableItem.setCrossReferenceItem("GpsData", "Id");
		
		tableDef.addTableItem(dateItem = new PamTableItem("SystemDate", Types.TIMESTAMP));
		tableDef.addTableItem(channelItem = new PamTableItem("Channel", Types.INTEGER));
		tableDef.addTableItem(durationItem = new PamTableItem("Duration", Types.DOUBLE));
		tableDef.addTableItem(lowFreqItem = new PamTableItem("lowFrequency", Types.DOUBLE));
		tableDef.addTableItem(highFreqItem = new PamTableItem("highFrequency", Types.DOUBLE));
		tableDef.addTableItem(energyItem = new PamTableItem("energyDB", Types.DOUBLE));
		tableDef.setUseCheatIndexing(true);

		setTableDefinition(tableDef);
		return tableDef;
	}

//	@Override
//	/**
//	 * This information will get used to automatically create an appropriate database
//	 * table and to generate SQL fetch and insert statements. 
//	 */
//	public PamTableDefinition getTableDefinition() {
//		/*
//		 * return the single instance of tableDefinition that was created in 
//		 * the constructor. This gets called quite often and we don't want 
//		 * to be creating a ne one every time. 
//		 */
//		return tableDefinition;
//	}

	@Override
	/*
	 * This gets called back from the database manager whenever a new dataunit is
	 * added to the datablock. All we have to do is set the data values for each 
	 * field and they will be inserted into the database. Note that we don't need 
	 * to set a value for the GpsIndex field since this will be cross referenced
	 * automatically. 
	 * If formats are incorrect, the SQL write statement is likely to fail !
	 */
	public void setTableData(PamDataUnit pamDataUnit) {
//		PamTableItem dateItem, durationItem, lowFreqItem, highFreqItem, energyItem;

		WorkshopDataUnit wdu = (WorkshopDataUnit) pamDataUnit;
		channelItem.setValue(PamUtils.getSingleChannel(wdu.getChannelBitmap()));
		dateItem.setValue(PamCalendar.getTimeStamp(wdu.getTimeMilliseconds()));
		durationItem.setValue((double) wdu.getDuration() / workshopController.workshopProcess.getSampleRate());
		lowFreqItem.setValue(wdu.getFrequency()[0]);
		highFreqItem.setValue(wdu.getFrequency()[1]);
		energyItem.setValue(wdu.getAmplitudeDB());

		
	}

}
