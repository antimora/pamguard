//package PamGraph3D.graphDisplay3D;
//
//import java.awt.BorderLayout;
//
//import javax.swing.JPanel;
//import javax.swing.JPopupMenu;
//
//import PamguardMVC.PamDataUnit;
//import pamScrollSystem.AbstractPamScroller;
//import pamScrollSystem.PamScrollObserver;
//import pamScrollSystem.PamScroller;
//import pamScrollSystem.RangeSpinner;
//import pamScrollSystem.RangeSpinnerListener;
//
//public class Time3DAxis extends AbstractGraphDisplay3DAxis   {
//	
//	private PamScroller hScrollBar;
//	private JPanel scrollBarControls;
//	private RangeSpinner rangeSpinner;
//	
//
//	public Time3DAxis(GraphDisplay3D timeDisplay3D, int orientation) {
//		super(timeDisplay3D, orientation);
//		
//		if (orientation==super.VERTICAL){
//			hScrollBar = new PamScroller("", 
//					AbstractPamScroller.VERTICAL, 100, 5*60*1000, true);
//		}
//		
//		else{
//			hScrollBar = new PamScroller("", 
//					AbstractPamScroller.HORIZONTAL, 100, 5*60*1000, true);
//		}
//		hScrollBar.addObserver(new HScrollObserver());
//		scrollBarControls=new JPanel(new BorderLayout());
//		rangeSpinner = new RangeSpinner();
//		rangeSpinner.addRangeSpinnerListener(new TimeScale());
//		scrollBarControls.add(BorderLayout.EAST,rangeSpinner.getComponent());
//		scrollBarControls.add(BorderLayout.CENTER,hScrollBar.getComponent());
//
//	}
//	
//	
//	//listeners for scroll bar.
//	class HScrollObserver implements PamScrollObserver {
//		
//		double Java3dunits;
//
//		@Override
//		public void scrollRangeChanged(AbstractPamScroller pamScroller) {
//			timeDisplay3D.setSelectedDetection(null);
////			if (timeDisplay3D.pamDataBlock!=null){
////			timeDisplay3D.addDataBlock(timeDisplay3D.pamDataBlock);
////			}
//			scrollValueChanged(pamScroller);
//		}
//		
//		@Override
//		public void scrollValueChanged(AbstractPamScroller pamScroller) {
//			timeDisplay3D.setScrollBars();
//			
//		}
//
//	}
//	
//	class TimeScale implements RangeSpinnerListener {
//		@Override
//		public void valueChanged(double oldValue, double newValue) {
//			timeDisplay3D.rangeValueChanged();
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
//		return rangeSpinner.getSpinnerValue();		
//	}
//
//	@Override
//	public double getAxisMax() {
//		return hScrollBar.getRangeMillis()/1000.0+hScrollBar.getMinimumMillis()/1000.0;
//	}
//
//	@Override
//	public double getAxisMin() {
//		return hScrollBar.getMinimumMillis()/1000.0;
//	}
//
//	@Override
//	public double getAxisValue() {
//		return 0;
//	}
//
//	@Override
//	public double getAxisVisibleRange() {
//		return rangeSpinner.getSpinnerValue();
//	}
//	
//	public double getScrollBarMax(){
//		return hScrollBar.getMaximumMillis()/1000.0;
//	}
//	
//	public double getScrollBarMin(){
//		return hScrollBar.getMinimumMillis()/1000.0;
//	}
//	
//	public double getScrollBarValue(){
//		return hScrollBar.getValueMillis()/1000.0;
//	}
//	
//	@Override
//	public double getMeasurment(PamDataUnit pamDataUnit) {
//		return pamDataUnit.getTimeMilliseconds()/1000.0;
//	}
//
//	@Override
//	public String getAxisName() {
//		// TODO Auto-generated method stub
//		return "Time Axis";
//	}
//
//	@Override
//	public void addMenuItems(JPopupMenu popUpMenu) {
//		
//	}
//
//	
//
//
//}
