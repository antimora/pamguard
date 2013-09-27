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
package GPS;

import geoMag.MagneticVariation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import NMEA.AcquireNmeaData;
import PamUtils.LatLong;
import PamUtils.PamCalendar;

/**
 * @author Doug Gillespie, Paul Redmond, David McLaren
 * 
 */
public class GpsData extends LatLong {

	//	stream classdesc serialVersionUID = -7919081673367069167, local class serialVersionUID = -7607279743558807495
	static final long serialVersionUID = -7919081673367069167L;

	//	Scanner parser;

	//	private double latitude; // decimal degrees

	//	private double longitude; // decimal degrees

	private double speed;

	private boolean dataOk = false;

	/**
	 * renamed from heading
	 */
	private double courseOverGround; 

	/**
	 * renamed from trueCourse
	 */
	private Double trueHeading;

	private Double magneticHeading;

	private Double magneticVariation;

	//	private double variation;

	private int time;

	private int date;

	//private String timeString;

	//Date dateO;

	private int day= 0;

	private int month = 0;

	private int year = 0;

	private int hours = 0;

	private int mins = 0;

	private int secs = 0;

	private int millis = 0;

	private long timeInMillis;

	private Calendar gpsCalendar;

	private static GpsData lastGlobalGpsData = null; 

	private GpsData lastGpsData = null;

	private double distanceFromLast;

	public static final double METERSPERMILE = 1852.;

	private static final long millisPerHalfDay = 3600 * 1000 * 12;
	private static final long millisPerHour = 3600 * 1000;

	static private final int CHAROFFSET = 48;

	public GpsData() {

//		sortDistanceFromLast();

	};

	/**
	 * Constructor used in viewer and Mixed Mode 
	 * @param latitude latitude
	 * @param longitude longitude
	 * @param speed speed (knots)
	 * @param courseOverGround course over ground
	 * @param timeInMillis java millisecond time
	 * @param trueHeading true heading
	 * @param magneticHeading magnetic heading
	 * 
	 */
	public GpsData(double latitude, double longitude, double speed, 
			double courseOverGround, long timeInMillis, 
			Double trueHeading, Double magneticHeading) {
		super(latitude, longitude);
		this.speed = speed;
		this.courseOverGround = courseOverGround;
		this.timeInMillis = timeInMillis;
		this.trueHeading = trueHeading;
		this.magneticHeading = magneticHeading;
		dataOk = true;

		sortDistanceFromLast();
	}
	
	/**
	 * Used for buoy data received over the network. 
	 * @param latitude
	 * @param longitude
	 * @param timeInMillis
	 */
	public GpsData(double latitude, double longitude,  double height, long timeInMillis) {
		super(latitude, longitude, height);
		this.timeInMillis = timeInMillis;
		this.speed = 0;
		this.courseOverGround = 0;
		this.trueHeading = 0.;
		this.magneticHeading = 0.;
		dataOk = true;
	}

	public GpsData(StringBuffer nmeaString, int stringType, boolean isGlobal) {
		/*
		 * Unpack the string buffer to populate the above datas
		 */
		switch (stringType) {
		case GPSParameters.READ_GGA:
			unpackGGAString(nmeaString);
			break;
		default:
			unpackRMCString(nmeaString);
			break;
		}

		if (dataOk == false) return;

		if (isGlobal) {
			sortDistanceFromLast();
		}
	};

	private void sortDistanceFromLast() {

		lastGpsData = lastGlobalGpsData;

		if (lastGpsData != null) {
			distanceFromLast = this.distanceToMetres(lastGpsData);
		}

		lastGlobalGpsData = this;
	}


