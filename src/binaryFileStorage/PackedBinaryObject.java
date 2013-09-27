package binaryFileStorage;

public class PackedBinaryObject {

	public byte[] data;
	
	public int objectId;

	/**
	 * @param objectId
	 * @param data
	 */
	public PackedBinaryObject(int objectId, byte[] data) {
		super();
		this.objectId = objectId;
		this.data = data;
	}
	
	
}
