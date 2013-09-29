package WILDInterface;

import GPS.GPSDataBlock;
import java.io.File;
import java.io.Serializable;

public class WILDParameters implements Serializable, Cloneable {

	static public final long serialVersionUID = 1;

    /**
     * Serial port name
     */
	private String serialPortName = "";

    /**
     * The GPS data source.  This is not serializable, so it is marked transient.
     * Unfortunately this means the user must re-select the GPS source every
     * time the program is run
     */
    private transient GPSDataBlock gpsSource;

    /** output filename */
    private File outputFilename = new File("C:\\WILDOutput.csv");

    /** boolean indicating whether or not we're saving the data */
    private boolean saveOutput = false;

    /**
     * default baud rate for WILD interface
     */
    private int baud = 9600;
		
	@Override
	public WILDParameters clone() {
		try {
			return (WILDParameters) super.clone();
		}
		catch (CloneNotSupportedException Ex) {
			Ex.printStackTrace();
		}
		return null;
	}

    public GPSDataBlock getGpsSource() {
        return gpsSource;
    }

    public void setGpsSource(GPSDataBlock gpsSource) {
        this.gpsSource = gpsSource;
    }

    public String getSerialPortName() {
        return serialPortName;
    }

    public void setSerialPortName(String serialPortName) {
        this.serialPortName = serialPortName;
    }

    public int getBaud() {
        return baud;
    }

    public void setBaud(int baud) {
        this.baud = baud;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }
    
    public File getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(File outputFilename) {
        this.outputFilename = outputFilename;
    }

    public boolean isOutputSaved() {
        return saveOutput;
    }

    public void setSaveOutput(boolean saveOutput) {
        this.saveOutput = saveOutput;
    }


}
