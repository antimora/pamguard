/**
 * 
 */
package loggerForms.controlDescriptions;


import generalDatabase.EmptyTableDefinition;

import java.sql.Types;

import PamUtils.LatLong;

import loggerForms.FormDescription;
import loggerForms.FormsTableItem;
import loggerForms.LoggerForm;
import loggerForms.controls.LatLongControl;
import loggerForms.controls.LoggerControl;

/**
 * @author GrahamWeatherup
 *
 */
public class CdLatLong extends ControlDescription {

	/**
	 * @param formDescription
	 */
	public CdLatLong(FormDescription formDescription) {
		super(formDescription);
		
	}
	
	@Override
	public FormsTableItem[] getFormsTableItems() {
		if(formsTableItems==null){
			FormsTableItem[] tis={
			new FormsTableItem(this, getDbTitle()+"_LAT", Types.DOUBLE),
			new FormsTableItem(this, getDbTitle()+"_LON", Types.DOUBLE)};
			formsTableItems=tis;
		}
		return formsTableItems;
		
	};
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new LatLongControl(this, loggerForm);
	}
	
	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.ControlDescription#moveDataToTableItems(java.lang.Object)
	 */
	@Override
	public void moveDataToTableItems(Object data) {
		if (data == null) {
			formsTableItems[0].setValue(null);
			formsTableItems[1].setValue(null);
		}
		else {
			LatLong latLong = (LatLong) data;
			formsTableItems[0].setValue(new Double(latLong.getLatitude()));
			formsTableItems[1].setValue(new Double(latLong.getLongitude()));
		}
	}
	
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.ControlDescription#moveDataFromTableItems()
	 */
	@Override
	public Object moveDataFromTableItems() {
		Double lat = (Double) formsTableItems[0].getValue();
		Double lon = (Double) formsTableItems[1].getValue();
		if (lat == null || lon == null) {
			return null;
		}
		return new LatLong(lat, lon);
	}

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.ControlDescription#getHint()
	 */
	@Override
	public String getHint() {
		if (super.getHint()==null||super.getHint().length()==0){
			return"<html>Press F1 to update, F4 to input or right-click to input manually or paste from map";
		}else{
			return super.getHint();
		}
	}

}
