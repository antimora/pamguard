package loggerForms;

/**
 * 
 * @author Graham Weatherup
 *
 */
public class PropertyDescription extends ItemDescription implements Cloneable{

	public static enum propertyTypes {
		AUTOALERT,AUTORECORD,BEARING,BLOBSIZE,FONT,DBTABLENAME,
		/*DDEENABLE,*/FORMCOLOUR,FORMCOLOR,GPSDATASET,HEADING,HIDDEN,
		HOTKEY,NOCLEAR,NOCANCEL,NOTOFFLINE,NOTONLINE,NOTSHOWDATA,
		POPUP,PLOT,RANGE,READONTIMER,READONGPS,READONNMEA,SUBFORM,SUBTABS,
		SYMBOLTYPE};
	
	private propertyTypes eType;
	
	public PropertyDescription(FormDescription formDescription) {
		// TODO Auto-generated constructor stub
		super(formDescription);
		eType = propertyTypes.valueOf(getType());

	}
	
//	private bearingTypes bType;
//	public static enum rangeUnitTypes{m,km,nmi}
//	public static enum rangeTypes{FIXED,}
//	public static enum headingUnitTypes{m,km,nmi,pix}
//	private bearingTypes hUType;
	
//	public static enum headingTypes{MAGNETIC,TRUE,RELATIVESHIP,RELATIVEBEARING}
//	private bearingTypes hType;
	
	public static enum hotkeys{/*F1,F2,F3,F4,*/F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,F17,F18,F19,F20,F21,F22,F23,F24}
	
	/**
	 * @return the property Type
	 */
	public propertyTypes getPropertyType() {
		return eType;
	}

	public static boolean isProperty(String type){		
		propertyTypes c = null;
		try {
			c = propertyTypes.valueOf(type);
		}
		catch (IllegalArgumentException e) {
		}
		return (c != null);
	}

	/**
	 * 
	 */
	@Override
	public String getItemWarning() {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected PropertyDescription clone(){
		
		try {
			return (PropertyDescription)super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
		
}
