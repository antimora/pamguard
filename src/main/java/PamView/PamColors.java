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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;


/**
 * @author Doug Gillespie
 * 
 * Some standard colours to use for various bits of a view.
 * <p>
 * Ultimately, it should bepossible to set these dynamically during operation or
 * have night / day settings, etc.
 * <p>
 * Any bit of the display can register with a single instance of PamColors and
 * it will then receive notifications whenever any of the colours change.
 * 
 */
public class PamColors implements PamSettings {

	public static enum PamColor {
		PlOTWINDOW, BORDER, PLAIN, AXIS, GRID, MAP, WARNINGBORDER
	};
	

	static private PamColors singleInstance;

//	ArrayList<Component> componentList;

//	ArrayList<PamColor> colorList;

	private Color pamBorder = new Color(222, 222, 222); // poss increase to 220
	
	private Color pamWarningBorder = new Color(128, 0, 0);

	private Color pamPlotWindow = Color.WHITE;

	private Color plain = Color.BLACK;

	private Color axis = Color.BLACK;
	
	private Color grid = Color.BLACK;

	private Color[] channelColors = { Color.BLUE, Color.RED, Color.GREEN,
			Color.MAGENTA, Color.ORANGE, Color.BLACK, Color.CYAN, Color.PINK };
	
	private Color[] lineColors = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN,
			Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.PINK };
	
	private Color mapColor = new Color(180, 180, 255);
	
	public final int NWHALECOLORS = 13;
	
	private Color[] whaleColors = new Color[13];

	private MenuItemEnabler nightMenuEnabler = new MenuItemEnabler(); 
	private MenuItemEnabler dayMenuEnabler = new MenuItemEnabler(); 
	
	private ColorSettings colorSettings = new ColorSettings();

//	private Color standardPanelColour;
	
	private PamColors() {
//		componentList = new ArrayList<Component>();
//
//		colorList = new ArrayList<PamColor>();	
		/*
		 * Try to get a default colour from a JPanel
		 */
//		JPanel p = new JPanel();
//		standardPanelColour = p.getBackground();
		
		whaleColors[0] = new Color(0,0,0);
		whaleColors[1] = new Color(255, 0, 0);
		whaleColors[2] = new Color(0,255,0);
		whaleColors[3] = new Color(0,0,255);
		whaleColors[4] = new Color(255,0,255);
		whaleColors[5] = new Color(0,255,255);
		whaleColors[6] = new Color(255,128,0);
		whaleColors[7] = new Color(255,128,192);
		whaleColors[8] = new Color(192,192,192);
		whaleColors[9] = new Color(255,255,0);
		whaleColors[10] = new Color(44,167,146);
		whaleColors[11] = new Color(20,175,235);
		whaleColors[12] = new Color(135,67,165);
		
		setDayTime();

//		restoreBorderCol();
	

		getMenu();
		setDayTime();
		
		PamSettingManager.getInstance().registerSettings(this);
	}
