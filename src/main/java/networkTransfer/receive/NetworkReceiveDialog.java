package networkTransfer.receive;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class NetworkReceiveDialog extends PamDialog {
	
	private static NetworkReceiveDialog singleInstance;
	
	private NetworkReceiver networkReceiver;
	
	private NetworkReceiveParams networkReceiveParams;
	
	private JTextField receivePort;
	
	private JRadioButton channelsRenumber, channelsMaintain;
	

	private NetworkReceiveDialog(Window parentFrame, NetworkReceiver networkReceiver) {
		super(parentFrame, networkReceiver.getUnitName(), true);
		this.networkReceiver = networkReceiver;
		this.networkReceiveParams = networkReceiver.getNetworkReceiveParams().clone();
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JPanel portPanel = new JPanel();
		portPanel.setBorder(new TitledBorder("Connection"));
		portPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		addComponent(portPanel, new JLabel("Recieve Port ", JLabel.RIGHT), c);
		c.gridx++;
		addComponent(portPanel, receivePort = new JTextField(5), c);
		mainPanel.add(portPanel);
		
		JPanel channelPanel = new JPanel();
		channelPanel.setBorder(new TitledBorder("Channel Numbering"));
		channelPanel.setLayout(new GridBagLayout());
		c = new PamGridBagContraints();
		ButtonGroup bg = new ButtonGroup();
		bg.add(channelsRenumber = new JRadioButton("Renumber channels"));
		bg.add(channelsMaintain = new JRadioButton("Maintain channel numbers"));
		channelsRenumber.setToolTipText("Channels will re renumbered 0, 1, 2, etc. a unique number being used for each sending station." +
				"\nThis is generally the best option when multiple stations are sending to this receiving station.");
		channelsMaintain.setToolTipText("Channels will not be renumbered. i.e. they will keep the numbers assigned by the sending station." +
				"\nThis is generally the best option when a single remote station is sending to this receiving station.");
		addComponent(channelPanel, channelsMaintain, c);
		c.gridy++;
		addComponent(channelPanel, channelsRenumber, c);
		mainPanel.add(channelPanel);
		
		setDialogComponent(mainPanel);
		
	}

	public static NetworkReceiveParams showDialog(Window parentFrame, NetworkReceiver networkReceiver) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame || 
				singleInstance.networkReceiver != networkReceiver) {
			singleInstance = new NetworkReceiveDialog(parentFrame, networkReceiver);
		}
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.networkReceiveParams;
	}
	
	private void setParams() {
		receivePort.setText(String.format("%d", networkReceiveParams.receivePort));
		channelsRenumber.setSelected(networkReceiveParams.channelNumberOption == NetworkReceiveParams.CHANNELS_RENUMBER);
		channelsMaintain.setSelected(networkReceiveParams.channelNumberOption == NetworkReceiveParams.CHANNELS_MAINTAIN);
	}

	@Override
	public void cancelButtonPressed() {
		networkReceiveParams = null;
	}

	@Override
	public boolean getParams() {
		try {
			networkReceiveParams.receivePort = Integer.valueOf(receivePort.getText());
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid port number");
		}
		if (channelsRenumber.isSelected()) {
			networkReceiveParams.channelNumberOption = NetworkReceiveParams.CHANNELS_RENUMBER;
		}
		else {
			networkReceiveParams.channelNumberOption = NetworkReceiveParams.CHANNELS_MAINTAIN;
		}
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		networkReceiveParams = new NetworkReceiveParams();
		setParams();
	}
	
}
