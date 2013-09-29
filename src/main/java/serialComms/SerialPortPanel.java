package serialComms;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;

/**
 * Standard panel for entering serial port settings. Should be 
 * combined into larger dialog panels. 
 * @author Douglas Gillespie
 *
 */
public class SerialPortPanel extends Object {

	private JPanel panel;
	
	private JComboBox portList;
	
	private JComboBox baudList;
	
	private JComboBox bitsList;
	
	private JComboBox stopBitsList;
	
	private JComboBox parityList;
	
	private JComboBox flowControlList;
	
	private boolean canBaud = false, canBits = false, canStopBits = false, canParity = false, canFlowControl = false;
	
	private String title;
		
	int baudData[] = {110, 300, 1200, 2400, 4800, 9600, 
			19200, 38400, 57600, 115200, 230400, 460800, 921600}; 
	int dataBitData[] = {SerialPort.DATABITS_5, SerialPort.DATABITS_6, SerialPort.DATABITS_7, SerialPort.DATABITS_8};
	int stopBitData[] = {SerialPort.STOPBITS_1, SerialPort.STOPBITS_1_5, SerialPort.STOPBITS_2};
	String stopBitStrings[] = {"1", "1.5", "2"};
	int parityData[] = {SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN, SerialPort.PARITY_SPACE, SerialPort.PARITY_MARK};
	String parityStrings[] = {"PARITY NONE", "PARITY ODD", "PARITY EVEN", "PARITY SPACE", "PARITY MARK"};
	int flowData[] = {SerialPort.FLOWCONTROL_NONE, SerialPort.FLOWCONTROL_XONXOFF_OUT, SerialPort.FLOWCONTROL_XONXOFF_IN,
			SerialPort.FLOWCONTROL_RTSCTS_OUT, SerialPort.FLOWCONTROL_RTSCTS_IN};
	String flowStrings[] = {"FLOWCONTROL NONE","FLOWCONTROL_XONXOFF OUT","FLOWCONTROL XONXOFF IN","FLOWCONTROL RTSCTS OUT","FLOWCONTROL RTSCTS IN"};
	
	public SerialPortPanel() {
		createPanel();
	}
	
	public SerialPortPanel(String title) {
		this.title = title;
		createPanel();
	}

	public SerialPortPanel(String title, boolean canBaud, boolean canBits, boolean canStopBits, boolean canParity, boolean canFlowControl) {
		super();
		this.title = title;
		this.canBaud = canBaud;
		this.canBits = canBits;
		this.canStopBits = canStopBits;
		this.canParity = canParity;
		this.canFlowControl = canFlowControl;
		createPanel();
	}

	public JPanel getPanel() {
		return panel;
	}
	
	private void createPanel() {
		panel = new JPanel();
		if (title != null) {
			panel.setBorder(new TitledBorder(title));
		}
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(2,5,2,5);
		PamDialog.addComponent(panel, new JLabel("Port Id"), c);
		c.gridx = 1;
		PamDialog.addComponent(panel, portList = new JComboBox(), c);
		if (canBaud) {
			c.gridy++;
			c.gridx = 0;
			PamDialog.addComponent(panel, new JLabel("Bits per second (BAUD)"), c);
			c.gridx = 1;
			PamDialog.addComponent(panel, baudList = new JComboBox(), c);
		}
		if (canBits) {
			c.gridy++;
			c.gridx = 0;
			PamDialog.addComponent(panel, new JLabel("Data Bits"), c);
			c.gridx = 1;
			PamDialog.addComponent(panel, bitsList = new JComboBox(), c);
		}
		if (canParity) {
			c.gridy++;
			c.gridx = 0;
			PamDialog.addComponent(panel, new JLabel("Parity"), c);
			c.gridx = 1;
			PamDialog.addComponent(panel, parityList = new JComboBox(), c);
		}
		if (canStopBits) {
			c.gridy++;
			c.gridx = 0;
			PamDialog.addComponent(panel, new JLabel("Stop Bits"), c);
			c.gridx = 1;
			PamDialog.addComponent(panel, stopBitsList = new JComboBox(), c);
		}
		if (canFlowControl) {
			c.gridy++;
			c.gridx = 0;
			PamDialog.addComponent(panel, new JLabel("Flow Control"), c);
			c.gridx = 1;
			PamDialog.addComponent(panel, flowControlList = new JComboBox(), c);
		}
		
		fillLists();
	}
	
