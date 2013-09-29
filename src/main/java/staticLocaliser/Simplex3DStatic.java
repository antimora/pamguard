package staticLocaliser;

import java.awt.Color;
import java.awt.Frame;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransformGroup;
import javax.swing.JPanel;
import javax.vecmath.Point3f;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;

import staticLocaliser.MarkovChainStatic.MCMCPanelStatic;
import staticLocaliser.panels.AbstractLocaliserControl;
import staticLocaliser.panels.LocShape3D;
import staticLocaliser.panels.StaticDialogComponent;
import staticLocaliser.panels.StaticLocalisationMainPanel;
import Array.ArrayManager;
import Localiser.timeDelayLocalisers.LikelihoodSurface;
import Localiser.timeDelayLocalisers.MCMC;
import Localiser.timeDelayLocalisers.Simplex;
import PamDetection.PamDetection;
import PamView.PamSymbol;

/**
 * Simplex3D for the static localiser. Includes a 'LocalisationInformation' panel which allows the visualisation the chi2 surface around the localisation point in three different dimensions. 
 * @author Jamie Macaulay
 *
 */
public class Simplex3DStatic extends AbstractStaticLocaliserAlgorithm implements StaticLocalisationAlgorithmModel{

	private StaticLocalise staticLocaliser;
	protected Simplex simplex;
	private SimplexSurface simplexSurface;
	private PamSymbol pamSymbol;

	public Simplex3DStatic(StaticLocalise staticLocalise) {
		super(staticLocalise);
		this. staticLocaliser=staticLocalise;
		this. simplex=new Simplex();
		//this.simplexSurface=new SimplexSurface();
	}

	@Override
	public String getName() {
		return "Simplex";
	}

	@Override
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasParameters() {
		return false;
	}

	@Override
	public boolean parametersDialog() {
		return false;
	}

	@Override
	public ArrayList<StaticLocalisationResults> runModel() {
				
		calcDetectionMatchTDs(staticLocaliser.getPamDetection(), staticLocaliser.getDataBlock(),  staticLocaliser.getDetectionType());
		ArrayList<ArrayList<Double>> timeDelays=new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> timeDelayErrors=new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Point3f>> hydrophonePos=new ArrayList<ArrayList<Point3f>>();
		
	
		Integer tdSel=staticLocaliser.getTDSel();
		if (tdSel==null) tdSel=getTimeDelays().size()-1;
	
		hydrophonePos=getHydrophonePos();
		timeDelays=getTimeDelays().get(tdSel);
		timeDelayErrors=getTimeDelayErrors().get(tdSel);
		
		simplex.setTimeDelays(timeDelays);
		simplex.setHydrophonePos(hydrophonePos);
		simplex.setSampleRate(staticLocaliser.getPamDetection().getParentDataBlock().getSampleRate());
		simplex.setTimeDelaysErrors(timeDelayErrors);
		simplex.setSoundSpeed(ArrayManager.getArrayManager().getCurrentArray().getSpeedOfSound());
		
		simplex.runAlgorithm();
				
		ArrayList<StaticLocalisationResults> results = new ArrayList<StaticLocalisationResults>();
		StaticLocalisationResults stResults=new StaticLocalisationResults(staticLocaliser.getPamDetection().getTimeMilliseconds(), this);
		
		stResults.setX(simplex.getLocation()[0]);
		stResults.setY(simplex.getLocation()[1]);
		stResults.setZ(simplex.getLocation()[2]);
		stResults.setRange(Math.sqrt(Math.pow(simplex.getLocation()[0],2)+Math.pow(simplex.getLocation()[1],2)+Math.pow(simplex.getLocation()[2],2)));
		stResults.setXError(simplex.getLocationErrors()[0]);
		stResults.setYError(simplex.getLocationErrors()[1]);
		stResults.setZError(simplex.getLocationErrors()[2]);
		stResults.setRangeError(Math.sqrt(Math.pow(simplex.getLocationErrors()[0],2)+Math.pow(simplex.getLocationErrors()[1],2)+Math.pow(simplex.getLocationErrors()[2],2)));
		stResults.setChi2(simplex.getChi2());
		stResults.setTimeDelay(tdSel);
		stResults.setNTimeDelayPossibilities(getTimeDelays().size());
		stResults.setRunTimeMillis((long) simplex.getRunTime());
		
		results.add(stResults);
		
	
		
		return results;
	}

