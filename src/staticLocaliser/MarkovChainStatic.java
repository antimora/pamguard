package staticLocaliser;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;

import staticLocaliser.panels.LocShape3D;
import staticLocaliser.panels.StaticDialogComponent;
import staticLocaliser.panels.StaticLocalisationMainPanel;



import Array.ArrayManager;
import Localiser.timeDelayLocalisers.MCMC;
import Localiser.timeDelayLocalisers.MCMCPanel;
import Localiser.timeDelayLocalisers.MCMCParams;
import Localiser.timeDelayLocalisers.MCMCParamsDialog;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamDetection.PamDetection;
import PamGraph3D.PamShapes3D;
import PamUtils.PamUtils;
import PamView.PamSymbol;
/**
 * An iteration of MCMC for the static localiser. Includes a panel which shows the chi2 values for each jumps. 
 * @author Jamie Macaulay
 *
 */
public class MarkovChainStatic extends AbstractStaticLocaliserAlgorithm implements PamSettings {
	
	private StaticLocalise staticLocaliser;
	MCMCParams mCMCParams=new MCMCParams();
	MCMC mCMC;
	PamSymbol pamSymbol;
	MCMCPanelStatic mCMCPanelStatic;
	
	
	/**
	 * Flag for locShape which identifies it as part of the burn in phase of the MCMC chain.
	 */
	public static final int BURN_IN=0x1;
	
	/**
	 * Flag for locShape which identifies it as part of the final probability distribution of the MCMC chain.
	 */
	public static final int PROB_DISTRIBUTION=0X2;
	
	/**
	 * Flag for simple locShape.
	 */
	public static final int SIMPLE_MCMC_LOCSHAPE=0x3;
	
	//jump compression for 3D
	int jumpCompression=25000;

	public MarkovChainStatic (StaticLocalise staticLocaliser){
		super(staticLocaliser);
		
		this.staticLocaliser=staticLocaliser;
		this. mCMC=new MCMC(mCMCParams);
		PamSettingManager.getInstance().registerSettings(this);
	}

	@Override
	public String getName() {
		return "MCMC ";
	}

	@Override
	public String getToolTipText() {
		return null;
	}


	@Override
	public boolean parametersDialog() {
		MCMCParamsDialog.showDialog(null, mCMC.getSettings());
		return true;
	}

	@Override
	public ArrayList<StaticLocalisationResults> runModel() {
		
		calcDetectionMatchTDs(staticLocaliser.getPamDetection(), staticLocaliser.getDataBlock(),  staticLocaliser.getDetectionType());
		
		ArrayList<ArrayList<Double>> timeDelays=new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> timeDelayErrors=new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Point3f>> hydrophonePos=new ArrayList<ArrayList<Point3f>>();
		

		
		hydrophonePos=getHydrophonePos();
		
		Integer tdSel=staticLocaliser.getTDSel();
		if (tdSel==null) tdSel=getTimeDelays().size()-1;
		timeDelays=getTimeDelays().get(tdSel);
		timeDelayErrors=getTimeDelayErrors().get(tdSel);
		
		System.out.println("time delays: "+timeDelays);
		System.out.println("time delay Errors: "+ timeDelayErrors);
		System.out.println("hydrophonePos: "+ hydrophonePos);


		//set mcmc localiser input variables.
		mCMC.setTimeDelays(timeDelays);
		mCMC.setHydrophonePos(hydrophonePos);
		mCMC.setSampleRate(staticLocaliser.getPamDetection().getParentDataBlock().getSampleRate());
		mCMC.setTimeDelaysErrors(timeDelayErrors);
		mCMC.setSoundSpeed(ArrayManager.getArrayManager().getCurrentArray().getSpeedOfSound());
		
		//System.out.println("MCMC Time Delays: "+timeDelays);
		
		//run algorithm
		long timeStart=System.currentTimeMillis();
		mCMC.runAlgorithm();
		long timeEnd=System.currentTimeMillis();
		
//		ArrayList<Double> results=mCMC.getResults();
//		StaticLocalisationResults stResults=new StaticLocalisationResults(staticLocaliser.getPamDetection().getTimeMilliseconds(), this);
//		stResults.setX(results.get(0));
//		stResults.setY(results.get(1));
//		stResults.setZ(results.get(2));
//		stResults.setRange(results.get(3));
//		stResults.setXError(results.get(4));
//		stResults.setYError(results.get(5));
//		stResults.setZError(results.get(6));
//		stResults.setRangeError(results.get(7));
//		if (staticLocaliser.getStaticLocaliserControl().getParams().useHiResSymbols==true) 	stResults.setJumps(mCMC.getJumps());
//		stResults.setChi2(results.get(8));
//		stResults.setTimeDelay(tdSel);
//		stResults.setNTimeDelayPossibilities(getTimeDelays().size());
//		stResults.setRunTimeMillis(timeEnd-timeStart);
//		
//		ArrayList<StaticLocalisationResults> resultsMCMC=new ArrayList<StaticLocalisationResults>();
//		resultsMCMC.add(stResults);
//		
//		System.out.println("Chi value Jump Size: "+mCMC.getChiJumps().get(0).size());
//		
//		//update the display panel
//		
//		for (int i=0; i<mCMC.getChiJumps().size(); i++){
//		mCMCPanelStatic.addChiValuestoGraph(mCMC.getChiJumps().get(i),stResults);
//		}
//		
//		System.out.println("return MCMC");
		return null;
		
	}
	
