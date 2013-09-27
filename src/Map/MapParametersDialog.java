package Map;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;



public class MapParametersDialog extends PamDialog {

	static MapParametersDialog singleInstance;

	static MapParameters mapParameters;

	JTextField trackShowtime, dataKeepTime, dataShowTime;

	boolean showHydrophones;

	FilePanel filePanel;

	JCheckBox hydroCheckBox = (new JCheckBox("Show hydrophones", false));

	JCheckBox keepShip = (new JCheckBox("Keep ship on map"));

	MapFileManager mapFileManager;

	private MapParametersDialog(Frame parentFrame) {

		super(parentFrame, "Map Options", true);

		OptionsPanel op = new OptionsPanel();
		filePanel = new FilePanel(this);
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Map File", filePanel);
		tabbedPane.add("Options", op);

//		JPanel  p = new JPanel(new BorderLayout());
//		p.add(BorderLayout.CENTER, t);
//		p.add(BorderLayout.SOUTH, );
		setDialogComponent(tabbedPane);
		setHelpPoint("mapping.mapHelp.docs.mapMenuItems");
		//		this.enableHelpButton(true);

		//hydroCheckBox.get
	}

	public static MapParameters showDialog(Frame parentFrame, MapParameters oldParameters, MapFileManager mapFile) {
		mapParameters = oldParameters.clone();
		if (singleInstance == null) {
			singleInstance = new MapParametersDialog(parentFrame);
		}
		singleInstance.mapFileManager = mapFile;
		singleInstance.setParams(mapParameters);
		singleInstance.setVisible(true);
		return mapParameters;
	}



	private void setParams(MapParameters mapParameters) {
		trackShowtime.setText(String.format("%d", mapParameters.trackShowTime));
		dataKeepTime.setText(String.format("%d", mapParameters.dataKeepTime));
		dataShowTime.setText(String.format("%d", mapParameters.dataShowTime));

		hydroCheckBox.setSelected(mapParameters.showHydrophones);
		keepShip.setSelected(mapParameters.keepShipOnMap);

		filePanel.setMapFile(mapParameters.mapFile);
	}

	@Override
	public boolean getParams() {
		try {
			mapParameters.trackShowTime = Integer.valueOf(trackShowtime.getText());
			mapParameters.dataKeepTime = Integer.valueOf(dataKeepTime.getText());
			mapParameters.dataShowTime = Integer.valueOf(dataShowTime.getText());
			mapParameters.showHydrophones = hydroCheckBox.isSelected();
			mapParameters.keepShipOnMap = keepShip.isSelected();

		}
		catch (Exception Ex) {
			return false;
		}
		mapParameters.mapFile = filePanel.getMapFile();
		mapParameters.mapContours = filePanel.getContoursList();
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		mapParameters = null;
	}

	@Override
	public void restoreDefaultSettings() {

		setParams(new MapParameters());

	}

	class OptionsPanel extends JPanel {
		public OptionsPanel() {
			setBorder(new TitledBorder("General Options"));
			GridBagLayout layout;
			setLayout(layout = new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			//		t.setLayout(new GridLayout(4,3));
			constraints.anchor = GridBagConstraints.WEST;
			constraints.gridx = 0;
			constraints.gridy = 0;
			addComponent(this, new JLabel("Track Length "), constraints);
			constraints.gridx ++;
			addComponent(this,trackShowtime = new JTextField(7), constraints);
			constraints.gridx ++;
			addComponent(this,new JLabel(" s"), constraints);
			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(this,new JLabel("Data storage time "), constraints);
			constraints.gridx ++;
			addComponent(this,dataKeepTime = new JTextField(7), constraints);
			constraints.gridx ++;
			addComponent(this,new JLabel(" s"), constraints);
			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(this,new JLabel("Data display time "), constraints);
			constraints.gridx ++;
			addComponent(this,dataShowTime = new JTextField(7), constraints);
			constraints.gridx ++;
			addComponent(this,new JLabel(" s"), constraints);
			constraints.gridx = 0;
			constraints.gridy ++;
			constraints.gridwidth = 3;
			addComponent(this,hydroCheckBox, constraints);
			constraints.gridy ++;
			addComponent(this,keepShip, constraints);
		}
	}

	class FilePanel extends JPanel {
		private JTextField mapName;
		JButton browseButton, clearButton, allButton, noneButton;
		GetMapFile getMapFile = new GetMapFile();
		JPanel contourPanel;
		JCheckBox[] contourCheck;
		File mapFile;
		MapParametersDialog mapParametersDialog;

