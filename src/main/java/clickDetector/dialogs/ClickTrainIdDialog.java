package clickDetector.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import PamView.PamDialog;
import clickDetector.ClickParameters;

public class ClickTrainIdDialog extends PamDialog {

	static ClickTrainIdDialog singleInstance;
	
	ClickParameters clickParameters;
	
	JCheckBox runClickTrainId;
	
	private ClickTrainIdDialog(Frame parentFrame) {

		super(parentFrame, "Click Train Identification", true);

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setLayout(new BorderLayout());

		// put in  a tab control
		JTabbedPane tabbedPane = new JTabbedPane();
		//tabbedPane.addTab("Id Parameters", new BTPanel());
		
		//JPanel g = new JPanel();
		runClickTrainId = new JCheckBox("Run Click Train Id");
		//g.add(runClickTrainId);	
		tabbedPane.add("Options", runClickTrainId);
		
		setDialogComponent(tabbedPane);
	}
	
	static public ClickParameters showDialog(Frame parentFrame, ClickParameters newParameters) {
		
		
		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
			singleInstance = new ClickTrainIdDialog(parentFrame);
		}
		
		singleInstance.clickParameters = newParameters.clone();
		
		singleInstance.setParams(newParameters);
		
		singleInstance.setVisible(true);
		
		return singleInstance.clickParameters;
	}


	@Override
	public void cancelButtonPressed() {
		clickParameters = null;
	}
	
	private void setParams(ClickParameters newParameters) {
		runClickTrainId.setSelected(clickParameters.runClickTrainId);
	}

	@Override
	public void restoreDefaultSettings() {
		setParams(new ClickParameters());
	}


	@Override
	public boolean getParams(){
		clickParameters.runClickTrainId = runClickTrainId.isSelected();
		return true;
	}
}
