package bearingTimeDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import pamScrollSystem.AbstractPamScroller;
import pamScrollSystem.PamScrollObserver;
import pamScrollSystem.PamScroller;
import pamScrollSystem.RangeSpinner;
import pamScrollSystem.RangeSpinnerListener;
import Layout.PamAxis;
import PamguardMVC.PamDataUnit;

public class TimeAxis extends AbstractAxis implements AxisProvider{
	
	private PamScroller hScrollBar;
	private JPanel scrollBarControls;
	private RangeSpinner rangeSpinner;
	private PamAxis axis;
	
	public TimeAxis(GraphDisplay timeDisplay, int orientation) {
		super(timeDisplay, orientation);
		
		scrollBarControls=new JPanel(new BorderLayout());
		rangeSpinner = new RangeSpinner();
		rangeSpinner.addRangeSpinnerListener(new TimeScale());
		
		if (orientation==super.VERTICAL){
			hScrollBar = new PamScroller("", 
			AbstractPamScroller.VERTICAL, 100, 5*60*1000, true);
			scrollBarControls.add(BorderLayout.NORTH,rangeSpinner.getComponent());
			JPanel whitespace=new JPanel(new BorderLayout());
			//add whitespace 
			whitespace.setPreferredSize(new Dimension(10,10));
			whitespace.setBackground(Color.WHITE);
			scrollBarControls.add(BorderLayout.WEST, whitespace);
		}
		
		else{
			hScrollBar = new PamScroller("", 
			AbstractPamScroller.HORIZONTAL, 100, 5*60*1000, true);
			scrollBarControls.add(BorderLayout.EAST,rangeSpinner.getComponent());
		}
		
		scrollBarControls.add(BorderLayout.CENTER,hScrollBar.getComponent());
		hScrollBar.addObserver(new HScrollObserver());
		
		axis = new PamAxis(0, 0, 1, 1, 0, 1, true, null, "%d");


	}

	//listeners for scroll bar.
	class HScrollObserver implements PamScrollObserver {
		
		double Java3dunits;

		@Override
		public void scrollRangeChanged(AbstractPamScroller pamScroller) {
			scrollValueChanged(pamScroller);
		}
		
		@Override
		public void scrollValueChanged(AbstractPamScroller pamScroller) {
			refereshAxisDisplay();
			graphDisplay.scrollBarChanged();
		}

	}
	
	class TimeScale implements RangeSpinnerListener {
		@Override
		public void valueChanged(double oldValue, double newValue) {
			refereshAxisDisplay();
			graphDisplay.rangeValueChanged();
		}
	}
	
	private  void refereshAxisDisplay(){
		double[] minMax=GraphDisplay.getWindowMinMax(this);
		axis.setMinVal(0);
		axis.setMaxVal(minMax[1]-minMax[0]);
		axis.setInterval((minMax[1]-minMax[0])/4.0);
	}

	@Override
	public JPanel getAxisControlPanel() {
		return scrollBarControls;
	}
	

	@Override
	public double getRange() {
		return rangeSpinner.getSpinnerValue();		
	}

	@Override
	public double getAxisMax() {
		return hScrollBar.getRangeMillis()/1000.0+hScrollBar.getMinimumMillis()/1000.0;
	}

	@Override
	public double getAxisMin() {
		return hScrollBar.getMinimumMillis()/1000.0;
	}

	@Override
	public double getAxisVisibleRange() {
		return rangeSpinner.getSpinnerValue();
	}
	
	public double getScrollBarMax(){
		return hScrollBar.getScrollBar().getMaximum();
	}
	
	public double getScrollBarMin(){
		return hScrollBar.getScrollBar().getMinimum();
	}
	
	public double getScrollBarValue(){
		return hScrollBar.getScrollBar().getValue();
		//return (hScrollBar.getValueMillis()-hScrollBar.getMinimumMillis())/1000.0;
	}
	
	@Override
	public double getMeasurment(PamDataUnit pamDataUnit) {
		return pamDataUnit.getTimeMilliseconds()/1000.0;
	}

	@Override
	public String getAxisName() {
		// TODO Auto-generated method stub
		return "Time Axis";
	}

	@Override
	public void addMenuItems(JPopupMenu popUpMenu) {
		
	}

	@Override
	public boolean invert() {
		return true;
	}


	@Override
	public PamAxis getPamAxis() {
		return axis;
	}

	


}
