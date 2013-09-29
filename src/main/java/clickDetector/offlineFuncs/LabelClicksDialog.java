package clickDetector.offlineFuncs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import clickDetector.ClickBTDisplay;
import clickDetector.ClickControl;
import clickDetector.ClickDetection;

import PamView.PamDialog;

/**
 * Dialog for adding and removing clicks to and from 
 * offline events. 
 * @author Doug Gillespie
 *
 */
public class LabelClicksDialog extends PamDialog {
	
	private OfflineEventListPanel offlineEventListPanel;
	
	private static LabelClicksDialog singleInstance;
	
	private ClickControl clickControl;
	
	private ClickBTDisplay btDisplay;
	
	private ClickDetection singleClick;
	
	private JButton newEventButton;
	
	private Window parentFrame;
	
	private OfflineEventDataBlock offlineEventDataBlock;
	
	private ArrayList<ClickDetection> markedClicks;

	private LabelClicksDialog(Window parentFrame, ClickControl clickControl) {
		super(parentFrame, clickControl.getUnitName() + " Label clicks", false);
		this.parentFrame = parentFrame;
		this.clickControl = clickControl;
		offlineEventDataBlock = clickControl.getClickDetector().getOfflineEventDataBlock();
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel eventListPanel = new JPanel(new BorderLayout());
		offlineEventListPanel = new OfflineEventListPanel(clickControl);
		eventListPanel.add(BorderLayout.CENTER, offlineEventListPanel.getPanel());
		eventListPanel.add(BorderLayout.NORTH, new JLabel("Event list"));
		mainPanel.add(BorderLayout.CENTER, eventListPanel);
		
		JPanel southPanel = new JPanel(new BorderLayout());
		JPanel sePanel = new JPanel(new BorderLayout());
		sePanel.add(BorderLayout.NORTH, newEventButton = new JButton("New Event ..."));
		southPanel.add(BorderLayout.EAST, sePanel);
		newEventButton.addActionListener(new NewEvent());
		southPanel.add(BorderLayout.WEST, offlineEventListPanel.getSelectionPanel());
//		southPanel.add(editSpeciesButton = new JButton("Edit list"));
//		editSpeciesButton.addActionListener(new EditSpeciesList());
		mainPanel.add(BorderLayout.SOUTH, southPanel);
		
		offlineEventListPanel.addMouseListener(new TableMouse());
		offlineEventListPanel.addListSelectionListener(new ListSelection());
		
		setResizable(true);
		
		setDialogComponent(mainPanel);
	}
	
	public static void showDialog(Window parentFrame, ClickControl clickControl, 
			ClickBTDisplay clickBTDisplay, ClickDetection singleClick) {
		if (singleInstance == null || singleInstance.getOwner() != parentFrame || 
				singleInstance.clickControl != clickControl) {
			singleInstance = new LabelClicksDialog(parentFrame, clickControl);
		}
		singleInstance.singleClick = singleClick;
		singleInstance.btDisplay = clickBTDisplay;
		singleInstance.setParams();
		singleInstance.setVisible(true);
	}
	
	private void setParams() {
		offlineEventListPanel.tableDataChanged();
		if (singleClick != null) {
			markedClicks = new ArrayList<ClickDetection>();
			markedClicks.add(singleClick);
		}
		else {
			markedClicks = btDisplay.getMarkedClicks();
		}
		enableControls();
	}

	private void enableControls() {
		boolean haveClicks = (markedClicks != null && markedClicks.size() > 0);
		newEventButton.setEnabled(haveClicks);
		
		OfflineEventDataUnit selEvent = offlineEventListPanel.getSelectedEvent();
		getOkButton().setEnabled(selEvent != null);
	}
	
	@Override
	public void cancelButtonPressed() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getParams() {
		// called when the OK button is pressed. 
		OfflineEventDataUnit selEvent = offlineEventListPanel.getSelectedEvent();
		if (selEvent != null) {
			addClicksToEvent(selEvent, false);
			return true;
		}
		return false;
	}

	/**
	 * Add a group of clicks to a new event, then optinally close the dialog
	 * @param event event to add to 
	 * @param thenClose option to close dialog
	 */
	private void addClicksToEvent(OfflineEventDataUnit event, boolean thenClose) {
		event.addClicks(markedClicks);
		offlineEventListPanel.tableDataChanged();
		clickControl.setLatestOfflineEvent(event);
		if (thenClose) {
			setVisible(false);
		}		
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}
	
	private class ListSelection implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			enableControls();
		}
	}
	
	private class TableMouse extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				mouseDoubleClick();
			}
		}
	}
	
	/**
	 * Double clicking on an event will automatically add the marked clicks 
	 * to that event and close the dialog. 
	 */
	public void mouseDoubleClick() {
		OfflineEventDataUnit event = offlineEventListPanel.getSelectedEvent();
		if (event == null) {
			return;
		}
		event = OfflineEventDialog.showDialog(parentFrame, clickControl, event);
		if (event != null) {
			addClicksToEvent(event, true);
			event.notifyUpdate();
		}
	}

	private class NewEvent implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			newEvent();
		}
	}

	public void newEvent() {
//		int nextNum = offlineEventDataBlock.getNextFreeEventNumber();
		OfflineEventDataUnit newUnit = new OfflineEventDataUnit(null, 0, null);
		newUnit = OfflineEventDialog.showDialog(parentFrame, clickControl, newUnit);
		if (newUnit != null) {
			addClicksToEvent(newUnit, false);
			if (newUnit.getParentDataBlock() == null) {
				offlineEventDataBlock.addPamData(newUnit);
				offlineEventDataBlock.notifyObservers(newUnit);
			}
			setVisible(false);
		}
	}
	

}
