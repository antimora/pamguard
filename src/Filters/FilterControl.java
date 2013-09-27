package Filters;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;

public class FilterControl extends PamControlledUnit implements PamSettings {

	FilterParameters_2 filterParams = new FilterParameters_2();
	
	FilterProcess filterProcess;
	
	public FilterControl(String unitName) {
		super("IIRF Filter", unitName);
		filterProcess = new FilterProcess(this);
		addPamProcess(filterProcess);
		PamSettingManager.getInstance().registerSettings(this);
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenu menu = new JMenu(getUnitName());
		JMenuItem menuItem;
		
		menuItem = new JMenuItem("Data source...");
		menuItem.addActionListener(new DataSourceAction(parentFrame));
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Filter Settings...");
		menuItem.addActionListener(new DetectionMenuAction(parentFrame));
		menu.add(menuItem);
		
		return menu;
	}

	class DataSourceAction implements ActionListener {

		Frame parentFrame;
		
		public DataSourceAction(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}

		public void actionPerformed(ActionEvent e) {

			FilterParameters_2 newParams = FilterDataSourceDialog.showDialog(filterParams, parentFrame, filterProcess.outputData);
			
			if (newParams != null) {
				filterParams = newParams.clone();
				filterProcess.setupProcess();
			}
		}
		
	}
	class DetectionMenuAction implements ActionListener {

		Frame parentFrame;
		
		public DetectionMenuAction(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}
		public void actionPerformed(ActionEvent e) {

			FilterParams newParams = FilterDialog.showDialog(parentFrame,
					filterParams.filterParams, filterProcess.getSampleRate());
			if (newParams != null) {
				filterParams.filterParams = newParams.clone();
				filterProcess.setupProcess();
			}
		}
		
	}
	
	public Serializable getSettingsReference() {
		return filterParams;
	}

	public long getSettingsVersion() {
		return FilterParams.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		filterParams = ((FilterParameters_2) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#fillXMLParameters(org.w3c.dom.Document, org.w3c.dom.Element)
	 */
	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		return filterParams.fillXMLParameters(doc, paramsEl);
	}

}
