/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.DoubleControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdDouble extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdDouble(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.DOUBLE;
	}

	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		// TODO Auto-generated method stub
		return new DoubleControl(this,loggerForm);
	}

}
