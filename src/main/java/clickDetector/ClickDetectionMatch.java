package clickDetector;

import java.util.ArrayList;

import Localiser.Correlations;
import PamDetection.AbstractDetectionMatch;
import PamDetection.PamDetection;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;

public class ClickDetectionMatch extends AbstractDetectionMatch {

	/**
	 * Use only classified clicks to localise.
	 */
	static public final int USE_CLASSIFICATION =1;
	
	/**
	 * Use only clicks which are a part of the smae event as this click
	 */
	static public final int USE_EVENT = (1 << 1);
	
	/**
	 * Use all detected clicks (note, this will override all other flags) in the bitmap;
	 */
	static public final int USE_ALL = (1 << 2);
	
	/**
	 * Use only non echoe clicks
	 */
	static public final int NO_ECHO = (1 << 3);
	
	
	/**
	 * A bitmap of flags dictating what types of clicks to use. 
	 */
	Integer clickTypes=USE_ALL;
	
	//click information;
	ClickDetection clickDataUnit;
	
	ArrayList<ClickDetection> clicks;
	
	ArrayList<ClickDetection> clicksSorted;

	private Correlations correlations = new Correlations();

	private ArrayList<ArrayList<ClickDetectionLite>> clickDetectionsAll;
	
	//click settings
	private ClickControl clickControl;
	private int correlationLength;
	private double sampleRate;
	
	

	public ClickDetectionMatch(ClickDetection parentDetection) {
		
		super(parentDetection);
		// TODO Auto-generated constructor stub
		clickDataUnit = parentDetection;
		clickControl= clickDataUnit.getClickDetector().getClickControl();
		correlationLength = clickDataUnit.getCurrentSpectrumLength();
		sampleRate=clickDataUnit.getClickDetector().getSampleRate();
	}
	
	public ClickDetectionMatch(ClickDetection parentDetection, Integer clickTypes) {
		
		super(parentDetection, clickTypes);
		// TODO Auto-generated constructor stub
		clickDataUnit = parentDetection;
		this.clickTypes=clickTypes;
		super.detectionType=clickTypes;
		clickControl= clickDataUnit.getClickDetector().getClickControl();
		correlationLength = clickDataUnit.getCurrentSpectrumLength();
		sampleRate=clickDataUnit.getClickDetector().getSampleRate();
	}
	
	
	@Override
	public ArrayList<ArrayList<Double>> getGroupTimeDelays(){
		
		if (correlationLength<1) correlationLength=PamUtils.getMinFftLength(clickDataUnit.getWaveData()[0].length);
	
		ArrayList<ArrayList<Double>> timeDelaysAll=new ArrayList<ArrayList<Double>>();
		ArrayList<Double> timeDelaysA;
		
		ArrayList<PamDataUnit> clicksAbstract= getDetectionGroup();
		
		//need to cast to clickDetection
		clicks=new ArrayList<ClickDetection>();
		for (int i=0; i<clicksAbstract.size(); i++){
			clicks.add((ClickDetection) clicksAbstract.get(i));
		}
		
		//sort clicks
		clicksSorted=sortClicks(clicks);
		if (clicksSorted==null) return null;
		
		
		//Deconstruct grouping etc so we have lists of clicks on each channel. Better if people analyse without grouping but must be compatible with those who aren't familier with the inner workings of Pamguard
		//how to store the clicks....create a simple click class- all we need is the startSample and the waveform...although I'm sure there's more info which may come in useful;
		//This is a difficult section of code- watch out for bugs. 
		this.clickDetectionsAll= sortChannels(clicksSorted);
		//System.out.println("Click Group: "+clickDetectionsAll);

		//now we need to calculate all possible combinations;
		int[][] combinations=mapPossibleCombinations(clickDetectionsAll);
		
		//now calculate all the possible time delays
		Double[] timeDelays;
		ArrayList<ClickDetectionLite> clicks;
		
		for (int j=0; j<combinations.length; j++){
			
			timeDelaysA=new ArrayList<Double>();
			
			clicks=new ArrayList<ClickDetectionLite>();
			for (int i=0; i<clickDetectionsAll.size(); i++){
				
			if (clickDetectionsAll.get(i)==null || clickDetectionsAll.get(i).size()==0){
			clicks.add(null);
			}
			else{
			clicks.add(clickDetectionsAll.get(i).get(combinations[j][i]));
			}
			}
			
			timeDelays=calcTimeDelays(clicks);
			

			for (int k=0; k<timeDelays.length;k++){
				timeDelaysA.add(timeDelays[k]);
			}
			
			timeDelaysAll.add(timeDelaysA);
			
		}

		//return all possible time delays.
		return timeDelaysAll;
	}
	
