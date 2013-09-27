package Localiser.bearingLocaliser;

import PamDetection.PamDetection;
import PamguardMVC.PamDataBlock;
import generalDatabase.PamDetectionLogging;

public class GroupDetectionLogging extends PamDetectionLogging {

	public GroupDetectionLogging(PamDataBlock pamDataBlock, int updatePolicy) {
		super(pamDataBlock, updatePolicy);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean fillDataUnit(PamDetection pamDetection) {
		boolean basicFillOk =  super.fillDataUnit(pamDetection);
		GroupDetection groupDetection = (GroupDetection) pamDetection;
		
		return basicFillOk;
	}

}
