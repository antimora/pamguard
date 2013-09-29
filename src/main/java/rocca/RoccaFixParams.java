/*
 *  PAMGUARD - Passive Acoustic Monitoring GUARDianship.
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

package rocca;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//
//
//
///**
// * Short script to load an existing contour file (Rocca output), recalculate the parameters and save
// * the data into a new summar file.
// * 
// * The contour file must have the following format:
// * 		column A: Time [ms]
// * 		column B: Peak Frequency [Hz]
// * 		column C: Duty Cycle
// * 		column D: Energy
// * 		column E: WindowRMS
// * 
// * 
// * 
// * 
//
// * @author Mike
// *
// */
public class RoccaFixParams {
//
//	private RoccaContourDataBlock rcdb;
//	
//	private String dirIn;
//	
//	private String csvIn;
//	
//	private int numFiles=0;
//	
//	private File[] listOfFiles;
//	
//	
//	public RoccaFixParams() {
//		
//		/* initialize the BufferedReader */
//		BufferedReader currentFile = null;
//		
//		/* initialize the RoccaContourDataBlock */
//		rcdb = new RoccaContourDataBlock();
//		
//		/* initialize the RoccaContourDataUnit */
//		RoccaContourDataUnit rcdu = null;
//		
//		/* set the directory */
//		this.dirIn = new String("C:\\Users\\Mike\\Documents\\Work\\Java\\EclipseWorkspace\\testing\\RoccaFixParams_testing");
//		
//		/* get a list of csv files in that directory */
//		/* Hard-coded for now.  To Do: read list of files from directory */
//		this.csvIn = new String("Detection1-D08_20110928_231007_W02-Channel0-20110928_231008.csv");
//		this.numFiles = 1;
//		listOfFiles = new File[numFiles];
//		listOfFiles[0] = new File(dirIn, csvIn);
//		
//		/* loop through the files one at a time */
//		for (int i=0; i<numFiles; i++) {
//			
//			/* load the file */
//			try {
//				currentFile = new BufferedReader(new FileReader(listOfFiles[i]));
//			} catch (FileNotFoundException e) {
//				System.out.println("Cannot load file "+listOfFiles[i]);
//				e.printStackTrace();
//				return;
//			}
//			
//			/* The first line is the header; read it and ignore it.  Start processing
//			 * after reading in the second line
//			 */
//			String dataRow = null;
//			try {
//				dataRow = currentFile.readLine();
//				dataRow = currentFile.readLine();
//			} catch (IOException e) {
//				System.out.println("Cannot read first 2 lines from "+listOfFiles[i]);
//				e.printStackTrace();
//				return;
//			}
//			
//			/* Once the data is null, it means we've hit the end of the file */
//			while (dataRow != null){
//
//				/* split the row up */
//				String[] dataArray = dataRow.split(",");
//
//				/* load the parameters into the RoccaContourDataUnit, and add it
//				 * to the RoccaContourDataBlock
//				 */
//				rcdu = new RoccaContourDataUnit(0,0,0,0);
//				rcdu.setTimeMilliseconds(Long.parseLong(dataArray[0]));
//				rcdu.setPeakFreq(Double.parseDouble(dataArray[1]));
//				rcdu.setDutyCycle(Double.parseDouble(dataArray[2]));
//				rcdu.setEnergy(Double.parseDouble(dataArray[3]));
//				rcdu.setWindowRMS(Double.parseDouble(dataArray[4]));
//				rcdb.addPamData(rcdu);
//
//
//				/* read the next line of data */
//				try {
//					dataRow = currentFile.readLine();
//				} catch (IOException e) {
//					System.out.println("Cannot read line from "+listOfFiles[i]);
//					e.printStackTrace();
//					return;
//				}
//			} // parse the next line
//			
//			/* close the file */
//			try {
//				currentFile.close();
//			} catch (IOException e) {
//				System.out.println("Cannot close "+listOfFiles[i]);
//				e.printStackTrace();
//				return;
//			}  
//		} // read the next file
//		
//		/* calculate parameters */
//		rcdb.calculateStatistics();
//		
//		System.out.println("Stats Calculated");
//		
//		
//	} // end of constructor
//	
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		
//		// create a Fixer
//		new RoccaFixParams();
//	}
//	
}
