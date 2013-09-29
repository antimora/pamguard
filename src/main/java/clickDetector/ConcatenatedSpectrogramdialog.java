package clickDetector;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import fftManager.FFTLengthModel;
import PamGraph3D.spectrogram3D.Surface3D;
import PamGraph3D.spectrogram3D.Spectrogram3DPamGraph;
import PamView.ColourArray;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.ColourArray.ColourArrayType;

public class ConcatenatedSpectrogramdialog  extends PamDialog {
	
	private JPanel mainPanel;
	private static ConcatenatedSpectrogramdialog singleInstance;
	private ConcatenatedSpectrogram concatenatedSpectrogram;
	private ConcatenatedSpectParams concatenatedSpectParams;
	private JComboBox colourList;
	private JCheckBox logScale;
	private JTextField logRange;
	private JCheckBox normalise;
	

	
	private ConcatenatedSpectrogramdialog(Window parentFrame, Point location) {
		super(parentFrame, "Concatenated Spectrogram Options", false);
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JPanel p=new JPanel();
		p.setBorder(new TitledBorder("Set Colour Scale"));
		p.add( colourList = new JComboBox());
		ColourArrayType[] types = ColourArray.ColourArrayType.values();
		for (int i = 0; i < ColourArray.ColourArrayType.values().length; i++) {
			colourList.addItem(ColourArray.getName(types[i]));
		}
		colourList.addActionListener(new ColourSet());
	
		JPanel r = new JPanel(new GridBagLayout());
		PamGridBagContraints c = new PamGridBagContraints();
		r.setBorder(new TitledBorder("Scale"));
		c.gridwidth = 3;
		addComponent(r, logScale = new JCheckBox("Log scale"), c);
		logScale.addActionListener(new Log3DScale());
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy++;
		addComponent(r, new JLabel("Scale range "), c);
		c.gridx++;
		addComponent(r, logRange = new JTextField(3), c);
		c.gridx++;
		addComponent(r, new JLabel(" dB"), c);
		c.gridy++;
		
		JPanel q=new  JPanel(new GridBagLayout());
		c = new PamGridBagContraints();
		q.setBorder(new TitledBorder("Normalise"));
		c.gridwidth = 3;
		addComponent(q, normalise = new JCheckBox("Normalise all"), c);
		normalise.addActionListener(new Normalise());
		c.gridy++;
		
		mainPanel.add(r);
		mainPanel.add(p);
		mainPanel.add(q);

		
		
		setDialogComponent(mainPanel);
		setLocation(location);
		
	}
	
	public static ConcatenatedSpectParams showDialog(Window frame, Point pt,ConcatenatedSpectParams concatenatedSpectParams){
		
			if (singleInstance == null || singleInstance.getOwner() != frame) {
				singleInstance = new ConcatenatedSpectrogramdialog(frame, pt);
			}
			singleInstance.concatenatedSpectParams = concatenatedSpectParams.clone();
			singleInstance.setParams();
			singleInstance.setVisible(true);
			return singleInstance.concatenatedSpectParams;
	}
	
	public void setParams() {
		colourList.setSelectedIndex(concatenatedSpectParams.colourMap.ordinal());
		logScale.setSelected(concatenatedSpectParams.logVal);
		logRange.setText(String.format("%3.1f",concatenatedSpectParams.maxLogValS));
		logRange.setEnabled(logScale.isSelected());
	}
	
	private class Log3DScale implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			concatenatedSpectParams.logVal=logScale.isSelected();
			logRange.setEnabled(logScale.isSelected());
		}
	}
	
	private class Normalise implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			concatenatedSpectParams.normaliseAll=normalise.isSelected();
		}
	}
	
	private class ColourSet implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			concatenatedSpectParams.setColourMap(ColourArrayType.values()[colourList.getSelectedIndex()]);
		}
	}

	@Override
	public boolean getParams() {
		
		try {
			concatenatedSpectParams.maxLogValS = Double.valueOf(logRange.getText());
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid range value");
		}
		if (concatenatedSpectParams.maxLogValS == 0) {
			return showWarning("The Scale range must be greater than zero");
		}
		
		
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
