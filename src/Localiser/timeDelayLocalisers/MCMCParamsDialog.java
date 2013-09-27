package Localiser.timeDelayLocalisers;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import PamView.PamDialog;
import PamView.PamGridBagContraints;
import PamView.SourcePanel;


public class MCMCParamsDialog extends PamDialog{
	


	private static MCMCParamsDialog singleInstance;
	
	private MCMCParams 	mCMCParameters;
	
	private SourcePanel sourcePanel;

	private JTextField Error;
	private JTextField jumpSize;
	private JTextField numberOfJumps;
	private JTextField numberOfChains;
	private JTextField ChainStartDispersion;
	private JCheckBox cylindricalCoOrdinates;
	
	private JRadioButton percentageButton;
	private JTextField percentage;
	
	private JRadioButton kMeansClustering;
	private JTextField nClusters; 
	private JTextField minDistance;

	private JRadioButton noClustering; 


	
	public MCMCParamsDialog(Window parentFrame) {
		
		
		super(parentFrame, "MCMC Settings", false);

	
		//mainPanel.add(BorderLayout.NORTH, sourcePanel.getPanel());
		//JPanel southPanel = new JPanel(new GridBagLayout());
		//southPanel.setBorder(new TitledBorder("Measurement"));
		GridBagConstraints c = new PamGridBagContraints();
		
		JPanel errors=new JPanel(new GridBagLayout());
		errors.setBorder(new TitledBorder("Errors"));
		
		PamDialog.addComponent(errors, new JLabel("Time Error ", SwingConstants.RIGHT), c);
		c.gridx++;
		PamDialog.addComponent(errors, Error = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(errors, new JLabel(" (samples)", SwingConstants.LEFT), c);
		c.gridy++;
		
		JPanel markovChain=new JPanel(new GridBagLayout());
		markovChain.setBorder(new TitledBorder("Markov Chain"));
		
		
		c.gridx=c.gridx-2;
		PamDialog.addComponent(markovChain, new JLabel("Max Jump Size ", SwingConstants.RIGHT), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, jumpSize = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, new JLabel(" (meters) ", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx=c.gridx-2;
		PamDialog.addComponent(markovChain, new JLabel(" Number of Jumps ", SwingConstants.RIGHT), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, numberOfJumps  = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, new JLabel(" (int)", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx=c.gridx-2;
		PamDialog.addComponent(markovChain, new JLabel("Number of Chains ", SwingConstants.RIGHT), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, numberOfChains  = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, new JLabel(" (int) ", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx=c.gridx-2;
		PamDialog.addComponent(markovChain, new JLabel(" Chain Start Dispersion", SwingConstants.RIGHT), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, ChainStartDispersion  = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, new JLabel(" (meters) ", SwingConstants.LEFT), c);
		c.gridy++;
		c.gridx=c.gridx-2;
		PamDialog.addComponent(markovChain, new JLabel("", SwingConstants.RIGHT), c);
		c.gridx++;
		PamDialog.addComponent(markovChain, cylindricalCoOrdinates  = new JCheckBox("Cylindrical Co-Ordinates"),c);
		c.gridx++;
		
		JPanel markovChainDiagnosis=new JPanel(new GridBagLayout());
		markovChainDiagnosis.setBorder(new TitledBorder("Chain Convergence"));
		
		percentageButton = new JRadioButton("Remove Fixed Percentage");
		percentageButton.addActionListener(new PercentageButtonSelect());
		
		ButtonGroup group = new ButtonGroup();
		group.add(percentageButton);
		
		
		c.gridx=c.gridx-2;
		PamDialog.addComponent(markovChainDiagnosis, percentageButton,c);
		c.gridx++;
		PamDialog.addComponent(markovChainDiagnosis,  percentage = new JTextField(6), c);
		c.gridx++;
		PamDialog.addComponent(markovChainDiagnosis,  new JLabel("%"), c);
		
		
		JPanel clustering=new JPanel(new GridBagLayout());
		clustering.setBorder(new TitledBorder("Chain Clustering"));
		
		noClustering = new JRadioButton("none: ");
		noClustering.addActionListener(new kMeansButtonSelect());
		
		kMeansClustering = new JRadioButton("k-means: ");
		kMeansClustering.addActionListener(new kMeansButtonSelect());
		
		ButtonGroup clusterGroup = new ButtonGroup();
		clusterGroup.add(kMeansClustering);
		clusterGroup.add(noClustering);
		
		
		c.gridx=c.gridx-2;
		
		PamDialog.addComponent(clustering,noClustering ,c);
		c.gridy++;
		PamDialog.addComponent(clustering,kMeansClustering ,c);
		c.gridx++;
		PamDialog.addComponent(clustering,  new JLabel("No. clusters"), c);
		c.gridx++;
		PamDialog.addComponent(clustering,  nClusters = new JTextField(4), c);
		c.gridx++;
		PamDialog.addComponent(clustering,  new JLabel(" (int) "), c);
		c.gridy++;
		c.gridx=c.gridx-2; 
		PamDialog.addComponent(clustering,  new JLabel("Max cluster Size"), c);
		c.gridx++;
		PamDialog.addComponent(clustering,  minDistance = new JTextField(4), c);
		c.gridx++;
		PamDialog.addComponent(clustering,  new JLabel(" (meteres) "), c);
		
		
		
		GridBagConstraints cMain = new PamGridBagContraints();
		JPanel mainPanel = new JPanel(new GridBagLayout());
		PamDialog.addComponent(mainPanel,  errors, cMain);
		cMain.gridy++;
		cMain.gridy++;
		PamDialog.addComponent(mainPanel,  markovChain, cMain);
		cMain.gridy++;
		cMain.gridy++;
		PamDialog.addComponent(mainPanel,  markovChainDiagnosis, cMain);
		cMain.gridy++;
		cMain.gridy++;
		PamDialog.addComponent(mainPanel,  clustering, cMain);

		
		setDialogComponent(mainPanel);
	}
	
	class PercentageButtonSelect implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			percentage.setEnabled(percentageButton.isSelected());	
		}
	}
	
	class kMeansButtonSelect implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			kMeanSelected(kMeansClustering.isSelected());
		}
	}
	
	public void kMeanSelected(boolean selected){
		nClusters.setEnabled(selected);	
		minDistance.setEnabled(selected);
	}
	
	
	public static MCMCParams showDialog(Frame frame, MCMCParams mcmcParams) {
		if (singleInstance == null || singleInstance.getOwner() != frame) {
			singleInstance = new MCMCParamsDialog(frame);
		}
		singleInstance.mCMCParameters = mcmcParams;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		
		return singleInstance.mCMCParameters;
	}

	private void setParams() {
		
		
		MCMCParams params =mCMCParameters;
		
		Error.setText(String.format("%3.2f", params.timeError));
		
		jumpSize.setText(String.format("%3.2f", params.jumpSize));
		
		numberOfJumps.setText(params.numberOfJumps.toString());
		
		numberOfChains.setText(params.numberOfChains.toString());
		
		ChainStartDispersion.setText(String.format("%3.2f",params.ChainStartDispersion));
		
		cylindricalCoOrdinates.setSelected(params.cylindricalCoOrdinates);
		
		//chain analysis 
		if (params.chainAnalysis==MCMCParams.IGNORE_PERCENTAGE) percentageButton.setSelected(true);
		else percentageButton.setSelected(false);
		
		percentage.setText(String.format("%3.1f", params.percentageToIgnore));
		
		//cluster analysis 
		if (params.clusterAnalysis==MCMCParams.NONE) noClustering.setSelected(true);
		if (params.clusterAnalysis==MCMCParams.K_MEANS) kMeansClustering.setSelected(true);
		kMeanSelected(kMeansClustering.isSelected());
	
		
		nClusters.setText(params.nKMeans.toString());
		minDistance.setText(String.format("%3.2f",params.maxClusterSize));

	}

	@Override
	public boolean getParams() {
		
		try {
			//Error
			double newError = Double.valueOf(Error.getText());
			//Markov Chain
			System.out.println("newnumberOfJumps: "+newError);
			double newJumpsize = Double.valueOf(jumpSize.getText());
			int newnumberOfJumps = Integer.valueOf(numberOfJumps.getText());
			System.out.println("newnumberOfJumps: "+newnumberOfJumps);
			int newnumberOfChains = Integer.valueOf(numberOfChains.getText());
			System.out.println("newnumberOfChains: "+newnumberOfChains);
			double newChainStartDispersion = Double.valueOf(ChainStartDispersion.getText());
			boolean newCylindricalCoOrdinates=cylindricalCoOrdinates.isSelected();
			//Diagnosis	
			double newPercentage = Double.valueOf(percentage.getText());
			//Cluster analysis
			int numClusters=Integer.valueOf(nClusters.getText());
			double minClustereDist=Double.valueOf(minDistance.getText());
			
		
			//errors
			mCMCParameters.timeError = newError;
			System.out.println("New Error"+mCMCParameters.timeError);
			//chain properties
			mCMCParameters.jumpSize= newJumpsize;
			mCMCParameters.numberOfJumps=newnumberOfJumps;
			mCMCParameters.numberOfChains=newnumberOfChains;
			mCMCParameters.ChainStartDispersion=newChainStartDispersion;
			mCMCParameters.cylindricalCoOrdinates=newCylindricalCoOrdinates;
			// chain analysis
			if (percentageButton.isSelected()){
				mCMCParameters.chainAnalysis=mCMCParameters.IGNORE_PERCENTAGE;
			}
			mCMCParameters.percentageToIgnore=newPercentage;
			
			//clustering
			if (kMeansClustering.isSelected()) mCMCParameters.clusterAnalysis=mCMCParameters.K_MEANS;
			if (noClustering.isSelected()) mCMCParameters.clusterAnalysis=mCMCParameters.NONE;
			
			mCMCParameters.nKMeans= numClusters;
			mCMCParameters.maxClusterSize=minClustereDist;
		}
		
		catch (NumberFormatException e) {
			return PamDialog.showWarning(null, "MCMC Parameters", "Invalid value");
		}
		
		
		
	
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		mCMCParameters = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}



}
