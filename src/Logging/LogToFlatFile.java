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

/*
 * Example on writing to text files. You can use printl and print methods. In
 * this example, we simply copy the contents of the textfield to a file. Do not
 * forget to close the output file. If you don't close it, the information may
 * not be stored properly.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import PamUtils.PamFileFilter;

public class LogToFlatFile {
	private PrintWriter writeToFile;

	private File logFile;

	FileOutputStream fileOutputStream;

	JFileChooser fileChooser;

	FileFilter logFileFilter;

	PamFileFilter filter;

	boolean fileOpened;

	int state;

	public LogToFlatFile() {
		// fileChooser = new JFileChooser("test");
	}

	public void openNewLogFile() {

		try {
			fileChooser = new JFileChooser("test");
			fileChooser.setFileFilter(filter);
			fileChooser.setDialogTitle("PAM Log File Selection...");
			fileChooser.addChoosableFileFilter(filter);
			fileChooser.setFileHidingEnabled(true);
			fileChooser.setApproveButtonText("Create PAM log file");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			javax.swing.filechooser.FileFilter[] filters = fileChooser
					.getChoosableFileFilters();
			for (int i = 0; i < filters.length; i++) {
				fileChooser.removeChoosableFileFilter(filters[i]);
			}
			fileChooser.addChoosableFileFilter(
					new PamFileFilter("PAMGUARD log file (comma delimited) (*.pam)", "pam"));

			fileChooser.setSelectedFile(new File(createTimeBasedFilename(
					"PAMGUARD_Log_", ".pam")));

			state = fileChooser.showSaveDialog(null);
			logFile = fileChooser.getSelectedFile();

			// //System.out.println("logFile " + logFile.toString() + " " +
			// logFile.exists());

			if (!logFile.exists()) {
				logFile.createNewFile();
			}

			if (state == JFileChooser.CANCEL_OPTION) {
				fileOpened = false;
			}

			if (!logFile.canWrite()) {
				JOptionPane.showMessageDialog(null, "Invalid Log File!",
						"Invalid Log File", JOptionPane.WARNING_MESSAGE);

				fileOpened = false;
			} else {
				fileOpened = true;
			}
		} catch (IOException ex) {
			System.out.println(ex.toString());
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}

	}

	public void logData(StringBuffer logString) {
		try {
			writeToFile = new PrintWriter(new FileOutputStream(logFile, true));
			writeToFile.println(logString);
			writeToFile.close();
		} catch (IOException ex) {
			// System.out.println(ex.toString());
		} catch (Exception ex) {
			// System.out.println(ex.toString());
		}
	}

	public void closeLogFile() {
		if (writeToFile != null) writeToFile.close();
		writeToFile = null;
		fileOpened = false;
	}

	// Method Author : Paul Redmond
	// Creates a file name with a the current date/time to
	// provide uniqueness and logical sequencing of filenames.
	// TODO: base this on pamcalendar
	public String createTimeBasedFilename(String prefix, String suffix) {
		String filename;

		GregorianCalendar calendar = new GregorianCalendar();
		Date date = calendar.getTime();

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String dateStr = format.format(date);

		filename = prefix + dateStr + suffix;

		return (filename);
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public File getLogFile() {
		return logFile;
	}

	public void setWriteToFile(PrintWriter outFile) {
		this.writeToFile = outFile;
	}

	public boolean isFileOpened() {
		return fileOpened;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

}
