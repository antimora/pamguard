package PamView;

import javax.swing.BoundedRangeModel;
import javax.swing.JProgressBar;

import PamView.PamColors.PamColor;

public class PamProgressBar extends JProgressBar implements ColorManaged{

	public PamProgressBar() {
		// TODO Auto-generated constructor stub
	}

	public PamProgressBar(int orient) {
		super(orient);
		// TODO Auto-generated constructor stub
	}

	public PamProgressBar(BoundedRangeModel newModel) {
		super(newModel);
		// TODO Auto-generated constructor stub
	}

	public PamProgressBar(int min, int max) {
		super(min, max);
		// TODO Auto-generated constructor stub
	}

	public PamProgressBar(int orient, int min, int max) {
		super(orient, min, max);
		// TODO Auto-generated constructor stub
	}

	private PamColor defaultColor = PamColor.BORDER;
	
	public PamColor getDefaultColor() {
		return defaultColor;
	}

	public void setDefaultColor(PamColor defaultColor) {
		this.defaultColor = defaultColor;
	}

	@Override
	public PamColor getColorId() {
		return defaultColor;
	}
}
