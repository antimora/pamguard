package PamDetection;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import clickDetector.ClickDetection;

import pamMaths.PamVector;

import Array.ArrayManager;
import Array.Hydrophone;
import Array.PamArray;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * This class aims to match detections picked up on different hydrophones, primarily on WIDE APERTURE arrays. Although this technqiue should also work on closely spaced elements AbstractLocalisation should be used instead. 
 * Note that matching detections on different hydrophones is the initial step taken in localisation using wide aperture arrays. Often there are multiple possibilities for the correct detection on each hydrophone. These can be somewhat reduced, for example by using classifiers, although it may be necessary to 
 * pass multiple possible combinations of clicks (i.e multiple sets of time delays) to the localisation algorithm.   
 * @author Jamie Macaulay
 *
 */

abstract public class AbstractDetectionMatch implements DetectionMatchModel{
	

	
	boolean unsynchronised=false;
 	 
	private PamDataUnit pamDataUnit;
	
	private PamDataBlock pamParentDetection;
	
	protected Integer detectionType=0;
	
	ArrayManager arrayManager=ArrayManager.getArrayManager();
	
	private PamArray currentArray;
	
	int[] hydrophoneMap;
	
	
	
	/**
	 * The selected click is regarded as belonging to the 'primary hydrophone' If the click is part of the group, the primary hydrophone is the first click in tat group.
	 */
	private int primaryChannel;
	


