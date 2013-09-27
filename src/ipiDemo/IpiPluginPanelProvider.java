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

package ipiDemo;

/**
 * Display the signal processing output for the IPI module.
 * Modified from SpectrumPluginPanelProvider
 * @author Brian Miller
 */
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import echoDetector.EchoDataUnit;
import Layout.DisplayPanel;
import Layout.DisplayPanelContainer;
import Layout.DisplayPanelProvider;
import Layout.DisplayProviderList;
import Layout.PamAxis;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamCalendar;
import PamView.PamColors;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

public class IpiPluginPanelProvider implements DisplayPanelProvider {

	private IpiController ipiController;
	
	public IpiPluginPanelProvider(IpiController ipiController) {
		// hold a reference to the Controller running this display
		this.ipiController = ipiController;
		// tell the provider list that I'm available.
		DisplayProviderList.addDisplayPanelProvider(this);
	}

	public DisplayPanel createDisplayPanel(
			DisplayPanelContainer displayPanelContainer) {
		return new IpiPluginPanel(this, displayPanelContainer);
	}

	public String getDisplayPanelName() {
		return ipiController.getUnitName();
	}

	public class IpiPluginPanel extends DisplayPanel implements PamObserver,
			PamSettings {

		private IpiProcess ipiProcess;

		PamDataBlock<EchoDataUnit> ipiDataBlock;

		private IpiDataDisplayOptions plotOptions = new IpiDataDisplayOptions();

		private PamAxis westAxis;

		private PamAxis southAxis;

		int tempCounter = 0;

		double maxVal = .1;

		double minCepstrumVal = 0;

		double xMin, xMax;

		int x0, y0, x1, y1;

		long previousMillis = 0;

		long currentMillis = 0;

		int[] channelNumToIndex;

		public IpiPluginPanel(IpiPluginPanelProvider ipiPluginPanelProvider,
				DisplayPanelContainer displayPanelContainer) {
			super(ipiPluginPanelProvider, displayPanelContainer);
			ipiProcess = ipiPluginPanelProvider.ipiController.ipiProcess;
			ArrayList<PamDataBlock> echoDataBlocks = PamController
					.getInstance().getDataBlocks(EchoDataUnit.class, false);
			PamDataBlock<EchoDataUnit> ipiDataBlock = echoDataBlocks
					.get(ipiController.ipiProcessParameters.echoDataBlock);
			ipiDataBlock.addObserver(this);

			westAxis = new PamAxis(0, 0, 1, 1, getScaleMin(), getScaleMax(),
					true, "au", "%.0f");
			//westAxis.setInterval();
			southAxis = new PamAxis(0, 0, 1, 1, xMin, xMax, true, "IPI Delay (ms)", "%.0f");
			southAxis.setLabelPos(PamAxis.LABEL_NEAR_CENTRE);
			southAxis.setTickPosition(PamAxis.BELOW_RIGHT);
			southAxis.setFormat("%.1f");
			setupSouthAxis();
			x0 = 0;
			y0 = 0;
			getInnerPanel().addMouseMotionListener(new IpiPanelMouse());

			PamSettingManager.getInstance().registerSettings(this);
		}

		private double getScaleMax() {
			return plotOptions.maxVal;
		}

		private double getScaleMin() {
			return plotOptions.minVal;
		}

		@Override
		public PamAxis getWestAxis() {
			return westAxis;
		}

		@Override
		public PamAxis getSouthAxis() {
			return southAxis;
		}

		// JCheckBoxMenuItem menuAutoScale;
		JMenuItem scaleMenuItem;

		@Override
		protected JPopupMenu createPopupMenu() {
			// / TODO Auto-generated method stub
			JPopupMenu menu = new JPopupMenu();
			scaleMenuItem = new JMenuItem("Rescale axes");
			scaleMenuItem.addActionListener(new OptionsListener());
			menu.add(scaleMenuItem);
			return menu;
		}

		class OptionsListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				showDialog();
			}

		}

		void showDialog() {

			setAxisRange();

		}

		void setAxisRange() {
			westAxis.setRange(getScaleMin(), getScaleMax());
			setupSouthAxis();
			displayPanelContainer
					.panelNotify(DisplayPanelContainer.DRAW_BORDER);
		}

		@Override
		public void containerNotification(
				DisplayPanelContainer displayContainer, int noteType) {
			// TODO Auto-generated method stub

		}

		@Override
		public void destroyPanel() {

			if (ipiDataBlock != null) {
				ipiDataBlock.deleteObserver(this);
			}

		}

		public String getObserverName() {
			return "IPI plug in panel";
		}

		public long getRequiredDataHistory(PamObservable o, Object arg) {
			return 0;
		}

		public void noteNewSettings() {
			// TODO Auto-generated method stub
		}

		public void removeObservable(PamObservable o) {
			// TODO Auto-generated method stub

		}

		public void setSampleRate(float sampleRate, boolean notify) {
			// setupSouthAxis(sampleRate,xMin,xMax);
		}

		private void setupSouthAxis(){
			if (southAxis == null)
				return;
			double xMin = 0;
			double xMax = ipiProcess.getNumIpiSamples() / 
				ipiProcess.getSampleRate() * 1e3;
			
			southAxis.setRange(xMin, xMax);
			displayPanelContainer
					.panelNotify(DisplayPanelContainer.DRAW_BORDER);
		}
	
		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			// TODO Auto-generated method stub
		}

		public void update(PamObservable o, PamDataUnit arg) {
			// TODO Auto-generated method stub

			/* We're plotting the data every time a new echoDataUnit is ready.
			 * Instead we should actually package the IPI data into its own data
			 * unit, or call this function directly from ipiProcess.
			 */
			double[] ipiData = ipiController.ipiProcess
					.getSummedIpiFunction();

			int[] ipiHistogram = ipiController.ipiProcess.getIpiHistogram();
			
			currentMillis = PamCalendar.getTimeInMillis();
			if (currentMillis - previousMillis > 100) {
				previousMillis = currentMillis;
				Rectangle r = getPanel().getBounds();
				double xScale, yScale;
				BufferedImage image = getDisplayImage();
				if (image == null)
					return;
				this.clearImage();
				Graphics2D g2d = (Graphics2D) image.getGraphics();
				xScale = 2 * (double) r.width / (ipiData.length - 1);
				
				// First plot the ensemble averaged cepstrum
				yScale = r.height / ipiProcess.getIpiValueEnsembleAvg() * .8;
				g2d.setColor(PamColors.getInstance().getChannelColor(0));				
				g2d.drawString("Ensemble averaged cepstrum ",r.width - 200, 20);
				x0 = 0;
				y0 = r.height - (int) (yScale * getScaleMax() - getScaleMin());
				for (int j = 1; j < ipiData.length; j++) {
					x1 = (int) (j * xScale);
					double magVal = ipiData[j];
					y1 = r.height - (int) (yScale * magVal - getScaleMin());
					g2d.drawLine(x0, y0, x1, y1);
					x0 = x1;
					y0 = y1;
				}

				// Now plot a histogram of individual IPIs
				yScale = (double) r.height / ipiProcess.getMaxHistogramCount() *.8;
				g2d.setColor(PamColors.getInstance().getChannelColor(1));
				g2d.drawString("IPI delay histogram ",r.width - 200, 40);
				x0 = 0;
				y0 = r.height - (int) (yScale * getScaleMax() - getScaleMin());
				for (int j = 1; j < ipiData.length; j++) {
					x1 = (int) (j * xScale);
					double magVal = ipiHistogram[j];
					y1 = r.height - (int) (yScale * magVal - getScaleMin());
					g2d.drawLine(x0, y0, x1, y1);
					x0 = x1;
					y0 = y1;
				}
				repaint();
			}

		}

		public Serializable getSettingsReference() {
			return plotOptions;
		}

		public long getSettingsVersion() {
			return IpiDataDisplayOptions.serialVersionUID;
		}

		public String getUnitName() {
			return getDisplayPanelName();
			// return displayPanelProvider.getDisplayPanelName();
		}

		public String getUnitType() {
			return "IpiPluginPanelDisplayOptions";
		}

		public boolean restoreSettings(
				PamControlledUnitSettings pamControlledUnitSettings) {
			plotOptions = ((IpiDataDisplayOptions) pamControlledUnitSettings
					.getSettings()).clone();
			return true;
		}

		private void setMousePosition(Point pt) {
			double x = southAxis.getDataValue(pt.x); // scale is variable
			double y = westAxis.getDataValue(pt.y);
			this.getInnerPanel().setToolTipText(
					String.format("%1.3f ms, %3.1f au", x, y));
		}

		class IpiPanelMouse extends MouseAdapter {

			@Override
			public void mouseMoved(MouseEvent e) {
				setMousePosition(e.getPoint());
			}

		}

		@Override
		public PamObserver getObserverObject() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