	private static GpsData previousGgaGps = null;
	private void unpackGGAString(StringBuffer nmeaString) {

		char[] nmeaSentence = new char[nmeaString.length()];
		nmeaString.getChars(0, nmeaString.length(), nmeaSentence, 0);
		int delimeterCount = 0;
		int[] delimeters = new int[nmeaSentence.length];

		delimeterCount = 0;
		for (int i = 0; i < nmeaSentence.length; i++) {
			if (nmeaSentence[i] == ',') {
				delimeters[delimeterCount++] = i;
			}
		}
		if (delimeterCount < 13) {
			return;
		}

		// pick out time
		dataOk = (unpackTime(nmeaSentence, delimeters[0], delimeters[1]) &&
				unpackLatitude(nmeaSentence, delimeters[1], delimeters[2], delimeters[3]) &&
				unpackLongitude(nmeaSentence, delimeters[3], delimeters[4], delimeters[5]));

		gpsCalendar = Calendar.getInstance();
		gpsCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		long now; 
		gpsCalendar.setTimeInMillis(now = PamCalendar.getTimeInMillis());
		gpsCalendar.set(Calendar.HOUR_OF_DAY, hours);
		gpsCalendar.set(Calendar.MINUTE, mins);
		gpsCalendar.set(Calendar.SECOND, secs);
		gpsCalendar.set(Calendar.MILLISECOND, millis);
		timeInMillis = gpsCalendar.getTimeInMillis();

		// check it's not a day out as we roll past midnight. 
		if (timeInMillis - now > millisPerHalfDay) {
			timeInMillis -= (millisPerHalfDay * 2);
		}
		if (timeInMillis - now < -millisPerHalfDay) {
			timeInMillis += (millisPerHalfDay * 2);
		}


		if (previousGgaGps != null) {
			double dist = previousGgaGps.distanceToMiles(this);
			double bear = previousGgaGps.bearingTo(this);
			speed = dist / (this.timeInMillis - previousGgaGps.timeInMillis) * millisPerHour;
			courseOverGround = bear;
		}
		previousGgaGps = this;
	}
	
	private boolean unpackTime(char[] nmeaSentence, int d1, int d2) {
		if (d2 - d1 < 7) {
			return false;
		}	

		time = (nmeaSentence[d1 + 1] - 48) * 100000
		+ (nmeaSentence[d1 + 2] - 48) * 10000
		+ (nmeaSentence[d1 + 3] - 48) * 1000
		+ (nmeaSentence[d1 + 4] - 48) * 100
		+ (nmeaSentence[d1 + 5] - 48) * 10
		+ (nmeaSentence[d1 + 6] - 48);

		hours = (nmeaSentence[d1 + 1] - 48) * 10
		+ (nmeaSentence[d1 + 2] - 48) * 1;

		mins = (nmeaSentence[d1 + 3] - 48) * 10
		+ (nmeaSentence[d1 + 4] - 48) * 1;

		secs = (nmeaSentence[d1 + 5] - 48) * 10
		+ (nmeaSentence[d1 + 6] - 48);

		if ((d2-d1) >= 10) {
			millis = (nmeaSentence[d1 + 8] - 48) * 100
			+ (nmeaSentence[d1 + 9] - 48) * 10;
		}

		return true;
	}
	private boolean unpackLatitude(char[] nmeaSentence, int d1, int d2, int d3) {

		double degrees, minutes;
		double scaleFac = 10;
		degrees = ((nmeaSentence[d1 + 1] - 48) * 10. 
				+(nmeaSentence[d1 + 2] - 48));
		minutes = (nmeaSentence[d1 + 3] - 48) * 10.
		+ (nmeaSentence[d1 + 4] - 48);
		for (int i = d1+6; i < d2; i++) {
			minutes += (nmeaSentence[i] - 48) / scaleFac;
			scaleFac *= 10;
		}
		latitude = degrees + minutes / 60.;

		// If there is one character between commas 3 and 4, set
		// latitude North or South
		if ('S' == nmeaSentence[d2 + 1]) {
			latitude = latitude * -1;
		}

		return true;
	}
	private boolean unpackLongitude(char[] nmeaSentence, int d1, int d2, int d3) {

		double degrees, minutes;
		double scaleFac = 10;
		degrees = (nmeaSentence[d1 + 1] - 48) * 100. 
		+ (nmeaSentence[d1 + 2] - 48) * 10. 
		+(nmeaSentence[d1 + 3] - 48);
		minutes = (nmeaSentence[d1 + 4] - 48) * 10.
		+ (nmeaSentence[d1 + 5] - 48);
		for (int i = d1+7; i < d2; i++) {
			minutes += (nmeaSentence[i] - 48) / scaleFac;
			scaleFac *= 10;
		}
		longitude = degrees + minutes / 60.;

		if (true) { //(delimeters[6] - delimeters[5]) == 2) {
			if ('W' == nmeaSentence[d2 + 1]) {
				longitude = longitude * -1;
			}
		}
		return true;
	}
	private boolean unpackVariation(char[] nmeaSentence, int d1, int d2) {
		if (d1 - d2 >= 4) {
			magneticVariation = unpackFloat(nmeaSentence, d1, d2);
			return true;
		}
		magneticVariation = null;
		return false;		
	}

