package clickDetector.offlineFuncs;

import generalDatabase.lookupTables.LookupComponent;
import generalDatabase.lookupTables.LookupItem;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import clickDetector.ClickControl;

import PamView.DBTextArea;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class OfflineEventDialog extends PamDialog {
	
	private OfflineEventDataUnit offlineEventDataUnit;
	
	private static OfflineEventDialog singleInstance;
	
	private ClickControl clickControl;
	
	private JTextField eventNumber;
	
//	private JComboBox eventType, eventColour;
	private LookupComponent speciesList;
	
	private JTextField minNum, bestNum, maxNum;
	
	private DBTextArea commentText;

	public OfflineEventDialog(Window parentFrame, ClickControl clickControl) {
		super(parentFrame, "Click event", false);
		this.clickControl = clickControl;
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridx = c.gridy = 0;
		c.gridwidth = 3;
		addComponent(mainPanel, new JLabel("Event Number", SwingConstants.RIGHT), c);
		c.gridx += c.gridwidth;
		c.gridwidth = 2;
		addComponent(mainPanel, eventNumber = new JTextField(3), c);
		eventNumber.setEditable(false);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 6;
		//TODO FIXME! check this!!!
		speciesList = new LookupComponent("OfflineRCEvents", null);
		speciesList.setNorthTitle("Event type / species");
		addComponent(mainPanel, speciesList.getComponent(), c);
//		addComp
//		c.gridx++;
//		addComponent(mainPanel, new JLabel(" Colour"), c);
//		c.gridx++;
//		c.gridwidth = 2;
//		addComponent(mainPanel, eventColour = new JComboBox(), c);
		
		c.gridx = 0;
		c.gridwidth = 6;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		addComponent(mainPanel, new JLabel("Estimated number of animals"), c);
		c.gridy++;
		c.gridwidth = 1;
		addComponent(mainPanel, new JLabel("Min ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(mainPanel, minNum = new JTextField(3), c);
		c.gridx++;
		addComponent(mainPanel, new JLabel(" Best ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(mainPanel, bestNum = new JTextField(3), c);
		c.gridx++;
		addComponent(mainPanel, new JLabel(" Max ", SwingConstants.RIGHT), c);
		c.gridx++;
		addComponent(mainPanel, maxNum = new JTextField(3), c);
		c.gridx++;
		c.gridx = 0;
		c.gridwidth = 6;
		c.gridy++;
		addComponent(mainPanel, new JLabel("Comment "), c);
		c.gridy++;
		commentText = new DBTextArea(1, 2, OfflineEventLogging.COMMENT_LENGTH);
		commentText.getComponent().setPreferredSize(new Dimension(250, 150));
		addComponent(mainPanel, commentText.getComponent(), c);
		
		setDialogComponent(mainPanel);
		
	}
	
	public static OfflineEventDataUnit showDialog(Window parentFrame, ClickControl clickControl, OfflineEventDataUnit offlineEventDataUnit) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame || 
				singleInstance.clickControl != clickControl) {
			singleInstance = new OfflineEventDialog(parentFrame, clickControl);
		}
		singleInstance.offlineEventDataUnit = offlineEventDataUnit;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.offlineEventDataUnit;
	}

	@Override
	public void cancelButtonPressed() {
		offlineEventDataUnit = null;
	}  
	
	private void setParams() {
		if (offlineEventDataUnit == null) {
			eventNumber.setText(null);
			minNum.setText(null);
			bestNum.setText(null);
			maxNum.setText(null);
			commentText.setText(null);
			speciesList.setSelectedCode(null);
		}
		else {
			setIntegerText(eventNumber, offlineEventDataUnit.getEventNumber());
			setShortText(minNum, offlineEventDataUnit.getMinNumber());
			setShortText(bestNum, offlineEventDataUnit.getBestNumber());
			setShortText(maxNum, offlineEventDataUnit.getMaxNumber());
			commentText.setText(offlineEventDataUnit.getComment());
			speciesList.setSelectedCode(offlineEventDataUnit.getEventType());
		}
	}
	
	private void setIntegerText(JTextField tf, Integer intNum) {
		if (intNum == null) {
			tf.setText(null);
		}
		else {
			tf.setText(String.format("%d", intNum));
		}
	}
	private void setShortText(JTextField tf, Short intNum) {
		if (intNum == null) {
			tf.setText(null);
		}
		else {
			tf.setText(String.format("%d", intNum));
		}
	}
	
	private Integer getIntegerValue(JTextField tf) {
		try {
			return Integer.valueOf(tf.getText());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
	private Short getShortValue(JTextField tf) {
		try {
			return Short.valueOf(tf.getText());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public boolean getParams() {
		if (offlineEventDataUnit == null) {
			return false;
		}
		LookupItem selItem = speciesList.getSelectedItem();
		if (selItem == null) {
			return showWarning("You must select a species / event type");
		}
		offlineEventDataUnit.setEventType(selItem.getCode());
		offlineEventDataUnit.setMinNumber(getShortValue(minNum));
		offlineEventDataUnit.setBestNumber(getShortValue(bestNum));
		offlineEventDataUnit.setMaxNumber(getShortValue(maxNum));
		offlineEventDataUnit.setComment(commentText.getText());
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