	public AbstractDetectionMatch(PamDataUnit pamDataUnit) {
		super();
		this.pamDataUnit = pamDataUnit;
		this.pamParentDetection=pamDataUnit.getParentDataBlock();
		primaryChannel=PamUtils.getChannelArray(pamDataUnit.getChannelBitmap())[0];
		hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());
	}
	
	public AbstractDetectionMatch(PamDataUnit pamDataUnit, Integer detectionType) {
		super();
		this.pamDataUnit = pamDataUnit;
		this.pamParentDetection=pamDataUnit.getParentDataBlock();
		this.detectionType=detectionType;
		primaryChannel=PamUtils.getChannelArray(pamDataUnit.getChannelBitmap())[0];
		hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());
	}
	
	

	
	/**
	 * We want to be into and out of the PamDataBlock as quickly and as few times as possible. So find the max time window, grab all clicks on all channels within this time window and 
	 * then we can sort them out once allocated to a different section of memory. 
	 * @return all detections within the maximum time window of pamDataUnit.
	 */
	public ArrayList<PamDataUnit> getDetectionGroup(){
		
		double[] timeWindows=getTimeWindows(); 
		double  maxTime=0;
		for (int i=0; i<timeWindows.length;i++){
			if (maxTime<timeWindows[i]){
				maxTime=timeWindows[i];
			}
		}
		
		maxTime=Math.ceil(maxTime*1000);
		
		//System.out.println("maxTime: "+maxTime);
		long min=(long) (pamDataUnit.getTimeMilliseconds()-(maxTime*1.2));
		long max=(long) (pamDataUnit.getTimeMilliseconds()+(maxTime*1.2));
		
		ArrayList<PamDataUnit> detectionsWithinWindow=pamParentDetection.searchUnitsinInterval(min,max);
		
		//TODO
		//this will be applied to datablocks which contain units of unsynchronised data
		if (unsynchronised==true){
			ArrayList<PamDataUnit> detectionsWithinWindowUnsync=new ArrayList<PamDataUnit>();
			for (int i=0; i<detectionsWithinWindow.size(); i++){
				
			}
		}
		//////////////////////
		
		
		return detectionsWithinWindow;
	}
	
	

	/**
	 * 
	 * @return the number of hydrophones in the current array.
	 */
	public int getNumberofHydrophones(){
		return PamUtils.getNumChannels(pamDataUnit.getParentDataBlock().getChannelMap());
	}
	
	/**
	 * Get the current array and calculate the distances between every hydrophone element;
	 * @return
	 */
	public double[] getHydrophoneDistances(){
		
		currentArray=arrayManager.getCurrentArray();
		hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());

		
		Hydrophone H;
		PamVector primaryHCoOrd;
		PamVector HCoOrd;
		double dist;
		
		int N=getNumberofHydrophones();
		
	  	primaryHCoOrd=new PamVector(currentArray.getHydrophone(primaryChannel).getCoordinates());
	  	
		double[] hydrophoneDistances=new double[N];
		//int[] hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());
		
		
		
		for (int i=0; i<N; i++){
			H=currentArray.getHydrophone(hydrophoneMap[i]);
			HCoOrd=new PamVector(H.getCoordinates());
			dist=HCoOrd.dist(primaryHCoOrd);
			hydrophoneDistances[i]=dist;
		}
		
		return hydrophoneDistances;
	}
	
	/**
	 * Takes a set of time delays and removes any which are between channels that included in the 'channels' array. 
	 * @param timedelays- the time delays to filter
	 * @param channels- the channels which SHOULD NOT BE used. 
	 * @return
	 */
	public ArrayList<ArrayList<Double>> filterTimeDelaysbyChannels(ArrayList<ArrayList<Double>> timedelays, int[] channels){
		
		if (channels==null) return timedelays;
		if (channels.length==0) return timedelays;
		
		ArrayList<Double> tdFiltered;
		ArrayList<ArrayList<Double>> tdFilteredAll= new ArrayList<ArrayList<Double>>();
		
		int N=getNumberofHydrophones();
		
		ArrayList<Integer> indexM1=indexM1(N);
		ArrayList<Integer> indexM2=indexM2(N);
		
		boolean includeTD=true; 
		
		for (int i=0; i<timedelays.size(); i++){
			tdFiltered=new ArrayList<Double>();
			 for (int j=0; j<timedelays.get(i).size(); j++){
				 includeTD=true; 
				 for (int k=0; k<channels.length; k++){
					 if (hydrophoneMap[indexM1.get(j)]==channels[k] || hydrophoneMap[indexM2.get(j)]==channels[k]){
						 includeTD=false;
					 }
				 }
				 if (includeTD==true) tdFiltered.add(timedelays.get(i).get(j));
				 else tdFiltered.add(null);
			 }
			 tdFilteredAll.add(tdFiltered);
		}
				
		return tdFilteredAll;
	}
	
	
	/**
	 * 
	 * @return a list of PamVectors denoting hydrophone positions;
	 */
	public ArrayList<PamVector> getHydrophones(){
		currentArray=arrayManager.getCurrentArray();
		hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());

		int N=getNumberofHydrophones();
		 ArrayList<PamVector> hydrophones=new ArrayList<PamVector>();
		//int[] hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());

		for (int i=0; i<N; i++){
			hydrophones.add(new PamVector(currentArray.getHydrophone(hydrophoneMap[i]).getCoordinates()));
		}
		
		return hydrophones;
	}
	
	/**
	 * 
	 * @return a list of Point3f's denoting hydrophone positions;
	 */
	public ArrayList<Point3f> getHydrophones3d(){
		currentArray=arrayManager.getCurrentArray();
		hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());

		double[] array;
		float[] arrayf;
		int N=getNumberofHydrophones();
		//int[] hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());
		ArrayList<Point3f> hydrophones=new  ArrayList<Point3f>();
		for (int i=0; i<N; i++){
			arrayf=new float[3];
			array=currentArray.getHydrophone(hydrophoneMap[i]).getCoordinates();
			arrayf[0]=(float) array[0];
			arrayf[1]=(float) array[1];
			arrayf[2]=(float) array[2];
			hydrophones.add(new Point3f(arrayf));
		}
		
		return  hydrophones;
	}
	

	/**
	 * Time windows are needed to match clicks. These correpsond to the inter element distances with an  array, Not that the time window for the primary hydrophone is 0; Hydrophones are ordered as in the array manager
	 * @return a double of the time window in SECONDS from the primary hydrophone to each other hydrophone within the array. Time windows are arranged by channels, i.e. timeWindows[0] is the time window between channel 0 and the primary hydrophone...etc
	 */
	public double[] getTimeWindows(){
		currentArray=arrayManager.getCurrentArray();
		int N=getNumberofHydrophones();
		double[] timeWindows=new double[N];
		double[] distances=getHydrophoneDistances();
		for (int i=0; i<N; i++){
			timeWindows[i]=distances[i]/currentArray.getSpeedOfSound();
		}
		return timeWindows;
	}
	
	
	/**
	 * 
	 * @return the reference channel used to create the groups. 
	 */
	public int getPrimaryChannel() {
		return primaryChannel;
	}
	
