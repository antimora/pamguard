package portcomms;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Open a serial port using Java Communications.
 *
 */
public class OpenCommPort {
	
	// These constants should be parameterised.
	// They are fine here for testing and development
	
    public static final int SERIALPORT_OPEN_TIMEOUTSECONDS = 30;
    public static final int BAUD = 9600; //9600;
    public static final int FLOWCONTROL_HARDWARE = SerialPort.FLOWCONTROL_RTSCTS_IN 
													& SerialPort.FLOWCONTROL_RTSCTS_OUT;
    public static final int FLOWCONTROL_XONXOFF = SerialPort.FLOWCONTROL_XONXOFF_IN 
    												& SerialPort.FLOWCONTROL_XONXOFF_OUT;
    public static final int FLOWCONTROL_NONE = SerialPort.FLOWCONTROL_NONE;
	
    public static final int[] BITS_PER_SECOND_LIST = {2400, 9600, 115200};
    
    /** The parent Frame, for the chooser. */
    protected Frame parent;
    /** The input stream */
    //protected DataInputStream inputStream;
    protected BufferedReader inputStream;    
    
    /** The output stream */
    protected PrintStream outputStream;
    /** The last line read from the serial port. */
    protected String response;
    /** A flag to control debugging output. */
    protected boolean debug = true;
    /** The chosen Port Identifier */
    CommPortIdentifier commPortID;
    /** The chosen Port itself */
    CommPort thePort;

    public static void main(String[] argv)
        throws IOException, NoSuchPortException, PortInUseException,
            UnsupportedCommOperationException {

        new OpenCommPort(null).converse(  );

        System.exit(0);
    }

    
    
    public String getCommPortName(Frame f)
    {   
        // Use the PortChooser from before. Pop up the JDialog.
        PortChooser chooser = new PortChooser(null);

        String portName = null;
        do {
            chooser.setVisible(true);
            
            // Dialog done. Get the port name.
            portName = chooser.getSelectedName();

            if (portName == null)
                System.out.println("No port selected. Try again.\n");
        } while (portName == null);
        
        return(portName);
    }
 		
    		
    /* Constructor */
    public OpenCommPort(String portName)
        throws IOException, NoSuchPortException, PortInUseException,
            UnsupportedCommOperationException {
        
        // Use the PortChooser from before. Pop up the JDialog.
        //PortChooser chooser = new PortChooser(null);

        if (portName == null) {
        	portName = getCommPortName(null);
        }
        
        commPortID = PortChooser.commPortNameToID(portName);

        if (commPortID == null) {
        	System.out.println(portName + " does not exist on the system");
        	return;
        }
        // Get the CommPortIdentifier.
        //commPortID = chooser.getSelectedIdentifier(  );

        // open the port.
        // This form of openPort takes an Application Name and a timeout.
        // 
        System.out.println("Trying to open " + commPortID.getName(  ) + "...");

        if(commPortID.getPortType() != CommPortIdentifier.PORT_SERIAL) 
        {
        	System.out.println("PORT TYPE IS NOT SERIAL : " + commPortID);
        }
        else 
        {
            thePort = commPortID.open("DarwinSys DataComm",
                SERIALPORT_OPEN_TIMEOUTSECONDS * 1000);
            
            // Create and initialise the port
            SerialPort serialPort = (SerialPort) thePort;

            serialPort.setSerialPortParams(BAUD, SerialPort.DATABITS_8,
            					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);         
            serialPort.setFlowControlMode(FLOWCONTROL_NONE);
        }

        // Get the input and output streams
        // Printers can be write-only
        try {
            inputStream = new BufferedReader(new InputStreamReader(thePort.getInputStream()));
          //  BufferedReader d = new BufferedReader(new InputStreamReader(in));
        } catch (IOException e) {
            System.err.println("Can't open input stream: write-only");
            inputStream = null;
        }
        outputStream = new PrintStream(thePort.getOutputStream( ), true);
    }

    /** This method will be overridden by non-trivial subclasses
     * to hold a conversation. 
     */
    protected void converse(  ) throws IOException {

        System.out.println("Ready to read and write port.");

        // Input/Output code not written -- must subclass.

        // Finally, clean up.
        if (inputStream != null)
            inputStream.close(  );
        outputStream.close(  );
        thePort.close();
    }
}

					