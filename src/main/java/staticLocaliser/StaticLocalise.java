package staticLocaliser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.media.j3d.TransformGroup;
import javax.swing.JMenuItem;
import javax.swing.SwingWorker;

import staticLocaliser.panels.StaticLocalisationMainPanel;
import targetMotion.EventLocalisationProgress;



import Localiser.bearingLocaliser.AbstractLocaliser;
import PamController.PamControlledUnit;
import PamDetection.PamDetection;
import PamGraph3D.PamShapes3D;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * This class controls the localisations for the static localiser. There are three options:
 * <p>
 * RUN
 * <p>
 * This will localise with all selected algorithms just one of the possible time delays- either the one which is selected or the one which corresponds to the first detections on each channel in time. 
 * <p>
 * RUN ALL
 * <p>
 * This localises all potential time delays for the selected detection using all selected algorithms. The algorithm and time delay combo with the lowest chi value is selected as being the default correct localisation. 
 * <p>
 * BATCH RUN
 * <p>
 * This will localise all detections which are taken from the control panel by the 'getALLCurrentDetections()' function. Note this is not the same as all detection in the datablock. For example, for the offline_click_events datablock only those events which have been highlighted in the table will be localised
 * by pressing the batch run button. 
 *  @author Jamie Macaulay
 *
 */
public class StaticLocalise {
	
	
	private ArrayList<PamDataBlock> pamDataBlock;
	private PamDetection pamDetection;
	private Integer detectionType=null;
	private Integer tdSel=null;
	private int NtimeDelays=0;
	public EventLocaliserWorker eventLocaliserWorker;
	
	private int bestResult=0;
	
	public final static int RUN=0x1;
	public final static int RUN_ALL=0x4;
	public final static int RUN_BATCH=0x8;
	public int currentRunConfig=-1;
	
	
	private StaticLocaliserControl staticLocaliserControl;
	protected ArrayList<AbstractStaticLocaliserAlgorithm> algorithms=new ArrayList<AbstractStaticLocaliserAlgorithm>();
	protected ArrayList<StaticLocalisationResults> results;
	protected PamDataUnit pamDataUnit;
	
	/**
	 * This constructor determines which algorithms are included. If creating a new localisation algorithm  simply add it to the algorithms ArrayList<AbstractStaticLocaliserAlgorithm>;
	 *<<<<<<<<<<<<<<<<<<Add new localisation algorithms here>>>>>>>>>>>>>>>>>>>
	 * @param staticLocaliserControl
	 */
	public StaticLocalise(StaticLocaliserControl staticLocaliserControl) {
		this.staticLocaliserControl=staticLocaliserControl;
		algorithms.add(new MarkovChainStatic(this));
		algorithms.add(new Simplex3DStatic(this));
	}
	
	/**
	 * Iterates through every selected algorithm and calls the runModel() function. Once the algorithm has calculated a result, the algorithm specific plot symbol is be added to the 3d map.Note that the results are added to the 'results' arrayList in the algorithm class. This is to allow algorithms which have degenerate results to add as many StaicLodcalisationResults as
	 * necessary.
	 */
	public void runAlgorithms(){
		
		TransformGroup symbol;
		ArrayList<StaticLocalisationResults> algorithmResults;
		results=new ArrayList<StaticLocalisationResults>();
		
		if (pamDetection==null){
			System.out.println("PamDetection is  null");
			return;
		}
		
		for (int i=0; i<algorithms.size(); i++){
			if (algorithms.get(i).isSelected()){
				if (algorithms.get(i).isSelected()){
					algorithmResults=algorithms.get(i).runModel();
					results.addAll(algorithmResults);
					for (int j=0; j<algorithmResults.size();j++){
						symbol=algorithms.get(i).getPlotSymbol3D(results.get(results.size()-1));
						//if the thread has been cancelled, either by the stop button or moving to a new detection then don't bother adding to the map.
						if (eventLocaliserWorker.isCancelled()==false){
							staticLocaliserControl.getStaticMainPanel().getDialogMap3D().setLocalisationSymbol(symbol);
						}	
					}
				}
			}
		}
		//Hint to the java vm that we should do a garbage collection
		System.gc();
		
	}
	