	@Override
	/**
	 * Creates a specific MCMC loc symbol. This is series of lines following each MCMC jump, with the burn in phase coloured cyan and the rest of the chain coloured a random colour of blue. 
	 */
	public TransformGroup getPlotSymbol3D(StaticLocalisationResults staticLocalisationResults) {
		
		//if not using hi resolution option the return a simple symbol
		if (staticLocaliser.getStaticLocaliserControl().getParams().useHiResSymbols){
			return getProbDistributionSymb( staticLocalisationResults);
		}
		
		else{
			return getSimpleSymbol( staticLocalisationResults);
		}
		
	
	}
	
	/**
	 * Returns a cross showing the errors of the localisation
	 * @param staticLocalisationResults
	 * @return
	 */
	public TransformGroup getSimpleSymbol(StaticLocalisationResults staticLocalisationResults){
		return null;
//		//dump the data in the jump array 
//		staticLocalisationResults.setJumps(null);
//		
//		Vector3f vector=new Vector3f( staticLocalisationResults.getX().floatValue(), staticLocalisationResults.getY().floatValue(),staticLocalisationResults.getZ().floatValue());
//		
//		Appearance lineApp=PamShapes3D.lineAppearance(2f, false, PamShapes3D.blue);
//		Shape3D cross=PamShapes3D.create3DCross(staticLocalisationResults.getXError().floatValue(),staticLocalisationResults.getYError().floatValue(),staticLocalisationResults.getZError().floatValue(),lineApp);
//		LocShape3D locshape3d=new LocShape3D(cross,staticLocalisationResults, SIMPLE_MCMC_LOCSHAPE);
//		
//		TransformGroup tg = new TransformGroup();
//		tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
//		tg.setCapability( BranchGroup.ALLOW_DETACH );
//		
//		Transform3D transform = new Transform3D();
//		
//		try{
//		transform.setTranslation(vector);	
//		tg.addChild(locshape3d);
//		tg.setTransform(transform);
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//	
//		return tg;
	}
	
	/**
	 * returns a symbol shwoing the true probability distribution of the MCMC algorithm. Requires a good graphics card.  
	 * @param staticLocalisationResults
	 * @return
	 */
	public TransformGroup getProbDistributionSymb(StaticLocalisationResults staticLocalisationResults){
		
		LocShape3D shape3dtail;
		LocShape3D shape3d;
		TransformGroup tg = new TransformGroup();
		tg.setCapability( BranchGroup.ALLOW_DETACH );
		tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		
		ArrayList<ArrayList<Point3f>> jumps=staticLocalisationResults.getJumps();
		if (jumps==null){
			return tg;
		}
		
		jumps=MCMC.compressMCMCResults(jumps, jumpCompression);
		
		for (int k=0; k<jumps.size();k++){

			ArrayList<Point3f> Jumps3f= new ArrayList<Point3f>();
			
			//create tail
			if (mCMC.getSettings().percentageToIgnore!=0){
				Point3f data3f;
				for (int l=0; l<(int) ((mCMC.getSettings().percentageToIgnore/100.0)*jumps.get(k).size()); l++){
					data3f=jumps.get(k).get(l);
					Jumps3f.add(data3f);	
				}
			}
			
			if (mCMC.getSettings().cylindricalCoOrdinates==true){
				shape3dtail=new LocShape3D(PamShapes3D.pointArray3D(Jumps3f,getBurnInAppearance()),staticLocalisationResults, BURN_IN);
			}
			else{
				shape3dtail=new LocShape3D(PamShapes3D.linePolygon3D(Jumps3f,getBurnInAppearance()),staticLocalisationResults, BURN_IN);
			}
			
			//create prob distribution
			Jumps3f= new ArrayList<Point3f>();
			
			for (int l=(int) ((mCMC.getSettings().percentageToIgnore/100.0)*jumps.get(k).size()); l<jumps.get(k).size(); l++){
				Point3f Data3f=jumps.get(k).get(l);
				Jumps3f.add(Data3f);	
			}
	
			
			if (mCMC.getSettings().cylindricalCoOrdinates==true){
			shape3d=new LocShape3D(PamShapes3D.pointArray3D(Jumps3f,getNormalAppearance()),staticLocalisationResults, PROB_DISTRIBUTION);
			}
			else{
			shape3d=new LocShape3D(PamShapes3D.linePolygon3D(Jumps3f,getNormalAppearance()),staticLocalisationResults, PROB_DISTRIBUTION);
			}
			
			
			tg.addChild(shape3dtail);
			tg.addChild(shape3d);
			
			//now delete the jumps to save memory
			staticLocalisationResults.setJumps(null);
			
		}
		
		return tg;
	}
	

	
	@Override
	public void setNormalAppearance(LocShape3D locShape){
		if (locShape.getFlag()==PROB_DISTRIBUTION){
		locShape.setAppearance(getNormalAppearance());
		}
		if(locShape.getFlag()==BURN_IN){
		locShape.setAppearance(getBurnInAppearance());	
		}
		if(locShape.getFlag()==SIMPLE_MCMC_LOCSHAPE){
		locShape.setAppearance(PamShapes3D.lineAppearance(2f, false, PamShapes3D.blue));	
		}
	}
	
