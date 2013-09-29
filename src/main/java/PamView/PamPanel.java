package PamView;

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import PamView.PamColors.PamColor;

public class PamPanel extends JPanel implements ColorManaged{

	public PamPanel() {
		super();
		setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
		setForeground(PamColors.getInstance().getForegroudColor(PamColor.BORDER));
	}

	public PamPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
		setForeground(PamColors.getInstance().getForegroudColor(PamColor.BORDER));
	}

	public PamPanel(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
		setForeground(PamColors.getInstance().getForegroudColor(PamColor.BORDER));
	}

	public PamPanel(LayoutManager layout) {
		super(layout);
		setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
		setForeground(PamColors.getInstance().getForegroudColor(PamColor.BORDER));
	}

	public PamPanel(PamColor defaultColor) {
		super();
		setDefaultColor(defaultColor);
	}

	private PamColor defaultColor = PamColor.BORDER;
	
	public PamColor getDefaultColor() {
		return defaultColor;
	}

	public void setDefaultColor(PamColor defaultColor) {
		PamColors.getInstance().setColor(this, defaultColor);
		this.defaultColor = defaultColor;
	}

	@Override
	public PamColor getColorId() {
		return defaultColor;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#setBackground(java.awt.Color)
	 */
	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		Border border = getBorder();
		if (border != null && TitledBorder.class.isAssignableFrom(border.getClass())) {
			((TitledBorder) border).setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
		}
	}
	
	
}
