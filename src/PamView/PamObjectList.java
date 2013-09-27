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
package PamView;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;

import PamController.PamController;
import PamController.PamControllerInterface;
import PamModel.PamModelInterface;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;

/**
 * @author Doug Gillespie
 *         <p>
 *         Creates a simple pop-up window which displays lists of Data Objects
 *         and Pam Processes
 * 
 */
public class PamObjectList implements WindowListener {

	JFrame frame;

	// JTextArea textArea;
	JPanel mainPanel;

	JTable table;

	Timer timer;

	String[] columnNames;

	TableData tableData;

	private static PamObjectList pamObjectList;

	public static void ShowObjectList() {
		if (pamObjectList == null) {
			pamObjectList = new PamObjectList(null, null);
		}
		pamObjectList.frame.setVisible(true);
		pamObjectList.timer.start();
	}

	private PamObjectList(PamControllerInterface pamControllerInterface,
			PamModelInterface pamModelInterface) {
		// super(pamControllerInterface, pamModelInterface);
		frame = new JFrame("Object List");
		frame.setSize(300, 200);
		frame.setLocation(200, 300);
		frame.setAlwaysOnTop(true);
		// frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(this);

		mainPanel = new JPanel(new GridLayout(1, 0));
		mainPanel.setOpaque(true);

		columnNames = new String[] { "Pam Process", "Data Block", "Data Units", "First", "Last" };
		tableData = new TableData();
		table = new JTable(tableData);

		JScrollPane scrollPanel = new JScrollPane(table);
		mainPanel.add(scrollPanel);

		frame.setContentPane(mainPanel);

		// now sort out the menu
		// frame.setJMenuBar(PamMenu.CreateBasicMenu(null, new MenuListener()));

		frame.pack();
		// frame.setVisible(true);

		timer = new Timer(500, new TimerListener());
		timer.setInitialDelay(10);
		// timer.start();

	}

	class TableData extends AbstractTableModel {

		// ArrayList<PamProcess> processList;

		public TableData() {
			// processList= pamModelInterface.GetModelProcessList();
			// for (int i = 0; i < processList.size(); i++){
			// //processList.get(i).GetOutputDataBlock().addObserver(this);
			// }
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		public synchronized int getRowCount() {
			return PamController.getInstance().getDataBlocks().size();
		}

		public synchronized int getColumnCount() {
			return columnNames.length;
		}

		public synchronized Object getValueAt(int row, int col) {
			/*
			 * need to find the right process by going through and seeing which
			 * one our block is in for the given row
			 */
			ArrayList<PamDataBlock> blockList = PamController.getInstance()
			.getDataBlocks();
			int blocks = 0;

			PamProcess process = null;
			if (row >= blockList.size())
				return null;

			PamDataBlock block = blockList.get(row);
			process = block.getParentProcess();
			PamDataUnit aUnit;

			// if (process != null)
			String str = new String();
			switch (col) {
			case 0:
				if (process != null)
					return process.toString();
			case 1:
				return block.toString();
			case 2:
				str = String.format("%d", block.getUnitsCount());
				if (block.getLongestObserver() != null) str += "  " + block.getLongestObserver().toString();
				return str;
			case 3:
				aUnit = block.getFirstUnit();
				if (aUnit == null) {
					return("-");
				}
				else {
					return PamCalendar.formatTime(aUnit.getTimeMilliseconds());
				}
			case 4:
				aUnit = block.getLastUnit();
				if (aUnit == null) {
					return("-");
				}
				else {
					return PamCalendar.formatTime(aUnit.getTimeMilliseconds());
				}
			}
			return "";
		}

	}

	class TimerListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			tableData.fireTableDataChanged();
		}
	}

	class MenuListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			// table.
			// tableData.fireTableRowsUpdated(0,tableData.getRowCount()-1);
			JMenuItem menuItem = (JMenuItem) ev.getSource();
			//System.out.println(menuItem.getText());

			if (menuItem.getText().equals(("Start PAM"))) {
				PamController.getInstance().pamStart();
			} else if (menuItem.getText().equals(("Stop PAM"))) {
				PamController.getInstance().pamStop();
			}
		}
	}

	public void PamStarted() {
		timer.start();
	}

	public void PamEnded() {
		// timer.stop();
	}

	/**
	 * Implementation of WindowListener
	 */
	public void windowActivated(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		timer.stop();
	}

	public void windowOpened(WindowEvent e) {
		timer.start();
	}

	public void windowIconified(WindowEvent e) {

	}

	public void windowDeiconified(WindowEvent e) {

	}

	public void windowDeactivated(WindowEvent e) {
		// PamSettingManager.getInstance().SaveSettings();

	}

	public void windowClosed(WindowEvent e) {
		// PamSettingManager.getInstance().SaveSettings();

	}
}
