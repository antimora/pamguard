package PamView;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;

import PamView.PamColors.PamColor;

/**
 * Extension of JLabel to use standard PAMGUARD colours
 * <p> generally not used in dialogs which use the default
 * colours, but should be used in 
 * preference to JLabel in on screen information. 
 * 
 * @author Doug Gillespie
 *
 */
public class PamLabel extends JLabel implements ColorManaged {

	public PamLabel(Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamLabel(String text, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamLabel() {
		super();
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamLabel(Icon image) {
		super(image);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamLabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamLabel(String text) {
		super(text);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
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
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		this.setForeground(PamColors.getInstance().getColor(PamColor.AXIS));
	}
 
	
}
