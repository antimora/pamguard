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


/**
 *
 * Facilitates data logging to database 
 *
 * @author David J McLaren, Douglas Gillespie, Paul Redmond
 *
 */

package GPS;

import generalDatabase.DBProcess;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import javax.swing.JOptionPane;





import PamController.PamViewParameters;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

public class GpsLogger extends SQLLogging {
	
	PamTableDefinition tableDefinition;
	
	int gpsDateCol;
	
	ProcessNmeaData nmeaProcess;
	
	PamTableItem gpsDate, pcTime, gpsTime, latitude, longitude, speed, 
	speedType, heading, headingType, gpsError, dataStatus, 
	trueHeading, magneticHeading, magneticVariation;
	
	public GpsLogger(PamDataBlock pamDataBlock) {
		
		super(pamDataBlock);
		
		try {
			nmeaProcess = (ProcessNmeaData) pamDataBlock.getParentProcess();
		}
		catch (ClassCastException e) {
			nmeaProcess = null;
		}
		
		setUpdatePolicy(UPDATE_POLICY_OVERWRITE);
		setCanView(true);
		
		tableDefinition = new PamTableDefinition("gpsData", getUpdatePolicy());
		gpsDateCol = tableDefinition.addTableItem(gpsDate = new PamTableItem("GpsDate", Types.TIMESTAMP));
		tableDefinition.addTableItem(pcTime = new PamTableItem("PCTime", Types.TIMESTAMP));
		tableDefinition.addTableItem(gpsTime = new PamTableItem("GPSTime", Types.INTEGER));
		tableDefinition.addTableItem(latitude = new PamTableItem("Latitude", Types.DOUBLE));
		tableDefinition.addTableItem(longitude = new PamTableItem("Longitude", Types.DOUBLE));
		tableDefinition.addTableItem(speed = new PamTableItem("Speed", Types.DOUBLE));
		tableDefinition.addTableItem(speedType = new PamTableItem("SpeedType", Types.CHAR, 2));
		tableDefinition.addTableItem(heading = new PamTableItem("Heading", Types.DOUBLE));
		tableDefinition.addTableItem(headingType = new PamTableItem("HeadingType", Types.CHAR, 2));
		tableDefinition.addTableItem(trueHeading = new PamTableItem("TrueHeading", Types.DOUBLE));
		tableDefinition.addTableItem(magneticHeading = new PamTableItem("MagneticHeading", Types.DOUBLE));
		tableDefinition.addTableItem(magneticVariation = new PamTableItem("MagneticVariation", Types.DOUBLE));
		tableDefinition.addTableItem(gpsError = new PamTableItem("GPSError", Types.INTEGER));
		tableDefinition.addTableItem(dataStatus = new PamTableItem("DataStatus", Types.CHAR, 3));
		
		tableDefinition.setUseCheatIndexing(true);
		
		setTableDefinition(tableDefinition);
	}

	@Override
	public synchronized boolean doExtraChecks(DBProcess dbProcess, Connection connection) {
		if (dbProcess.tableExists(tableDefinition) == false) {
			return true; // PAMGUARD will create the table normally. 
		}
		/*
		 *  the table exists - is it a Logger table ?
		 *  A logger table will have an Index column, but not
		 *  Id, UTC, pcLocaltime or updateOf column. 
		 */
		PamTableItem indexItem = new PamTableItem("Index", Types.INTEGER);
		PamTableItem pctimeItem = new PamTableItem("PCTime", Types.TIMESTAMP);
		indexItem.setPrimaryKey(true);
		indexItem.setCounter(true);
		boolean hasIndex = dbProcess.columnExists(tableDefinition, indexItem);
		boolean hasPCTime = dbProcess.columnExists(tableDefinition, pctimeItem);
		boolean hasId = dbProcess.columnExists(tableDefinition, tableDefinition.getIndexItem());
		boolean hasUTC = dbProcess.columnExists(tableDefinition, tableDefinition.getTimeStampItem());
		boolean hasLoctime = dbProcess.columnExists(tableDefinition, tableDefinition.getLocalTimeItem());
		if (hasIndex && hasPCTime && hasId == false && hasUTC == false && hasLoctime == false) {
			// seems to be a logger table
			String warningText = String.format("The data in table %s appear to be from the IFAW\n" +
					"Logger 2000 software and are incompatible with PAMGUARD\n\n" +
					"Click Yes to convert the data to a PAMGUARD compatible format " +
					"or Canel to leave the table unchanged. \n\n" +
					"Converting the data may make the data incompatible with the Logger software",
					tableDefinition.getTableName());
			int ans = JOptionPane.showConfirmDialog(null, warningText, "GPS Data Format", 
					JOptionPane.OK_CANCEL_OPTION);
			if (ans == JOptionPane.CANCEL_OPTION) {
				JOptionPane.showMessageDialog(null, "You should exit PAMGUARD and either convert the\n" +
						"data yourself, or create a new database table");
			}
			else {
				return convertLoggerGpsTable(dbProcess, connection);
			}
			
		}
		
		return true;
	}

