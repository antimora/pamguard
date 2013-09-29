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
package PamUtils;

import java.io.File;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JOptionPane;

import PamController.PamController;


/**
 * @author Doug Gillespie
 * 
 * Date and time in Pamguard are critical. It's likely that we'll be taking time
 * from the GPS in the longer term, so for now, make sure that all calendar
 * functions come via this
 * 
 */
public class PamCalendar {

	private static TimeZone defaultTimeZone = TimeZone.getTimeZone("UTC");

	public static final long millisPerDay = 1000L*24L*3600L;
	
	/**
	 * true if data are from a file based data source, false
	 * if the data are arriving in real time. If analysing 
	 * file data, times are based on the file start time (if known)
	 * and the position of the read pointer within the file. 
	 */
	private static boolean soundFile;

	/**
	 * time from the start of the file to the currentmoment. 
	 * This is updated every time data re read from the file, so is
	 * accurate to about 1/10 second. 
	 * For accurate timing within detectors, always try to use sample number
	 * and count samples from the start time for the detector.
	 */
	private static long soundFileTimeInMillis;

	/**
	 * Time that data processing started - can be set to a file time
	 * when files are being processed, otherwise it's just the current time. 
	 */
	private static long sessionStartTime;

	/**
	 * When running in viewer mode, use the sessionStartTime and the viewEndtime
	 * to control the calendar. 
	 */
	private static long viewEndTime;

	/** 
	 * view is controlled by a slider which sets the 
	 * viewPositions which is the number of milliseconds from the sessionsStartTime.
	 */
	private static long viewPosition;


	/**
	 * If files are being analysed, return the time based on the file
	 * position. Otherwise just take the normal system time.
	 * @return time in milliseconds
	 */
	static public long getTimeInMillis() {

		// Date d = new Date();
		// return d.getTime();
		switch (PamController.getInstance().getRunMode()) {
		case PamController.RUN_NORMAL:
		case PamController.RUN_MIXEDMODE:
			if (soundFile) {
				return sessionStartTime + soundFileTimeInMillis;
			} else {
				return System.currentTimeMillis();
			}
		case PamController.RUN_PAMVIEW:
			return sessionStartTime + viewPosition;
		case PamController.RUN_NETWORKRECEIVER:
			return System.currentTimeMillis();
		}
		return System.currentTimeMillis();
	}

	/**
	 * 	a formatted time string
	 */
	static public long getTime() {

		// Date d = new Date();
		// return d.getTime();
		return getTimeInMillis();

	}

	/**
	 * 
	 * @return a formatted date string
	 */
	static public String getDate() {

		//		Date d = new Date();
		//		return d.toString();
		return formatDateTime(getTimeInMillis());

	}

	/**
	 * Compares two times in milliseconds to see if they are on the same day or not. 
	 * @param t1 first time
	 * @param t2 second time
	 * @return true if times are on the same day
	 */
	public static final boolean isSameDay(long t1, long t2) {
		long d1 = t1/millisPerDay;
		long d2 = t2/millisPerDay;
		return d1 == d2;
	}

	/**
	 * Get the current date
	 * @return the date as a Calendar object (in GMT)
	 */
	static public Calendar getCalendarDate() {

		Calendar c =  Calendar.getInstance();
		c.setTimeZone(defaultTimeZone);
		c.setTimeInMillis(getTimeInMillis());
		return c;

	}	

	/**
	 * Get the date for a given time 
	 * @param timeInMillis time in milliseconds
	 * @return the date as a Calendar object (in GMT)
	 */
	public static Calendar getCalendarDate(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(defaultTimeZone);
        return c;
    }

