package nmeaEmulator;

import java.io.Serializable;

public class NMEAEmulatorParams implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;
			
	public boolean repeat;

	@Override
	protected NMEAEmulatorParams clone() {
		try {
			return (NMEAEmulatorParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