//	void restoreBorderCol() {
//		/*
//		 * Try to get a default colour from a JPanel
//		 */
//		JPanel p = new JPanel();
//		Color c = p.getBackground();
//		pamBorder = c;
//	}
	public JMenuItem getMenu() {

		JMenu colorMenu = new JMenu("Color Scheme");
		JMenuItem dayMenuItem, nightMenuItem;
		
		dayMenuItem = new JCheckBoxMenuItem("Day");
		dayMenuItem.addActionListener(new menuColorDay());
		colorMenu.add(dayMenuItem);
		dayMenuEnabler.addMenuItem(dayMenuItem);
		
		nightMenuItem = new JCheckBoxMenuItem("Night");
		nightMenuItem.addActionListener(new menuColorNight());
		colorMenu.add(nightMenuItem);
		nightMenuEnabler.addMenuItem(nightMenuItem);
		
		return colorMenu;
	}
	
	class menuColorDay implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			setDayTime();
		}
	}
	
	class menuColorNight implements ActionListener {
		public void actionPerformed(ActionEvent ev){
			setNightTime();
		}
	}
	public void setNightTime() {

		pamBorder = Color.BLACK ;//new Color(68, 68, 68); // poss increase to 220
//		pamBorder = Color.green;
		
		pamWarningBorder = new Color(100, 0, 100);
		
        pamPlotWindow = new Color(32, 32, 32);

		plain = Color.WHITE;

		axis = Color.RED;

		grid = new Color(128, 0, 0);

		mapColor = new Color(18, 18, 32);

		channelColors = new Color[] { Color.BLUE, Color.RED, Color.GREEN,
				Color.MAGENTA, Color.ORANGE, Color.BLACK, Color.CYAN, Color.PINK };
		
		lineColors = new Color[] { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN,
				Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.PINK };

		whaleColors[0] = Color.WHITE;

		whaleColors[0] = new Color(192, 0, 0);
		whaleColors[1] = new Color(255, 255, 255);

		colorSettings.nightTime = true;
		
		setColors();
	
		
		nightMenuEnabler.selectItems(true);
		dayMenuEnabler.selectItems(false);
		nightMenuEnabler.enableItems(false);
		dayMenuEnabler.enableItems(true);
		
	}
	public void setDayTime() {

//		pamBorder = new Color(222, 222, 222); // poss increase to 220
		JPanel p = new JPanel();
		Color standardPanelColour = p.getBackground();
		pamBorder = standardPanelColour;
//		pamBorder = Color.red;
		
		pamWarningBorder = new Color(255, 0, 0);
		
        pamPlotWindow = Color.WHITE;

		plain = Color.BLACK;

		axis = Color.BLACK;

		grid = Color.BLACK;

		mapColor = new Color(180, 180, 255);

		channelColors = new Color[] { Color.BLUE, Color.RED, Color.GREEN,
				Color.MAGENTA, Color.ORANGE, Color.BLACK, Color.CYAN, Color.PINK };
		
		lineColors = new Color[] { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN,
				Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.PINK };

		whaleColors[0] = Color.BLACK;
		whaleColors[1] = new Color(255, 0, 0);

		colorSettings.nightTime = false;
		
		setColors();

		nightMenuEnabler.selectItems(false);
		dayMenuEnabler.selectItems(true);
		nightMenuEnabler.enableItems(true);
		dayMenuEnabler.enableItems(false);
	}

	static public PamColors getInstance() {
		if (singleInstance == null) {
			singleInstance = new PamColors();
		}
		return singleInstance;
	}

	public void notifyModelChanged(int changeType) {
		if (PamController.getInstance().isInitializationComplete() == false) {
			return;
		}
		switch (changeType) {
		case PamControllerInterface.ADD_CONTROLLEDUNIT:
		case PamControllerInterface.INITIALIZATION_COMPLETE:
		case PamControllerInterface.CHANGED_DISPLAY_SETTINGS:
			SwingUtilities.invokeLater(new SetColoursLater());
		}
	}
	
	class SetColoursLater implements Runnable {

		@Override
		public void run() {
			setColors();
		}
		
	}
