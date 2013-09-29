package ipiDemo;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import echoDetector.EchoDataUnit;
import PamController.PamController;
import PamUtils.PamUtils;
import PamView.PamDialog;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;

/**
 * A dialog box used to control the IPI computation parameters. Mostly copied 
 * and pasted from the WorkShopProcessParametersDialog.
 * @author Brian Miller
 *
 */
public class IpiParametersDialog extends PamDialog {

	private static final long serialVersionUID = 1;

	/*
	 * Make the dialog a singleton - saves time recreating it every time it's
	 * used and will also leave the same tab showing for multi tab dialogs
	 * (doesn't really make any difference for this simple dialog)
	 */
	static private IpiParametersDialog singleInstance;

	/*
	 * local copy of parameters
	 */
	IpiProcessParameters ipiProcessParameters;

	/*
	 * source panel is a handy utility for listing available data sources.
	 */
	SourcePanel sourcePanel;

	/*
	 * reference for data fields
	 */
	JTextField duration, minIpiTime, maxIpiTime, peakWidthPercent,
			outputFileName;

	private IpiParametersDialog(Frame parentFrame) {
		super(parentFrame, "Ipi demo parameters", true);

		/*
		 * Use the Java layout manager to constructs nesting panels of all the
		 * parameters.
		 */
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		/*
		 * put a sourcePanel in the top of the dialog panel. need to put it in
		 * an inner panel in order to add a titled border (appearance is
		 * everything)
		 */
		sourcePanel = new SourcePanel(this, EchoDataUnit.class, true, true);
		JPanel sourceSubPanel = new JPanel();
		sourceSubPanel.setLayout(new BorderLayout());
		sourceSubPanel
				.setBorder(new TitledBorder("Echo Detection Data source"));
		sourceSubPanel.add(BorderLayout.CENTER, sourcePanel.getPanel());
		mainPanel.add(BorderLayout.NORTH, sourceSubPanel);

		// make another panel for the rest of the parameters.
		JPanel ipiPanel = new JPanel();
		ipiPanel.setBorder(new TitledBorder("IPI parameters"));
		// use the gridbaglaoyt - it's the most flexible
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();
		ipiPanel.setLayout(layout);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.ipadx = 3;
		constraints.gridx = 0;
		constraints.gridy = 0;

		addComponent(ipiPanel, new JLabel("IPI duration"), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, duration = new JTextField(4), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, new JLabel(" ms"), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		addComponent(ipiPanel, new JLabel("Minimum IPI delay"), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, minIpiTime = new JTextField(6), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, new JLabel(" ms"), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		addComponent(ipiPanel, new JLabel("Maximum IPI delay"), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, maxIpiTime = new JTextField(6), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, new JLabel(" ms"), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		addComponent(ipiPanel, new JLabel("IPI Peak Width"), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, peakWidthPercent = new JTextField(6),
				constraints);
		constraints.gridx++;
		addComponent(ipiPanel, new JLabel(" %"), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		addComponent(ipiPanel, new JLabel("Output File"), constraints);
		constraints.gridx++;
		addComponent(ipiPanel, outputFileName = new JTextField(15), constraints);
		// constraints.gridx++;
		// addComponent(ipiPanel, new JLabel(" "), constraints);

		mainPanel.add(BorderLayout.CENTER, ipiPanel);

		setDialogComponent(mainPanel);
	}

	public static IpiProcessParameters showDialog(Frame parentFrame,
			IpiProcessParameters ipiProcessParameters) {
		if (singleInstance == null || singleInstance.getParent() != parentFrame) {
			singleInstance = new IpiParametersDialog(parentFrame);
		}
		singleInstance.ipiProcessParameters = ipiProcessParameters.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.ipiProcessParameters;
	}

	public void setParams() {
		/*
		 * set the parameters in the source list. including the channel list and
		 * the actual data source.
		 */
		sourcePanel.setChannelList(ipiProcessParameters.channel);
		ArrayList<PamDataBlock> ipiSources = PamController.getInstance()
				.getDetectorDataBlocks();
		sourcePanel.setSource(ipiSources
				.get(ipiProcessParameters.echoDataBlock));
		sourcePanel.setChannelList(ipiProcessParameters.channel);

		duration.setText(String.format("%3.0f",
				ipiProcessParameters.ipiDuration));
		minIpiTime.setText(String.format("%3.0f",
				ipiProcessParameters.minIpiTime));
		maxIpiTime.setText(String.format("%3.0f",
				ipiProcessParameters.maxIpiTime));
		peakWidthPercent.setText(String.format("%2.0f",
				ipiProcessParameters.ipiPeakWidthPercent));
		outputFileName.setText(ipiProcessParameters.outputFileName);
		// threshold.setText(String.format("%.1f",
		// ipiProcessParameters.threshold));
	}

	@Override
	public void cancelButtonPressed() {
		ipiProcessParameters = null;
	}

	@Override
	/**
	 * return true if all parameters are OK, otherwise, return false. 
	 */
	public boolean getParams() {
		/*
		 * get the source parameters
		 */
		ipiProcessParameters.echoDataBlock = sourcePanel.getSourceIndex();
		/*
		 * TODO: IPI detector uses only one channel, so modify the parameter
		 * dialog to only allow selection of one channel.
		 */
		int channelList = sourcePanel.getChannelList();
		if (channelList == 0) {
			return false;
		}
		// For now just pick the lowest selected ;
		ipiProcessParameters.channel = PamUtils.getLowestChannel(channelList);

		// will throw an exception if the number format of any of the parameters
		// is invalid,
		// so catch the exception and return false to prevent exit from the
		// dialog.
		try {
			ipiProcessParameters.ipiDuration = Double.valueOf(duration
					.getText());
			ipiProcessParameters.minIpiTime = Double.valueOf(minIpiTime
					.getText());
			ipiProcessParameters.maxIpiTime = Double.valueOf(maxIpiTime
					.getText());
			ipiProcessParameters.ipiPeakWidthPercent = Double
					.valueOf(peakWidthPercent.getText());
			ipiProcessParameters.outputFileName = String.valueOf(outputFileName
					.getText());
			// ipiProcessParameters.threshold =
			// Double.valueOf(threshold.getText());

		} catch (NumberFormatException ex) {
			return false;
		}

		return true;
	}

	@Override
	public void restoreDefaultSettings() {

		ipiProcessParameters = new IpiProcessParameters();
		setParams();

	}

}