	/**
	 * Creates a 1D  ArrayList of the results to display in the results panel.
	 * @param resultsAll
	 */
	private void displayableResults(ArrayList<ArrayList<StaticLocalisationResults>> resultsAll){
		
		Double lowestChi=Double.MAX_VALUE;

			results=new ArrayList<StaticLocalisationResults>();
			//display all possible results for all time delays
			for (int i=0; i<resultsAll.size();i++ ){
				for (int j=0; j<resultsAll.get(i).size();j++ ){
					results.add(resultsAll.get(i).get(j));
					if (resultsAll.get(i).get(j).getChi2()<lowestChi ){
						lowestChi=resultsAll.get(i).get(j).getChi2();
						bestResult=results.size()-1;
					}
				}
			}
			
	}
	
	
	/**
	 * Creates a separate thread and runs the localisation algorithms. 
	 * @param runConfig
	 */
	public void localise(int runConfig) {
		eventLocaliserWorker = new EventLocaliserWorker(runConfig);
		this.currentRunConfig=runConfig;
		eventLocaliserWorker.execute();
	}
	
	
	public EventLocaliserWorker getEventLocaliserWorker(){
		return eventLocaliserWorker;	
	}
	
	public void cancelThread(){
		System.out.println("thread cancelled");
		
		if (eventLocaliserWorker!=null) {
			eventLocaliserWorker.cancel(true);	
		}
	}
	
	public class EventLocaliserWorker extends SwingWorker<Integer, EventLocalisationProgress> {
		int runConfig=0;
		
		protected EventLocaliserWorker(int runConfig){
			this.runConfig=runConfig;
		}
		