	/**
	 * Unpack a floating point number between two deliminators
	 * @param nmeaSentence nmea Sentence
	 * @param d1 position of first ,
	 * @param d2 position of second ,
	 * @return unpacked number
	 */
	private double unpackFloat(char[] nmeaSentence, int d1, int d2) {
		//		work from d1 + 1 to d2 -1 watching out for the decimal point. 
		//		in fact, start by searching for the decimal point so we know how
		//		much to scale numbers in front of it by
		double number = 0;
		int newDigit;
		boolean foundPoint = false;
		double scaleFac = 10;
		for (int i = d1 + 1; i < d2; i++) {
			if (nmeaSentence[i] == '.') {
				foundPoint = true;
				continue;
			}
			if (foundPoint == false) {
				newDigit = (nmeaSentence[i] - 48);
				number = (number * 10 + newDigit);
			}
			else {
				number += ((nmeaSentence[i] - 48) / scaleFac);
				scaleFac *= 10;
			}
		}
		return number;
	}

	private void unpackRMCString(StringBuffer nmeaString) {


		char[] nmeaSentence = new char[nmeaString.length()];
		nmeaString.getChars(0, nmeaString.length(), nmeaSentence, 0);

		char[] timeChars = new char[6];

		String id = nmeaString.substring(0, 6);

		int delimeterCount = 0;
		int[] delimeters = new int[nmeaSentence.length]; 

		delimeterCount = 0;
		for (int i = 0; i < nmeaSentence.length; i++) {
			if (nmeaSentence[i] == ',') {
				delimeters[delimeterCount++] = i;
			}
		}
		if (delimeterCount < 11) {
			return;
		}

		gpsCalendar = Calendar.getInstance();

		// pick out time
		if ((delimeters[1] - delimeters[0]) >= 7) {
			time = (nmeaSentence[delimeters[0] + 1] - 48) * 100000
			+ (nmeaSentence[delimeters[0] + 2] - 48) * 10000
			+ (nmeaSentence[delimeters[0] + 3] - 48) * 1000
			+ (nmeaSentence[delimeters[0] + 4] - 48) * 100
			+ (nmeaSentence[delimeters[0] + 5] - 48) * 10
			+ (nmeaSentence[delimeters[0] + 6] - 48);

			hours = (nmeaSentence[delimeters[0] + 1] - 48) * 10
			+ (nmeaSentence[delimeters[0] + 2] - 48) * 1;

			mins = (nmeaSentence[delimeters[0] + 3] - 48) * 10
			+ (nmeaSentence[delimeters[0] + 4] - 48) * 1;

			secs = (nmeaSentence[delimeters[0] + 5] - 48) * 10
			+ (nmeaSentence[delimeters[0] + 6] - 48);

			if ((delimeters[1] - delimeters[0]) >= 10) {
				millis = (nmeaSentence[delimeters[0] + 8] - 48) * 100
				+ (nmeaSentence[delimeters[0] + 9] - 48) * 10;
			}
		}

		// pick out latitude
		//		if (true) { //(delimeters[3] - delimeters[2]) == 9) {
		double minFac = 10;
		double minutes;
		unpackLatitude(nmeaSentence, delimeters[2], delimeters[3], delimeters[4]);

		unpackLongitude(nmeaSentence, delimeters[4], delimeters[5], delimeters[6]);


		// PR: SPEED -  This version handles extra decimal precision found in the darwin DGPS.
		// Needs further testing.
		//		if (true) { //(delimeters[7] - delimeters[6]) > 0) {

		//		int numDigits = delimeters[7] - delimeters[6];
		speed = unpackFloat(nmeaSentence, delimeters[6], delimeters[7]);

		courseOverGround = unpackFloat(nmeaSentence, delimeters[7], delimeters[8]);

		//		int fpPosition = 0; 
		//		for (int i = 0; i< numDigits; i++) {
		//			if (nmeaSentence[delimeters[6] + i] == '.') {
		//				fpPosition = i;
		//			}
		//		}
		//
		//
		//		int decimalShift = 0;
		//		speed = 0.0;
		//		for (int i = 1; i< numDigits; i++) {
		//			if(i != fpPosition) {
		//
		//				decimalShift = (fpPosition - i);						
		//				if(i< fpPosition)
		//					decimalShift = decimalShift -1;
		//
		//				//System.out.println("decimal Shift: " + decimalShift);
		//				//System.out.print("value:" +nmeaSentence[delimeters[6] + i]);
		//				//System.out.println(", = " + ((nmeaSentence[delimeters[6] + i])-48) * (Math.pow(10, decimalShift)));
		//
		//				speed += ((nmeaSentence[delimeters[6] + i])-48) * (Math.pow(10.0, decimalShift));
		//			}
		//		}

		//System.out.println("gpsSpeed: " + speed);

		//		}

		//		// Darwin GPS
		//		// $GPRMC,084238,A,3519.490993,N,00636.062753,W,2.53,42,260306,,*11
		//		// PR: TRUE COURSE - REPLACES the version for speed but no time to check during Darwin cruise.
		//		// This version handles decimal place. But hasnt been tested with values after the dp. Suspect 
		//		// it might fail. Need to review all after Darwin anyway cause this is messy.
		////		if (true) { //(delimeters[8] - delimeters[7]) > 0) {
		//		numDigits = (delimeters[8] - delimeters[7])-1;
		//		fpPosition = numDigits+1; 
		//		for (int i = 0; i< numDigits; i++) {
		//			if (nmeaSentence[delimeters[7] + i] == '.') {
		//				fpPosition = i;
		//			}
		//		}
		//
		//		//	System.out.println("fpPosition: " + fpPosition);
		//		//	System.out.println("numDigits:" + numDigits);
		//		decimalShift = 0;
		//
		//		trueCourse = 0.0;
		//		decimalShift = fpPosition;	
		//		for (int i = 1; i <= numDigits; i++) {		
		//			if(i != fpPosition) {
		//				decimalShift--;// = (fpPosition - i);						
		//
		//				//System.out.println("decimal Shift: " + decimalShift);
		//				//System.out.print("value:" +nmeaSentence[delimeters[7] + i]);
		//				//System.out.println(", = " + ((nmeaSentence[delimeters[7] + i])-48) * (Math.pow(10, decimalShift)));
		//
		//				trueCourse += ((nmeaSentence[delimeters[7] + i])-48) * (Math.pow(10.0, decimalShift));
		//
		//			}
		//		}
		//		heading = trueCourse=trueCourse/10.0;


		magneticVariation = null;
		if (delimeters[10] - delimeters[9] >= 3) {
			unpackVariation(nmeaSentence, delimeters[9], delimeters[10]);
		}

		//		if (true) { // (delimeters[9] - delimeters[8]) == 7) {
		date = (nmeaSentence[delimeters[8] + 1] - 48) * 100000
		+ (nmeaSentence[delimeters[8] + 2] - 48) * 10000
		+ (nmeaSentence[delimeters[8] + 3] - 48) * 1000
		+ (nmeaSentence[delimeters[8] + 4] - 48) * 100
		+ (nmeaSentence[delimeters[8] + 5] - 48) * 10
		+ (nmeaSentence[delimeters[8] + 6] - 48);
		//	System.out.println("date: " + date);

		day = (nmeaSentence[delimeters[8] + 1] - 48) * 10
		+ (nmeaSentence[delimeters[8] + 2] - 48) * 1;

		month = (nmeaSentence[delimeters[8] + 3] - 48) * 10
		+ (nmeaSentence[delimeters[8] + 4] - 48) * 1 - 1;

		year = (nmeaSentence[delimeters[8] + 5] - 48) * 10
		+ (nmeaSentence[delimeters[8] + 6] - 48) + 2000;

		// //System.out.println("GpsData: day: " + day + " month: " +
		// month + " year:" + year);

		//		}

		gpsCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		gpsCalendar.set(year, month, day, hours, mins, secs);
		gpsCalendar.set(Calendar.MILLISECOND, millis);
		// gpsCalendar.set(5, 12, 8, 12, 32, 43);
		timeInMillis = gpsCalendar.getTimeInMillis();


		// //System.out.println(gpsCalendar.getTime());
		dataOk = (nmeaSentence[delimeters[1]+1] == 'A');

	}

