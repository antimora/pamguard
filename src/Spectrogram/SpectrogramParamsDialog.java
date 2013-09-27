/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package Spectrogram;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import fftManager.FFTDataBlock;

import Layout.DisplayPanelProvider;
import Layout.DisplayProviderList;
import PamController.PamController;
import PamView.ColourArray;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.ColourArray.ColourArrayType;
import PamguardMVC.PamConstants;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class SpectrogramParamsDialog extends PamDialog implements ActionListener {

	static private SpectrogramParamsDialog singleInstance;

	private JComboBox sourceList;

	private JLabel panelChannelLabel[] = new JLabel[PamConstants.MAX_CHANNELS];
	private JComboBox panelChannelList[] = new JComboBox[PamConstants.MAX_CHANNELS];

	private JLabel source, sourceData, channel, fftLen, fftLenData, fftHop, fftHopData,
			sampleRate, sampleRateData;

	private JTextField minFData, maxFData, nPanels;

	private JButton minDefault, maxDefault;

	private JTextField minAmplitude, maxAmplitude;
	
	private JComboBox colourList;

	private JTextField pixsPerSlice, secsPerScreen;
	
	private JRadioButton wrapDisplay, scrollDisplay;

	private JRadioButton pixs, secs;
	
	private int currentNumPanels;
	
	private int numChannel=0; //Xiao Yan Deng

	
	private SourcePanel sourcePanel;

	private FFTDataBlock fftBlock;

	private float defaultMinFreq, defaultMaxFreq;
	
	private PluginPanel pluginPanel;
	
	private ObserverPanel observerPanel;

	private static SpectrogramParameters spectrogramParameters;

	private SpectrogramParamsDialog(Window parentFrame, SpectrogramParameters spectrogramParameters) {
		
		super(parentFrame, "Spectrogram Parameters",false);

		JPanel p = new JPanel();
//		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// p.setLayout(new BorderLayout());
		// p.add(BorderLayout.CENTER, new SourcePanel());
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JPanel[] ps = new JPanel[5];
//		ps[4] = waveformPanel = new WaveformPanel();
		ps[3] = new TimePanel();
		ps[2] = new AmplitudePanel();
		ps[1] = new FrequencyPanel();
		ps[4] = new ScrollPanel();
		sourcePanel = new SourcePanel();
		

		JTabbedPane tabbedPane = new JTabbedPane();
		p.add(tabbedPane);
		JPanel scalePanel;
		tabbedPane.addTab("Data Source", sourcePanel);
		tabbedPane.addTab("Scales", scalePanel = new JPanel());
		scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.Y_AXIS));
		
		//sourcePanel.add(ps[0]);
		scalePanel.add(ps[1]);
		scalePanel.add(ps[2]);
		scalePanel.add(ps[3]);
		scalePanel.add(ps[4]);
		
		fillSourcePanelData();

		setDialogComponent(p);
		
		pluginPanel = new PluginPanel();
		tabbedPane.addTab("Plug ins", pluginPanel);

		observerPanel = new ObserverPanel();
		tabbedPane.addTab("Mark Observers", observerPanel);
		

		sourceList.addActionListener(this);
		minDefault.addActionListener(this);
		maxDefault.addActionListener(this);
		pixs.addActionListener(this);
		secs.addActionListener(this);
		nPanels.addActionListener(this);
		
		sortChannelLists();
		
		setHelpPoint("displays.spectrogramDisplayHelp.docs.UserDisplay_Spectrogram_Configuring");
		
		setSendGeneralSettingsNotification(false);
	}

	public static SpectrogramParameters showDialog(Window parentFrame, SpectrogramParameters spectrogramParameters) {
		SpectrogramParamsDialog.spectrogramParameters = spectrogramParameters.clone();
		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
			singleInstance = new SpectrogramParamsDialog(parentFrame, spectrogramParameters);
		}
		singleInstance.initialiseSourcePanelData();
		singleInstance.fillSourcePanelData();
		singleInstance.pluginPanel.buildList();
		singleInstance.setVisible(true);
		return SpectrogramParamsDialog.spectrogramParameters;
	}

	@Override
	public void cancelButtonPressed() {
		spectrogramParameters = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == pixs) {
			EnableButtons();

		} else if (e.getSource() == secs) {
			EnableButtons();
		} else if (e.getSource() == minDefault) {
			setDefaultMinFreq();
		} else if (e.getSource() == maxDefault) {
			setDefaultMaxFreq();
		} else if (e.getSource() == sourceList) {
			fillSourcePanelData();
		}
		else if (e.getSource() == nPanels) {
			sortChannelLists();
		}
	}

	private void EnableButtons() {
		pixsPerSlice.setEnabled(pixs.isSelected());
		secsPerScreen.setEnabled(secs.isSelected());
//		waveformPanel.enableAutoWaveforms();
	}

	class SourcePanel extends JPanel {
		SourcePanel() {
			super();

			sourceList = new JComboBox();

//			ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
//					.getFFTDataBlocks();
//			PamProcess fftDataSource;
//			sourceList.removeAllItems();
//			for (int i = 0; i < fftBlocks.size(); i++) {
//				fftDataSource = fftBlocks.get(i).getParentProcess();
//				sourceList.addItem(fftDataSource.getProcessName() + "-"
//						+ fftBlocks.get(i).getDataName());
//			}
//			if (spectrogramParameters.fftBlockIndex < sourceList.getItemCount()) {
//				sourceList.setSelectedIndex(spectrogramParameters.fftBlockIndex);
//			}

			channel = new JLabel("Channel");
			//channelData = new JComboBox();
			nPanels = new JTextField(5);

			source = new JLabel("Data source");
			sourceData = new JLabel("");
			fftLen = new JLabel("FFT Length");
			fftLenData = new JLabel("11111 pt ( s)");
			fftHop = new JLabel("FFT Hop");
			fftHopData = new JLabel("??? pt ( s)");
			sampleRate = new JLabel("Sample Rate");
			sampleRateData = new JLabel(" ????? Hz");

			setBorder(BorderFactory.createTitledBorder("Source Data"));

			JPanel bot = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			bot.setLayout(layout);
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;

			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridwidth = 4;
//			addComponent(bot, source, constraints);
			addComponent(bot, sourceData, constraints);
			constraints.gridy = 1;
			constraints.gridwidth = 1;
			addComponent(bot, new JLabel("Number of Panels  "), constraints);
			constraints.gridx = 1;
			constraints.gridwidth = 1;
			addComponent(bot, nPanels, constraints);
			constraints.gridx = 2;
			constraints.gridwidth = 1;
			addComponent(bot, new JLabel("(hit enter)"), constraints);
			for (int i = 0; i < PamConstants.MAX_CHANNELS; i++){

				constraints.gridx = 0;
				constraints.gridy ++;
				constraints.gridwidth = 2;
				addComponent(bot, panelChannelLabel[i] = new JLabel("Panel " + i + " channel "), constraints);
				constraints.gridx = 2;
				constraints.gridwidth = 2;
				addComponent(bot, panelChannelList[i] = new JComboBox(), constraints);
				
			}
//			addComponent(bot, channel, constraints);
//			addComponent(bot, channelData, constraints);
			constraints.gridx = 0;
			constraints.gridy ++;
			constraints.gridwidth = 2;
			addComponent(bot, fftLen, constraints);
			constraints.gridx = 2;
			addComponent(bot, fftLenData, constraints);
			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(bot, fftHop, constraints);
			constraints.gridx = 2;
			addComponent(bot, fftHopData, constraints);
			constraints.gridx = 0;
			constraints.gridy ++;
			addComponent(bot, sampleRate, constraints);
			constraints.gridx = 2;
			addComponent(bot, sampleRateData, constraints);

			setLayout(new BorderLayout());
			add(BorderLayout.NORTH, sourceList);
			add(BorderLayout.CENTER, bot);

			pack();

		}
//		void addComponent(JPanel panel, Component p, GridBagConstraints constraints){
//			((GridBagLayout) panel.getLayout()).setConstraints(p, constraints);
//			panel.add(p);
//		}
		
		/**
		 * Any number of channels are allowed except zero. 
		 * Users may want to display the same spectrogram 
		 * multiple times with different overlays. 
		 */
		int getNumPanels() {
			int nP;
			try {
				nP = Integer.valueOf(nPanels.getText());
			}
			catch (NumberFormatException Ex) {
				nP = 0;;
			}
			if (nP <= 0) {
				nPanels.setText("1");
				nP = 1;
			}
			return nP;
		}

//		int getNumPanels() {  //Xiao Yan Deng
//			try {
//				int np = Integer.valueOf(nPanels.getText());
//				if(np>0){
//					if (np<numChannel){
//						return Math.min(np, panelChannelList.length);
//					}
//					else{
//						return Math.min(numChannel, panelChannelList.length);
//					}			
//				}
//				else{
//					return Math.min(numChannel, panelChannelList.length);	
//				}
//			}
//			catch (Exception Ex) {
//				return 0;
//			}
//		}
	}

	private void initialiseSourcePanelData() {

		ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
		.getFFTDataBlocks();

		PamProcess pamDataSource;
		sourceList.removeAllItems();
		for (int i = 0; i < fftBlocks.size(); i++) {
			pamDataSource = fftBlocks.get(i).getParentProcess();
			sourceList.addItem(pamDataSource.getProcessName() + "-"
					+ fftBlocks.get(i).getDataName());
		}
		if (spectrogramParameters.fftBlockIndex < sourceList.getItemCount()) {
			sourceList.setSelectedIndex(spectrogramParameters.fftBlockIndex);
		}
		
	}

	private void fillSourcePanelData() {


		int iset = sourceList.getSelectedIndex();
		ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
		.getFFTDataBlocks();

		PamProcess pamDataSource;

//		FFTDataBlock fftDataSource;
		if (iset >= 0 && iset < fftBlocks.size()) {
			fftBlock = (FFTDataBlock) fftBlocks.get(iset);
//			fftDataSource = (FFTDataSource) fftBlock.getParentProcess();
			pamDataSource = fftBlock.getParentProcess();
			defaultMinFreq = 0;
			defaultMaxFreq = fftBlock.getSampleRate() / 2;
		} else {
			return;
		}
		
		sortChannelLists();

		nPanels.setText(String.format("%d", spectrogramParameters.nPanels)); //Xiao Yan Deng commented
//		System.out.println("SpectrogramParameterDialog.java->fillSourcePanelData()numChannel:"+numChannel);
//		nPanels.setText(String.format("%d", numChannel)); //Xiao Yan Deng
		
		sourceData.setText(pamDataSource.getProcessName() + " " + iset);
		fftLenData.setText(fftBlock.getFftLength() + " samples");
		fftHopData.setText(fftBlock.getFftHop() + " samples ");
		sampleRateData.setText(fftBlock.getSampleRate() + " Hz");

		if (spectrogramParameters.frequencyLimits[0] == 0
				|| spectrogramParameters.frequencyLimits[0] > defaultMaxFreq / 2) {
			spectrogramParameters.frequencyLimits[0] = defaultMinFreq;
		}
		if (spectrogramParameters.frequencyLimits[1] == 0
				|| spectrogramParameters.frequencyLimits[1] > defaultMaxFreq / 2) {
			spectrogramParameters.frequencyLimits[1] = defaultMaxFreq;
		}
		minFData.setText(String.format("%.0f",
				spectrogramParameters.frequencyLimits[0]));
		maxFData.setText(String.format("%.0f",
				spectrogramParameters.frequencyLimits[1]));

		minAmplitude.setText(String.format("%.1f",
				spectrogramParameters.amplitudeLimits[0]));
		maxAmplitude.setText(String.format("%.1f",
				spectrogramParameters.amplitudeLimits[1]));
		colourList.setSelectedIndex(spectrogramParameters.getColourMap().ordinal());

		pixs.setSelected(spectrogramParameters.timeScaleFixed == false);
		secs.setSelected(spectrogramParameters.timeScaleFixed == true);

		pixsPerSlice.setText(String.format("%d",
				spectrogramParameters.pixelsPerSlics));
		secsPerScreen.setText(String.format("%.1f",
				spectrogramParameters.displayLength));
		wrapDisplay.setSelected(spectrogramParameters.wrapDisplay);
		scrollDisplay.setSelected(!spectrogramParameters.wrapDisplay);

		EnableButtons();

	}
	private void sortChannelLists() {
		// first of all, only show the ones in range of nPanels
		int nP = sourcePanel.getNumPanels();
		numChannel=0;
		
		for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
			panelChannelLabel[i].setVisible(i < nP);
			panelChannelList[i].setVisible(i < nP);
		}

		if (fftBlock != null) {
			int channelMap = fftBlock.getChannelMap();
			for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) { //Xiao Yan Deng
				if ((channelMap& (1 << i)) != 0) {
					numChannel++;
				}
			}
	
			for (int iL = 0; iL < nP; iL++) {
				panelChannelList[iL].removeAllItems();
				if (fftBlock == null) continue;
				for (int i = 0; i < PamConstants.MAX_CHANNELS; i++) {
					if ((channelMap & (1 << i)) != 0) {
						panelChannelList[iL].addItem(i);
//						if (spectrogramParameters.channelList[iL] == i) {
//						panelChannelList[iL].setSelectedIndex(panelChannelList[iL].getItemCount()-1);
//						}
					}
				}
				panelChannelList[iL].setSelectedItem(spectrogramParameters.channelList[iL]); //Xiao Yan Deng commented
	
//				panelChannelList[iL].setSelectedIndex(iL); //Xiao Yan Deng
//				System.out.println("SpectrogramParameter.java->sortChannelLists:spectrogramParameters.channelList:"+iL+":"+spectrogramParameters.channelList[iL]);
			}
			
			currentNumPanels = nP;
		}
		
		pack();
	}

	private void setDefaultMinFreq() {
		minFData.setText(String.format("%.0f", this.defaultMinFreq));
	}

	private void setDefaultMaxFreq() {
		maxFData.setText(String.format("%.0f", this.defaultMaxFreq));
	}

	class FrequencyPanel extends JPanel {
		FrequencyPanel() {
			super();

			minFData = new JTextField(8);
			maxFData = new JTextField(8);

			minDefault = new JButton("Default");
			maxDefault = new JButton("Default");

			this.setBorder(BorderFactory.createTitledBorder("Frequency Range"));
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();

			addComponent(this, new JLabel("Min "), c);
			c.gridy++;
			addComponent(this, new JLabel("Max "), c);

			c.gridx++;
			c.gridy = 0;
			addComponent(this, minFData, c);
			c.gridy++;
			addComponent(this, maxFData, c);

			c.gridx++;
			c.gridy = 0;
			addComponent(this, new JLabel(" Hz "), c);
			c.gridy++;
			addComponent(this, new JLabel(" Hz "), c);

			c.gridx++;
			c.gridy = 0;
			addComponent(this, minDefault, c);
			c.gridy++;
			addComponent(this, maxDefault, c);
			
//
//			p = new JPanel();
//			p.setLayout(new GridLayout(2, 1));
//			p.add(minDefault);
//			p.add(maxDefault);
//			add(p);

		}
	}

	class AmplitudePanel extends JPanel {

		AmplitudePanel() {
			super();

			minAmplitude = new JTextField(6);
			maxAmplitude = new JTextField(6);

			this.setBorder(BorderFactory.createTitledBorder("Amplitude Range"));
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();

			addComponent(this, new JLabel("Min "), c);
			c.gridy++;
			addComponent(this, new JLabel("Max "), c);
			
			c.gridx++;
			c.gridy = 0;
			addComponent(this, minAmplitude, c);
			c.gridy++;
			addComponent(this, maxAmplitude, c);

			c.gridx++;
			c.gridy = 0;
			addComponent(this, new JLabel(" dB "), c);
			c.gridy++;
			addComponent(this, new JLabel(" dB "), c);
			
			c.gridx = 0;
			c.gridy++;
			addComponent(this, new JLabel("Colour model "), c);
			c.gridx++;
			c.gridwidth = 2;
			addComponent(this, colourList = new JComboBox(), c);
			
			ColourArrayType[] types = ColourArray.ColourArrayType.values();
			for (int i = 0; i < ColourArray.ColourArrayType.values().length; i++) {
				colourList.addItem(ColourArray.getName(types[i]));
			}
			
		}
	}

	class TimePanel extends JPanel {

		TimePanel() {
			super();

			pixsPerSlice = new JTextField(4);
			secsPerScreen = new JTextField(4);
			ButtonGroup g = new ButtonGroup();
			pixs = new JRadioButton("Pixels per FFT");
			g.add(pixs);
			secs = new JRadioButton("Window length (s)");
			g.add(secs);

			this.setBorder(BorderFactory.createTitledBorder("Time Range"));
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();

			addComponent(this, pixs, c);
			c.gridy++;
			addComponent(this, secs, c);
			
			c.gridx++;
			c.gridy = 0;
			addComponent(this, pixsPerSlice, c);
			c.gridy++;
			addComponent(this, secsPerScreen, c);
			
//			c.gridx = 0;
//			c.gridy++;
//			addComponent(this, wrapDisplay = new JRadioButton("Wrap Display"),  c);
//			c.gridx++;
//			c.gridwidth = 2;
//			addComponent(this, scrollDisplay = new JRadioButton("Scroll Display"),  c);
//			ButtonGroup bg = new ButtonGroup();
//			bg.add(wrapDisplay);
//			bg.add(scrollDisplay);
		}
	}
	class ScrollPanel extends JPanel {

		ScrollPanel() {
			super();

			this.setBorder(BorderFactory.createTitledBorder("Scrolling"));
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();

			c.gridx = 0;
			c.gridy++;
			addComponent(this, wrapDisplay = new JRadioButton("Wrap Display"),  c);
			c.gridx++;
			c.gridwidth = 2;
			addComponent(this, scrollDisplay = new JRadioButton("Scroll Display"),  c);
			ButtonGroup bg = new ButtonGroup();
			bg.add(wrapDisplay);
			bg.add(scrollDisplay);
		}
	}
	class PluginPanel extends JPanel {
		
		JCheckBox[] providerList;
		
		JPanel northPanel;
		
		PluginPanel() {
			super();
			northPanel = new JPanel();
			setLayout(new BorderLayout());
			add(BorderLayout.NORTH, northPanel);
			buildList();
		}
		
		void buildList() {
			DisplayPanelProvider dp;
			northPanel.removeAll();
			int n = DisplayProviderList.getDisplayPanelProviders().size();
			if (n == 0) return;
			providerList = new JCheckBox[n];
			northPanel.setLayout(new GridLayout(n+1, 1));
			northPanel.setBorder(new EmptyBorder(10, 10, 0, 0));
			northPanel.add(new JLabel("Select additional display panels ..."));
			for (int i = 0; i < n; i++) {
				dp = DisplayProviderList.getDisplayPanelProviders().get(i);
				providerList[i] = new JCheckBox(dp.getDisplayPanelName());
				northPanel.add(providerList[i]);
			}
			if (spectrogramParameters.showPluginDisplay != null) {
				for (int i = 0; i < Math.min(n, spectrogramParameters.showPluginDisplay.length); i++) {
					providerList[i].setSelected(spectrogramParameters.showPluginDisplay[i]);
				}
			}

		}
		boolean[] getList() {
			
			int n = DisplayProviderList.getDisplayPanelProviders().size();
			if (n == 0) return null;
			
			boolean[] selection = new boolean[n];
			for (int i = 0; i < n; i++) {
				selection[i] = providerList[i].isSelected();
			}
			
			return selection;
		}
	}
	class ObserverPanel extends JPanel {
		
		JCheckBox[] observerList;
		
		JPanel northPanel;
		
		ObserverPanel() {
			super();
			northPanel = new JPanel();
			setLayout(new BorderLayout());
			add(BorderLayout.NORTH, northPanel);
			buildList();
		}
		
		void buildList() {
			SpectrogramMarkObserver observer;
			northPanel.removeAll();
			int n = SpectrogramMarkObservers.getSpectrogramMarkObservers().size();
			if (n == 0) return;
			observerList = new JCheckBox[n];
			northPanel.setLayout(new GridLayout(n+1, 1));
			northPanel.setBorder(new EmptyBorder(10, 10, 0, 0));
			northPanel.add(new JLabel("Select spectrogram mark observers ..."));
			for (int i = 0; i < n; i++) {
				observer = SpectrogramMarkObservers.getSpectrogramMarkObservers().get(i);
				observerList[i] = new JCheckBox(observer.getMarkObserverName());
				northPanel.add(observerList[i]);
			}
			if (spectrogramParameters.useSpectrogramMarkObserver != null) {
				for (int i = 0; i < Math.min(n, spectrogramParameters.useSpectrogramMarkObserver.length); i++) {
					observerList[i].setSelected(spectrogramParameters.useSpectrogramMarkObserver[i]);
				}
			}

		}
		boolean[] getList() {
			
			int n = SpectrogramMarkObservers.getSpectrogramMarkObservers().size();
			if (n == 0) return null;
			
			boolean[] selection = new boolean[n];
			for (int i = 0; i < n; i++) {
				selection[i] = observerList[i].isSelected();
			}
			
			return selection;
		}
	}
	
	
	@Override
	public boolean getParams() {

		int iset = sourceList.getSelectedIndex();
		ArrayList<PamDataBlock> fftBlocks = PamController.getInstance()
				.getFFTDataBlocks();
		FFTDataBlock fftBlock;
//		FFTDataSource fftDataSource;
		int ch;
		if (iset >= 0 && iset < fftBlocks.size()) {
			fftBlock = (FFTDataBlock) fftBlocks.get(iset);
//			fftDataSource = (FFTDataSource) fftBlock.getParentProcess();
			spectrogramParameters.fftBlockIndex = iset;
		} else {
			return false;
		}

		try {
			spectrogramParameters.nPanels = sourcePanel.getNumPanels();
			for (int i = 0; i < Math.min(spectrogramParameters.nPanels, currentNumPanels); i++) {
				ch = (Integer) panelChannelList[i].getSelectedItem();
//				ch = Integer.valueOf(str);
				spectrogramParameters.channelList[i] = ch;
			}

			spectrogramParameters.frequencyLimits[0] = Double.valueOf(
					minFData.getText());
			spectrogramParameters.frequencyLimits[1] = Double.valueOf(
					maxFData.getText());

			spectrogramParameters.amplitudeLimits[0] = Double.valueOf(
					minAmplitude.getText());
			spectrogramParameters.amplitudeLimits[1] = Double.valueOf(
					maxAmplitude.getText());
			spectrogramParameters.setColourMap(ColourArrayType.values()[colourList.getSelectedIndex()]);

			spectrogramParameters.timeScaleFixed = secs.isSelected();
			spectrogramParameters.displayLength = Double.valueOf(
					secsPerScreen.getText());
			spectrogramParameters.pixelsPerSlics = Integer.valueOf(pixsPerSlice
					.getText());
			
//			spectrogramParameters.showWaveform = showWaveforms.isSelected();
//			spectrogramParameters.autoScaleWaveform = autoScaleWaveforms.isSelected();

		} catch (Exception Ex) {
			Ex.printStackTrace();
			return false;
		}

		spectrogramParameters.fftBlockIndex = iset;
		spectrogramParameters.sourceName = fftBlock.getDataName();
		spectrogramParameters.windowName = spectrogramParameters.sourceName;
		spectrogramParameters.showPluginDisplay = pluginPanel.getList();
		spectrogramParameters.useSpectrogramMarkObserver = observerPanel.getList();
		spectrogramParameters.wrapDisplay = wrapDisplay.isSelected();

		return true;
	}
}
