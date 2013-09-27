/**
 * 
 */
package loggerForms.controlDescriptions;

import generalDatabase.EmptyTableDefinition;

import java.sql.Types;


import PamUtils.PamCalendar;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.LoggerControl;
import loggerForms.controls.TimestampControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdTimestamp extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdTimestamp(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.TIMESTAMP;
	}
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new TimestampControl(this, loggerForm);
		
	}
	
	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#getHint()
	 */
	@Override
	public String getHint() {
		
		if (super.getHint()==null||EmptyTableDefinition.deblankString(super.getHint()).length()==0){
			return"<html>Press F1 to update time and date automatically";
		}else{
			return EmptyTableDefinition.deblankString(super.getHint());
		}
	}
	
	
//	public void moveDataToTableItems(Object data) {
//		Long millis=(Long)data;
//		formsTableItems[0].setValue(PamCalendar.getTimeStamp(millis));
//	}
//	
//	/**
//	 * Get data from a database table item. 
//	 * @return data object - will be in a type suitable for that control. 
//	 */
//	public Object moveDataFromTableItems() {
//		java.sql.Timestamp ts= (java.sql.Timestamp) formsTableItems[0].getValue();
//		return new Long(PamCalendar.millisFromTimeStamp(ts));
//	}

}
