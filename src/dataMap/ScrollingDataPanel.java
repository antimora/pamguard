package dataMap;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.border.EmptyBorder;

import Layout.PamAxis;
import PamController.OfflineDataStore;
import PamController.PamController;
import PamUtils.PamCalendar;
import PamView.PamBorderPanel;
import PamView.PamColors;
import PamguardMVC.PamDataBlock;

public class ScrollingDataPanel extends PamBorderPanel {

	/**
	 * Main panel returned to the DataMapPanel 
	 */
//	private OuterPanel panel;
	
	/**
	 * Scrolled panel which will contain the actual DataStreamPanels
	 */
	private JPanel scrolledPanel;
	
//	private JScrollPane scrollPane;
	private JPanel scrollPanelContainer;
	
	private DataMapControl dataMapControl;
	
	protected DataMapPanel dataMapPanel;
	
	private JScrollBar hScrollBar;
	
//	private JScrollBar vScrollBar;
	
	private ArrayList<DataStreamPanel> dataStreamPanels = new ArrayList<DataStreamPanel>();
	
	private SettingsStrip settingsStrip;
	
	/**
	 * List of offline data sources. 
	 */
	ArrayList<OfflineDataStore> offlineDataStores;

	private long screenStartMillis = -1;

	private long screenEndMillis = -1;

	private double screenSeconds;

	private BoxLayout boxLayout;
	
	private DataMapLayout dataMapLayout;
	
	public ScrollingDataPanel(DataMapControl dataMapControl, DataMapPanel dataMapPanel) {
		this.dataMapControl = dataMapControl;
		this.dataMapPanel = dataMapPanel;
//		panel = new JPanel(new BorderLayout());
		this.setLayout(new BorderLayout());
		settingsStrip = new SettingsStrip(this);
		scrolledPanel = new JPanel();
//		scrolledPanel.setLayout(boxLayout = new BoxLayout(scrolledPanel, BoxLayout.Y_AXIS));
		scrolledPanel.setLayout(dataMapLayout = new DataMapLayout(this));
//		scrolledPanel.setLayout(new GridLayout(1,1));
		scrollPanelContainer = new JPanel(new BorderLayout());
		hScrollBar = new JScrollBar(Adjustable.HORIZONTAL);
		hScrollBar.addAdjustmentListener(new HScrollListener());
		scrollPanelContainer.add(BorderLayout.CENTER, scrolledPanel);
		scrollPanelContainer.add(BorderLayout.SOUTH, hScrollBar);
		scrollPanelContainer.setBorder(BorderFactory.createBevelBorder(1));
//		scrollPane = new JScrollPane(scrolledPanel);
//		panel.setBackground(Color.GREEN);
//		scrolledPanel.setBackground(Color.BLUE);
//		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.add(BorderLayout.CENTER, scrollPanelContainer);
		setPanelInsets();
		setupScrollBar();
	}
	
	private int lastHScaleChoice = -1;
	public void scaleChange() {
		if (lastHScaleChoice != dataMapControl.dataMapParameters.hScaleChoice) {
			lastHScaleChoice = dataMapControl.dataMapParameters.hScaleChoice;
			setupScrollBar();
		}
		scrolledPanel.repaint();
		scrollPanelContainer.repaint();
		repaint();
	}

	private int lastWidth = 0;
	public void frameResized() {
		if (getWidth() != lastWidth) {
			setupScrollBar();
			lastWidth = getWidth();
		}
		invalidate();
		repaintAll();
	}
	
	public void repaintAll() {
		for (int i = 0; i < dataStreamPanels.size(); i++) {
			dataStreamPanels.get(i).getPanel().repaint();
		}
		scrolledPanel.repaint();
		scrollPanelContainer.repaint();
		repaint();
	}

	public void showHideGraph() {
		DataStreamPanel dsp;
//		frameResized();
		scrolledPanel.invalidate();
		scrollPanelContainer.invalidate();
		invalidate();
		scrolledPanel.repaint();
		scrollPanelContainer.repaint();
		repaint();
	}

	public void newDataSources() {
		offlineDataStores = PamController.getInstance().findOfflineDataStores();
		createDataGraphs();
		setupScrollBar();		
	}
	
	public double getPixelsPerHour() {
		return dataMapControl.dataMapParameters.getPixeslPerHour();
	}

	public void setupScrollBar() {
		/**
		 * Do scrolling in seconds - will give up to 68 years with a 
		 * 32 bit integer control of scroll bar. milliseconds would give < 1 year !
		 */
		int currentPos = hScrollBar.getValue();
		long dataStart = dataMapControl.getFirstTime();
		long dataEnd = dataMapControl.getLastTime();
		int dataSeconds = (int) ((dataEnd-dataStart)/1000) + 1;
		double pixsPerHour = getPixelsPerHour(); 
		double pixsPerSecond = pixsPerHour / 3600;
		int screenWidth = scrolledPanel.getWidth();
		screenSeconds = screenWidth / pixsPerSecond;
		if (dataStart == Long.MAX_VALUE || screenSeconds >= dataSeconds) {
			/* 
			 * hide the scroll bar and stretch the display to fit the window 
			 */
			hScrollBar.setVisible(false);
			screenStartMillis = dataStart;
			screenEndMillis = dataEnd;
		}
		else {
			hScrollBar.setVisible(true);
			hScrollBar.setMaximum(0);
			hScrollBar.setMaximum((int) Math.ceil(dataSeconds));
			hScrollBar.setBlockIncrement((int) Math.max(1, screenSeconds * 4/5));
			hScrollBar.setUnitIncrement((int) Math.max(1, screenSeconds / 20));
			hScrollBar.setVisibleAmount((int) screenSeconds);
			hScrollBar.setValue(currentPos);
		}
		repaint();
	}
	
