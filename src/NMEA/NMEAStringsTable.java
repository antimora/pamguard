package NMEA;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;

public class NMEAStringsTable {

	static NMEAStringsTable singleInstance;
	
	NMEADataBlock nmeaDataBlock;
	
	String[] tableColumns = {"Last update", "Id", "Data"};
	
	JFrame frame;
	
	JPanel mainPanel;
	
	JTable nmeaTable;
	
	Timer timer;
	
	NMEATableData nmeaTableData;
	
	NMEAControl nmeaControl;
	
	private NMEAStringsTable(NMEAControl nmeaControl) {
		
		this.nmeaControl = nmeaControl;

		frame = new NMEAFrame(nmeaControl.getUnitName() + " Strings List");
//		frame.setSize(800, 200);
		frame.setLocation(100, 300);
		frame.setAlwaysOnTop(true);
		// frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		frame.addWindowListener(this);

		mainPanel = new JPanel(new GridLayout(1, 0));
		mainPanel.setOpaque(true);

		nmeaTableData = new NMEATableData();
		nmeaTable = new JTable(nmeaTableData);
		nmeaTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		nmeaTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		nmeaTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		nmeaTable.getColumnModel().getColumn(2).setPreferredWidth(600);

		JScrollPane scrollPanel = new JScrollPane(nmeaTable);
		mainPanel.add(scrollPanel);

		mainPanel.setPreferredSize(new Dimension(800, 200));

		frame.setContentPane(mainPanel);

		frame.pack();

		timer = new Timer(500, new TimerListener());
		timer.setInitialDelay(10);
	}
	
	class NMEAFrame extends JFrame {
		// so I can override set visible !
		public NMEAFrame(String title) throws HeadlessException {
			super(title);
		}

		/* (non-Javadoc)
		 * @see java.awt.Component#setVisible(boolean)
		 */
		@Override
		public void setVisible(boolean b) {
			if (b) timer.start();
			else timer.stop();
			super.setVisible(b);
		}
		
	}
	
	static public void show(NMEAControl nmeaControl) {
		
		if (singleInstance == null || singleInstance.nmeaControl != nmeaControl) {
			singleInstance = new NMEAStringsTable(nmeaControl);
		}
		
		singleInstance.nmeaDataBlock = nmeaControl.acquireNmeaData.getOutputDatablock();
		
		singleInstance.frame.setVisible(true);
	}

	class TimerListener implements ActionListener {
		boolean doneLayout;
		public void actionPerformed(ActionEvent ev) {
			// table.
//			nmeaTableData.fireTableRowsUpdated(0, 10);
			nmeaTableData.fireTableDataChanged();
			
			if (doneLayout == false && nmeaTableData.getRowCount() > 0) {
				doneLayout = true;
			}
		}
	}

	class NMEATableData extends AbstractTableModel {

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int column) {
			// TODO Auto-generated method stub
			return tableColumns[column];
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return tableColumns.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			if (nmeaDataBlock == null) return 0;
			return nmeaDataBlock.getUnitsCount();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			NMEADataUnit pamDataUnit = nmeaDataBlock.getDataUnit(rowIndex, PamDataBlock.REFERENCE_CURRENT);
			switch(columnIndex) {
			case 0: // time
				return PamCalendar.formatDateTime(pamDataUnit.getTimeMilliseconds());
			case 1:
				return pamDataUnit.getStringId();
			case 2:
				return pamDataUnit.getCharData();
			}
			return null;
		}
		
	}

}
