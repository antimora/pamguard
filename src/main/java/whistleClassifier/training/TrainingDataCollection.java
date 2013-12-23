package whistleClassifier.training;

import java.io.File;
import java.util.ArrayList;

import whistleClassifier.WhistleClassificationParameters;
import whistleClassifier.WhistleClassifierControl;

import PamUtils.FileParts;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import org.apache.commons.io.FilenameUtils;

/**
 * A collection of training data for multiple species.
 * <p>
 * Contains references to a list of several TrainingDataGroups (one per species)
 * <p>
 * each of which contains references to one or more TrainingDataSets (one per file /
 * storage unit)
 *
 * @author Doug Gillespie
 * @see TrainingDataGroup
 * @see TrainingDataSet
 *
 */
public class TrainingDataCollection {

    private ArrayList<TrainingDataGroup> trainingDataGroups;

    private TrainingDataStore currentTrainingStore;

    private TrainingDataStore trainingStore = new FileTrainingStore();

    private WhistleClassifierControl whistleClassifierControl;

    /**
     * Create a new training data collection - a collection of multiple TRaining data
     * groups - one per species.
     *
     * @param whistleClassifierControl
     * @see TrainingDataGroup
     */
    public TrainingDataCollection(WhistleClassifierControl whistleClassifierControl) {
        this.whistleClassifierControl = whistleClassifierControl;
        trainingDataGroups = new ArrayList<TrainingDataGroup>();
    }

    public void clearStore() {
        trainingDataGroups.clear();
        currentTrainingStore = null;
    }

    /**
     * Load all the training data for all species from a given store.
     * <p>
     * This functionality should probably be put into another abstract class so that
     * different types of store can be used. Maniana !
     *
     * @param trainingDataStore
     * @return true if loaded something successfully
     */
    public boolean loadTrainingData(WhistleClassificationParameters classifierParams, boolean subFolders,
            boolean useFolderNames, ProgressInformation progressInformation) {
        clearStore();
        TrainingFileList tfl = new TrainingFileList();
        ArrayList<File> trainingFiles = tfl.getFileList(classifierParams.trainingDataFolder, subFolders);
        if (trainingFiles == null) {
            return false;
        }
        if (progressInformation != null) {
            progressInformation.setText("Loading training data from " + classifierParams.trainingDataFolder);
            progressInformation.setProgressLimits(0, trainingFiles.size() - 1);
            progressInformation.setProgress(0);
        }
        for (int i = 0; i < trainingFiles.size(); i++) {
            addFileData(trainingFiles.get(i), useFolderNames);
            if (progressInformation != null) {
                progressInformation.setProgress(i);
            }
        }

        return true;
    }

    /**
     * Add all the training data from the given file.
     *
     * @param file file containing Whistle contour data.
     * @param useFolderNames use folder names as species names. Otherwise the Species
     * names set in the headers of the training files are used.
     * @return true (always).
     */
    private boolean addFileData(File file, boolean useFolderNames) {
        TrainingDataSet dataSet = trainingStore.readData(file.getAbsolutePath());
        if (dataSet == null) {
            return false;
        }
        dataSet.setStorageSource(file.getName());
        String species = dataSet.getSpecies();
        if (useFolderNames) {
            FileParts fileParts = new FileParts(file);
            species = fileParts.getLastFolderPart();
        }
        int nZeroContours = 0;
        /**
         * Run some checks in our search for zero Hz whistles.
         */
        int ncont = dataSet.getNumContours();
        TrainingContour contour;
        double f[];
        int nZero;
        for (int i = 0; i < ncont; i++) {
            contour = dataSet.getTrainingContour(i);
            f = contour.getFreqsHz();
            nZero = 0;
            for (int j = 0; j < f.length; j++) {
                if (f[j] == 0) {
                    nZero++;
                }
            }
            if (nZero == f.length) {
                nZeroContours++;
//				 String txt = String.format("Zero contour no. %d in training set %s species %s",
//						 i, file, species);
//				 System.out.println(txt);
            }
        }
        if (nZeroContours > 0) {
            String txt = String.format("training set %s species %s contains %d of %d zero contours = %3.2f%%",
                    file, species, nZeroContours, ncont, (double) (nZeroContours * 100) / ncont);
            System.out.println(txt);

        }

        TrainingDataGroup trainingDataGroup = findDataGroup(species);
        if (trainingDataGroup == null) {
            trainingDataGroup = createDataGroup(species);
        }
        trainingDataGroup.addDataSet(dataSet);

        return true;
    }

