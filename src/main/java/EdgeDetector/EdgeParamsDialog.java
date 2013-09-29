package EdgeDetector;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import PamController.PamController;
import PamView.PamDialog;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class EdgeParamsDialog extends PamDialog {

	private static EdgeParamsDialog singleInstance;
	
	private EdgeParameters edgeParameters;
	
	JComboBox sourceList;
	
	EdgeParamsDialog(Frame parentFrame) {
		super(parentFrame, "Edge Detection", true);

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

		sourceList = new JComboBox();
		p.setLayout(new BorderLayout());
		p.add(BorderLayout.NORTH, sourceList);

		setDialogComponent(p);
	}
	
	
	static public EdgeParameters showDialog(Frame parentFrame, EdgeParameters edgeParameters) {
		
		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
			singleInstance = new EdgeParamsDialog(parentFrame);
		}
		
		singleInstance.edgeParameters = edgeParameters.clone();
		
		singleInstance.setParams(edgeParameters);
		
		singleInstance.setVisible(true);
		
		return singleInstance.edgeParameters;
	}
	
	private void setParams(EdgeParameters edgeParameters) {

		ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
				.getFFTDataBlocks();
		sourceList.removeAllItems();
		PamProcess fftDataSource;
		for (int i = 0; i < fftBlocks.size(); i++) {
			fftDataSource = fftBlocks.get(i).getParentProcess();
			sourceList.addItem(fftDataSource.getProcessName() + "-"
					+ fftBlocks.get(i).getDataName());
		}
		sourceList.setSelectedIndex(edgeParameters.fftBlockIndex);
	}
	
	@Override
	public boolean getParams() {
		edgeParameters.fftBlockIndex = sourceList.getSelectedIndex();
		return true;
	}


	@Override
	public void cancelButtonPressed() {
		edgeParameters = null;		
	}


	@Override
	public void restoreDefaultSettings() {
		setParams(new EdgeParameters());
	}
	
}