		FilePanel(MapParametersDialog mapParametersDialog) {
			this.mapParametersDialog = mapParametersDialog;
			this.setLayout(new BorderLayout());
			//			this.add(BorderLayout.NORTH, new JLabel("ASCII Map File ..."));
			JPanel top = new JPanel(new BorderLayout());
			top.setBorder(new TitledBorder("Gebco ASCII File "));
			top.add(BorderLayout.CENTER, mapName = new JTextField());

			JPanel p = new JPanel();
			JPanel p2 = new JPanel();
			p.setLayout(new FlowLayout(FlowLayout.RIGHT));
			p.add(clearButton = new JButton("Clear"));
			clearButton.addActionListener(new ClearButton());
			p.add(browseButton = new JButton("Browse"));
			browseButton.addActionListener(new BrowseButton());
			p2.setLayout(new BorderLayout());
			p2.add(BorderLayout.EAST, p);
			top.add(BorderLayout.SOUTH, p2);
			this.add(BorderLayout.NORTH, top);

			JPanel centPanel = new JPanel(new BorderLayout());
			centPanel.setBorder(new TitledBorder("Contours"));
			contourPanel = new JPanel();
			JScrollPane scrollPane = new JScrollPane(contourPanel);
			scrollPane.setPreferredSize(new Dimension(0, 200));
			//			p2.add(BorderLayout.CENTER, contourPanel = new JPanel());
			centPanel.add(BorderLayout.CENTER, scrollPane);
			JPanel centRight = new JPanel(new BorderLayout());
			centRight.setBorder(new EmptyBorder(new Insets(0,5,0,5)));
			JPanel centTopRight = new JPanel(new GridLayout(2,1));
			centTopRight.add(allButton = new JButton("Select All"));
			allButton.addActionListener(new AllButton());
			centTopRight.add(noneButton = new JButton("Select None"));
			noneButton.addActionListener(new NoneButton());
			centRight.add(BorderLayout.NORTH, centTopRight);
			centPanel.add(BorderLayout.EAST, centRight);
			this.add(BorderLayout.CENTER, centPanel);
		}

		public void setMapFile(File file) {
			if (file == null) return;
			mapFile = file;
			mapName.setText(file.getAbsolutePath());
			if (file.exists()) {
				mapFileManager.readFileData(file);
				fillContourPanel();
			}
		}
		public File getMapFile() {
			if (mapName.getText() == null || mapName.getText().length() == 0) {
				return null;	
			}
			else {
				return new File(mapName.getText());
			}
		}

		boolean[] getContoursList() {
			if (contourCheck == null) return null;
			boolean[] cl = new boolean[contourCheck.length];
			for (int i = 0; i < contourCheck.length; i++) {
				cl[i] = contourCheck[i].isSelected();
			}
			return cl;
		}

		private void fillContourPanel() {
			contourPanel.removeAll();
//			contourPanel.add(new JLabel("Contours"));
			contourPanel.setLayout(new BoxLayout(contourPanel, BoxLayout.Y_AXIS));
			Vector<Integer> availableContours = mapFileManager.getAvailableContours();
			if (availableContours == null) return;
			if (mapParameters.mapContours == null || mapParameters.mapContours.length <
					availableContours.size()) {
				mapParameters.mapContours = new boolean[availableContours.size()];
			}
			contourCheck = new JCheckBox[availableContours.size()];
			String name;
			for (int i = 0; i < availableContours.size(); i++) {
				name = String.format("%d m", availableContours.get(i));
				if (availableContours.get(i) == 0) {
					name += " (coast)";
				}
				contourPanel.add(contourCheck[i] = 
					new JCheckBox(name));
				contourCheck[i].setSelected(mapParameters.mapContours[i]);
			}
			mapParametersDialog.pack();
			//			invalidate();
		}
		private void browseMaps() {
			File newFile = mapFileManager.selectMapFile(getMapFile());
			if (newFile != null) {
				mapName.setText(newFile.getAbsolutePath());
				mapFileManager.readFileData(newFile);
				//				Vector<Double> availableContours = mapFile.getAvailableContours();
				fillContourPanel();
			}
		}
		private class BrowseButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				browseMaps();
			}
		}
		private class ClearButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				clearMap();
			}
		}
		private class AllButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				selectAllContours(true);
			}
		}
		private class NoneButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				selectAllContours(false);
			}

		}
		private void selectAllContours(boolean b) {
			if (contourCheck == null) {
				return;
			}
			for (int i = 0; i < contourCheck.length; i++) {
				contourCheck[i].setSelected(b);
			}
		}
		private void clearMap() {
			mapName.setText("");
			mapFileManager.clearFileData();
			fillContourPanel();
		}
		
	}



}
