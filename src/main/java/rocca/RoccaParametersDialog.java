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

package rocca;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import fftManager.FFTDataUnit;

import PamController.PamController;
import PamView.PamDialog;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import whistlesAndMoans.AbstractWhistleDataUnit;

/**
 * code copied from WorkshopParametersDialog.java
 * @author Michael Oswald
 *
 */
public class RoccaParametersDialog extends PamDialog implements ActionListener {

	/*
	 * Make the dialog a singleton - saves time recreating it 
	 * every time it's used and will also leave the same tab showing
	 * for multi tab dialogs
	 */
	static private RoccaParametersDialog singleInstance;
    private RoccaControl roccaControl;
	RoccaParameters roccaParameters;
	JPanel sourceSubPanel;
	SourcePanel fftSourcePanel;        // list avail FFT data sources
	JPanel fftSourceSubPanel;			// panel holding the SourcePanel fftSourcePanel
	SourcePanel wmSourcePanel;		// list avail whistle & moan sources
	JPanel wmSourceSubPanel;			// panel holding the SourcePanel wmSourcePanel
    JTabbedPane tabbedPane;
    JTextField noiseSensitivity, energyBinSize;
    JLabel outputDirLbl;
    JTextField outputDirTxt;
    JButton outputDirectoryButton;
    JLabel outputContourStatsLbl;
    JLabel outputSightingStatsLbl;
    JTextField outputContourStatsTxt;
    JTextField outputSightingStatsTxt;
    JButton outputContourStatsFileButton;
    File outputDirectory;
    File outputContourStatsFile;
    File outputSightingStatsFile;
    JLabel classifierLbl;
    JTextField classifierTxt;
    JButton classifierButton;
    File classifierFile;
    JLabel classDetLbl;
    JTextField classificationThreshold;
    JTextField sightingThreshold;
    JTextField filenameTemplate;
    JLabel filenameTemplateLbl;
    JLabel templateSymbols;
    JRadioButton fftButton;
    JRadioButton wmButton;
    String fftButtonText = "Use FFT Source";
    String wmButtonText = "Use Whistle & Moan Source";
    


	
	private RoccaParametersDialog(Frame parentFrame) {
		super(parentFrame, "Rocca Parameters", true);

		/*
		 * Use the Java layout manager to constructs nesting panels 
		 * of all the parameters. 
		 */
        tabbedPane = new JTabbedPane();
		JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel classifierPanel = new JPanel(new BorderLayout());
        JPanel outputPanel = new JPanel(new BorderLayout());
        JPanel filenamePanel = new JPanel(new BorderLayout());
        tabbedPane.add("Source Data", mainPanel);
        tabbedPane.add("Contours/Classifier", classifierPanel);
        tabbedPane.add("Output", outputPanel);
        tabbedPane.add("Filename Template", filenamePanel);
		
		/* 
		 * Radio buttons to allow user to select whether the source data comes from an FFT
		 * source or a whistle&moan detector source
		 */
		JPanel sourceSelection = new JPanel(new GridLayout(0, 1, 0, 0));
		sourceSelection.setBorder(new TitledBorder("Select Data source"));
		
		fftButton = new JRadioButton(fftButtonText, true);
        fftButton.setActionCommand(fftButtonText);
        wmButton = new JRadioButton(wmButtonText, false);
        wmButton.setActionCommand(wmButtonText);
        
        //Group the radio buttons
        ButtonGroup sourceGroup = new ButtonGroup();
        sourceGroup.add(fftButton);
        sourceGroup.add(wmButton);
        
        //Register a listener for the radio buttons.         
        fftButton.addActionListener(this);
        wmButton.addActionListener(this);
        
		sourceSelection.add(fftButton);
		sourceSelection.add(wmButton);
		mainPanel.add(BorderLayout.PAGE_START, sourceSelection);
        
		/*
		 * create a source sub panel containing the FFT and whistle&moan detector selections
		 */
		sourceSubPanel = new JPanel(new GridLayout(0,1));
		sourceSubPanel.setBorder(new EmptyBorder(0,10,0,0));
		
		/*
		 * FFT source subpanel
		 */
		fftSourcePanel = new SourcePanel(this, FFTDataUnit.class, true, true);
		fftSourceSubPanel = new JPanel();
		fftSourceSubPanel.setLayout(new BorderLayout());
		fftSourceSubPanel.setBorder(new TitledBorder("FFT Data source"));
		fftSourceSubPanel.add(BorderLayout.CENTER, fftSourcePanel.getPanel());
		sourceSubPanel.add(fftSourceSubPanel);
		
		/*
		 * Whistle & Moan Detector source subpanel
		 */
		wmSourcePanel = new SourcePanel(this, AbstractWhistleDataUnit.class, false, true);
		wmSourceSubPanel = new JPanel();
		wmSourceSubPanel.setLayout(new BorderLayout());
		wmSourceSubPanel.setBorder(new TitledBorder("Whistle & Moan Data source"));
		wmSourceSubPanel.add(BorderLayout.CENTER, wmSourcePanel.getPanel());
		sourceSubPanel.add(wmSourceSubPanel);
		mainPanel.add(BorderLayout.CENTER, sourceSubPanel);
		
        /* create the second tab, for the classifier parameters */
        /* top subpanel - classifier */
		JPanel classifierSubPanel = new JPanel();
		classifierSubPanel.setBorder(new TitledBorder("Classifier"));
        classifierTxt = new JTextField(30);
        classifierTxt.setEditable(false);
		classifierButton = new JButton("Select Classifier");
        classifierButton.addActionListener(this);
        GroupLayout classPanelLayout = new GroupLayout(classifierSubPanel);
        classifierSubPanel.setLayout(classPanelLayout);
        classPanelLayout.setAutoCreateGaps(true);
        classPanelLayout.setAutoCreateContainerGaps(true);
        classPanelLayout.setHorizontalGroup(
            classPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(classifierTxt)
                .addComponent(classifierButton)
        );
        classPanelLayout.setVerticalGroup(
            classPanelLayout.createSequentialGroup()
                .addComponent(classifierTxt)
                .addComponent(classifierButton)
        );
		classifierPanel.add(BorderLayout.PAGE_START, classifierSubPanel);

        /* Classification Threshold subpanel */
		JPanel thresholdSubPanel = new JPanel();
		thresholdSubPanel.setBorder(new TitledBorder("Classification Thresholds"));
        JLabel whistleLbl = new JLabel("Whistle Threshold");
        classificationThreshold = new JTextField(3);
        classificationThreshold.setMaximumSize(new Dimension(40, classificationThreshold.getHeight()));
        JLabel whistleUnits = new JLabel("%");
        JLabel schoolLbl = new JLabel("School Threshold");
        sightingThreshold = new JTextField(3);
        sightingThreshold.setMaximumSize(new Dimension(40, sightingThreshold.getHeight()));
        JLabel schoolUnits = new JLabel("%");
        GroupLayout thresholdLayout = new GroupLayout(thresholdSubPanel);
        thresholdSubPanel.setLayout(thresholdLayout);
        thresholdLayout.setAutoCreateGaps(true);
        thresholdLayout.setAutoCreateContainerGaps(true);
        thresholdLayout.setHorizontalGroup(
            thresholdLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(thresholdLayout.createSequentialGroup()
                    .addComponent(whistleLbl)
                    .addComponent(classificationThreshold)
                    .addComponent(whistleUnits))
                .addGroup(thresholdLayout.createSequentialGroup()
                    .addComponent(schoolLbl)
                    .addComponent(sightingThreshold)
                    .addComponent(schoolUnits))
        );
        thresholdLayout.setVerticalGroup(
            thresholdLayout.createSequentialGroup()
                .addGroup(thresholdLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(whistleLbl)
                    .addComponent(classificationThreshold)
                    .addComponent(whistleUnits))
                .addGroup(thresholdLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(schoolLbl)
                    .addComponent(sightingThreshold)
                    .addComponent(schoolUnits))
        );
        thresholdLayout.linkSize(SwingConstants.HORIZONTAL, whistleLbl, schoolLbl);
		classifierPanel.add(BorderLayout.CENTER, thresholdSubPanel);

		// make another panel for the parameters.
		JPanel detPanel = new JPanel();
		detPanel.setBorder(new TitledBorder("Extraction parameters"));
        JLabel noiseSensLbl = new JLabel("Noise Sensitivity");
        JLabel energyBinLbl = new JLabel("Energy Bin Calc Size");
        noiseSensitivity = new JTextField(4);
        noiseSensitivity.setMaximumSize(new Dimension(40, noiseSensitivity.getHeight()));
        energyBinSize = new JTextField(4);
        energyBinSize.setMaximumSize(new Dimension(40, energyBinSize.getHeight()));
        JLabel noiseSensUnits = new JLabel("%");
        JLabel energyBinUnits = new JLabel(" Hz");
        GroupLayout detPanelLayout = new GroupLayout(detPanel);
        detPanel.setLayout(detPanelLayout);
        detPanelLayout.setAutoCreateGaps(true);
        detPanelLayout.setAutoCreateContainerGaps(true);
        detPanelLayout.setHorizontalGroup(
            detPanelLayout.createSequentialGroup()
                .addGroup(detPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(noiseSensLbl)
                    .addComponent(energyBinLbl))
                .addGroup(detPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(noiseSensitivity)
                    .addComponent(energyBinSize))
                .addGroup(detPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(noiseSensUnits)
                    .addComponent(energyBinUnits))
        );
        detPanelLayout.setVerticalGroup(
            detPanelLayout.createSequentialGroup()
                .addGroup(detPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(noiseSensLbl)
                    .addComponent(noiseSensitivity)
                    .addComponent(noiseSensUnits))
                .addGroup(detPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(energyBinLbl)
                    .addComponent(energyBinSize)
                    .addComponent(energyBinUnits))
        );
		classifierPanel.add(BorderLayout.PAGE_END, detPanel);


        /* classifier details subpanel - not currently implemented */
//		JPanel classDetSubPanel = new JPanel();
//		classDetSubPanel.setBorder(new TitledBorder("Classifier Details"));
//		classDetSubPanel.setLayout(new GridBagLayout());
//		GridBagConstraints classDetConstraints = new GridBagConstraints();
//		classDetConstraints.anchor = GridBagConstraints.LINE_START;
//		classDetConstraints.insets = new Insets(0,3,0,0);
//        classDetConstraints.fill = GridBagConstraints.NONE;
//        classDetConstraints.gridwidth = 2;
//		classDetConstraints.gridx = 0;
//		classDetConstraints.gridy = 0;
//        addComponent(classDetSubPanel, classDetLbl = new JLabel("Model details here..."), classDetConstraints);
//		classifierPanel.add(BorderLayout.PAGE_END, classDetSubPanel);

        /* create the third tab, for the output files */
		JPanel outputSubPanel = new JPanel();
		outputSubPanel.setBorder(new TitledBorder("Output File Details"));
        outputDirLbl = new JLabel("Output Directory");
        outputDirTxt = new JTextField(30);
        outputDirTxt.setEditable(false);
		outputDirectoryButton = new JButton("Select Directory");
        outputDirectoryButton.addActionListener(this);
        outputContourStatsLbl = new JLabel("Contour Stats Save File");
        outputContourStatsTxt = new JTextField(30);
        outputContourStatsTxt.setEditable(true);
        outputSightingStatsLbl = new JLabel("School Stats Save File");
        outputSightingStatsTxt = new JTextField(30);
        outputSightingStatsTxt.setEditable(true);
        GroupLayout outputLayout = new GroupLayout(outputSubPanel);
        outputSubPanel.setLayout(outputLayout);
        outputLayout.setAutoCreateGaps(true);
        outputLayout.setAutoCreateContainerGaps(true);
        outputLayout.setHorizontalGroup(
            outputLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(outputDirLbl)
                .addComponent(outputDirTxt)
                .addComponent(outputDirectoryButton)
                .addComponent(outputContourStatsLbl)
                .addComponent(outputContourStatsTxt)
                .addComponent(outputSightingStatsLbl)
                .addComponent(outputSightingStatsTxt)
        );
        outputLayout.setVerticalGroup(
            outputLayout.createSequentialGroup()
                .addComponent(outputDirLbl)
                .addComponent(outputDirTxt)
                .addComponent(outputDirectoryButton)
                .addGap(20)
                .addComponent(outputContourStatsLbl)
                .addComponent(outputContourStatsTxt)
                .addGap(20)
                .addComponent(outputSightingStatsLbl)
                .addComponent(outputSightingStatsTxt)
        );
		outputPanel.add(BorderLayout.PAGE_START, outputSubPanel);

        // create the fourth tab, for the filename template
		JPanel filenameSubPanel = new JPanel();
		filenameSubPanel.setBorder(new EtchedBorder());
		filenameSubPanel.setLayout(new GridBagLayout());
		GridBagConstraints filenameConstraints = new GridBagConstraints();
		filenameConstraints.anchor = GridBagConstraints.PAGE_START;
		filenameConstraints.insets = new Insets(5,3,0,0);
        filenameConstraints.fill = GridBagConstraints.NONE;
        filenameConstraints.gridwidth = 2;
		filenameConstraints.gridx = 0;
		filenameConstraints.gridy = 0;
        addComponent(filenameSubPanel, filenameTemplateLbl = new JLabel("<html>" +
                "Rocca uses a template to create the file names for whistle<br>" +
                "clips and contour points.  You can enter the template<br>" +
                "in the textfield below, using any of the symbols shown<br>" +
                "at the bottom of the window.  When Rocca creates the file,<br>" +
                "it will substitute the actual values for the symbols.<br><br>" +
                "Filename Template<html>"), filenameConstraints);
        filenameConstraints.insets = new Insets(0,3,0,0);
		filenameConstraints.gridy++;
		addComponent(filenameSubPanel, filenameTemplate = new JTextField(35), filenameConstraints);
        filenameTemplate.setEditable(true);
        filenameConstraints.insets = new Insets(15,3,5,3);
        filenameConstraints.gridwidth = 1;
		filenameConstraints.gridx = 0;
		filenameConstraints.gridy++;
        addComponent(filenameSubPanel, templateSymbols = new JLabel("<html>" +
                "%f = name of source<br>" +
                "%n = detection number<br>" +
                "%X = detection tally<br>" +
                "%t = channel/track num<br>" +
                "%Y = year, 4 digits<br>" +
                "%y = year, 2 digits<br>" +
                "%M = month<br>" +
                "%D = day of month<br>" +
                "%J = day of year (3 digits)<html>"), filenameConstraints);
		filenameConstraints.gridx++;
        addComponent(filenameSubPanel, templateSymbols = new JLabel("<html>" +
                "%H = hour, 24-hour clock<br>" +
                "%h = hour, 12-hour clock<br>" +
                "%a = 'am' or 'pm'<br>" +
                "%m = minute<br>" +
                "%s = second<br>" +
                "%S = second of the day (5 digits)<br>" +
                "%d = tenths of a second<br>" +
                "%c = hundredths of a second<br>" +
                "%i = thousandths of a second<html>"), filenameConstraints);
		filenamePanel.add(BorderLayout.CENTER, filenameSubPanel);

        // set the dialog component focus
		setDialogComponent(tabbedPane);
	}

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == outputDirectoryButton) {
            selectDirectory();
        } else if (e.getSource() == outputContourStatsFileButton) {
            selectContourStatsFile();
        } else if (e.getSource() == classifierButton) {
            selectClassifier();
        } else if (e.getSource() == fftButton) {
        	enableFFT();
        	roccaParameters.setUseFFT(true);
        } else if (e.getSource() == wmButton) {
        	enableWM();
        	roccaParameters.setUseFFT(false);
        }
    }

    protected void selectDirectory() {
        String currDir = outputDirTxt.getText();
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select output directory...");
        fileChooser.setFileHidingEnabled(true);
        fileChooser.setApproveButtonText("Select");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (currDir != null) fileChooser.setSelectedFile(new File(currDir));
		int state = fileChooser.showOpenDialog(outputDirTxt);
		if (state == JFileChooser.APPROVE_OPTION) {
			currDir = fileChooser.getSelectedFile().getAbsolutePath();
            outputDirectory = fileChooser.getSelectedFile();
			//System.out.println(currFile);
		}
        outputDirTxt.setText(currDir);
    }
	
    protected void selectContourStatsFile() {
        String currFile = outputContourStatsTxt.getText();
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select contour stats output file...");
        fileChooser.setFileHidingEnabled(true);
        fileChooser.setApproveButtonText("Select");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (currFile != null) fileChooser.setSelectedFile(new File(currFile));
		int state = fileChooser.showOpenDialog(outputDirTxt);
		if (state == JFileChooser.APPROVE_OPTION) {
			currFile = fileChooser.getSelectedFile().getAbsolutePath();
            outputContourStatsFile = fileChooser.getSelectedFile();
			//System.out.println(currFile);
		}
        outputContourStatsTxt.setText(currFile);
    }

    protected void selectClassifier() {
        String currFile = classifierTxt.getText();
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select classifier model...");
        fileChooser.setFileHidingEnabled(true);
        fileChooser.setApproveButtonText("Select");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (currFile != null) fileChooser.setSelectedFile(new File(currFile));
		int state = fileChooser.showOpenDialog(outputDirTxt);
		if (state == JFileChooser.APPROVE_OPTION) {
			currFile = fileChooser.getSelectedFile().getAbsolutePath();
            classifierFile = fileChooser.getSelectedFile();
		}
        classifierTxt.setText(currFile);
    }
    
    protected void enableFFT() {
    	fftSourceSubPanel.setEnabled(true);
    	fftSourcePanel.setEnabledWithChannels(true);
    	wmSourceSubPanel.setEnabled(false);
    	wmSourcePanel.setEnabled(false);
    }
    
    protected void enableWM() {
    	fftSourceSubPanel.setEnabled(false);
    	fftSourcePanel.setEnabledWithChannels(false);
    	wmSourceSubPanel.setEnabled(true);
    	wmSourcePanel.setEnabled(true);
    }

	public static RoccaParameters showDialog(Frame parentFrame, 
            RoccaParameters roccaParameters,
            RoccaControl roccaControl) {
		if (singleInstance == null || singleInstance.getParent() != parentFrame) {
			singleInstance = new RoccaParametersDialog(parentFrame);
		}
        singleInstance.roccaControl = roccaControl;
        singleInstance.roccaParameters = roccaParameters.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.roccaParameters;
	}

	/**
	 * Enables/disables the FFT and Whistle&Moan sources based on the value found
	 * in the current roccaParameters object 
	 */
	public void enableTheCorrectSource() {
		if (roccaParameters.weAreUsingFFT()) {
			enableFFT();
			fftButton.setSelected(true);
			wmButton.setSelected(false);
		} else {
			enableWM();
			fftButton.setSelected(false);
			wmButton.setSelected(true);
		}
	}

	/**
     * sets up the labels to be shown in the dialog box, based on the current
     * roccaParameters object
     */
	public void setParams() {
		fftSourcePanel.setChannelList(roccaParameters.getChannelMap());
		ArrayList<PamDataBlock> fftSources = PamController.getInstance().getFFTDataBlocks();
		fftSourcePanel.setSource(fftSources.get(roccaParameters.fftDataBlock));
		wmSourcePanel.setSourceList();
		wmSourcePanel.setSource(roccaParameters.getWmDataSource());
		enableTheCorrectSource();
		
        noiseSensitivity.setText(String.format("%.1f",
                roccaParameters.getNoiseSensitivity()));
		energyBinSize.setText(String.format("%d", 
                roccaParameters.getEnergyBinSize()));
        classificationThreshold.setText(String.format("%d",
                roccaParameters.getClassificationThreshold()));
        sightingThreshold.setText(String.format("%d",
                roccaParameters.getSightingThreshold()));
        filenameTemplate.setText(roccaParameters.getFilenameTemplate());
        try {
            outputDirectory = roccaParameters.roccaOutputDirectory;
            String dirString = outputDirectory.getAbsolutePath();
//            int dirLen = dirString.length();
//            String dirSubString;
//            if (dirLen > 30) {
//                dirSubString = "..." + dirString.substring(dirString.length()-30);
//            } else {
//                dirSubString = dirString;
//            }
//            outputDirTxt.setText(dirSubString);
            outputDirTxt.setText(dirString);
        } catch (NullPointerException ex) {
            outputDirectory = new File("C:\\");
            outputDirTxt.setText("C:\\");
        }
        try {
            outputContourStatsFile = roccaParameters.roccaContourStatsOutputFilename;
            outputContourStatsTxt.setText(
                    roccaParameters.roccaContourStatsOutputFilename.getName());
        } catch (NullPointerException ex) {
            outputContourStatsFile = new File("C:\\RoccaContourStats.csv");
            outputContourStatsTxt.setText("RoccaContourStats.csv");
        }
        try {
            outputSightingStatsFile = roccaParameters.roccaSightingStatsOutputFilename;
            outputSightingStatsTxt.setText(
                    roccaParameters.roccaSightingStatsOutputFilename.getName());
        } catch (NullPointerException ex) {
            outputSightingStatsFile = new File("C:\\RoccaSightingStats.csv");
            outputSightingStatsTxt.setText("RoccaSightingStats.csv");
        }
        try {
            classifierFile = roccaParameters.roccaClassifierModelFilename;
            String dirString = classifierFile.getAbsolutePath();
            classifierTxt.setText(dirString);
        } catch (NullPointerException ex) {
            classifierFile = new File("C:\\RF8sp14att.model");
            classifierTxt.setText("RF8sp14att.model");
        }
    }

	@Override
	public void cancelButtonPressed() {
		roccaParameters = null;
	}

	@Override
	/**
	 * takes the values of the labels in the dialog box and sets the
     * roccaParameter object's fields
	 */
	public boolean getParams() {
		/*
		 * get the source parameters
		 */
		if (roccaParameters.weAreUsingFFT()) {
			roccaParameters.fftDataBlock = fftSourcePanel.getSourceIndex();
			roccaParameters.setChannelMap(fftSourcePanel.getChannelList());
			if (roccaParameters.getChannelMap() == 0) {
				return false;
			}
		} else {
			roccaParameters.setWmDataSource(wmSourcePanel.getSourceName());
		}
		
		// will throw an exception if the number format of any of the parameters is invalid, 
		// so catch the exception and return false to prevent exit from the dialog. 
		try {
			roccaParameters.noiseSensitivity = Double.valueOf(noiseSensitivity.getText());
			roccaParameters.energyBinSize = Integer.valueOf(energyBinSize.getText());
            roccaParameters.setClassificationThreshold
                    (Integer.valueOf(classificationThreshold.getText()));
            roccaParameters.setSightingThreshold
                    (Integer.valueOf(sightingThreshold.getText()));

            // set the directory and filename fields
            roccaParameters.roccaOutputDirectory = outputDirectory;
            roccaParameters.roccaContourStatsOutputFilename = new File
                    (outputDirectory,
                    outputContourStatsTxt.getText());
            roccaParameters.roccaSightingStatsOutputFilename = new File
                    (outputDirectory,
                    outputSightingStatsTxt.getText());

            // if a new model has been selected, set the flag to indicate it
            // hasn't been loaded yet
            if (!classifierFile.equals(roccaParameters.roccaClassifierModelFilename)) {
                roccaControl.roccaProcess.setClassifierLoaded(false);
            }
            roccaParameters.roccaClassifierModelFilename = classifierFile;
            roccaParameters.setFilenameTemplate(filenameTemplate.getText());
		}
		catch (NumberFormatException ex) {
			return false;
		}		
		return true;
	}

	@Override
	public void restoreDefaultSettings() {

		roccaParameters = new RoccaParameters();
		setParams();
		
	}

}
