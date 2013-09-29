package userDisplay;

import java.io.Serializable;
import java.util.ArrayList;

import Spectrogram.SpectrogramParameters;

import radardisplay.RadarParameters;

public class UserDisplayParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 11;

	ArrayList<SpectrogramParameters> spectrogramParameters;
	
	ArrayList<RadarParameters> radarParameters;
	
	ArrayList<DisplayProviderParameters> displayProviderParameters;
	
	@Override
	public UserDisplayParameters clone() {
		try {
			return (UserDisplayParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}
	
}
