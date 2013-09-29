package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import staticLocaliser.StaticLocaliserControl;

import PamDetection.PamDetection;

/**
 * Under construction- control panel for whistles and moans. 
 * @author Jamie Macaulay
 *
 */
public class WhistleLocaliserControl extends AbstractLocaliserControl implements LocaliserControlModel{

	public WhistleLocaliserControl(StaticLocaliserControl staticLocaliserControl){
		
	}
	
	@Override
	public JPanel getPanel() {
		JPanel mainPanel=new JPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Whistle Localiser Control"));
		return mainPanel;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		// TODO Auto-generated method stub
		
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

	@Override
	public PamDetection getCurrentDetection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<PamDetection> getCurrentDetections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getDetectionType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void refreshData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canLocalise(PamDetection pamDetection) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] getChannelMap() {
		// TODO Auto-generated method stub
		return null;
	}

}
