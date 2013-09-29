package clickDetector.offlineFuncs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;
import soundPlayback.PlaybackControl;

import clickDetector.BTDisplayParameters;
import clickDetector.ClickBTDisplay;
import clickDetector.ClickControl;
import clickDetector.ClickDisplay;
import clickDetector.ClickClassifiers.ClickIdentifier;

import PamView.PamPanel;
import PamView.PamRadioButton;
import PamView.PamToolBar;

public class OfflineToolbar {

	private PamToolBar toolBar;
	
	private ClickControl clickControl;
	
	private JButton playClicks, reAnalyseClicks;
	
	private JCheckBox[] speciesButtons;
	
	private JCheckBox showNonSpecies;
	
	private JCheckBox showEchoes;
		
	private JCheckBox clicksInAnEvent;
	
	private ClickBTDisplay currentBTDisplay;

	private boolean isViewer;

	private JComboBox<String> andOrSelection;

	private boolean firstSetup;
	
	public JToolBar getToolBar() {
		return toolBar;
	}

	public void setToolBar(PamToolBar toolBar) {
		this.toolBar = toolBar;
	}


	public OfflineToolbar(ClickControl clickControl) {
		super();
		this.clickControl = clickControl;
		isViewer = clickControl.isViewerMode();
		
		toolBar = new PamToolBar("Offline Click Analysis");
		
		if (isViewer) {
			playClicks = new JButton(new ImageIcon(ClassLoader
					.getSystemResource("Resources/clickPlayStart.png")));
			playClicks.addActionListener(new PlayClicks());
			playClicks.setToolTipText("Play clicks (pack empty space with 0's)");
			PlaybackControl.registerPlayButton(playClicks);

			reAnalyseClicks = new JButton(new ImageIcon(ClassLoader
					.getSystemResource("Resources/reanalyseClicks.png")));
			reAnalyseClicks.addActionListener(new ReanalyseClicks());
			reAnalyseClicks.setToolTipText("Re-analyse clicks");
		}
		
		createStandardButtons();
		createSpeciesButtons();
		
	}
	
	private void createStandardButtons() {
		if (playClicks != null) {
			toolBar.add(playClicks);
		}
		if (reAnalyseClicks != null) {
			toolBar.add(reAnalyseClicks);
		}
		
		enableButtons();
	}
	
	public void setupToolBar() {
		toolBar.removeAll();
		createStandardButtons();
		createSpeciesButtons();
	}

//	public void addButtons(JButton[] buttons) {
//		toolBar.removeAll();
//		for (int i = 0; i < buttons.length; i++) {
//			if (buttons[i] != null) {
//				toolBar.add(buttons[i]);
//			}
//			else {
//				toolBar.addSeparator();
//			}
//		}
//		createStandardButtons();
//	}
	
	private void createSpeciesButtons() {
		ClickIdentifier clickId = clickControl.getClickIdentifier();

		ShowClicks showClicks = new ShowClicks();
		toolBar.add(new JLabel("  Show: "));
		toolBar.add(showEchoes = new JCheckBox("Echoes"));
		showEchoes.addActionListener(showClicks);
		
		PamPanel speciesBar1 = new PamPanel(new BorderLayout());
		PamPanel speciesBar = new PamPanel(new FlowLayout());
		speciesBar1.add(BorderLayout.WEST, speciesBar);
		toolBar.add(speciesBar1);
		showNonSpecies = new JCheckBox("Unclassified clicks");
		speciesBar.add(showNonSpecies);
		showNonSpecies.addActionListener(showClicks);
		if (clickId == null) {
			return;
		}
		String[] speciesList = clickId.getSpeciesList();
		if (speciesList == null || speciesList.length == 0) {
			return;
		}
		speciesButtons = new JCheckBox[speciesList.length];
		for (int i = 0; i < speciesList.length; i++) {
			speciesButtons[i] = new JCheckBox(speciesList[i]);
			speciesBar.add(speciesButtons[i]);
			speciesButtons[i].addActionListener(showClicks);
		}
		speciesBar.add(andOrSelection = new JComboBox<String>());
		speciesBar.add(clicksInAnEvent = new JCheckBox("Event clicks only"));
		andOrSelection.addItem("AND");
		andOrSelection.addItem("OR");
		andOrSelection.addActionListener(showClicks);
		clicksInAnEvent.addActionListener(showClicks);
		
	}

	private void enableButtons() {
//		boolean storeOpen = (clickControl.getClicksOffline() != null && clickControl.getClicksOffline().isOpen());
		if (reAnalyseClicks != null) {
			reAnalyseClicks.setEnabled(true);
		}
	}

	class ReanalyseClicks implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			clickControl.getClicksOffline().reAnalyseClicks();
		}
	}

	class PlayClicks implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			clickControl.playClicks();
		}
	}
	class ShowClicks implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			showClicksChanged();
		}
	}

	public void displayActivated(ClickDisplay clickDisplay) {
		if (clickDisplay.getClass() == ClickBTDisplay.class) {
			ClickBTDisplay btDisplay = (ClickBTDisplay) clickDisplay;
			currentBTDisplay = btDisplay;
			if (showEchoes==null||showNonSpecies==null||clicksInAnEvent==null||andOrSelection==null){
				System.out.println("stdButtonsNull in clicks offline toolbar");//createSpeciesButtons();
				return;
			}
			checkButtons(btDisplay.getBtDisplayParameters());
		}
	}

	public void showClicksChanged() {
		if (currentBTDisplay == null || firstSetup == false) {
			return;
		}
		try {
			BTDisplayParameters btDisplayParameters = currentBTDisplay.getBtDisplayParameters();
			btDisplayParameters.setShowSpecies(0, showNonSpecies.isSelected());
			btDisplayParameters.showEchoes = showEchoes.isSelected();
			btDisplayParameters.showEventsOnly = clicksInAnEvent.isSelected();
			btDisplayParameters.showANDEvents = (andOrSelection.getSelectedIndex() == 0);
			int n = speciesButtons.length;
			for (int i = 0; i < n; i++) {
				btDisplayParameters.setShowSpecies(i+1, speciesButtons[i].isSelected());
			}
			currentBTDisplay.repaintTotal();
		}
		catch (NullPointerException e) {

		}
	}

	private void checkButtons(BTDisplayParameters btDisplayParameters) {
		showEchoes.setSelected(btDisplayParameters.showEchoes);
		showNonSpecies.setSelected(btDisplayParameters.getShowSpecies(0));
		clicksInAnEvent.setSelected(btDisplayParameters.showEventsOnly);
		andOrSelection.setSelectedIndex(btDisplayParameters.showANDEvents ? 0: 1);
		if (speciesButtons != null) {
			int n = speciesButtons.length;
			for (int i = 0; i < n; i++) {
				speciesButtons[i].setSelected(btDisplayParameters.getShowSpecies(i+1));
			}
		}
		firstSetup = true;
	}
	
}
