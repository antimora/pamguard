/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.IntegerControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdInteger extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdInteger(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.INTEGER;
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		// TODO Auto-generated method stub
		return new IntegerControl(this,loggerForm);
	}
}