	/**
	 * Create a map of all the possible combinations of clicks, leading to all the possible combinations of time delays. 
	 * @param clickDetectionsAll
	 * @return
	 */
	public  int[][] mapPossibleCombinations(ArrayList<ArrayList<ClickDetectionLite>> clickDetectionsAll){
		if (clickDetectionsAll==null) return null;
		
		int N=1;
		//calculate number of possibilities
		for (int i=0; i<clickDetectionsAll.size();i++){
			if (clickDetectionsAll.get(i).size()!=0){
			N=N*clickDetectionsAll.get(i).size();
			}
		}

		int[][] combinations=new int[N][clickDetectionsAll.size()];
		
		int nTemp=1;
		int fill=1;
		 for (int j=0; j<clickDetectionsAll.size();j++){
			 nTemp=1;
			 	//calc number of combinations for each element in current clickDetectionsAll vector;
				 for (int k=j+1; k<clickDetectionsAll.size();k++){
						if (clickDetectionsAll.get(k).size()!=0){
							nTemp=nTemp*clickDetectionsAll.get(k).size();
						}
				 }
				 //System.out.println("ntemp: "+nTemp);
				 
				int counter=0;
				for (int n=0; n<fill;n++){
				 for(int l=0;l<clickDetectionsAll.get(j).size();l++){
						 for (int m=0; m<nTemp; m++){
						 combinations[counter][j]=l;
						 counter++;
					}
				 } 
				}
				 if (clickDetectionsAll.get(j).size()!=0)  fill*=clickDetectionsAll.get(j).size();
		}

//		 for (int i=0; i<combinations.length; i++){
//			 System.out.println("");
//			 for (int j=0; j<combinations[i].length; j++){
//				 System.out.print(combinations[i][j]+ ",");
//			 }
//		 }	 System.out.println("");
		 
		return combinations;
	}
	
	
	/**
	 * 
	 * @param clicks
	 * @return
	 */
	public Double[] calcTimeDelays(ArrayList<ClickDetectionLite> clicks){
				
		fftManager.Complex[] complex1;
		fftManager.Complex[] complex2;
		ArrayList<Integer> indexM1=indexM1(clicks.size());
		ArrayList<Integer> indexM2=indexM2(clicks.size());
		
		Double[] delaySecs = new Double[indexM1.size()];
		double delaySamples;

			for (int i = 0; i < indexM1.size(); i++) {
				
				if (clicks.get(indexM1.get(i))==null || clicks.get(indexM2.get(i))==null){
					delaySecs[i] =null;
					continue;
				}
					
				 	complex1=ClickDetection.getComplexSpectrum(clicks.get(indexM2.get(i)).getClickWaveform(),correlationLength);
					complex2=ClickDetection.getComplexSpectrum(clicks.get(indexM1.get(i)).getClickWaveform(),correlationLength);

					delaySamples =correlations.getDelay(complex1, complex2, correlationLength);
	
					//add click start sample delay.
					double delayStartSample=(double) clicks.get(indexM2.get(i)).getClickStartSample()-clicks.get(indexM1.get(i)).getClickStartSample();
					
					delaySecs[i] = (delaySamples +delayStartSample)/ sampleRate;
										
//					System.out.println("Time delay: "+delaySamples);
//					System.out.println("delayStartSamples: "+delayStartSample);
				
				}
			
			return delaySecs;
	}
	

