package clickDetector;

import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleHeader;

public class ClickBinaryModuleHeader extends ModuleHeader {

	public ClickBinaryModuleHeader(int moduleVersion) {
		super(moduleVersion);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean createHeader(BinaryObjectData binaryObjectData,
			BinaryHeader binaryHeader) {
		// TODO Auto-generated method stub
		return false;
	}

//	@Override
//	public byte[] getByteArray() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
