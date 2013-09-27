package clickDetector;

import Localiser.bearingLocaliser.GroupDetection;

public class ClickGroupDetection extends GroupDetection<ClickDetection> {

	public ClickGroupDetection(ClickDetection firstClick) {
		super(firstClick);

		setEventId(firstClick.getEventId());
	}

}
