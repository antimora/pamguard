package networkTransfer.send;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;


import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamguardMVC.PamDataBlock;

public class NetworkSendDialog extends PamDialog {

	private static NetworkSendDialog singleInstance;

	private NetworkSendParams networkSendParams;

	private JTextField ipAddress, portNumber, userName;

	private JPasswordField password;

	private JCheckBox rememberPassword;

	private JButton testConnection;

	private NetworkSender networkSender;

	private DataPanel dataPanel;

	private QueuePanel queuePanel;

	private JTabbedPane tabbedPane;

	private NetworkSendDialog(Window parentFrame, NetworkSender networkSender) {
		super(parentFrame, "Network Sending", false);
		this.networkSender = networkSender;

		tabbedPane = new JTabbedPane();

		JPanel idPanel = new JPanel();
		idPanel.setBorder(new TitledBorder("Host details"));
		idPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		addComponent(idPanel, new JLabel("Host address ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(idPanel, ipAddress = new JTextField(20), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(idPanel, new JLabel("Port number ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(idPanel, portNumber = new JTextField(20), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(idPanel, new JLabel("User name ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(idPanel, userName = new JTextField(20), c);
		c.gridx = 0;
		c.gridy++;
		addComponent(idPanel, new JLabel("password ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(idPanel, password = new JPasswordField(20), c);
		c.gridx = 1;
		c.gridy++;
		addComponent(idPanel, rememberPassword = new JCheckBox("Remember Password"), c);
		c.gridx = 1;
		c.gridy++;
		c.gridwidth = 1;
		addComponent(idPanel, testConnection = new JButton("Test connection"), c);
		testConnection.addActionListener(new TestConnection());

		tabbedPane.add("Connection", idPanel);

		queuePanel = new QueuePanel();
		tabbedPane.add("Queue", queuePanel);

		dataPanel = new DataPanel();
		tabbedPane.add("Data Sources", dataPanel);

		setDialogComponent(tabbedPane);

		setResizable(true);
	}

	public static NetworkSendParams showDialog(Window frame, NetworkSender networkSender, NetworkSendParams networkSendParams) {
		if (singleInstance == null || singleInstance.getOwner() != frame) {
			singleInstance = new NetworkSendDialog(frame, networkSender);
		}
		singleInstance.networkSendParams = networkSendParams.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.networkSendParams;
	}

	private void setParams() {
		ipAddress.setText(networkSendParams.ipAddress);
		portNumber.setText(String.format("%d",networkSendParams.portNumber));
		userName.setText(networkSendParams.userId);
		password.setText(networkSendParams.password);
		rememberPassword.setSelected(networkSendParams.savePassword);

		queuePanel.setParams();
		dataPanel.setParams();
		tabbedPane.invalidate();
	}

	@Override
	public void cancelButtonPressed() {
		networkSendParams = null;
	}

	@Override
	public boolean getParams() {
		networkSendParams.ipAddress = ipAddress.getText();
		networkSendParams.userId = userName.getText();
		networkSendParams.password = new String(password.getPassword());
		try {
			networkSendParams.portNumber = Integer.valueOf(portNumber.getText());
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid port address: " + portNumber.getText());
		}
		return (dataPanel.getParams() && queuePanel.getParams());
	}

	private class TestConnection implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			testConnection();
		}
	}
	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

	public void testConnection() {
		showWarning("Test function not yet implemented");
	}

	private class DataPanel extends JPanel {

		private JCheckBox[] checkBoxes;
		private ArrayList<PamDataBlock> possibles;
		private JPanel streamPanel;
		private JTextField stationId1, stationId2;

		public DataPanel() {
			super();
//			setBorder(new TitledBorder("Data"));
//			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setLayout(new BorderLayout());
			
			JPanel idPanelO = new JPanel(new BorderLayout());
			JPanel idPanel = new JPanel();
			add(BorderLayout.NORTH, idPanelO);
			idPanelO.setBorder(new TitledBorder("Station id"));
			idPanel.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();
			addComponent(idPanel, new JLabel("Station Id 1"), c);
			c.gridx++;
			addComponent(idPanel, stationId1 = new JTextField(4), c);
			c.gridx = 0;
			c.gridy++;
			addComponent(idPanel, new JLabel("Station Id 2"), c);
			c.gridx++;
			idPanelO.add(BorderLayout.WEST, idPanel);
			addComponent(idPanel, stationId2 = new JTextField(4), c);
			
			
			
			streamPanel = new JPanel();
			streamPanel.setBorder(new TitledBorder("Output Streams"));
			streamPanel.setLayout(new BoxLayout(streamPanel, BoxLayout.Y_AXIS));
			add(BorderLayout.CENTER, streamPanel);
		}

		public void setParams() {
			stationId1.setText(String.format("%d", networkSendParams.stationId1));
			stationId2.setText(String.format("%d", networkSendParams.stationId2));
			
			streamPanel.removeAll();
			possibles = networkSender.listPossibleDataSources();
			if (possibles == null) {
				return;
			}
			checkBoxes = new JCheckBox[possibles.size()];
			int i = 0;
			for (PamDataBlock aBlock:possibles) {
				checkBoxes[i] = new JCheckBox(aBlock.getDataName());
				streamPanel.add(checkBoxes[i]);
				if (networkSendParams.findDataBlock(aBlock) != null) {
					checkBoxes[i].setSelected(true);
				}
				i++;
			}
		}

		public boolean getParams() {
			try {
				networkSendParams.stationId1 = Integer.valueOf(stationId1.getText());
				networkSendParams.stationId2 = Integer.valueOf(stationId2.getText());
			}
			catch (NumberFormatException e) {
				return showWarning("Invalid satation id");
			}
			
			networkSendParams.clearDataBlocks();
			if (checkBoxes == null) {
				return true;
			}
			for (int i = 0; i < checkBoxes.length; i++) {
				if (checkBoxes[i].isSelected()) {
					networkSendParams.setDataBlock(possibles.get(i), true);
				}
			}
			return true;
		}

	}

	private class QueuePanel extends JPanel {

		JTextField queueSize, queueLength;
		public QueuePanel() {
			setBorder(new TitledBorder("Max Queue Size"));
			setLayout(new BorderLayout());
			JPanel inny = new JPanel();
			add(BorderLayout.NORTH, inny);
			inny.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();
			addComponent(inny, new JLabel("Max Queue Size ", JLabel.RIGHT), c);
			c.gridx++;
			addComponent(inny, queueSize = new JTextField(5), c);
			c.gridx++;
			addComponent(inny, new JLabel(" kilobytes"), c);
			c.gridx = 0;
			c.gridy++;
			addComponent(inny, new JLabel("Max Queue Length ", JLabel.RIGHT), c);
			c.gridx++;
			addComponent(inny, queueLength = new JTextField(5), c);
			c.gridx++;
			addComponent(inny, new JLabel(" objects"), c);
			
		}

		public void setParams() {
			queueLength.setText(String.format("%d", networkSendParams.maxQueuedObjects));
			queueSize.setText(String.format("%d", networkSendParams.maxQueueSize));
		}

		public boolean getParams() {
			try {
				networkSendParams.maxQueuedObjects = Integer.valueOf(queueLength.getText());
				networkSendParams.maxQueueSize = Integer.valueOf(queueSize.getText());
			}
			catch (NumberFormatException e) {
				return showWarning("Invalid queue size or length parameter");
			}
			return true;
		}

	}
}
