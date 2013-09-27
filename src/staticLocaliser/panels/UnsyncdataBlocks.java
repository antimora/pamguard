package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;

import staticLocaliser.StaticLocaliserControl;

import clickDetector.ConcatenatedSpectrogramdialog;


import PamView.ColourArray;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.PamPanel;
import PamView.ColourArray.ColourArrayType;
import PamguardMVC.PamDataBlock;

/**
 * This is a simple dialog which allows the user to combine several datablocks to be used by the static localiser. The new datablocks appear in the datablock selection combo box. 
 * @author Jamie Macaulay
 *
 */
public class UnsyncdataBlocks  extends PamDialog{
	
	private JPanel mainPanel;
	private static UnsyncdataBlocks singleInstance;
	private StaticLocalisationMainPanel staticlocalisationDialog;
	private ArrayList<JComboBox> datBlockSelectionBoxes;
	JPanel comboBoxes;
	JScrollPane scrollPane;
	JTextField numberBox;
	 JTextField nameBox;
	
	private UnsyncdataBlocks(Window parentFrame, Point location, StaticLocalisationMainPanel staticlocalisationDialog) {
		super(parentFrame, "Create Unsynchronised DataBlock", false);
		
		
		super.getRootPane().setDefaultButton(null);
		
		this.staticlocalisationDialog=staticlocalisationDialog;
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Create Unsynchronised DataBlock"));
		
		JPanel r = new JPanel(new GridBagLayout());
		PamGridBagContraints c = new PamGridBagContraints();
		r.setBorder(new TitledBorder("Data Blocks"));
		c.gridx=0;
		c.gridwidth=3;
		addComponent(r, new JLabel("Unsynced DataBlock Name"), c);
		c.gridy++;
		c.gridx=0;
		addComponent(r, nameBox = new JTextField(3), c);
		c.gridy++;
		c.ipady=10;
		addComponent(r,  new JLabel(""), c);
		c.gridy++;
		c.ipady=8;
		c.gridx = 0;
		c.gridwidth = 1;
		addComponent(r, new JLabel("Select no. of data blocks"), c);
		c.gridx++;
		addComponent(r, numberBox = new JTextField(3), c);
		numberBox.addKeyListener(new ChangeNoDataBlocks());
		c.gridx++;
		addComponent(r, new JLabel("int"), c);
		c.gridy++;

		comboBoxes=comboBoxList(3);
		scrollPane=new JScrollPane(comboBoxes);
		mainPanel.add(BorderLayout.NORTH,r);
		mainPanel.add(BorderLayout.CENTER,scrollPane);
		setPreferredSize(new Dimension(300, 450));

		setDialogComponent(mainPanel);
		setLocation(location);
	
		
	}
	
	private void setMinimumSize(int i, int j) {
		// TODO Auto-generated method stub
		
	}

	class ChangeNoDataBlocks implements KeyListener{
		
		
		@Override
		public void keyPressed(KeyEvent arg0) {}
		@Override
		public void keyReleased(KeyEvent e) {}
		@Override
		public void keyTyped(KeyEvent e) {
			int  key = e.getKeyChar();
			if (key == KeyEvent.VK_ENTER ){
				try {
					int N = Integer.valueOf(numberBox.getText());
					mainPanel.remove(scrollPane);
					comboBoxes=comboBoxList(N);
					comboBoxes.validate();
					scrollPane=new JScrollPane(comboBoxes);
					mainPanel.add(scrollPane);
					mainPanel.validate();
					scrollPane.revalidate();

				}
				
				catch (NumberFormatException b) {
					 showWarning("Invalid integer value");
				}
				
			}
		}
	}
	
	
	public JPanel comboBoxList(int N){
		JPanel comboBoxes=new JPanel(new GridBagLayout());		
		PamGridBagContraints c = new PamGridBagContraints();

		comboBoxes.setBorder(new TitledBorder("Data Block Selection"));
		comboBoxes.removeAll();
		datBlockSelectionBoxes=new ArrayList<JComboBox>(N);
		
		for (int i=0; i<N; i++){
			c.ipady=10;
			JComboBox dataBlockComboBox=staticlocalisationDialog.createComboDataList(true);
			if (i==0) {
				addComponent(comboBoxes,new JLabel("Primary data block"),c);
			}
			else{
				addComponent(comboBoxes,new JLabel("Data Block: " + i),c);
			}
			c.gridy++;
			datBlockSelectionBoxes.add(dataBlockComboBox);
			addComponent(comboBoxes,dataBlockComboBox,c);
			c.ipady=10;
			c.gridy++;
		}	
		return comboBoxes;
	}
	
	
	public static void showDialog(Window frame, Point pt, StaticLocalisationMainPanel staticlocalisationDialog ){
		if (singleInstance == null || singleInstance.getOwner() != frame) {
			singleInstance = new UnsyncdataBlocks(frame, pt, staticlocalisationDialog);
		}
		singleInstance.setParams();
		singleInstance.setVisible(true);
	}
	
	public void setParams(){
		numberBox.setText(String.format("%d",datBlockSelectionBoxes.size()));
	}
	
	@Override
	public boolean getParams() {
		
		String unSyncDataBlockName=nameBox.getText();
		if (unSyncDataBlockName.isEmpty()) unSyncDataBlockName=("Unsync Data Block");
		
		PamDataBlock dataBlock;
		ArrayList<PamDataBlock> dataBlocks=new ArrayList<PamDataBlock>();
		for (int i=0; i<datBlockSelectionBoxes.size(); i++){
			dataBlock=staticlocalisationDialog.getStaticLocaliserControl().getDataBlocks().get(datBlockSelectionBoxes.get(i).getSelectedIndex()).get(0);
			dataBlocks.add(dataBlock);
		}
		
		staticlocalisationDialog.getStaticLocaliserControl().getDataBlocksUnsync().add(dataBlocks);
		staticlocalisationDialog.updateControlPanelList();
		staticlocalisationDialog.updateComboList();
		
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
