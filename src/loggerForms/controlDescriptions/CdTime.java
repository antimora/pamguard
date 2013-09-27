/**
 * 
 */
package loggerForms.controlDescriptions;

import generalDatabase.EmptyTableDefinition;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.LoggerControl;
import loggerForms.controls.TimeControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdTime extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdTime(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.TIME;
	}

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new TimeControl(this, loggerForm);
	}
	
	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#getHint()
	 */
	@Override
	public String getHint() {
		
		if (super.getHint()==null||EmptyTableDefinition.deblankString(super.getHint()).length()==0){
			return"<html>Press F1 to update time automatically";
		}else{
			return EmptyTableDefinition.deblankString(super.getHint());
		}
	}

}
