package binaryFileStorage;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;

/**
 * Not really a dialog, but displayed by BinaryStore while its making maps 
 * of stored data during Viewer operation. 
 * @author Doug Gillespie
 *
 */
public class BinaryMapMakingDialog extends PamDialog {

	private static BinaryMapMakingDialog singleInstance;
	
	private JProgressBar streamProgress;
	
	private JLabel streamName;
	
	private BinaryMapMakingDialog(Window parentFrame) {
		super(parentFrame, "Binary Data Mapping", false);
		
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Creating data map"));
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		p.add(streamName = new JLabel(""));
		p.add(streamProgress = new JProgressBar());
//		p.setPreferredSize(new Dimension(400, 200));
		streamName.setPreferredSize(new Dimension(250, 5));
		
		setDialogComponent(p);
		
		getOkButton().setVisible(false);
		getCancelButton().setVisible(false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setModalityType(Dialog.ModalityType.MODELESS);
	}
	
	public static BinaryMapMakingDialog showDialog(Window parentFrame) {
		if (singleInstance == null || singleInstance.getOwner() == null ||
				singleInstance.getOwner() != parentFrame) {
			singleInstance = new BinaryMapMakingDialog(parentFrame);
		}
		
		singleInstance.setVisible(true);
		return singleInstance;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible == false) {
//			dispose();
//			singleInstance = null;
			closeLater();
		}
		else {
			super.setVisible(visible);
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

	public void setProgress(BinaryMapMakeProgress binaryMapMakeProgress) {
		switch (binaryMapMakeProgress.getStatus()) {
		case BinaryMapMakeProgress.STATUS_IDLE:
			streamName.setText("Idle");
			return;
		case BinaryMapMakeProgress.STATUS_COUNTING_FILES:
			streamName.setText("Counting Files");
			streamProgress.setIndeterminate(true);
			return;
		case BinaryMapMakeProgress.STATUS_DESERIALIZING:
			streamName.setText("Loading serialized data map");
			streamProgress.setIndeterminate(true);
			return;
		case BinaryMapMakeProgress.STATUS_SERIALIZING:
			streamName.setText("Saving serialized data map");
//			streamProgress.setIndeterminate(true);
			return;
		case BinaryMapMakeProgress.STATUS_ANALYSING_FILES:
			streamProgress.setIndeterminate(false);
			streamName.setText(binaryMapMakeProgress.getStreamName());
			streamProgress.setMaximum(binaryMapMakeProgress.getTotalStreams());
			streamProgress.setValue(binaryMapMakeProgress.getCurrentStream());
			return;
		}
	}

}
