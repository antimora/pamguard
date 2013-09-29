package WILDInterface;

import GPS.GPSDataBlock;
import GPS.GpsDataUnit;
import NMEA.NMEADataBlock;
import java.awt.Frame;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import NMEA.NMEADataUnit;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import serialComms.SerialPortPanel;

public class WILDParametersDialog extends PamDialog implements ItemListener, ActionListener  {

	private static WILDParametersDialog wildParametersDialog;
	
	private WILDParameters wildParameters;

    private WILDControl wildControl;
	
//	private MainStringPanel mainStringPanel;
	
    private SourcePanel sourcePanelGPS;
    private SerialPortPanel serialPortPanel;

    /** checkbox to select output file */
    private JCheckBox outputButton;

    /** textbox displaying the output filename, including path */
    JTextField outputDirTxt;

    /** Jbutton for selecting output file */
    JButton outputDirectoryButton;


	private WILDParametersDialog(Frame parentFrame) {
		super(parentFrame, "WILD ArcGIS Interface Options", false);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		JTabbedPane tabbedPane = new JTabbedPane();
		
		sourcePanelGPS = new SourcePanel(this, "Select GPS Source", GpsDataUnit.class, false, false);
        JPanel serialPort = new JPanel();
    	serialPortPanel = new SerialPortPanel(null, true, false, false, false, false);
        serialPort.setBorder(new TitledBorder("Select WILD Output Port"));
//		mainStringPanel = new MainStringPanel();
        p1.add(sourcePanelGPS.getPanel());
        serialPort.add(serialPortPanel.getPanel());
		p1.add(serialPort);
//		p1.add(mainStringPanel);
		tabbedPane.addTab("COM Settings", p1);
		p.add(tabbedPane);

        /* output file */
		JPanel s = new JPanel(new GridBagLayout());
		JTabbedPane tabbedPane2 = new JTabbedPane();
		GridBagConstraints c = new PamGridBagContraints();
		s.setBorder(new TitledBorder("Output Parameters"));
        c.insets = new Insets(0, 10, 0, 0);
		c.gridwidth = 1;
		addComponent(s, outputButton = new JCheckBox("Output to File"), c);
        outputButton.setSelected(false);
        outputButton.addItemListener(this);
        c.gridwidth = 2;
		c.gridy++;
        addComponent(s, outputDirTxt = new JTextField(30), c);
        outputDirTxt.setEditable(true);
        outputDirTxt.setEnabled(false);
        c.gridwidth = 1;
        c.gridx++;
		c.gridy++;
        c.insets.left = 0;
        addComponent(s, outputDirectoryButton = new JButton("Select File"),c);
        outputDirectoryButton.addActionListener(this);
        outputDirectoryButton.setEnabled(false);
		tabbedPane2.addTab("Output Settings", s);
		p.add(tabbedPane2);


//		setHelpPoint("mapping.NMEA.docs.ConfiguringGPS");
		setDialogComponent(p);
//		setModal(true);
	}

	public static WILDParameters showDialog(Frame parentFrame,
            WILDParameters wildParameters,
            WILDControl wildControl) {
		if (wildParametersDialog == null ||
                wildParametersDialog.getParent() != parentFrame ) {
			wildParametersDialog = new WILDParametersDialog(parentFrame);
		}
		wildParametersDialog.wildParameters = wildParameters.clone();
        wildParametersDialog.wildControl = wildControl;
		wildParametersDialog.setParams();
		wildParametersDialog.setVisible(true);
		return wildParametersDialog.wildParameters;
	}

    /**
     * Loads the parameters stored in WILDParameters into the GUI
     */
	private void setParams() {
        serialPortPanel.setPort(wildParameters.getSerialPortName());
        serialPortPanel.setBaudRate(wildParameters.getBaud());
        sourcePanelGPS.setSource(wildParameters.getGpsSource());
//		mainStringPanel.setParams();
        outputButton.setSelected(wildParameters.isOutputSaved());

        try {
            outputDirTxt.setText(wildParameters.getOutputFilename().getAbsolutePath());
//            String dirString = idiParams.getOutputFilename().getAbsolutePath();
//            int dirLen = dirString.length();
//            String dirSubString;
//            if (dirLen > 30) {
//                dirSubString = "..." + dirString.substring(dirString.length()-30);
//            } else {
//                dirSubString = dirString;
//            }
//            outputDirTxt.setText(dirSubString);
        } catch (NullPointerException ex) {
            outputDirTxt.setText("C:\\");
        }
	}

    /**
     * Saves the current values in the GUI to the WILDParameters object
     *
     * @return boolean indicating success or failure
     */
	public boolean getParams() {
//		if (!mainStringPanel.getParams()) {
//			return false;
//		}
		wildParameters.setSerialPortName(new String(serialPortPanel.getPort()));
        wildParameters.setBaud(serialPortPanel.getBaudRate());
        wildParameters.setGpsSource((GPSDataBlock) sourcePanelGPS.getSource());

        /* try to save the output filename */
        try {
            wildParameters.setOutputFilename(new File(outputDirTxt.getText()));
            wildParameters.setSaveOutput(outputButton.isSelected());
        }
        catch (Exception e) {
            return showWarning("Error creating save file");
        }
        return true;
	}
	
