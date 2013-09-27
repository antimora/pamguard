/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.FloatControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdSingle extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdSingle(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.REAL;
	}
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new FloatControl(this,loggerForm);
	}

}