		@Override
		protected Integer doInBackground() throws Exception {
			

			StaticLocalisationMainPanel staticDialog=staticLocaliserControl.getStaticMainPanel();
			detectionType=staticDialog.getCurrentControlPanel().getDetectionType();
			pamDataBlock=staticLocaliserControl.getDataBlocksAll().get(staticDialog.getDataBlockList().getSelectedIndex());

			Double lowestChi=Double.MAX_VALUE;
			ArrayList<ArrayList<StaticLocalisationResults>> resultsAll;

			switch (runConfig){
			
			case (RUN):
				
				System.out.println("Run");
				
				pamDetection=staticDialog.getCurrentControlPanel().getCurrentDetection();
				
//				if (pamDetection.getDetectionMatch(detectionType).getNPossibilities(staticLocaliserControl.getParams().channels)>=staticLocaliserControl.getParams().maximumNumberofPossibilities) return null;
				
				staticDialog.setLocaliserControlEnabled(false);
				
				NtimeDelays=staticDialog.getLocalisationVisualisation().getNTimeDelays();
				
				if ( tdSel==null) tdSel=0;

				resultsAll=runAll(tdSel, 1);
				
				//don't add results if the thread has been cancelled.
				if (this.isCancelled()==true) break;
					
				displayableResults(resultsAll);
				
				//update the results table 
				staticDialog.getResultsPanel().update(0);
				
				staticDialog.setLocaliserControlEnabled(true);


			break;
			
			case (RUN_ALL):
						
				System.out.println("Run All");

				pamDetection=staticDialog.getCurrentControlPanel().getCurrentDetection();
				
//				if (pamDetection.getDetectionMatch(detectionType).getNPossibilities(staticLocaliserControl.getParams().channels)>=staticLocaliserControl.getParams().maximumNumberofPossibilities) return null;
				
				staticDialog.setLocaliserControlEnabled(false);

				NtimeDelays=staticDialog.getLocalisationVisualisation().getNTimeDelays();
				
				resultsAll=runAll(0, NtimeDelays);
				
				//don't add results if the thread has been cancelled.
				if (this.isCancelled()==true) break;
				
				displayableResults(resultsAll);
				
				//update the results panel.
				staticDialog.getResultsPanel().update(0);
				staticDialog.setLocaliserControlEnabled(true);
				
			break;
			
			case (RUN_BATCH):
				
				System.out.println("Batch Run");
				currentRunConfig=RUN_BATCH;
			
			
				ArrayList<PamDetection> detections=staticDialog.getCurrentControlPanel().getCurrentDetections();
				
					
					if (this.isCancelled()==true) break;
					
					//batch run does not get it's pamDection from a control panel therefore we must be carefull how we update the other panels. 
					staticLocaliserControl.getStaticMainPanel().getDialogMap3D().setCurrentDetection(pamDetection);
					staticLocaliserControl.getStaticMainPanel().getDialogMap3D().update(StaticLocaliserControl.SEL_DETECTION_CHANGED);
					staticLocaliserControl.getStaticMainPanel().getLocalisationVisualisation().setCurrentDetection(pamDetection);
					staticLocaliserControl.getStaticMainPanel().getLocalisationVisualisation().update(StaticLocaliserControl.SEL_DETECTION_CHANGED_BATCH);
					staticLocaliserControl.getStaticMainPanel().getLocalisationInformation().update(StaticLocaliserControl.SEL_DETECTION_CHANGED_BATCH);


					if (pamDetection==null) return null;
//					if (pamDetection.getDetectionMatch().getNPossibilities(staticLocaliserControl.getParams().channels)>=staticLocaliserControl.getParams().maximumNumberofPossibilities) return null;
//					if (pamDetection.getDetectionMatch().getNumberofNonNullDelays(staticLocaliserControl.getParams().channels)<staticLocaliserControl.getParams().minNumberofTDs) return null;

					//If primary hydrophone is selected check this detection is OK to use, if not continue to the next detection

					boolean detectionOK=true;
					if (staticLocaliserControl.getParams().primaryHydrophoneSel){
						detectionOK=false;
						int [] channels=PamUtils.getChannelArray(pamDetection.getChannelBitmap());
	
						for (int k=0; k<channels.length; k++){
							if (channels[k]==staticLocaliserControl.getParams().primaryHydrophone) {
								detectionOK=true;
							}	
						}
					}

					if (detectionOK==false)  return null;

					//check to see if we run all time delays or just the first. 
					NtimeDelays=staticDialog.getLocalisationVisualisation().getNTimeDelays();
					int tdsToRun=1;
					int tdStart=0;
					if (staticLocaliserControl.getParams().firstOnly!=true){ 
//						tdsToRun=pamDetection.getDetectionMatch(detectionType).getNPossibilities(staticLocaliserControl.getParams().channels); 
					}
						
//					int N=staticLocaliserControl.getStaticMainPanel().getDialogMap3D().getPickGroup().numChildren();
						
					resultsAll=runAll(tdStart, tdsToRun);
						
					//if the stop button or user has moved to a new algorithm then don't display results.
					if (this.isCancelled()==true) break;

//					//remove the previous localisation symbols without deleting the current ones
//					for (int n=0; n<N; n++){
//						staticLocaliserControl.getStaticMainPanel().getDialogMap3D().getPickGroup().removeChild(0);
//					}
						
					displayableResults(resultsAll);
					
					//update the results panel
					staticDialog.getResultsPanel().update(0);
					
					//now automatically save the results. If the user has selected display all then save all possible results. If the user has selected display only lowest chi values then save only the lowest chi values. 
					staticLocaliserControl.saveAll();
					

			break;	
			
			}
			
			staticDialog.setLocaliserControlEnabled(true);
			currentRunConfig=-1;
			return null;
			
		}
		
