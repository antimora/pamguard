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

import Acquisition.AcquisitionProcess;
import PamController.PamController;
import PamUtils.FileParts;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import Spectrogram.SpectrogramDisplay;
import fftManager.FFTDataBlock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.swing.JOptionPane;

import wavFiles.WavFile;
import whistlesAndMoans.AbstractWhistleDataBlock;
import whistlesAndMoans.AbstractWhistleDataUnit;
import whistlesAndMoans.ConnectedRegionDataUnit;
import whistlesAndMoans.SliceData;

/**
 * Main Rocca process
 *
 *
 * @author Michael Oswald
 */
public class RoccaProcess extends PamProcess {

    // local reference to the RoccaControl controlling this process
    RoccaControl roccaControl;

    // source data block
    FFTDataBlock fftDataBlockIn;

    // contour process and parameters
    RoccaContour roccaContour;
    
    // contour classifier
    RoccaClassifier roccaClassifier;

    // channel map of data source
    int channelMap;

    /** The number of detections captured since the program started - only used for the whistle and moan detector */
    private static int numDetections = 0;

    /** reference to the FFT data source process */
    AcquisitionProcess daqProcess;
    
    /** flag indicating whether or not the classifier model has been loaded */
    boolean classifierLoaded = false;

    /** reference to the Spectrogram Display */
    SpectrogramDisplay display;

    /** data block for logging contour statistics to database */
    RoccaLoggingDataBlock rldb;

    /** Whistle & Moan detector source */
	private AbstractWhistleDataBlock whistleSourceData;



    /**
     * Main constructor
     *
     * @param roccaControl
     */
    public RoccaProcess (RoccaControl roccaControl) {
        super(roccaControl, null);
        this.roccaControl = roccaControl;
        roccaContour = new RoccaContour(this);
        roccaClassifier = new RoccaClassifier(this);
        rldb = new RoccaLoggingDataBlock(this, 0);

        rldb.SetLogging(new RoccaStatsLogger(roccaControl, rldb));
		rldb.setMixedDirection(PamDataBlock.MIX_INTODATABASE);
        addOutputDataBlock(rldb);

    }


    @Override
    public void pamStart() {
//        firstUnit = true;
    }


    @Override
    public void pamStop() {
    }


    @Override
    /*
     * Find and subscribe to the FFTDataBlock that's been specified in the RoccaParameters
     */
    public void prepareProcess() {
        super.prepareProcess();

        /* if user has specified an FFT data source, subscribe to it */
        if (roccaControl.roccaParameters.weAreUsingFFT()) {
        	
        	// get list of all fftDataBlocks
        	ArrayList<PamDataBlock> allfftDataBlocks = PamController.getInstance().getFFTDataBlocks();

        	// if no fftDataBlocks exist yet, return a null
        	if (allfftDataBlocks == null || allfftDataBlocks.size() <= roccaControl.roccaParameters.fftDataBlock) {
        		setParentDataBlock(null);
        		return;
        	}

        	// if fftDataBlocks exist, subscribe to the one specified in RoccaParameters
        	fftDataBlockIn = (FFTDataBlock) allfftDataBlocks.get(roccaControl.roccaParameters.fftDataBlock);
        	setParentDataBlock(fftDataBlockIn);
        	fftDataBlockIn.getRawSourceDataBlock2().setNaturalLifetimeMillis(5000);
        	this.channelMap = roccaControl.roccaParameters.getChannelMap();
        	
        /* if user has specified a whistle & moan detector source, subscribe to it */
        } else {
        
        	whistleSourceData = (AbstractWhistleDataBlock) PamController.getInstance().
        			getDataBlock(AbstractWhistleDataUnit.class, roccaControl.roccaParameters.getWmDataSource());
        	if (whistleSourceData == null) {
        		return;
        	}

        	setParentDataBlock(whistleSourceData);
        	whistleSourceData.getRawSourceDataBlock2().setNaturalLifetimeMillis(5000);
        	this.channelMap = whistleSourceData.getChannelMap();
        }
    }


