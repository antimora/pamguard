package portcomms;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;

/**
 * Subclasses CommPortOpen and adds send/expect handling for dealing
 * with Hayes-type modems.
 *
 */
public class CommPortModem extends OpenCommPort {
	
    /** The last line read from the serial port. */
    protected String rxData;
    
    /** A flag to control debugging output. */
    protected boolean debug = true;


    
    public CommPortModem(String portName)
    throws IOException, NoSuchPortException,PortInUseException,
        UnsupportedCommOperationException {
    super(portName);
}

    
    
    
    /** Send to the port
     * Send Car Return & Line Feed (\r\n), 
     * instead of using println(  ).
     */
    public boolean send(String txData) throws IOException {
        if (debug) {System.out.println(">" +txData);}
        if (outputStream == null) return false;
        outputStream.print(txData + "\r\n");
        return true;
    }

    
    
    
    /** Read from the port
     *  store response in rxData. 
     */
    public String readSerial() throws IOException {
    	if (thePort == null || inputStream == null) return null;
    	 System.out.println("recieve timeout :" + thePort.getReceiveTimeout());
    	 try{
    		 thePort.enableReceiveTimeout(10000); // milliseconds  
    	    //=================================== 
    	    rxData = inputStream.readLine();
    	    
    	    //===================================      
    	 } catch (Exception e) {
//    		 e.printStackTrace();
        	 System.out.println("recieve timeout :" + thePort.getReceiveTimeout());
    	 }
    	 

         if (debug){System.out.println("<"+ rxData);}
        
         return(rxData);
    }
       
}

					    