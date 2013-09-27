package loggerForms;

import java.io.Serializable;

/**
 * Manage a bit of static data for a single Logger form description. 
 * @author Doug Gillespie
 *
 */
public class FormSettings implements Cloneable, Serializable{

	public static final long serialVersionUID = 1L;

	public Integer splitPanelPosition;

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected FormSettings clone() {
		try {
			return (FormSettings) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
