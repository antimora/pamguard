package GPS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import javax.swing.SwingWorker;

import Map.TransformUtilities;
import NMEA.AcquireNmeaData;
import NMEA.NMEADataUnit;
import PamController.PamController;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.PamFileFilter;

/**
 * Class to take external GPS data and record in a databse table. 
 * @author Jamie Macaulay
 *
 */
public class ImportGPSData {
	
	GPSDataBlock gpsDataBlock;
	
	GPSControl gpsControl; 
	
	PamFileFilter fileFilter;
	
	ImportGPSThread importGPSThread;

	private GpsDataUnit gpsDataUnit;

	private ImportGPSLoadBar importLoadBar;

	public final static int  NMEA_TEXT=1;
	
	public final static int KML=2;
	
	public final static int GPX=3; 
	
	//last Five Units
	private  ArrayList<Long> lastUnits=new ArrayList<Long>();
	
	private int maxDaysOut=30;
	
	
	public ImportGPSData(GPSControl gpsControl){	
		this.gpsControl=gpsControl;
		this.gpsDataBlock=gpsControl.gpsDataBlock;
		//types of file to load
		fileFilter=new PamFileFilter("GPS Data", ".txt"); 
		fileFilter.addFileType(".asc");
		fileFilter.addFileType(".nmea");
		
	}
	
	protected PamFileFilter getGPSFileFilter(){
		return fileFilter;
	}
	
	/**
	 * Import data from a file 
	 * @param file
	 * @return
	 */
	public void loadFile(String fileString){
		
		importLoadBar=ImportGPSLoadBar.showDialog(PamController.getInstance().getMainFrame(), "Import GPS Data");
		importLoadBar.getLoadBar().setProgress(0);
		importLoadBar.getLoadBar().setIntermediate(true);
		PamController.getInstance().enableGUIControl(false);
		
		
		importGPSThread=new ImportGPSThread(fileString);
		importLoadBar.getCancelButton().addActionListener(new StopGPSImportThread(importGPSThread));
		importGPSThread.execute();
		
		
	}
	
	class StopGPSImportThread implements ActionListener{
		ImportGPSThread importGPSThread;
		
		public StopGPSImportThread(ImportGPSThread importGPSThread){
			this.importGPSThread=importGPSThread;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			importGPSThread.cancel(true);
			importGPSThread.done();
		}
		
		
	}
	
	class ImportGPSThread extends SwingWorker<Boolean, Void> {
		
