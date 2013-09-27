package videoRangePanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Map.MasterReferencePoint;
import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamView.PamBorderPanel;
import PamView.PamLabel;

public class ImageAnglePanel extends PamBorderPanel {

	private VRControl vrControl;
	
	private JTextField angle;
	
	private JSpinner tiltSpinner;
	
	private JButton leftBig, leftSmall, rightBig, rightSmall;
	
	private JLabel imageWidthInfo, shoreInfo;
	
	private double smallStep = 0.1;
	
	private double bigStep = 1.0;

	public ImageAnglePanel(VRControl vrControl) {
		super();
		this.vrControl = vrControl;
		FlowLayout flowLayout;
		setLayout(flowLayout = new FlowLayout(FlowLayout.LEFT));
//		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
//		flowLayout.setVgap(0);
		add(new PamLabel("Bearing  "));
		add(leftBig = new JButton("<<"));
		add(leftSmall = new JButton("<"));
		add(angle = new JTextField(5));
		add(new PamLabel("\u00B0T"));
		add(rightSmall = new JButton(">"));
		add(rightBig = new JButton(">>"));
		add(new PamLabel("Horizon Tilt (\u00B0)"));
		tiltSpinner = new JSpinner(new SpinnerNumberModel(0,-60,60,0.1));
		tiltSpinner.addChangeListener(new TiltListener());
		add(tiltSpinner);
		
		add(imageWidthInfo = new PamLabel(" "));
		add(shoreInfo = new PamLabel(" "));
		
		Dimension eD = angle.getPreferredSize();
		Dimension sD = tiltSpinner.getPreferredSize();
		sD.height = eD.height;
//		sD.width = eD.width * 3/2;
//		tiltSpinner.setPreferredSize(sD);
		Dimension eD2 = tiltSpinner.getEditor().getPreferredSize();
		eD2.width = eD.width;
		eD2.height = eD.height;
		tiltSpinner.getEditor().setPreferredSize(eD2);
		
		leftBig.setToolTipText(String.format("Decrease angle by %.1f\u00B0", bigStep));
		leftSmall.setToolTipText(String.format("Decrease angle by %.1f\u00B0", smallStep));
		rightSmall.setToolTipText(String.format("Increase angle by %.1f\u00B0", smallStep));
		rightBig.setToolTipText(String.format("Increase angle by %.1f\u00B0", bigStep));
		
		leftBig.addMouseListener(new AngleButtonListener(leftBig, -bigStep));
		leftSmall.addMouseListener(new AngleButtonListener(leftSmall, -smallStep));
		rightSmall.addMouseListener(new AngleButtonListener(rightSmall, smallStep));
		rightBig.addMouseListener(new AngleButtonListener(rightBig, bigStep));
		
		angle.addFocusListener(new AngleFocus());
//		angle.addKeyListener(AngleKe)
		
		setAngle(vrControl.getImageAngle());
	}
	
	public Double getAngle() {
		try {
			return Double.valueOf(angle.getText());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
	
	public void setTilt(double tilt) {
		tilt = PamUtils.roundNumber(tilt, 0.1);
		tiltSpinner.setValue(tilt);
	}
	public double getTilt() {
		return (Double) tiltSpinner.getValue();
	}
	
	public void setAngle(Double angleValue) {
		if (angleValue != null) {
			angle.setText(String.format("%3.1f", angleValue));
		}
		sayShoreInfo();
	}
	
	/*
	 * Called when a new image is loaded. 
	 */
	public void newImage() {
		sayHorizonInfo();
		sayShoreInfo();
	}
	
	/**
	 * Called for new calibration or new height data
	 */
	public void newCalibration() {
		sayHorizonInfo();
		sayShoreInfo();
	}
	
	/**
	 * Called when a new VR Method is selected
	 */
	public void newMethod() {
		sayHorizonInfo();
		sayShoreInfo();
	}
	
	private void sayHorizonInfo() {
		int imageWidth = vrControl.vrTabPanelControl.vrPanel.getImageWidth();
		if (imageWidth == 0) {
			imageWidthInfo.setText(" No Image");
			return;
		}
		VRHeightData heightData = vrControl.vrParameters.getCurrentheightData();
		if (heightData == null) {
			imageWidthInfo.setText(" No Height Data");	
			return;
		}
		VRCalibrationData calData = vrControl.vrParameters.getCurrentCalibrationData();
		if (calData == null) {
			imageWidthInfo.setText(" No Calibration Data");			
			return;
		}
		double imageAngle = imageWidth * calData.degreesPerUnit;
		double horizonDistance = vrControl.rangeMethods.getCurrentMethod().getHorizonDistance(heightData.height);
		double horizonLength = horizonDistance * imageAngle * Math.PI / 180;
		
		imageWidthInfo.setText(String.format("Camera angle = %.2f\u00B0; Horizon Distance = %.0fm; Horizon length = %.0f m",
				imageAngle, horizonDistance, horizonLength));
	}
	
	private void sayShoreInfo() {
		Double shoreAngle = getAngle();
		if (shoreAngle == null) {
			shoreInfo.setText("");
			return;
		}
		LatLong globalReference = MasterReferencePoint.getRefLatLong();
		if (globalReference == null) {
			shoreInfo.setText("No master reference position - We've no idea where we are !");
			return;
		}
		double[] shoreIntercepts = vrControl.getShoreRanges();
		if (shoreIntercepts == null) {
			shoreInfo.setText("No land intercepts on current bearing");
			return;
		}
		String str = "Land intercepts at ";
		for (int i = 0; i < shoreIntercepts.length; i++) {
			str += String.format("%.0f m  ", shoreIntercepts[i]);
		}
		shoreInfo.setText(str);
	}
	
	public void enableControls() {
		tiltSpinner.setEnabled(vrControl.getVrStatus() == VRControl.MEASURE_FROM_SHORE);
	}
	
	class TiltListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {
			vrControl.setHorizonTilt(getTilt());
		}
		
	}
	class AngleButtonListener extends MouseAdapter  {

		private Object pressedButton;
		private double step;
		private int initialTime = 300;
		private int subsequentTimes = 100;
		Timer timer;
		public AngleButtonListener(Object pressedButton, double step) {
			super();
			this.pressedButton = pressedButton;
			this.step = step;
			timer = new Timer(subsequentTimes, new TimerAction());
			timer.setInitialDelay(initialTime);
		}
		

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			timer.start();
			vrControl.stepImageAngle(step);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
			timer.stop();
		}
		
		class TimerAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				vrControl.stepImageAngle(step);
			}
		}
	}
	
	class AngleFocus extends FocusAdapter {

		@Override
		public void focusLost(FocusEvent e) {
			vrControl.setImageAngle(getAngle());
		}
		
	}
}