	/**
	 * Get a formatted date and time string. 
	 * @param date Date
	 * @return formatted String
	 */
	public static String formatDateTime(Date date) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.UK);
		df.setTimeZone(defaultTimeZone);
		return df.format(date);
	}
	
	/**
	 * Get a formatted local date and time string. 
	 * @param date Date
	 * @return formatted String
	 */
	public static String formatLocalDateTime(Date date) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
		return df.format(date);
	}

	/**
	 * Formats the time and data in a long format
	 * @param timeInMillis time in milliseconds
	 * @return formated data and time
	 */
	public static String formatDateTime(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		return formatDateTime(c.getTime());
	}
	/**
	 * Formats the local time and data in a long format
	 * @param timeInMillis time in milliseconds
	 * @return formated data and time
	 */
	public static String formatLocalDateTime(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		return formatLocalDateTime(c.getTime());
	}

	/**
	 * Formats the time and data in a long format
	 * but without the GMT label at the end. 
	 * @param timeInMillis time in milliseconds
	 * @return formated data and time
	 */
	public static String formatDateTime2(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(defaultTimeZone);

		DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();

		return df.format(d);
	}

	/**
	 * Formats the date and time in the correct format for database output. 
	 * <p>"yyyy-MM-dd HH:mm:ss"
	 * @param timeInMillis time in milliseconds
	 * @return formated data and time
	 */
	public static String formatDBDateTime(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(defaultTimeZone);

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		//		return String.format("%tY-%<tm-%<td %<tH:%<tM:%<tS", d);

		return df.format(d);
	}
	/**
	 * Formats the date and time in the correct format for database output. 
	 * <p>"yyyy-MM-dd"
	 * @param timeInMillis time in milliseconds
	 * @return formated data and time
	 */
	public static String formatDBDate(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(defaultTimeZone);

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		//		return String.format("%tY-%<tm-%<td %<tH:%<tM:%<tS", d);

		return df.format(d);
	}

	/**
	 * Get a formatted string in the correct format to include in database queries
	 * <p>e.g. {ts '2012-06-25 17:22:54'}
	 * @param timeMillis time in milliseconds
	 * @return formatted string
	 */
	public static String formatDBDateTimeQueryString(long timeMillis) {
		return "{ts '" + formatDBDateTime(timeMillis) + "'}";
	}

	/**
	 * Format a time string in the format HH:MM:DD
	 * @param timeMillis time in milliseconds
	 * @return formatted string
	 */
	public static String formatTime(long timeMillis) {
		return formatTime(timeMillis, false);
	}

	/**
	 * Format a time string optionally showing the milliseconds with
	 * a given precision for UTC time zone
	 * @param timeMillis time in milliseconds
	 * @param millisDigits number of millisecond decimal places. 
	 * @return formatted time string. 
	 */
	public static String formatTime(long timeMillis, boolean showMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		c.setTimeZone(defaultTimeZone);
		String formatString = "HH:mm:ss";
		if (showMillis) {
			formatString += ".SSS";
		}
		DateFormat df = new SimpleDateFormat(formatString);
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		return df.format(d);
	}
	
	/**
	 * Format a time string optionally showing the milliseconds with
	 * a given precision for UTC time zone
	 * @param timeMillis time in milliseconds
	 * @param millisDigits number of millisecond decimal places. 
	 * @return formatted time string. 
	 */
	public static String formatLocalTime(long timeMillis, boolean showMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		String formatString = "HH:mm:ss";
		if (showMillis) {
			formatString += ".SSS";
		}
		DateFormat df = new SimpleDateFormat(formatString);
		Date d = c.getTime();
		return df.format(d);
	}

	/**
	 * Format a time string optionally showing the milliseconds with
	 * a given precision.  The time string is formatted as HH:mm:ss.SSSSS.
	 * @param timeMillis time in milliseconds
	 * @param millisDigits number of millsecond decimal places.
	 * @return formatted time string.
	 */
	public static String formatTime(long timeMillis, int millisDigits) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		String formatString = "HH:mm:ss";
        if (millisDigits > 0) {
            formatString += ".";
            for (int i=1; i<=millisDigits; i++) {
                formatString += "S";
            }
        }
		DateFormat df = new SimpleDateFormat(formatString);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = c.getTime();
		return df.format(d);
	}

	/**
	 * Format a time string optionally showing the milliseconds with
	 * a given precision.  The time string is formatted as HHmmss.SSSSS.
	 * @param timeMillis time in milliseconds
	 * @param millisDigits number of millsecond decimal places.
	 * @return formatted time string.
	 */
	public static String formatTime2(long timeMillis, int millisDigits) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		String formatString = "HHmmss";
        if (millisDigits > 0) {
            formatString += ".";
            for (int i=1; i<=millisDigits; i++) {
                formatString += "S";
            }
        }
		DateFormat df = new SimpleDateFormat(formatString);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = c.getTime();
		return df.format(d);
	}



	/**
	 * Format a time in milliseconds as a number of days / seconds, etc. 
	 * @param timeInMillis time in milliseconds. 
	 * @return formatted time interval
	 */
	public static String formatDuration(long timeInMillis) {
		long aDay = 3600 * 24 * 1000;
		if (timeInMillis < aDay) {
			return formatTime(timeInMillis);
		}
		long days = (int) Math.floor(timeInMillis / aDay);
		long millis = timeInMillis - days * aDay;
		if (days == 1) {
			return String.format("%d day %s", days, formatTime(millis));
		}
		else {
			return String.format("%d days %s", days, formatTime(millis));
		}
	}

	/**
	 * Format the data in the dd MMMM yyyy format
	 * @param timeInMillis time in milliseconds
	 * @return formatted string. 
	 */
	public static String formatDate(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(defaultTimeZone);
		DateFormat df = new SimpleDateFormat("dd MMMM yyyy");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		return df.format(d);
		//		return String.format("%td %th \'%ty", c.getTime(),c.getTime(),c.getTime());
	}

	/**
	 * Format the data in the ddMMyy format
	 * @param timeInMillis time in milliseconds
	 * @return formatted string.
	 */
	public static String formatDate2(long timeInMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeInMillis);
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		DateFormat df = new SimpleDateFormat("ddMMyy");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = c.getTime();
		return df.format(d);
	}

	/**
	 * Format date in format "yyyyMMdd"
	 * @param timeMillis
	 * @return
	 */
	public static String formatFileDate(long timeMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		c.setTimeZone(defaultTimeZone);
		//		Date date = c.getTime();
		//		String.format("%4.4d%2.2d%2.2d", date.getYear(), date.get)
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		return df.format(d);
	}
	/**
	 * Format date and time in format "yyyyMMdd_HHmmss"
	 * @param timeMillis
	 * @return formatted time string
	 */
	public static String formatFileDateTime(long timeMillis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timeMillis);
		c.setTimeZone(defaultTimeZone);
		//		Date date = c.getTime();
		//		String.format("%4.4d%2.2d%2.2d", date.getYear(), date.get)
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		df.setTimeZone(defaultTimeZone);
		Date d = c.getTime();
		return df.format(d);
	}
	
	/**
	 * "H:m:s" added by Graham Weatherup for Logger Time control, another option is to add "01-01-1970 " in the TimeControl to the string first...
	 */
	private static String dateFormats[] = {"y-M-d H:m:s", "y-M-d H:m", "y-M-d H", "y-M-d"/*, "H:m:s"*/};
	/**
	 * Read a date string and turn it into a millisecond time.
	 * @param dateString
	 * @return time in milliseconds
	 */
	public static long msFromDateString(String dateString) {
		Date d = dateFromDateString(dateString);
		if (d != null) {
			Calendar cl = Calendar.getInstance();
			cl.setTime(d);
			return cl.getTimeInMillis();
		}

		return -1;
	}

	/**
	 * Read a date string and turn it into a Date
	 * @param dateString
	 * @return time as a Date object
	 */
	public static Date dateFromDateString(String dateString) {
		DateFormat df;
		Date d = null;
		for (int i = 0; i < dateFormats.length; i++) {

			df = new SimpleDateFormat(dateFormats[i]);
			df.setTimeZone(defaultTimeZone);

			try {
				d = df.parse(dateString);
				
				return d;
			}
			catch (java.text.ParseException ex) {
				//				JOptionPane.showMessageDialog(this, "Invalid data format", "Error", JOptionPane.ERROR_MESSAGE);
				//				ex.printStackTrace();
				//				return null;
				d = null;
			}
		}
		return d;
	}
	
	private static String timeFormats[] = {"H:m:s", "H:m", "H"};
	
	/**
	 * Read a time string and turn it into a millisecond time.
	 * @param timeString
	 * @return time in milliseconds
	 */
	public static long msFromTimeString(String timeString) {
		Date d = timeFromTimeString(timeString);
		if (d != null) {
			Calendar cl = Calendar.getInstance();
			cl.setTime(d);
			return cl.getTimeInMillis();
		}

		return -1;
	}
	/**
	 * Read a time string and turn it into a Date
	 * @param timeString
	 * @return time as a Date object
	 */
	public static Date timeFromTimeString(String timeString) {
		DateFormat df;
		Date d = null;
		for (int i = 0; i < timeFormats.length; i++) {

			df = new SimpleDateFormat(timeFormats[i]);
			df.setTimeZone(defaultTimeZone);

			try {
				d = df.parse(timeString);
				
				return d;
			}
			catch (java.text.ParseException ex) {
				//				JOptionPane.showMessageDialog(this, "Invalid data format", "Error", JOptionPane.ERROR_MESSAGE);
				//				ex.printStackTrace();
				//				return null;
				d = null;
			}
		}
		return d;
	} 

	/**
	 * 
	 * @return true if the sound source is a file
	 */
	public static boolean isSoundFile() {
		return soundFile;
	}

	/**
	 * 
	 * @param soundFile set whether the sound source is a file
	 */
	public static void setSoundFile(boolean soundFile) {
		PamCalendar.soundFile = soundFile;
	}

	/**
	 * 
	 * @return The time that processing started
	 */
	public static long getSessionStartTime() {
		return sessionStartTime;
	}

	/**
	 * 
	 * @param sessionStartTime the time that processing started
	 */
	public static void setSessionStartTime(long sessionStartTime) {
		PamCalendar.sessionStartTime = sessionStartTime;
	}

	/**
	 * 
	 * @param soundFileTimeMillis The start time of a sound file
	 */
	public static void setSoundFileTimeInMillis(long soundFileTimeMillis) {
		PamCalendar.soundFileTimeInMillis = soundFileTimeMillis;
	}

	/**
	 * Create a file name based on a time and other information
	 * @param fileStartTime File time
	 * @param directory Directory / folder
	 * @param prefix file prefix (part of file name to inlcude before the time stamp)
	 * @param fileType file end
	 * @return File path and name, ending with a time stamp
	 */
	public static String createFileName(long fileStartTime, String directory, String prefix, String fileType) {
		return directory + FileParts.getFileSeparator() + createFileName(fileStartTime, prefix, fileType);
	}

	/**
	 * Create a file name that doesn't contain a time
	 * @param directory Directory / folder
	 * @param prefix file prefix (part of file name to inlcude before the time stamp)
	 * @param fileType file end
	 * @return File path and name
	 */
	public static String createFileName(String directory, String prefix, String fileType) {
		return directory + FileParts.getFileSeparator() + createFileName(getTimeInMillis(), prefix, fileType);
	}

	/**
	 * Creates a file name containing the time and a user defined
	 * prefix and file end
	 * @param fileStartTime time
	 * @param prefix prefix for file name
	 * @param fileType file type (with or without the '.')
	 * @return file name
	 */
	public static String createFileName(long fileStartTime, String prefix, String fileType) {

		String fileName;
		if (fileType == null || fileType.length() < 1) {
			fileName = String.format("%s%s", prefix, fileTimeString(fileStartTime));
		}
		else {
			if (fileType.charAt(0) != '.') {
				fileType = "." + fileType;
			}
			fileName = String.format("%s%s%s",
					prefix, fileTimeString(fileStartTime),	fileType);
		}
		return fileName;
	}
	
	/**
	 * Like createFileName but the time now also includes milliseconds. 
	 * @param fileStartTime
	 * @param prefix
	 * @param fileType
	 * @return
	 */
	public static String createFileNameMillis(long fileStartTime, String prefix, String fileType) {

		String fileName;
		if (fileType == null || fileType.length() < 1) {
			fileName = String.format("%s%s", prefix, fileTimeStringms(fileStartTime));
		}
		else {
			if (fileType.charAt(0) != '.') {
				fileType = "." + fileType;
			}
			fileName = String.format("%s%s%s",
					prefix, fileTimeStringms(fileStartTime),	fileType);
		}
		return fileName;
	}

	private static Object fileTimeStringms(long fileStartTime) {
		String str = fileTimeString(fileStartTime);
		int millis = (int) (fileStartTime%1000);
		str += String.format("_%03d", millis);
		return str;
	}

	/**
	 * 
	 * @param fileStartTime tiem in milliseconds
	 * @return a date / time in a compressed format suitable to a file name
	 */
	private static String fileTimeString(long fileStartTime) {

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(fileStartTime);
		c.setTimeZone(defaultTimeZone);

		//                              %3$tY%3$tm%3$td_%3$tH%3$tM%3$tS
		String fileStr = String.format("%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS", c);

		return fileStr;

	}

	/**
	 * 
	 * @return a date string in a very compressed format (suitable for file names) 
	 */
	public static String getUnpunctuatedDate(){
		//		 TODO Check that timezone is correct
		Calendar c = Calendar.getInstance();
		c.setTimeZone(defaultTimeZone);
		Date date = new Date(c.getTimeInMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateStr = format.format(date) + "_GMT";
		return dateStr;
	}

	/**
	 * Get a GMT timestamp for output to a database
	 * @param millis time in milliseconds
	 * @return GMT timestamp
	 */
	public static Timestamp getTimeStamp(long millis) {
		TimeZone tz = TimeZone.getDefault();
		//		System.out.println("RAw offset = " + tz.getOffset(millis));
		return new Timestamp(millis - tz.getOffset(millis));
	}
	
	/**
	 * Get a local timestamp using system default time zone. 
	 * @param millis time in milliseconds. 
	 * @return local timestamp. 
	 */
	public static Timestamp getLocalTimeStamp(long millis) {
		return new Timestamp(millis);
	}
	
	/**
	 * convert a time stamp read from a database into milliseconds. 
	 * @param timestamp GMT timestamp
	 * @return time in milliseconds. 
	 */
	public static long millisFromTimeStamp(Timestamp timestamp) {
		// this is used when reading back data from database.
		// it's totally unclear to me whether I need to correct for time zone
		// based on the time my computer is at, or when the data were at !
		TimeZone tz = TimeZone.getDefault();
		return timestamp.getTime() + tz.getOffset(timestamp.getTime());
	}

	public static void setViewTimes(long start, long end) {
		setSessionStartTime(start);
		viewEndTime = end;
		viewPosition = (end-start) / 2;
	}

	public static long getViewEndTime() {
		return viewEndTime;
	}

	public static long getViewPosition() {
		return viewPosition;
	}

	public static void setViewPosition(long viewPosition) {
		PamCalendar.viewPosition = viewPosition;
	}



}
