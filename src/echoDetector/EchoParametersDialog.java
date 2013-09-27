package echoDetector;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import clickDetector.ClickDetection;

import PamController.PamController;
import PamView.PamDialog;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;
/**
 * Allows user to change echo detector parameters. 
 * @author Brian Miller
 *
 */
public class EchoParametersDialog extends PamDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * Make the dialog a singleton - saves time recreating it 
	 * every time it's used and will also leave the same tab showing
	 * for multi tab dialogs (doesn't really make any difference
	 * for this simple dialog)
	 */
	static private EchoParametersDialog singleInstance;
	
	/*
	 * local copy of parameters
	 */
	EchoProcessParameters echoProcessParameters;
	
	/*
	 * source panel is a handy utility for listing available data sources. 
	 */
	SourcePanel sourcePanel;
	
	/*
	 * reference for data fields
	 *
	 */
	JTextField duration, minEchoTime, maxEchoTime, threshold;
	
	private EchoParametersDialog(Frame parentFrame) {
		super(parentFrame, "Echo demo parameters", true);

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
		sourcePanel = new SourcePanel(this, ClickDetection.class, true, true);
		JPanel sourceSubPanel = new JPanel();
		sourceSubPanel.setLayout(new BorderLayout());
		sourceSubPanel.setBorder(new TitledBorder("Click Detection Data source"));
		sourceSubPanel.add(BorderLayout.CENTER, sourcePanel.getPanel());
		mainPanel.add(BorderLayout.NORTH, sourceSubPanel);
		
		// make another panel for the rest of the parameters.
		JPanel echoPanel = new JPanel();
		echoPanel.setBorder(new TitledBorder("IPI parameters"));
		// use the gridbaglaoyt - it's the most flexible
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();
		echoPanel.setLayout(layout);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.ipadx = 3;
		constraints.gridx = 0;
		constraints.gridy = 0;
		
		addComponent(echoPanel, new JLabel("Echo duration"), constraints);
		constraints.gridx++;
		addComponent(echoPanel, duration = new JTextField(4), constraints);
		constraints.gridx++;
		addComponent(echoPanel, new JLabel(" samples"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		addComponent(echoPanel, new JLabel("Minimum echo delay"), constraints);
		constraints.gridx++;
		addComponent(echoPanel, minEchoTime = new JTextField(6), constraints);
		constraints.gridx++;
		addComponent(echoPanel, new JLabel(" ms"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		addComponent(echoPanel, new JLabel("Maximum echo delay"), constraints);
		constraints.gridx++;
		addComponent(echoPanel, maxEchoTime = new JTextField(6), constraints);
		constraints.gridx++;
		addComponent(echoPanel, new JLabel(" ms"), constraints);
		constraints.gridx = 0;
		constraints.gridy ++;
		/*addComponent(echoPanel, new JLabel("Detection Threshold"), constraints);
		constraints.gridx++;
		addComponent(echoPanel, threshold = new JTextField(4), constraints);
		constraints.gridx++;
		addComponent(echoPanel, new JLabel(" dB"), constraints);
		*/
		mainPanel.add(BorderLayout.CENTER, echoPanel);
		
		setDialogComponent(mainPanel);
	}
	
	public static EchoProcessParameters showDialog(Frame parentFrame, EchoProcessParameters echoProcessParameters) {
		if (singleInstance == null || singleInstance.getParent() != parentFrame) {
			singleInstance = new EchoParametersDialog(parentFrame);
		}
		singleInstance.echoProcessParameters = echoProcessParameters.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.echoProcessParameters;
	}

	public void setParams() {
		/* 
		 * set the parameters in the source list. 
		 * including the channel list and the actual data source. 
		 */
		sourcePanel.setChannelList(echoProcessParameters.channelList);
		ArrayList<PamDataBlock> echoSources = PamController.getInstance().getDetectorDataBlocks();
		sourcePanel.setSource(echoSources.get(echoProcessParameters.clickDetector));
		
		duration.setText(String.format("%d", echoProcessParameters.echoDuration));
		minEchoTime.setText(String.format("%d", echoProcessParameters.minEchoTime));
		maxEchoTime.setText(String.format("%d", echoProcessParameters.maxEchoTime));
		//threshold.setText(String.format("%.1f", echoProcessParameters.threshold));*/
	}

	@Override
	public void cancelButtonPressed() {
		echoProcessParameters = null;		
	}

	@Override
	/**
	 * return true if all parameters are OK, otherwise, return false. 
	 */
	public boolean getParams() {
		/*
		 * get the source parameters
		 */
		echoProcessParameters.clickDetector = sourcePanel.getSourceIndex();
		/*
		 * TODO: IPI detector uses only one channel, so modify the parameter
		 * dialog to only allow selection of one channel. 
		 */
		int channelList = sourcePanel.getChannelList();
		if (channelList == 0) {
			return false;
		}
			
		// will throw an exception if the number format of any of the parameters is invalid, 
		// so catch the exception and return false to prevent exit from the dialog. 
		try {
			echoProcessParameters.echoDuration = Integer.valueOf(duration.getText());
			echoProcessParameters.minEchoTime = Integer.valueOf(minEchoTime.getText());
			echoProcessParameters.maxEchoTime = Integer.valueOf(maxEchoTime.getText());
			//echoProcessParameters.threshold = Double.valueOf(threshold.getText());
		 
		}
		catch (NumberFormatException ex) {
			return false;
		}
		
		return true;
	}

	@Override
	public void restoreDefaultSettings() {

		echoProcessParameters = new EchoProcessParameters();
		setParams();
		
	}

}
