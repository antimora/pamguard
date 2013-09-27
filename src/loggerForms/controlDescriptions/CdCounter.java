/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.CounterControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdCounter extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdCounter(FormDescription formDescription) {
		super(formDescription);
		if(getLength()==null||getLength()==0){
			setLength(4);
		}
		primarySQLType=Types.CHAR;
	}


	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new CounterControl(this, loggerForm);
	}

	

}
