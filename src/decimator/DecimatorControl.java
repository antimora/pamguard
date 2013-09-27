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
package decimator;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pamScrollSystem.ViewLoadObserver;

import Acquisition.offlineFuncs.OfflineFileServer;
import PamController.OfflineRawDataStore;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RequestCancellationObject;

/**
 * @author Doug Gillespie
 * 
 * Quite a simple control unit that filters and decimates raw data producing a
 * new data stream
 * <p>
 * Needs a control dialog
 * 
 */
public class DecimatorControl extends PamControlledUnit implements PamSettings, OfflineRawDataStore {

	DecimatorParams decimatorParams = new DecimatorParams();
	
	DecimatorProcess decimatorProcess;

	DecimatorControl decimatorControl;
	
	private OfflineFileServer offlineFileServer;
	
	public DecimatorControl(String name) {
		
		super("Decimator", name);
		
		decimatorControl = this;

		isViewer = PamController.getInstance().getRunMode() == PamController.RUN_PAMVIEW;
		
//		
//
//		PamRawDataBlock rawDataBlock = null;
//		if (inputControl.GetPamProcess(0).GetOutputDataBlock(0).GetDataType() == DataType.RAW) {
//			rawDataBlock = (PamRawDataBlock) inputControl.GetPamProcess(0).GetOutputDataBlock(0);
//		}
		
		addPamProcess(decimatorProcess = new DecimatorProcess(this));

		PamSettingManager.getInstance().registerSettings(this);

		if (isViewer) {
			offlineFileServer = new OfflineFileServer(this);
		}
		
		decimatorProcess.newSettings();
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#CreateDetectionMenu(boolean)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + "...");
		menuItem.addActionListener(new DetectionMenu(parentFrame));
		return menuItem;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	class DetectionMenu implements ActionListener {
		Frame parentFrame;
		
		public DetectionMenu(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}
		
		public void actionPerformed(ActionEvent e) {
			DecimatorParams newParams = DecimatorParamsDialog.showDialog(parentFrame, decimatorControl, decimatorParams);
			if (newParams != null) {
				decimatorParams = newParams.clone();
				decimatorProcess.newSettings();
				if (isViewer) {
					offlineFileServer.createOfflineDataMap(parentFrame);
				}
			}
		}
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch(changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			if (isViewer) {
				offlineFileServer.createOfflineDataMap(PamController.getMainFrame());
			}
		}
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#GetSettingsReference()
	 */
	public Serializable getSettingsReference() {
		return decimatorParams;
	}


	/* (non-Javadoc)
	 * @see PamController.PamSettings#GetSettingsVersion()
	 */
	public long getSettingsVersion() {
		return DecimatorParams.serialVersionUID;
	}

	/* (non-Javadoc)
	 * @see PamController.PamSettings#RestoreSettings(PamController.PamControlledUnitSettings)
	 */
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		decimatorParams = ((DecimatorParams) pamControlledUnitSettings.getSettings()).clone();
				
		return true;
	}

	/* (non-Javadoc)
	 * @see PamguardMVC.PamControlledUnit#SetupControlledUnit()
	 */
	@Override
	public void setupControlledUnit() {
		// if there is no data block in the system, setup on the first one available. 
		PamDataBlock rawBlock = PamController.getInstance().getRawDataBlock(decimatorParams.rawDataSource);
		if (rawBlock == null) {
			rawBlock = PamController.getInstance().getRawDataBlock(0);
			if (rawBlock != null) decimatorParams.rawDataSource = rawBlock.toString();
		}
		super.setupControlledUnit();
		if (decimatorProcess != null) decimatorProcess.newSettings();
	}

	@Override
	protected boolean fillXMLParameters(Document doc, Element paramsEl) {
		paramsEl.setAttribute("newSampleRate", (new Double(decimatorParams.newSampleRate)).toString());
		Element filterEl = doc.createElement("Filter");
		decimatorParams.filterParams.fillXMLParameters(doc, filterEl);
		paramsEl.appendChild(filterEl);
		return true;
	}

	@Override
	public void createOfflineDataMap(Window parentFrame) {
		offlineFileServer.createOfflineDataMap(parentFrame);
	}
	@Override
	public String getDataSourceName() {
		return offlineFileServer.getDataSourceName();
	}
	@Override
	public boolean loadData(PamDataBlock dataBlock, long dataStart, long dataEnd, 
			RequestCancellationObject cancellationObject, ViewLoadObserver loadObserver) {
		return offlineFileServer.loadData(dataBlock, dataStart, dataEnd, cancellationObject, loadObserver);
	}
	@Override
	public boolean saveData(PamDataBlock dataBlock) {
		return offlineFileServer.saveData(dataBlock);
	}

	@Override
	public OfflineFileServer getOfflineFileServer() {
		return offlineFileServer;
	}

	@Override
	public PamProcess getParentProcess() {
		return decimatorProcess;
	}

	@Override
	public PamRawDataBlock getRawDataBlock() {
		// TODO Auto-generated method stub
		return decimatorProcess.getOutputDataBlock();
	}
}