//	/**
//	 * Set the primary channel. 
//	 * @param primaryHydrophone
//	 */
//	public void setPrimaryChannel(int primaryHydrophone) {
//		this. primaryChannel=primaryHydrophone;
//	}
	
	/**
	 * 
	 * @return Parent detection containing this localisation information
	 */
	public PamDataUnit getDetection() {
		return pamDataUnit;
	}
	
	/**
	 * 
	 * @param parentDetection Parent detection containing this detection match information
	 */
	public void setDetection(PamDetection parentDetection) {
		this.pamDataUnit = parentDetection;
	} 
	
	/**
	 * 
	 * @return Parent detection
	 */
	public PamDataBlock getParentDetections() {
		return pamParentDetection;
	}
	

	/**
	 * Set Pamdatablock
	 * @param parentBlock
	 */
	public void setParentDetections(PamDataBlock parentBlock) {
		this. pamParentDetection=parentBlock;
	}
	
	 
	
	/**IndexM1 and IndexM2 specify which hydrophones to calculate time delays between. In the case of a paired array this will simply be the hydrophones in pairs 
	 * so Index M1 will be 0 and even numbers and IndexM1 will be odd numbers. 
	 * @return
	 */
	public static ArrayList<Integer> indexM1(int numberOfHydrophones){
		ArrayList<Integer> IndexM1=new ArrayList<Integer>();
		for (int i=0; i<numberOfHydrophones; i++){
			for (int j=0; j<numberOfHydrophones-(i+1);j++){
				int HN=j+i+1;
		IndexM1.add(HN);	
			}
		}
		return IndexM1;
	}

	
	public static ArrayList<Integer> indexM2(int numberOfHydrophones){
		ArrayList<Integer> IndexM2=new ArrayList<Integer>();
		for (int i=0; i<numberOfHydrophones; i++){
			for (int j=0; j<numberOfHydrophones-(i+1);j++){
				int HN=i;
		IndexM2.add(HN);	
			}
		}
		return IndexM2;
	}
	
	/**
	 * For N synchronised hydrophones there are N*(N-1)/2 time delays.  For N time delays there are 0.5+sqrt(1+2N)/2 hydrophones. This is a simple function which calculates the number of hydrophones from the number of time delays. 
	 * @return
	 */
	public static int calcHydrophoneNumber(int numberOfDelays){
		int hydrophoneNumber=(int) (0.5+Math.sqrt(1+8*numberOfDelays)/2);
		return hydrophoneNumber;
	}
	
	/**
	 * For N synchronised hydrophones there are N*(N-1)/2 time delays.  For N time delays there are 0.5+sqrt(1+2N)/2 hydrophone. This is a simple function which calculates the number of time delays from the number of hydrophones. 
	 * @return
	 */
	public static int calcTDNumber(int numberofHydrophones){
		int tdNumber=(int) numberofHydrophones*(numberofHydrophones-1)/2;
		return tdNumber;
	}

	
	/**
	 * Returns the location time delay error between hydrophones H0 and H1 based on location error specified by the user in the hydophone array manager. 
	 * @param channel number H0
	 * @param channel number H1
	 * @param speed of sound c
	 * @return The time delay error between H1 and H2, based on location errors. 
	 */
	public static double getTimeDelayError(int H0, int H1, PamArray currentArray) {
		
		double[] error0=currentArray.getHydrophone(H0).getCoordinateErrors();
		double[] error1=currentArray.getHydrophone(H1).getCoordinateErrors();
		
		double totSqrd=0;
		for (int i=0; i<3; i++){
			totSqrd+=Math.pow(error0[i],2)+Math.pow(error1[i],2);
		}
		
		double tdError=Math.sqrt(totSqrd)/currentArray.getSpeedOfSound(); 
	
		return tdError;
	}
	
	/**
	 * Calculate all the time delay errors for this pamDetection. 
	 * @return an ArrayList of td delay errors based on IndexM1/M2 
	 */
	public ArrayList<Double> getTDErrrorsAll(){
		currentArray=arrayManager.getCurrentArray();
		hydrophoneMap=PamUtils.getChannelArray(pamDataUnit.getParentDataBlock().getChannelMap());

		int N=getNumberofHydrophones();
		
		ArrayList<Integer> indexM1=indexM1(N);
		ArrayList<Integer> indexM2=indexM2(N);

		ArrayList<Double> timeDelays=new ArrayList<Double>();
		
		for (int i=0; i<indexM1.size(); i++){
			timeDelays.add(getTimeDelayError(hydrophoneMap[indexM1.get(i)], hydrophoneMap[indexM2.get(i)],currentArray));
		}
		
		return timeDelays;
	}


	public void setDetectionType(Integer detectionType) {
		this.detectionType=detectionType;
	}
	
	public Integer getDetectionType() {
		return this.detectionType;
	}


}
