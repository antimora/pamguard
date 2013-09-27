package loggerForms;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;


import PamView.PamPanel;
import PamView.PamTabPanel;

public class FormsTabPanel implements PamTabPanel {
	
	private FormsControl formsControl;
	
	private JPanel mainPanel;
	
	private LoggerTabbedPane mainTabbedPane;

	public LoggerTabbedPane getMainTabbedPane() {
		return mainTabbedPane;
	}

	public FormsTabPanel(FormsControl formsControl) {
		super();
		this.formsControl = formsControl;
		mainPanel = new FormsPanel();
	}

	@Override
	public JMenu createMenu(Frame parentFrame) {
		// TODO Auto-generated method stub
		JMenu menu = new JMenu("");
		JMenuItem plotMenu = new JMenuItem( " plot options ...");
		menu.add(plotMenu);
		return menu;
	}

	@Override
	public JComponent getPanel() {
		return mainPanel;
	}

	@Override
	public JToolBar getToolBar() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void createForms() {
		int nForms = formsControl.getNumFormDescriptions();
		FormDescription fD;
		JComponent formComponent;
		
		for (int i = 0; i < nForms; i++) {
			fD = formsControl.getFormDescription(i);
			formComponent = fD.getTabComponent();
			if (formComponent != null) {
				mainTabbedPane.addTab(fD.getFormTabName(), null, formComponent, fD.getTabToolTip());
			}
		}
		
	}

	/**
	 * Set the name of a tab using the default name. These can change dynamically as subforms 
	 * are added and removed. 
	 * @param formDescription 
	 */
	public void setTabName(FormDescription formDescription) {
		setTabName(formDescription, null);
	}
	
	/**
	 * Set the name of a tab. These can change dynamically as subforms 
	 * are added and removed. 
	 * @param formDescription
	 * @param newName
	 */
	public void setTabName(FormDescription formDescription, String newName) {
		if (newName == null) {
			newName = formDescription.getFormTabName();
		}
		int ind = mainTabbedPane.findTabIndex(formDescription);
		if (ind > 0) {
			mainTabbedPane.setTitleAt(ind, newName);
		}
	}

	class FormsPanel extends PamPanel {

		public FormsPanel() {
			super();
			setLayout(new BorderLayout());
			
			add(BorderLayout.CENTER, mainTabbedPane = new LoggerTabbedPane(formsControl));
		}
		
	}

	/**
	 * Called when forms are to be regenerated. <p>
	 * will remove all tabbed panes from the display. 
	 */
	public void removeAllForms() {
		mainTabbedPane.removeAll();
	}

}
