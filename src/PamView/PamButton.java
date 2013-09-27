package PamView;

import java.awt.Color;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import PamView.PamColors.PamColor;

public class PamButton extends JButton implements ColorManaged {

	public PamButton() {
		super();
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamButton(Action a) {
		super(a);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamButton(Icon icon) {
		super(icon);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamButton(String text, Icon icon) {
		super(text, icon);
//		PamColors.getInstance().registerComponent(this, PamColor.BORDER);
	}

	public PamButton(String text) {
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