    private TrainingDataGroup findDataGroup(String species) {
        if (trainingDataGroups == null) {
            return null;
        }
        for (int i = 0; i < trainingDataGroups.size(); i++) {
            if (trainingDataGroups.get(i).species.equalsIgnoreCase(species)) {
                return trainingDataGroups.get(i);
            }
        }
        return null;
    }

    private TrainingDataGroup createDataGroup(String species) {
        if (trainingDataGroups == null) {
            trainingDataGroups = new ArrayList<TrainingDataGroup>();
        }
        TrainingDataGroup newGroup = new TrainingDataGroup(species);
        trainingDataGroups.add(newGroup);
        return newGroup;
    }

    public int getNumTrainingGroups() {
        if (trainingDataGroups == null) {
            return 0;
        }
        return trainingDataGroups.size();
    }

    public TrainingDataGroup getTrainingDataGroup(int iGroup) {
        return trainingDataGroups.get(iGroup);
    }

    public String[] getSpeciesList() {
        String[] list = new String[getNumTrainingGroups()];
        for (int i = 0; i < list.length; i++) {
            list[i] = getTrainingDataGroup(i).species;
        }
        return list;
    }

    public void dumpStoreContent() {
        System.out.println("Training store contains data for " + trainingDataGroups.size() + " species:");
        TrainingDataGroup tdg;
        String species;
        int nContours;
        int nFragments;
        int nFiles;
        for (int i = 0; i < trainingDataGroups.size(); i++) {
            tdg = trainingDataGroups.get(i);
            species = tdg.species;
            nFiles = tdg.getNumDataSets();
            nContours = tdg.getNumContours();
            nFragments = tdg.getNumFragments(whistleClassifierControl.getWhistleFragmenter(), 0, 0, 0);
            System.out.println(String.format("  %s total %d files with %d contours and %d fragments (%d long)",
                    species, nFiles, nContours, nFragments, 8));
        }
    }

    public void exportData(File directory) {

        try {

            System.out.println("Exporting the data to " + directory.getAbsolutePath());

            TrainingDataGroup tdg;

            String species;
            int nContours;
            int nFragments;
            int nFiles;

            for (int i = 0; i < trainingDataGroups.size(); i++) {
                tdg = trainingDataGroups.get(i);

                File speciesDir = new File(directory, tdg.species);
                speciesDir.mkdirs();

                for (TrainingDataSet tds : tdg.trainingDataSets) {

                    String fileName = FilenameUtils.getBaseName(tds.getStorageSource());
                    if (tds.getNumContours() == 0) {

                        System.out.println("Skipping " + fileName);
                        continue;

                    }

                    File dataFile = new File(speciesDir, fileName + ".csv");

                    System.out.println("Exporting to : " + dataFile.getAbsolutePath());

                    dataFile.createNewFile();

                    PrintWriter csv = new PrintWriter(new FileWriter(dataFile));

                    csv.println(String.format("contour_seq,time,frequency"));

                    int countourId = 0;

                    for (TrainingContour tc : tds.getTrainingContours()) {


                        boolean skip = false;

                        // skip bad data
                        for (int y = 0; y < tc.getLength(); y++) {
                            if (Double.isNaN(tc.getTimesInSeconds()[y]) ||
                                    Double.isNaN(tc.getFreqsHz()[y]) ||
                                    tc.getFreqsHz()[y] == 0) {
                                skip = true;
                                break;
                            }

                        }


                        if (!skip) {

                            countourId++;

                            for (int y = 0; y < tc.getLength(); y++) {
                                csv.println(
                                        String.format("%d,%f,%f",
                                                countourId,
                                                tc.getTimesInSeconds()[y],
                                                tc.getFreqsHz()[y]));
                            }
                        }

                    }

                    csv.close();

                }

                //TODO: record the meta data
//            species = tdg.species;
//            nFiles = tdg.getNumDataSets();
//            nContours = tdg.getNumContours();
//            nFragments = tdg.getNumFragments(whistleClassifierControl.getWhistleFragmenter(), 0, 0, 0);
//            System.out.println(String.format("  %s total %d files with %d contours and %d fragments (%d long)",
//                    species, nFiles, nContours, nFragments, 8));
            }

            System.out.println("Completed exporting the data.");

        } catch (FileNotFoundException ex) {
            System.err.println("File not found");
        } catch (IOException ex) {
            System.err.println("Cound not create file");
        }

    }
}
