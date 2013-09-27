package bearingTimeDisplay;

/**
 * The Abstract Axis class defines almost everything within the GraphDisplay. Any axis controls the maximum and minimum data range, the visible data range, the 
 * @author Jamie Macaulay
 *
 */

public abstract class AbstractAxis implements AxisProvider  {
	
public GraphDisplay graphDisplay;
	
	public final static int HORIZONTAL=0x1;
	public final static int VERTICAL=0x2;
	
	private int orientation;
	
	public AbstractAxis(GraphDisplay timeDisplay, int orientation){
		this.graphDisplay=timeDisplay;
		this.orientation=orientation;

	
	}
	
	
	public GraphDisplay getTimeDisplay3D(){
		return graphDisplay;
	}
	
	/**
	 * The maximum value of the scroll bar. If an axis has scrollbar then this will usually be theScrollBar.getMaximum();
	 *  If no scroll bar then leave this function as it is.
	 */
	public double getScrollBarMax(){
		return 0;	
	}
	
	/**
	 * The maximum value of the scroll bar. If an axis has scrollbar then this will usually be theScrollBar.getMinimum();
	 * If no scroll bar then leave this function as it is.
	 */
	public double getScrollBarMin(){
		return 0;
	}
	
	/**
	 * The maximum value of the scroll bar. If an axis has scrollbar then this will usually be theScrollBar.getValue();
	 * If no scroll bar then leave this function as it is.
	 */
	public double getScrollBarValue(){
		return 0;
	}

	/**
	 * Returns the data range.
	 * @return
	 */
	public double getRange() {
		return getAxisMax()-getAxisMin();
	}
	

}
