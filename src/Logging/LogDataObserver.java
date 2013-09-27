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
package Logging;

import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JFileChooser;

import GPS.GpsData;
import GPS.GpsDataUnit;
import PamController.PamController;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;

public class LogDataObserver implements PamObserver {

	ArrayList<PamDataBlock> dataBlocks;

	GpsData gpsData;

	String logString;

	Calendar tempDate;

	long dateInMillis;

	LogToFlatFile logToFlatFile;

	boolean loggingActive = false;

	boolean logFileOpened = false;

	public LogDataObserver() {
		tempDate = Calendar.getInstance();

		dataBlocks = PamController.getInstance().getDataBlocks();
		logToFlatFile = new LogToFlatFile();

		// add this as an observer of the data block
		for (int i = 0; i < dataBlocks.size(); i++) {
			dataBlocks.get(i).addObserver(this);
		}
	}

	@Override
	public PamObserver getObserverObject() {
		return this;
	}

	public void openLogFile() {
		logToFlatFile.openNewLogFile();

		if (logToFlatFile.getState() == JFileChooser.APPROVE_OPTION) {
			loggingActive = true;
			logFileOpened = true;
		} else {
			loggingActive = false;
			logFileOpened = false;
		}
	}

	public void startLogging() {
		if (logToFlatFile.isFileOpened()) {
			loggingActive = true;
		} else {
			loggingActive = false;
		}
	}

	public void stopLogData() {
		loggingActive = false;
	}

	public void closeLogFile() {
		logToFlatFile.closeLogFile();
		logFileOpened = false;
	}

	public void update(PamObservable o, PamDataUnit arg) {
		PamDataBlock block = (PamDataBlock) o;
//		if (block.canLog() & loggingActive == true) {
//			logToFlatFile.logData(block.getLoggableString(arg));
//		} // else { /* System.out.println("Detection: Not Loggable."); */ }
	}

	public void noteNewSettings() {

	}

	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		// TODO Auto-generated method stub
		
	}

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		PamDataBlock block = (PamDataBlock) o;
		if (block.getUnitClass() == GpsDataUnit.class) {
			return 60 * 1000;
		}
		return 0;
	}
	
	public String getObserverName() {
		return "Data Logging observer";
	}

	public ArrayList<PamDataBlock> getDataBlocks() {
		return dataBlocks;
	}

	public boolean isLoggingActive() {
		return loggingActive;
	}

	public void setLoggingActive(boolean loggingActive) {
		this.loggingActive = loggingActive;
	}

	public boolean isLogFileOpened() {
		return logFileOpened;
	}

	public void removeObservable(PamObservable o) {
		// TODO Auto-generated method stub
		
	}

}