	@Override
	public PamSymbol getPlotSymbol(int iResult) {
		if (pamSymbol == null) {
			pamSymbol = new PamSymbol(PamSymbol.SYMBOL_CIRCLE, 9, 9, true, Color.PINK, Color.PINK);
		}
		return pamSymbol;
	}
	
	@Override
	public StaticDialogComponent getDisplayPanel(){
		
		
		if (simplexSurface==null){
			Frame frame =staticLocaliser.getStaticLocaliserControl().getPamView().getGuiFrame();
			this.simplexSurface=new SimplexSurface(frame, this);
		}
		return simplexSurface;
	}
	
	class SimplexSurface extends LikelihoodSurface implements StaticDialogComponent{
		

		Integer tdSel=null;
		private Simplex3DStatic simplexAlgorithm;
		private double[] currentLocation;

		SimplexSurface(Frame frame, Simplex3DStatic simplexAlgorithm) {
			super(frame);
			this.simplexAlgorithm=simplexAlgorithm;
		}

		@Override
		public void setCurrentDetection(PamDetection pamDetection) {

		}
		
		@Override
		public void update(int flag) {
			switch(flag){
			case(StaticLocaliserControl.RUN_ALGORITHMS):
				removeGraphData();
				tdSel=null;
				break;
			case(StaticLocaliserControl.SEL_DETECTION_CHANGED):
				removeGraphData();
				tdSel=null;
				break;
			case(StaticLocaliserControl.TD_SEL_CHANGED):
				setTDSel(staticLocaliser.getTDSel());
				break;
			case (StaticLocaliserControl.SEL_DETECTION_CHANGED_BATCH):
				removeGraphData();
			break;
			}			
		}
		
		/**
		 * Draw the surface for the selected graph;
		 * @param tdSel
		 */
		public void setTDSel(Integer tdSel){
			if (tdSel==this.tdSel) return;
			this.tdSel=tdSel;
			removeGraphData();
			resetPlot();
			
			if (staticLocaliser.getResults()==null) return;
			
			for (int i=0; i<staticLocaliser.getResults().size(); i++ ){
				
				//System.out.println("Info: "+ staticLocaliser.getResults().get(i).getX()+ " "+staticLocaliser.getResults().get(i).getY()+" "+staticLocaliser.getResults().get(i).getZ()+" "+staticLocaliser.getResults().get(i).getAlgorithm().getName());
				//System.out.println("results size: "+staticLocaliser.getResults().size());
				
				if (staticLocaliser.getResults().get(i).getTimeDelay()==tdSel && staticLocaliser.getResults().get(i).getAlgorithm().getName() == simplexAlgorithm.getName()){
					System.out.println("Info: "+ staticLocaliser.getResults().get(i).getX()+ " "+staticLocaliser.getResults().get(i).getY()+" "+staticLocaliser.getResults().get(i).getZ()+" "+staticLocaliser.getResults().get(i).getAlgorithm().getName());
				try{
					 currentLocation=new double[3];
					 currentLocation[0]=staticLocaliser.getResults().get(i).getX();
					 currentLocation[1]=staticLocaliser.getResults().get(i).getY();
					 currentLocation[2]=staticLocaliser.getResults().get(i).getZ();
						//need to set the observed time delays for the right chi squared function to create surface. 
					 simplex.setTimeDelays(getTimeDelays().get(tdSel));
					 simplexSurface.calcLikilihoodSurface();
					}
				catch(Exception e){
					System.out.println("Error in creating chi surface for the static localiser simplex algorithm");
					e.printStackTrace();
				}
				}
			}
			
		}

		@Override
		public StaticLocalisationMainPanel getStaticMainPanel() {
			return staticLocaliser.getStaticLocaliserControl().getStaticMainPanel();
		}
		
		

		
		
		/**
		 * Creates a surface for the current selected simplex localisation. 
		 * @param location- the localisation point; 
		 * @param surfaceRange- the 
		 * @param rangeBin
		 * @throws FunctionEvaluationException 
		 */
		public void calcLikilihoodSurface() throws FunctionEvaluationException{
			createSurface(simplex.getChi2Function(), currentLocation);
			
		}

	}

}
