package PamView;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;

public class CornerLayout  implements LayoutManager2 {



	private ArrayList<Component> components = new ArrayList<Component>();
	private ArrayList<CornerLayoutContraint> constraints = new ArrayList<CornerLayoutContraint>();

	private CornerLayoutContraint defaultConstraint;

	public CornerLayout(CornerLayoutContraint defaultConstraint) {
		this.defaultConstraint = defaultConstraint;
	}
	
	public void invalidateLayout(Container arg0) {
		layoutContainer(arg0);
	}

	public Dimension maximumLayoutSize(Container arg0) {
		return null;
	}

	public void addLayoutComponent(String arg0, Component arg1) {
		System.out.println("CornerLayout.addLayoutComponent");
		return;
		
	}

	public void layoutContainer(Container arg0) {

		Component component;
		CornerLayoutContraint c;
		int x, y;
		for (int i = 0; i < components.size(); i++) {
			component = components.get(i);
			c = constraints.get(i);

			component.setSize(component.getPreferredSize());
			
			Insets insets = arg0.getInsets();
			if (insets == null) {
				insets = new Insets(0,0,0,0);
			}
			
			x = y = 0;
			if (c != null) { 
				switch(c.anchor) { // sort out x coordinate
				case CornerLayoutContraint.FIRST_LINE_START:
				case CornerLayoutContraint.LAST_LINE_START:
				case CornerLayoutContraint.LINE_START:
					x = insets.left;
					break;
				case CornerLayoutContraint.PAGE_START:
				case CornerLayoutContraint.PAGE_END:
				case CornerLayoutContraint.CENTER:
					x = (arg0.getWidth() - component.getWidth()) / 2;
					break;
				case CornerLayoutContraint.FIRST_LINE_END:
				case CornerLayoutContraint.LAST_LINE_END:
				case CornerLayoutContraint.LINE_END:
					x = arg0.getWidth() - component.getWidth() - insets.right;
					break;
				default:
					x = insets.left;
				}
				switch(c.anchor) { // sort out y coordinate
				case CornerLayoutContraint.FIRST_LINE_START:
				case CornerLayoutContraint.PAGE_START:
				case CornerLayoutContraint.FIRST_LINE_END:
					y = insets.top;
					break;
				case CornerLayoutContraint.LINE_START:
				case CornerLayoutContraint.LINE_END:
				case CornerLayoutContraint.CENTER:
					y = (arg0.getHeight() - component.getHeight()) / 2;
					break;
				case CornerLayoutContraint.LAST_LINE_END:
				case CornerLayoutContraint.LAST_LINE_START:
				case CornerLayoutContraint.PAGE_END:
					y = arg0.getHeight() - component.getHeight() - insets.bottom;
					break;
				default:
					y = insets.top;
				}
				component.setLocation(x, y);
//				component.repaint();
			}
			
		}
		
	}

	public Dimension minimumLayoutSize(Container arg0) {
		return new Dimension(arg0.getWidth(), arg0.getHeight());
	}

	public Dimension preferredLayoutSize(Container arg0) {
		return new Dimension(arg0.getWidth(), arg0.getHeight());
	}

	public void removeLayoutComponent(Component arg0) {

		int ind = components.indexOf(arg0);
		if (ind >= 0) {
			constraints.remove(ind);
		}
		components.remove(arg0);
		
	}

	public void addLayoutComponent(Component arg0, Object arg1) {
		components.add(arg0);
		if (arg1 == null) {
			constraints.add(defaultConstraint.clone());
		}
		else {
			constraints.add(((CornerLayoutContraint) arg1).clone());
		}
	}

	public float getLayoutAlignmentX(Container arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getLayoutAlignmentY(Container arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

}
