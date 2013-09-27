/**
 * 
 */
package loggerForms.controlDescriptions;

import java.sql.Types;


import loggerForms.FormDescription;
import loggerForms.FormsTableItem;
import loggerForms.LatLongTime;
import loggerForms.LoggerForm;
import loggerForms.controls.LatLongControl;
import loggerForms.controls.LatLongTimeControl;
import loggerForms.controls.LoggerControl;
import PamUtils.LatLong;

/**
 * @author GrahamWeatherup
 *
 */
public class CdLatLongTime extends ControlDescription{
	/**
	 * @param formDescription
	 */
	public CdLatLongTime(FormDescription formDescription) {
		super(formDescription);
		
	}
	
	@Override
	public FormsTableItem[] getFormsTableItems() {
		if(formsTableItems==null){
			FormsTableItem[] tis={
			new FormsTableItem(this, getDbTitle()+"_LAT", Types.DOUBLE),
			new FormsTableItem(this, getDbTitle()+"_LON", Types.DOUBLE),
			new FormsTableItem(this, getDbTitle()+"_TIME", Types.TIMESTAMP)};
			formsTableItems=tis;
		}
		return formsTableItems;
		
	};
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.Cd#makeControl(loggerForms.LoggerForm)
	 */
	@Override
	public LoggerControl makeControl(LoggerForm loggerForm) {
		return new LatLongTimeControl(this, loggerForm);
	}
	
	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.ControlDescription#moveDataToTableItems(java.lang.Object)
	 */
	@Override
	public void moveDataToTableItems(Object data) {//LatLongTime
		
		if (data == null) {
			formsTableItems[0].setValue(null);
			formsTableItems[1].setValue(null);
			formsTableItems[2].setValue(null);
		}
		else {
			LatLongTime latLongTime = (LatLongTime) data;
			formsTableItems[0].setValue(new Double(latLongTime.getLatitude()));
			formsTableItems[1].setValue(new Double(latLongTime.getLongitude()));
			formsTableItems[2].setValue(new Long(latLongTime.getTimeInMillis()));
		}
	}
	
	

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.ControlDescription#moveDataFromTableItems()
	 */
	@Override
	public Object moveDataFromTableItems() {
		Double lat = (Double) formsTableItems[0].getValue();
		Double lon = (Double) formsTableItems[1].getValue();
		Long timeInMillis = (Long) formsTableItems[2].getValue();
		if (lat == null || lon == null || timeInMillis==null) {
			return null;
		}
		return new LatLongTime(lat, lon, timeInMillis) ;
	}

	/* (non-Javadoc)
	 * @see loggerForms.controlDescriptions.ControlDescription#getHint()
	 */
	@Override
	public String getHint() {
		if (super.getHint()==null||super.getHint().length()==0){
			return"<html>Press F1 to update";
		}else{
			return super.getHint();
		}
	}
	
	
}
