package decimator;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import Acquisition.offlineFuncs.OfflineDAQDialogPanel;
import Acquisition.offlineFuncs.OfflineFileParameters;
import Filters.FilterDialog;
import Filters.FilterParams;
import PamController.PamController;
import PamDetection.RawDataUnit;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;
import PamView.SourcePanelMonitor;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamRawDataBlock;

public class DecimatorParamsDialog extends PamDialog {

	private static DecimatorParamsDialog singleInstance;
	
	private static DecimatorParams decimatorParams;
	
	private DecimatorControl decimatorControl;
	
	private JTextField newSampleRate;
	
	private SourcePanel sourcePanel;
	
	private JButton filterButton;
	
	private JLabel sourceSampleRate;
	
	private float sampleRate = 1;
	
	private Frame parentFrame;

	private boolean isViewer;

	private OfflineDAQDialogPanel offlineDAQDialogPanel;
	
	private DecimatorParamsDialog(Frame parentFrame, DecimatorControl decimatorControl) {
		
		super(parentFrame, "Decimator ...", false);
		
		this.parentFrame = parentFrame;
		this.decimatorControl = decimatorControl;
		
		JPanel mainPanel = new JPanel();
		
		GridBagConstraints constraints = new PamGridBagContraints();
//		constraints.insets = new Insets(2,2,2,2);
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		sourcePanel = new SourcePanel(this, "Input Data Source", RawDataUnit.class, true, true);
		sourcePanel.addSourcePanelMonitor(new SPMonitor());
		sourcePanel.addSelectionListener(new SPSelection());
		mainPanel.add(sourcePanel.getPanel());		
		
		JPanel decimatorPanel = new JPanel();
		decimatorPanel.setBorder(new TitledBorder("Decimator settings"));
		decimatorPanel.setLayout(new GridBagLayout());
		mainPanel.add(decimatorPanel);
		
		constraints.gridx = 0;
		addComponent(decimatorPanel, new JLabel("Source sample rate "), constraints);
		constraints.gridx++;
		addComponent(decimatorPanel, sourceSampleRate = new JLabel(" - Hz"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		addComponent(decimatorPanel, new JLabel("Output sample rate "), constraints);
		constraints.gridx ++;
		addComponent(decimatorPanel, newSampleRate = new JTextField(5), constraints);
		constraints.gridx ++;
		addComponent(decimatorPanel, new JLabel(" Hz"), constraints);
		constraints.gridy ++;
		constraints.gridx = 1;
		constraints.gridwidth = 2;
		addComponent(decimatorPanel, filterButton = new JButton("Filter settings"), constraints);
		filterButton.addActionListener(new FilterButton());


		isViewer = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;
		if (isViewer) {
			JTabbedPane tabbedPane = new JTabbedPane();
			offlineDAQDialogPanel = new OfflineDAQDialogPanel(decimatorControl, this);
			tabbedPane.add("Offline Files", offlineDAQDialogPanel.getComponent());
			tabbedPane.add("Runtime Settings", mainPanel);
			setDialogComponent(tabbedPane);
		}
		else {
			setDialogComponent(mainPanel);
		}
		
		setHelpPoint("sound_processing.decimatorHelp.docs.decimator_decimator");
	}
	
		
	private class SPSelection implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			newDataSource();
		}
	}
	
	private class SPMonitor implements SourcePanelMonitor {

		@Override
		public void channelSelectionChanged() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void selectFilters() {
		FilterParams newFP = FilterDialog.showDialog(parentFrame,
				decimatorParams.filterParams, sampleRate);
		if (newFP != null) {
			decimatorParams.filterParams = newFP.clone();
		}
	}
	
	class FilterButton implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			selectFilters();
		}
	}

	public static DecimatorParams showDialog(Frame parentFrame, DecimatorControl decimatorControl, DecimatorParams oldParams) {
		decimatorParams = oldParams.clone();
		if (singleInstance == null || singleInstance.getOwner() != parentFrame || singleInstance.decimatorControl != decimatorControl) {
			singleInstance = new DecimatorParamsDialog(parentFrame, decimatorControl);
		}
		singleInstance.decimatorControl = decimatorControl;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return decimatorParams;
	}
	
	private void setParams() {
		sourcePanel.excludeDataBlock(decimatorControl.decimatorProcess.getOutputDataBlock(0), true);
		sourcePanel.setSourceList();
		sourcePanel.setChannelList(decimatorParams.channelMap);
		PamRawDataBlock currentBlock = PamController.getInstance().getRawDataBlock(decimatorParams.rawDataSource);
		sourcePanel.setSource(currentBlock);
		newSampleRate.setText(String.format("%.1f", decimatorParams.newSampleRate));
		newDataSource();
		if (offlineDAQDialogPanel != null) {
			offlineDAQDialogPanel.setParams();
		}
	}
	
	private void newDataSource() {
		PamDataBlock block = sourcePanel.getSource();
		if (block != null) {
			sourceSampleRate.setText(String.format("%.1f Hz", 
					sampleRate = block.getSampleRate()));
		}
	}
	
	@Override
	public boolean getParams() {
		try {
			ArrayList<PamDataBlock> rawBlocks = PamController.getInstance().getRawDataBlocks();
			decimatorParams.rawDataSource =  sourcePanel.getSource().getDataName();
			decimatorParams.channelMap = sourcePanel.getChannelList();
			decimatorParams.newSampleRate = java.lang.Float.valueOf(newSampleRate.getText());
		}
		catch (Exception Ex) {
			return false;
		}
		
		if (decimatorParams.channelMap == 0) {
			return showWarning("You must select at least one channel for decimation");
		}

		if (offlineDAQDialogPanel != null) {
			OfflineFileParameters ofp = offlineDAQDialogPanel.getParams();
			if (ofp == null) {
				return false;
			}
			decimatorControl.getOfflineFileServer().setOfflineFileParameters(ofp);
		}
		
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		decimatorParams = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}

//	void addComponent(JPanel panel, Component p, GridBagConstraints constraints){
//		((GridBagLayout) panel.getLayout()).setConstraints(p, constraints);
//		panel.add(p);
//	}
}
