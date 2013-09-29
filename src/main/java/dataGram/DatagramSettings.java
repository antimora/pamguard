package dataGram;

import java.io.Serializable;

public class DatagramSettings implements Serializable, Cloneable {


	public static final long serialVersionUID = 1L;

	public int datagramSeconds = 600;

	@Override
	public DatagramSettings clone() {
		try {
			return (DatagramSettings) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
