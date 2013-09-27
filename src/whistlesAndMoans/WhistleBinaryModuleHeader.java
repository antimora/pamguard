package whistlesAndMoans;

import java.io.Serializable;

import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleHeader;

public class WhistleBinaryModuleHeader extends ModuleHeader implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public int delayScale = 1;

	public WhistleBinaryModuleHeader(int moduleVersion) {
		super(moduleVersion);
	}

	@Override
	public boolean createHeader(BinaryObjectData binaryObjectData,
			BinaryHeader binaryHeader) {			// TODO Auto-generated method stub
		return false;
	}
	
}
