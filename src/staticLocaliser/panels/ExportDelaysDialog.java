package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import clickDetector.ClickTemplate;

import PamUtils.PamFileFilter;
import PamView.PamDialog;

import staticLocaliser.ExportTimeDelays;
import staticLocaliser.StaticLocaliserParams;

public class ExportDelaysDialog extends PamDialog  {
	
	static ExportDelaysDialog singleInstance;
	
	static StaticLocaliserParams staticParams;
	
	Window parentFrame;
	
	JComboBox fileList;
	
	String currentFile;
	
	JButton browseButton;
	
	JCheckBox append;
	
	ArrayList<ArrayList<Double>> timeDelays;
	
	File filePath;

	
	public ExportDelaysDialog(Window parentFrame, Point pt ){
		super(parentFrame, "Export Delays", false);
		
		this.parentFrame=parentFrame;
		JPanel mainPanel = new JPanel();
		
		JPanel q = new JPanel();
		q.setBorder(new TitledBorder("Export Time Delays"));
		q.setLayout(new BorderLayout());
		q.add(BorderLayout.NORTH, fileList = new JComboBox());
		fileList.setEditable(true);
		fileList.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXX");
		q.add(BorderLayout.EAST, browseButton = new JButton("Browse"));
		q.add(BorderLayout.WEST, append = new JCheckBox("Append"));

			
		browseButton.addActionListener(new BrowseButtonAction());
		
		mainPanel.add(BorderLayout.CENTER,q);
		
		setDialogComponent(mainPanel);
		if (pt != null) {
			setLocation(pt);
		}

	}
	
	public static StaticLocaliserParams showDialog(Window frame, Point pt, StaticLocaliserParams optionsParams, ArrayList<ArrayList<Double>> timeDelays){
		if (singleInstance == null || singleInstance.getOwner() != frame) {
			singleInstance = new ExportDelaysDialog(frame, pt);
		}
		singleInstance.staticParams = optionsParams.clone();
		//carefull-must not do anything to these time delays- read only;
		singleInstance.timeDelays=(ArrayList<ArrayList<Double>>) timeDelays.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.staticParams;
	}
	
	class BrowseButtonAction implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			
			currentFile=saveFileBrowser();
			fileList.removeAllItems();
			fileList.addItem(currentFile);
			for (int i=0; i<staticParams.tdExportFiles.size(); i++){
				fileList.addItem(staticParams.tdExportFiles.get(i));
			}

		}
	}
	
	
	
	public String saveFileBrowser(){
		
		JFileChooser fileChooser = new JFileChooser();
		PamFileFilter fileFilter = new PamFileFilter(".csv",".csv");		
		fileChooser.setFileFilter(fileFilter);
		
		int state = fileChooser.showSaveDialog(parentFrame);
		if (state == JFileChooser.APPROVE_OPTION) {
			File currFile = fileChooser.getSelectedFile();
			if (currFile.getAbsolutePath().endsWith(".csv")==false){
				return (currFile.getAbsolutePath()+".csv");
			}
			else{
				return currFile.getAbsolutePath();
			}
			
		}
		
	return null;
	}

	@Override
	public boolean getParams() {
		ExportTimeDelays.writeTDResultstoFile(timeDelays,currentFile, append.isSelected());
		staticParams.tdExportFiles.add(0,fileList.getItemAt(fileList.getSelectedIndex()).toString());
		staticParams.appendDelays=append.isSelected();
		return true;
	}
	
	public void setParams(){
		fileList.removeAllItems();
		for (int i=0; i<staticParams.tdExportFiles.size(); i++){
			fileList.addItem(staticParams.tdExportFiles.get(i));
		}
		append.setSelected(staticParams.appendDelays);
	}

	@Override
	public void cancelButtonPressed() {
		
	}

	@Override
	public void restoreDefaultSettings() {
		
	}

}
