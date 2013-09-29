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
package PamguardMVC;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;


import PamModel.PamProfiler;
import PamUtils.SystemTiming;
import PamView.GeneralProjector;
import PamView.PamKeyItem;
import PamView.PanelOverlayDraw;

/**
 * @author Doug Gillespie
 *         <p>
 *         Similar functionality to the Observable Class, but works with the
 *         PamObserver interface which has a bit more functionality.
 *         <p>
 *         e.g. The PamDataBlock, which is the most common class of
 *         PamObservable asks the observers what the first required sample is
 *         for each observing process or view.
 */
public class PamObservable implements PanelOverlayDraw {

	static private final long LARGEST_SAMPLE = Long.MAX_VALUE;

	
	/**
	 * List of PamObservers
	 */
	protected List<PamObserver> pamObservers;

	private long cpuUsage[];
	private long lastCPUCheckTime = System.currentTimeMillis();
	private double cpuPercent[];

	protected boolean objectChanged = true;

	long totalCalls;

	private PamObserver longestObserver;
	
	/**
	 * Reference to a class that knows how to draw these things on any of the
	 * standard displays in the view, e.g. whistle contours, particular shapes
	 * on the map, etc.
	 */
	protected PanelOverlayDraw overlayDraw;
	
	protected PamProfiler pamProfiler;
	
	/**
	 * Sample numbers are now passed around to all observers. 
	 */
	protected long masterClockSample = 0;

	PamObservable() {
		pamObservers = new ArrayList<PamObserver>();
		pamProfiler = PamProfiler.getInstance();
		t.start();
	}

	/**
	 * Adds a PamObserver, which will then receive notifications when data is
	 * added. This is for single thread ops only 
	 * 
	 * @param o
	 *            Reference to the observer
	 */
	public void addObserver(PamObserver o) {
		// check each observer only observes once.
		if (pamObservers.contains(o) == false) {
			pamObservers.add(o);
			if (cpuUsage == null || cpuUsage.length < pamObservers.size()) {
				cpuUsage = new long[pamObservers.size()];
				cpuPercent = new double[pamObservers.size()];
			}
		}
	}
	
	public void addObserver(PamObserver o, boolean reThread) {
//		reThread = false;
		if (reThread == false) {
			addObserver(o);
			return;
		}

		/*
		 * need to check more carefully for a multi threaded observer. 
		 * if findThreadedObserver returns non null, then the observable
		 * is already being observed in some class or other. 
		 * <p>
		 * If we get null back, then add the new observer. 
		 */
		if (findThreadedObserver(o) == null) {
			addObserver(new ThreadedObserver(this, o));
		}
	}
	
