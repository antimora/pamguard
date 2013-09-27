package alarm;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import PamView.PamColors;
import PamView.PamPanel;
import PamView.PamSidePanel;
import PamView.PamColors.PamColor;

public class AlarmSidePanel implements PamSidePanel {

	private AlarmControl alarmControl;

	private PamPanel sidePanel;

	private TitledBorder border;

	private JProgressBar alarmProgress;

	private JLabel alarmMessage;

	public AlarmSidePanel(AlarmControl alarmControl) {
		this.alarmControl = alarmControl;
		sidePanel = new PamPanel(new BorderLayout());
		sidePanel.setBorder(border = new TitledBorder(alarmControl.getUnitName()));
		alarmProgress = new JProgressBar();
		sidePanel.add(BorderLayout.NORTH, alarmProgress);
		sidePanel.add(BorderLayout.SOUTH, alarmMessage = new JLabel(" "));
	}

	@Override
	public JComponent getPanel() {
		return sidePanel;
	}

	@Override
	public void rename(String newName) {
		border.setTitle(newName);
	}

	public void updateAlarmScore(double alarmCount) {
		int alarmPercent = (int) (alarmCount / 
				alarmControl.alarmParameters.getTriggerCount(AlarmParameters.COUNT_LEVELS-1) * 100);
		alarmProgress.setValue(alarmPercent);
		alarmMessage.setText(String.format("%3.1f/%3.1f", alarmCount,
				alarmControl.alarmParameters.getTriggerCount(AlarmParameters.COUNT_LEVELS-1)));
		//		System.out.println("Alarm score = " + alarmCount);
		int state = alarmControl.getAlarmStatus();
		if (state == 0) {
			sidePanel.setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
		}
		else {
			sidePanel.setBackground(AlarmParameters.alarmColours[state-1]);
		}
	}
	

}
