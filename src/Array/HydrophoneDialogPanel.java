package Array;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import PamController.PamController;
import PamUtils.LatLong;
import PamUtils.LatLongDialog;

/**
 * Panel for the ArrayDialog to show a selection of hydrophone arrays and
 * listed details of the currently selected array
 * @author dgillespie
 * @see Array.ArrayDialog
 * @see Array.PamArray
 *
 */
public class HydrophoneDialogPanel implements ActionListener, ListSelectionListener {

	JPanel hydrophonePanel;
	
	JButton deleteButton, addButton, editButton;
	
	JTable hydrophoneTable;
	
	JComboBox recentArrays;
	
	JComboBox arrayLocators;
	
	private HydrophoneTableData hydrophoneTableData;

	private StreamerTableData streamerTableData = new StreamerTableData();
	
	private StreamerPanel streamerPanel;
	
	StaticTowedPanel staticTowedPanel;
	
	StaticPositionPanel staticPositionPanel;
	
	String[] hydrophoneColumns = {"Id", "x", "y", "depth", "x Err", "y Err", " depth Err", "Streamer"};
	
	String[] streamerColumns = {"Id", "x", "y", "depth", "x Err", "y Err", "depth Err", "buoy"};
	
	int[] hydrophoneMap;
	
	ArrayDialog arrayDialog;

	HydrophoneDialogPanel (ArrayDialog arrayDialog) {
		
		this.arrayDialog = arrayDialog;
		
		staticTowedPanel = new StaticTowedPanel();
		
		streamerPanel = new StreamerPanel();

		staticPositionPanel = new StaticPositionPanel();
		
		hydrophoneTableData = new HydrophoneTableData();
		
		hydrophonePanel = makePanel();
		
		updateData();
	}
	
	private JPanel makePanel(){
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		//panel.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel c = new JPanel();
		c.setLayout(new BorderLayout());
		panel.setBorder(new TitledBorder("Array Configuration"));
		c.add(BorderLayout.NORTH, new JLabel("Hydrophone Elements"));
		hydrophoneTable = new JTable(hydrophoneTableData);
		hydrophoneTable.setToolTipText("Hydrophone coordinates are relative to Streamer coordinates");
		hydrophoneTable.setBorder(new EmptyBorder(10,10,10,10));
		hydrophoneTable.getSelectionModel().addListSelectionListener(this);
		hydrophoneTable.addMouseListener(new HydrophoneMouse());
//		hydrophoneTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
//		hydrophoneTable.getColumnModel().getColumn(5).setPreferredWidth(150);
		JScrollPane scrollPane = new JScrollPane(hydrophoneTable);
		scrollPane.setPreferredSize(new Dimension(320, 90));
		c.add(BorderLayout.CENTER, scrollPane);
		panel.add(BorderLayout.CENTER, c);
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		topPanel.add(BorderLayout.NORTH, recentArrays = new JComboBox());
		topPanel.add(BorderLayout.CENTER, staticTowedPanel);
		JPanel topSouthPanel = new JPanel(new BorderLayout());
		topSouthPanel.add(BorderLayout.SOUTH, staticPositionPanel);
		topSouthPanel.add(BorderLayout.NORTH, streamerPanel);
		topPanel.add(BorderLayout.SOUTH, topSouthPanel);
		recentArrays.addActionListener(this);
		panel.add(BorderLayout.NORTH, topPanel);
		
		JPanel s = new JPanel();
		s.setLayout(new FlowLayout(FlowLayout.LEFT));
		s.add(addButton = new JButton("Add..."));
		s.add(editButton = new JButton("Edit..."));
		s.add(deleteButton = new JButton("Delete"));
		addButton.addActionListener(this);
		editButton.addActionListener(this);
		deleteButton.addActionListener(this);
		c.add(BorderLayout.SOUTH, s);
		
		return panel;
	}
	
	public JPanel getPanel() {
		return hydrophonePanel;
	}
	
	void enableButtons() {
		// add is always selected. edit and delete are only enabled if a row is selected
		int selRow = hydrophoneTable.getSelectedRow();
		editButton.setEnabled(selRow >= 0);
		deleteButton.setEnabled(selRow >= 0);
		streamerPanel.enableButtons();
	}
	
