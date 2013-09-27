package staticLocaliser.panels;

import javax.swing.JPanel;

import PamDetection.PamDetection;
import PamView.PamPanel;

/**
 * This interface should be  implemented for most panels in the static localiser. (That includes algorithm specific panel);
 * @author Jamie Macaulay
 *
 */
public interface StaticDialogComponent {
	
		/**
		 * 
		 * @return the panel to show in the main panel. 
		 */
		JPanel getPanel();
		
		/**
		 * Set the current detection. Some dialog components will change depending on the type of PamDetection
		 * @param pamDetection. 
		 */
		void setCurrentDetection(PamDetection pamDetection);
		
		
		/**
		 * Update the dialog. This can be called any time from any other part of the static localisation module at any time. Use to interact other dialog components. Integer variable is a flag which indicates what kind of update to perform. Specific types of update are found in 'Static Localise'. These include runnig an algorithm, changing the selected pamDetection and changing the selected time delay. 
		 */
		void update(int flag);
		
		/**
		 * Get the static main panel that this component belongs to. 
		 * @return
		 */
		StaticLocalisationMainPanel getStaticMainPanel();
				
	}



