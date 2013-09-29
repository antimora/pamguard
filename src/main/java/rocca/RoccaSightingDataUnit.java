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

import PamDetection.PamDetection;
import java.text.DecimalFormat;
import javax.swing.JLabel;

/**
 * Container holding information about the current sighting, including pointers
 * to the contours already measured
 *
 * @author Michael Oswald
 */
public class RoccaSightingDataUnit extends PamDetection<PamDetection, PamDetection> {

    public static final String NONE = "<None>";

    /* Sighting number */
    String sightNum = NONE;

    /* the list of species names, saved as a JLabel array to make it easier
     to display on the side panel */
    JLabel[] species;

    /* the current whistle count for each species */
    int[] speciesClassCount;

    /* the current tree vote count for each species */
    double[] speciesTreeVotes;

    /* the overall sighting classification */
    String sightClass = NONE;

    /* boolean indicating whether or not the sighting has been saved to the
     * database
     */
    boolean sightingSaved = false;

    /* boolean indicating whether we've classified any whistles in the sighting
     * yet
     */
    boolean whistleClassified = false;

    /**
     * Create a new RoccaSightingDataUnit
     *
     * @param timeMilliseconds
     * @param channelBitmap
     * @param startSample
     * @param duration
     * @param sNum  the current sighting number
     * @param speciesList   a JLabel array containing the species list.
     */
    public RoccaSightingDataUnit(
            long timeMilliseconds,
            int channelBitmap,
            long startSample,
            long duration,
            String sNum,
            JLabel[] speciesList) {
		super(timeMilliseconds, channelBitmap, startSample, duration);

        this.sightNum = sNum;
        this.species = speciesList;
        speciesClassCount = new int[species.length];
        speciesTreeVotes = new double[species.length];
        clearCounts();
	}

    /**
     * Create a new RoccaSightingDataUnit
     *
     * @param timeMilliseconds
     * @param channelBitmap
     * @param startSample
     * @param duration
     * @param sNum  the current sighting number
     * @param speciesList   a String array containing the species list.
     */
    public RoccaSightingDataUnit(
            long timeMilliseconds,
            int channelBitmap,
            long startSample,
            long duration,
            String sNum,
            String[] speciesList) {
		super(timeMilliseconds, channelBitmap, startSample, duration);

        this.sightNum = sNum;
        setSpecies(speciesList);
	}

    /**
     * Create a new RoccaSightingDataUnit.  This constructor is used primarily
     * when the sidePanel is created and the user loads the classifier, but
     * no sighting number exists yet.
     *
     * @param timeMilliseconds
     * @param channelBitmap
     * @param startSample
     * @param duration
     */
    public RoccaSightingDataUnit(
            long timeMilliseconds,
            int channelBitmap,
            long startSample,
            long duration) {
		super(timeMilliseconds, channelBitmap, startSample, duration);

//        this.sightNum = "<Add Sighting>";
        species = new JLabel[1];
        species[0] = new JLabel(RoccaClassifier.AMBIG);

	}

    public String getSightNum() {
        return sightNum;
    }

