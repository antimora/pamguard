package nmeaEmulator;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import serialComms.SerialPortCom;

import NMEA.NMEAParameters;

public class SerialOutput {

	private NMEAParameters serialOutputParameters = new NMEAParameters();

	public NMEAParameters getSerialOutputParameters() {
		return serialOutputParameters;
	}

	public void setSerialOutputParameters(NMEAParameters serialOutputParameters) {
		this.serialOutputParameters = serialOutputParameters;
	}

	private SerialOutputComm serialOutputComm;

	private CommPortIdentifier portId;
	
	private BufferedWriter serialWriter;

	private SerialPort serialPort;
	
	public SerialOutput(String unitName) {
				
//		openSerialPort();
	}
	
	
	public boolean openSerialPort() {
		closeSerialPort();

		if (serialOutputParameters.serialPortName == null) {
			System.out.println("No COM Port selected");
			return false;
		}
		
		ArrayList<CommPortIdentifier> commPortIds = SerialPortCom.getPortArrayList();
		boolean portFound = false;
		for (int i = 0; i < commPortIds.size(); i++) {
			portId = commPortIds.get(i);
			if (portId.getName().equals(serialOutputParameters.serialPortName)) {
				portFound = true;
				break;
			}
		}

		if (portId == null) {
			System.out.println("Unable to locate com port " + serialOutputParameters.serialPortName);
			return false;
		}
		String currentOwner = portId.getCurrentOwner();
		if (currentOwner != null) {
			System.out.println("Port " + serialOutputParameters.serialPortName + " is in use by " + currentOwner);
			return false;
		}
		
		try {			
			serialPort = (SerialPort)  portId.open(serialOutputParameters.serialPortName, 200);
		}
		catch (PortInUseException e) {
			e.printStackTrace();
			return false;
		}
		
		try {
			serialPort.setSerialPortParams(serialOutputParameters.serialPortBitsPerSecond, SerialPort.DATABITS_8, 
					SerialPort.STOPBITS_1, 
					SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e1) {
			e1.printStackTrace();
			return false;
		}
		
		try {
			serialWriter = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream(), "ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		System.out.println(String.format("Serial port %s open for writing at %d bps",
				serialOutputParameters.serialPortName, serialOutputParameters.serialPortBitsPerSecond));
		return true;
	}
	
	public void closeSerialPort() {
		if (serialWriter != null) {
			try {
				serialWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			serialWriter = null;
		}
		if (serialPort != null) {
			serialPort.close();
			serialPort = null;
		}
		portId = null;
	}
	
	/*
	 * Send a new string to the serial port. 
	 * A return and line feed will be added automatically
	 */
	public synchronized boolean sendSerialString(String string) {
		if (serialWriter == null) {
			return false;
		}
		try {
			serialWriter.write(string);
			serialWriter.write('\r');
			serialWriter.write('\n');
			serialWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	class SerialOutputComm implements SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
//	class SerialOutFileMenuAction implements ActionListener {
//		SerialOutput serialOutput;
//		JFrame parentFrame;
//		public SerialOutFileMenuAction(SerialOutput serialOutput,
//				JFrame parentFrame) {
//			this.serialOutput = serialOutput;
//			this.parentFrame = parentFrame;
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			SerialOutputParameters newParams = SerialOutputdialog.showDialog(parentFrame, serialOutputParameters);
//			if (newParams != null) {
//				serialOutputParameters = newParams.clone();
//				openSerialPort();
//			}
//		}
//	}

}
