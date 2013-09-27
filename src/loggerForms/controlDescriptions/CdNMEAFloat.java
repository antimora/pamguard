/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.LoggerControl;
import loggerForms.controls.NMEAFloatControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdNMEAFloat extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdNMEAFloat(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.REAL;
	}
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new NMEAFloatControl(this,loggerForm);
	}

}
