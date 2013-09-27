package staticLocaliser.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import staticLocaliser.StaticLocalise;

import PamDetection.PamDetection;
import PamView.PamPanel;

/**
 * Produces a tabbed pane of graphs specific to each localisation algorithm. Localisation algorithms can have the panel set to null, in which case no tab will be added. The panel can show anything, from simple statistical information, to a complex 3D graph.
 * @author Jamie Macaulay
 *
 */
public class LocalisationInformation  implements StaticDialogComponent{
	
	StaticLocalisationMainPanel staticlocaisationDialog;
	StaticLocalise staticLocaliser;
	
	PamPanel mainPanel;
	
	public LocalisationInformation(StaticLocalisationMainPanel staticlocaisationDialog){
		this.staticlocaisationDialog=staticlocaisationDialog;
		this.staticLocaliser=staticlocaisationDialog.getStaticLocaliserControl().getStaticLocaliser();
		
		mainPanel=new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Algorithm Display"));
		
		JTabbedPane tabs=new JTabbedPane();
		for (int i=0; i<staticLocaliser.getAlgorithms().size();i++){
			if (staticLocaliser.getAlgorithms().get(i).getDisplayPanel()==null) continue;
			tabs.addTab( staticLocaliser.getAlgorithms().get(i).getName(), null, staticLocaliser.getAlgorithms().get(i).getDisplayPanel().getPanel());	
		}
	
			
		mainPanel.add(BorderLayout.CENTER, tabs);
		mainPanel.setPreferredSize(new Dimension(100,100));
		
	}

	@Override
	public JPanel getPanel() {
		return mainPanel;
	}

	@Override
	public void setCurrentDetection(PamDetection pamDetection) {
		// TODO Auto-generated method stub
	}


	@Override
	public void update(int flag) {
		//update all the panels
		for (int i=0; i<staticLocaliser.getAlgorithms().size();i++){
			if (staticLocaliser.getAlgorithms().get(i).getDisplayPanel()==null) continue;
			staticLocaliser.getAlgorithms().get(i).getDisplayPanel().update(flag);	
		}		
	}

	@Override
	public StaticLocalisationMainPanel getStaticMainPanel() {
		return staticlocaisationDialog;
	}

}