    /**
     * Selects/Deselects the checkbox to save the output to file
     *
     * @param e
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            outputDirTxt.setEnabled(true);
            outputDirectoryButton.setEnabled(true);
            wildParameters.setSaveOutput(true);
        } else {
            outputDirTxt.setEnabled(false);
            outputDirectoryButton.setEnabled(false);
            wildParameters.setSaveOutput(false);
        }
    }

    /**
     * Starts the open file dialog
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        String currFile = outputDirTxt.getText();
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select output file...");
        fileChooser.setFileHidingEnabled(true);
        fileChooser.setApproveButtonText("Select");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(new csvFileFilter());
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (currFile != null) fileChooser.setSelectedFile(new File(currFile));
		int state = fileChooser.showSaveDialog(outputDirTxt);
		if (state == JFileChooser.APPROVE_OPTION) {
			currFile = fileChooser.getSelectedFile().getAbsolutePath();
            wildParameters.setOutputFilename(fileChooser.getSelectedFile());
		}
        outputDirTxt.setText(wildParameters.getOutputFilename().getAbsolutePath());
    }

    /**
     * filter for csv files
     */
    private class csvFileFilter extends FileFilter {
        //Accept all directories and csv files.
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String extension = getExtension(f);
            if (extension != null) {
                if (extension.equals("csv") ||
                    extension.equals("CSV")) {
                        return true;
                } else {
                    return false;
                }
            }

            return false;
        }

        public String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 &&  i < s.length() - 1) {
                ext = s.substring(i+1).toLowerCase();
            }
            return ext;
        }

        //The description of this filter
        public String getDescription() {
            return "Comma-Separated File (*.csv)";
        }
    }

	public void cancelButtonPressed() {
		wildParameters = null;
	}

	@Override
	public void restoreDefaultSettings() {
        wildParameters = new WILDParameters();
        setParams();
    }

    /** Copied from GPSParametersDialog - commented out, but not deleted in case
     * we add more possible NMEA identifiers in the future and want to be
     * able to display them
     */
//	class MainStringPanel extends JPanel implements ActionListener {
//
//		JRadioButton rmcString;
//		JRadioButton ggaString;
//		JTextField rmcInitials;
//		JTextField ggaInitials;
//		public MainStringPanel() {
//			super();
//			setBorder(new TitledBorder("Main Nav' data string"));
//			setLayout(new GridBagLayout());
//			GridBagConstraints c = new GridBagConstraints();
//			c.gridx = c.gridy = 0;
//			c.fill = GridBagConstraints.HORIZONTAL;
//
//			ButtonGroup buttonGroup = new ButtonGroup();
//
//			addComponent(this, new JLabel("RMC String"), c);
//			c.gridx++;
//			addComponent(this, rmcString = new JRadioButton(""), c);
//			c.gridx++;
//			addComponent(this, rmcInitials = new JTextField(2), c);
//			c.gridx++;
//			addComponent(this, new JLabel(" RMC"), c);
//
//			c.gridx = 0;
//			c.gridy ++;
//			addComponent(this, new JLabel("GGA String"), c);
//			c.gridx++;
//			addComponent(this, ggaString = new JRadioButton(""), c);
//			c.gridx++;
//			addComponent(this, ggaInitials = new JTextField(2), c);
//			c.gridx++;
//			addComponent(this, new JLabel(" GGA"), c);
//
//			rmcString.addActionListener(this);
//			ggaString.addActionListener(this);
//			buttonGroup.add(rmcString);
//			buttonGroup.add(ggaString);
//		}
//
//		public void setParams() {
//			rmcString.setSelected(wildParameters.mainString == WILDParameters.READ_RMC);
//			ggaString.setSelected(wildParameters.mainString == WILDParameters.READ_GGA);
//			rmcInitials.setText(wildParameters.rmcInitials);
//			ggaInitials.setText(wildParameters.ggaInitials);
//			enableControls();
//		}
//
//		public boolean getParams() {
//			if (ggaString.isSelected()) {
//				wildParameters.mainString = WILDParameters.READ_GGA;
//			}
//			else {
//				wildParameters.mainString = WILDParameters.READ_RMC;
//			}
//			wildParameters.rmcInitials = rmcInitials.getText();
//			if (wildParameters.rmcInitials.length() != 2) {
//				return false;
//			}
//			wildParameters.ggaInitials = ggaInitials.getText();
//			if (wildParameters.ggaInitials.length() != 2) {
//				return false;
//			}
//			return true;
//		}
//
//		public void actionPerformed(ActionEvent e) {
//			enableControls();
//		}
//
//		private void enableControls() {
//			rmcInitials.setEnabled(rmcString.isSelected());
//			ggaInitials.setEnabled(ggaString.isSelected());
//		}
//	}
	
}
