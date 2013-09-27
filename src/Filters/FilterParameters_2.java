package Filters;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * FilterPArameters for use when the filter is on it's own (within a FilterController)
 * rather than combined into some other detector. 
 */
public class FilterParameters_2 implements Serializable, Cloneable {

	public static final long serialVersionUID = 1;
	
	public String rawDataSource;
	
	public int channelBitmap;
	
	FilterParams filterParams = new FilterParams();
	
	@Override
	public FilterParameters_2 clone() {
		try{
//			filterParams = filterParams.clone();
			if (filterParams == null) filterParams = new FilterParams();
			FilterParameters_2 newParams = (FilterParameters_2) super.clone();
			newParams.filterParams = this.filterParams.clone();
			return newParams;
		}
		catch (CloneNotSupportedException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("ChannelMap", (new Integer(channelBitmap)).toString());
		return filterParams.fillXMLParameters(doc, paramsEl);
	}
}
