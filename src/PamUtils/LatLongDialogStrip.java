package PamUtils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class LatLongDialogStrip extends JPanel implements KeyListener, ActionListener{
	
	JLabel formattedText;
	JTextField degrees, minutes, seconds, decminutes;
	JLabel dl, ml, sl, dml;
	JComboBox nsew;
	boolean isLatitude;
//	boolean decimalMinutes = true;
	
	public LatLongDialogStrip(boolean latitude) {
		
		isLatitude = latitude;
		String borderTitle;
		if (isLatitude) borderTitle = "Latitude";
		else borderTitle = "Longitude";
		this.setBorder(new TitledBorder(borderTitle));
		
		degrees = new JTextField(4);
		minutes = new JTextField(3);
		seconds = new JTextField(6);
		decminutes = new JTextField(8);
		nsew = new JComboBox();
		dl = new JLabel("deg.");
		ml = new JLabel("min.");
		sl = new JLabel("sec.");
		dml = new JLabel("dec min.");
		formattedText = new JLabel("Position");
		if (isLatitude) {
			nsew.addItem("N");
			nsew.addItem("S");
		}
		else{
			nsew.addItem("E");
			nsew.addItem("W");
		}
		setLayout(new BorderLayout());
		
		JPanel mp = new JPanel();
		mp.setLayout(new BoxLayout(mp, BoxLayout.X_AXIS));
		mp.setLayout(new FlowLayout(FlowLayout.LEFT));
		mp.add(dl);
		mp.add(degrees);
		mp.add(ml);
		mp.add(minutes);
		mp.add(sl);
		mp.add(seconds);
		mp.add(dml);
		mp.add(decminutes);
		mp.add(nsew);
		
		degrees.addKeyListener(this);
		minutes.addKeyListener(this);
		seconds.addKeyListener(this);
		decminutes.addKeyListener(this);
		nsew.addActionListener(this);
		
		this.add(BorderLayout.CENTER, mp);
		this.add(BorderLayout.SOUTH, formattedText);
		
		showControls();
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		newTypedValues(null);
		
	}
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		
		newTypedValues(e);
		
	}
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		
		newTypedValues(e);
		
	}
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
		
		newTypedValues(e);
		
	}
	private void newTypedValues(KeyEvent e) {
		double v = getValue();
		// now need to put that into the fields that
		// are not currently shown so that they are
		// ready if needed. 
		if (e != null) {
			sayValue(v, true);
		}
		// and say the formated version
		sayFormattedValue(v);
	}
	public void showControls() {
		boolean decimal = (LatLong.getFormatStyle() == LatLong.FORMAT_DECIMALMINUTES);
		minutes.setVisible(decimal == false);
		ml.setVisible(decimal == false);
		seconds.setVisible(decimal == false);
		sl.setVisible(decimal == false);
		decminutes.setVisible(decimal);
		dml.setVisible(decimal);
		sayFormattedValue(getValue());
	}
	/**
	 * Set data in the lat long dialog strip
	 * @param value Lat or Long in decimal degrees.
	 */
	public void sayValue(double value) {
		sayValue(value, false);
	}
	public void sayValue(double value, boolean hiddenOnly) {
		if (value >= 0) {
			nsew.setSelectedIndex(0);
		}
		else {
			nsew.setSelectedIndex(1);
		}
		double deg = LatLong.getSignedDegrees(value);
		if (degrees.isVisible() == false || !hiddenOnly) degrees.setText(String.format("%d", (int)Math.abs(deg)));
		if (minutes.isVisible() == false || !hiddenOnly) minutes.setText(String.format("%d", LatLong.getIntegerMinutes(value)));
		if (decminutes.isVisible() == false || !hiddenOnly) decminutes.setText(String.format("%3.5f", LatLong.getDecimalMinutes(value)));
		if (seconds.isVisible() == false || !hiddenOnly) seconds.setText(String.format("%3.5f", LatLong.getSeconds(value)));
		if (nsew.isVisible() == false || !hiddenOnly) nsew.setSelectedIndex(deg >= 0 ? 0 : 1);
		sayFormattedValue(value);
	}
	public double getValue() {
		double deg = 0;
		double min = 0;
		double sec = 0;
		double sin = 1.;
		if (nsew.getSelectedIndex() == 1) sin = -1.;
		try {
		  deg = Integer.valueOf(degrees.getText());
		}
		catch (NumberFormatException Ex) {
			return Double.NaN;
		}
		if (LatLong.getFormatStyle() == LatLong.FORMAT_DECIMALMINUTES){
			try {
				min = Double.valueOf(decminutes.getText());
			}
			catch (NumberFormatException ex) {
				return Double.NaN;
			}
		}
		else {
			try {
				min = Integer.valueOf(minutes.getText());
			}
			catch (NumberFormatException ex) {
				return Double.NaN;
			}
			try {
				sec = Double.valueOf(seconds.getText());
			}
			catch (NumberFormatException ex) {
				return Double.NaN;
			}
			
		}
		deg += min/60 + sec/3600;
		deg *= sin;
		return deg;
	}
	public void clearData() {
		degrees.setText("");
		minutes.setText("");
		seconds.setText("");
		decminutes.setText("");
	}
	public void sayFormattedValue(double value) {
		if (isLatitude) {
			formattedText.setText(LatLong.formatLatitude(value));
		}
		else {
			formattedText.setText(LatLong.formatLongitude(value));
		}
	}
//	public boolean isDecimalMinutes() {
//		return decimalMinutes;
//	}
//	public void setDecimalMinutes(boolean decimalMinutes) {
//		this.decimalMinutes = decimalMinutes;
//		showControls();
//	}
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		degrees.setEnabled(enabled);
		minutes.setEnabled(enabled);
		seconds.setEnabled(enabled);
		decminutes.setEnabled(enabled);
		nsew.setEnabled(enabled);
	}
}
