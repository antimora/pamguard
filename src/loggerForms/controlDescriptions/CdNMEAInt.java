/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.LoggerControl;
import loggerForms.controls.NMEAIntegerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdNMEAInt extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdNMEAInt(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.INTEGER;
	}

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new NMEAIntegerControl(this,loggerForm);
	}
}
