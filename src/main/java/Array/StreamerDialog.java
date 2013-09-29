package Array;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamController.PamController;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class StreamerDialog extends PamDialog {
	
	private JTextField x, y, z, dx, dy, dz;
	
	static public final int textLength = 4;
	static public final int errorTextLength = 6;
	
	private Streamer streamer;

	private JTextField buoyId; 
	
	private static StreamerDialog singleInstance;

	private StreamerDialog(Window parentFrame) {
		super(parentFrame, "Hydrophone Streamer", false);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder("Location"));
		GridBagConstraints c = new PamGridBagContraints();
		c.gridx = 1;
		addComponent(p, new JLabel("Position"), c);
		c.gridx+=2;
		addComponent(p, new JLabel("Error"), c);
		c.gridx = 0;
		c.gridy ++;
		addComponent(p, new JLabel("x "), c);
		c.gridx++;
		addComponent(p, x = new JTextField(textLength), c);
		c.gridx++;
		addComponent(p, new JLabel(" +/- "), c);
		c.gridx++;
		addComponent(p, dx = new JTextField(errorTextLength), c);

		c.gridx = 0;
		c.gridy ++;
		addComponent(p, new JLabel("y "), c);
		c.gridx++;
		addComponent(p, y = new JTextField(textLength), c);
		c.gridx++;
		addComponent(p, new JLabel(" +/- "), c);
		c.gridx++;
		addComponent(p, dy = new JTextField(errorTextLength), c);

		c.gridx = 0;
		c.gridy ++;
		addComponent(p, new JLabel("Depth "), c);
		c.gridx++;
		addComponent(p, z = new JTextField(textLength), c);
		c.gridx++;
		addComponent(p, new JLabel(" +/- "), c);
		c.gridx++;
		addComponent(p, dz = new JTextField(errorTextLength), c);
		
		JPanel bPanel = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		bPanel.setBorder(new TitledBorder("PAMBuoy Id"));
		addComponent(bPanel, new JLabel("Buoy Id "), c);
		c.gridx++;
		addComponent(bPanel, buoyId = new JTextField(textLength), c);
		buoyId.setToolTipText("PAMBuoy ID can be null, leave blank for automatic assignment");
		if (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER) {
			mainPanel.add(bPanel);
		}

		mainPanel.add(p);
		setDialogComponent(mainPanel);
		
	}
	
	public static Streamer showDialog(Window window, Streamer streamer) {
		if (singleInstance == null || singleInstance.getOwner() != window) {
			singleInstance = new StreamerDialog(window);
		}
		singleInstance.streamer = streamer.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.streamer;
	}

	private void setParams() {
		x.setText(formatDouble(streamer.getX()));
		y.setText(formatDouble(streamer.getY()));
		z.setText(formatDouble(-streamer.getZ()));
		dx.setText(formatDouble(streamer.getDx()));
		dy.setText(formatDouble(streamer.getDy()));
		dz.setText(formatDouble(streamer.getDz()));
		if (streamer.getBuoyId1() != null) {
			buoyId.setText(streamer.getBuoyId1().toString());
		}
		else {
			buoyId.setText("");
		}
	}

	@Override
	public void cancelButtonPressed() {
		streamer = null;
	}

	@Override
	public boolean getParams() {
		try {
			streamer.setX(Double.valueOf(x.getText()));
			streamer.setY(Double.valueOf(y.getText()));
			streamer.setZ(-Double.valueOf(z.getText()));
			streamer.setDx(Double.valueOf(dx.getText()));
			streamer.setDy(Double.valueOf(dy.getText()));
			streamer.setDz(Double.valueOf(dz.getText()));		
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid parameter");
		}
		try {
			streamer.setBuoyId1(Integer.valueOf(buoyId.getText()));
		}
		catch (NumberFormatException e) {
			streamer.setBuoyId1(null);
		}
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
