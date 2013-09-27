package clickDetector;

import generalDatabase.PamTableItem;
import Localiser.bearingLocaliser.GroupDetection;
import Localiser.bearingLocaliser.GroupDetectionLogging;
import PamDetection.AbstractLocalisation;
import PamDetection.PamDetection;
import PamUtils.LatLong;
import PamguardMVC.PamDataBlock;

public class ClickGroupLogging extends GroupDetectionLogging {

	public ClickGroupLogging(PamDataBlock pamDataBlock, int updatePolicy) {
		super(pamDataBlock, updatePolicy);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean fillDataUnit(PamDetection pamDetection) {
		// TODO Auto-generated method stub
		boolean fillOk = super.fillDataUnit(pamDetection);
		GroupDetection groupDetection = (GroupDetection) pamDetection;
		Object data, data2;
		PamTableItem[] tableItems, tableItems2;
		
		tableItems = getBearing();
		if (tableItems != null) {
			for (int i = 0; i < tableItems.length; i++) {
				data = tableItems[i].getValue();
				if (data != null) {
					groupDetection.setBearing(i, ((Double) data));
					groupDetection.getLocalisation().addLocContents(AbstractLocalisation.HAS_BEARING);
				}
			}
		}

		tableItems = getRange();
		if (tableItems != null) {
			for (int i = 0; i < tableItems.length; i++) {
				data = tableItems[i].getValue();
				if (data != null) {
					groupDetection.setRange(i, (Double) data);
					groupDetection.getLocalisation().addLocContents(AbstractLocalisation.HAS_RANGE);
				}
			}
		}
		
		tableItems = getLatitude();
		tableItems2 = getLongitude();
		if (tableItems != null) {
			for (int i = 0; i < tableItems.length; i++) {
				data = tableItems[i].getValue();
				if (data != null) {
					data2 = tableItems2[i].getValue();
					groupDetection.setLatLong(i, new LatLong((Double) data, (Double) data2));
					groupDetection.getLocalisation().addLocContents(AbstractLocalisation.HAS_LATLONG);
				}
			}
			groupDetection.setNumLatLong(tableItems.length);
		}
			
		return fillOk;
	}

}
