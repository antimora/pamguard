package staticLocaliser.panels;

import java.awt.*;
import PamView.*;

import javax.swing.JLayer;
import javax.swing.JPanel;

import staticLocaliser.StaticLocalise;
import staticLocaliser.StaticLocaliserControl;

import PamguardMVC.PamDataBlock;

/**
 * The basic control panel for changing the pamdetection on the control panel.
 * @author Jamie Macaulay
 *
 */
public abstract class AbstractLocaliserControl implements StaticDialogComponent , LocaliserControlModel{
	
	PamDataBlock pamDataBlock;
	JLayer<JPanel> jlayer;
	PamWaitAnimation layerUI;
	
	public String getLocaliserPanelName(){
		return "";
	}
	
	/**
	 * This return the panel with a JLayer overlay. This overlay is used for disabling the panel during batch run. Note that this is a new feature of Java 7. 
	 * @return
	 */
	public JLayer<JPanel> getLayerPanel(){
		
		layerUI = new PamWaitAnimation();
		jlayer = new JLayer<JPanel>(getPanel(), layerUI);

		return jlayer;
		
	}
	
	/**
	 * Enable or disable the panel. Panel enabled shows the normal panel with all components enabled. Disabling the panel adds an animation over the top of the panel, greys out the rest of the panel and disables all components. 
	 * @param enable
	 */
	public void setlayerPanelEnabled(boolean enable){
		
//		if (enable==true){
//			((PamWaitAnimation) layerUI).stop();
//		}
//		if (enable==false){
//			if (!layerUI.isRunning()) ((PamWaitAnimation) layerUI).start();
//		}
		
		enableComponents(enable, getPanel());

	}
	
	/**
	 * Enable or disable ALL components within the main JPanel. Be careful using this if you have individual components disabled or enabled.
	 * @param enable
	 * @param component
	 */
	public static void enableComponents(boolean enable, Component component){
		Component[] com = ((Container) component).getComponents();  
		if (com.length==0) return;
		for (int a = 0; a < com.length; a++) {
			enableComponents(enable, com[a]);
		    com[a].setEnabled(enable);  
		}  
	}
	
	
	/**
	 * Passes update flag to other dialog components. Use this to update other panels when, for example, a new detection has been selected. 
	 */
	public void notifyDetectionUpdate(){
		
		if (getStaticMainPanel()!=null){
			
			//cancel running thread if the user moves to a new detection. Note during batch run this should  not  be called.
			if (getStaticMainPanel().getStaticLocaliserControl().getStaticLocaliser().getCurrentRunConfig()!=StaticLocalise.RUN_BATCH){
			getStaticMainPanel().getStaticLocaliserControl().getStaticLocaliser().cancelThread();
			}
			
			
			//When moving to a new detection set the selected time delay possibility to null
			getStaticMainPanel().getStaticLocaliserControl().getStaticLocaliser().settdSel(null);
						
			if (getStaticMainPanel().getLocalisationVisualisation()!=null){
				getStaticMainPanel().getLocalisationVisualisation().update(StaticLocaliserControl.SEL_DETECTION_CHANGED);
			}
			if (getStaticMainPanel().getDialogMap3D()!=null){
				getStaticMainPanel().getDialogMap3D().update(StaticLocaliserControl.SEL_DETECTION_CHANGED);
			}
			if (getStaticMainPanel().getLocalisationInformation()!=null){
				getStaticMainPanel().getLocalisationInformation().update(StaticLocaliserControl.SEL_DETECTION_CHANGED);
			}
			if (getStaticMainPanel().getResultsPanel()!=null){
				getStaticMainPanel().getResultsPanel().update(StaticLocaliserControl.SEL_DETECTION_CHANGED);
			}
		}
		
	}
	

}
