package staticLocaliser;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransformGroup;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import pamMaths.PamVector;

import staticLocaliser.panels.LocShape3D;
import staticLocaliser.panels.StaticDialogComponent;
import targetMotion.TargetMotionResult;
import PamDetection.PamDetection;
import PamView.PamSymbol;

/**
 * This is the basic interface that allows a localisation algorithm to integrate with the static localiser. This includes both the localisation algorithms and the 3D symbols which be displayed on the map. Use AbstractStaticLocaliserAlgorithm to fill in many of these functions. 
 * @author Jamie Macaulay
 *
 */
public interface StaticLocalisationAlgorithmModel {
	
	/**
	 * 
	 * @return the name of this algorithm 
	 */
	String getName();
	
	String getToolTipText();
	
	/**
	 * 
	 * @return whether the algorithm is selected
	 */
	boolean isSelected();
	
	/**
	 * Specifies if the algorithm has a settings dialoge
	 * @return true if the localiser has a settings dialog. 
	 */
	boolean hasParameters();
	
	/**
	 * Opens a settings dialog if the 'settings' button on the algorithm panel is selected. 
	 * @return
	 */
	boolean parametersDialog();
	
	/**
	 * Run model. This should implement  AbstractDetectionMatch class. 
	 * From the <b>StaticLocalise<b> class grab:
	 * <p>
	 * pamDetection- the current detection selected in the control panel.
	 * tdSel- the current selected time delay on the map, loclaisationVisualisation and maybe the localisationInformation panels.
	 * detectionType- this is an integer flag which is datablock specific and is passed to the AbstractDetectionMatch class-usually specifies which corresponding detections to calculate time delays from.
	 *For example, 'detectiontype' in clickDetectionMatch refers to events, classified species and a combination of the two. For whistles this may refer to whistle classification etc- tdSel refers to which time delay to select. Abstractdetectionmatch creates an array of possible time delays for a given detection. 
	 *
	 *Using these three bits of info use the detections subclass of AbstractDetectionMatch to calc time delays and loclaise. 
	 * @return an arraylist of StaticLocalisationResults. The reason for using an arraylist is multiple solutions maybe obtained. 
	 */
	ArrayList<StaticLocalisationResults> runModel();
	
	/**
	 * A panel which shows results, chi distributions etc. Can be null if no panel is required. 
	 * @return
	 */
	public StaticDialogComponent getDisplayPanel();
		
	/**
	 * Simple 2D symbol for the map and results table
	 * @param iResult
	 * @return
	 */
	public PamSymbol getPlotSymbol(int iResult);
	
	/**
	 * The 3D symbol which represents results on the DialogMap3DSL. Default is an ellipse which has dimensions corresponding to errors. 
	 * @param stat
	 * @return
	 */
	public TransformGroup getPlotSymbol3D(StaticLocalisationResults stat);
	
	/**
	 * The normal appearance of the 3D localisation symbol. 
	 * @return
	 */
	public Appearance getNormalAppearance();
	
	/**
	 * If the symbol is selected it changes appearance to this. 
	 * @return
	 */
	public Appearance getHighlightAppearance();

	/**
	 * Called whenever an algorithm is selected or deselected. 
	 * @param selected
	 */
	public void setSelected(boolean selected);
	
	/**
	 * Set the normal appearance of the locShape. Note usually this will just be using getNormalAppearance() functionality, however more complicated shapes may want to modify this function. 
	 * @param locShape
	 */
	public void setNormalAppearance(LocShape3D locShape);
	
	/**
	 * Set the highlighted appearance of a locShape.Note usually this will just be using getHighlightAppearance() functionality, however more complicated shapes may want to modify this function. 
	 * @param locShape
	 */
	public void setHighlightAppearance(LocShape3D locShape);
	
	
	
}
