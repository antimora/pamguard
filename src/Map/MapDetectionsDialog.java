package Map;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;

public class MapDetectionsDialog extends PamDialog {

	private MapDetectionsManager mapDetectionsManager;
	
	private MapDetectionsParameters mapDetectionsParameters;
	
	JCheckBox[] plotCheckBox;
	
	JTextField[] showTimes;
	
	JButton[] defaults;
	
	JCheckBox[] allAvailable;
	
	private MapDetectionsDialog(Frame parentFrame, MapDetectionsManager mapDetectionsManager) {
		
		super(parentFrame, "Detection Overlay options", false);
		
		this.mapDetectionsManager = mapDetectionsManager;
		
		mapDetectionsParameters = mapDetectionsManager.getMapDetectionsParameters().clone();
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel dPanel = new JPanel();
		dPanel.setBorder(new TitledBorder("Data overlay options"));
		dPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		
		addComponent(dPanel, new JLabel("Data name"), c);
		c.gridx++;
		addComponent(dPanel, new JLabel(" Plot "), c);
		c.gridx++;
		addComponent(dPanel, new JLabel(" Time (s) "), c);
		c.gridx++;
		addComponent(dPanel, new JLabel("  "), c);
		c.gridx++;
		addComponent(dPanel, new JLabel(" All "), c);

		
		int n = mapDetectionsParameters.mapDetectionDatas.size();
		plotCheckBox = new JCheckBox[n];
		showTimes = new JTextField[n];
		defaults = new JButton[n];
		allAvailable = new JCheckBox[n];
		
		MapDetectionData md;
		for (int i = 0; i < n; i++) {
			md = mapDetectionsParameters.mapDetectionDatas.get(i);
			if (md.dataBlock == null) {
				continue;
			}
			c.gridx = 0;
			c.gridy ++;
			c.anchor = GridBagConstraints.EAST;
			addComponent(dPanel, new JLabel(md.dataName), c);
			c.gridx++;
			c.anchor = GridBagConstraints.CENTER;
			addComponent(dPanel, plotCheckBox[i] = new JCheckBox(""), c);
			plotCheckBox[i].setSelected(md.shouldPlot);
			plotCheckBox[i].addActionListener(new PlotEnabler(i));
			plotCheckBox[i].setToolTipText("Plot these data");
			c.gridx++;
			c.anchor = GridBagConstraints.CENTER;
			addComponent(dPanel, showTimes[i] = new JTextField(6), c);
			showTimes[i].setText(String.format("%d", md.displaySeconds));
			showTimes[i].setToolTipText("Enter the maximum time these data should display for");
			c.gridx++;
			addComponent(dPanel, defaults[i] = new JButton("default"), c);
			defaults[i].addActionListener(new DefaultAction(i));
			defaults[i].setToolTipText("Use the default map display time");
			c.gridx++;
			addComponent(dPanel, allAvailable[i] = new JCheckBox(""), c);
			allAvailable[i].setSelected(md.allAvailable);
			allAvailable[i].addActionListener(new AvailableEnabler(i));
			allAvailable[i].setToolTipText("Show all available data");
			
			
			enableRow(i);
		}
		
		mainPanel.add(BorderLayout.CENTER, dPanel);
		
		setDialogComponent(mainPanel);
	}
	
	public static MapDetectionsParameters showDialog(Frame parent, MapDetectionsManager mapDetectionsManager) {
		
		MapDetectionsDialog mapDetectionsDialog = new MapDetectionsDialog(parent, mapDetectionsManager);
		
		mapDetectionsDialog.setVisible(true); 
		
		return mapDetectionsDialog.mapDetectionsParameters;
	}

	@Override
	public void cancelButtonPressed() {

		mapDetectionsParameters = null;

	}

	@Override
	public boolean getParams() {
		
		int n = mapDetectionsParameters.mapDetectionDatas.size();
		
		MapDetectionData md;
		for (int i = 0; i < n; i++) {
			md = mapDetectionsParameters.mapDetectionDatas.get(i);
			if (md.dataBlock == null) {
				continue;
			}
			md.shouldPlot = plotCheckBox[i].isSelected();
			try {
				md.displaySeconds = Integer.valueOf(showTimes[i].getText());
			}
			catch (NumberFormatException ex) {
				return false;
			}
			md.allAvailable = allAvailable[i].isSelected();
		}
		return true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}
	
	class PlotEnabler implements ActionListener {
		int iOverlay;

		public PlotEnabler(int overlay) {
			super();
			iOverlay = overlay;
		}

		public void actionPerformed(ActionEvent e) {
			enableRow(iOverlay);
		}
	}
	
	private void enableRow(int iOverlay) {
		boolean e = plotCheckBox[iOverlay].isSelected();
		boolean e2 = allAvailable[iOverlay].isSelected();
		showTimes[iOverlay].setEnabled(e && e2 == false);
		defaults[iOverlay].setEnabled(e && e2 == false);
		allAvailable[iOverlay].setEnabled(e);
	}

	class DefaultAction implements ActionListener {
		int iOverlay;

		public DefaultAction(int overlay) {
			super();
			iOverlay = overlay;
		}

		public void actionPerformed(ActionEvent e) {
			
			showTimes[iOverlay].setText(String.format("%d", mapDetectionsManager.getDefaultTime()));
			
		}
		
	}

	class AvailableEnabler implements ActionListener {
		int iOverlay;

		public AvailableEnabler(int overlay) {
			super();
			iOverlay = overlay;
		}

		public void actionPerformed(ActionEvent e) {

			enableRow(iOverlay);
			
		}
		
	}
}
