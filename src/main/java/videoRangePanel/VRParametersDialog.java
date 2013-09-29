package videoRangePanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import angleMeasurement.AngleDataUnit;

import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;
import PamguardMVC.PamDataBlock;

public class VRParametersDialog extends PamDialog {

	private JTabbedPane tabbedPanel;
	
	private static VRParametersDialog singleInstance;
	
	private ImagePanel imagePanel;
	
	private CalibrationPanel calibrationPanel;
	
	private CalcPanel calcPanel;
	
	private HeightPanel heightPanel;
	
	private AnglesPanel anglesPanel;
	
	private ShorePanel shorePanel; 
	
	private VRControl vrControl;
	
	private VRParameters vrParameters;
	
	private Frame parentFrams;
	
	private VRParametersDialog THIS;
	
	private VRParametersDialog(Frame parentFrame, VRControl vrControl) {
		super(parentFrame, "Video Range Settings", false);
		this.vrControl = vrControl;
		this.parentFrams = parentFrame;
		THIS = this;
		tabbedPanel = new JTabbedPane();
		calcPanel = new CalcPanel();
		calibrationPanel = new CalibrationPanel();
		imagePanel = new ImagePanel();
		heightPanel = new HeightPanel();
		anglesPanel = new AnglesPanel();
		shorePanel = new ShorePanel();
		tabbedPanel.add("Calculation", calcPanel);
		tabbedPanel.add("Heights", heightPanel);
		tabbedPanel.add("Calibration", calibrationPanel);
		tabbedPanel.add("Display", imagePanel);
		tabbedPanel.add("Angles", anglesPanel);
		tabbedPanel.add("Shore", shorePanel);
		
		setDialogComponent(tabbedPanel);
	}
	
	public static VRParameters showDialog(Frame frame, VRControl vrControl) {
		if (singleInstance == null || singleInstance.getOwner() != frame) {
			singleInstance = new VRParametersDialog(frame, vrControl);
		}
		singleInstance.vrControl = vrControl;
		singleInstance.vrParameters = vrControl.vrParameters.clone();
		singleInstance.setParams() ;
		singleInstance.setVisible(true);
		
		return singleInstance.vrParameters;
	}

	@Override
	public void cancelButtonPressed() {
		vrParameters = null;
	}
	
	private void setParams() {
		calibrationPanel.setParams();
		imagePanel.setParams();
		calcPanel.setParams();
		heightPanel.setParams();
		anglesPanel.setParams();
		shorePanel.setParams();
	}

	@Override
	public boolean getParams() {
		if (calibrationPanel.getParams() == false) {
			return false;
		}
		if (imagePanel.getParams() == false) {
			return false;
		}
		if (calcPanel.getParams() == false) {
			return false;
		}
		if (heightPanel.getParams() == false) {
			return false;
		}
		if (anglesPanel.getParams() == false) {
			return false;
		}
		if (shorePanel.getParams() == false) {
			return false;
		}
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}
	
	class CalibrationPanel extends JPanel {
		
		JTable list;
		ArrayList<VRCalibrationData> localCalList;
		JButton deleteButton, addButton, editbutton;
		AbstractTableModel tableData;
		public CalibrationPanel() {
			super();
//			setBorder(new TitledBorder("Calibration"));
			setLayout(new BorderLayout());
			list = new JTable(tableData = new CalTableData());
			list.setRowSelectionAllowed(true);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane scrollPane = new JScrollPane(list);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setPreferredSize(new Dimension(1, 130));
			add(BorderLayout.CENTER, scrollPane);
			
			add(BorderLayout.NORTH, new JLabel("Select and manage calibrations"));
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.add(addButton = new JButton("Add"));
			buttonPanel.add(editbutton = new JButton("Edit"));
			buttonPanel.add(deleteButton = new JButton("Delete"));
			addButton.addActionListener(new AddButton());
			editbutton.addActionListener(new EditButton());
			deleteButton.addActionListener(new DeleteButton());
			this.add(BorderLayout.SOUTH, buttonPanel);
			
		}
		
		public void setParams() {
//			list.removeAll();
			localCalList = vrParameters.getCalibrationDatas();
			tableData.fireTableDataChanged();
			if (localCalList != null && vrParameters.getCurrentCalibrationIndex() < tableData.getRowCount()) {
				list.setRowSelectionInterval(vrParameters.getCurrentCalibrationIndex(), 
						vrParameters.getCurrentCalibrationIndex());
			}
		}
		
