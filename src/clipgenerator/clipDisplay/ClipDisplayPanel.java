package clipgenerator.clipDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.io.Serializable;
import java.util.ListIterator;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamCalendar;
import PamView.ColourArray;
import PamView.PamPanel;
import PamView.ColourArray.ColourArrayType;
import PamView.hidingpanel.HidingPanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import Spectrogram.DirectDrawProjector;

import clipgenerator.ClipControl;
import clipgenerator.ClipDataUnit;

/**
 * Clip display panel. Can be incorporated into a tab panel or stand alone in 
 * a general display. 
 * @author Doug Gillespie
 *
 */
public class ClipDisplayPanel implements PamSettings {

	private ClipControl clipControl;

	private JPanel displayPanel;

	private JPanel unitsPanel;

	protected ClipDisplayParameters clipDisplayParameters = new ClipDisplayParameters();

	private DisplayControlPanel displayControlPanel;

	private float sampleRate;

	private ColourArray colourArray;

	protected Font clipFont;

	private JScrollPane scrollPane;

	private ClipLayout clipLayout;

	private ColourArrayType currentColours = null;

	private ClipDataProjector clipDataProjector;

	private boolean isViewer;

	public ClipDisplayPanel(ClipControl clipControl) {
		super();
		this.clipControl = clipControl;

		clipFont = new Font("Arial", Font.PLAIN, 10);
		displayPanel = new ClipMainPanel(new BorderLayout());



		unitsPanel = new PamPanel(clipLayout = new ClipLayout(FlowLayout.LEFT));

		clipLayout.setHgap(2);
		clipLayout.setVgap(2);
		//		unitsPanel.setPreferredSize(new Dimension(200, 0));
		//		unitsPanel.setBackground(Color.blue);
		scrollPane = new JScrollPane(unitsPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//		scrollPane.get
		displayPanel.add(BorderLayout.CENTER, scrollPane);

		displayControlPanel = new DisplayControlPanel(clipControl, this);
		HidingPanel hp = new HidingPanel(displayPanel, displayControlPanel.getControlPanel(), HidingPanel.HORIZONTAL, true);
		hp.setTitle("clip viewer controls");
		displayPanel.add(BorderLayout.NORTH, hp);

		clipDataProjector = new ClipDataProjector(this);

		makeColourArray();

		clipControl.getClipProcess().getClipDataBlock().addObserver(new ClipObserver());

		PamSettingManager.getInstance().registerSettings(this);
		displayControlPanel.setValues(clipDisplayParameters);

		isViewer = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;
	}

	private class ClipObserver implements PamObserver {

		@Override
		public String getObserverName() {
			return "display panel";
		}

		@Override
		public PamObserver getObserverObject() {
			return this;
		}

		@Override
		public long getRequiredDataHistory(PamObservable o, Object arg) {
			return Math.max(10000, clipDisplayParameters.maxMinutes * 60L * 1000L);
		}

		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			// TODO Auto-generated method stub

		}

		@Override
		public void noteNewSettings() {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeObservable(PamObservable o) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setSampleRate(float sampleRate, boolean notify) {
			newSampleRate(sampleRate);
		}

		@Override
		public void update(PamObservable o, PamDataUnit arg) {
			newDataUnit((ClipDataUnit) arg);
		}

	}

	private void newDataUnit(ClipDataUnit clipDataUnit) {
		PamDataUnit triggerDataUnit = findTriggerDataUnit(clipDataUnit);
		ClipDisplayUnit clipDisplayUnit = new ClipDisplayUnit(this, clipDataUnit, triggerDataUnit);
		synchronized (unitsPanel.getTreeLock()) {
			unitsPanel.add(clipDisplayUnit, 0);
		}

		if (!isViewer) {
			removeOldClips();

			updatePanel();
		}
	}

	private void removeOldClips() {
		if (isViewer) {
			return;
		}
		synchronized (unitsPanel.getTreeLock()) {
			int compCount = unitsPanel.getComponentCount();
			ClipDisplayUnit clipDisplayUnit;
			long minTime = PamCalendar.getTimeInMillis() - clipDisplayParameters.maxMinutes * 60000L;
			for (int i = compCount-1; i >= 0; i--) {
				clipDisplayUnit = (ClipDisplayUnit) unitsPanel.getComponent(i);
				if (i > clipDisplayParameters.maxClips || clipDisplayUnit.getClipDataUnit().getTimeMilliseconds() < minTime) {
					unitsPanel.remove(i);
				}
			}
		}
	}

	//	public void newViewerDataLoaded() {
	//
	//	}

	private void removeAllClips() {
		unitsPanel.removeAll();
	}

	public void newViewerTimes(long start, long end) {
		removeAllClips();
		ListIterator<ClipDataUnit> it = clipControl.getClipProcess().getClipDataBlock().getListIterator(0);
		while (it.hasNext()) {
			newDataUnit(it.next());
		}
		updatePanel();
	}

	private PamDataUnit findTriggerDataUnit(ClipDataUnit clipDataUnit) {
		String trigName = clipDataUnit.triggerName;
		long trigMillis = clipDataUnit.triggerMilliseconds;
		long startMillis = clipDataUnit.getTimeMilliseconds();
		PamDataBlock<PamDataUnit> dataBlock = findTriggerDataBlock(trigName);
		if (dataBlock == null) {
			return null;
		}
		return dataBlock.findDataUnit(trigMillis, 0);
		//		System.out.println(String.format("Name %s, trigger %s, start %s", trigName, PamCalendar.formatTime(trigMillis, true),
		//				PamCalendar.formatTime(startMillis, true)));
		//		return null;
	}

	private String lastFoundName;
	private PamDataBlock<PamDataUnit> lastFoundBlock;
	private PamDataBlock<PamDataUnit> findTriggerDataBlock(String dataName) {
		if (dataName.equals(lastFoundName)) {
			return lastFoundBlock;
		}
		PamDataBlock<PamDataUnit> dataBlock = PamController.getInstance().getDetectorDataBlock(dataName);
		if (dataBlock == null) {
			return null;
		}
		lastFoundName = new String(dataName);
		lastFoundBlock = dataBlock;
		return dataBlock;
	}

	/**
	 * update the panel layout. 
	 */
	private void updatePanel() {
		int space = scrollPane.getWidth() - scrollPane.getVerticalScrollBar().getWidth();
		Insets ins = scrollPane.getInsets();
		if (ins != null) {
			space -= (ins.left + ins.right);
		}
		clipLayout.setPanelWidth(space);
		unitsPanel.invalidate();
		unitsPanel.repaint(100);

		//		System.out.println(String.format("Current display size = %d by %d",
		//				unitsPanel.getWidth(), unitsPanel.getHeight()));
	}

	private void updateAllDisplayUnits(boolean needNewImage) {
		synchronized (unitsPanel.getTreeLock()) {
			int n = unitsPanel.getComponentCount();
			ClipDisplayUnit displayUnit;
			for (int i = 0; i < n; i++) {
				displayUnit = (ClipDisplayUnit) unitsPanel.getComponent(i);
				displayUnit.redrawUnit(needNewImage);
			}
		}
		updatePanel();
	}

	private void newSampleRate(float sampleRate) {
		this.setSampleRate(sampleRate);
	}

	/**
	 * @return the displayPanel
	 */
	public JPanel getDisplayPanel() {
		return displayPanel;
	}

	/**
	 * @return the colourArray
	 */
	public ColourArray getColourArray() {
		return colourArray;
	}

	/**
	 * @param sampleRate the sampleRate to set
	 */
	public void setSampleRate(float sampleRate) {
		this.sampleRate = sampleRate;
		displayControlPanel.setValues(clipDisplayParameters);
	}

	/**
	 * @return the sampleRate
	 */
	public float getSampleRate() {
		return sampleRate;
	}

	class ClipMainPanel extends PamPanel {

		private int lastWidth;

		public ClipMainPanel(LayoutManager layout) {
			super(layout);
			// TODO Auto-generated constructor stub
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (getWidth() != lastWidth) {
				updatePanel();
				lastWidth = getWidth();
			}
		}

	}

	public void displayControlChanged(boolean needNewImage) {
		if (displayControlPanel == null) {
			return;
		}
		boolean ok = displayControlPanel.getValues(clipDisplayParameters);
		if (needNewImage) {
			makeColourArray();
		}
		updateAllDisplayUnits(needNewImage);
	}

	private void makeColourArray() {
		if (currentColours == clipDisplayParameters.getColourMap()) {
			return;
		}
		currentColours = clipDisplayParameters.getColourMap();

		colourArray = ColourArray.createStandardColourArray(256, currentColours);
	}

	/**
	 * @return the spectrogramProjector
	 */
	public ClipDataProjector getClipDataProjector() {
		return clipDataProjector;
	}

	@Override
	public Serializable getSettingsReference() {
		return clipDisplayParameters;
	}

	@Override
	public long getSettingsVersion() {
		return ClipDisplayParameters.serialVersionUID;
	}

	@Override
	public String getUnitName() {
		return clipControl.getUnitName();
	}

	@Override
	public String getUnitType() {
		return "Clip Display Panel";
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		this.clipDisplayParameters = ((ClipDisplayParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	/**
	 * @return the scrollPane
	 */
	protected JScrollPane getScrollPane() {
		return scrollPane;
	}

}
