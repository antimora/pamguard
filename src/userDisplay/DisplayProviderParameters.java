package userDisplay;

import java.awt.Dimension;
import java.awt.Point;
import java.io.Serializable;

public class DisplayProviderParameters  implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;

	public String displayName;
	
	public Class providerClass;
	
	public Point location;
	 
	public Dimension size;


	public DisplayProviderParameters(Class providerClass, String displayName) {
		super();
		this.providerClass = providerClass;
		this.displayName = displayName;
	}



	@Override
	protected DisplayProviderParameters clone() {
		try {
			return (DisplayProviderParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
