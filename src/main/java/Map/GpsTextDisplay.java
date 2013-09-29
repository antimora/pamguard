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
package Map;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import GPS.GpsData;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamView.PamBorderPanel;
import PamView.PamColors;
import PamView.PamLabel;
import PamView.PamColors.PamColor;

public class GpsTextDisplay extends PamBorderPanel {
	// JTextArea title;
//	JTextArea gpsDisplay;

//	JTextArea cursorDataDisplay;

//	JTextArea vessel2CursorRangeDisplay;




	private LatLong lastMouseLatLong;

	private double mouseX;

	private double mouseY;

	private double vessel2CursorRange = 9;

	private double pixelsPerMetre;

//	Coordinate3d shipPosition;
	
	private double v2cAngle;
	
	private GpsData lastFix;

//	private GpsData shipGps;
	private MapController mapController;
	private SimpleMap simpleMap;
	
	private PamLabel latitude, longitude, time, date, course, heading, speed, 
	cursorLat, cursorLong, cursorRange, cursorBearing;
	private PamLabel cursorReference;
	private PamLabel lastFixTime;
	private PamBorderPanel gpsPanel;
	
	private TitledBorder gpsBorder, cursorBorder;
	

	/**
	 * 
	 */
	public GpsTextDisplay(MapController mapController, SimpleMap simpleMap) {
		super();
		this.simpleMap = simpleMap;
		this.mapController = mapController;
//		shipGps = null; //new GpsData();
//		shipPosition = new Coordinate3d();
		Font titleFont = new Font("Arial", Font.BOLD, 12);
		this.setOpaque(true);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		// this.setLayout(new GridLayout(0,1));
		// this.setLayout(new GridLayout(0,1));
		// t//his.setLayout(new LayoutManager(BoxLayout));
		// // TODO Auto-generated constructor stub

//		this.setPreferredSize(new Dimension(20, 50));
//		this.setBackground(new Color(156, 150, 95));
		
		GridBagConstraints c = new GridBagConstraints();
		gpsPanel = new PamBorderPanel();
		gpsPanel.setBorder(gpsBorder = BorderFactory.createTitledBorder("GPS data"));
		gpsPanel.setLayout(new GridBagLayout());
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		c.ipadx = 4;
		addComponent(gpsPanel, new PamLabel("Latitude"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Longitude"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Time"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Last Fix"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Date"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Course"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Heading"), c);
		c.gridy++;
		addComponent(gpsPanel, new PamLabel("Speed"), c);
		c.gridy++;
		c.gridx = 1;
		c.gridy = 0;
		addComponent(gpsPanel, latitude = new PamLabel(""), c);
		c.gridy++;
		addComponent(gpsPanel, longitude = new PamLabel(""), c);
		c.gridy++;
		addComponent(gpsPanel, time = new PamLabel(""), c);
		c.gridy++;
		addComponent(gpsPanel, lastFixTime = new PamLabel("  "), c);
//		lastFixTime.setBackground(Color.RED);
//		lastFixTime.setForeground(Color.RED);
		c.gridy++;
		addComponent(gpsPanel, date = new PamLabel(""), c);
		c.gridy++;
		addComponent(gpsPanel, course = new PamLabel(""), c);
		c.gridy++;
		addComponent(gpsPanel, heading = new PamLabel(""), c);
		c.gridy++;
		addComponent(gpsPanel, speed = new PamLabel(""), c);
		this.add(gpsPanel);
		
		PamBorderPanel cursorPanel = new PamBorderPanel();
		cursorPanel.setBorder(cursorBorder = BorderFactory.createTitledBorder("Cursor Position"));
		cursorPanel.setLayout(new GridBagLayout());
		c.anchor = GridBagConstraints.WEST;
		c.gridx = c.gridy = 0;
		c.ipadx = 4;
		addComponent(cursorPanel, new PamLabel("Latitude"), c);
		c.gridy++;
		addComponent(cursorPanel, new PamLabel("Longitude"), c);
		c.gridy++;
		c.gridwidth = 2;
//		addComponent(cursorPanel, new PamLabel("Range and bearing from ship"), c);
		c.gridy++;
		c.gridwidth = 1;
		addComponent(cursorPanel, new PamLabel("Bearing"), c);
		c.gridy++;
		addComponent(cursorPanel, new PamLabel("Range"), c);
		c.gridy++;
		c.gridx = 1;
		c.gridy = 0;
		addComponent(cursorPanel, cursorLat = new PamLabel(""), c);
		c.gridy++;
		addComponent(cursorPanel, cursorLong = new PamLabel(""), c);
		c.gridy+=2;
		addComponent(cursorPanel, cursorBearing = new PamLabel(""), c);
		c.gridy++;
		addComponent(cursorPanel, cursorRange = new PamLabel(""), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		addComponent(cursorPanel, cursorReference = new PamLabel(" "), c);
		this.add(cursorPanel);

//		gpsDisplay = new JTextArea();
//		gpsDisplay.setBorder(BorderFactory.createTitledBorder("GPS data"));
//		gpsDisplay.setText("No GPS received ....");
//		// gpsDisplay.setPreferredSize(new Dimension(120,100));
//		gpsDisplay.setBackground(new Color(238, 238, 238));

//		cursorDataDisplay = new JTextArea();
//		cursorDataDisplay.setBackground(new Color(238, 238, 238));
//		// cursorDataDisplay.setPreferredSize(new Dimension(120,40));
//		cursorDataDisplay.setBorder(BorderFactory
//				.createTitledBorder("Cursor position"));

//		vessel2CursorRangeDisplay = new JTextArea();
//		vessel2CursorRangeDisplay.setBackground(new Color(238, 238, 238));
//		// cursorDataDisplay.setPreferredSize(new Dimension(120,40));
//		vessel2CursorRangeDisplay.setBorder(BorderFactory
//				.createTitledBorder("Range data"));

		// this.add(title);
//		this.add(gpsDisplay);
		// this.add(mouseTitle);
//		this.add(cursorDataDisplay);
//		this.add(vessel2CursorRangeDisplay);
		// this.setBorder(BorderFactory.createRaisedBevelBorder());

		setBackground(getBackground());
	}

	public static void addComponent(JPanel panel, Component p, GridBagConstraints constraints){
		((GridBagLayout) panel.getLayout()).setConstraints(p, constraints);
		panel.add(p);
	}
	@Override
	public void setPreferredSize(Dimension preferredSize) {
		// TODO Auto-generated method stub
		super.setPreferredSize(preferredSize);
	}

	@Override
	public void setSize(Dimension d) {
		// TODO Auto-generated method stub
		super.setSize(d);
	}

	@Override
	public void setSize(int width, int height) {
		// TODO Auto-generated method stub
		super.setSize(width, height);
	}

	public void updateGpsTextArea() {
		GpsData gpsData = simpleMap.mapPanel.getShipGpsData(true);
		if (gpsData == null) return;
//		DecimalFormat f = new DecimalFormat();
//		DecimalFormat twoDigits = new DecimalFormat();
//		DecimalFormat threeDigits = new DecimalFormat();
//		twoDigits.setMinimumIntegerDigits(2);
//		threeDigits.setMinimumIntegerDigits(3);
//		DecimalFormat decMinutes = new DecimalFormat();
//		decMinutes.setMaximumFractionDigits(3);
//		decMinutes.setMaximumIntegerDigits(2);
//
//		f.setMinimumIntegerDigits(3);
//		f.setMinimumFractionDigits(6);
//		f.setMaximumFractionDigits(6);

//		Font bodyFont = new Font("Arial", Font.PLAIN, 12);

//		gpsDisplay.setText("");
//
//		gpsDisplay.setFont(bodyFont);
//
//		gpsDisplay.append("Latitude \t" + LatLong.formatLatitude(gpsData.getLatitude()) + "'\n");
//		gpsDisplay.append("Longitude \t" + LatLong.formatLongitude(gpsData.getLongitude())+ "'\n");

//		timeHrs = (int) Math.floor((double) (gpsData.getTime()) / 10000);
//		timeMins = (int) Math
//				.floor((double) ((gpsData.getTime() - timeHrs * 10000)) / 100);
//		timeSecs = gpsData.getTime() - timeHrs * 10000 - timeMins * 100;
//
//		dateDay = (int) Math.floor((double) (gpsData.getDate()) / 10000);
//		dateMonth = (int) Math
//				.floor((double) ((gpsData.getDate() - dateDay * 10000)) / 100);
//		dateYear = gpsData.getDate() - dateDay * 10000 - dateMonth * 100;

//		gpsDisplay.append("Time \t" + twoDigits.format(timeHrs) + ":"
//				+ twoDigits.format(timeMins) + ":" + twoDigits.format(timeSecs)
//				+ "\n");
//		gpsDisplay.append("Date \t" + twoDigits.format(dateDay) + "/"
//				+ twoDigits.format(dateMonth) + "/"
//				+ twoDigits.format(dateYear) + "\n");
//		gpsDisplay.append("True Course \t" + gpsData.getTrueCourse()
//				+ (char) 176 + "\n");
//		gpsDisplay.append("Speed \t" + twoDigits.format(gpsData.getSpeed()) + " knots \n");
		latitude.setText(LatLong.formatLatitude(gpsData.getLatitude()));
		longitude.setText(LatLong.formatLongitude(gpsData.getLongitude()));
		time.setText(PamCalendar.formatTime(gpsData.getTimeInMillis()));
		if (lastFix != null) {
			lastFixTime.setText(PamCalendar.formatTime(lastFix.getTimeInMillis()));
		}
		long timeFromFix = 0;
		if (lastFix != null) {
			timeFromFix = Math.abs(PamCalendar.getTimeInMillis() - lastFix.getTimeInMillis());
		}
		if (lastFix == null || timeFromFix > mapController.getMaxInterpolationTime()*1000) {
			gpsPanel.setBackground(PamColors.getInstance().getColor(PamColor.WARNINGBORDER));
			setBackground(PamColors.getInstance().getColor(PamColor.WARNINGBORDER));
		}
		else {
			gpsPanel.setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
			setBackground(PamColors.getInstance().getColor(PamColor.BORDER));
//			lastFixTime.setBackground(Color.green);
		}
		date.setText(PamCalendar.formatDate(gpsData.getTimeInMillis()));
		course.setText(String.format("%.1f \u00B0T", gpsData.getCourseOverGround()));
		// will have to try various options to find out the heading - may be none
		// or may be true of may be magnetic. 
		if (gpsData.getTrueHeading() != null) {
			heading.setText(String.format("%.1f \u00B0T", gpsData.getTrueHeading()));
		}
		else if (gpsData.getMagneticHeading() != null) {
			heading.setText(String.format("%.1f \u00B0M", gpsData.getMagneticHeading()));
		}
		else {
			heading.setText("-");
		}
		
		speed.setText(String.format("%.1f N", gpsData.getSpeed()));

	};
	public void updateGpsTextAreaWithStaticData(GpsData gpsData) {

//		Font bodyFont = new Font("Arial", Font.PLAIN, 12);

//		gpsDisplay.setText("Fixed Hydrophone array\n");
//
//		gpsDisplay.setFont(bodyFont);
//		gpsDisplay.append("Latitude \t" + LatLong.formatLatitude(gpsData.getLatitude()) + "\n");
//		gpsDisplay.append("Longitude \t" + LatLong.formatLongitude(gpsData.getLongitude())+ "\n\n");
//		
//		long timeMS = PamCalendar.getTimeInMillis();
//		gpsDisplay.append("Date \t" + PamCalendar.formatDate(timeMS) + "\n");
//		gpsDisplay.append("Time \t" + PamCalendar.formatTime(timeMS) + " GMT\n");

		latitude.setText(LatLong.formatLatitude(gpsData.getLatitude()));
		longitude.setText(LatLong.formatLongitude(gpsData.getLongitude()));
		time.setText(PamCalendar.formatTime(PamCalendar.getTimeInMillis()));
		date.setText(PamCalendar.formatDate(PamCalendar.getTimeInMillis()));
		course.setText(String.format("%.1f\u00B0", gpsData.getCourseOverGround()));
		speed.setText(String.format("%.1f", gpsData.getSpeed()));
	}
	

	public void displayZoomedorRotated() {
		if (lastMouseLatLong != null) {
			updateMouseCoords(lastMouseLatLong);
		}
	}
	
	public void updateMouseCoords(LatLong mouseLatLong) {
		if (mouseLatLong == null) {
			cursorRange.setText("           ");
			cursorBearing.setText("           ");
			return;
		}

		cursorLat.setText(LatLong.formatLatitude(mouseLatLong.getLatitude()));
		cursorLong.setText(LatLong.formatLongitude(mouseLatLong.getLongitude()));
		
		LatLong shipLatLong = simpleMap.mapPanel.getShipGpsData(true);

		vessel2CursorRange = shipLatLong.distanceToMetres(mouseLatLong);
		v2cAngle = shipLatLong.bearingTo(mouseLatLong);

		cursorRange.setText(String.format("%.0f m", vessel2CursorRange));
		cursorBearing.setText(String.format("%.1f \u00B0T", v2cAngle));
		
		String cursorRef = MasterReferencePoint.getReferenceName();
		if (cursorRef == null) {
			cursorReference.setText(" ");
		}
		else {
			cursorReference.setText(String.format("(re. %s)", cursorRef));
		}
		
		lastMouseLatLong = mouseLatLong;
		
	}
	public void mouseExited() {
		cursorLat.setText("              ");
		cursorLong.setText("");
		cursorBearing.setText("");
		cursorRange.setText("");
	}
	public void newShipGps() {
//		this.shipGps = shipGps;
	}

	public void setPixelsPerMetre(double pixelsPerMetre) {
		this.pixelsPerMetre = pixelsPerMetre;
	}

//	public void setShipPosition(Coordinate3d shipPosition) {
//		this.shipPosition = shipPosition;
//	}

	public void setMouseX(double mouseX) {
		this.mouseX = mouseX;
	}

	public void setMouseY(double mouseY) {
		this.mouseY = mouseY;
	}

public void copyMouseMapPositionToClipboard(double mouseLat, double mouseLon){
		
	
	Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
	
	cb.setContents(new LatLong(mouseLat, mouseLon)
//												.getTransferData(LatLong.latLongFlavor)
												, simpleMap.clipboardCopier);
	
		//TODO added this code to copy the last clicked lat long string to the clipboard - find a better way!! David McL
//		String mouseLLString = "Lat:" + LatLong.formatLatitude(mouseLat) 
//		+ ", Lon:" + LatLong.formatLongitude(mouseLon)
//		+ ", Range:"
//		+ (int) vessel2CursorRange + "m"
//		+ ", Bearing:" + (int) v2cAngle + "\u00B0";
////		System.out.println(mouseLLString);
//		JTextField tempField = new JTextField(mouseLLString);
//		tempField.selectAll();
//		tempField.copy();
	}

@Override
public void setBackground(Color bg) {
	// TODO Auto-generated method stub
	super.setBackground(bg);
	if (gpsBorder == null || cursorBorder == null) return;
	gpsBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
	cursorBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
}

public void setLastFix(GpsData lastFix) {
	this.lastFix = lastFix;
}


}