    /**
     * this method called when new data arrives
     */
    @Override
    public void newData (PamObservable o, PamDataUnit arg) {
    	
    	/* if this is a whistle and moan detection, convert to RoccaDataBlock,
    	 * calculate the statistics, classify and update the side panel
    	 */
		if (o == whistleSourceData) {
			RoccaContourDataBlock rcdb = newWMDetectorData((ConnectedRegionDataUnit) arg);
			rcdb.calculateStatistics();
	        roccaClassifier.classifyContour(rcdb);
	        
	        /* check the side panel for a detection number.  If one has not yet been created,
	         * create a default one now.  The user can always rename it later
	         */
	        String sNum = roccaControl.roccaSidePanel.getSightingNum();
	        if (sNum.equals(RoccaSightingDataUnit.NONE)) {
                sNum = roccaControl.roccaSidePanel.sidePanel.addASighting("S001");
            }	        
	        updateSidePanel(rcdb);
	        saveContourPoints(rcdb, rcdb.getChannelMap(), ++numDetections, sNum);
	        saveContourStats(rcdb, rcdb.getChannelMap(), numDetections, sNum);
	        saveContour(rcdb, rcdb.getChannelMap(), numDetections, sNum);

		}
    }

    
    private void updateSidePanel(RoccaContourDataBlock rcdb) {
        /* check that the species list for the current classifier matches the
         * species list of the detection we're trying to save to.  If it
         * doesn't, force the user to create a new detection number.  This can
         * happen if the user captured a sound event after scrolling backwards
         * through the detection list to an older detection created with a
         * different classifier.
         * Note that this method also checks if the classifier species list
         * contains 'Ambig', and adds it if it doesn't.
         */
    	String sNum = roccaControl.roccaSidePanel.getSightingNum();
        String[] sNumList = roccaControl.roccaSidePanel.rsdb.
                findDataUnitBySNum(sNum).getSpeciesAsString();
        String[] classifierList = RoccaSightingDataUnit.checkForAmbig(
                roccaClassifier.getClassifierSpList());
        if (!Arrays.equals(sNumList, classifierList)) {
            
            /* if the arrays are not equal, throw an error and force the user
             * to create a new detection
             */
            String message = "Classifier species list does not match the \n" +
                    "detection number species list.  You must create\n" +
                    "a new detection for this whistle.";
            int messageType = JOptionPane.ERROR_MESSAGE;
            JOptionPane.showMessageDialog
                    (null, message, "Species List conflict", messageType);
            String newSNum =
                    roccaControl.roccaSidePanel.sidePanel.addASighting(true);
            if (newSNum!=null) {
                sNum = newSNum;
            }

        } else {

            /* if the arrays are equal, add the current tree votes to the tree vote
             * tally on the sidepanel
             */
            roccaControl.roccaSidePanel.addSpeciesTreeVotes(sNum, rcdb.getTreeVotes());
            roccaControl.roccaSidePanel.incSpeciesCount(sNum, rcdb.getClassifiedAs());
        }
    }
    
    
    /**
     * Checks to see if classifier has been loaded
     * 
     * @return boolean indicating whether classifier has been loaded
     */
    public boolean isClassifierLoaded() {
        return classifierLoaded;
    }

    
    /**
     * Sets the classifier loaded flag
     * 
     * @param classifierLoaded true=loaded, false=not loaded
     */
    public void setClassifierLoaded(boolean classifierLoaded) {
        this.classifierLoaded = classifierLoaded;
    }

    
    /**
     * Method run on startup
     */
    @Override
	public void setupProcess() {
        // if the model hasn't been loaded yet, do that now
        if (!isClassifierLoaded()) {
            setClassifierLoaded(roccaClassifier.setUpClassifier());

            // if there was an error loading the model, return "Err"
            if (!isClassifierLoaded()) {
                System.err.println("Cannot load classifier");
            }
        }

        /* check if the SchoolStats file already exists.  If it does, give the
         * user a chance to load or rename it before it gets overwritten.
         */
        try {
            File fName = roccaControl.roccaParameters.getRoccaSightingStatsOutputFilename();

            if (fName.exists() & roccaControl.roccaSidePanel.getRSDB()
                    .getFirstUnit().getSightNum().equals(RoccaSightingDataUnit.NONE)) {

                String message = "The Sighting Stats file specified by the Rocca Parameters:\n\n" +
                        fName.getAbsolutePath() + "\n\n" +
                        "already exists, and it will be overwritten by\n" +
                        "Rocca during use.  If you wish to load the contents of\n" +
                        "the file into the Rocca sidebar, you can do so now.\n" +
                        "If you wish to keep the file, you should rename it now\n" +
                        "before it is overwritten.\n" +
                        "If a database module is also being used, the detection\n" +
                        "information being loaded will NOT be saved to the database.";
                Object[] options = {"Load file", "Continue"};
                int messageType = JOptionPane.WARNING_MESSAGE;
                int n = JOptionPane.showOptionDialog
                        (null,
                        message,
                        "Sighting Stats file already exists!",
                        JOptionPane.YES_NO_OPTION,
                        messageType,
                        null,
                        options,
                        options[0]);

                /* if the user wants to load the file, load it */
                if (n == JOptionPane.YES_OPTION) {
                    roccaControl.roccaSidePanel.sidePanel.loadSighting();
                }
            }
        } catch(Exception ex) {
            System.out.println("Problem checking the Sighting Stats file");
            ex.printStackTrace();
        }
    }

    
    /**
     * Takes an AbstractWhistleDataUnit created in the Whistle and Moan detector,
     * and copies the data into a RoccaContourDataBlock for further analysis and
     * classification
     * 
     * @param wmDataUnit the ConnectedRegionDataUnit containing the whistle
     * @return a RoccaContourDataBlock containing the whistle's time/freq pairs
     */
    public RoccaContourDataBlock newWMDetectorData(ConnectedRegionDataUnit wmDataUnit) {

    	// define a new RoccaContourDataBlock and RoccaContourDataUnit
    	roccaControl.roccaParameters.setChannelMap(wmDataUnit.getChannelBitmap());
    	RoccaContourDataBlock roccaContourDataBlock =
    			new RoccaContourDataBlock(this,
    					roccaControl.roccaParameters.getChannelMap());
    	RoccaContourDataUnit rcdu = null;    	
    	
		/* get a reference to the raw data so we can save a wav clip later */
        PamRawDataBlock prdb = wmDataUnit.getParentDataBlock().getRawSourceDataBlock2();
        
        /* get a list of the time slices that the whistle occupies.  The 'connectedRegion' class contains
         * this information, and the time slice list has details (timeInMilliseconds, peakFreq, etc) about each slice */
		List<SliceData> sliceData = wmDataUnit.getConnectedRegion().getSliceData();

		/* create a new PamRawDataBlock, a subset of the prdb that will contain only
		 * the raw data in the current whistle */
		PamRawDataBlock newBlock = new PamRawDataBlock(
				prdb.getDataName(),
				this,
				roccaControl.roccaParameters.getChannelMap(),
				prdb.getSampleRate());

		/* find the index number of the PamRawDataUnit closest to the start time
		 * of the first FFT data unit, and in the lowest channel
		 * position to be saved.  Use the .getPreceedingUnit method to ensure
		 * that the start time of the raw data is earlier than the start time
		 * of the FFT data (otherwise we'll crash later in RoccaContour)
		 */
//		System.out.println(String.format("First time= %d", sliceData.get(0).getFftDataUnit().getTimeMilliseconds() ));
//		System.out.println(String.format("First startSample= %d", sliceData.get(0).getFftDataUnit().getStartSample() ));
//		System.out.println(String.format("PRDB first time= at %d", prdb.getFirstUnit().getTimeMilliseconds() ));
//		System.out.println(String.format("PRDB first startSample= %d", prdb.getFirstUnit().getStartSample() ));

		int[] lowestChanList = new int[1];
		lowestChanList[0] =
				PamUtils.getLowestChannel(roccaControl.roccaParameters.channelMap);
		int firstIndx = prdb.getUnitIndex(prdb.getPreceedingUnit(
				sliceData.get(0).getFftDataUnit().getTimeMilliseconds(),
				PamUtils.makeChannelMap(lowestChanList)));

		/* find the index number of the PamRawDataUnit closest to the start time
		 * of the last FFT data unit, and in the highest channel
		 * position to be saved.  Use the .getNextUnit method to ensure
		 * that the start time of the raw data is later than the start time
		 * of the FFT data (otherwise we'll crash later in RoccaContour)
		 */		 
//		System.out.println(String.format("Last time= %d", sliceData.get(sliceData.size()-1).getFftDataUnit().getTimeMilliseconds() ));
//		System.out.println(String.format("Last startSample= %d", sliceData.get(sliceData.size()-1).getFftDataUnit().getStartSample() ));
//		System.out.println(String.format("PRDB last time= at %d", prdb.getLastUnit().getTimeMilliseconds() ));
//		System.out.println(String.format("PRDB last startSample= %d", prdb.getLastUnit().getStartSample() ));

		int[] highestChanList = new int[1];
		highestChanList[0] =
				PamUtils.getHighestChannel(roccaControl.roccaParameters.channelMap);
		int lastIndx = prdb.getUnitIndex(prdb.getNextUnit(
				sliceData.get(sliceData.size()-1).getFftDataUnit().getTimeMilliseconds(),
				PamUtils.makeChannelMap(highestChanList)));

		/* add the units, from firstIndx to lastIndx, to the new PamRawDataBlock and save this to the contour data block */
		for (int j = firstIndx; j <= lastIndx; j++) {
			newBlock.addPamData
			(prdb.getDataUnit
					(j, PamDataBlock.REFERENCE_CURRENT ));
		}
		roccaContourDataBlock.setPrdb(newBlock);

		/* get an array of all the peak frequencies */
    	double[] whistleFreqs = wmDataUnit.getFreqsHz();

    	/* loop through the slices one at a time, creating a RoccaContourDataUnit for each */
    	for (int i=0; i<sliceData.size(); i++) {
			rcdu = new RoccaContourDataUnit (
					sliceData.get(i).getFftDataUnit().getTimeMilliseconds(),
					sliceData.get(i).getFftDataUnit().getChannelBitmap(),
					sliceData.get(i).getFftDataUnit().getStartSample(),
					sliceData.get(i).getFftDataUnit().getDuration() );
			
			/* set the peak frequency */
			rcdu.setPeakFreq(whistleFreqs[i]);
			
			/* set the time in milliseconds */
			rcdu.setTime(sliceData.get(i).getFftDataUnit().getTimeMilliseconds());
			
			/* set the duty cycle, energy and windowRMS params to 0 since we're not using them */
			rcdu.setDutyCycle(0);
			rcdu.setEnergy(0);
			rcdu.setWindowRMS(0);
			
			/* add the data unit to the data block */
			roccaContourDataBlock.addPamData(rcdu);	
    	}
    	
    	return roccaContourDataBlock;
    }

    
    /**
     * Save the clip as a wav file
     *
     * @param rcdb the contour data block containing the raw data to save
     * @return boolean indicating success
     */
    private boolean saveContour(RoccaContourDataBlock rcdb, 
    		int channel,
    		int thisDetection,
    		String sNum) {

        if (rcdb.getPrdb() == null) {
            System.out.println("RoccaProcess: No source audio data found");
            return false;
        }
        
        long currentStartSample = rcdb.getPrdb().getFirstUnit().getStartSample();
        long deltaMS = (rcdb.getPrdb().getLastUnit().getTimeMilliseconds()-
        		rcdb.getPrdb().getFirstUnit().getTimeMilliseconds());
        float deltaS = ((float) deltaMS)/1000;
        float dur = deltaS*rcdb.getPrdb().getSampleRate();
        int duration = (int) dur;

        /* get the raw data */
        double[][] rawDataValues = null;
        try {
        	rawDataValues = rcdb.getPrdb().getSamples(currentStartSample, duration, channel );
        }
        catch (RawDataUnavailableException e) {
        	System.out.println(e.getMessage());	
        }

        if (rawDataValues==null) {
            System.out.println(String.format
                    ("Error obtaining raw data during contour save; currentStartSample = %d",
                    currentStartSample));
        }

        /* figure out the filename, based on the input filename and the time */
        File contourFilename = getDataBlockFilename(rcdb, ".wav", channel, thisDetection, sNum);

        /* create a new WavFile object */
        WavFile wavFile = new WavFile(contourFilename.getAbsolutePath(), "w");

        /* save the data to a wav file */
//        System.out.println(String.format("writing contour  to ... %s",
//                contourFilename));
        wavFile.write(rcdb.getPrdb().getSampleRate(),
                PamUtils.getNumChannels(channel),
                rawDataValues );
        return true;
    }

