/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import loggerForms.FormDescription;
import loggerForms.FormsTableItem;
import loggerForms.LoggerForm;
import loggerForms.controls.CharAreaControl;
import loggerForms.controls.CharControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdChar extends ControlDescription {

//	protected static String primarySQLType="";//Types.CHAR;
	
	/**
	 * @param formDescription
	 */
	public CdChar(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.CHAR;
		
		// TODO Auto-generated constructor stub
	}

	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
//		return new CharControl(this,loggerForm);
		return new CharAreaControl(this,loggerForm);
	}

	
	
	/**
	 * 
	 * @return data in Double/LatLong type.etc
	 */
	public Object getFormData(){
		return null;
	}
	
	public String getTableData(){
		return null;
	}

}
