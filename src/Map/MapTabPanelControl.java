package Map;

import java.awt.Frame;
import java.awt.event.MouseAdapter;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import PamView.PamTabPanel;

public class MapTabPanelControl implements PamTabPanel  {

	SimpleMap simpleMap;
	
	MapController mapController;
	
	MapTabPanelControl(MapController mapController) {
		this.mapController = mapController;
		simpleMap = new SimpleMap(mapController);
	}
	
	public JMenu createMenu(Frame parentFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	public JComponent getPanel() {
		return simpleMap.getPanel();
	}

	public JToolBar getToolBar() {
		// TODO Auto-generated method stub
		return null;
	}

	public SimpleMap getSimpleMap() {
		return simpleMap;
	}

	/**
	 * @param mouseAdapter
	 */
	public void addMouseAdapterToMapPanel(MouseAdapter mouseAdapter) {
		// TODO Auto-generated method stub
		simpleMap.addMouseAdapterToMapPanel(mouseAdapter);
	}

	public void mapCanScroll(boolean b) {	
		simpleMap.mapCanScroll(b);
	}

	/**
	 * 
	 */
	public void refreshDetectorList() {
		// TODO Auto-generated method stub
		simpleMap.refreshDetectorList();
	}
	
}

