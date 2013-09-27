package bearingTimeDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Layout.PamAxis;
import Layout.PamAxisPanel;
import PamDetection.PamDetection;
import PamView.PamLabel;
import PamguardMVC.PamDataUnit;


public class BearingAmpAxis extends AbstractAxis {
	
	//Swing components
	private JScrollBar vertScrollBar;
	private JPanel scrollBarControls;
	private JSpinner spinner;
	private PamAxis axis;
	
	private BTAxisParams bearingAmpParams;
	private GraphDisplay graphDisplay;

	public BearingAmpAxis(GraphDisplay graphDisplay, int orientation) {
		super(graphDisplay, orientation);
		
		this.graphDisplay=graphDisplay;
		bearingAmpParams=new BTAxisParams();
		//Create the scroll bar and JSpinner. This will allow the axis to change the visible range of data.
		SpinnerModel model =
		        new SpinnerNumberModel(180.0, //initial value
		                               1.0, //min
		                               500, //max
		                               10.0);  
		spinner = new JSpinner(model);
		spinner.addChangeListener(new SpinnerListener());
		
		spinner.setPreferredSize(new Dimension(40,25));
		
		scrollBarControls=new JPanel(new BorderLayout());
		
		if (orientation==super.VERTICAL){
		vertScrollBar=new JScrollBar(JScrollBar.VERTICAL,0,1,0,100);
		scrollBarControls.add(BorderLayout.NORTH, spinner);
		//add whitespace to prevent over size scroll bar.
		JPanel whitespace=new JPanel(new BorderLayout());
		whitespace.setPreferredSize(new Dimension(10,10));
		whitespace.setBackground(Color.WHITE);
		scrollBarControls.add(BorderLayout.WEST, whitespace);

		}
		else{
		vertScrollBar=new JScrollBar(JScrollBar.HORIZONTAL,0,1,0,100);
		scrollBarControls.add(BorderLayout.EAST, spinner);

		}
		
		scrollBarControls.add(BorderLayout.CENTER,vertScrollBar);
		vertScrollBar.addAdjustmentListener(new VertScrollObserver());
		
		//create the PamAxis();
		axis = new PamAxis(0, 0, 1, 1, 0, 1, true, getAxisLabel(), "%d");

		
//		create a panel which shows bearing amplitude values scale values. 
//		scaleDisplay=new JPanel(new BorderLayout());
//		scaleDisplay.setPreferredSize(new Dimension (100,100));
//		Rotate the text if the axis is vertical.
//		
//		if (orientation==super.VERTICAL){
//			axisLabel=new JLabel(getAxisLabel());
//		}
//		else{
//			axisLabel=new JLabel(getAxisLabel());
//		}
		
		refereshAxisDisplay();
	

	}
	
	class SpinnerListener implements ChangeListener{
		@Override
		public void stateChanged(ChangeEvent arg0) {
			spinnerChanged();
		}
	}
	
	/**
	 * The range value is the visible range which is seen on the graph...it is therefore always a positive number. This function informs the grpah3DDisplay when the visible range has been changed and ensures the visible range is never greater than the range
	 * between the graph maximum and minimum. 
	 */
	public void spinnerChanged(){
		double range=(Double) spinner.getValue();
		double maxRange=bearingAmpParams.vertMax-bearingAmpParams.vertMin;
		//must convert to radians if a bearing
		if (bearingAmpParams.mode==BTAxisParams.BEARING){
			if (Math.toRadians(range)>maxRange){
				bearingAmpParams.vertVisRange=maxRange;
				spinner.setValue(Math.toDegrees(maxRange));
			}
			else{
				bearingAmpParams.vertVisRange=Math.toRadians(range);
			}
		}
		else{
			if (range>maxRange){
				bearingAmpParams.vertVisRange=maxRange;
				spinner.setValue(maxRange);
			}
			else{
				bearingAmpParams.vertVisRange=range;
			}
		}
		refereshAxisDisplay();
		graphDisplay.rangeValueChanged();
	}
	
	
	class VertScrollObserver implements AdjustmentListener {
		@Override
		public void adjustmentValueChanged(AdjustmentEvent arg0) {
			graphDisplay.scrollBarChanged();
			refereshAxisDisplay();
		}
	}

