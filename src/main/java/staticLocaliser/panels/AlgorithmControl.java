package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import staticLocaliser.AbstractStaticLocaliserAlgorithm;
import staticLocaliser.StaticLocalise;

import PamDetection.PamDetection;
import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.PamPanel;

public class AlgorithmControl implements StaticDialogComponent {
	
	private StaticLocalise staticLocaliser;
	private StaticLocalisationMainPanel staticLocalisationDialog;
	JCheckBox[] enableModel;
	JButton[] modelParams;
	ArrayList<AbstractStaticLocaliserAlgorithm> algorithms;
	
	//Dialog Components
	private PamPanel mainPanel;

	public AlgorithmControl(StaticLocalise staticLocaliser, StaticLocalisationMainPanel staticLocalisationDialog){
		this.staticLocaliser=staticLocaliser;
		this.staticLocalisationDialog=staticLocalisationDialog;
		
		mainPanel=new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Algorithm Control"));
		
		
		AbstractStaticLocaliserAlgorithm algorithm;
		algorithms=staticLocaliser.getAlgorithms();
		int nModels = staticLocaliser.getAlgorithms().size();
		enableModel = new JCheckBox[nModels];
		modelParams = new JButton[nModels];

		
		PamPanel algorithmsCon = new PamPanel(new GridBagLayout());
		PamGridBagContraints c= new PamGridBagContraints();
		
		
		for (int i = 0; i < nModels; i++) {
			algorithm = algorithms.get(i);
			enableModel[i] = new JCheckBox(algorithm.getName());
			enableModel[i].addActionListener(new AlgorithmEnable(algorithm));
			modelParams[i] = new JButton("Settings...");
			modelParams[i].setVisible(algorithm.hasParameters());
			modelParams[i].addActionListener(new AlgorithmParams(algorithm));
			enableModel[i].setToolTipText(algorithm.getToolTipText());
			modelParams[i].setToolTipText(algorithm.getToolTipText());
			c.gridx = 0;
			PamDialog.addComponent(algorithmsCon, enableModel[i], c);
			c.gridx++;
			PamDialog.addComponent(algorithmsCon, modelParams[i], c);
			c.gridy++;
		}
		
		mainPanel.add(BorderLayout.NORTH,algorithmsCon);

	}
	
	
	class AlgorithmEnable implements ActionListener {
		AbstractStaticLocaliserAlgorithm model;

		public AlgorithmEnable(AbstractStaticLocaliserAlgorithm model) {
			super();
			this.model = model;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			enableControls();
		}
	}
	

	class AlgorithmParams implements ActionListener {
		AbstractStaticLocaliserAlgorithm model;

		public AlgorithmParams(AbstractStaticLocaliserAlgorithm model) {
			
			super();
			this.model = model;
		}
		@Override
		//settings panel
		public void actionPerformed(ActionEvent arg0) {
			model.parametersDialog();
		}
	}
	
	

	@Override
	public JPanel getPanel() {
		return mainPanel;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		// TODO Auto-generated method stub
		
	}

	public void enableControls() {
		ArrayList<AbstractStaticLocaliserAlgorithm> algorithms=staticLocaliser.getAlgorithms();
		for (int i=0; i<enableModel.length; i++){
				algorithms.get(i).setSelected(enableModel[i].isSelected());
		};		
	}

	@Override
	public void update(int flag) {
		// TODO Auto-generated method stub

	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		// TODO Auto-generated method stub
		return null;
	}

}
