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
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.ProgressMonitorInputStream;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Rocca/Weka interface for classifying a contour
 * <p>
 * @author Michael Oswald
 */
public class RoccaClassifier {

    RoccaControl roccaControl;
    AbstractClassifier roccaClassifierModel = null;
    Instances trainedDataset = null;
    boolean fieldsSet = false;
    public static final String AMBIG = "Ambig";

    /**
     * Create RoccaClassifier object for RoccaProcess
     * <p>
     *
     * @param roccaProcess
     */
    public RoccaClassifier(RoccaProcess roccaProcess) {
        this.roccaControl = roccaProcess.roccaControl;
    }


    /**
     * Classifies the passed contour
     * <p>
     * Checks to make sure a classifier has been loaded, then runs the
     * setAttributes method to match the passed contour to the parameter list
     * expected by the classifier.  Once the attribute vector has been created,
     * the classifier is run and the results are saved to the datablock's
     * classifiedAs field.
     *
     * @param rcdb  {@link RoccaContourDataBlock} to be classified
     */
    public void classifyContour(RoccaContourDataBlock rcdb) {
        String classifiedAs = AMBIG;
        // if the model hasn't been loaded yet, do that now
        if (!roccaControl.roccaProcess.isClassifierLoaded()) {
            roccaControl.roccaProcess.setClassifierLoaded(setUpClassifier());

            // if there was an error loading the model, return "Err"
            if (!roccaControl.roccaProcess.isClassifierLoaded()) {
                System.err.println("Cannot load classifier");
                rcdb.setClassifiedAs("Err");
            }
        }
        
        // set up the attribute vector
        DenseInstance rcdbInst = setAttributes(rcdb);
        if (rcdbInst==null) {
            System.err.println("Error creating Instance from Contour");
            rcdb.setClassifiedAs("Err");
        }

        // call the classifier
        try {
            double speciesNum = roccaClassifierModel.classifyInstance(rcdbInst);
            double[] treeConfArray =
                    roccaClassifierModel.distributionForInstance(rcdbInst);
            double treeConfClassified = treeConfArray[(int) speciesNum];
            rcdbInst.setClassValue(speciesNum);
            
            /* if the tree confidence vote is greater than the threshold value,
             * set the classifiedAs field to the species.  Otherwise, keep it
             * as "Ambig"
             */
            if (treeConfClassified >=
                    ((float) roccaControl.roccaParameters.getClassificationThreshold())
                    /100) {
                classifiedAs = trainedDataset.classAttribute().value((int) speciesNum);
            }

            /* set the species field */
            rcdb.setClassifiedAs(classifiedAs);

            /* set the tree votes field */
            rcdb.setTreeVotes(treeConfArray);


        } catch (Exception ex) {
            System.err.println("Classification failed: " + ex.getMessage());
            rcdb.setClassifiedAs("Err");
        }
    }


    /**
     * Loads the Classifier model from the file specified in RoccaParameters
     *
     * @return      boolean flag indicating success or failure of load
     *
     */
    public boolean setUpClassifier() {
        // load the classification tree
        String fname = roccaControl.roccaParameters.roccaClassifierModelFilename.getAbsolutePath();
        try {
            BufferedInputStream input = new BufferedInputStream(
                    (new ProgressMonitorInputStream(null, "Loading Classifier - Please wait",
                    new FileInputStream(fname))));
          Object[] modelParams = SerializationHelper.readAll(input);

            // separate the classifier model from the training dataset info
            roccaClassifierModel = (AbstractClassifier) modelParams[0];
            trainedDataset = (Instances) modelParams[1];
        } catch (Exception ex) {
            System.err.println("Deserialization failed: " + ex.getMessage());
            return false;
        }

        // update the species list in the side panel
        String[] speciesList = new String[trainedDataset.numClasses()];
        Attribute classAttribute = trainedDataset.classAttribute();

        for (int i=0; i<trainedDataset.numClasses(); i++) {
            speciesList[i] = classAttribute.value(i);
        }

        /* check to make sure a unit exists - if Pamguard has just started,
         * there is no RoccaSightingDataUnit yet.  If that's the case, create
         * a blank one first and then update the species list
         */
        if (roccaControl.roccaSidePanel.rsdb.getUnitsCount()==0) {
            RoccaSightingDataUnit unit = new RoccaSightingDataUnit
                    (PamCalendar.getTimeInMillis(),0,0,0);
            roccaControl.roccaSidePanel.rsdb.addPamData(unit);
            roccaControl.roccaSidePanel.setCurrentUnit(unit);
        }
        roccaControl.roccaSidePanel.setSpecies(speciesList);
        return true;
    }

    /**
     * Sets up the available datablock fields to match the classifier model
     * <p>
     * Compares the fields in the passed RoccaContourDataBlack to the fields
     * required for the classifier.  If any fields are missing, an error is
     * displayed and the method returns false.
     * If all the fields are available, an Instance is created matching the order
     * of the datablock fields to the order of the classifier fields.
     *
     * @param rcdb  {@link RoccaContourDataBlock} to be classified
     * @return      {@link Instance} containing the values of the rcdb parameters that
     *              match the classifier attributes
     */
    public DenseInstance setAttributes(RoccaContourDataBlock rcdb) {
        fieldsSet = false;
        ArrayList<String> keyNames = rcdb.getKeyNames();

        // if the model hasn't been loaded yet, do that now
        if (!roccaControl.roccaProcess.isClassifierLoaded()) {
            roccaControl.roccaProcess.setClassifierLoaded(setUpClassifier());
            // if there was an error loading the model, return null
            if (!roccaControl.roccaProcess.isClassifierLoaded()) {
                return null;
            }
        }

        // loop through the trainedDataset attribute list, making sure the
        // required fields are available in the datablock and loading them into
        // a vector
        DenseInstance rcdbInst = new DenseInstance(trainedDataset.numAttributes());
        int index;

        // loop through the training attributes one at a time
        for (int i=0; i<trainedDataset.numAttributes(); i++) {

            // skip the index position if it's the class attribute
            if (i!=trainedDataset.classIndex()) {

                // find the index in the map of the key that matches the current attribute
                index = keyNames.indexOf(trainedDataset.attribute(i).name().toUpperCase(Locale.ENGLISH));

                // if the index is 0 or higher, the attribute has been found; load
                // it into the instance
                if (index >= 0) {
                    rcdbInst.setValue(i, rcdb.getContour()
                            .get(RoccaContourStats.ParamIndx.values()[index]));
                
                // otherwise, the attirbute wasn't found; return an error
                } else {
                    System.out.println("Error - Classifier attribute " +
                            trainedDataset.attribute(i).name() +
                            " not found in contour datablock");
                    return null;
                }
            }
        }

        // set the dataset for the instance to the trainedDataset
        rcdbInst.setDataset(trainedDataset);

        // set the flag to true and return the Instance object
        fieldsSet = true;
        return rcdbInst;
    }

    public boolean areFieldsSet() {
        return fieldsSet;
    }

    public void setFieldsSet(boolean fieldsSet) {
        this.fieldsSet = fieldsSet;
    }

    public String[] getClassifierSpList() {
        String[] speciesList = new String[trainedDataset.numClasses()];
        for (int i=0; i<trainedDataset.numClasses(); i++ ) {
            speciesList[i] = trainedDataset.classAttribute().value(i);
        }
        return speciesList;
    }
}