	/**
	 * Sort clicks depending on classification/ being a part of an event/ echoes etc. Anything you want to program. 
	 */
	public ArrayList<ClickDetection> sortClicks(ArrayList<ClickDetection> clicks){
		
		System.out.println("ClickTypes: "+clickTypes);
		
//		for (int i = 0; i < 32; i++) {
//			System.out.println((1 << i & clickTypes));
//		}
		
		ArrayList<ClickDetection> clicksSorted=clicks;
		
		if(clickTypes!=null){
			 if ((1 << 0 & clickTypes)!=0){
				 System.out.println("ClickDetectionMatch.Selecting classified clicks");
				 clicksSorted=sortClassified( clicksSorted, clickDataUnit.getClickType());
			 }
			 if ((1 << 1 & clickTypes)!=0){
				 System.out.println("ClickDetectionMatch.Selecting event clicks");
				 clicksSorted=sortEvents( clicksSorted, clickDataUnit.getSuperDetection(0));
			 }		 
			 if ((1 << 3 & clickTypes)!=0){
				 System.out.println("ClickDetectionMatch.Selecting Echoes");
				 clicksSorted=sortClicksEchoes(clicksSorted);
			 }
			 if ((1 << 2 & clickTypes)!=0){
				 System.out.println("ClickDetectionMatch.Selecting all clicks");
				 clicksSorted=clicks;
			 }
		}
		
		return clicksSorted;
		
	}
	
	
	/**
	 * Take a list of click detections and sort into channels, based on time windows (which are themselves based on spacing between hydrophones);
	 * @param clicksSorted
	 */
	public ArrayList<ArrayList<ClickDetectionLite>> sortChannels(ArrayList<ClickDetection> clicksSorted){
		
		int[] channels;
		long startTimePrim=clickDataUnit.getStartSample();
		long startTime;
		double timeWindow;
		double[] waveform;
		
		
		ArrayList<ArrayList<ClickDetectionLite>> clickDetectionsAll=new ArrayList<ArrayList<ClickDetectionLite>>();
		
		ArrayList<ClickDetectionLite> clickDetections;
		double[] timeWindows=getTimeWindows();
		int[] hydrophoneMap=PamUtils.getChannelArray(clickDataUnit.getParentDataBlock().getChannelMap());
		

		for (int j=0; j<getNumberofHydrophones();j++){

			timeWindow=(timeWindows[j]*clickDataUnit.getClickDetector().getSampleRate());
			
			
			clickDetections=new ArrayList<ClickDetectionLite>();
			
			
			if (hydrophoneMap[j]==getPrimaryChannel()){
				clickDetections.add(new ClickDetectionLite(clickDataUnit.getTimeMilliseconds(),clickDataUnit.getStartSample(),clickDataUnit.getWaveData(0),j,clickDataUnit.getClickDetector().getSampleRate()));
			}
			else{
				for (int k=0; k<clicksSorted.size();k++){
				
					startTime=clicksSorted.get(k).getStartSample();
					channels=PamUtils.getChannelArray(clicksSorted.get(k).getChannelBitmap());

					if (startTime<startTimePrim+timeWindow && startTime>startTimePrim-timeWindow){
				
						for (int l=0; l<channels.length;l++){
							
							if (hydrophoneMap[j]==channels[l]){
								waveform=clicksSorted.get(k).getWaveData(l);
								clickDetections.add(new ClickDetectionLite(clicksSorted.get(k).getTimeMilliseconds(),clicksSorted.get(k).getStartSample(),waveform,j,sampleRate));
							}
						}
					}
				}
			}
			clickDetectionsAll.add(clickDetections);
		}
				
		return clickDetectionsAll;
	}
	
	
	/**
	 * Delete all clicks which are not the same type as the primary click.
	 * @param clicks
	 * @param clickType
	 * @return an arrayList of clickDetections containing only clicks of the same classification as the primary click.
	 */
	public static ArrayList<ClickDetection> sortClassified(ArrayList<ClickDetection> clicks, byte clickType){
		System.out.println("Selecting only classified clicks...");
		if (clicks==null) return null;
		ArrayList<ClickDetection> sortedClicks=new ArrayList<ClickDetection>();
		for (int i=0; i<clicks.size();i++){
			if (clicks.get(i).getClickType()==clickType){
				sortedClicks.add(clicks.get(i));
			}
		}
		return sortedClicks;
	}
	
