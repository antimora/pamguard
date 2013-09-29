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

import PamUtils.PamCalendar;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamBorderPanel;
import PamView.PamColors;
import PamView.PamSidePanel;
import PamView.PamColors.PamColor;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.String;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

public class RoccaSidePanel implements PamSidePanel , PamObserver {

	RoccaControl roccaControl;
	
	SidePanel sidePanel;
	TitledBorder titledBorder;
	
    RoccaSightingDataBlock rsdb;
    RoccaSightingDataUnit currentUnit=null;

    RoccaDetectionLogger rdl;

    JTextField sightingNum = new JTextField(15);
    JTextField sightingClass = new JTextField(10);
    JTextField startTime = new JTextField(15);
    JTextField startFreq = new JTextField(15);
    JTextField endTime = new JTextField(15);
    JTextField endFreq = new JTextField(15);
    JLabel sightingSaved;
    JTextField[] speciesClassCountTxt;
    JButton prevSight;
    JButton nextSight;
    JButton renameSighting;
    JButton newSightingNum;
    JButton deleteSighting;
    JButton saveSighting;

    /*
     * the frequency, in milliseconds, of the sighting data autosave function.  The
     * default is 600,000 milliseconds (10 minutes)
     */
    int autosaveFreq = 1000 * 60 * 10;      // 10 minutes

    int currentSighting;
	

	public RoccaSidePanel(RoccaControl roccaControl) {
		this.roccaControl = roccaControl;

        /* create a new SightingDataBlock and set it's natural lifetime to a
         * maximum so it never gets deleted
         */

        this.rsdb = new RoccaSightingDataBlock
                (roccaControl.roccaProcess,
                roccaControl.roccaParameters.getChannelMap());
        rsdb.setNaturalLifetimeMillis(Integer.MAX_VALUE);
        rdl = new RoccaDetectionLogger(this, rsdb);
        rsdb.SetLogging(rdl);
        rsdb.setMixedDirection(PamDataBlock.MIX_INTODATABASE);
        roccaControl.roccaProcess.addOutputDataBlock(rsdb);

        /* create a new SightingDataUnit and add it to the block; we don't have
         * a sighting number yet, but when the classifier is loaded an error
         * will be thrown if we haven't created a unit
         */
//        this.currentUnit = new RoccaSightingDataUnit
//                (PamCalendar.getTimeInMillis(),0,0,0);
//        rsdb.addPamData(currentUnit);

		/* create a new SidePanel */
		sidePanel = new SidePanel();
	}

    public RoccaSightingDataBlock getRSDB() {
        return rsdb;
    }

	@Override
	public PamObserver getObserverObject() {
		return this;
	}


	public class SidePanel extends PamBorderPanel {

		public SidePanel() {
			super();
			setBorder(titledBorder = new TitledBorder(roccaControl.getUnitName()));
			titledBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));

            GridBagLayout gb = new GridBagLayout();
			setLayout(gb);