	/**
	 * Go through the observer list and check inside any that
	 * are wrapped in threaded observers, 
	 * @param o
	 * @return reference to threadedobserver. 
	 */
	public ThreadedObserver findThreadedObserver(PamObserver o) {
		PamObserver pamObserver;
		ThreadedObserver threadedObserver;
		for (int i = 0; i < pamObservers.size(); i++) {
			pamObserver = pamObservers.get(i);
			if (pamObserver.getClass() == ThreadedObserver.class) {
				threadedObserver = (ThreadedObserver) pamObserver;
				if (threadedObserver.getSingleThreadObserver() == o) {
					return threadedObserver;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Go through all the threaded observers and wait for them to finish processing any
	 * data remaining in their lists. 
	 * @param timeOutms timeout in milliseconds
	 * @return false if the timeout time was reached before all threaded observers had
	 * emptied their queues. true if all queues had been emptied successfully. 
	 */
	public boolean waitForThreadedObservers(long timeOutms) {
		long startTime = System.currentTimeMillis();
		PamObserver pamObserver;
		ThreadedObserver threadedObserver;
		
		int waitingUnits;
		while(true) {
			waitingUnits = 0;
			for (int i = 0; i < pamObservers.size(); i++) {
				pamObserver = pamObservers.get(i);
				if (pamObserver.getClass() == ThreadedObserver.class) {
					threadedObserver = (ThreadedObserver) pamObserver;
					waitingUnits += threadedObserver.getInterThreadListSize();
					if (threadedObserver.isEmptyRead() == false) {
						waitingUnits++;
					}
				}
			}
//			System.out.println("Waiting data units = " + waitingUnits);
			if (waitingUnits == 0) {
				// no units waiting, so OK to get out.
				return true;
			}
			if (System.currentTimeMillis() - startTime > timeOutms) {
				// have taken too long, so return that we've failed. 
				System.out.println("Wait timeout in threaded observer");
				return false;
			}
			try {
				// wait a little and go check again to see if threaded observers have caught up.
				Thread.sleep(2);
			}
			catch (InterruptedException e) {
				
			}
		}
	}
	

	/**
	 * Removes an observer from the list
	 * 
	 * @param o
	 *            Observer to remove
	 */
	synchronized public void deleteObserver(PamObserver o) {
		/*
		 * Double check all instances are removed
		 * Though in principle, it should be impossible to add more than one
		 * instance of any observer
		 */
		while (pamObservers.remove(o));
		PamObserver threadedObserver = findThreadedObserver(o);
		if (threadedObserver != null) {
			deleteObserver(threadedObserver);
		}
	}

	/**
	 * Removes all observers from the list
	 */
	public void deleteObservers() {
		pamObservers.clear();
	}

	/**
	 * @return Count of PamObservers subscribing to this observable
	 */
	public int countObservers() {
		return pamObservers.size();
	}
	

	public List<PamObserver> getPamObservers() {
		return pamObservers;
	}

	/**
	 * Instruction to notify observers that data have changed, but not to send
	 * them any information about the new data.
	 */
	public void notifyObservers() {
		notifyObservers(null);
	}

	/**
	 * Set flag to say the data have changed
	 */
	public void setChanged() {
		objectChanged = true;
	}

	/**
	 * Set flag to say the data have changed
	 */
	public void clearchanged() {
		objectChanged = false;
	}

	/**
	 * Notify observers that data have changed and send them a reference to the
	 * new Data
	 * 
	 * Have tried to synchronise this, but it can cuase a lockup - not 100% sure what to do
	 * 
	 * 
	 * @param o
	 *            Reference to the new PamDataUnit
	 */
	 public void notifyObservers(PamDataUnit o) {
		int nObservers = countObservers();
		for (int i = 0; i < nObservers; i++) {
			// PR : This needs to be portable and currently isnt.
			//     TODO: catch the java.lang.UnsatisfiedLinkError
			//     maybe even test if running on windows, 
			//     if dll is available, and 
			//     perhaps running in a diagnostic mode.  			
			//     
			long cpuStart = SystemTiming.getProcessCPUTime();
			pamObservers.get(i).update(this, o);
			long cpuEnd = SystemTiming.getProcessCPUTime();
			cpuUsage[i] += (cpuEnd - cpuStart);
		}
		clearchanged();
	}

	Timer t = new Timer(1000, new ActionListener() {
		public void actionPerformed(ActionEvent evt) {
			long now = System.currentTimeMillis();
			if (cpuUsage == null) return;
			if (lastCPUCheckTime == now) return;
			for (int i = 0; i < cpuUsage.length; i++){
				cpuPercent[i] = (double) cpuUsage[i] / (now - lastCPUCheckTime) / 100.;
				cpuUsage[i] = 0;
			}
			lastCPUCheckTime = now;
		}
	});
	
	public double getCPUPercent(int objectIndex) {
		if (objectIndex < 0 || objectIndex >= cpuPercent.length) return -1;
		return cpuPercent[objectIndex];
	}
	
	public double getCPUPercent(PamObserver pamObserver) {
		return getCPUPercent(pamObservers.indexOf(pamObserver));
	}
	
	/**
	 * Goes through all observers and works out which requires data 
	 * the longest.
	 * @return Required data storage time in milliseconds. 
	 */
	public long getRequiredHistory() {

		long longestTime = 0;
		long rt;
		
		longestObserver = null;
		
		for (int i = 0; i < countObservers(); i++) {
			if ((rt = pamObservers.get(i).getRequiredDataHistory(this, null)) > 0) {
				if (rt > longestTime) {
					longestObserver = pamObservers.get(i);
				}
				longestTime = Math.max(longestTime, rt);
			}
		}
		totalCalls++;
		return longestTime;
	}

	public PamObserver getLongestObserver() {
		return longestObserver;
	}

	/**
	 * @param overlayDraw
	 *            Instance of a concrete class implementing the PanelIverlayDraw
	 *            interface.
	 *            <p>
	 *            Called to set the class which can draw overlays of the
	 *            PamDataUnits held in this data block.
	 */
	public void setOverlayDraw(PanelOverlayDraw overlayDraw) {
		this.overlayDraw = overlayDraw;
	}

	/**
	 * @param g
	 *            An awt Graphics object
	 *            <p>
	 * @param projectorInfo
	 *            Class implementing GeneralProjector which can convert data to
	 *            screen coordinates.
	 *            <p>
	 * @return a rectangle containing the pixels which will need to be
	 *         invalidated or redrawn by the calling process
	 *         <p>
	 *         Any PamObserver which has been notified by a PamObservable that a
	 *         new PamDatUnit has been created can call this function to draw
	 *         some arbitrary shape on the image, e.g. a whistle contour, a
	 *         picture of a whale, etc.
	 *         <p>
	 *         This should be overridden in the concrete class if it is to do
	 *         anything.
	 */
	public Rectangle drawDataUnit(Graphics g, PamDataUnit pamDataUnit,
			GeneralProjector projectorInfo) {
		if (overlayDraw != null) {
			return overlayDraw.drawDataUnit(g, pamDataUnit, projectorInfo);
		}
		return null;
	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see PamView.PanelOverlayDraw#CanDraw(PamView.GeneralProjector) Pass the
	 *      questin on to the OverlayDraw class if it exists .
	 */
	public boolean canDraw(GeneralProjector projectorInfo) {
		if (overlayDraw != null) {
			return overlayDraw.canDraw(projectorInfo);
		}
		return false;
	}
	
	public String getHoverText(GeneralProjector generalProjector, PamDataUnit dataUnit, int iSide) {
		if (overlayDraw != null) {
			return overlayDraw.getHoverText(generalProjector, dataUnit, iSide);
		}
		return null;
	}
	


	public PamKeyItem createKeyItem(GeneralProjector generalProjector,int keyType) {
		if (overlayDraw != null) {
			return overlayDraw.createKeyItem(generalProjector, keyType);
		}
		return null;
	}

	@Override
	public boolean hasOptionsDialog(GeneralProjector generalProjector) {
		if (overlayDraw != null) {
			return overlayDraw.hasOptionsDialog(generalProjector);
		}
		return false;
	}

	@Override
	public boolean showOptions(Window parentWindow,
			GeneralProjector generalProjector) {
		if (overlayDraw != null) {
			return overlayDraw.showOptions(parentWindow, generalProjector);
		}
		return false;
	}
}