	public void setParams(PamArray selArray) {
		recentArrays.removeAllItems();
		ArrayList<PamArray> arrays = ArrayManager.getArrayManager().recentArrays;
		for (int i = 0; i < arrays.size(); i++) {
			recentArrays.addItem(arrays.get(i));
		}
		if (selArray != null) {
			recentArrays.setSelectedItem(selArray);
		}
		enableButtons();
		hydrophoneTable.doLayout();
	}
	
	public PamArray getDialogSelectedArray(){
		if (recentArrays == null) return null;
		return (PamArray) recentArrays.getSelectedItem();
	}
	
	public void updateData() {

		PamArray currentArray = getDialogSelectedArray();
		if (currentArray == null) return;
		staticPositionPanel.sayData();
		staticTowedPanel.sayData();
		hydrophoneTableData.fireTableDataChanged();
		streamerTableData.fireTableDataChanged();
		
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == recentArrays) {
			arrayDialog.newArraySelection();
			updateData();
		}	
		else if (e.getSource() == deleteButton) {
			deleteElement();
		}
		else if (e.getSource() == editButton) {
			editElement();
		}
		else if (e.getSource() == addButton) {
			addElement();
		}
		arrayDialog.newArraySelection();
		hydrophoneTableData.fireTableDataChanged();
//		EnableButtons();		// gets called from ArrayDialog anyway
	}
	
	public void deleteElement() {
		PamArray currentArray = getDialogSelectedArray();
		if (currentArray == null) return;
		int selRow = hydrophoneTable.getSelectedRow();
		if (selRow < 0) return;
		if (JOptionPane.showConfirmDialog(arrayDialog, "Are you want to delete hydrophone eleent " + selRow, 
				"Confirm Hydrophone Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
		currentArray.removeHydrophone(currentArray.getHydrophone(selRow));
		updateData();
	}
	
	public void editElement() {
		PamArray currentArray = getDialogSelectedArray();
		if (currentArray == null) return;
		int selRow = hydrophoneTable.getSelectedRow();
		if (selRow < 0) return;
		Hydrophone newHydrophone = HydrophoneElementDialog.showDialog(arrayDialog, 
				currentArray.getHydrophone(selRow), false, currentArray);
		if (newHydrophone != null) {
			currentArray.updateHydrophone(selRow, newHydrophone);
			arrayDialog.newArraySelection();
			updateData();
		}
	}
	
	public void addElement() {
		// count existing hydrophones to get an id for the new one
		PamArray currentArray = getDialogSelectedArray();
		// clone the last hydrophone in the list to pick up the same gain and
		// sensitivity data
		int nH = currentArray.getHydrophoneArray().size();
		Hydrophone newHydrophone;
		if (nH > 0) {
			Hydrophone lastHydrophone = currentArray.getHydrophone(nH-1);
			newHydrophone = lastHydrophone.clone();
			newHydrophone.setID(nH);
		}
		else {
			newHydrophone = new Hydrophone(nH);
		}
		newHydrophone = HydrophoneElementDialog.showDialog(arrayDialog, newHydrophone, true, currentArray);
		if (newHydrophone != null) {
			currentArray.addHydrophone(newHydrophone);
			arrayDialog.newArraySelection();
			updateData();
		}
	}
	
	class StaticTowedPanel extends JPanel implements ActionListener{
		JRadioButton towedButton, staticButton;

		public StaticTowedPanel () {
//			setLayout(new GridLayout(1, 2));
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new EmptyBorder(5, 0, 5, 0));
			add(staticButton = new JRadioButton("Static array"));
			add(towedButton = new JRadioButton("Towed array    "));
			add(new JLabel(" Locator: "));
			add(arrayLocators = new JComboBox());
			ButtonGroup g = new ButtonGroup();
			g.add(towedButton);
			g.add(staticButton);
			towedButton.addActionListener(this);
			staticButton.addActionListener(this);
			arrayLocators.addActionListener(this);
			arrayLocators.removeAllItems();
			for (int i = 0; i < HydrophoneLocators.getInstance().getCount(); i++) {
				arrayLocators.addItem(HydrophoneLocators.getInstance().getName(i));
			}
		}
		
		public void actionPerformed(ActionEvent e) {
			if (staticPositionPanel != null) {
				staticPositionPanel.setVisible(staticButton.isSelected());
				streamerPanel.setVisible(towedButton.isSelected());
			}
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray != null) {
				currentArray.setArrayType(staticButton.isSelected() ? 
						PamArray.ARRAY_TYPE_STATIC : PamArray.ARRAY_TYPE_TOWED);
				currentArray.setArrayLocator(arrayLocators.getSelectedIndex());
			}
			arrayDialog.newArraySelection();
			enableControls();
		}
		
		public void sayData() {
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null) return;
			towedButton.setSelected(currentArray.getArrayType() == PamArray.ARRAY_TYPE_TOWED);
			staticButton.setSelected(currentArray.getArrayType() == PamArray.ARRAY_TYPE_STATIC);
			arrayLocators.setSelectedIndex(currentArray.getArrayLocator());
			enableControls();
		}
		