	// TODO change to return char[] ?
	/*
	 * eg3. $GPRMC,220516,A,5133.82,N,00042.24,W,173.8,231.8,130694,004.2,W*70 1
	 * 2 3 4 5 6 7 8 9 10 1112  1:220516 Time Stamp  2:A validity - A-ok,
	 * V-invalid  3:5133.82 current Latitude  4:N North/South  5:00042.24 current
	 * Longitude  6: W East/West  7:173.8 Speed in knots  8:231.8 True course 
	 * 9:130694 Date Stamp  10: 004.2 Variation  11:W East/West 12:*70 checksum
	 */
	public String gpsDataToRMC(int nDecPlaces) {
		SimpleDateFormat gpsDateFormat = new SimpleDateFormat("ddMMyy");
		SimpleDateFormat gpsTimeFormat = new SimpleDateFormat("HHmmss");
		gpsDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		gpsTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		// System.out.println
		StringBuffer nmea = new StringBuffer("$GPRMC,");

		// 0: ID
		//		nmea.append("GPRMC,");

		// 1: TimeStamp
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(TimeZone.getTimeZone("GMT"));

		nmea.append(gpsTimeFormat.format(c.getTime()) + ",");

		// 2: Validity: A:OK, V:Invalid
		nmea.append("A,");

		nmea.append(formatNMEALatitude(nDecPlaces) + ',');
		nmea.append(formatNMEALongitude(nDecPlaces) + ',');


		// 7: Speed in knots
		nmea.append(String.format("%05.1f,", speed ));

		// 8: True course
		if (speed > -10) {
			nmea.append(String.format("%05.1f,", courseOverGround));
		}
		else {
			double randomCourse = Math.random() * 360;
			nmea.append(String.format("%.1f,", randomCourse));
		}

		// 9: Date Stamp
		nmea.append(gpsDateFormat.format(c.getTime()) + ",");

		// 10: Variation
		nmea.append(000.0 + ",");

		// 11: W East/West
		nmea.append("W");

		// 12: checksum
		int checkSum = AcquireNmeaData.createStringChecksum(nmea);
		nmea.append(String.format("*%02X", checkSum));

		//System.out.println("Fabricated String:" + nmea);
		return (nmea.toString());
	}

