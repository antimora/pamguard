package alarm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;

import PamController.PamControlledUnit;
import PamController.PamController;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;

public class AlarmProcess extends PamProcess {

	private AlarmControl alarmControl;
	private AlarmDataBlock alarmDataBlock;
	private AlarmCounter alarmCounter;
	
	List<AlarmDataPoint> alarmPoints = new LinkedList<AlarmDataPoint>();
	private double alarmCount;
	private PamDataBlock dataSource;
	
	public AlarmProcess(AlarmControl alarmControl) {
		super(alarmControl, null, "Alarm Process");
		this.alarmControl = alarmControl;
		alarmDataBlock = new AlarmDataBlock(this);
		addOutputDataBlock(alarmDataBlock);
	}

	@Override
	public void pamStart() {
		setupAlarm();
	}

	@Override
	public void pamStop() {
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#masterClockUpdate(long, long)
	 */
	@Override
	public void masterClockUpdate(long timeMilliseconds, long sampleNumber) {
		super.masterClockUpdate(timeMilliseconds, sampleNumber);
		removeOldPoints(System.currentTimeMillis()-2000);
		alarmControl.updateAlarmScore(alarmCount);
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamProcess#updateData(PamguardMVC.PamObservable, PamguardMVC.PamDataUnit)
	 */
	@Override
	public synchronized void newData(PamObservable o, PamDataUnit dataUnit) {
		double score;
		removeOldPoints(dataUnit.getTimeMilliseconds());
		score = alarmCounter.getValue(alarmControl.alarmParameters.countType, dataUnit);

		if (score > 0) {
			alarmPoints.add(new AlarmDataPoint(dataUnit.getTimeMilliseconds(), score));
			alarmCount = alarmCounter.addCount(alarmCount, score, alarmControl.alarmParameters.countType);
		}
		
		alarmControl.updateAlarmScore(alarmCount);
	}


	public boolean setupAlarm() {
		dataSource = PamController.getInstance().getDetectorDataBlock(alarmControl.alarmParameters.dataSourceName);
		if (dataSource == null) {
			return false;
		}
		setParentDataBlock(dataSource, true);
		if (AlarmDataSource.class.isAssignableFrom(dataSource.getClass())) {
			alarmCounter = ((AlarmDataSource) dataSource).getAlarmCounter();
		}
		if (alarmCounter == null) {
			alarmCounter = new SimpleAlarmCounter();
		}
		resetCount();
		
		return true;
	}

	private synchronized void resetCount() {
		alarmPoints.clear();
		alarmCount = 0;
		alarmControl.updateAlarmScore(alarmCount);
	}
	
	private synchronized void removeOldPoints(long currentTime) {
		ListIterator<AlarmDataPoint> it = alarmPoints.listIterator();
		long minTime = currentTime - alarmControl.alarmParameters.countIntervalMillis;
		AlarmDataPoint adp;
		while (it.hasNext()) {
			adp = it.next();
			if (adp.timeMilliseconds < minTime) {
				it.remove();
				alarmCount = alarmCounter.subtractCount(alarmCount, adp.score, alarmControl.alarmParameters.countType);
			}
		}
	}

}
