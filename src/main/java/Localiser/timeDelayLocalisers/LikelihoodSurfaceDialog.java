package Localiser.timeDelayLocalisers;

import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamView.ColourArray;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.ColourArray.ColourArrayType;
import clickDetector.ConcatenatedSpectParams;
import clickDetector.ConcatenatedSpectrogram;
import clickDetector.ConcatenatedSpectrogramdialog;


public class LikelihoodSurfaceDialog extends PamDialog{
	
	
	private JPanel mainPanel;
	private static LikelihoodSurfaceDialog singleInstance;
	private LikelihoodSurfaceDialog likilihoodSurface;
	private LikelihoodSurfaceParams likilihoodSurfaceParams;
	private JComboBox colourList;
	private JTextField maxChi2;
	private JTextField gridMeshSize;
	private JTextField gridRange;
	

	
	private LikelihoodSurfaceDialog(Window parentFrame, Point location) {
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
		r.setBorder(new TitledBorder("Surface Dimensions"));
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy++;
		addComponent(r, new JLabel("<html>Max X<sup>2</sup> value</html> "), c);
		c.gridx++;
		addComponent(r, maxChi2 = new JTextField(6), c);
		c.gridx++;
		addComponent(r, new JLabel(" "), c);
		c.gridy++;
		c.gridx = 0;
		addComponent(r, new JLabel("Grid Range"), c);
		c.gridx++;
		addComponent(r, gridRange = new JTextField(3), c);
		c.gridx++;
		addComponent(r, new JLabel(" m"), c);
		c.gridy++;
		c.gridx = 0;
		addComponent(r, new JLabel("Grid Mesh Size"), c);
		c.gridx++;
		addComponent(r, gridMeshSize = new JTextField(3), c);
		c.gridx++;
		addComponent(r, new JLabel(" m"), c);
		c.gridy++;
		c.gridx = 0;
		
		mainPanel.add(r);
		mainPanel.add(p);

		
		setDialogComponent(mainPanel);
		setLocation(location);
		
	}
	
	public static LikelihoodSurfaceParams showDialog(Window frame, Point pt,LikelihoodSurfaceParams likilihoodSurfaceParams){
		
			if (singleInstance == null || singleInstance.getOwner() != frame) {
				singleInstance = new LikelihoodSurfaceDialog(frame, pt);
			}
			singleInstance.likilihoodSurfaceParams = likilihoodSurfaceParams.clone();
			singleInstance.setParams();
			singleInstance.setVisible(true);
			return singleInstance.likilihoodSurfaceParams;
	}
	
	public void setParams() {
		maxChi2.setText(String.format("%3.1f",likilihoodSurfaceParams.maxChiValue));
		gridRange.setText(String.format("%3.1f",likilihoodSurfaceParams.gridRange));
		gridRange.setText(String.format("%3.1f",likilihoodSurfaceParams.gridRange));
		gridMeshSize.setText(String.format("%3.1f",likilihoodSurfaceParams.gridMeshSize));
		colourList.setSelectedIndex(likilihoodSurfaceParams.colourMap.ordinal());
	}
	

	private class ColourSet implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			likilihoodSurfaceParams.setColourMap(ColourArrayType.values()[colourList.getSelectedIndex()]);
		}
	}

	@Override
	public boolean getParams() {

		try {
			likilihoodSurfaceParams.maxChiValue = Float.valueOf(maxChi2.getText());
			likilihoodSurfaceParams.gridRange = Float.valueOf(gridRange.getText());
			likilihoodSurfaceParams.gridMeshSize = Float.valueOf(gridMeshSize.getText());
			likilihoodSurfaceParams.setColourMap(ColourArrayType.values()[colourList.getSelectedIndex()]);
		}
		catch (NumberFormatException e) {
			return showWarning("Invalid range value");
		}
		
		if ((likilihoodSurfaceParams.gridRange/likilihoodSurfaceParams.gridMeshSize)>200) {
			 showWarning("You have selected a very fine grid size. This will likely take a long time to load");
			
		}
		
		if (likilihoodSurfaceParams.gridMeshSize == 0 || likilihoodSurfaceParams.gridRange==0) {
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