	@Override 
	public void setHighlightAppearance(LocShape3D locShape){
		if (locShape.getFlag()==PROB_DISTRIBUTION || locShape.getFlag()==BURN_IN){
		locShape.setAppearance(getHighlightAppearance());
		}
		if(locShape.getFlag()==SIMPLE_MCMC_LOCSHAPE){
		locShape.setAppearance(PamShapes3D.lineAppearance(2f, false, PamShapes3D.red));	
		}
	}
	
	@Override
	public Appearance getNormalAppearance(){
		Color3f blueC=PamShapes3D.randomBlue();
		return PamShapes3D.lineAppearance(1, false, blueC);
		
	}
	
	public Appearance getBurnInAppearance(){
			return PamShapes3D.lineAppearance(1, false,new Color3f(0f,0.9f,0.9f));
		
	}
	
	


	public Appearance getHighlightAppearance(){
		return PamShapes3D.lineAppearance(1, false, PamShapes3D.red);
	}

	@Override
	public PamSymbol getPlotSymbol(int iResult) {
			if (pamSymbol == null) {
				pamSymbol = new PamSymbol(PamSymbol.SYMBOL_TRIANGLED, 9, 9, true, Color.CYAN, Color.blue);
			}
			return pamSymbol;
		}	

	

	@Override
	public boolean hasParameters() {
		return true;
	}


	@Override
	public StaticDialogComponent getDisplayPanel() {
		if (mCMCPanelStatic==null){
			Frame frame =staticLocaliser.getStaticLocaliserControl().getPamView().getGuiFrame();
			this.mCMCPanelStatic=new MCMCPanelStatic(frame);
		}
		return mCMCPanelStatic;
	}
	
	
	/**
	 * This class extends the basic MCMC chi result panel and adds in pick functionality to highlight sapes etc. 
	 * @author Jamie Macaulay
	 *
	 */
	class MCMCPanelStatic extends MCMCPanel implements StaticDialogComponent{
		
		private Integer currentDelay=null;
		
		public MCMCPanelStatic(Frame frame){
			super(frame);
			super.getPickCanvas().getCanvas().addMouseListener(new MousePick(super.getPickCanvas()));
		}

		@Override
		public JPanel getPanel() {
			return super.getPanel();
		}
		
		private class MousePick extends MouseAdapter{
			
			PickCanvas pickCanvas;
			
			public MousePick(PickCanvas pickCanvas){
				this.pickCanvas=pickCanvas;
			}
			
			public void mouseClicked(MouseEvent e){
				LocShape3D shape3D;
				pickCanvas.setShapeLocation(e.getX(),e.getY());
				PickResult result = pickCanvas.pickClosest();
				
				//return all shapes to blue
				if (result==null){
					if (e.getButton()==MouseEvent.BUTTON3 || e.getButton()==MouseEvent.BUTTON2){
					}
					
					System.out.println("null pick result");
					for (int i=0; i<pickGroup.numChildren();i++){
						 shape3D=(LocShape3D) ((BranchGroup) pickGroup.getChild(i)).getChild(0);
						if (shape3D.getStaticLocResult().getTimeDelay()==currentDelay){
							 shape3D.setAppearance(PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.randomBlue()));
						}
					}
					currentDelay=null;
					staticLocaliser.settdSel(currentDelay);
					staticLocaliser.getStaticLocaliserControl().getStaticMainPanel().getDialogMap3D().update(StaticLocaliserControl.TD_SEL_CHANGED);
					staticLocaliser.getStaticLocaliserControl().getStaticMainPanel().getLocalisationVisualisation().update(StaticLocaliserControl.TD_SEL_CHANGED);
					return;
				}
				