	/**
	 * Delete all clicks which are not the same event as the primary click.
	 * @param clicks
	 * @param currentEvent
	 * @return an arrayList of clickDetections containing only clicks of the same event as the primary click.
	 */
	public static ArrayList<ClickDetection> sortEvents(ArrayList<ClickDetection> clicks, PamDetection currentEvent){
		System.out.println("Selecting only clicks within an event...");
		PamDetection tempEvent;
		if (clicks==null) return null;
		if (currentEvent==null) return null;
		
		ArrayList<ClickDetection> sortedClicks=new ArrayList<ClickDetection>();
		
		for (int i=0; i<clicks.size();i++){
			tempEvent=clicks.get(i).getSuperDetection(0);
			if (tempEvent==currentEvent){
				sortedClicks.add(clicks.get(i));
			}	
		}
		return sortedClicks;
	}
	
	

//	/**
//	 * Delete all clicks which are not the same event or classification as the primary click.
//	 * @param clicks
//	 * @param currentEvent
//	 * @return an arrayList of clickDetections containing only clicks of the same event as the primary click.
//	 */
//	public static ArrayList<ClickDetection> sortEventsClassified(ArrayList<ClickDetection> clicks, PamDetection currentEvent, byte ClickType){
//		System.out.println("Selecting only classifed clicks within an event.");
//		PamDetection tempEvent;
//		if (clicks==null) return null;
//		if (currentEvent==null) return null;
//		
//		ArrayList<ClickDetection> sortedClicks=new ArrayList<ClickDetection>();
//		
//		for (int i=0; i<clicks.size();i++){
//			tempEvent=clicks.get(i).getSuperDetection(0);
//			if (tempEvent==currentEvent &&  clicks.get(i).getClickType()==	ClickType){
//				sortedClicks.add(clicks.get(i));
//			}	
//		}
//		return sortedClicks;
//	}
	
	/**
	 * Delete all clicks which are echoes
	 * @param clicks
	 * @param currentEvent
	 * @return an arrayList of clickDetections containing only clicks which are not echoes
	 */
	public static ArrayList<ClickDetection> sortClicksEchoes(ArrayList<ClickDetection> clicks){
		System.out.println("Selecting only classified clicks...");
		if (clicks==null) return null;
		ArrayList<ClickDetection> sortedClicks=new ArrayList<ClickDetection>();
		for (int i=0; i<clicks.size();i++){
			if (clicks.get(i).isEcho()==false){
				sortedClicks.add(clicks.get(i));
			}
		}
		return sortedClicks;
	}
	
	public void setClickType(int clickTypes){
		this.clickTypes=clickTypes;
	}
	
	public int getClickType(){
		return clickTypes;
	}
	
	@Override
	public void setDetectionType(Integer clickTypes){
		this.clickTypes=clickTypes;
	}
	
	private ArrayList<ClickDetection> getSortedClicks(){
		ArrayList<PamDataUnit> clicksAbstract= getDetectionGroup();
		
		//need to cast to clickDetection
		ArrayList<ClickDetection> clicks=new ArrayList<ClickDetection>();
		for (int i=0; i<clicksAbstract.size(); i++){
			clicks.add((ClickDetection) clicksAbstract.get(i));
		}
		
		//sort clicks
		ArrayList<ClickDetection> clicksSorted=sortClicks(clicks);

		return clicksSorted;
	}

	
	@Override
	public int getNPossibilities() {
		
		ArrayList<ClickDetection> clicksSorted=getSortedClicks();
		
		if (clicksSorted==null) return 0;
		
		ArrayList<ArrayList<ClickDetectionLite>> clickDetectionsAll= sortChannels(clicksSorted);
		
		int N=1;
		for (int i=0; i<clickDetectionsAll.size();i++){
			if (clickDetectionsAll.get(i).size()!=0){
			N=N*clickDetectionsAll.get(i).size();
			}
		}
		
		return N;
	}

	@Override
	public int getNumberofNonNullDelays() {
		ArrayList<ClickDetection> clicksSorted=getSortedClicks();
		ArrayList<ArrayList<ClickDetectionLite>> clickDetectionsAll= sortChannels(clicksSorted);
		int N=0; 
		for (int i=0; i<clickDetectionsAll.size(); i++){
			if (clickDetectionsAll.get(i).size()>0) N++;
		}
		return calcTDNumber(N);
	}
		


}
