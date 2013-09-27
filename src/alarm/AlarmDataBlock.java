package alarm;

import PamguardMVC.PamDataBlock;

public class AlarmDataBlock extends PamDataBlock<AlarmDataUnit> {

	public AlarmDataBlock(AlarmProcess alarmProcess) {
		super(AlarmDataUnit.class, "Alarms", alarmProcess, 0);
		// TODO Auto-generated constructor stub
	}

}
