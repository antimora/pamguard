package networkTransfer.receive;

import java.io.Serializable;

public class NetworkReceiveParams implements Cloneable, Serializable {
	
	public static final int CHANNELS_RENUMBER = 1;
	public static final int CHANNELS_MAINTAIN = 2;

	public static final long serialVersionUID = 1L;
	
	public int receivePort = 8013;
	
	public int channelNumberOption = CHANNELS_RENUMBER;


	@Override
	protected NetworkReceiveParams clone() {
		try {
			return (NetworkReceiveParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