            /* if we've loaded a classifier, draw the species list */
            if (roccaControl.roccaProcess.isClassifierLoaded() == true) {
                drawThePanel();
                
            /* otherwise, warn the user that no classifier has been loaded */
            } else {
    			GridBagConstraints c = new GridBagConstraints();
                c.anchor = GridBagConstraints.NORTH;
                c.ipadx = 5;
                c.gridx = c.gridy = 0;
                c.gridwidth = 2;
                addComponent(this, new JLabel("No Classifier Loaded"), c);
            }
		}


		@Override
		public void setBackground(Color bg) {
			super.setBackground(bg);
			if (titledBorder != null) {
				titledBorder.setTitleColor(PamColors.getInstance().getColor(PamColor.AXIS));
			}
		}

        public void drawThePanel() {
            
            /* set the constraints */
			GridBagConstraints c = new GridBagConstraints();

            /* add the sighting number */
			c.anchor = GridBagConstraints.PAGE_START;
//			c.ipadx = 5;
			c.gridx = c.gridy = 0;
			c.gridwidth = 2;
			addComponent(this, new JLabel("Detection Number"), c);
            c.insets = new Insets(0,0,0,0);
			c.gridx = 0;
			c.gridy ++;
            c.gridwidth = 2;
            addComponent(this, sightingNum, c);
            if (currentUnit==null) {
                sightingNum.setText(RoccaSightingDataUnit.NONE);
            } else {
                sightingNum.setText(currentUnit.getSightNum());
                sightingNum.setHorizontalAlignment(SwingConstants.CENTER);
                sightingNum.setEditable(false);
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 1;
                c.insets = new Insets(0,0,20,0);
                c.anchor = GridBagConstraints.LINE_END;
                prevSight = new JButton("<");
                addComponent(this, prevSight, c );
                prevSight.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        prevSighting();
                    }
                });
                c.gridx++;
                c.anchor = GridBagConstraints.LINE_START;
                nextSight = new JButton(">");
                addComponent(this, nextSight, c);
                nextSight.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        nextSighting();
                    }
                });
                enableFlipButtons();

                /* add the species list */
                c.insets = new Insets(0,0,0,0);
                c.gridwidth = 1;
                c.gridx = 0;
                c.gridy ++;
                c.anchor = GridBagConstraints.PAGE_START;
                speciesClassCountTxt = new JTextField[currentUnit.getSpecies().length];
                for (int i=0; i<currentUnit.getSpecies().length; i++) {
                    addComponent(this, currentUnit.species[i], c);
                    c.gridx ++;
                    speciesClassCountTxt[i] = new JTextField(5);
                    speciesClassCountTxt[i].setText(""+currentUnit.speciesClassCount[i]);
                    speciesClassCountTxt[i].setEditable(false);
                    addComponent(this, speciesClassCountTxt[i],c);
                    c.gridx = 0;
                    c.gridy ++;
                }

                /* add the sighting classification */
    //            currentUnit.classifySighting
    //                    (roccaControl.roccaParameters.getSightingThreshold());
                c.insets = new Insets(20,0,0,0);
                c.gridwidth = 2;
                c.gridx = 0;
                c.gridy++;
                addComponent(this, new JLabel("School Classification"), c);
                c.insets = new Insets(0,0,20,0);
                c.gridx = 0;
                c.gridy ++;
                addComponent(this, sightingClass, c);
                sightingClass.setText(currentUnit.getSightClass());
                sightingClass.setHorizontalAlignment(SwingConstants.CENTER);
                sightingClass.setEditable(false);

                /* add the buttons */
                c.insets = new Insets(0,0,3,0);
                c.gridwidth = 2;
                c.gridx = 0;
                c.gridy++;
                renameSighting = new JButton("Rename Detection");
                addComponent(this, renameSighting, c);
                renameSighting.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
    //                    String oldNum = currentUnit.getSightNum();
    //                    currentUnit.setSightNum(RoccaSightingDataUnit.NONE);
    //                    String dummy = addASighting(false);
    //                    if (currentUnit.getSightNum().equalsIgnoreCase
    //                            (RoccaSightingDataUnit.NONE)) {
    //                        currentUnit.setSightNum(oldNum);
    //                    }
                        renameSightingAndFiles();
                    }
                });
                c.gridy++;
                saveSighting = new JButton("Save Detection Now");
                addComponent(this, saveSighting, c);
                saveSighting.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        saveSighting();
                    }
                });
    //            c.gridy++;
    //            deleteSighting = new JButton("Delete Sighting");
    //            addComponent(this, deleteSighting, c);
    //            deleteSighting.addActionListener(new ActionListener() {
    //                public void actionPerformed(ActionEvent evt) {
    //                    deleteSighting();
    //                }
    //            });
                c.gridy++;
                newSightingNum = new JButton("New Detection");
                addComponent(this, newSightingNum, c);
                newSightingNum.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        String dummy = addASighting(false);
                    }
                });

                /* make the buttons the same size (delete button is biggest) */
    //            Dimension buttonWidth = deleteSighting.getPreferredSize();
                Dimension buttonWidth = renameSighting.getPreferredSize();
    //            deleteSighting.setMinimumSize(buttonWidth);
    //            deleteSighting.setMaximumSize(buttonWidth);
                saveSighting.setPreferredSize(buttonWidth);
                saveSighting.setMinimumSize(buttonWidth);
                saveSighting.setMaximumSize(buttonWidth);
                renameSighting.setPreferredSize(buttonWidth);
                renameSighting.setMinimumSize(buttonWidth);
                renameSighting.setMaximumSize(buttonWidth);
                newSightingNum.setPreferredSize(buttonWidth);
                newSightingNum.setMinimumSize(buttonWidth);
                newSightingNum.setMaximumSize(buttonWidth);

                /* add the start/end points */
                c.insets = new Insets(20,0,0,0);
                c.gridwidth = 2;
                c.gridx = 0;
                c.gridy++;
                addComponent(this, new JLabel("Whistle Start"), c);
                c.insets = new Insets(0,0,0,0);
                c.gridx = 0;
                c.gridy ++;
                addComponent(this, startTime, c);
                startTime.setHorizontalAlignment(SwingConstants.CENTER);
                startTime.setEditable(false);
                startTime.setText("<not selected>");
                c.gridy ++;
                addComponent(this, startFreq, c);
                startFreq.setHorizontalAlignment(SwingConstants.CENTER);
                startFreq.setEditable(false);
                startFreq.setText("<not selected>");
                c.insets = new Insets(20,0,0,0);
                c.gridwidth = 2;
                c.gridx = 0;
                c.gridy++;
                addComponent(this, new JLabel("Whistle End"), c);
                c.insets = new Insets(0,0,0,0);
                c.gridx = 0;
                c.gridy ++;
                addComponent(this, endTime, c);
                endTime.setHorizontalAlignment(SwingConstants.CENTER);
                endTime.setEditable(false);
                endTime.setText("<not selected>");
                c.gridy ++;
                addComponent(this, endFreq, c);
                endFreq.setHorizontalAlignment(SwingConstants.CENTER);
                endFreq.setEditable(false);
                endFreq.setText("<not selected>");
            }

            /* clean up the display */
            sidePanel.revalidate();
            sidePanel.repaint();
        }

        /**
         * enable/disable the sighting scroll buttons, depending on where we
         * are in the sighting list
         */
        public void enableFlipButtons() {
            int i = rsdb.getUnitIndex(currentUnit);
            if (i==0) {
                prevSight.setEnabled(false);
            } else {
                prevSight.setEnabled(true);
            }

            if (i==rsdb.getUnitsCount()-1) {
                nextSight.setEnabled(false);
            } else {
                nextSight.setEnabled(true);
            }
        }

        public void prevSighting() {
            int i = rsdb.getUnitIndex(currentUnit);
            i--;
            currentUnit = rsdb.getDataUnit(i,PamDataBlock.REFERENCE_CURRENT);

            /* clear anything in the existing sidePanel and redraw */
            sidePanel.removeAll();
            sidePanel.drawThePanel();
        }

        public void nextSighting() {
            int i = rsdb.getUnitIndex(currentUnit);
            i++;
            currentUnit = rsdb.getDataUnit(i,PamDataBlock.REFERENCE_CURRENT);

            /* clear anything in the existing sidePanel and redraw */
            sidePanel.removeAll();
            sidePanel.drawThePanel();
            
        }

        public void clearSpeciesCount() {
            currentUnit.clearCounts();
            for (int i=0; i<currentUnit.getSpecies().length; i++) {
                speciesClassCountTxt[i].setText
                        (""+
                        currentUnit.getClassCount(i));
            }
            sidePanel.repaint();
        }

        /**
         * Rename the sighting number and the files with that sighting number
         * as well.  This routine sets the current sighting number to NONE, and
         * then calls the addASighting method.  Since the current sighting
         * number is NONE, addASighting will simply change the text of the
         * current sighting instead of creating a whole new sighting.
         */
        public void renameSightingAndFiles() {
            String oldNum = currentUnit.getSightNum();
            currentUnit.setSightNum(RoccaSightingDataUnit.NONE);
            String dummy = addASighting(false);

            /*
             * if something happened while trying to change the sighting number,
             * just set it back to what it was before
             */
            if (currentUnit.getSightNum().equalsIgnoreCase
                    (RoccaSightingDataUnit.NONE)) {
                currentUnit.setSightNum(oldNum);
            }

            /*
             * if there weren't any errors, try renaming the csv and wav files
             * as well
             */
            else {
                String newNum = currentUnit.getSightNum();
                File saveDir = roccaControl.roccaParameters.roccaOutputDirectory;
                FilenameFilter sightFilter = new SightNumFilter(oldNum);
                File[] sightFiles = saveDir.listFiles(sightFilter);
//                String[] newName = new String[sightFiles.length];
                String oldName = null;
                String newName = null;
                File newFilename = null;
                for (int i=0; i< sightFiles.length; i++) {
//                    newName[i] = sightFiles[i].replaceAll(oldNum, newNum);
                    oldName = sightFiles[i].getName();
                    newName = oldName.replaceAll(oldNum, newNum);
                    newFilename = new File(saveDir, newName);
                    if (!sightFiles[i].renameTo(newFilename)) {
                        System.out.println("Error renaming "+oldName);
                    }
                }
            }
        }

        /**
         * Inner class to return a list of files that start with the sighting
         * number
         */
        private class SightNumFilter implements FilenameFilter {
            String sNum;

            /**
             * Constructor
             * @param sNum the sighting number to search for
             */
            public SightNumFilter(String sNum) {
                this.sNum = sNum + "-";
            }
            public boolean accept(File dir, String name) {
                return name.startsWith(sNum);
            }
        }

        /**
         * Save the sighting information immediately
         *
         */
        public void saveSighting() {

            // open file
            try {
                File fName = roccaControl.roccaParameters.getRoccaSightingStatsOutputFilename();
//                System.out.println(String.format("writing detection stats to ... %s", fName.getAbsolutePath()));

                // write a header line if this is a new file
//                if (!fName.exists()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fName,false));
                String hdr = createDetectionStatsHeader();
                writer.write(hdr);
                writer.newLine();
//                    writer.close();
//                }

//                BufferedWriter writer = new BufferedWriter(new FileWriter(fName, true));
//                writer = new BufferedWriter(new FileWriter(fName, true));
                /*
                 * loop through the roccaSightingDataUnits, writing the data
                 * for each to the file
                 */
                String detectionStats = null;
                for (int i=0; i<rsdb.getUnitsCount(); i++) {
                    detectionStats = createDetectionStatsString
                            (rsdb.getDataUnit
                            (i, PamDataBlock.REFERENCE_CURRENT));
                    writer.write(detectionStats);
                    writer.newLine();
                }
//                String detectionStats = createDetectionStatsString();
//                writer.write(detectionStats);
//                writer.newLine();
                writer.close();
            } catch(FileNotFoundException ex) {
                System.out.println("Could not find file");
                ex.printStackTrace();
            } catch(IOException ex) {
                System.out.println("Could not write detection statistics to file");
                ex.printStackTrace();
            }

            /* set the flag and label to indicate a successful save */
            currentUnit.setSightingSaved(true);
        }

        /**
         * Creates a string containing the header information for the detection
         * stats file
         *
         * @return a String containing the header
         */
        public String createDetectionStatsHeader() {
            String detectionHeader = "Detection Number, ";

            /* add the species names for the tally */
            for (JLabel sp : currentUnit.species) {
                detectionHeader = detectionHeader + sp.getText() + ", ";
            }

            /* add the species names for the tree vote information */
            for (JLabel sp : currentUnit.getSpecies()) {
                detectionHeader = detectionHeader + sp.getText() + " votes, ";
            }

            /* add the species list double-check and overall detection
             * classification
             */
            detectionHeader = detectionHeader + "Species List, Detection Classification";

            // remove the last ", " in the string and return
    //        detectionHeader = detectionHeader.substring(0, detectionHeader.length()-2);
            return detectionHeader;
        }

        /**
         * Creates a string containing the data for the passed detection.  The
         * data includes the detection number, the counts for the current species
         * list, the tree votes for the current species list, a species list
         * combined into a single column as a double-check against the column
         * headers (in case the classifier has changed), and finally the
         * detection classification
         * <p>
         * Note that the {@link #loadSighting() loadSighting()} method reads in
         * the sighting stats information in the same order as shown here;
         * if this format is changed, the loadSighting() method must be changed
         * as well.
         *
         * @param unit the RoccaSightingDataUnit to get the data from
         * @return a string containing the current detection data
         */
        public String createDetectionStatsString
                (RoccaSightingDataUnit unit) {
            /* First add the detection number */
            String detectionString = unit.getSightNum();

            /* Add the tally counts for the species list */
            for (int spCount : unit.getSpeciesClassCount()) {
                detectionString += ", " + spCount;
            }

            /* Add the cumulative tree votes for the species list */
            for (double spVote : unit.getSpeciesTreeVotes()) {
                detectionString += ", " + spVote;
            }

            /* Add the species list, as a double-check in case the classifier
             * has changed and the column headers are no longer valid
             */
            String temp = ", (";
            for (String sp : unit.getSpeciesAsString()) {
                temp += sp + "-";
            }
            detectionString += temp + "), ";

            /* Add the detection classification */
            detectionString += unit.getSightClass();

            /* return the detectionString */
            return detectionString;
        }


        /**
         * Load the sighting information from an existing file.  Note that this
         * method assumes the csv file has the same format as the one specified
         * in the {@link createDetectionStatsString
         * (RoccaSightingDataUnit unit) createDetectionStatsString} method.  If
         * the format in that method is ever changed, it should be changed here
         * as well.  Note also that this method does not check the existing
         * datablock for duplicate sighting numbers.  Since (at the moment) this
         * method is only called to load a file when Pamguard starts and the
         * datablock is empty, this isn't a problem.
         */
        public void loadSighting() {

            try {
                File fName = roccaControl.roccaParameters.getRoccaSightingStatsOutputFilename();

                /* load the header */
                BufferedReader reader = new BufferedReader(new FileReader(fName));
                String fileHdr = reader.readLine();

                /* create a new RoccaSightingDataUnit, and fill it with data
                 * from the file.  Once filled, we will add it to the rsdb
                 */
                RoccaSightingDataUnit tempUnit = null;

                /*
                 * load one line at a time, and parse the string into an array
                 * using the comma as a delimiter
                 */
                String line = null;
                String delims = "[,]";
                String spDelims = "[-]";

                while((line=reader.readLine()) != null) {
                    tempUnit = new RoccaSightingDataUnit
                            (PamCalendar.getTimeInMillis(),0,0,0);
                    String[] lineArray = line.split(delims);

                    /* the first array index is the sighting number */
                    tempUnit.setSightNum(lineArray[0]);

                    /* the last array index is the school classification */
                    tempUnit.setSightClass(lineArray[lineArray.length-1]);

                    /* the second last array index is the species list.  The
                     * string starts with '(' and ends with '-)', so drop the
                     * first character and last two characters.
                     * Each species name is separated from the
                     * other by a '-', so parse on that delimiter.
                     */
                    String tempSpecies = lineArray[lineArray.length-2].trim();
                    String tempSpeciesSub = tempSpecies.substring(1,tempSpecies.length()-2);
                    String[] speciesArray = tempSpeciesSub.split(spDelims);
                    tempUnit.setSpecies(speciesArray);

                    /* between the sighing number and the species list are
                     * a set of x integers representing the tally
                     * counts for each species, followed by x doubles
                     * representing the tree votes for each species.  The problem
                     * is that 'x' is the number of species, and that depends
                     * on the classifier used.  'x' can change within the file,
                     * so we have to calculate it for each line.
                     */
                    int numSpecies = (lineArray.length
                            -2      // subtract the last two columns in the CSV
                            -1)     // subtract the first column in the CSV
                            / 2;    // divide by two, to split the tally count
                                    // from the tree votes

                    int[] tallyCount = new int[numSpecies];
                    for (int i=0; i<numSpecies; i++) {
                        tallyCount[i]=Integer.parseInt(lineArray[i+1].trim());
                    }
                    tempUnit.setSpeciesClassCount(tallyCount);

                    double[] treeVotes = new double[numSpecies];
                    for (int i=0; i<numSpecies; i++) {
                        treeVotes[i]=Double.parseDouble(lineArray[numSpecies+i+1].trim());
                    }
                    tempUnit.addSpeciesTreeVotes(treeVotes);
                    updateSightingClass();

                    /* add the unit to the data block and update the classification */
                    rsdb.addPamData(tempUnit);


                    /* update the detection count */
                    RoccaSpecPopUp.incNumDetections();
                }

                /* close the reader */
                reader.close();

                /* if we've loaded detection data and the first unit in the
                 * rsdb is still <none> (the default), get rid of it
                 */
                RoccaSightingDataUnit firstUnit = rsdb.getFirstUnit();
                if (rsdb.getUnitsCount()>0 &
                        firstUnit.getSightNum().equals(RoccaSightingDataUnit.NONE)) {
                    rsdb.remove(firstUnit);
                }
                
                /* set the current data unit field to the last temporary unit */
                currentUnit = tempUnit;

                /* clear anything in the existing sidePanel and draw the new one */
                sidePanel.removeAll();
                sidePanel.drawThePanel();

            } catch(FileNotFoundException ex) {
                System.out.println("Could not find file");
                ex.printStackTrace();
            } catch(IOException ex) {
                System.out.println("Could not write detection statistics to file");
                ex.printStackTrace();
            }
        }

        /**
         * Delete a detection
         *
         * Currently untested and not attached to any buttons
         */
        public void deleteSighting() {
            RoccaSightingDataUnit removeThis = currentUnit;
            if (rsdb.getUnitsCount()==1) {
                String dummy = addASighting(false);
            } else {
                int i = rsdb.getUnitIndex(currentUnit);

                /* if we're on the first unit, go to the next */
                if (i==1) {
                    i++;

                /* otherwise go to the previous unit */
                } else {
                    i--;
                }
                currentUnit = rsdb.getDataUnit(i, PamDataBlock.REFERENCE_CURRENT);
            }

            /* delete the unit */
            rsdb.remove(removeThis);

            /* clear anything in the existing sidePanel and redraw */
            sidePanel.removeAll();
            sidePanel.drawThePanel();
        }

