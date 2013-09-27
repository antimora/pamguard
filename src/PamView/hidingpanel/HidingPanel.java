package PamView.hidingpanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;

import PamView.PamButton;
import PamView.PamPanel;

/**
 * Class for a hiding, possibly sliding panel to hold things like
 * the side bar, top control panel of the clip display, etc. 
 * @author Doug Gillespie
 *
 */
public class HidingPanel extends PamPanel {

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;
	
	private Component mainComponent;
	private int direction; 
	private boolean canScroll;
	private PamPanel edgePanel;
	JButton showButton, hideButton;
	private String showSymbol, hideSymbol;
	private JScrollPane scrollPanel;
	private Component thingToHide;
	private Component componentFrame; // frame to inalidate once thing is hidden. 
	private String title;
	
	public HidingPanel(Component componentFrame, Component mainComponent, int direction, boolean canScroll) {
		super(new BorderLayout());
		this.componentFrame = componentFrame;
		this.mainComponent = mainComponent;
		this.direction = direction;
		this.canScroll = canScroll;
		edgePanel = new PamPanel(new BorderLayout());

		if (canScroll) {
			scrollPanel = new PanelScrollPane(mainComponent);
			thingToHide = scrollPanel;
		}
		else {
			thingToHide = mainComponent;
		}
		this.add(BorderLayout.CENTER, thingToHide);
		
		if (direction == HORIZONTAL) {
			showButton = new JButton("", new ImageIcon(ClassLoader
					.getSystemResource("Resources/SidePanelShowH.png")));
			showButton.addActionListener(new ShowButton());
			showButton.setToolTipText("Show panels");
			showButton.setMargin(new Insets(0, 0, 0, 0));
			hideButton = new JButton("", new ImageIcon(ClassLoader
					.getSystemResource("Resources/SidePanelHideH.png")));
			hideButton.addActionListener(new HideButton());
			hideButton.setToolTipText("Hide panels");
			hideButton.setMargin(new Insets(0, 0, 0, 0));
			edgePanel.add(BorderLayout.NORTH, showButton);
			edgePanel.add(BorderLayout.SOUTH, hideButton);
			this.add(BorderLayout.WEST, edgePanel);
			if (scrollPanel != null) {
				scrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
				scrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			}
		}
		else {
			showButton = new JButton("", new ImageIcon(ClassLoader
					.getSystemResource("Resources/SidePanelShow.png")));
			showButton.addActionListener(new ShowButton());
			showButton.setToolTipText("Show panels");
			showButton.setMargin(new Insets(0, 0, 0, 0));
			hideButton = new JButton("", new ImageIcon(ClassLoader
					.getSystemResource("Resources/SidePanelHide.png")));
			hideButton.addActionListener(new HideButton());
			hideButton.setToolTipText("Hide panels");
			hideButton.setMargin(new Insets(0, 0, 0, 0));
			edgePanel.add(BorderLayout.WEST, showButton);
			edgePanel.add(BorderLayout.EAST, hideButton);
			this.add(BorderLayout.NORTH, edgePanel);
			if (scrollPanel != null) {
				scrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			}
		}
		this.addMouseListener(new PanelMouse());
		edgePanel.addMouseListener(new PanelMouse());
		showPanel(true);
	}
	
	public void setTitle(String title) {
		this.title = title;
		setToolTips();
	}
	
	private void setToolTips() {
		if (title != null) {
			showButton.setToolTipText("Click to show " + title);
			hideButton.setToolTipText("Click to hide " + title);
			edgePanel.setToolTipText("Click");
			edgePanel.setToolTipText(thingToHide.isVisible() ? hideButton.getToolTipText() : showButton.getToolTipText());
			this.setToolTipText(thingToHide.isVisible() ? hideButton.getToolTipText() : showButton.getToolTipText());
		}
		
	}

	class ShowButton implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			showPanel(true);
		}

	}

	class HideButton implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			showPanel(false);
		}
	}
	
	private class PanelMouse extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent arg0) {
			showPanel(!thingToHide.isVisible());
		}
	}

	public void showPanel(boolean state) {
		thingToHide.setVisible(state);
		showButton.setVisible(!state);
		hideButton.setVisible(state);
		setToolTips();
		if (mainComponent != null) {
			mainComponent.invalidate();
		}
	}
	
	private class PanelScrollPane extends JScrollPane {
		
		public PanelScrollPane(Component mainComponent) {
			super(mainComponent);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			Dimension tD = this.getSize();
			if (getVerticalScrollBar().isVisible()){// || d.width > tD.width) {
				d.width += getVerticalScrollBar().getWidth();
			}
			if (getHorizontalScrollBar().isVisible()) {
				d.height += getHorizontalScrollBar().getHeight();
			}
			return d;
		}
	}
}
