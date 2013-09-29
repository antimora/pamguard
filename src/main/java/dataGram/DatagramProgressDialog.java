package dataGram;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import PamView.CancelObserver;
import PamView.PamDialog;

public class DatagramProgressDialog extends PamDialog {

	private JProgressBar  mapPointProgress;

	private JLabel blockName;

	private int nDataBlocks;

	private int nMapPoints;
	

	private static DatagramProgressDialog singleInstance;
	
	public DatagramProgressDialog(Window parentFrame) {
		super(parentFrame, "Datagram Creation Progress", false);JPanel p = new JPanel();
		
		p.setBorder(new TitledBorder("Creating Datagram"));
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		p.add(blockName = new JLabel("Data name"));
		p.add(mapPointProgress = new JProgressBar());
		
//		p.setPreferredSize(new Dimension(400, 200));
		blockName.setPreferredSize(new Dimension(250, 15));
		
		setDialogComponent(p);
		
		getOkButton().setVisible(false);
		getCancelButton().setVisible(true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setModalityType(Dialog.ModalityType.MODELESS);
	}
	
	public static DatagramProgressDialog showDialog(Window parentFrame, CancelObserver cancelObserver) {
		if (singleInstance == null || singleInstance.getOwner() == null ||
				singleInstance.getOwner() != parentFrame) {
			singleInstance = new DatagramProgressDialog(parentFrame);
		}
		singleInstance.setCancelObserver(cancelObserver);
		singleInstance.setVisible(true);
		return singleInstance;
	}
	
	void setProgress(DatagramProgress datagramProgress) {
		switch(datagramProgress.statusType) {
		case DatagramProgress.STATUS_BLOCKCOUNT:
			nDataBlocks = datagramProgress.nDataBlocks;
			break;
		case DatagramProgress.STATUS_STARTINGBLOCK:
			blockName.setText(datagramProgress.dataBlock.getDataName());
			nMapPoints = datagramProgress.pointsToUpdate;
			mapPointProgress.setMaximum(nMapPoints);
			break;
		case DatagramProgress.STATUS_STARTINGFILE:
			mapPointProgress.setValue(datagramProgress.currentPoint);
			break;
		case DatagramProgress.STATUS_ENDINGFILE:
			mapPointProgress.setValue(datagramProgress.currentPoint);
			break;
		}
		
	}

	@Override
	public void cancelButtonPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getParams() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}
}