		File file;
		String extension;
	
		
		public ImportGPSThread(String fileString){
			
			 file= new File(fileString);
			 extension=PamFileFilter.getExtension(file);
			
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			
			lastUnits=new ArrayList<Long>();
			
			try{
				switch (extension){
				
				case "txt":
					loadGPSTextFile(file, this);
					break;
				case "asc":
					loadGPSTextFile(file, this);
					break;
				case "nmea":
					loadGPSTextFile(file, this);
					break;
				case "kml":
					loadKMLFile(file , this);
					break;
				case "gpx":
					loadGPXFile(file , this);
					break;
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
	
			return true;
			
		}
		

		public void setLoadProgress(int prog){
			if (importLoadBar.getLoadBar().isIntermediate()) importLoadBar.getLoadBar().setIntermediate(false);
			importLoadBar.getLoadBar().setProgress(prog);
			setProgress(prog);
		}
		
		public void setTextProgress(int N, int ofN, String name){
			importLoadBar.getLoadBar().setTextUpdate(N + " of "+  ofN + " "+name+" loaded.");
		}
		
		@Override
        public void done() {
			super.done();
			PamController.getInstance().enableGUIControl(true);
			if (importLoadBar!=null){
				System.out.println("done loading data");
				importLoadBar.getLoadBar().setIntermediate(false);
				importLoadBar.setVisible(false);
			}
		}
			
		

	}
	
	/**
	 * Import from .csv or txt- creates an array of strings. 
	 * @param filename
	 * @return
	 */
	public static Collection<String> importFileStrings(String filename){
		
		Collection<String> lines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
				new File(filename)
			));
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return lines;
		
	}
	
		
	private boolean checkGGA( Collection<String> nmeaStrings){
		
		 String stringType; 
		 int nmeaPos;
		 int typePos;
		 
		 boolean GGA=false; 
		 
		 for (String line : nmeaStrings) {
			 
			 nmeaPos=line.indexOf("$");
			 if (nmeaPos<0) continue;
			 line=line.substring(nmeaPos,line.length());
			 
			 //find the type of nmea string
			 typePos =line.indexOf(",");
			 if (typePos<0) typePos=line.indexOf("*");
			 if (typePos<0) continue; 
			 stringType= line.substring(1, typePos);
			 
			 if (stringType.equals("GPGGA")){
				 GGA=true;
				 return GGA; 
			 }
		
 		 }
		 
		 return GGA; 
	}	
	
	
	 private void loadGPSTextFile(File file, ImportGPSThread importGPSThread){
	
		 Collection<String> nmeaStrings=importFileStrings( file.getAbsolutePath());
		 
		 if  (checkGGA(nmeaStrings)) {
			 GPSImportParams newParams=ImportGGADialog.showDialog(PamController.getInstance().getMainFrame(), PamController.getInstance().getMainFrame().getMousePosition(), gpsControl.gpsImportParams, this) ;
			 if (newParams!=null) gpsControl.gpsImportParams=newParams.clone();
		 }
	
		 int n=0; 
		 
		 StringBuffer nmeaString;
		 String stringType; 
		 GpsData gpsData=null; 
		 String headingString;
		 Double trueHeading;
		 Double magHeading; 
		 Double varHeading;
		 int nmeaPos;
		 int typePos;
		 
		 for (String line : nmeaStrings) {
			 
			 if (importGPSThread.isCancelled()) break; 
			 
			 nmeaPos=line.indexOf("$");
			 if (nmeaPos<0) continue;
			 line=line.substring(nmeaPos,line.length());
			 
			 //find the type of nmea string
			 typePos =line.indexOf(",");
			 if (typePos<0) typePos=line.indexOf("*");
			 if (typePos<0) continue; 
			 stringType= line.substring(1, typePos);
	
			 nmeaString=new StringBuffer(line);
			 
			// System.out.println("NMEA String: " +nmeaString);
			 
			 if (n%100==0){
				 gpsDataBlock.saveViewerData();
			 }
			 
			 if (n%5==0){
				 System.out.println("GPS load progress: " + (double) 100* n/ (double) nmeaStrings.size()+ " %");
				 importGPSThread.setLoadProgress((int) Math.round((double) 100*n/ (double) nmeaStrings.size()));
				 importGPSThread.setTextProgress(n, nmeaStrings.size(), "NMEA strings");
			 }
			 n++;
			 
			 try{
				 
				 switch (stringType){
			 
					 case "GPGGA":{
						 
						 if (gpsControl.gpsImportParams.useGGA){
							 if (gpsData!=null ){
								 if (gpsData.isDataOk()) saveGPSData(gpsData);
								 
							 }
							 
							 gpsData=new GpsData(nmeaString, gpsControl.gpsParameters.READ_GGA, true);
							 ///GGA strings contain no date so we need to set it. 
							 gpsData.getGpsCalendar().set(Calendar.YEAR, gpsControl.gpsImportParams.year);
							 gpsData.getGpsCalendar().set(Calendar.MONTH, gpsControl.gpsImportParams.month);
							 gpsData.getGpsCalendar().set(Calendar.DAY_OF_MONTH, gpsControl.gpsImportParams.day);
							 gpsData.setTimeInMillis(gpsData.getGpsCalendar().getTimeInMillis());
							 
							 //System.out.println("gpsData:" +gpsData.getGpsCalendar().getTime().toString());
							 // System.out.println("nmeaStrings:" +nmeaStrings.size());
						 }
						 break;
					 }
					 
					 
					 case "GPRMC":{
						 
						 if (gpsData!=null ){
							 if (gpsData.isDataOk()) saveGPSData(gpsData);
						 }
						 System.out.println("nmea string: "+nmeaString);
						 gpsData=new GpsData(nmeaString, gpsControl.gpsParameters.READ_RMC, true);
						 
						 break; 
					 }
					 case "GPHDT":{
						 //true heading- likely from a vector GPS
						 if (gpsData!=null && gpsData.isDataOk()){
							 headingString=nmeaString.substring(nmeaString.indexOf(",")+1, nmeaString.length());
							 headingString=headingString.substring(0,  (headingString.indexOf(",")-1));
							 trueHeading=Double.valueOf(headingString);
							 gpsData.setTrueHeading(trueHeading);
						 }
						 
						 break;
					}
					 
					case "HCHDG":{
						 //Garmin eTrex summit, Vista and GPS76S receivers output value for the internal flux-gate compass. A magnetic heading. 
						 if (gpsData!=null && gpsData.isDataOk()){
							 
							 headingString=nmeaString.substring(nmeaString.indexOf(",")+1, nmeaString.length());
							 
							 //magentic heading
							 magHeading=Double.valueOf(headingString.substring(0,  (headingString.indexOf(",")-1)));
							 
							 //heading variation
							 headingString=headingString.substring(headingString.indexOf(",")+1,  headingString.lastIndexOf("*"));
							 headingString=headingString.substring(headingString.indexOf(",")+1,  headingString.length());
							 headingString=headingString.substring(headingString.indexOf(",")+1,  headingString.length());
							 							 
							 varHeading=Double.valueOf(headingString.substring(0, headingString.indexOf(",")-1));
							 
							 //true heading
							 if (headingString.substring(headingString.indexOf(",")+1,headingString.length()).contentEquals(new StringBuffer("W"))){
								 trueHeading=magHeading+varHeading; 
							 }
							 else{
								 trueHeading=magHeading-varHeading; 
							 }
							 
							 gpsData.setTrueHeading(trueHeading);
						 }
						 
						 break;
					}
			 	}
			 }
			 catch(Exception e){
				 e.printStackTrace();
			 }

		 }
		 
		 gpsDataBlock.saveViewerData();

	 };
	 