//		public boolean getData() {
//			PamArray currentArray = getDialogSelectedArray();
//			if (currentArray != null) {
//				currentArray.setArrayType(staticButton.isSelected() ? 
//						PamArray.ARRAY_TYPE_STATIC : PamArray.ARRAY_TYPE_TOWED);
//				currentArray.setArrayLocator(arrayLocators.getSelectedIndex());
//			}
//			return true;
//		}
		
		public void enableControls() {
			PamArray currentArray = getDialogSelectedArray();
			arrayLocators.setEnabled(towedButton.isSelected());
			if (staticButton.isSelected()) {
				arrayLocators.setSelectedIndex(0);
			}
			else {
				arrayLocators.setSelectedIndex(Math.max(0
						, arrayLocators.getSelectedIndex()));
			}
		}
	}
	
	class StreamerPanel extends JPanel implements ListSelectionListener {
		private JButton addButton, editButton, removeButton;
		private JTable streamerTable;
		
		public StreamerPanel() {
			super();
			streamerTable = new JTable(streamerTableData);
			streamerTable.getSelectionModel().addListSelectionListener(this);
			streamerTable.addMouseListener(new StreamerMouse());
			JScrollPane scrollPane = new JScrollPane(streamerTable);
			scrollPane.setPreferredSize(new Dimension(290, 70));
			setLayout(new BorderLayout());
			add(BorderLayout.NORTH, new JLabel("Hydrophone Streamers"));
			add(BorderLayout.CENTER, scrollPane);
			JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT));
			b.add(addButton = new JButton("Add ..."));
			b.add(editButton = new JButton("Edit ..."));
			b.add(removeButton = new JButton("Delete"));
			addButton.addActionListener(new AddButton());
			editButton.addActionListener(new EditButton());
			removeButton.addActionListener(new RemoveButton());
			add(BorderLayout.SOUTH, b);
			enableButtons();
		}
		
		private void dataChanged() {
			enableButtons();
			streamerTableData.fireTableDataChanged();
			arrayDialog.newArraySelection();
		}
		
		private void enableButtons() {
			int row = streamerTable.getSelectedRow();
			editButton.setEnabled(row >= 0);
			PamArray currArray = getDialogSelectedArray();
			int nH = 0;
			if (currArray != null) {
				nH = currArray.getStreamerHydrophoneCount(row);
			}
			removeButton.setEnabled(row >= 0 && streamerTableData.getRowCount() > 1 && nH == 0);
		}

		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			enableButtons();
		}
		private void addStreamer() {
			Streamer streamer = StreamerDialog.showDialog(arrayDialog, new Streamer());
			if (streamer != null) {
				PamArray currentArray = getDialogSelectedArray();
				currentArray.addStreamer(streamer);
				dataChanged();
			}
			
		}
		private void editStreamer() {
			int row = streamerTable.getSelectedRow();
			if (row < 0) {
				return;
			}
			PamArray currentArray = getDialogSelectedArray();
			Streamer streamer = currentArray.getStreamer(row);
			streamer = StreamerDialog.showDialog(arrayDialog, streamer);
			if (streamer != null) {
				currentArray.updateStreamer(row, streamer);
				dataChanged();
			}
		}
		private void removeStreamer() {
			int row = streamerTable.getSelectedRow();
			if (row < 0) {
				return;
			}
			PamArray currentArray = getDialogSelectedArray();
			currentArray.removeStreamer(row);
			dataChanged();
		}
		class AddButton implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addStreamer();
			}
		}
		class EditButton implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				editStreamer();
			}
		}
		class RemoveButton implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				removeStreamer();
			}
		}
		
		class StreamerMouse extends MouseAdapter {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editStreamer();
				}
			}
		}
		
	}
	
	class StreamerTableData extends AbstractTableModel {

		@Override
		public int getColumnCount() {
			if (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER) {
				return streamerColumns.length;
			}
			else {
				return streamerColumns.length-1;
			}
		}

		@Override
		public String getColumnName(int iCol) {	
			return streamerColumns[iCol];
		}

		@Override
		public int getRowCount() {
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null) return 0;
			return currentArray.getNumStreamers();
		}

		@Override
		public Object getValueAt(int iRow, int iCol) {
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null) return null;
			Streamer streamer = currentArray.getStreamer(iRow);
			switch (iCol) {
			case 0:
				return iRow;
			case 1:
				return streamer.getX();
			case 2:
				return streamer.getY();
			case 3:
				return -streamer.getZ();
			case 4:
				return streamer.getDx();
			case 5:
				return streamer.getDy();
			case 6:
				return streamer.getDz();
			case 7:
				return streamer.getBuoyId1();
			}
			return null;
		}
		
	}

	class HydrophoneMouse extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				editElement();
			}
		}
	}
	
	class StaticPositionPanel extends JPanel implements ActionListener {
		JLabel fixedLatitude, fixedLongitude;
		JButton changeLatitude, changeLongitude;
		public StaticPositionPanel () {
			
			setLayout(new BorderLayout());
			add(BorderLayout.NORTH, new JLabel("Hydrophone reference point"));
			
			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new GridLayout(2,1));
			JPanel c = new JPanel();
			c.setLayout(new BorderLayout());
			c.add(BorderLayout.CENTER, fixedLatitude = new JLabel("Lat"));
			c.add(BorderLayout.EAST, changeLatitude = new JButton("change"));
			changeLatitude.addActionListener(this);
			mainPanel.add(c);

			JPanel e = new JPanel();
			e.setLayout(new BorderLayout());
			e.add(BorderLayout.CENTER, fixedLongitude = new JLabel("Long"));
			e.add(BorderLayout.EAST, changeLongitude = new JButton("change"));
			changeLongitude.addActionListener(this);
			mainPanel.add(e);
			
			add(BorderLayout.CENTER, mainPanel);
			sayLatLong();
		}
		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {

			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null) return;
			
			LatLong newLatLong = LatLongDialog.showDialog(null, currentArray.getFixedLatLong(),
					"Hydrophone array reference position");
			
			if (newLatLong != null) {
				currentArray.setFixedLatLong(newLatLong.clone());
				sayLatLong();
			}
			
		}
		public void sayLatLong() {
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null){
				fixedLatitude.setText("");
				fixedLongitude.setText("");
			}
			else {
				LatLong ll = currentArray.getFixedLatLong();
				if (ll != null) {
					fixedLatitude.setText(LatLong.formatLatitude(ll.getLatitude()));
					fixedLongitude.setText(LatLong.formatLongitude(ll.getLongitude()));
				}
			}
		}
		public void sayData() {
			sayLatLong();
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null) return;
			setVisible(currentArray.getArrayType() == PamArray.ARRAY_TYPE_STATIC);
		}
	}

	class HydrophoneTableData extends AbstractTableModel {

		public int getColumnCount() {
			return hydrophoneColumns.length;
		}
		

		public int getRowCount() {
			PamArray currentArray = getDialogSelectedArray();
			if (currentArray == null) return 0;
			return currentArray.getHydrophoneArray().size();
		}

		@Override
		public String getColumnName(int column) {
			return hydrophoneColumns[column];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			PamArray array = getDialogSelectedArray();
			Hydrophone hydrophone = array.getHydrophone(rowIndex);
			
			switch (columnIndex) {
			case 0:
				return rowIndex;
			case 1:
				return hydrophone.getX();
			case 2:
				return hydrophone.getY();
			case 3:
				return hydrophone.getDepth();
			case 4:
				return hydrophone.getdX();
//				double[] bw = hydrophone.getBandwidth();
//				return String.format("%.1f-%.1f kHz", bw[0]/1000., bw[1]/1000.);
			case 5:
				return hydrophone.getdY();
			case 6:
				return hydrophone.getdZ();
			case 7:
				return hydrophone.getStreamerId();
			}
			return null;
		}
		
	}
	
	public int[] getHydrophoneMap() {
		return hydrophoneMap;
	}

	public void setHydrophoneMap(int[] hydrophoneMap) {
		this.hydrophoneMap = hydrophoneMap;
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		enableButtons();		
	}
}
