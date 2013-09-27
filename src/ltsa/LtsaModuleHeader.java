package ltsa;

import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleHeader;

public class LtsaModuleHeader extends ModuleHeader {

	private static final long serialVersionUID = 1L;
	
	int fftLength;
	int ffthop;
	int intervalSeconds;
	/**
	 * @param fftLength
	 * @param ffthop
	 * @param intervalSeconds
	 */
	public LtsaModuleHeader(int version, int fftLength, int ffthop, int intervalSeconds) {
		super(version);
		this.fftLength = fftLength;
		this.ffthop = ffthop;
		this.intervalSeconds = intervalSeconds;
	}
	@Override
	public boolean createHeader(BinaryObjectData binaryObjectData,
			BinaryHeader binaryHeader) {
		// TODO Auto-generated method stub
		return false;
	}
}
