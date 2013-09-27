package depthReadout;

import java.io.Serializable;

import mcc.MccJniInterface;

public class MccDepthParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 0;
		
	public int iBoard;
	
	public int range = MccJniInterface.BIP10VOLTS;
	
	MccSensorParameters[] mccSensorParameters;
	
	public MccSensorParameters getSensorParameters(int iSensor) {
		if (mccSensorParameters == null || iSensor >= mccSensorParameters.length) {
			return null;
		}
		return mccSensorParameters[iSensor];
	}
	
	public class MccSensorParameters implements Serializable, Cloneable {

		static public final long serialVersionUID = 0;
		
		public int iChan;
		
		/**
		 * Deth = scaleA * voltage + scaleB;
		 */		
		public double scaleA = 1;
		
		public double scaleB = 0;
		
	}

	@Override
	public MccDepthParameters clone() {
		try {
			return (MccDepthParameters) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
}
