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
package PamView;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import PamController.PamController;
import PamUtils.Coordinate3d;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * 
 * @author Doug Gillespie
 *         <p>
 *         GeneralProjector is an abstract class used to draw any type of
 *         information contained within a PamDataBlock on a display (e.g. Map,
 *         spectrogram, etc).
 *         <p>
 *         The display must set the parameter types and the units (these lists
 *         can be added to as necessary)
 *         <p>
 *         Drawing is all done via calls that the display makes to
 *         PanelOverlayDraw.DrawDataUnit(...)
 *         <p>
 *         The DataBlocks using the projector will need to check parameter types
 *         and units when parsed a projector to check that they can provide
 *         these parameters.
 *         <p>
 *         The owning display will need to create concrete instances of
 *         GeneralProjector which implement getCoord3d. It is also possible that
 *         they will need to create other functions which are called by the
 *         display to set scaling parameters, scale offsets or other information
 *         Required by the projector that may change with time. If a display
 *         requires many types of similar projectors (e.g. different map
 *         projections) then it will be necessary to create a special interface
 *         to manage these functions and to implement that interface in all
 *         concrete classes used by that display.
 *         
 *         @see PamUtils.Coordinate3d
 *         @see PanelOverlayDraw
 *        
 * 
 */
public abstract class GeneralProjector {

	static public enum ParameterType {
		TIME, FREQUENCY, AMPLITUDE, LATITUDE, LONGITUDE, BEARING, RANGE, SLANTANGLE
	};

	static public enum ParameterUnits {
		SECONDS, HZ, DB, RAW, DECIMALDEGREES, METERS, NMILES, DEGREES, RADIANS
	};

	static public final int NPARAMETERS = 3;

	private ParameterType[] parmeterType = new ParameterType[NPARAMETERS];

	private ParameterUnits[] parmeterUnits = new ParameterUnits[NPARAMETERS];
	
	/**
	 * Flag to check that the clear function is being called, if not,
	 * then it will not be possible to add hover data to the list. 
	 */
//	private boolean isHoverClearing = false;
	
	private int minHoverDistance = 10;
	
//	List<Coordinate3d> hoverCoordinates = new Vector<Coordinate3d>();
//	
//	List<PamDataUnit> hoverDataUnits = new Vector<PamDataUnit>();
//	
//	List<Integer> dataUnitSide = new Vector<Integer>();
	private List<Coordinate3d> hoverCoordinates = Collections.synchronizedList(new LinkedList<Coordinate3d>());
	
	private List<PamDataUnit> hoverDataUnits = Collections.synchronizedList(new LinkedList<PamDataUnit>());
	
	private List<Integer> dataUnitSide = Collections.synchronizedList(new LinkedList<Integer>());
	
	private PamDataUnit hoveredDataUnit;
	
	protected boolean viewer = (PamController.getInstance().getRunMode()== PamController.RUN_PAMVIEW);

	/**
	 * Function ultimately used by a PamDataBlock to convert it's own data, in
	 * whatever form that is in into screen coordinates.
	 * 
	 * @param d1
	 *            d2 and d3 are data representing whatever is appropriate for
	 *            the concrete instance of the projector (e.g. Latitude,
	 *            Longitude, depth, Time Frequency, etc)
	 * @return A 3 dimensional coordinate (realistically z is never currently
	 *         used)
	 */
	abstract public Coordinate3d getCoord3d(double d1, double d2, double d3);

	/**
	 * Returns the prameter type for a specific data type required by
	 * Coordinate3d
	 * 
	 * @param iDim
	 *            Dimension number (0 - 2)
	 * @return enum ParameterType
	 */
	public ParameterType getParmeterType(int iDim) {
		return parmeterType[iDim];
	}

	/**
	 * Sets the parameter type for a specific data type required by Coordinate3d
	 * 
	 * @param iDim
	 *            dimension number (0 - 2)
	 * @param parmeterType
	 *            Parameter Type (see enum ParmaeterType)
	 */
	public void setParmeterType(int iDim, ParameterType parmeterType) {
		this.parmeterType[iDim] = parmeterType;
	}

	/**
	 * Returns the prameter unit for a specific data type required by
	 * Coordinate3d
	 * 
	 * @param iDim
	 *            Dimension number (0 - 2)
	 * @return enum ParameterUnit
	 */
	public ParameterUnits getParmeterUnits(int iDim) {
		return parmeterUnits[iDim];
	}

	/**
	 * Sets the parameter unit for a specific data type required by Coordinate3d
	 * 
	 * @param iDim
	 *            Dimension number (0 - 2)
	 * @param parmeterUnits
	 *            enum ParameterUnit
	 */
	public void setParmeterUnits(int iDim, ParameterUnits parmeterUnits) {
		this.parmeterUnits[iDim] = parmeterUnits;
	}
	
	MouseHoverAdapter mouseHoverAdapter;
	
	JComponent toolTipComponent;
	