    public void setSightNum(String sightNum) {
        this.sightNum = sightNum;

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    /**
     * Creates a new list of species and sets the counts to 0.
     * This method will add 'Ambig' to the species list, if it isn't already
     * included.
     *
     * @param speciesAsString   a string array of species names (max 5 characters).
     */
    public void setSpecies(String[] speciesAsString) {
        this.species = null;
        this.speciesClassCount = null;
        this.speciesTreeVotes = null;
        this.whistleClassified = false;
        
        /* add 'Ambig' to the species list, if it isn't already there */
        String[] speciesListWithAmbig = checkForAmbig(speciesAsString);

        /* create the species JLabel array, tally count array and tree vote
         * array.  Set the tally counts and tree votes to 0
         */
        species = new JLabel[speciesListWithAmbig.length];
        speciesClassCount = new int[speciesListWithAmbig.length];
        speciesTreeVotes = new double[speciesListWithAmbig.length];
        for (int i=0; i<speciesListWithAmbig.length; i++) {
            species[i] = new JLabel(speciesListWithAmbig[i]);
            speciesClassCount[i]=0;
            speciesTreeVotes[i] = 0;
        }

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    /**
     * Checks the passed String array for 'Ambig' (RoccaClassifier.AMBIG).  If
     * it's there, the array is returned without any changes.  If it's not there,
     * it is added to the beginning of the array and the new array is returned.
     * <p>
     * Note that this method is static, so calls to it do not need a specific
     * RoccaSightingDataUnit instance.  This was done to make it easier to
     * call from the RoccaSpecPopUp object, which does not see any instances.
     * 
     * @param speciesAsString
     * @return
     */
    public static String[] checkForAmbig(String[] speciesAsString) {
        /* check if the passed array already contains 'Ambig'.  If so, just
         * return the original array
         */
        for (String species : speciesAsString) {
            if (species.equals(RoccaClassifier.AMBIG)) {
                return speciesAsString;
            }
        }

        /* if we got here, it means Ambig is not in the list.  Add it to the
         * beginning, and return the new array
         */
        String[] newSpList = new String[speciesAsString.length+1];
        newSpList[0]=RoccaClassifier.AMBIG;
        System.arraycopy(speciesAsString, 0, newSpList, 1, speciesAsString.length);
        return newSpList;
    }

    /**
     * clear the counts for the species list
     */
    public void clearCounts() {
        for (int i=0; i<species.length; i++) {
            speciesClassCount[i] = 0;
            speciesTreeVotes[i] = 0;
            whistleClassified = false;
        }
    }


    /**
     * returns the species list as a JLabel array
     *
     * @return a JLabel array containing the species list
     */
    public JLabel[] getSpecies() {
        return species;
    }


    /**
     * returns the species list as a String array
     *
     * @return a String array containing the species list
     */
    public String[] getSpeciesAsString() {
        String[] speciesListString = new String[species.length];
        for (int i=0; i<species.length; i++) {
            speciesListString[i] = species[i].getText();
        }
        return speciesListString;
    }


    /**
     * Returns the classification counts for the current species list
     *
     * @return  an int array containing the classification counts
     */
    public int[] getSpeciesClassCount() {
        return speciesClassCount;
    }

    /**
     * Returns the classification count for a single species
     *
     * @param speciesNum the index of the desired species count
     * @return the classification count
     */
    public int getClassCount(int speciesNum) {
        return speciesClassCount[speciesNum];
    }


    /**
     * Sets all the species counts.
     *
     * @param speciesClassCount   an int array with the counts for each species.
     *   The length of the array must match the length of the species list
     */
    public void setSpeciesClassCount(int[] speciesClassCount) {
        if (speciesClassCount.length == this.species.length) {
            this.speciesClassCount = speciesClassCount;
            whistleClassified = true;
        } else {
            System.out.println("RSDU Error - passed species tally is the wrong size");
        }

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    /**
     * Increments a specific species classification count
     *
     * @param speciesToInc the index of the species to increment
     */
    public void incSpeciesCount(int speciesToInc) {
        if (speciesToInc<species.length) {
            speciesClassCount[speciesToInc]++;
            whistleClassified = true;
        }

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    /**
     * Increments a specific species classification count
     *
     * @param speciesToInc the index of the species to increment
     */
    public void incSpeciesCount(String speciesToInc) {
        for (int i=0; i<species.length; i++) {
            if (species[i].getText().equalsIgnoreCase(speciesToInc)) {
                speciesClassCount[i]++;
                whistleClassified = true;
            }
        }

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    public String getSightClass() {
        return sightClass;
    }

    /**
     * Sets the sighting classification to the passed string
     *
     * @param sightClass a string containing the class
     */
    public void setSightClass(String sightClass) {
        this.sightClass = sightClass;

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    /**
     * classify the sighting, based on the tree votes.  Divide the total number
     * of tree votes for each species by the overall whistle count, to get an
     * average for that species.  The sighting threshold is passed, and used
     * to evaluate the vote.  If the vote is less than the threshold, the
     * class is set to Ambiguous.
     *
     * @param the threshold to compare the tree votes against
     */
    public void classifySighting(int threshold) {
        double maxVote = 0.0;
        if (whistleClassified) {
            int whistleCount = countWhistles();
            maxVote = speciesTreeVotes[1]/whistleCount;
            sightClass = species[1].getText();
            for (int i=2; i<speciesTreeVotes.length; i++) {
                if (speciesTreeVotes[i]/whistleCount>maxVote) {
                    maxVote = speciesTreeVotes[i]/whistleCount;
                    sightClass = species[i].getText();
                }
            }
        }
        if (maxVote < (((double) threshold)/100)) {
            sightClass = RoccaClassifier.AMBIG;
        }

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    /**
     * classify the sighting, based on the tree votes.  Divide the total number
     * of tree votes for each species by the overall whistle count, to get an
     * average for that species (note that Ambig is not included in this calc).
     *
     * @return The tree vote of the classified species
     */
    public double classifySighting() {
        double maxVote = 0.0;
        if (whistleClassified) {
            int whistleCount = countWhistles();
            maxVote = speciesTreeVotes[1]/whistleCount;
            sightClass = species[1].getText();
            for (int i=2; i<speciesTreeVotes.length; i++) {
                if (speciesTreeVotes[i]/whistleCount>maxVote) {
                    maxVote = speciesTreeVotes[i]/whistleCount;
                    sightClass = species[i].getText();
                }
            }
        }

        /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }

        return maxVote;
    }

    public int countWhistles() {
        int sum = 0;
        for (int i=0; i<speciesClassCount.length; i++) {
            sum+=speciesClassCount[i];
        }
        return sum;
    }

    /**
     * returns the array of tree votes
     * @return a double array of tree votes
     */
    public double[] getSpeciesTreeVotes() {
        return speciesTreeVotes;
    }

    /**
     * Adds the passed array of tree votes to the current array.  Note that
     * the passed array may not include an 'Ambig' class, so the size will be
     * one smaller than the current array.  If that is the case, make sure to
     * skip the first position (the Ambig position) when adding.
     * 
     * @param newTreeVotes a double array of tree votes
     */
    public void addSpeciesTreeVotes(double[] newTreeVotes) {
        if (newTreeVotes.length == speciesTreeVotes.length-1) {
            for (int i=0; i<newTreeVotes.length; i++) {
                speciesTreeVotes[i+1]+=newTreeVotes[i];
            }
        } else if (newTreeVotes.length == speciesTreeVotes.length) {
            for (int i=0; i<newTreeVotes.length; i++) {
                speciesTreeVotes[i]+=newTreeVotes[i];
            }
        }

    /* If this data unit has already been added to a data block (that is,
         * if it has a parent block), notify the observers that this data unit
         * has been updated (to log it to the database, if necessary).
         * If the data unit has not been added to a data block (if the parent
         * block is null) then don't notify the observers, because they will
         * be notified once it's properly added
         */
        if (this.getParentDataBlock() != null) {
            this.getParentDataBlock().updatePamData(this, timeMilliseconds);
        }
    }

    public boolean isSightingSaved() {
        return sightingSaved;
    }

    public void setSightingSaved(boolean sightingSaved) {
        this.sightingSaved = sightingSaved;
    }

    /**
     * checks to see if any whistles have been classified yet
     * @return boolean indicating whether or not a whistle has been classified
     * yet
     */
    public boolean isWhistleClassified() {
        return whistleClassified;
    }

    /**
     * Create a single string containing the species, using
     * the '-' character separating the values
     */
    public String createSpList() {
        String spList = "(";
        for (int i=0; i<species.length; i++) {
            spList = spList + species[i].getText() + "-";
        }
        spList = spList + ")";
        return spList;
    }

    /**
     * Reads the species from the passed string and sets the species list.  The
     * string starts with '(' and ends with '-)', so drop the first character
     * and last two characters.  Each species name is separated from the
     * other by a '-', so parse on that delimiter.
     *
     * @param spListSingleString the String to parse.  See method description
     * for the String format required
     */
    public void parseAndSetSpList(String spListSingleString) {
        String tempSpecies = spListSingleString.trim();
        String tempSpeciesSub = tempSpecies.substring(1,tempSpecies.length()-2);
        String[] speciesArray = tempSpeciesSub.split("[-]");
        this.setSpecies(speciesArray);
    }

    /**
     * Create a single string containing the species class counts, using
     * the '-' character separating the values
     */
    public String createClassCountList() {
        String classCountList = "(";
        for (int i=0; i<speciesClassCount.length; i++) {
            classCountList = classCountList + speciesClassCount[i] + "-";
        }
        classCountList = classCountList + ")";
        return classCountList;
    }

    /**
     * Reads the class counts from the passed string and sets the tally.  The
     * string starts with '(' and ends with '-)', so drop the first character
     * and last two characters.  Each tally count is separated from the
     * other by a '-', so parse on that delimiter.
     *
     * @param classCountListSingleString the String to parse.  See method
     * description for the String format required
     */
    public void parseAndSetClassCountList(String classCountListSingleString) {
        String tempCount = classCountListSingleString.trim();
        String tempCountSub = tempCount.substring(1,tempCount.length()-2);
        String[] tallyCountAsString = tempCountSub.split("[-]");
        int[] tallyCount = new int[tallyCountAsString.length];
        for (int i=0; i<tallyCountAsString.length; i++) {
            tallyCount[i]=Integer.parseInt(tallyCountAsString[i].trim());
        }
        this.setSpeciesClassCount(tallyCount);
    }

    /**
     * Create a single string containing the tree votes, using
     * the '-' character separating the values.  Note that the votes are rounded
     * to 5 decimal places to stop the string from getting too long.
     */
    public String createVoteList() {
        String voteList = "(";
        DecimalFormat df = new DecimalFormat("#.#####");
        double roundedVal = 0.0;
        for (int i=0; i<speciesTreeVotes.length; i++) {
            roundedVal = Double.valueOf(df.format(speciesTreeVotes[i]));
            voteList = voteList + roundedVal + "-";
        }
        voteList = voteList + ")";
        return voteList;
    }

    /**
     * Reads the class counts from the passed string and sets the tally.  The
     * string starts with '(' and ends with '-)', so drop the first character
     * and last two characters.  Each tally count is separated from the
     * other by a '-', so parse on that delimiter.
     *
     * @param classCountListSingleString the String to parse.  See method
     * description for the String format required
     */
    public void parseAndSetVoteList(String voteListSingleString) {
        String tempVotes = voteListSingleString.trim();
        String tempVotesSub = tempVotes.substring(1,tempVotes.length()-2);
        String[] votesAsString = tempVotesSub.split("[-]");
        double[] votes = new double[votesAsString.length];
        for (int i=0; i<votesAsString.length; i++) {
            votes[i]=Double.parseDouble(votesAsString[i].trim());
        }
        this.addSpeciesTreeVotes(votes);
    }
}