	public void saveGPSData(GpsData gpsData){
		//System.out.println("GPSData to save:  " +gpsData + "gpsDataBlock: "+gpsDataBlock);
		//System.out.println("GPSData time:  " +gpsData.getTimeInMillis());
		if (gpsData.getHeading(true)==null){
			 //need to calculate course over ground. 
			 GpsDataUnit oldGPS = gpsDataBlock.getLastUnit();
			 if (oldGPS!=null){
				 LatLong oldLatLong=new LatLong( oldGPS.getGpsData().getLatitude(), oldGPS.getGpsData().getLongitude());
				 double bearing=oldLatLong.bearingTo(new LatLong(gpsData.getLatitude(),gpsData.getLongitude()));
				 gpsData.setCourseOverGround(bearing);
			 }
		 }
		
		gpsDataUnit= new GpsDataUnit(gpsData.getTimeInMillis(), gpsData) ;
		if (checkDate(gpsDataUnit)){
			gpsDataUnit.setChannelBitmap(-1);
			
			gpsDataBlock.addPamData(gpsDataUnit);	
		}
		else{
			System.out.println("Unlikely date: GPS Data not saved to database...");
		}
	}
	
	/**
	 * Occasionally a completely spurious date is recorded. The field lastUnits keep a record of the past five timeMillis. If the average of these five is more than maxDaysOut out from the gps dat unit then the gps data unit is not saved. 
	 * This prevents dates decades in the future or past been recorded. 
	 * @param gpsDataUnit
	 * @return
	 */
	public boolean checkDate(GpsDataUnit gpsDataUnit){
	
		long average = 0;
		if (lastUnits.size()>=5){
			for (int i=0; i<lastUnits.size(); i++){
				average+=lastUnits.get(i);
			}
			average=average/lastUnits.size();
		
			if (Math.abs(average-gpsDataUnit.getTimeMilliseconds())>PamCalendar.millisPerDay*maxDaysOut) return false;
			else{
				lastUnits.add(0,gpsDataUnit.getTimeMilliseconds());
				lastUnits.remove(6);
			}
		}
		else{
			lastUnits.add(gpsDataUnit.getTimeMilliseconds());
		}
		return true; 
	}
	 
	private void loadKMLFile(File file , ImportGPSThread importGPSThread){
		// TODO Auto-generated method stub
	};
	 
	private void loadGPXFile(File file2, ImportGPSThread importGPSThread) {
		// TODO Auto-generated method stub
			
	}
	 
	
	
	
	
	

}
