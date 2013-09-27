//package PamGraph3D.graphDisplay3D;
//
//import javax.swing.JPanel;
//
//
//import pamScrollSystem.AbstractPamScroller;
//import pamScrollSystem.PamScrollObserver;
//import pamScrollSystem.PamScroller;
//
//public abstract class AbstractGraphDisplay3DAxis implements GraphDisplay3DAxisProvider {
//	
//	public GraphDisplay3D timeDisplay3D;
//	
//	public final static int HORIZONTAL=0x1;
//	public final static int VERTICAL=0x2;
//	
//	private int orientation;
//	
//	public AbstractGraphDisplay3DAxis(GraphDisplay3D timeDisplay3D, int orientation){
//		this.timeDisplay3D=timeDisplay3D;
//		this.orientation=orientation;
//
//	
//	}
//	
//	
//	public GraphDisplay3D getTimeDisplay3D(){
//		return timeDisplay3D;
//	}
//	
//	public double getScrollBarMax(){
//		return 0;	
//	}
//	
//	public double getScrollBarMin(){
//		return 0;
//	}
//	
//	public double getScrollBarValue(){
//		return 0;
//	}
//	
//	public void getAxisOptions() {
//	}
//	
//	public boolean hasAxisOptions() {
//		return false;
//	}
//	
//	public double getRange() {
//		return getAxisMax()-getAxisMin();
//	}
//	
//
//	
//
//}
