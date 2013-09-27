package angleMeasurement;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import java.awt.Frame;
import java.io.Serializable;

import serialComms.SerialPortCom;

import PamController.PamControlledUnitSettings;
import PamUtils.PamUtils;

/**
 * Read out a Fluxgate World 3030 shaft encoder. 
 * @author Douglas Gillespie
 *
 */
public class FluxgateWorldAngles extends AngleMeasurement  {
	
	private FluxgateWorldParameters fluxgateWorldParameters;
	
	private FGSerialPortCom fgSerialPortCom;
	
	private CommPortIdentifier comnPortIdentifier;
	
	private static final int BAUDRATE = 9600;
	
	private Double latestAngle = null;
	 
	private long latestTime;
	
	private volatile boolean commandSent = false;
	
	private String name;
	
	// commands to send fluxgate
	private static final String COMMAND_DIGITAL = "$Dr"; 
		
	public FluxgateWorldAngles(String name, boolean autoStart) {
		super(name);
		if (fluxgateWorldParameters == null) {
			fluxgateWorldParameters = new FluxgateWorldParameters();
		}
		this.name = name;
		setAngleParameters(fluxgateWorldParameters);
		if (autoStart) {
			start();
		}
	}

	@Override
	public Double getRawAngle() {
		return latestAngle;
	}
	
	@Override
	public Double getCorrectedAngle() {
		if (latestAngle == null) {
			return null;
		}
		double calibratedAngle = getCalibratedAngle(latestAngle);
		Double ang =  calibratedAngle - getAngleOffset();
		ang = PamUtils.constrainedAngle(ang);
		return ang;
	}

	@Override
	public void setZero() {

		if (latestAngle != null) {
			super.setAngleOffset(latestAngle);
		}

	}

	@Override
	public boolean settings(Frame parentFrame) {

		FluxgateWorldParameters newParams = FluxgateWorldDialog.showDialog(parentFrame, this, fluxgateWorldParameters);
		if (newParams != null) {
			fluxgateWorldParameters = newParams.clone();
			setAngleParameters(fluxgateWorldParameters);
			start();
			return true;
		}
		return false;
	}

	public Serializable getSettingsReference() {
		return fluxgateWorldParameters;
	}

	public long getSettingsVersion() {
		return FluxgateWorldParameters.serialVersionUID;
	}

	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		fluxgateWorldParameters = ((FluxgateWorldParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}
	
	public boolean start() {
		stop();
		CommPortIdentifier comPortIdentifier;
		try {
			comPortIdentifier = CommPortIdentifier.getPortIdentifier(fluxgateWorldParameters.portName);
		}
		catch (NoSuchPortException ex) {
			System.out.println(String.format("Serial port %s does not exist", fluxgateWorldParameters.portName));
			return false;
		}
		
		fgSerialPortCom = new FGSerialPortCom(fluxgateWorldParameters.portName, BAUDRATE, comPortIdentifier);
		sendCommand(COMMAND_DIGITAL, true, 2000);
		sendCommand(fluxgateWorldParameters.getRateCommand(), true, 2000);
		return true;
	}
	
	private boolean sendCommand(String command, boolean waitAnswer, long timeOut) {
		command += "\r\n";
		commandSent = true;
		fgSerialPortCom.writeToPort(command);
		long now = System.currentTimeMillis();
		while (commandSent) {
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (System.currentTimeMillis() - now > timeOut) {
				return false;
			}
		}
		return true;
	}
	
	private void interpretCommandReply(StringBuffer data) {
		
	}
	
	public void stop() {
		if (fgSerialPortCom == null) {
			return;
		}
		fgSerialPortCom.close();
		fgSerialPortCom = null;
	}
	class FGSerialPortCom extends SerialPortCom {

		public FGSerialPortCom(String portName, int baud, CommPortIdentifier portId) {
			super(portName, baud, portId, name);
		}

		@Override
		public void readData(StringBuffer result) {

			if (commandSent) {
				interpretCommandReply(result);
				commandSent = false;
				return;
			}
			try {
				latestAngle = Double.valueOf(result.substring(1));
				if (fluxgateWorldParameters.invertAngles) {
					latestAngle = 360. - latestAngle;
				}
				latestTime = System.currentTimeMillis();
			}
			catch (NumberFormatException e) {
				latestAngle = null;
			}
			notifyAngleMeasurementListeners();
		}
		
	}
	public FluxgateWorldParameters getFluxgateWorldParameters() {
		return fluxgateWorldParameters;
	}

	public void setFluxgateWorldParameters(FluxgateWorldParameters fluxgateWorldParameters) {
		super.setAngleParameters(fluxgateWorldParameters);
		this.fluxgateWorldParameters = fluxgateWorldParameters;
	}
	public void setFluxgateWorldParameters(FluxgateWorldParameters fluxgateWorldParameters, boolean start) {
		super.setAngleParameters(fluxgateWorldParameters);
		this.fluxgateWorldParameters = fluxgateWorldParameters;
		if (start) {
			start();
		}
	}


}
