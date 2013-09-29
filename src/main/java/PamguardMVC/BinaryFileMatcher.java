package PamguardMVC;

import binaryFileStorage.DataUnitFileInformation;

/**
 * Match data based on binary file information.
 * <p>Must supply two parameters. First is binary file name
 * second is click number in file. 
 * @author Doug Gillespie
 *
 */
public class BinaryFileMatcher implements DataUnitMatcher {

	@Override
	public boolean match(PamDataUnit dataUnit, Object... criteria) {
		String fileName = (String) criteria[0];
		int clickNumber = (Integer) criteria[1];
		DataUnitFileInformation fileInfo = dataUnit.getDataUnitFileInformation();
		return (fileInfo.getShortFileName(fileName.length()).equals(fileName) &&
				clickNumber == fileInfo.getIndexInFile());
	}

}
