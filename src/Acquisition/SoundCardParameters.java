package Acquisition;

import java.io.Serializable;

/**
 * Used by SoundCardSystem
 * @author Doug Gillespie
 * @see Acquisition.SoundCardSystem
 *
 */
public class SoundCardParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 1;
	
	public int deviceNumber;
	
	
	@Override
	public SoundCardParameters clone() {
		try{
			return (SoundCardParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}
}
