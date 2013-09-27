package Array;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;

public class EnvironmentPanel {

	private ArrayDialog arrayDialog;
	
	private JPanel environmentPanel;
	
	private JTextField speedOfSound, sosError;// m/s
	
	private double newSpeed;
	
	private double newError;
	

	EnvironmentPanel(ArrayDialog arrayDialog) {
		this.arrayDialog = arrayDialog;
		environmentPanel = makePanel();
	}
	
	JPanel makePanel()
	{
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder("Environment"));
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		panel.add(new JLabel("Speed of sound "));
		panel.add(speedOfSound = new JTextField(6));
		panel.add(new JLabel(" +/- "));
		panel.add(sosError = new JTextField(5));
		panel.add(new JLabel(" m/s"));
		
		return panel;
	}

	public boolean getParams() {
		try {
			newSpeed = Double.valueOf(speedOfSound.getText());
			newError = Double.valueOf(sosError.getText());
		}
		catch (Exception Ex) {
			return false;
		}
		return true;
	}
	/**
	 * @return Returns the newSpeed.
	 */
	public double getNewSpeed() {
		return newSpeed;
	}
	
	public double getNewError() {
		return newError;
	}

	/**
	 * @param newSpeed The newSpeed to set.
	 */
	public void setNewSpeed(double newSpeed) {
		speedOfSound.setText(PamDialog.formatDouble(newSpeed));
		this.newSpeed = newSpeed;
	}
	
	public void setNewError(double newError) {
		sosError.setText(PamDialog.formatDouble(newError));
		this.newError = newError;
	}
	/**
	 * @return Returns the environmentPanel.
	 */
	public JPanel getEnvironmentPanel() {
		return environmentPanel;
	}
	
}
