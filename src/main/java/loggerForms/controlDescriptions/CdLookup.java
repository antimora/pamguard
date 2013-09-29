/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;

import generalDatabase.EmptyTableDefinition;
import generalDatabase.lookupTables.LookUpTables;
import generalDatabase.lookupTables.LookupList;
import loggerForms.FormDescription;
import loggerForms.LoggerForm;
import loggerForms.controls.LoggerControl;
import loggerForms.controls.LookupControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdLookup extends ControlDescription {

	/**
	 * used for lookuptypes
	 */
	private LookupList lookupList;
	
	/**
	 * @param formDescription
	 */
	public CdLookup(FormDescription formDescription) {
		super(formDescription);
		primarySQLType=Types.CHAR;
	}
	
	
	/**
	 * @return the lookupList
	 */
	public LookupList getLookupList() {
		if (lookupList == null) {
			lookupList = LookUpTables.getLookUpTables().getLookupList(getTopic());
		}
		return lookupList;
	}



	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		
		
		return new LookupControl(this, loggerForm);
	}


	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#getHint()
	 */
	@Override
	public String getHint() {
		
		if (super.getHint()==null||EmptyTableDefinition.deblankString(super.getHint()).length()==0){
			return"Press F1 to update or right-click to input manually or paste from map";
		}else{
			return EmptyTableDefinition.deblankString(super.getHint());
		}
	}

}