	private boolean convertLoggerGpsTable(DBProcess dbProcess, Connection connection) {
		/**
		 * Jobs list is to 
		 * 1. Rename Index to Id
		 * 2. add columns for UTC and Local time
		 * 3. Copy the data from PCTime into UTC and Localtime
		 */
		
		
		String sqlStr;
		
		/*
		 * Can't rename, so create an Integer index column. 
		 * copy, fill and then try to swap over primary key
		 */
		sqlStr = String.format("ALTER TABLE %s ADD COLUMN Id LONG", tableDefinition.getTableName());
		if (!runStmt(connection, sqlStr)) {
			return false;
		} 
		sqlStr = String.format("UPDATE %s SET Id = Index",
				tableDefinition.getTableName());
//		sqlStr = String.format("ALTER TABLE %s RENAME COLUMN \"Index\" to \"Id\"",
//				tableDefinition.getTableName());
		if (!runStmt(connection, sqlStr)) {
			return false;
		}
		
		// now add the three other columns. 
		System.out.println("Add standard gps data table time comumns");
		dbProcess.checkColumn(tableDefinition, tableDefinition.getTimeStampItem());
		dbProcess.checkColumn(tableDefinition, tableDefinition.getLocalTimeItem());
		dbProcess.checkColumn(tableDefinition, gpsDate);
		String[] timeCols = new String[3];
		timeCols[0] = tableDefinition.getTimeStampItem().getName();
		timeCols[1] = tableDefinition.getLocalTimeItem().getName();
		timeCols[2] = gpsDate.getName();
		for (int i = 0; i < 3; i++) {
			sqlStr = String.format("UPDATE %s SET %s = PCTime", tableDefinition.getTableName(),
					timeCols[i]);
			if (!runStmt(connection, sqlStr)) {
				return false;
			}
		}
		// finally, convert the Lat, Long, Speed and Head columns from single to Double
		System.out.println("Change column format for " + latitude.getName());
		dbProcess.changeColumnFormat(tableDefinition.getTableName(), latitude);
		System.out.println("Change column format for " + longitude.getName());
		dbProcess.changeColumnFormat(tableDefinition.getTableName(), longitude);
		System.out.println("Change column format for " + speed.getName());
		dbProcess.changeColumnFormat(tableDefinition.getTableName(), speed);
		System.out.println("Change column format for " + heading.getName());
		dbProcess.changeColumnFormat(tableDefinition.getTableName(), heading);
		boolean hasHeading;
		hasHeading = dbProcess.columnExists(tableDefinition, magneticHeading);
		if (hasHeading) {
			System.out.println("Change column format for " + magneticHeading.getName());
			dbProcess.changeColumnFormat(tableDefinition.getTableName(), magneticHeading);
		}
		hasHeading = dbProcess.columnExists(tableDefinition, trueHeading);
		if (hasHeading) {
			System.out.println("Change column format for " + trueHeading.getName());
			dbProcess.changeColumnFormat(tableDefinition.getTableName(), trueHeading);
		}
		
		return true;
	}
	private boolean runStmt(Connection con, String str) {
		Statement stmt;
		try {
			stmt = con.createStatement();   
			int addResult;
			addResult = stmt.executeUpdate(str); 
		}
		catch (SQLException ex) {
			System.out.println(str);
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		
		GpsData gpsData = ((GpsDataUnit) pamDataUnit).getGpsData();
		int gpsTimeVal = gpsData.getHours()*10000 + gpsData.getMins() * 100 + gpsData.getSecs();
		
		gpsDate.setValue(PamCalendar.getTimeStamp(gpsData.getTimeInMillis()));
		pcTime.setValue(PamCalendar.getTimeStamp(gpsData.getTimeInMillis()));
		gpsTime.setValue(gpsTimeVal);
		latitude.setValue(gpsData.getLatitude());
		longitude.setValue(gpsData.getLongitude());
		speed.setValue(gpsData.getSpeed());
		speedType.setValue(new String("N"));
		heading.setValue(gpsData.getCourseOverGround());
		headingType.setValue(new String("T"));
		trueHeading.setValue(gpsData.getTrueHeading());
		magneticHeading.setValue(gpsData.getMagneticHeading());
		magneticVariation.setValue(gpsData.getMagneticVariation());
		gpsError.setValue(0);
		dataStatus.setValue(new String("A"));
		
	}

//	long prevT;
//	int prevInd;
	@Override
	protected PamDataUnit createDataUnit(long dataTime, int iD) {

//		Timestamp ts = (Timestamp) tableDefinition.getTimeStampItem().getValue();
//		long t = PamCalendar.millisFromTimeStamp(ts);
//		int index = tableDefinition.getIndexItem().getIntegerValue();
//		if (t < prevT || index < prevInd) {
//			System.out.println(String.format("Odd GPS Order index %d and %d, %s %s",
//					index, prevInd, PamCalendar.formatDateTime(t), PamCalendar.formatDateTime(prevT)));
//		}
//		prevT = t;
//		prevInd = index;
		
		double lat = (Double) latitude.getValue();
		double longi = (Double) longitude.getValue();
		double head = (Double) heading.getValue();
		double spd = (Double) speed.getValue();
		Double magHead, trueHead, magVar;
		magHead = (Double) magneticHeading.getValue();
		trueHead = (Double) trueHeading.getValue();
		magVar = (Double) magneticVariation.getValue();
//		double crse = (Double) heading.getValue();
		GpsData gpsData = new GpsData(lat, longi, spd, head, dataTime, trueHead, magHead);
		gpsData.setMagneticVariation(magVar);
		
		
		GpsDataUnit gpsDataUnit = new GpsDataUnit(dataTime, gpsData);
		gpsDataUnit.setDatabaseIndex(iD);
		if (nmeaProcess == null || nmeaProcess.wantDataUnit(gpsDataUnit)) {
			((PamDataBlock<GpsDataUnit>) getPamDataBlock()).addPamData(gpsDataUnit);
		}
		gpsDataUnit.setChannelBitmap(-1);
		return gpsDataUnit;
	}

	
	public boolean getCanLog() {
		return true;
	}


}