				// if a shape is highlight, return the previous shapes to blue and the highlighted shapes to red. 
				LocShape3D s = (LocShape3D)result.getNode(PickResult.SHAPE3D);
				System.out.println("MCMCPanel result: "+s);
				for (int i=0; i<pickGroup.numChildren();i++){
					 shape3D=(LocShape3D) ((BranchGroup) pickGroup.getChild(i)).getChild(0);
					if (shape3D.getStaticLocResult().getTimeDelay()==currentDelay){
						 shape3D.setAppearance(PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.randomBlue()));
					}
					if (s==shape3D ||  shape3D.getStaticLocResult().getTimeDelay()==s.getStaticLocResult().getTimeDelay()){
						 shape3D.setAppearance(PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.red));
					}
					
				}

				currentDelay=s.getStaticLocResult().getTimeDelay();
				staticLocaliser.settdSel(currentDelay);
				staticLocaliser.getStaticLocaliserControl().getStaticMainPanel().getDialogMap3D().update(StaticLocaliserControl.TD_SEL_CHANGED);
				staticLocaliser.getStaticLocaliserControl().getStaticMainPanel().getLocalisationVisualisation().update(StaticLocaliserControl.TD_SEL_CHANGED);
				}
		}
		
		public void addChiValuestoGraph(ArrayList<Double> chiList, StaticLocalisationResults result){
			Point3f point;
//			ArrayList<Point3f> displayPoints=new ArrayList<Point3f>();
//			
//			for (int i=0; i<chiList.size(); i=i+compressVal){
//				//maxNoJumps
//				point=new Point3f((float) ((j3DGraphSize*i/ maxNoJumps)-j3DGraphSize/2),(float)((j3DGraphSize*chiList.get(i).floatValue()/maxChiValue)-j3DGraphSize/2),0f);
//				displayPoints.add(point);
//			}
//			
//			BranchGroup bg=new BranchGroup();
//			bg.setCapability(BranchGroup.ALLOW_DETACH);
//			bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
//			bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//			
//			LocShape3D chiLine=new LocShape3D( PamShapes3D.linePolygon3D(displayPoints, PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.randomBlue())),result);
//			chiLine.setCapability(Shape3D.ENABLE_PICK_REPORTING);
//
//			bg.addChild(chiLine);
//
//			pickGroup.addChild(bg);
		}

		@Override
		public void setCurrentDetection(PamDetection pamDetection) {
			// TODO Auto-generated method stub
		}
		
		public void setTDSel(Integer tdSel){
			LocShape3D shape3D;
			for (int i=0; i<pickGroup.numChildren();i++){
				 shape3D=(LocShape3D) ((BranchGroup) pickGroup.getChild(i)).getChild(0);
				if (shape3D.getStaticLocResult().getTimeDelay()==currentDelay){
					 shape3D.setAppearance(PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.randomBlue()));
				}
				if ( shape3D.getStaticLocResult().getTimeDelay()==tdSel){
					 shape3D.setAppearance(PamShapes3D.lineAppearance(0.2f, false, PamShapes3D.red));
				}
			}
			currentDelay=tdSel;
		}

		@Override
		public void update(int flag) {
			switch(flag){
			case(StaticLocaliserControl.RUN_ALGORITHMS):
				removeAllGraphData();
			break;
			case(StaticLocaliserControl.SEL_DETECTION_CHANGED):
				removeAllGraphData();
			break;
			case(StaticLocaliserControl.TD_SEL_CHANGED):
				setTDSel(staticLocaliser.getTDSel());
			break;
			case (StaticLocaliserControl.SEL_DETECTION_CHANGED_BATCH):
				removeAllGraphData();
			break;
			}
		}

		@Override
		public StaticLocalisationMainPanel getStaticMainPanel() {
			return staticLocaliser.getStaticLocaliserControl().getStaticMainPanel();
		}
		
		
		
	}
	
	@Override
	public String getUnitName() {
		return staticLocaliser.getStaticLocaliserControl().getUnitName();
	}

	@Override
	public String getUnitType() {
		return "MCMC Parameters";
	}

	@Override
	public Serializable getSettingsReference() {
		return mCMCParams;
	}

	@Override
	public long getSettingsVersion() {
		return  MCMCParams.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		mCMCParams= ((MCMCParams) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}


}