package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import staticLocaliser.StaticLocalisationResults;
import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;
import staticLocaliser.StaticLocaliserSQLLogging;

import PamDetection.PamDetection;
import PamUtils.LatLong;
import PamView.PamPanel;
import PamView.PamSymbol;

/**
 * Panel which shows results from the localiser displayed as a table. 
 * @author Jamie Macaulay
 *
 */
public class StaticLocalisationResultsPanel implements StaticDialogComponent {
	
	private StaticLocalise staticLocaliser;
	private StaticLocalisationMainPanel staticLocalisationDialog;
	
	//Dialog Components
	private PamPanel mainPanel;
	
	//Table Components 
	ResultTableDataModel tableDataModel;
	JTable resultTable;

	public StaticLocalisationResultsPanel(StaticLocalise staticLocaliser, StaticLocalisationMainPanel staticLocalisationDialog){
		this.staticLocaliser=staticLocaliser;
		this.staticLocalisationDialog=staticLocalisationDialog;
		
		mainPanel=new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Results"));
		
		JPanel saveControls=new JPanel(new GridLayout(0,1));
		JButton save=new JButton("Save");
		save.addActionListener(new Save());
		saveControls.add(save);
		JButton saveAll=new JButton("Save All");
		saveAll.addActionListener(new SaveALL());
		saveControls.add(saveAll);
		JButton setNull=new JButton("Set null");
		setNull.addActionListener(new Null());
		saveControls.add(setNull);
		
		saveControls.setBorder(new TitledBorder("Save Controls"));
		saveControls.setPreferredSize(new Dimension((int) StaticLocalisationMainPanel.controlPanelDimensions.getWidth(), 100));

	
		tableDataModel = new ResultTableDataModel();
		resultTable = new JTable(tableDataModel);
		resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane sp = new JScrollPane(resultTable);
		sp.setPreferredSize(new Dimension(0, 100));
		
		mainPanel.add(BorderLayout.CENTER, sp);
		mainPanel.add(BorderLayout.WEST, saveControls);
		
		int n = tableDataModel.getColumnCount();
		TableColumn col;
		for (int i = 0; i < n; i++) {
			 col = resultTable.getColumnModel().getColumn(i);
			Integer width = tableDataModel.getColumnWidth(i);
			if (width != null) {
				col.setPreferredWidth(width);
			}
		}

		
	}
	
