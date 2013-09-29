//package PamGraph3D.graphDisplay3D;
//
//import java.awt.BorderLayout;
//import java.awt.Dimension;
//import java.awt.Point;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.AdjustmentEvent;
//import java.awt.event.AdjustmentListener;
//
//import javax.swing.JMenuItem;
//import javax.swing.JPanel;
//import javax.swing.JPopupMenu;
//import javax.swing.JScrollBar;
//import javax.swing.JSeparator;
//import javax.swing.JSpinner;
//import javax.swing.SpinnerModel;
//import javax.swing.SpinnerNumberModel;
//import javax.swing.border.TitledBorder;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//
//import staticLocaliser.StaticLocaliserParams;
//import staticLocaliser.dialog.OptionsDialog;
//
//import PamDetection.PamDetection;
//import PamguardMVC.PamDataUnit;
//
///**
// * An axis which allows any PAMDETECTION (not PamDataUnit) to be displayed. These include clics and whistles. This axis allows the user to switch between bearing and amplitude for any PamDetection. 
// * 
// * @author spn1
// *
// */
//public class BearingAmpAxis extends AbstractGraphDisplay3DAxis  {
//	
//	//Swing components
//	private JScrollBar vertScrollBar;
//	private JPanel scrollBarControls;
//	private JSpinner spinner;
//	
//	private BTAxis3DParams bearingAmpParams;
//	private GraphDisplay3D timeDisplay3D;
//
//	public BearingAmpAxis(GraphDisplay3D timeDisplay3D, int orientation) {
//		super(timeDisplay3D, orientation);
//		
//		this.timeDisplay3D=timeDisplay3D;
//		bearingAmpParams=new BTAxis3DParams();
//		
//		scrollBarControls=new JPanel(new BorderLayout());
//		if (orientation==super.VERTICAL){
//		vertScrollBar=new JScrollBar(JScrollBar.VERTICAL,0,1,0,100);
//		}
//		else{
//		vertScrollBar=new JScrollBar(JScrollBar.HORIZONTAL,0,1,0,100);
//		}
//		vertScrollBar.addAdjustmentListener(new VertScrollObserver());
//		scrollBarControls.add(BorderLayout.CENTER,vertScrollBar);
//		
//		SpinnerModel model =
//		        new SpinnerNumberModel(180.0, //initial value
//		                               1.0, //min
//		                               500, //max
//		                               10.0);  
//		spinner = new JSpinner(model);
//		spinner.addChangeListener(new SpinnerListener());
//		
//		spinner.setPreferredSize(new Dimension(40,25));
//		
//		scrollBarControls.add(BorderLayout.NORTH, spinner);
//		
//		
//	}
//	
//	class SpinnerListener implements ChangeListener{
//
//		@Override
//		public void stateChanged(ChangeEvent arg0) {
//			spinnerChanged();
//		}
//		
//	}
//	
//	/**
//	 * The range value is the visible range which is seen on the graph...it is therefore always a positive number. This function informs the grpah3DDisplay when the visible range has been changed and ensures the visible range is never greater than the range
//	 * between the graph maximum and minimum. 
//	 */
//	public void spinnerChanged(){
//		double range=(Double) spinner.getValue();
//		double maxRange=bearingAmpParams.vertMax-bearingAmpParams.vertMin;
//		//must convert to radians if a bearing
//		if (bearingAmpParams.mode==BTAxis3DParams.BEARING){
//			if (Math.toRadians(range)>maxRange){
//				bearingAmpParams.vertVisRange=maxRange;
//				spinner.setValue(Math.toDegrees(maxRange));
//			}
//			else{
//				bearingAmpParams.vertVisRange=Math.toRadians(range);
//			}
//		}
//		else{
//			if (range>maxRange){
//				bearingAmpParams.vertVisRange=maxRange;
//				spinner.setValue(maxRange);
//			}
//			else{
//				bearingAmpParams.vertVisRange=range;
//			}
//		}
//		timeDisplay3D.rangeValueChanged();
//	}
//	
//	
//	class VertScrollObserver implements AdjustmentListener {
//		@Override
//		public void adjustmentValueChanged(AdjustmentEvent arg0) {
//			timeDisplay3D.setScrollBars();
//		}
//	}
//
//	@Override
//	public JPanel getPanel() {
//		return scrollBarControls;
//	}
//
//	@Override
//	public double getRange() {
//		return bearingAmpParams.vertVisRange;
//	}
//
//	@Override
//	public double getAxisMax() {
//		return bearingAmpParams.vertMax;
//	}
//
//	@Override
//	public double getAxisMin() {
//		return bearingAmpParams.vertMin;
//	}
//
//	@Override
//	public double getAxisValue() {
//		return 0;
//	}
//
//	@Override
//	public double getAxisVisibleRange() {
//		return bearingAmpParams.vertVisRange;
//	}
//
//	@Override
//	public double getScrollBarValue() {
//		return vertScrollBar.getValue();
//		//return vertScrollBar.getMaximum()-vertScrollBar.getValue();
//	}
//
//	@Override
//	public double getScrollBarMax() {
//		return vertScrollBar.getMaximum();
//	}
//
//	@Override
//	public double getScrollBarMin() {
//		return vertScrollBar.getMinimum();
//	}
//
//	@Override
//	public double getMeasurment(PamDataUnit pamDataUnit) {
//		double unit=0;
//		PamDetection detection=(PamDetection) pamDataUnit;
//		if (detection==null) return 0;
//
//		switch  (bearingAmpParams.mode){
//		case BTAxis3DParams.BEARING:
//			if (detection.getLocalisation()!=null){
//				if (detection.getLocalisation().getAngles()!=null){
//					unit= detection.getLocalisation().getAngles()[0];
//				}
//			}
//			else{
//			unit=0.5;
//			}
//			break;
//		case BTAxis3DParams.AMPLITUDE:
//			unit= detection.getAmplitudeDB();
//			break;
//		case BTAxis3DParams.ICI:
//			unit= 0.0;
//			break;
//		}
//		
//		return unit;
//	}
//	
//	public void axisChanged(){
//		timeDisplay3D.refreshData();
//		spinner.setValue(180.0);
//		spinnerChanged();
//		timeDisplay3D.setScrollBars();
//	}
//
//
//	@Override
//	public String getAxisName() {
//		return "Bearing, Amplitude";
//	}
//
//	@Override
//	public void addMenuItems(JPopupMenu popUpMenu){
//		JMenuItem menuItem=new JMenuItem("Bearing, Amplitude Options");
//		menuItem.addActionListener(new BTOptions());
//		popUpMenu.add(menuItem);
//	}
//	
//	class BTOptions implements ActionListener{
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			BTAxis3DParams newParams=BTAxis3DOptionsDialog.showDialog(timeDisplay3D.getFrame(),timeDisplay3D.getMousePosition() , getParams());
//			if (newParams!=null) {
//				bearingAmpParams=newParams.clone();
//				axisChanged();
//			}
//		}
//	}
//
//	public BTAxis3DParams getParams() {
//		return bearingAmpParams;
//	}
//
//}
