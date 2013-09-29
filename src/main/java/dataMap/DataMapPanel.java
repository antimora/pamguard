package dataMap;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import PamView.PamBorderPanel;
import PamView.PamTabPanel;
/**
 * The main panel managed by DataMapControl
 * In reality, this does most of the actual controlling. 
 * @author Doug Gillespie
 *
 */
public class DataMapPanel extends PamBorderPanel implements PamTabPanel {
	
	private DataMapControl dataMapControl;

	private SummaryPanel summaryPanel;
	
	private ScalePanel scalePanel;
	
	protected ScrollingDataPanel scrollingDataPanel;

	private Dimension graphDimension;
	
	protected DataMapPanel(DataMapControl dataMapControl) {
		this.dataMapControl = dataMapControl;
		summaryPanel = new SummaryPanel(dataMapControl, this);
		scalePanel = new ScalePanel(dataMapControl, this);
		scrollingDataPanel = new ScrollingDataPanel(dataMapControl, this);
		JPanel northPanel = new PamBorderPanel(new BorderLayout());
		this.setLayout(new BorderLayout());
		northPanel.add(BorderLayout.CENTER, summaryPanel.getPanel());
		northPanel.add(BorderLayout.EAST, scalePanel);
		this.add(BorderLayout.NORTH, northPanel);
		this.add(BorderLayout.CENTER, scrollingDataPanel.getPanel());

		Insets panelInsets = new Insets(0, 70, 0, 10);
		northPanel.setBorder(new EmptyBorder(panelInsets));
	}
	
	/**
	 * Create a new set of data graphs to go into the panel. 
	 * @return
	 */
	public int createDataGraphs() {
		/**
		 * First check the limits of the database and binary stores. 
		 */
		setGraphDimensions();
		
		return scrollingDataPanel.createDataGraphs();
	}
	
	/**
	 * Based on the scale and on the total length of data
	 * work out how big the little panels need to be 
	 */
	private void setGraphDimensions() {
		long totalLength = dataMapControl.getLastTime() - dataMapControl.getFirstTime();
		graphDimension = new Dimension(2000, 100);
	}
	
	/**
	 * @return the graphDimension for use with the DataStreamPanels. 
	 */
	public Dimension getGraphDimension() {
		return graphDimension;
	}

	@Override
	public JMenu createMenu(Frame parentFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JComponent getPanel() {
		return this;
	}

	@Override
	public JToolBar getToolBar() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Called from ScalePanel when anything 
	 * to do with scaling changes. 
	 */
	public void scaleChanged() {
		if (scalePanel == null || scrollingDataPanel == null) {
			return;
		}
		scalePanel.getParams(dataMapControl.dataMapParameters);
		scrollingDataPanel.scaleChange();
	}

	public void frameResized() {
		scrollingDataPanel.frameResized();
	}

	public void newDataSources() {
		scrollingDataPanel.newDataSources();
		summaryPanel.newDataSources();
	}

	public void newSettings() {
		scalePanel.setParams(dataMapControl.dataMapParameters);
	}

	/**
	 * Called when mouse moves over a data graph to set time
	 * on scale Panel. Set null to clear cursor info on panel.
	 * @param timeMillis time in millis or null. 
	 */
	public void dataGraphMouseTime(Long timeMillis) {
		summaryPanel.setCursorTime(timeMillis);
	}

	public void repaintAll() {
		scrollingDataPanel.repaintAll();
	}
	
}
