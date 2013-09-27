/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.LoggerControl;
import loggerForms.controls.NMEACharControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdNMEAChar extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdNMEAChar(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.CHAR;
	}

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new NMEACharControl(this, loggerForm);
	}

}