	public MouseHoverAdapter getMouseHoverAdapter(JComponent component) {
		ToolTipManager tt = ToolTipManager.sharedInstance();
		tt.registerComponent(component);
		toolTipComponent = component;
		toolTipComponent.setToolTipText(null);
		if (mouseHoverAdapter == null) {
			mouseHoverAdapter = new MouseHoverAdapter(this);
		}
		return mouseHoverAdapter;
	}
	
	/**
	 * @return data unit currently being hovered, or null.
	 */
	public PamDataUnit getHoveredDataUnit() {
		return hoveredDataUnit;
	}

	class MouseHoverAdapter  implements ActionListener, MouseMotionListener, MouseListener {

		Timer timer;
		
		Point mousePoint = null;
		
		GeneralProjector generalProjector;
		
		
		public MouseHoverAdapter(GeneralProjector generalProjector) {
			super();
			this.generalProjector = generalProjector;
			timer = new Timer(10,this);
		}

		public void mouseExited(MouseEvent e) {
			hideHoverItem();
			timer.stop();
		}

		public void mouseMoved(MouseEvent e) {
			mousePoint = e.getPoint();
			hideHoverItem();
			timer.restart();
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseWheelMoved(MouseWheelEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
			timer.start();
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == timer) {
				showHoverItem();
			}
			
		}
		
		public boolean showHoverItem() {
			hideHoverItem();
			timer.stop();
			String hoverText = getHoverText(mousePoint);
			toolTipComponent.setToolTipText(hoverText);
			return (hoverText != null);
		}
		
		public void hideHoverItem() {
			timer.restart();
		}

		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseDragged(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	public String getHoverText(Point mousePoint) {
				hoveredDataUnit = null;
				if (mousePoint == null) return null;
	//			System.out.println("Mouse at (" + mousePoint.x + "," + mousePoint.y + ")");
//				timer.stop();
				int unitIndex = findClosestDataUnitIndex(new Coordinate3d(mousePoint.x, mousePoint.y));
				if (unitIndex < 0){
					return null;
				}
				hoveredDataUnit = hoverDataUnits.get(unitIndex);
				if (hoveredDataUnit == null) return null;
				PamDataBlock dataBlock = hoveredDataUnit.getParentDataBlock();
				if (dataBlock == null) return null;
				String hintText = dataBlock.getHoverText(this, hoveredDataUnit, dataUnitSide.get(unitIndex));
				if (hintText == null) return null;
	//			System.out.println(hintText);
				return hintText;
			}

	/**
	 * Any display that is using click hovering should call this at the start of their 
	 * paintComponent(Graphics g) function. The list will be repopulated as data are drawn in the
	 * PanelOverlayDraw implementations. 
	 *
	 *@see PanelOverlayDraw
	 */
	synchronized public void clearHoverList() {
//		isHoverClearing = true;
		hoverCoordinates.clear();
		hoverDataUnits.clear();
		dataUnitSide.clear();
	}

	/**
	 * 
	 * @param coordinate3d 3D coordinate of point plotted on map
	 * @param pamDataUnit corresponding data unit
	 * @return true
	 */
	synchronized public boolean addHoverData(Coordinate3d coordinate3d, PamDataUnit pamDataUnit) {
		return addHoverData(coordinate3d, pamDataUnit, 0);
	}
	
	/**
	 * 
	 * @param coordinate3d 3D coordinate of point plotted on map
	 * @param pamDataUnit corresponding data unit
	 * @param iSide either 0 or 1 (the index, not +/- 1) 
	 * @return true
	 */
	synchronized public boolean addHoverData(Coordinate3d coordinate3d, PamDataUnit pamDataUnit, int iSide) {
//		if (isHoverClearing == false) return false;
		hoverCoordinates.add(coordinate3d);
		hoverDataUnits.add(pamDataUnit);
		dataUnitSide.add(iSide);
		return true;
	}

	public int findClosestDataUnitIndex(Coordinate3d coordinate3d) {
		
		return findClosestDataUnitIndex(coordinate3d, minHoverDistance);
		
	}
	
	public int findClosestDataUnitIndex(Coordinate3d cTest, int minDistance) {
		/*
		 * Go through all stored coordinates and see which is closest. 
		 * 
		 */
		double closestDistance = Math.pow(minDistance,2);
		double dist;
		Coordinate3d c3d;
//		PamDataUnit closestUnit = null;
		int closestIndex = -1;
		
		Iterator<Coordinate3d> hoverIterator = hoverCoordinates.iterator();
//		for (int i = 0; i < hoverCoordinates.size(); i++) {
//			c3d = hoverCoordinates.get(i);
		int i = 0;
		while (hoverIterator.hasNext()) {
			c3d = hoverIterator.next();
			dist = Math.pow(c3d.x - cTest.x,2) + Math.pow(c3d.y - cTest.y,2) + Math.pow(c3d.z - cTest.z,2);
			if (dist <= closestDistance) {
				closestDistance = dist;
				closestIndex = i;
//				closestUnit = hoverDataUnits.get(i);
			}
			i++;
		}
		return closestIndex;
	}

	/**
	 * @return the viewer
	 */
	public boolean isViewer() {
		return viewer;
	}

}