		public boolean getParams() {
			vrParameters.setCalibrationDatas(localCalList);
			int row = list.getSelectedRow();
			if (localCalList == null) {
				// have to allow this at start so that height data can be entered and
				// the first calibration made.
				return true;
			}
			if (row < 0 || (localCalList != null && row >= localCalList.size())) {
				return false;
			}
			vrParameters.setCurrentCalibrationIndex(row);
			return true;
		}
		
		class AddButton implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				VRCalibrationData newCalibration = VRCalibrationDialog.showDialog(parentFrams, vrControl, new VRCalibrationData());
				if (localCalList == null) {
					localCalList = new ArrayList<VRCalibrationData>();
				}
				if (newCalibration != null) {
					localCalList.add(newCalibration);
					tableData.fireTableDataChanged();
					int lastRow = localCalList.size()-1;
					list.setRowSelectionInterval(lastRow, lastRow);
				}
			}
			
		}
		
		class EditButton implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				int row = list.getSelectedRow();
				if (row >= 0&& row < localCalList.size()) {
					VRCalibrationData vrcd = localCalList.get(row);
					VRCalibrationData newCalibration = VRCalibrationDialog.showDialog(parentFrams, vrControl, vrcd);
					if (newCalibration != null) {
						vrcd.update(newCalibration);
						tableData.fireTableDataChanged();
						list.setRowSelectionInterval(row, row);
					}
				}
			}
			
		}
		
		class DeleteButton implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				int row = list.getSelectedRow();
				if (localCalList == null) return;
				if (row <= 0 || row >= localCalList.size()) {
					return;
				}
				localCalList.remove(row);

				tableData.fireTableDataChanged();
			}
			
		}
		
		class CalTableData extends AbstractTableModel {

			public int getColumnCount() {
				return 2;
			}

			public int getRowCount() {
				if (localCalList == null) {
					return 0;
				}
				return localCalList.size();
			}
			private String[] columnNames = {"Name", "Calibration"};
			@Override
			public String getColumnName(int column) {
				return columnNames[column];
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				if (localCalList == null) {
					return null;
				}
				VRCalibrationData cd = localCalList.get(rowIndex);
				if (cd == null) {
					return "Unknown";
				}
				switch (columnIndex) {
				case 0:
					return cd.name;
				case 1:
					return String.format("%.4f\u00B0/pix", cd.degreesPerUnit);
				}
				return null;
			}
			
		}
		
	}
	
	class CalcPanel extends JPanel  {

		JComboBox methodList;
		RangeDialogPanel rangeDialogPanel = null;
		JPanel panel;
		
		public CalcPanel () {
			
			setLayout(new BorderLayout());

			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			JPanel q = new JPanel();
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.fill = GridBagConstraints.HORIZONTAL;

			q.setLayout(new BoxLayout(q, BoxLayout.Y_AXIS));
			q.setBorder(new TitledBorder("Calculation method"));
			q.add(methodList = new JComboBox());
			methodList.addActionListener(new MethodAction());
//			addComponent(panel, q, c);
			panel.add(q);
			for (int i = 0; i < vrControl.rangeMethods.getNames().size(); i++) {
				methodList.addItem(vrControl.rangeMethods.getNames().get(i));
			}
			
			this.add(BorderLayout.NORTH, panel);
			
		}
		void setParams() {
			methodList.setSelectedIndex(vrParameters.rangeMethod);
			setMethod();
		}
		boolean getParams() {
			vrParameters.rangeMethod = methodList.getSelectedIndex();
			if (rangeDialogPanel != null) {
				if (rangeDialogPanel.getParams() == false) {
					return false;
				}
			}
			return true;
		}
		
		class MethodAction implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				setMethod();
			}
		}
		
		private void setMethod() {
			int index = methodList.getSelectedIndex();
			if (index < 0) return;
			VRRangeMethod selMethod = vrControl.rangeMethods.getMethod(index);
			RangeDialogPanel newRangePanel = selMethod.dialogPanel();
			if (newRangePanel == rangeDialogPanel) {
				return;
			}
			if (rangeDialogPanel != null) {
				panel.remove(rangeDialogPanel.getPanel());
			}
			if (newRangePanel != null) {
				panel.add(newRangePanel.getPanel());
			}
			rangeDialogPanel = newRangePanel;
			if (rangeDialogPanel != null) {
				rangeDialogPanel.setParams();
			}
			pack();
		}
	}

	class ImagePanel extends JPanel {

		JComboBox scaleList;
		
		JCheckBox drawHorizon;
				
		public ImagePanel() {
			super();
//			setBorder(new TitledBorder("Image Scaling"));
			this.setLayout(new BorderLayout());

			JPanel op = new JPanel();
			op.setLayout(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.setBorder(new TitledBorder("Image scaling"));
			p.add(BorderLayout.NORTH, scaleList = new JComboBox());
			
			addComponent(op, p, c);
			
			for (int i = 0; i < VRParameters.scaleNames.length; i++) {
				 scaleList.addItem(VRParameters.scaleNames[i]);
			}
			
			c.gridy++;
			JPanel q = new JPanel();
			q.setLayout(new BoxLayout(q, BoxLayout.Y_AXIS));
			q.setBorder(new TitledBorder("Options"));
			q.add(drawHorizon = new JCheckBox("Draw horizon line during selection"));			
			drawHorizon.setToolTipText("Draws the horizon line while you are moving the mouse to the second horizon point");
			addComponent(op, q, c);
			
			this.add(BorderLayout.NORTH, op);
		}
		
		void setParams() {
			scaleList.setSelectedIndex(vrParameters.imageScaling);
			drawHorizon.setSelected(vrParameters.drawTempHorizon);
		}
		boolean getParams() {
			vrParameters.imageScaling = scaleList.getSelectedIndex();
			vrParameters.drawTempHorizon = drawHorizon.isSelected();
			return true;
		}
	}
	
	class HeightPanel extends JPanel {

		JTable list;
		ArrayList<VRHeightData> localHeightList;
		JButton deleteButton, addButton, editbutton;
		AbstractTableModel tableData;
		
		HeightPanel() {

			super();
//			setBorder(new TitledBorder("Calibration"));
			setLayout(new BorderLayout());
			list = new JTable(tableData = new HeightTableData());
			list.setRowSelectionAllowed(true);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane scrollPane = new JScrollPane(list);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setPreferredSize(new Dimension(1, 130));
			add(BorderLayout.CENTER, scrollPane);
			
			add(BorderLayout.NORTH, new JLabel("Manage camera heights"));
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.add(addButton = new JButton("Add"));
			addButton.addActionListener(new AddButton());
			buttonPanel.add(editbutton = new JButton("Edit"));
			editbutton.addActionListener(new EditButton());
			buttonPanel.add(deleteButton = new JButton("Delete"));
			deleteButton.addActionListener(new DeleteButton());
			this.add(BorderLayout.SOUTH, buttonPanel);
		}
		protected void setParams() {
			localHeightList = vrParameters.getHeightDatas();
			tableData.fireTableDataChanged();
			if (localHeightList != null && vrParameters.getCurrentHeightIndex() < tableData.getRowCount()) {
				list.setRowSelectionInterval(vrParameters.getCurrentHeightIndex(), vrParameters.getCurrentHeightIndex());
			}
		}
		protected boolean getParams() {
			vrParameters.setHeightDatas(localHeightList);
			int row = list.getSelectedRow();
			if (row < 0 || (localHeightList != null && row >= localHeightList.size())) {
				return false;
			}
			vrParameters.setCurrentHeightIndex(row);
			return true;
		}
		class HeightTableData extends AbstractTableModel {

			private String[] columnNames = {"Name", "Height (m)"};
			@Override
			public String getColumnName(int column) {
				return columnNames[column];
			}

			public int getColumnCount() {
				return columnNames.length;
			}

			public int getRowCount() {
				if (localHeightList == null) {
					return 0;
				}
				return localHeightList.size();
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				if (localHeightList == null) {
					return null;
				}
				if (rowIndex < 0 || rowIndex >= localHeightList.size()) {
					return null;
				}
				VRHeightData vrh = localHeightList.get(rowIndex);
				switch (columnIndex) {
				case 0:
					return vrh.name;
				case 1:
					return String.format("%.1f m", vrh.height);
				}
				return null;
			}
			
		}
		class DeleteButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				int row = list.getSelectedRow();
				if (row < 0 || row >= localHeightList.size()) {
					return;
				}
				localHeightList.remove(row);
				tableData.fireTableDataChanged();
			}
		}
		
		class AddButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				VRHeightData newData = HeightDialog.showDialog(null, null);
				if (newData != null) {
					if (localHeightList == null) {
						localHeightList = new ArrayList<VRHeightData>();
					}
					localHeightList.add(newData);
					int row = localHeightList.size()-1;
					tableData.fireTableDataChanged();
					list.setRowSelectionInterval(row, row);
				}
			}
		}
		
		class EditButton implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				int row = list.getSelectedRow();
				if (row < 0 || row >= localHeightList.size()) {
					return;
				}
				VRHeightData heightData = localHeightList.get(row);
				VRHeightData newData = HeightDialog.showDialog(null, heightData);
				if (newData != null) {
					heightData.update(newData);
					tableData.fireTableDataChanged();
				}
				
			}
		}
	}
	class AnglesPanel extends JPanel {

		JCheckBox readAngles;
		SourcePanel angleSource;
		public AnglesPanel() {
			super();
			setBorder(new TitledBorder("Angle Measurement"));
			setLayout(new BorderLayout());
			add(BorderLayout.NORTH, readAngles = new JCheckBox("Read angles"));
			angleSource = new SourcePanel(THIS, AngleDataUnit.class, false, false);
			add(BorderLayout.CENTER, angleSource.getPanel());
			readAngles.addActionListener(new ReadAngles());
		}
		
		void setParams() {
			readAngles.setSelected(vrParameters.measureAngles);
			angleSource.setSource(vrParameters.angleDataBlock);
			enableControls();
		}
		
		boolean getParams() {
			vrParameters.measureAngles = readAngles.isSelected();
			PamDataBlock dataBlock = angleSource.getSource();
			if (dataBlock != null) {
				vrParameters.angleDataBlock = dataBlock.getDataName();
			}
			return true;
		}
		
		void enableControls() {
			readAngles.setEnabled(angleSource.getSourceCount() > 0);
			if (readAngles.isEnabled() == false) {
				readAngles.setSelected(false);
			}
			angleSource.setEnabled(readAngles.isSelected());
		}
		
		class ReadAngles implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				enableControls();
			}
		}
		
	}
	
	class ShorePanel extends JPanel {
		
		JButton browseButton;
		JTextField gebcoFile;
		JCheckBox ignoreClosest, drawShore, drawShorePoints;
		
		public ShorePanel() {
			setBorder(new TitledBorder("Shore"));
			setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();
			c.gridwidth = 2;
			addComponent(this, new JLabel("Ascii file for shoreline data"), c);
			c.gridy++;
			addComponent(this, gebcoFile = new JTextField(20), c);
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
//			addComponent(this, new JLabel("                            "), c);
			c.gridx++;
			c.fill = GridBagConstraints.NONE;
			addComponent(this, browseButton = new JButton("Browse..."), c);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 2;
			addComponent(this, ignoreClosest = new JCheckBox("Ignore closest shore segments"), c);
			c.gridy++;
			addComponent(this, new JLabel("(Check this if working from land station)"), c);
			c.gridy++;
			addComponent(this, drawShore = new JCheckBox("Draw Shore on image"), c);
			c.gridy++;
			addComponent(this, drawShorePoints = new JCheckBox("Highlight shore vector points"), c);
			
			
			gebcoFile.setEnabled(false);
			browseButton.addActionListener(new BrowseGebcoFile());
			drawShore.addActionListener(new DrawShoreListener());
		}
		
		class BrowseGebcoFile implements ActionListener {

			public void actionPerformed(ActionEvent e) {

				File newFile = vrControl.mapFileManager.selectMapFile(getMapFile());
				if (newFile != null) {
					gebcoFile.setText(newFile.getAbsolutePath());
					vrParameters.shoreFile = newFile;
					vrControl.mapFileManager.readFileData(newFile, false);
				}
				enableControls();
			}

		}
		public File getMapFile() {
			if (gebcoFile.getText() == null || gebcoFile.getText().length() == 0) {
				return null;	
			}
			else {
				return new File(gebcoFile.getText());
			}
		}

		void setParams() {
			ignoreClosest.setSelected(vrParameters.ignoreClosest);
			drawShore.setSelected(vrParameters.showShore);
			drawShorePoints.setSelected(vrParameters.showShorePoints);
			if (vrParameters.shoreFile != null) {
				gebcoFile.setText(vrParameters.shoreFile.getAbsolutePath());
			}
			else {
				gebcoFile.setText("");
			}
			enableControls();
		}
		
		boolean getParams() {
			vrParameters.ignoreClosest = ignoreClosest.isSelected();
			vrParameters.showShore = drawShore.isSelected();
			vrParameters.showShorePoints = drawShorePoints.isSelected();
			return true;
		}
		
		void enableControls() {
			boolean en = gebcoFile.getText() != null && gebcoFile.getText().length() > 0;
			ignoreClosest.setEnabled(en);
			drawShore.setEnabled(en);
			if (en == false) {
				ignoreClosest.setSelected(false);
				drawShore.setSelected(false);
			}
			drawShorePoints.setEnabled(drawShore.isSelected());
			if (drawShorePoints.isEnabled() == false) {
				drawShorePoints.setSelected(false);
			}
		}
		class DrawShoreListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				enableControls();
			}
		}
		
	}
}
