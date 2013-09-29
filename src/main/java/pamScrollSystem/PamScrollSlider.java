package pamScrollSystem;

import java.awt.BorderLayout;
import java.awt.event.MouseWheelEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PamScrollSlider extends AbstractPamScroller {
	
	private JSlider slider;
	
	private JPanel panel;

	public PamScrollSlider(String name, int orientation, int stepSizeMillis,
			long defaultLoadTime, boolean hasMenu) {
		super(name, orientation, stepSizeMillis, defaultLoadTime, hasMenu);
		
		panel = new JPanel(new BorderLayout());
		slider = new JSlider(orientation);
		slider.addChangeListener(new SliderListener());
		addMouseWheelSource(panel);
		addMouseWheelSource(slider);
		if (orientation == HORIZONTAL) {
			panel.add(BorderLayout.CENTER, slider);
			panel.add(BorderLayout.EAST, getButtonPanel());
		}
		else {
			panel.add(BorderLayout.CENTER, slider);
			panel.add(BorderLayout.SOUTH, getButtonPanel());
		}
	}
	
	class SliderListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			scrollMoved();			
		}
	}

	@Override
	void doMouseWheelAction(MouseWheelEvent mouseWheelEvent) {
		int n = mouseWheelEvent.getWheelRotation();
		slider.setValue(slider.getValue() - n);
		scrollMoved();
	}
	public void scrollMoved() {
		AbstractScrollManager.getScrollManager().moveInnerScroller(this, getValueMillis());	
		notifyValueChange();
	}

	@Override
	public void anotherScrollerMovedInner(long newValue) {
		// TODO Auto-generated method stub

	}

	@Override
	public JComponent getComponent() {
		return panel;
	}


	@Override
	public void rangesChanged(long setValue) {
		slider.setMinimum(0);
		slider.setMaximum((int) ((scrollerData.maximumMillis-scrollerData.minimumMillis)/
				scrollerData.stepSizeMillis));
//		setValueMillis(setValue);
//		AbstractScrollManager.getScrollManager().moveOuterScroller(this, getMinimumMillis(), getMaximumMillis());
	}

	@Override
	public long getValueMillis() {
		return scrollerData.minimumMillis + slider.getValue() * scrollerData.stepSizeMillis;
	}

	@Override
	public void valueSetMillis(long valueMillis) {
		valueMillis = Math.max(scrollerData.minimumMillis, Math.min(scrollerData.maximumMillis, valueMillis));
		int val = (int) ((valueMillis - scrollerData.minimumMillis) / scrollerData.stepSizeMillis);
		if (val >= slider.getMinimum() && val <= slider.getMaximum()) {
			slider.setValue(val);
		}
	}

}