	@Override
	public JPanel getAxisControlPanel() {
		return scrollBarControls;
	}
	

	@Override
	public double getRange() {
		return bearingAmpParams.vertVisRange;
	}

	@Override
	public double getAxisMax() {
		return bearingAmpParams.vertMax;
	}

	@Override
	public double getAxisMin() {
		return bearingAmpParams.vertMin;
	}


	@Override
	public double getAxisVisibleRange() {
		return bearingAmpParams.vertVisRange;
	}

	@Override
	public double getScrollBarValue() {
		return vertScrollBar.getValue();
		//return vertScrollBar.getMaximum()-vertScrollBar.getValue();
	}

	@Override
	public double getScrollBarMax() {
		return vertScrollBar.getMaximum();
	}

	@Override
	public double getScrollBarMin() {
		return vertScrollBar.getMinimum();
	}

	@Override
	public double getMeasurment(PamDataUnit pamDataUnit) {
		double unit=0;
		PamDetection detection=(PamDetection) pamDataUnit;
		if (detection==null) return 0;

		switch  (bearingAmpParams.mode){
		case BTAxisParams.BEARING:
			if (detection.getLocalisation()!=null){
				if (detection.getLocalisation().getAngles()!=null){
					unit= detection.getLocalisation().getAngles()[0];
				}
			}
			else{
			unit=0.5;
			}
			break;
		case BTAxisParams.AMPLITUDE:
			unit= detection.getAmplitudeDB();
			break;
		case BTAxisParams.ICI:
			unit= 0.0;
			break;
		}
		
		return unit;
	}
	
	public void axisChanged(){
		graphDisplay.refreshAllData();
		//reset to 180 or less (due to spinner changed function) if axis is changed.
		spinner.setValue(180.0);
		spinnerChanged();
		graphDisplay.scrollBarChanged();
		refereshAxisDisplay();
	}
	
	private void refereshAxisDisplay(){
		//change display panel 
		double[] minMax=GraphDisplay.getWindowMinMax(this);
		if (bearingAmpParams.mode==BTAxisParams.BEARING){
			minMax[0]=Math.toDegrees(minMax[0]);
			minMax[1]=Math.toDegrees(minMax[1]);
		}
		getDisplayAxis().setLabel(getAxisLabel());
		getDisplayAxis().setMinVal(minMax[0]);
		getDisplayAxis().setMaxVal(minMax[1]);
		getDisplayAxis().setInterval((minMax[1]-minMax[0])/4.0);
	}
	
	private PamAxis getDisplayAxis(){
		return axis;
	}
	
	@Override
	public String getAxisName() {
		return "Bearing, Amplitude";
	}
	
	private String getAxisLabel(){
		String axisName="";
		if (bearingAmpParams.mode==BTAxisParams.BEARING) axisName="Bearing (degrees)";
		if (bearingAmpParams.mode==BTAxisParams.AMPLITUDE) axisName="Amplitude (dB)";
		return axisName;
	}

	@Override
	public void addMenuItems(JPopupMenu popUpMenu){
		JMenuItem menuItem=new JMenuItem("Bearing, Amplitude Options");
		menuItem.addActionListener(new BTOptions());
		popUpMenu.add(menuItem);
	}
	
	class BTOptions implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			BTAxisParams newParams=BearingTimeOptionsDialog.showDialog(graphDisplay.getFrame(),graphDisplay.getMousePosition() , getParams());
			if (newParams!=null) {
				bearingAmpParams=newParams.clone();
				axisChanged();
			}
		}
	}
	

	public BTAxisParams getParams() {
		return bearingAmpParams;
	}

	@Override
	public boolean invert() {
		if (bearingAmpParams.mode==BTAxisParams.BEARING){
			return true;
		}
		return false;
	}


	@Override
	public PamAxis getPamAxis() {
		return axis;
	}
	
}