		/**
		 *Runs localisation algorithms for all the possible time delays from tdStart to tdStart+nDelays.If tdStart is null then tdStart is assumed to be 0; 
		 * @param tdStart- the possible time delay to start from. 
		 * @param nDelays- number of time delays after td start to localise. 
		 * @return
		 */
		private ArrayList<ArrayList<StaticLocalisationResults>> runAll(Integer tdStart, int nDelays){
			int NtimeDelays= tdStart+nDelays; 
			ArrayList<ArrayList<StaticLocalisationResults>> resultsAll=new ArrayList<ArrayList<StaticLocalisationResults>>();
			for (int i=tdStart; i<NtimeDelays; i++){
				System.out.println("i: "+i+" of "+	NtimeDelays);
				if (this.isCancelled()==true) break;
				tdSel=i;
				runAlgorithms();
				resultsAll.add(results);
			}
			return resultsAll;
		}
		
		 @Override
	       protected void done() {
	           try { 
	        	  System.out.println("Thread is done");
	        	  staticLocaliserControl.getStaticMainPanel().setLocaliserControlEnabled(true);
	        	  currentRunConfig=-1;
	           } catch (Exception e) {
	        	   currentRunConfig=-1;
	        	   e.printStackTrace();
	           }
	       }
	}
	
	/**
	 * The current run configuration. Null if no localisation process is running. Otherwise StaticLocalise.RUN,  StaticLocalise.RUN_ALL, StaticLocalise.BATCH_RUN.
	 * @return the current run configuration. 
	 */
	public int getCurrentRunConfig(){
		return this.currentRunConfig;
	}

	public ArrayList<AbstractStaticLocaliserAlgorithm> getAlgorithms(){
		return this.algorithms;
	}

	public ArrayList<PamDataBlock> getDataBlock() {
		return pamDataBlock;		
	}
	
	/**
	 * Sets the current PamDetection and also updates all panels. Use this for batch run. 
	 * @param pamDetection
	 */
	public void setPamDetection(PamDetection pamDetection){
		this.pamDetection=pamDetection;
	}
	
	public void setDetectionType(Integer detectionType){
		this.detectionType=detectionType;
	}
	
	public void settdSel(Integer tdSel){
		this.tdSel=tdSel;
	}
	

	public PamDetection getPamDetection() {
		return pamDetection;
	}

	public Integer getDetectionType() {
		return detectionType;
	}

	public Integer getTDSel() {
		return tdSel;
	}

	public void setPamDataBlock(ArrayList<PamDataBlock> pamDataBlock) {
		this.pamDataBlock=pamDataBlock;		
	}

	public ArrayList<StaticLocalisationResults> getResults() {
		return results;
	}

	public int getBestResultIndex() {
		return bestResult;
	}

	public void setBestResultIndex(int row) {
		bestResult=row;		
	}

	public StaticLocaliserControl getStaticLocaliserControl() {
		return staticLocaliserControl;
	}
	
	public ArrayList<StaticLocalisationResults> getResultsforTable(){
		
		int bestRow= getBestResultIndex();
		//if only selecting the best results and showing in table then need to find the best results and set table variables. 
		if (getStaticLocaliserControl().getParams().showOnlyLowestChiValueDelay==true && results!=null){
			if (results.size()==0) return null;
			//find the time delay with the lowest Chi2 value
			int n=0;
			ArrayList<StaticLocalisationResults> resultsBest=new ArrayList<StaticLocalisationResults> ();
			Integer tdSelBest=results.get(getBestResultIndex()).getTimeDelay();
			for (int i=0; i<results.size();i++ ){
				if (results.get(i).getTimeDelay()==tdSelBest) {
						resultsBest.add(results.get(i));
						if (i==getBestResultIndex()) bestRow=n; 
						n++;
				}
			}
				System.out.println("ResultsBest: "+resultsBest);
			return resultsBest;
		}
		return results;
	}
	
	public int getBestTableRow(){
		if (getStaticLocaliserControl().getParams().showOnlyLowestChiValueDelay==true && results!=null){
		int n=0;
		int bestRow=0;
		Integer tdSelBest=results.get(getBestResultIndex()).getTimeDelay();
		for (int i=0; i<results.size();i++ ){
			if (results.get(i).getTimeDelay()==tdSelBest) {
				if (i==getBestResultIndex()) {
					bestRow=n; 
					continue;
				}
				n++;
			}
			
		}	
		return bestRow;
		}
		return bestResult;
		
	}

}
