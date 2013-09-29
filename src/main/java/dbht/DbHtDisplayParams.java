package dbht;

import java.io.Serializable;

public class DbHtDisplayParams implements Serializable, Cloneable {

	public static final long serialVersionUID = 2L;
	
	public double minAmplitude = 0;
	
	public double maxAmplitude = 100;
	
	public long timeRange = 600;
	
	public boolean autoScale = true;
	
	public int symbolSize = 10;
	
	public boolean drawLine;
	
	public int showWhat = 0xF;
	
	public boolean showGrid;
	
	public boolean colourByChannel = true;

	@Override
	protected DbHtDisplayParams clone(){
		try {
			DbHtDisplayParams newParams = (DbHtDisplayParams) super.clone();
			return newParams;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
