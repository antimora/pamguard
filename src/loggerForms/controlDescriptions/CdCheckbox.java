/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.CheckboxControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdCheckbox extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdCheckbox(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.BIT;
	}

	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new CheckboxControl(this,loggerForm);
	}

}
