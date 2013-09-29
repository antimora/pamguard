package dataMap;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import PamController.PamController;
import PamController.PamSettingsGroup;
import PamController.PamSettingsSource;
import PamController.UsedModuleInfo;
import PamUtils.PamCalendar;
import PamView.JPanelWithPamKey;
import PamView.PamColors;
import PamView.PamSymbol;

/**
 * Display a simple strip along the top of the Scrolling data panel
 * which shows a little arrow or something for each set of PAMGAURD settings
 * @author Doug Gillespie
 *
 */
public class SettingsStrip extends JPanelWithPamKey {

	private static final int STRIPHEIGHT = 15;
	
	private ScrollingDataPanel scrollingDataPanel;
	
	private PamController pamController;
	
	private LinkedList<DrawnQuickFind> quickFinds = new LinkedList<DrawnQuickFind>();
	
	public SettingsStrip(ScrollingDataPanel scrollingDataPanel) {
		super();
		this.scrollingDataPanel = scrollingDataPanel;
		pamController = PamController.getInstance();
		
		SettingsMouse sm = new SettingsMouse();
		addMouseListener(sm);
		addMouseMotionListener(sm);
	}

	public int getStripHeight() {
		return STRIPHEIGHT;
	}

	public void scrollChanged() {
		repaint();		
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		quickFinds.clear();
		ArrayList<PamSettingsSource> settingsSources = pamController.findSettingsSources();
		int totalSettings = 0;
		for (int i = 0; i < settingsSources.size(); i++) {
			totalSettings += paintSettings(g, settingsSources.get(i), i);
		}
		if (totalSettings == 0) {
			FontMetrics fm = g.getFontMetrics();
			String str = "No settings information available";
			g.drawString(str, (getWidth() - fm.stringWidth(str))/2, 
					(getHeight()+fm.getHeight())/2);
		}
	}
	double pixelsPerMilli = 1;
	long startMillis;
	
	private int paintSettings(Graphics g, PamSettingsSource settingsSource, int index) {
		double pixsPerHour = scrollingDataPanel.getPixelsPerHour();
		pixelsPerMilli = pixsPerHour / 3600 / 1000;
		startMillis = scrollingDataPanel.getScreenStartMillis();
		long endMillis = scrollingDataPanel.getScreenEndMillis();
		int n = settingsSource.getNumSettings();
		PamSettingsGroup settingsGroup;
		long settingsTime;
		int x, y;
		Color col = PamColors.getInstance().getChannelColor(index);
		PamSymbol symbol = new PamSymbol(PamSymbol.SYMBOL_TRIANGLED, STRIPHEIGHT, 12, true, col, Color.BLACK);
		for (int i = 0; i < n; i++) {
			settingsSource.getSettings(i);
			settingsGroup = settingsSource.getSettings(i);
			settingsTime = settingsGroup.getSettingsTime();
			if (settingsTime < startMillis) {
				continue;
			}
			else if (settingsTime > endMillis) {
				break;
			}
			x = (int) ((settingsTime - startMillis) * pixelsPerMilli);
			y = 5;
			symbol.draw(g, new Point(x,y));
			quickFinds.add(new DrawnQuickFind(symbol.getDrawnPolygon(), settingsGroup));
		}
		return n;
	}

	class SettingsMouse extends MouseAdapter {
		@Override
		public void mouseMoved(MouseEvent me) {
			DrawnQuickFind dq = findQuickFind(me.getPoint());
			if (dq != null) {
				createHoverText(dq);
			}
			else {
				createHoverText(me.getX());
			}
		}

		@Override
		public void mousePressed(MouseEvent me) {
			if (me.isPopupTrigger()) {
				showPopupMenu(me);
			}
		}

		@Override
		public void mouseReleased(MouseEvent me) {
			if (me.isPopupTrigger()) {
				showPopupMenu(me);
			}
		}
	}

	public void showPopupMenu(MouseEvent me) {
		DrawnQuickFind dq = findQuickFind(me.getPoint());
		if (dq == null) {
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem menuItem;
		menuItem = new JMenuItem("Load these settings from " + PamCalendar.formatDateTime(dq.settingsGroup.getSettingsTime()));
		menuItem.addActionListener(new LoadMenuItem(dq.settingsGroup));
		menu.add(menuItem);
		menu.show(this, me.getX(), me.getY());
		
	}
	
	class LoadMenuItem implements ActionListener {
		
		PamSettingsGroup settingsGroup;

		public LoadMenuItem(PamSettingsGroup settingsGroup) {
			super();
			this.settingsGroup = settingsGroup;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			pamController.loadOldSettings(settingsGroup);
		}
		
	}

	/**
	 * Make a hover text containing information about the settings 
	 * modules list. 
	 * @param dq
	 */
	private void createHoverText(DrawnQuickFind dq) {
		PamSettingsGroup settingsGroup = dq.settingsGroup;
		String str = String.format("<html><u><b><center>%s</center></b></u>", PamCalendar.formatDateTime(settingsGroup.getSettingsTime()));
		ArrayList<UsedModuleInfo> mi = settingsGroup.getUsedModuleInfo();
		boolean have, available;
		Class moduleClass = null;
		UsedModuleInfo aModuleInfo;
		for (int i = 0; i < mi.size(); i++) {
			aModuleInfo = mi.get(i);
			try {
				moduleClass = Class.forName(aModuleInfo.className);
				available = true;
			} catch (ClassNotFoundException e) {
				moduleClass = null;
				available = false;
			}
			have = (pamController.findControlledUnit(moduleClass, aModuleInfo.unitName) != null);
			if (available == false) {
				str += String.format("<p><span style=\"color:red\">%s (unavailable)</span>", aModuleInfo.unitName);
			}
			else if (have == false) {
				str += String.format("<p><b>%s (not present)</b>", aModuleInfo.unitName);
			}
			else {
				str += String.format("<p>%s", mi.get(i).unitName);
			}
		}
		str +="<p><b><center>(Right click for more options)</center></b>";
		str += "</html>";
		setToolTipText(str);
	}

	/**
	 * convert x to a time and make a simple hover text. 
	 * @param x
	 */
	private void createHoverText(int x) {
		long t = (long)(startMillis + x/pixelsPerMilli);
		setToolTipText(PamCalendar.formatDateTime(t));
	}

	DrawnQuickFind findQuickFind(Point pt) {
		ListIterator<DrawnQuickFind> iterator = quickFinds.listIterator();
		DrawnQuickFind dq;
		while (iterator.hasNext()) {
			dq = iterator.next();
			if (dq.polygon.contains(pt)) {
				return dq;
			}
		}
		return null;
	}
	
	/**
	 * Class to enable quick retrieval of settings information as the 
	 * mouse hovers over the display. 
	 * @author Doug gillespie
	 *
	 */
	class DrawnQuickFind {
		private Polygon polygon;
		private PamSettingsGroup settingsGroup;
		public DrawnQuickFind(Polygon polygon, PamSettingsGroup settingsGroup) {
			super();
			this.polygon = polygon;
			this.settingsGroup = settingsGroup;
		}
	}
}