	/**
	 * Saves only the selected result. Note the selected result changes the bestResult param in the staticLocalise class. THe default value for the bestResult is the result which has the lowest chi value.
	 * @author jamie
	 *
	 */
	class Save implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			staticLocaliser.getStaticLocaliserControl().saveBest();
		}
		
	}
	
	/**
	 * Saves all the results which are displayed in the table.
	 * @author Jamie Macaulay
	 *
	 */
	class SaveALL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			staticLocaliser.getStaticLocaliserControl().saveAll();
		}
		
	}
	
	
	/**
	 * Delete all localisation with the same time stamp as the current localisations
	 * @author spn1
	 *
	 */
	class Null implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
					
		}
		
	}

	@Override
	public JPanel getPanel() {
		// TODO Auto-generated method stub
		return mainPanel;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void update(int flag) {
		if (flag==staticLocaliser.getStaticLocaliserControl().SEL_DETECTION_CHANGED && tableDataModel.getRowCount()!=0){
			tableDataModel.reset();
		}
		else{
			tableDataModel.fireTableDataChanged();
		}
	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticLocalisationDialog;
	}
	
	private class ResultTableDataModel extends AbstractTableModel {
		
		ArrayList<StaticLocalisationResults> results;

		private String[] colNames = {"Sel", "Algorithm", "Symb", "Ambiguity", "Lat Long", "Depth(m)", "x(m)" ,"y(m)" ,"Dist (m)", "Depth Error(m)",
				"x Errror(m)", "y Error(m)", "Dist Error", "Chi2", "p", "nDF", "AIC", "time delay no.", "time delay possibilities","millis"};
		@Override
		public int getColumnCount() {
			return colNames.length;
		}

		public Integer getColumnWidth(int iCol) {
			switch(iCol) {
			case 0:
				return 10;
			case 1:
				return 100;
			case 2:
				return 20;
			case 3:
				return 50;
			case 4:
				return 150;
			case 5:
				return 50;
			case 6:
				return 50;
			case 7:
				return 50;
			case 8:
				return 50;
			case 9:
				return 50;
			case 10:
				return 50;
			case 11:
				return 50;
			case 12:
				return 50;
			case 13:
				return 50;
			case 14:
				return 50;
			case 15:
				return 50;
			case 16:
				return 50;
			case 17:
				return 50;
			case 18:
				return 50;
			case 19:
				return 50;
			}
			return null;
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return Boolean.class;
			}
			else if (columnIndex == 2) {
				return ImageIcon.class;
			}
			return super.getColumnClass(columnIndex);
		}
		@Override
		public String getColumnName(int col) {
			return colNames[col];
		}

		@Override
		public int getRowCount() {
			if (staticLocaliser.getResults()==null) return 1;
			if (staticLocaliser.getResultsforTable()==null) return 1;
			return staticLocaliser.getResultsforTable().size();
		}
		
		public void reset() {  
			if (results==null) return;
			int n=results.size();
			for (int i=0; i<n; i++){
			results.remove(0);
			fireTableRowsDeleted(i,i);  
			}
		}  

		@Override
		public Object getValueAt(int row, int col) {
			
			 results = staticLocaliser.getResultsforTable();
			
			if (results==null || results.size()==0){
				return null;
			}
			
			StaticLocalisationResults aResult = results.get(row);
			
			Double a;
			Integer intVal;
			switch(col) {
			case 0:
				return row == staticLocaliser.getBestTableRow();
			case 1:
				return aResult.getAlgorithm().getName();
			case 2:
				PamSymbol aSymbol = aResult.getAlgorithm().getPlotSymbol(0);
				if (aSymbol == null) {
					return null;
				}
				return aSymbol;
			case 3:
				return aResult.getAmbiguity();
			case 4:
				LatLong ll = aResult.getLatLong();
				if (ll == null) {
					return "No position result";
				}
				return aResult.getLatLong().formatLatitude() + "  " + aResult.getLatLong().formatLongitude();
			case 5:
				Double depth =aResult.getDepth();
				if (depth == null) {
					return null;
				}
				return String.format("%3.1fm", depth);
			case 6:
				Double x = aResult.getX();
				if (x == null) {
					return null;
				}
				return String.format("%3.1fm", x);
			case 7:
				Double y = aResult.getY();
				if (y == null) {
					return null;
				}
				return String.format("%3.1fm", y);
			case 8:
				Double dist = aResult.getRange();
				if (dist == null) {
					return null;
				}
				return String.format("%3.1f",dist);
			case 9:
				Double depthError = aResult.getDepthError();
				if (depthError == null){ 
					return null;
				}
				return String.format("%3.1f", depthError);
			case 10:
				Double xError = aResult.getXError();
				if (xError == null){ 
					return null;
				}
				return String.format("%3.1f", xError);
			case 11:
				Double yError= aResult.getYError();
				if (yError == null){ 
					return null;
				}
				return String.format("%3.1f", yError);
			case 12:
				Double distError= aResult.getRangeError();
				if (distError == null){ 
					return null;
				}
				return String.format("%3.1f", distError);
			case 13:
				Double Chi2 = aResult.getChi2();
				if (Chi2 == null){ 
					return null;
				}
				return String.format("%3.1f", Chi2);
			case 17:
				Integer td = aResult.getTimeDelay();
				if (td == null){ 
					return null;
				}
				return String.format("%d", td);
			case 18:
				Integer tdPos = aResult.getNTimeDelayPossibilities();
				if (tdPos == null){ 
					return null;
				}
				return String.format("%d", tdPos);
			case 19:
				Long runTime = aResult.getRunTimeMillis();
				if (runTime == null){ 
					return null;
				}
				return String.format("%d%n", runTime);

			}
			return null;
			
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return (col == 0);
		}

		@Override
		public void setValueAt(Object aValue, int row, int col) {
			if (col == 0) {
				staticLocaliser.setBestResultIndex(row);
				
				update(0) ;
			}
			else {
				super.setValueAt(aValue, row, col);
			}
		}

	}

}