	class HScrollListener implements AdjustmentListener {

		@Override
		public void adjustmentValueChanged(AdjustmentEvent arg0) {
			screenStartMillis = dataMapControl.getFirstTime() + 
			hScrollBar.getValue() * 1000L;
			screenEndMillis = screenStartMillis + (long) (screenSeconds * 1000);
						
			repaint();
			notifyScrollChange();
		}
		
	}

	public void notifyScrollChange() {
		// tell all panalettes to repaint. 
		for (int i = 0; i < dataStreamPanels.size(); i++) {
			dataStreamPanels.get(i).scrollChanged();
		}
		settingsStrip.scrollChanged();
	}

	private void setPanelInsets() {
		Insets panelInsets = new Insets(10, 70, 30, 10);
		this.setBorder(new EmptyBorder(panelInsets));
		this.invalidate();
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#print(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Insets insets = getInsets();
		if (screenStartMillis < 0) {
			return;
		}
		FontMetrics fm = g.getFontMetrics();
		String str;
		int y = getHeight() - insets.bottom + fm.getHeight();
		str = PamCalendar.formatDateTime(screenStartMillis);
		g.drawString(str, insets.left, y);
		str = PamCalendar.formatDateTime(screenEndMillis);
		g.drawString(str, getWidth() - insets.right - fm.stringWidth(str), 
				y);
		
		Point thisPt = this.getLocationOnScreen();
		Point thatPt;
		int dspX, dspY, h;
		// paint a label next to the settings strip.
		if (dataMapLayout.getSettingsStrip() != null) {
			thatPt = settingsStrip.getLocationOnScreen();
			dspX = thatPt.x-thisPt.x;
			dspY = thatPt.y-thisPt.y;
			dspY += settingsStrip.getStripHeight()/2;
			dspY += fm.getHeight()/2;
			g.drawString("Settings", dspX-fm.stringWidth("Settings "), dspY);
		}
		
		// now paint the y Axis for each of the components. 
		PamAxis yAxis;
		DataStreamPanel dsp;
		for (int i = 0; i < dataStreamPanels.size(); i++) {
			dsp = dataStreamPanels.get(i);
			if (dsp.isGraphVisible() == false) {
				continue;
			}
			thatPt = dsp.getDataGraph().getLocationOnScreen();
			dspX = thatPt.x-thisPt.x;
			dspY = thatPt.y-thisPt.y;
			h = dsp.getDataGraph().getHeight();
			if (h <= 2) {
				continue;
			}
			yAxis = new PamAxis(0, 1, 0, 1, 0, 0, PamAxis.ABOVE_LEFT, null, 0, "%d");
			yAxis.setLogScale(dataMapControl.dataMapParameters.vLogScale);
			yAxis.setLogScale(dsp.isLogScale());
			yAxis.setMinVal(dsp.getYScaleMin());
			yAxis.setMaxVal(dsp.getYScaleMax());
			yAxis.setLabel(dsp.getScaleUnits());
			yAxis.setLabelPos(PamAxis.LABEL_NEAR_CENTRE);
			yAxis.drawAxis(g, dspX, dspY+h, dspX, dspY);
			dsp.setAxis(yAxis);
		}
		
	}

	public JPanel getPanel() {
		return this;
	}

	public SettingsStrip getSettingsStrip() {
		return settingsStrip;
	}

	/**
	 * @return the dataMapPanel
	 */
	public DataMapPanel getDataMapPanel() {
		return dataMapPanel;
	}

	protected Color getDataStreamColour(OfflineDataStore dataSource) {
		if (offlineDataStores == null) {
			return Color.DARK_GRAY;
		}
		int ind = offlineDataStores.indexOf(dataSource);
		if (ind < 0) {
			return Color.GREEN;
		}
		return PamColors.getInstance().getChannelColor(ind);
	}
	
	/**
	 * @return the dataMapControl
	 */
	public DataMapControl getDataMapControl() {
		return dataMapControl;
	}

	/**
	 * Create the data graphs to go into the panel. 
	 * @return number created. 
	 */
	public int createDataGraphs() {
		scrolledPanel.removeAll();
		dataStreamPanels.clear();
		ArrayList<PamDataBlock> dataBlocks = dataMapControl.getMappedDataBlocks();
		if (dataBlocks == null) {
			return 0;
		}
		DataStreamPanel aStreamPanel;
		scrolledPanel.add(settingsStrip);
		dataMapLayout.addLayoutComponent("", settingsStrip);
		for (int i = 0; i < dataBlocks.size(); i++) {
			aStreamPanel = new DataStreamPanel(dataMapControl, this, dataBlocks.get(i));
			dataMapLayout.addLayoutComponent("", aStreamPanel);
			dataStreamPanels.add(aStreamPanel);
			scrolledPanel.add(aStreamPanel.getPanel());
		}
		scrolledPanel.invalidate();
		return dataBlocks.size();
	}
	

	/**
	 * @return the screenStartMillis
	 */
	public long getScreenStartMillis() {
		return screenStartMillis;
	}

	/**
	 * @return the screenEndMillis
	 */
	public long getScreenEndMillis() {
		return screenEndMillis;
	}

	/**
	 * @return the screenSeconds
	 */
	public double getScreenSeconds() {
		return screenSeconds;
	}

	public void scrollToData(PamDataBlock dataBlock) {
		long startTime = dataBlock.getCurrentViewDataStart();
		int val = (int) ((startTime - getScreenStartMillis())/1000 - getScreenSeconds()/5)  ;
		hScrollBar.setValue(val);
	}
}
