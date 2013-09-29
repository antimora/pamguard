package generalDatabase.dataExport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import PamUtils.PamFileFilter;

import generalDatabase.PamTableItem;
import generalDatabase.pamCursor.PamCursor;

public class FileExportsystem extends DataExportSystem {

	DataExportDialog exportDialog;
	
	/**
	 * @param exportDialog
	 */
	public FileExportsystem(DataExportDialog exportDialog) {
		super();
		this.exportDialog = exportDialog;
	}

	@Override
	boolean exportData(ArrayList<PamTableItem> tableItems, PamCursor exportCursor) {
		/**
		 * Open a dialog to get a text file name, then write all the data to it. 
		 * Will worry about options such as types of deliminator later on 
		 */
		File file = getExportFile();
		if (file == null) {
			return false;
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));

			int iCol = 0;
			int nCol = tableItems.size();
			Object dataObject;
			for (int i = 0; i < nCol; i++) {
				if (iCol++ > 0) {
					writeDeliminator(writer);
				}
				writer.append(tableItems.get(i).getName());
			}
			writer.newLine();
			exportCursor.beforeFirst();
			while (exportCursor.next()) {
				exportCursor.moveDataToTableDef(true);
				iCol = 0;
				for (int i = 0; i < nCol; i++) {
					if (iCol++ > 0) {
						writeDeliminator(writer);
					}
					dataObject = tableItems.get(i).getValue();
					if (dataObject != null) {
						writer.append(dataObject.toString());
					}
				}
				writer.newLine();
			}
			
			writer.close();
		} catch (IOException e) {
			System.out.println("Unable to export data fo file : " + file.getAbsolutePath());
		} catch (SQLException sqlE) {
			System.out.println("SQL exception exporting data to file : " + file.getAbsolutePath());
			sqlE.printStackTrace();
		}
		return true;
	}

	private void writeDeliminator(BufferedWriter writer) throws IOException {
		writer.append(", ");
	}

	private File getExportFile() {
		PamFileFilter fileFilter = new PamFileFilter("Text Files", ".txt");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(fileFilter);
//		fileChooser.setSelectedFile(new File(fileName));
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
//		fileChooser.set
		int state = fileChooser.showSaveDialog(null);
		if (state == JFileChooser.APPROVE_OPTION) {
			File aFile = fileChooser.getSelectedFile();
			return PamFileFilter.checkFileEnd(aFile, "txt", true);
		}
		else {
			return null;
		}
	}

	@Override
	String getSystemName() {
		return "File system";
	}

}
