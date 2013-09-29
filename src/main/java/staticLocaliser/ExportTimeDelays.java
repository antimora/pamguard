package staticLocaliser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import PamDetection.AbstractDetectionMatch;


/**
 * A simple class for exporting time delays to a .csv file.
 * @author Jamie Macaulay
 *
 */
public class ExportTimeDelays {
	
	public static void saveTDtoCSV(ArrayList<Double> timedelays, FileWriter fw){
		System.out.println("Time Delays to write: "+timedelays);
		try {
			String results;
			if (timedelays.get(0)==null) results="null";
			else results=Double.toString(timedelays.get(0));
	 		for (int i=1; i<timedelays.size(); i++){
	 		if (timedelays.get(i)==null)	results=results+",null";
	 		else results=results+","+Double.toString(timedelays.get(i));
	 		}
	 		fw.write(results + "\n");
        } 
		
		catch (IOException e) {
	 		e.printStackTrace();
	 	}

	}
	
	public static void saveIndextoCSV(ArrayList<Integer> indexMX, FileWriter fw){
		
		try {
			String results=Integer.toString(indexMX.get(0));
	 		for (int i=1; i<indexMX.size(); i++){
	 	
	 		 results=results+","+Integer.toString(indexMX.get(i));
	 		}
	 		fw.write(results + "\n");
        } 
		
		catch (IOException e) {
	 		// TODO Auto-generated catch block
	 		e.printStackTrace();
	 	}

	}
	
	
	public static void writeTDResultstoFile(ArrayList<ArrayList<Double>> timeDelays, String OutputFileForAnalysedResults, boolean append){
		
		File tdFile=new File(OutputFileForAnalysedResults);
		if (timeDelays==null) return;
		
		try {	
			FileWriter fw = new FileWriter(tdFile, true);
			saveIndextoCSV(AbstractDetectionMatch.indexM1(AbstractDetectionMatch.calcHydrophoneNumber(timeDelays.get(0).size())),fw);
			saveIndextoCSV(AbstractDetectionMatch.indexM2(AbstractDetectionMatch.calcHydrophoneNumber(timeDelays.get(0).size())),fw);
			for (int i=0; i<timeDelays.size(); i++){
				saveTDtoCSV(timeDelays.get(i), fw);
			}
	 		fw.flush();
            fw.close();
		}		
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
