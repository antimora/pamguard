package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.sun.j3d.utils.scenegraph.io.retained.SymbolTable;

import staticLocaliser.StaticLocaliserParams;


import PamUtils.PamUtils;
import PamView.PamDialog;
import PamView.PamGridBagContraints;

public class OptionsDialog extends PamDialog {
	
	private static OptionsDialog singleInstance;
	
	private StaticLocaliserParams singleLocaliserParams;
	
	int[] hydrophoneChannelList;
	
	//Batch run
	JCheckBox firstOnly;
	JTextField minTD;
	JCheckBox usePrimeHydrophone;
	JTextField primaryHydrophone;
	
	//General
	JCheckBox saveLowestChiValue;
	JTextField maxPos;
	
	//Channels
	JCheckBox[] enableChannels; 
	int[] hydrophoneMap;
	
	//Map
	public JCheckBox hiResplotSymbol;

	
	//Dialog Components
	JPanel mainPanel; 
	JPanel batchOptions;
	JPanel mapOptions;
	JPanel genOptions;
	JPanel channelOptions;

	private StaticLocalisationMainPanel staticlocalisationDialog;

	private JCheckBox showLowestChi;


	
	private OptionsDialog(Window parentFrame, Point location, StaticLocalisationMainPanel staticlocalisationDialog) {
		super(parentFrame, "Static Localiser Options", false);
		
		this.staticlocalisationDialog=staticlocalisationDialog;
		//TODO
		//this seems a bit weird, getting the first unit in the datablock, however needed for offline datablocks. Need to fix
		hydrophoneMap=staticlocalisationDialog.getCurrentControlPanel().getChannelMap();
		
		mainPanel=new JPanel(new BorderLayout());
		batchOptions=new JPanel(new GridLayout(0,1));
		mapOptions=new JPanel(new GridBagLayout());
		genOptions=new JPanel(new GridLayout(0,1));
		channelOptions=new JPanel(new GridLayout(0,1));
		
		JTabbedPane tabs=new JTabbedPane();
		tabs.addTab("General", null, genOptions);
		if (staticlocalisationDialog.getStaticLocaliserControl().isViewer==true) tabs.addTab("Batch Localise", null, batchOptions );
		else tabs.addTab("Localisation", null, batchOptions );
		tabs.addTab("Map", null, mapOptions );
		tabs.addTab("Channels", null, channelOptions);
		
		//batch run options
		JPanel r = new JPanel(new GridBagLayout());
		PamGridBagContraints c = new PamGridBagContraints();
		r.setBorder(new TitledBorder("Time Delays"));
		c.gridwidth = 3;
		addComponent(r, firstOnly = new JCheckBox("Use only first time delay"), c);
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy++;
		addComponent(r, new JLabel("Min no. of time delays "), c);
		c.gridx++;
		addComponent(r, minTD = new JTextField(3), c);
		c.gridx++;
		addComponent(r, new JLabel("int"), c);
		c.gridy++;
		
		JPanel q = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		q.setBorder(new TitledBorder("Hydrophones"));
		c.gridwidth = 3;
		addComponent(q, usePrimeHydrophone = new JCheckBox("Use a primary hydrophone"), c);
		usePrimeHydrophone.addActionListener(new PrimaryHydrophoneCheckBox());
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy++;
		addComponent(q, new JLabel("Primary Hydrophone"), c);
		c.gridx++;
		addComponent(q, primaryHydrophone = new JTextField(3), c);
		c.gridx++;
		addComponent(q, new JLabel("int"), c);
		c.gridy++;
		
		JPanel s = new JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		c.gridwidth = 3;
		s.setBorder(new TitledBorder("Export Data"));
		
		batchOptions.add( r);
		batchOptions.add( q);
		batchOptions.add( s);
		
		//map options
		
		JPanel m=new JPanel(new GridBagLayout());
		m.setBorder(new TitledBorder("Map Symbols"));
		c = new PamGridBagContraints();
		c.gridy++;
		c.gridwidth = 3;
		addComponent(m, hiResplotSymbol = new JCheckBox("Use high res. plot symbols"), c);
		hiResplotSymbol.addActionListener(new HiResSymbolsListener());
		c.gridy++;
		mapOptions.add(m);
		//general options
		
		JPanel t = new JPanel(new GridBagLayout());
		t.setBorder(new TitledBorder("Results"));
		c = new PamGridBagContraints();
		c.gridx = 0;
		c.gridwidth = 1;
		addComponent(t, showLowestChi = new JCheckBox(), c);
		c.gridx++;
		addComponent(t, new JLabel("Display only lowest"), c);
		c.gridx++;
		addComponent(t, new JLabel("<html>X<sup>2</sup></html>"), c);
		c.gridx++;
		addComponent(t, new JLabel("value"), c);
		c.gridx++;
		t.setBorder(new TitledBorder("Results"));
		c.gridy++;
		c.gridx = 0;
		JPanel u = new JPanel(new GridBagLayout());
		u.setBorder(new TitledBorder("Time Delay Possibilities"));
		addComponent(u, new JLabel("Max no. of Possibilities"), c);
		c.gridx++;
		addComponent(u, maxPos = new JTextField(3), c);
		c.gridx++;
		addComponent(u, new JLabel("int"), c);
		c.gridy++;
		
		genOptions.add(t);
		genOptions.add(u);
		
		JPanel v=new JPanel(new GridBagLayout());
		v.setBorder(new TitledBorder("Channel Map"));
		//get the channel array
		c = new PamGridBagContraints();
		c.gridx = 0;
		c.gridwidth = 1;
		enableChannels = new JCheckBox[hydrophoneMap.length];
		for (int i=0; i<hydrophoneMap.length; i++){
			enableChannels[i]=new JCheckBox();
			enableChannels[i].setSelected(true);
			c.gridx = 0;
			addComponent(v, new JLabel("Channel: "+hydrophoneMap[i]), c);
			c.gridx++;
			addComponent(v,enableChannels[i], c);
			c.gridx++;
			c.gridy++;
		}
		
		channelOptions.add(v);
		
		mainPanel.add(BorderLayout.CENTER, tabs);
		
		setDialogComponent(mainPanel);
		setLocation(location);

	}
	