    /**
     * Saves all contour stats to the contour stats summary file, as defined in
     * the Rocca Parameters.  If the file does not exist, it is created and a
     * header line is written.  If it does exist, data is appended to the end.
     * Each contour is 1 row.
     *
     * Note that the last few columns contain the random forest votes, and so
     * the number of columns depends on how many species are in the classifier.
     * This won't be a problem if the same classifier is always used, but if a
     * different classifier with a different number of species is tried out the
     * number of columns from one row to the next will not match.  More
     * importantly, the order of the species from one row to the next might
     * not match and you would never know unless you remember.  For this
     * reason, instead of saving the species names as the column headers (which
     * would only be accurate for the classifier selected when the file had
     * been created), the species names are written in the row AFTER the tree
     * votes.  The classifier name is also saved, for reference.  While this
     * is awkward, at least results from different classifiers can be
     * interpreted properly without having to guess which vote is for which species
     *
     * @return boolean indicating whether or not the save was successful
     */
    public boolean saveContourStats(RoccaContourDataBlock rcdb, 
    		int channel,
    		int thisDetection,
    		String sNum) {

        // open file
        try {
            File fName = roccaControl.roccaParameters.roccaContourStatsOutputFilename;
//            System.out.println(String.format("writing contour stats to ... %s", fName.getAbsolutePath()));

            // write a header line if this is a new file
            if (!fName.exists()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fName));
                String hdr = rcdb.createContourStatsHeader();
                writer.write(hdr);
                writer.newLine();
                writer.close();
            }