	static SimpleDateFormat gpsDateFormat = new SimpleDateFormat("ddMMyy");
	static SimpleDateFormat gpsTimeFormat = new SimpleDateFormat("HHmmss");

	/**
	 * Get the time formatted in the simple ddmmyy way
	 * @return formatted time
	 */
	private String getGpsTimeString() {
		gpsTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		return gpsTimeFormat.format(c.getTime());
	}

	/**
	 * Get the date formatted in the simple ddmmyy way
	 * @return formatted date
	 */
	private String getGpsDateString() {
		gpsDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		return gpsDateFormat.format(c.getTime());
	}

	/**
	 * Get the whole degrees
	 * @param latLong lat or long in decimal degrees
	 * @return integer degrees
	 */
	private int getWholeDegrees(double latLong) {
		return (int) Math.floor(Math.abs(latLong));
	}

	/**
	 * Get the whole number of degrees
	 * @param latLong
	 * @return
	 */
	private double getMinutes(double latLong) {
		latLong = Math.abs(latLong);
		return 60. * (latLong - getWholeDegrees(latLong));
	}

	/**
	 * 
	 * @return N or S
	 */
	private char getLatDirection() {
		if (latitude >= 0) {
			return 'N';
		}
		else {
			return 'S';
		}
	}

	/**
	 * 
	 * @return E or W
	 */
	private char getLongDirection() {
		if (longitude >= 0) {
			return 'E';
		}
		else {
			return 'W';
		}
	}

	/**
	 * formatted latitude string for simulated NMEA data. 
	 * @return formatted string
	 */
	private String formatNMEALatitude(int nDecPlaces) {
		// want something like "%02d%06.3f,%c"
		String formatString = String.format("%%02d%%0%d.%df,%%c", nDecPlaces+3, nDecPlaces);
		return String.format(formatString, getWholeDegrees(latitude), 
				getMinutes(latitude), getLatDirection());
	}

	private String formatNMEALongitude(int nDecPlaces) {
		// want something like "%03d%02.3f,%c"
		String formatString = String.format("%%03d%%0%d.%df,%%c", nDecPlaces+3, nDecPlaces);
		return String.format(formatString, getWholeDegrees(longitude), 
				getMinutes(longitude), getLongDirection());
	}


