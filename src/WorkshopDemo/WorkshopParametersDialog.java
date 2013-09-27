package WorkshopDemo;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import fftManager.FFTDataUnit;

import PamController.PamController;
import PamView.PamDialog;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;

public class WorkshopParametersDialog extends PamDialog {

	/*
	 * Make the dialog a singleton - saves time recreating it 
	 * every time it's used and will also leave the same tab showing
	 * for multi tab dialogs (doesn't really make any difference
	 * for this simple dialog)
	 */
	static private WorkshopParametersDialog singleInstance;
	
	/*
	 * local copy of parameters
	 */
	WorkshopProcessParameters workshopProcessParameters;
	
	/*
	 * source panel is a handy utility for listing available data sources. 
	 */
	SourcePanel sourcePanel;
	
	/*
	 * reference for data fields
	 *
	 */
	JTextField background, lowFreq, highFreq, threshold;
	
	private WorkshopParametersDialog(Frame parentFrame) {
		super(parentFrame, "Workshop demo parameters", true);

		/*
		 * Use the Java layout manager to constructs nesting panels 
		 * of all the parameters. 
		 */
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		/* 
		 * put a sourcePanel in the top of the dialog panel. 
		 * need to put it in an inner panel in order to add 
		 * a titled border (appearance is everything)
		 */
		sourcePanel = new SourcePanel(this, FFTDataUnit.class, true, true);
		JPanel sourceSubPanel = new JPanel();
		sourceSubPanel.setLayout(new BorderLayout());
		sourceSubPanel.setBorder(new TitledBorder("FFT Data source"));
		sourceSubPanel.add(BorderLayout.CENTER, sourcePanel.getPanel());
		mainPanel.add(BorderLayout.NORTH, sourceSubPanel);
		
		// make another panel for the rest of the parameters.
		JPanel detPanel = new JPanel();
		detPanel.setBorder(new TitledBorder("Detection parameters"));
		// use the gridbaglaoyt - it's the most flexible
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();
		detPanel.setLayout(layout);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.ipadx = 3;
		constraints.gridx = 0;
		constraints.gridy = 0;
		addComponent(detPanel, new JLabel("Background smoothing Constant"), constraints);
		constraints.gridx++;
		addComponent(detPanel, background = new JTextField(4), constraints);
		constraints.gridx++;
		addComponent(detPanel, new JLabel(" s"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		addComponent(detPanel, new JLabel("low Frequency"), constraints);
		constraints.gridx++;
		addComponent(detPanel, lowFreq = new JTextField(6), constraints);
		constraints.gridx++;
		addComponent(detPanel, new JLabel(" Hz"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		addComponent(detPanel, new JLabel("high Frequency"), constraints);
		constraints.gridx++;
		addComponent(detPanel, highFreq = new JTextField(6), constraints);
		constraints.gridx++;
		addComponent(detPanel, new JLabel(" Hz"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		addComponent(detPanel, new JLabel("Detection Threshold"), constraints);
		constraints.gridx++;
		addComponent(detPanel, threshold = new JTextField(4), constraints);
		constraints.gridx++;
		addComponent(detPanel, new JLabel(" dB"), constraints);
		
		mainPanel.add(BorderLayout.CENTER, detPanel);
		
		setDialogComponent(mainPanel);
	}
	
	public static WorkshopProcessParameters showDialog(Frame parentFrame, WorkshopProcessParameters workshopProcessParameters) {
		if (singleInstance == null || singleInstance.getParent() != parentFrame) {
			singleInstance = new WorkshopParametersDialog(parentFrame);
		}
		singleInstance.workshopProcessParameters = workshopProcessParameters.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.workshopProcessParameters;
	}

	public void setParams() {
		/* 
		 * set the parameters in the source list. 
		 * including the channel list and the actual data source. 
		 */
		sourcePanel.setChannelList(workshopProcessParameters.channelList);
		ArrayList<PamDataBlock> fftSources = PamController.getInstance().getFFTDataBlocks();
		sourcePanel.setSource(fftSources.get(workshopProcessParameters.fftDataBlock));
		
		background.setText(String.format("%.1f", workshopProcessParameters.backgroundTimeConstant));
		lowFreq.setText(String.format("%d", workshopProcessParameters.lowFreq));
		highFreq.setText(String.format("%d", workshopProcessParameters.highFreq));
		threshold.setText(String.format("%.1f", workshopProcessParameters.threshold));
	}

	@Override
	public void cancelButtonPressed() {
		workshopProcessParameters = null;		
	}

	@Override
	/**
	 * return true if all parameters are OK, otherwise, return false. 
	 */
	public boolean getParams() {
		/*
		 * get the source parameters
		 */
		workshopProcessParameters.fftDataBlock = sourcePanel.getSourceIndex();
		workshopProcessParameters.channelList = sourcePanel.getChannelList();
		if (workshopProcessParameters.channelList == 0) {
			return false;
		}
		// will throw an exception if the number format of any of the parameters is invalid, 
		// so catch the exception and return false to prevent exit from the dialog. 
		try {
			workshopProcessParameters.backgroundTimeConstant = Double.valueOf(background.getText());
			workshopProcessParameters.lowFreq = Integer.valueOf(lowFreq.getText());
			workshopProcessParameters.highFreq = Integer.valueOf(highFreq.getText());
			workshopProcessParameters.threshold = Double.valueOf(threshold.getText());
		}
		catch (NumberFormatException ex) {
			return false;
		}
		
		return true;
	}

	@Override
	public void restoreDefaultSettings() {

		workshopProcessParameters = new WorkshopProcessParameters();
		setParams();
		
	}

}
