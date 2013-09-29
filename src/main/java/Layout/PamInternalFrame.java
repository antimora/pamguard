/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package Layout;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * @author Doug Gillespie
 *         <p>
 *         To make lots of pretty similar looking internal frames with an inner
 *         panel of graphics that sizes sensible, create a few subclasses of
 *         JInternalFrame with appropriate JPanels that handle all the bordering
 *         and sizing functions.
 *         <p>
 *         The constructor requires a subclass of the abstract PamFramePlots. 
 *         PamFramePlots contains references to the main compnents making up a 
 *         Java border layout. 
 *         <p> For an example see SpectrogramDisplay which is a subclass of PamFramePlots
 *         SpectrogramDisplay sets 
 * 
 */
public class PamInternalFrame extends JInternalFrame implements
		ComponentListener {

	/**
	 * axisPanel is the main (outer) panel that fills the entire 
	 * JInternalFrame centre
	 */
	private JPanel axisPanel;

	/**
	 * plotPanel is the inner panel containing various the actual plot,
	 * e.g. a spectrogram, bearing time display, waveform, etc. 
	 */
	private JPanel plotPanel;
	
    /**
     * in the case of a dual display, where there are two plot panels beside each
     * other sharing a common vertical axis, define left and right panel objects
     * UNTESTED - use at your own risk
     */
    private JPanel leftPlotPanel, rightPlotPanel;


	protected EmptyBorder emptyBorder;

	private Dimension lastSize = new Dimension();

	private PamFramePlots framePlots;
	
	public PamInternalFrame(PamFramePlots pamFramePlots, boolean canClose) {
		
		super(pamFramePlots.getName(), true, canClose, true, true);

		framePlots = pamFramePlots;

		pamFramePlots.setFrame(this);

		if (!System.getProperty("os.name").equals("Linux")) {
			setFrameIcon(new ImageIcon(ClassLoader
					.getSystemResource("Resources/pamguardIcon.png")));
		}
		
        boolean dualPlots = pamFramePlots.checkDualDisplay();

        if (dualPlots) {
            makeFrame (pamFramePlots.getName(), pamFramePlots.getAxisPanel(),
				pamFramePlots.getLeftPlotPanel(), pamFramePlots.getRightPlotPanel());
        } else {
		makeFrame(pamFramePlots.getName(), pamFramePlots.getAxisPanel(),
				pamFramePlots.getPlotPanel());
	}
	}

	private void makeFrame(String name, Component axisPanel, Component plotPanel) {

		this.axisPanel = (JPanel) axisPanel;

		this.plotPanel = (JPanel) plotPanel;

		emptyBorder = new EmptyBorder(20, 20, 20, 20);

		Component panel;

		if (axisPanel != null) {

			this.axisPanel.setBorder(emptyBorder);

			if (plotPanel != null) {
				this.axisPanel.setLayout(new GridLayout(1, 0));
//				PamColors.getInstance().registerComponent(plotPanel,
//						PamColors.PamColor.PlOTWINDOW);
				plotPanel.setSize(100, 100);
				plotPanel.setVisible(true);
				this.plotPanel.setBorder(BorderFactory
						.createLoweredBevelBorder());
				this.axisPanel.add(plotPanel);
			}

			if ((panel = framePlots.getNorthPanel()) != null) {
				this.add(BorderLayout.NORTH, panel);
			}

			if ((panel = framePlots.getSouthPanel()) != null) {
				this.add(BorderLayout.SOUTH, panel);
			}

			if ((panel = framePlots.getEastPanel()) != null) {
				this.add(BorderLayout.EAST, panel);
			}

			if ((panel = framePlots.getWestPanel()) != null) {
				this.add(BorderLayout.WEST, panel);
			}

//			PamColors.getInstance().registerComponent(axisPanel,
//					PamColors.PamColor.BORDER);
			axisPanel.setSize(10, 10);
			axisPanel.setVisible(true);

			this.add(BorderLayout.CENTER, axisPanel);
		}

		setSize(900, 400);

		setVisible(true);

		addComponentListener(this);
	}

    /**
     * Overloaded makeFrame method, to handle dual display (two displays with
     * a common vertical axis)
     * UNTESTED - use at your own risk
     *
     * @param name
     * @param axisPanel
     * @param plotPanel
     */
	private void makeFrame(String name, Component axisPanel,
            Component leftPlotPanel, Component rightPlotPanel) {

		this.axisPanel = (JPanel) axisPanel;
		this.leftPlotPanel = (JPanel) leftPlotPanel;
        this.rightPlotPanel = (JPanel) rightPlotPanel;

		emptyBorder = new EmptyBorder(20, 20, 20, 20);

		Component panel;

		if (axisPanel != null) {

			this.axisPanel.setBorder(emptyBorder);

			if (leftPlotPanel != null) {
				this.axisPanel.setLayout(new GridLayout(1, 0));
				leftPlotPanel.setSize(100, 100);
				leftPlotPanel.setVisible(true);
				this.leftPlotPanel.setBorder(BorderFactory
						.createLoweredBevelBorder());
				this.axisPanel.add(leftPlotPanel);
			}

			if (rightPlotPanel != null) {
				rightPlotPanel.setSize(100, 100);
				rightPlotPanel.setVisible(true);
				this.rightPlotPanel.setBorder(BorderFactory
						.createLoweredBevelBorder());
				this.rightPlotPanel.add(rightPlotPanel);
			}
			if ((panel = framePlots.getNorthPanel()) != null) {
				this.add(BorderLayout.NORTH, panel);
			}

			if ((panel = framePlots.getSouthPanel()) != null) {
				this.add(BorderLayout.SOUTH, panel);
			}

			if ((panel = framePlots.getEastPanel()) != null) {
				this.add(BorderLayout.EAST, panel);
			}

			if ((panel = framePlots.getWestPanel()) != null) {
				this.add(BorderLayout.WEST, panel);
			}

//			PamColors.getInstance().registerComponent(axisPanel,
//					PamColors.PamColor.BORDER);
			axisPanel.setSize(10, 10);
			axisPanel.setVisible(true);

			this.add(BorderLayout.CENTER, axisPanel);
		}

		setSize(900, 400);

		setVisible(true);

		addComponentListener(this);
	}

	void setBorderSize(EmptyBorder newBorder) {

		emptyBorder = newBorder;

	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentResized(ComponentEvent e) {
		repaint();
	}

	public void componentShown(ComponentEvent e) {
	}

	public PamFramePlots getFramePlots() {
		return framePlots;
	}

	public JPanel getPlotPanel() {
		return plotPanel;
	}

	public JPanel getAxisPanel() {
		return axisPanel;
	}

    public JPanel getLeftPlotPanel() {
        return leftPlotPanel;
}

    public JPanel getRightPlotPanel() {
        return rightPlotPanel;
    }
}
