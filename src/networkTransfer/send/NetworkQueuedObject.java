package networkTransfer.send;

public class NetworkQueuedObject {

	int buoyId1;
	int buoyId2;
	int dataType1;
	int dataType2;
	byte[] data;
	int dataLength;
	
	/**
	 * @param buoyId1
	 * @param buoyId2
	 * @param dataType1
	 * @param dataType2
	 * @param dataLength
	 * @param data
	 */
	public NetworkQueuedObject(int buoyId1, int buoyId2, int dataType1,
			int dataType2, byte[] data) {
		super();
		this.buoyId1 = buoyId1;
		this.buoyId2 = buoyId2;
		this.dataType1 = dataType1;
		this.dataType2 = dataType2;
		if (data != null) {
			this.data = data;
			dataLength = data.length;
		}
	}
	
	
}
