//package PamGraph3D.graphDisplay3D;
//
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.Dimension;
//import java.awt.GridBagLayout;
//import java.awt.Point;
//import java.awt.Window;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.KeyEvent;
//
//import javax.swing.BoxLayout;
//import javax.swing.ButtonGroup;
//import javax.swing.JCheckBox;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JRadioButton;
//import javax.swing.JTabbedPane;
//import javax.swing.JTextField;
//import javax.swing.border.TitledBorder;
//
//import staticLocaliser.StaticLocaliserParams;
//import staticLocaliser.dialog.OptionsDialog;
//import staticLocaliser.dialog.StaticLocalisationMainPanel;
//
//import PamView.PamDialog;
//import PamView.PamGridBagContraints;
//
//public class BTAxis3DOptionsDialog extends PamDialog{
//	
//	private JPanel mainPanel;
//	private BearingAmpAxis bearingAmplitudeAxis;
//	private static BTAxis3DOptionsDialog singleInstance;
//	private BTAxis3DParams bearingAmpParams;
//	private ButtonGroup group;
//	JRadioButton btButton;
//	JRadioButton amplitudeButton;
//	
//	JPanel amplitude;
//	JPanel bearing;
//	
//	private JTextField ampMinTextField;
//	private JTextField ampMaxTextField;
//	private JTextField bMinTextField;
//	private JTextField bMaxTextField;
//	private JCheckBox fullBearingCheckBox;
//
//	public BTAxis3DOptionsDialog(Window parentFrame, Point location, BTAxis3DParams params) {
//		super(parentFrame, "Bearing Amplitude Axis Settings" ,false);
//		
//		
//		mainPanel=new JPanel(new BorderLayout());
//		
//		PamGridBagContraints c = new PamGridBagContraints();
//		
//		btButton = new JRadioButton("Bearing");
//		btButton.addActionListener(new JRadioButtonSel());
//		if (params.mode==BTAxis3DParams.BEARING){
//			btButton.setSelected(true);
//		}
//		amplitudeButton = new JRadioButton("Amplitude");
//		amplitudeButton.addActionListener(new JRadioButtonSel());
//		if (params.mode==BTAxis3DParams.AMPLITUDE){
//			btButton.setSelected(true);
//		}
//		group = new ButtonGroup();
//		group.add(btButton);
//		group.add(amplitudeButton);
//
//		
//		
//	 bearing=new JPanel(new GridBagLayout());
//		bearing.setBorder(new TitledBorder("Bearing (degress)"));
//		c.gridwidth=5;
//		bearing.add(btButton,c);
//		c.gridy++;
//		c.gridx=0;
//		c.gridwidth=1;
//		bearing.add(new JLabel("Bearing Range"),c);
//		c.gridx++;
//		bMinTextField=new JTextField();
//		bMinTextField.setPreferredSize(new Dimension(40,20));
//		bearing.add(bMinTextField,c);
//		c.gridx++;
//		bearing.add(new JLabel(" to "),c);
//		c.gridx++;
//		bMaxTextField=new JTextField();
//		bMaxTextField.setPreferredSize(new Dimension(40,20));
//		bearing.add(bMaxTextField,c);
//		c.gridx++;
//		bearing.add(new JLabel("(degrees)"),c);
//		c.gridy++;
//		c.gridwidth=5;
//		c.gridx=0;
//		fullBearingCheckBox=new JCheckBox("Full -180-180 degree view");
//		fullBearingCheckBox.addActionListener(new FullBearingCheckBox());
//		bearing.add(fullBearingCheckBox,c);
//		
//		
//		amplitude=new JPanel(new GridBagLayout());
//		amplitude.setBorder(new TitledBorder("Amplitude (dB)"));
//		c.gridwidth=5;
//		amplitude.add(amplitudeButton,c);
//		c.gridy++;
//		c.gridx=0;
//		c.gridwidth=1;
//		amplitude.add(new JLabel("Amplitude Range "),c);
//		c.gridx++;
//		ampMinTextField=new JTextField();
//		ampMinTextField.setPreferredSize(new Dimension(40,20));
//		amplitude.add(ampMinTextField,c);
//		c.gridx++;
//		amplitude.add(new JLabel(" to "),c);
//		c.gridx++;
//		ampMaxTextField=new JTextField();
//		ampMaxTextField.setPreferredSize(new Dimension(40,20));
//		amplitude.add(ampMaxTextField,c);
//		c.gridx++;
//		amplitude.add(new JLabel("(dB)"),c);
//				
//		mainPanel.add(BorderLayout.NORTH,bearing);
//		mainPanel.add(BorderLayout.SOUTH,amplitude);
//		
//		setDialogComponent(mainPanel);
//		setLocation(location);
//		
//		enablePanels();
//	}
//	
//	class JRadioButtonSel implements ActionListener{
//
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			 enablePanels();
//		}
//		
//	}
//	
//	class FullBearingCheckBox implements ActionListener{
//
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			boolean enabled=!fullBearingCheckBox.isSelected();
//		
//			bMinTextField.setEnabled(enabled);
//			bMaxTextField.setEnabled(enabled);
//		}
//		
//	}
//	
//	public void enablePanels(){
//		if (amplitudeButton.isSelected()){
//			disableAllComponents(bearing);
//			enableAllComponents(amplitude);
//		}
//		if (btButton.isSelected()){
//			disableAllComponents(amplitude);
//			enableAllComponents(bearing);
//		}
//		
//	}
//	
//	public void disableAllComponents(JPanel panel){
//		Component[] com = panel.getComponents();  
//		for (int a = 0; a < com.length; a++) { 
//			if (com[a] instanceof JRadioButton){
//				
//			}
//			else{
//				com[a].setEnabled(false);  
//			}
//		}  
//	}
//	
//	public void enableAllComponents(JPanel panel){
//		Component[] com = panel.getComponents();  
//		for (int a = 0; a < com.length; a++) { 
//			if (com[a] instanceof JRadioButton){
//				
//			}
//			else{
//				com[a].setEnabled(true);  
//			}
//		}  
//		
//	}
//	
//	
//	
//	public static BTAxis3DParams showDialog(Window frame, Point pt, BTAxis3DParams params){
//		if (singleInstance == null || singleInstance.getOwner() != frame) {
//			singleInstance = new BTAxis3DOptionsDialog(frame, pt,  params);
//		}
//		
//		singleInstance.bearingAmpParams =params .clone();
//		singleInstance.setParams();
//		singleInstance.setVisible(true);
//		return singleInstance.bearingAmpParams;
//	}
//	
//	
//	private void setParams() {
//		  ampMinTextField.setText(String.format("%1$,.1f",bearingAmpParams.ampMin));
//		  ampMaxTextField.setText(String.format("%1$,.1f",bearingAmpParams.ampMax));
//		  bMinTextField.setText(String.format("%1$,.1f",bearingAmpParams.bearingMin));
//		  bMaxTextField.setText(String.format("%1$,.1f",bearingAmpParams.bearingMax));
//	}
//
//
//	@Override
//	public boolean getParams() {
//
//		try{
//			if(fullBearingCheckBox.isSelected()){
//				bearingAmpParams.bearingMin = -180.0;
//				bearingAmpParams.bearingMax = 180.0;
//			}
//			else{
//				bearingAmpParams.bearingMin = Double.valueOf(bMinTextField.getText());
//				bearingAmpParams.bearingMax = Double.valueOf(bMaxTextField.getText());
//			}
//			bearingAmpParams.ampMin = Double.valueOf(ampMinTextField.getText());
//			bearingAmpParams.ampMax = Double.valueOf(ampMaxTextField.getText());
//		}
//		
//		catch (Exception e){
//			return showWarning("Min/Max bearing and amplitude text fields must have a valid number.");
//		}
//		
//		if (amplitudeButton.isSelected()){
//			bearingAmpParams.mode=BTAxis3DParams.AMPLITUDE;
//			bearingAmpParams.vertMin=bearingAmpParams.ampMin;
//			bearingAmpParams.vertMax=bearingAmpParams.ampMax;
//		}
//		if (btButton.isSelected()){
//			bearingAmpParams.mode=BTAxis3DParams.BEARING;
//			bearingAmpParams.vertMin=Math.toRadians(bearingAmpParams.bearingMin);
//			bearingAmpParams.vertMax=Math.toRadians(bearingAmpParams.bearingMax);
//		}
//		
//		return true;
//	}
//
//	@Override
//	public void cancelButtonPressed() {		
//	}
//
//	@Override
//	public void restoreDefaultSettings() {
//		// TODO Auto-generated method stub
//		
//	}
//	
//
//}
