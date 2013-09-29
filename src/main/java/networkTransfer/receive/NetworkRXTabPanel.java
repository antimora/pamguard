package networkTransfer.receive;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;

import PamUtils.PamCalendar;
import PamView.PamTabPanel;
import PamguardMVC.PamDataBlock;

public class NetworkRXTabPanel implements PamTabPanel {

	private NetworkReceiver networkReceiver;

	private JPanel mainPanel;

	private TablePanel tablePanel;

	private JTable buoyTable;

	private TableDataModel buoyTableData;

	public NetworkRXTabPanel(NetworkReceiver networkReceiver) {
		this.networkReceiver = networkReceiver;
		buoyTableData = new TableDataModel();
		buoyTable = new JTable(buoyTableData);
		mainPanel = new JPanel(new BorderLayout());
		tablePanel = new TablePanel();
		mainPanel.add(BorderLayout.CENTER, tablePanel);
		setColumnWidths();


		Timer t = new Timer(2000, new TimerAction());
		t.start();
	}

	@Override
	public JMenu createMenu(Frame parentFrame) {
		return null;
	}

	@Override
	public JComponent getPanel() {
		return mainPanel;
	}

	@Override
	public JToolBar getToolBar() {
		return null;
	}
	
	public void notifyModelChanged(int changeType) {
		buoyTableData.fireTableStructureChanged();
		setColumnWidths();
	}

	private void setColumnWidths() {
		for (int i = 0; i < buoyTableData.getColumnCount(); i++) {
			buoyTable.getColumnModel().getColumn(i).setPreferredWidth(buoyTableData.getColWidth(i));
		}		
	}

	private class TablePanel extends JPanel {
		private TablePanel(){
			setLayout(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane(buoyTable);
			this.add(BorderLayout.CENTER, scrollPane);
		}
	}

	private class TimerAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			buoyTableData.fireTableDataChanged();
		}
	}
	private class TableDataModel extends AbstractTableModel {

		BuoyStatusDataBlock rxStats;

		TableDataModel() {
			rxStats = networkReceiver.getBuoyStatusDataBlock();
		}

		String[] colNames = {"Station Id","IP Addr", "Channel", "Status", "First Data", "Last Data", "Position", "GPS", "Tot' Packets"};
		int[] colWidths = {   50,          70,      50,          50,       100,           100,     200,       40,     40};
		int nStandardCols = colNames.length;

		@Override
		public int getColumnCount() {
			int n = colNames.length;
			if (networkReceiver.getRxDataBlocks() != null) {
				n += networkReceiver.getRxDataBlocks().size();
			}
			return n;
		}

		@Override
		public int getRowCount() {
			return rxStats.getUnitsCount();
		}

		public int getColWidth(int iCol) {
			if (iCol < nStandardCols) {
				return colWidths[iCol];
			}
			else return 40;
		}

		@Override
		public Object getValueAt(int row, int col) {
			BuoyStatusDataUnit b = rxStats.getDataUnit(row, PamDataBlock.REFERENCE_ABSOLUTE);
			switch(col) {
			case 0:
				return String.format("%d(%d)", b.getBuoyId1(), b.getBuoyId2());
			case 1:
				return b.getIPAddr();
			case 2:
				return b.getChannel();
			case 3:
				return NetworkReceiver.getPamCommandString(b.getCommandStatus());
			case 4:
				return PamCalendar.formatTime(b.getCreationTime());
			case 5:
				return PamCalendar.formatTime(b.getLastDataTime());
			case 6:
				return b.getPositionString();
			case 7:
				return b.getGpsCount();
			case 8:
				return b.getTotalPackets();
			}
			int iBlock = col-nStandardCols;
			return b.getBlockPacketCount(iBlock);
		}

		@Override
		public String getColumnName(int iCol) {
			if (iCol < nStandardCols) {
				return colNames[iCol];
			}
			if (networkReceiver.getRxDataBlocks() != null) {
				return networkReceiver.getRxDataBlocks().get(iCol-nStandardCols).getDataName();
			}
			return null;
		}

	}
}