	class PrimaryHydrophoneCheckBox implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			primaryHydrophone.setEnabled(usePrimeHydrophone.isSelected());
		}
		
	}
	
	class HiResSymbolsListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (hiResplotSymbol.isSelected()){
				showWarning("Memory Intensive Option", "Selecting high resolution plot symbols is memory intensive and requires a powerful graphics card. Systems which do not meet minimum criterea are likely to crash.");
			}
		}
		
	}
		
	public static StaticLocaliserParams showDialog(Window frame, Point pt, StaticLocalisationMainPanel staticlocalisationDialog, StaticLocaliserParams staticLocaliserParams ){
		if (singleInstance == null || singleInstance.getOwner() != frame) {
			singleInstance = new OptionsDialog(frame, pt, staticlocalisationDialog);
		}
		singleInstance.hydrophoneMap=PamUtils.getChannelArray(staticlocalisationDialog.getStaticLocaliserControl().getCurrentDatablock().getChannelMap());
		singleInstance.singleLocaliserParams = staticLocaliserParams.clone();
		singleInstance.setParams();
		singleInstance.setVisible(true);
		return singleInstance.singleLocaliserParams;
	}
	
	/**
	 * Check to see if a channel has been excluded from locaisation and if so don't select the check box. 
	 */
	private void setSelectedChannels(){
		boolean checked=true;
		int[] channels=singleLocaliserParams.channels;
		for (int i=0; i<enableChannels.length; i++){
			checked=false;
			if (channels!=null){
				for (int j=0; j<channels.length; j++){
					if (hydrophoneMap[i]==channels[j]) checked=true;
				}
			}
			enableChannels[i].setSelected(checked);
		}
	}
	
	
	public void setParams(){
		//batch run
		firstOnly.setSelected(singleLocaliserParams.firstOnly);
		minTD.setText(String.format("%d",singleLocaliserParams.minNumberofTDs));
		usePrimeHydrophone.setSelected(singleLocaliserParams.primaryHydrophoneSel);
		primaryHydrophone.setText(String.format("%d",singleLocaliserParams.primaryHydrophone));
		primaryHydrophone.setEnabled(singleLocaliserParams.primaryHydrophoneSel);
		//general
		showLowestChi.setSelected(singleLocaliserParams.showOnlyLowestChiValueDelay);
		maxPos.setText(String.format("%d",singleLocaliserParams.maximumNumberofPossibilities));
		//map
		hiResplotSymbol.setSelected(singleLocaliserParams.useHiResSymbols);
		//channels
		setSelectedChannels();
	}
	
	@Override
	public boolean getParams() {
		
		//Batch run
		hydrophoneChannelList=staticlocalisationDialog.getStaticLocaliserControl().getHydrophoneList();
		singleLocaliserParams.firstOnly = firstOnly.isSelected();
		try {
			singleLocaliserParams.minNumberofTDs = Integer.valueOf(minTD.getText());
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid integer value");
		}
		
		singleLocaliserParams.primaryHydrophoneSel = usePrimeHydrophone.isSelected();
		try {
			int hydrophone=Integer.valueOf(primaryHydrophone.getText());
			boolean hydrophoneOK=false;
			String hydrophoneString=" ";
			for (int i=0; i<hydrophoneChannelList.length; i++){
				if (hydrophoneChannelList[i]==hydrophone) hydrophoneOK=true;
				hydrophoneString=(hydrophoneString+hydrophoneChannelList[i]+" ");
			}
			if (hydrophoneOK==true){
			singleLocaliserParams.primaryHydrophone = Integer.valueOf(primaryHydrophone.getText());
			}
			else{
				return showWarning("Primary hydrophone must be hydrophones"+hydrophoneString);
				}
		}
		catch (NumberFormatException e) {
			return showWarning("Primary hydrophone must be an Integer value");
		}
		
		//General
		singleLocaliserParams.showOnlyLowestChiValueDelay=showLowestChi.isSelected();
		try {
		singleLocaliserParams.maximumNumberofPossibilities=Integer.valueOf(maxPos.getText());
		}
		catch (NumberFormatException e) {
			return showWarning("Maximum possibilities must be an integer value");
		}

		
		//Channels
		//figure out which channels are not checked and hence will be excluded
		ArrayList<Integer> channels=new ArrayList<Integer>();
		for (int i=0; i<enableChannels.length; i++){
			if (enableChannels[i].isSelected()){
				channels.add(hydrophoneChannelList[i]);
				
			}
		}
		
		int[] excludeChannelsArray=null;
		if (channels.size()!=0){
			excludeChannelsArray=new int[channels.size()];
			for (int j=0; j<channels.size(); j++){
				excludeChannelsArray[j]=channels.get(j);
			}
		}
	
		singleLocaliserParams.channels=excludeChannelsArray;
		
		
		//map
		singleLocaliserParams.useHiResSymbols=hiResplotSymbol.isSelected();
		
		return true;
	}



	@Override
	public void cancelButtonPressed() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}
}
