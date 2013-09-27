package Array;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionParameters;
import PamView.PamDialog;
import PamView.PamHelp;


/**
 * Main dialog for hydrophone array configuration
 * 
 * @author Doug Gillespie
 *
 */
public class ArrayDialog extends PamDialog implements ActionListener {

	static private ArrayDialog singleInstance = null;

	private HydrophoneDialogPanel hydrophoneDialogPanel;

	private ChannelPanel channelPanel;

	private EnvironmentPanel environmentPanel;

	private HydrophoneDiagram hydrophoneDiagram;

	private JButton okButton, cancelButton;

	private JButton newArrayButton, copyArrayButton, importArrayButton, exportArrayButton;
	private JButton deleteArrayButton, renameArrayButton, helpButton;

	private ArrayDialog(Frame parentFrame) {
		super(parentFrame, "Pamguard hydrophone array", false);

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p.setLayout(new BorderLayout());

		JPanel eastPanel = new JPanel();
		eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));

		hydrophoneDialogPanel = new HydrophoneDialogPanel(this);
		eastPanel.add(hydrophoneDialogPanel.getPanel());

		channelPanel = new ChannelPanel(this);
		eastPanel.add(channelPanel.getChannelPanel());

		environmentPanel = new EnvironmentPanel(this);
		//		eastPanel.add(environmentPanel.getEnvironmentPanel());

		p.add(BorderLayout.EAST, eastPanel);

		hydrophoneDiagram = new HydrophoneDiagram(this);
		JPanel westPanel = new JPanel(new BorderLayout());
		westPanel.add(BorderLayout.CENTER, hydrophoneDiagram.getPlotPanel());
		westPanel.add(BorderLayout.SOUTH, environmentPanel.getEnvironmentPanel());
		p.add(BorderLayout.CENTER, westPanel);

		JPanel s = new JPanel();
		s.setBorder(new TitledBorder("Array Management"));
		//s.setLayout(new BoxLayout(s, BoxLayout.X_AXIS));
		s.add(okButton = new JButton("  Ok  "));
		getRootPane().setDefaultButton(okButton);
		s.add(cancelButton = new JButton("Cancel"));

		s.add(newArrayButton = new JButton("New Array..."));
		s.add(copyArrayButton = new JButton("Copy..."));
		s.add(renameArrayButton = new JButton("Rename..."));
		s.add(importArrayButton = new JButton("Import..."));
		s.add(exportArrayButton = new JButton("Export..."));
		s.add(deleteArrayButton = new JButton("Delete"));
		s.add(helpButton = new JButton("Help..."));

		getContentPane().add(BorderLayout.SOUTH, s);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		newArrayButton.addActionListener(this);
		copyArrayButton.addActionListener(this);
		renameArrayButton.addActionListener(this);
		importArrayButton.addActionListener(this);
		exportArrayButton.addActionListener(this);
		deleteArrayButton.addActionListener(this);
		helpButton.addActionListener(this);

		p.add(BorderLayout.SOUTH, s);
		setDialogComponent(p);

		pack();
		//		setLocation(300, 200);
		//		setModal(true);
		//		setModalityType(ModalityType.APPLICATION_MODAL);
		//this.setModal(true);
		//this.setResizable(false);
		//setSize(new Dimension(500, 50));
		//		Dimension D = getSize();
		//		//D.width += D.height * 4/5;
		//		setSize(D);
		//		this.setAlwaysOnTop(true);
		//		setHelpPoint("docs.Array_Configuration");
		setResizable(true);
		setHelpPoint("utilities.hydrophoneArrayManagerHelp.docs.Array_Configuration");
	}

	/**
	 * Open main dialog for hydrophone array configuration
	 * @param parentFrame parent frame or window. 
	 */
	public static void showDialog (Frame parentFrame) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
			singleInstance = new ArrayDialog(parentFrame);
		}
		singleInstance.setParams(null);
		singleInstance.setVisible(true);
	}

	void setParams(PamArray selArray) {
		hydrophoneDialogPanel.setParams(selArray);
		channelPanel.setParams();
		hydrophoneDiagram.rePaint();
		if (selArray != null) {
			environmentPanel.setNewSpeed(selArray.getSpeedOfSound());
		}
		enableButtons();
	}

	@Override
	public boolean getParams() {
		// all OK, so
		// move the selected array to the top of the list in the ArrayManager
		if (okOK(true) == false) return false;

		PamArray array = hydrophoneDialogPanel.getDialogSelectedArray();
		array.setupFixedPointReferenceBlock();
		ArrayManager.getArrayManager().setCurrentArray(array);
		array.setSpeedOfSound(environmentPanel.getNewSpeed());
		array.setSpeedOfSoundError(environmentPanel.getNewError());

		if (checkDaqChannels(array) == false) {
			return false;
		}

		//		new MLGridBearingLocaliser(PamUtils.makeChannelMap(array.getHydrophoneCount()), 1./96000.);
		return true;
	}

	/**
	 * Check that all software channels are assigned before 
	 * allowing to close the dialog. 
	 * @param array
	 * @return true of all channels are assigned a hydrophone
	 */
	private boolean checkDaqChannels(PamArray array) {
		ArrayList<AcquisitionControl> daqControllers = AcquisitionControl.getControllers();
		boolean allOk = true;
		for (int i = 0; i < daqControllers.size(); i++) {
			if (checkDaqChannels(daqControllers.get(i), array) == false) {
				allOk = false;
			}
		}
		return allOk;
	}

	private boolean checkDaqChannels(AcquisitionControl acquisitionControl,
			PamArray array) {
		int nPhones = array.getHydrophoneCount();
		AcquisitionParameters daqParams = acquisitionControl.acquisitionParameters;
		int nChan = daqParams.nChannels;
		int[] phoneList = daqParams.getHydrophoneList();
		for (int i = 0; i < nChan; i++) {
			if (phoneList[i] >= nPhones) {
				return showWarning(String.format("Channel %d of the data acquisition system \"%s\" " +
						"is not assigned to a hydrophone. \n\nAll software channels must be assigned to " +
						"a hydrophone.\n ", i,
						acquisitionControl.getUnitName()));
			}
		}
		return true;
	}

	private boolean okOK(boolean verbose) {
		// only test to do here is to check that the array has > 0 elements in it
		PamArray array = hydrophoneDialogPanel.getDialogSelectedArray();
		if (array == null) {
			if (verbose) {
				JOptionPane.showMessageDialog(this, "You must select a hydrophone array");
			}
			return false;
		}
		else if (array.getHydrophoneArray() == null || array.getHydrophoneArray().size() == 0) {
			if (verbose) {
				JOptionPane.showMessageDialog(this, "You must select an array that has at least one hydrophone", 
						"Array Configuration",	JOptionPane.WARNING_MESSAGE);
			}
			return false;
		}
		else if (environmentPanel.getParams() == false) {
			return false;
		}


		return true;
	}

	void newArraySelection() {
		if (hydrophoneDiagram != null) hydrophoneDiagram.rePaint();
		enableButtons();
		if (hydrophoneDialogPanel == null) return;
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		if (currentArray != null) currentArray.checkHydrophoneIds();
		if (channelPanel != null) channelPanel.newArraySelection();
		if (currentArray != null) {
			environmentPanel.setNewSpeed(currentArray.getSpeedOfSound());
			environmentPanel.setNewError(currentArray.getSpeedOfSoundError());
		}
	}

	void newChannelSelection() {
		if (hydrophoneDiagram != null) hydrophoneDiagram.rePaint();
	}

	void enableButtons() {
		//PamArray currentArray = ArrayManager.getArrayManager().getCurrentArray();
		if (hydrophoneDialogPanel == null) return;
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		//JButton newArrayButton, importArrayButton, exportArrayButton, deleteArrayButton;
		okButton.setEnabled(okOK(false));
		exportArrayButton.setEnabled(currentArray != null);
		deleteArrayButton.setEnabled(ArrayManager.getArrayManager().getArrayCount() > 1);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancelButton) {
			this.setVisible(false);
		}
		else if (e.getSource() == okButton) {
			if (getParams()) {
				this.setVisible(false);
			}
		}
		else if (e.getSource() == newArrayButton) {
			newArray();
		}
		else if (e.getSource() == copyArrayButton) {
			copyArray();
		}
		else if (e.getSource() == renameArrayButton) {
			renameArray();
		}
		else if (e.getSource() == importArrayButton) {
			importArray();
		}
		else if (e.getSource() == exportArrayButton) {
			exportArray();
		}
		else if (e.getSource() == deleteArrayButton) {
			deleteArray();
		}
		else if (e.getSource() == helpButton) {

			PamHelp.getInstance().displayContextSensitiveHelp(getHelpPoint());
		}
	}

	private void newArray() {
		//		Just create a really basic array then let the user edit it
		// pop up a simle dialog to get the array name.
		String newName = JOptionPane.showInputDialog(this, "Enter a name for the new hydrophone array", 
				"New Array", JOptionPane.OK_CANCEL_OPTION);
		if (newName != null && newName.length() > 0) {
			PamArray newArray = new PamArray(newName, PamArray.ARRAY_TYPE_TOWED);
			addArray(newArray);
		}

	}
	private void copyArray() {
		//		Just create a really basic array then let the user edit it
		// pop up a simle dialog to get the array name.
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		if (currentArray == null) return;
		String newName = JOptionPane.showInputDialog(this, "Enter a name for the array copy", 
				"Copy Array", JOptionPane.OK_CANCEL_OPTION);
		if (newName != null && newName.length() > 0) {
			PamArray newArray = currentArray.clone();
			newArray.setArrayName(newName);
			newArray.setArrayFile(null);
			addArray(newArray);
		}		
	}

	private void renameArray() {
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		if (currentArray == null) return;
		String newName = (String) JOptionPane.showInputDialog(this, "Enter a new name for the  array", 
				"Rename Array",  JOptionPane.OK_CANCEL_OPTION, null, null, currentArray.getArrayName());
		if (newName != null) {
			currentArray.setArrayName(newName);
			hydrophoneDialogPanel.setParams(currentArray);
		}
	}

	private void importArray() {
		//		PamArray currArray = ArrayManager.getArrayManager().getCurrentArray();
		//		if (currentArray == null) return;
		JFileChooser fileChooser = getArrayFileChooser();
		int state = fileChooser.showOpenDialog(this);
		if (state == JFileChooser.APPROVE_OPTION) {
			PamArray newArray = ArrayManager.loadArrayFromFile(fileChooser.getSelectedFile().getAbsolutePath());
			newArray.setArrayFile(fileChooser.getSelectedFile().getAbsolutePath());
			addArray(newArray);
		}
	}

	public void addArray(PamArray newArray) {
		ArrayManager.getArrayManager().addArray(newArray);
		setParams(newArray);
	}

	private void exportArray() {
		//		PamArray currArray = ArrayManager.getArrayManager().getCurrentArray();
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		if (currentArray == null) return;
		JFileChooser fileChooser = getArrayFileChooser();
		int state = fileChooser.showSaveDialog(this);
		if (state == JFileChooser.APPROVE_OPTION) {
			currentArray.setArrayFile(fileChooser.getSelectedFile().getAbsolutePath());
			ArrayManager.saveArrayToFile(currentArray);
		}
	}

	private void deleteArray() {
		//		PamArray currArray = ArrayManager.getArrayManager().getCurrentArray();
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		if (currentArray == null) return;
		if (ArrayManager.getArrayManager().getRecentArrays().size() < 2) {
			JOptionPane.showMessageDialog(this, "You cannot delete the last array in the list", "Array delete",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		int ans = JOptionPane.showConfirmDialog(this, 
				"Are you sure you want to delete the array \"" + currentArray.toString() + "\"",
				"Confirm array delete", JOptionPane.YES_NO_OPTION);
		if (ans == JOptionPane.NO_OPTION) return;
		// go ahead and delete it !
		ArrayManager.getArrayManager().removeArray(currentArray);
		setParams(null);
	}


	public JFileChooser getArrayFileChooser() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Pamguard Array File...");
		fileChooser.setFileHidingEnabled(true);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		javax.swing.filechooser.FileFilter[] filters = fileChooser
		.getChoosableFileFilters();
		for (int i = 0; i < filters.length; i++) {
			fileChooser.removeChoosableFileFilter(filters[i]);
		}
		fileChooser.addChoosableFileFilter(new ArrayFileFilter());
		PamArray currentArray = hydrophoneDialogPanel.getDialogSelectedArray();
		//		PamArray currArray = ArrayManager.getArrayManager().getCurrentArray();
		fileChooser.setSelectedFile(new File(currentArray.getArrayFileName()));

		return fileChooser;
	}

	class ArrayFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {

			if (f.isDirectory()) {
				return true;
			}

			String extension = getExtension(f);
			if (extension != null) {
				if (extension.equals(ArrayManager.getArrayFileType())) {
					return true;
				} 
				else {
					return false;
				}
			}

			return false;
		}
		private String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if (i > 0 && i < s.length() - 1) {
				ext = s.substring(i + 1).toLowerCase();
			}
			return ext;
		}

		// The description of this filter
		@Override
		public String getDescription() {
			return "Array Files (" + ArrayManager.getArrayFileType() + ")";
		}


	}

	@Override
	public void cancelButtonPressed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

	public HydrophoneDialogPanel getHydrophoneDialogPanel() {
		return hydrophoneDialogPanel;
	}

	public HydrophoneDiagram getHydrophoneDiagram() {
		return hydrophoneDiagram;
	}

	public ChannelPanel getChannelPanel() {
		return channelPanel;
	}

}
