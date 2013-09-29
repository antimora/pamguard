package alarm;

import PamguardMVC.PamDataUnit;

public class SimpleAlarmCounter extends AlarmCounter {

	@Override
	public double getValue(int countType, PamDataUnit dataUnit) {
		return 1;
	}

}
