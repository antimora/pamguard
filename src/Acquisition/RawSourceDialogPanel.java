package Acquisition;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamController.PamController;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamRawDataBlock;

public class RawSourceDialogPanel extends JPanel {

	JComboBox sourceList;
	
	public RawSourceDialogPanel(String borderName) {
		super();
		setBorder(new TitledBorder(borderName));
		setLayout(new BorderLayout());
		sourceList = new JComboBox();
		//setPreferredSize(new Dimension(250, 70));
		add(BorderLayout.NORTH, sourceList);
	}
	
	public void setSource(String sourceName) {
		sourceList.removeAllItems();
		PamRawDataBlock rawDataBlock = PamController.getInstance().getRawDataBlock(sourceName);// = clickParameters.rawDataSource;
		ArrayList<PamDataBlock> rd = PamController.getInstance().getRawDataBlocks();
		for (int i = 0; i < rd.size(); i++) {
			sourceList.addItem(rd.get(i));
		}
		sourceList.setSelectedItem(rawDataBlock);
		
	}
	
	public String getSource() {
		PamRawDataBlock rawDataBlock = (PamRawDataBlock) sourceList.getSelectedItem();
		if (rawDataBlock == null) return null;
		return rawDataBlock.toString();
	}
}