//	
//	public void colorMeNow(Component component, PamColors.PamColor col) {
//		setColor(component, col);
//	}
//	public void registerComponent(Component component, PamColors.PamColor col) {
//		if (true) return;
//		componentList.add(component);
//		colorList.add(col);
//		setColor(component, col);
//	}
//
//	public void removeComponent(Component component) {
//		int index = componentList.indexOf(component);
//		componentList.remove(index);
//		colorList.remove(index);
//	}

	public void setColors() {
//		for (int i = 0; i < componentList.size(); i++) {
//			setColor(componentList.get(i), colorList.get(i));
//		}
		notifyAllComponents();
	}
	
	private void notifyAllComponents() {
		PamController pc = PamController.getInstance();
		if (pc == null) {
			return;
		}
		int nG = pc.getGuiFrameManager().getNumFrames();
		for (int i = 0; i < nG; i++) {
			notifyContianer(pc.getGuiFrameManager().getFrame(i));
		}
	}
	
	/** 
	 * Tells a container to set it's colour and the colour of 
	 * all it's components if they implement the ColorManaged
	 * interface. 
	 * <p>
	 * Generally this should be called initially for each frame 
	 * to start the iteration through all the swing components. 
	 * @param container container / or frame to start searching from 
	 */
	public void notifyContianer(Container container) {
		if (container == null) {
			return;
		}
//		System.out.println("in notifyContainer " + container);
		setColorManagedColor(container);
		int nC = container.getComponentCount();
		Component c;
		for (int i = 0; i < nC; i++) {
			c = container.getComponent(i);
			if (Container.class.isAssignableFrom(c.getClass())) {
//				System.out.println("Call sub container " + c);
				notifyContianer((Container) c);
			}
			else {
				setColorManagedColor(c);
			}
//			System.out.println("It's just a component " + c);
		}
	}
	
	private void setColorManagedColor(Component c) {
		if (ColorManaged.class.isAssignableFrom(c.getClass()) == false) {
			return;
		}
		setColor(c, ((ColorManaged) c).getColorId());
//		System.out.println("It's a colour managed component " + c);
	}



	/**
	 * Color a component immediately. 
	 * @param component
	 * @param col
	 */
	public void setColor(Component component, PamColor col) {
		if (col == null) {
			return;
		}
		component.setBackground(getColor(col));
		component.setForeground(getForegroudColor(col));
		component.repaint();
	}

	public Color getColor(PamColor col) {
		switch (col) {
		case BORDER:
			return pamBorder;
		case PlOTWINDOW:
			return pamPlotWindow;
		case PLAIN:
			return plain;
		case AXIS:
			return axis;
		case GRID:
			return grid;
		case MAP:
			return mapColor;
		case WARNINGBORDER:
			return pamWarningBorder;
		default:
			return plain;
		}
	}
	
	public Color getForegroudColor(PamColor col) {
		switch (col) {
		case BORDER:
			return axis;
		case PlOTWINDOW:
			return axis;
		case PLAIN:
			return axis;
		case AXIS:
			return axis;
		case GRID:
			return axis;
		default:
			return axis;
		}
	}
	
	public Color getWhaleColor(int col) {
		if (col >= whaleColors.length) {
			col = 1 + (col-1)%(whaleColors.length-1);
		}
		return whaleColors[col];
	}

	public Color getChannelColor(int iChan) {
		// gives back a colour - cycles if iChan is too big.
		int i = iChan;
		if (i < 0) i = 0;
		while (i >= channelColors.length) {
			i -= channelColors.length;
		}
		return channelColors[i];
	}
	
	static private Font boldFont;
	public Font getBoldFont() {
		if (boldFont == null) {
			boldFont = new Font("system", Font.BOLD, 12);
		}
		return boldFont;
	}
	
	public Serializable getSettingsReference() {
		return colorSettings;
	}

	public long getSettingsVersion() {
		return ColorSettings.serialVersionUID;
	}

	public String getUnitName() {
		return "Pam Color Manager";
	}

	public String getUnitType() {
		return "Pam Color Manager";
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		ColorSettings newSettings = (ColorSettings) pamControlledUnitSettings.getSettings();
		this.colorSettings.nightTime = newSettings.nightTime;
		if (colorSettings.nightTime) setNightTime();
		else setDayTime();
		
		
		return true;
	}

	public ColorSettings getColorSettings() {
		return colorSettings;
	}
	
	/**
	 * Interpret a colour string of the type used in Logger forms.
	 * <p>These can take two basic formats, first a colour name (e.g. blue)
	 * or a RGB code in the format RGB(RRR,GGG,BBB) where RRR, GGG and BBB
	 * are integer colour codes for red, green and blue each of which must lie
	 * between 0 and 255.
	 * @param colString Colour string
	 * @return colour or null if the colour cannot be interpreted. 
	 */
	public static Color interpretColourString(String colString) {
		if (colString == null) {
			return null;
		}
		colString = colString.toUpperCase();
		if (colString.equals("RED")) {
			return Color.RED;
		}
		else if (colString.equals("BLACK")) {
			return Color.BLACK;
		}
		else if (colString.equals("BLUE")) {
			return Color.BLUE;
		}
		else if (colString.equals("CYAN")) {
			return Color.CYAN;
		}
		else if (colString.equals("DARK_GRAY")) {
			return Color.DARK_GRAY;
		}
		else if (colString.equals("GRAY")) {
			return Color.GRAY;
		}
		else if (colString.equals("GREEN")) {
			return Color.GREEN;
		}
		else if (colString.equals("LIGHT_GRAY")) {
			return Color.LIGHT_GRAY;
		}
		else if (colString.equals("MAGENTA")) {
			return Color.MAGENTA;
		}
		else if (colString.equals("ORANGE")) {
			return Color.ORANGE;
		}
		else if (colString.equals("PINK")) {
			return Color.PINK;
		}
		else if (colString.equals("WHITE")) {
			return Color.WHITE;
		}
		else if (colString.equals("YELLOW")) {
			return Color.YELLOW;
		}
		
		Color aCol = null;
		try {
			aCol = Color.decode(colString);
		}
		catch (Exception e) {
			return null;
		}
		return aCol;
	}
}