	private void fillLists() {
		if (portList != null) {
			ArrayList<CommPortIdentifier> portStrings = SerialPortCom.getPortArrayList();
			for (int i = 0; i < portStrings.size(); i++) {
				portList.addItem(portStrings.get(i).getName());
			}
		}
		if (baudList != null) {
			for (int i = 0; i < baudData.length; i++) {
				baudList.addItem(new Integer(baudData[i]));
			}
		}
		if (bitsList != null) {
			for (int i = 0; i < dataBitData.length; i++) {
				bitsList.addItem(dataBitData[i]);
			}
		}
		if (stopBitsList != null) {
			for (int i = 0; i < stopBitData.length; i++) {
				stopBitsList.addItem(stopBitStrings[i]);
			}
		}
		if (parityList != null) {
			for (int i = 0; i < parityStrings.length; i++) {
				parityList.addItem(parityStrings[i]);
			}
		}
		if (flowControlList != null) {
			for (int i = 0; i < flowStrings.length; i++) {
				flowControlList.addItem(flowStrings[i]);
			}
		}
	}

	public int getBaudRate() {
		return baudData[baudList.getSelectedIndex()];
	}

	public void setBaudRate(int baudRate) {
		for (int i = 0; i < baudData.length; i++) {
		  if (baudData[i] == baudRate) {
			  baudList.setSelectedIndex(i);
		  }
		}
	}

	public int getDataBits() {
		return dataBitData[bitsList.getSelectedIndex()];
	}

	public void setDataBits(int dataBits) {
		for (int i = 0; i < dataBitData.length; i++) {
			if (dataBitData[i] == dataBits) {
				bitsList.setSelectedIndex(i);
			}
		}
	}

	public int getFlowControl() {
		return flowData[flowControlList.getSelectedIndex()];
	}

	public void setFlowControl(int flowControl) {
		for (int i = 0; i < flowData.length; i++) {
			if (flowData[i] == flowControl) {
				flowControlList.setSelectedIndex(i);
			}
		}
	}

	public int getParity() {
		return parityData[parityList.getSelectedIndex()];
	}

	public void setParity(int parity) {
		for (int i = 0; i < parityData.length; i++) {
			if (parityData[i] == parity) {
				parityList.setSelectedIndex(i);
			}
		}
	}

	public String getPort() {
		return portList.getSelectedItem().toString();
	}

	public void setPort(String port) {
		for (int i = 0; i < portList.getItemCount(); i++) {
			if (port.equalsIgnoreCase(portList.getItemAt(i).toString())) {
				portList.setSelectedIndex(i);
			}
		}
	}

	public int getStopBits() {
		return stopBitData[stopBitsList.getSelectedIndex()];
	}

	public void setStopBits(int stopBits) {
		for (int i = 0; i < stopBitData.length; i++) {
			if (stopBitData[i] == stopBits) {
				stopBitsList.setSelectedIndex(i);
			}
		}
	}

	public JComboBox getBaudList() {
		return baudList;
	}

	public JComboBox getBitsList() {
		return bitsList;
	}

	public JComboBox getFlowControlList() {
		return flowControlList;
	}

	public JComboBox getParityList() {
		return parityList;
	}

	public JComboBox getPortList() {
		return portList;
	}

	public JComboBox getStopBitsList() {
		return stopBitsList;
	}
	
}