            /* create a String containing the contour stats and associated
             * variables (filename, species list, detection number, etc) we
             * want to save, and save it to the file.  This method will also
             * create and populate a RoccaLoggingDataUnit
             */
            BufferedWriter writer = new BufferedWriter(new FileWriter(fName, true));
//            String contourStats = roccaContourDataBlock.createContourStatsString();
            String contourStats =
                    rcdb.createContourStatsString
                    ((getDataBlockFilename(rcdb, ".csv", channel, thisDetection, sNum)).getAbsolutePath(),
                    thisDetection);
            writer.write(contourStats);
            writer.newLine();
            writer.close();

        } catch(FileNotFoundException ex) {
            System.out.println("Could not find file");
            ex.printStackTrace();
            return false;
        } catch(IOException ex) {
            System.out.println("Could not write contour statistics to file");
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Saves the contour points in the datablock in a csv file.  Saves the time
    * and peak frequency of each point, along with the duty cycle, energy, and
    * windows RMS
    *
     * @param roccaContourDataBlock the datablock containing the selected
     *                              whistle contour
     * @return saveContourPoints boolean indicating success or failure
     */
    public boolean saveContourPoints(RoccaContourDataBlock rcdb, 
    		int channel,
    		int thisDetection,
    		String sNum) {

        // figure out the filename, based on the input filename and the time
        File contourFilename = getDataBlockFilename(rcdb, ".csv", channel, thisDetection, sNum);

        try {
//            System.out.println(String.format("writing contour points to ... %s",
//                    contourFilename));
            BufferedWriter writer = new BufferedWriter(new FileWriter(contourFilename));

            // Write the header line
            String hdr = new String("Time [ms], Peak Frequency [Hz], Duty Cycle, Energy, WindowRMS");
            writer.write(hdr);
            writer.newLine();

            /* Cycle through the contour points one at a time.  Save the points
             * that are within the range selected by the user (points outside
             * of the range will have a frequency equal to the constant
             * NO_CONTOUR_HERE
             */
            RoccaContourDataUnit rcdu = null;
            int numPoints = rcdb.getUnitsCount();
            for (int i = 0; i < numPoints; i++) {
                rcdu = rcdb.getDataUnit(i, RoccaContourDataBlock.REFERENCE_CURRENT);
                if (rcdu.getPeakFreq() != RoccaParameters.NO_CONTOUR_HERE ) {
                    String contourPoints =   rcdu.getTime() + "," +
                            rcdu.getPeakFreq() + "," +
                            rcdu.getDutyCycle() + "," +
                            rcdu.getEnergy() + "," +
                            rcdu.getWindowRMS();
                    writer.write(contourPoints);
                    writer.newLine();
                }
            }
            writer.close();
        } catch(FileNotFoundException ex) {
            System.out.println("Could not find file");
            ex.printStackTrace();
            return false;
        } catch(IOException ex) {
            System.out.println("Could not write contour points to file");
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Generates a filename for the wav clip and the csv file containing the
     * contour points, based on the filename template stored in RoccaParameters
     *
     * @param rcdb The RoccaContourDataBlock containing the data to save
     * @param ext The filename extension (wav or csv)
     * @return A File
     */
    public File getDataBlockFilename
    		(RoccaContourDataBlock rcdb,
    		String ext,
    		int channel,
    		int thisDetection,
    		String sNum) {
        File soundFile = rcdb.getWavFilename();
		FileParts fileParts = new FileParts(soundFile);
		String nameBit = fileParts.getFileName();
        
        /* get the filename template */
        String filenameTemplate = roccaControl.roccaParameters.getFilenameTemplate();
        String filenameString = "";
        char c;

        /* step through the template, one character at a time, looking for codes */
        for ( int i=0,n=filenameTemplate.length(); i<n; i++ ) {
            c = filenameTemplate.charAt(i);

            /* if we've hit a symbolic code, determine which one it is and
             * substitute the correct text
             */
            if (c=='%') {
                if (i==n-1) break;
                i++;
                c = filenameTemplate.charAt(i);
                switch (c) {

                    /* source name */
                    case 'f':
//                        File soundFile = rcdb.getWavFilename();
//                        FileParts fileParts = new FileParts(soundFile);
//                        String nameBit = fileParts.getFileName();
                        filenameString = filenameString + nameBit;
                        break;

                    /* detection number */
                    case 'n':
                        filenameString = filenameString + sNum;
                        break;

                    /* detection tally */
                    case 'X':
                        filenameString = filenameString +
                                String.format("%d", thisDetection);
                        break;

                    /* channel/track number */
                    case 't':
                        filenameString = filenameString +
                                String.format("%d", channel);
                        break;

                    /* it has something to do with the date or time, so initialize
                     * some calendar variables
                     */
                    default:
                        long firstTime = rcdb.getFirstUnit().getTime();
                        Calendar cal = PamCalendar.getCalendarDate(firstTime);
                        DateFormat df = new SimpleDateFormat("yyyy");
                        DecimalFormat decf = new DecimalFormat(".#");
                        Double msAsDouble = 0.0;
                        int msAsInt = 0;

                        switch (c) {

                            /* year, 4 digits */
                            case 'Y':
                                df = new SimpleDateFormat("yyyy");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* year, 2 digits */
                            case 'y':
                                df = new SimpleDateFormat("yy");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* month */
                            case 'M':
                                df = new SimpleDateFormat("MM");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* day of the month */
                            case 'D':
                                df = new SimpleDateFormat("dd");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* day of the year (3 digits) */
                            case 'J':
                                df = new SimpleDateFormat("DDD");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* hour, 24-hour clock */
                            case 'H':
                                df = new SimpleDateFormat("kk");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* hour, 12-hour clock */
                            case 'h':
                                df = new SimpleDateFormat("hh");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* 'am' or 'pm' */
                            case 'a':
                                df = new SimpleDateFormat("a");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* minute */
                            case 'm':
                                df = new SimpleDateFormat("mm");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* second */
                            case 's':
                                df = new SimpleDateFormat("ss");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* second of the day (5 digits) */
                            case 'S':
                                int secOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 +
                                        cal.get(Calendar.MINUTE) * 60 +
                                        cal.get(Calendar.SECOND);
                                filenameString = filenameString +
                                        String.format("%05d", secOfDay);
                                break;

                            /* tenths of a second */
                            case 'd':
                                df = new SimpleDateFormat(".SSS");
                        		df.setTimeZone(cal.getTimeZone());
                                msAsDouble = Double.valueOf(df.format(cal.getTime()).toString());
                                decf = new DecimalFormat(".#");
                                msAsInt = (int) (Double.valueOf(decf.format(msAsDouble))*10);
                                filenameString = filenameString + String.format("%01d", msAsInt);
                                break;

                            /* hundredths of a second */
                            case 'c':
                                df = new SimpleDateFormat(".SSS");
                        		df.setTimeZone(cal.getTimeZone());
                                msAsDouble = Double.valueOf(df.format(cal.getTime()).toString());
                                decf = new DecimalFormat(".##");
                                msAsInt = (int) (Double.valueOf(decf.format(msAsDouble))*100);
                                filenameString = filenameString + String.format("%02d", msAsInt);
                                break;

                            /* thousandths of a second */
                            case 'i':
                                df = new SimpleDateFormat("SSS");
                        		df.setTimeZone(cal.getTimeZone());
                                filenameString = filenameString +
                                        df.format(cal.getTime());
                                break;

                            /* code not recognized; just use the % sign and character */
                            default:
                                filenameString = filenameString + "%" + c;
                                break;
                    }
                }

            /* if we haven't hit a code, just copy the character to the filename
             * string
             */
            } else {
               filenameString = filenameString + c;
            }
        }
        filenameString = filenameString + ext;
//       return fName;
       return new File(roccaControl.roccaParameters.roccaOutputDirectory.getAbsolutePath(),
               filenameString);
   }



}