	public String gpsDataToGGA(int nDecPlaces) {

		// System.out.println
		StringBuffer nmea = new StringBuffer("$GPGGA,");

		nmea.append(getGpsTimeString() + ".00,");
		nmea.append(formatNMEALatitude(nDecPlaces) + ',');
		nmea.append(formatNMEALongitude(nDecPlaces) + ',');
		// fix quality
		nmea.append("8,");
		// n satellites
		nmea.append("00,");
		// horizontal dilution
		nmea.append("0.0,");
		// altitude, Meters, above mean sea level
		nmea.append("0.0,M,");
		// altitude, Height of geoid (mean sea level) above WGS84  ellipsoid
		nmea.append("0.0,M,");
		// (empty field) time in seconds since last DGPS update
		nmea.append(",");
		// (empty field) DGPS station ID number
		//		nmea.append(","); // don't need a ',' after the last field !

		// 12: checksum
		int checkSum = AcquireNmeaData.createStringChecksum(nmea);
		nmea.append(String.format("*%02X", checkSum));

		//System.out.println("Fabricated String:" + nmea);
		return (nmea.toString());
	}

	public void printGpsValues() {
		// System.out.println("1: TimeStampTime: " + time);
		// System.out.println("3: Latitude: " + latitude);
		// System.out.println("Longitude: " + longitude);

	}

	public long getTimeInMillis() {
		return timeInMillis;
	}

	public Calendar getGpsCalendar() {
		return gpsCalendar;
	}

	public int getDate() {
		return date;
	}

	/**
	 * Gets the best available data on the vessels true heading. If true heading
	 * data is available (e.g. from a Gyro compass), then that is returned. If 
	 * true heading is not available, then attempt to use magnetic heading (e.g. 
	 * from a fluxgate compass, which should be automatically corrected for magnetic 
	 * variation. Finally, if neither true or magnetic heading data are available, 
	 * just return course over ground from the GPS. Note that in this last case, the 
	 * data may be inaccurate at low speeds or in a cross current. 
	 * @return Best Heading in degrees relative to true North. 
	 */
	public double getHeading() {
		if (trueHeading != null) {
			return trueHeading;
		}
		else if (magneticHeading != null) {
			if (magneticVariation != null) {
				return (magneticHeading + magneticVariation); 
			}
			else {
				return magneticHeading;
			}
		}
		else {
			return courseOverGround;
		}
	}
	/**
	 * Gets the best available data on the vessels true heading. If true heading
	 * data is available (e.g. from a Gyro compass), then that is returned. If 
	 * true heading is not available, then attempt to use magnetic heading (e.g. 
	 * from a fluxgate compass, which should be automatically corrected for magnetic 
	 * variation. Finally, if neither true or magnetic heading data are available, 
	 * this function will either
	 *  return course over ground from the GPS of null depending on the
	 *  value of the noGPSCourse parameter. Note that in this last case, the 
	 * data may be inaccurate at low speeds or in a cross current. 
	 * @param noGPSCourse set true if you only want courses from proper heading sensors. 
	 * If noGPSSource is true and gyro or fluxgate data are unavailable null will be
	 * returned. 
	 * @return
	 */
	public Double getHeading(boolean noGPSCourse) {
		if (trueHeading != null) {
			return trueHeading;
		}
		else if (magneticHeading != null) {
			if (magneticVariation != null) {
				return (magneticHeading + magneticVariation); 
			}
			else {
				return magneticHeading;
			}
		}
		if (noGPSCourse) {
			return null;
		}
		else {
			return courseOverGround;
		}
	}

	/**
	 * @return  true heading read from a gyro or similar device. 
	 */
	public Double getTrueHeading() {
		return trueHeading;
	}

	/**
	 * @param trueHeading the trueHeading to set
	 */
	public void setTrueHeading(Double trueHeading) {
		this.trueHeading = trueHeading;
	}

	/**
	 * @param magneticHeading the magneticHeading to set
	 */
	public void setMagneticHeading(Double magneticHeading) {
		this.magneticHeading = magneticHeading;
		if (magneticVariation == null) {
			magneticVariation = MagneticVariation.getInstance().getVariation(this);
		}
	}

	/**
	 * 
	 * @return the magnetic heading (read from a fluxgate compass)
	 */
	public Double getMagneticHeading() {
		return magneticHeading;
	}

	/**
	 * Return the course over ground from the GPS. Note that this is the 
	 * direction the vessel is moving in relative to the earth and may not
	 * be the direction the vessel is pointing in. 
	 * @return the course over ground.
	 */
	public double getCourseOverGround() {
		return courseOverGround;
	}