//        public void clearSpeciesList() {
//            for (int i=0; i<species.length; i++) {
//                this.remove(species[i]);
//                this.remove(speciesClassCountTxt[i]);
//            }
//            sidePanel.validate();
//        }

        /**
         * Ask the user for a new sighting number, and add it to the side panel
         * 
         * @param duplicatesOk Boolean indicating whether or not it's ok to use duplicate sighting numbers
         * @return the new sighting number
         */
        public String addASighting(boolean duplicatesOk) {
        	
            /* pop up a window asking for the new sighting number */
            boolean numBad;
            RoccaSightingDataUnit unit;
            String newSNum;
            String message = "Enter new detection number";
            int messageType = JOptionPane.PLAIN_MESSAGE;
            do {
                numBad = false;
                newSNum = JOptionPane.showInputDialog
                        (sidePanel,
                        message,
                        "Add Detection",
                        messageType);

                if (newSNum==null) break;
                
                for (int i=0; i<rsdb.getUnitsCount(); i++) {
                    unit = rsdb.getDataUnit
                            (i, PamDataBlock.REFERENCE_CURRENT);
                    if (newSNum.equalsIgnoreCase(unit.getSightNum())) {
                        if (duplicatesOk) {
                            return newSNum;
                        } else {
                            numBad = true;
                            message = "Detection already exists - enter new number";
                            messageType = JOptionPane.ERROR_MESSAGE;
                        }
                    }
                }
            } while (numBad);

            /* if the user didn't hit cancel.... */
            if (newSNum != null ) {
            	return addASighting(newSNum);
            } else {
            	return RoccaSightingDataUnit.NONE;
            }
        }
        
        /**
         * Add the new sighting number to the sidepanel
         * 
         * @param newSNum the new sighting number to be used
         * @return the sighting number
         */
        public String addASighting(String newSNum) {
            /* check if this is the first sighting added; if it is, just
             * change the sighting number text field.  If it isn't, create
             * a new data unit.  Also, start the autosave timer
             */
            if (currentUnit.getSightNum().equals(RoccaSightingDataUnit.NONE)) {

                /* if all we're doing is changing the sighting number,
                 * set the logging policy to writenew so that a new database
                 * record gets written.  Otherwise, it may try to rewrite
                 * a nonexistent entry (because upon start-up the blank
                 * dataunit gets added to the datablock BEFORE a database
                 * observer is created, so it never gets written to the
                 * database)
                 */
//                    PamProcess.newData(rsdb, currentUnit);
                rsdb.remove(currentUnit);
//                    currentUnit.setSightNum(newSNum);

//                    sightingNum.setText(newSNum);
//                    sidePanel.repaint();

                Timer autosaveTimer = new Timer();
                autosaveTimer.schedule(new TimerTask() {
                    @Override
					public void run() {
                        saveSighting();
                    }
                }, autosaveFreq, autosaveFreq);
            }

            /* create a new RoccaSightingDataUnit and add it to the block.  Use
             * the classifier species list for the new unit
             */
            RoccaSightingDataUnit rsdu = new RoccaSightingDataUnit
            		(PamCalendar.getTimeInMillis(),
            				0,
            				0,
            				0,
            				newSNum,
            				roccaControl.roccaProcess.roccaClassifier.getClassifierSpList());
            rsdb.addPamData(rsdu);

            /* set the current data unit field to this new one */
            currentUnit = rsdu;

            /* clear anything in the existing sidePanel and draw the new one */
            sidePanel.removeAll();
            sidePanel.drawThePanel();
            return newSNum;
        }
	}


	public JComponent getPanel() {
		return sidePanel;
	}

    
	public void rename(String newName) {
		titledBorder.setTitle(newName);	
		sidePanel.repaint();		
	}
	
    
	public String getObserverName() {
		return "Rocca side panel";
	}

    
    /**
     * Creates a new list of species and sets the counts to 0.  Repaints the
     * sidepanel once the fields are updated
     * 
     * @param speciesAsString   a string array of species names (max 5 characters).
     * Note that "Ambig" is automatically added to the beginning of the list, and
     * should note be included in the speciesAsString parameter.
     */
    public void setSpecies(String[] speciesAsString) {
        currentUnit.setSpecies(speciesAsString);

        /* clear anything in the existing sidePanel and redraw it */
        sidePanel.removeAll();
        sidePanel.drawThePanel();
    }

    public void updateSightingClass() {
        currentUnit.classifySighting
                (roccaControl.roccaParameters.getSightingThreshold());
        double maxVal = currentUnit.classifySighting();
        if (maxVal <
                (((double) roccaControl.roccaParameters.getSightingThreshold())/100)) {
            currentUnit.setSightClass(RoccaClassifier.AMBIG);
        }
        sightingClass.setText(currentUnit.getSightClass());
        sidePanel.repaint();
    }


    /**
     * Returns the classification counts for the current species list
     * 
     * @return  an int array containing the classification counts
     */
    public int[] getSpeciesClassCount() {
        return currentUnit.getSpeciesClassCount();
    }


    /**
     * Sets all the species counts.
     *
     * @param speciesClassCount   an int array with the counts for each species.
     *   The length of the array must match the length of the species list
     */
    public void setSpeciesClassCount(int[] speciesClassCount) {
        currentUnit.setSpeciesClassCount(speciesClassCount);
        updateSightingClass();
        sidePanel.repaint();
    }


    /**
     * Searches through the Sighting Data Block for the passed sighting number.
     * If the sighting number is not found, a "-1" is returned
     *
     * @param sNum The sighting number to search for
     * @return The RoccaSightingDataUnit matching the sighting number passed, or
     * a new RoccaSightingDataUnit if the sighting number wasn't found
     * found
     */
    public RoccaSightingDataUnit findSightingUnit(String sNum) {
        RoccaSightingDataUnit unit = null;
        int sightIndx = -1;

        /* search for the sighting number */
        for (int i=0; i<rsdb.getUnitsCount(); i++) {
            unit = rsdb.getDataUnit(i, PamDataBlock.REFERENCE_CURRENT);
            if (sNum.equalsIgnoreCase(unit.getSightNum())) {
                sightIndx = i;
                break;
            }
        }

        /* if we couldn't find the sighting number, create a new one */
        if (sightIndx==-1) {
            unit = new RoccaSightingDataUnit
                    (PamCalendar.getTimeInMillis(),
                    0,0,0, sNum, currentUnit.getSpecies());
            rsdb.addPamData(unit);
            currentUnit = unit;
        }
        return unit;
    }


    /**
     * Increments a specific species classification count
     *
     * @param speciesToInc the index of the species to increment
     */
    public void incSpeciesCount(String sNum, int speciesToInc) {
        RoccaSightingDataUnit unit = findSightingUnit(sNum);

        /* add to the correct species */
        if (speciesToInc<unit.getSpecies().length) {
            unit.incSpeciesCount(speciesToInc);
            if (unit.equals(currentUnit)) {
                speciesClassCountTxt[speciesToInc].setText
                        (""+unit.speciesClassCount[speciesToInc]);
                updateSightingClass();
                sidePanel.repaint();
            }
        }
    }

    /**
     * Increments a specific species classification count
     *
     * @param sNum the sighting number to increment
     * @param speciesToInc the index of the species to increment
     */
    public void incSpeciesCount(String sNum, String speciesToInc) {
        RoccaSightingDataUnit unit = findSightingUnit(sNum);
        
        /* add to the correct species */
        for (int i=0; i<unit.getSpecies().length; i++) {
            if (unit.species[i].getText().equalsIgnoreCase(speciesToInc)) {
                unit.incSpeciesCount(i);
                if (unit.equals(currentUnit)) {
                    speciesClassCountTxt[i].setText(""+unit.speciesClassCount[i]);
                    updateSightingClass();
                    sidePanel.repaint();
                }
            }
        }
    }

    /**
     * Finds the RoccaSightingDataUnit with the passed sighting number, and adds
     * the tree votes to the running total.  If the sighting number is not
     * found, a new RoccaSightingDataUnit is created.
     *
     * @param sNum The sighting number to search for
     * @param newTreeVotes An array of tree votes
     */
    public void addSpeciesTreeVotes(String sNum, double[] newTreeVotes) {
        /* first, find the passed sighting number.  If it doesn't exist, create
         * a new sighting data unit
         */
        RoccaSightingDataUnit unit = findSightingUnit(sNum);
        unit.addSpeciesTreeVotes(newTreeVotes);
        updateSightingClass();
    }


    public String getSightingNum() {
        return currentUnit.getSightNum();
    }

    public void setStartOfWhistle(long time, double freq) {
        this.startTime.setText(String.format("%d ms", time));
        this.startFreq.setText(String.format("%3.1f Hz", freq));
        this.endTime.setText("<not selected>");
        this.endFreq.setText("<not selected>");
        sidePanel.repaint();
    }

    public void setEndOfWhistle(long time, double freq) {
        this.endTime.setText(String.format("%d ms", time));
        this.endFreq.setText(String.format("%3.1f Hz", freq));
        sidePanel.repaint();
    }

    public int getAutosaveFreq() {
        return autosaveFreq;
    }

    /**
     * Note: this method changes the field, but does not update the timer.  In
     * order to adjust the period the old timer would need to be deleted and a
     * new timer started.  Could also use a ScheduledThreadPoolExecutor...
     * 
     * @param autosaveFreq
     */
    public void setAutosaveFreq(int autosaveFreq) {
        this.autosaveFreq = autosaveFreq;
    }

    public void setCurrentUnit(RoccaSightingDataUnit currentUnit) {
        this.currentUnit = currentUnit;
    }

	public long getRequiredDataHistory(PamObservable o, Object arg) {
		return 60000;
	}

	public void noteNewSettings() {
	}

	public void removeObservable(PamObservable o) {
	}

	public void setSampleRate(float sampleRate, boolean notify) {
	}

	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
	}

	public void update(PamObservable o, PamDataUnit arg) {
	}


}