	/**
	 * @param courseOverGround the courseOverGround to set
	 */
	public void setCourseOverGround(double courseOverGround) {
		this.courseOverGround = courseOverGround;
	}

	/**
	 * Returns the speed in knots
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Gets the speed in metres per second
	 * @return speed in metres per second
	 */
	public double getSpeedMetric() {
		return speed * METERSPERMILE / 3600;
	}

	public int getTime() {
		return time;
	}

	//	public double getTrueCourse() {
	//		return trueCourse;
	//	}


	//	public void setLatitude(double latitude) {
	//	this.latitude = latitude;
	//	}

	//	public void setLongitude(double longitude) {
	//	this.longitude = longitude;
	//	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	//
	//	public void setTrueCourse(double trueCourse) {
	//		this.trueCourse = trueCourse;
	//	}


	//	public int getDay() {
	//	return day;
	//	}

	//	public void setDay(int day) {
	//	this.day = day;
	//	}

	public int getHours() {
		return hours;
	}

	//	public void setHours(int hours) {
	//	this.hours = hours;
	//	}

	/**
	 * @return the magneticVariation
	 */
	public Double getMagneticVariation() {
		return magneticVariation;
	}

	/**
	 * @param magneticVariation the magneticVariation to set
	 */
	public void setMagneticVariation(Double magneticVariation) {
		this.magneticVariation = magneticVariation;
	}

	public int getMins() {
		return mins;
	}

	//	public void setMins(int mins) {
	//	this.mins = mins;
	//	}

	//	public int getMonth() {
	//	return month;
	//	}

	//	public void setMonth(int month) {
	//	this.month = month;
	//	}

	public int getSecs() {
		return secs;
	}

	public void setDate(int date) {
		this.date = date;
	}

	public void setGpsCalendar(Calendar gpsCalendar) {
		this.gpsCalendar = gpsCalendar;
	}

	//	public void setHeading(double heading) {
	//		this.heading = heading;
	//	}

	public void setTime(int time) {
		this.time = time;
	}

	public void setTimeInMillis(long timeInMillis) {
		this.timeInMillis = timeInMillis;
	}

	public double getDistanceFromLast() {
		return distanceFromLast;
	}

	public boolean isDataOk() {
		return dataOk;
	}

	public void setDataOk(boolean dataOk) {
		this.dataOk = dataOk;
	}

	/*
	 * Predict where the gps position will be at a given time
	 * based on the current speed and heading. 
	 */
	public GpsData getPredictedGPSData(long predictionTime) {
		GpsData predictedData = this.clone();
		if (getCourseOverGround() > 360) {
			// this happens with a lot of AIS data that does not have heading info.
			return predictedData;
		}
		double dt = (predictionTime - getTimeInMillis()) / 1000. / 3600;
		LatLong newPos = this.TravelDistanceMiles(getCourseOverGround(), getSpeed() * dt);
		predictedData.latitude = newPos.getLatitude();
		predictedData.longitude = newPos.getLongitude();
		predictedData.timeInMillis = predictionTime;
		return predictedData;
	}
	
	/**
	 * 
	 * Predict where the gps position will be at a given time
	 * based on the current speed and heading, but interpolating to some maximum 
	 * amount. 
	 * @param predictionTime time of prediction
	 * @param maxInterpMillis max milliseconds to interpolate. 
	 * @return Interpolated GPS position. 
	 */
	public GpsData getPredictedGPSData(long predictionTime, long maxInterpMillis) {
		GpsData predictedData = this.clone();
		if (getCourseOverGround() > 360) {
			// this happens with a lot of AIS data that does not have heading info.
			return predictedData;
		}
//		System.out.println(String.format("Predict GPS data from %s to %s", 
//				PamCalendar.formatDateTime(this.getTimeInMillis()), PamCalendar.formatDateTime(predictionTime)));
		double dt = (predictionTime - getTimeInMillis());
		if (dt > maxInterpMillis) {
			dt = maxInterpMillis;
		}
		if (dt < -maxInterpMillis) {
			dt = -maxInterpMillis;
		}
		dt /= (1000. * 3600.);
		LatLong newPos = this.TravelDistanceMiles(getCourseOverGround(), getSpeed() * dt);
		predictedData.latitude = newPos.getLatitude();
		predictedData.longitude = newPos.getLongitude();
		predictedData.timeInMillis = predictionTime;
		return predictedData;
	}

	@Override
	public GpsData clone() {
		return (GpsData) super.clone();
	}


}
